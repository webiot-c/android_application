package com.webiot_c.cprss_notifi_recv;

import java.util.Random;

public class AEDInformation {

    private String aed_id;
    private double lat;
    private double lon;

    private long unique_id;

    public AEDInformation(String aed_id, double latitude, double longitude){
        this.aed_id = aed_id;
        this.lat = latitude;
        this.lon = longitude;
        unique_id = new Random().nextLong();
    }

    public String getAed_id() {
        return aed_id;
    }

    public double getLatitude() {
        return lat;
    }

    public void setLatitude(int lat) {
        this.lat = lat;
    }

    public double getLongitude() {
        return lon;
    }

    public void setLongitude(int lon) {
        this.lon = lon;
    }

    public void setLocation(int latitude, int longitude){
        this.lat = latitude;
        this.lon = longitude;
    }

    public long getUniqueID(){
        return unique_id;
    }


}
