package mrm.demo.util;

public class ErrorCode {
    static  public String MSG_TAG_ERROR = "ERR";

    static final public int MRM_ERR_NO_ERROR                                             = 0x00000000;
    static final public int MRM_ERR_UNSUPPORT_OPERATION                                  = 0x00000003;
    static final public int MRM_ERR_IVCP_GSENSOR_DATA_NOT_READY                          = 0x01000012;
    static final public int MRM_ERR_ANDROID_JNI_EVENT_LISTENING_THREAD_ALREADY_RUNNING   = 0x10000006;
    static final public int MRM_ERR_DEVICE_NOT_EXIST                                        = 0x00000014;

    static public String errorCodeToString(int errorCode) {
        if(errorCode == MRM_ERR_UNSUPPORT_OPERATION) {
            return String.format("UNSUPPORT OPERATION");
        } else if(errorCode == MRM_ERR_DEVICE_NOT_EXIST) {
            return String.format("DEVICE NOT EXIST");
        } else {
            return String.format("%s - 0x%08X", MSG_TAG_ERROR, errorCode);
        }
    }
}
