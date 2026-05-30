package com.jay.travellog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jay.travellog.ui.ListFragment

// ⚠️ 임시 — Day 3 확인용. Day 4에서 BottomNavigationView 버전으로 교체합니다.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ListFragment())
                .commit()
        }
    }
}