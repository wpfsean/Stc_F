package com.tehike.client.stc.app.project;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.ZysjSystemManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.tehike.client.stc.app.project.entity.SipBean;
import com.tehike.client.stc.app.project.entity.VideoBean;
import com.tehike.client.stc.app.project.execption.Cockroach;
import com.tehike.client.stc.app.project.execption.CrashLog;
import com.tehike.client.stc.app.project.execption.ExceptionHandler;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.services.TerminalUpdateIpService;
import com.tehike.client.stc.app.project.services.InitSystemSettingService;
import com.tehike.client.stc.app.project.update.InstallUtils;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.ServiceUtil;
import com.tehike.client.stc.app.project.utils.StringUtils;
import com.tehike.client.stc.app.project.utils.ToastUtils;
import com.tehike.client.stc.app.project.utils.UIUtils;
import com.tehike.client.stc.app.project.utils.WriteLogToFile;

import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 描述：全局配置
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2019/1/2 16:25
 */
public class App extends Application {

    /**
     * 提供一个供全局使用的application的Context上下文
     */
    public static App mContext;

    /**
     * 众云（提供接口）
     */
    public static ZysjSystemManager mZysjSystemManager;

    /**
     * 线程池
     */
    public static ExecutorService mExecutorService = null;

    /**
     * 本机的最大的线程
     */
    int threadCount = -1;

    /**
     * Sip字典对象集合
     */
    static List<SipBean> allSipList = null;

    /**
     * Video字典对象集合
     */
    static List<VideoBean> allVideoList = null;

    /**
     * 监听字典资源是否缓存完成的广播
     */
    public CacheDictionaryBroadcast mCacheDictionaryBroadcast;

    /**
     * 讯飞合成对象
     */
    private static SpeechSynthesizer mTts;

    /**
     * 串口管理对象
     */
    public static SerialPortManager serialPortManager;

    @Override
    public void onCreate() {
        super.onCreate();
        //异常后注册广播用来接收sip缓存完成的通知
        registerCacheDictionaryBroadcast();
        boolean deviceIsRoot = InstallUtils.isRoot();
        WriteLogToFile.info("当前设备是否root--->>" + deviceIsRoot);
        //初始化
        init();
        //sip和video字典
        initCacheDictionarySources();
        //用于捕获异常
        installUncaughtExceptionHandler();
        //启动服务
        startServices();
        //初始化语音播放参数
        initializeParamater();
        //声音初始化
        initAppVoice();
    }

