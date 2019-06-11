package com.tehike.client.stc.app.project.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.db.DbHelper;
import com.tehike.client.stc.app.project.db.DbUtils;
import com.tehike.client.stc.app.project.entity.AlarmTypeBean;
import com.tehike.client.stc.app.project.entity.AlarmVideoSource;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.utils.ActivityUtils;
import com.tehike.client.stc.app.project.utils.ByteUtil;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.RecordAlarmLog;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;
import com.tehike.client.stc.app.project.utils.TimeUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * 描述：接收报警的服务
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/11/26 14:11
 */

public class ReceiverAlarmService extends Service {

    /**
     * 接收报警消息的子线程
     */
    ReceivingAlarmThread mReceivingAlarmThread = null;

    /**
     * TcpSocketServer
     */
    ServerSocket serverSocket = null;

    /**
     * 此服务是否正在运行标识
     */
    boolean serviceIsStop = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        serviceIsStop = true;

        //启动子线程执行socket服务
        if (mReceivingAlarmThread == null)
            mReceivingAlarmThread = new ReceivingAlarmThread();
        new Thread(mReceivingAlarmThread).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //重置标识
        serviceIsStop = false;
        //关闭tcpServer服务
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = null;
        }
        //移除 Handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
    }

    /**
     * 接收友邻哨报警
     */
    class ReceivingAlarmThread extends Thread {
        @Override
        public void run() {
            try {
                //启动tcp服务
                if (serverSocket == null)
                    serverSocket = new ServerSocket(SysinfoUtils.getSysinfo().getNeighborWatchPort(), 3);
                InputStream in = null;
                while (serviceIsStop) {
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        in = socket.getInputStream();

                        byte[] header = new byte[524];
                        int read = in.read(header);
                        //获取报警报文数据头
                        byte[] flageByte = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            flageByte[i] = header[i];
                        }
                        String flag = new String(flageByte, "gb2312");
                        if (flag.equals("ATIF")) {
                            OutputStream os = socket.getOutputStream();
                            byte[] data = new byte[1];
                            data[0] = 100;
                            os.write(data);
                            os.flush();
                        }
                        AlarmVideoSource alarmVideoSource = new AlarmVideoSource();
                        //获取报警发送者
                        byte[] senderByte = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            senderByte[i] = header[i + 4];
                        }
                        int senderP = ByteUtil.getPosiotion(senderByte);
                        String sender = new String(senderByte, 0, senderP, "gb2312");


                        byte[] videoIdByte = new byte[48];
                        for (int i = 0; i < 48; i++) {
                            videoIdByte[i] = header[i + 40];
                        }
                        int videoIdPosition = ByteUtil.getPosiotion(videoIdByte);

                        alarmVideoSource.setSenderIp(sender);
                        alarmVideoSource.setFaceVideoId(new String(videoIdByte, 0, videoIdPosition, "gb2312"));
                        //视频源名称
                        byte[] videoNameByte = new byte[128];
                        for (int i = 0; i < 128; i++) {
                            videoNameByte[i] = header[i + 88];
                        }
                        int videoNameP = ByteUtil.getPosiotion(videoNameByte);
                        String videoName = new String(videoNameByte, 0, videoNameP, "gb2312");
                        alarmVideoSource.setFaceVideoName(videoName);
                        //报警类型
                        byte[] alarmTypeByte = new byte[32];
                        for (int i = 0; i < 32; i++) {
                            alarmTypeByte[i] = header[i + 460];
                        }
                        int alarmTypeP = ByteUtil.getPosiotion(alarmTypeByte);
                        String alarmType = new String(alarmTypeByte, 0, alarmTypeP, "gb2312");
                        alarmVideoSource.setAlarmType(alarmType);

                        Logutil.e("Flag-->>>" + flag);
                        if (flag.equals("CMsg")) {
                            App.getApplication().sendBroadcast(new Intent("closeAlarm"));
                        } else {
                            EventBus.getDefault().post("receivingAlarm");
                            Message message = new Message();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("AlarmVideoSource", alarmVideoSource);
                            message.setData(bundle);
                            message.what = 1;
                            handler.sendMessage(message);
                        }
                    } catch (IOException e) {
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "\n接收友邻哨报警socket异常:" + e.getMessage());
            }
        }
    }

    /**
     * 处理报警信息
     */
    private void handlerAlarm(AlarmVideoSource mAlarmVideoSource) {
        //清除屏保
        if (ActivityUtils.getTopActivity().getClass().getName().equals("com.tehike.client.stc.app.project.ui.ScreenSaverActivity")) {
            ActivityUtils.getTopActivity().finish();
        }
        //打开警灯
        try {
            if (!AppConfig.KEYBOARD_WITH_SCREEN)
                handlerRequestOpenLight(mAlarmVideoSource);
        } catch (InterruptedException e) {
            Logutil.e("接收到报警时开灯异常:"+e.getMessage());
        }
        //报警记录写入数据库，方便记录的历史记录依据
        ContentValues contentValues = new ContentValues();
        contentValues.put("time", TimeUtils.getCurrentTime1());
        contentValues.put("senderIp", mAlarmVideoSource.getSenderIp());
        contentValues.put("faceVideoId", mAlarmVideoSource.getFaceVideoId());
        contentValues.put("faceVideoName", mAlarmVideoSource.getFaceVideoName());
        contentValues.put("alarmType", mAlarmVideoSource.getAlarmType());
        contentValues.put("isHandler", "否");
        new DbUtils(App.getApplication()).insert(DbHelper.TAB_NAME, contentValues);
        //此记录写入file供别人参考(SD卡)
        RecordAlarmLog.wirteLog(mAlarmVideoSource.toString() + "发生报警");
        //广播，通知此报警
        Intent alarmIntent = new Intent();
        alarmIntent.setAction(AppConfig.ALARM_ACTION);
        alarmIntent.putExtra("alarm", mAlarmVideoSource);
        App.getApplication().sendBroadcast(alarmIntent);
    }

    /**
     * 收到报警时亮灯
     */
    private void handlerRequestOpenLight(AlarmVideoSource alarm) throws InterruptedException {

        //开灯消息
        byte[] openLightMess = null;
        //报警类型及警灯颜色对应关系表
        List<AlarmTypeBean> mAlarmTypeList = null;
        //当前报警的警灯颜色
        String alarmColor = "";
        //判断报警对象是否为空
        if (alarm == null) {
            Logutil.e("alarm is null");
            return;
        }
        //判断报警类型
        String alarmType = alarm.getAlarmType().trim();
        if (TextUtils.isEmpty(alarmType)) {
            Logutil.e("alarmType is null");
            return;
        }
        //取出本地保存警灯颜色和类型对应表
        try {
            mAlarmTypeList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.ALARM_COLOR).toString()), AlarmTypeBean.class);
        } catch (Exception e) {
            mAlarmTypeList = null;
            Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "error:" + e.getMessage());
        }
        //判断集合数据是否存在
        if (mAlarmTypeList == null || mAlarmTypeList.isEmpty()) {
            Logutil.e("mAlarmTypeList is null");
            return;
        }
        //遍历查询当前的获取颜色
        for (AlarmTypeBean bean : mAlarmTypeList) {
            if (bean.getTypeName().equals(alarmType)) {
                alarmColor = bean.getTypeColor().trim();
                break;
            }
        }
        //判断当前报警的警灯的颜色是否为空
        if (TextUtils.isEmpty(alarmColor)) {
            Logutil.e("未获取到当前报警警灯颜色");
            //没查找到警灯颜色时默认红色
            alarmColor = "red";
        }
        Logutil.w("alarmColor--->>" + alarmColor);
        //操作指定的继电器闭合
        switch (alarmColor) {
            case "red":
                //应急
                openLightMess = AppConfig.FIRST_OPEN;
                break;
            case "yellow":
                //袭击
                openLightMess = AppConfig.SECOND_OPEN;
                break;
            //灾害
            case "blue":
                openLightMess = AppConfig.THIRD_OPEN;
                break;
            case "green":
                //暴狱
                openLightMess = AppConfig.FORTH_OPEN;
                break;
            case "pink":
                //突发
                openLightMess = AppConfig.FIFTH_OPEN;
                break;
            case "orange":
                //挟持
                openLightMess = AppConfig.SIXTH_OPEN;
                break;
            default:
                //默认是红色
                openLightMess = AppConfig.FIRST_OPEN;
                break;

        }
        App.getSerialPortManager().sendBytes(AppConfig.CLOSE_ALL_ALARM_LIGHT);
        Thread.sleep(500);

        //发送命令开启警灯
        boolean isOpenAlarmLight = App.getSerialPortManager().sendBytes(openLightMess);
        Logutil.i("isOpenAlarmLight:" + isOpenAlarmLight);
    }

    /**
     * Handler处理子线程发送过来 的数据
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case 1:
                    //接收到的报警消息（封装实体）
                    Bundle bundle = msg.getData();
                    AlarmVideoSource mAlarmVideoSource = (AlarmVideoSource) bundle.getSerializable("AlarmVideoSource");
                    //处理报警信息
                    handlerAlarm(mAlarmVideoSource);
                    break;
            }
        }
    };
}