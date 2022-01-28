# Introduction
## VCIL OVerview
MRM VCIL SDK is a set of libraries for user to control the vehicle communication protocols on devices which are equipped with VCIM (Vehicle Communication Interface Module) MCU. VCIM supports two CAN ports for  CAN, J1939, OBD2 protocols, and one J1708 port for J1708, J1587 protocols and controls the data flow of those protocols on the data buses.

MRM VCIL SDK provides API modules for applications to control each protocol to achieve various purposes. The software stack of SDK can be described as the following figure.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_overview.png)

The MRM VCIL API is designed as libraries for the customer's APP to load and access

The MRM VCIL API for Android includes three parts:
- MRM Data Classes And Constants Definitions ( **MrmDef.jar** )
- MRM Java APIs  (**MrmJni.jar**)
- Native libs  ( **libvcilJni.so,  libvcil.so**)

To use the VCIL, you must import the above libraries in your APP project.
In your APP, you must call vcil_init() to initialize the MCU before accessing the VCIL APIs and call vcil_deinit() to release allocated resources before your APP is destroyed. 

# Using VCIL
## VCIL Conventions
### SDK Namespaces
- **mrm.VCIL**
The class of VCIL API.
- **mrm.define.VCIL**
The definition of data structures used by VCIL API.
- **mrm.define.MRM_ENUM**
The definition of enumeration values used by VCIL API.
- **mrm.define.MRM_CONSTANTS**
The definition of constants used by VCIL API.
- **mrm.define.MRM_ERROR**
The definition of error codes.
### Basic API usage
- Each VCIL API has a prefix "vcil_" immediately followed by the function name and the operation name
- You must create an instance of mrm.VCIL to usage VCIL APIs .
- To use the VCIL APIs, you must first initialize the library and deinitialize before your APP is closed.
	The flow is described as following:
	1. You must vcil_init() before using the other IVCP APIs.
	2. Call VCIL APIs.
	3. You must call vcil_deinit() before you APP closed.

![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_sequence_diagram.png)

- APIs for reading data need an array for argument to store data. The array should be allocated before you pass it to the API and the data will be stored at index 0 of the array.
- You should always check the return value of APIs for error checking. The value should equal to MRM_ERR_NO_ERROR(0) when success or other value when failed.

## JAVA With Android Studio
To access VCIL funtions from your APP, you must import the VCIL libraries into you project.

Please find the MrmJni.jar,  MrmDef.jar and jniLibs/ folder in the MRM SDK package. 
Copy the MrmJni.jar,  MrmDef.jar to the directory /[Module Name]/libs/  in your Android Studio project  (the default module name might be "app")  and copy the jniLibs/ folder to the directory /[Module Name]/src/main/
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_android_studio.png)

Then import the Java libraries by following the steps below:
- Right click on you APP module. Click "Open module settings"
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_open_module_settings.png)

- Click the "Dependency" tab. Then click "+" -> "File dependency"
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_dependency.png)

- Select the lib file.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_select_library_file.png)

- Repeat the above steps to add all libs  and you will see all libs are added to the list.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_repeate_dependency.png)

# Application Programming Interface
## VCIL Management Functions
### Usage
#### Basic Usage
Please refer to the VCIL Conventions for basic usage for Android.
#### Protocol Mode Setting
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_sequence_diagram1.png)

To start using VCIL APII module and related modules, you should first call vcil_init() before using the other VCIL APIs. To stop using VCIL module, you must call vcil_deinit() to close API.

To set the activated protocols for each port, you can use vcil_set_mode() to set. 

The available modes for each CAN Port are as followings:
- (0) VCIL_MODE_CAN - CAN protocol  (DEFAULT)
- (1) VCIL_MODE_J1939 - J1939 protocol
- (2) VCIL_MODE_ODB2 - OBD2 protocol

The available modes for each J1708 Port are as followings:
- (3) VCIL_MODE_J1708 - J1708 protocol (DEFAULT)
- (4) VCIL_MODE_J1587 - J1587 protocol

