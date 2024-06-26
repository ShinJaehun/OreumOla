package com.shinjaehun.oreumola.ui.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.opengl.GLException
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.graphics.gl.GLSurfaceView
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.LabelTransition
import com.kakao.vectormap.label.Transition
import com.kakao.vectormap.shape.MapPoints
import com.kakao.vectormap.shape.PolylineOptions
import com.kakao.vectormap.shape.ShapeManager
import com.shinjaehun.oreumola.BuildConfig
import com.shinjaehun.oreumola.R
import com.shinjaehun.oreumola.databinding.FragmentTrackingBinding
import com.shinjaehun.oreumola.db.Run
import com.shinjaehun.oreumola.other.Constants.ACTION_PAUSE_SERVICE
import com.shinjaehun.oreumola.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.shinjaehun.oreumola.other.Constants.ACTION_STOP_SERVICE
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
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.IntBuffer
import java.util.Calendar
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10
import kotlin.math.round

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

    private var menu: Menu? = null

    private var weight = 80f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentTrackingBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KakaoMapSdk.init(requireContext(), BuildConfig.KAKAO_APP_KEY)

        binding.btnToggleRun.setOnClickListener {
            toggleRun()
        }

        binding.btnFinishRun.setOnClickListener {
            // 이상하게 이렇게 처리하니까...
            // zoom하기 전에 지도를 캡처해버렸다?? 이유는 모르겠음.
//            zoomToSeeWholeTrack()
            endRunAndSaveToDb(binding.mapView.surfaceView as GLSurfaceView)
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
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
            // 걷다가 중지할 때마다 지금까지 경로를 zoom하는 걸로...?
            zoomToSeeWholeTrack()
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (curTimeInMillis > 0L) {
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelTrackingDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run?")
            .setMessage("Are you sure to cancel the current run and delete all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _, _ ->
                stopRun()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun stopRun() {
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if(!isTracking) {
            binding.btnToggleRun.text = "Start"
            binding.btnFinishRun.visibility = View.VISIBLE
        } else {
            binding.btnToggleRun.text = "Stop"
            menu?.getItem(0)?.isVisible = true
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

    private fun zoomToSeeWholeTrack() {
        // 어쨌든 이건 정상적으로 작동함!

        var points = mutableListOf<LatLng>()
        for (polyline in pathPoints) {
            for (point in polyline) {
                points.add(point)
            }
        }

        if(pathPoints.isNotEmpty()) {
            kakaoMap?.moveCamera(CameraUpdateFactory.fitMapPoints(points.toTypedArray(), 50))
        }
    }

//    private fun endRunAndSaveToDb() {
//        screenShot(binding.mapView.surfaceView as GLSurfaceView)
////        val bitmap = screenShot(activity?.window!!.decorView!!.rootView) // for fragment
//
//        stopRun()
//    }

    private fun bitmapToImage(bitmap: Bitmap, filename: String): Boolean {
        val path = requireActivity().getExternalFilesDir(null)
        val folder = File(path, "images")
        folder.mkdirs()
        val outputFile = File(folder, filename)
        try {
            val outputStream = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            return true
        } catch (e: FileNotFoundException) {
            Timber.e("FileNotFoundException : " + e.message)
        } catch (e: IOException) {
            Timber.e("IOException : " + e.message)
        }
        return false
    }

    private fun endRunAndSaveToDb(surfaceView: GLSurfaceView) {
        val fileName = "MapCapture_" + System.currentTimeMillis() + ".png"
        surfaceView.queueEvent(object: Runnable{
            // 어쨌든 이 과정을 Runnable에서 한다는 거 자체가 맘에 안들어
            override fun run() {
                val egl = EGLContext.getEGL() as EGL10
                val gl = egl.eglGetCurrentContext().gl as GL10

                val bitmap = createBitmapFromGLSurface(0, 0, surfaceView.width, surfaceView.height, gl)
                if (bitmap == null) {
                    Timber.e("bitmap 파일 생성 실패(null)")
                    Snackbar.make(requireActivity().findViewById(R.id.rootView),
                        "Failed to save run", Snackbar.LENGTH_LONG).show()
                    stopRun()
                    return // 이렇게 종료해도 되나요?
                }

                var distanceInMeters = 0
                for (polyline in pathPoints) {
                    distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
                }
                val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
                val dateTimestamp = Calendar.getInstance().timeInMillis
                val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
                val run = Run(bitmap, dateTimestamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)
                viewModel.insertRun(run)
                Snackbar.make(requireActivity().findViewById(R.id.rootView),
                    "Run saved successfully", Snackbar.LENGTH_LONG).show()

                bitmapToImage(bitmap, fileName)
                stopRun()

                // 이렇게 하면 안 되는 이유가 멀까?
//                createBitmapFromGLSurface(0, 0, surfaceView.width, surfaceView.height, gl).also {
//                    if (it == null) {
//                        Timber.e("bitmap 파일 생성 실패(null)")
//                        Snackbar.make(requireActivity().findViewById(R.id.rootView),
//                            "Failed to save run", Snackbar.LENGTH_LONG).show()
//                    } else {
////                        bitmapToImage(it, fileName)
//
//                        var distanceInMeters = 0
//                        for (polyline in pathPoints) {
//                            distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
//                        }
//                        val avgSpeed = round((distanceInMeters / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
//                        val dateTimestamp = Calendar.getInstance().timeInMillis
//                        val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
//                        val run = Run(null, dateTimestamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)
//                        viewModel.insertRun(run)
//                        Snackbar.make(requireActivity().findViewById(R.id.rootView),
//                            "Run saved successfully", Snackbar.LENGTH_LONG).show()
//
//                    }
//                }
//
//                stopRun()

                // 예제에는 이런 식으로 구현되어 있던데... (callback 이용)
//                val isSucceed: Boolean = bitmapToImage(bitmap!!, fileName)
//                activity!!.runOnUiThread(object: Runnable{
//                    override fun run() {
//
//                    }
//
//                })

            }
        })
   }

    private fun createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int, gl: GL10): Bitmap? {
        val bitmapBuffer = IntArray(w * h)
        val bitmapSource = IntArray(w * h)
        val intBuffer = IntBuffer.wrap(bitmapBuffer)
        intBuffer.position(0)
        try {
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
            var offset1: Int
            var offset2: Int
            for (i in 0 until h) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0 until w) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
        } catch (e: GLException) {
            Timber.e("GLException : " + e.message)
            return null
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }

}