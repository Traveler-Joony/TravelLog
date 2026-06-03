package com.jay.travellog.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jay.travellog.R
import com.jay.travellog.data.DBHelper

class DetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    private lateinit var imgPhoto: ImageView
    private lateinit var tvPlace: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvMemo: TextView
    private lateinit var btnEdit: Button

    private var recordNo: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        dbHelper = DBHelper(this)

        imgPhoto = findViewById(R.id.imgPhoto)
        tvPlace = findViewById(R.id.tvPlace)
        tvDate = findViewById(R.id.tvDate)
        tvMemo = findViewById(R.id.tvMemo)
        btnEdit = findViewById(R.id.btnEdit)

        recordNo = intent.getIntExtra(EXTRA_NO, 0)
        if (recordNo <= 0) {
            Toast.makeText(this, "잘못된 접근입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnEdit.setOnClickListener {
            // 수정 모드로 AddEditActivity 실행 (no 전달 → 기존 값 채워짐)
            val editIntent = Intent(this, AddEditActivity::class.java)
            editIntent.putExtra(AddEditActivity.EXTRA_NO, recordNo)
            startActivity(editIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        // 수정 화면에서 돌아왔을 때 최신 내용으로 다시 표시
        showRecord()
    }

    private fun showRecord() {
        val record = dbHelper.getRecordById(recordNo)
        if (record == null) {
            // 조회 실패(또는 추후 삭제됨) → 화면 종료
            Toast.makeText(this, "기록을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvPlace.text = record.place
        tvDate.text = record.visitDate
        tvMemo.text = if (record.memo.isBlank()) "(메모 없음)" else record.memo

        if (!record.photoUri.isNullOrBlank()) {
            try {
                imgPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                imgPhoto.setImageURI(Uri.parse(record.photoUri))
            } catch (_: Exception) {
                showPhotoPlaceholder()
            }
        } else {
            showPhotoPlaceholder()
        }
    }

    private fun showPhotoPlaceholder() {
        imgPhoto.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imgPhoto.setImageResource(R.drawable.ic_image_placeholder)
    }

    companion object {
        /** 표시할 기록 번호(no)를 전달하는 Intent extra 키. */
        const val EXTRA_NO = "extra_no"
    }
}
