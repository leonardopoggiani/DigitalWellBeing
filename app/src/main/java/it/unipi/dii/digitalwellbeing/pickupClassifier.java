package it.unipi.dii.digitalwellbeing;

import android.hardware.SensorEvent;
import android.util.Log;
import android.widget.TextView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import android.content.Context;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import it.unipi.dii.digitalwellbeing.ml.PickupClassifier;

public class pickupClassifier {

    private static final String TAG = "PickupClassifier";
    TreeMap<Long,Float[]> toBeClassified = new TreeMap<>();
    long timestamp;
    boolean already_recognized = false;
    private Context ctx;


    private void classifySamples() {
        // classify the samples
        TensorBuffer inputFeature0 = null;
        float[] data = new float[12];

        try {
            PickupClassifier model = PickupClassifier.newInstance(ctx.getApplicationContext());
            for (Map.Entry<Long, Float[]> entry : toBeClassified.entrySet()) {
                Log.d(TAG, "rowString length: " + (entry.getValue() != null ? entry.getValue().length : 0));

                int[] shape = new int[]{1, 12};
                TensorBuffer tensorBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32);

                for (int i = 0; i < entry.getValue().length; i++) {
                    data[i] = entry.getValue()[i];
                }

                tensorBuffer.loadArray(data);

                inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 12, 1}, DataType.FLOAT32);
                ByteBuffer byteBuffer = tensorBuffer.getBuffer();
                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                PickupClassifier.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                data = outputFeature0.getFloatArray();

                /*
                TextView tv = findViewById(R.id.activity);
                TextView tv2 = findViewById(R.id.counter);
                 */

                // tv.setText(outputFeature0.getDataType().toString());
                if (data[0] <= 0.5) {
                    // tv.setText("Picking up phone!");
                    // CharSequence counter = tv2.getText();
                    // int count = Integer.parseInt(counter.toString());
                    // count += 1;
                    // tv2.setText(String.valueOf(count));
                    already_recognized = true;
                } else {
                    // tv.setText("Other activities");
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

    private void addMapValues(SensorEvent event, int i1, int i2, int i3) {
        boolean ret = false;

        // puó succedere che arrivino due valori di accelerometro consecutivi, si potrebbe fare quindi la media anziché scartare il valore
        // la media sarebbe sempre tra due campioni non molto distanti tra loro, accettabile come approssimazione?

        for(int i = i1; i <= i3 ; i++){
            if(toBeClassified.size() != 0 && !isFull()) {

                if(Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] != null) {
                    Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] =
                            (Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] + event.values[i % 3]) / 2;
                    Log.d(TAG, "Campione duplicato faccio la MEDIA");
                } else {
                    Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] = event.values[i % 3];
                }

                ret = true;
            }
        }

        if(!ret) {
            toBeClassified.put(event.timestamp, new Float[12]);

            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i1] = event.values[0];
            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i2] = event.values[1];
            Objects.requireNonNull(toBeClassified.get(event.timestamp))[i3] = event.values[2];
        }

        // si puó prendere un campione ogni 10 (non abbiamo bisogno di tanti campioni per classificare)
        // oppure si puó pensare di aggregare questi campioni in qualche modo (media?)
        if(toBeClassified.size() >= 40) {
            classifySamples();
        }

        /*
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

        if(toBeClassified.size() >= 10) {
            classifyFiftySamples();
        }

         */
    }

    private boolean isFull() {
        for(int i = 0; i < Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey())).length; i++) {
            if(Objects.requireNonNull(toBeClassified.get(toBeClassified.lastKey()))[i] == null) {
                return false;
            }
        }

        return true;
    }
}
