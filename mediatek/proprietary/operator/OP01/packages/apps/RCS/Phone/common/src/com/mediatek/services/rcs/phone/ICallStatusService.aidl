package com.mediatek.services.rcs.phone;
import com.mediatek.services.rcs.phone.IServiceMessageCallback;
import com.mediatek.services.rcs.phone.IServicePresenterCallback;
interface ICallStatusService {
    void notifyToClient(String name, String status, String time);
    void registerMessageCallback(IServiceMessageCallback callback);  
    void unregisterMessageCallback(IServiceMessageCallback callback);
    void registerPresenterCallback(IServicePresenterCallback callback);      
    void unregisterPresenterCallback(IServicePresenterCallback callback);
}