    /**
     * 声音初始化（全部最大）
     */
    private void initAppVoice() {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
            am.setStreamVolume(AudioManager.STREAM_SYSTEM, am.getStreamMaxVolume(AudioManager.STREAM_SYSTEM), 0);
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);
            am.setSpeakerphoneOn(true);
            if (am.isMicrophoneMute())
                am.setMicrophoneMute(false);
        }
    }

    /**
     * 初始化语音播放参数
     */
    private void initializeParamater() {
        SpeechUtility.createUtility(this, "appid=5c2db3fb");
        mTts = SpeechSynthesizer.createSynthesizer(this, null);
        if (mTts == null) {
            Log.e("TAG", "实例化");
            return;
        }

        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, "purextts");
        //设置发音人资源路径
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "yifeng");


        mTts.setParameter(SpeechConstant.SPEED, "70");
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, "50");
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, "80");
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    /**
     * 加载语音播放时的配置参数
     */
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/pureXtts_common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/" + "yifeng" + ".jet"));
        return tempBuffer.toString();
    }

    /**
     * 返回语音播放对象
     */
    public static SpeechSynthesizer getmTts() {
        return mTts;
    }

    /**
     * 语音播放
     */
    public static void startSpeaking(String str) {
        String content = "";

        for (int i = 0; i < str.length(); i++) {
            if (StringUtils.isChineseChar(str.charAt(i))) {
                content += str.charAt(i);
            } else {
                content += str.charAt(i) + " ";
            }
        }

        mTts.startSpeaking(content, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                //Log.e("TAG", "开始播放");
            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {
                Log.e("TAG", "");
            }

            @Override
            public void onSpeakPaused() {
                Log.e("TAG", "暂停播放");
            }

            @Override
            public void onSpeakResumed() {
                Log.e("TAG", "继续播放");
            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {
                Log.e("TAG", "");
            }

            @Override
            public void onCompleted(SpeechError speechError) {
                // Log.e("TAG", "播放完成");
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {
                Log.e("TAG", "");
            }
        });
    }

    /**
     * 初始化
     */
    @SuppressLint("WrongConstant")
    private void init() {
        mContext = this;
        //初始化
        mZysjSystemManager = (ZysjSystemManager) getSystemService("zysj");

        serialPortManager = new SerialPortManager();

        //获取可用的处理器数
        threadCount = Runtime.getRuntime().availableProcessors();
        //线程池内运行程序可执行的最大线程数
        if (mExecutorService == null) {
            mExecutorService = Executors.newFixedThreadPool(threadCount);
        }
    }

    /**
     * 启动服务
     */
    private void startServices() {
        //启动被动修改ip
        if (!ServiceUtil.isServiceRunning(TerminalUpdateIpService.class)) {
            ServiceUtil.startService(TerminalUpdateIpService.class);
        }
        //用于修改系统设置
        if (!ServiceUtil.isServiceRunning(InitSystemSettingService.class)) {
            ServiceUtil.startService(InitSystemSettingService.class);
        }
    }

    /**
     * 替换系统默认的异常处理机制，用于捕获异常
     */
    private void installUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler sysExcepHandler = Thread.getDefaultUncaughtExceptionHandler();
        Cockroach.install(new ExceptionHandler() {
            @Override
            protected void onUncaughtExceptionHappened(Thread thread, final Throwable throwable) {
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", throwable);
                //把崩溃异常写入文件
                CrashLog.saveCrashLog(mContext, throwable);
                Logutil.e(throwable.getMessage());
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                throwable.printStackTrace();//打印警告级别log，该throwable可能是最开始的bug导致的，无需关心
                ToastUtils.showShort("Arrest!");
            }

            @Override
            protected void onEnterSafeMode() {

            }

            @Override
            protected void onMayBeBlackScreen(Throwable e) {
                Thread thread = Looper.getMainLooper().getThread();
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", e);
                //黑屏时建议直接杀死app
                sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
            }

        });
    }

    /**
     * 获取application上下文
     */
    public static App getApplication() {
        return mContext;
    }

    /**
     * 众云
     */
    public static ZysjSystemManager getSystemManager() {
        return mZysjSystemManager;
    }

    /**
     * 串口管理类
     */
    public static SerialPortManager getSerialPortManager() {
        return serialPortManager;
    }

    /**
     * 获取本机可用的最大线程池对象
     */
    public static ExecutorService getExecutorService() {
        return mExecutorService;
    }

    /**
     * 返回sip字典集合
     */
    public static List<SipBean> getSipS() {
        return allSipList;
    }

    /**
     * 返回video字典集合
     */
    public static List<VideoBean> getVideoS() {
        return allVideoList;
    }

    /**
     * 加载本地缓存资源
     */
    public void initCacheDictionarySources() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    allSipList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_SIP).toString()), SipBean.class);
                    allVideoList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_VIDEO).toString()), VideoBean.class);
                } catch (Exception e) {
                    allSipList = null;
                    allVideoList = null;
                }
            }
        }).start();
    }

    /**
     * 注册广播监听资源缓存完成
     */
    public void registerCacheDictionaryBroadcast() {
        mCacheDictionaryBroadcast = new CacheDictionaryBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SipDone");
        this.registerReceiver(mCacheDictionaryBroadcast, intentFilter);
    }

    /**
     * 广播监听资源是否缓存完成
     */
    public static class CacheDictionaryBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        allSipList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_SIP).toString()), SipBean.class);
                        allVideoList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_VIDEO).toString()), VideoBean.class);
                        EventBus.getDefault().post("fileCacheSuccess");
                    } catch (Exception e) {
                        allSipList = null;
                        allVideoList = null;
                    }
                }
            }).start();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        switch (level) {
            case TRIM_MEMORY_RUNNING_CRITICAL:
                //内存不足(后台进程不足3个)，并且该进程优先级比较高，需要清理内存
                Logutil.e("  1//内存不足(后台进程不足3个)，并且该进程优先级比较高，需要清理内存");
                WriteLogToFile.info("  //内存不足(后台进程不足3个)，并且该进程优先级比较高，需要清理内存");
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                //内存不足(后台进程不足5个)，并且该进程优先级比较高，需要清理内存
                Logutil.e("  2//内存不足(后台进程不足5个)，并且该进程优先级比较高，需要清理内存");
                System.gc();
                WriteLogToFile.info("  //内存不足(后台进程不足5个)，并且该进程优先级比较高，需要清理内存");
                break;
            case TRIM_MEMORY_RUNNING_MODERATE:
                //内存不足(后台进程超过5个)，并且该进程优先级比较高，需要清理内存
                Logutil.e("    3//内存不足(后台进程超过5个)，并且该进程优先级比较高，需要清理内存");
                WriteLogToFile.info("  //内存不足(后台进程超过5个)，并且该进程优先级比较高，需要清理内存");
                System.gc();
                // android.os.Process.killProcess(android.os.Process.myPid());
                break;
            default:
                break;
        }
        super.onTrimMemory(level);
    }

    @Override
    public void onTerminate() {
        Logutil.e("程序终止");
        super.onTerminate();
    }

    @Override
    public void onLowMemory() {
        Logutil.e("onLowMemory");
        super.onLowMemory();
    }
}
