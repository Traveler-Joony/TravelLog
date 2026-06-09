package com.jay.travellog.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jay.travellog.R
import com.jay.travellog.adapter.AdminAdapter
import com.jay.travellog.data.DBHelper
import com.jay.travellog.model.TravelRecord
import com.jay.travellog.util.ImageUtils

/** DB 관리용 관리자 화면. 전체 레코드 조회 + 생성/수정/삭제(CRUD) + 타임스탬프 표시. */
class AdminActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var adapter: AdminAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        dbHelper = DBHelper(this)
        recycler = findViewById(R.id.recyclerAdmin)
        emptyView = findViewById(R.id.txtAdminEmpty)

        adapter = AdminAdapter(
            onEdit = { record ->
                val intent = Intent(this, AddEditActivity::class.java)
                intent.putExtra(AddEditActivity.EXTRA_NO, record.no)
                startActivity(intent)
            },
            onDelete = { record -> confirmDelete(record) }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Create: 추가 화면 재사용
        findViewById<FloatingActionButton>(R.id.fabAdminAdd).setOnClickListener {
            startActivity(Intent(this, AddEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        // 원시 테이블 순서대로 보기: no 오름차순
        val records = dbHelper.getAllRecords().sortedBy { it.no }
        adapter.submit(records)

        val isEmpty = records.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(record: TravelRecord) {
        AlertDialog.Builder(this)
            .setTitle("삭제")
            .setMessage("#${record.no} '${record.place}' 기록을 삭제할까요?")
            .setPositiveButton("삭제") { _, _ ->
                if (dbHelper.deleteRecord(record.no) > 0) {
                    ImageUtils.deleteInternalPhoto(this, record.photoUri)
                    loadData()
                    toast("삭제되었습니다")
                } else {
                    toast("삭제에 실패했습니다")
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
