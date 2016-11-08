package com.mediatek.vendoremail.utils;

/**
 * This class provides the ui information for the provider list.
 *
 */
public class ProviderUiInfo {

    public String name;
    public String domain;
    public String hint;
    public String defaultProtocol;
    public int iconResId;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(name).append("\n");
        sb.append("Domain: ").append(domain).append("\n");
        sb.append("Default protocol: ").append(defaultProtocol).append("\n");
        sb.append("Hint: ").append(hint).append("\n");
        sb.append("Icon: ").append(iconResId);
        return sb.toString();
    }
}
