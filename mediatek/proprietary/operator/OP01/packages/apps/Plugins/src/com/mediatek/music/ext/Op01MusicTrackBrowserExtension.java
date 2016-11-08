/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.music.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.mediatek.common.PluginImpl;
import com.mediatek.op01.plugin.R;

/**
 * A class for customizing Music's behavior in OP01 project.
 *
 * We support clear playlist feature and add music to playlist here.
 */
@PluginImpl(interfaceName = "com.mediatek.music.ext.IMusicTrackBrowser")
public class Op01MusicTrackBrowserExtension extends DefaultMusicTrackBrowser {

    private static final String TAG = "Op01MusicTrackBrowserExtension";

    private static final int CLEAR_PLAYLIST = PluginUtils.MENU_ITEM_BASE_ID;
    private static final int ADD_SONG_TO_PLAYLIST = PluginUtils.MENU_ITEM_BASE_ID + 1;
    private static final int REQUEST_TO_ADD_SONG_TO_PLAYLIST
                             = PluginUtils.ACTIVITY_REQUEST_CODE_BASE_ID;
    private static final String SELECT_FILE_FROM_FILEMANAGER = "com.mediatek.filemanager.ADD_FILE";

    /**
     * Constructor.
     * @param context the context of the host.
     */
    public Op01MusicTrackBrowserExtension(Context context) {
        super(context);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, String activityName, Bundle options) {

        Log.d(TAG, "onCreateOptionsMenuForPlugin activityName" + activityName);
        if (activityName.equals(PluginUtils.TRACK_BROWSER_ACTIVITY)) {

            String playlistName = options.getString(PluginUtils.PLAYLIST_NAME);
            Log.d(TAG, "onCreateOptionsMenuForPlugin playlistName" + playlistName);
            if (playlistName != null &&
                !playlistName.equals(PluginUtils.RECENTLY_ADDED) &&
                !playlistName.equals(PluginUtils.PODCASTS)) {

                /* host app will add the clear playlist menu in now_playing list */
                if (!playlistName.equals(PluginUtils.NOW_PLAYING)) {

                    CharSequence title = getString(R.string.clear_playlist);
                    menu.add(0, CLEAR_PLAYLIST, 0, title);
                }

                CharSequence title2 = getString(R.string.add_file);
                menu.add(0, ADD_SONG_TO_PLAYLIST, 0, title2);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, String activityName, Bundle options) {

        if (activityName.equals(PluginUtils.TRACK_BROWSER_ACTIVITY)) {

            MenuItem item = menu.findItem(CLEAR_PLAYLIST);
            String playlistName = options.getString(PluginUtils.PLAYLIST_NAME);
            int playlistLen = options.getInt(PluginUtils.PLAYLIST_LEN);
            Log.d(TAG, "onPrepareOptionsMenuForPlugin " + playlistName + " " + playlistLen);

            if (item != null && playlistName != null) {

                /* hide the clear playlist menu when there are no songs in it */
                if (playlistLen <= 0) {
                    item.setVisible(false);
                } else {
                    item.setVisible(true);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(Context context, MenuItem menuItem,
                                         String activityName, Activity activity,
                                         PluginUtils.IMusicListenter musicListener,
                                         Bundle options) {

        if (activityName.equals(PluginUtils.TRACK_BROWSER_ACTIVITY)) {

            Log.d(TAG, "onOptionItemSelected " + menuItem.getItemId());
            switch (menuItem.getItemId()) {

                case CLEAR_PLAYLIST:

                    String playlistName = options.getString(PluginUtils.PLAYLIST_NAME);
                    Log.d(TAG, "onOptionItemSelected playlist " + Integer.parseInt(playlistName));
                    musicListener.onCallClearPlaylist(context, Integer.parseInt(playlistName));
                    activity.finish();
                    return true;

                case ADD_SONG_TO_PLAYLIST:

                    Intent intent = new Intent(SELECT_FILE_FROM_FILEMANAGER);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    activity.startActivityForResult(intent, REQUEST_TO_ADD_SONG_TO_PLAYLIST);
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent,
            Context context, PluginUtils.IMusicListenter musicListener,
            String activityName, Activity activity, Bundle options) {

        Log.d(TAG, "onActivityResultForPlugin " + activityName + " " + resultCode);
        if (requestCode == REQUEST_TO_ADD_SONG_TO_PLAYLIST
            && activityName.equals(PluginUtils.TRACK_BROWSER_ACTIVITY)
            && resultCode == Activity.RESULT_OK) {

            if (intent != null && intent.getData() != null) {
                String playlistName = options.getString(PluginUtils.PLAYLIST_NAME);
                addSongToPlaylist(context, musicListener, intent.getData(), playlistName);
            }
        }
    }

    private void addSongToPlaylist(Context context,
                                   PluginUtils.IMusicListenter musicListener,
                                   Uri uri,
                                   String playlistName) {
        String data = Uri.decode(uri.toString());
        if (data == null) {
            return;
        }

        data = data.replaceAll("'", "''");
        data = data.replaceFirst("file://", "");

        String where = MediaStore.Audio.Media.DATA + " LIKE '%" + data + "'";
        Cursor cursor =
                getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Media._ID }, where, null, null);
        long selectFileId = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                selectFileId = cursor.getLong(0);
            }
            cursor.close();
            cursor = null;
        }
        if (selectFileId < 0) {
            Log.d(TAG, "addFileToPlay: select file is not audio file!");
            String toastShow = getString(R.string.non_audio_file);
            Toast.makeText(context, toastShow, Toast.LENGTH_SHORT).show();
            return;
        }

        long[] list = new long[] { selectFileId };
        if (playlistName.equals(PluginUtils.NOW_PLAYING)) {
            musicListener.onCallAddToCurrentPlaylist(context, list);
        } else {
            musicListener.onCallAddToPlaylist(context, list, Integer.parseInt(playlistName));
        }
    }
}
