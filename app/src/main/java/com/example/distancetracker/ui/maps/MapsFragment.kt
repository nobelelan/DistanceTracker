package com.example.distancetracker.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.distancetracker.R
import com.example.distancetracker.databinding.FragmentMapsBinding
import com.example.distancetracker.service.TrackerService
import com.example.distancetracker.ui.maps.MapsUtil.setCameraPosition
import com.example.distancetracker.utils.Constants.ACTION_SERVICE_START
import com.example.distancetracker.utils.Constants.ACTION_SERVICE_STOP
import com.example.distancetracker.utils.ExtensionFunctions.disable
import com.example.distancetracker.utils.ExtensionFunctions.enable
import com.example.distancetracker.utils.ExtensionFunctions.hide
import com.example.distancetracker.utils.ExtensionFunctions.show
import com.example.distancetracker.utils.Permissions.hasBackgroundLocationPermission
import com.example.distancetracker.utils.Permissions.requestBackgroundLocationPermission
import com.google.android.gms.maps.CameraUpdateFactory

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.ButtCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MapsFragment : Fragment(), OnMapReadyCallback, OnMyLocationButtonClickListener, EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    private var startTime = 0L
    private var stopTime = 0L

    private var locationList = mutableListOf<LatLng>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)

        binding.apply {
            btnStart.setOnClickListener {
                onStartButtonClick()
            }
            btnStop.setOnClickListener {
                onStopButtonClicked()
            }
            btnReset.setOnClickListener {  }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled = false
        }
        observeTrackerService()
    }

    private fun observeTrackerService(){
        TrackerService.locationList.observe(viewLifecycleOwner) {
            if (it != null) {
                locationList = it
                if (locationList.size > 1){
                    binding.btnStop.enable()
                }
                drawPolyline()
                followPolyline()
            }
        }
        TrackerService.startTime.observe(viewLifecycleOwner) {
            startTime = it
        }
        TrackerService.stopTime.observe(viewLifecycleOwner){
            stopTime = it
        }
    }

    private fun drawPolyline(){
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.RED)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
    }

    private fun followPolyline(){
        if (locationList.isNotEmpty()){
            map.animateCamera((
                    CameraUpdateFactory.newCameraPosition(
                        setCameraPosition(
                            locationList.last()
                        )
                    )
                    ), 1000, null)
        }
    }

    private fun onStartButtonClick() {
        if (hasBackgroundLocationPermission(requireContext())){
            startCountDown()
            binding.btnStart.disable()
            binding.btnStart.hide()
            binding.btnStop.show()
        }else{
            requestBackgroundLocationPermission(this)
        }
    }

    private fun onStopButtonClicked(){
        stopForegroundService()
        binding.apply {
            btnStop.hide()
            btnStart.show()
        }
    }

    private fun startCountDown() {
        binding.txtTimer.show()
        binding.btnStop.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0"){
                    binding.txtTimer.text = "GO"
                    binding.txtTimer.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.black
                    ))
                }else{
                    binding.txtTimer.text = currentSecond.toString()
                    binding.txtTimer.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.blue
                    ))
                }
            }

            override fun onFinish() {
                sendActionCommandToService(ACTION_SERVICE_START)
                binding.txtTimer.hide()
            }
        }
        timer.start()
    }

    private fun stopForegroundService() {
        binding.btnStart.disable()
        sendActionCommandToService(ACTION_SERVICE_STOP)
    }

    private fun sendActionCommandToService(action: String){
        Intent(
            requireContext(),
            TrackerService::class.java
        ).apply {
            this.action = action
            requireContext().startService(this)
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.txtHint.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.txtHint.hide()
            binding.btnStart.show()
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        }else{
            requestBackgroundLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClick()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}