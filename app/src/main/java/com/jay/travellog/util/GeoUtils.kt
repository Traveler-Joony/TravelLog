package com.jay.travellog.util

object GeoUtils {

    /** 위도(-90~90)·경도(-180~180) 범위와 NaN/무한대 여부를 검사한다. */
    fun isValidLatLng(lat: Double, lng: Double): Boolean {
        if (lat.isNaN() || lng.isNaN() || lat.isInfinite() || lng.isInfinite()) return false
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }
}
