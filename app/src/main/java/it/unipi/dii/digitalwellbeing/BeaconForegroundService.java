package it.unipi.dii.digitalwellbeing;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import java.util.ArrayList;
import java.util.List;

public class BeaconForegroundService extends Service {

    public static final String TAG = BeaconForegroundService.class.getSimpleName();

    public static final String ACTION_DEVICE_DISCOVERED = "DEVICE_DISCOVERED_ACTION";
    /*public static final String EXTRA_DEVICE = "DeviceExtra";
    public static final String EXTRA_DEVICES_COUNT = "DevicesCountExtra";*/

    private static final String STOP_SERVICE_ACTION = "STOP_SERVICE_ACTION";

    private static final String NOTIFICATION_CHANEL_NAME = "Kontakt SDK Samples";
    private static final String NOTIFICATION_CHANEL_ID = "scanning_service_channel_id";

    private ProximityManager proximityManager;
    private boolean isRunning; // Flag indicating if service is already running.
    private NotificationChannel channel;
    private NotificationManager notificationManager;
    private Notification notificationForeground;
    private DatabaseReference db;
    List<Beacon> beacon_list;
    private String device;
    private boolean notfound;
    Beacon lastbeacon;

    public static Intent createIntent(final Context context) {
        return new Intent(context, BeaconForegroundService.class);
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this, "Foreground.", Toast.LENGTH_SHORT).show();
        super.onCreate();
        db  = FirebaseDatabase.getInstance("https://digitalwellbeing-83177-default-rtdb.europe-west1.firebasedatabase.app/").getReference();;
        beacon_list = new ArrayList<>();
        setupProximityManager();
        isRunning = false;
        device = android.os.Build.MODEL;
        lastbeacon = new Beacon();
        beacon_list.clear();
        notfound = true;
        /*if(device.equals("Error")){
            onDestroy();
        }*/
    }

    private void setupProximityManager() {
        // Create proximity manager instance
        proximityManager = ProximityManagerFactory.create(this);

        // Configure proximity manager basic options
        proximityManager.configuration()
                //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanPeriod(ScanPeriod.RANGING)
                //Using BALANCED for best performance/battery ratio
                .scanMode(ScanMode.BALANCED);

        // Set up iBeacon listener
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    private boolean checkCondition(Beacon b){
        if(notfound) return false;
        if (lastbeacon == null) return false;
        if (b.getUserDevice().equals(lastbeacon.getUserDevice())) return false;
        if (b.getTimestamp() < lastbeacon.getTimestamp() - 300000 || b.getTimestamp() > lastbeacon.getTimestamp() + 300000 ) return false;
        if (!b.getProximity().equals(lastbeacon.getProximity())) return false;
        return true;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        beacon_list.clear();
        if (STOP_SERVICE_ACTION.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Check if service is already active
        if (isRunning) {
            Toast.makeText(this, "Service is already running.", Toast.LENGTH_SHORT).show();
            return START_STICKY;
        }


        db.addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Toast.makeText(getApplicationContext(), "Qualcuno ha scritto nel db", Toast.LENGTH_SHORT).show();
                beacon_list.clear();
                Beacon beacon = new Beacon();
                for (DataSnapshot postSnapshot : dataSnapshot.child("Beacon").getChildren()) {

                        beacon.setProximity(postSnapshot.child("proximity").getValue(String.class));
                        beacon.setTimestamp(postSnapshot.child("timestamp").getValue(Long.class));
                        beacon.setRssi(postSnapshot.child("rssi").getValue(Integer.class));
                        beacon.setUserDevice(postSnapshot.child("userDevice").getValue(String.class));
                        beacon.setId(postSnapshot.child("id").getValue(String.class));
                        beacon.setDistance(postSnapshot.child("distance").getValue(Double.class));
                        if(checkCondition(beacon)){
                            beacon_list.add(beacon);
                        }
                        //Toast.makeText(getApplicationContext(), "DataChange" + beacon, Toast.LENGTH_SHORT).show();



                }
                int userDetected=0;
                if (beacon_list.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Empty", Toast.LENGTH_SHORT).show();
                }else {
                    userDetected = beacon_list.size();
                    Toast.makeText(getApplicationContext(), "User detected:" + beacon_list.size(), Toast.LENGTH_SHORT).show();
                    // Create notification channel
                    Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                    String CHANNEL_ID="MYCHANNEL";
                    NotificationChannel notificationChannel=new NotificationChannel(CHANNEL_ID,"name",NotificationManager.IMPORTANCE_LOW);
                    Notification notification=new Notification.Builder(getApplicationContext(),CHANNEL_ID)
                            .setContentText("User detected")
                            .setContentTitle("Devices detected in your zone")
                            .setAutoCancel(true)
                            .setSmallIcon(android.R.drawable.sym_action_chat)
                            .build();

                    NotificationManager notificationManager=(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(String.valueOf(userDetected).hashCode(),notification);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });

        startInForeground();
        startScanning();
        isRunning = true;
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (proximityManager != null) {
            proximityManager.disconnect();
            proximityManager = null;
        }
        Toast.makeText(this, "Scanning service stopped.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    private void startInForeground() {
        // Create notification intent
        final Intent notificationIntent = new Intent();
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                0
        );

        // Create stop intent with action
        final Intent intent = BeaconForegroundService.createIntent(this);
        intent.setAction(STOP_SERVICE_ACTION);
        final PendingIntent stopIntent = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // Build notification
        final NotificationCompat.Action action = new NotificationCompat.Action(0, "Stop", stopIntent);
        notificationForeground = new NotificationCompat.Builder(this, NOTIFICATION_CHANEL_ID)
                .setContentTitle("Scan service")
                .setContentText("Actively scanning iBeacons")
                .addAction(action)
                .setSmallIcon(android.R.mipmap.sym_def_app_icon)
                .setContentIntent(pendingIntent)
                .build();

        // Start foreground service
        startForeground(1, notificationForeground);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        channel = new NotificationChannel(
                NOTIFICATION_CHANEL_ID,
                NOTIFICATION_CHANEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(channel);
    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
                //devicesCount = 0;
                Toast.makeText(BeaconForegroundService.this, "Scanning service started.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                onDeviceDiscovered(ibeacon);
                Log.i(TAG, "onIBeaconDiscovered: " + ibeacon.toString());
            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                super.onIBeaconLost(ibeacon, region);
                Log.e(TAG, "onIBeaconLost: " + ibeacon.toString());
            }
        };
    }

    public void insert (DatabaseReference db, Beacon beacon, Context context){
        //Saving the beacon object
        String pushKey = db.push().getKey();
        db.child("Beacon").push().setValue(beacon);
        //Toast.makeText(context, "Insert firebase", Toast.LENGTH_LONG).show();

    }

    private void onDeviceDiscovered(final RemoteBluetoothDevice device) {
        notfound = false;
        lastbeacon.setAddress(device.getAddress());
        lastbeacon.setDistance(device.getDistance());
        lastbeacon.setId(device.getUniqueId());
        lastbeacon.setProximity(device.getProximity().toString());
        lastbeacon.setRssi(device.getRssi());
        lastbeacon.setTimestamp(device.getTimestamp());
        lastbeacon.setUserDevice(this.device);
        insert(db, lastbeacon, getApplicationContext());
        //Send a broadcast with discovered device
        Intent intent = new Intent();
        intent.setAction(ACTION_DEVICE_DISCOVERED);
        sendBroadcast(intent);
    }

}


