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

#ifndef _MTKCAM_HWNODE_UTILIIIES_H_
#define _MTKCAM_HWNODE_UTILIIIES_H_

#include <common.h>
#include <cutils/properties.h>
//
//
#include <sys/prctl.h>
#include <sys/resource.h>
#include <system/thread_defs.h>
//
//#include <utils/RWLock.h>
//#include <utils/Thread.h>
//
//#include <UITypes.h>
//
#include <IHal3A.h>
#if (USING_3A_SIMULATOR)
#include <drv/isp_reg.h>
#include <mtk_platform_metadata_tag.h>
#include <metadata/client/mtk_metadata_tag.h>
#include <metadata/IMetadataProvider.h>
#if (USING_3A_SIMULATOR_SOF)
#include <iopipe/CamIO/INormalPipe.h>
#endif
#endif

/******************************************************************************
 *
 ******************************************************************************/
namespace NSCam {
namespace v3 {

// utilities for crop

inline MINT32 div_round(MINT32 const numerator, MINT32 const denominator) {
    return (( numerator < 0 ) ^ (denominator < 0 )) ?
        (( numerator - denominator/2)/denominator) : (( numerator + denominator/2)/denominator);
}

struct vector_f //vector with floating point
{
    MPoint  p;
    MPoint  pf;

                                vector_f(
                                        MPoint const& rP = MPoint(),
                                        MPoint const& rPf = MPoint()
                                        )
                                    : p(rP)
                                    , pf(rPf)
                                {}
};

struct simpleTransform
{
    // just support translation than scale, not a general formulation
    // translation
    MPoint    tarOrigin;
    // scale
    MSize     oldScale;
    MSize     newScale;

