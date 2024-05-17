package com.shinjaehun.oreumola.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.LabelTransition
import com.kakao.vectormap.label.Transition
import com.kakao.vectormap.shape.DotPoints
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.PolylineOptions
import com.kakao.vectormap.shape.ShapeManager
import com.shinjaehun.oreumola.BuildConfig
import com.shinjaehun.oreumola.R
import com.shinjaehun.oreumola.databinding.FragmentTrackingBinding
import com.shinjaehun.oreumola.other.Constants.ACTION_PAUSE_SERVICE
import com.shinjaehun.oreumola.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.shinjaehun.oreumola.other.Constants.DURATION
import com.shinjaehun.oreumola.other.Constants.MAP_ZOOM
import com.shinjaehun.oreumola.other.Constants.POLYLINE_COLOR
import com.shinjaehun.oreumola.other.Constants.POLYLINE_WIDTH
import com.shinjaehun.oreumola.other.TrackingUtility
import com.shinjaehun.oreumola.services.Polyline
import com.shinjaehun.oreumola.services.TrackingService
import com.shinjaehun.oreumola.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

private const val TAG = "TrackingFragment"

@AndroidEntryPoint
class TrackingFragment: Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    private var kakaoMap: KakaoMap? = null

    private var shapeManager: ShapeManager? = null

    private val pos = LatLng.from(33.471374, 126.541913)

    private var curTimeInMillis = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KakaoMapSdk.init(requireContext(), BuildConfig.KAKAO_APP_KEY)

        binding.btnToggleRun.setOnClickListener {
//            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            toggleRun()
        }

        binding.mapView.start(lifeCycleCallback, readyCallback)

        subscribeToObserver()
    }

    private fun subscribeToObserver() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            binding.tvTimer.text = formattedTime
        })
    }

    private fun toggleRun() {
        if(isTracking) {
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.btnToggleRun.text = "Start"
            binding.btnFinishRun.visibility = View.VISIBLE
        } else {
            binding.btnToggleRun.text = "Stop"
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            kakaoMap?.moveCamera(
                CameraUpdateFactory.newCenterPosition(pathPoints.last().last(), MAP_ZOOM),
                CameraAnimation.from(DURATION))
        }
    }

    private fun addAllPolylines(){
        for(polyline in pathPoints) {
            val polylineOptions = PolylineOptions.from(
                MapPoints.fromLatLng(polyline),
                POLYLINE_WIDTH, POLYLINE_COLOR
            )
            shapeManager?.getLayer()?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline(){
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
           val preLastLatLng = pathPoints.last()[pathPoints.last().size-2]
           val lastLatLng = pathPoints.last().last()
           val polylineOptions = PolylineOptions.from(
               MapPoints.fromLatLng(mutableListOf(preLastLatLng, lastLatLng)),
                   POLYLINE_WIDTH, POLYLINE_COLOR)

            shapeManager?.getLayer()?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }


    private val lifeCycleCallback = object : MapLifeCycleCallback() {
        override fun onMapResumed() {
            super.onMapResumed()
//            Toast.makeText(requireContext(), "onMapResumed", Toast.LENGTH_SHORT).show()
        }

        override fun onMapPaused() {
            super.onMapPaused()
//            Toast.makeText(requireContext(), "onMapPaused", Toast.LENGTH_SHORT).show()
        }

        override fun onMapDestroy() {
            // 지도 API 가 정상적으로 종료될 때 호출됨
            Toast.makeText(requireContext(), "onMapDestroy", Toast.LENGTH_SHORT).show()
        }

        override fun onMapError(error: Exception?) {
            Timber.tag(TAG).i("error: %s", error?.message)
            Toast.makeText(requireContext(), error?.message, Toast.LENGTH_SHORT).show()
        }
    }

    private val readyCallback = object : KakaoMapReadyCallback() {
        override fun onMapReady(map: KakaoMap) {

            kakaoMap = map
            shapeManager = kakaoMap!!.shapeManager!!

            addAllPolylines()

//            Toast.makeText(requireContext(), "onMapReady", Toast.LENGTH_SHORT).show()
            val labelLayer = kakaoMap!!.labelManager?.layer
            val styles = kakaoMap!!.labelManager?.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(R.drawable.yellow_marker)
                        .setTextStyles(LabelTextStyle.from(requireContext(), R.style.labelTextStyle_1))
                        .setIconTransition(LabelTransition.from(Transition.None, Transition.None))))

            labelLayer?.addLabel(
                LabelOptions.from("뭐야", pos).setStyles(styles)
                    .setTexts("해우와 녹우의 집"))
        }

        override fun getPosition(): LatLng {
            return pos
        }

        override fun getZoomLevel(): Int {
            return 17
        }

//        override fun getViewName(): String {
//            return "OreumOla"
//        }
//
//        override fun isVisible(): Boolean {
//            return true
//        }
//
//        override fun getTag(): Any {
//            return "OreumOlaTag"
//        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.resume() // 어쨌든 official document에서 하라고 하니까...
//        Toast.makeText(requireContext(), "onResumed", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.pause() // 어쨌든 official document에서 하라고 하니까...
//        Toast.makeText(requireContext(), "onPaused", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }

}