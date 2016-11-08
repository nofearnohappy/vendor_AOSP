WHAT IT DOES?
=============
libsensorhubservice.so is a MediaTek developed sensorhub related module.
It implements native sensorhub services. Sensorhub service receives and handles 
all clients' sensorhub requests, such as setting up conditions and actions, 
canceling requests, updating conditions and so on. 
It also dispatches messages parsed out from md32 messages to the right client.  


HOW IT WAS BUILT?
==================
It is not built if the feature option 'MTK_SENSOR_HUB_SUPPORT' is not enabled.
To enable it, add 'MTK_SENSOR_HUB_SUPPORT = yes' in corresponding project 
configuration file ProjectConfig.mk.


HOW TO USE IT?
==============
It is started by the system_server in the method android_server_SystemServer_nativeInit
of file frameworks/base/services/core/jni/com_android_server_SystemServer.cpp.
It is only started when the system property 'ro.mtk_sensorhub_support' equals to 1.
The property should and only be configurated to 1 when the feature option 
'MTK_SENSOR_HUB_SUPPORT' is enabled.
