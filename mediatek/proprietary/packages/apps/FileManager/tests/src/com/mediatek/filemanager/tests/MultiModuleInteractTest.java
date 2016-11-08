package com.mediatek.filemanager.tests;

import java.io.File;
import java.util.Locale;

import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.mediatek.filemanager.FileInfo;
import com.mediatek.filemanager.MountPointManager;
import com.mediatek.filemanager.tests.utils.TestUtils;
import com.mediatek.filemanager.utils.DrmManager;
import com.mediatek.filemanager.utils.LogUtils;
import com.mediatek.filemanager.utils.OptionsUtils;
import com.mediatek.storage.StorageManagerEx;

public class MultiModuleInteractTest extends AbsOperationActivityTest {
    private final String TAG = "MultiModuleInteractTest";
    private final Uri mAudioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private final Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private final Uri mVideoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private Cursor mCur = null;

    public void test001MediaStoreWithRename() {
        LogUtils.d(TAG, "testMediaStoreWithRename");
/*
        final String folderName = "testMediaStore";
        final String subFolderName = "testRename";
        final String newSubFolderName = "testRenameDone";

        final String audioName = "minutes";
        final String audioExtension = ".mp3";
        final String newAudioName = "newminutes";

        final String imageName = "beauty";
        final String imageExtension = ".jpg";
        final String newImageName = "newbeauty";

        final String videoName = "spy";
        final String videoExtension = ".3gp";
        final String newVideoName = "newspy";

        String path = TestUtils.getTestPath(folderName);
        assertTrue(launchWithPath(TestUtils.getTestPath(null)));
        deleteFile(new FileInfo(new File(path)), true);
        TestUtils.sleep(500);

        // create sub folder
        File subFolder = new File(path + MountPointManager.SEPARATOR + subFolderName);
        TestUtils.createDirectory(subFolder);

        // create audio file
        File audioFile = new File(path + MountPointManager.SEPARATOR + subFolderName
                + MountPointManager.SEPARATOR + audioName + audioExtension);
        TestUtils.createFile(audioFile);
        FileInfo audioFileInfo = new FileInfo(audioFile);
        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.minutes, audioFile);

        // create image file
        File imageFile = new File(path + MountPointManager.SEPARATOR + subFolderName
                + MountPointManager.SEPARATOR + imageName + imageExtension);
        TestUtils.createFile(imageFile);
        FileInfo imageFileInfo = new FileInfo(imageFile);
        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.beauty, imageFile);

        // create video file
        File videoFile = new File(path + MountPointManager.SEPARATOR + subFolderName
                + MountPointManager.SEPARATOR + videoName + videoExtension);
        TestUtils.createFile(videoFile);
        FileInfo videoFileInfo = new FileInfo(videoFile);
        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.spy, videoFile);

        assertTrue(launchWithStartActivity(path));
        ContentResolver cr = getActivity().getContentResolver();

        ContentValues cvAudio = new ContentValues();
        cvAudio.put(MediaStore.Audio.Media.DATA, audioFile.getAbsolutePath());
        cvAudio.put(MediaStore.Audio.Media.TITLE, "wrong value");
        cvAudio.put(MediaStore.Audio.Media.DISPLAY_NAME, "wrong value");
        cvAudio.put(MediaStore.Audio.Media.IS_MUSIC, 0);
        Uri result = cr.insert(mAudioUri, cvAudio);
        assertNotNull(result);

        ContentValues cvImage = new ContentValues();
        cvImage.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
        cvImage.put(MediaStore.Images.Media.TITLE, "wrong value");
        cvImage.put(MediaStore.Images.Media.DISPLAY_NAME, "wrong value");
        result = cr.insert(mImageUri, cvImage);
        assertNotNull(result);

        ContentValues cvVideo = new ContentValues();
        cvVideo.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
        cvVideo.put(MediaStore.Video.Media.TITLE, "wrong value");
        cvVideo.put(MediaStore.Video.Media.DISPLAY_NAME, "wrong value");
        result = cr.insert(mVideoUri, cvVideo);
        assertNotNull(result);

        // 1.test update image bucket_display_name with rename folder
        renameFile(new FileInfo(subFolder), newSubFolderName, true);
        TestUtils.sleep(3000);

        String newPath = path + MountPointManager.SEPARATOR + newSubFolderName
                + MountPointManager.SEPARATOR + imageName + imageExtension;
        LogUtils.d(TAG, "after rename folder, image path = " + newPath);

        mCur = cr.query(mImageUri, null,
                MediaStore.Images.Media.DATA + "=" + "\"" + newPath + "\"", null, null);
        assertTrue(mCur != null && mCur.getCount() == 1);
        if (mCur.moveToFirst()) {
            String bucketName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            mCur.close();
            LogUtils.d(TAG, "after reanme folder, image bucketName = " + bucketName);
            //Log.d(TAG, "after reanme folder, image bucketName = " + bucketName + " newSubFolderName: " + newSubFolderName);
            //assertTrue(bucketName.equalsIgnoreCase(newSubFolderName));
        }

        // 2.test update video bucket_display_name with rename folder

        newPath = path + MountPointManager.SEPARATOR + newSubFolderName
                + MountPointManager.SEPARATOR + videoName + videoExtension;
        LogUtils.d(TAG, "after rename folder, video path = " + newPath);

        mCur = cr.query(mVideoUri, null, MediaStore.Video.Media.DATA + "=" + "\"" + newPath + "\"",
                null, null);
        assertTrue(mCur != null && mCur.getCount() == 1);
        if (mCur.moveToFirst()) {
            String bucketName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
            mCur.close();
            LogUtils.d(TAG, "after reanme folder, video bucketName = " + bucketName);
            //assertTrue(bucketName.equalsIgnoreCase(newSubFolderName));
        }

        // 3. test update field title && _display_name && is_music
        int index = TestUtils.getListViewItemIndex(this, getActivity(), new FileInfo(new File(path
                + MountPointManager.SEPARATOR + newSubFolderName)));
        TestUtils.clickOneItem(this, getActivity(), index);
        TestUtils.sleep(100);

        // rename audio
        FileInfo newAudioFileInfo = new FileInfo(new File(path + MountPointManager.SEPARATOR
                + newSubFolderName + MountPointManager.SEPARATOR + audioName + audioExtension));
        renameFile(newAudioFileInfo, newAudioName + audioExtension, true);
        TestUtils.sleep(1000);

        newPath = path + MountPointManager.SEPARATOR + newSubFolderName
                + MountPointManager.SEPARATOR + newAudioName + audioExtension;
        mCur = cr.query(mAudioUri, null, MediaStore.Audio.Media.DATA + "=" + "\"" + newPath + "\"",
                null, null);
        if (mCur.moveToFirst()) {
            String displayName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
            int isMusic = mCur.getInt(mCur.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
            mCur.close();
            LogUtils.d(TAG, "after rename audio, displayName = " + displayName + ", isMusic = "
                    + isMusic);
            assertTrue(displayName.equalsIgnoreCase(newAudioName + audioExtension));
            assertTrue(isMusic == 1);
        }

        // rename image
        FileInfo newImageFileInfo = new FileInfo(new File(path + MountPointManager.SEPARATOR
                + newSubFolderName + MountPointManager.SEPARATOR + imageName + imageExtension));
        renameFile(newImageFileInfo, newImageName + imageExtension, true);
        TestUtils.sleep(1000);

        newPath = path + MountPointManager.SEPARATOR + newSubFolderName
                + MountPointManager.SEPARATOR + newImageName + imageExtension;
        mCur = cr.query(mImageUri, null,
                MediaStore.Images.Media.DATA + "=" + "\"" + newPath + "\"", null, null);
        LogUtils.d(TAG, "after rename image, mCursor count = " + mCur.getCount());
        assertTrue(mCur != null && mCur.getCount() == 1);
        if (mCur.moveToFirst()) {
            String displayName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            String title = mCur.getString(mCur.getColumnIndex(MediaStore.Images.Media.TITLE));
            mCur.close();
            LogUtils.d(TAG, "after rename image, displayName = " + displayName + ", title = "
                    + title);
            assertTrue(displayName.equalsIgnoreCase(newImageName + imageExtension));
            assertTrue(title.equalsIgnoreCase(newImageName));
        }

        // rename video
        FileInfo newVideoFileInfo = new FileInfo(new File(path + MountPointManager.SEPARATOR
                + newSubFolderName + MountPointManager.SEPARATOR + videoName + videoExtension));
        renameFile(newVideoFileInfo, newVideoName + videoExtension, true);
        TestUtils.sleep(1000);

        newPath = path + MountPointManager.SEPARATOR + newSubFolderName
                + MountPointManager.SEPARATOR + newVideoName + videoExtension;
        mCur = cr.query(mVideoUri, null, MediaStore.Video.Media.DATA + "=" + "\"" + newPath + "\"",
                null, null);
        assertTrue(mCur != null && mCur.getCount() == 1);
        if (mCur.moveToFirst()) {
            String displayName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
            String title = mCur.getString(mCur.getColumnIndex(MediaStore.Video.Media.TITLE));
            mCur.close();
            LogUtils.d(TAG, "after rename video, displayName = " + displayName + ", title = "
                    + title);
            assertTrue(displayName.equalsIgnoreCase(newVideoName + videoExtension));
            assertTrue(title.equalsIgnoreCase(newVideoName));
        }
*/
    }

