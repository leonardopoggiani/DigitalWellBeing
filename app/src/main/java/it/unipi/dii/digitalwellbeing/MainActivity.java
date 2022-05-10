package it.unipi.dii.digitalwellbeing;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.opencsv.exceptions.CsvValidationException;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import it.unipi.dii.digitalwellbeing.ml.PickupClassifier;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor accelerometer;
    private Sensor proximity;
    private Sensor gyroscope;
    private Sensor gravity;
    private Sensor rotation;
    private Sensor linear;
    private Sensor magnetometer;

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
    private File mag;

    private FileWriter writerAcc;
    private FileWriter writerGyr;
    private FileWriter writerRot;
    private FileWriter writerGrav;
    private FileWriter writerLin;
    private FileWriter writerMag;

    final float[] rotationMatrix = new float[9];
    final float[] orientationAngles = new float[3];

    protected Interpreter tflite;

    TreeMap<Long, List<Float>> toBeClassified = new TreeMap<>();
    long timestamp;

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
        mag = new File(storagePath, "SensorData_Mag_"+counter+".csv");

        try {
            writerAcc = new FileWriter(accel);
            writerGyr = new FileWriter(gyr);
            writerRot = new FileWriter(rot);
            writerGrav = new FileWriter(grav);
            writerLin = new FileWriter(linearAcc);
            writerMag = new FileWriter(mag);
        } catch (IOException e) {
            e.printStackTrace();
            //FileWriter creation could be failed so the rate must be reset on low frequency rate
            Log.d(TAG,"Some writer is failed");
            stopListener();
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
        magnetometer = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

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
        sm.registerListener (this, accelerometer, 10000000, 10000000);
        sm.registerListener (this, gravity, 10000000, 10000000);
        sm.registerListener (this, gyroscope, 10000000, 10000000);
        sm.registerListener (this, rotation, 10000000, 10000000);
        sm.registerListener (this, linear, 10000000, 10000000);
        sm.registerListener (this, magnetometer, 10000000, 10000000);

        /*
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, gravity, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, linear, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
         */
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
            } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerGyr);
            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerLin);
            } else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);
                String temp = (Math.toDegrees(orientationAngles[0])) + "," + (Math.toDegrees(orientationAngles[1])) + "," + (Math.toDegrees(orientationAngles[2])) + "," + event.timestamp + ","  + activity_tag + ",\n";
                appendToCSV(temp, writerRot);
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerGrav);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                String temp = event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.timestamp + "," + activity_tag + ",\n";
                appendToCSV(temp, writerMag);
            }
        } else {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                if(toBeClassified.get(event.timestamp) == null) {
                    List<Float>valuesList = new ArrayList<Float>() {
                        {
                            add(event.values[0]);
                            add(event.values[1]);
                            add(event.values[2]);
                        }
                    };
                    if(toBeClassified.size() == 0 || toBeClassified.get(toBeClassified.lastKey()).size() == 12) {
                        timestamp = event.timestamp;
                        toBeClassified.put(event.timestamp, valuesList);
                    } else {
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[0]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[1]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[2]);
                    }
                } else {
                    toBeClassified.get(event.timestamp).add(event.values[0]);
                    toBeClassified.get(event.timestamp).add(event.values[1]);
                    toBeClassified.get(event.timestamp).add(event.values[2]);
                }

                if(toBeClassified.size() >= 2) {
                    classifyFiftySamples();
                }
            } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if(toBeClassified.get(event.timestamp) == null) {
                    List<Float>valuesList = new ArrayList<Float>() {
                        {
                            add(event.values[0]);
                            add(event.values[1]);
                            add(event.values[2]);
                        }
                    };
                    if(toBeClassified.size() == 0 || toBeClassified.get(toBeClassified.lastKey()).size() == 12) {
                        timestamp = event.timestamp;
                        toBeClassified.put(event.timestamp, valuesList);
                    } else {
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[0]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[1]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[2]);
                    }
                } else {
                    toBeClassified.get(event.timestamp).add(event.values[0]);
                    toBeClassified.get(event.timestamp).add(event.values[1]);
                    toBeClassified.get(event.timestamp).add(event.values[2]);
                }

                if(toBeClassified.size() >= 2) {
                    classifyFiftySamples();
                }

            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                if(toBeClassified.get(event.timestamp) == null) {
                    List<Float>valuesList = new ArrayList<Float>() {
                        {
                            add(event.values[0]);
                            add(event.values[1]);
                            add(event.values[2]);
                        }
                    };
                    if(toBeClassified.size() == 0 || toBeClassified.get(toBeClassified.lastKey()).size() == 12) {
                        timestamp = event.timestamp;
                        toBeClassified.put(event.timestamp, valuesList);
                    } else {
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[0]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[1]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[2]);
                    }
                } else {
                    toBeClassified.get(event.timestamp).add(event.values[0]);
                    toBeClassified.get(event.timestamp).add(event.values[1]);
                    toBeClassified.get(event.timestamp).add(event.values[2]);
                }

                if(toBeClassified.size() >= 2) {
                    classifyFiftySamples();
                }
            } /*else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                if(toBeClassified.get(event.timestamp) == null) {
                    List<Float>valuesList = new ArrayList<Float>() {
                        {
                            add((float) Math.toDegrees(orientationAngles[0]));
                            add((float) Math.toDegrees(orientationAngles[1]));
                            add((float) Math.toDegrees(orientationAngles[2]));
                        }
                    };
                    timestamp = event.timestamp;
                    toBeClassified.put(event.timestamp, valuesList);
                } else {
                    toBeClassified.get(event.timestamp).add(event.values[0]);
                    toBeClassified.get(event.timestamp).add(event.values[1]);
                    toBeClassified.get(event.timestamp).add(event.values[2]);
                }

                if(toBeClassified.size() >= 50 && isToBeClassifiedComplete()) {
                    classifyFiftySamples();
                }
            }*/ else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                if(toBeClassified.get(event.timestamp) == null) {
                    List<Float>valuesList = new ArrayList<Float>() {
                        {
                            add(event.values[0]);
                            add(event.values[1]);
                            add(event.values[2]);
                        }
                    };
                    if(toBeClassified.size() == 0 || toBeClassified.get(toBeClassified.lastKey()).size() == 12) {
                        timestamp = event.timestamp;
                        toBeClassified.put(event.timestamp, valuesList);
                    } else {
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[0]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[1]);
                        toBeClassified.get(toBeClassified.lastKey()).add(event.values[2]);
                    }
                } else {
                    toBeClassified.get(event.timestamp).add(event.values[0]);
                    toBeClassified.get(event.timestamp).add(event.values[1]);
                    toBeClassified.get(event.timestamp).add(event.values[2]);
                }

                if(toBeClassified.size() >= 2) {
                    classifyFiftySamples();
                }

                for(Map.Entry<Long, List<Float>> entry : toBeClassified.entrySet()) {
                    Log.d(TAG, String.valueOf(entry.getKey()) + ": " + entry.getValue().toString());
                }

                Log.d(TAG, "_-_-_-_-__-_-_-_-__-_-_-_-__-_-_-_-__-_-_-_-__-_-_-_-__-_-_-_-__-_-_-_-_");

            } /*else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                if(toBeClassified.get(event.timestamp) == null) {
                    List<Float>valuesList = new ArrayList<Float>() {
                        {
                            add(event.values[0]);
                            add(event.values[1]);
                            add(event.values[2]);
                        }
                    };
                    timestamp = event.timestamp;
                    toBeClassified.put(event.timestamp, valuesList);
                } else {
                    toBeClassified.get(event.timestamp).add(event.values[0]);
                    toBeClassified.get(event.timestamp).add(event.values[1]);
                    toBeClassified.get(event.timestamp).add(event.values[2]);
                }

                for(Map.Entry<Long, List<Float>> entry : toBeClassified.entrySet()) {
                    Log.d(TAG, String.valueOf(entry.getKey()) + ": " + entry.getValue().toString());
                }

                if(toBeClassified.size() >= 50 && isToBeClassifiedComplete()) {
                    classifyFiftySamples();
                }
            }*/
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

            // monitoring = true;
            Button start_button = (Button) findViewById(R.id.start);
            start_button.setText("STOP");
        } else {
            Button stop_button = (Button)findViewById(R.id.start);
            stop_button.setText("START");

            putdown.setChecked(false);
            pickup.setChecked(false);
            other.setChecked(false);

            monitoring = false;
        }

    }

    private void classifyFiftySamples() {
        // classify the samples
        TensorBuffer inputFeature0 = null;
        float[] data = new float[12];

        try {
            PickupClassifier model = PickupClassifier.newInstance(this);
            for (Map.Entry<Long, List<Float>> entry : toBeClassified.entrySet()) {
                List<Float> floatList = entry.getValue();
                Log.d(TAG, "rowString length: " + (floatList != null ? floatList.size() : 0));
                if(floatList.size() != 12) {
                    continue;
                }

                int[] shape = new int[]{1, 12};
                TensorBuffer tensorBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32);

                for (int i = 0; i < floatList.size(); i++) {
                    data[i] = floatList.get(i);
                }

                tensorBuffer.loadArray(data);

                inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 12, 1}, DataType.FLOAT32);
                ByteBuffer byteBuffer = tensorBuffer.getBuffer();
                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                PickupClassifier.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                data = outputFeature0.getFloatArray();

                // Releases model resources if no longer used.
                TextView tv = findViewById(R.id.activity);

                tv.setText(outputFeature0.getDataType().toString());
                if (data[0] <= 0.5) {
                    tv.setText("Picking up phone!");
                } else {
                    tv.setText("Other activities");
                }

                Log.d(TAG, "predictActivities: output array: " + Arrays.toString(outputFeature0.getFloatArray()));
                break;
            }
            toBeClassified.clear();
            // Releases model resources if no longer used.
            model.close();

        } catch (IOException e) {
            // TODO Handle the exception
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
            writerMag.flush();
            writerMag.close();
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