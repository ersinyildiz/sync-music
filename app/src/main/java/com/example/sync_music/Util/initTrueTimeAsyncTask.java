package com.example.sync_music.Util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.instacart.library.truetime.TrueTime;

import java.io.IOException;




public class initTrueTimeAsyncTask extends AsyncTask<Void, Void, Void> {
    private Context ctx;
    public initTrueTimeAsyncTask (Context context){
        ctx = context;
    }
    protected Void doInBackground(Void... params) {
        try {
            TrueTime.build()
                    .withSharedPreferences(ctx)
                    .withNtpHost("time.google.com")
                    .withLoggingEnabled(false)
                    .withConnectionTimeout(31_428)
                    .initialize();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("ersiny", "Exception when trying to get TrueTime",e);
        }
        return null;
    }

}