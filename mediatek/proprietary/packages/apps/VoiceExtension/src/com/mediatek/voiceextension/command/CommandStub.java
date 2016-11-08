package com.mediatek.voiceextension.command;

import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.mediatek.common.voiceextension.IVoiceExtCommandListener;
import com.mediatek.common.voiceextension.IVoiceExtCommandManager;

/**
 * Implement IVoiceExtCommandManager entry point.
 *
 */
public class CommandStub extends IVoiceExtCommandManager.Stub {

    // private CommonManager mCommonMgr = CommonManager.getInstance();

    private CommandManager mCommandMgr;

    /**
     * CommandStub constructor.
     */
    public CommandStub() {
        mCommandMgr = new CommandManager();
    }

    @Override
    public int registerListener(IVoiceExtCommandListener listener)
            throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.registerListener(pid, uid, listener);
    }

    @Override
    public int createCommandSet(String name) throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.createSet(pid, uid, name);
    }

    @Override
    public int deleteCommandSet(String name) throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.deleteSet(pid, uid, name);
    }

    @Override
    public int selectCurrentCommandSet(String name) throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.selectSet(pid, uid, name);
    }

    @Override
    public String getCommandSetSelected() throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.getSetSelected(pid, uid);
    }

    @Override
    public int isCommandSetCreated(String name) throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.isSetCreated(pid, uid, name);
    }

    @Override
    public String[] getCommandSets() throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.getAllSets(pid, uid);
    }

    @Override
    public String[] getCommands() throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        return mCommandMgr.getCommands(pid, uid);
    }

    @Override
    public void setCommandsFile(ParcelFileDescriptor fd, int offset, int length)
            throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        mCommandMgr.setCommands(pid, uid, fd, offset, length);
    }

    @Override
    public void setCommandsStrArray(String[] commands) throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        mCommandMgr.setCommands(pid, uid, commands);

    }

    @Override
    public void startRecognition() throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        mCommandMgr.startRecognition(pid, uid);

    }

    @Override
    public void stopRecognition() throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        mCommandMgr.stopRecognition(pid, uid);
    }

    @Override
    public void pauseRecognition() throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        mCommandMgr.pauseRecognition(pid, uid);
    }

    @Override
    public void resumeRecognition() throws RemoteException {
        // TODO Auto-generated method stub
        int pid = Binder.getCallingPid();
        int uid = Binder.getCallingUid();

        mCommandMgr.resumeRecognition(pid, uid);
    }

}
