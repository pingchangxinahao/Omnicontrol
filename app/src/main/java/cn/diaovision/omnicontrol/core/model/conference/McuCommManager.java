package cn.diaovision.omnicontrol.core.model.conference;


import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cn.diaovision.omnicontrol.BaseCyclicThread;
import cn.diaovision.omnicontrol.conn.TcpClient;
import cn.diaovision.omnicontrol.core.message.conference.BaseMessage;
import cn.diaovision.omnicontrol.core.message.conference.ConfConfigMessage;
import cn.diaovision.omnicontrol.core.message.conference.McuMessage;
import cn.diaovision.omnicontrol.core.message.conference.ReqMessage;
import cn.diaovision.omnicontrol.core.message.conference.ResMessage;
import cn.diaovision.omnicontrol.core.message.conference.UserMessage;
import cn.diaovision.omnicontrol.rx.RxExecutor;
import cn.diaovision.omnicontrol.rx.RxMessage;
import cn.diaovision.omnicontrol.rx.RxReq;
import cn.diaovision.omnicontrol.rx.RxSubscriber;
import cn.diaovision.omnicontrol.rx.RxThen;
import cn.diaovision.omnicontrol.util.ByteBuffer;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/** MCU communication manager
 * Created by liulingfeng on 2017/4/17.
 */

public class McuCommManager {
    private final static int RECV_BUFF_LEN = 65535; //buffer length for receiving
    private final static int ACK_TIMEOUT = 5000; //ACK timeout (in ms)
    private final static int QUEUE_LEN = 10;

//    private BlockingQueue<McuBundle> txQueue;

    private LinkedList<McuBundle> ackList;
    private ReentrantLock ackListLock;

    ByteBuffer recvBuff;

    TcpClient client;

    List<BaseCyclicThread> threadList;

    CommListener commListener;

    public McuCommManager(Mcu mcu){
        client = new TcpClient(mcu.ip, mcu.port);

        recvBuff = new ByteBuffer(RECV_BUFF_LEN);

        ackList = new LinkedList<>();
        ackListLock = new ReentrantLock();
        threadInit();
    }

    /*
     * connect to mcu server
     */
    public void connect(RxSubscriber subscriber){
        RxExecutor.getInstance().post(new RxReq() {
            @Override
            public RxMessage request() {
                client.connect();
                if (client.getState() == TcpClient.STATE_CONNECTED) {
                    threadStart();
                    return new RxMessage(RxMessage.CONNECTED);
                }
                else if (client.getState() == TcpClient.STATE_DISCONNECTED){
                    return new RxMessage(RxMessage.DISCONNECTED);
                }
                else {
                    return new RxMessage(RxMessage.DISCONNECTED);
                }
            }
        }, subscriber, RxExecutor.SCH_IO, RxExecutor.SCH_ANDROID_MAIN, 2000);
    }

    /* ***************************************************
     * disconnect will return immediately
     * ***************************************************/
    public void disconnect(){
        RxExecutor.getInstance().post(new RxReq() {
            @Override
            public RxMessage request() {
                client.disconnect();
                return null;
            }
        }, RxExecutor.SCH_IO);

        threadStop();
    }

