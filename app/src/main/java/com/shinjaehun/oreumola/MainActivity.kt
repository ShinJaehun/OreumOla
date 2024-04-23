package com.shinjaehun.oreumola

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapType
import com.kakao.vectormap.MapView
import com.kakao.vectormap.MapViewInfo
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextStyle
import com.kakao.vectormap.label.LabelTransition
import com.kakao.vectormap.label.Transition
import timber.log.Timber
import java.lang.Exception
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        KakaoMapSdk.init(this, BuildConfig.KAKAO_APP_KEY)

        getHashKey()
        val mapView = findViewById<MapView>(R.id.map_view)

        mapView.start(object : MapLifeCycleCallback() {
          override fun onMapDestroy() {
            // 지도 API 가 정상적으로 종료될 때 호출됨
            Toast.makeText(applicationContext, "onMapDestroy", Toast.LENGTH_SHORT).show()
          }

          override fun onMapError(error: Exception?) {
            Timber.tag(TAG).i("error: %s", error?.message)
            Toast.makeText(applicationContext, error?.message, Toast.LENGTH_SHORT).show()
          }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(kakaoMap: KakaoMap) {
                val pos = LatLng.from(33.471374, 126.541913)

                Toast.makeText(applicationContext, "onMapReady", Toast.LENGTH_SHORT).show()
                val labelLayer = kakaoMap.labelManager?.layer
                val styles = kakaoMap.labelManager?.addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.yellow_marker)
                    .setTextStyles(LabelTextStyle.from(baseContext, R.style.labelTextStyle_1))
                    .setIconTransition(LabelTransition.from(Transition.None, Transition.None))))

                labelLayer?.addLabel(LabelOptions.from("뭐야", pos).setStyles(styles)
                    .setTexts("해우와 녹우의 집"))
            }

            override fun getPosition(): LatLng {
                return LatLng.from(33.471374, 126.541913)
            }

            override fun getZoomLevel(): Int {
                return 17
            }

//            override fun getMapViewInfo(): MapViewInfo {
//                return MapViewInfo.from(MapType.NORMAL.toString())
//            }

            override fun getViewName(): String {
                return "OreumOla"
            }

            override fun isVisible(): Boolean {
                return true
            }

            override fun getTag(): Any {
                return "OreumOlaTag"
            }

        })

    }

    private fun getHashKey() {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        if (packageInfo == null) Log.e("KeyHash", "KeyHash:null")
        for (signature in packageInfo!!.signatures) {
            try {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            } catch (e: NoSuchAlgorithmException) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=$signature", e)
            }
        }
    }
}