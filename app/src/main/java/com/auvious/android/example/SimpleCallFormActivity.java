package com.auvious.android.example;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.auvious.call.ui.AuviousCallException;
import com.auvious.call.ui.AuviousSimpleCallActivity;
import com.auvious.call.ui.AuviousSimpleCallDelegate;
import com.auvious.call.ui.AuviousSimpleCallFactory;
import com.auvious.call.ui.AuviousSimpleCallOptions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class SimpleCallFormActivity extends Activity {
    private static final String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.RECORD_AUDIO
    };

    private static final int RC_RTC_PERM = 111;


    public static final String TAG = SimpleCallFormActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_call);

        final EditText userId = findViewById(R.id.userId);
        final EditText target = findViewById(R.id.target);

        findViewById(R.id.connect_button).setOnClickListener(view -> {
            AuviousSimpleCallOptions options = new AuviousSimpleCallOptions();
            options.setBaseEndpoint("https://prxbauviousvideo.praxiabank.com");
            options.setMqttEndpoint("wss://prxbauviousvideo.praxiabank.com/ws");

            options.setUsername(userId.getText().toString());
            options.setPassword(options.getUsername());
            Map<String, String> sipHeaders = new LinkedHashMap<>();
            sipHeaders.put("X-Genesys-Video_MSISDN", options.getUsername());
            sipHeaders.put("X-Genesys-Video_EMAIL", "test@test.gr");
            sipHeaders.put("X-Genesys-Video_APPSESSIONID", UUID.randomUUID().toString());
            sipHeaders.put("X-Genesys-Video_TOPIC", "OnBoarding");
            sipHeaders.put("X-Genesys-Video_LANG", "GR");

            options.setSipHeaders(sipHeaders);
            options.setTarget(target.getText().toString());
            options.setTimeout(25);

            AuviousSimpleCallDelegate delegate = new AuviousSimpleCallDelegate() {
                @Override
                public void onCallSuccess() {
                    Log.d(TAG, "call successful");
                }

                @Override
                public void onCallError(AuviousCallException exception) {
                    Log.e(TAG, String.format("call error: %s", exception.getMessage()), exception);
                }
            };

            AuviousSimpleCallFactory.initialize(options, delegate);

            Intent intent = new Intent(getApplicationContext(), AuviousSimpleCallActivity.class);

            startActivity(intent);
        });

        EasyPermissions.requestPermissions(this, getString(R.string.rtc_perms_rationale),
                RC_RTC_PERM, perms);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_RTC_PERM)
    private void methodRequiresTwoPermission() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            Log.d(TAG, "Already have permissions");
        } else {
            // Do not have permissions, request them now
            Log.d(TAG, "Requesting permissions");
            EasyPermissions.requestPermissions(this, getString(R.string.rtc_perms_rationale),
                    RC_RTC_PERM, perms);
        }
    }
}