package cn.diaovision.omnicontrol.core.message.conference;

import java.util.Date;
import java.util.List;

import cn.diaovision.omnicontrol.core.model.conference.LiveConf;
import cn.diaovision.omnicontrol.core.model.conference.Term;
import cn.diaovision.omnicontrol.util.ByteUtils;
import cn.diaovision.omnicontrol.util.DateHelper;

/****************************************************************
 * 视频会议消息协议
 * data formats:
 * 1. byte stores 1 byte data
 * 2. int stores 2 bytes data
 * 3. long stores 4 bytes data
 * 4. String stores byte array (size specified in toBytes())
 * Created by liulingfeng on 2017/2/22.
 * TODO: create seperate Message class files in package conference
 ****************************************************************/

public class McuMessage {
    public final static byte VERSION = 0x01;

    public final static byte TYPE_REQ = 1;
    public final static byte TYPE_RES = 2;
    public final static byte TYPE_CREATE_CONF = 3;
    public final static byte TYPE_INVITE_TERM = 4;
    public final static byte TYPE_USER = 5; //login
    public final static byte TYPE_ADD_CONF = 6;
    public final static byte TYPE_ADD_TERM = 7;
    public final static byte TYPE_ADD_ADDRBOOK = 8;
    public final static byte TYPE_DEL_ADDRBOOK = 9;
    public final static byte TYPE_VER_INFO = 10;
    public final static byte TYPE_SPEAKER = 11; //发言
    public final static byte TYPE_CONFIG = 12;
    public final static byte TYPE_UPGRAGE_MSG = 13;
    public final static byte TYPE_UPGRAGE_MSG_RSP = 14;
    public final static byte TYPE_CHAIRUSER = 0x25;
    public final static byte TYPE_MCMPINFO = 15;
    //远遥，摄像头远程控制，Added by jinyuhe,2010.12.24
    public final static byte TYPE_REMOTECTRL = 20;
    //注册请求，提交注册码(license)，Added by jinyuhe,2011.3.21
    public final static byte TYPE_REGISTER = 21;
    //流媒体操作请求，Added by jinyuhe,2011.4.19
    public final static byte TYPE_STREAMMEDIA = 22;
    //终端控制请求，Added by jinyuhe,2011.8.4
    public final static byte TYPE_TERM_CTRL = 23;
    //简单消息扩展，Added by jinyuhe,2013.3.5
    public final static byte TYPE_REQ_EXTEND = 24;

    //WEB-->MCU Manager，WEB响应MCU Manager的主席请求消息，
    //在处理时按照MCU Manager的请求消息来处理
    public final static byte TYPE_RES_APPLYCHAIR = 0x68; //申请主席
    public final static byte TYPE_RES_FREECHAIR = 0x69; //释放主席
    public final static byte TYPE_RES_RECOVERYCHAIR = 0x6A; //回收主席，此消息由WEB发起
    public final static byte TYPE_RES_APPLYFLOOR = 0x6B; //申请发言
    public final static byte TYPE_RES_CANCELFLOOR = 0x6C; //取消发言
    //Ended

    private Header header;
    private byte[] payload;

