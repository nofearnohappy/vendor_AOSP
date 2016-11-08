package com.mediatek.datatransfer.modules;

public class CallLogsData {
    public static final String BEGIN_VCALL = "BEGIN:VCALL";
    public static final String VCL_END_OF_LINE = "\r\n";
    public static final String END_VCALL = "END:VCALL";
    public static final String ID = "ID:";
    public static final String NEW = "NEW:";
    public static final String NAME = "NAME:";
    public static final String NMUBER_TYPE = "NMUBER_TYPE:";

    public static final String SIMID = "SLOT:";
    public static final String TYPE = "TYPE:";
    public static final String DATE = "DATE:";
    public static final String NUMBER = "NUMBER:";
    public static final String DURATION = "DURATION:";

    int id = 0;
    int new_Type = 0;
    String name = "";
    int number_type = 0;
    /*****************************CT spec*************************************/
    long simid = 0;
    int type = 0;
    long date = 0;
    String number = "";
    long duration = 0;
    /*****************************CT spec*************************************/
    public CallLogsData() {
    }

    @Override
    public String toString() {
        return "CallLogsData [id=" + id + ", new_Type=" + new_Type + ", type="
                + type + ", name=" + name + ", date=" + date + ", number="
                + number + ", duration=" + duration + ", number_type="
                + number_type + ", simid=" + simid + "]";
    }
}