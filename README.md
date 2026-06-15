# 여행 기록 (TravelLog)

다녀온 여행지를 사진·날짜·메모로 남기고, 지도에서 모아 볼 수 있는 안드로이드 앱입니다.
모바일 프로그래밍 기말 프로젝트로 만들었습니다.

## 어떤 앱인가

여행을 다녀올 때마다 사진이랑 방문 날짜, 간단한 메모를 기록해두는 앱입니다.
사진에 위치 정보(GPS)가 들어있으면 그걸 읽어서 지도에 자동으로 찍어주고,
없으면 지도에서 직접 위치를 골라 지정할 수 있습니다.
데이터는 전부 기기 안 SQLite에 저장돼서 앱을 껐다 켜도 그대로 남아 있습니다.

## 기능

기본 기능
- 여행 기록 추가 / 조회 / 수정 / 삭제 (SQLite 저장, 앱 재실행해도 유지)
- 목록 화면: RecyclerView로 썸네일 + 여행지명 + 날짜를 카드로 표시
- 상세 화면: 사진·메모까지 전체 보기
- 추가/수정을 한 화면에서 처리 (DatePicker, 입력값 검사)
- 항목 삭제(롱클릭) / 전체 삭제, 삭제 전 확인 다이얼로그
- 카메라 촬영 또는 갤러리에서 사진 첨부 (권한 처리, 내부 저장소에 복사 보관)
- 하단 탭(목록/지도)으로 Fragment 전환, 백스택 관리
- 옵션 메뉴(정렬/전체삭제/앱정보) + 컨텍스트 메뉴(수정/삭제)
- 날짜순 ↔ 이름순 정렬 토글

추가로 넣은 것
- 네이버 지도에 기록 위치를 마커로 표시 (정보창에 여행지명·날짜, 여러 개면 자동 줌)
- 사진 EXIF의 GPS 좌표를 읽어 위치 자동 지정
- GPS 없는 사진은 지도에서 직접 위치 선택 (중앙 고정 핀 방식)
- 코루틴으로 이미지 비동기 로딩 (백그라운드 다운샘플링, 로딩 중 ProgressBar)
- 검색, 스와이프 삭제(실행취소), 사진 풀스크린 보기, 상세 화면 미니지도

## 기술 스택

- Kotlin, 최소 SDK 26 (Android 8.0), Material 3
- SQLite (SQLiteOpenHelper) — 로컬 DB
- AndroidX: RecyclerView, Fragment, Lifecycle
- Kotlin Coroutines (Dispatchers.IO + lifecycleScope)
- 네이버 지도 SDK (com.naver.maps:map-sdk:3.23.2)
- BitmapFactory 다운샘플링, ExifInterface, FileProvider

## 프로젝트 구조

```
com.jay.travellog
├── MainActivity.kt          하단 네비게이션 + Fragment 전환/백스택
├── model/TravelRecord.kt    여행 기록 데이터 클래스
├── data/DBHelper.kt         SQLiteOpenHelper, CRUD
├── adapter/TravelAdapter.kt RecyclerView 어댑터, 썸네일 로딩, 컨텍스트 메뉴
├── ui/
│   ├── ListFragment.kt      목록 + 메뉴 + 정렬 + 검색
│   ├── MapFragment.kt       네이버 지도 + 마커
│   ├── AddEditActivity.kt   추가/수정, 사진·위치 입력
│   ├── DetailActivity.kt    상세 보기
│   └── MapPickerActivity.kt 지도에서 위치 선택
└── util/ImageUtils.kt       EXIF 좌표 추출 + 비동기 디코딩
```

## 빌드하기

네이버 지도를 쓰기 때문에 Client ID가 필요합니다.

1. [네이버 클라우드 플랫폼](https://www.ncloud.com)에서 Maps 애플리케이션을 등록하고
   Android 패키지 이름에 `com.jay.travellog`를 넣어 Key ID를 발급받습니다.
2. 프로젝트 루트 `local.properties`에 아래 한 줄을 추가합니다.
   (이 파일은 .gitignore에 들어 있어 커밋되지 않습니다.)
   ```
   NAVER_MAP_CLIENT_ID=발급받은_Key_ID
   ```
3. Android Studio에서 Gradle Sync 후 실행. (API 26 이상)

키 없이도 빌드는 되지만 지도 화면이 정상적으로 안 나옵니다.

## DB 스키마

테이블 `travel`

| 컬럼 | 타입 | 설명 |
|------|------|------|
| no | INTEGER PK AUTOINCREMENT | 번호 |
| place | TEXT NOT NULL | 여행지명 |
| visit_date | TEXT NOT NULL | 방문 날짜 (YYYY-MM-DD) |
| memo | TEXT | 메모 |
| photo_uri | TEXT | 사진 파일 URI |
| latitude / longitude | REAL | 좌표 |

## 알려진 제약

- 사진은 기록당 1장만 첨부됩니다.
- 일부 기기/에뮬레이터에서는 갤러리 사진의 EXIF 위치를 못 읽을 수 있어, 그럴 땐 수동 지정으로 대체합니다.
- 라이트 테마 기준으로 만들었습니다.
