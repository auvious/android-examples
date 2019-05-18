package com.auvious.android.example;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.auvious.authentication.AuthenticationApi;
import com.auvious.call.CallApi;

public abstract class BaseActivity extends AppCompatActivity {

    protected BaseActivity getContext() {
        return this;
    }

    protected CallApi getCallApi() {
        if (!TextUtils.isEmpty(DemoAccessToken.token) && !TextUtils.isEmpty(DemoAccessToken.uuid)) {
            return DemoApplication.getInstance().getCallApi(DemoAccessToken.token, DemoAccessToken.uuid);
        } else {
            return null;
        }
    }

    protected AuthenticationApi getAuthenticationApi() {
        return DemoApplication.getInstance().getAuthApi();
    }
}
