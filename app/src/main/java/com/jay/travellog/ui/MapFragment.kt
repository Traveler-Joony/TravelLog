package com.jay.travellog.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jay.travellog.R
import com.jay.travellog.data.DBHelper
import com.jay.travellog.util.GeoUtils
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

    // 현재 정보 창이 가리키는 기록 번호 (정보 창 탭 시 상세로 이동)
    private var selectedNo: Int = -1

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

        // 정보 창을 누르면 해당 기록의 상세 화면으로 이동
        infoWindow.setOnClickListener {
            if (selectedNo > 0) {
                val intent = Intent(requireContext(), DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_NO, selectedNo)
                startActivity(intent)
            }
            true
        }

        showMarkers(map)
    }

    private fun showMarkers(map: NaverMap) {
        // getAllRecords는 실패 시 빈 리스트 반환(크래시 없음). 좌표는 유효성까지 검사.
        val located = DBHelper(requireContext()).getAllRecords().filter {
            it.latitude != null && it.longitude != null &&
                GeoUtils.isValidLatLng(it.latitude!!, it.longitude!!)
        }

        if (located.isEmpty()) {
            map.cameraPosition = CameraPosition(LatLng(37.5666, 126.9784), 11.0)
            return
        }

        located.forEach { record ->
            val marker = Marker()
            marker.position = LatLng(record.latitude!!, record.longitude!!)
            marker.captionText = record.place
            marker.map = map
            marker.setOnClickListener {
                // 마커 탭 → 정보 창에 여행지명 + 날짜 + 안내문, 대상 기록 기억
                selectedNo = record.no
                infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(requireContext()) {
                    override fun getText(iw: InfoWindow): CharSequence =
                        "${record.place}\n${record.visitDate}\n(탭하여 상세 보기)"
                }
                infoWindow.open(marker)
                true
            }
        }

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