For example, 
to activate CAN on CAN port 0, J1939 on CAN port 1 and J1708 on J1708 port 0, you can call the API with following parameters - 
vcil_set_mode(VCIL_MODE_CAN, VCIL_MODE_J1939, VCIL_MODE_J1708).

To reset the MCU, you can use  vcil_firmware_reset().

### Enumeration
#### VCIL_MODE
- class:
mrm.define.MRM_ENUM.VCIL_MODE
- Enum:

|Name|Type|Value|Comment|
|---|---|---|---|
|VCIL_MODE_CAN|int|0|Active at CAN mode.|
|VCIL_MODE_J1939|int|1|Active at J1939 mode.|
|VCIL_MODE_OBD2|int|2|Active at OBD2 mode.|
|VCIL_MODE_J1708|int|3|Active at J1708 mode.|
|VCIL_MODE_J1587|int|4|Active at J1587 mode.|

- Remark:
Use the method getValue() to get the enum value. 
ex:  MRM_ENUM.VCIL_MODE.VCIL_MODE_J1939.getValue()   returns  1.
Please refer to sample code for detailed usage. 

### Constant
- Class:
mrm.define.MRM_CONSTANTS

- Fields:

|Field Name|Type|Value|
|---|---|---|
|VCIL_EVENT_ID_UNKNOWN|int|-1|
|VCIL_EVENT_ID_RECEIVED_MSG_CAN|int|0|
|VCIL_EVENT_ID_RECEIVED_MSG_J1939|int|1|
|VCIL_EVENT_ID_RECEIVED_MSG_OBD2|int|2|
|VCIL_EVENT_ID_RECEIVED_MSG_J1708|int|3|
|VCIL_EVENT_ID_RECEIVED_MSG_J1708|int|4|

### APIs
#### vcil_init
- Syntax  
````
int vcil_init(String port)
````

- Description  
  Initialize the VCIL library.
- Parameters  
	- port [in]: Pointer to a buffer that will hold the string of VCIL device path. For C, the path string is end of '\0'. Example: port = "/dev/ttyA0"   for Android (default)

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  Prior to calling any VCIL API function the library needs to be initialized by calling this function. The return code for all VCIL API function will be MRM_ERR_LIBRARY_NOT_INIT unless this function is called.

#### vcil_deinit
- Syntax  
````
int  vcil_deinit()
````

- Description  
  Deinitialize the VCIL library
- Parameters  
  none
- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### vcil_get_version
- Syntax  
````
int vcil_get_version(byte[] version)
````

- Description  
  Get the version of SDK
- Parameters  
	- version [out]: Pointer to a buffer that will hold the version of SDK. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00 

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  The maximum length of version string is IVCP_MAXIMUM_LIBRARY_STRING_LENGTH(24)

#### vcil_set_mode
- Syntax  
````
int vcil_set_mode(int can_port0, int can_port1, int j1708_port0)
````

- Description  
  Set the protocol mode of each port.

- Parameters  
	- can_port0 [in]: Setup the CAN port 0 protocol mode.
	- can_port1 [in]: Setup the CAN port 1 protocol mode.
	- j1708_port0 [in]: Setup the J1708 port 0 protocol mode
	
	For the definition of mode ID, please refer to VCIL_MODE.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### vcil_get_mode
- Syntax  
````
int vcil_get_mode(int[] can_port0, int[] can_port1, int[] j1708_port0)
````

- Description  
  Get the protocol mode of each port.

- Parameters  
	- can_port0 [out]: The current CAN port 0 protocol mode.
	- can_port1 [out]: The current CAN port 1 protocol mode.
	- j1708_port0 [inout]: The current J1708 port 0 protocol mode
	
	For the definition of mode ID, please refer to VCIL_MODE.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none


## Firmware Management Functions
### Usage
#### Basic Usage
Please refer to the VCIL Conventions for basic usage for Android.
### Constant
- Class:
mrm.define.MRM_CONSTANTS

- Fields:

|Field Name|Type|Value|
|---|---|
|VCIL_MAXIMUM_FIRMWARE_VERSION_LENGTH|int|16|

### APIs
#### vcil_firmware_get_version
- Syntax  
````
int vcil_firmware_get_version (byte[] version)
````

