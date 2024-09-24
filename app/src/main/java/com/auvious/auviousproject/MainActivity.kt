package com.auvious.auviousproject

import android.Manifest
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.auvious.auviousproject.databinding.ActivityMainBinding
import com.auvious.auvioussdk.core.AuviousConferenceSDK
import com.auvious.auvioussdk.ui.simpleconference.AuviousSdkSimpleConferenceError
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceActivity
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceOptions
import com.auvious.auvioussdk.utils.helpers.parcelable
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var activityForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode != Activity.RESULT_OK) {
                //  you will get result here in result.data
                result.data?.parcelable<AuviousSdkSimpleConferenceError>(
                    AuviousSimpleConferenceActivity.getResultIntentName()
                )?.let {
                    Toast.makeText(baseContext, "Exit with code: ${it.errorCode}", Toast.LENGTH_LONG)
                        .show()
                }
            }
            AuviousConferenceSDK.instance.onDestroy()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupViews()
        // app-center
        AppCenter.start(
            application, "f1c0fdfc-612a-40c1-ab66-8cb5a8cfd722",
            Analytics::class.java, Crashes::class.java
        )
    }

    private fun setupViews() {
        binding.requestCameraPerm.setOnClickListener { checkForCameraStoragePermission() }
        binding.requestMicPerm.setOnClickListener { checkForAudioRecordPermission() }
        binding.requestStoragePerm.setOnClickListener { checkForWriteStoragePermission() }
        binding.joinCall.setOnClickListener {
            if (binding.ticketText.text.isNullOrEmpty()) {
                Toast.makeText(
                    this,
                    "Please supply a valid ticket id to continue",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!permissionConfirmed()) {
                Toast.makeText(
                    this,
                    "Some of the permissions are not granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startSimpleConferenceActivity(
                    enableCam = !binding.micOnlySwitch.isChecked,
                    enableSpeaker = binding.speakerSwitch.isChecked,
                )
            }
        }

    }
    private fun startSimpleConferenceActivity(
        enableMic: Boolean = true,
        enableCam: Boolean = true,
        enableSpeaker: Boolean = true
    ) {
        val callOptions = AuviousSimpleConferenceOptions(
            "customer",
            "https://auvious.video",
            "wss://auvious.video/ws",
            mapOf(
                "ticket" to binding.ticketText.text.toString(),
                "grant_type" to "password",
                AuviousSimpleConferenceOptions.microphoneOption to enableMic.toString(),
                AuviousSimpleConferenceOptions.cameraOption to enableCam.toString(),
                AuviousSimpleConferenceOptions.speakerOption to enableSpeaker.toString()
            )
        )
        activityForResult.launch(AuviousSimpleConferenceActivity.getIntent(this, callOptions))
    }

    private fun checkForCameraStoragePermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Camera permission is granted", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA))
        }
    }

    private fun checkForAudioRecordPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Record Audio permission is granted", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun checkForWriteStoragePermission() {
        val permissionRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                permissionRequest,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Read Storage permission is granted", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(arrayOf(permissionRequest))
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                Random.nextInt(100, 999)
            )
        }
    }

    private fun permissionConfirmed(): Boolean {
        return listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).all {
            ContextCompat.checkSelfPermission(this, it) == PERMISSION_GRANTED
        }
    }
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}