                                simpleTransform(
                                        MPoint rOrigin = MPoint(),
                                        MSize  rOldScale = MSize(),
                                        MSize  rNewScale = MSize()
                                        )
                                    : tarOrigin(rOrigin)
                                    , oldScale(rOldScale)
                                    , newScale(rNewScale)
                                {}
};

// transform MPoint
inline MPoint transform(simpleTransform const& trans, MPoint const& p) {
    return MPoint(
            div_round( (p.x - trans.tarOrigin.x) * trans.newScale.w, trans.oldScale.w),
            div_round( (p.y - trans.tarOrigin.y) * trans.newScale.h, trans.oldScale.h)
            );
};

inline MPoint inv_transform(simpleTransform const& trans, MPoint const& p) {
    return MPoint(
            div_round( p.x * trans.oldScale.w, trans.newScale.w) + trans.tarOrigin.x,
            div_round( p.y * trans.oldScale.h, trans.newScale.h) + trans.tarOrigin.y
            );
};

inline int int_floor(float x) {
    int i = (int)x;
    return i - (i > x);
}

// transform vector_f
inline vector_f transform(simpleTransform const& trans, vector_f const& p) {
    MFLOAT const x = (p.p.x + (p.pf.x/(MFLOAT)(1u<<31))) * trans.newScale.w / trans.oldScale.w;
    MFLOAT const y = (p.p.y + (p.pf.y/(MFLOAT)(1u<<31))) * trans.newScale.h / trans.oldScale.h;
    int const x_int = int_floor(x);
    int const y_int = int_floor(y);
    return vector_f(
            MPoint(x_int, y_int),
            MPoint((x - x_int) * (1u<<31), (y - y_int) * (1u<<31))
            );
};

inline vector_f inv_transform(simpleTransform const& trans, vector_f const& p) {
    MFLOAT const x = (p.p.x + (p.pf.x/(MFLOAT)(1u<<31))) * trans.oldScale.w / trans.newScale.w;
    MFLOAT const y = (p.p.y + (p.pf.y/(MFLOAT)(1u<<31))) * trans.oldScale.h / trans.newScale.h;
    int const x_int = int_floor(x);
    int const y_int = int_floor(y);
    return vector_f(
            MPoint(x_int, y_int),
            MPoint((x - x_int) * (1u<<31), (y - y_int) * (1u<<31))
            );
};

// transform MSize
inline MSize transform(simpleTransform const& trans, MSize const& s) {
    return MSize(
            div_round( s.w * trans.newScale.w, trans.oldScale.w),
            div_round( s.h * trans.newScale.h, trans.oldScale.h)
            );
};

inline MSize inv_transform(simpleTransform const& trans, MSize const& s) {
    return MSize(
            div_round( s.w * trans.oldScale.w, trans.newScale.w),
            div_round( s.h * trans.oldScale.h, trans.newScale.h)
            );
};

// transform MRect
inline MRect transform(simpleTransform const& trans, MRect const& r) {
    return MRect(transform(trans, r.p), transform(trans, r.s));
};

inline MRect inv_transform(simpleTransform const& trans, MRect const& r) {
    return MRect(inv_transform(trans, r.p), inv_transform(trans, r.s));
};


/******************************************************************************
 *  Metadata Access
 ******************************************************************************/
template <typename T>
inline MBOOL
tryGetMetadata(
    IMetadata const* pMetadata,
    MUINT32 const tag,
    T & rVal
)
{
    if (pMetadata == NULL) {
        CAM_LOGW("pMetadata == NULL");
        return MFALSE;
    }
    //
    IMetadata::IEntry entry = pMetadata->entryFor(tag);
    if(!entry.isEmpty()) {
        rVal = entry.itemAt(0, Type2Type<T>());
        return MTRUE;
    }
    //
    return MFALSE;
}

template <typename T>
inline MBOOL
trySetMetadata(
    IMetadata* pMetadata,
    MUINT32 const tag,
    T const& val
)
{
    if (pMetadata == NULL) {
        CAM_LOGW("pMetadata == NULL");
        return MFALSE;
    }
    //
    IMetadata::IEntry entry(tag);
    entry.push_back(val, Type2Type<T>());
    if (OK == pMetadata->update(tag, entry)) {
        return MTRUE;
    }
    //
    return MFALSE;
}

/******************************************************************************
 *  Simulate HAL 3A.
 ******************************************************************************/

using namespace NS3Av3;
using namespace android;

#if (USING_3A_SIMULATOR)

#ifdef HAL3A_SIMULATOR_REAL_SOF
#undef HAL3A_SIMULATOR_REAL_SOF
#endif
#if (USING_3A_SIMULATOR_SOF) // #if (0) // for force disable REAL_SOF
#define HAL3A_SIMULATOR_REAL_SOF (1)
#else
#define HAL3A_SIMULATOR_REAL_SOF (0)
#endif

#ifdef LDVT_TIMING_FACTOR
#undef LDVT_TIMING_FACTOR
#endif
#ifdef USING_MTK_LDVT
#define LDVT_TIMING_FACTOR 4 //(1 * 30) for 1sec
#else
#define LDVT_TIMING_FACTOR 1
#endif


class IHal3ASimulator
{

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
protected:  //    Ctor/Dtor.
                        IHal3ASimulator(){};
    virtual             ~IHal3ASimulator(){};

private: // disable copy constructor and copy assignment operator
                        IHal3ASimulator(const IHal3ASimulator&);
    IHal3A&             operator=(const IHal3ASimulator&);

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Interfaces.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public:
    #if 0
    /**
     * @brief Create instance of IHal3A
     * @param [in] eVersion, E_Camera_1, E_Camera_3
     * @param [in] i4SensorIdx
     */
    static IHal3ASimulator*      createInstance(
        IHal3A::E_VER eVersion, MINT32 const i4SensorIdx, const char* strUser);
    #endif

    /**
     * @brief destroy instance of IHal3A
     */
    virtual MVOID       destroyInstance(const char* /*strUser*/) {}

