package com.tehike.client.stc.app.project.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.utils.HttpBasicRequest;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;
import com.tehike.client.stc.app.project.utils.UIUtils;
import com.tehike.client.stc.app.project.utils.WriteLogToFile;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 描述：用于修改系统时间
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2019/3/4 15:21
 */

public class UpdateSystemTimeService extends Service {

    /**
     * tts是否播报标识
     */
    boolean isTTs = false;

    /**
     * 定时线程池任务
     */
    ScheduledExecutorService mScheduledExecutorService = null;

    @Override
    public void onCreate() {
        super.onCreate();
        //校时服务
        initialize();
        //创建定时线程池任务
        if (mScheduledExecutorService == null) {
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }
        //创建定时任务（延迟执行）
        mScheduledExecutorService.scheduleWithFixedDelay(new TimingCheckDateThread(), 5000L, 30 *60* 1000, TimeUnit.MILLISECONDS);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //销毁定时线程任务
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService.shutdown();
            mScheduledExecutorService = null;
        }
        //重置标识
        isTTs = false;
        //移除handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * 定时校时线程池
     */
    class TimingCheckDateThread extends Thread {
        @Override
        public void run() {
            initialize();
        }
    }

    /**
     * 初始化
     */
    private void initialize() {
        //修改系统时间
        updateSystemTime();
    }

    /**
     * 修改系统时间
     */
    private void updateSystemTime() {
        //服务器地址
        String serverIp = (String) SharedPreferencesUtils.getObject(App.getApplication(), "serverIp", "");
        if (TextUtils.isEmpty(serverIp)) {
            serverIp = SysinfoUtils.getSysinfo().getWebresourceServer();
        }
        //更新时间地址
        String getServerTimeUrl = AppConfig.WEB_HOST + serverIp + AppConfig.SERVER_TIME;
        //  Logutil.d("更新时间---Url"+getServerTimeUrl);
        HttpBasicRequest httpBasicRequest = new HttpBasicRequest(getServerTimeUrl, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                Message message = new Message();
                message.what = 1;
                message.obj = result;
                handler.sendMessage(message);
            }
        });
        new Thread(httpBasicRequest).start();
    }

    /**
     * 获取系统的时间
     */
    private String getDeviceDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String currentDate = dateFormat.format(date);
        return currentDate;
    }

    /**
     * 设置系统时间
     */
    private void setTime(String reuslt) {
        String deviceDate = getDeviceDate();
        if (!TextUtils.isEmpty(reuslt) && deviceDate.equals(reuslt)) {
            //不设置
        } else {
            try {
                //解析json
                JSONObject jsonObject = new JSONObject(reuslt);
                String dateTime = jsonObject.getString("datetime");
                //日期
                String serverDate = dateTime.split(" ")[0];
                //时间
                String serverTime = dateTime.split(" ")[1];
                //更改时间
                int r = App.getSystemManager().ZYsetSysTime(serverDate, serverTime);
                //TTs第一次播报
//                if (r == 0) {
//                    if (!isTTs)
//                        App.startSpeaking(UIUtils.getString(R.string.str_school_time_sucuess));
//                } else {
//                    if (!isTTs)
//                        App.startSpeaking("校时失败");
//                }
                isTTs = true;
            } catch (Exception e) {
                Logutil.e("解析时间数据异常:" + e.getMessage());
            }
        }
    }

    /**
     * handler处理子线程发送的消息
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String reuslt = (String) msg.obj;
                    setTime(reuslt);
                    break;
            }
        }
    };
}
