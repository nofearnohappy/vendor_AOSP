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
import android.content.ContentResolver;
import android.content.ContentValues;
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
import com.mediatek.op09.plugin.R;

/**
 * A class for customizing Music's behavior in OP09 project.
 *
 * We support play folder and play file feature here.
 */
@PluginImpl(interfaceName = "com.mediatek.music.ext.IMusicTrackBrowser")
public class Op09MusicTrackBrowserExtension extends DefaultMusicTrackBrowser {

    private static final String TAG = "Op09MusicTrackBrowserExtension";
    private static final String SELECT_FOLDER_FROM_FILEMANAGER
                                = "com.mediatek.filemanager.DOWNLOAD_LOCATION";
    private static final String SELECT_FILE_FROM_FILEMANAGER
                                = "com.mediatek.filemanager.ADD_FILE";
    private static final int REQUEST_TO_ADD_FOLDER_TO_PLAY
                             = PluginUtils.ACTIVITY_REQUEST_CODE_BASE_ID;
    private static final int REQUEST_TO_ADD_SONG_TO_PLAY
                             = PluginUtils.ACTIVITY_REQUEST_CODE_BASE_ID + 1;
    private static final int REQUEST_TO_ADD_FOLDER_AS_PLAYLIST
                             = PluginUtils.ACTIVITY_REQUEST_CODE_BASE_ID + 2;

    private static final int ADD_FOLDER_TO_PLAY = PluginUtils.MENU_ITEM_BASE_ID;
    private static final int ADD_SONG_TO_PLAY = PluginUtils.MENU_ITEM_BASE_ID + 1;
    private static final int ADD_FOLDER_AS_PLAYLIST = PluginUtils.MENU_ITEM_BASE_ID + 2;

    private final static long [] sEmptyList = new long[0];

    /**
     *  Contructor.
     *  @param context host app context
     */
    public Op09MusicTrackBrowserExtension(Context context) {
        super(context);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, String activityName, Bundle options) {

        if (activityName.equals(PluginUtils.MUSIC_BROWSER_ACTIVITY)) {

             int tabIndex = options.getInt(PluginUtils.TAB_INDEX);

             if (tabIndex == PluginUtils.SONG_TAB_INDEX) {
                CharSequence title = getString(R.string.add_folder_to_play);
                menu.add(0, ADD_FOLDER_TO_PLAY, 0, title);

                CharSequence title2 = getString(R.string.add_song_to_play);
                menu.add(0, ADD_SONG_TO_PLAY, 0, title2);

             } else if (tabIndex == PluginUtils.PLAYLIST_TAB_INDEX) {
                CharSequence title3 = getString(R.string.add_folder_as_playlist);
                menu.add(0, ADD_FOLDER_AS_PLAYLIST, 0, title3);
             }

        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu, String activityName, Bundle options) {
    }

    @Override
    public boolean onOptionsItemSelected(
                           Context context,
                           MenuItem menuItem,
                           String activityName,
                           Activity activity,
                           PluginUtils.IMusicListenter musicListener,
                           Bundle options) {

        if (activityName.equals(PluginUtils.MUSIC_BROWSER_ACTIVITY)) {

            int tabIndex = options.getInt(PluginUtils.TAB_INDEX);
            int itemId = menuItem.getItemId();
            if (tabIndex == PluginUtils.SONG_TAB_INDEX) {
                if (itemId == ADD_FOLDER_TO_PLAY) {

                    Intent intent = new Intent(SELECT_FOLDER_FROM_FILEMANAGER);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    activity.startActivityForResult(intent, REQUEST_TO_ADD_FOLDER_TO_PLAY);
                    return true;
                } else if (itemId == ADD_SONG_TO_PLAY) {

                    Intent intent = new Intent(SELECT_FILE_FROM_FILEMANAGER);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    activity.startActivityForResult(intent, REQUEST_TO_ADD_SONG_TO_PLAY);
                    return true;
                }
            } else if (tabIndex == PluginUtils.PLAYLIST_TAB_INDEX) {

                if (itemId == ADD_FOLDER_AS_PLAYLIST) {

                    Intent intent = new Intent(SELECT_FOLDER_FROM_FILEMANAGER);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    activity.startActivityForResult(intent, REQUEST_TO_ADD_FOLDER_AS_PLAYLIST);
                    return true;
                }
            }

        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent,
            Context context, PluginUtils.IMusicListenter musicListener,
            String activityName, Activity activity, Bundle options) {

            if (activityName.equals(PluginUtils.PLAYLIST_BROWSER_ACTIVITY)) {

                 if (requestCode == REQUEST_TO_ADD_FOLDER_AS_PLAYLIST) {

                     if (intent != null) {
                         if (intent != null) {
                             addFolderToMusic(
                                 context,
                                 intent.getStringExtra("download path"),
                                 -1,
                                 false,
                                 musicListener);
                         }
                     }
                 }


            } else if (activityName.equals(PluginUtils.TRACK_BROWSER_ACTIVITY)) {
                 if (requestCode == REQUEST_TO_ADD_SONG_TO_PLAY) {
                      if (intent != null) {
                          addSongToPlay(activity, intent.getData(), musicListener);
                      }
                 } else if (requestCode == REQUEST_TO_ADD_FOLDER_TO_PLAY) {

                      if (intent != null) {
                          addFolderToMusic(
                              activity,
                              intent.getStringExtra("download path"),
                              -1,
                              true,
                              musicListener);
                      }
                 }
            }
    }

    private void addSongToPlay(
                         Context context,
                         Uri uri,
                         PluginUtils.IMusicListenter musicListener) {
        String data = Uri.decode(uri.toString());
        if (data == null) {
            return;
        }
        /// M: to avoid the JE when query the file whoes name contains "'"
        /// which is the escape character of SQL.
        Log.d(TAG, "addFileToPlay: data=" + data);
        data = data.replaceAll("'", "''");
        data = data.replaceFirst("file://", "");

        /// When select file can not found in media database, it means the file is not a audio file,
        /// show toast to user.
        String where = MediaStore.Audio.Media.DATA + " LIKE '%" + data + "'";
        Cursor cursor = context
                            .getContentResolver()
                            .query(
                                 MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                 , new String[] {MediaStore.Audio.Media._ID}
                                 , where
                                 , null, null);
        long selectFileId = -1;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                selectFileId = cursor.getLong(0);
            }
            cursor.close();
            cursor = null;
        }
        if (selectFileId < 0) {
            Log.w(TAG, "addFileToPlay: select file is not audio file!");
            String toastShow = getString(R.string.none_audio_file);
            Toast.makeText(context, toastShow, Toast.LENGTH_SHORT).show();
            return;
        }

        /// Select file is audio, play all audio in the same folder.
        String folderPath = data.substring(0, data.lastIndexOf("/"));
        addFolderToMusic(context, folderPath, selectFileId, true, musicListener);
    }

