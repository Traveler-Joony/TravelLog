package com.jay.travellog

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 전역 크래시 로깅. 잡히지 않은 예외가 발생하면 스택 트레이스를 파일로 남기고,
 * 기본 핸들러로 넘겨 정상적으로 종료한다(예외를 삼켜 좀비 상태로 만들지 않음).
 *
 * 로그 위치: /Android/data/com.jay.travellog/files/crash_logs/crash_*.txt
 */
class TravelLogApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(throwable)
            } catch (e: Exception) {
                Log.e(TAG, "크래시 로그 저장 실패: ${e.message}")
            }
            // 기본 핸들러로 위임 → 시스템이 정상적으로 크래시 처리
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        val baseDir = getExternalFilesDir(null) ?: filesDir
        val dir = File(baseDir, "crash_logs").apply { if (!exists()) mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "crash_$stamp.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }

        val report = buildString {
            appendLine("시각: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("기기: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("앱 버전: $versionName")
            appendLine()
            appendLine("=== Stack Trace ===")
            append(sw.toString())
        }
        file.writeText(report)
        Log.e(TAG, "크래시 로그 저장됨: ${file.absolutePath}")
    }

    companion object {
        private const val TAG = "TravelLogApp"
    }
}
