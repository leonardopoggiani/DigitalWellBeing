package it.unipi.dii.digitalwellbeing;

public class Configuration {

    static final Double DELTA = 0.03; //seconds

    protected enum axis {X,Y,Z,PITCH,ROLL}

    static final int SIGNAL_LENGTH = 8; //seconds
    static final int SAMPLING_RATE = 50; //seconds
    static final int FRAGMENT_LENGTH = 8; //seconds
    static final int WINDOW_SIZE = 4; //seconds

    // Range values for accelerometer {

    // smartphone leaning on the table
    static final double X_LOWER_BOUND_POCKET = -1.0;
    static final double X_UPPER_BOUND_POCKET = 1.0;
    static final double Y_LOWER_BOUND_POCKET = -1.0;
    static final double Y_UPPER_BOUND_POCKET = 1.0;
    static final double Z_LOWER_BOUND_POCKET = 9.0;
    static final double Z_UPPER_BOUND_POCKET = 11.0;

}