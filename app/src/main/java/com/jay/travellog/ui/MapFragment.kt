package com.jay.travellog.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jay.travellog.R
import com.jay.travellog.data.DBHelper
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var naverMap: NaverMap? = null
    private val infoWindow = InfoWindow()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = root.findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return root
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        showMarkers(map)
    }

    private fun showMarkers(map: NaverMap) {
        val records = DBHelper(requireContext()).getAllRecords()
        val located = records.filter { it.latitude != null && it.longitude != null }

        if (located.isEmpty()) {
            // 좌표 있는 기록이 없으면 기본 위치(서울)만 표시
            map.cameraPosition = CameraPosition(LatLng(37.5666, 126.9784), 11.0)
            return
        }

        located.forEach { record ->
            val marker = Marker()
            marker.position = LatLng(record.latitude!!, record.longitude!!)
            marker.captionText = record.place
            marker.map = map
            marker.setOnClickListener {
                // 마커 탭 → 정보 창에 여행지명 + 날짜 표시
                infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(requireContext()) {
                    override fun getText(iw: InfoWindow): CharSequence =
                        "${record.place}\n${record.visitDate}"
                }
                infoWindow.open(marker)
                true
            }
        }

        // 카메라를 마커들이 모두 보이도록 이동
        if (located.size == 1) {
            val only = LatLng(located[0].latitude!!, located[0].longitude!!)
            map.moveCamera(CameraUpdate.scrollTo(only))
            map.moveCamera(CameraUpdate.zoomTo(14.0))
        } else {
            val bounds = LatLngBounds.Builder().apply {
                located.forEach { include(LatLng(it.latitude!!, it.longitude!!)) }
            }.build()
            map.moveCamera(CameraUpdate.fitBounds(bounds, 120))
        }
    }

    // ───────── MapView 생명주기 전달 ─────────
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState)
    }
    override fun onDestroyView() { mapView.onDestroy(); super.onDestroyView() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
}
