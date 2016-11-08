package com.mediatek.rcs.pam.util;

import java.util.List;

public class PaVcardParserResult {
    private final String mName;
    private final String mOrganization;
    private final String mTitle;
    private final List<PaVcardData> mNumber;
    private final List<PaVcardData> mEmail;
    private final byte[] mPhoto;
    private final String mPhotoFormat;
    
    public PaVcardParserResult(String name, String organization, String title,
            List<PaVcardData> number, List<PaVcardData> email, byte[] photo, String photoFormat) {
        mName = name;
        mOrganization = organization;
        mTitle = title;
        mNumber = number;
        mEmail = email;
        mPhoto = photo;
        mPhotoFormat = photoFormat;
    }
    
    public String getName() {
        return mName;
    }
    
    public String getOrganization() {
        return mOrganization;
    }
    
    public String getTitle() {
        return mTitle;
    }
    
    public List<PaVcardData> getNumber() {
        return mNumber;
    }
    
    public List<PaVcardData> getEmail() {
        return mEmail;
    }
    
    public byte[] getPhoto() {
        return mPhoto;
    }
    
    public String getPhotoFormat() {
        return mPhotoFormat;
    }
}
