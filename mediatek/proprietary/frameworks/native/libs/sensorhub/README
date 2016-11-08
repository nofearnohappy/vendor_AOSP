WHAT IT DOES?
=============
libsensorhub.so is a MediaTek developed sensorhub related module.
It defines sensorhub basic classes and data structures. 
It is a bridge between sensorhub service(libsensorhubservice.so) and 
sensorhub HAL module(libhwsensorhub.so).  


HOW IT WAS BUILT?
==================
It is not built if the feature option 'MTK_SENSOR_HUB_SUPPORT' is not enabled.
To enable it, add 'MTK_SENSOR_HUB_SUPPORT = yes' in corresponding project 
configuration file ProjectConfig.mk.


HOW TO USE IT?
==============
libsensorhub_jni.so and libsensorhubservice.so depend on this library.
Both libsensorhub_jni.so and libsensorhubservice.so declare it in their 
LOCAL_SHARED_LIBRARIES.