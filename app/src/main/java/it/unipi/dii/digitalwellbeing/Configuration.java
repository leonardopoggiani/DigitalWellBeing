package it.unipi.dii.digitalwellbeing;

public class Configuration {

    protected enum axis {X,Y,Z,PITCH,ROLL}

    static final int SIGNAL_LENGTH = 8; //seconds
    static final int SAMPLING_RATE = 50; //seconds
    static final int FRAGMENT_LENGTH = 8; //seconds
    static final int WINDOW_SIZE = 4; //seconds

    // Range values for accelerometer {

    // smartphone leaning on the table
    static final double X_LOWER_BOUND_TABLE = -1.0;
    static final double X_UPPER_BOUND_TABLE = 1.0;
    static final double Y_LOWER_BOUND_TABLE = -1.0;
    static final double Y_UPPER_BOUND_TABLE = 1.0;
    static final double Z_LOWER_BOUND_TABLE = 9.0;
    static final double Z_UPPER_BOUND_TABLE = 11.0;

    // smartphone hand held by the user
    static final double X_LOWER_BOUND_HANDHELD = -1.0;
    static final double X_UPPER_BOUND_HANDHELD = 2.0;
    static final double Y_LOWER_BOUND_HANDHELD = 3.0;
    static final double Y_UPPER_BOUND_HANDHELD = 10.0;
    static final double Z_LOWER_BOUND_HANDHELD = 2.0;
    static final double Z_UPPER_BOUND_HANDHELD = 10.0;

    // smartphone up in the user's pocket
    static final double X_LOWER_BOUND_UPWARDS = -3.0;
    static final double X_UPPER_BOUND_UPWARDS = 2.0;
    static final double Y_LOWER_BOUND_UPWARDS = 8.0;
    static final double Y_UPPER_BOUND_UPWARDS = 11.0;
    static final double Z_LOWER_BOUND_UPWARDS = -1.5;
    static final double Z_UPPER_BOUND_UPWARDS = 3.0;

    // smartphone down in the user's pocket
    static final double X_LOWER_BOUND_DOWNWARDS = -2.0;
    static final double X_UPPER_BOUND_DOWNWARDS = 4.0;
    static final double Y_LOWER_BOUND_DOWNWARDS = -8.0;
    static final double Y_UPPER_BOUND_DOWNWARDS = -11.0;
    static final double Z_LOWER_BOUND_DOWNWARDS = -1.5;
    static final double Z_UPPER_BOUND_DOWNWARDS = 3.0;

}