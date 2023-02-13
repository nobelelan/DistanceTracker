package com.example.distancetracker.ui.maps

import android.location.Location
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

object MapsUtil {

    fun setCameraPosition(location: LatLng): CameraPosition{
        return CameraPosition.Builder()
            .target(location)
            .zoom(18f)
            .build()
    }
}