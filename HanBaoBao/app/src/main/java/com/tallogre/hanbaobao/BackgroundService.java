package com.tallogre.hanbaobao;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.tallogre.hanbaobao.Utilities.CharacterUtil;

import rx.Observable;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
    private static final int NOTIFICATION_ID = 38389001;
    public static final String ACTION_BACKGROUND = "background";
    public static final String ACTION_STOP = "stop";

    private IBinder serviceBinder;
    public FloatingWidget widget;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null) action = intent.getAction();

        if (action != null && action.equals(ACTION_STOP)) {
            widget.hide();
            stopForeground(true);
            NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(BackgroundService.ACTION_STOP);
            sendBroadcast(broadcastIntent);
            stopSelf();
            return START_STICKY;
        }

        if (action == null || !action.equals(ACTION_BACKGROUND)) {
            show();
        }

        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action == null || !action.equals(ACTION_BACKGROUND)) {
            show();
        }
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getApplicationContext().setTheme(R.style.AppTheme);
        if (serviceBinder == null) serviceBinder = new ServiceBinder();
        if (widget == null) {
            widget = new FloatingWidget();
        }

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }

    private void destroy() {
        if (widget != null) widget.hide();
    }

    private Notification createNotification() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_burger);
        builder.setLargeIcon(icon);
        builder.setLocalOnly(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        builder.setContent(createNotificationContent());

        final Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyPendingIntent);

        return builder.build();
    }

    public Intent getExitIntent() {
        Intent stopIntent = new Intent(this, BackgroundService.class);
        stopIntent.setAction(ACTION_STOP);
        return stopIntent;
    }

    private RemoteViews createNotificationContent() {
        RemoteViews result = new RemoteViews(getPackageName(), R.layout.notification);
        PendingIntent deleteIntent = PendingIntent.getService(this, 0, getExitIntent(), 0);
        result.setOnClickPendingIntent(R.id.close, deleteIntent);
        return result;
    }

    public void show() {
        widget.show();
    }

    public void onAccessibilityEventText(CharSequence text) {
        if (widget.isTouchModeEnabled() && CharacterUtil.isProbablyChinese(text)) {
            widget.convertToPinyin(text);
        }
    }

    public boolean onKeyEvent(KeyEvent event) {
        return widget.onKeyEvent(event);
    }

    public class ServiceBinder extends Binder {
        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }
}
