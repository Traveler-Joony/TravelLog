package com.jay.travellog.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * 지도 화면 placeholder.
 * Day 9에서 지도 SDK 연동 + fragment_map.xml 인플레이트로 교체합니다.
 */
class MapFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = TextView(requireContext()).apply {
        text = "🗺  지도 화면\n\nDay 9에서 지도 SDK를 연결합니다."
        textSize = 16f
        gravity = Gravity.CENTER
    }
}
