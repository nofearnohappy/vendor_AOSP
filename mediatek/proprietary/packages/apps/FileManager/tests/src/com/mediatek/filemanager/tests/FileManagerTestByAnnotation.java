package com.mediatek.filemanager.tests;

import java.io.File;
import java.io.IOException;

import android.content.Intent;
import com.mediatek.filemanager.service.FileManagerService;
import com.mediatek.filemanager.tests.annotation.*;

public class FileManagerTestByAnnotation extends AbsOperationActivityTest {
    private final String TAG = "FileManagerTestByAnnotation";
    private final String FILE_NAME = "test.txt";
    private final String FILE_RECEIVED = "com.mediatek.hotknot.action.FILEMANAGER_FILE_RECEIVED";

    @FwkAnnotation
    public void testcase001_testFile() {
        //FileManagerService service = new FileManagerService();
        //service.createFolder("", "", "");
        File file = new File(FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
        assertTrue(!file.exists());
        boolean createRes = false;
        try {
            createRes = file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertTrue(!createRes);
        //assertTrue(file.exists());
        //file.delete();
    }

    @ExternalApiAnnotation
    public void testcase002_testEnterFolder() {
        Intent itt = new Intent();
        itt.setAction(FILE_RECEIVED);
        //getInstrumentation().getContext().startActivity(itt);
    }

    @InternalApiAnnotation
    public void testcase003_testCreateFolder() {
        FileManagerService service = new FileManagerService();
        assertTrue(null != service);
    }
}