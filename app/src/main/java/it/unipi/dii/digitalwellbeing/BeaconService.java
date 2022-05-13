package it.unipi.dii.digitalwellbeing;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;
import com.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class BeaconService extends Service {

    public static final String TAG = "BackgroundScanService";
    public static final String ACTION_DEVICE_DISCOVERED = "DeviceDiscoveredAction";
    public static final String EXTRA_DEVICE = "DeviceExtra";
    public static final String EXTRA_DEVICES_COUNT = "DevicesCountExtra";

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private final Handler handler = new Handler();
    private ProximityManager proximityManager;
    private boolean isRunning; // Flag indicating if service is already running.
    private int devicesCount; // Total discovered devices count
    private boolean csvfile;

    @Override
    public void onCreate() {
        super.onCreate();
        setupProximityManager();
        Toast.makeText(this, "Service is running.", Toast.LENGTH_SHORT).show();
        isRunning = false;
        csvfile = false;
    }

    private void setupProximityManager() {
        //Create proximity manager instance
        proximityManager = ProximityManagerFactory.create(getApplicationContext());

        //Configure proximity manager basic options
        proximityManager.configuration()
                //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanPeriod(ScanPeriod.RANGING)
                //Using BALANCED for best performance/battery ratio
                .scanMode(ScanMode.BALANCED);

        //Setting up iBeacon and Eddystone listeners
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Check if service is already active
        Toast.makeText(this, "Service is running.", Toast.LENGTH_SHORT).show();
        Log.i("BeaconService:","Dentro il servizio");
        if (isRunning) {
            Toast.makeText(this, "Service is already running.", Toast.LENGTH_SHORT).show();
            return START_STICKY;
        }
        startScanning();
        isRunning = true;
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
                devicesCount = 0;
                Toast.makeText(BeaconService.this, "Scanning service started.", Toast.LENGTH_SHORT).show();
            }
        });
        stopAfterDelay();
    }

    private void stopAfterDelay() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                proximityManager.disconnect();
                stopSelf();
            }
        }, TIMEOUT);
    }


    private void saveAsCSV (String device, String region, int devicesCount) throws IOException {
        String data = ""+device+","+region+","+String.valueOf(devicesCount)+"";
        Toast.makeText(this, "Computing CSV.", Toast.LENGTH_SHORT).show();
        //String csv = "beacon.csv";
        Writer output;
        output = new BufferedWriter(new FileWriter("beacon.txt", true));
        output.append(data);
        output.close();
        /*CSVWriter writer = new CSVWriter(new FileWriter(csv, true));

        String [] record =data.split(",");
        Log.d("BeaconService", data);
        writer.writeNext(record);

        writer.close();
        /*FileWriter csvWriter = new FileWriter("beacon.csv", true);
        csvWriter.append(device);
        csvWriter.append(",");
        csvWriter.append(region);
        csvWriter.append(",");
        csvWriter.append(String.valueOf(devicesCount));
        csvWriter.append(",");
        csvWriter.append(LocalDateTime.now().toString());
        csvWriter.append("\n");

        csvWriter.flush();
        csvWriter.close();*/
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i(TAG, "onIBeaconDiscovered: " + ibeacon.toString());
                onDeviceDiscovered(ibeacon, region);

            }
        };
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onDeviceDiscovered(RemoteBluetoothDevice device, IBeaconRegion region) {
        devicesCount++;
        try {
            saveAsCSV(device.toString(), region.toString(), devicesCount);
        } catch (IOException e) {
            Log.i("BeaconService", "Exception on CSV writing");
        }
        //Send a broadcast with discovered device
        Intent intent = new Intent();
        intent.setAction(ACTION_DEVICE_DISCOVERED);
        intent.putExtra(EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_DEVICES_COUNT, devicesCount);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (proximityManager != null) {
            proximityManager.disconnect();
            proximityManager = null;
        }
        Toast.makeText(BeaconService.this, "Scanning service stopped.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}
