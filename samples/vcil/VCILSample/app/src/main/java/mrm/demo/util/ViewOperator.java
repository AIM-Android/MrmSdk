package mrm.demo.util;

import android.widget.TextView;

public class ViewOperator {

    static public void setLabelText(TextView lab, String str) {
        int txtColor = 0xFFFFFFFF;

        if(
            str.contains("OK")          ||
            str.compareTo("OK") == 0    ||
            str.compareTo("TRUE") == 0  ||
            str.compareTo("ON") == 0    ||
            str.compareTo("ENABLE") == 0      ) {

            txtColor = 0xFF00AA00; //GREEN

        } else if(
            str.compareTo("FALSE") == 0     ||
            str.compareTo("OFF") == 0       ||
            str.compareTo("DISABLE") == 0      ) {

            txtColor = 0xFFFF0000; //RED

        } else if(
            str.contains(ErrorCode.MSG_TAG_ERROR)) {
            txtColor = 0xFFFF00FF; //PURPLE
        }

        else {
            txtColor = 0xFF000000; //BLACK

        }

        lab.setTextColor(txtColor);
        lab.setText(str);
    }

}