    /**
     * M: get a song list with given cursor and eliminate these songs in sub folder.
     *
     * @param cursor
     * @param folderPath
     * @return The song list
     */
    private long [] getSongListForCursorExceptSubFolder(Cursor cursor, String folderPath) {
        if (cursor == null || folderPath == null) {
            return sEmptyList;
        }
        int len = cursor.getCount();
        long [] listAll = new long[len];
        cursor.moveToFirst();
        int columnId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        int columnData = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        String data;
        String path;
        int songNum = 0;
        for (int i = 0; i < len; i++) {
            data = cursor.getString(columnData);
            path = data.substring(0, data.lastIndexOf("/"));
            /// Only put these songs in select folder to list
            Log.d(TAG, "getSongListForCursorExceptSubFolder: path = "
                  + path + ", folderPath = " + folderPath);
            if (folderPath.equals(path)) {
                listAll[songNum++] = cursor.getLong(columnId);
            }
            cursor.moveToNext();
        }
        /// If there are no audio in select folder except sub folder, return a empty list.
        if (songNum == 0) {
            Log.w(TAG, "getSongListForCursorExceptSubFolder: select folder has no music!");
            return sEmptyList;
        }
        /// Copy these audio in select folder to a new list
        len = songNum;
        long [] listExceptSubFolder = new long[len];
        for (int i = 0; i < len; i++) {
            listExceptSubFolder[i] = listAll[i];
        }
        return listExceptSubFolder;
    }

