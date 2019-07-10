package com.auvious.android.example;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.auvious.authentication.data.models.AccessToken;
import com.auvious.authentication.data.request.AuthenticationRequest;
import com.auvious.network.Callback;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import static android.view.View.GONE;
import static com.auvious.android.example.Constants.UUID;

public class RegisterActivity extends BaseActivity {

    public static final String TAG = "RegisterActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private boolean allowRegister;

    // UI references.
    private EditText mClientIdView;
    private EditText mOrganizationView;

    private EditText mUsernameView;
    private EditText mPasswordView;

    private View mProgressView;
    private View mRegisterFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);
        // Set up the login form.
        mClientIdView = findViewById(R.id.clientId);
        mOrganizationView = findViewById(R.id.organization);
        mUsernameView = findViewById(R.id.username);
        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == getResources().getInteger(R.integer.loginImeActionId) || id == EditorInfo.IME_NULL) {
                getDemoAccessToken();
                return true;
            }
            return false;
        });

        Button mConnectButton = findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(view -> getDemoAccessToken());

        mRegisterFormView = findViewById(R.id.register_form_outer);
        mProgressView = findViewById(R.id.login_progress);

        if (useStandardOauth2()) {
            mClientIdView.setVisibility(GONE);
            mOrganizationView.setVisibility(GONE);
            mPasswordView.setVisibility(GONE);
        }

        AppCenter.start(getApplication(), "01870f11-723b-4f26-8c86-6d233eb613a3",
                Analytics.class, Crashes.class);

        allowRegister = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptConnect(String userId) {
        if (!allowRegister) {
            return;
        }

        allowRegister = false;

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();

        if (!TextUtils.isEmpty(userId)) {
            username = userId;
        }

        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username address.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra(HomeActivity.EXTRA_USER_ID, userId);
            startActivity(intent);
        }
    }

    private boolean isUsernameValid(String username) {
        return username.length() > 0;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 0;
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

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mRegisterFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void getDemoAccessToken() {
        showProgress(true);
        if (useStandardOauth2()) {
            standardOauth2Impl();
        } else {
            auviousLoginImpl();
        }
    }

    private void standardOauth2Impl() {
        final String username = mUsernameView.getText().toString();
        //final String password = mPasswordView.getText().toString();
        final String password = username;

        getAuthenticationApi().oauth2Login(username, password, loginCallback());
    }

    private void auviousLoginImpl() {
        final String username = mUsernameView.getText().toString();
        final String password = mPasswordView.getText().toString();
        final String organization = mOrganizationView.getText().toString();
        final String clientId = mClientIdView.getText().toString();

        getAuthenticationApi()
                .auviousLogin(new AuthenticationRequest(
                        username,
                        password,
                        organization,
                        UUID,
                        clientId
                ), loginCallback());
    }

    private Callback<AccessToken> loginCallback() {
        return new Callback<AccessToken>() {
            @Override
            public void onSuccess(AccessToken data) {
                Log.d(TAG, "Authentication UserID: " + data.getUserId() + " DemoAccessToken: "
                        + data.getAccessToken());
                DemoAccessToken.token = data.getAccessToken();
                DemoAccessToken.uuid = UUID;
                DemoAccessToken.userId = data.getUserId();
                attemptConnect(data.getUserId());
            }

            @Override
            public void onError(Throwable e) {
                showProgress(false);

                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();

                Log.e(TAG, e.getMessage());
            }
        };
    }

    private boolean useStandardOauth2() {
        return DemoApplication.getInstance().useStandardOauth2;
    }

}