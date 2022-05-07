package it.unipi.dii.digitalwellbeing;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor proximity;
    private Sensor gyroscope;
    private Sensor gravity;
    private Sensor rotation;
    private Sensor linear;

    private Context ctx;

    private static String TAG = "DigitalWellBeing";

    boolean monitoring = false;
    boolean in_pocket = false;
    private int counter;
    private File storagePath;
    String activity_tag = "";

    private File accel;
    private File gyr;
    private File rot;
    private File grav;
    private File linearAcc;

    private FileWriter writerAcc;
    private FileWriter writerGyr;
    private FileWriter writerRot;
    private FileWriter writerGrav;
    private FileWriter writerLin;

    final float[] rotationMatrix = new float[9];
    final float[] orientationAngles = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storagePath = getApplicationContext().getExternalFilesDir(null);
        Log.d(TAG, "[STORAGE_PATH]: " + storagePath);

        counter = 0;

        accel = new File(storagePath, "SensorData_Acc_"+counter+".csv");
        gyr = new File(storagePath, "SensorData_Gyr_"+counter+".csv");
        rot = new File(storagePath, "SensorData_Rot_"+counter+".csv");
        grav = new File(storagePath, "SensorData_Grav_"+counter+".csv");
        linearAcc = new File(storagePath, "SensorData_LinAcc_"+counter+".csv");

        try {
            writerAcc = new FileWriter(accel);
            writerGyr = new FileWriter(gyr);
            writerRot = new FileWriter(rot);
            writerGrav = new FileWriter(grav);
            writerLin = new FileWriter(linearAcc);
        } catch (IOException e) {
            e.printStackTrace();
            //FileWriter creation could be failed so the rate must be reset on low frequency rate
            Log.d(TAG,"Some writer is failed");
            stopListener();
            sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Setup sensors
        sensorSetup();
    }

    private
    void sensorSetup(){

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);

        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximity = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        gyroscope = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        rotation = sm.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        gravity = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        linear = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        //if(accelerometer == null || proximity == null || gyroscope == null
        //        || rotation == null || gravity == null || linear == null) {
        //    Log.d(TAG, "Sensor(s) unavailable");
        //    finish();
        //}

        while(true) {
            File counter_value = new File(storagePath + "/SensorData_Acc_" + counter + ".csv");
            if(!counter_value.exists()) {
                break;
            } else {
                counter++;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, proximity, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, gravity, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, linear, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(monitoring) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerAcc);
            } else if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                in_pocket = event.values[0] == 0;
            } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerGyr);
            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerLin);
            } else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);
                String temp = (Math.toDegrees(orientationAngles[1])) + "," + (Math.toDegrees(orientationAngles[2])) + "," + event.timestamp + ","  + activity_tag + ",\n";
                appendToCSV(temp, writerRot);
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerGrav);
            }
        }
    }

    private void appendToCSV(String temp, FileWriter writer) {
        try {
            writer.append(temp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkRangePocket(SensorEvent event) {
        return (event.values[0] >= Configuration.X_LOWER_BOUND_POCKET && event.values[0] <= Configuration.X_UPPER_BOUND_POCKET) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND_POCKET && event.values[1] <= Configuration.Y_UPPER_BOUND_POCKET) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND_POCKET && event.values[2] <= Configuration.Z_UPPER_BOUND_POCKET);
    }

    public void startMonitoring(View view) throws CsvValidationException, IOException {

        RadioButton putdown = (RadioButton) findViewById(R.id.putdown);
        RadioButton pickup = (RadioButton) findViewById(R.id.pickup);
        RadioButton other = (RadioButton) findViewById(R.id.other);

        if(!monitoring) {
            if (putdown.isChecked()) {
                activity_tag = "PUTDOWN";
            } else if (pickup.isChecked()) {
                activity_tag = "PICKUP";
            } else if (other.isChecked()) {
                activity_tag = "OTHER";
            }

            monitoring = true;
            Button start_button = (Button) findViewById(R.id.start);
            start_button.setText("STOP");

        } else {
            Button stop_button = (Button)findViewById(R.id.start);
            stop_button.setText("START");

            putdown.setChecked(false);
            pickup.setChecked(false);
            other.setChecked(false);

            monitoring = false;

            /* ****************

            FeatureExtraction fe = new FeatureExtraction(this);

            fe.calculateFeatures(0);

            while(true) {
                File counter_value = new File(storagePath + "/SensorData_Acc_" + counter + ".csv");
                if(!counter_value.exists()) {
                    break;
                } else {
                    fe.calculateFeatures(counter);
                    counter++;
                }
            }
             **************** */
        }

    }

    private void stopListener() {
        if(sm != null)
            sm.unregisterListener(this);

        try {
            writerAcc.flush();
            writerAcc.close();
            writerGyr.flush();
            writerGyr.close();
            writerRot.flush();
            writerRot.close();
            writerGrav.flush();
            writerGrav.close();
            writerLin.flush();
            writerLin.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        monitoring = false;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed");
    }

}