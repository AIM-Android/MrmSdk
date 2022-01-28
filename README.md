# MRM SDK
The MRM(Mobile Resource Management) SDK is a set of software libraries which provides APIs for controlling various functions of the target device. 

## Project structure
The MRM SDK package contains the following contents:  
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/package_contents.png)

The description of each of the folder at the top level is listed below:
| Files/Directories | Description |
| ----------------- | ----------- |
|bin/library/|The Java library and native library files.<br>These libraries should be imported in to your APP project.|
|bin/service/|The MRM service APK file.<br>The service APK file must be installed into your device before running your APP or prebuilt sample APPs.|
|bin/prebuilt_sample/|The prebuilt APK files of sample codes.|
|doc|The Documents.|
|samples|The sample code.|


## Quick Start
1. Install MRM Services (mrm_service.apk)  
**( NOTE: This step is only necessary for IVCP and SDP function. You can skip this step if you only need VCIL functions )**
Connect device to you computer with ADB.  Execute the following ADB command:

````shell
adb install -r ./bin/service/mrm_service.apk
````

After installed, you will get the following package in your devices
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/mrm_service.png)

There will also be an MRM Service Console APP named "MRM" in the APP list. This is a utility for testing MRM Services and checking the basic information.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/mrm_service_app.png)

![](https://github.com/AIM-Android/MrmSdk/blob/main/images/mrm_control_panel.png)

When MRM is launched, it will try to bind all MRM services. The MRM Services will be started and initialize related hardware resources. 
If initialization failed, you can get message with error code in the notification area (drag down from left top of screen).
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/notification.png)

In the MRM, the service status will should be shown with the service process ID. The status will be one of the followings:

- RUNNING  
	- Service process is working correctly.  
ex:  
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/running.png)

- NOT_INITIALIZED  
	- Service process exists but the hardware resources can not be initialized. In this status, the IVCP APIs can not work properly.
	- You can find the error code message in the notification area.  
ex:  
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/not_initialized.png)

- UNKNOWN  
	- Service process exists but the initialization status can not be confirmed.
	- The error code will be also shown. (For the definition of error codes, please refer to the IVCP, VCIL, SDP User Manual)  
ex:  
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/unknown.png)

- STOP  
	- Service process does not exist.  
ex:  
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/stop.png)


2. Install Prebuilt Sample Apps
Connect device to you computer with ADB.  Execute the following ADB command:
````shell
adb install -r ./bin/prebuilt_sample/IVCPSample.apk
adb install -r ./bin/prebuilt_sample/SDPSample.apk
adb install -r ./bin/prebuilt_sample/VCILSample.apk
````

**Please note that you must install the MRM Services (mrm_service.apk) fist or the sample APPs will not work**

## API reference documents
If you want to develop your own app according to MRM SDK, please refer to [MRM SDK](https://github.com/AIM-Android/MrmSdk/wiki)
