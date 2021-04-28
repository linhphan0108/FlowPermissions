package com.linhphan.sample

import android.Manifest
import android.Manifest.permission
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linhphan.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.linhphan.flowpermission.FlowPermissions
import com.linhphan.flowpermission.Permission
import java.io.IOException

private const val TAG = "FlowPermissionsSample"
class MainActivity : AppCompatActivity() {
    private var camera: Camera? = null
    private lateinit var binding: ActivityMainBinding
    private val flowPermission by lazy { FlowPermissions(this).apply {
            setLogging(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        registerEventListeners()
    }

    override fun onStop() {
        super.onStop()
        releaseCamera()
    }

    private fun registerEventListeners(){
        binding.enableCamera.setOnClickListener {
            lifecycleScope.launch {
                flowPermission.requestEachCombined(permission.CAMERA, permission.RECORD_AUDIO)
                    .catch { Log.e(TAG, it.message, it) }
                    .collect { permissions ->
                        onRequestPermissionResult(permissions)
                    }
            }
        }
    }

    private fun onRequestPermissionResult(permission: Permission) {
        when {
            permission.granted -> {
                openCamera()
            }
            permission.shouldShowRequestPermissionRationale -> {
                // Denied permission without ask never again
                Toast.makeText(
                    this@MainActivity,
                    "Denied permission without ask never again",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                // Denied permission with ask never again
                // Need to go to the settings
            }
        }
    }

    private fun onRequestPermissionResult(permissions: List<Permission>) {
        permissions.forEach { permission ->
            when(permission.name){
                Manifest.permission.CAMERA -> {
                    when {
                        permission.granted -> {
                            openCamera()
                        }
                        permission.shouldShowRequestPermissionRationale -> {
                            // Denied permission without ask never again
                            Toast.makeText(
                                this@MainActivity,
                                "Denied permission without ask never again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            // Denied permission with ask never again
                            // Need to go to the settings
                        }
                    }
                }
                Manifest.permission.RECORD_AUDIO -> {
                    when {
                        permission.granted -> {
                        }
                        permission.shouldShowRequestPermissionRationale -> {
                            // Denied permission without ask never again
                            Toast.makeText(
                                this@MainActivity,
                                "Denied permission without ask never again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            // Denied permission with ask never again
                            // Need to go to the settings
                        }
                    }
                }
            }
        }

    }

    private fun openCamera(){
        releaseCamera()
        camera = Camera.open(1)
        try {
            camera!!.setPreviewDisplay(binding.surfaceView.holder)
            camera!!.startPreview()
        } catch (e: IOException) {
            Log.e(TAG, "Error while trying to display the camera preview", e)
        }
    }

    private fun releaseCamera() {
        if (camera != null) {
            camera!!.release()
            camera = null
        }
    }
}