package com.auvious.android.example;

import android.app.Application;

import com.auvious.authentication.AuthenticationApi;
import com.auvious.call.CallApi;

public class DemoApplication extends Application {
    private static DemoApplication instance = null;

    private CallApi callApi = null;
    private AuthenticationApi authenticationApi = null;
    public String baseUrl = "https://staging-rtc.auvious.com/rtc-api/";
    public String mqttUsername = "auvious";
    public String mqttPassword = "auvious123";
    public String mqttUri = "wss://mqtt.auvious.com/ws";

    public static DemoApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        authenticationApi = new AuthenticationApi();

    }

    public CallApi getCallApi(String token, String uuid) {
        if (callApi == null) {
            this.callApi = new CallApi(this, token, uuid, baseUrl, mqttUri, mqttUsername, mqttPassword);
        } else {
            callApi.setToken(token);
        }
        return callApi;
    }

    public AuthenticationApi getAuthApi() {
        return authenticationApi;
    }
}
