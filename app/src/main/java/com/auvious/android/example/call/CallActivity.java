package com.auvious.android.example.call;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.auvious.android.example.AppRTCAudioManager;
import com.auvious.android.example.BaseActivity;
import com.auvious.android.example.R;
import com.auvious.call.domain.ProxyVideoSink;
import com.auvious.call.domain.SharedEglBase;
import com.auvious.call.domain.TopicListener;
import com.auvious.call.domain.entity.Event;
import com.auvious.call.domain.entity.StreamType;
import com.auvious.network.Callback;

import org.webrtc.Logging;
import org.webrtc.SurfaceViewRenderer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import pub.devrel.easypermissions.EasyPermissions;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT;

public class CallActivity extends BaseActivity implements TopicListener, EasyPermissions.PermissionCallbacks {
    private static final String TAG = "CallActivity";

    public static final String USER_ID = "USER_ID";
    public static final String TARGET = "TARGET";
    public static final String MIRROR_LOCAL_VIEW = "MIRROR_LOCAL_VIEW";

    private String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    private static final int RC_RTC_PERM = 111;

    private String target, userId;
    private SurfaceViewRenderer pipView, fullscreenView;

    private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();
    private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();

    private Button hangupBtn, answerBtn;
    @Nullable
    private AppRTCAudioManager audioManager;

    private boolean mirrorLocalView;
    private Map<String, String> sipHeaders;

    private boolean isSwappedFeeds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");

        setContentView(R.layout.activity_call);

        hangupBtn = findViewById(R.id.hangup);
        answerBtn = findViewById(R.id.answer);
        pipView = findViewById(R.id.surface_user);
        fullscreenView = findViewById(R.id.surface_guest);

        fullscreenView.setOnClickListener(view -> swapViews(!isSwappedFeeds));

        pipView.init(SharedEglBase.getEglBase().getEglBaseContext(), null);
        pipView.setZOrderMediaOverlay(true);
        pipView.setEnableHardwareScaler(true);
        pipView.setScalingType(SCALE_ASPECT_FIT);
        pipView.setZOrderOnTop(true);

        fullscreenView.init(SharedEglBase.getEglBase().getEglBaseContext(), null);
        fullscreenView.setZOrderMediaOverlay(true);
        fullscreenView.setEnableHardwareScaler(true);
        fullscreenView.setScalingType(SCALE_ASPECT_FILL);

        swapViews(true);

        Intent intent = getIntent();

        // get parameters
        userId = intent.getStringExtra(USER_ID);
        target = intent.getStringExtra(TARGET);
        mirrorLocalView = intent.getBooleanExtra(MIRROR_LOCAL_VIEW, true);
        sipHeaders = new LinkedHashMap<>();

        if (intent.getExtras() != null) {
            for (String key : intent.getExtras().keySet()) {
                if (key.startsWith("X-Genesys-Video")) {
                    sipHeaders.put(key, intent.getStringExtra(key));
                }
            }
        }

        requestPermissions();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(getApplicationContext());
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...");
        // This method will be called each time the number of available audio
// devices has changed.
        audioManager.start(this::onAudioManagerDevicesChanged);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
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
        getCallApi().answer(this, sdp, pipView, fullscreenView, StreamType.MIC_AND_CAM);
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
            case CAMERA_REQUEST:
                if (event.getCameraRequestType().equalsIgnoreCase("CAMERA_SWITCH")) {
                    swapViews(!isSwappedFeeds);
                }
                break;
            case CREATED:
                Toast.makeText(this, "brrring, brrring!", Toast.LENGTH_LONG).show();
                answerBtn.setVisibility(View.VISIBLE);
                answerBtn.setOnClickListener(v -> answer(event.getSpdOffer()));
                break;
            case RINGING:
                Toast.makeText(this, "Waiting for answer!", Toast.LENGTH_LONG).show();
                break;
            case ANSWERED:
                swapViews(false);

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
            case CONNECTION_ERROR:
                Toast.makeText(this, "Call connection error!", Toast.LENGTH_LONG).show();
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
        getCallApi().call(
                this,
                localProxyVideoSink,
                remoteProxyRenderer,
                StreamType.MIC_AND_CAM,
                target,
                sipHeaders);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Ending call!", Toast.LENGTH_LONG).show();
        getCallApi().hangup("Back button pressed");
    }

    public void hangup(View view) {
        Toast.makeText(this, "Call ended!", Toast.LENGTH_LONG).show();
        getCallApi().hangup("hangup button pressed");
        if (pipView != null) {
            pipView.release();
            pipView = null;
        }

        if (fullscreenView != null) {
            fullscreenView.release();
            fullscreenView = null;
        }
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AppRTCAudioManager.AudioDevice device,
            final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    private void swapViews(boolean isSwappedFeeds) {
        Logging.d(TAG, "swapViews: " + isSwappedFeeds);
        this.isSwappedFeeds = isSwappedFeeds;
        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenView : pipView);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipView : fullscreenView);
        if (mirrorLocalView) {
            fullscreenView.setMirror(isSwappedFeeds);
            pipView.setMirror(!isSwappedFeeds);
        }
    }

}