    public void test002MediaStoreWithCut() {/*
        LogUtils.d(TAG, "testMediaStoreWithCut");

        final String folderName = "testMediaStore";
        final String ringtoneFolderName = "ringtones";
        final String subFolderName = "testCut";

        final String audioName = "minutes";
        final String audioExtension = ".mp3";

        final String imageName = "beauty";
        final String imageExtension = ".jpg";

        final String videoName = "spy";
        final String videoExtension = ".3gp";

        String path = TestUtils.getTestPath(folderName);
        assertTrue(launchWithPath(TestUtils.getTestPath(null)));
        deleteFile(new FileInfo(new File(path)), true);
        TestUtils.sleep(100);

        // create sub folder && ringtones folder
        File subFolder = new File(path + MountPointManager.SEPARATOR + subFolderName);
        File ringtoneFolder = new File(subFolder.getAbsolutePath() + MountPointManager.SEPARATOR
                + ringtoneFolderName);
        TestUtils.createDirectory(ringtoneFolder);

        // create audio file
        File audioFile = new File(path + MountPointManager.SEPARATOR + subFolderName
                + MountPointManager.SEPARATOR + audioName + audioExtension);
        TestUtils.createFile(audioFile);
        FileInfo audioFileInfo = new FileInfo(audioFile);
        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.minutes, audioFile);

        // create image file
        File imageFile = new File(path + MountPointManager.SEPARATOR + subFolderName
                + MountPointManager.SEPARATOR + imageName + imageExtension);
        TestUtils.createFile(imageFile);
        FileInfo imageFileInfo = new FileInfo(imageFile);
        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.beauty, imageFile);

        // create Video file
        File videoFile = new File(path + MountPointManager.SEPARATOR + subFolderName
                + MountPointManager.SEPARATOR + videoName + videoExtension);
        TestUtils.createFile(videoFile);
        FileInfo videoFileInfo = new FileInfo(videoFile);
        TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                com.mediatek.filemanager.tests.R.raw.spy, videoFile);

        TestUtils.sleep(100);
        assertTrue(launchWithStartActivity(subFolder.getAbsolutePath()));

        ContentResolver cr = getActivity().getContentResolver();

        ContentValues cvAudio = new ContentValues();
        cvAudio.put(MediaStore.Audio.Media.DATA, audioFile.getAbsolutePath());
        cvAudio.put(MediaStore.Audio.Media.TITLE, "wrong value");
        cvAudio.put(MediaStore.Audio.Media.DISPLAY_NAME, "wrong value");
        cvAudio.put(MediaStore.Audio.Media.IS_MUSIC, 0);
        cvAudio.put(MediaStore.Audio.Media.IS_RINGTONE, 0);
        Uri result = cr.insert(mAudioUri, cvAudio);
        assertNotNull(result);

        ContentValues cvImage = new ContentValues();
        cvImage.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
        cvImage.put(MediaStore.Images.Media.TITLE, "wrong value");
        cvImage.put(MediaStore.Images.Media.DISPLAY_NAME, "wrong value");
        result = cr.insert(mImageUri, cvImage);
        assertNotNull(result);

        ContentValues cvVideo = new ContentValues();
        cvVideo.put(MediaStore.Video.Media.DATA, videoFile.getAbsolutePath());
        cvVideo.put(MediaStore.Video.Media.TITLE, "wrong value");
        cvVideo.put(MediaStore.Video.Media.DISPLAY_NAME, "wrong value");
        result = cr.insert(mVideoUri, cvVideo);
        assertNotNull(result);

        // test update field title && _display_name && is_music && is_ringtone
        cutFile(audioFileInfo, true);
        TestUtils.sleep(100);

        int index = TestUtils.getListViewItemIndex(this, getActivity(),
                new FileInfo(ringtoneFolder));
        TestUtils.clickOneItem(this, getActivity(), index);
        TestUtils.sleep(100);
        paste();
        TestUtils.sleep(1000);

        // test audio
        File newAudioFile = new File(ringtoneFolder.getAbsolutePath() + MountPointManager.SEPARATOR
                + audioName + audioExtension);
        mCur = cr.query(mAudioUri, null, MediaStore.Audio.Media.DATA + "=" + "\""
                + newAudioFile.getAbsolutePath() + "\"", null, null);
        assertTrue(mCur != null && mCur.getCount() == 1);
        if (mCur.moveToFirst()) {
            String displayName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
            int isMusic = mCur.getInt(mCur.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
            int isRingtone = mCur.getInt(mCur.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE));
            mCur.close();
            LogUtils.d(TAG, "after cut audio, displayName =" + displayName + ", isMusic ="
                    + isMusic + ", isRingtone =" + isRingtone);
            assertTrue(displayName.equalsIgnoreCase(audioName + audioExtension));
            // is_music should be 0, when it is in ringtones folder.
            assertTrue(isMusic == 0);
            assertTrue(isRingtone == 1);
        }

        // test image
        File newImageFile = new File(ringtoneFolder.getAbsolutePath() + MountPointManager.SEPARATOR
                + imageName + imageExtension);
        mCur = cr.query(mImageUri, null, MediaStore.Images.Media.DATA + "=" + "\""
                + newImageFile.getAbsolutePath() + "\"", null, null);
        LogUtils.d(TAG, "after cut image, mCursor count = " + mCur.getCount());
        assertTrue(mCur != null && mCur.getCount() == 1);

        if (mCur.moveToFirst()) {
            String displayName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            String title = mCur.getString(mCur.getColumnIndex(MediaStore.Images.Media.TITLE));
            String bucketName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
            mCur.close();
            LogUtils.d(TAG, "after cut image, displayName =" + displayName + ", title =" + title
                    + ", bucketName =" + bucketName);
            assertTrue(displayName.equalsIgnoreCase(imageName + imageExtension));
            assertTrue(title.equalsIgnoreCase(imageName));
            assertTrue(bucketName.equalsIgnoreCase(ringtoneFolderName));
        }

        // test Video
        File newVideoFile = new File(ringtoneFolder.getAbsolutePath() + MountPointManager.SEPARATOR
                + videoName + videoExtension);
        mCur = cr.query(mVideoUri, null, MediaStore.Video.Media.DATA + "=" + "\""
                + newVideoFile.getAbsolutePath() + "\"", null, null);
        assertTrue(mCur != null && mCur.getCount() == 1);

        if (mCur.moveToFirst()) {
            String displayName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
            String title = mCur.getString(mCur.getColumnIndex(MediaStore.Video.Media.TITLE));
            String bucketName = mCur.getString(mCur
                    .getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
            mCur.close();
            LogUtils.d(TAG, "after cut video, displayName =" + displayName + ", title =" + title
                    + ", bucketName =" + bucketName);
            assertTrue(displayName.equalsIgnoreCase(videoName + videoExtension));
            assertTrue(title.equalsIgnoreCase(videoName));
            assertTrue(bucketName.equalsIgnoreCase(ringtoneFolderName));
        }

    */}