- Description  
  Get the version of firmware.

- Parameters  
	- version [out]: Pointer to a buffer that will hold the version of firmware. The buffer is C string that end of '\0'. The content of unused bytes filled 0x00.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  The maximum length of version string is VCIL_MAXIMUM_FIRMWARE_VERSION_LENGTH(16)
  
#### vcil_firmware_reset
- Syntax  
````
int  vcil_firmware_reset()
````

- Description  
  Reset the VCIL firmware.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  All function mask/filter will be reset after call this function.


## CAN Functions
### Usage
#### Basic Usage
Please refer to the VCIL Conventions for basic usage for Android.
Also, before using CAN related APIs, you must first set the protocol mode of proper CAN port to CAN mode. Please refer to the protocol mode setting section.
#### CAN Bus Speed Setting
To start data transmission of CAN, J1939, OBD2 protocol on CAN bus, you must first configure the CAN bus speed for MCU. 

This following figure describes how to set CAN bus speed through SDK.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_bus_speed_setting.png)

#### CAN Message Reading
You can implement your CAN message reading application in either Polling or Event Handling style.
- Polling

![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_message_reading_polling.png)

The most simple way to get received message is to hold a loop in your application and keep calling vcil_can_read() or vcil_can_read_multi() to get received message(s) from SDK internal buffer.

The advantage of reading messages in polling style is that it is simple and relatively lower overhead (i.e. no event handling) to read message(s).
The disadvantage is that you need to keep your application process reading message even when there is actually no message in SDK internal buffer, which may result in unnecessary power consuming.
- Event Handling

![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_message_reading_event_handling.png)

MRM SDK leverage the Android Handler mechanism to inform CAN message receive event.

To read messages in event handling style, you need to create an instance of Handler and call vcil_can_set_event_handler() first to register the Handler instance to VCIL. When MCU receive a CAN message from CAN bus, VCIL adds the received message to internal buffer and trigger the event to inform registered handler.

When the event is triggered, the handleMessage() callback of registered handler instance will be triggered. you can then run a loop to calling vcil_can_read() or vcil_can_read_multi() to get and consume the received messages from the internal buffer. 

Due to that VCIL will trigger event whenever it receive a CAN message, to avoid from getting multiple unnecessary events, you should call vcil_can_wait_event( FALSE ) to ask VCIL temporarily stop passing event to handler before your APP start the alarm data getting loop. 
After all messages in buffer are consumed,  you should call vcil_can_wait_event( TRUE ) to ask VCIL continue the event informing.

If you do not need to listen to alarm event anymore, you should call vcil_can_unset_event_handler() to unregister the Handler instance from VCIL.

The advantage of reading messages in event handling style is that application process only work when there are messages can be read, which relatively cost less system resources and power.
The disadvantage is that there would be some overhead of event handling to read a message.

Please refer to the sample code for implementation details.

#### CAN Message Writing
This following figure describes how to write CAN messages to CAN bus by using SDK APIs.

The CAN bus speed must be set correctly before you do read/write operation. Please refer to CAN Bus Speed Setting section for the details.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_message_writing.png)

#### CAN Acceptance filter Setting
##### Overview
In the car system there might be many nodes on the bus and the number of CAN messages transmitted to the bus might be enormous. Due to the performance and application purpose concerns, it might not be a good idea to try to process all messages on the bus. To focus on the messages you interest in, you can use the filter functions provided by VCIL MCU.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_filter_overview.png)

The VCIL MCU provides 14 configurable CAN identifier filter banks for filtering the incoming messages for each CAN channel. The filters act as "white list". If the CAN channel is configured to work with filters, the MCU which will only receive the CAN messages which match the filter conditions and drop others. 

For each filter, a identifier value and mask is set. The identifier value defines a desired identifier and the mask defined the bits of identifier the filter care about. If the mask of filter is set to 0, then it means the filter "don't care" any bit of the identifier and all CAN messages from the bus will be passed through this filter. When VCIL MCU get a CAN message from bus, it AND the identifier of message and the identifier of each filter with mask to test whether the message should be passed or not.

