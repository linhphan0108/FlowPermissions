package com.linhphan.flowpermission

import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.*

class FlowPermissionsFragment : Fragment() {
    // Contains all the current permission requests.
    // Once granted or denied, they are removed from it.
    private val mSubjects: MutableMap<String, MutableSharedFlow<Permission>?> = HashMap()
    private var mLogging = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissions(permissions: Array<String>) {
        requestPermissions(permissions, PERMISSIONS_REQUEST_CODE)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSIONS_REQUEST_CODE) return
        val shouldShowRequestPermissionRationale = BooleanArray(permissions.size)
        for (i in permissions.indices) {
            shouldShowRequestPermissionRationale[i] = shouldShowRequestPermissionRationale(
                permissions[i]
            )
        }
        onRequestPermissionsResult(permissions, grantResults, shouldShowRequestPermissionRationale)
    }

    fun onRequestPermissionsResult(
        permissions: Array<String>,
        grantResults: IntArray,
        shouldShowRequestPermissionRationale: BooleanArray
    ) {
        lifecycleScope.launch {
            permissions.forEachIndexed { i, permission ->
                log("onRequestPermissionsResult  $permission")
                // Find the corresponding subject
                val subject = mSubjects[permission]
                if (subject == null) {
                    // No subject found
                    Log.e(
                        FlowPermissions.TAG,
                        "FlowPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request."
                    )
                    return@launch
                }
                mSubjects.remove(permission)
                val granted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                subject.emit(Permission(
                    permission,
                    granted,
                    shouldShowRequestPermissionRationale[i]
                ))
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun isGranted(permission: String?): Boolean {
        val fragmentActivity = activity
            ?: throw IllegalStateException("This fragment must be attached to an activity.")
        return fragmentActivity.checkSelfPermission(permission!!) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun isRevoked(permission: String?): Boolean {
        val fragmentActivity = activity
            ?: throw IllegalStateException("This fragment must be attached to an activity.")
        return fragmentActivity.packageManager.isPermissionRevokedByPolicy(
            permission!!,
            activity!!.packageName
        )
    }

    fun setLogging(logging: Boolean) {
        mLogging = logging
    }

    fun getSubjectByPermission(permission: String): MutableSharedFlow<Permission>? {
        return mSubjects[permission]
    }

    fun containsByPermission(permission: String): Boolean {
        return mSubjects.containsKey(permission)
    }

    fun setSubjectForPermission(permission: String, subject: MutableSharedFlow<Permission>) {
        mSubjects[permission] = subject
    }

    fun log(message: String?) {
        if (mLogging) {
            Log.d(FlowPermissions.TAG, message!!)
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 42
    }
}