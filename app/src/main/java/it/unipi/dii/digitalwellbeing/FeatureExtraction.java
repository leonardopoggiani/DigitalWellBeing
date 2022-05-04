package it.unipi.dii.digitalwellbeing;


import android.content.Context;
import android.util.Log;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class FeatureExtraction {

    private static final String TAG = "FeatureExtraction";
    private Context ctx;

    private Mean mn;
    private Variance var;
    private StandardDeviation stDv;
    private Kurtosis kurtosis;
    private Skewness skewness;

    private File featureFile;
    private FileWriter featureFileWriter;

    HashMap<Integer, String[]> rowMap = new HashMap<Integer, String[]>();
    HashMap<Integer, Double> timeLastMap = new HashMap<Integer, Double>();

    public FeatureExtraction(Context ctx) {
        this.ctx = ctx;
        mn = new Mean();
        var = new Variance();
        stDv = new StandardDeviation();
        kurtosis = new Kurtosis();
        skewness = new Skewness();

        featureFile = new File(ctx.getExternalFilesDir(null), "unlabeledData.arff");
        try {
            // The file doesn't exists -> The header of the arff file has to be created
            featureFileWriter = new FileWriter(featureFile);
            headerBuild(featureFileWriter);
        }catch(IOException e) {
            e.printStackTrace();
            try {
                featureFileWriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    //Create the header of the ARFF file used from weka module with all the attributes extracted
    private void headerBuild(FileWriter file) throws IOException{
        file.write("@RELATION trainingSet \n \n");

        for (int i = 1; i <= Configuration.FRAGMENT_LENGTH/ Configuration.WINDOW_SIZE; i++) {
            file.append("@ATTRIBUTE AccX_win" + i + "_mean REAL\n@ATTRIBUTE AccX_win" + i + "_stDv REAL\n@ATTRIBUTE AccX_win" + i + "_kurtosis REAL\n");
            file.append("@ATTRIBUTE AccX_win" + i + "_skewness REAL\n");
        }
        for (int i = 1; i <= Configuration.FRAGMENT_LENGTH/ Configuration.WINDOW_SIZE; i++) {
            file.append("@ATTRIBUTE AccY_win" + i + "_mean REAL\n@ATTRIBUTE AccY_win" + i + "_stDv REAL\n@ATTRIBUTE AccY_win" + i + "_kurtosis REAL\n");
            file.append("@ATTRIBUTE AccY_win" + i + "_skewness REAL\n");
        }
        for (int i = 1; i <= Configuration.FRAGMENT_LENGTH/ Configuration.WINDOW_SIZE; i++){
            file.append("@ATTRIBUTE AccZ_win" + i + "_mean REAL\n@ATTRIBUTE AccZ_win" + i + "_stDv REAL\n@ATTRIBUTE AccZ_win" + i + "_kurtosis REAL\n");
            file.append("@ATTRIBUTE AccZ_win" + i + "_skewness REAL\n");
        }
        file.append("@ATTRIBUTE class {Others, Pickup}\n" + "\n" + "@DATA\n");
        file.flush();
    }

    public void extractFeatures() throws CsvValidationException, IOException {
        CSVReader reader = new CSVReader(new FileReader(new File(ctx.getExternalFilesDir(null), "dataset_accelerometer.csv")));
        String[] lineInArray;
        int biggestID = 0;
        while ((lineInArray = reader.readNext()) != null) {
            if (lineInArray[0].compareTo("id") != 0) {
                rowMap.put(Integer.parseInt(lineInArray[0]), lineInArray);
                biggestID = Integer.parseInt(lineInArray[0]);
            }
        }

        timeLastMap.put(0, Double.parseDouble(Objects.requireNonNull(rowMap.get(biggestID))[4]) / 1000000000);

        for (int i = 0; i < Configuration.SIGNAL_LENGTH / Configuration.FRAGMENT_LENGTH; i++) {

            double[] x_axis = new double[Configuration.FRAGMENT_LENGTH * Configuration.SAMPLING_RATE];
            double[] y_axis = new double[Configuration.FRAGMENT_LENGTH * Configuration.SAMPLING_RATE];
            double[] z_axis = new double[Configuration.FRAGMENT_LENGTH * Configuration.SAMPLING_RATE];

            int count = 0;
            String[] row = rowMap.get(0);
            for (; count < Configuration.FRAGMENT_LENGTH * Configuration.SAMPLING_RATE && row != null; count++) {
                row = rowMap.get(count);
                assert row != null;
                x_axis[count] = Double.parseDouble(row[1]);
                y_axis[count] = Double.parseDouble(row[2]);
                z_axis[count] = Double.parseDouble(row[3]);
            }

            computeFeature(x_axis, 0, Configuration.axis.X);
            computeFeature(y_axis, 0, Configuration.axis.Y);
            computeFeature(z_axis, 0, Configuration.axis.Z);
        }

        featureFileWriter.append("Others" + "\n");
        featureFileWriter.flush();
        featureFileWriter.close();
    }

    public void computeFeature(double[] data, int key, Configuration.axis axis) throws IOException {
        computeMeanDevStd(data, key, axis);
    }

    private void computeMeanDevStd(double[] data, int key, Configuration.axis ax) throws IOException{
        for (int i = 0;
             i < Configuration.FRAGMENT_LENGTH * Configuration.SAMPLING_RATE && i < data.length;
             i += (Configuration.WINDOW_SIZE * Configuration.SAMPLING_RATE)) {

            double mean = mn.evaluate(data, i, Configuration.WINDOW_SIZE * Configuration.SAMPLING_RATE);
            featureFileWriter.append(mean + ",");
            featureFileWriter.append(stDv.evaluate(data, mean, i, Configuration.WINDOW_SIZE * Configuration.SAMPLING_RATE) + ",");
            computeSkewness(data, i);
            computeKurtosis(data, i);
        }
    }

    private void computeSkewness(double[] data, int i) throws IOException{
        double skew = skewness.evaluate(data, i, Configuration.WINDOW_SIZE * Configuration.SAMPLING_RATE);
        if(Double.isNaN(skew))
            featureFileWriter.append(0.0 + ",");
        else
            featureFileWriter.append(skew + ",");
    }

    private void computeKurtosis(double[] data, int i) throws IOException{
        double kurt = kurtosis.evaluate(data, i, Configuration.WINDOW_SIZE * Configuration.SAMPLING_RATE);
        if(Double.isNaN(kurt))
            featureFileWriter.append(-2.041 + ",");
        else
            featureFileWriter.append(kurt + ",");
    }

}


