package com.auvious.auviousproject

import android.Manifest
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.auvious.auviousproject.databinding.ActivityMainBinding
import com.auvious.auvioussdk.core.AuviousConferenceSDK
import com.auvious.auvioussdk.ui.simpleconference.AuviousSdkSimpleConferenceError
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceActivity
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceOptions
import com.auvious.auvioussdk.utils.helpers.parcelable
import kotlin.random.Random

/**
 * Created by BKarampinis on 13-Jul-20
 */
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
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindowInsets()
        setupViews()
    }

    private fun setupViews() {
        binding.requestCameraPerm.setOnClickListener { checkForCameraStoragePermission() }
        binding.requestMicPerm.setOnClickListener { checkForAudioRecordPermission() }
        binding.requestStoragePerm.setOnClickListener { checkForWriteStoragePermission() }
        binding.sdkVersion.text = "SDK v${getString(com.auvious.auvioussdk.R.string.version_name)}"
        /**
         * AuviousSimpleConferenceActivity example
         * */
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
                    enableEarPieceSpeaker = binding.speakerSwitch.isChecked,
                    availableCamButton = binding.cameraAvailableSwitch.isChecked,
                    availableMicButton = binding.micAvailableSwitch.isChecked,
                    availableSpeakerButton = binding.speakerAvailableSwitch.isChecked,
                    customConferenceBackgroundColor = binding.conferenceBackgroundSwitch.isChecked
                )
            }
        }
    }
    private fun startSimpleConferenceActivity(
        enableMic: Boolean = true,
        enableCam: Boolean = true,
        enableEarPieceSpeaker: Boolean = false,
        availableMicButton: Boolean = true,
        availableCamButton: Boolean = true,
        availableSpeakerButton: Boolean = true,
        customConferenceBackgroundColor: Boolean = false,
        enablePipMenu: Boolean = true,
        enableScreenShare: Boolean = true,
        autoEnterPipOnHome: Boolean = true
    ) {

        val callOptions = AuviousSimpleConferenceOptions(
            "customer",
            "https://auvious.video",
            "wss://auvious.video/ws",
            mapOf(
                "ticket" to binding.ticketText.text.toString(),
                "grant_type" to "password",
                AuviousSimpleConferenceOptions.speakerOption to (!enableEarPieceSpeaker).toString(),
                AuviousSimpleConferenceOptions.microphoneOption to enableMic.toString(),
                AuviousSimpleConferenceOptions.cameraOption to enableCam.toString(),
                AuviousSimpleConferenceOptions.cameraAvailable to availableCamButton.toString(),
                AuviousSimpleConferenceOptions.microphoneAvailable to availableMicButton.toString(),
                AuviousSimpleConferenceOptions.speakerAvailable to availableSpeakerButton.toString(),
                AuviousSimpleConferenceOptions.conferenceBackgroundColor to if (customConferenceBackgroundColor) Color.parseColor("#3366ff").toString() else Color.BLACK.toString(),
                // Picture-in-Picture options (Android 8.0+)
                AuviousSimpleConferenceOptions.pipAvailability to enablePipMenu.toString(),
                AuviousSimpleConferenceOptions.autoEnterPip to autoEnterPipOnHome.toString(),
                // Screen sharing (Android 5.0+)
                AuviousSimpleConferenceOptions.screenShareAvailability to enableScreenShare.toString()
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

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                navigationBars.bottom
            )

            insets
        }
    }
}