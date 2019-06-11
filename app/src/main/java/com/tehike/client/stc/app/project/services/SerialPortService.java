package com.tehike.client.stc.app.project.services;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.db.DbHelper;
import com.tehike.client.stc.app.project.db.DbUtils;
import com.tehike.client.stc.app.project.entity.OpenBoxParamater;
import com.tehike.client.stc.app.project.entity.SipBean;
import com.tehike.client.stc.app.project.entity.SysInfoBean;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.phone.Linphone;
import com.tehike.client.stc.app.project.phone.SipManager;
import com.tehike.client.stc.app.project.plandutyentity.PlanDutyBean;
import com.tehike.client.stc.app.project.thread.HandlerAmmoBoxThread;
import com.tehike.client.stc.app.project.thread.SendAlarmToServerThread;
import com.tehike.client.stc.app.project.utils.ByteUtil;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.NetworkUtils;
import com.tehike.client.stc.app.project.utils.ScreenUtils;
import com.tehike.client.stc.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.stc.app.project.utils.StringUtils;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;
import com.tehike.client.stc.app.project.utils.TimeUtils;
import com.tehike.client.stc.app.project.utils.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerDelegate;
import cn.nodemedia.NodePlayerView;

/**
 * 描述：串口管理工具类
 * ttyACM0子弹箱
 * ttyACM1 摇杆和串口屏
 * ttyACM2 键盘和灯
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2019/5/15 11:31
 */

public class SerialPortService extends Service {

    /**
     * 摇杆串口管理对象
     */
    SerialPortManager rockerSerialPortManager;

    /**
     * 键盘串口管理对象
     */
    SerialPortManager keyBoardSerialPortManager;

    /**
     * 弹箱串口管理对象
     */
    SerialPortManager ammoboxSerialPortManager;

    /**
     * 摇杆串口对象
     */
    Device rockerSerialPortDevice;

    /**
     * 键盘串口对象
     */
    Device keyBoardSerialPortDevice;

    /**
     * 弹箱串口对象
     */
    Device ammoBoxSerialPortDevice;

    /**
     * 弹箱串口是否打开标识
     */
    boolean isOpenAmmoBoxSerialSuccess = false;

    /**
     * 键盘串口是否打开标识
     */
    boolean isOpenKeyBoardSerialPortFlag = false;

    /**
     * 摇杆串口是否打开标识
     */
    boolean isOpenRockerSerialPortFlag = false;

    /**
     * 摇杆指令
     */
    String rockerSingleCommand = "";

    /**
     * 键盘指令
     */
    String keyBoardSingleCommand = "";

    /**
     * 弹箱定时刷新线程池服务
     */
    ScheduledExecutorService timingRequestAmmoStatusService = null;

    /**
     * 本机的哨位Id
     */
    String sentryId = "";

    /**
     * 是否正在通话
     */
    boolean isCallingFlag = false;

    /**
     * 盛放键值的集合
     */
    List<Integer> keyBoardNumList = new ArrayList<>();

    /**
     * 系统相机设备
     */
    android.hardware.Camera mCamer = null;

    /**
     * 广播监听人脸比对结果的广播
     */
    FaceComparisonResultBroadcast mFaceComparisonResult;

    /**
     * 当前的功能描述(预览弹箱，申请供弹)
     */
    TextView ammoBoxPreviewFunctionDescLayout;

    /**
     * 提示结果（申请发送失败or成功or拒绝）
     */
    TextView ammoBoxResultLayout;

    /**
     * udp服务是否运行的标识
     */
    boolean udpServerIsRun = false;

