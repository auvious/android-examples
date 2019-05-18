package com.auvious.android.example.call;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.auvious.call.domain.TopicListener;
import com.auvious.call.domain.entity.Event;
import com.auvious.call.domain.entity.StreamType;
import com.auvious.network.Callback;
import com.auvious.android.example.BaseActivity;
import com.auvious.android.example.R;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import pub.devrel.easypermissions.EasyPermissions;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class CallActivity extends BaseActivity implements TopicListener, EasyPermissions.PermissionCallbacks {

    private static final String TAG = "CallActivity";
    public static final String USER_ID = "USER_ID";
    public static final String CALLING_ID = "CALLING_ID";

    private String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };
    private static final int RC_RTC_PERM = 111;

    private String target, userId;
    private SurfaceViewRenderer localView, remoteView;

    private Button hangupBtn, answerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");

        setContentView(R.layout.activity_call);

        hangupBtn = findViewById(R.id.hangup);
        answerBtn = findViewById(R.id.answer);
        localView = findViewById(R.id.surface_user);
        remoteView = findViewById(R.id.surface_guest);

        EglBase rootEglBase = EglBase.create();

        remoteView.init(rootEglBase.getEglBaseContext(), null);
        remoteView.setZOrderMediaOverlay(true);

        localView.init(rootEglBase.getEglBaseContext(), null);
        localView.setZOrderMediaOverlay(true);
        localView.setMirror(true);

        Intent intent = getIntent();

        if (Objects.requireNonNull(intent.getExtras()).containsKey(CALLING_ID)) {
            target = intent.getStringExtra(CALLING_ID);
        }

        userId = intent.getStringExtra(USER_ID);

        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        registerUser();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    private void requestPermissions() {
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Permissions needed", RC_RTC_PERM, perms);
        } else {
            registerUser();
        }
    }

    private void registerUser() {
        getCallApi().setCallback(this);
        getCallApi().register(userId, new Callback() {
            @Override
            public void onSuccess(Object data) {

            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Register user error: " + e);
            }
        });
    }

    void answer(String sdp) {
        getCallApi().answer(this, sdp, localView, remoteView, StreamType.MIC_AND_CAM);
        answerBtn.setVisibility(View.GONE);
    }

    @Override
    public void initialized() {
    }

    @Override
    public void subscribed() {
        if (target != null) {
            //DO a call
            call();
        } else {
            //just wait to receive a notification
        }

    }

    @Override
    public void exit() {
        finish();
    }

    @Override
    public void error(String error) {
        Log.e(TAG, error);
    }

    @Override
    public void onMessageReceived(Event event) {
        Log.d(TAG, event.getType().toString());
        switch (event.getType()) {
            case CREATED:
                Toast.makeText(this, "brrring, brrring!", Toast.LENGTH_LONG).show();
                answerBtn.setVisibility(View.VISIBLE);
                answerBtn.setOnClickListener(v -> answer(event.getSpdOffer()));
                break;
            case RINGING:
                Toast.makeText(this, "Waiting for answer!", Toast.LENGTH_LONG).show();
                break;
            case ANSWERED:
                Completable.timer(2, TimeUnit.SECONDS)
                        .observeOn(mainThread())
                        .subscribe(
                                () -> this.hangupBtn.setVisibility(View.VISIBLE),
                                error -> Log.w(TAG, error.getMessage(), error)
                        );
                break;
            case REJECTED:
                Toast.makeText(this, "Rejected!", Toast.LENGTH_LONG).show();
                hangup(null);
                break;
            case ENDED:
                Toast.makeText(this, "Call ended!", Toast.LENGTH_LONG).show();
                Completable.timer(2, TimeUnit.SECONDS)
                        .observeOn(mainThread())
                        .subscribe(() -> hangup(null), error -> Log.w(TAG, error.getMessage(), error));
                break;
        }
    }

    protected void hideKeyboard() {
        View current = getCurrentFocus();
        if (current != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(current.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }

    protected void call() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        Objects.requireNonNull(audioManager).setSpeakerphoneOn(true);

        getCallApi().call(this, localView, remoteView, StreamType.MIC_AND_CAM, target);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Ending call!", Toast.LENGTH_LONG).show();
        getCallApi().hangup("Back button pressed");
    }

    public void hangup(View view) {
        Toast.makeText(this, "Call ended!", Toast.LENGTH_LONG).show();
        getCallApi().hangup("hangup button pressed");
    }
}