    /**
     * M: Get the playlist id with given name.
     *
     * @param context context
     * @param name playlist name
     * @return playlist id with given name if exist, otherwise -1.
     */
    private int idForplaylist(Context context, String name) {
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[] { MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME},
                null,
                null,
                MediaStore.Audio.Playlists.NAME);
        int id = -1;
        c.moveToFirst();
        while (! c.isAfterLast()) {
            String playlistname = c.getString(1);
            if (playlistname != null && playlistname.compareToIgnoreCase(name) == 0) {
                id = c.getInt(0);
                break;
            }
            c.moveToNext();
        }
        c.close();
        return id;
    }

    /**
     * M: make playlist name not exsit in database with given template.
     *
     * @param context Context
     * @param template A template to format the playlist name.
     * @return
     */
    static String makePlaylistName(Context context, String template) {
        int num = 1;

        String[] cols = new String[] {
                MediaStore.Audio.Playlists.NAME
        };
        ContentResolver resolver = context.getContentResolver();
        String whereclause = MediaStore.Audio.Playlists.NAME + " != ''";
        Cursor c = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
            cols, whereclause, null,
            MediaStore.Audio.Playlists.NAME);

        if (c == null) {
            return null;
        }

        String suggestedname;
        suggestedname = String.format(template, num++);

        // Need to loop until we've made 1 full pass through without finding a match.
        // Looping more than once shouldn't happen very often, but will happen if
        // you have playlists named "New Playlist 1"/10/2/3/4/5/6/7/8/9, where
        // making only one pass would result in "New Playlist 10" being erroneously
        // picked for the new name.
        boolean done = false;
        while (!done) {
            done = true;
            c.moveToFirst();
            while (! c.isAfterLast()) {
                String playlistname = c.getString(0);
                if (playlistname.compareToIgnoreCase(suggestedname) == 0) {
                    suggestedname = String.format(template, num++);
                    done = false;
                }
                c.moveToNext();
            }
        }
        c.close();
        return suggestedname;
    }


    private void addFolderToMusic(
                        Context context,
                        String folderPath,
                        long needMoveToFirstAudioId,
                        boolean needPlay,
                        PluginUtils.IMusicListenter musicListener) {
        Log.d(TAG, "addFolderToMusic: folderPath = " + folderPath + ", needMoveToFirstAudioData = "
                + needMoveToFirstAudioId + ", needPlay = " + needPlay);
        if (folderPath == null) {
            return;
        }
        /* to avoid the JE when query the file
           whose name contains "'"
           which is the escape character of SQL. */
        String data = folderPath.replaceAll("'", "''");
        String where = "_data LIKE '%" + data + "%'";
        Log.d(TAG, "addFolderToMusic: where = " + where);
        Cursor cursor = context
                            .getContentResolver()
                            .query(
                                  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                  new String[]
                                  {
                                      MediaStore.Audio.Media._ID,
                                      MediaStore.Audio.Media.DATA
                                  },
                                  where,
                                  null,
                                  MediaStore.Audio.Media.TITLE_PINYIN_KEY);
        boolean isSelectFolderEmpty = true;
        if (cursor != null && cursor.moveToFirst()) {
            long [] list = getSongListForCursorExceptSubFolder(cursor, folderPath);
            int length = list.length;
            if (length > 0) {
                /* If need play,
                   we add this songs to current playlist to play,
                   otherwise we save them as a playlist.*/
                if (needPlay) {
                    if (needMoveToFirstAudioId >= 0) {
                        /// M: We need put the select audio to first position to play first.
                        for (int i = 0; i < length; i++) {
                            if (needMoveToFirstAudioId == list[i]) {
                                long ret = list[0];
                                list[0] = list[i];
                                list[i] = ret;
                                break;
                            }
                        }
                    }
                    musicListener.onCallPlay(context, list, 0);
                } else {
                    /// Get the folder name to be as playlist name
                    String name = data.substring(data.lastIndexOf("/") + 1);
                    Log.d(TAG, "addFolderToMusic: name = " + name);
                    /* If the folder name has existed in playlist,
                       make a new playlist name with a number
                       suffix and insert it to database*/
                    int playlistId = idForplaylist(context, name);
                    if (playlistId >= 0) {
                        name = makePlaylistName(context, name + "%d");
                    }
                    ContentValues values = new ContentValues(1);
                    values.put(MediaStore.Audio.Playlists.NAME, name);
                    Uri uri = context.getContentResolver()
                              .insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                    playlistId = Integer.parseInt(uri.getLastPathSegment());
                    musicListener.onCallAddToPlaylist(context, list, playlistId);
                }
                isSelectFolderEmpty = false;
            }
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
        /// If selected folder has no music, we should toast to user.
        if (isSelectFolderEmpty) {
            String toastShow = getString(R.string.select_folder_empty);
            Toast.makeText(context, toastShow, Toast.LENGTH_SHORT).show();
        }
    }




}
