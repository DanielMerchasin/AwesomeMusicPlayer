package com.daniel.awesomemusicplayer;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

/**
 * Main Application class
 * Handles the notification channel for the foreground service
 */
public class App extends Application {

    private static final String LOG_TAG = App.class.getSimpleName();

    public static final String SERVICE_CHANNEL_ID = "MUSIC_PLAYER_CHANNEL_1";
    public static final String SERVICE_CHANNEL_NAME = "AwesomeMusicPlayer Service";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            // Create the channel
            createNotificationChannel();
        else
            // No need to use channels
            Log.d(LOG_TAG, "Notification channel not created.");
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLightColor(getColor(R.color.colorPrimary));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null)
            notificationManager.createNotificationChannel(channel);
        Log.d(LOG_TAG, "Notification channel created.");
    }

}
