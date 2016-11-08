/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#ifndef _MTK_HARDWARE_INCLUDE_MTKCAM_LEGACYPIPELINE_LEGACYPIPELINEBUILDER_H_
#define _MTK_HARDWARE_INCLUDE_MTKCAM_LEGACYPIPELINE_LEGACYPIPELINEBUILDER_H_

namespace NSCam {
namespace v1 {
namespace NSLegacyPipeline {
/******************************************************************************
 *
 ******************************************************************************/
struct ImageStreamUsage{
    sp<IImageStreamInfo>        spImageStreamInfo;

    //for buffer provider
    MBOOL                       useImgHalPool;
    MBOOL                       isConsumer;
    StreamId_T                  providerStreamId;
    MINT32                      bufferQueueDepth;
    sp<IImgBufProvider>         spProvider;
    sp<IImageCallback>          spCallback;
    vector<sp<IImageBuffer> >   spvBuffers;
};

struct NodeStreamUsage{
    StreamId_T  streamId;
    MUINT       memoryUsage;//for image stream only
};

struct NodeSetting{
    MINT32                  nodeId;
    char*                   nodeName;
    vector<NodeStreamUsage> vNodeStreamUsage;
};

struct PipelineSensorParam{
    MINT32      sensorId;
    MUINT       mode;
    MINT32      rawType;//not used now
    MSize       size;
    MUINT       fps;
    MUINT32     pixelmode;
};

class LegacyPipelineBuilder
        : public virtual RefBase
{
public:     ////                    Interface for create and destroy
                                    LegacyPipelineBuilder(char* const pipeName);
                                    ~LegacyPipelineBuilder();

public:     ////                    Interface for setup PipelineContext.
    /**
     * set sensor id and sensor parameter.
     *
     * @param[in] sensroId: sensor id for all nodes.
     *
     * @param[in] sensorParam: sensor parameter for P1 node.
     *
     */
            MERROR                  setSensor(MINT32 const sensroId, PipelineSensorParam* const pSensorParam);

    /**
     * add meta stream infos.
     *
     * @param[in] appMetaStreamInfos: IMetaStreamInfo for app meta.
     *
     * @param[in] halMetaStreamInfos: IMetaStreamInfo for hal meta.
     *
     */
            MERROR                  addMetaStream(vector<sp<IMetaStreamInfo> > const appMetaStreamInfos, vector<sp<IMetaStreamInfo> > const halMetaStreamInfos);

    /**
     * add image stream infos and buffer providers.
     *
     * @param[in] imageStreamUsages: IImageStreamInfo and buffer provider setting.
     *
     */
            MERROR                  addImageStream(vector<ImageStreamUsage> const vImageStreamUsage);

    /**
     * add one node of PipelineContext.
     *
     * @param[in] nodeId: node id.
     *
     * @param[in] nodeName: node name.
     *
     * @param[in] nodeStreamUsages: stream id, port, memory usage of streams.
     *
     */
            MERROR                  addNode(IPipelineNode::NodeId_T const nodeId, char* const nodeName, vector<NodeStreamUsage> const vNodeStreamUsage);

    /**
     * set flow of PipelineContext.
     *
     * @param[in] rootNodeId: root node id.
     *
     * @param[in] nodeEdges: edges of pipeline flow.
     *
     */
            MERROR                  setFlow(IPipelineNode::NodeId_T const rootNodeId, vector<NodeEdge> const vNodeEdge);

    /**
     * set scenario control.
     *
     * @param[in] pControl: Scenario Control.
     *
     */
            MERROR                  setScenarioControl(android::sp<IScenarioControl> const pControl);

    /**
     * create PipelineContext, should be called after all setXXX functions.
     *
     * @param[in] mgr: Legacy Pipeline Manager.
     *
     * @param[out] pipelineId: pipeline id.
     *
     * @param[out] legacyPipeline: Legacy Pipeline.
     *
     */
            MERROR                  create(sp<LegacyPipelineManager> const mgr, MINT32& pipelineId, wp<ILegacyPipeline>& legacyPipeline);

protected:  ////                    Interface for self use.
    /**
     * get IMetaStreamInfo from stream id.
     *
     * @param[in] streamId: stream id.
     *
     */
            sp<IMetaStreamInfo>     getMetaStreamInfo(StreamId_T const streamId);

    /**
     * get IImageStreamInfo from stream id.
     *
     * @param[in] streamId: stream id.
     *
     */
            sp<IImageStreamInfo>    getImageStreamInfo(StreamId_T const streamId);

    /**
     * get Stream Buffer Provider from stream id.
     *
     * @param[in] streamId: stream id.
     *
     * @param[in] spvStreamBufferProvider: Stream Buffer Providers.
     *
     */
            sp<StreamBufferProvider>    getStreamBufferProvider(StreamId_T const streamId, vector<sp<StreamBufferProvider> > const spvStreamBufferProvider);

    /**
     * get StreamType of ImageStream from stream id.
     *
     * @param[in] streamId: stream id.
     *
     */
            MINT32                      getImageStreamType(StreamId_T const streamId);

    /**
     * check pipeline builder setting.
     *
     */
            MERROR                  checkSetting();

    /**
     * build node.
     *
     * @param[in] setting: node setting.
     *
     * @param[in] cfgParam: node config parameter.
     *
     * @param[out] spPipelineContext: Pipeline Context for building.
     *
     */
            template <class _Node_>
            MERROR                  buildNode(NodeSetting const setting, typename _Node_::ConfigParams* const cfgParam, sp<PipelineContext> spPipelineContext);

    /**
     * build P1Node config parameter.
     *
     * @param[in] setting: node setting.
     *
     * @param[in] spPipelineContext: pipeline context.
     *
     * @param[out] cfgParam: P1Node config parameter.
     *
     */
            MVOID                   buildP1NodeConfig(NodeSetting const setting, sp<PipelineContext> const spPipelineContext, P1Node::ConfigParams* cfgParam);

    /**
     * build P2Node config parameter.
     *
     * @param[in] setting: node setting.
     *
     * @param[out] cfgParam: P2Node config parameter.
     *
     */
            MVOID                   buildP2NodeConfig(NodeSetting const setting, P2Node::ConfigParams* cfgParam);

    /**
     * build JpegNode config parameter.
     *
     * @param[in] setting: node setting.
     *
     * @param[out] cfgParam: JpegNode config parameter.
     *
     */
            MVOID                   buildJpegNodeConfig(NodeSetting const setting, JpegNode::ConfigParams* cfgParam);

protected:
    char*                           mPipeName;
    MINT32                          mSensorId;
    PipelineSensorParam             mSensorParam;
    vector<sp<IMetaStreamInfo> >    mspvAppMetaStreamInfo;
    vector<sp<IMetaStreamInfo> >    mspvHalMetaStreamInfo;
    vector<ImageStreamUsage>        mvImageStreamUsage;
    vector<NodeSetting>             mvNodeSetting;
    MINT32                          mRootNodeId;
    vector<NodeEdge>                mvNodeEdge;
    sp<IScenarioControl>            mspScenarioControl;
};
}; //namespace NSLegacyPipeline
}; //namespace v1
}; //namespace NSCam
#endif