    /**
     * @brief start 3A
     */
    virtual MINT32      start(MINT32 i4StartNum=0) = 0;

    /**
     * @brief stop 3A
     */
    virtual MINT32      stop() = 0;

    // interfaces for metadata processing
    /**
     * @brief Set list of controls in terms of metadata via IHal3A
     * @param [in] controls list of MetaSet_T
     */
    virtual MINT32      set(const List<MetaSet_T>& controls) = 0;

    /**
     * @brief Set pass2 tuning in terms of metadata via IHal3A
     * @param [in] flowType 0 for processed raw, 1 for pure raw
     * @param [in] control MetaSet_T
     * @param [out] pRegBuf buffer address for register setting
     * @param [out] result IMetadata
     */
    virtual MINT32      setIsp(
        MINT32 flowType, const MetaSet_T& control,
        void* pRegBuf, MetaSet_T* pResult) = 0;

    /**
     * @brief Get dynamic result with specified frame ID via IHal3A
     * @param [in] frmId specified frame ID (magic number)
     * @param [out] result in terms of metadata
     */
    //virtual MINT32      get(MUINT32 frmId, IMetadata&  result) = 0;
    virtual MINT32      get(MUINT32 frmId, MetaSet_T& result) = 0;

    /**
     * @brief Attach callback for notifying
     * @param [in] eId Notification message type
     * @param [in] pCb Notification callback function pointer
     */
    virtual MINT32      attachCb(IHal3ACb::ECb_T eId, IHal3ACb* pCb) = 0;

    /**
     * @brief Dettach callback
     * @param [in] eId Notification message type
     * @param [in] pCb Notification callback function pointer
     */
    virtual MINT32      detachCb(IHal3ACb::ECb_T eId, IHal3ACb* pCb) = 0;

    /**
     * @brief Get capacity of metadata list via IHal3A
     * @return
     * - MINT32 value of capacity.
     */
    virtual MINT32      getCapacity() const = 0;

    /**
     * @brief set sensor mode
     * @param [in] i4SensorMode
     */
    virtual MVOID       setSensorMode(MINT32 i4SensorMode) = 0;

