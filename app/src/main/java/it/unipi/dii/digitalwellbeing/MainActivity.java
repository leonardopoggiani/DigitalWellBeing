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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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

    TreeMap<Long,Float[]> toBeClassified = new TreeMap<>();
    long timestamp;
    boolean already_recognized = true;

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
        sm.registerListener (this, accelerometer, 33330);
        sm.registerListener (this, gravity, 33330);
        sm.registerListener (this, gyroscope, 33330);
        sm.registerListener (this, rotation, 33330);
        sm.registerListener (this, linear, 33330);
        sm.registerListener (this, magnetometer, 33330);
        sm.registerListener (this, proximity, 33330);
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
                addMapValues(event, 0, 1, 2);
            } else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                addMapValues(event, 3, 4, 5);
            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                addMapValues(event, 6, 7, 8);
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                addMapValues(event, 9, 10, 11);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                addMapValues(event, 12, 13, 14);
            } else if (event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationAngles);

                event.values[0] = (float) Math.toDegrees(orientationAngles[0]);
                event.values[1] = (float) Math.toDegrees(orientationAngles[1]);
                event.values[2] = (float) Math.toDegrees(orientationAngles[2]);

                addMapValues(event, 15, 16, 17);
            } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                Log.d(TAG, "Proximity: " + event.values[0]);
                if(event.values[0] == 0.0 && checkRangePocket(event)) {
                    already_recognized = false;
                }
            }
        }
    }

    private void addMapValues(SensorEvent event, int i1, int i2, int i3) {
        boolean ret = false;

        // puó succedere che arrivino due valori di accelerometro consecutivi, si potrebbe fare quindi la media anziché scartare il valore
        // la media sarebbe sempre tra due campioni non molto distanti tra loro, accettabile come approssimazione?

        for(int i = i1; i <= i3 ; i++){
            if(toBeClassified.size() != 0 && !isFull()) {

                if(Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] != null) {
                    Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] =
                            (Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] + event.values[i % 3]) / 2;
                } else {
                    Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] = event.values[i % 3];
                }

                ret = true;
            }
        }

        if(!ret) {
            toBeClassified.put(event.timestamp, new Float[18]);

            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i1] = event.values[0];
            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i2] = event.values[1];
            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i3] = event.values[2];
        }

        // si puó prendere un campione ogni 10 (non abbiamo bisogno di tanti campioni per classificare)
        // oppure si puó pensare di aggregare questi campioni in qualche modo (media?)
        if(toBeClassified.size() >= 50) {
            long last_timestamp = toBeClassified.lastKey();
            Collection<Float[]> values = toBeClassified.values();
            Float[] toClassify = new Float[18];
            int[] count = new int[18];

            for(int i = 0; i < 18; i++) {
                for (Iterator<Float[]> it = values.iterator(); it.hasNext(); ) {
                    Float[] value = it.next();
                    if(value[i] == null) {
                        continue;
                    } else {
                        count[i]++;
                    }

                    if(toClassify[i] == null) {
                        toClassify[i] = value[i];
                    } else {
                        toClassify[i] = (toClassify[i] + value[i]);
                    }
                }
            }

            for(int i = 0; i < 18; i++) {
                toClassify[i] = toClassify[i] / count[i];
            }

            classifySamples(toClassify);

        }
    }

    private boolean isFull() {
        for(int i = 0; i < Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey())).length; i++) {
            if(Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] == null) {
                return false;
            }
        }

        return true;
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

        RadioButton pickup = (RadioButton) findViewById(R.id.pickup);
        RadioButton other = (RadioButton) findViewById(R.id.other);

        if(!monitoring) {
            if (pickup.isChecked()) {
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

            pickup.setChecked(false);
            other.setChecked(false);

            monitoring = false;
        }

    }

    private void classifySamples(Float[] toClassify) {
        // classify the samples
        TensorBuffer inputFeature0 = null;
        float[] data = new float[18];

        try {
            PickupClassifier model = PickupClassifier.newInstance(this);
            for (Map.Entry<Long, Float[]> entry : toBeClassified.entrySet()) {
                Log.d(TAG, "rowString length: " + (entry.getValue() != null ? entry.getValue().length : 0));

                int[] shape = new int[]{1, 18};
                TensorBuffer tensorBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32);

                for (int i = 0; i < toClassify.length; i++) {
                    data[i] = toClassify[i];
                }

                tensorBuffer.loadArray(data);

                inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 18, 1}, DataType.FLOAT32);
                ByteBuffer byteBuffer = tensorBuffer.getBuffer();
                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                PickupClassifier.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                data = outputFeature0.getFloatArray();

                // Releases model resources if no longer used.
                TextView tv = findViewById(R.id.activity);
                TextView tv2 = findViewById(R.id.counter);

                tv.setText(outputFeature0.getDataType().toString());
                if (data[0] > 0.5) {

                    tv.setText("Picking up phone!");
                    CharSequence counter = tv2.getText();
                    int count = Integer.parseInt(counter.toString());

                    if(!already_recognized)
                        count += 1;

                    tv2.setText(String.valueOf(count));
                    already_recognized = true;
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
            e.printStackTrace();
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