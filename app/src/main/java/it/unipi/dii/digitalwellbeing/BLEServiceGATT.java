package it.unipi.dii.digitalwellbeing;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class BLEServiceGATT extends Service {
    private Binder binder = new LocalBinder();

    public static final String TAG = "BluetoothLeService";

    private BluetoothAdapter bluetoothAdapter;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class LocalBinder extends Binder {
        public BLEServiceGATT getService() {
            return BLEServiceGATT.this;
        }
    }

    public boolean initialize() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }
}
