package it.unipi.dii.digitalwellbeing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor proximity;
    public static final int REQUEST_CODE_PERMISSIONS = 100;

    private static String TAG = "DigitalWellBeing";

    boolean monitoring = false;
    boolean in_pocket = false;
    double ax,ay,az;   // these are the acceleration in x,y and z axis
    int already_recognized = 0;
    private File storagePath;
    File dataset;
    private FileWriter writerDataset;
    int id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.toast(getApplicationContext(), "BLE not supported");
            finish();
        }


        storagePath = getApplicationContext().getExternalFilesDir(null);
        Log.d(TAG, "[STORAGE_PATH]: " + storagePath);

        // Setup sensors
        sensorSetup();
    }

    private
    void sensorSetup(){
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(accelerometer == null || proximity == null) {
            Log.d(TAG, "Sensor(s) unavailable");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {

        if(monitoring) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                ax = event.values[0];
                ay = event.values[1];
                az = event.values[2];
                TextView tv = (TextView) findViewById(R.id.provaID);
                tv.setText(MessageFormat.format("ax: {0}, ay: {1}, az: {2}\n", ax, ay, az));

                if ( checkRangeDownwardsPocket(event) && in_pocket ) {
                    TextView tvLabel = (TextView) findViewById(R.id.Label);
                    tvLabel.setText("Downwards Trouser pocket");
                    Log.i(TAG, " Trouser pocket ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                    appendToCSV(id, ax, ay, az, event.timestamp, writerDataset, "POCKET_DOWNWARDS");

                    if (already_recognized == 0) {
                        already_recognized = 1;
                        Log.i(TAG, "Trouser pocket \n");
                    }
                } else if ( checkRangeUpwardsPocket(event) && in_pocket) {
                    TextView tvLabel = (TextView) findViewById(R.id.Label);
                    tvLabel.setText("Upwards Trouser pocket");
                    Log.i(TAG, " Trouser pocket ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                    appendToCSV(id, ax, ay, az, event.timestamp, writerDataset, "POCKET_UPWARDS");

                    if (already_recognized == 0) {
                        already_recognized = 1;
                        Log.i(TAG, "Trouser pocket \n");
                    }
                } else if ( checkRangeHandheld(event) && !in_pocket) {
                    TextView tvLabel = (TextView) findViewById(R.id.Label);
                    tvLabel.setText("Handheld");
                    Log.i(TAG, " Handheld ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                    appendToCSV(id, ax, ay, az, event.timestamp, writerDataset, "HANDHELD");

                } else if ( checkRangeTable(event) && !in_pocket) {
                    TextView tvLabel = (TextView) findViewById(R.id.Label);
                    tvLabel.setText("Table");
                    Log.i(TAG, " On the table ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                    appendToCSV(id, ax, ay, az, event.timestamp, writerDataset, "TABLE");

                } else {
                    appendToCSV(id, ax, ay, az, event.timestamp, writerDataset, "OTHER");

                    TextView tvLabel = (TextView) findViewById(R.id.Label);
                    tvLabel.setText("Other");
                    Log.i(TAG, " Not in trouser pocket ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                    already_recognized = 0;
                }
            } else if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                in_pocket = event.values[0] == 0;
            }
        }
    }

    private void appendToCSV(int id, double x, double y, double z, long timestamp, FileWriter writer, String tag) {
        StringBuilder sb = new StringBuilder();

        sb.append(id);
        sb.append(',');
        sb.append(x);
        sb.append(',');
        sb.append(y);
        sb.append(',');
        sb.append(z);
        sb.append(',');
        sb.append(timestamp);
        sb.append(',');
        sb.append(tag);
        sb.append('\n');

        this.id++;

        try {
            writer.append(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkRangeUpwardsPocket(SensorEvent event) {
        return (event.values[0] >= Configuration.X_LOWER_BOUND_UPWARDS && event.values[0] <= Configuration.X_UPPER_BOUND_UPWARDS) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_UPWARDS && event.values[1] <= Configuration.Y_UPPER_BOUND_UPWARDS) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_UPWARDS && event.values[2] <= Configuration.Z_UPPER_BOUND_UPWARDS);
    }

    public boolean checkRangeDownwardsPocket(SensorEvent event) {
        return (event.values[0] >= Configuration.X_LOWER_BOUND_DOWNWARDS && event.values[0] <= Configuration.X_UPPER_BOUND_DOWNWARDS) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_DOWNWARDS && event.values[1] <= Configuration.Y_UPPER_BOUND_DOWNWARDS) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_DOWNWARDS && event.values[2] <= Configuration.Z_UPPER_BOUND_DOWNWARDS);
    }

    public boolean checkRangeHandheld(SensorEvent event) {
        return (event.values[0] >= Configuration.X_LOWER_BOUND_HANDHELD && event.values[0] <= Configuration.X_UPPER_BOUND_HANDHELD) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_HANDHELD && event.values[1] <= Configuration.Y_UPPER_BOUND_HANDHELD) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_HANDHELD && event.values[2] <= Configuration.Z_UPPER_BOUND_HANDHELD);
    }

    public boolean checkRangeTable(SensorEvent event) {
        return (event.values[0] >= Configuration.X_LOWER_BOUND_TABLE && event.values[0] <= Configuration.X_UPPER_BOUND_TABLE) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_TABLE && event.values[1] <= Configuration.Y_UPPER_BOUND_TABLE) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_TABLE && event.values[2] <= Configuration.Z_UPPER_BOUND_TABLE);
    }

    public void startBeacon(View view){
        checkPermissions();
        startService(new Intent(this, BeaconService.class));
    }


    public void startMonitoring(View view) {

        if(!monitoring) {
            dataset = new File(storagePath, "dataset.csv");

            try {
                writerDataset = new FileWriter(dataset);

                StringBuilder sb = new StringBuilder();
                sb.append("id");
                sb.append(',');
                sb.append("ax");
                sb.append(',');
                sb.append("ay");
                sb.append(',');
                sb.append("az");
                sb.append(',');
                sb.append("timestamp");
                sb.append(',');
                sb.append("tag");
                sb.append('\n');

                writerDataset.append(sb);

            } catch (IOException e) {
                e.printStackTrace();
            }

            Button start_button = (Button)findViewById(R.id.start);
            start_button.setText("STOP");
            monitoring = true;

        } else {
            stopListener();
            Button stop_button = (Button)findViewById(R.id.start);
            stop_button.setText("START");

            FeatureExtraction fe = new FeatureExtraction(this);
            RandomForestClassifier rfc = new RandomForestClassifier(this);
            fe.calculateFeatures();
            double activity = rfc.classify();
            //The classifier can return 0.0 for "Others" activity, 1.0 for "Washing_Hands"
            // activity or -1.0 in case of errors.
            if (activity == 1.0) {
                Log.d(TAG, "WASHING_HANDS");
                //if (serviceCallbacks != null) {
                //    serviceCallbacks.setBackground("GREEN");
                //}
            } else if (activity == 0.0) {
                Log.d(TAG, "OTHERS");
                //if (serviceCallbacks != null) {
                //    serviceCallbacks.setBackground("RED");
                //}
            }
        }

    }

    private void stopListener() {
        if(sm != null)
            sm.unregisterListener(this);

        try {
            writerDataset.flush();
            writerDataset.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        monitoring = false;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed");
    }


    private void checkPermissions() {
        String[] requiredPermissions = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                ? new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}
                : new String[]{ android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION };
        if(isAnyOfPermissionsNotGranted(requiredPermissions)) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean isAnyOfPermissionsNotGranted(String[] requiredPermissions){
        for(String permission: requiredPermissions){
            int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, permission);
            if(PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (REQUEST_CODE_PERMISSIONS == requestCode) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location permissions are mandatory to use BLE features on Android 6.0 or higher", Toast.LENGTH_LONG).show();
        }
    }

}