    public McuMessage(Header header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    public byte[] toBytes(){
        byte[] bytes = new byte[header.toBytes().length + payload.length];
        System.arraycopy(header, 0, bytes, 0, header.toBytes().length);
        System.arraycopy(payload.length, 0, bytes, header.toBytes().length, payload.length);
        return bytes;
    }

    static public McuMessage buildLogin(String name, String password){
        if (name.length() == 0 || password.length() == 0){
            return null;
        }

        byte[] payload = new byte[65];
        payload[0] = UserMessage.LOGIN;
        System.arraycopy(name.getBytes(), 0, payload, 1, name.length() > 32 ? 32 : name.length());
        System.arraycopy(password.getBytes(), 0, payload, 32, password.length() > 32 ? 32 : password.length());
        Header header = new Header(65, TYPE_USER);
        return new McuMessage(header, payload);
    }

    /* **********************************
     * 获得所有会议信息
     * **********************************/
    static public McuMessage buildReqConfAll(){
        int payloadLen = 1 + 2 + 100*4;


        Header header = new Header(payloadLen, TYPE_REQ);

        ReqMessage reqMsg = new ReqMessage(ReqMessage.REQ_CONF_ALL);

        return new McuMessage(header, reqMsg.toBytes());
    }


    /* **********************************
     * 获得某个会议信息
     * **********************************/
    static public McuMessage buildReqConf(int id){
        int payloadLen = 1 + 2 + 100*4;
        Header header = new Header(payloadLen, TYPE_REQ);

        ReqMessage reqMsg = new ReqMessage(ReqMessage.REQ_CONF);
        reqMsg.confId = id;

        return new McuMessage(header, reqMsg.toBytes());
    }


    /* **********************************
     * 获得已配置会议信息
     * **********************************/
    static public McuMessage buildReqConfConfiged(){
        int payloadLen = 1 + 2 + 100*4;
        Header header = new Header(payloadLen, TYPE_REQ);

        ReqMessage reqMsg = new ReqMessage(ReqMessage.REQ_CONF_CONFIGED);

        return new McuMessage(header, reqMsg.toBytes());
    }


    /* **********************************
     * 获得所有参会终端信息
     * **********************************/
    static public McuMessage buildReqTermAll(int confId){
        int payloadLen = 1 + 2 + 100*4;
        Header header = new Header(payloadLen, TYPE_REQ);

        ReqMessage reqMsg = new ReqMessage(ReqMessage.REQ_TERM_ALL);
        reqMsg.confId = confId;

        return new McuMessage(header, reqMsg.toBytes());
    }


    /* **********************************
     * 请求创建会议
     * **********************************/
    static public McuMessage buildReqCreateConf(Date dateStart, Date dateEnd, ConfConfigMessage template){
        DateHelper h = DateHelper.getInstance();
        int yS = h.getYear(dateStart);
        int mS = h.getMonth(dateStart);
        int dS = h.getDay(dateStart);
        int hS = h.getHour(dateStart);
        int minS = h.getMin(dateStart);

        int yE = h.getYear(dateEnd);
        int mE = h.getMonth(dateEnd);
        int dE = h.getDay(dateEnd);
        int hE = h.getHour(dateEnd);
        int minE = h.getMin(dateEnd);

        ConfConfigMessage confConfigMsg = ConfConfigMessage.copyFrom(template);
        confConfigMsg.startYear = yS;
        confConfigMsg.startMonth = (byte) mS;
        confConfigMsg.startDay = (byte) dS;
        confConfigMsg.startHour = (byte) hS;
        confConfigMsg.startMin = (byte) minS;

        confConfigMsg.endYear = yE;
        confConfigMsg.endMonth = (byte) mE;
        confConfigMsg.endDay = (byte) dE;
        confConfigMsg.endHour = (byte) hE;
        confConfigMsg.endMin = (byte) minE;

        confConfigMsg.termNum = 0;
        confConfigMsg.termAttrNum = 0;

        int payloadLen = confConfigMsg.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_CREATE_CONF);

        return new McuMessage(header, confConfigMsg.toBytes());
    }

//    /* **********************************
//     * 请求结束会议
//     * **********************************/
//    public McuMessage buildReqDeleteConfAll(){
//        ReqMessage reqMessage = new ReqMessage();
//        reqMessage.type = ReqMessage.DEL_CONF;
//        int payloadLen = reqMessage.calcMessageLength();
//        Header header = new Header(payloadLen, TYPE_REQ);
//
//        return new McuMessage(header, reqMessage.toBytes());
//    }

