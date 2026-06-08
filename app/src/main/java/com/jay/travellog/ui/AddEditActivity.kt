package com.jay.travellog.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.jay.travellog.util.ImageUtils
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
    private lateinit var btnPickLocation: Button
    private lateinit var tvLocation: TextView
    private lateinit var btnSave: Button

    private var editingNo: Int = 0
    private var photoUri: String? = null
    private var latitude: Double? = null
    private var longitude: Double? = null

    private var cameraImageFile: File? = null

    // ───────── ActivityResultLauncher ─────────

    // 갤러리: ACTION_PICK → MediaStore URI를 돌려줌(setRequireOriginal로 원본 GPS 읽기 가능)
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    // 1) 원본 URI에서 EXIF 좌표 시도
                    ImageUtils.extractLatLng(this, uri)?.let { (lat, lng) ->
                        latitude = lat; longitude = lng
                        updateLocationStatus()
                        toast("사진에서 위치를 가져왔어요")
                    }
                    // 2) 표시·영속성을 위해 내부 저장소로 복사
                    val saved = copyToInternal(uri)
                    if (saved != null) setPhoto(Uri.fromFile(saved).toString())
                    else toast("사진을 불러오지 못했습니다")
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageFile != null) {
                val fileUri = Uri.fromFile(cameraImageFile)
                ImageUtils.extractLatLng(this, fileUri)?.let { (lat, lng) ->
                    latitude = lat; longitude = lng
                    updateLocationStatus()
                    toast("사진에서 위치를 가져왔어요")
                }
                setPhoto(fileUri.toString())
            } else {
                cameraImageFile?.delete()
            }
        }

    // 갤러리 권한: 읽기 권한 + ACCESS_MEDIA_LOCATION 함께 요청
    private val galleryPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val readPerm = readImagePermission()
            if (result[readPerm] == true) {
                launchGallery()
            } else {
                toast("갤러리 접근 권한이 필요합니다")
            }
            // ACCESS_MEDIA_LOCATION은 best-effort: 거부돼도 갤러리는 열되 GPS만 못 읽음(→ 수동 지정)
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera()
            else toast("카메라 권한이 필요합니다")
        }

    private val mapPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val lat = data.getDoubleExtra(MapPickerActivity.EXTRA_LAT, Double.NaN)
                val lng = data.getDoubleExtra(MapPickerActivity.EXTRA_LNG, Double.NaN)
                if (!lat.isNaN() && !lng.isNaN()) {
                    latitude = lat; longitude = lng
                    updateLocationStatus()
                }
            }
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
        updateLocationStatus()

        etDate.setOnClickListener { showDatePicker() }
        btnPickPhoto.setOnClickListener { showPhotoSourceDialog() }
        btnPickLocation.setOnClickListener { openMapPicker() }
        btnSave.setOnClickListener { save() }
    }

    private fun bindViews() {
        tvTitle = findViewById(R.id.tvTitle)
        etPlace = findViewById(R.id.etPlace)
        etDate = findViewById(R.id.etDate)
        etMemo = findViewById(R.id.etMemo)
        btnPickPhoto = findViewById(R.id.btnPickPhoto)
        imgPreview = findViewById(R.id.imgPreview)
        btnPickLocation = findViewById(R.id.btnPickLocation)
        tvLocation = findViewById(R.id.tvLocation)
        btnSave = findViewById(R.id.btnSave)
    }

    // ───────── 위치 ─────────

    private fun openMapPicker() {
        val intent = Intent(this, MapPickerActivity::class.java)
        latitude?.let { intent.putExtra(MapPickerActivity.EXTRA_LAT, it) }
        longitude?.let { intent.putExtra(MapPickerActivity.EXTRA_LNG, it) }
        mapPickerLauncher.launch(intent)
    }

    private fun updateLocationStatus() {
        tvLocation.text = if (latitude != null && longitude != null)
            "위치: %.5f, %.5f".format(latitude, longitude)
        else
            "위치 없음 (사진에 GPS가 없으면 지도에서 지정)"
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

    /** API 33+ 는 READ_MEDIA_IMAGES, 그 이하는 READ_EXTERNAL_STORAGE */
    private fun readImagePermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    private fun checkGalleryPermissionAndLaunch() {
        val perms = mutableListOf(readImagePermission())
        // Android 10(Q)+ 에서 사진 원본 GPS를 읽으려면 ACCESS_MEDIA_LOCATION 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms += Manifest.permission.ACCESS_MEDIA_LOCATION
        }
        // 이미 모두 허용돼 있으면 시스템이 즉시 콜백(다이얼로그 없음)
        galleryPermissionLauncher.launch(perms.toTypedArray())
    }

    /** ACTION_PICK → content://media/... URI 반환 (setRequireOriginal 사용 가능) */
    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun launchCamera() {
        try {
            val file = createImageFile()
            cameraImageFile = file
            val authority = "$packageName.fileprovider"
            val uri = FileProvider.getUriForFile(this, authority, file)
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            toast("카메라를 실행할 수 없습니다")
        }
    }

    private fun createImageFile(): File {
        val dir = File(filesDir, "images").apply { if (!exists()) mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "IMG_$stamp.jpg")
    }

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
        latitude = record.latitude
        longitude = record.longitude
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
            photoUri = photoUri,
            latitude = latitude,
            longitude = longitude
        )

        val success = if (editingNo > 0) {
            dbHelper.updateRecord(record) > 0
        } else {
            dbHelper.insertRecord(record) > 0
        }

        if (success) {
            toast(if (editingNo > 0) "수정되었습니다" else "저장되었습니다")
            finish()
        } else {
            toast("저장에 실패했습니다")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_NO = "extra_no"
    }
}
