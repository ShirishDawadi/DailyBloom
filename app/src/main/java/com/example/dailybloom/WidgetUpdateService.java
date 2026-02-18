package com.example.dailybloom;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WidgetUpdateService extends Service {
    private static final String TAG = "WidgetUpdateService";
    private static final String CHANNEL_ID = "widget_update_channel";
    private static final int NOTIFICATION_ID = 1001;

    private ListenerRegistration dailyEntryListener;
    private FirebaseFirestore db;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
        }

        db = FirebaseFirestore.getInstance();
        setupDailyEntryListener();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Widget Updates",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps your widget up to date");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Daily Bloom")
                .setContentText("Keeping your widget fresh")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void setupDailyEntryListener() {
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", null);

        if (userId == null) {
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        dailyEntryListener = db.collection("users")
                .document(userId)
                .collection("daily_entries")
                .document(today)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String quote = documentSnapshot.getString("quote");
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        SharedPreferences prefs = getSharedPreferences("daily_data", MODE_PRIVATE);
                        prefs.edit()
                                .putString("daily_quote", quote)
                                .putString("daily_image_url", imageUrl)
                                .putString("daily_date", today)
                                .commit();

                        updateAllWidgets();
                    }
                });
    }

    private void updateAllWidgets() {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            ComponentName componentName = new ComponentName(this, FlowerWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length > 0) {
                Intent updateIntent = new Intent(this, FlowerWidgetProvider.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
                sendBroadcast(updateIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update widgets", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dailyEntryListener != null) {
            dailyEntryListener.remove();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}