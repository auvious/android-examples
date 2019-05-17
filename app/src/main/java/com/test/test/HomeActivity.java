package com.test.test;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.auvious.core.data.models.Schedule;
import com.auvious.network.Callback;
import com.test.test.call.CallActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.EasyPermissions;

public class HomeActivity extends BaseActivity {

    public static final String TAG = "HomeActivity";
    public static final String EXTRA_USER_ID = "extra_user_id";
    String[] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
    private static final int RC_RTC_PERM = 111;

    private String userId;

    @BindView(R.id.call_username)
    EditText callTargetView;
    @BindView(R.id.conference_create_name)
    EditText conferenceCreateNameView;
    @BindView(R.id.conference_mode_spinner)
    Spinner conferenceModeSpinner;
    @BindView(R.id.conference_join_name)
    EditText conferenceJoinNameView;
    @BindView(R.id.chat_join_name)
    EditText chatName;
    @BindView(R.id.presentation_join_name)
    EditText presentationName;

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
    public void oneOnOneSession(View view) {
        Log.d(TAG, "oneOnOneSession: Starting Call activity");
        String username = callTargetView.getText().toString().trim();
        initCall(username);
        hideKeyboard();
    }

    public void logout(View view) {
        Log.d(TAG, "logout: ");
    }


    public void initCall(String callingId) {
        Intent intent = new Intent(HomeActivity.this, CallActivity.class);
        intent.putExtra(CallActivity.USERNAME_EXTRA, Constants.USERNAME);
        intent.putExtra(CallActivity.USER_ID, userId);
        if (callingId != null && !callingId.isEmpty()) {
            intent.putExtra(CallActivity.CALLING_ID, callingId);
        }
        startActivity(intent);
    }
}
