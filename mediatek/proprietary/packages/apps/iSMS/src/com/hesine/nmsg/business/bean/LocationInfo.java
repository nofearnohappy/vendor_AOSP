package com.hesine.nmsg.business.bean;

public class LocationInfo {

    public static final int LOCATION_GPS = 1;
    public static final int LOCATION_NETWORK = 2;

    private int type = 2;
    private String province = null;
    private String city = null;
    private Coord coord = null;
    private String district = null;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Coord getCoord() {
        return coord;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

}