    virtual MVOID       notifyP1Done(
        MUINT32 u4MagicNum, MVOID* pvArg = 0) = 0;

};

class Hal3ASimulator
    : public IHal3ASimulator//IHal3A
{

public:
    Hal3ASimulator(
        IHal3A::E_VER eVersion,
        MINT32 const i4SensorIdx,
        const char* strUser)
        : IHal3ASimulator()//IHal3A()
        , mLogLevel(0)
        , mUserName(strUser)
        , mSensorIdx(i4SensorIdx)
        , mVer(eVersion)
        , mCapacity(3)
        , mNumQueueLock()
        , mCallbackLock()
        , mCallbackCond()
        , mCallbackEnable(MFALSE)
        , mLastCallbackTime(0)
        , mCallbackTimeIntervalUs(33333 * LDVT_TIMING_FACTOR)
        #if HAL3A_SIMULATOR_REAL_SOF
        , mpCamIO(NULL)
        , mUserKey(0)
        #endif
        {
            char cLogLevel[PROPERTY_VALUE_MAX] = {0};
            ::property_get("debug.camera.log", cLogLevel, "0");
            mLogLevel = ::atoi(cLogLevel);
            #if 1/*[EP_TEMP]*/ //[FIXME] TempTestOnly
            #warning "[FIXME] force enable Hal3ASimulator log"
            if (mLogLevel < 2) {
                mLogLevel = 2;
            }
            #endif
            //
            {
                Mutex::Autolock _l(mCallbackLock);
                for (int i = 0; i < IHal3ACb::eID_MSGTYPE_NUM; i++) {
                    mCbSet[i] = NULL;
                }
            }
            mvNumQueue.clear();
            mpCallbackThread =  new CallbackThread(this);
            CAM_LOGI("Hal3ASimulator(%d, %d, %s) LogLevel(%d)",
                eVersion, i4SensorIdx, strUser, mLogLevel);
        };

    Hal3ASimulator()
    {
        CAM_LOGD("Hal3ASimulator()");
    };
    virtual ~Hal3ASimulator()
    {
        CAM_LOGD("~Hal3ASimulator()");
    };

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  IHal3A Interface.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    //
    /**
     * @brief Create instance of IHal3A
     * @param [in] eVersion, E_Camera_1, E_Camera_3
     * @param [in] i4SensorIdx
     */
    static /*IHal3A*/Hal3ASimulator*      createInstance(
                            IHal3A::E_VER eVersion,
                            MINT32 const i4SensorIdx,
                            const char* strUser)
    {
        if (eVersion == IHal3A::E_Camera_3) {
            return new Hal3ASimulator(eVersion, i4SensorIdx, strUser);
        }
        return NULL;
    };

    /**
     * @brief destroy instance of IHal3A
     */
    virtual MVOID       destroyInstance(const char* strUser)
    {
        if (mUserName == strUser) {
            CAM_LOGD_IF(mLogLevel >= 2, "[%s] 3A simulator destroy user[%s] ",
                mUserName.string(), strUser);
            delete this;
        } else {
            CAM_LOGW("[%s] 3A simulator user[%s] not found",
                mUserName.string(), strUser);
        }
    };

    /**
     * @brief start 3A
     */
    virtual MINT32      start(MINT32 i4StartNum=0)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator start (%d)",
            mUserName.string(), i4StartNum);
        #if HAL3A_SIMULATOR_REAL_SOF
        if (mpCamIO == NULL) {
            #ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly
            mpCamIO = NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::
                        createInstance(mSensorIdx, "iopipeUseTM3AS");
            #else
            mpCamIO = NSCam::NSIoPipe::NSCamIOPipe::INormalPipe::
                        createInstance(mSensorIdx, mUserName.string());
            #endif
            //
            if (mpCamIO == NULL) {
                CAM_LOGW("[%s] CamIO create fail", mUserName.string());
                return 0;
            }
            mUserKey = mpCamIO->attach("S3A_CB");
        }
        #endif
        mLastCallbackTime = 0;
        mpCallbackThread->run("Hal3ASimulatorCallback");
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator thread start",
            mUserName.string());
        return 0;
    };

    /**
     * @brief stop 3A
     */
    virtual MINT32      stop()
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator stop",
            mUserName.string());
        {
            Mutex::Autolock _l(mCallbackLock);
            mCallbackEnable = MFALSE;
        }
        mpCallbackThread->requestExit();
        mpCallbackThread->join();
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator thread stop",
            mUserName.string());
        //
        #if HAL3A_SIMULATOR_REAL_SOF
        if (mpCamIO != NULL) {
            #ifdef USING_MTK_LDVT /*[EP_TEMP]*/ //[FIXME] TempTestOnly
            mpCamIO->destroyInstance("iopipeUseTM");
            #else
            mpCamIO->destroyInstance(mUserName.string());
            #endif
            mpCamIO = NULL;
        }
        #endif
        return 0;
    };

    // interfaces for metadata processing
    /**
     * @brief Set list of controls in terms of metadata via IHal3A
     * @param [in] controls list of MetaSet_T
     */
    virtual MINT32      set(const List<MetaSet_T>& controls)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator set",
            mUserName.string());
        return setMetaList(controls);
    };

    /**
     * @brief Set pass2 tuning in terms of metadata via IHal3A
     * @param [in] flowType 0 for processed raw, 1 for pure raw
     * @param [in] control MetaSet_T
     * @param [out] pRegBuf buffer address for register setting
     * @param [out] result IMetadata
     */
    virtual MINT32      setIsp(
                            MINT32 flowType,
                            const MetaSet_T& control,
                            void* pRegBuf,
                            MetaSet_T* pResult)
    {
        unsigned int tuningsize = sizeof(dip_x_reg_t);
        CAM_LOGD_IF(mLogLevel >= 1,
            "[%s] 3A simulator setIsp type(%d) reg(%p) size(%d) result(%p)",
            mUserName.string(), flowType, pRegBuf, tuningsize, (void*)pResult);
        #if 0 // clear tuning and return 0
        ::memset((unsigned char*)(pRegBuf), 0, tuningsize);
        if (pResult != NULL) {
            pResult->appMeta = control.appMeta;
            pResult->halMeta = control.halMeta;
        }
        #else
        CAM_LOGD_IF(mLogLevel >= 1, "setIsp - skip action");
        return (-1);
        #endif
        return 0;
    };

    /**
     * @brief Get dynamic result with specified frame ID via IHal3A
     * @param [in] frmId specified frame ID (magic number)
     * @param [out] result in terms of metadata
     */
    //virtual MINT32      get(MUINT32 frmId, IMetadata&  result) = 0;
    virtual MINT32      get(MUINT32 frmId, MetaSet_T& /*result*/)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator get frmId (%d)",
            mUserName.string(), frmId);
        return 0;
    };
    #if 0 // from IHal3A
    virtual MINT32      getCur(MUINT32 frmId, MetaSet_T& /*result*/)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator getCur frmId (%d)",
            mUserName.string(), frmId);
        return 0;
    };
    #endif

    /**
     * @brief Attach callback for notifying
     * @param [in] eId Notification message type
     * @param [in] pCb Notification callback function pointer
     */
    virtual MINT32      attachCb(
                            IHal3ACb::ECb_T eId,
                            IHal3ACb* pCb)
    {
        Mutex::Autolock _l(mCallbackLock);
        if (eId < IHal3ACb::eID_MSGTYPE_NUM) {
            mCbSet[eId] = pCb;
        } else {
            CAM_LOGW("[%s] 3A simulator can not attachCb at [%d]",
                mUserName.string(), eId);
        }
        return 0;
    };

    /**
     * @brief Dettach callback
     * @param [in] eId Notification message type
     * @param [in] pCb Notification callback function pointer
     */
    virtual MINT32      detachCb(
                            IHal3ACb::ECb_T eId,
                            IHal3ACb* pCb)
    {
        Mutex::Autolock _l(mCallbackLock);
        if (eId < IHal3ACb::eID_MSGTYPE_NUM) {
            if (mCbSet[eId] == pCb) {
                mCbSet[eId] = NULL;
            } else {
                CAM_LOGW("[%s] 3A simulator can not found detachCb %p at [%d]",
                mUserName.string(), (void*)pCb, eId);
            }
        } else {
            CAM_LOGW("[%s] 3A simulator can not detachCb at [%d]",
                mUserName.string(), eId);
        }
        return 0;
    };

    #if 0 // from IHal3A
    /**
     * @brief Get delay frames via IHal3A
     * @param [out] delay_info in terms of metadata with MTK defined tags.
     */
    virtual MINT32      getDelay(IMetadata& /*delay_info*/) const
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator getDelay",
            mUserName.string());
        return 0;
    };

    /**
     * @brief Get delay frames via IHal3A
     * @param [in] tag belongs to control+dynamic
     * @return
     * - MINT32 delay frame.
     */
    virtual MINT32      getDelay(MUINT32 tag) const
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator getDelay tag (%d)",
            mUserName.string(), tag);
        return 0;
    };
    #endif

    /**
     * @brief Get capacity of metadata list via IHal3A
     * @return
     * - MINT32 value of capacity.
     */
    virtual MINT32      getCapacity() const
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator getCapacity (%d)",
            mUserName.string(), mCapacity);
        return mCapacity;
    };

    #if 0 // from IHal3A
    virtual MINT32      send3ACtrl(
                            E3ACtrl_T e3ACtrl,
                            MINTPTR i4Arg1,
                            MINTPTR i4Arg2)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator send3ACtrl (0x%x) %d %d",
            mUserName.string(), e3ACtrl, i4Arg1, i4Arg2);
        return 0;
    };
    #endif

    /**
     * @brief set sensor mode
     * @param [in] i4SensorMode
     */
    virtual MVOID       setSensorMode(MINT32 i4SensorMode)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator setSensorMode (0x%x)",
            mUserName.string(), i4SensorMode);
        return;
    };

    virtual MVOID       notifyP1Done(MUINT32 u4MagicNum, MVOID* pvArg = 0)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator notifyP1Done (%d) %p",
            mUserName.string(), u4MagicNum, pvArg);
        return;
    };

    #if 0 // from IHal3A
    virtual MVOID       setFDEnable(MBOOL fgEnable)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator setFDEnable (%d)",
            mUserName.string(), fgEnable);
        return;
    };

    virtual MBOOL       setFDInfo(MVOID* prFaces)
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator setFDInfo (%p)",
            mUserName.string(), prFaces);
        return MFALSE;
    };


    #if 1 // temp for IHal3A in v3
    /**
     * @brief notify sensor power on
     */
    virtual MBOOL       notifyPwrOn()
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator notifyPwrOn",
            mUserName.string());
        return MFALSE;
    };
    /**
     * @brief notify sensor power off
     */
    virtual MBOOL       notifyPwrOff()
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator notifyPwrOff",
            mUserName.string());
        return MFALSE;
    };
    /**
     * @brief check whether flash on while capture
     */
    virtual MBOOL       checkCapFlash()
    {
        CAM_LOGD_IF(mLogLevel >= 1, "[%s] 3A simulator checkCapFlash",
            mUserName.string());
        return MFALSE;
    };
    #endif
    #endif

    class CallbackThread
        : public Thread
    {

    public:

        CallbackThread(Hal3ASimulator* pHal3ASimulatorImp)
            : mpSim3A(pHal3ASimulatorImp)
        {};

        ~CallbackThread()
        {};

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //  Thread Interface.
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    public:
        virtual status_t    readyToRun()
        {
            CAM_LOGD("[%s] readyToRun callback thread",
                mpSim3A->mUserName.string());

            // set name
            ::prctl(PR_SET_NAME, (unsigned long)"Cam@3ASimulator", 0, 0, 0);
            // set normal
            struct sched_param sched_p;
            sched_p.sched_priority = 0;
            ::sched_setscheduler(0, (SCHED_OTHER), &sched_p);
            ::setpriority(PRIO_PROCESS, 0, ANDROID_PRIORITY_DISPLAY);
                                            //  Note: "priority" is nice value.
            //
            ::sched_getparam(0, &sched_p);
            CAM_LOGD(
                "Tid: %d, policy: %d, priority: %d"
                , ::gettid(), ::sched_getscheduler(0)
                , sched_p.sched_priority
            );
            //
            return OK;
        };
    private:
        virtual bool        threadLoop()
        {
            return callbackLoop();
        };

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //  Data Member.
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private:
        Hal3ASimulator* mpSim3A;

    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    //  Function Member.
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private:
        MBOOL callbackLoop()
        {
            CAM_LOGD_IF(mpSim3A->mLogLevel >= 2, "enter callback thread");
            if (mpSim3A == NULL) {
                CAM_LOGD("caller not exist - exit callback thread");
                return MFALSE;
            }
            if (!exitPending()) {
                CAM_LOGD_IF(mpSim3A->mLogLevel >= 2, "go-on callback thread");
                MINT64 current_time = 0;
                struct timeval tv;
                //
                MINT32 num = 0;
                MINT32 idx = 0;
                //
                gettimeofday(&tv, NULL);
                current_time = tv.tv_sec * 1000000 + tv.tv_usec;
                {
                    Mutex::Autolock _l(mpSim3A->mCallbackLock);
                    if (!mpSim3A->mCallbackEnable) {
                        CAM_LOGD("CB wait+");
                        mpSim3A->mCallbackCond.wait(mpSim3A->mCallbackLock);
                        CAM_LOGD("CB wait-");
                        mpSim3A->mLastCallbackTime = current_time;
                        return MTRUE;
                    }
                }
                //
                #if HAL3A_SIMULATOR_REAL_SOF
                if (mpSim3A->mpCamIO != NULL) {
                    CAM_LOGD_IF(mpSim3A->mLogLevel >= 2,
                        "CB wait SOF +++ (%lld)", current_time);
                    mpSim3A->mpCamIO->wait(
                        NSCam::NSIoPipe::NSCamIOPipe::EPipeSignal_SOF,
                        NSCam::NSIoPipe::NSCamIOPipe::EPipeSignal_ClearWait,
                        mpSim3A->mUserKey,
                        mpSim3A->mCallbackTimeIntervalUs);
                    if (mpSim3A->mLogLevel >= 2) {
                        gettimeofday(&tv, NULL);
                        current_time = tv.tv_sec * 1000000 + tv.tv_usec;
                    }
                    CAM_LOGD_IF(mpSim3A->mLogLevel >= 2,
                        "CB wait SOF --- (%lld)", current_time);
                    num = mpSim3A->deQueueNum();
                    if (num == -1) {
                        num = 0;
                    }
                    //
                    #if 1
                    // after the SOF arrival,
                    // it might need to take some time for this calculation
                    // then, it can perform callback
                    // for simulation, adjust the sleep time
                    usleep(mpSim3A->mCallbackTimeIntervalUs >> 4);
                    #endif
                    //
                    mpSim3A->mpCamIO->sendCommand(
                        NSCam::NSIoPipe::NSCamIOPipe::ENPipeCmd_GET_CUR_SOF_IDX,
                        (MINTPTR)&idx, 0, 0);
                    //
                    mpSim3A->performCallback(num, idx);
                    //
                }
                #else
                if ((current_time - mpSim3A->mLastCallbackTime) >
                    mpSim3A->mCallbackTimeIntervalUs) {

                    mpSim3A->mLastCallbackTime = current_time;
                    CAM_LOGD_IF(mpSim3A->mLogLevel >= 2,
                        "Current CB Thread Time = %lld",
                        mpSim3A->mLastCallbackTime);
                    num = mpSim3A->deQueueNum();
                    if (num == -1) {
                        num = 0;
                    }
                    //
                    mpSim3A->performCallback(num, idx);
                    //
                } else {
                    CAM_LOGD_IF(mpSim3A->mLogLevel >= 2,
                        "CB Thread Time = %lld / %lld",
                        mpSim3A->mLastCallbackTime, current_time);
                    // next time for check, the time interval can be adjusted
                    usleep(mpSim3A->mCallbackTimeIntervalUs >> 4);
                }
                #endif
                return MTRUE;
            }
            CAM_LOGD("exit callback thread");
            return MFALSE;
        };

    };

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Data Member.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:

    MINT32              mLogLevel;
    String8             mUserName;
    MINT32              mSensorIdx;
    IHal3A::E_VER       mVer;
    IHal3ACb*           mCbSet[IHal3ACb::eID_MSGTYPE_NUM];
    MINT32              mCapacity;
    //
    mutable Mutex       mNumQueueLock;
    Vector<MINT32>      mvNumQueue;
    //
    mutable Mutex       mCallbackLock;
    Condition           mCallbackCond;
    MBOOL               mCallbackEnable;
    //
    MINT64              mLastCallbackTime;
    MINT64              mCallbackTimeIntervalUs;
    //
    sp<CallbackThread>  mpCallbackThread;
    //