    @Override
    public void onCreate() {
        EventBus.getDefault().register(this);
        //注册广播接收人脸比对结果
        registerFaceComparisonResultBroadcast();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //初始化串口管理类
                initSerialPortManager();
                //初始化摇杆
                initRockerSerialPort();
                //初始化键盘
                initKeyboardSerialPort();
                //初始化弹箱
                initAmmoBoxSerialPort();
                //udp监听值班室直接开启弹箱
                initAmmoboxUdpService();
            }
        }).start();
        super.onCreate();
    }

    /**
     * udp监听值班室直接开启弹箱
     */
    private void initAmmoboxUdpService() {
        udpServerIsRun = true;
        int udpServerPort = 6655;
        try {
            DatagramSocket udpServerSocket = new DatagramSocket(udpServerPort);
            while (udpServerIsRun) {
                DatagramPacket datagramPacket = new DatagramPacket(new byte[72], 72);
                udpServerSocket.receive(datagramPacket);
                byte[] data = datagramPacket.getData();
                String udpFromIp = datagramPacket.getAddress().getHostAddress();
                String sendIp = data[24] + "." + data[25] + "." + data[26] + "." + data[27];
                String dutyIp = findDutyIp();
                Logutil.d(Arrays.toString(data) + "\n" + udpFromIp + "\n" + sendIp + "\n" + dutyIp);
                if (dutyIp.equals(udpFromIp) || dutyIp.equals(sendIp)) {
                    openAmmoBox();
                }
            }
        } catch (Exception e) {
            Logutil.e("Udp异常:" + e.getMessage());
        }
    }

    /**
     * 查找值班室Ip
     */
    private String findDutyIp() {
        List<SipBean> list = App.getSipS();
        if (list == null || list.isEmpty())
            return "";
        for (SipBean sipBean : list) {
            if (sipBean.getSentryId().equals("0")) {
                return sipBean.getIpAddress();
            }
        }
        return "";
    }

    /**
     * 注册广播接收人脸
     */
    private void registerFaceComparisonResultBroadcast() {
        mFaceComparisonResult = new FaceComparisonResultBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("faceComparisonResultAction");
        this.registerReceiver(mFaceComparisonResult, intentFilter);

    }

    /**
     * 初始化串口管理对象
     */
    private void initSerialPortManager() {
        rockerSerialPortManager = new SerialPortManager();
        keyBoardSerialPortManager = new SerialPortManager();
        ammoboxSerialPortManager = new SerialPortManager();
    }

    /**
     * 初始化摇杆
     */
    private void initRockerSerialPort() {
        String ygSelected = (String) SharedPreferencesUtils.getObject(App.getApplication(), "ygserialport", "");
        if (TextUtils.isEmpty(ygSelected)) {
            rockerSerialPortDevice = new Device("ttyACM1", "", new File("/dev/ttyACM1"));
            SharedPreferencesUtils.putObject(App.getApplication(), "ygserialport", GsonUtils.GsonString(rockerSerialPortDevice));
        } else {
            rockerSerialPortDevice = GsonUtils.GsonToBean(ygSelected, Device.class);
        }
        //ttyACM1
        boolean openSerialPort = rockerSerialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
            @Override
            public void onDataReceived(byte[] bytes) {
                //byte[]转可见字符串
                String header = ByteUtil.ByteArrToHex(bytes).trim().replace(" ", "");
                rockerSingleCommand += header;
                if (rockerSingleCommand.startsWith("FF") && rockerSingleCommand.length() == 18) {
                    EventBus.getDefault().post(rockerSingleCommand);
                    rockerSingleCommand = "";
                } else if (rockerSingleCommand.length() >= 18 && rockerSingleCommand.endsWith("06") && rockerSingleCommand.length() % 18 == 0) {
                    rockerSingleCommand = "";
                }
            }

            @Override
            public void onDataSent(byte[] bytes) {
            }
        }).openSerialPort(rockerSerialPortDevice.getFile(), 9600);

        isOpenRockerSerialPortFlag = openSerialPort;
        Logutil.i("摇杆串口初始化--->>>" + isOpenRockerSerialPortFlag);

        //初始化小屏
        if (AppConfig.KEYBOARD_WITH_SCREEN)
            initSmallScreen();
    }

    /**
     * 初始化键盘
     */
    private void initKeyboardSerialPort() {
        //取出本地保存的串口标识
        String keyboardSelected = (String) SharedPreferencesUtils.getObject(App.getApplication(), "keyboardserialport", "");
        if (TextUtils.isEmpty(keyboardSelected)) {
            keyBoardSerialPortDevice = new Device("ttyACM2", "", new File("/dev/ttyACM2"));
            SharedPreferencesUtils.putObject(App.getApplication(), "keyboardserialport", GsonUtils.GsonString(keyBoardSerialPortDevice));
        } else {
            keyBoardSerialPortDevice = GsonUtils.GsonToBean(keyboardSelected, Device.class);
        }
        //ttyACM2（测试）
        boolean openSerialPort = keyBoardSerialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
            @Override
            public void onDataReceived(byte[] bytes) {
                String header = ByteUtil.ByteArrToHex(bytes).trim();
                keyBoardSingleCommand += header.trim().replace(" ", "");
                if (keyBoardSingleCommand.startsWith("FA") && keyBoardSingleCommand.length() == 4) {
                    //处理键盘数据
                    handlerKeyBoardCommand();
                } else if (keyBoardSingleCommand.length() > 4) {
                    //防止串口数据紊乱
                    keyBoardSingleCommand = "";
                }
            }

            @Override
            public void onDataSent(byte[] bytes) {
            }
        }).openSerialPort(keyBoardSerialPortDevice.getFile(), 9600);

        isOpenKeyBoardSerialPortFlag = openSerialPort;
        Logutil.i("键盘串口初始化--->>>" + isOpenKeyBoardSerialPortFlag);
        //警灯开机自检
        if (isOpenKeyBoardSerialPortFlag && !AppConfig.KEYBOARD_WITH_SCREEN)
            deviceBootCheckLight();
    }

    /**
     * 初始化弹箱
     */
    private void initAmmoBoxSerialPort() {
        //取出本地保存的串口标识
        ammoBoxSerialPortDevice = new Device("ttyACM0", "", new File("/dev/ttyACM0"));
        //打开com0（测试）
        boolean openSerialPort = ammoboxSerialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
            @Override
            public void onSuccess(File device) {
                //Logutil.d("1设备成功" + device.getName());
            }

            @Override
            public void onFail(File device, OnOpenSerialPortListener.Status status) {
                Logutil.d("1设备失败" + status);
            }
        })
                .setOnSerialPortDataListener(new OnSerialPortDataListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                    @Override
                    public void onDataReceived(byte[] bytes) {
                        String header = ByteUtil.ByteArrToHex(bytes).trim().replace(" ", "");
                        //防止串口数据紊乱，只查看最后两个字符
                        if (header.length() > 2) {
                            header = header.substring(header.length() - 2, header.length());
                        }
                        //  Logutil.d("header:"+header);
                        //判断开关
                        if (header.equals("37") || header.equals("C5")) {
                            //     Logutil.e("关");
                            if (AppConfig.AMMO_STATUS != 1) {
                                App.startSpeaking("子弹箱已关闭");
                            }
                            AppConfig.AMMO_STATUS = 1;
                        } else if (header.equals("D6") || header.equals("35") || header.equals("1A")) {
                            if (AppConfig.AMMO_STATUS != 0) {
//                                if (!isOpenNormal && header.equals("1A")) {
//                                    App.startSpeaking("子弹箱非法打开");
//                                } else {
                                App.startSpeaking("子弹箱已开启");
//                                    isOpenNormal = false;
//                                }
                            }
                            //   Logutil.d("开");
                            AppConfig.AMMO_STATUS = 0;
                        }
                    }

                    @Override
                    public void onDataSent(byte[] bytes) {
                        //  Logutil.d("Send:" + Arrays.toString(bytes));
                    }
                }).setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
                    @Override
                    public void onSuccess(File device) {
                        isOpenAmmoBoxSerialSuccess = true;
                    }

                    @Override
                    public void onFail(File device, Status status) {
                        isOpenAmmoBoxSerialSuccess = false;
                        Logutil.e("isOpenAmmoBoxSerialSuccess:" + isOpenAmmoBoxSerialSuccess + "\n" + status);
                    }
                }).openSerialPort(ammoBoxSerialPortDevice.getFile(), 9600);

        isOpenAmmoBoxSerialSuccess = openSerialPort;

        //开启定时服务，每秒查询一次子弹箱状态
        if (isOpenAmmoBoxSerialSuccess) {
            if (timingRequestAmmoStatusService == null) {
                timingRequestAmmoStatusService = Executors.newSingleThreadScheduledExecutor();
            }
            //子线程延迟5秒（防止点击登录就tts播报弹箱状态）
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timingRequestAmmoStatusService.scheduleWithFixedDelay(new TimingRequestAmmoStatusThread(), 0, 1000, TimeUnit.MILLISECONDS);
                }
            }).start();
        }
        Logutil.i("弹箱串口初始化--->>>" + isOpenAmmoBoxSerialSuccess);
    }

    /**
     * 子线程向子弹箱串口发送数据查询当前状态
     */
    class TimingRequestAmmoStatusThread extends Thread {
        @Override
        public void run() {
            if (isOpenAmmoBoxSerialSuccess != false && ammoboxSerialPortManager != null) {
                byte data[] = new byte[8];
                data[0] = (byte) 0xFE;
                data[1] = 04;
                data[2] = 03;
                data[3] = (byte) 0xE8;
                data[4] = 00;
                data[5] = 06;
                data[6] = (byte) 0xE4;
                data[7] = (byte) 0x77;
                boolean isSendSuccess = ammoboxSerialPortManager.sendBytes(data);
            }
        }
    }

    /**
     * 初始化键盘小屏
     */
    private void initSmallScreen() {
        if (isOpenRockerSerialPortFlag) {
            sendSerialScreenTime();
            cleanSerialScreenPort();
            sendSerialScreenPort("设备启动成功!");
            smallScreenReset(1500);
        }
    }

    /**
     * 清空屏数据
     */
    public void cleanSerialScreenPort() {
        try {

            byte[] contentBytes = "                                                                             ".getBytes("GBK");

            byte[] buf = new byte[6 + contentBytes.length];
            buf[0] = (byte) 0x5A;
            buf[1] = (byte) 0xA5;
            buf[2] = (byte) (0x03 + contentBytes.length);
            buf[3] = (byte) 0x82;
            buf[4] = ((byte) (0x1000 >> 8));
            buf[5] = (byte) 0x1000;
            for (int i = 0; i < contentBytes.length; i++) {
                buf[6 + i] = contentBytes[i];
            }
            if (rockerSerialPortManager != null && buf.length > 0) {
                boolean isCleanSuccess = rockerSerialPortManager.sendBytes(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置小屏时间
     */
    public void sendSerialScreenTime() {
        Calendar calendar = Calendar.getInstance();
        int year = (calendar.get(Calendar.YEAR) - 2000) >= 0 ? (calendar.get(Calendar.YEAR) - 2000) : 00;
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        int sec = calendar.get(Calendar.SECOND);
        byte[] buf = new byte[14];
        buf[0] = (byte) 0x5A;
        buf[1] = (byte) 0xA5;
        buf[2] = (byte) 0x0B;
        buf[3] = (byte) 0x82;
        buf[4] = (byte) 0x00;
        buf[5] = (byte) 0x9C;
        buf[6] = (byte) 0x5A;
        buf[7] = (byte) 0xA5;
        buf[8] = (byte) year;
        buf[9] = (byte) month;
        buf[10] = (byte) day;
        buf[11] = (byte) hour;
        buf[12] = (byte) min;
        buf[13] = (byte) sec;
        if (rockerSerialPortManager != null && isOpenRockerSerialPortFlag && buf != null) {
            boolean isSetTimeSuccess = rockerSerialPortManager.sendBytes(buf);
            Logutil.i("isSetTimeSuccess:" + isSetTimeSuccess);
        }
    }

    /**
     * 发送数据
     */
    public void sendSerialScreenPort(String content) {
        try {
            byte[] contentBytes = content.getBytes("GBK");
            int contentLenth = contentBytes.length;
            //需要添加的空格数
            int needSpace = (24 - contentLenth) / 2 + 26;
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < needSpace; i++) {
                stringBuffer.append(" ");
            }
            //新生成的带格式的内容
            String newContent = stringBuffer.toString() + content;
            byte[] newContentBytes = newContent.getBytes("GBK");

            byte[] buf = new byte[6 + newContentBytes.length];
            buf[0] = (byte) 0x5A;
            buf[1] = (byte) 0xA5;
            buf[2] = (byte) (0x03 + newContentBytes.length);
            buf[3] = (byte) 0x82;
            buf[4] = ((byte) (0x1000 >> 8));
            buf[5] = (byte) 0x1000;
            for (int i = 0; i < newContentBytes.length; i++) {
                buf[6 + i] = newContentBytes[i];
            }
            if (rockerSerialPortManager != null && isOpenRockerSerialPortFlag && buf.length > 0) {
                boolean isSendSuccess = rockerSerialPortManager.sendBytes(buf);
                // Logutil.i("isSendSuccess:" + isSendSuccess);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 小屏显示信息重置
     */
    private void smallScreenReset(final int time) {
        if (isCallingFlag) {
            return;
        }
        //本地缓存的Sip字典
        List<SipBean> allCacheList = null;

        //本机的Guid
        String nativieGuid = SysinfoUtils.getSysinfo().getDeviceGuid();
        //所有的sip集合
        allCacheList = App.getSipS();
        //比对本机的哨位ID
        if (allCacheList != null && !allCacheList.isEmpty() && !TextUtils.isEmpty(nativieGuid)) {
            for (SipBean bean : allCacheList) {
                if (!TextUtils.isEmpty(bean.getId()) && bean.getId().equals(nativieGuid)) {
                    sentryId = bean.getSentryId();
                    break;
                }
            }
        } else {
            sentryId = "";
        }
        //子线程延时
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                cleanSerialScreenPort();
                sendSerialScreenPort("哨号台(" + sentryId + ")");
            }
        }).start();
    }

    /**
     * 处理键盘指令
     */
    private void handlerKeyBoardCommand() {
        //判断是否有键盘串口屏
        if (AppConfig.KEYBOARD_WITH_SCREEN) {
            if (keyBoardSingleCommand.equals("FA67")) {
                //应急报警
                App.startSpeaking("发送应急报警");
                cleanSerialScreenPort();
                sendSerialScreenPort("发送应急报警");
                sendAlarmToServer("应急");
            } else if (keyBoardSingleCommand.equals("FA66")) {
                //紧急供弹
                App.startSpeaking("发送紧急供弹");
                cleanSerialScreenPort();
                sendSerialScreenPort("紧急供弹");
                //断网时离线开锁
                if (!NetworkUtils.isConnected()) {
                    handler.sendEmptyMessage(21);
                } else {
                    //子线程去申请供弹
                    Message message = new Message();
                    message.what = 10;
                    message.obj = "applyOpenAmmoBox";
                    handler.sendMessage(message);
                }
            } else if (keyBoardSingleCommand.equals("FA65")) {
                //呼叫上级
                makeCallDutyRoom();
            } else if (keyBoardSingleCommand.equals("FA28")) {
                cleanSerialScreenPort();
                //脱逃
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType1", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendSerialScreenPort("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    //脱逃
                    sendSerialScreenPort("脱逃报警");
                    App.startSpeaking("发送脱逃报警");
                    sendAlarmToServer("脱逃");
                }
            } else if (keyBoardSingleCommand.equals("FA38")) {
                cleanSerialScreenPort();
                //袭击报警
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType3", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendSerialScreenPort("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    sendSerialScreenPort("发送袭击报警");
                    App.startSpeaking("发送袭击报警");
                    sendAlarmToServer("袭击");
                }
            } else if (keyBoardSingleCommand.equals("FA30")) {
                cleanSerialScreenPort();
                //挟持报警
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType5", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendSerialScreenPort("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    sendSerialScreenPort("发送挟持报警");
                    App.startSpeaking("发送挟持报警");
                    sendAlarmToServer("挟持");
                }
            } else if (keyBoardSingleCommand.equals("FA29")) {
                cleanSerialScreenPort();
                //暴狱报警
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType2", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendSerialScreenPort("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    //脱逃
                    sendSerialScreenPort("发送暴狱报警");
                    App.startSpeaking("发送暴狱报警");
                    sendAlarmToServer("暴狱");
                }
            } else if (keyBoardSingleCommand.equals("FA39")) {
                cleanSerialScreenPort();
                //自然报警
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType4", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendSerialScreenPort("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    //脱逃
                    sendSerialScreenPort("发送自然灾害报警");
                    App.startSpeaking("发送灾害报警");
                    sendAlarmToServer("灾害");
                }
            } else if (keyBoardSingleCommand.equals("FA31")) {
                cleanSerialScreenPort();
                //突发报警
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType6", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendSerialScreenPort("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    sendSerialScreenPort("发送突发报警");
                    App.startSpeaking("发送突发报警");
                    sendAlarmToServer("突发");
                }
            } else if (keyBoardSingleCommand.equals("FA2A")) {
                //报警解除
                App.startSpeaking("报警解除");
                cleanSerialScreenPort();
                sendSerialScreenPort("报警解除");
                smallScreenReset(1000);
            } else if (keyBoardSingleCommand.equals("FA2B")) {
                //检查弹箱
                App.startSpeaking("检查蛋箱");
                cleanSerialScreenPort();
                sendSerialScreenPort("检查弹箱");
                Message message = new Message();
                message.what = 10;
                message.obj = "ammoboxPreview";
                handler.sendMessage(message);
            } else if (keyBoardSingleCommand.equals("FA3A")) {
                //勤务上哨
                cleanSerialScreenPort();
                sendSerialScreenPort("勤务上哨");
                smallScreenReset(1500);
                handler.sendEmptyMessage(26);
            } else if (keyBoardSingleCommand.equals("FA32")) {
                //勤务下哨
                App.startSpeaking("勤务下哨");
                cleanSerialScreenPort();
                sendSerialScreenPort("勤务下哨");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA34")) {
                //本级查勤
                App.startSpeaking("本级察勤");
                cleanSerialScreenPort();
                sendSerialScreenPort("本级查勤");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA3C")) {
                //上级查勤
                App.startSpeaking("上级察勤");
                cleanSerialScreenPort();
                sendSerialScreenPort("上级查勤");
                smallScreenReset(1500);
                handler.sendEmptyMessage(17);
            } else if (keyBoardSingleCommand.equals("FA1D")) {
                //分屏模式
                App.startSpeaking("分屏模式");
                App.getApplication().sendBroadcast(new Intent(AppConfig.CUSTOM_SCREEN_MODE));
                cleanSerialScreenPort();
                sendSerialScreenPort("分屏模式");
                smallScreenReset(1000);
            } else if (keyBoardSingleCommand.equals("FA15")) {
                //窗口选择
                App.startSpeaking("窗口选择");
                App.getApplication().sendBroadcast(new Intent(AppConfig.CUSTOM_WINDWN));
                cleanSerialScreenPort();
                sendSerialScreenPort("窗口选择");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA0D")) {
                //资源选择
                App.startSpeaking("资源选择");
                cleanSerialScreenPort();
                sendSerialScreenPort("资源选择");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA1C")) {
                //语音警告
                App.startSpeaking("语音警告");
                keyBoardVoiceWarring();
                cleanSerialScreenPort();
                sendSerialScreenPort("语音警告");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA14")) {
                //鸣枪警告
                App.startSpeaking("鸣枪警告");
                keyBoardGunshootWarring();
                cleanSerialScreenPort();
                sendSerialScreenPort("鸣枪警告");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA0C")) {
                //自主喊话
                App.startSpeaking("自主喊话");
                cleanSerialScreenPort();
                sendSerialScreenPort("自主喊话");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA04")) {
                //远程控制
                App.startSpeaking("远程控制");
                cleanSerialScreenPort();
                sendSerialScreenPort("远程控制");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA1B")) {
                //灯光开启
                keyBoardOpenLight();
            } else if (keyBoardSingleCommand.equals("FA13")) {
                //灯光关闭
                keyBoardCloseLight();
            } else if (keyBoardSingleCommand.equals("FA0B")) {
                //灯光云台
                keyBoardControlPtz();
            } else if (keyBoardSingleCommand.equals("FA03")) {
                //证件登记
                App.startSpeaking("证件登记");
                cleanSerialScreenPort();
                sendSerialScreenPort("证件登记");
                smallScreenReset(1500);
            } else if (keyBoardSingleCommand.equals("FA1A")) {
                keyBoardNumList.add(1);
                App.startSpeaking("1");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA19")) {
                keyBoardNumList.add(2);
                App.startSpeaking("2");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA18")) {
                keyBoardNumList.add(3);
                App.startSpeaking("3");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA12")) {
                keyBoardNumList.add(4);
                App.startSpeaking("4");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA11")) {
                keyBoardNumList.add(5);
                App.startSpeaking("5");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA10")) {
                keyBoardNumList.add(6);
                App.startSpeaking("6");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA0A")) {
                keyBoardNumList.add(7);
                App.startSpeaking("7");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA09")) {
                keyBoardNumList.add(8);
                App.startSpeaking("8");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA08")) {
                keyBoardNumList.add(9);
                App.startSpeaking("9");
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA01")) {
                keyBoardNumList.add(0);
                App.startSpeaking("0");
                cleanSerialScreenPort();
                sendSerialScreenPort(returnCurrentKey());
            } else if (keyBoardSingleCommand.equals("FA02")) {
                //确认
                keyBoardConfirm();
            } else if (keyBoardSingleCommand.equals("FA00")) {
                //取消
                App.startSpeaking("取消");
                cleanSerialScreenPort();
                sendSerialScreenPort("取消");
                clearKeyList();
                keyBoardFunctionCancel();
                smallScreenReset(1000);
            }
        } else {
            if (keyBoardSingleCommand.equals("FA0D")) {
                //取消
                keyBoardFunctionCancel();
            } else if (keyBoardSingleCommand.equals("FA0C")) {
                //分屏模式
                App.startSpeaking("分屏模式");
                App.getApplication().sendBroadcast(new Intent(AppConfig.CUSTOM_SCREEN_MODE));
            } else if (keyBoardSingleCommand.equals("FA14")) {
                //窗口选择
                App.startSpeaking("窗口选择");
                App.getApplication().sendBroadcast(new Intent(AppConfig.CUSTOM_WINDWN));
            } else if (keyBoardSingleCommand.equals("FA1C")) {
                //资源选择
                App.startSpeaking("资源选择");
            } else if (keyBoardSingleCommand.equals("FA2A")) {
                //勤务上哨
                handler.sendEmptyMessage(26);
            } else if (keyBoardSingleCommand.equals("FA2D")) {
                App.startSpeaking("勤务下哨");
            } else if (keyBoardSingleCommand.equals("FA3D")) {
                App.startSpeaking("上级察勤");
                handler.sendEmptyMessage(17);
            } else if (keyBoardSingleCommand.equals("FA3A")) {
                App.startSpeaking("本级察勤");
            } else if (keyBoardSingleCommand.equals("FA1B")) {
                //远程喊话
                App.startSpeaking("远程喊话");
            } else if (keyBoardSingleCommand.equals("FA13")) {
                //鸣枪警告
                keyBoardGunshootWarring();
            } else if (keyBoardSingleCommand.equals("FA0B")) {
                //语音警告
                keyBoardVoiceWarring();
            } else if (keyBoardSingleCommand.equals("FA08")) {
                //1
                keyBoardNumList.add(1);
                App.startSpeaking("1");
            } else if (keyBoardSingleCommand.equals("FA10")) {
                //2
                keyBoardNumList.add(2);
                App.startSpeaking("2");
            } else if (keyBoardSingleCommand.equals("FA18")) {
                //3
                keyBoardNumList.add(3);
                App.startSpeaking("3");
            } else if (keyBoardSingleCommand.equals("FA09")) {
                //4
                keyBoardNumList.add(4);
                App.startSpeaking("4");
            } else if (keyBoardSingleCommand.equals("FA11")) {
                //5
                keyBoardNumList.add(5);
                App.startSpeaking("5");
            } else if (keyBoardSingleCommand.equals("FA19")) {
                //6
                keyBoardNumList.add(6);
                App.startSpeaking("6");
            } else if (keyBoardSingleCommand.equals("FA0A")) {
                //7
                keyBoardNumList.add(7);
                App.startSpeaking("7");
            } else if (keyBoardSingleCommand.equals("FA12")) {
                //8
                keyBoardNumList.add(8);
                App.startSpeaking("8");
            } else if (keyBoardSingleCommand.equals("FA1A")) {
                //9
                keyBoardNumList.add(9);
                App.startSpeaking("9");
            } else if (keyBoardSingleCommand.equals("FA15")) {
                //0
                keyBoardNumList.add(0);
                App.startSpeaking("0");
            } else if (keyBoardSingleCommand.equals("FA1D")) {
                //确认
                keyBoardConfirm();
            } else if (keyBoardSingleCommand.equals("FA29")) {
                //检查弹柜
                App.startSpeaking("检查蛋箱");
                Message message = new Message();
                message.what = 10;
                message.obj = "ammoboxPreview";
                handler.sendMessage(message);
            } else if (keyBoardSingleCommand.equals("FA39")) {
                //申请供弹
                App.startSpeaking("申请供弹");
                Message message = new Message();
                message.what = 10;
                message.obj = "applyOpenAmmoBox";
                handler.sendMessage(message);
            } else if (keyBoardSingleCommand.equals("FA2C")) {
                //脱逃
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType1", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    App.startSpeaking("发送脱逃报警");
                    sendAlarmToServer("脱逃");
                }
            } else if (keyBoardSingleCommand.equals("FA34")) {
                //暴狱
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType2", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    App.startSpeaking("发送暴狱报警");
                    sendAlarmToServer("暴狱");
                }
            } else if (keyBoardSingleCommand.equals("FA3C")) {
                //袭击
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType3", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    App.startSpeaking("发送袭击报警");
                    sendAlarmToServer("袭击");
                }
            } else if (keyBoardSingleCommand.equals("FA2B")) {
                //自然
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType4", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    App.startSpeaking("发送自然灾害报警");
                    sendAlarmToServer("自然");
                }
            } else if (keyBoardSingleCommand.equals("FA33")) {
                //挟持
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType5", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    App.startSpeaking("发送挟持报警");
                    sendAlarmToServer("挟持");
                }
            } else if (keyBoardSingleCommand.equals("FA3B")) {
                //突发报警
                String type = (String) SharedPreferencesUtils.getObject(App.getApplication(), "alarmType6", "");
                if (!TextUtils.isEmpty(type)) {
                    App.startSpeaking("发送" + type + "报警");
                    sendAlarmToServer(type);
                } else {
                    App.startSpeaking("发送突发报警");
                    sendAlarmToServer("突发");
                }
            } else if (keyBoardSingleCommand.equals("FA31")) {
                //呼叫上级
                makeCallDutyRoom();
            } else if (keyBoardSingleCommand.equals("FA28")) {
                //灯光开启
                keyBoardOpenLight();
            } else if (keyBoardSingleCommand.equals("FA30")) {
                //灯光关闭
                keyBoardCloseLight();
            } else if (keyBoardSingleCommand.equals("FA38")) {
                //云台控制
                keyBoardControlPtz();
            }
        }
        //发送广播刷新副屏的事件列表和已处理的报警列表
        App.getApplication().sendBroadcast(new Intent(AppConfig.REFRESH_ACTION));
        keyBoardSingleCommand = "";
    }

    /**
     * 弹箱视频预览状态回调
     */
    private void previewAmmoBoxPlayerCallback(int event) {
        //状态回调判断
        if (event == 1102) {
            if (ammoboxVideoLoadingPrLayout != null)
                ammoboxVideoLoadingPrLayout.setVisibility(View.GONE);
            if (ammoboxVideoLoadingTvLayout != null)
                ammoboxVideoLoadingTvLayout.setVisibility(View.GONE);
        } else {
            if (ammoboxVideoLoadingPrLayout != null)
                ammoboxVideoLoadingPrLayout.setVisibility(View.VISIBLE);
            if (ammoboxVideoLoadingPrLayout != null) {
                ammoboxVideoLoadingTvLayout.setVisibility(View.VISIBLE);
                ammoboxVideoLoadingTvLayout.setTextSize(12);
                ammoboxVideoLoadingTvLayout.setTextColor(UIUtils.getColor(R.color.red));
            }
            if (event == 1000) {
                if (ammoboxVideoLoadingPrLayout != null)
                    ammoboxVideoLoadingTvLayout.setText("正在连接...");
            } else if (event == 1001) {
                if (ammoboxVideoLoadingPrLayout != null)
                    ammoboxVideoLoadingTvLayout.setText("连接成功...");
            } else if (event == 1104) {
                if (ammoboxVideoLoadingPrLayout != null)
                    ammoboxVideoLoadingTvLayout.setText("切换视频...");
            } else {
                if (ammoboxVideoLoadingPrLayout != null)
                    ammoboxVideoLoadingTvLayout.setText("重新连接...");
            }
        }
    }

    /**
     * 上级查勤时播放器的回调
     */
    private void superiorPlayerCallback(int event) {
        //状态回调判断
        if (event == 1102) {
            if (superiorCheckDutyPlayerViewLayout != null)
                superiorCheckDutyPlayerViewLayout.setVisibility(View.VISIBLE);
            if (superiorCheckDutyLoadingPrLayout != null)
                superiorCheckDutyLoadingPrLayout.setVisibility(View.GONE);
            if (superiorCheckDutyLoadingtvLayout != null)
                superiorCheckDutyLoadingtvLayout.setVisibility(View.GONE);
        } else {
            if (superiorCheckDutyLoadingPrLayout != null)
                superiorCheckDutyLoadingPrLayout.setVisibility(View.VISIBLE);
            if (superiorCheckDutyLoadingtvLayout != null) {
                superiorCheckDutyLoadingtvLayout.setVisibility(View.VISIBLE);
                superiorCheckDutyLoadingtvLayout.setTextSize(12);
                superiorCheckDutyLoadingtvLayout.setTextColor(UIUtils.getColor(R.color.red));
            }
            if (event == 1000) {
                if (superiorCheckDutyLoadingtvLayout != null)
                    superiorCheckDutyLoadingtvLayout.setText("正在连接...");
            } else if (event == 1001) {
                if (superiorCheckDutyLoadingtvLayout != null)
                    superiorCheckDutyLoadingtvLayout.setText("连接成功...");
            } else if (event == 1104) {
                if (superiorCheckDutyLoadingtvLayout != null)
                    superiorCheckDutyLoadingtvLayout.setText("切换视频...");
            } else {
                if (superiorCheckDutyLoadingtvLayout != null)
                    superiorCheckDutyLoadingtvLayout.setText("重新连接...");
            }
        }
    }

    /**
     * 弹箱视频预览计时
     */
    public void ammoboxPreviewThreadStart() {
        isTimingThreadWork = true;
        if (timingThread != null && timingThread.isAlive()) {
        } else {
            timingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isTimingThreadWork) {

                        try {
                            Thread.sleep(1 * 1000);
                            handler.sendEmptyMessage(8);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            timingThread.start();
        }
    }

    /**
     * 停止弹箱视频预览计时
     */
    public void ammoboxPreviewThreadStop() {
        if (isTimingThreadWork) {
            if (timingThread != null && timingThread.isAlive()) {
                timingThread.interrupt();
                timingThread = null;
            }
            timingNumber = 0;
            isTimingThreadWork = false;
        }
        //串口屏重置
        if (AppConfig.KEYBOARD_WITH_SCREEN)
            smallScreenReset(1200);
    }

    /**
     * 自动关闭弹箱预览
     */
    private void autoCloseAmmoboxPreview() {
        //弹箱预览计时
        timingNumber++;
        //显示剩余时间
        if (ammoboxVideoCountDownTvLayout != null) {
            //剩余时间
            int time = (15 - timingNumber);
            ammoboxVideoCountDownTvLayout.setText(Html.fromHtml("<b><font color = \"#ff0000\">" + time + "</font></b>秒后消失"));
        }
        //到时消失
        if (timingNumber == 15) {
            destoryAmmoBoxDialog();
        }
    }

    /**
     * 弹箱预览事件发送到服务器（日志记录）
     */
    private void sendAmmoBoxPreviewEventToServer(String action, Bitmap bitmap) {

        Logutil.d("哈哈。我要保存了");

    }

    //弹箱视频Url
    String ammoBoxVideoUrl = "";
    //面部视频url
    String ammoBoxFaceUrl = "";
    //面部视频截图url
    String ammoBoxFaceShotUrl = "";

    /**
     * 头像视频截图bitmap
     */
    Bitmap ammoboxFaceVideoShotBitmap;

    /**
     * 头像视频预览窗口
     */
    NodePlayerView faceVideoNodePlayerViewLayout;

    /**
     * 头像预览截图窗口
     */
    ImageView ammoboxFaceVideoShotPicLayout;

    /**
     * 弹箱倒计时提示
     */
    TextView ammoboxFaceVideoCountTimeLayout;

    /**
     * 弹箱预览的计时线程是否正在运行
     */
    boolean isTimingThreadWork = false;

    /**
     * 计时的子线程
     */
    Thread timingThread = null;

    /**
     * 计时
     */
    int timingNumber = 0;
    /**
     * 播放报警视频的播放器
     */
    NodePlayer ammoboxPreviewPlayer = null;

    /**
     * 面部播放器
     */
    NodePlayer ammoboxFaceVideoPlayer = null;

    /**
     * 预览窗口
     */
    Dialog ammoBoxPreviewDialog = null;

    /**
     * 播放器所有的View
     */
    NodePlayerView ammoboxPlayerNodeViewLayout = null;

    /**
     * 预览弹箱的信息
     */
    TextView ammoboxVideoPromptTvLayout;

    /**
     * 预览弹箱加载提示
     */
    TextView ammoboxVideoLoadingTvLayout;

    /**
     * 预览弹箱加载进度提示
     */
    ProgressBar ammoboxVideoLoadingPrLayout;

    /**
     * 弹箱预览计时
     */
    TextView ammoboxVideoCountDownTvLayout;

    /**
     * 面部视频截图倒计时
     */
    TextView ammoboxFaceCountDownTvLayout;

    /**
     * 子弹箱预览功能
     */
    private void previewAmmoBoxVideo(final String action) {
        //判断动作（预览弹窗,申请开启弹窗）
        if (TextUtils.isEmpty(action)) {
            App.startSpeaking("操作异常");
            return;
        }
        //本机sip对象
        currentSipBean = findNativeSipBean();
        if (currentSipBean == null)
            return;
        //弹箱面部视频
        if (currentSipBean.getVideoBean() != null) {
            ammoBoxFaceUrl = currentSipBean.getVideoBean().getRtsp();
            ammoBoxFaceShotUrl = currentSipBean.getVideoBean().getShotPicUrl();
        }
        //弹箱视频
        if (currentSipBean.getAmmoBean() != null)
            ammoBoxVideoUrl = currentSipBean.getAmmoBean().getRtsp();
        //用dialog显示
        final AlertDialog.Builder builder = new AlertDialog.Builder(App.getApplication());
        builder.setCancelable(false);
        View view = View.inflate(App.getApplication(), R.layout.ammo_box_preview_layout, null);
        builder.setView(view);
        //提示弹箱视频源
        ammoboxVideoPromptTvLayout = view.findViewById(R.id.display_box_item_video_info_layout);
        ammoboxVideoPromptTvLayout.setText("弹箱视频源");
        //弹箱视频加载提示
        ammoboxVideoLoadingTvLayout = view.findViewById(R.id.box_video_loading_tv_layout);
        //弹箱视频加载进度提示
        ammoboxVideoLoadingPrLayout = view.findViewById(R.id.box_video_loading_pr_layout);
        //视频倒计时提示
        ammoboxVideoCountDownTvLayout = view.findViewById(R.id.display_preview_box_time_layout);
        //面部视频view
        faceVideoNodePlayerViewLayout = view.findViewById(R.id.avator_preview_video_layout);
        //面部截图view
        ammoboxFaceVideoShotPicLayout = view.findViewById(R.id.authentication_avator_shotpic_layout);
        //倒计时提示
        ammoboxFaceVideoCountTimeLayout = view.findViewById(R.id.authentication_avator_time_layout);
        //倒计时截图
        ammoboxFaceCountDownTvLayout = view.findViewById(R.id.prompt_countdown_shotpic_tv_layout);
        //功能描述
        ammoBoxPreviewFunctionDescLayout = view.findViewById(R.id.ammobox_preview_function_desc_layout);
        //结果提示
        ammoBoxResultLayout = view.findViewById(R.id.ammobox_result_layout);
        //播放器视图
        ammoboxPlayerNodeViewLayout = view.findViewById(R.id.box_video_preview_view_layout);
        //播放弹箱视频
        ammoboxPreviewPlayer = new NodePlayer(App.getApplication());
        ammoboxPreviewPlayer.setPlayerView(ammoboxPlayerNodeViewLayout);
        ammoboxPreviewPlayer.setAudioEnable(false);
        ammoboxPreviewPlayer.setVideoEnable(true);
        ammoboxPreviewPlayer.setNodePlayerDelegate(new NodePlayerDelegate() {
            @Override
            public void onEventCallback(NodePlayer player, int event, String msg) {
                Message message = new Message();
                message.what = 7;
                message.arg1 = event;
                handler.sendMessage(message);
            }
        });
        ammoboxPreviewPlayer.setInputUrl(ammoBoxVideoUrl);
        ammoboxPreviewPlayer.start();
        //播放预览者面部视频
        ammoboxFaceVideoPlayer = new NodePlayer(App.getApplication());
        ammoboxFaceVideoPlayer.setPlayerView(faceVideoNodePlayerViewLayout);
        ammoboxFaceVideoPlayer.setAudioEnable(false);
        ammoboxFaceVideoPlayer.setVideoEnable(true);
        ammoboxFaceVideoPlayer.setNodePlayerDelegate(new NodePlayerDelegate() {
            @Override
            public void onEventCallback(NodePlayer player, int event, String msg) {
                Message message = new Message();
                message.what = 4;
                message.arg1 = event;
                handler.sendMessage(message);
            }
        });
        ammoboxFaceVideoPlayer.setInputUrl(ammoBoxFaceUrl);
        ammoboxFaceVideoPlayer.start();
        //显示dialog
        ammoBoxPreviewDialog = builder.create();
        ammoBoxPreviewDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        ammoBoxPreviewDialog.show();
        //通过当前的dialog获取window对象
        Window window = ammoBoxPreviewDialog.getWindow();
        //设置背景，防止变形
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = ScreenUtils.getInstance(App.getApplication()).getWidth() - 444;//两边设置的间隙相当于margin
        lp.height = ScreenUtils.getInstance(App.getApplication()).getHeight() - 220;//两边设置的间隙相当于margin
        lp.alpha = 0.9f;
        window.setDimAmount(0.7f);//使用时设置窗口后面的暗淡量
        window.setAttributes(lp);
        //开启计时
        ammoboxPreviewThreadStart();
        //倒计时截图
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 3; i >= 1; i--) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //提示剩余时间
                    Message message = new Message();
                    message.what = 6;
                    message.arg1 = i;
                    handler.sendMessage(message);
                    //截图
                    if (i == 1) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    HttpURLConnection connection = (HttpURLConnection) new URL(ammoBoxFaceShotUrl).openConnection();
                                    connection.setRequestMethod("GET");
                                    connection.setReadTimeout(3000);
                                    connection.setConnectTimeout(3000);
                                    connection.connect();
                                    if (connection.getResponseCode() == 200) {
                                        InputStream inputStream = connection.getInputStream();
                                        ammoboxFaceVideoShotBitmap = BitmapFactory.decodeStream(inputStream);
                                        if (ammoboxFaceVideoShotBitmap != null) {
                                            handler.sendEmptyMessage(5);
                                        } else {
                                            ammoboxFaceVideoShotBitmap = null;
                                        }
                                        connection.disconnect();
                                    } else {
                                        ammoboxFaceVideoShotBitmap = null;
                                    }
                                } catch (Exception e) {
                                    ammoboxFaceVideoShotBitmap = null;
                                }
                            }
                        }).start();
                        handler.sendEmptyMessage(9);
                        //把预览弹箱事件发送到服务器（日志记录）
                        sendAmmoBoxPreviewEventToServer(action, ammoboxFaceVideoShotBitmap);
                    }
                }
            }
        }).start();

        switch (action) {
            case "applyOpenAmmoBox":
                if (ammoBoxPreviewFunctionDescLayout != null)
                    ammoBoxPreviewFunctionDescLayout.setText("申请供弹");
                eventRecord("申请供弹");
                if (AppConfig.AMMO_STATUS != 0) {
                    OpenBoxParamater openBoxParamater = new OpenBoxParamater();
                    openBoxParamater.setFalg("ReqB");
                    openBoxParamater.setBoxId(SysinfoUtils.getSysinfo().getDeviceGuid());
                    new Thread(new HandlerAmmoBoxThread(openBoxParamater, 0)).start();
                    App.startSpeaking("申请已发出");
                    if (AppConfig.KEYBOARD_WITH_SCREEN) {
                        cleanSerialScreenPort();
                        sendSerialScreenPort("申请已发出");
                        smallScreenReset(1000);
                    }
                } else {
                    App.startSpeaking("申请供弹失败");
                    if (ammoBoxResultLayout != null) {
                        ammoBoxResultLayout.setText("申请供弹失败\t 请检查弹箱是否已开启");
                    }
                    if (AppConfig.KEYBOARD_WITH_SCREEN) {
                        cleanSerialScreenPort();
                        sendSerialScreenPort("申请供弹失败");
                        smallScreenReset(5000);
                    }
                }
                break;
            case "ammoboxPreview":
                if (ammoBoxPreviewFunctionDescLayout != null)
                    ammoBoxPreviewFunctionDescLayout.setText("弹箱预览");
                eventRecord("预览弹箱");
                break;
        }


