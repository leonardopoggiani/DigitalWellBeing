package it.unipi.dii.digitalwellbeing;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;

public class RandomForestClassifier {

    private Classifier randomForest;
    private Context ctx;

    public RandomForestClassifier(Context ctx) {
        this.ctx = ctx;
        this.randomForest = new RandomForest();
        try {
            randomForest = (Classifier) SerializationHelper.read(ctx.getAssets().open("pocketClassifier.model"));
        } catch (Exception e) {
            Log.d("ERROR", "not initialized");
            e.printStackTrace();
        }
    }

    public double classify(){
        try {

            Instances unlabeled = new Instances(
                    new BufferedReader(
                            new FileReader(ctx.getExternalFilesDir(null) + "/dataset_accelerometer.arff")));

            // set class attribute
            unlabeled.setClassIndex(unlabeled.numAttributes() - 1);

            // create copy
            Instances labeled = new Instances(unlabeled);

            // label instances
            for (int i = 0; i < unlabeled.numInstances(); i++) {
                double clsLabel = randomForest.classifyInstance(unlabeled.instance(i));
                labeled.instance(i).setClassValue(clsLabel);
            }
            // save labeled data
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(ctx.getExternalFilesDir(null) + "/dataset_accelerometer_classified.csv"));
            writer.write(labeled.toString());
            writer.newLine();
            writer.flush();
            writer.close();

            // Label instances. Return 0.0 if the activity is "Others" or 1.0 if the activity is "Washing_Hands"
            return 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1.0;
        }

    }

}