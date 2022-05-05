package it.unipi.dii.digitalwellbeing;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

public class BLEScanner {
    MainActivity ma;
    private BluetoothAdapter bluetoothAdapter; //reference
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning;  //true if the scanning is already started
    private Handler handler;
    private long scanPeriod;
    private int signalStrength;
    private DeviceListAdapter leDeviceListAdapter = new DeviceListAdapter();

    BLEScanner(MainActivity ma, long period, int signal) {
        this.ma = ma;
        this.scanPeriod = period;
        this.signalStrength = signal;
        final BluetoothManager bluetoothManager =
                (BluetoothManager) ma.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        handler = new Handler();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    private boolean isScanning() {
        return isScanning;
    }

    public void start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            //Utils.requestUserBluetooth(ma);
            ma.stopScan();
        } else {
            scanLeDevice();
        }
    }

    public void stop() {
        scanLeDevice();
    }

    private void scanLeDevice() {
        if (!isScanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    if (ActivityCompat.checkSelfPermission(ma, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, scanPeriod);

            isScanning = true;
            bluetoothLeScanner.stopScan(leScanCallback);
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    leDeviceListAdapter.addDevice(result.getDevice());
                    leDeviceListAdapter.notifyDataSetChanged();
                }
            };
}
