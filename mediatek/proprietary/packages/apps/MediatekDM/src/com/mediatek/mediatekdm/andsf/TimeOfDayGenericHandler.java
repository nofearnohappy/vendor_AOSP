package com.mediatek.mediatekdm.andsf;

import com.mediatek.common.collaboradio.andsf.NodeTimeOfDay;
import com.mediatek.mediatekdm.iohandler.NodeIoHandlerWrapper;
import com.mediatek.mediatekdm.iohandler.PlainStringHandler;
import com.mediatek.mediatekdm.mdm.MdmTree;

public class TimeOfDayGenericHandler extends NodeIoHandlerWrapper {
    static final String TIME_START = "TimeStart";
    static final String TIME_STOP = "TimeStop";
    static final String DATE_START = "DateStart";
    static final String DATE_STOP = "DateStop";

    private NodeTimeOfDay.X mNode;
    private String mUri;

    public TimeOfDayGenericHandler(String uri, NodeTimeOfDay.X node) {
        mUri = uri;
        mNode = node;
        String nodeName = MdmTree.getNodeName(uri);

        if (nodeName.equals(TIME_START)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.timeStart = value;
                }

                @Override
                protected String readValue() {
                    return mNode.timeStart;
                }
            });
        } else if (nodeName.equals(TIME_STOP)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.timeStop = value;
                }

                @Override
                protected String readValue() {
                    return mNode.timeStop;
                }
            });
        } else if (nodeName.equals(DATE_START)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.dateStart = value;
                }

                @Override
                protected String readValue() {
                    return mNode.dateStart;
                }
            });
        } else if (nodeName.equals(DATE_STOP)) {
            setHandler(new PlainStringHandler(uri) {
                @Override
                protected void writeValue(String value) {
                    mNode.dateStop = value;
                }

                @Override
                protected String readValue() {
                    return mNode.dateStop;
                }
            });
        } else {
            throw new Error("Unsupported Uri: " + mUri);
        }
    }
}
