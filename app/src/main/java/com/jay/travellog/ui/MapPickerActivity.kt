package com.jay.travellog.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.jay.travellog.R
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var naverMap: NaverMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val target = naverMap?.cameraPosition?.target ?: return@setOnClickListener
            val data = Intent().apply {
                putExtra(EXTRA_LAT, target.latitude)
                putExtra(EXTRA_LNG, target.longitude)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        // 전달받은 좌표가 있으면 그 위치에서 시작, 없으면 서울
        val lat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)
        val start = if (!lat.isNaN() && !lng.isNaN()) LatLng(lat, lng) else LatLng(37.5666, 126.9784)
        map.cameraPosition = CameraPosition(start, 15.0)
    }

    // ───────── MapView 생명주기 전달 ─────────
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState)
    }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
    }
}
