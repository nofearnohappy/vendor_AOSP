/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cmcc.ccs.ft;

import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.IFileTransfer;

/**
 * This class offers the main entry point to transfer files and to
 * receive files. Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author
 */
public class ExtFileTransfer extends FileTransfer {
    
    public ExtFileTransfer(IFileTransfer transferIntf) {
        super(transferIntf);
        // TODO Auto-generated constructor stub
    }

    /**
     * API
     */
    public static final String TAG = "DAPI-ExtFileTransfer";

    /**
     * transfers a file to a group of contacts outside of a current group chat. The
     * parameter file contains the complete filename including the path to be transferred.
     * See also the method GroupChat.sendFile() of the Chat API to send a file from an
     * existing group chat conversation
     * 
     * @param chatId
     * @param filename Filename to transfer
     * @param listener File transfer event listener
     * @return File transfer
     */
    public ExtFileTransfer transferFileToGroupChat(String chatId, String filename, FileTransferListener listener) {
        // TODO
        return null;
    }

    /**
     * transfers a file to a group of contacts outside of a current group chat. The
     * parameter file contains the complete filename including the path to be transferred.
     * See also the method GroupChat.sendFile() of the Chat API to send a file from an
     * existing group chat conversation
     * 
     * @param chatId 
     * @param filename Filename to transfer
     * @param fileicon Fileicon to transfer
     * @param listener File transfer event listener
     * @return File transfer
     */
    public ExtFileTransfer transferFileToGroupChat(String chatId, String filename, String fileicon, FileTransferListener listener) {
        // TODO
        return null;
    }

}
