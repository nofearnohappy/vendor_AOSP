#ifeq ($(strip $(MTK_AUTO_TEST)), yes)
	LOCAL_PATH:= $(call my-dir)
	include $(CLEAR_VARS)
    # We only want this apk build for tests.
	LOCAL_MODULE_TAGS := tests
    # Include all test java files.
	LOCAL_SRC_FILES := $(call all-java-files-under, src)

	LOCAL_PACKAGE_NAME := DataTransferTests

	LOCAL_JAVA_LIBRARIES := android.test.runner #robotium	#just for auto test case
	
	#Add for juint report
    LOCAL_STATIC_JAVA_LIBRARIES := libjunitreport-for-datatransfer-tests librobotium4
    #End add

	LOCAL_INSTRUMENTATION_FOR := DataTransfer #just for auto test case

	LOCAL_CERTIFICATE := platform


	include $(BUILD_PACKAGE)

	include $(call all-makefiles-under,$(LOCAL_PATH))
	
	#Add for junit report
    include $(CLEAR_VARS)
    LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libjunitreport-for-datatransfer-tests:android-junit-report-1.2.6.jar
    include $(BUILD_MULTI_PREBUILT)
    #Edd add

#endif