The following are examples of filter mask setting. 
- Accepted example
If we set a filter with mask value = [1 1 1 1 1 1 0 0] (0xFC) and identifier = [1 0 1 1 1 0 1 0](0xBA), then MCU will care the first 6 bits of the identifier.
Thus MCU will only accept messages with identifier 0xB8~BA. (Binary:1011 = Hex:B)
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_filter_accepted_example.png)

- Unaccepted  example
If we set a filter with mask value = [1 1 1 1 1 1 1 1] (0xFF) and identifier = [1 0 1 1 1 0 1 0](0xBA), then MCU will care the all bits of the identifier.
Thus MCU will only accept messages with identifier 0xBA. 
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_filter_unaccepted_example.png)

The following shows examples of filtering.
- Example - Single filter

|CAN MASK Configuration|Receive CAN data|Result|
|---|---|---|
|Filter 1:CAN ID=10111010b (0xBA)Mask = 11111111b (0xFF)|10111010 (0xBA)|Accept|
|Filter 1:CAN ID=10111010b (0xBA)Mask = 11111111b (0xFF)|10111011 (0xBB)|Drop|

|CAN MASK Configuration|Receive CAN data|Result|
|---|---|---|
|Filter 1:CAN ID=10111010b (0xBA)Mask = 11111100b (0xFC)|10111000 (0xB8)|Accept|
|Filter 1:CAN ID=10111010b (0xBA)Mask = 11111100b (0xFC)|10111001 (0xB9)|Accept|
|Filter 1:CAN ID=10111010b (0xBA)Mask = 11111100b (0xFC)|10111010 (0xBA)|Accept|
|Filter 1:CAN ID=10111010b (0xBA)Mask = 11111100b (0xFC)|10111100 (0xBC)|Drop|

- Example - Multiple filters
You can set multiple filter at same time for multiple range of targets. For example,  ID 0x28~0x37 and 0x3000~0x31FF and 0x1600

|CAN MASK Configuration|Receive CAN data|Result|
|---|---|---|
|Filter 1: CAN ID=0000000000101000b (0x0028), Mask = 1111111111111110b (0xFFFE)<br>Filter 2: CAN ID=0000000000110000b (0x0030) Mask = 1111111111111000b (0xFFF8)<br>Filter 3: CAN ID=0011000000000000b (0x3000) Mask = 1111111000000000b (0xFE00)<br>Filter 4: CAN ID=0001011000000000b (0x1600) Mask = 1111111111111111b (0xFFFF)|00101000b(0x29)|Accept|
|Filter 1: CAN ID=0000000000101000b (0x0028) Mask = 1111111111111110b (0xFFFE)<br>Filter 2: CAN ID=0000000000110000b (0x0030) Mask = 1111111111111000b (0xFFF8)<br>Filter 3: CAN ID=0011000000000000b (0x3000) Mask = 1111111000000000b (0xFE00)<br>Filter 4: CAN ID=0001011000000000b (0x1600) Mask = 1111111111111111b (0xFFFF)|01001000b(0x48)|Drop|
|Filter 1:CAN ID=0000000000101000b (0x0028)Mask = 1111111111111110b (0xFFFE)<br>Filter 2:CAN ID=0000000000110000b (0x0030)Mask = 1111111111111000b (0xFFF8)<br>Filter 3:CAN ID=0011000000000000b (0x3000)Mask = 1111111000000000b (0xFE00)<br>Filter 4:CAN ID=0001011000000000b (0x1600)Mask = 1111111111111111b (0xFFFF)|0011000100000010b(0x3102)|Accept|
|Filter 1:CAN ID=0000000000101000b (0x0028)Mask = 1111111111111110b (0xFFFE)<br>Filter 2:CAN ID=0000000000110000b (0x0030)Mask = 1111111111111000b (0xFFF8)<br>Filter 3:CAN ID=0011000000000000b (0x3000)Mask = 1111111000000000b (0xFE00)<br>Filter 4:CAN ID=0001011000000000b (0x1600)Mask = 1111111111111111b (0xFFFF)|0011001000000000b(0x3200)|Drop|

