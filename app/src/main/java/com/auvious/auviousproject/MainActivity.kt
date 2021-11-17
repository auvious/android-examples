package com.auvious.auviousproject

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.auvious.auvioussdk.core.AuviousConferenceSDK
import com.auvious.auvioussdk.ui.simpleconference.AuviousSdkSimpleConferenceError
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceActivity
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceOptions
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by BKarampinis on 13-Jul-20
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        // app-center
        AppCenter.start(
            application, "f1c0fdfc-612a-40c1-ab66-8cb5a8cfd722",
            Analytics::class.java, Crashes::class.java
        )
    }

    private fun setupViews() {

        /**
         * AuviousSimpleConferenceActivity example
         * */
        button_conference.setOnClickListener {
            if (edit_ticket.text.isNullOrEmpty()) {
                Toast.makeText(
                    this,
                    "Please supply a valid ticket id to continue",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!permissionConfirmed()) {
                Toast.makeText(
                    this,
                    "Please give needed permissions to application first",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startSimpleConferenceActivity()
            }
        }

    }

    private fun startSimpleConferenceActivity() {
        val callOptions = AuviousSimpleConferenceOptions(
            "customer",
            "https://auvious.video",
            "wss://auvious.video/ws",
            mapOf(
                "ticket" to edit_ticket.text.toString(),
                "grant_type" to "password"
            )
        )

        val startForResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                if (result.resultCode != Activity.RESULT_OK) {
                    //  you will get result here in result.data
                    result.data?.getParcelableExtra<AuviousSdkSimpleConferenceError>(
                        AuviousSimpleConferenceActivity.getResultIntentName()
                    )?.let {
                        Toast.makeText(this, "Error code is ${it.errorCode}", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                AuviousConferenceSDK.instance.onDestroy()
            }
        startForResult.launch(AuviousSimpleConferenceActivity.getIntent(this, callOptions))
    }

    private fun permissionConfirmed(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PERMISSION_GRANTED
    }
}