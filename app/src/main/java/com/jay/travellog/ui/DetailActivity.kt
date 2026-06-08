package com.jay.travellog.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jay.travellog.R
import com.jay.travellog.data.DBHelper
import com.jay.travellog.util.ImageUtils
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    private lateinit var imgPhoto: ImageView
    private lateinit var progressPhoto: ProgressBar
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
        progressPhoto = findViewById(R.id.progressPhoto)
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
            val editIntent = Intent(this, AddEditActivity::class.java)
            editIntent.putExtra(AddEditActivity.EXTRA_NO, recordNo)
            startActivity(editIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        showRecord()
    }

    private fun showRecord() {
        val record = dbHelper.getRecordById(recordNo)
        if (record == null) {
            Toast.makeText(this, "기록을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvPlace.text = record.place
        tvDate.text = record.visitDate
        tvMemo.text = if (record.memo.isBlank()) "(메모 없음)" else record.memo

        loadPhoto(record.photoUri)
    }

    /** 사진을 코루틴으로 비동기 디코딩해서 표시 (로딩 중 ProgressBar) */
    private fun loadPhoto(uriString: String?) {
        if (uriString.isNullOrBlank()) {
            progressPhoto.visibility = ProgressBar.GONE
            showPhotoPlaceholder()
            return
        }
        progressPhoto.visibility = ProgressBar.VISIBLE
        imgPhoto.setImageDrawable(null)

        lifecycleScope.launch {
            val bmp = ImageUtils.decodeSampledBitmap(this@DetailActivity, uriString, 1280, 1280)
            progressPhoto.visibility = ProgressBar.GONE
            if (bmp != null) {
                imgPhoto.scaleType = ImageView.ScaleType.CENTER_CROP
                imgPhoto.setImageBitmap(bmp)
            } else {
                showPhotoPlaceholder()
            }
        }
    }

    private fun showPhotoPlaceholder() {
        imgPhoto.scaleType = ImageView.ScaleType.CENTER_INSIDE
        imgPhoto.setImageResource(R.drawable.ic_image_placeholder)
    }

    companion object {
        const val EXTRA_NO = "extra_no"
    }
}
