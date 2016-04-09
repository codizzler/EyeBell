package com.csce.tamu.eyebell;

import android.app.Application;
import android.util.Log;

import com.backendless.Backendless;

/**
 * Created by codytaylor on 3/6/16.
 */
public class MyApp extends Application {
    public MyApp() {
        // this method fires only once per application start.
        // getApplicationContext returns null here

        Log.i("main", "Constructor fired");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String appVersion = "v1";
        Backendless.initApp(this, getString(R.string.app_id),
                getString(R.string.android_id), appVersion);


        // this method fires once as well as constructor
        // but also application has context here

        Log.i("main", "onCreate fired");
    }
}
