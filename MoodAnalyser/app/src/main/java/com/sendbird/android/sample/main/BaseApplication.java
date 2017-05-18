package com.sendbird.android.sample.main;


import android.app.Application;

import com.sendbird.android.SendBird;

public class BaseApplication extends Application {

    private static final String APP_ID = "F2769BC6-7F70-4812-8854-DBF3C3492E3F"; // US-1 Demo
    public static final String VERSION = "3.0.27";

    @Override
    public void onCreate() {
        super.onCreate();
        SendBird.init(APP_ID, getApplicationContext());
    }
}