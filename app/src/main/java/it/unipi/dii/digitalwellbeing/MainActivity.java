package it.unipi.dii.digitalwellbeing;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sm;
    private Sensor s1;
    private Sensor s2;
    private Sensor s3;
    private static String TAG = "DigitalWellBeing";
    double ax,ay,az;   // these are the acceleration in x,y and z axis
    int already_recognized = 0;
    int pickup_events = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup sensors
        sensorSetup();
    }

    private void sensorSetup(){
        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        s1 = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        List<Sensor> sensor = sm.getSensorList(Sensor.TYPE_ALL);

        for(Sensor sens : sensor) {
            System.out.println("sensor: " + sens.getName());
        }
        
        if(s1 == null) {
            Log.d(TAG, "Sensor(s) unavailable");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, s1, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(this, s2, SensorManager.SENSOR_DELAY_GAME);
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

            if(ay > -10 && ay < -9.5) {
                Log.i(TAG, "ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                if(already_recognized == 0) {
                    pickup_events++;
                    already_recognized = 1;
                    TextView tv2 = (TextView) findViewById(R.id.pickup);
                    tv2.setText(String.format("%d" , pickup_events));
                }
            } else if(ay > 9.3 && ay < 9.8) {
                Log.i(TAG, "ax: " + ax + ", ay: " + ay + ", az: " + az + "\n");
                if(already_recognized == 0) {
                    pickup_events++;
                    already_recognized = 1;
                    TextView tv2 = (TextView) findViewById(R.id.pickup);
                    tv2.setText(String.format("%d" , pickup_events));
                }
            } else {
                already_recognized = 0;
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "Accuracy changed");
    }
}