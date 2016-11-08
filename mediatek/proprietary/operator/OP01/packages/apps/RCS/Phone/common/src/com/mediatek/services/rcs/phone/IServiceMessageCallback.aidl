package com.mediatek.services.rcs.phone;
interface IServiceMessageCallback {
    void updateMsgStatus(String name, String status, String time);
    void stopfromClient();
}