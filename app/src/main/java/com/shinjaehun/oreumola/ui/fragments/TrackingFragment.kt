package com.shinjaehun.oreumola.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.LabelTransition
import com.kakao.vectormap.label.Transition
import com.shinjaehun.oreumola.BuildConfig
import com.shinjaehun.oreumola.R
import com.shinjaehun.oreumola.databinding.FragmentTrackingBinding
import com.shinjaehun.oreumola.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

private const val TAG = "TrackingFragment"

@AndroidEntryPoint
class TrackingFragment: Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentTrackingBinding? = null
    private val binding get() = _binding!!

    private val pos = LatLng.from(33.471374, 126.541913)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KakaoMapSdk.init(requireContext(), BuildConfig.KAKAO_APP_KEY)

        binding.mapView.start(lifeCycleCallback, readyCallback)
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
        override fun onMapReady(kakaoMap: KakaoMap) {

            Toast.makeText(requireContext(), "onMapReady", Toast.LENGTH_SHORT).show()
            val labelLayer = kakaoMap.labelManager?.layer
            val styles = kakaoMap.labelManager?.addLabelStyles(
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