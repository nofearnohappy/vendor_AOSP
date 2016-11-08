package com.mediatek.voiceextension.common;

/**
 * The interface manage common information for all voice feature.
 *
 */
public interface ISetHandler {

    /**
     * Creates a command set for commands operation.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param keyName
     *            command set name
     * @return result
     */
    int createSet(int pid, int uid, String keyName);

    /**
     * Deletes the command set.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param keyName
     *            command set name
     * @return result
     */
    int deleteSet(int pid, int uid, String keyName);

    /**
     * Selects a command set.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param setName
     *            command set name
     * @return result
     */
    int selectSet(int pid, int uid, String setName);

    /**
     * Checks if the command set already exists.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @param setName
     *            command set name
     * @return result
     */
    int isSetCreated(int pid, int uid, String setName);

    /**
     * Gets the current selected command set.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @return current selected command set
     */
    String getSetSelected(int pid, int uid);

    /**
     * Gets all command sets.
     *
     * @param pid
     *            process id
     * @param uid
     *            user id
     * @return all command sets
     */
    String[] getAllSets(int pid, int uid);

}
