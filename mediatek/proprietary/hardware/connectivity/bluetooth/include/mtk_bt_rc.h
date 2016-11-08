/* Copyright Statement:
 * *
 * * This software/firmware and related documentation ("MediaTek Software") are
 * * protected under relevant copyright laws. The information contained herein
 * * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * * Without the prior written permission of MediaTek inc. and/or its licensors,
 * * any reproduction, modification, use or disclosure of MediaTek Software,
 * * and information contained herein, in whole or in part, shall be strictly prohibited.
 * *
 * * MediaTek Inc. (C) 2010. All rights reserved.
 * *
 * * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 * *
 * * The following software/firmware and/or related documentation ("MediaTek Software")
 * * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * * applicable license agreements with MediaTek Inc.
 * */

#ifndef MTK_INCLUDE_BT_RC_H
#define MTK_INCLUDE_BT_RC_H

__BEGIN_DECLS

/* Macros */
#define BTRC_MAX_ATTR_STR_LEN       255
#define BTRC_UID_SIZE               8
#define BTRC_MAX_APP_SETTINGS       8
#define BTRC_MAX_FOLDER_DEPTH       4
#define BTRC_MAX_APP_ATTR_SIZE      16
#define BTRC_MAX_ELEM_ATTR_SIZE     7
#define BTRC_FEATURE_MASK_SIZE      16

/* Media player major player type bit map */
#define BTRC_MJ_TYPE_AUDIO  0x01  /* Audio */
#define BTRC_MJ_TYPE_VIDEO  0x02  /* Video */
#define BTRC_MJ_TYPE_BC_AUDIO 0x04  /* Broadcasting Audio */
#define BTRC_MJ_TYPE_BC_VIDEO 0x08  /* Broadcasting Video */

/* Player sub type bit map */
#define BTRC_SUB_TYPE_AUDIO_BOOK    0x00000001  /* Audio Book */
#define BTRC_SUB_TYPE_PODCAST       0x00000002  /* Podcast */

/* the frequently used character set ids */
#define BTRC_CHARSET_ID_ASCII                  ((UINT16) 0x0003) /* ASCII */
#define BTRC_CHARSET_ID_UTF8                   ((UINT16) 0x006a) /* UTF-8 */
#define BTRC_CHARSET_ID_UTF16                  ((UINT16) 0x03f7) /* 1015 */
#define BTRC_CHARSET_ID_UTF32                  ((UINT16) 0x03f9) /* 1017 */