#if HAL3A_SIMULATOR_REAL_SOF
    NSCam::NSIoPipe::NSCamIOPipe::INormalPipe*        mpCamIO;
    MINT32              mUserKey;
#endif

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//  Function Member.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
private:
    void performCallback(MINT32 num, MINT32 idx)
    {
        CAM_LOGD_IF(mLogLevel >= 2, "CB NUM(%d) IDX(%d)", num, idx);
        // callback if exist
        for (int i = 0; i < IHal3ACb::eID_MSGTYPE_NUM; i++) {
            IHal3ACb* pCb = NULL;
            {
                Mutex::Autolock _l(mCallbackLock);
                pCb = mCbSet[i];
            }
            if (pCb != NULL) {
                CAM_LOGD_IF(mLogLevel >= 2,
                    "do notify [%d] %d %d +++", i, num, idx);
                //
                pCb->doNotifyCb(i, num, idx, 0);
                //
                CAM_LOGD_IF(mLogLevel >= 2,
                    "do notify [%d] %d %d ---", i, num, idx);
            }
        }
    };

    MINT32 setMetaList(const List<MetaSet_T>& controls)
    {
        List<MetaSet_T>::const_iterator it = controls.begin();
        MINT32 nMagicNum = 0;

        #ifdef HAL3A_SIMULATOR_REQ_PROC_KEY
        #undef HAL3A_SIMULATOR_REQ_PROC_KEY
        #endif
        #define HAL3A_SIMULATOR_REQ_PROC_KEY 2

        for (int i = 0; i < getCapacity() && it != controls.end(); i++, it++) {
            if (i == HAL3A_SIMULATOR_REQ_PROC_KEY) {
                if (!tryGetMetadata<MINT32>(&(it->halMeta),
                    MTK_P1NODE_PROCESSOR_MAGICNUM, nMagicNum)) {
                    CAM_LOGW("[%s] Set Meta List fail", mUserName.string());
                    break;
                }
                enQueueNum(nMagicNum);
            }
        }
        if (mLogLevel >= 2) {
            dumpQueueNum();
        }

        #undef HAL3A_SIMULATOR_REQ_PROC_KEY

        {
            Mutex::Autolock _l(mCallbackLock);
            if (!mCallbackEnable) {
                #if 0
                // after the first set,
                // it might need to take some time for the first calculation
                // then, it can start to callback
                // for simulation, adjust the sleep time
                usleep(mCallbackTimeIntervalUs / 2);
                #endif
                mCallbackEnable = MTRUE;
                mCallbackCond.broadcast();
            }
        }

        return nMagicNum;
    };

    void enQueueNum(MINT32 num)
    {
        Mutex::Autolock _l(mNumQueueLock);
        mvNumQueue.push_back(num);
        return;
    };

    MINT32 deQueueNum(void)
    {
        Mutex::Autolock _l(mNumQueueLock);
        MINT32 num = -1;
        if (mvNumQueue.size() > 0) {
            Vector<MINT32>::iterator it = mvNumQueue.begin();
            num = *(it);
            mvNumQueue.erase(it);
        }
        return num;
    };

    void dumpQueueNum(void)
    {
        Mutex::Autolock _l(mNumQueueLock);
        String8 str = String8::format("Q[%d] = { ", (int)(mvNumQueue.size()));
        Vector<MINT32>::iterator it = mvNumQueue.begin();
        for(; it != mvNumQueue.end(); it++) {
            str += String8::format(" %d ", (*it));
        }
        str += String8::format(" }");
        CAM_LOGD("%s", str.string());
        return;
    };

};

typedef IHal3ASimulator     IHal3A_T;
typedef Hal3ASimulator      IHal3AImp_T;

#else //(!USING_3A_SIMULATOR)

typedef IHal3A              IHal3A_T;
typedef IHal3A              IHal3AImp_T;

#endif



};
};

#endif //_MTKCAM_HWNODE_UTILIIIES_H_

