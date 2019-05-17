package com.test.test;

import android.app.Application;

import com.auvious.authentication.AuthenticationApi;
import com.auvious.call.OneToOneCallApi;

public class DemoApplication extends Application {
    private static DemoApplication instance = null;

    private OneToOneCallApi oneToOneCallApi = null;
    private AuthenticationApi authenticationApi = null;
    public String baseUrl = "https://staging-rtc.auvious.com/rtc-api/";
    public String busUsername = "auvious";
    public String busPassword = "auvious123";
    public String busUri = "wss://mqtt.auvious.com/ws";

    public static DemoApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        authenticationApi = new AuthenticationApi();

    }

    public OneToOneCallApi getOneToOneApi(String token, String uuid) {
        if (oneToOneCallApi == null) {
            this.oneToOneCallApi = new OneToOneCallApi(this, token, uuid, baseUrl, busUri, busUsername, busPassword);
        } else {
            oneToOneCallApi.setToken(token);
        }
        return oneToOneCallApi;
    }

    public AuthenticationApi getAuthApi() {
        return authenticationApi;
    }
}