/* Player feature bit mask */
#define BTRC_SELECT_MASK             (1 << 0)
#define BTRC_UP_MASK                 (1 << 1)
#define BTRC_DOWN_MASK               (1 << 2)
#define BTRC_LEFT_MASK               (1 << 3)
#define BTRC_RIGHT_MASK              (1 << 4)
#define BTRC_RIGHTUP_MASK            (1 << 5)
#define BTRC_RIGHTDOWN_MASK          (1 << 6)
#define BTRC_LEFTUP_MASK             (1 << 7)
#define BTRC_LEFTDOWN_MASK           (1 << 8)
#define BTRC_ROOT_MENU_MASK          (1 << 9)
#define BTRC_SETUP_MENU_MASK         (1 << 10)
#define BTRC_CONTENTS_MENU_MASK      (1 << 11)
#define BTRC_FAVORITE_MENU_MASK      (1 << 12)
#define BTRC_EXIT_MASK               (1 << 13)
#define BTRC_0_MASK                  (1 << 14)
#define BTRC_1_MASK                  (1 << 15)
#define BTRC_2_MASK                  (1 << 16)
#define BTRC_3_MASK                  (1 << 17)
#define BTRC_4_MASK                  (1 << 18)
#define BTRC_5_MASK                  (1 << 19)
#define BTRC_6_MASK                  (1 << 20)
#define BTRC_7_MASK                  (1 << 21)
#define BTRC_8_MASK                  (1 << 22)
#define BTRC_9_MASK                  (1 << 23)
#define BTRC_DOT_MASK                (1 << 24)
#define BTRC_ENTER_MASK              (1 << 25)
#define BTRC_CLEAR_MASK              (1 << 26)
#define BTRC_CHNL_UP_MASK            (1 << 27)
#define BTRC_CHNL_DOWN_MASK          (1 << 28)
#define BTRC_PREV_CHNL_MASK          (1 << 29)
#define BTRC_SOUND_SEL_MASK          (1 << 30)
#define BTRC_INPUT_SEL_MASK          (1 << 31)
#define BTRC_DISP_INFO_MASK          (1 << 32)
#define BTRC_HELP_MASK               (1 << 33)
#define BTRC_PAGE_UP_MASK            (1 << 34)
#define BTRC_PAGE_DOWN_MASK          (1 << 35)
#define BTRC_POWER_MASK              (1 << 36)
#define BTRC_VOL_UP_MASK             (1 << 37)
#define BTRC_VOL_DOWN_MASK           (1 << 38)
#define BTRC_MUTE_MASK               (1 << 39)
#define BTRC_PLAY_MASK               (1 << 40)
#define BTRC_STOP_MASK               (1 << 41)
#define BTRC_PAUSE_MASK              (1 << 42)
#define BTRC_RECORD_MASK             (1 << 43)
#define BTRC_REWIND_MASK             (1 << 44)
#define BTRC_FAST_FWD_MASK           (1 << 45)
#define BTRC_EJECT_MASK              (1 << 46)
#define BTRC_FORWARD_MASK            (1 << 47)
#define BTRC_BACKWARD_MASK           (1 << 48)
#define BTRC_ANGLE_MASK              (1 << 49)
#define BTRC_SUBPICTURE_MASK         (1 << 50)
#define BTRC_F1_MASK                 (1 << 51)
#define BTRC_F2_MASK                 (1 << 52)
#define BTRC_F3_MASK                 (1 << 53)
#define BTRC_F4_MASK                 (1 << 54)
#define BTRC_F5_MASK                 (1 << 55)
#define BTRC_VENDOR_MASK             (1 << 56)
#define BTRC_GROUP_NAVI_MASK         (1 << 57)
#define BTRC_ADV_CTRL_MASK           (1 << 58)
#define BTRC_BROWSE_MASK             (1 << 59)
#define BTRC_SEARCH_MASK             (1 << 60)
#define BTRC_ADD2NOWPLAY_MASK        (1 << 61)
#define BTRC_UID_UNIQUE_MASK         (1 << 62)
#define BTRC_BR_WH_ADDR_MASK         (1 << 63)
#define BTRC_SEARCH_WH_ADDR_MASK     (1 << 64)
#define BTRC_NOW_PLAY_MASK           (1 << 65)
#define BTRC_UID_PERSIST_MASK        (1 << 66)
#define BTRC_NUMBER_OF_ITEM_MASK     (1 << 67)
#define BTRC_COVER_ART_MASK          (1 << 68)

typedef uint8_t btrc_uid_t[BTRC_UID_SIZE];

typedef enum {
    BTRC_FEAT_NONE = 0x00,    /* AVRCP 1.0 */
    BTRC_FEAT_METADATA = 0x01,    /* AVRCP 1.3 */
    BTRC_FEAT_ABSOLUTE_VOLUME = 0x02,    /* Supports TG role and volume sync */
    BTRC_FEAT_BROWSE = 0x04,    /* AVRCP 1.4 and up, with Browsing support */
} btrc_remote_features_t;

typedef enum {
    BTRC_PLAYSTATE_STOPPED = 0x00,    /* Stopped */
    BTRC_PLAYSTATE_PLAYING = 0x01,    /* Playing */
    BTRC_PLAYSTATE_PAUSED = 0x02,    /* Paused  */
    BTRC_PLAYSTATE_FWD_SEEK = 0x03,    /* Fwd Seek*/
    BTRC_PLAYSTATE_REV_SEEK = 0x04,    /* Rev Seek*/
    BTRC_PLAYSTATE_ERROR = 0xFF,    /* Error   */
} btrc_play_status_t;

