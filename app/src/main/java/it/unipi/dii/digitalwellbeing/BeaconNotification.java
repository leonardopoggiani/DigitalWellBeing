package it.unipi.dii.digitalwellbeing;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

public class BeaconNotification extends Application implements BootstrapNotifier {
    public static BeaconManager beaconManager;
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    public static Region region;
    @Override
    public void onCreate(){
        super.onCreate();
        //Getting bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //Checking if bluetooth is supported by device or not
        if(bluetoothAdapter == null){
            Utils.toast(getApplicationContext(), "Bluetooth not supported");
        }else {
            if(!bluetoothAdapter.isEnabled()){
                Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                bluetoothIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivities(new Intent[]{bluetoothIntent});

            }
        }
        //Getting beaconManager
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        //Scanning period
        beaconManager.setForegroundScanPeriod(1100l);
        beaconManager.setForegroundBetweenScanPeriod(0l);
        //Allows Android to use BLE Scanning
        beaconManager.setAndroidLScanningDisabled(true);
        //Duration in milliseconds spent not scanning
        // when no ranging/monitoring clients are in the foreground
        beaconManager.setBackgroundBetweenScanPeriod(01);
        //Sets the duration in milliseconds of each Bluetooth LE scan cycle to look for beacons.
        beaconManager.setBackgroundScanPeriod(1100l);
        try {
            //Updates an already running scan
            beaconManager.updateScanPeriods();
        } catch (Exception e) {
        }
        // wake up the app when a beacon is seen
        region = new Region("backgroundRegion",
                null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%.
        backgroundPowerSaver = new BackgroundPowerSaver(this);
    }

    @Override
    public void didEnterRegion(Region region) {
        // Starting the BeaconService class that extends Service
        try{
        Intent i = new   Intent(getApplicationContext(), BeaconService.class);
        startService(i);
        } catch (Exception e){
        Log.i("BeaconNotification", "didEnterRegionError");
    }
    }

    @Override
    public void didExitRegion(Region region) {
        try {
            // Starting the BeaconService class that extends Service
            Intent k = new Intent(getApplicationContext(),BeaconService.class);
            startService(k);
        }
        catch (Exception e) {
            Log.i("BeaconNotification", "didExitRegionError");
        }

    }

    /*
      This override method will Determine the state for the device , whether device is in range
      of beacon or not , if yes then i = 1 and if no then i = 0
     */
    @Override
    public void didDetermineStateForRegion(int i, Region region) {

        try {
            // Starting the BeaconService class that extends Service
            Intent k = new Intent(getApplicationContext(), BeaconService.class);
            startService(k);
        }
        catch (Exception e) {
            Log.i("BeaconNotification", "didExitRegionError");
        }
    }


}
