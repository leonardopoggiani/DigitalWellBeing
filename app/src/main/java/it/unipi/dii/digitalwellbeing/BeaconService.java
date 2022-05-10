package it.unipi.dii.digitalwellbeing;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;


public class BeaconService extends Service implements BeaconConsumer,MonitorNotifier {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Binding the BeaconNotification Application Class BeaconManager to BeaconService.
        BeaconNotification.beaconManager.bind(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Printing to check whether service called inside logcat
        System.out.println("SERVICE CALLED ------------------------------------------------->");

        return Service.START_STICKY;
    }


    @Override
    public void onBeaconServiceConnect() {
        //Specifies a class that should be called each time
        // the BeaconService sees or stops seeing a Region of beacons.
        BeaconNotification.beaconManager.addMonitorNotifier(this);

    }

    @Override
    public void didEnterRegion(Region region) {
        // Showing Notification that beacon is found
        showNotification("Found Beacon in the range","For more info go the app");
    }

    @Override
    public void didExitRegion(Region region) {
        // Showing Notification that beacon is exited from region
        showNotification("Founded Beacon Exited","For more info go the app");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

    }
    // Method for Showing Notifications
    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[] { notifyIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }
    // Method to start Broadcasting to restart the service
    // (BeaconBroadcast class)
    public void startBroadcasting(){
        Intent broadcastIntent = new Intent("com.example.anmol.beacons.RestartBeaconService");
        sendBroadcast(broadcastIntent);
    }

    // Override onDestroy method
    @Override
    public void onDestroy() {
        super.onDestroy();
        // if by chance service gets destroyed then start broadcasting to again start the service.
        startBroadcasting();
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setAction("");
        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);
    }
}
