# Introduction
## IVCP SDK Overview
MRM IVCP SDK is a set of libraries for controlling IVCP (Intelligent Vehicle Co-Processor).

MRM IVCP SDK  is composed of the following API modules:  
**NOTE:   Not all API modules are available on all device platforms. Please refer to the hardware specification for corresponding functions.**


|Module|Feature|
|---|---|
|IVCP Management|The basic functions(ex:initialization, deinitialization) of IVCP SDK. Please also refer to the IVCP convention section.|
|Firmware Management|Save/load the default configurations of IVCP functions.|
|Power Management|Power management functions such as boot control, Ignition control, event delay adjustments and low voltage protection.|
|Watchdog|A watch dog timer mechanism which guarantees to shutdown the device in specific expiry time.|
|Alarm|Device alarm wakeup functions. You can set time to IVCP and set the rules to wakeup device at specific time.|
|Digital IO Control|Get/set status of Digital input/output pins.|
|Battery|Backup battery status monitoring.|
|G-Sensor|Gravity sensor functions. You can get data from gravity sensor and set gravity sensor as device wakeup source with specified threshold.|
|P-Sensor|Pressure sensor functions. You can get data from pressure sensor.|
|Sensor|System sensor functions. You can get data from internal system sensor.|
|Peripheral Control|Control power an relative functions of various peripheral devices though IVCP. (ex: WWAN module, WIFI module, GPS, Rear view camera.)|
|Storage|Functions for access the permanent storage on device. You can store arbitrary data to the storage and the data will not be cleared due to the hard disk format or crash.|
|Speed Counter|Functions for monitoring rotation of tire.|
|Hotkey|Get/set keycode of each on-device hardware hotkey. Get/set the LED brightness of  the hotkeys.|


The MRM IVCP API is designed as a background service which acts as a proxy for client APPs to access the IVCP functions
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/ivcp_overview.png)

The MRM IVCP API for Android includes three parts:
- MRM Services  (** mrm_service.apk** )
- MRM Service Client APIs  ( **MrmServiceClientAPI.jar** )
- MRM Data Classes And Constants Definitions ( **MrmDef.jar** )

To use the IVCP service, you must install MRM Services APK  to your device and import the above jar libraries in your APP project.
In your APP, you must call ivcp_bind_service() to connect your APP process with the IVCP Service before accessing the IVCP APIs and call ivcp_unbind_service() to disconnect service before your APP is destroyed. 

# Using IVCP
## IVCP Conventions
### SDK Namespaces
- **mrm.client.IVCPServiceClient** The main class of MRM Service Client API. Use this class to access IVCP features.
- **mrm.client.IVCPServiceConnection** The service connection interface used by ivcp_bind_service().
- **mrm.define.IVCP** The definition of data structures used by MRM Service Client API.
- **mrm.define.MRM_ENUM** The definition of enumeration values used by MRM Service Client API.
- **define.MRM_CONSTANTS** The definition of constants used by MRM Service Client API.

### Basic API Usage
- Each IVCP API has a prefix "ivcp_" immediately followed by the function name and the operation name
- You must create a instance of mrm.client.IVCPServiceClient to usage IVCP APIs .
- To usage the IVCP APIs, you must first "bind" your APP's process to the IVCP service and "unbind" before your APP is closed.
	The flow is described as following:

	1. Create an instance of mrm.client.IVCPServiceConnection and implement the interface on_service_connected() and on_service_disconnected(), The interface is used to will inform your APP when the service is connected/disconnected. Please refer to the document of ivcp_bind_service()  and IVCP sample code for further details.
	2. Call ivcp_bind_service() with created IVCPServiceConnection instance to connect your APP process to the IVCP Service.
	3. Call IVCP Served APIs
	4. Call ivcp_unbind_service() before your APP's application context is destroyed.   
	**NOTE: Please refer to the IVCP sample code for the details.**

