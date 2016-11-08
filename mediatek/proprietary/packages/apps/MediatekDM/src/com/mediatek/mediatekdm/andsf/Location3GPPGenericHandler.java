package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.Node3GPPLocation;
import com.mediatek.mediatekdm.iohandler.NodeIoHandlerWrapper;
import com.mediatek.mediatekdm.iohandler.PlainBinHandler;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;

public class Location3GPPGenericHandler extends NodeIoHandlerWrapper {
    static final String PLMN = "PLMN";
    static final String TAC = "TAC";
    static final String LAC = "LAC";
    static final String GERAN_CI = "GERAN_CI";
    static final String UTRAN_CI = "UTRAN_CI";
    static final String EUTRA_CI = "EUTRA_CI";

    private Node3GPPLocation.X mNode;
    private String mUri;

    public Location3GPPGenericHandler(String uri, Node3GPPLocation.X node) {
        mUri = uri;
        mNode = node;
        String nodeName = MdmTree.getNodeName(uri);

        if (nodeName.equals(PLMN)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.plmn = value;
                }

                @Override
                protected String readValue() {
                    return mNode.plmn;
                }
            });
        } else if (nodeName.equals(TAC)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.tac = value;
                }

                @Override
                protected String readValue() {
                    return mNode.tac;
                }
            });
        } else if (nodeName.equals(LAC)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.lac = value;
                }

                @Override
                protected String readValue() {
                    return mNode.lac;
                }
            });
        } else if (nodeName.equals(GERAN_CI)) {

            // 16 bits
            setHandler(new PlainBinHandler(uri) {
                @Override
                protected void writeValue(byte[] value) {
                    mNode.geranCI = value[0] << 8 + value[1];
                }

                @Override
                protected byte[] readValue() {
                    return new byte[] {(byte) ((mNode.geranCI >> 8) & 255),
                            (byte) (mNode.geranCI & 255)};
                }
            });
        } else if (nodeName.equals(UTRAN_CI)) {

            // 28 bits
            setHandler(new PlainBinHandler(uri) {
                @Override
                protected void writeValue(byte[] value) {
                    mNode.utranCI = value[0] << 24 + value[1] << 16
                            + value[2] << 8 + value[3];
                }

                @Override
                protected byte[] readValue() {
                    return new byte[] {(byte) ((mNode.utranCI >> 24) & 16),
                            (byte) ((mNode.utranCI >> 16) & 255),
                            (byte) ((mNode.utranCI >> 8) & 255),
                            (byte) (mNode.utranCI & 255)};
                }
            });
        } else if (nodeName.equals(EUTRA_CI)) {

            // 28 bits
            setHandler(new PlainBinHandler(uri) {
                @Override
                protected void writeValue(byte[] value) {
                    mNode.eutraCI = value[0] << 24 + value[1] << 16
                            + value[2] << 8 + value[3];
                }

                @Override
                protected byte[] readValue() {
                    return new byte[] {(byte) ((mNode.eutraCI >> 24) & 16),
                            (byte) ((mNode.eutraCI >> 16) & 255),
                            (byte) ((mNode.eutraCI >> 8) & 255),
                            (byte) (mNode.eutraCI & 255)};
                }
            });
        } else {
            throw new Error("Unsupported Uri: " + mUri);
        }
    }

}