##### Using
- To get/set filter:
Call vcil_can_set_mask() to set the mask and call vcil_can_get_mask() to get mask.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_filter_set.png)

- To remove specific filter:
Call vcil_can_remove_mask() to remove filter of specified filter bank.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_filter_remove_specific.png)

- To remove all filter:
Call vcil_can_reset_mask() to reset all filter.
![](https://github.com/AIM-Android/MrmSdk/blob/main/images/vcil_can_filter_remove_all.png)

### Enumeration
#### VCIL_CAN_SPEED
- class:
mrm.define.MRM_ENUM.VCIL_CAN_SPEED

- Enum:

|Name|Type|value|Comment|
|---|---|---|---|
|VCIL_CAN_SPEED_125K|int|0|125 kbit/s|
|VCIL_CAN_SPEED_250K|int|1|250 kbit/s|
|VCIL_CAN_SPEED_500K|int|2|500 kbit/s|
|VCIL_CAN_SPEED_1M|int|3|1M bit/s|
|VCIL_CAN_SPEED_200K|int|4|200 kbit/s|
|VCIL_CAN_SPEED_100K|int|5|100 kbit/s|
|VCIL_CAN_SPEED_800K|int|6|800 kbit/s|
|VCIL_CAN_SPEED_83K|int|7|83 kbit/s|
|VCIL_CAN_SPEED_50K|int|8|50 kbit/s|
|VCIL_CAN_SPEED_20K|int|9|20 kbit/s|
|VCIL_CAN_SPEED_10K|int|10|10 kbit/s|
|VCIL_CAN_SPEED_5K|int|11|5 kbit/s|
|VCIL_CAN_SPEED_USER_DEFINE|int|0xFF|user-defined bit-rate|

- Remark:
Use the method getValue() to get the enum value. 
ex:  MRM_ENUM.CAN_SPEED.VCIL_CAN_SPEED_250K.getValue()   returns  1.
Please refer to sample code for detailed usage. 

#### VCIL_CAN_BUS_MODE
- class:
mrm.define.MRM_ENUM.VCIL_CAN_BUS_MODE

- Enum:

|Name|Type|value|Comment|
|---|---|---|---|
|VCIL_CAN_BUS_NORMAL_MODE|int|0|CAN controller operates in normal mode. In normal mode, CAN controller synchronize the bit traffic on the CAN bus and is able to receive/transmit CAN messages.|
|VCIL_CAN_BUS_LISTEN_MODE|int|1|CAN controller operates in listen mode.In listen mode, the CAN controller is only able to receive valid data frames and remote request frames and NOT able to transmit. The CAN controller only monitor the bit traffic on bus without interfering the bus (e.g. keep in  recessive state) and NO dominant bit will be sent (i.e. Acknowledge Bits, Error Frames) .Listen mode can be used to monitor the traffic on a CAN bus without interfering the bus.|
|VCIL_CAN_BUS_INIT_MODE|int|2|CAN controller operates in initiation mode.In Initialization Mode, the CAN controller is uninitialized and stops transmitting and receiving to/from the CAN bus, and keep the bus output in recessive status.|

### Constant
- Class:
mrm.define.MRM_CONSTANTS

- Fields:

|Field Name|Type|Value|
|---|---|
|VCIL_MAX_CAN_DATA_SIZE|int|8|

### Classes
#### VCIL_CAN_MESSAGE
- class:
mrm.define.VCIL.VCIL_CAN_MESSAGE

- Fields:

|Field Name|Type|Input/Output|Comment|
|---|---|---|---|
|port|int|in, out|The port ID which this CAN message is received from/sent to.|
|length|int|in, out|Length of data of the CAN message. This data length should not over VCIL_MAX_CAN_DATA_SIZE(8).|
|remote_request|boolean|in, out|This field is used to indicate whether this CAN message is a remote transmit request(RTR) frame. The value is TRUE if the message is a RTR frame(the RTR field of the CAN message identifier is 1).The value is FALSE if the message is not a RTR frame.|
|extended_frame|boolean|in, out|This field is used to indicate that the message is a standard format(CAN2.0A) or a extended format(CAN2.0B) message.The value is TRUE if the message is a CAN2.0B message (with 29-bits identifier).The value is FALSE if the message is a CAN2.0A message (with 11-bits identifier).|
|id|int|in, out|The Identifier of the CAN message.|
|data|byte[]|in, out|A byte array containing the data of the CAN message.|

#### VCIL_CAN_MASK
- class:
mrm.define.VCIL.VCIL_CAN_MASK

- Fields:

|Field Name|Type|Input/Output|Comment|
|---|---|---|---|
|type|byte|-|The mask type.This field is reserved for future use and currently ignored.|
|bank|byte|in, out|The mask bank, the VCIL supported maximum 14 bank which 0~13. This bank should not over 13. You can think of the bank is a rule of  CAN message hardware filter.|
|remote_request|boolean|in, out|This field is used to indicate whether this CAN message is a remote transmit request(RTR) frame. The value is TRUE if the message is a RTR frame(the RTR field of the CAN message identifier is 1).The value is FALSE if the message is not a RTR frame.|
|extended_frame|boolean|in, out|This field is used to indicate that the message is a standard format(CAN2.0A) or a extended format(CAN2.0B) message.The value is TRUE if the message is a CAN2.0B message (with 29-bits identifier).The value is FALSE if the message is a CAN2.0A message (with 11-bits identifier).|
|id1|int|in, out|The Identifier 1 of bank.|
|mask1|int|in, out|The mask 1 of the bank.|
|id2|int|in, out|The Identifier 2 of bank.|
|mask2|int|in, out|The mask 2 of the bank.|


#### VCIL_CAN_ERROR_STATUS
- class:
mrm.define.VCIL.VCIL_CAN_ERROR_STATUS

- Fields:

|Field Name|Type|Input/Output|Comment|
|---|---|---|---|
|rec|int|out|Receive error counter. The implementing part of the fault confinement mechanism of the CAN protocol. In case of an error during reception, this counter is incremented by 1 or by 8 depending on the error condition as defined by the CAN standard. After every successful reception the counter is decremented by 1 or reset to 120 if its value was higher than 128. When the counter value exceeds 127, the CAN controller enters the error passive state.|
|tec|int|out|Transmit error counter.The implementing part of the fault confinement mechanism of the CAN protocol.|
|last_error_code|int|out|This field is set by hardware and holds a code which indicates the error condition of the last error detected on the CAN bus.If a message has been transferred (reception or transmission) without error, this field will be cleared to ‘0’.The LEC[2:0] bits can be set to value 0b111 by software. They are updated by hardware to indicate the current communication status.000: No Error 001: Stuff Error 010: Form Error 011: Acknowledgment Error 100: Bit recessive Error 101: Bit dominant Error 110: CRC Error 111: Set by software|
|error_flag|int|out|CAN Bus error flag. bit0: Error warning flagThis bit is set by hardware when the warning limit has been reached (Receive Error Counter or Transmit Error Counter≥96). bit1: Error passive flagThis bit is set by hardware when the Error Passive limit has been reached (Receive VCIM Command Specification 124 / 153 Error Counter or Transmit Error Counter>127). bit2: Bus-off flag This bit is set by hardware when it enters the bus-off state. The bus-off state is entered on TEC overflow, greater than 255|

### APIs
#### vcil_can_read
- Syntax  
````
int vcil_can_read(VCIL_CAN_MESSAGE message)
````

- Description  
  Get a CAN message from VCIL library CAN buffer if available otherwise you may get a error code(MRM_ERR_VCIL_DATA_NOT_READY) and a invalid CAN message.

- Parameters  
	- message [out]: Instance of VCIL_CAN_MESSAGE which is used to store received CAN message

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  You can call this function to receive CAN message from each CAN port. 

#### vcil_can_read_multi
- Syntax  
````
int vcil_can_read_multi(List<VCIL_CAN_MESSAGE> messages, int desiredReadNum, int[] resultReadNum)
````

- Description  
  Read multiple received CAN messages from the SDK internal buffer.

- Parameters  
	- message [out]: List of VCIL_CAN_MESSAGE which is used to store received CAN messages.
	- desiredReadNum [in]: The number of CAN message you expect to get.
	- resultReadNum [out]: An allocated array of size 1 for storing the number of CAN message the SDK actually returned. The return value will be stored at index 0. 

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### vcil_can_write
- Syntax  
````
int vcil_can_write(VCIL_CAN_MESSAGE message)
````

- Description  
  Write a CAN message to specified CAN port.

- Parameters  
	- message [out]: Instance of VCIL_CAN_MESSAGE which stores the CAN message to be sent.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  You can call this function to receive CAN message from each CAN port. 

#### vcil_can_set_speed
- Syntax  
````
int vcil_can_set_speed(byte port, int speed)
````

- Description  
  Set the specified CAN port bus baud rate.

- Parameters   
	- port [in]: The CAN port. The first port is 0.
	- speed [in]: The bus baud rate.  please refer to VCIL_CAN_SPEED.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### vcil_can_set_speed_listen_mode
- Syntax  
````
int vcil_can_set_speed_listen_mode(byte port, int speed)
````

- Description  
  Set the specified CAN port bus baud rate at Listen mode. This mode setup controller only listen data and not ACK bus.

- Parameters  
	- port [in]: The CAN port. The first port is 0.
	- speed [in]: The bus baud rate. The detail please refer to vcil_can_speed.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  Some of bitrate is user-define value for customize , when you setup the specified bitrate and using vcil_can_get_bitrate may get user-define value

#### vcil_can_get_speed
- Syntax  
````
int vcil_can_get_speed(byte port, int[] speed, int[] mode)
````

- Description  
  Set the specified CAN port bus baud rate.

- Parameters  
	- port [in]: The CAN port. The first port is 0.
	- speed [out]: The bus baud rate.  please refer to VCIL_CAN_SPEED.
	- mode [out]: Current mode.  please refer to VCIL_CAN_BUS_MODE.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### vcil_can_get_bus_error_status
- Syntax  
````
int vcil_can_get_bus_error_status(byte port, VCIL_CAN_ERROR_STATUS status)
````

- Description  
  Get the specified CAN port error status. this API can be using to detect bus error status.

- Parameters  
	- port [in]: The CAN port. The first port is 0.
	- status [out]: Instance of VCIL_CAN_ERROR_STATUS which is used to store the CAN error status

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  none

#### vcil_can_set_mask
- Syntax  
````
int vcil_can_set_mask(byte port, VCIL_CAN_MASK mask)
````

- Description  
  Set the specified CAN message filter to specified filter bank of specified CAN port and enable it.

- Parameters  
	- port [in]: The CAN port. The first port is 0.
	- mask [in]: The mask configuration. please refer to VCIL_CAN_MASK.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  The field values of mask should be set base on the type of CAN protocol you want to apply to.
  You can set two filters in the a filter bank if you set filter for CAN2.0A and can set one filter  in the a filter bank if you set filter for CAN2.0B.

  For example,
  If you want to set a CAN2.0A message filters to filter bank 0, you must set the "bank" field of mask to 0 and set "extended frame" field to 0(FALSE). In this case you are allowed to set two filters in filter bank 0.  Set the values of first filter to "id1" and "mask1" field and the second filter to "id2" and "mask2".

  If you want to set a CAN2.0B message filters to filter bank 1, you must set the "bank" field of mask to 1 and set "extended frame" field to 1(TRUE). In this case you are allowed to set only one filter in filter bank 1.  Set the value of filter to "id1" and "mask1" field. The values of  "id2" and "mask2" will be ignored.

#### vcil_can_get_mask
- Syntax  
````
int vcil_can_get_mask(byte port, VCIL_CAN_MASK mask)
````

- Description  
  Get the a CAN message filter from specified filter bank of specified CAN port.

- Parameters  
	- port [in]: The CAN port. The first port is 0.
	- mask [out]: The mask configuration. please refer to VCIL_CAN_MASK.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  You must set the "bank" field of mask to specified which  filter bank you want to get from.

#### vcil_can_remove_mask
- Syntax  
````
int vcil_can_remove_mask(byte port, byte bank)
````

- Description  
  Remove a filter from specified filter bank of specified CAN port

- Parameters  
	- port [in]: The CAN port. The first port is 0.
	- bank [in]: The bank of the mask to be removed.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  If all bank are removed there will be no rule for passing CAN message. In order words, no CAN message can passed the filter and be received by the APP.
  
#### vcil_can_reset_mask
- Syntax  
````
int vcil_can_reset_mask(byte port)
````

- Description  
  Reset all filter bank of the specified CAN port

- Parameters  
	- port [in]: The CAN port. The first port is 0.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  After reset, all bank will be cleared and disabled. 
  The MCU firmware will automatically add a filter of mask 0 and id 0 to filter bank 0 which means pass all CAN message without checking ant bit of the identifier. All CAN message will pass through this filter and be received by the APP.

#### vcil_can_set_event_handler
- Syntax  
````
int vcil_can_set_event_handler(Handler handler)
````

- Description  
  Set handler which handles CAN message received event.

- Parameters  
	- handler [in]: An instance of Handler.

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  Please refer to the usage guide and sample code for details.
  On alarm event triggered, the Handler will receive message with the "what" field equals to VCIL_EVENT_ID_RECEIVED_MSG_CAN

#### vcil_can_unset_event_handler
- Syntax  
````
int vcil_can_unset_event_handler()
````

- Description  
  Unregister handler of CAN message received event.

- Parameters  
  none

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  Please refer to the usage guide and sample code for details.

#### vcil_can_wait_event
- Syntax  
````
int vcil_can_wait_event(boolean status)
````

- Description  
  Allow/Disallow VCIL to pass CAN message received event to registered handler when a message is pushed into SDK internal buffer.

- Parameters  
	- status [in]: The status of whether VCIL should pass CAN message received event to registered handler. TRUE: Inform FLASE: Not to inform

- Returns  
  **MRM_ERR_NO_ERROR** - On success.
  Otherwise see the error code list.

- Remarks  
  Please refer to the usage guide and sample for details.


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
- **(0x00000014) MRM_ERR_DEVICE_NOT_EXIST** - Encounter the cradle not attached.
- **(0x10000001) MRM_ERR_ANDROID_JNI_NULL_POINTER** - Null pointer error occurred on service side(JNI). 
- **(0x10000002) MRM_ERR_ANDROID_JNI_OUT_OF_MEMORY** - Out of memory error occurred on service side(JNI).
- **(0x10000003) MRM_ERR_ANDROID_JNI_EVENT_INIT_FAILED** - Failed to init event handle on service side(JNI).
- **(0x10000004) MRM_ERR_ANDROID_JNI_EVENT_DEINIT_FAILED** - Failed to init event handle on service side(JNI).
- **(0x10000005) MRM_ERR_ANDROID_JNI_EVENT_LISTENING_THREAD_ALREADY_RUNNING** - Event listening thread in service is already running(JNI).
- **(0x10000006) MRM_ERR_ANDROID_JNI_EVENT_LISTENING_THREAD_CREATE_FAILED** - Failed to create event listening thread in service(JNI).

## VCIL Error
- **(0x03000001) MRM_ERR_VCIL_DEVICE_NODE_OPEN_FAIL** - Open VCIL device node fail. Please checkout VCIL is exist or the device not use by another application.
- **(0x03000002) MRM_ERR_VCIL_DEVICE_NODE_WRITE_FAIL** - Encounter write operation fail. Please retry operation.
- **(0x03000003) MRM_ERR_VCIL_DEVICE_NODE_READ_FAIL** - Encounter read operation fail. Please retry operation.
- **(0x03000004) MRM_ERR_VCIL_IS_BUSY** - Encounter library is busy. Please retry operation.
- **(0x03000008) MRM_ERR_VCIL_DEVICE_NODE_READ_TIMEOUT** - Encounter read operation timeout. Please retry operation.
- **(0x03000009) MRM_ERR_VCIL_DATA_NOT_READY** - Encounter data buffer empty.
- **(0x03000010) MRM_ERR_VCIL_CAN_FILTER_NOT_ENABLE** - This return code indicate this bank CAN filter not enable.
