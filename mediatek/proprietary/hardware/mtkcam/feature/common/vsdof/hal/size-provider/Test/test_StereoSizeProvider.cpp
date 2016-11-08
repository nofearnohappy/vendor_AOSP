#include <limits.h>
#include <gtest/gtest.h>
#include <vsdof/hal/stereo_size_provider.h>

const double FE_RESIZE_RATIO = StereoSettingProvider::getStereoCameraFOVRatio();
const float PADDING_RATIO = StereoSettingProvider::getStereoCameraFOVRatio()-1.0f;

//=============================================================================
//  PASS 1
//=============================================================================
TEST(PASS1_TEST, PREVIEW_SCENRATIO)
{
    ENUM_STEREO_SCENARIO scenario = eSTEREO_SCENARIO_PREVIEW;
    int index1, index2;
    StereoSettingProvider::getStereoSensorIndex(index1, index2);

    StereoSizeProvider *sizeProvider = StereoSizeProvider::getInstance();
    StereoArea area = sizeProvider->getPass1RRZOSize(index1, scenario);
    EXPECT_TRUE(area.size == MSize(2304, 1296));

    area = sizeProvider->getPass1RRZOSize(index2, scenario);
    EXPECT_TRUE(area.size == MSize(1600, 900));
}

TEST(PASS1_TEST, RECORD_SCENRATIO)
{
    ENUM_STEREO_SCENARIO scenario = eSTEREO_SCENARIO_RECORD;
    int index1, index2;
    StereoSettingProvider::getStereoSensorIndex(index1, index2);

    StereoSizeProvider *sizeProvider = StereoSizeProvider::getInstance();
    StereoArea area = sizeProvider->getPass1RRZOSize(index1, eSTEREO_SCENARIO_RECORD);
    EXPECT_TRUE(area.size == MSize(2304, 1296));

    area = sizeProvider->getPass1RRZOSize(index2, scenario);
    EXPECT_TRUE(area.size == MSize(1600, 900));
}

TEST(PASS1_TEST, CAPTURE_SCENRATIO)
{
    ENUM_STEREO_SCENARIO scenario = eSTEREO_SCENARIO_CAPTURE;
    int index1, index2;
    StereoSettingProvider::getStereoSensorIndex(index1, index2);

    StereoSizeProvider *sizeProvider = StereoSizeProvider::getInstance();
    StereoArea area = sizeProvider->getPass1RRZOSize(index1, eSTEREO_SCENARIO_CAPTURE);
    EXPECT_TRUE(area.size == MSize(2304, 1296));

    area = sizeProvider->getPass1RRZOSize(index2, scenario);
    EXPECT_TRUE(area.size == MSize(1600, 900));

    EXPECT_TRUE(sizeProvider->getPass1IMGOSize() == MSize(4415, 2944));
}
//=============================================================================
//  PASS 2
//=============================================================================
TEST(PASS2_TEST, PREVIEW_SCENRATIO)
{
    ENUM_STEREO_SCENARIO scenario = eSTEREO_SCENARIO_PREVIEW;

    int index1, index2;
    StereoSettingProvider::getStereoSensorIndex(index1, index2);

    StereoSizeProvider *sizeProvider = StereoSizeProvider::getInstance();
    Pass2SizeInfo pass2SizeInfo;

    // PASS2A
    sizeProvider->getPass2SizeInfo(PASS2A, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA.size == MSize(1920, 1080));
    EXPECT_TRUE(pass2SizeInfo.szWROT == MSize(960, 544));
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(1600, 900));
    EXPECT_TRUE(pass2SizeInfo.szIMG2O.size == MSize(640, 320));

    // PASS2A'
    sizeProvider->getPass2SizeInfo(PASS2A_P, scenario, pass2SizeInfo);
    MSize newSize(960, 544);
    newSize.w *= FE_RESIZE_RATIO;
    newSize.h *= FE_RESIZE_RATIO;
    EXPECT_TRUE(pass2SizeInfo.szWROT == newSize);
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(1600, 900));

    // PASS2A-2
    sizeProvider->getPass2SizeInfo(PASS2A_2, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA.size == MSize(240, 136));
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == MSize(480, 272));
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(960, 544));

    // PASS2A'-2
    sizeProvider->getPass2SizeInfo(PASS2A_P_2, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA.size == MSize(240, 136));
    MSize szIMG2O(480, 272);
    szIMG2O.w *= FE_RESIZE_RATIO;
    szIMG2O.h *= FE_RESIZE_RATIO;
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == szIMG2O);
    EXPECT_TRUE(pass2SizeInfo.areaFEO == StereoArea(960*FE_RESIZE_RATIO, 544*FE_RESIZE_RATIO,
                                                    960*PADDING_RATIO,   544*PADDING_RATIO,
                                                    960*PADDING_RATIO/2, 544*PADDING_RATIO/2));

    // PASS2A-3
    sizeProvider->getPass2SizeInfo(PASS2A_3, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == MSize(128, 72));
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(480, 272));

    // PASS2A'-3
    sizeProvider->getPass2SizeInfo(PASS2A_P_3, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == MSize(128, 72));
    EXPECT_TRUE(pass2SizeInfo.areaFEO == StereoArea(480*FE_RESIZE_RATIO, 272*FE_RESIZE_RATIO,
                                                    480*PADDING_RATIO,   272*PADDING_RATIO,
                                                    480*PADDING_RATIO/2, 272*PADDING_RATIO/2));
}

