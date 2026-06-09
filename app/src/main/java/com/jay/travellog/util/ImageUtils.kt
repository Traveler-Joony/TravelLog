package com.jay.travellog.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ImageUtils {

    private const val TAG = "ImageUtils"

    // ───────── EXIF 위치 추출 (Day 10) ─────────

    fun extractLatLng(context: Context, uri: Uri): Pair<Double, Double>? {
        readExifLatLng(context, uri)?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.authority == MediaStore.AUTHORITY) {
            try {
                val original = MediaStore.setRequireOriginal(uri)
                readExifLatLng(context, original)?.let { return it }
            } catch (e: Exception) {
                Log.w(TAG, "setRequireOriginal 실패: ${e.message}")
            }
        }
        return null
    }

    private fun readExifLatLng(context: Context, uri: Uri): Pair<Double, Double>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val latLng = ExifInterface(input).latLong
                if (latLng != null) latLng[0] to latLng[1] else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ───────── 비동기 이미지 디코딩 (Day 11) ─────────

    suspend fun decodeSampledBitmap(
        context: Context,
        uriString: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            Log.w(TAG, "decode 실패: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // ───────── 내부 사진 파일 정리 ─────────

    /**
     * 우리가 내부 저장소(filesDir/images)에 복사한 사진 파일만 안전하게 삭제한다.
     * 외부 URI(content://, 갤러리 원본 등)나 다른 경로는 절대 건드리지 않는다.
     */
    fun deleteInternalPhoto(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) return
        try {
            val path = Uri.parse(uriString).path ?: return
            val file = File(path)

            // 안전장치: 반드시 우리 앱 내부 images 폴더 안의 파일만 삭제
            val imagesDir = File(context.filesDir, "images").canonicalFile
            if (!file.canonicalFile.startsWith(imagesDir)) {
                Log.w(TAG, "내부 사진이 아니라 삭제 생략: $uriString")
                return
            }
            if (file.exists() && file.delete()) {
                Log.d(TAG, "사진 파일 삭제: ${file.name}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "사진 삭제 실패: ${e.message}")
        }
    }
}