typedef enum {
    BTRC_EVT_PLAY_STATUS_CHANGED = 0x01,
    BTRC_EVT_TRACK_CHANGE = 0x02,
    BTRC_EVT_TRACK_REACHED_END = 0x03,
    BTRC_EVT_TRACK_REACHED_START = 0x04,
    BTRC_EVT_PLAY_POS_CHANGED = 0x05,
    BTRC_EVT_APP_SETTINGS_CHANGED = 0x08,
    BTRC_EVT_AVAL_PLAYERS_CHANGE = 0x0a,
    BTRC_EVT_ADDR_PLAYER_CHANGE = 0x0b,
} btrc_event_id_t;

typedef enum {
    BTRC_NOTIFICATION_TYPE_INTERIM = 0,
    BTRC_NOTIFICATION_TYPE_CHANGED = 1,
    BTRC_NOTIFICATION_TYPE_REJECTED = 2,
} btrc_notification_type_t;

typedef enum {
    BTRC_PLAYER_ATTR_EQUALIZER = 0x01,
    BTRC_PLAYER_ATTR_REPEAT = 0x02,
    BTRC_PLAYER_ATTR_SHUFFLE = 0x03,
    BTRC_PLAYER_ATTR_SCAN = 0x04,
} btrc_player_attr_t;

typedef enum {
    BTRC_MEDIA_ATTR_TITLE = 0x01,
    BTRC_MEDIA_ATTR_ARTIST = 0x02,
    BTRC_MEDIA_ATTR_ALBUM = 0x03,
    BTRC_MEDIA_ATTR_TRACK_NUM = 0x04,
    BTRC_MEDIA_ATTR_NUM_TRACKS = 0x05,
    BTRC_MEDIA_ATTR_GENRE = 0x06,
    BTRC_MEDIA_ATTR_PLAYING_TIME = 0x07,
} btrc_media_attr_t;

typedef enum {
    BTRC_PLAYER_VAL_OFF_REPEAT = 0x01,
    BTRC_PLAYER_VAL_SINGLE_REPEAT = 0x02,
    BTRC_PLAYER_VAL_ALL_REPEAT = 0x03,
    BTRC_PLAYER_VAL_GROUP_REPEAT = 0x04
} btrc_player_repeat_val_t;

typedef enum {
    BTRC_PLAYER_VAL_OFF_SHUFFLE = 0x01,
    BTRC_PLAYER_VAL_ALL_SHUFFLE = 0x02,
    BTRC_PLAYER_VAL_GROUP_SHUFFLE = 0x03
} btrc_player_shuffle_val_t;

typedef enum {
    BTRC_STS_BAD_CMD        = 0x00, /* Invalid command */
    BTRC_STS_BAD_PARAM      = 0x01, /* Invalid parameter */
    BTRC_STS_NOT_FOUND      = 0x02, /* Specified parameter is wrong or not found */
    BTRC_STS_INTERNAL_ERR   = 0x03, /* Internal Error */
    BTRC_STS_NO_ERROR       = 0x04, /* Operation Success */
    BTRC_STS_BAD_PLAYER_ID  = 0x11, /* Invalid player id */
    BTRC_STS_PLAYER_N_ADDR  = 0x13, /* player is not addressed */
    BTRC_STS_NO_AVAL_PLAYER = 0x15, /* No available player */
    BTRC_STS_ADDR_PLAYER_CHG = 0x16, /* Addressed player is changed */
} btrc_status_t;

typedef enum {
    BTRC_SCOPE_PLAYER_LIST = 0x00,  /* Media Player Item - Contains all available media players */
    BTRC_SCOPE_FILE_SYSTEM = 0x01,  /* Folder Item, Media Element Item - The virtual filesystem containing the media content of the browsed player */
    BTRC_SCOPE_SEARCH      = 0x02,  /* Media Element Item  The results of a search operation on the browsed player */
    BTRC_SCOPE_NOW_PLAYING = 0x03,  /* Media Element Item  The Now Playing list (or queue) of the addressed player */
}btrc_browsable_scope_t;

