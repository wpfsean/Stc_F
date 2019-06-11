package com.tehike.client.stc.app.project.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.entity.SysInfoBean;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.phone.Linphone;
import com.tehike.client.stc.app.project.phone.SipManager;
import com.tehike.client.stc.app.project.phone.SipService;
import com.tehike.client.stc.app.project.services.ReceiverAlarmService;
import com.tehike.client.stc.app.project.services.RemoteVoiceOperatService;
import com.tehike.client.stc.app.project.services.RequestWebApiDataService;
import com.tehike.client.stc.app.project.services.SerialPortService;
import com.tehike.client.stc.app.project.services.TerminalUpdateIpService;
import com.tehike.client.stc.app.project.services.TimingAutoUpdateService;
import com.tehike.client.stc.app.project.services.TimingCheckSipStatus;
import com.tehike.client.stc.app.project.services.TimingRefreshNetworkStatus;
import com.tehike.client.stc.app.project.services.TimingRequestAlarmTypeService;
import com.tehike.client.stc.app.project.services.InitSystemSettingService;
import com.tehike.client.stc.app.project.services.TimingSendHbService;
import com.tehike.client.stc.app.project.services.UpdateSystemTimeService;
import com.tehike.client.stc.app.project.ui.fragments.AlarmFragment;
import com.tehike.client.stc.app.project.ui.fragments.IntercomCallFragment;
import com.tehike.client.stc.app.project.ui.fragments.ManagmentServiceFragment;
import com.tehike.client.stc.app.project.ui.fragments.SystemSetFragment;
import com.tehike.client.stc.app.project.ui.fragments.VideoMonitorFragment;
import com.tehike.client.stc.app.project.ui.views.CustomViewPagerSlide;
import com.tehike.client.stc.app.project.utils.ActivityUtils;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.HttpBasicRequest;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.ServiceUtil;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;
import com.tehike.client.stc.app.project.utils.WriteLogToFile;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;

/**
 * 描述：勤务综合管控终端
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/12/4 11:03
 */

public class StcDutyMainActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener, IntercomCallFragment.CallBackValue {

    /**
     * 主界面滑动的ViewPager布局
     */
    @BindView(R.id.main_viewpager_layout)
    public CustomViewPagerSlide CustomViewPagerLayout;

    /**
     * 显示当前的哨位名
     */
    @BindView(R.id.current_user_name_layout)
    public TextView currentNameLayout;

    /**
     * 显示当前连接状态
     */
    @BindView(R.id.current_connected_status_layout)
    public TextView currentConntectLayout;

    /**
     * 显示当前时间
     */
    @BindView(R.id.current_time_layout)
    public TextView currentTimeLayout;

    /**
     * 显示当前日期
     */
    @BindView(R.id.current_date_layout)
    public TextView currentDateLayout;

    /**
     * 底部的radioGroup
     */
    @BindView(R.id.bottom_radio_group_layout)
    public RadioGroup bottomRadioGroupLayout;

    /**
     * 显示中队名称
     */
    @BindView(R.id.stc_unitname_tv_layout)
    TextView stcUnitnameTvLayout;

    /**
     * 页面Fragment集合
     */
    List<Fragment> allFragmentList = new ArrayList<>();

    /**
     * 时间格式
     */
    SimpleDateFormat dateFormat = null;

    /**
     * 显示时间的线程是否正在运行
     */
    boolean threadIsRun = true;

    /**
     * 来电广播
     */
    InComingCallBroadcast incomingBroadcast;

    /**
     * 刷新网络状态广播
     */
    NetworkStatusBroadcast mFreshNetworkStatusBroadcast;

    /**
     * 屏保计时
     */
    int screenSaverCount = 0;

    /**
     * 是否正在通话的标识
     */
    boolean isCallingFlag = true;

    /**
     * 屏保定时器
     */
    Timer timer = null;


    @BindView(R.id.current_server_center_status_layout)
    TextView currentServerCenterTv;


    @Override
    protected int intiLayout() {
        return R.layout.activity_dtcduty_layout;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        //注册来电监听
        registerInComingCallBroadcast();
        //注册网络状态变化广播
        registerNetworkChangedBroadcast();
        //启动服务
        startAllServices();
        //去注册SIp
        new Thread(new Runnable() {
            @Override
            public void run() {
                registerSipToServer(SysinfoUtils.getSysinfo());
            }
        }).start();

        //初始化页面
        initializeViewPagerFragment();
        //初始化显示时间
        initializeTime();
        //初始化数据
        initializeData();
        //获取当前中队名称
        initUnitNameData();
        //开启屏保计时
        startScreenSaverTiming();
    }

