package com.jay.travellog.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.jay.travellog.R
import com.jay.travellog.data.DBHelper
import com.jay.travellog.model.TravelRecord
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    private var photoUri: String? = null    // 내부 저장소 파일 URI(file://...) 문자열

    // 카메라가 사진을 기록할 내부 파일
    private var cameraImageFile: File? = null

    // ───────── ActivityResultLauncher (생성 시점에 등록) ─────────

    /** 갤러리에서 이미지 선택 → 내부 저장소로 복사 */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val saved = copyToInternal(uri)
                if (saved != null) setPhoto(Uri.fromFile(saved).toString())
                else toast("사진을 불러오지 못했습니다")
            }
        }

    /** 카메라 촬영 → 미리 만든 파일에 저장됨 */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageFile != null) {
                setPhoto(Uri.fromFile(cameraImageFile).toString())
            } else {
                cameraImageFile?.delete()   // 취소 시 빈 파일 정리
            }
        }

    /** 갤러리 권한 요청 결과 */
    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) galleryLauncher.launch("image/*")
            else toast("갤러리 접근 권한이 필요합니다")
        }

    /** 카메라 권한 요청 결과 */
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
            else toast("카메라 권한이 필요합니다")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        dbHelper = DBHelper(this)
        bindViews()

        editingNo = intent.getIntExtra(EXTRA_NO, 0)
        val isEdit = editingNo > 0
        tvTitle.text = if (isEdit) "기록 수정" else "기록 추가"
        if (isEdit) loadRecord(editingNo)

        etDate.setOnClickListener { showDatePicker() }
        btnPickPhoto.setOnClickListener { showPhotoSourceDialog() }
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

    // ───────── 사진 선택 ─────────

    private fun showPhotoSourceDialog() {
        val options = arrayOf("카메라로 촬영", "갤러리에서 선택")
        AlertDialog.Builder(this)
            .setTitle("사진 추가")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> checkGalleryPermissionAndLaunch()
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        val perm = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(perm)
        }
    }

    private fun checkGalleryPermissionAndLaunch() {
        // Android 13(API 33)+ 는 READ_MEDIA_IMAGES, 그 이하는 READ_EXTERNAL_STORAGE
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*")
        } else {
            galleryPermissionLauncher.launch(perm)
        }
    }

    private fun launchCamera() {
        try {
            val file = createImageFile()
            cameraImageFile = file
            // FileProvider로 content:// URI를 만들어 카메라 앱에 전달 (file:// 직접 전달은 금지됨)
            val authority = "$packageName.fileprovider"
            val uri = FileProvider.getUriForFile(this, authority, file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            toast("카메라를 실행할 수 없습니다")
        }
    }

    /** 내부 저장소(filesDir/images)에 새 이미지 파일 경로 생성 */
    private fun createImageFile(): File {
        val dir = File(filesDir, "images").apply { if (!exists()) mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "IMG_$stamp.jpg")
    }

    /** 갤러리 URI의 내용을 내부 저장소로 복사 → 영속성 보장 (URI 권한 만료와 무관) */
    private fun copyToInternal(source: Uri): File? {
        return try {
            val dest = createImageFile()
            contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest
        } catch (e: Exception) {
            null
        }
    }

    private fun setPhoto(uriString: String) {
        photoUri = uriString
        // 지금은 메인 스레드 단순 로딩. Day 11에서 코루틴 + ProgressBar로 교체합니다.
        try {
            imgPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            imgPreview.setImageURI(Uri.parse(uriString))
        } catch (_: Exception) {
            imgPreview.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imgPreview.setImageResource(R.drawable.ic_image_placeholder)
        }
    }

    // ───────── 수정 모드 로딩 ─────────

    private fun loadRecord(no: Int) {
        val record = dbHelper.getRecordById(no)
        if (record == null) {
            toast("기록을 불러올 수 없습니다.")
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
            } catch (_: Exception) { /* placeholder 유지 */ }
        }
    }

    // ───────── 날짜 ─────────

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
                etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ───────── 저장 ─────────

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
            toast("방문 날짜를 선택하세요")
            return
        }

        val record = TravelRecord(
            no = editingNo,
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
            toast(if (editingNo > 0) "수정되었습니다" else "저장되었습니다")
            finish()   // 목록은 ListFragment.onResume()에서 자동 갱신됨
        } else {
            toast("저장에 실패했습니다")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_NO = "extra_no"
    }
}