typedef enum {
    BTRC_ITEM_PLAYER =          1,
    BTRC_ITEM_FOLDER =          2,
    BTRC_ITEM_MEDIA  =          3
}btrc_browsable_item_type_t;

typedef struct {
    uint8_t num_attr;
    uint8_t attr_ids[BTRC_MAX_APP_SETTINGS];
    uint8_t attr_values[BTRC_MAX_APP_SETTINGS];
} btrc_player_settings_t;

typedef struct {
    uint16_t player_id;
    uint16_t uid_counter;
} btrc_addr_player_t;

typedef union
{
    btrc_play_status_t play_status;
    btrc_uid_t track; /* queue position in NowPlaying */
    uint32_t song_pos;
    btrc_player_settings_t player_setting;
    btrc_addr_player_t addr_player;
} btrc_register_notification_t;

typedef struct {
    uint8_t id; /* can be attr_id or value_id */
    uint8_t text[BTRC_MAX_ATTR_STR_LEN];
} btrc_player_setting_text_t;

typedef struct {
    uint32_t attr_id;
    uint8_t text[BTRC_MAX_ATTR_STR_LEN];
} btrc_element_attr_val_t;

typedef struct {
    uint16_t charset_id;
    uint16_t str_len;
    uint8_t  *p_str;
} btrc_full_name_t;

typedef struct {
    uint16_t player_id;
    uint8_t major_type;
    uint32_t sub_type;
    btrc_play_status_t play_status;
    uint8_t features[BTRC_FEATURE_MASK_SIZE];
    btrc_full_name_t full_name;
} btrc_media_player_item_t;

typedef struct
{
    btrc_browsable_scope_t scope;
    uint32_t start_item;
    uint32_t end_item;
    uint8_t attr_count;
    btrc_media_attr_t attribute[BTRC_MAX_ELEM_ATTR_SIZE];
} btrc_get_folder_item_cmd_t;

/* only support media player item now */
typedef struct
{
    btrc_browsable_item_type_t   item_type; /* AVRC_ITEM_PLAYER, AVRC_ITEM_FOLDER, or AVRC_ITEM_MEDIA */
    union
    {
        btrc_media_player_item_t player;     /* The properties of a media player item.*/
    } u;
} btrc_browsable_item_t;

typedef struct
{
    btrc_status_t rsp_status;
    uint16_t uid_counter;
    uint16_t item_num;
    btrc_browsable_item_t *item_list;
} btrc_get_folder_item_rsp_t;

/** Callback for the controller's supported feautres */
typedef void (* btrc_remote_features_callback)(bt_bdaddr_t *bd_addr,
                                                      btrc_remote_features_t features);

/** Callback for play status request */
typedef void (* btrc_get_play_status_callback)();

/** Callback for list player application attributes (Shuffle, Repeat,...) */
typedef void (* btrc_list_player_app_attr_callback)();

/** Callback for list player application attributes (Shuffle, Repeat,...) */
typedef void (* btrc_list_player_app_values_callback)(btrc_player_attr_t attr_id);

/** Callback for getting the current player application settings value
**  num_attr: specifies the number of attribute ids contained in p_attrs
*/
typedef void (* btrc_get_player_app_value_callback) (uint8_t num_attr, btrc_player_attr_t *p_attrs);

/** Callback for getting the player application settings attributes' text
**  num_attr: specifies the number of attribute ids contained in p_attrs
*/
typedef void (* btrc_get_player_app_attrs_text_callback) (uint8_t num_attr, btrc_player_attr_t *p_attrs);

/** Callback for getting the player application settings values' text
**  num_attr: specifies the number of value ids contained in p_vals
*/
typedef void (* btrc_get_player_app_values_text_callback) (uint8_t attr_id, uint8_t num_val, uint8_t *p_vals);

