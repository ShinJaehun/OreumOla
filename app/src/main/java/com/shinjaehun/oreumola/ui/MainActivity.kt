package com.shinjaehun.oreumola.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shinjaehun.oreumola.R
import com.shinjaehun.oreumola.db.RunDAO
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var runDAO: RunDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        Log.d("runDao", "RUNDAO: ${runDAO.hashCode()}")

//        KakaoMapSdk.init(this, BuildConfig.KAKAO_APP_KEY)
//
//        getHashKey()
//        val mapView = findViewById<MapView>(R.id.map_view)
//
//        mapView.start(object : MapLifeCycleCallback() {
//          override fun onMapDestroy() {
//            // 지도 API 가 정상적으로 종료될 때 호출됨
//            Toast.makeText(applicationContext, "onMapDestroy", Toast.LENGTH_SHORT).show()
//          }
//
//          override fun onMapError(error: Exception?) {
//            Timber.tag(TAG).i("error: %s", error?.message)
//            Toast.makeText(applicationContext, error?.message, Toast.LENGTH_SHORT).show()
//          }
//        }, object : KakaoMapReadyCallback() {
//            override fun onMapReady(kakaoMap: KakaoMap) {
//                val pos = LatLng.from(33.471374, 126.541913)
//
//                Toast.makeText(applicationContext, "onMapReady", Toast.LENGTH_SHORT).show()
//                val labelLayer = kakaoMap.labelManager?.layer
//                val styles = kakaoMap.labelManager?.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.yellow_marker)
//                    .setTextStyles(LabelTextStyle.from(baseContext, R.style.labelTextStyle_1))
//                    .setIconTransition(LabelTransition.from(Transition.None, Transition.None))))
//
//                labelLayer?.addLabel(LabelOptions.from("뭐야", pos).setStyles(styles)
//                    .setTexts("해우와 녹우의 집"))
//            }
//
//            override fun getPosition(): LatLng {
//                return LatLng.from(33.471374, 126.541913)
//            }
//
//            override fun getZoomLevel(): Int {
//                return 17
//            }
//
////            override fun getMapViewInfo(): MapViewInfo {
////                return MapViewInfo.from(MapType.NORMAL.toString())
////            }
//
//            override fun getViewName(): String {
//                return "OreumOla"
//            }
//
//            override fun isVisible(): Boolean {
//                return true
//            }
//
//            override fun getTag(): Any {
//                return "OreumOlaTag"
//            }
//
//        })

    }

//    private fun getHashKey() {
//        var packageInfo: PackageInfo? = null
//        try {
//            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//        }
//        if (packageInfo == null) Log.e("KeyHash", "KeyHash:null")
//        for (signature in packageInfo!!.signatures) {
//            try {
//                val md: MessageDigest = MessageDigest.getInstance("SHA")
//                md.update(signature.toByteArray())
//                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
//            } catch (e: NoSuchAlgorithmException) {
//                Log.e("KeyHash", "Unable to get MessageDigest. signature=$signature", e)
//            }
//        }
//    }
}