    /* **********************************
     * 请求结束会议 (with confId)
     * **********************************/
    static public McuMessage buildReqDeleteConf(int confId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.DEL_CONF);
        reqMessage.confId = confId;
        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);

        return new McuMessage(header, reqMessage.toBytes());
    }

    /* **********************************
     * 请求发送会议视频流到本地端口
     * **********************************/
    static public McuMessage buildReqStream(int confId, byte type, String localIp){

        StreamMediaMessage streamMediaMessage = new StreamMediaMessage();
        streamMediaMessage.type = type;
        streamMediaMessage.ipAddr = ByteUtils.ip2num(localIp); //本机IP
        streamMediaMessage.videoPort = 6002;
        streamMediaMessage.audioPort = 6000;


        int payloadLen = streamMediaMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_STREAMMEDIA);

        return new McuMessage(header, streamMediaMessage.toBytes());
    }

    /* **********************************
     * 请求静音参会者 (with confId and termId)
     * **********************************/
    static public McuMessage buildReqMute(int confId, int termId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.DISABLE_INPUT);
        reqMessage.confId =  confId;
        reqMessage.termId[0] = termId;

        return new McuMessage(new Header(1, TYPE_REQ), reqMessage.toBytes());
    }

    /* **********************************
     * 请求开启声音参会者 (with mcu and confConfig)
     * **********************************/
     static public McuMessage buildReqUnmute(int confId, int termId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.ENABLE_INPUT);
        reqMessage.confId =  confId;
        reqMessage.termId[0] = termId;

        return new McuMessage(new Header(1, TYPE_REQ), reqMessage.toBytes());
    }


    /* **********************************
     * 请求参会者画面广播
     * **********************************/
    static public McuMessage buildReqTermBroadcast(int confId, long termId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.SWITCH_MEDIA);
        reqMessage.confId =  confId;
        reqMessage.termId[0] = termId;

        return new McuMessage(new Header(1, TYPE_REQ), reqMessage.toBytes());
    }

    /* **********************************
     * 请求参会者画面为选看端（明确一下功能）
     * **********************************/
    static public McuMessage buildReqTermSelectView(int confId, long termId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.ASSIGN_SELECTVIEW);
        reqMessage.confId =  confId;
        reqMessage.termId[0] = termId;

        return new McuMessage(new Header(1, TYPE_REQ), reqMessage.toBytes());
    }

    /* **********************************
     * 请求参会者发言
     * **********************************/
    static public McuMessage buildReqTermSpeach(int confId, long termId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.CALLOVER);
        reqMessage.confId =  confId;
        reqMessage.termId[0] = termId;

        return new McuMessage(new Header(1, TYPE_REQ), reqMessage.toBytes());
    }

    /* **********************************
     * 请求参会者取消发言
     * TODO: check format
     * **********************************/
    static public McuMessage buildReqTermCancelSpeach(int confId, long termId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.CANCEL_FLOOR);
        reqMessage.confId =  confId;
        reqMessage.termId[0] = termId;

        return new McuMessage(new Header(1, TYPE_REQ), reqMessage.toBytes());
    }


    /* **********************************
     * 请求控制参会者
     * **********************************/
    public McuMessage buildReqAttendCtrl(int confId, int termId, long termIp, byte msgType, long ctrlVal){
        TermCtrlMessage termCtrlMessage = new TermCtrlMessage(msgType);
        termCtrlMessage.confId = confId;
        termCtrlMessage.termId = termId;
        termCtrlMessage.termIp = termIp;
        termCtrlMessage.val = ctrlVal;

        int payloadLen = termCtrlMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_TERM_CTRL);
        return new McuMessage(header, termCtrlMessage.toBytes());
    }

    /* **********************************
     * 请求邀请参会者
     * **********************************/
    public McuMessage buildReqInviteTerm(int confId, long termId){
        TermConfigMessage termConfigMessage = new TermConfigMessage();
        termConfigMessage.id = termId;
        termConfigMessage.port = confId;
        termConfigMessage.termType = 1;

        int payloadLen = termConfigMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_TERM_CTRL);
        return new McuMessage(header, termConfigMessage.toBytes());
    }


    /* **********************************
     * 请求挂断参会者
     * **********************************/
    public McuMessage buildReqHangupTerm(int confId, long termId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.HANGUP_TERM);
        reqMessage.confId = confId;
        reqMessage.termId[0] = termId;
        int paylaodLen = reqMessage.calcMessageLength();
        Header header = new Header(paylaodLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }


    /* **********************************
     * 请求控制远程摄像头（目前不使用）
     * **********************************/
    @Deprecated
    public McuMessage buildReqCameralCtrl(int confId, long termId){
        return null;
    }

    /* **********************************
     * 请求改变会议模式（修改多画面轮训间隔）
     * **********************************/
    public McuMessage buildReqChangeConfConfig(int confId, byte confType, int interval){
        ReqMessage reqMessage = new ReqMessage(confType);
        reqMessage.confId = confId;
        reqMessage.termId[0] = interval;
        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }

    /* **********************************
     * 启动或停止双流(需要明确一下功能)
     * **********************************/
    public McuMessage buildReqDS(int confId, boolean enabled){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.MIX_AUDIO);
        reqMessage.confId = confId;
        if (enabled) {
            reqMessage.termId[0] = 1;
        }
        else {
            reqMessage.termId[0] = 0;
        }

        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }

    /* **********************************
     * 启动混音(最多4个终端)
     * **********************************/
    public McuMessage buildReqConfAudioMix(int confId, List<Integer> termIdList){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.MIX_AUDIO);
        reqMessage.confId = confId;

        if (termIdList.size() > 4){
            for (int m = 0 ; m < 4; m ++){
                reqMessage.termId[m] = termIdList.get(m);
            }
        }
        else {
            for (int m = 0 ; m < termIdList.size(); m ++){
                reqMessage.termId[m] = termIdList.get(m);
            }
        }

        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }

    /* **********************************
     * 取消混音 (所有的termid置零)
     * **********************************/
    public McuMessage buildReqCancelConfAudioMix(int confId) {
        ReqMessage reqMessage = new ReqMessage(ReqMessage.REQ_DS);
        reqMessage.confId = confId;

        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }


    /* **********************************
     * 终端主席操作应答消息
     * **********************************/
    public McuMessage buildReqChairResponse(int confId, long termId, byte msgType, long msgVal) {
        ReqMessage reqMessage = new ReqMessage(msgType);
        reqMessage.confId = confId;
        reqMessage.termId[0] = termId;
        reqMessage.termId[1] = msgVal;

        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }

    public McuMessage buildReqConfVideoMix(int confId, List<Integer> termIdList){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.MIX_VIDEO);
        reqMessage.confId = confId;

        if (termIdList.size() > 36){
            for (int m = 0 ; m < 36; m ++){
                reqMessage.termId[m] = termIdList.get(m);
            }
        }
        else {
            for (int m = 0 ; m < termIdList.size(); m ++){
                reqMessage.termId[m] = termIdList.get(m);
            }
        }

        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }


    public McuMessage buildReqCancelConfVideoMix(int confId){
        ReqMessage reqMessage = new ReqMessage(ReqMessage.MIX_VIDEO);
        reqMessage.confId = confId;

        int payloadLen = reqMessage.calcMessageLength();
        Header header = new Header(payloadLen, TYPE_REQ);
        return new McuMessage(header, reqMessage.toBytes());
    }

    /* **********************************
     * 请求会议终端多画面轮巡(暂时不用)
     * **********************************/
