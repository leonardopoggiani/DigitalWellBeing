package it.unipi.dii.digitalwellbeing;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;


public class ClassificationService extends IntentService {

    private String TAG = "ClassificationService";

    private final IBinder binder = new LocalBinder();
    // private ServiceCallbacks serviceCallbacks;

    private boolean status;
    private FeatureExtraction fe;
    private RandomForestClassifier rfc;

    private Intent intentResult;

    public ClassificationService() {
        super("ClassificationService");

    }

    //Class used for the client Binder
    public class LocalBinder extends Binder {
        ClassificationService getService() {
            return ClassificationService.this;
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Log.d(TAG, "Started");
        fe = new FeatureExtraction(this);
        rfc = new RandomForestClassifier(this);
        //intentResult = new Intent(this, HandActivityService.class);
        //intentResult.setAction("Classification_Result");
        status = false;
        return super.onStartCommand(intent, flags, startId);
    }

    //Performs the features extraction and the classification and sends an intent to the HandActivityService
    //with the result of the classification
    private void handleClassification() {
        status = fe.calculateFeatures();
        if (status) {
            double activity = rfc.classify();
            //The classifier can return 0.0 for "Others" activity, 1.0 for "Washing_Hands"
            // activity or -1.0 in case of errors.
            if (activity == 1.0) {
                Log.d(TAG, "WASHING_HANDS");
                intentResult.putExtra("activity_key", "WASHING_HANDS");
                //if (serviceCallbacks != null) {
                //    serviceCallbacks.setBackground("GREEN");
                //}
            } else if (activity == 0.0) {
                Log.d(TAG, "OTHERS");
                intentResult.putExtra("activity_key", "OTHERS");
                //if (serviceCallbacks != null) {
                //    serviceCallbacks.setBackground("RED");
                //}
            }
            startService(intentResult);
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent.getAction() != null && intent.getAction().compareTo("Classify") == 0) {
            int counter = intent.getIntExtra("counter", -1);
            if (counter != -1)
                handleClassification();
            else
                Log.d(TAG, "Counter value not correct");

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    //public void setCallbacks(ServiceCallbacks callbacks) {
    //    serviceCallbacks = callbacks;
    //}
}
