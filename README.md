# FlowPermissions
This library allows the usage of Kotlin flow on Android M onwards.

## Usage

Create a `FlowPermissions` instance :

```kotlin
private val flowPermission by lazy { FlowPermissions(this).apply {
            setLogging(true)
        }
    }; // where this is an Activity or Fragment instance
```
**NOTE:** `FlowPermissions(this)` the `this` parameter can be a FragmentActivity or a Fragment. If you are using `FlowPermissions` inside of a fragment you should pass the fragment instance(`FlowPermissions(this)`) as constructor parameter rather than `new FlowPermissions(fragment.getActivity())` or you could face a `java.lang.IllegalStateException: FragmentManager is already executing transactions`.


Example 1 : request the CAMERA permissio
```kotlin
flowPermission.requestEachCombined(permission.CAMERA, permission.RECORD_AUDIO)
                    .catch { Log.e(TAG, it.message, it) }
                    .collect { permissions ->
                        onRequestPermissionResult(permissions)
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
```

there are also other options to request the permissions so explore them yourself.
```kotlin
request(vararg permissions: String) : Flow<Boolean>
requestEachCombined(vararg permissions: String) : Flow<Permission>
```