# MRM SDK

## MRM SDK Overview
The MRM (Mobile Resource Management) SDK is a set of software libraries which provides APIs for controlling various functions of the target device.
The following figure describes the software stack of MRM SDK:
![](https://github.com/AIM-Android/MrmSdkSample/blob/main/images/overview.png)


MRM SDK is composed of the following function domains:

### IVCP (Intelligent Vehicle Co-Processor)
A VPM(Vehicle Power Management) MCU(Micro Controller Unit) is embedded in the device, which controls the power status of device and peripheral devices such as G-Sensor and P-Sensors. 
The IVCP function domain is designed in client-service architecture. The IVCP Service acts as a proxy to access the VPM MCU and is able to serve multiple APP processes simultaneously. In your APP, you can use the IVCP APIs exported in the MRM Client APIs Library to communicate with the IVCP service.

**IVCP Service Client API Modules:**
- **Firmware APIs** - Get VPM MCU firmware version. Save/Load default settings
- **Power Management APIs** - VPM related functions. ex: boot control, Ignition control, event delay adjustments, low battery protection and etc.
- **Battery APIs** - Backup battery related information and functions
- **Alarm APIs** - Internal RTC time setting and device alarm wakeup related functions.
- **Watchdog APIs** - Watch dog functions.
- **Peripheral Control APIs** - Power status management of peripheral devices.
- **Storage APIs** - Internal EEPROM storage access.
- **G Sensor APIs** - Access G sensor data. G sensor related settings.
- **G Sensor Alarm APIs** - G sensor device wakeup functions.
- **P Sensor APIs** - Access P sensor data.


### SDP (Smart Display Panel)
Depends on the specific device spec, the device may bundle with a smart display panel module. The smart display panel module is embedded with a MCU to control functions of the module. Similar with IVCP function domain, the SDP function domain is also designed in client-service architecture. You can use the SDP APIs exported in the MRM Client APIs Library to communicate with the SDP service.

**SDP Service Client API Modules:**
- **Firmware APIs**  - Get SDP MCU firmware version. Save/Load default settings
- **Backlight APIs** - Configure brightness of smart display.
- **Sensor APIs** - Access sensor on smart display
- **Hotkey APIs** - hotkeys related settings.
- **Speaker API** - Speaker related settings
- **USB API** - USB port related settings

### VCIL (Vehicle Communication Interface Layer)
A VCIM(Vehicle Communication Interface Module) MCU is embedded in the device for controlling the vehicle communication protocols (e.g. CAN, J1939, OBD2, J1708, J1587). For the performance considerations, the VCIL function domain is designed in form of libraries, You can use the VCIL APIs exported in the VCIL API Library to control the MCU directly. For VCIL does not has service layer, the VCIL API Library does NOT support multi-process access.

**VCIL API Modules:**
- **VCIL APIs** - Get VCIL MCU firmware version. Physical port protocol settings.
- **CAN APIs** - Read / write data with CAN protocol.
- **J1939 APIs** - Read / write data with J1939 protocol.
- **OBD2 APIs** - Read / write data with OBD2 protocol.
- **J1708 APIs** - Read / write data with J1708 protocol.
- **J1587 APIs** - Read / write data with J1587 protocol.

## How MRM SDK Works
### IVCP and SDP
IVCP and SDP functions in the MRM SDK for Android is designed in client-service architecture. 

To make your APP work with the MRM services to control the device you must first include the Service Client API library into you APP project. Before calling APIs to control the device, you must first "bind" you APP process to the MRM service processes. After binding is done, you can then call the IVCP, SDP APIs to communicate with the services. The MRM services act as proxies for client APP to access the hardware functions.

Due to the nature of client-service structure, the MRM SDK  for Android supports multi-processes access. It is available for the services to serve multiple application processes at the same time. The hardware resources are managed by the services and the client application does not need to worry about hardware resource occupation.
![](https://github.com/AIM-Android/MrmSdkSample/blob/main/images/client-service_architecture.png)

### VCIL
VCIL functions in the MRM SDK for Android is designed in form of libraries.

Before calling APIs to control the device, you must first call the initialization API to make the VCIM MCU ready to work. After initialization is done, you can then call the VCIL APIs to do operations of vehicle protocols.
![](https://github.com/AIM-Android/MrmSdkSample/blob/main/images/libraries_architecture.png)






