package it.unipi.dii.digitalwellbeing;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class BLEDevice {
    private BluetoothDevice device;
    private int signal;

    public BLEDevice(BluetoothDevice bluetoothDevice) {
        this.device = bluetoothDevice;
    }

    public String getAddress() {
        return device.getAddress();
    }

    public String getName(MainActivity ma) {
        if (ActivityCompat.checkSelfPermission(ma, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return device.getName();
        }
        return device.getName();
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

    public int getSignal() {
        return signal;
    }
}
