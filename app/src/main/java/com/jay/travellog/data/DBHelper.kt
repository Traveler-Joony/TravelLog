package jay.travellog.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import jay.travellog.model.TravelRecord

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "travel.db"
        private const val DB_VERSION = 1

        const val TABLE = "travel"
        const val COL_NO = "no"
        const val COL_PLACE = "place"
        const val COL_DATE = "visit_date"
        const val COL_MEMO = "memo"
        const val COL_PHOTO = "photo_uri"
        const val COL_LAT = "latitude"
        const val COL_LNG = "longitude"
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
                $COL_LNG REAL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 학습용: 버전이 오르면 테이블을 새로 만든다 (기존 데이터는 사라짐)
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    // ───────── Create ─────────
    fun insertRecord(record: TravelRecord): Long =
        writableDatabase.insert(TABLE, null, valuesOf(record))

    // ───────── Read ─────────
    fun getAllRecords(byDateDesc: Boolean = true): List<TravelRecord> {
        val order = if (byDateDesc) "$COL_DATE DESC" else "$COL_PLACE COLLATE NOCASE ASC"
        val result = mutableListOf<TravelRecord>()
        readableDatabase.query(TABLE, null, null, null, null, null, order).use { c ->
            while (c.moveToNext()) result.add(readRecord(c))
        }
        return result
    }

    fun getRecordById(no: Int): TravelRecord? {
        readableDatabase.query(
            TABLE, null, "$COL_NO = ?", arrayOf(no.toString()),
            null, null, null
        ).use { c ->
            return if (c.moveToFirst()) readRecord(c) else null
        }
    }

    // ───────── Update ─────────
    fun updateRecord(record: TravelRecord): Int =
        writableDatabase.update(
            TABLE, valuesOf(record),
            "$COL_NO = ?", arrayOf(record.no.toString())
        )

    // ───────── Delete ─────────
    fun deleteRecord(no: Int): Int =
        writableDatabase.delete(TABLE, "$COL_NO = ?", arrayOf(no.toString()))

    fun deleteAll(): Int =
        writableDatabase.delete(TABLE, null, null)

    // ───────── 매핑 헬퍼 ─────────
    private fun valuesOf(record: TravelRecord) = ContentValues().apply {
        put(COL_PLACE, record.place)
        put(COL_DATE, record.visitDate)
        put(COL_MEMO, record.memo)
        put(COL_PHOTO, record.photoUri)   // null이면 NULL로 저장됨
        put(COL_LAT, record.latitude)     // Double? 그대로 넣어도 OK
        put(COL_LNG, record.longitude)
        // no(PK)는 넣지 않음 → AUTOINCREMENT가 자동 부여
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
            longitude = if (c.isNull(lngIdx)) null else c.getDouble(lngIdx)
        )
    }
}