    public void test003Storage() {
        LogUtils.d(TAG, "testStorage");

        final String phoneStorage = "Phone storage";
        final String SDCard = "SD card";
        MountPointManager mpm = MountPointManager.getInstance();
        Configuration config = getActivity().getResources().getConfiguration();
        String language = config.locale.getLanguage();
        String path = mpm.getDefaultPath();
        //String path = StorageManagerEx.getInternalPath();
        String phoneStoragePath = StorageManagerEx.getInternalStoragePath();
        String description = mpm.getDescriptionPath(path);
        LogUtils.d(TAG, "language = " + language + ", default path = " + path + ", description = "
                + description);

        // test description
        if (language.equalsIgnoreCase(Locale.ENGLISH.getLanguage())) {
            if (path.equals(phoneStoragePath)) {
                assertTrue(description.equalsIgnoreCase(phoneStorage));
            } else {
                assertTrue(description.equalsIgnoreCase(SDCard));
            }
        } else if (language.equalsIgnoreCase(Locale.CHINESE.getLanguage())) {
            if (path.equals(phoneStoragePath)) {
                assertFalse(description.equalsIgnoreCase(phoneStorage));
            } else {
                assertFalse(description.equalsIgnoreCase(SDCard));
            }
        }
    }

    public void test004Drm() {
        LogUtils.d(TAG, "testDrm");
        if (OptionsUtils.isMtkDrmApp()) {
            final String testFolderName = "testDrm";
            final String drmFileName = "pictrue";
            final String fakeDrmFileName = "fakeDrm";
            final String notExistFileName = "IAmNotExist";
            final String drmExtension = "." + DrmManager.EXT_DRM_CONTENT;
            DrmManager dm = DrmManager.getInstance();
            dm.init(getInstrumentation().getContext());
            String path = TestUtils.getTestPath(testFolderName);
            File testFolder = new File(path);
            File notExistFile = new File(path + MountPointManager.SEPARATOR + notExistFileName
                    + drmExtension);
            TestUtils.deleteFile(testFolder);
            TestUtils.deleteFile(notExistFile);
            TestUtils.createDirectory(testFolder);

            File drmFile = new File(path + MountPointManager.SEPARATOR + drmFileName + drmExtension);
            FileInfo drmFileInfo = new FileInfo(drmFile);
            TestUtils.createFile(drmFile);
            TestUtils.copyRawFile(this.getInstrumentation().getContext(),
                    com.mediatek.filemanager.tests.R.raw.picture, drmFile);

            File fakeDrmFile = new File(path + MountPointManager.SEPARATOR + fakeDrmFileName
                    + drmExtension);
            FileInfo fakeDrmFileInfo = new FileInfo(fakeDrmFile);
            TestUtils.createFile(fakeDrmFile);

            // 1. test checkDrmObjectType()
            assertTrue(dm.checkDrmObjectType(drmFile.getAbsolutePath()));
            assertTrue(dm.checkDrmObjectType(fakeDrmFile.getAbsolutePath()));

            // 2. test getOriginalMineType() with real file/fake file/ not exist
            // path
            String mimeType = dm.getOriginalMimeType(drmFile.getAbsolutePath());
            String fakeMimeType = dm.getOriginalMimeType(fakeDrmFile.getAbsolutePath());
            String notExistMimeType = dm.getOriginalMimeType(notExistFile.getAbsolutePath());
            LogUtils.d(TAG, "mimeType = " + mimeType + ", fakeMimeType = " + fakeMimeType
                    + ", notExistMimeType = " + notExistMimeType);
            // anyway return should not be null
            assertTrue(mimeType != null && !mimeType.equalsIgnoreCase(""));
            assertTrue(fakeMimeType != null && fakeMimeType.equalsIgnoreCase(""));
            assertTrue(notExistMimeType != null && notExistMimeType.equalsIgnoreCase(""));
            dm.release();
        }
    }

