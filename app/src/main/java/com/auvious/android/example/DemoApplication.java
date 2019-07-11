package com.auvious.android.example;

import android.app.Application;

import com.auvious.authentication.AuthenticationApi;
import com.auvious.call.CallApi;

public class DemoApplication extends Application {
    private static DemoApplication instance = null;

    private CallApi callApi = null;
    private AuthenticationApi authenticationApi = null;
    public String baseUrl = "https://prxbauviousvideo.praxiabank.com/";
    public String mqttUri = "wss://prxbauviousvideo.praxiabank.com/ws";
    // public String baseUrl = "https://test-rtc.auvious.com/";
    // public String mqttUri = "wss://test-rtc.auvious.com/ws";

    public boolean useStandardOauth2 = true;

    public static DemoApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public CallApi getCallApi(String token, String uuid) {
        if (callApi == null) {
            this.callApi = new CallApi(this, token, uuid, baseUrl, mqttUri);
        } else {
            callApi.setToken(token);
        }
        return callApi;
    }

    public AuthenticationApi getAuthApi() {
        if (this.authenticationApi == null) {
            this.authenticationApi = new AuthenticationApi(baseUrl);
        }

        return authenticationApi;
    }
}
