/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2008 The Android Open Source Project
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
 */
package com.mediatek.voiceextension;

/**
 * Provides all voice command results notified from voice command service.
 *
 */

public class VoiceCommandResult {

    /**
     * @hide
     */
    private VoiceCommandResult() {
    }

    // ==========================RetCode Common==========================//
    private static final int VOICE_COMMON_INDEX = 0;

    /**
     * Failure result, including all synchronous and asynchronous operations
     * in {@link VoiceCommandManager}.
     */
    public static final int FAILURE = VOICE_COMMON_INDEX - 1;

    /**
     * Successful result, including all synchronous and asynchronous operations
     * in {@link VoiceCommandManager}.
     */
    public static final int SUCCESS = VOICE_COMMON_INDEX + 1;

    /**
     * Failed to write data to storage. It may be caused by insufficient phone
     * storage space.
     */
    public static final int WRITE_STORAGE_FAIL = VOICE_COMMON_INDEX + 2;

    /**
     * Failed to initialize the microphone.
     */
    public static final int MIC_INIT_FAIL = VOICE_COMMON_INDEX + 3;

    /**
     * The microphone is occupied by other functions.
     */
    public static final int MIC_OCCUPIED = VOICE_COMMON_INDEX + 4;

    /**
     * Listener is illegal.
     */
    public static final int LISTENER_ILLEGAL = VOICE_COMMON_INDEX + 5;

    /**
     * Listener wasn't set before.
     */
    public static final int LISTENER_NEVER_SET = VOICE_COMMON_INDEX + 6;

    /**
     * The Listener already exists.
     */
    public static final int LISTENER_ALREADY_SET = VOICE_COMMON_INDEX + 7;

    /**
     * Recognition has not started before; should be started first.
     */
    public static final int RECOGNITION_NEVER_START = VOICE_COMMON_INDEX + 8;

    /**
     * Recognition has not paused before.
     */
    public static final int RECOGNITION_NEVER_PAUSE = VOICE_COMMON_INDEX + 9;

    /**
     * Recognition has already started.
     */
    public static final int RECOGNITION_ALREADY_STARTED = VOICE_COMMON_INDEX + 10;

    /**
     * Recognition has already paused.
     */
    public static final int RECOGNITION_ALREADY_PAUSED = VOICE_COMMON_INDEX + 11;

    /**
     * Service does not exist.
     */
    public static final int SERVICE_NOT_EXIST = VOICE_COMMON_INDEX + 12;

    /**
     * Service has been disconnected.
     */
    public static final int SERVICE_DISCONNECTTED = VOICE_COMMON_INDEX + 13;

    /**
     * Process is illegal.
     */
    public static final int PROCESS_ILLEGAL = VOICE_COMMON_INDEX + 14;

    // ==============================Voice Command=======================//
    /**
     * The Command Set already exists.
     */
    public static final int COMMANDSET_ALREADY_EXIST = VoiceCommonState.RET_SET_ALREADY_EXIST;

    /**
     * The Command Set has not yet existed.
     */
    public static final int COMMANDSET_NOT_EXIST = VoiceCommonState.RET_SET_NOT_EXIST;

    /**
     * The Command Set is invalid. Check the format.
     */
    public static final int COMMANDSET_NAME_ILLEGAL = VoiceCommonState.RET_SET_ILLEGAL;

    /**
     * The Command Set string length exceeds the limit.
     */
    public static final int COMMANDSET_NAME_LENGTH_EXCEED_LIMIT = VoiceCommonState.RET_SET_EXCEED_LIMIT;

    /**
     * The Command Set has been selected.
     */
    public static final int COMMANDSET_ALREADY_SELECTED = VoiceCommonState.RET_SET_SELECTED;

    /**
     * The Command Set hasn't been selected.
     */
    public static final int COMMANDSET_NOT_SELECTED = VoiceCommonState.RET_SET_NOT_SELECTED;

    /**
     * The Command Set is occupied; can not be deleted.
     */
    public static final int COMMANDSET_OCCUPIED = VoiceCommonState.RET_SET_OCCUPIED;

    /**
     * Commands data are invalid. Check the data format.
     */
    public static final int COMMANDS_DATA_INVALID = VoiceCommonState.RET_COMMAND_DATA_INVALID;

    /**
     * Commands file is illegal. Check the file path or the file data format.
     */
    public static final int COMMANDS_FILE_ILLEGAL = VoiceCommonState.RET_COMMAND_FILE_ILLEGAL;

    /**
     * Commands number exceeds the limit. Reduce the commands number.
     */
    public static final int COMMANDS_NUM_EXCEED_LIMIT = VoiceCommonState.RET_COMMAND_NUM_EXCEED_LIMIT;

}