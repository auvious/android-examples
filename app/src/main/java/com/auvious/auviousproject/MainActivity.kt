package com.auvious.auviousproject

import android.Manifest
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.auvious.auviousproject.cobrowse.CardNumberField
import com.auvious.auviousproject.cobrowse.CobrowseDemoSettings
import com.auvious.auviousproject.cobrowse.InCallCobrowseDemo
import com.auvious.auviousproject.cobrowse.SendMoneyActivity
import com.auvious.auviousproject.databinding.ActivityMainBinding
import com.auvious.auvioussdk.cobrowse.AuviousCobrowse
import com.auvious.auvioussdk.cobrowse.AuviousCobrowseConfiguration
import com.auvious.auvioussdk.cobrowse.AuviousCobrowseEndReason
import com.auvious.auvioussdk.cobrowse.AuviousCobrowseError
import com.auvious.auvioussdk.cobrowse.AuviousCobrowseListener
import com.auvious.auvioussdk.cobrowse.AuviousMaskingRules
import com.auvious.auviousproject.databinding.DialogSelectServerUrlBinding
import com.auvious.auvioussdk.R
import com.auvious.auvioussdk.ui.simpleconference.AuviousCallMode
import com.auvious.auvioussdk.ui.simpleconference.AuviousSdkSimpleConferenceError
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceActivity
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceOptions
import com.auvious.auvioussdk.utils.helpers.parcelable
import com.auvious.auvioussdk.utils.helpers.remote.LogLevel
import kotlin.random.Random
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

/**
 * Created by BKarampinis on 13-Jul-20
 */
class MainActivity : AppCompatActivity() {
    companion object {
        const val SHARED_PREFS_NAME = "sample_app_env_prefs"  // File name
        const val PREF_KEY_ENVIRONMENT = "selected_environment"  // Key for environment preference
        const val PREF_KEY_CALL_MODE = "selected_call_mode"
    }

