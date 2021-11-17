package com.auvious.auviousproject

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.auvious.auvioussdk.core.AuviousConferenceSDK
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceOptions
import com.auvious.auvioussdk.ui.simpleconference.AuviousSimpleConferenceActivity
import com.auvious.auvioussdk.ui.simpleconference.AuviousSdkSimpleConferenceError
import kotlinx.android.synthetic.main.activity_main.*

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

/**
 * Created by BKarampinis on 13-Jul-20
 */
class MainActivity: AppCompatActivity() {

    private val simpleConferenceActivityRequestCode = 999

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

    private fun setupViews(){

        /**
         * AuviousSimpleConferenceActivity example
         * */
        button_conference.setOnClickListener {
            if(edit_ticket.text.isNullOrEmpty()){
                Toast.makeText(this, "Please supply a valid ticket id to continue", Toast.LENGTH_SHORT).show()
            }else if(!permissionConfirmed()){
                Toast.makeText(this, "Please give needed permissions to application first", Toast.LENGTH_SHORT).show()
            }else{
                startSimpleConferenceActivity()
            }
        }

    }

    private fun startSimpleConferenceActivity(){
        val clientId = "customer"
        val baseEndpoint = "https://auvious.video"
        val mqttEndpoint = "wss://auvious.video/ws"
        val params = mapOf(
            "ticket" to edit_ticket.text.toString(),
            "grant_type" to "password"
        )

        val callOptions = AuviousSimpleConferenceOptions(
            clientId,
            baseEndpoint,
            mqttEndpoint,
            params
        )

        startActivityForResult(
            AuviousSimpleConferenceActivity.getIntent(this, callOptions),
            simpleConferenceActivityRequestCode
        )

    }

    private fun permissionConfirmed(): Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == simpleConferenceActivityRequestCode){
            if(resultCode != Activity.RESULT_OK){
                data?.getParcelableExtra<AuviousSdkSimpleConferenceError>(AuviousSimpleConferenceActivity.getResultIntentName())?.let{
                    Toast.makeText(this, "Error code is ${it.errorCode}", Toast.LENGTH_LONG).show()
                }
            }
            AuviousConferenceSDK.instance.onDestroy()
        }else{
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}