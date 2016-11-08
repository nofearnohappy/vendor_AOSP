package com.mediatek.mediatekdm.mdm.scomo;

/**
 * Represents a deployment component. The deployment components are managed by the implementor of
 * PLInventory. To get an instance of this class ScomoFactory can be used. In case this
 * implementation is too naive, the user may extend this class and ScomoFactory class.
 */
public class ScomoComponent {
    private String mDescription;
    private String mEnvType;
    private String mId;
    private String mName;
    private String mVersion;
    private boolean mActive;

    public ScomoComponent() {
        // do nothing
    }

    public void create(String id, String name, String version, String description, String envType,
            boolean isActive) {
        mId = id;
        mName = name;
        mVersion = version;
        mDescription = description;
        mEnvType = envType;
        mActive = isActive;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getEnvType() {
        return mEnvType;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public void setEnvType(String envType) {
        mEnvType = envType;
    }

    public void setId(String id) {
        mId = id;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public boolean isActive() {
        return mActive;
    }

    public void setActive(boolean isActive) {
        mActive = isActive;
    }
}
