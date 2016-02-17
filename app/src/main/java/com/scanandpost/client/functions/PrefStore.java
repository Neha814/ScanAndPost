package com.scanandpost.client.functions;

import android.content.Context;
import android.content.SharedPreferences;

import android.util.Log;

import com.scanandpost.client.android.BuildConfig;

/**
 * Created by Neha on 12/29/2015.
 */
public class PrefStore {

    private Context mAct;
    private String PREFS_NAME = "transport";
    private String TAG = "PrefStore";

    public PrefStore(Context context) {
        this.mAct = context;
    }

    // save client id
    public void SaveClientID(String clientID) {
        SharedPreferences settings = mAct.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("client_id", clientID);
        // Commit the edits!
        editor.commit();
        log(clientID);
    }


    public void log(String string) {
        if (BuildConfig.DEBUG)
            Log.i(TAG, string);

    }

}
