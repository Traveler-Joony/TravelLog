package jay.travellog.model

/** 여행 기록 한 건. no = 0 이면 아직 저장 전(신규)이라는 뜻. */
data class TravelRecord(
    val no: Int = 0,
    val place: String,            // 여행지명
    val visitDate: String,        // "2026-06-01" 형식
    val memo: String = "",
    val photoUri: String? = null, // 사진 URI (없을 수 있음)
    val latitude: Double? = null, // 위도 (가산점, NULL 허용)
    val longitude: Double? = null // 경도 (가산점, NULL 허용)
)
