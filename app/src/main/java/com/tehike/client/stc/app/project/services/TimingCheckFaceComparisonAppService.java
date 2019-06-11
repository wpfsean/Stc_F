package com.tehike.client.stc.app.project.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.phone.Linphone;
import com.tehike.client.stc.app.project.phone.SipManager;
import com.tehike.client.stc.app.project.phone.SipService;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.ServiceUtil;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 描述：定时检查人脸比对的服务是否丢失的服务
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2019/5/20 17:09
 */


public class TimingCheckFaceComparisonAppService extends Service {

    //定时任务的线程池
    ScheduledExecutorService mScheduledExecutorService = null;

    String packName = "com.tehike.facecomprison.project";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //延迟10秒后每30秒执行一次
        if (mScheduledExecutorService == null) {
            mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }
      //  mScheduledExecutorService.scheduleWithFixedDelay(new CheckFaceComparisonThread(), 0, 15 * 1000, TimeUnit.MILLISECONDS);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService.shutdown();
            mScheduledExecutorService = null;
        }
    }

    /**
     * 定时子线程
     */
    class CheckFaceComparisonThread extends Thread {
        @Override
        public void run() {
            boolean isInstall = checkAppInstalled(App.getApplication(), packName);

            boolean isLiving = isAppIsInBackground(App.getApplication());
            // Logutil.d(isInstall + "\n" + isLiving);
            if (!isLiving) {
                Intent resolveIntent = App.getApplication().getPackageManager().getLaunchIntentForPackage(packName);
                App.getApplication().startActivity(resolveIntent);
                Logutil.w("启动了");
            }


        }
    }

    /**
     * 应用是否安装
     */
    private boolean checkAppInstalled(Context context, String pkgName) {
        if (pkgName == null || pkgName.isEmpty()) {
            return false;
        }
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        if (packageInfo == null) {
            return false;
        } else {
            return true;//true为安装了，false为未安装
        }
    }

    /**
     * 应用是否在后台
     */
    private boolean isAppIsInBackground(Context context) {


        boolean isRunning = ServiceUtil.isServiceRunning("com.tehike.facecomprison.project.CustomService");


        return isRunning;
    }
}
