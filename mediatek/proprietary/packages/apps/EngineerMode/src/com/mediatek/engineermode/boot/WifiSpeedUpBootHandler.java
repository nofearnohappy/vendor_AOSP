package com.mediatek.engineermode.boot;

import com.mediatek.engineermode.wifi.EmPerformanceWrapper;

/**
 * wifi speed up boot handler.
 * @author: mtk81238
 */
public class WifiSpeedUpBootHandler implements IBootServiceHandler {

    @Override
    public int handleStartRequest(EmBootStartService service) {
        EmPerformanceWrapper.initialize(service, true);
        return HANDLE_DONE;
    }

}
