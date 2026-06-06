package com.jay.travellog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jay.travellog.ui.ListFragment
import com.jay.travellog.ui.MapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 옵션 메뉴를 표시할 앱 바 설정 (NoActionBar 테마 기준)
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

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

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ListFragment(), TAG_LIST)
                .commit()
        }
    }

    private fun goHome() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun openMap() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MapFragment(), TAG_MAP)
            .addToBackStack(TAG_MAP)
            .commit()
    }

    private fun syncBottomNav() {
        val onMap = supportFragmentManager.backStackEntryCount > 0
        val targetId = if (onMap) R.id.nav_map else R.id.nav_list
        if (bottomNav.selectedItemId != targetId) {
            bottomNav.menu.findItem(targetId).isChecked = true
        }
    }

    companion object {
        private const val TAG_LIST = "list"
        private const val TAG_MAP = "map"
    }
}
