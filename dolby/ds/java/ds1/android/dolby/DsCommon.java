/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2013 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package android.dolby;

/**
 * DsCommon class provides the common code between the service
 * and the clients.
 */
public class DsCommon
{
    /**
     * The message that notifies the DS effect on/off change.
     */
    public static final int DS_STATUS_CHANGED_MSG = 1;
    /**
     * The message that notifies the profile selected.
     */
    public static final int PROFILE_SELECTED_MSG = 2;
    /**
     * The message that notifies the profile settings change.
     */
    public static final int PROFILE_SETTINGS_CHANGED_MSG = 3;
    /**
     * The message that notifies the profile name change.
     */
    public static final int PROFILE_NAME_CHANGED_MSG = 4;
    /**
     * The message that notifies the visualizer update.
     */
    public static final int VISUALIZER_UPDATED_MSG = 5;
    /**
     * The message that notifies the visualizer suspend.
     */
    public static final int VISUALIZER_SUSPENDED_MSG = 6;
    /**
     * The message that notifies the Eq settings change.
     */
    public static final int EQ_SETTINGS_CHANGED_MSG = 7;
    /**
     * The message that notifies the DS parameter change.
     */
    public static final int DS_PARAM_CHANGED_MSG = 8;
    /// add more messages here.


    /**
     * Name of profile array.
     */
    public static final String PROFILE_NAMES[] = {"Movie", "Music", "Game", "Voice", "Custom 1", "Custom 2"};

    /**
     * Name of preset array.
     */
    public static final String IEQ_PRESET_NAMES[] = {"Off", "Open", "Rich", "Focused", "Bright", "Balanced", "Warm"};

    /**
     * Name of profile array in XML file.
     */
    public static final String PROFILE_NAMES_XML[] = {"movie", "music", "game", "voice", "user1", "user2"};

    /**
     * Name of preset array in XML file.
     */
    public static final String IEQ_PRESET_NAMES_XML[] = {"ieq_off", "ieq_open", "ieq_rich", "ieq_focused", "ieq_bright", "ieq_balanced", "ieq_warm"};

    /**
     * Name of GEQ settings array in XML file.
     */
    public static final String GEQ_NAMES_XML[][] = {
                                        {"geq_movie_off", "geq_movie_open", "geq_movie_rich", "geq_movie_focused", "geq_movie_bright", "geq_movie_balanced", "geq_movie_warm"},
                                        {"geq_music_off", "geq_music_open", "geq_music_rich", "geq_music_focused", "geq_music_bright", "geq_music_balanced", "geq_music_warm"},
                                        {"geq_game_off", "geq_game_open", "geq_game_rich", "geq_game_focused", "geq_game_bright", "geq_game_balanced", "geq_game_warm"},
                                        {"geq_voice_off", "geq_voice_open", "geq_voice_rich", "geq_voice_focused", "geq_voice_bright", "geq_voice_balanced", "geq_voice_warm"},
                                        {"geq_user1_off", "geq_user1_open", "geq_user1_rich", "geq_user1_focused", "geq_user1_bright", "geq_user1_balanced", "geq_user1_warm"},
                                        {"geq_user2_off", "geq_user2_open", "geq_user2_rich", "geq_user2_focused", "geq_user2_bright", "geq_user2_balanced", "geq_user2_warm"},
                                     };

    /**
     * Information for the intents send from the widget .
     */
    public static final String INIT_ACTION = "com.dolby.ds.srvcmd.init";
    public static final String ONOFF_ACTION = "com.dolby.ds.srvcmd.toggleonoff";
    public static final String SELECTPROFILE_ACTION = "com.dolby.ds.srvcmd.select";
    public static final String LAUNCH_DOLBY_APP_ACTION = "com.dolby.ds.srvcmd.launchapp";
    public static final String CMDNAME = "cmd";
    public static final String CMDINIT = "init";
    public static final String CMDONOFF = "on off";

    public static final int CMDSELECTMOVIE = 0;
    public static final int CMDSELECTMUSIC = 1;
    public static final int CMDSELECTGAME = 2;
    public static final int CMDSELECTVOICE = 3;
    public static final int CMDSELECTPRESET1 = 4;
    public static final int CMDSELECTPRESET2 = 5;

    public static final String WIDGET_CLASS = "widget class";

    /**
     * Key codes for the widget.
     */
    public static final int CODE_DS_OFF = 0x10;
    public static final int CODE_DS_ON = 0x11;
    public static final int CODE_SET_PROFILE_0 = 0x20;
    public static final int CODE_SET_PROFILE_1 = 0x21;
    public static final int CODE_SET_PROFILE_2 = 0x22;
    public static final int CODE_SET_PROFILE_3 = 0x23;
    public static final int CODE_SET_PROFILE_4 = 0x24;
    public static final int CODE_SET_PROFILE_5 = 0x25;
    public static final int CODE_LAUNCH_APP = 0x30;

    /**
     * Internal Error codes.
     * 
     * Negative values indicate exceptions have or should be raised
     * Positive values are reserved for return values to applications
     */
    public static final int DS_NO_ERROR = DsConstants.DS_NO_ERROR;
    public static final int DS_INVALID_ARGUMENT = -1;
    public static final int DS_NOT_RUNNING = -2;
    public static final int DS_INVALID_STATE = -3;
    public static final int DS_OPERATION_NOT_PERMITTED = -4;
    public static final int DS_UNKNOWN_ERROR = -5;

    /**
     * Return codes for profile settings modifications.
     */
    public static final int DS_PROFILE_NOT_MODIFIED = 0;
    public static final int DS_PROFILE_SETTINGS_MODIFIED    = 1 << 0;
    public static final int DS_PROFILE_NAME_MODIFIED        = 1 << 1;
    public static final int DS_PROFILE_NAME_AND_SETTINGS_MODIFIED = DS_PROFILE_SETTINGS_MODIFIED | DS_PROFILE_NAME_MODIFIED;

}
