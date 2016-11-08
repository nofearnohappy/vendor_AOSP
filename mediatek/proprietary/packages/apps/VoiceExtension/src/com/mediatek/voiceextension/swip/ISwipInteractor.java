package com.mediatek.voiceextension.swip;

/**
 * The SwipAdapter interface.
 *
 */
public interface ISwipInteractor {

    /**
     * Checks if swip is ready.
     *
     * @return true if ready, otherwise false
     */
    public boolean isSwipReady();

    /**
     * Swip creates a command set for commands operation.
     *
     * @param name
     *            swip command set name
     * @param featureType
     *            feature type
     * @return result
     */
    public int createSetName(String name, int featureType);

    /**
     * Swip deletes the command set.
     *
     * @param name
     *            swip command set name
     * @return {@link VoiceCommandResult#SUCCESS},
     *         {@link VoiceCommandResult#COMMANDSET_OCCUPIED},
     *         {@link VoiceCommandResult#FAILURE}
     */
    public int deleteSetName(String name);

    /**
     * Swip checks if the command set already exists.
     *
     * @param name
     *            swip command set name
     * @param featureType
     *            feature type
     * @return result
     */
    public int isSetCreated(String name, int featureType);

    /**
     * Gets all command sets from swip.
     *
     * @param processName
     *            swip process name
     * @param featureType
     *            feature type
     * @return all command sets from swip
     */
    public String[] getAllSets(String processName, int featureType);

    /**
     * Swip start command recognition.
     *
     * @param setName
     *            swip command set name
     * @param featureType
     *            feature type
     */
    public void startRecognition(String setName, int featureType);

    /**
     * Swip stop command recognition.
     *
     * @param setName
     *            swip command set name
     * @param featureType
     *            feature type
     */
    public void stopRecognition(String setName, int featureType);

    /**
     * Swip pause command recognition.
     *
     * @param setName
     *            swip command set name
     * @param featureType
     *            feature type
     */
    public void pauseRecognition(String setName, int featureType);

    /**
     * Swip resume command recognition.
     *
     * @param setName
     *            swip command set name
     * @param featureType
     *            feature type
     */
    public void resumeRecognition(String setName, int featureType);

    /**
     * Swip sets up the commands list.
     *
     * @param setName
     *            swip command set name
     * @param commands
     *            commands list
     */
    public void setCommands(String setName, String[] commands);

    /**
     * Swip sets up the commands data.
     *
     * @param setName
     *            swip command set name
     * @param data
     *            commands data
     * @param end
     *            whether the data is end
     */
    public void setCommands(String setName, byte[] data, boolean end);

    /**
     * Gets the commands list from swip.
     *
     * @param setName
     *            swip command set name
     * @return the commands list
     */
    public String[] getCommands(String setName);

    /**
     * Register callback that receive asynchronous notification from swip.
     *
     * @param featureType
     *            feture type
     * @param callback
     *            a callback that receive asynchronous notification from swip
     */
    public void registerCallback(int featureType, ISwipCallback callback);

}