/** Callback for setting the player application settings values */
typedef void (* btrc_set_player_app_value_callback) (btrc_player_settings_t *p_vals);

/** Callback to fetch the get element attributes of the current song
**  num_attr: specifies the number of attributes requested in p_attrs
*/
typedef void (* btrc_get_element_attr_callback) (uint8_t num_attr, btrc_media_attr_t *p_attrs);

/** Callback for register notification (Play state change/track change/...)
**  param: Is only valid if event_id is BTRC_EVT_PLAY_POS_CHANGED
*/
typedef void (* btrc_register_notification_callback) (btrc_event_id_t event_id, uint32_t param);

/* AVRCP 1.4 Enhancements */
/** Callback for volume change on CT
**  volume: Current volume setting on the CT (0-127)
*/
typedef void (* btrc_volume_change_callback) (uint8_t volume, uint8_t ctype);

/** Callback for passthrough commands */
typedef void (* btrc_passthrough_cmd_callback) (int id, int key_state);

/** Callback for set addressed player commands */
typedef void (* btrc_set_addressed_player_callback) (int player_id);

/** Callback for get folder items commands */
typedef void (* btrc_get_folder_items_callback) (btrc_get_folder_item_cmd_t *p_getfolder);

/** Callback for get total number of items commands - only support media player scope now*/
typedef void (* btrc_get_total_items_num_callback) (btrc_browsable_scope_t scope);

/** BT-RC Target callback structure. */
typedef struct {
    /** set to sizeof(BtRcCallbacks) */
    size_t      size;
    btrc_remote_features_callback               remote_features_cb;
    btrc_get_play_status_callback               get_play_status_cb;
    btrc_list_player_app_attr_callback          list_player_app_attr_cb;
    btrc_list_player_app_values_callback        list_player_app_values_cb;
    btrc_get_player_app_value_callback          get_player_app_value_cb;
    btrc_get_player_app_attrs_text_callback     get_player_app_attrs_text_cb;
    btrc_get_player_app_values_text_callback    get_player_app_values_text_cb;
    btrc_set_player_app_value_callback          set_player_app_value_cb;
    btrc_get_element_attr_callback              get_element_attr_cb;
    btrc_register_notification_callback         register_notification_cb;
    btrc_volume_change_callback                 volume_change_cb;
    btrc_passthrough_cmd_callback               passthrough_cmd_cb;
    btrc_set_addressed_player_callback          set_addressed_player_cb;
    btrc_get_folder_items_callback              get_folder_items_cb;
    btrc_get_total_items_num_callback           get_total_items_num_cb;
} btrc_callbacks_t;