    private val sharedPrefs by lazy {
        getSharedPreferences(
            SHARED_PREFS_NAME,
            MODE_PRIVATE
        )
    }
    private lateinit var binding: ActivityMainBinding
    private var activityForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            if (result.resultCode != RESULT_OK) {
                //  you will get result here in result.data
                result.data?.parcelable<AuviousSdkSimpleConferenceError>(
                    AuviousSimpleConferenceActivity.getResultIntentName()
                )?.let {
                    Toast.makeText(
                        baseContext,
                        "Exit with code: ${it.errorCode}",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }

    // Android stops showing the prompt after two denials: the app's notification settings take over
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Toast.makeText(this, "Notifications permission is granted", Toast.LENGTH_SHORT).show()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                openNotificationSettings()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reopening from the launcher can stack a new main screen when the task wasn't started by it:
        // return to where the customer was (e.g. a co-browse page) instead
        if (!isTaskRoot && intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            finish()
            return
        }
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWindowInsets()
        setupViews()
    }

    private fun setupWindowInsets() {
        val bottomBarPadding = binding.bottomBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Keeps the action button above the navigation bar, and above the keyboard while typing
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()).bottom

            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomBar.setPadding(
                binding.bottomBar.paddingLeft,
                binding.bottomBar.paddingTop,
                binding.bottomBar.paddingRight,
                bottomBarPadding + bottom
            )

            insets
        }
    }

    private fun setupViews() {
        binding.requestCameraPerm.setOnClickListener { checkForCameraStoragePermission() }
        binding.requestMicPerm.setOnClickListener { checkForAudioRecordPermission() }
        binding.requestStoragePerm.setOnClickListener { checkForWriteStoragePermission() }
        binding.requestNotificationsPerm.setOnClickListener { checkForNotificationPermission() }
        binding.sdkVersion.text = "SDK v${getString(R.string.version_name)}"
        binding.serverUrlButton.setOnClickListener { showServerDialog() }
        binding.callModeButton.setOnClickListener { showCallModeDialog() }
        updateCallMode()
        updateServerUrl()

        binding.modeToggle.addOnButtonCheckedListener { _, _, isChecked -> if (isChecked) updateMode() }
        updateMode()

        // Start from the SDK defaults
        val maskingDefaults = AuviousMaskingRules()
        binding.maskMarkedViewsSwitch.isChecked = CobrowseDemoSettings.marksViews
        binding.maskIdentifiersSwitch.isChecked = true
        binding.maskClassesSwitch.isChecked = true
        binding.maskSecureFieldsSwitch.isChecked = maskingDefaults.maskSecureTextFields
        binding.maskAllTextInputsSwitch.isChecked = maskingDefaults.maskAllTextInputs
        val cobrowseDefaults = AuviousCobrowseConfiguration(ticket = "", baseEndpoint = "", mqttEndpoint = "")
        binding.entireScreenShareSwitch.isChecked = cobrowseDefaults.allowsEntireScreenShare
        binding.maskHintsSwitch.isChecked = cobrowseDefaults.showsMaskHints
        binding.debugMasksSwitch.isChecked = AuviousCobrowse.debugHighlightMasks

        /**
         * AuviousSimpleConferenceActivity example
         * */
        binding.joinCall.setOnClickListener {
            val env = AuviousEnvironment.fromName(sharedPrefs.getString(PREF_KEY_ENVIRONMENT, AuviousEnvironment.PRODUCTION.name))
            if (binding.ticketText.text.isNullOrBlank()) {
                binding.ticket.error = "Please enter your ticket"
                binding.ticketText.requestFocus()
            } else if (isCobrowseMode) {
                // Co-browse needs no camera or microphone permission
                startCobrowse(env)
            } else if (!permissionConfirmed()) {
                Toast.makeText(
                    this,
                    "Some of the permissions are not granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startSimpleConferenceActivity(
                    environment = env,
                    callMode = callMode,
                    enableEarPieceSpeaker = binding.speakerSwitch.isChecked,
                    availableCamButton = binding.cameraAvailableSwitch.isChecked,
                    availableMicButton = binding.micAvailableSwitch.isChecked,
                    availableSpeakerButton = binding.speakerAvailableSwitch.isChecked,
                    availablePipButton = binding.pipAvailableSwitch.isChecked,
                    availableScreenShareButton = binding.screenShareAvailableSwitch.isChecked,
                    customConferenceBackgroundColor = binding.conferenceBackgroundSwitch.isChecked,
                    allowsEntireScreenShare = binding.callEntireScreenShareSwitch.isChecked,
                    allowsCobrowse = binding.callCobrowseSwitch.isChecked,
                    allowsArPointer = binding.callArPointerSwitch.isChecked
                )
            }
        }
        binding.ticketText.setOnFocusChangeListener { _, _ -> binding.ticket.error = null }
    }

    private val participantName: String?
        get() = binding.participantNameText.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    /** The stream published when joining, picked in the call options */
    private val callMode: AuviousCallMode
        get() = AuviousCallMode.entries.firstOrNull { it.name == sharedPrefs.getString(PREF_KEY_CALL_MODE, null) }
            ?: AuviousCallMode.AUDIO_VIDEO

    private val AuviousCallMode.title: String
        get() = when (this) {
            AuviousCallMode.AUDIO -> "Audio"
            AuviousCallMode.VIDEO -> "Video"
            AuviousCallMode.AUDIO_VIDEO -> "Audio + video"
        }

    private val isCobrowseMode: Boolean
        get() = binding.modeToggle.checkedButtonId == binding.modeCobrowse.id

    /** Shows the call options or the co-browse ones below the session details */
    private fun updateMode() {
        binding.callOptions.visibility = if (isCobrowseMode) View.GONE else View.VISIBLE
        binding.cobrowseOptions.visibility = if (isCobrowseMode) View.VISIBLE else View.GONE
        binding.joinCall.text = if (isCobrowseMode) "Start co-browse" else "Join call"
        binding.joinCall.setIconResource(if (isCobrowseMode) com.auvious.auviousproject.R.drawable.ic_screen_share else com.auvious.auviousproject.R.drawable.ic_videocam)
    }

    /**
     * AuviousCobrowse example: share this app's screen with the agent who created the ticket.
     * The demo screens show the masking rules configured here.
     */
    private fun startCobrowse(environment: AuviousEnvironment) {
        val configuration = AuviousCobrowseConfiguration(
            ticket = binding.ticketText.text.toString(),
            baseEndpoint = environment.baseUrl,
            mqttEndpoint = environment.mqttUrl,
            participantName = participantName,
            masking = maskingRules(),
            showsMaskHints = binding.maskHintsSwitch.isChecked,
            allowsEntireScreenShare = binding.entireScreenShareSwitch.isChecked
        )
        // The sample forms read this when they build their fields
        CobrowseDemoSettings.marksViews = binding.maskMarkedViewsSwitch.isChecked
        AuviousCobrowse.debugHighlightMasks = binding.debugMasksSwitch.isChecked

        AuviousCobrowse.start(this, configuration, object : AuviousCobrowseListener {
            override fun onCobrowseFailed(error: AuviousCobrowseError) {
                Toast.makeText(this@MainActivity, "Co-browse failed: $error", Toast.LENGTH_LONG).show()
            }

            override fun onCobrowseEnded(reason: AuviousCobrowseEndReason) {
                Toast.makeText(this@MainActivity, "Co-browse ended: $reason", Toast.LENGTH_SHORT).show()
                // Close the demo screens and return to the page that started the session
                startActivity(
                    Intent(this@MainActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
        })

        if (AuviousCobrowse.isActive) {
            startActivity(Intent(this, SendMoneyActivity::class.java))
        }
    }

    /** The co-browse masking options, for co-browse on its own and in a call */
    private fun maskingRules() = AuviousMaskingRules(
        // Mask by name: any view tagged with this identifier, and every CardNumberField
        identifiers = if (binding.maskIdentifiersSwitch.isChecked) setOf("payment.iban") else emptySet(),
        viewClasses = if (binding.maskClassesSwitch.isChecked) listOf(CardNumberField::class.java) else emptyList(),
        maskSecureTextFields = binding.maskSecureFieldsSwitch.isChecked,
        maskAllTextInputs = binding.maskAllTextInputsSwitch.isChecked
    )

    private fun startSimpleConferenceActivity(
        environment: AuviousEnvironment = AuviousEnvironment.PRODUCTION,
        callMode: AuviousCallMode = AuviousCallMode.AUDIO_VIDEO,
        enableEarPieceSpeaker: Boolean = false,
        availableMicButton: Boolean = true,
        availableCamButton: Boolean = true,
        availableSpeakerButton: Boolean = true,
        availablePipButton: Boolean = true,
        availableScreenShareButton: Boolean = true,
        customConferenceBackgroundColor: Boolean = false,
        allowsEntireScreenShare: Boolean = false,
        allowsCobrowse: Boolean = false,
        allowsArPointer: Boolean = true
    ) {
        // The agent can co-browse the call: masking and the sample pages follow the co-browse options
        if (allowsCobrowse) {
            AuviousCobrowse.maskingRules = maskingRules()
            CobrowseDemoSettings.marksViews = binding.maskMarkedViewsSwitch.isChecked
            AuviousCobrowse.debugHighlightMasks = binding.debugMasksSwitch.isChecked
            InCallCobrowseDemo.install(application)
        } else {
            InCallCobrowseDemo.uninstall()
        }

        val callOptions = AuviousSimpleConferenceOptions(
            clientId = "customer",
            baseEndpoint = environment.baseUrl,
            mqttEndpoint = environment.mqttUrl,
            logLevel = LogLevel.FULL,
            params = mapOf(
                "ticket" to binding.ticketText.text.toString(),
                "grant_type" to "password",
                AuviousSimpleConferenceOptions.speakerOption to (!enableEarPieceSpeaker).toString(),
                AuviousSimpleConferenceOptions.callMode to callMode.value,
                AuviousSimpleConferenceOptions.cameraAvailable to availableCamButton.toString(),
                AuviousSimpleConferenceOptions.microphoneAvailable to availableMicButton.toString(),
                AuviousSimpleConferenceOptions.speakerAvailable to availableSpeakerButton.toString(),
                AuviousSimpleConferenceOptions.pipAvailability to availablePipButton.toString(),
                AuviousSimpleConferenceOptions.screenShareAvailability to availableScreenShareButton.toString(),
                AuviousSimpleConferenceOptions.allowsEntireScreenShare to allowsEntireScreenShare.toString(),
                AuviousSimpleConferenceOptions.cobrowseAvailable to allowsCobrowse.toString(),
                AuviousSimpleConferenceOptions.arPointerAvailable to allowsArPointer.toString(),
                AuviousSimpleConferenceOptions.cobrowseShowsMaskHints to binding.maskHintsSwitch.isChecked.toString(),
            ) + participantName?.let { mapOf(AuviousSimpleConferenceOptions.participantName to it) }.orEmpty() + if (customConferenceBackgroundColor) {
                // Without it the SDK uses its default background (#232931)
                mapOf(AuviousSimpleConferenceOptions.conferenceBackgroundColor to "#3366ff".toColorInt().toString())
            } else {
                emptyMap()
            },
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

    /** Lets the screen-sharing notification show while sharing */
    private fun checkForNotificationPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED ->
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            // Granted, but the customer can still turn them off in Settings
            !NotificationManagerCompat.from(this).areNotificationsEnabled() -> openNotificationSettings()
            else -> Toast.makeText(this, "Notifications permission is granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
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

    private fun showServerDialog() {
        // Inflate the dialog layout using the binding
        val binding = DialogSelectServerUrlBinding.inflate(layoutInflater)
        val dialog =
            AlertDialog.Builder(this).setView(binding.root).setTitle("Server Connection URL")
                .create()

        // One radio button per environment, the current one checked
        val current = AuviousEnvironment.fromName(sharedPrefs.getString(PREF_KEY_ENVIRONMENT, AuviousEnvironment.PRODUCTION.name))
        AuviousEnvironment.entries.forEach { environment ->
            val radio = RadioButton(this).apply {
                id = View.generateViewId()
                text = "${environment.title} (${environment.baseUrl.removePrefix("https://")})"
                contentDescription = "${environment.title} server radio button"
                tag = environment.name
            }
            binding.serverOptionsGroup.addView(radio)
            radio.isChecked = environment == current
        }

        // Save the selected server URL
        binding.saveButton.setOnClickListener {
            val checked = binding.serverOptionsGroup.findViewById<RadioButton>(binding.serverOptionsGroup.checkedRadioButtonId)
            val selectedEnvName = AuviousEnvironment.fromName(checked?.tag as? String).name

            // Save the selected environment name in SharedPreferences
            sharedPrefs.edit { putString(PREF_KEY_ENVIRONMENT, selectedEnvName) }
            updateServerUrl()
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    private fun showCallModeDialog() {
        val modes = AuviousCallMode.entries
        AlertDialog.Builder(this)
            .setTitle("Call mode")
            .setSingleChoiceItems(modes.map { it.title }.toTypedArray(), modes.indexOf(callMode)) { dialog, which ->
                sharedPrefs.edit { putString(PREF_KEY_CALL_MODE, modes[which].name) }
                updateCallMode()
                dialog.dismiss()
            }
            .show()
    }

    private fun updateCallMode() {
        binding.callModeButton.text = callMode.title
    }

    private fun updateServerUrl() {
        binding.serverUrlButton.text = AuviousEnvironment.fromName(sharedPrefs.getString(PREF_KEY_ENVIRONMENT, AuviousEnvironment.PRODUCTION.name)).baseUrl.removePrefix("https://")
    }
}