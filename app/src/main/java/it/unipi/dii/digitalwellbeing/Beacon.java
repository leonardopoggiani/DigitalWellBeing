package it.unipi.dii.digitalwellbeing;

import com.kontakt.sdk.android.common.Proximity;


public class Beacon {
    private String key;
    private String address;
    private double distance;
    private String proximity;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private int rssi;
    private long timestamp;
    private String id;
    private String userDevice;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getProximity() {
        return proximity;
    }

    public void setProximity(String proximity) {
        this.proximity = proximity;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserDevice() {
        return userDevice;
    }

    public void setUserDevice(String userDevice) {
        this.userDevice = userDevice;
    }
}
