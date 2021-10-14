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
import kotlinx.android.synthetic.main.activity_main.edit_password
import kotlinx.android.synthetic.main.activity_main.edit_username

/**
 * Created by BKarampinis on 13-Jul-20
 */
class MainActivity: AppCompatActivity() {

    private val simpleConferenceActivityRequestCode = 999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
    }

    private fun setupViews(){

        /**
         * AuviousSimpleConferenceActivity example
         * */
        button_conference.setOnClickListener {
            if(edit_username.text.isNullOrEmpty() || edit_password.text.isNullOrEmpty()){
                Toast.makeText(this, "Please fill up fields in order to continue", Toast.LENGTH_SHORT).show()
            }else if(!permissionConfirmed()){
                Toast.makeText(this, "Please give needed permissions to application first", Toast.LENGTH_SHORT).show()
            }else{
                startSimpleConferenceActivity()
            }
        }

    }

    private fun startSimpleConferenceActivity(){
        val clientId = "clientId"
        val baseEndpoint = "baseEndpoint/"
        val mqttEndpoint = "mqttEndpoint/"
        val params = mapOf(
            "username" to edit_username.text.toString(),
            "password" to edit_password.text.toString(),
            "grant_type" to "password",
            "conference" to edit_conference_target.text.toString()
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