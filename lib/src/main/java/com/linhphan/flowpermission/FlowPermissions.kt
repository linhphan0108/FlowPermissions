/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linhphan.flowpermission

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class FlowPermissions {
    @VisibleForTesting
    private var mFlowPermissionsFragment: Lazy<FlowPermissionsFragment>

    constructor(activity: FragmentActivity) {
        mFlowPermissionsFragment = getLazySingleton(activity.supportFragmentManager)
    }

    constructor(fragment: Fragment) {
        mFlowPermissionsFragment = getLazySingleton(fragment.childFragmentManager)
    }

    private fun getLazySingleton(fragmentManager: FragmentManager): Lazy<FlowPermissionsFragment> {
        return object : Lazy<FlowPermissionsFragment> {
            private var flowPermissionsFragment: FlowPermissionsFragment? = null
            @Synchronized
            override fun get(): FlowPermissionsFragment {
                if (flowPermissionsFragment == null) {
                    flowPermissionsFragment = getFlowPermissionsFragment(fragmentManager)
                }
                return flowPermissionsFragment!!
            }
        }
    }

    private fun getFlowPermissionsFragment(fragmentManager: FragmentManager): FlowPermissionsFragment? {
        var flowPermissionsFragment: FlowPermissionsFragment? =
            findRxPermissionsFragment(fragmentManager)
        val isNewInstance = flowPermissionsFragment == null
        if (isNewInstance) {
            flowPermissionsFragment = FlowPermissionsFragment()
            fragmentManager
                .beginTransaction()
                .add(flowPermissionsFragment, TAG)
                .commitNow()
        }
        return flowPermissionsFragment
    }

    private fun findRxPermissionsFragment(fragmentManager: FragmentManager): FlowPermissionsFragment? {
        return fragmentManager.findFragmentByTag(TAG) as FlowPermissionsFragment?
    }

    fun setLogging(logging: Boolean) {
        mFlowPermissionsFragment.get().setLogging(logging)
    }

    /**
     * Map emitted items from the source flow into `true` if permissions in parameters
     * are granted, or `false` if not.
     *
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    fun request(vararg permissions: String) = flow {
        var allPermissionsGranted = true
        requestImplementation(*permissions)
            .collect {
                it.forEach { permission ->
                    if (!permission.granted){
                        allPermissionsGranted = false
                    }
                    if (!allPermissionsGranted){
                        emit(false)
                        return@collect
                    }
                }
                emit(allPermissionsGranted)
            }
    }

    /**
     * Map emitted items from the source flow into [Permission] objects for each
     * permission in parameters.
     *
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    fun requestEach(vararg permissions: String): Flow<List<Permission>> {
        return requestImplementation(*permissions)
    }

    /**
     * Map emitted items from the source flow into one combined [Permission] object. Only if all permissions are granted,
     * permission also will be granted. If any permission has `shouldShowRationale` checked, than result also has it checked.
     *
     *
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    fun requestEachCombined(vararg permissions: String) = flow {
        requestImplementation(*permissions)
            .collect {
                emit(Permission(it))
            }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestImplementation(vararg permissions: String): Flow<List<Permission>> {

        require(permissions.isNotEmpty()) { "RxPermissions.request/requestEach requires at least one input permission" }

        val list: MutableList<MutableSharedFlow<Permission>> = ArrayList(permissions.size)
        val unrequestedPermissions: MutableList<String> = ArrayList()

        // In case of multiple permissions, we create an Observable for each of them.
        // At the end, the observables are combined to have a unique response.
        for (permission in permissions) {
            mFlowPermissionsFragment.get().log("Requesting permission $permission")
            if (isGranted(permission)) {
                // Already granted, or not Android M
                // Return a granted Permission object.
                list.add(MutableSharedFlow<Permission>().apply {
                    getLifecycleScope().launch {
                        delay(1_000)//set delay
                        emit(Permission(permission, true, false))
                    }
                })
                continue
            }
            if (isRevoked(permission)) {
                // Revoked by a policy, return a denied Permission object.
                list.add(MutableSharedFlow<Permission>().apply {
                    getLifecycleScope().launch {
                        delay(1_000)
                        emit(Permission(permission, false, false))
                    }
                })
                continue
            }
            var subject: MutableSharedFlow<Permission>? =
                mFlowPermissionsFragment.get().getSubjectByPermission(permission)
            // Create a new subject if not exists
            if (subject == null) {
                unrequestedPermissions.add(permission)
                subject = MutableSharedFlow()
                mFlowPermissionsFragment.get().setSubjectForPermission(permission, subject)
            }
            list.add(subject)
        }
        if (unrequestedPermissions.isNotEmpty()) {
            val unrequestedPermissionsArray = unrequestedPermissions.toTypedArray()
            requestPermissionsFromFragment(unrequestedPermissionsArray)
        }
        return list.flattenFlow()
    }

    /**
     * Invokes Activity.shouldShowRequestPermissionRationale and wraps
     * the returned value in an observable.
     *
     *
     * In case of multiple permissions, only emits true if
     * Activity.shouldShowRequestPermissionRationale returned true for
     * all revoked permissions.
     *
     *
     * You shouldn't call this method if all permissions have been granted.
     *
     *
     * For SDK &lt; 23, the observable will always emit false.
     */
    fun shouldShowRequestPermissionRationale(
        activity: Activity,
        vararg permissions: String
    ) = flow{
        if (!isMarshmallow) {
            emit(false)
        } else {
            emit(shouldShowRequestPermissionRationaleImplementation(activity, *permissions))
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun shouldShowRequestPermissionRationaleImplementation(
        activity: Activity,
        vararg permissions: String
    ): Boolean {
        for (p in permissions) {
            if (!isGranted(p) && !activity.shouldShowRequestPermissionRationale(p)) {
                return false
            }
        }
        return true
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissionsFromFragment(permissions: Array<String>) {
        mFlowPermissionsFragment.get()
            .log("requestPermissionsFromFragment " + TextUtils.join(", ", permissions))
        mFlowPermissionsFragment.get().requestPermissions(permissions)
    }

    /**
     * Returns true if the permission is already granted.
     *
     *
     * Always true if SDK < 23.
     */
    fun isGranted(permission: String?): Boolean {
        return !isMarshmallow || mFlowPermissionsFragment.get().isGranted(permission)
    }

    /**
     * Returns true if the permission has been revoked by a policy.
     *
     *
     * Always false if SDK &lt; 23.
     */
    fun isRevoked(permission: String?): Boolean {
        return isMarshmallow && mFlowPermissionsFragment.get().isRevoked(permission)
    }

    private val isMarshmallow: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray) {
        mFlowPermissionsFragment.get()
            .onRequestPermissionsResult(permissions, grantResults, BooleanArray(permissions.size))
    }

    private fun getLifecycleScope(): LifecycleCoroutineScope {
        return mFlowPermissionsFragment.get().lifecycleScope
    }

    interface Lazy<V> {
        fun get(): V
    }

    companion object {
        val TAG = FlowPermissions::class.java.simpleName
        val TRIGGER = Any()
    }
}