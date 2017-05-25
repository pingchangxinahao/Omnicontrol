package cn.diaovision.omnicontrol.model;

import java.util.Date;

/**
 * Created by liulingfeng on 2017/4/24.
 */

public interface Config {
    String getMainName();
    String getMainPasswd();

    String getConfName();
    String getConfPasswd();

    String getMcuIp();
    int getMcuPort();


    //Matrix attributes
    int getMatrixId();
    String getMatrixIp();
    int getMatrixUdpIpPort();
    String getMatrixPreviewIp();
    int getMtatrixPreviewIpPort();

    //get the port where the preview channel is plugged on to the matrix
    int getMatrixPreviewPort();

    int getMatrixInputVideoNum();
    int getMatrixOutputVideoNum();

    byte getSubtitleFontSize();
    byte getSubtitleFontColor();

    Date getConfStartDate();
    Date getConfEndDate();
}
