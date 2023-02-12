package com.example.distancetracker

import android.content.Context
import androidx.fragment.app.Fragment
import com.example.distancetracker.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {

    fun hasLocationPermission(context: Context) =
        EasyPermissions.hasPermissions(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

    fun requestLocationPermission(fragment: Fragment){
        EasyPermissions.requestPermissions(
            fragment,
            "This application needs location permission.",
            PERMISSION_LOCATION_REQUEST_CODE,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}