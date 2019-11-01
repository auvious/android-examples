package com.auvious.android.example;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.auvious.call.ui.AuviousCallException;
import com.auvious.call.ui.AuviousSimpleCallActivity;
import com.auvious.call.ui.AuviousSimpleCallDelegate;
import com.auvious.call.ui.AuviousSimpleCallFactory;
import com.auvious.call.ui.AuviousSimpleCallOptions;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

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

        AppCenter.start(getApplication(), "01870f11-723b-4f26-8c86-6d233eb613a3",
                Analytics.class, Crashes.class);

        setContentView(R.layout.activity_simple_call);

        final EditText userId = findViewById(R.id.userId);
        final EditText password = findViewById(R.id.password);
        final EditText target = findViewById(R.id.target);

        findViewById(R.id.connect_button).setOnClickListener(view -> {
            AuviousSimpleCallOptions options = new AuviousSimpleCallOptions();
            options.setBaseEndpoint("https://test-rtc.auvious.com");
            options.setMqttEndpoint("wss://test-rtc.auvious.com/ws");

            options.setUsername(userId.getText().toString());
            options.setPassword(password.getText().toString());

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
                    String msg = String.format("Call from %s to %s ended successfully",
                            options.getUsername(),
                            options.getTarget());
                    Log.d(TAG, msg);
                    Analytics.trackEvent(msg);

                    Toast.makeText(SimpleCallFormActivity.this, msg,
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCallError(AuviousCallException exception) {
                    String msg = String.format("Call from %s to %s failed: %s %s",
                            options.getUsername(),
                            options.getTarget(),
                            exception.getClass().getSimpleName(),
                            exception.getMessage());

                    Log.e(TAG, msg, exception);
                    Analytics.trackEvent(msg);

                    Toast.makeText(SimpleCallFormActivity.this, msg,
                            Toast.LENGTH_LONG).show();
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