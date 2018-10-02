package com.microntek.btmusic;

import android.app.Application;
import android.util.Log;

import com.ag.lfm.Lfm;
import com.microntek.CrashHandler;

public class MainApplication extends Application {
    public void onCreate() {
        Log.i("com.microntek.btmusic","MainApplication: onCreate");
        Lfm.initialize(this);
        CrashHandler.handleCrashes(getApplicationContext());
        super.onCreate();
    }
}