//        applyOpenAmmoBox
//
//                ammoboxPreview

    }

    /**
     * 关闭弹箱
     */
    private void destoryAmmoBoxDialog() {
        //关闭dialog
        if (ammoBoxPreviewDialog != null && ammoBoxPreviewDialog.isShowing()) {
            ammoBoxPreviewDialog.dismiss();
            ammoBoxPreviewDialog = null;
        }
        //销毁弹箱播放器
        if (ammoboxPreviewPlayer != null) {
            ammoboxPreviewPlayer.release();
            ammoboxPreviewPlayer = null;
        }
        //销毁面部播放器
        if (ammoboxFaceVideoPlayer != null) {
            ammoboxFaceVideoPlayer.release();
            ammoboxFaceVideoPlayer = null;
        }
        ammoboxPreviewThreadStop();
    }

    /**
     * 离线申请开箱弹窗
     */
    Dialog offlineRequestOpenAmmoBoxDialog;

    /**
     * 离线开锁码输入
     */
    TextView offlineRequestCodeLayout;

    /**
     * 离线开锁的请求码
     */
    TextView offlineNativeReuestCodeLayout;

    /**
     * 离线开锁结果提示
     */
    TextView offlineRequestCodeResultLayout;

    /**
     * 倒计时
     */
    TextView offlineTimeLayout;

    /**
     * 输入的申请码
     */
    String offlneEditRequestCode;

    /**
     * 正确的开锁码
     */
    String offlneSureRequestCode;

    /**
     * 输入离线申请码字符
     */
    String offloneEditCodePromptStr = "";

    /**
     * 倒计时
     */
    int offlineCountDownNum = 0;

    /**
     * 总的倒计时
     */
    int offlineTotalCountDownNum = 60;

    /**
     * 离线开锁输入按键
     */
    Button offlineBtn0, offlineBtn1, offlineBtn2, offlineBtn3, offlineBtn4, offlineBtn5, offlineBtn6, offlineBtn7, offlineBtn8, offlineBtn9, offlineBtnBack, offlineBtnSure;

    /**
     * 离线申请开锁
     */
    private void offlineRequestOpenAmmoBox() {
        //判断当前弹箱是否开启状态
        if (AppConfig.AMMO_STATUS == 0) {
            if (AppConfig.KEYBOARD_WITH_SCREEN) {
                cleanSerialScreenPort();
                sendSerialScreenPort("操作异常");
                smallScreenReset(1000);
            }
            App.startSpeaking("操作异常");
            return;
        }
        //关闭离线开锁弹窗
        destoryOfflineOpenAmmoBoxDialog();
        //弹窗
        final AlertDialog.Builder builder = new AlertDialog.Builder(App.getApplication());
        builder.setCancelable(false);
        //view
        View view = View.inflate(App.getApplication(), R.layout.activity_offline_request_open_ammobox_item_layout, null);
        builder.setView(view);
        //输入开锁码
        offlineRequestCodeLayout = view.findViewById(R.id.edit_request_open_ammobox_code_layout);
        //本地生成的开锁码请求
        offlineNativeReuestCodeLayout = view.findViewById(R.id.native_request_open_ammobox_code_layout);
        //请求结果提示
        offlineRequestCodeResultLayout = view.findViewById(R.id.request_open_ammobox_result_layout);
        //倒计时
        offlineTimeLayout = view.findViewById(R.id.count_down_time_layout);
        offlineBtn0 = view.findViewById(R.id.request_open_ammobox_btn0_layout);
        offlineBtn1 = view.findViewById(R.id.request_open_ammobox_btn1_layout);
        offlineBtn2 = view.findViewById(R.id.request_open_ammobox_btn2_layout);
        offlineBtn3 = view.findViewById(R.id.request_open_ammobox_btn3_layout);
        offlineBtn4 = view.findViewById(R.id.request_open_ammobox_btn4_layout);
        offlineBtn5 = view.findViewById(R.id.request_open_ammobox_btn5_layout);
        offlineBtn6 = view.findViewById(R.id.request_open_ammobox_btn6_layout);
        offlineBtn7 = view.findViewById(R.id.request_open_ammobox_btn7_layout);
        offlineBtn8 = view.findViewById(R.id.request_open_ammobox_btn8_layout);
        offlineBtn9 = view.findViewById(R.id.request_open_ammobox_btn9_layout);
        offlineBtnBack = view.findViewById(R.id.request_open_ammobox_back_layout);
        offlineBtnSure = view.findViewById(R.id.request_open_ammobox_sure_layout);

        //本地生成的计算码
        String nativeRequestCode = "";
        //哨位Id
        String sentryId = "";
        SipBean nativeDeviceSipBean = findNativeSipBean();
        if (nativeDeviceSipBean != null) {
            sentryId = nativeDeviceSipBean.getSentryId();
        }
        //6位随机数
        String random6 = ((int) ((Math.random() * 9 + 1) * 100000) + "");
        //判断哨位Id是否为空
        if (TextUtils.isEmpty(sentryId)) {
            nativeRequestCode = "00000" + random6;
        } else {
            if (sentryId.length() == 2) {
                nativeRequestCode = ("000" + sentryId) + random6;
            } else if (sentryId.length() == 3) {
                nativeRequestCode = ("00" + sentryId) + random6;
            } else if (sentryId.length() == 4) {
                nativeRequestCode = ("0" + sentryId) + random6;
            } else {
                nativeRequestCode = "00000" + random6;
            }
        }
        //显示申请码
        offlineNativeReuestCodeLayout.setText(nativeRequestCode);
        //计算正确的开锁码
        offlneSureRequestCode = calculateSureCode(sentryId, random6);
        Logutil.d("offlneSureRequestCode:" + offlneSureRequestCode);
        //显示dialog
        offlineRequestOpenAmmoBoxDialog = builder.create();
        offlineRequestOpenAmmoBoxDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        offlineRequestOpenAmmoBoxDialog.show();
        //通过当前的dialog获取window对象
        Window window = offlineRequestOpenAmmoBoxDialog.getWindow();
        //设置背景，防止变形
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = ScreenUtils.getInstance(App.getApplication()).getWidth() - 210;
        lp.height = ScreenUtils.getInstance(App.getApplication()).getHeight() - 220;
        window.setGravity(Gravity.CENTER);
        window.setDimAmount(0.7f);//使用时设置窗口后面的暗淡量
        window.setAttributes(lp);
        offlineBtn0.setOnClickListener(new OffLineBtnClick());
        offlineBtn1.setOnClickListener(new OffLineBtnClick());
        offlineBtn2.setOnClickListener(new OffLineBtnClick());
        offlineBtn3.setOnClickListener(new OffLineBtnClick());
        offlineBtn4.setOnClickListener(new OffLineBtnClick());
        offlineBtn5.setOnClickListener(new OffLineBtnClick());
        offlineBtn6.setOnClickListener(new OffLineBtnClick());
        offlineBtn7.setOnClickListener(new OffLineBtnClick());
        offlineBtn8.setOnClickListener(new OffLineBtnClick());
        offlineBtn9.setOnClickListener(new OffLineBtnClick());
        offlineBtnBack.setOnClickListener(new OffLineBtnClick());
        offlineBtnSure.setOnClickListener(new OffLineBtnClick());
        //离线倒计时
        offlineTimingThreadStart();
    }

    /**
     * 关闭离线开锁弹窗
     */
    private void destoryOfflineOpenAmmoBoxDialog() {
        //判断窗口是否正在显示
        if (offlineRequestOpenAmmoBoxDialog != null && offlineRequestOpenAmmoBoxDialog.isShowing()) {
            offlineRequestOpenAmmoBoxDialog.dismiss();
            offlineRequestOpenAmmoBoxDialog = null;
        }
        //关闭倒计时
        offlineTimingThreadStop();
    }

    /**
     * 离线倒计时开始
     */
    public void offlineTimingThreadStart() {
        isTimingThreadWork = true;
        if (timingThread != null && timingThread.isAlive()) {
        } else {
            timingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isTimingThreadWork) {

                        try {
                            Thread.sleep(1 * 1000);
                            handler.sendEmptyMessage(23);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            timingThread.start();
        }
    }

    /**
     * 离线倒计时停止
     */
    public void offlineTimingThreadStop() {
        if (isTimingThreadWork) {
            if (timingThread != null && timingThread.isAlive()) {
                timingThread.interrupt();
                timingThread = null;
            }
            offlineCountDownNum = 0;
            isTimingThreadWork = false;
        }
    }

    /**
     * 离线开锁时键盘的点击事件
     */
    class OffLineBtnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.request_open_ammobox_btn1_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "1";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn2_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "2";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn3_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "3";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn4_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "4";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn5_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "5";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn6_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "6";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn7_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "7";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn8_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "8";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn9_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "9";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_btn0_layout:
                    if (offloneEditCodePromptStr.length() < 6)
                        offloneEditCodePromptStr += "0";
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_back_layout:
                    //退格
                    if (offloneEditCodePromptStr.length() > 0)
                        offloneEditCodePromptStr = offloneEditCodePromptStr.substring(0, offloneEditCodePromptStr.length() - 1);
                    promptOpenAmmoBoxCode(offloneEditCodePromptStr);
                    break;
                case R.id.request_open_ammobox_sure_layout:
                    //确认离线开锁
                    offlineSureOpenAmmoBox();
                    break;
            }
        }
    }

    /**
     * 离线开锁确认开锁
     */
    private void offlineSureOpenAmmoBox() {
        offlneEditRequestCode = offlineRequestCodeLayout.getText().toString().trim();
        //判断输入
        if (TextUtils.isEmpty(offlneEditRequestCode)) {
            offlineRequestCodeResultLayout.setText("请输入正确的开锁码");
            return;
        }
        //判断输入是否正确
        if (offlneEditRequestCode.length() != 6) {
            offlineRequestCodeResultLayout.setText("请输入6位开锁码");
            return;
        }
        if (offlneEditRequestCode.equals(offlneSureRequestCode)) {
            openAmmoBox();
            offloneEditCodePromptStr = "";
            //弹窗消失
            destoryOfflineOpenAmmoBoxDialog();
        } else {
            offlineRequestCodeResultLayout.setText("请输入正确的开锁码");
        }
    }

    /**
     * 提示输入的开锁申请码
     */
    private void promptOpenAmmoBoxCode(String str) {
        Message message = new Message();
        message.what = 22;
        message.obj = str;
        handler.sendMessage(message);
    }

    /**
     * 计算正确的离线开锁码
     */
    private String calculateSureCode(String sentryId, String random6) {
        String nativeRequestCode1 = random6 + sentryId;
        //对申请码进行Md5转换
        byte[] data = StringUtils.getMd5Str(nativeRequestCode1);
        //截取前四个字节
        byte[] uint32Data = new byte[4];
        System.arraycopy(data, 0, uint32Data, 0, 4);
        //反转
        byte[] uint32ResetData = new byte[4];
        for (int i = 0; i < uint32ResetData.length; i++) {
            uint32ResetData[i] = uint32Data[uint32Data.length - 1 - i];
        }
        byte[] longData = new byte[8];
        System.arraycopy(uint32ResetData, 0, longData, 4, 4);
        //byte数组转long
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(longData, 0, longData.length);
        buffer.flip();//need flip
        String code = buffer.getLong() + "";
        buffer.clear();
        //只需要前6位
        return code.substring(0, 6);
    }

    /**
     * 本机的SipBean对象
     */
    SipBean currentSipBean = null;


    /**
     * 人脸比对
     */
    private void faceComprison() {
        Logutil.i("开始比对");
        //发送消息让人脸比对模块去比对
        App.getApplication().sendBroadcast(new Intent("startFaceComparison"));
        //提示正在比对
    }

    /**
     * 监听人脸比对结果的广播
     */
    class FaceComparisonResultBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("result");
            Logutil.d("result:" + result);
            if (!TextUtils.isEmpty(result)) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String code = jsonObject.getString("code");
                    String result1 = jsonObject.getString("result");
                    Logutil.d(code + "\n" + result1);
                    Message message = new Message();
                    message.what = 13;
                    message.obj = code + "==" + result1;
                    handler.sendMessage(message);
                } catch (Exception e) {
                    Logutil.e("人脸比对结果异常:" + e.getMessage());
                }
            }
        }
    }

    /**
     * 呼叫值班室
     */
    private void makeCallDutyRoom() {
        App.startSpeaking("呼叫值班室");
        //延时
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //Sip字典
        List<SipBean> allCacheList = null;
        //值班室号码
        String durySipNumber = "";
        //值班室名称
        String duryName = "";
        //防止异常
        try {
            allCacheList = App.getSipS();
        } catch (Exception e) {
            allCacheList = null;
            Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "error:" + e.getMessage());
        }
        //比对本机的哨位ID
        if (allCacheList != null && !allCacheList.isEmpty()) {
            for (SipBean sipBean : allCacheList) {
                if (sipBean.getSentryId().equals("0")) {
                    durySipNumber = sipBean.getNumber();
                    duryName = sipBean.getName();
                    break;
                }
            }
            //判断号码
            if (TextUtils.isEmpty(durySipNumber)) {
                Logutil.e("duryName is null");
                return;
            }
            //判断本机在线
            if (!AppConfig.SIP_STATUS) {
                Logutil.e("Sip is not line:" + AppConfig.SIP_STATUS);
                App.startSpeaking("未注册");
                return;
            }
            //判断当前是否有电话
            if (SipManager.getLc().getCalls().length == 0) {
                Linphone.callTo(durySipNumber, false);
                isCallingFlag = true;
                cleanSerialScreenPort();
                sendSerialScreenPort("正在与" + duryName + "通话中");
            } else {
                SipManager.getLc().terminateAllCalls();
                smallScreenReset(1500);
            }
        } else {
            //空
            App.startSpeaking("操作异常");
            smallScreenReset(1500);
        }
    }

    /**
     * 发送报警
     */
    private void sendAlarmToServer(final String type) {

        //判断报警时是否要鸣枪警告
        boolean isShotgun = (boolean) SharedPreferencesUtils.getObject(App.getApplication(), "alarmWithShotgun", false);
        if (isShotgun) {
            keyBoardGunshootWarring();
        }
        //判断报警时是否要申请供弹
        boolean isOpenAmmoBox = (boolean) SharedPreferencesUtils.getObject(App.getApplication(), "alarmWithBox", false);
        if (isOpenAmmoBox) {
            OpenBoxParamater openBoxParamater = new OpenBoxParamater();
            openBoxParamater.setFalg("ReqB");
            openBoxParamater.setBoxId(SysinfoUtils.getSysinfo().getDeviceGuid());
            new Thread(new HandlerAmmoBoxThread(openBoxParamater, 0)).start();
            App.startSpeaking("申请已发出");
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SendAlarmToServerThread sendAlarmToServerThread = new SendAlarmToServerThread(type, new SendAlarmToServerThread.SendAlarmCallback() {
            @Override
            public void getCallbackData(String result) {
                Logutil.e("result-->" + result);
                cleanSerialScreenPort();
                if (!TextUtils.isEmpty(result) && result.contains("100")) {
                    App.startSpeaking("报警发送成功");
                    if (AppConfig.KEYBOARD_WITH_SCREEN) {
                        sendSerialScreenPort("报警发送成功");
                    }
                } else {
                    App.startSpeaking("报警发送失败");
                    if (AppConfig.KEYBOARD_WITH_SCREEN) {
                        sendSerialScreenPort("报警发送失败");
                    }
                }
                //2秒后小屏显示恢复
                smallScreenReset(2000);

            }
        });
        new Thread(sendAlarmToServerThread).start();
    }

    /**
     * 查找本机SipBean对象
     */
    private SipBean findNativeSipBean() {
        SysInfoBean sysInfoBean = SysinfoUtils.getSysinfo();
        if (sysInfoBean == null)
            return null;
        String nativeGuid = sysInfoBean.getDeviceGuid();
        if (TextUtils.isEmpty(nativeGuid))
            return null;
        List<SipBean> allSips = App.getSipS();
        if (allSips == null || allSips.isEmpty())
            return null;
        for (SipBean sipBean : allSips) {
            if (sipBean != null) {
                String id = sipBean.getId();
                if (!TextUtils.isEmpty(id)) {
                    if (nativeGuid.equals(id)) {
                        return sipBean;
                    }
                }
            }
        }
        if (currentSipBean == null)
            return null;
        return null;
    }

    /**
     * 取消按键
     */
    private void keyBoardFunctionCancel() {
        //销毁摄像机预览
        if (mCamer != null) {
            mCamer.setPreviewCallback(null);
            mCamer.stopPreview();
            mCamer.lock();
            mCamer.release();
            mCamer = null;
        }
        //离线开锁弹窗消失
        destoryOfflineOpenAmmoBoxDialog();
        //销毁上级查勤弹窗
        destorySuperiorCheckPlanDutyDialog();
        //销毁勤务弹窗
        destoryPlanDutyDialog();
        //先判断当前弹箱是否正在预览
        destoryAmmoBoxDialog();
        //判断当前是否正在通话
        if (isCallingFlag) {
            //判断是否正在通话
            App.startSpeaking("呼叫已挂断");
            eventRecord("本机中断通话");
            SipManager.getLc().terminateAllCalls();
            cleanSerialScreenPort();
            sendSerialScreenPort("对讲已挂断");
        } else {
            //未操作
            App.startSpeaking("取消");
            //挂断所有电话
            if (SipManager.getLc().getCalls().length > 0) {
                SipManager.getLc().terminateAllCalls();
                eventRecord("本机中断当前通话");
            }
        }
        //无操作时，再去执行关灯动作
        if (!AppConfig.KEYBOARD_WITH_SCREEN) {
            if (isOpenKeyBoardSerialPortFlag && keyBoardSerialPortManager != null)
                keyBoardSerialPortManager.sendBytes(AppConfig.CLOSE_ALL_ALARM_LIGHT);
        }
        smallScreenReset(1000);
        //清空指令集合
        clearKeyList();

        isCallingFlag = false;
    }

    /**
     * 确认按键
     */
    private void keyBoardConfirm() {

        //判断当前是否正在通话中（挂断），防止重复操作
        if (isCallingFlag) {
            SipManager.getLc().terminateAllCalls();
            isCallingFlag = false;
        }
        //延时一秒后执行下一步
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Logutil.w("Key:--->>>" + returnCurrentKey());
        //判断是否指定对象
        if (!TextUtils.isEmpty(returnCurrentKey())) {
            if (returnCurrentKey().equals("123456")) {
                if (AppConfig.AMMO_STATUS == 0) {
                    if (AppConfig.KEYBOARD_WITH_SCREEN) {
                        cleanSerialScreenPort();
                        sendSerialScreenPort("无效操作");
                        smallScreenReset(1000);
                    }
                    clearKeyList();
                    return;
                }
                openAmmoBox();
                if (AppConfig.KEYBOARD_WITH_SCREEN) {
                    cleanSerialScreenPort();
                    sendSerialScreenPort("打开子弹箱");
                    smallScreenReset(2000);
                }
                clearKeyList();
                return;
            }
            SipBean mSipBean = returnCurrentSipBean(returnCurrentKey());
            if (mSipBean == null) {
                clearKeyList();
                App.startSpeaking("未找到操作对象");
            } else {
                Logutil.d("AA-->>" + mSipBean.toString());
                //判断网络是否正常
                if (!NetworkUtils.isConnected()) {
                    return;
                }
                //把数据传递到勤务通信页面显示拨打电话的UI
                EventBus.getDefault().post(mSipBean);
                if (AppConfig.KEYBOARD_WITH_SCREEN) {
                    //清屏
                    cleanSerialScreenPort();
                    //小屏显示
                    sendSerialScreenPort("呼叫" + mSipBean.getName());
                    smallScreenReset(3000);
                }
                //事件记录
                eventRecord("呼叫" + mSipBean.getName());

                clearKeyList();
            }
        } else {
            App.startSpeaking("确认");
            clearKeyList();
        }

        //清空键值集合
        clearKeyList();
    }

    /**
     * 鸣枪警告
     */
    private void keyBoardGunshootWarring() {
        Logutil.w("Key:--->>>" + returnCurrentKey());

        //本身操作
        App.startSpeaking("鸣枪警告");
        //延时
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //本身鸣枪
        RemoteVoiceOperatService.playVoice(R.raw.gunshoot);
        //清空键值集合
        clearKeyList();

    }

    /**
     * 语音警告
     */
    private void keyBoardVoiceWarring() {
        Logutil.w("Key:--->>>" + returnCurrentKey());

        //本身操作
        App.startSpeaking("语音警告");
        //延时
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //本身鸣枪
        RemoteVoiceOperatService.playVoice(R.raw.warning);
        //清空键值集合
        clearKeyList();
    }

    /**
     * 灯光开启
     */
    private void keyBoardOpenLight() {
        App.startSpeaking("灯光开启");
        cleanSerialScreenPort();
        sendSerialScreenPort("灯光开启");
        smallScreenReset(1500);
    }

    /**
     * 灯光关闭
     */
    private void keyBoardCloseLight() {
        App.startSpeaking("灯光关闭");
        cleanSerialScreenPort();
        sendSerialScreenPort("灯光关闭");
        smallScreenReset(1500);
    }

    /**
     * 控制云台
     */
    private void keyBoardControlPtz() {
        App.startSpeaking("控制云台");
        cleanSerialScreenPort();
        sendSerialScreenPort("控制云台");
        smallScreenReset(1500);
    }

    /**
     * 获取当前的操作对象（喊话，警告，鸣枪等功能）
     */
    private SipBean returnCurrentSipBean(String str) {
        SipBean mSipBean = null;
        try {
            List<SipBean> allSipList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_SIP).toString()), SipBean.class);
            if (allSipList != null && allSipList.size() > 0) {
                for (SipBean bean : allSipList) {
                    if (bean.getSentryId().equals(str)) {
                        mSipBean = bean;
                        return mSipBean;
                    }
                }
            } else {
                return mSipBean;
            }
        } catch (Exception e) {
            return mSipBean;
        }
        return mSipBean;
    }

    /**
     * 得到当前数字键盘输入的指令集合
     */
    private String returnCurrentKey() {
        String currnetKey = "";
        if (keyBoardNumList != null && keyBoardNumList.size() > 0) {
            for (int a : keyBoardNumList) {
                currnetKey += a;
            }
        }
        return currnetKey;
    }

    /**
     * 清除数字键盘输入的指令集合
     */
    private void clearKeyList() {
        if (keyBoardNumList != null) {
            keyBoardNumList.clear();
        }
    }

    /**
     * 事件记录
     */
    public static void eventRecord(String str) {
        ContentValues contentValues1 = new ContentValues();
        contentValues1.put("time", TimeUtils.getCurrentTime1());
        contentValues1.put("event", str);
        new DbUtils(App.getApplication()).insert(DbHelper.EVENT_TAB_NAME, contentValues1);
    }

    /**
     * 开机自检查（警灯闪烁）
     */
    private void deviceBootCheckLight() {
        //判断串口是否打开
        if (!isOpenKeyBoardSerialPortFlag) {
            Logutil.e("串口打开异常--->>>" + isOpenKeyBoardSerialPortFlag);
            return;
        }
        //六色灯开关指令
        final List<byte[]> openLightMessList = new ArrayList<>();
        openLightMessList.add(AppConfig.FIRST_OPEN_1);
        openLightMessList.add(AppConfig.SECOND_OPEN_1);
        openLightMessList.add(AppConfig.THIRD_OPEN_1);
        openLightMessList.add(AppConfig.FORTH_OPEN_1);
        openLightMessList.add(AppConfig.FIFTH_OPEN_1);
        openLightMessList.add(AppConfig.SIXTH_OPEN_1);
        openLightMessList.add(AppConfig.FIRST_ClOSE);
        openLightMessList.add(AppConfig.SECOND_ClOSE);
        openLightMessList.add(AppConfig.THIRD_ClOSE);
        openLightMessList.add(AppConfig.FORTH_ClOSE);
        openLightMessList.add(AppConfig.FIFTH_ClOSE);
        openLightMessList.add(AppConfig.SIXTH_ClOSE);
        //子线程执行延时操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < openLightMessList.size(); i++) {
                    //开灯
                    if (isOpenKeyBoardSerialPortFlag && keyBoardSerialPortManager != null) {
                        keyBoardSerialPortManager.sendBytes(openLightMessList.get(i));
                    }
                    //延时
                    try {
                        Thread.currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //延时3秒后关闭所有的灯（防止，警灯在收到关灯动作时没关灯Bug）
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //发送
                if (isOpenKeyBoardSerialPortFlag && keyBoardSerialPortManager != null) {
                    boolean isCloseAllLight = keyBoardSerialPortManager.sendBytes(AppConfig.CLOSE_ALL_ALARM_LIGHT);
                    // Logutil.e("关闭所有灯的指令:" + isCloseAllLight);
                }
            }
        }).start();
    }

    /**
     * 子弹箱是否是用串口打开的
     */
    //boolean isOpenNormal = false;

    /**
     * 打开弹箱
     */
    private void openAmmoBox() {
        //   isOpenNormal = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isOpenAmmoBoxSerialSuccess != false && ammoboxSerialPortManager != null) {
                    byte newByte1[] = new byte[8];
                    newByte1[0] = (byte) 0xFE;
                    newByte1[1] = 05;
                    newByte1[2] = 00;
                    newByte1[3] = 00;
                    newByte1[4] = (byte) 0xFF;
                    newByte1[5] = 00;
                    newByte1[6] = (byte) 0x98;
                    newByte1[7] = (byte) 0x35;
                    boolean sendSuccess1 = ammoboxSerialPortManager.sendBytes(newByte1);
                    Logutil.d("open:" + "\t" + sendSuccess1);
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isOpenAmmoBoxSerialSuccess != false && ammoboxSerialPortManager != null) {
                    byte newByte[] = new byte[8];
                    newByte[0] = (byte) 0xFE;
                    newByte[1] = 05;
                    newByte[2] = 00;
                    newByte[3] = 00;
                    newByte[4] = 00;
                    newByte[5] = 00;
                    newByte[6] = (byte) 0xD9;
                    newByte[7] = (byte) 0xC5;
                    boolean sendSuccess = ammoboxSerialPortManager.sendBytes(newByte);
                    Logutil.d("close:" + "\t" + sendSuccess);
                }
                eventRecord("本机子弹箱已打开");
                cleanSerialScreenPort();
                sendSerialScreenPort("子弹箱已打开");
                smallScreenReset(1500);
            }
        }).start();
    }

    /**
     * 接收EventBus消息
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void eventBus(String str) {
        if (!TextUtils.isEmpty(str)) {
            Logutil.i("eventBus:" + str);
            switch (str) {
                case "openAmmoBoxAction":
                    openAmmoBox();
                    handler.sendEmptyMessage(15);
                    break;
                case "RejectOpenAmmoBoxAction":
                    handler.sendEmptyMessage(16);
                    break;
                case "receivingAlarm":
                    //接收到报警后弹箱预览窗口消失
                    receiveAlarmAndDismissDialog();
                    break;
                case "fileCacheSuccess":
                    //数据字典缓存完成(重新初始化小屏，防止数据丢失)
                    if (AppConfig.KEYBOARD_WITH_SCREEN)
                        smallScreenReset(1000);
                    break;
            }
        }
    }

    /**
     * 收到报警后弹箱预览窗口消失
     */
    private void receiveAlarmAndDismissDialog() {
        //关闭相机
        if (mCamer != null) {
            mCamer.setPreviewCallback(null);
            mCamer.stopPreview();
            mCamer.lock();
            mCamer.release();
            mCamer = null;
        }
        //关闭勤务弹窗
        destoryPlanDutyDialog();
        //关闭上级查勤弹窗
        destorySuperiorCheckPlanDutyDialog();
        //销毁离线开锁弹窗
        destoryOfflineOpenAmmoBoxDialog();
        //销毁弹箱功能窗口
        destoryAmmoBoxDialog();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //关闭串口
        if (rockerSerialPortManager != null)
            rockerSerialPortManager.closeSerialPort();
        if (keyBoardSerialPortManager != null)
            keyBoardSerialPortManager.closeSerialPort();
        if (ammoboxSerialPortManager != null)
            ammoboxSerialPortManager.closeSerialPort();
        //弹箱预览时面部视频截图的bitmap释放
        if (ammoboxFaceVideoShotBitmap != null)
            ammoboxFaceVideoShotBitmap.recycle();
        if (planDutyShotPicBitmap != null)
            planDutyShotPicBitmap.recycle();
        if (superiorShotPicBitmap != null)
            superiorShotPicBitmap.recycle();

        //移除handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        //反注册eventBus
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        //销毁人脸比对结果广播
        if (mFaceComparisonResult != null)
            this.unregisterReceiver(mFaceComparisonResult);
        udpServerIsRun = false;
        super.onDestroy();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //上级查勤时倒计时显示
                    int superiorCountDownTime = msg.arg1;
                    if (superiorCountDownShotPicTimeLayout != null)
                        superiorCountDownShotPicTimeLayout.setText(Html.fromHtml("<b><font color=\"#ff0000\">" + superiorCountDownTime + "</font></b>后自动截图"));
                    break;
                case 2:
                    //销毁上级查勤弹窗
                    destorySuperiorCheckPlanDutyDialog();
                    break;
                case 3:
                    //弹箱视频播放器回调
                    int planDutyPlayerPlayCallEvent = msg.arg1;
                    Logutil.d("planDutyPlayerPlayCallEvent:" + planDutyPlayerPlayCallEvent);
                    planDutyPlayerCallback(planDutyPlayerPlayCallEvent);
                    break;
                case 4:
                    //面部视频播放器回调
                    break;
                case 5:
                    //面部视频截图
                    if (ammoboxFaceVideoShotBitmap != null && ammoboxFaceVideoShotPicLayout != null)
                        ammoboxFaceVideoShotPicLayout.setImageBitmap(ammoboxFaceVideoShotBitmap);
                    break;
                case 6:
                    //提示倒计时截图
                    int shotPicTime = msg.arg1;
                    if (ammoboxFaceCountDownTvLayout != null)
                        ammoboxFaceCountDownTvLayout.setText(Html.fromHtml("<b><font color=\"#ff0000\">" + shotPicTime + "</font></b>后截图保存"));
                    break;
                case 7:
                    //弹箱预览视频状态回调
                    int preivewAmmoBoxPlayerPlayCallEvent = msg.arg1;
                    previewAmmoBoxPlayerCallback(preivewAmmoBoxPlayerPlayCallEvent);
                    break;
                case 8:
                    //自动关闭弹箱预览
                    autoCloseAmmoboxPreview();
                    break;
                case 9:
                    if (ammoboxFaceCountDownTvLayout != null) {
                        ammoboxFaceCountDownTvLayout.setText("");
                        ammoboxFaceCountDownTvLayout.setVisibility(View.INVISIBLE);
                    }
                    break;
                case 10:
                    //弹箱预览
                    String action1 = (String) msg.obj;
                    if (!TextUtils.isEmpty(action1))
                        previewAmmoBoxVideo(action1);
                    break;
                case 12:
                    //隐藏视频预览的窗口
                    if (ammoboxFaceVideoCountTimeLayout != null)
                        ammoboxFaceVideoCountTimeLayout.setVisibility(View.GONE);
                    break;
                case 13:
                    //得到人脸比对信息
                    String faceComparisonResult = (String) msg.obj;
                    Logutil.d(faceComparisonResult);
                    break;
                case 14:
                    //提示摄像机异常
                    if (ammoBoxResultLayout != null)
                        ammoBoxResultLayout.setText("未发现摄像机设备!");
                    break;
                case 15:
                    //提法申请供弹被批准
                    if (ammoBoxResultLayout != null)
                        ammoBoxResultLayout.setText("申请被批准");
                    break;
                case 16:
                    //提法申请供弹被拒绝
                    if (ammoBoxResultLayout != null)
                        ammoBoxResultLayout.setText("申请被拒绝");
                    break;
                case 17:
                    //上级查勤
                    superiorCheckPlanDuty();
                    break;
                case 18:
                    //上级查勤时播放器的回调
                    int suporiorCheckDutyPlayerPlayCallEvent = msg.arg1;
                    superiorPlayerCallback(suporiorCheckDutyPlayerPlayCallEvent);
                    break;
                case 19:
                    //上级查勤时截图失败
                    if (superiorShotPicFailParentLayout != null)
                        superiorShotPicFailParentLayout.setVisibility(View.VISIBLE);
                    break;
                case 20:
                    //上级查勤时截图显示
                    if (superiorShotPicLayout != null && superiorCheckDutyDialog != null && superiorShotPicBitmap != null)
                        superiorShotPicLayout.setImageBitmap(superiorShotPicBitmap);
                    //倒计时消失
                    if (superiorCountDownShotPicTimeLayout != null)
                        superiorCountDownShotPicTimeLayout.setText("");
                    break;
                case 21:
                    //离线开锁弹窗
                    offlineRequestOpenAmmoBox();
                    break;
                case 22:
                    //提示输入的开锁码（可能为空）
                    String editCode = (String) msg.obj;
                    if (offlineRequestCodeLayout != null)
                        offlineRequestCodeLayout.setText(editCode + "");
                    break;
                case 23:
                    //倒计时提示
                    offlineCountDownNum++;
                    if (offlineTimeLayout != null)
                        offlineTimeLayout.setText(Html.fromHtml("<b><font color=\"#ff00ff\">" + (offlineTotalCountDownNum - offlineCountDownNum) + "</font></b>后消失"));
                    //倒计时结果时关闭离线申请弹窗
                    if (offlineCountDownNum == offlineTotalCountDownNum)
                        destoryOfflineOpenAmmoBoxDialog();
                    break;
                case 26:
                    //勤务上哨
                    goPlanDutyMethod("goPlanDuty");
                    break;
                case 27:
                    //显示截图view
                    if (planDutyShotPicLayout != null && planDutyShotPicBitmap != null)
                        planDutyShotPicLayout.setImageBitmap(planDutyShotPicBitmap);
                    break;

            }
        }
    };

    /**
     * 勤务排班数据集合（包括所有的哨位）
     */
    List<PlanDutyBean> planDutyDataList;

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void eventPlanDutyData(List<PlanDutyBean> list) {
        Logutil.d(list.size() + "\n" + list.toString());
        if (list != null && !list.isEmpty()) {
            planDutyDataList = list;
        }
    }

    /**
     * 勤务弹窗（上哨）
     */
    Dialog planDutyDialog = null;

    /**
     * 勤务播放器
     */
    NodePlayer planDutyPlayer = null;

    /**
     * 勤务播放器view
     */
    NodePlayerView planDutyPlayerView;

    /**
     * 勤务播放器加载进度条
     */
    ProgressBar planDutyLoadingPr;

    /**
     * 勤务播放器加载提示
     */
    TextView planDutyLoadingTv;

    /**
     * 上哨士兵名称
     */
    String sentryGoPlanDutyName = "";

    /**
     * 展示截图
     */
    ImageView planDutyShotPicLayout;

    /**
     * 截图的bitmap
     */
    Bitmap planDutyShotPicBitmap;

    /**
     * 勤务上哨
     * 逻辑：
     * 先获取所有哨位的执班计划表
     * 再获取本哨位的执班计划表
     * 获取当前点击按键的时间，判断当前时间是否在某个时间段内，如果未，提示上哨失败
     * 如果在，获取下班哨的上哨时间，判断当前时间是否在下班哨开始时间的冗余时长内
     * 如果在，可以上哨，如果未在，上哨失败
     */
    private void goPlanDutyMethod(String action) {
        destoryPlanDutyDialog();
        switch (action) {
            case "goPlanDuty":
                //上哨
                sentryGoPlanDuty();
                break;
        }
        //勤务Dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(App.getApplication());
        builder.setCancelable(false);
        View view = View.inflate(App.getApplication(), R.layout.plan_duty_item_layout, null);
        builder.setView(view);
        //指纹验证父布局
        LinearLayout comparisonFingerprintParentLayout = view.findViewById(R.id.comparison_fingerprint_parent_layout);
        //人脸视频父布局
        LinearLayout comparisonFaceVideoParentLayout = view.findViewById(R.id.comparison_face_parent_layout);
        //人脸截图父布局
        LinearLayout comparisonFaceVideoShotPicParentLayout = view.findViewById(R.id.comparison_face_shotpic_parent_layout);
        //显示应该上哨人员名称
        TextView goSentryNameLayout = view.findViewById(R.id.gosentry_name_layout);
        //播放器加载进度
        planDutyLoadingPr = view.findViewById(R.id.planduty_comparison_video_loading_pr_layout);
        //播放器加载信息
        planDutyLoadingTv = view.findViewById(R.id.planduty_comparison_video_loading_tv_layout);
        //播放器VIew
        planDutyPlayerView = view.findViewById(R.id.planduty_comparison_video_view_layout);
        //截图View
        planDutyShotPicLayout = view.findViewById(R.id.planduty_shotpic_layout);

        goSentryNameLayout.setText(Html.fromHtml("上哨兵士:<b><font color=\"#ff0000\">" + sentryGoPlanDutyName + "</font></b>"));
        //显示dialog
        planDutyDialog = builder.create();
        planDutyDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        planDutyDialog.show();
        //通过当前的dialog获取window对象
        Window window = planDutyDialog.getWindow();
        //设置背景，防止变形
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = ScreenUtils.getInstance(App.getApplication()).getWidth() - 210;
        lp.height = ScreenUtils.getInstance(App.getApplication()).getHeight() - 220;
        window.setDimAmount(0.5f);
        //使用时设置窗口后面的暗淡量
        window.setAttributes(lp);

        //生物验证类型
        String checkType = (String) SharedPreferencesUtils.getObject(App.getApplication(), "checkType", "0");
        Logutil.d("checkType:" + checkType);

        switch (checkType) {
            case "1":
                comparisonFingerprintParentLayout.setVisibility(View.INVISIBLE);
                break;
        }
        startPlanDutyShotPicVideo();
    }

    /**
     * 哨兵上哨
     */
    private void sentryGoPlanDuty() {
        //tts提示
        App.startSpeaking("勤务上哨");
        //模拟1号哨上哨
        String nativeSentryName = "1号哨";
        //当前时间
        long currentTime = System.currentTimeMillis();
        String dateNow = "2019-06-03";
        String timeNow = new SimpleDateFormat("HH:mm").format(currentTime);
        //判断勤务排班数据是否正常
        if (planDutyDataList == null || planDutyDataList.isEmpty()) {
            App.startSpeaking("上哨失败");
            return;
        }
        //获取当前哨位勤务排班计划
        List<PlanDutyBean> currentSentryDataList = new ArrayList<>();
        for (PlanDutyBean mPlanDutyBean : planDutyDataList) {
            if (mPlanDutyBean.getGuardpost().equals(nativeSentryName) && mPlanDutyBean.getSchdate().equals(dateNow)) {
                currentSentryDataList.add(mPlanDutyBean);
            }
        }
        //判断当前哨位是否排班计划
        if (currentSentryDataList == null || currentSentryDataList.isEmpty()) {
            App.startSpeaking("上哨失败");
            return;
        }
        //定义冗余时长30分钟
        int redundancyTime = 30;
        //下班哨计划
        PlanDutyBean previewPlanDutyBean = null;
        try {
            Date nowTime = new SimpleDateFormat("HH:mm").parse(timeNow);
            for (PlanDutyBean planDutyBean : currentSentryDataList) {
                Date startTime = new SimpleDateFormat("HH:mm").parse(planDutyBean.getFrmtime());
                Date endTime = new SimpleDateFormat("HH:mm").parse(planDutyBean.getTotime());
                if (isEffectiveDate(nowTime, startTime, endTime)) {
                    Logutil.d(planDutyBean.toString());
                    int currentPlanDutyPosition = currentSentryDataList.indexOf(planDutyBean);
                    if ((currentPlanDutyPosition + 1) == currentSentryDataList.size()) {
                        previewPlanDutyBean = null;
                    } else {
                        previewPlanDutyBean = currentSentryDataList.get(currentPlanDutyPosition + 1);
                    }
                    break;
                }
            }
        } catch (ParseException e) {
            Logutil.e("上哨数据处理异常:" + e.getMessage());
            App.startSpeaking("上哨失败");
        }
        //无下班哨时提示上哨失败
        if (previewPlanDutyBean == null) {
            App.startSpeaking("上哨失败");
            return;
        }
        //下班哨计划上哨的人员姓名
        sentryGoPlanDutyName = previewPlanDutyBean.getMlist().get(0).getPersionname();
        //下班哨计划上哨的开始时间
        String previewPlanDutyTime = previewPlanDutyBean.getFrmtime();
        Logutil.i("previewPlanDutyTime:" + previewPlanDutyTime + "\n" + sentryGoPlanDutyName + "\n" + previewPlanDutyBean.toString());
        //当前时间
        long currentLongTiem = string2Millis(timeNow, "HH:mm");
        //上哨冗余时间前
        long startLongTime = string2Millis(previewPlanDutyTime, "HH:mm") - (redundancyTime * 60 * 1000);
        //上哨冗余时间后
        long endLongTime = string2Millis(previewPlanDutyTime, "HH:mm") + (redundancyTime * 60 * 1000);
        //判断是否是在上哨时间内
        if (startLongTime < currentLongTiem && currentLongTiem < endLongTime) {
            App.startSpeaking("上哨成功");
        } else {
            App.startSpeaking("非上哨时间");
            return;
        }
    }

    /**
     * 运行播放器（截图留证）
     */
    private void startPlanDutyShotPicVideo() {
        String rtsp = "";
        currentSipBean = findNativeSipBean();
        if (currentSipBean == null) {
            rtsp = "";
        }
        if (currentSipBean.getVideoBean() == null) {
            rtsp = "";
        }
        rtsp = currentSipBean.getVideoBean().getRtsp();

        Logutil.d("rtsp:" + rtsp);
        planDutyPlayer = new NodePlayer(App.getApplication());
        planDutyPlayer.setPlayerView(planDutyPlayerView);
        planDutyPlayer.setAudioEnable(false);
        planDutyPlayer.setVideoEnable(true);
        planDutyPlayer.setNodePlayerDelegate(new NodePlayerDelegate() {
            @Override
            public void onEventCallback(NodePlayer player, int event, String msg) {
                Message message = new Message();
                message.what = 3;
                message.arg1 = event;
                handler.sendMessage(message);
            }
        });
        planDutyPlayer.setInputUrl(rtsp);
        planDutyPlayer.start();
        //截图
        shotPicUrlMethod();
    }

    /**
     * 截图Url
     */
    String planDutyShotPicUrl;

    /**
     * 截图
     */
    private void shotPicUrlMethod() {
        //判断当前的Sip对象
        if (currentSipBean == null)
            return;
        //判断sip对象量的面部对象
        if (currentSipBean.getVideoBean() == null)
            return;
        //获取截图url
        planDutyShotPicUrl = currentSipBean.getVideoBean().getShotPicUrl();
        //判断截图url是否空
        if (TextUtils.isEmpty(planDutyShotPicUrl))
            return;
        //子线程去截图
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(planDutyShotPicUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setReadTimeout(3000);
                    connection.setConnectTimeout(3000);
                    connection.connect();
                    if (connection.getResponseCode() == 200) {
                        InputStream inputStream = connection.getInputStream();
                        planDutyShotPicBitmap = BitmapFactory.decodeStream(inputStream);
                        if (planDutyShotPicBitmap != null) {
                            handler.sendEmptyMessage(27);
                            //保存截图
                            saveShotPicMethod();
                        } else {
                            planDutyShotPicBitmap = null;
                        }
                        connection.disconnect();
                    } else {
                        planDutyShotPicBitmap = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    planDutyShotPicBitmap = null;
                }
            }
        }).start();
    }

    /**
     * 保存截图
     */
    private void saveShotPicMethod() {
        //判断bitmap是否空
        if (planDutyShotPicBitmap == null)
            return;
        //子线程去保存图片
        new Thread(new Runnable() {
            @Override
            public void run() {
                File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/tehike/shotPics");
                if (!path.exists())
                    path.mkdirs();
                File pictureFile = new File(path + "/" + "ShotPic" + ".png");
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    if (planDutyShotPicBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)) {
                        fos.flush();
                        fos.close();
                    }
                } catch (Exception e) {
                    Logutil.e("保存图片失败" + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 消除弹窗
     */
    private void destoryPlanDutyDialog() {
        if (planDutyPlayer != null) {
            planDutyPlayer.stop();
            planDutyPlayer.release();
            planDutyPlayer = null;
        }
        if (planDutyDialog != null && planDutyDialog.isShowing()) {
            planDutyDialog.dismiss();
            planDutyDialog = null;
        }
    }

    /**
     * 头像认证时播放器的回调
     */
    private void planDutyPlayerCallback(int event) {
        //状态回调判断
        if (event == 1102) {
            if (planDutyPlayerView != null)
                planDutyPlayerView.setVisibility(View.VISIBLE);
            if (planDutyLoadingPr != null)
                planDutyLoadingPr.setVisibility(View.GONE);
            if (planDutyLoadingTv != null)
                planDutyLoadingTv.setVisibility(View.GONE);
        } else {
            if (planDutyLoadingPr != null)
                planDutyLoadingPr.setVisibility(View.VISIBLE);
            if (planDutyLoadingTv != null) {
                planDutyLoadingTv.setVisibility(View.VISIBLE);
                planDutyLoadingTv.setTextSize(12);
                planDutyLoadingTv.setTextColor(UIUtils.getColor(R.color.red));
            }
            if (event == 1000) {
                if (planDutyLoadingTv != null) {
                    planDutyLoadingTv.setText("正在连接...");
                } else if (event == 1001) {
                    if (planDutyLoadingTv != null)
                        planDutyLoadingTv.setText("连接成功...");
                } else if (event == 1104) {
                    if (planDutyLoadingTv != null)
                        planDutyLoadingTv.setText("切换视频...");
                } else {
                    if (planDutyLoadingTv != null)
                        planDutyLoadingTv.setText("重新连接..");
                }
            }
        }
    }

    /**
     * 时间转long
     * 16:00转-57600000
     */
    public static Long string2Millis(String dateStr, String formatStr) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatStr);
            return simpleDateFormat.parse(dateStr).getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 判断时间是否在某两个时间之间
     */
    public static boolean isEffectiveDate(Date nowTime, Date startTime, Date endTime) {
        if (nowTime.getTime() == startTime.getTime()
                || nowTime.getTime() == endTime.getTime()) {
            return true;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        if (date.after(begin) && date.before(end)) {
            return true;
        } else {
            return false;
        }
    }


/////////////////////////////////////上级查勤 ////////////////////////////////////////////////////////////////////

    /**
     * 上级查勤时视频截图预览Layout
     */
    ImageView superiorShotPicLayout;

    /**
     * 上级查勤弹窗
     */
    Dialog superiorCheckDutyDialog = null;

    /**
     * 上级查勤播放器
     */
    NodePlayer superiorCheckDutyPlayer = null;

    /**
     * 上级查勤时视频加载提示ProgressBar
     */
    ProgressBar superiorCheckDutyLoadingPrLayout;

    /**
     * 上级查勤时视频加载提示TextView
     */
    TextView superiorCheckDutyLoadingtvLayout;

    /**
     * 上级查勤时播放器view
     */
    NodePlayerView superiorCheckDutyPlayerViewLayout;

    /**
     * 显示合计时截图
     */
    TextView superiorCountDownShotPicTimeLayout;

    /**
     * 上级给予差评
     */
    RadioButton superiorEvaluationCBoxLayout;

    /**
     * 上级给予差评
     */
    RadioButton superiorEvaluationLBoxLayout;

    /**
     * 上级给予差评
     */
    RadioButton superiorEvaluationYBoxLayout;

    /**
     * 上级查勤时截图失败的父而已view
     */
    RelativeLayout superiorShotPicFailParentLayout;

    /**
     * 上级评价内容
     */
    EditText superiorEvaluationContentLayout;

    /**
     * 评价等级
     */
    String superiorLevel = "";

    /**
     * 上级查勤时截图的Bitmap
     */
    Bitmap superiorShotPicBitmap = null;

    /**
     * 上级查勤
     */
    private void superiorCheckPlanDuty() {

        /**
         *  上级查勤时只需要拍照验证再评价
         */

        //销毁弹窗
        destorySuperiorCheckPlanDutyDialog();
        //Dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(App.getApplication());
        builder.setCancelable(false);
        View view = View.inflate(App.getApplication(), R.layout.activity_superior_checkduty_item_layout, null);
        builder.setView(view);
        //显示截图控件
        superiorShotPicLayout = view.findViewById(R.id.superior_check_duty_shotpic_layout);
        //显示内容父控件
        //显示视频加载提示ProgressBar
        superiorCheckDutyLoadingPrLayout = view.findViewById(R.id.superior_check_duty_pr_layout);
        //显示视频加载提示TextView
        superiorCheckDutyLoadingtvLayout = view.findViewById(R.id.superior_check_duty_tv_layout);
        //播放器视图
        superiorCheckDutyPlayerViewLayout = view.findViewById(R.id.suporior_check_duty_video_layout);
        //评价差
        superiorEvaluationCBoxLayout = view.findViewById(R.id.superior_check_evaluation_check_c_box);
        //评价良
        superiorEvaluationLBoxLayout = view.findViewById(R.id.superior_check_evaluation_check_l_box);
        //评价优
        superiorEvaluationYBoxLayout = view.findViewById(R.id.superior_check_evaluation_check_y_box);
        //评价备注
        superiorEvaluationContentLayout = view.findViewById(R.id.superior_check_evaluation_content_layout);
        superiorEvaluationCBoxLayout.setOnCheckedChangeListener(new SuperiorEvaluationRadioBtnOnCheckedChangeListener());
        superiorEvaluationLBoxLayout.setOnCheckedChangeListener(new SuperiorEvaluationRadioBtnOnCheckedChangeListener());
        superiorEvaluationYBoxLayout.setOnCheckedChangeListener(new SuperiorEvaluationRadioBtnOnCheckedChangeListener());
        //截图失败的父页面
        superiorShotPicFailParentLayout = view.findViewById(R.id.shotpic_fail_parent_layout);
        //显示倒计时截图
        superiorCountDownShotPicTimeLayout = view.findViewById(R.id.superior_countdown_time_layout);
        //要播放的Rtsp
        String rtsp = "";
        String shotUrl = "";
        //查找本机的SipBean对象
        currentSipBean = findNativeSipBean();
        //判断是否为空并查找当前的Rtsp
        if (currentSipBean != null && currentSipBean.getVideoBean() != null) {
            rtsp = currentSipBean.getVideoBean().getRtsp();
            shotUrl = currentSipBean.getVideoBean().getShotPicUrl();
        } else {
            rtsp = "";
            shotUrl = "";
        }
        //实例上级查勤播放器对象
        superiorCheckDutyPlayer = new NodePlayer(App.getApplication());
        //设置播放器View
        superiorCheckDutyPlayer.setPlayerView(superiorCheckDutyPlayerViewLayout);
        //禁止播放器声音
        superiorCheckDutyPlayer.setAudioEnable(false);
        //允许播放器视频显示
        superiorCheckDutyPlayer.setVideoEnable(true);
        //设置播放器回调
        superiorCheckDutyPlayer.setNodePlayerDelegate(new NodePlayerDelegate() {
            @Override
            public void onEventCallback(NodePlayer player, int event, String msg) {
                Message message = new Message();
                message.what = 18;
                message.arg1 = event;
                handler.sendMessage(message);
            }
        });
        superiorCheckDutyPlayer.setInputUrl(rtsp);
        //开始播放
        superiorCheckDutyPlayer.start();
        //显示dialog
        superiorCheckDutyDialog = builder.create();
        superiorCheckDutyDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        superiorCheckDutyDialog.show();
        //通过当前的dialog获取window对象
        Window window = superiorCheckDutyDialog.getWindow();
        //设置背景，防止变形
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = window.getAttributes();
        //设置弹窗尺寸
        lp.width = ScreenUtils.getInstance(App.getApplication()).getWidth() - 210;
        lp.height = ScreenUtils.getInstance(App.getApplication()).getHeight() - 220;
        window.setDimAmount(0.5f);
        //使用时设置窗口后面的暗淡量
        window.setAttributes(lp);
        //倒计时截图
        superiorCountDownShotPic(shotUrl);
        //确认
        view.findViewById(R.id.suporior_check_btn_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //确认查勤
                superiorMakeSurePlanDuty();
            }
        });
    }

    /**
     * 确认上级查勤
     */
    private void superiorMakeSurePlanDuty() {
        handler.sendEmptyMessage(2);

        /**
         * 把截图图片，评价内容，评价等级发送到后台
         */

        commitSuperiorCheckPlanDutyInfo();

        App.startSpeaking("上级察勤完成");
    }

    /**
     * 提交上级查勤所有信息
     */
    private void commitSuperiorCheckPlanDutyInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (superiorShotPicBitmap != null) {
                    int bytes = superiorShotPicBitmap.getByteCount();
                    ByteBuffer buf = ByteBuffer.allocate(bytes);
                    superiorShotPicBitmap.copyPixelsToBuffer(buf);
                    byte[] byteArray = buf.array();
                    byte[] bitmapData = Base64.encode(byteArray, 0);
                    String evaluationContent = superiorEvaluationContentLayout.getText().toString().trim();

                }
            }
        }).start();

    }

    /**
     * 上级查勤时倒计时截图
     */
    private void superiorCountDownShotPic(final String shotUrl) {
        //子线程倒计时截图
        new Thread(new Runnable() {
            @Override
            public void run() {
                int time = AppConfig.COUNT_DOWN_TIME;
                for (int i = time; i >= 1; i--) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message message = new Message();
                    message.what = 1;
                    message.arg1 = i;
                    handler.sendMessage(message);
                    if (i == 1) {
                        new Thread(new SuperiorShotPicThread(shotUrl)).start();
                    }
                }
            }
        }).start();
    }

    /**
     * 上级查勤时radioBtn点击事件
     */
    class SuperiorEvaluationRadioBtnOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.superior_check_evaluation_check_c_box:
                    if (isChecked) {
                        superiorLevel = "差";
                        superiorEvaluationLBoxLayout.setChecked(false);
                        superiorEvaluationYBoxLayout.setChecked(false);
                    }
                    break;
                case R.id.superior_check_evaluation_check_l_box:
                    if (isChecked) {
                        superiorLevel = "良";
                        superiorEvaluationCBoxLayout.setChecked(false);
                        superiorEvaluationYBoxLayout.setChecked(false);
                    }
                    break;
                case R.id.superior_check_evaluation_check_y_box:
                    if (isChecked) {
                        superiorLevel = "优";
                        superiorEvaluationLBoxLayout.setChecked(false);
                        superiorEvaluationCBoxLayout.setChecked(false);
                    }
                    break;
            }
        }
    }

    /**
     * 上级查勤时截图子线程
     */
    class SuperiorShotPicThread extends Thread {
        String shotPicUrl;

        public SuperiorShotPicThread(String shotPicUrl) {
            this.shotPicUrl = shotPicUrl;
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(shotPicUrl)) {
                superiorShotPicBitmap = null;
                handler.sendEmptyMessage(19);
                return;
            }
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(shotPicUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setReadTimeout(3000);
                connection.setConnectTimeout(3000);
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = connection.getInputStream();
                    superiorShotPicBitmap = BitmapFactory.decodeStream(inputStream);
                    if (superiorShotPicBitmap != null) {
                        handler.sendEmptyMessage(20);
                    } else {
                        superiorShotPicBitmap = null;
                    }
                    connection.disconnect();
                } else {
                    superiorShotPicBitmap = null;
                    handler.sendEmptyMessage(19);
                }
            } catch (IOException e) {
                e.printStackTrace();
                superiorShotPicBitmap = null;
                handler.sendEmptyMessage(19);
            }
        }
    }

    /**
     * 销毁上级查勤弹窗
     */
    private void destorySuperiorCheckPlanDutyDialog() {
        //销毁上级查勤播放器
        if (superiorCheckDutyPlayer != null) {
            superiorCheckDutyPlayer.stop();
            superiorCheckDutyPlayer.release();
            superiorCheckDutyPlayer = null;
        }
        //销毁上级查勤弹窗
        if (superiorCheckDutyDialog != null && superiorCheckDutyDialog.isShowing()) {
            superiorCheckDutyDialog.dismiss();
            superiorCheckDutyDialog = null;
        }
    }
}
