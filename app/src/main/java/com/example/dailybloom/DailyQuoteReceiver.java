package com.example.dailybloom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class DailyQuoteReceiver extends BroadcastReceiver {
    private static final String TAG = "DailyQuoteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        boolean isManualTrigger = intent.getBooleanExtra("manual_trigger", false);

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DailyQuoteWorker.class)
                .addTag("daily_update")
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        if (!isManualTrigger) {
            MyApp app = (MyApp) context.getApplicationContext();
            app.scheduleDailyAlarm();
        }
    }
}