    /**
     * 获取当前头部中队信息
     */
    private void initUnitNameData() {
        //请求Sysinfo接口
        String requestUnitUrl = AppConfig.WEB_HOST + SysinfoUtils.getSysinfo().getWebresourceServer() + AppConfig._UNITNAME;
        //httpbaisc请求
        HttpBasicRequest httpBasicRequest = new HttpBasicRequest(requestUnitUrl, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                Message message = new Message();
                message.obj = result;
                message.what = 10;
                handler.sendMessage(message);
            }
        });
        //运行子线程
        new Thread(httpBasicRequest).start();
    }

    /**
     * 显示当前中队名称
     */
    private void disPlayUnitNameInfor(String content) {
        //判断数据是否为空
        if (TextUtils.isEmpty(content)) {
            Logutil.e("content is null");
            return;
        }
        //json解析
        try {
            JSONObject jsonObject = new JSONObject(content);
            String name = jsonObject.getString("unitname");
            //展示
            if (!TextUtils.isEmpty(name)) {
                stcUnitnameTvLayout.setText(name);
            }
        } catch (Exception e) {
            Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "error:" + e.getMessage());
        }
    }

    /**
     * 实现接口（接收Fragment传递的数据）
     */
    @Override
    public void SendMessageValue(String strValue) {
        if (strValue.equals("true")) {
            //如果正在通话（不计时）
            screenSaverCount = 0;
            isCallingFlag = false;
        } else if (strValue.equals("false")) {
            //如果停止通话（开始计时）
            if (AppConfig.IS_ENABLE_SCREEN_SAVE) {
                screenSaverCount = 0;
                isCallingFlag = true;
//                new Thread(new TimingScreenSaverThread()).start();
                timingScreenSaver();

            }
        }
    }

    /**
     * 开启屏保计时功能
     */
    private void startScreenSaverTiming() {
        //开启屏保计时功能
        if (AppConfig.IS_ENABLE_SCREEN_SAVE) {
            timingScreenSaver();
        }
    }

    /**
     * 屏保计时
     */
    private void timingScreenSaver() {
        //子线程去操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                //判断定时器是否在执行（复位）
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                //实例定时器
                timer = new Timer();
                //开启定时任务
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (isCallingFlag) {
                            handler.sendEmptyMessage(9);
                        }
                    }
                }, 0, 1000);
            }
        }).start();
    }

    /**
     * 注册刷新网络状态的广播
     */
    private void registerNetworkChangedBroadcast() {
        mFreshNetworkStatusBroadcast = new NetworkStatusBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.REFRESH_NETWORK_ACTION);
        this.registerReceiver(mFreshNetworkStatusBroadcast, intentFilter);
    }

    /**
     * 广播接收网络状态变化（判断网线是否拨出）
     */
    public class NetworkStatusBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isNormal = intent.getBooleanExtra("isNormal", false);
                    if (isNormal) {
                        handler.sendEmptyMessage(5);
                    } else {
                        handler.sendEmptyMessage(6);
                    }
                }
            }).start();
        }
    }

    /**
     * 启动服务
     */
    private void startAllServices() {
        //启动获取SipResource的服务
        if (!ServiceUtil.isServiceRunning(RequestWebApiDataService.class)) {
            ServiceUtil.startService(RequestWebApiDataService.class);
        }
        //报警报警颜色及对类型对应表
        if (!ServiceUtil.isServiceRunning(TimingRequestAlarmTypeService.class)) {
            ServiceUtil.startService(TimingRequestAlarmTypeService.class);
        }
        //启动接收报警
        if (!ServiceUtil.isServiceRunning(ReceiverAlarmService.class)) {
            ServiceUtil.startService(ReceiverAlarmService.class);
        }
        //定时更新apk的服务
        if (!ServiceUtil.isServiceRunning(TimingAutoUpdateService.class)) {
            ServiceUtil.startService(TimingAutoUpdateService.class);
        }
        //开户设置时间的服务
        if (!ServiceUtil.isServiceRunning(UpdateSystemTimeService.class)) {
            ServiceUtil.startService(UpdateSystemTimeService.class);
        }
        //开启定发送心跳服务
        if (!ServiceUtil.isServiceRunning(TimingSendHbService.class)) {
            ServiceUtil.startService(TimingSendHbService.class);
        }
        //开启键盘串口服务
//        if (!ServiceUtil.isServiceRunning(KeyBoardService.class)) {
//            ServiceUtil.startService(KeyBoardService.class);
//        }
        //启动被动远程操作的服务
        if (!ServiceUtil.isServiceRunning(RemoteVoiceOperatService.class)) {
            ServiceUtil.startService(RemoteVoiceOperatService.class);
        }
        //定时刷新网络
        if (!ServiceUtil.isServiceRunning(TimingRefreshNetworkStatus.class)) {
            ServiceUtil.startService(TimingRefreshNetworkStatus.class);
        }
        if (!ServiceUtil.isServiceRunning(SerialPortService.class))
            ServiceUtil.startService(SerialPortService.class);
    }

    /**
     * 注册来电监听
     */
    private void registerInComingCallBroadcast() {
        incomingBroadcast = new InComingCallBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.INCOMING_CALL_ACTION);
        registerReceiver(incomingBroadcast, intentFilter);
    }

    /**
     * 监听来电广播（切换到来电页面）
     */
    class InComingCallBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.sendEmptyMessage(8);
        }
    }

    SysInfoBean cc = null;
    /**
     * 初始化数据
     */
    private void initializeData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //取出本地保存的sysinfo数据
                    String infor = CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SYSINFO).toString());
                    //判断是否为空
                    if (TextUtils.isEmpty(infor)) {
                        Logutil.e("AppConfig.SYSINFO无数据！");
                        WriteLogToFile.info("AppConfig.SYSINFO无数据！");
                        return;
                    }
                    //转对对象
                     cc = GsonUtils.GsonToBean(infor, SysInfoBean.class);
                    if (cc != null) {
                        handler.sendEmptyMessage(11);
                    }
                } catch (Exception e) {
                    Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "AppConfig.SYSINFO获取数据异常--->>>" + e.getMessage());
                    WriteLogToFile.info(Thread.currentThread().getStackTrace()[2].getClassName() + "AppConfig.SYSINFO获取数据异常--->>>" + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 初始化显示时间
     */
    private void initializeTime() {
        //计时
        if (dateFormat == null)
            dateFormat = new SimpleDateFormat("yyyy-MM-dd|HH:mm:ss");
        //开启计时线程
        TimingThread timeThread = new TimingThread();
        new Thread(timeThread).start();
    }

    /**
     * 每隔1秒刷新一下时间的线程
     */
    class TimingThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(4);
            } while (threadIsRun);
        }
    }

    /**
     * 显示当前的时间
     */
    private void disPlayCurrentTime() {
        //时间
        Date date = new Date();
        String format = dateFormat.format(date);
        String[] splits = format.split("\\|");
        //显示
        if (isVisible && currentTimeLayout != null && currentDateLayout != null) {
            currentTimeLayout.setText(splits[1]);
            currentDateLayout.setText(splits[0]);
        }
    }

    /**
     * 把Sip注册到sip服务器
     */
    private void registerSipToServer(SysInfoBean sysInfoBean) {
        if (sysInfoBean == null) {
            handler.sendEmptyMessage(1);
            Logutil.e("registerSipToServer sysInfoBean == null");
            WriteLogToFile.info("注册sip时，SysInfoBean为空");
            return;
        }
        //启动sip服务
        if (!SipService.isReady() || !SipManager.isInstanceiated()) {
            Linphone.startService(getApplicationContext());
        }
        //判断获取的sip数据是为空
        if (TextUtils.isEmpty(sysInfoBean.getSipUsername()) || TextUtils.isEmpty(sysInfoBean.getSipPassword()) || TextUtils.isEmpty(sysInfoBean.getSipServer())) {
            Logutil.e("SIp信息为空");
            handler.sendEmptyMessage(1);
            WriteLogToFile.info("注册sip时，SIp信息为空");
            return;
        }
        //当前的sip是否在线
        if (AppConfig.SIP_STATUS) {
            Logutil.i("已经注册了");
            WriteLogToFile.info("注册sip时，已经注册了");
            return;
        }
        //去注册
        Linphone.setAccount(sysInfoBean.getSipUsername(), sysInfoBean.getSipPassword(), sysInfoBean.getSipServer());
        Linphone.login();
        Logutil.d("Sip注册成功");
        WriteLogToFile.info("注册sip时，正常去注册");
    }

    /**
     * 底部RadioGroup监听
     */
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.bottom_intercom_radio_btn_layout:
                //勤务对讲
                CustomViewPagerLayout.setCurrentItem(0);
                break;
            case R.id.bottom_video_monitor_radio_btn_layout:
                //视频监控
                CustomViewPagerLayout.setCurrentItem(1);
                break;
            case R.id.bottom_service_mangement_radio_btn_layout:
                //勤务管理
                CustomViewPagerLayout.setCurrentItem(2);
                break;
            case R.id.bottom_alarm_radio_btn_layout:
                //应急报警
                CustomViewPagerLayout.setCurrentItem(3);
                break;
            case R.id.system_set_btn_layout:
                //系统设置
                CustomViewPagerLayout.setCurrentItem(4);
                break;
        }
    }

    /**
     * 初始化ViewPager页面
     */
    private void initializeViewPagerFragment() {
        //底部radiogroup监听
        bottomRadioGroupLayout.setOnCheckedChangeListener(this);
        //添加要滑动的Fragment
        allFragmentList.add(new IntercomCallFragment());
        allFragmentList.add(new VideoMonitorFragment());
        allFragmentList.add(new ManagmentServiceFragment());
        allFragmentList.add(new AlarmFragment());
        allFragmentList.add(new SystemSetFragment());
        //适配显示
        final ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        CustomViewPagerLayout.setAdapter(adapter);
        //预加载全部
        CustomViewPagerLayout.setOffscreenPageLimit(allFragmentList.size());
        CustomViewPagerLayout.setCurrentItem(0);
        CustomViewPagerLayout.setScanScroll(AppConfig.IS_CAN_SLIDE);
        CustomViewPagerLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Logutil.i("当前页面---->>" + position);

                if (AppConfig.IS_CAN_SLIDE) {
                    bottomRadioGroupLayout.getChildAt(position).setEnabled(true);
                    updateRadioGroupStatus(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    /**
     * 更改底部的radioGroup选中状态
     */
    private void updateRadioGroupStatus(int select) {
        for (int i = 0; i < bottomRadioGroupLayout.getChildCount(); i++) {
            if (select == i) {
                bottomRadioGroupLayout.check(bottomRadioGroupLayout.getChildAt(select).getId());
                continue;
            }
        }
    }

    /**
     * ViewPager适配器
     */
    class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);

        }

        @Override
        public Fragment getItem(int arg0) {
            return allFragmentList.get(arg0);
        }

        @Override
        public int getCount() {
            return allFragmentList == null ? 0 : allFragmentList.size();
        }
    }

    @Override
    protected void onRestart() {
        Logutil.i(Thread.currentThread().getStackTrace()[2].getClassName() + "又可见了");
        if (AppConfig.IS_ENABLE_SCREEN_SAVE) {
            screenSaverCount = 0;
            isCallingFlag = true;
            timingScreenSaver();
        }
        super.onRestart();
    }

    /**
     * 当前页面的事件分发机制(页面点击时屏保重新计时)
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        screenSaverCount = 0;
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 点击返回按键
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        StcDutyMainActivity.this.finish();
        exitApp();
    }

    /**
     * 退出登录(测试)
     */
    public static void exitApp() {
        if (ServiceUtil.isServiceRunning(RemoteVoiceOperatService.class)) {
            ServiceUtil.stopService(RemoteVoiceOperatService.class);
        }
        if (ServiceUtil.isServiceRunning(ReceiverAlarmService.class)) {
            ServiceUtil.stopService(ReceiverAlarmService.class);
        }
        if (ServiceUtil.isServiceRunning(TerminalUpdateIpService.class)) {
            ServiceUtil.stopService(TerminalUpdateIpService.class);
        }
        if (ServiceUtil.isServiceRunning(TimingAutoUpdateService.class)) {
            ServiceUtil.stopService(TimingAutoUpdateService.class);
        }
        if (ServiceUtil.isServiceRunning(RemoteVoiceOperatService.class)) {
            ServiceUtil.stopService(RemoteVoiceOperatService.class);
        }
        if (ServiceUtil.isServiceRunning(TimingRefreshNetworkStatus.class)) {
            ServiceUtil.stopService(TimingRefreshNetworkStatus.class);
        }
        if (ServiceUtil.isServiceRunning(TimingRequestAlarmTypeService.class)) {
            ServiceUtil.stopService(TimingRequestAlarmTypeService.class);
        }
        if (ServiceUtil.isServiceRunning(RequestWebApiDataService.class)) {
            ServiceUtil.stopService(RequestWebApiDataService.class);
        }
        if (ServiceUtil.isServiceRunning(InitSystemSettingService.class)) {
            ServiceUtil.stopService(InitSystemSettingService.class);
        }
        if (ServiceUtil.isServiceRunning(TimingCheckSipStatus.class)) {
            ServiceUtil.stopService(TimingCheckSipStatus.class);
        }

        if (ServiceUtil.isServiceRunning(UpdateSystemTimeService.class)) {
            ServiceUtil.stopService(UpdateSystemTimeService.class);
        }
        if (ServiceUtil.isServiceRunning(TimingSendHbService.class)) {
            ServiceUtil.stopService(TimingSendHbService.class);
        }
        if (ServiceUtil.isServiceRunning(SerialPortService.class))
            ServiceUtil.stopService(SerialPortService.class);

        ActivityUtils.removeAllActivity();

        AppConfig.SIP_STATUS = false;

        Linphone.getLC().clearProxyConfigs();
    }

    /**
     * 来电操作
     */
    private void incommingCall() {
        //来电清除屏保
        if (ActivityUtils.getTopActivity().getClass().getName().equals("com.tehike.client.stc.app.project.ui.ScreenSaverActivity")) {
            ActivityUtils.getTopActivity().finish();
        }
        //来电切换到勤务通信页面
        if (bottomRadioGroupLayout != null && CustomViewPagerLayout != null) {
            bottomRadioGroupLayout.check(bottomRadioGroupLayout.getChildAt(0).getId());
            CustomViewPagerLayout.setCurrentItem(0);
        }
    }

    /**
     * 开启屏保计时
     */
    private void turnOnScreenSaverTiming() {
        screenSaverCount++;
        Logutil.d("count-->>" + screenSaverCount);
        if (screenSaverCount == AppConfig.SCREEN_SAVE_TIME) {
            if (AppConfig.IS_HAVING_ALARM) {
                screenSaverCount = 0;
            } else {
                openActivity(ScreenSaverActivity.class);
                StcDutyMainActivity.this.sendBroadcast(new Intent(AppConfig.SCREEN_SAVER_ACTION));
                isCallingFlag = false;
                screenSaverCount = 0;
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onDestroy() {
        //时间线程标识更改
        threadIsRun = false;
        //重置屏保标识
        isCallingFlag = false;
        //重置屏保计时
        screenSaverCount = 0;
        //注销来电广播
        if (incomingBroadcast != null) {
            unregisterReceiver(incomingBroadcast);
        }
        //注销刷新网络广播
        if (mFreshNetworkStatusBroadcast != null) {
            unregisterReceiver(mFreshNetworkStatusBroadcast);
        }
        //移除Handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * Handler处理子线程发送的消息
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //提示无数据
                    if (isVisible)
                        showProgressFail(getString(R.string.str_resource_no_data));
                    break;
                case 2:
                    //提示无网络
                    if (isVisible)
                        showProgressFail(getString(R.string.str_resource_no_network));
                    break;
                case 4:
                    //每隔一秒刷新一下时间
                    disPlayCurrentTime();
                    //显示Sip状态（注册、未注册）
                    if (currentServerCenterTv != null && isVisible) {
                        if (AppConfig.SIP_STATUS) {
                            currentServerCenterTv.setTextColor(0xff6adeff);
                            currentServerCenterTv.setText("中心状态:已注册");
                        } else {
                            currentServerCenterTv.setTextColor(0xffff0000);
                            currentServerCenterTv.setText("中心状态:已断开");
                        }
                    }
                    break;
                case 5:
                    //显示网络状态正常
                    if (currentConntectLayout != null && isVisible) {
                        currentConntectLayout.setTextColor(0xff6adeff);
                        currentConntectLayout.setText("网络状态:连接正常");
                    }
                    break;
                case 6:
                    //显示网络状态断开
                    if (currentConntectLayout != null && isVisible) {
                        currentConntectLayout.setTextColor(0xffff0000);
                        currentConntectLayout.setText("网络状态:已断开");
                    }
                    break;
                case 8:
                    //来电操作
                    incommingCall();
                    break;
                case 9:
                    //屏保计时
                    turnOnScreenSaverTiming();
                    break;
                case 10:
                    //显示中队称呼
                    String content = (String) msg.obj;
                    disPlayUnitNameInfor(content);
                    break;
                case 11:
                    //显示哨位名
                    if (currentNameLayout != null)
                        currentNameLayout.setText("哨位名称:" + cc.getDeviceName());
                    break;
            }
        }
    };
}
