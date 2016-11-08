WHAT IT DOES?
=============
libsensorhub_jni.so is a MediaTek developed sensorhub related module.
It is a JNI library and it makes the java SensorHubService can call into 
the native SensorHubManager which is defined in libsensorhub.so module.
Java SensorHubService path: frameworks/base/core/java/com/mediatek/sensorhub/SensorHubService.java
libsensorhub.so path: vendor/mediatek/proprietary/frameworks/native/libs/sensorhub  


HOW IT WAS BUILT?
==================
It is not built if the feature option 'MTK_SENSOR_HUB_SUPPORT' is not enabled.
To enable it, add 'MTK_SENSOR_HUB_SUPPORT = yes' in corresponding project 
configuration file ProjectConfig.mk.


HOW TO USE IT?
==============
It is loaded into the JVM through the api System.loadLibrary("sensorhub_jni")
defined in a static block of SensorHubService.