package it.unipi.dii.digitalwellbeing;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor accelerometer;
    private static String TAG = "DigitalWellBeing";

    double ax,ay,az;   // these are the acceleration in x,y and z axis
    int already_recognized = 0;
    private File storagePath;
    File table;
    File other;
    File trouser_pocket_downwards;
    File trouser_pocket_upwards;
    File handheld;
    private FileWriter writerTable;
    private FileWriter writerOther;
    private FileWriter writerTrouserPocketDownwards;
    private FileWriter writerTrouserPocketUpwards;
    private FileWriter writerHandheld;
    int id_trouser_pocket_downwards = 0;
    int id_trouser_pocket_upwards = 0;
    int id_handheld = 0;
    int id_table = 0;
    int id_other = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storagePath = getApplicationContext().getExternalFilesDir(null);
        Log.d(TAG, "[STORAGE_PATH]: " + storagePath);

        table = new File(storagePath, "table.csv");
        other = new File(storagePath, "other.csv");
        handheld = new File(storagePath, "handheld.csv");
        trouser_pocket_downwards = new File(storagePath, "trouser_pocket_downwards.csv");
        trouser_pocket_upwards = new File(storagePath, "trouser_pocket_upwards.csv");

        try {
            writerTable = new FileWriter(table);
            writerOther = new FileWriter(other);
            writerHandheld = new FileWriter(handheld);
            writerTrouserPocketUpwards = new FileWriter(trouser_pocket_upwards);
            writerTrouserPocketDownwards = new FileWriter(trouser_pocket_downwards);

            StringBuilder sb = new StringBuilder();
            sb.append("id");
            sb.append(',');
            sb.append("ax");
            sb.append(',');
            sb.append("ay");
            sb.append(',');
            sb.append("az");
            sb.append('\n');

            writerTable.append(sb);
            writerOther.append(sb);
            writerHandheld.append(sb);
            writerTrouserPocketDownwards.append(sb);
            writerTrouserPocketUpwards.append(sb);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Setup sensors
        sensorSetup();
    }

    private
    void sensorSetup(){
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(accelerometer == null) {
            Log.d(TAG, "Sensor(s) unavailable");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];

            if( (ay > -11.0 && ay < -8.0)  && (ax < 5.0 && ax > -1.0) && (az < 4.0 && az > -0.5) ){
                Log.i(TAG, " Trouser pocket ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");

                try {
                    StringBuilder sb = new StringBuilder();

                    sb.append(id_trouser_pocket_downwards);
                    sb.append(',');
                    sb.append(ax);
                    sb.append(',');
                    sb.append(ay);
                    sb.append(',');
                    sb.append(az);
                    sb.append('\n');

                    id_trouser_pocket_downwards++;
                    writerTrouserPocketDownwards.append(sb.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                    //FileWriter creation could be failed so the rate must be reset on low frequency rate
                    Log.d(TAG,"Some writer is failed");
                    sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                }

                if(already_recognized == 0) {
                    already_recognized = 1;
                    Log.i(TAG, "Trouser pocket \n");
                }
            } else if( (ay > 9.0 && ay < 10.5) && (ax < 3.0 && ax > 0.0) && (az < 1.0 && az > -2.0 ) ){
                Log.i(TAG, " Trouser pocket ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");

                try {
                    StringBuilder sb = new StringBuilder();

                    sb.append(id_trouser_pocket_upwards);
                    sb.append(',');
                    sb.append(ax);
                    sb.append(',');
                    sb.append(ay);
                    sb.append(',');
                    sb.append(az);
                    sb.append('\n');

                    id_trouser_pocket_upwards++;
                    writerTrouserPocketUpwards.append(sb.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                    //FileWriter creation could be failed so the rate must be reset on low frequency rate
                    Log.d(TAG,"Some writer is failed");
                    sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                }

                if(already_recognized == 0) {
                    already_recognized = 1;
                    Log.i(TAG, "Trouser pocket \n");
                }
            } else if ( (ay > 3.0 && ay < 10.0) && (ax < 2.0 && ax > -1.0) && (az < 10.0 && az > 2.0 ) ){
                Log.i(TAG, " Handheld ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");

                try {
                    StringBuilder sb = new StringBuilder();

                    sb.append(id_handheld);
                    sb.append(',');
                    sb.append(ax);
                    sb.append(',');
                    sb.append(ay);
                    sb.append(',');
                    sb.append(az);
                    sb.append('\n');

                    id_handheld++;
                    writerHandheld.append(sb.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                    //FileWriter creation could be failed so the rate must be reset on low frequency rate
                    Log.d(TAG,"Some writer is failed");
                    sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                }

            } else if ( (ay > -1.0 && ay < 1.0) && (ax < 1.0 && ax > -1.0) && (az < 11.0 && az > 9.0 ) ){
                Log.i(TAG, " On the table ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");

                try {
                    StringBuilder sb = new StringBuilder();

                    sb.append(id_table);
                    sb.append(',');
                    sb.append(ax);
                    sb.append(',');
                    sb.append(ay);
                    sb.append(',');
                    sb.append(az);
                    sb.append('\n');

                    id_table++;
                    writerTable.append(sb.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                    //FileWriter creation could be failed so the rate must be reset on low frequency rate
                    Log.d(TAG,"Some writer is failed");
                    sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                }

            } else {
                try {
                    StringBuilder sb = new StringBuilder();

                    sb.append(id_other);
                    sb.append(',');
                    sb.append(ax);
                    sb.append(',');
                    sb.append(ay);
                    sb.append(',');
                    sb.append(az);
                    sb.append('\n');

                    id_other++;
                    writerOther.append(sb.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                    //FileWriter creation could be failed so the rate must be reset on low frequency rate
                    Log.d(TAG,"Some writer is failed");
                    sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                }

                Log.i(TAG, " Not in trouser pocket ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                already_recognized = 0;
            }
        }
    }

    //Check if accelerometer axis data are in the range of values related to a possible hands washing action
    public boolean isInRange(SensorEvent event) {
        if((event.values[0] >= Configuration.X_LOWER_BOUND && event.values[0] <= Configuration.X_UPPER_BOUND) &&
                (event.values[1] >= Configuration.Y_LOWER_BOUND && event.values[1] <= Configuration.Y_UPPER_BOUND) &&
                (event.values[2] >= Configuration.Z_LOWER_BOUND && event.values[2] <= Configuration.Z_UPPER_BOUND)) {
            Log.d(TAG, "ACC_X: "+event.values[0]+", ACC_Y: "+event.values[1]+", ACC_Z: "+event.values[2]+", TIMESTAMP: "+event.timestamp);
            return true;
        }
        else  return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed");
    }

}