/** Represents the standard BT-RC AVRCP Target interface. */
typedef struct {

    /** set to sizeof(BtRcInterface) */
    size_t          size;
    /**
     * Register the BtRc callbacks
     */
    bt_status_t (*init)( btrc_callbacks_t* callbacks );

    /** Respose to GetPlayStatus request. Contains the current
    **  1. Play status
    **  2. Song duration/length
    **  3. Song position
    */
    bt_status_t (*get_play_status_rsp)( btrc_play_status_t play_status, uint32_t song_len, uint32_t song_pos);

    /** Lists the support player application attributes (Shuffle/Repeat/...)
    **  num_attr: Specifies the number of attributes contained in the pointer p_attrs
    */
    bt_status_t (*list_player_app_attr_rsp)( int num_attr, btrc_player_attr_t *p_attrs);

    /** Lists the support player application attributes (Shuffle Off/On/Group)
    **  num_val: Specifies the number of values contained in the pointer p_vals
    */
    bt_status_t (*list_player_app_value_rsp)( int num_val, uint8_t *p_vals);

    /** Returns the current application attribute values for each of the specified attr_id */
    bt_status_t (*get_player_app_value_rsp)( btrc_player_settings_t *p_vals);

    /** Returns the application attributes text ("Shuffle"/"Repeat"/...)
    **  num_attr: Specifies the number of attributes' text contained in the pointer p_attrs
    */
    bt_status_t (*get_player_app_attr_text_rsp)( int num_attr, btrc_player_setting_text_t *p_attrs);

    /** Returns the application attributes text ("Shuffle"/"Repeat"/...)
    **  num_attr: Specifies the number of attribute values' text contained in the pointer p_vals
    */
    bt_status_t (*get_player_app_value_text_rsp)( int num_val, btrc_player_setting_text_t *p_vals);

    /** Returns the current songs' element attributes text ("Title"/"Album"/"Artist")
    **  num_attr: Specifies the number of attributes' text contained in the pointer p_attrs
    */
    bt_status_t (*get_element_attr_rsp)( uint8_t num_attr, btrc_element_attr_val_t *p_attrs);

    /** Response to set player attribute request ("Shuffle"/"Repeat")
    **  rsp_status: Status of setting the player attributes for the current media player
    */
    bt_status_t (*set_player_app_value_rsp)(btrc_status_t rsp_status);

    /* Response to the register notification request (Play state change/track change/...).
    ** event_id: Refers to the event_id this notification change corresponds too
    ** type: Response type - interim/changed
    ** p_params: Based on the event_id, this parameter should be populated
    */
    bt_status_t (*register_notification_rsp)(btrc_event_id_t event_id,
                                             btrc_notification_type_t type,
                                             btrc_register_notification_t *p_param);

    /* AVRCP 1.4 enhancements */

    /**Send current volume setting to remote side. Support limited to SetAbsoluteVolume
    ** This can be enhanced to support Relative Volume (AVRCP 1.0).
    ** With RelateVolume, we will send VOLUME_UP/VOLUME_DOWN opposed to absolute volume level
    ** volume: Should be in the range 0-127. bit7 is reseved and cannot be set
    */
    bt_status_t (*set_volume)(uint8_t volume);

    /** Closes the interface. */
    void  (*cleanup)( void );

    /** Response to the set address player command
    ** rsp_status:  If the specified Player Id does not refer to a valid player, return status BTRC_STS_INVALID_PLAYER_ID; otherwise, BTRC_STS_NO_ERROR
    **/
    bt_status_t (*set_addressed_player_rsp)(btrc_status_t rsp_status);

    /** Resupose to get folder item command
      ** now it only support media player list scope
      **/
    bt_status_t (*get_folder_item_rsp)(btrc_get_folder_item_rsp_t *p_item);

	/** Resupose to get total number of items command
      ** now it only support media player list scope
      **/
    bt_status_t (*get_total_items_num_rsp)(btrc_status_t rsp_status, uint16_t uid_counter, uint32_t num);
} btrc_interface_t;


typedef void (* btrc_passthrough_rsp_callback) (int id, int key_state);

typedef void (* btrc_connection_state_callback) (bool state, bt_bdaddr_t *bd_addr);

/** BT-RC Controller callback structure. */
typedef struct {
    /** set to sizeof(BtRcCallbacks) */
    size_t      size;
    btrc_passthrough_rsp_callback               passthrough_rsp_cb;
    btrc_connection_state_callback              connection_state_cb;
} btrc_ctrl_callbacks_t;

/** Represents the standard BT-RC AVRCP Controller interface. */
typedef struct {

    /** set to sizeof(BtRcInterface) */
    size_t          size;
    /**
     * Register the BtRc callbacks
     */
    bt_status_t (*init)( btrc_ctrl_callbacks_t* callbacks );

    /** send pass through command to target */
    bt_status_t (*send_pass_through_cmd) ( bt_bdaddr_t *bd_addr, uint8_t key_code, uint8_t key_state );

    /** Closes the interface. */
    void  (*cleanup)( void );
} btrc_ctrl_interface_t;

__END_DECLS

#endif /* MTK_INCLUDE_BT_RC_H */
