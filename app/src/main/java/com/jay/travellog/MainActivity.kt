package com.jay.travellog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jay.travellog.ui.ListFragment
import com.jay.travellog.ui.MapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)

        // 백스택이 바뀔 때마다 현재 화면에 맞춰 하단 탭 하이라이트를 동기화
        supportFragmentManager.addOnBackStackChangedListener { syncBottomNav() }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_list -> goHome()   // 목록(홈)
                R.id.nav_map -> openMap()   // 지도
            }
            true
        }

        // 최초 진입: 목록 화면 (백스택에 쌓지 않음 → 목록에서 뒤로가기 시 앱 종료)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ListFragment(), TAG_LIST)
                .commit()
        }
    }

    /** 목록(홈)으로: 백스택을 모두 비워 목록만 남긴다. */
    private fun goHome() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    /** 지도 화면을 백스택에 쌓아 띄운다. (뒤로가기 시 목록으로 복귀) */
    private fun openMap() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MapFragment(), TAG_MAP)
            .addToBackStack(TAG_MAP)
            .commit()
    }

    /** 백스택 깊이로 현재 화면을 판단해 하단 탭 선택 표시를 맞춘다. */
    private fun syncBottomNav() {
        val onMap = supportFragmentManager.backStackEntryCount > 0
        val targetId = if (onMap) R.id.nav_map else R.id.nav_list
        // selectedItemId 대신 isChecked 사용 → 리스너 재호출(무한 루프) 방지
        if (bottomNav.selectedItemId != targetId) {
            bottomNav.menu.findItem(targetId).isChecked = true
        }
    }

    companion object {
        private const val TAG_LIST = "list"
        private const val TAG_MAP = "map"
    }
}