    public void sendSequential(final List<McuBundle> bundleList, final RxSubscriber subscriber){
        if (client.getState() == TcpClient.STATE_CONNECTED) {
            Flowable.fromIterable(bundleList)
                    .concatMap(new Function<McuBundle, Publisher<?>>() {
                        @Override
                        public Publisher<?> apply(McuBundle mcuBundle) throws Exception {
                            final McuMessage msg = mcuBundle.msg;
                            final RxReq req = mcuBundle.req;
                            return Flowable.create(new FlowableOnSubscribe<McuMessage>() {
                                @Override
                                public void subscribe(FlowableEmitter<McuMessage> e) throws Exception {
                                    int res = client.send(msg.toBytes());
                                    if (res > 0) {
//                                    e.onNext(msg);
                                    } else {
                                        e.onError(new IOException());
                                    }
                                }
                            }, BackpressureStrategy.BUFFER)
                                    .map(new Function<McuMessage, RxMessage>() {
                                        @Override
                                        public RxMessage apply(McuMessage mcuMessage) throws Exception {
                                            if (!mcuMessage.requiresAck()) {
                                                return new RxMessage(RxMessage.DONE);
                                            }
                                            while (true) {
                                                McuMessage ackMsg = findAndPopAck(mcuMessage);
                                                if (ackMsg != null) {
                                                    req.request(); //work after done
                                                    return new RxMessage(RxMessage.ACK, ackMsg.getSubmsg());
                                                }
                                                Thread.sleep(20);
                                            }
                                        }
                                    })
                                    .timeout(ACK_TIMEOUT, TimeUnit.MILLISECONDS);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subscriber);
        }
        else {
            Flowable.just("")
                    .map(new Function<String, RxMessage>() {
                        @Override
                        public RxMessage apply(String s) throws Exception {
                            throw new IOException();
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subscriber);
        }

    }

    public void send(final McuMessage msg, final RxSubscriber subscriber) {
        if (client.getState() == TcpClient.STATE_CONNECTED) {
            Flowable.just("")
                    //send
                    .map(new Function<String, McuMessage>() {
                        @Override
                        public McuMessage apply(String s) throws Exception {
                            int res = client.send(msg.toBytes());
                            if (res > 0) {
                                return msg;
                            } else {
                                throw new IOException();
                            }
                        }
                    })
                    //wait ack
                    .map(new Function<McuMessage, RxMessage>() {
                        @Override
                        public RxMessage apply(McuMessage mcuMessage) throws Exception {
                            if (!mcuMessage.requiresAck()) {
                                return new RxMessage(RxMessage.DONE);
                            }
                            while (true) {
                                McuMessage ackMsg = findAndPopAck(mcuMessage);
                                if (ackMsg != null) {
                                    return new RxMessage(RxMessage.ACK, ackMsg.getSubmsg());
                                }
                                Thread.sleep(20);
                            }
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .timeout(ACK_TIMEOUT, TimeUnit.MILLISECONDS)
                    .subscribe(subscriber);
        }
        else {
            Flowable.just("")
                    .map(new Function<String, RxMessage>() {
                        @Override
                        public RxMessage apply(String s) throws Exception {
                            throw new IOException();
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subscriber);
        }
    }


    private void threadStart(){
        boolean hasInited = true;
        for (BaseCyclicThread thread  : threadList){
            if (thread == null) {
                hasInited = false;
                break;
            }
        }

        if (!hasInited){
            threadInit();
        }

        for (BaseCyclicThread thread : threadList){
            thread.start();
        }

    }

    private void threadStop(){
        for (BaseCyclicThread thread : threadList){
            thread.quit();
        }
        threadList.clear();

        ackListLock.lock();
        try {
            ackList.clear();
        }
        finally {
            ackListLock.unlock();
        }

        recvBuff.flush();
    }

    private void threadInit(){

        ackListLock.lock();
        try {
            ackList.clear();
        }
        finally {
            ackListLock.unlock();
        }

        recvBuff.flush();

        threadStop();

        BaseCyclicThread checkConnectThread = new BaseCyclicThread() {
            @Override
            public void work() {
                try {
                    Thread.sleep(1000);
                    if (client.getState() != TcpClient.STATE_CONNECTED){
                        disconnect();
                        connect(null);
                    }
                    if (commListener != null){
                        commListener.onConnectionChanged(client.getState());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        threadList.add(checkConnectThread);

        //client receive thread
        BaseCyclicThread recvThread = new BaseCyclicThread() {
            @Override
            public void work() {
                byte[] rxdata = new byte[1024];
                int len = client.recv(rxdata);
                if (len > 0) {
                    recvBuff.push(rxdata, len);
                }

                final McuMessage msg = McuMessage.parse(recvBuff);

                if (msg != null) {
                    if (msg.getType() == McuMessage.TYPE_RES) {
                        ackListLock.lock();
                        try {
                            McuBundle bundle = new McuBundle();
                            bundle.msg = msg;
                            bundle.timeRecv = System.currentTimeMillis();
                            bundle.subscriber = null;
                            ackList.add(bundle);
                        } finally {
                            ackListLock.unlock();
                        }
                    }
                    else {
                        //send other types of messages to listener
                        if (commListener != null){
                            commListener.onRecv(msg);
                        }
                    }
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        threadList.add(recvThread);

        //a single thread to pulse
        BaseCyclicThread pulseThread = new BaseCyclicThread() {
            @Override
            public void work() {
                try {
                    Thread.sleep(1000);
                    McuBundle bundle = new McuBundle();
                    bundle.msg = McuMessage.buildLogin("term", "123456");
                    bundle.subscriber = null;
                    send(bundle.msg, new RxSubscriber<RxMessage>() {
                        @Override
                        public void onRxResult(RxMessage msg) {
                            BaseMessage resMsg = (BaseMessage) msg.val;
                        }
                        @Override
                        public void onRxError(Throwable e) {
                            if (commListener != null) {
                                commListener.onConnectionChanged(client.getState());
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        threadList.add(pulseThread);

        //dieout recv ack messages
        BaseCyclicThread msgDieoutThread = new BaseCyclicThread() {
            @Override
            public void work(){

                ackListLock.lock();
                try {
                    McuBundle bundle = ackList.getFirst();
                    if (System.currentTimeMillis() - bundle.timeRecv > ACK_TIMEOUT) {
                        ackList.removeFirst();
                    }
                }
                finally {
                    ackListLock.unlock();
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        threadList.add(msgDieoutThread);
    }

    /* ************************************************************
     *find ACK given the req message, and pop it from the list
     * ************************************************************/
    public McuMessage findAndPopAck(McuMessage msg) {
        McuBundle bundle = null;

        if (msg.getType() == McuMessage.TYPE_REQ) {
            ReqMessage reqMsg = (ReqMessage) msg.getSubmsg();
            ackListLock.lock();
            try {
                boolean foundReq = false;
                int idx = -1;
                for (int m = 0; m < ackList.size(); m++) {
                    McuBundle bb = ackList.get(m);
                    if (bb.msg.getSubtype() == ResMessage.CONF_ALL && reqMsg.getType() == ReqMessage.REQ_CONF_ALL) {
                        foundReq = true;
                        idx = m;
                        break;
                    } else if (bb.msg.getSubtype() == ResMessage.CONF && reqMsg.getType() == ReqMessage.REQ_CONF) {
                        foundReq = true;
                        idx = m;
                        break;
                    } else if (bb.msg.getSubtype() == ResMessage.TERM_ALL && reqMsg.getType() == ReqMessage.REQ_TERM_ALL) {
                        foundReq = true;
                        idx = m;
                        break;
                    } else if (bb.msg.getSubtype() == ResMessage.CONF_CONFIG && reqMsg.getType() == ReqMessage.REQ_CONF_CONFIGED) {
                        foundReq = true;
                        idx = m;
                        break;
                    }
                }

                if (foundReq && idx > 0) {
                    bundle = ackList.get(idx);
                    ackList.remove(idx);
                }
            } finally {
                ackListLock.unlock();
            }
        } else if (msg.getType() == McuMessage.TYPE_CREATE_CONF) {
//            ConfConfigMessage confConfigMessage = (ConfConfigMessage) msg.getSubmsg();
            ackListLock.lock();
            try {
                boolean foundReq = false;
                int idx = -1;
                for (int m = 0; m < ackList.size(); m++) {
                    McuBundle bb = ackList.get(m);
                    if (bb.msg.getSubtype() == ResMessage.CREATE_CONF) {
                        foundReq = true;
                        idx = m;
                        break;
                    }
                }
                if (foundReq && idx > 0) {
                    bundle = ackList.get(idx);
                    ackList.remove(idx);
                }
            } finally {
                ackListLock.unlock();
            }
        } else if (msg.getType() == McuMessage.TYPE_USER){
//            UserMessage userMsg = (UserMessage) msg.getSubmsg();
            ackListLock.lock();
            try {
                boolean foundReq = false;
                int idx = -1;
                for (int m = 0; m < ackList.size(); m++) {
                    McuBundle bb = ackList.get(m);
                    if (bb.msg.getSubtype() == ResMessage.USER) {
                        foundReq = true;
                        idx = m;
                        break;
                    }
                }
                if (foundReq && idx > 0) {
                    bundle = ackList.get(idx);
                    ackList.remove(idx);
                }
            } finally {
                ackListLock.unlock();
            }
        }

        if (bundle != null) {
            return bundle.msg;
        } else {
            return null;
        }

    }

    public void setCommListener(CommListener listener) {
        this.commListener = listener;
        McuBundle bundle = new McuBundle();
    }

    /*
     * this interface handles non-async calling （返回非异步访问的并发回调接口）
     */
    public interface CommListener{

        //tcp连接问题
        void onConnectionChanged(int state);

        //处理MCU的转发报文
        void onRecv(McuMessage rxMessage);
    }

    static public class McuBundle{
        long timeRecv;
        McuMessage msg;
        RxSubscriber subscriber;
        RxReq req;
    }

}