    // NOTICE: this case should be run after MediaStore relative cases, because
    // mount message will cause MediaStore scan the whole storage, which may
    // block the cases.
    public void test005Mount() {
        LogUtils.i(TAG, "call testMount()");
        final String curFolderName = "testMount";
        final String curPath = TestUtils.getTestPath(curFolderName);
        File loadFile = new File(curPath);
        TestUtils.deleteFile(loadFile);
        TestUtils.createDirectory(loadFile);
        boolean launched = launchWithPath(curPath);
        assertTrue(launched);

/*        Uri uri;
        int mountCount = MountPointManager.getInstance().getMountCount();
        if (mountCount == 0) {
            return;
        } else {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MEDIA_UNMOUNTED);
            List<FileInfo> fileList = new ArrayList<FileInfo>();
            fileList = MountPointManager.getInstance().getMountPointFileInfo();
            uri = Uri.fromFile(fileList.get(mountCount - 1).getFile());
            intent.setData(uri);
            mInst.getContext().sendBroadcast(intent);
        }
        TestUtils.sleep(2000);
        assertTrue(mountCount > MountPointManager.getInstance().getMountCount());

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MEDIA_MOUNTED);
        intent.setData(uri);
        mInst.getContext().sendBroadcast(intent);

        TestUtils.sleep(5000);
        assertTrue(mountCount == MountPointManager.getInstance().getMountCount());

        // SDcard0 mount test
        Intent intent2 = new Intent();
        intent2.setAction(Intent.ACTION_MEDIA_UNMOUNTED);
        List<FileInfo> fileList = new ArrayList<FileInfo>();
        fileList = MountPointManager.getInstance().getMountPointFileInfo();
        uri = Uri.fromFile(fileList.get(0).getFile());
        intent2.setData(uri);
        mInst.getContext().sendBroadcast(intent2);

        TestUtils.sleep(2000);

        Intent intent3 = new Intent();
        intent.setAction(Intent.ACTION_MEDIA_MOUNTED);
        intent.setData(uri);
        mInst.getContext().sendBroadcast(intent);*/
    }
}
