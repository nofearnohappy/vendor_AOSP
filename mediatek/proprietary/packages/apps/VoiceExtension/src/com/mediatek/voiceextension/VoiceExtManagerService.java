package com.mediatek.voiceextension;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.ServiceManager;

import com.mediatek.voiceextension.command.CommandStub;
import com.mediatek.voiceextension.swip.SwipAdapter;

/**
 * Manage application information and notify swip event to application.
 *
 */
public class VoiceExtManagerService extends Service {

    private CommandStub mCommandStub;
    private SwipAdapter mSwipHandler;

    /**
     * VoiceExtManagerService constructor.
     */
    public VoiceExtManagerService() {
        super();
        mCommandStub = new CommandStub();
        ServiceManager.addService("vie_command", mCommandStub);
    }

    @Override
    public void onCreate() {
        // Init the voice command feature
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}
