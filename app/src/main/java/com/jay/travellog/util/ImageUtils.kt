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

object ImageUtils {

    private const val TAG = "ImageUtils"

    // ───────── EXIF 위치 추출 (Day 10) ─────────

    fun extractLatLng(context: Context, uri: Uri): Pair<Double, Double>? {
        // 1) URI에서 직접 읽기 (file://, Google Photos 등 원본을 그대로 주는 경우)
        readExifLatLng(context, uri)?.let { return it }

        // 2) 순수 MediaStore URI면 원본 요청으로 재시도 (위치 가림 해제)
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
                val latLng = ExifInterface(input).latLong   // [위도, 경도] 또는 null
                if (latLng != null) latLng[0] to latLng[1] else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ───────── 비동기 이미지 디코딩 (Day 11) ─────────

    /**
     * 사진을 백그라운드(IO)에서 다운샘플링하여 디코딩한다.
     * 원본을 그대로 메모리에 올리면 OOM/렉이 나므로, 표시 크기에 맞춰 inSampleSize로 줄여 읽는다.
     */
    suspend fun decodeSampledBitmap(
        context: Context,
        uriString: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)

            // 1) 실제 크기만 먼저 측정 (픽셀은 메모리에 올리지 않음)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            // 2) 표시 크기에 맞는 축소 비율 계산
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
            }

            // 3) 축소해서 실제 디코딩
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
            // 요청 크기보다 작아지지 않을 때까지 2배씩 축소
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