//    public McuMessage buildReqConfMultiPic(int confId, int  chairmanId, List<Attend> attends) {
//            return new McuMessage(buildHeader(0, VERSION, REQ_CONF), new byte[0]).toBytes();
//    }


//    /* **********************************
//     * 请求会议终端取消多画面轮巡(暂时不用)
//     * **********************************/
//    public byte[] buildReqCancelConfMultiPic(String confId, String chairmanId, List<Attend> attends) {
//            return new McuMessage(buildHeader(0, VERSION, REQ_CONF), new byte[0]).toBytes();
//    }



    /*TODO: check if this is used*/
    static public class ResInfoMessage{
        public final static int CONFINFO = 1;
        public final static int TERMINFO = 2;
        public final static int CONFDATA = 3;
        public final static int TERMDATA = 4;
        public final static int USERLIST = 5;
    }

    /*******************************************************************
     * 各个信息体结构
     *******************************************************************/




    /* ********************
     * 终端信息
     * ********************/
    static public class TermInfoMessage{
        byte termNum;
        Term.Config[] termConfig; //maximum 256
    }

    /* ********************
     * 会议信息
     * ********************/
    static public class ConfInfoMessage{
        byte confNum;
        LiveConf.Config[] confConfig; //maximum 32
    }


//            //Added by jinyuhe，2012.8.9
//            //定义发给终端的主席功能申请应答结果字
//            #define CHAIR_RES_IS_CHAIRMAN           0x5000  //该终端已经是主席
//            #define CHAIR_RES_REJECT_APPLYCHAIR     0x5001  //拒绝主席申请
//            #define CHAIR_RES_ACK_APPLYCHAIR        0x5002  //同意主席申请
//            #define CHAIR_RES_ISNOT_CHAIRMAN        0x5010  //该终端不是主席，不用释放
//            #define CHAIR_RES_REJECT_FREECHAIR      0x5011  //拒绝释放主席
//            #define CHAIR_RES_ACK_FREECHAIR         0x5012  //同意释放主席
//            #define CHAIR_RES_IS_SPEECHER           0x5020  //该终端正在发言
//            #define CHAIR_RES_REJECT_APPLYSPEECH    0x5021  //拒绝申请发言
//            #define CHAIR_RES_ACK_APPLYSPEECH       0x5022  //同意申请发言
//            #define CHAIR_RES_ISNOT_SPEECHER        0x5030  //该终端没有发言
//            #define CHAIR_RES_REJECT_CANCELSPEECH   0x5031  //拒绝取消发言
//            #define CHAIR_RES_ACK_CANCELSPEECH      0x5032  //同意取消发言
//            //Ended by jinyuhe，2012.8.9
}
