package com.jay.travellog.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jay.travellog.R
import com.jay.travellog.data.DBHelper
import com.jay.travellog.model.TravelRecord
import java.util.Calendar

class AddEditActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    private lateinit var tvTitle: TextView
    private lateinit var etPlace: EditText
    private lateinit var etDate: EditText
    private lateinit var etMemo: EditText
    private lateinit var btnPickPhoto: Button
    private lateinit var imgPreview: ImageView
    private lateinit var btnSave: Button

    private var editingNo: Int = 0          // 0이면 신규(insert), >0이면 수정(update)
    private var photoUri: String? = null    // Day 7에서 실제 사진 URI를 채움

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        dbHelper = DBHelper(this)
        bindViews()

        // Intent로 no가 넘어왔는지로 추가/수정 모드를 구분
        editingNo = intent.getIntExtra(EXTRA_NO, 0)
        val isEdit = editingNo > 0
        tvTitle.text = if (isEdit) "기록 수정" else "기록 추가"
        if (isEdit) loadRecord(editingNo)

        etDate.setOnClickListener { showDatePicker() }
        btnPickPhoto.setOnClickListener {
            // Day 7에서 카메라/갤러리 인텐트로 교체합니다.
            Toast.makeText(this, "사진 선택은 Day 7에서 연결됩니다.", Toast.LENGTH_SHORT).show()
        }
        btnSave.setOnClickListener { save() }
    }

    private fun bindViews() {
        tvTitle = findViewById(R.id.tvTitle)
        etPlace = findViewById(R.id.etPlace)
        etDate = findViewById(R.id.etDate)
        etMemo = findViewById(R.id.etMemo)
        btnPickPhoto = findViewById(R.id.btnPickPhoto)
        imgPreview = findViewById(R.id.imgPreview)
        btnSave = findViewById(R.id.btnSave)
    }

    /** 수정 모드: 기존 기록을 불러와 입력란을 채운다. */
    private fun loadRecord(no: Int) {
        val record = dbHelper.getRecordById(no)
        if (record == null) {
            Toast.makeText(this, "기록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        etPlace.setText(record.place)
        etDate.setText(record.visitDate)
        etMemo.setText(record.memo)
        photoUri = record.photoUri
        if (!record.photoUri.isNullOrBlank()) {
            try {
                imgPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                imgPreview.setImageURI(Uri.parse(record.photoUri))
            } catch (_: Exception) { /* 미리보기 실패 시 placeholder 유지 */ }
        }
    }

    /** 날짜 선택 다이얼로그. 이미 입력된 값이 있으면 그 날짜를 기본으로 띄운다. */
    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val parts = etDate.text.toString().split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) cal.set(y, m - 1, d)
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                // month는 0부터 시작 → +1, 2자리 0-패딩으로 "YYYY-MM-DD" 포맷
                etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /** 입력값 검증 후 DB에 저장(insert/update). */
    private fun save() {
        val place = etPlace.text.toString().trim()
        val date = etDate.text.toString().trim()
        val memo = etMemo.text.toString().trim()

        if (place.isEmpty()) {
            etPlace.error = "여행지명을 입력하세요"
            etPlace.requestFocus()
            return
        }
        if (date.isEmpty()) {
            Toast.makeText(this, "방문 날짜를 선택하세요", Toast.LENGTH_SHORT).show()
            return
        }

        val record = TravelRecord(
            no = editingNo,        // 0이면 insert 시 AUTOINCREMENT가 부여
            place = place,
            visitDate = date,
            memo = memo,
            photoUri = photoUri
            // latitude/longitude는 Day 10에서 사진 EXIF로부터 채웁니다.
        )

        val success = if (editingNo > 0) {
            dbHelper.updateRecord(record) > 0
        } else {
            dbHelper.insertRecord(record) > 0
        }

        if (success) {
            Toast.makeText(
                this,
                if (editingNo > 0) "수정되었습니다" else "저장되었습니다",
                Toast.LENGTH_SHORT
            ).show()
            finish()   // 목록은 ListFragment.onResume()에서 자동 갱신됨
        } else {
            Toast.makeText(this, "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        /** 수정 모드일 때 기록 번호(no)를 전달하는 Intent extra 키. */
        const val EXTRA_NO = "extra_no"
    }
}
