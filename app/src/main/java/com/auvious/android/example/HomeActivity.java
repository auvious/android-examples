package com.auvious.android.example;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.auvious.android.example.call.CallActivity;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.EasyPermissions;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class HomeActivity extends BaseActivity {

    public static final String TAG = "HomeActivity";
    public static final String EXTRA_USER_ID = "extra_user_id";
    String[] perms = {
            CAMERA,
            READ_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE,
            RECORD_AUDIO
    };
    private static final int RC_RTC_PERM = 111;

    private String userId;

    @BindView(R.id.call_username)
    EditText callTargetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        requestPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }

    @Override
    public void onBackPressed() {
        logout(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void requestPermissions() {
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Permissions needed", RC_RTC_PERM, perms);
        }
    }

    protected void hideKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(current.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
        }
    }

    @OnClick(R.id.call)
    public void call(View view) {
        Log.d(TAG, "call: Starting Call activity");
        String username = callTargetView.getText().toString().trim();
        initCall(username);
        hideKeyboard();
    }

    public void logout(View view) {
        Log.d(TAG, "logout: ");
    }

    public void initCall(String callingId) {
        Intent intent = new Intent(HomeActivity.this, CallActivity.class);
        intent.putExtra(CallActivity.USER_ID, userId);
        if (callingId != null && !callingId.isEmpty()) {
            intent.putExtra(CallActivity.TARGET, callingId);
        }
        intent.putExtra("X-Genesys-Video_MSISDN", "6971234567");
        intent.putExtra("X-Genesys-Video_EMAIL", "test@test.gr");
        intent.putExtra("X-Genesys-Video_APPSESSIONID", UUID.randomUUID().toString());
        intent.putExtra("X-Genesys-Video_TOPIC", "OnBoarding");
        intent.putExtra(CallActivity.MIRROR_LOCAL_VIEW, false);

        startActivity(intent);
    }
}