![](https://github.com/AIM-Android/MrmSdk/blob/main/images/ivcp_sequence_diagram.png)

- APIs for reading data need an array for argument to store data. The array should be allocated before you pass it to the API and the data will be stored at index 0 of the array.
- You should always check the return value of APIs for error checking. The value should equal to MRM_ERR_NO_ERROR(0) when success or other value when failed.

### IVCP Service Behavior
- The IVCP Service will be started when ivcp_bind_service() is called and keep alive when client APP  unbind.
- The IVCP Service might be stopped by user manually or by system automatically (ex: when low memory).

## Java With Andorid Studio
1. Install MRM IVCP Service to your device
Please find the mrm_service.apk in the MRM SDK package. 
Connect you device to your computer with ADB, then execute the following ADB commands to install MRM IVCP Service
````shell
adb install ./bin/mrm_service.apk
````

2. Import MRM IVCP Service Client API library to your project
	To access MRM IVCP Service, you must import the MRM IVCP Service Client API lib into you project.
	Please find the MrmServiceClientAPI.jar and MrmDef.jar in the MRM SDK package. Copy the libraries to the directory /[Module Name]/libs/  in you Android Studio project  (the default module name might be "app").
	Then import the libraries by following the steps below:

	- Right click on you APP module. Click "Open module settings"
	![](https://github.com/AIM-Android/MrmSdk/blob/main/images/ivcp_open_module_settings.png)
	- Click the "Dependency" tab. Then click "+" -> "Library dependency"
	![](https://github.com/AIM-Android/MrmSdk/blob/main/images/ivcp_dependency.png)
	- Select the library file
	![](https://github.com/AIM-Android/MrmSdk/blob/main/images/ivcp_select_library_file.png)
	- Repeat the above steps to add all libs  and you will see all libs are added to the list.
	![](https://github.com/AIM-Android/MrmSdk/blob/main/images/ivcp_repeate_dependency.png)

# Application Programming Interface
## IVCP Management Functions
### Usage
Please refer to the IVCP Conventions for basic usage.
### Constant
- Class: 
mrm.define.MRM_CONSTANTS

- Fields:

|Field Name|Type|Value|
|---|---|---|
|IVCP_EVENT_ID_UNKNOWN|int|-1|
|IVCP_EVENT_ID_GSENSOR_ALARM|int|0|

### APIs
#### ivcp_bind_service
- Syntax  
````
int  ivcp_bind_service(IVCPServiceConnection conn)
````

- Description  
  Connect current application context(ex: Activity) to IVCP Service.

- Parameters
	- conn: A instance of  mrm.client.IVCPServiceClient.IVCPServiceConnection. 
	You must implement the following interfaces of IVCPServiceConnection:
	**public void  on_service_connected()**: This is a callback which is called when the service is connected. It will be trigged after you called ivcp_bind_service().
	**public void  on_service_disonnected()**: This is a callback which is called when the service is disconnected. It will be trigged when the IVCP service process is stopped or died.IVCP Service might be stopped by user manually or by system automatically (ex: when low memory).
	Please refer to the IVCP sample code for the detailed usage.
	
- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks
  - You can only access IVCP API after **ivcp_bind_service()** is called and the **on_service_connected()** callback is triggered. Before that, for most IVCP API calls, you will get error code MRM_ERR_ANDROID_CLIENT_SERVICE_DISCONNECTED.
  - If IVCP Service is in stopped status, the service will be started when APP called **ivcp_bind_service()**.
  - Due to the nature of Android, a background service(e.g. IVCP Service) might be killed by system automatically, you should carefully implement **on_service_disonnected()** callback to deal with the service disconnect event. 
  - You can call **ivcp_bind_service()** in **on_service_disonnected()** callback to re-connect if you need to.
  - You should call **ivcp_unbind_service()** before your application context(ex: Activity) is destroyed or it may cause memory leak.


#### ivcp_unbind_service
- Syntax
````
int  ivcp_unbind_service()
````

- Description  
  Disconnect current application context from IVCP Service.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  You should call this function before your application context(ex: Activity) is destroyed or it may cause memory leak.

#### ivcp_is_service_connected
- Syntax
````
boolean  ivcp_is_service_connected()
````

- Description  
  Check whether the client application process is connected to IVCP Service.

- Parameters  
  none

- Returns  
  Returns TRUE, if connected.
  Returns FALSE, if disconnected..

- Remarks  
  none

#### ivcp_is_service_initialized
- Syntax
````
int  int ivcp_is_service_initialized(boolean[] status)
````

- Description  
  Get initialization status of IVCP Service..

- Parameters  
	- status: An allocated array of size 1 for storing returned initialization status. The returned value will be store at index 0. The value is TRUE, if IVCP Service is initialized and all served APIs are available. The value is FALSE, if IVCP Service is not initialized.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_get_version
- Syntax
````
int  ivcp_get_version(byte[] version)
````

- Description  
  Get the version of SDK.

- Parameters  
	- version [out]: Pointer to a buffer that will hold the version of SDK. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
	- The maximum length of version string is IVCP_MAXIMUM_LIBRARY_STRING_LENGTH(24)
	- For Android, you can find the constant value  IVCP_MAXIMUM_LIBRARY_STRING_LENGTH  under namespace  mrm.define

#### ivcp_get_platform_name
- Syntax
````
int  ivcp_get_platform_name(byte[] name)
````

- Description  
  Get the platform name.

- Parameters  
	- name [out]: Pointer to a buffer that will hold the version of SDK. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
    - The maximum length of version string is  IVCP_MAXIMUM_PLATFORM_STRING_LENGTH(16)
    - For Android, you can find the constant value  IVCP_MAXIMUM_PLATFORM_STRING_LENGTH  under namespace  mrm.define

#### ivcp_get_device_serial_number
- Syntax
````
int  ivcp_get_device_serial_number(byte[] serial_number)
````

- Description  
  Get the device serial number. This serial number wrote by Advantech at factory time.

- Parameters  
	- serial_number [out]: Pointer to a buffer that will hold the serial number of device. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00.

- Returns   
  MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
	- The maximum length of version string is  IVCP_MAXIMUM_DEVICE_SERIAL_NUMBER_STRING_LENGTH(64)
	- You can find the constant value  IVCP_MAXIMUM_DEVICE_SERIAL_NUMBER_STRING_LENGTH  under namespace  mrm.define

#### ivcp_firmware_get_cpu_serial_number
- Syntax
````
int  ivcp_firmware_get_cpu_serial_number(byte[] sn)
````

- Description  
  Get the VPM serial number from firmware.

- Parameters  
	- sn [out]: Pointer to a buffer that will hold the VPM serial number. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00

- Returns   
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
	- The maximum length of serial number string is  IVCP_MAXIMUM_FIRMWARE_SERIAL_NUMBER_LENGTH(24)
	- You can find the constant value  IVCP_MAXIMUM_FIRMWARE_SERIAL_NUMBER_LENGTH  under namespace  mrm.define


## Firmware Management Functions
### Usage
Please refer to the IVCP Conventions for basic usage.
### APIs
#### ivcp_firmware_get_version
- Syntax  
````
int  ivcp_firmware_get_version(byte[] version)
````

- Description  
  Get the version of firmware.

- Parameters  
	- version [out]:  Pointer to a buffer that will hold the version of firmware. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
	- The maximum length of version string is  IVCP_MAXIMUM_FIRMWARE_VERSION_LENGTH(16)
	- You can find the constant value  IVCP_MAXIMUM_FIRMWARE_VERSION_LENGTH  under namespace  mrm.define

#### ivcp_firmware_get_bootloader_version
- Syntax  
````
int  ivcp_firmware_get_bootloader_version(byte[] version)
````

- Description  
  Get the version of boot loader firmware.

- Parameters  
	- version [out]:  Pointer to a buffer that will hold the version of boot loader firmware. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
	- The maximum length of version string is  IVCP_MAXIMUM_FIRMWARE_VERSION_LENGTH(16)
	- You can find the constant value  IVCP_MAXIMUM_FIRMWARE_VERSION_LENGTH  under namespace  mrm.define

#### ivcp_firmware_load_default
- Syntax  
````
int  ivcp_firmware_load_default()
````

- Description  
  Load the default configuration to current configuration..

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
	- This function will take about 300ms to 600ms for loading the default configuration.
	- You should call ivcp_firmware_save_default() once to save your user default configuration to the device before you call this API to load,  or you will get error code MRM_ERR_IVCP_USER_DEFAULT_IS_EMPTY

#### ivcp_firmware_save_default
- Syntax  
````
int  ivcp_firmware_save_default()
````

- Description  
  Save current configuration as the default configuration.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  This function will take about 300ms to 600ms for saving the default configuration.

## Power Management Functions
### Usage
#### Ignition Control
#### AT Mode/Keep Alive Mode
#### Wakdeup Control
#### Low Battery Protection
#### Force Shutdown Control

### Enumeration
#### ivcp_power_mode
- class:
mrm.define.MRM_ENUM.IVCP_PM_POWER_MODE

- Enum:

|Name|Type|value|Comment|
|---|---|---|---|
|IVCP_POWER_MODE_48V|int|1|The power mode operation on 48V.|
|IVCP_POWER_MODE_24V|int|2|The power mode operation on 24V.|
|IVCP_POWER_MODE_12V|int|3|The power mode operation on 12V.|

- Remark:
Use the method getValue() to get the enum value. 
ex:  MRM_ENUM.IVCP_PM_POWER_MODE.IVCP_POWER_MODE_48V.getValue()   returns  1.
Please refer to sample code for detailed usage. 

#### ivcp_pm_wakeup_type
- class:
mrm.define.MRM_ENUM.IVCP_PM_WAKEUP_TYPE

- Enum:

|Name|Type|value|Comment|
|---|---|---|---|
|IVCP_WAKEUP_TYPE_POWER_BUTTON|int|0|Computer wakeup power button pushed.|
|IVCP_WAKEUP_TYPE_IGNITION|int|1|Computer wakeup form ignition off to on.|
|IVCP_WAKEUP_TYPE_WWAN|int|2|Computer wakeup form WWAN module.|
|IVCP_WAKEUP_TYPE_GSENSOR|int|3|Computer wakeup form gsensor.|
|IVCP_WAKEUP_TYPE_DI1|int|4|Computer wakeup form digital input 1.|
|IVCP_WAKEUP_TYPE_DI2|int|5|Computer wakeup form digital input 2.|
|IVCP_WAKEUP_TYPE_ALARM|int|6|Computer wakeup form RTC alarm.|
|IVCP_WAKEUP_TYPE_HOTKEY|int|7|Computer wakeup form hotkey board.|
|IVCP_WAKEUP_TYPE_DI3|int|8|Computer wakeup form digital input 3.|
|IVCP_WAKEUP_TYPE_DI4|int|9|Computer wakeup form digital input 4.|
|IVCP_WAKEUP_TYPE_KEEP_ALIVE_MODE|int|10|Computer wakeup form keep a live mode.|
|IVCP_WAKEUP_TYPE_AT_MODE|int|11|Computer wakeup form AT mode.|
|IVCP_WAKEUP_TYPE_RESET|int|12|Computer wakeup form software reset or reset button.|

#### ivcp_pm_shutdown_mask
- class:
mrm.define.MRM_ENUM.IVCP_PM_SHUTDOWN_MASK

- Enum:

|Name|Type|value|Comment|
|---|---|---|---|
|IVCP_SHUTDOWN_MASK_POWER_BUTTON|int|0|The shut-down mask of power button source.|
|IVCP_SHUTDOWN_MASK_IGNITION|int|1|The shutdown mask of ignition on to off source.|

- Remark:
Use the method getValue() to get the enum value. 
ex:  MRM_ENUM.IVCP_PM_SHUTDOWN_MASK.IVCP_SHUTDOWN_MASK_IGNITION.getValue()   returns  1.
Please refer to sample code for detailed usage. 

#### ivcp_pm_event
- class:
mrm.define.MRM_ENUM.IVCP_PM_EVENT

- Enum:

|Name|Type|value|Comment|
|---|---|---|---|
|IVCP_EVENT_LOW_VOLTAGE|int|0|The low voltage event.|
|IVCP_EVENT_LOW_VOLTAGE_HARD|int|1|The low voltage hard time event.|
|IVCP_EVENT_IGNITION_ON|int|2|The ignition off to on event.|
|IVCP_EVENT_IGNITION_OFF_TO_POWER_OFF|int|3|The ignition on to off and goto power off event.|
|IVCP_EVENT_IGNITION_OFF_HARD|int|4|The ignition on to off hard time event.|
|IVCP_EVENT_POST_BOOT_POWER_CHECK|int|5|The post-boot power check event.|
|IVCP_EVENT_IGNITION_OFF_TO_SUSPEND|int|6|The ignition on to off and goto suspend event.|
|IVCP_EVENT_POWER_OFF_ALARM|int|7|The power off alarm event.|

- Remark:
Use the method getValue() to get the enum value. 
ex:  MRM_ENUM.IVCP_PM_EVENT.IVCP_EVENT_LOW_VOLTAGE_HARD.getValue()   returns  1.
Please refer to sample code for detailed usage.

### APIs
#### ivcp_pm_power_off
- Syntax  
````
int  ivcp_pm_power_off()
````

- Description  
  Trigger the shutdown process of the operating system by setting the working mode of IVCP firmware to power off mode.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_ignition_status
- Syntax  
````
int  ivcp_pm_get_ignition_status(boolean[] status)
````

- Description  
  Get status of ignition.

- Parameters  
	- status [out]：Ignition status. The value 1 is On otherwise 0 is Off.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none
  
#### ivcp_pm_ignition_wakeup_enable
- Syntax
````
int  ivcp_pm_ignition_wakeup_enable()
````

- Description  
  Enable the ignition wakeup function.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none
  
  
#### ivcp_pm_ignition_wakeup_disable
- Syntax  
````
int  ivcp_pm_ignition_wakeup_disable()
````

- Description  
  Disable the ignition wakeup function.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none


#### ivcp_pm_get_ignition_wakeup_status
- Syntax  
````
int  ivcp_pm_get_ignition_wakeup_status(boolean[] status)
````

- Description  
  Get status of ignition wakeup function.

- Parameters  
	- status [out]  Ignition wakeup function status. The value 1 is Enable otherwise 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none
  
#### ivcp_pm_set_shutdown_mask
- Syntax  
````
int ivcp_pm_set_shutdown_mask(int mask, boolean status)
````

- Description  
  Set shut-down mask This function use to enable or disable the shut-down event source. When the shutdown source set enable, platform can using specifies source to shutdown the machine.

- Parameters  
	- mask [in]: mask enum type. The detail please refer to  ivcp_pm_shutdown_mask section. Some platform may not support to control specifies mask.
	- status [in]: Enable of disable mask. 1 is Enable and 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_shutdown_mask
- Syntax  
````
int ivcp_pm_get_shutdown_mask(int mask, boolean[] status)
````

- Description  
  Get shut-down mask This function use to enable or disable the shut-down event source. When the shutdown source set enable, platform can using specifies source to shutdown the machine.

- Parameters  
	- mask [in]:  mask enum type. The detail please refer to  ivcp_pm_shutdown_mask section. Some platform may not support to control specifies mask.
	- status [out]: Enable of disable mask. 1 is Enable and 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_power_off_alarm_enable
- Syntax  
````
int  ivcp_pm_power_off_alarm_enable()
````

- Description  
  Enable the power off alarm function. MCU will notify host machine when system go to power off.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_power_off_alarm_disable
- Syntax
````
int  ivcp_pm_power_off_alarm_disable()
````

- Description  
  Disable the power off alarm function.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_bind_service
- Syntax  

````
int  ivcp_pm_get_power_off_alarm_status(boolean[] status)
````

- Description  
  Get status of power off alarm function.

- Parameters  
	- status [out]:
	Power off alarm function status. The value 1 is Enable otherwise 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_voltage
- Syntax  

````
int  ivcp_pm_get_voltage(float[] voltage)
````

- Description  
  Get computer current voltage.

- Parameters  
	- voltage [out]:  
	Voltage of computer. The unit is volt.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_alive_mode
- Syntax  

````
int  ivcp_pm_get_alive_mode(boolean[] mode)
````

- Description  
  Get keep a live mode status.

- Parameters  
	- mode [out]:  
	Keep a live mode. The value 1 is Enable otherwise 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_set_alive_mode
- Syntax  

````
int  ivcp_pm_set_alive_mode(boolean mode)
````

- Description  
  Set keep a live mode status.

- Parameters  
	- mode [in]:  
	Keep a live mode. The value 1 is Enable otherwise 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_power_mode
- Syntax  

````
int  ivcp_pm_get_power_mode(byte[] mode)
````

- Description  
  Get power mode.

- Parameters  
	mode [out]:  
	Power mode. The value please refer to ivcp_power_mode.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_set_power_mode
- Syntax  

````
int  ivcp_pm_set_power_mode(byte mode)
````

- Description  
  Set the power mode.

- Parameters  
	- mode [in]:
	Power mode. The value please refer to ivcp_power_mode

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_power_status
- Syntax  

````
int  ivcp_pm_get_power_status(boolean[] status)
````

- Description  
  Get car power status.

- Parameters  
	- status [out]:  
	Car power exists status. The value is 1(true) if exists, 0(false) if not exists

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_at_mode
- Syntax  

````
int  ivcp_pm_get_at_mode(boolean[] mode)
````

- Description  
  Get AT mode status.

- Parameters  
	- mode [out]:  
	AT mode. The value 1 is Enable otherwise 0 is Disable

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_set_at_mode
- Syntax  

````
int  ivcp_pm_set_at_mode(boolean mode)
````

- Description  
  Set AT mode status.

- Parameters  
	- mode [in]:  
	AT mode. The value 1 is Enable otherwise 0 is Disable

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_event_delay
- **Syntax  

````
int  ivcp_pm_get_event_delay(int event_type, int[] second)
````

- Description  
  Get delay time of specifies event.

- Parameters  
	- event_type [in]:  
	Control event type. The detail please refer to  ivcp_pm_event section. Some platform may not support to contorl specifes event.
	- second [out]:  
	The event time. The unit is second.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_set_event_delay
- Syntax  

````
int  ivcp_pm_set_event_delay( int event_type, int second )
````

- Description  
  Set delay time of specifies event.

- Parameters  
	- event_type [in]:  
	Control event type. The detail please refer to  ivcp_pm_event section. Some platform may not support to control specifies event.
	- second [in]:  
	The event time. The unit is second

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_last_wakeup_source
- Syntax  

````
int  ivcp_pm_get_last_wakeup_source(int[] source)
````

- Description  
  Get the last wakeup source.

- Parameters  
	- source [out]:  
	The last wakeup source. The detail please refer to  ivcp_pm_wakeup_type section

- Returns  
  MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_lvp_range
- Syntax  

````
int  ivcp_pm_get_lvp_range(float[] min,  float[] max,  float[] default)
````

- Description  
  Get low voltage protection control range and default value.

- Parameters  
	- min [out]:
	The minimum voltage value can be set.
	- max [out]:
	The minimum voltage value can be set.
	- default [out]:
	The default voltage value

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none
  
#### ivcp_pm_get_lvp_preboot_threshold
- Syntax

````
int  ivcp_pm_get_lvp_preboot_threshold(float[] voltage)
````

- Description
  Get preboot low voltage protection threshold.

- Parameters
	- voltage [out]:
	The threshold. The unit is volt.

- Returns
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks
  none

#### ivcp_pm_set_lvp_preboot_threshold
- Syntax

````
int  ivcp_pm_set_lvp_preboot_threshold(float voltage)
````

- Description
  Set preboot low voltage protection threshold.

- Parameters
	- voltage [in]:
	The threshold. The unit is volt.

- Returns
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks
   In TREK530/TREK734/TREK733, the 12V LVP Threshold voltage is 10.11 to 12.26 and the 24V LVP Threshold voltage is 21.09 to 23.29 .

#### ivcp_pm_get_lvp_postboot_threshold
- Syntax  

````
int  ivcp_pm_get_lvp_postboot_threshold(float[] voltage)
````

- Description  
  Get postboot low voltage protection threshold.

- Parameters  
	- voltage [out]:
	The threshold. The unit is volt

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_set_lvp_postboot_threshold
- Syntax  

````
int  ivcp_pm_set_lvp_postboot_threshold(float voltage)
````

- Description  
  Set postboot low voltage protection threshold.

- Parameters  
	- voltage [in]:
	The threshold. The unit is volt.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  In TREK530/TREK734/TREK733, the 12V LVP Threshold voltage is 10.11 to 12.26 and the 24V LVP Threshold voltage is 21.09 to 23.29 .

#### ivcp_pm_reset_lvp_threshold
- Syntax  

````
int  ivcp_pm_reset_lvp_threshold()
````

- Description  
  Reset all the low voltage protection threshold to default.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_lvp_preboot_enable
- Syntax  

````
int  ivcp_pm_lvp_preboot_enable()
````

- Description  
  Enable preboot low voltage protection..

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_lvp_preboot_disable
- Syntax  

````
int  ivcp_pm_lvp_preboot_disable()
````

- Description  
  Disable preboot low voltage protection.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_lvp_postboot_enable
- Syntax  

````
int  ivcp_pm_lvp_postboot_enable()
````

- Description  
  Enable prostboot low voltage protection.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_lvp_postboot_disable
- Syntax  

````
int  ivcp_pm_lvp_postboot_disable()
````

- Description  
  Disable postboot low voltage protection.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_lvp_preboot_get_status
- Syntax  

````
int  ivcp_pm_lvp_preboot_get_status(boolean[] status)
````

- Description  
  Get status of preboot low voltage protection.

- Parameters  
	- status [out]:
	The preboot low voltage protection status. The value 1 is Enable otherwise 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_lvp_postboot_get_status
- Syntax  

````
int  ivcp_pm_lvp_postboot_get_status(boolean[] status)
````

- Description  
  Get status of postboot low voltage protection.

- Parameters  
	- status [out]:
	The postboot low voltage protection status. The value 1 is Enable otherwise 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_force_shutdown_enable
- Syntax  

````
int  ivcp_pm_force_shutdown_enable()
````

- Description  
  Enable force shutdown function. This function is disabled by default.
  If this function is enabled, when device is woken up and ignition status is off (e.g alarm wakeup is triggered), the device will be automatically shutdown after an specified delay time.  You can set delay time by using ivcp_pm_set_force_shutdown_delay() . To disable this function, you can use ivcp_pm_force_shutdown_disable().  To get the current enable status of this function, you can use ivcp_pm_get_force_shutdown_status().

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_force_shutdown_disable
- Syntax  

````
int  ivcp_pm_force_shutdown_disable()
````

- Description  
  Disable force shutdown function.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_force_shutdown_status
- Syntax  

````
int ivcp_pm_get_force_shutdown_status(boolean[] status)
````

- Description  
  Get the enable status of force shutdown function.

- Parameters  
	- status [out]:
	The enable status. The value 1 is Enable otherwise 0 is Disable.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### ivcp_pm_get_force_shutdown_delay
- Syntax  

````
int  ivcp_pm_get_force_shutdown_delay(int[] second)
````

- Description  
  Get delay time of force shutdown function.

- Parameters  
	- second [out]:
	The delay time. The unit is second

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none


#### ivcp_pm_set_force_shutdown_delay
- Syntax  

````
int  ivcp_pm_set_force_shutdown_delay(int second)
````

- Description  
  Set delay time of force shutdown function.

- Parameters  
	- second [in]:
	The delay time. The unit is second. The available range is 1 ~ 3600 sec

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none
  


# Error Code List
## Comman Error
- **(0x00000000) MRM_ERR_NO_ERROR** - On success.
- **(0x00000001) MRM_ERR_INVALID_POINTER** - Encounter invalid pointer.
- **(0x00000002) MRM_ERR_INVALID_ARGUMENT** - Encounter invalid argument. Please check out the API parameter.
- **(0x00000003) MRM_ERR_UNSUPPORT_OPERATION** - Encounter unsupported operation. Please check out spec. of  the platform supported.
- **(0x00000005) MRM_ERR_LIBRARY_NOT_INIT** - Function call before the library init.
- **(0x00000010) MRM_ERR_ILLEGAL_OPERATION** - Encounter illegal operation.
- **(0x00000011) MRM_ERR_LIBRARY_ALREADY_INIT** - Function call before the library init.
- **(0x00000012) MRM_ERR_ARRAY_OUR_OF_RANGE** - Encounter the access array out of range.
- **(0x00000013) MRM_ERR_OPERATION_FAIL** - Encounter operation fail.
- **(0x10000001) MRM_ERR_ANDROID_JNI_NULL_POINTER** - Null pointer error occurred on service side(JNI). 
- **(0x10000002) MRM_ERR_ANDROID_JNI_OUT_OF_MEMORY** - Out of memory error occurred on service side(JNI).
- **(0x10000003) MRM_ERR_ANDROID_JNI_EVENT_INIT_FAILED** - Failed to init event handle on service side(JNI).
- **(0x10000004) MRM_ERR_ANDROID_JNI_EVENT_DEINIT_FAILED** - Failed to init event handle on service side(JNI).
- **(0x10000005) MRM_ERR_ANDROID_JNI_EVENT_LISTENING_THREAD_ALREADY_RUNNING** - Event listening thread in service is already running(JNI).
- **(0x10000006) MRM_ERR_ANDROID_JNI_EVENT_LISTENING_THREAD_CREATE_FAILED** - Failed to create event listening thread in service(JNI).
- **(0x10100001) MRM_ERR_ANDROID_SERVICE_NULL_POINTER** - Null pointer error occurred on service side.
- **(0x10100002) MRM_ERR_ANDROID_SERVICE_UNKNWON_EXCEPTION** - Unknown exception occurred on service side.
- **(0x10100003) MRM_ERR_ANDROID_SERVICE_REMOTE_CALLBACK_LIST_NOT_FOUND** - Remote callback list not found on service side.
- **(0x10100004) MRM_ERR_ANDROID_SERVICE_REMOTE_CALLBACK_REGISTER_FAILED** - Failed to register remote callback on service side.
- **(0x10100005) MRM_ERR_ANDROID_SERVICE_REMOTE_CALLBACK_UNREGISTER_FAILED** - Failed to unregister remote callback on service side.
- **(0x10100006) MRM_ERR_ANDROID_SERVICE_REMOTE_CALLBACK_UPDATE_FAILED** - Failed to update remote callback cookie on service side.
- **(0x10200001) MRM_ERR_ANDROID_CLIENT_NULL_POINTER** - Null pointer error occurred on client side in IVCP Service Client API lib.
- **(0x10200002) MRM_ERR_ANDROID_CLIENT_FAILED_TO_BIND_SERVICE** - Unable to connect client application context to the service.
- **(0x10200003) MRM_ERR_ANDROID_CLIENT_SERVICE_ALREADY_CONNECTED** - Current client application context is already connected to the service.
- **(0x10200004) MRM_ERR_ANDROID_CLIENT_SERVICE_DISCONNECTED** - Current client application context is not connected to the service.
- **(0x10200005) MRM_ERR_ANDROID_CLIENT_REMOTE_EXCEPTION** - Remote exception received at client side. Failed to execute required task.
- **(0x10200006) MRM_ERR_ANDROID_CLIENT_FAILED_TO_UNBIND_SERVICE** - Unable to disconnect client application context to the service.

## IVCP Error
- **(0x01000001) MRM_ERR_IVCP_DEVICE_NODE_OPEN_FAIL** - Open IVCP device node fail. Please checkout IVCP is exist or the device not use by another application.
- **(0x01000002) MRM_ERR_IVCP_DEVICE_NODE_WRITE_FAIL** - Encounter write operation fail. Please retry operation.
- **(0x01000003) MRM_ERR_IVCP_DEVICE_NODE_READ_FAIL** - Encounter read operation fail. Please retry operation.
- **(0x01000004) MRM_ERR_IVCP_IS_BUSY** - Device is busy. try again.
- **(0x01000005) MRM_ERR_IVCP_ERROR_COMMAND** - Device is not recognize this command operation.
- **(0x01000006) MRM_ERR_IVCP_ERROR_RESPONSE_FORMAT_NOT_MATCH** - Device is not recognize this response.
- **(0x01000007) MRM_ERR_IVCP_PARAMETER_OUT_OF_RANGE** - The parameter out of range.
- **(0x01000008) MRM_ERR_IVCP_DEVICE_NODE_READ_TIMEOUT** - Device out of expect time.
- **(0x01000009) MRM_ERR_IVCP_NO_EEPROM_CHIP_FIND** - Device can't find the storage to save the config. Please contract your FAE to check out the hardware.
- **(0x0100000A) MRM_ERR_IVCP_DEVICE_NODE_WRITE_NOT_MATCH** - Device is not recognize this command operation.
- **(0x0100000B) MRM_ERR_IVCP_NO_GSENSOR_CHIP_FIND** - Device can't find the G-Sensor chip on the platform. Please contract your FAE to get supported.
- **(0x0100000C) MRM_ERR_IVCP_UNKNOW_RETURN_VALUE** - Library can not recognize response value.
- **(0x0100000D) MRM_ERR_IVCP_GSENSOR_DATA_INVALID** - The G-Sensor value is invalid.
- **(0x0100000E) MRM_ERR_IVCP_UNKNOW_ERROR_CODE** - Library can not recognize error code.
- **(0x0100000F) MRM_ERR_IVCP_PSENSOR_DATA_INVALID** - The P-Sensor value is invalid.
- **(0x01000010) MRM_ERR_IVCP_NO_PSENSOR_CHIP_FIND** -  Device can't find the P-Sensor chip on the platform. Please contract your FAE to get supported.
- **(0x01000011) MRM_ERR_IVCP_SMBUS_ENCOUTER_ERROR** - Library read SMBus encounter error. The operation aborted.
- **(0x01000012) MRM_ERR_IVCP_GSENSOR_DATA_NOT_READY** - The G-Sensor alarm data queue is empty.
- **(0x01000013) MRM_ERR_IVCP_USER_DEFAULT_IS_EMPTY** - The current user default configuration is empty and can not be loaded.



