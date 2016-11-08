package com.mediatek.vendoremail.utils;

/**
 * This class provides server/domain/username/incoming/outgoing information for providers.
 *
 */
public class ProviderInfo {

    public String id;
    public String label;
    public String domain;
    public String incomingUriTemplate;
    public String incomingUsernameTemplate;
    public String outgoingUriTemplate;
    public String outgoingUsernameTemplate;
    public String note;

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(id).append("\n");
        sb.append("Label: ").append(label).append("\n");
        sb.append("Domain: ").append(domain).append("\n");
        sb.append("IncomingUriTemplate: ").append(incomingUriTemplate).append("\n");
        sb.append("IncomingUsernameTemplate: ").append(incomingUsernameTemplate).append("\n");
        sb.append("OutgoingUriTemplate: ").append(outgoingUriTemplate).append("\n");
        sb.append("OutgoingUsernameTemplate: ").append(outgoingUsernameTemplate).append("\n");
        sb.append("Note: ").append(note).append("\n");
        sb.append("Domain: ").append(domain).append("\n");
        return sb.toString();
    }
}