TEST(PASS2_TEST, CAPTURE_SCENRATIO)
{
    ENUM_STEREO_SCENARIO scenario = eSTEREO_SCENARIO_CAPTURE;
    int index1, index2;
    StereoSettingProvider::getStereoSensorIndex(index1, index2);

    StereoSizeProvider *sizeProvider = StereoSizeProvider::getInstance();
    Pass2SizeInfo pass2SizeInfo;

    // PASS2A
    sizeProvider->getPass2SizeInfo(PASS2A, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA.size == MSize(3072, 1728));
    EXPECT_TRUE(pass2SizeInfo.szWROT == MSize(960, 544));
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(1600, 900));
    EXPECT_TRUE(pass2SizeInfo.szIMG2O.size == MSize(640, 320));

    // PASS2A'
    sizeProvider->getPass2SizeInfo(PASS2A_P, scenario, pass2SizeInfo);
    MSize newSize(960, 544);
    newSize.w *= FE_RESIZE_RATIO;
    newSize.h *= FE_RESIZE_RATIO;
    EXPECT_TRUE(pass2SizeInfo.szWROT == newSize);
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(1600, 900));

    // PASS2A-2
    sizeProvider->getPass2SizeInfo(PASS2A_2, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA == StereoArea(2176, 1152, 256, 72, 128, 36));
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == MSize(480, 272));
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(960, 544));

    // PASS2A'-2
    sizeProvider->getPass2SizeInfo(PASS2A_P_2, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA == StereoArea(2176, 1152, 256, 72, 128, 36));
    MSize szIMG2O(480, 272);
    szIMG2O.w *= FE_RESIZE_RATIO;
    szIMG2O.h *= FE_RESIZE_RATIO;
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == szIMG2O);
    EXPECT_TRUE(pass2SizeInfo.areaFEO == StereoArea(960*FE_RESIZE_RATIO, 544*FE_RESIZE_RATIO,
                                                    960*PADDING_RATIO,   544*PADDING_RATIO,
                                                    960*PADDING_RATIO/2, 544*PADDING_RATIO/2));

    // PASS2A-3
    sizeProvider->getPass2SizeInfo(PASS2A_3, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(480, 272));

    // PASS2A'-3
    sizeProvider->getPass2SizeInfo(PASS2A_P_3, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaFEO == StereoArea(480*FE_RESIZE_RATIO, 272*FE_RESIZE_RATIO,
                                                    480*PADDING_RATIO,   272*PADDING_RATIO,
                                                    480*PADDING_RATIO/2, 272*PADDING_RATIO/2));
}

TEST(PASS2_TEST, RECORD_SCENRATIO)
{
    ENUM_STEREO_SCENARIO scenario = eSTEREO_SCENARIO_RECORD;
    int index1, index2;
    StereoSettingProvider::getStereoSensorIndex(index1, index2);

    StereoSizeProvider *sizeProvider = StereoSizeProvider::getInstance();
    Pass2SizeInfo pass2SizeInfo;

    // PASS2A
    sizeProvider->getPass2SizeInfo(PASS2A, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA.size == MSize(1920, 1080));
    EXPECT_TRUE(pass2SizeInfo.szWROT == MSize(960, 544));
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == MSize(640, 360));
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(1600, 900));
    EXPECT_TRUE(pass2SizeInfo.szIMG2O.size == MSize(640, 320));

    // PASS2A'
    sizeProvider->getPass2SizeInfo(PASS2A_P, scenario, pass2SizeInfo);
    MSize newSize(960, 544);
    newSize.w *= FE_RESIZE_RATIO;
    newSize.h *= FE_RESIZE_RATIO;
    EXPECT_TRUE(pass2SizeInfo.szWROT == newSize);
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(1600, 900));

    // PASS2A-2
    sizeProvider->getPass2SizeInfo(PASS2A_2, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA.size == MSize(288, 162));
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == MSize(480, 272));
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(960, 544));

    // PASS2A'-2
    sizeProvider->getPass2SizeInfo(PASS2A_P_2, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaWDMA.size == MSize(288, 162));
    MSize szIMG2O(480, 272);
    szIMG2O.w *= FE_RESIZE_RATIO;
    szIMG2O.h *= FE_RESIZE_RATIO;
    EXPECT_TRUE(pass2SizeInfo.szIMG2O == szIMG2O);
    EXPECT_TRUE(pass2SizeInfo.areaFEO == StereoArea(960*FE_RESIZE_RATIO, 544*FE_RESIZE_RATIO,
                                                    960*PADDING_RATIO,   544*PADDING_RATIO,
                                                    960*PADDING_RATIO/2, 544*PADDING_RATIO/2));

    // PASS2A-3
    sizeProvider->getPass2SizeInfo(PASS2A_3, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaFEO.size == MSize(480, 272));

    // PASS2A'-3
    sizeProvider->getPass2SizeInfo(PASS2A_P_3, scenario, pass2SizeInfo);
    EXPECT_TRUE(pass2SizeInfo.areaFEO == StereoArea(480*FE_RESIZE_RATIO, 272*FE_RESIZE_RATIO,
                                                    480*PADDING_RATIO,   272*PADDING_RATIO,
                                                    480*PADDING_RATIO/2, 272*PADDING_RATIO/2));
}