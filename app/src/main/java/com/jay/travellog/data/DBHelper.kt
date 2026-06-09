package com.jay.travellog.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.jay.travellog.model.TravelRecord

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val TAG = "DBHelper"
        private const val DB_NAME = "travel.db"
        private const val DB_VERSION = 2   // v2: created_at / updated_at 추가

        const val TABLE = "travel"
        const val COL_NO = "no"
        const val COL_PLACE = "place"
        const val COL_DATE = "visit_date"
        const val COL_MEMO = "memo"
        const val COL_PHOTO = "photo_uri"
        const val COL_LAT = "latitude"
        const val COL_LNG = "longitude"
        const val COL_CREATED = "created_at"
        const val COL_UPDATED = "updated_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_NO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PLACE TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_MEMO TEXT,
                $COL_PHOTO TEXT,
                $COL_LAT REAL,
                $COL_LNG REAL,
                $COL_CREATED INTEGER NOT NULL DEFAULT 0,
                $COL_UPDATED INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 → v2: 데이터 보존하면서 타임스탬프 컬럼만 추가 (DROP 하지 않음)
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_CREATED INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_UPDATED INTEGER NOT NULL DEFAULT 0")
        }
    }

    // ───────── Create ─────────
    fun insertRecord(record: TravelRecord): Long = try {
        val now = System.currentTimeMillis()
        val cv = valuesOf(record).apply {
            put(COL_CREATED, now)
            put(COL_UPDATED, now)
        }
        writableDatabase.insert(TABLE, null, cv)
    } catch (e: Exception) {
        Log.e(TAG, "insertRecord 실패: ${e.message}", e)
        -1L
    }

    // ───────── Read ─────────
    fun getAllRecords(byDateDesc: Boolean = true): List<TravelRecord> {
        val order = if (byDateDesc) "$COL_DATE DESC" else "$COL_PLACE COLLATE NOCASE ASC"
        return try {
            val result = mutableListOf<TravelRecord>()
            readableDatabase.query(TABLE, null, null, null, null, null, order).use { c ->
                while (c.moveToNext()) result.add(readRecord(c))
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "getAllRecords 실패: ${e.message}", e)
            emptyList()
        }
    }

    fun getRecordById(no: Int): TravelRecord? = try {
        readableDatabase.query(
            TABLE, null, "$COL_NO = ?", arrayOf(no.toString()),
            null, null, null
        ).use { c ->
            if (c.moveToFirst()) readRecord(c) else null
        }
    } catch (e: Exception) {
        Log.e(TAG, "getRecordById 실패: ${e.message}", e)
        null
    }

    // ───────── Update ─────────
    fun updateRecord(record: TravelRecord): Int = try {
        val cv = valuesOf(record).apply {
            put(COL_UPDATED, System.currentTimeMillis())  // created_at은 건드리지 않음
        }
        writableDatabase.update(TABLE, cv, "$COL_NO = ?", arrayOf(record.no.toString()))
    } catch (e: Exception) {
        Log.e(TAG, "updateRecord 실패: ${e.message}", e)
        0
    }

    // ───────── Delete ─────────
    fun deleteRecord(no: Int): Int = try {
        writableDatabase.delete(TABLE, "$COL_NO = ?", arrayOf(no.toString()))
    } catch (e: Exception) {
        Log.e(TAG, "deleteRecord 실패: ${e.message}", e)
        0
    }

    fun deleteAll(): Int = try {
        writableDatabase.delete(TABLE, null, null)
    } catch (e: Exception) {
        Log.e(TAG, "deleteAll 실패: ${e.message}", e)
        0
    }

    // ───────── 매핑 헬퍼 ─────────
    private fun valuesOf(record: TravelRecord) = ContentValues().apply {
        put(COL_PLACE, record.place)
        put(COL_DATE, record.visitDate)
        put(COL_MEMO, record.memo)
        put(COL_PHOTO, record.photoUri)
        put(COL_LAT, record.latitude)
        put(COL_LNG, record.longitude)
        // created_at / updated_at 은 insert·update에서 직접 채움
    }

    private fun readRecord(c: Cursor): TravelRecord {
        val latIdx = c.getColumnIndexOrThrow(COL_LAT)
        val lngIdx = c.getColumnIndexOrThrow(COL_LNG)
        return TravelRecord(
            no = c.getInt(c.getColumnIndexOrThrow(COL_NO)),
            place = c.getString(c.getColumnIndexOrThrow(COL_PLACE)),
            visitDate = c.getString(c.getColumnIndexOrThrow(COL_DATE)),
            memo = c.getString(c.getColumnIndexOrThrow(COL_MEMO)) ?: "",
            photoUri = c.getString(c.getColumnIndexOrThrow(COL_PHOTO)),
            latitude = if (c.isNull(latIdx)) null else c.getDouble(latIdx),
            longitude = if (c.isNull(lngIdx)) null else c.getDouble(lngIdx),
            createdAt = c.getLong(c.getColumnIndexOrThrow(COL_CREATED)),
            updatedAt = c.getLong(c.getColumnIndexOrThrow(COL_UPDATED))
        )
    }
}
