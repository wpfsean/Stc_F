package com.tehike.client.stc.app.project.ui.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.db.DbHelper;
import com.tehike.client.stc.app.project.db.DbUtils;
import com.tehike.client.stc.app.project.entity.SipBean;
import com.tehike.client.stc.app.project.entity.SipGroupInfoBean;
import com.tehike.client.stc.app.project.entity.SipGroupItemInfoBean;
import com.tehike.client.stc.app.project.entity.VideoBean;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.phone.Linphone;
import com.tehike.client.stc.app.project.phone.PhoneCallback;
import com.tehike.client.stc.app.project.phone.RegistrationCallback;
import com.tehike.client.stc.app.project.phone.SipManager;
import com.tehike.client.stc.app.project.phone.SipService;
import com.tehike.client.stc.app.project.ui.BaseFragment;
import com.tehike.client.stc.app.project.ui.views.CustomViewPagerSlide;
import com.tehike.client.stc.app.project.ui.views.VerticalSeekBar;
import com.tehike.client.stc.app.project.utils.ByteUtil;
import com.tehike.client.stc.app.project.utils.ContextUtils;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.G711Utils;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.HttpBasicRequest;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.NetworkUtils;
import com.tehike.client.stc.app.project.utils.RemoteVoiceRequestUtils;
import com.tehike.client.stc.app.project.utils.StringUtils;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;
import com.tehike.client.stc.app.project.utils.TimeUtils;
import com.tehike.client.stc.app.project.utils.UIUtils;
import com.tehike.client.stc.app.project.utils.WriteLogToFile;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerDelegate;
import cn.nodemedia.NodePlayerView;

/**
 * 描述：对讲呼叫页面
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/12/4 14:06
 */
public class IntercomCallFragment extends BaseFragment {

    /**
     * 切换通话1
     */
    @BindView(R.id.swap_call1_btn_layout)
    Button swapCall1Btn;

    /**
     * 切换通话2
     */
    @BindView(R.id.swap_call2_btn_layout)
    Button swapCall2Btn;

    /**
     * 接通电话
     */
    @BindView(R.id.accept_btn_layout)
    Button acceptCallBtn;

    /**
     * 展示当前勤务通信分组的listView布局
     */
    @BindView(R.id.intercom_group_item_layout)
    public ListView intercomGroupListViewLayout;

    /**
     * 中间展示所有勤务通信成员的gridview布局
     */
    @BindView(R.id.sipitem_gridview_layout)
    public GridView allIntercomItemGridViewLayout;

    /**
     * 显示通话的主界面
     */
    @BindView(R.id.phone_status_layout)
    public RelativeLayout phoneCallParentLayout;

    /**
     * 显示sip状态的主界面
     */
    @BindView(R.id.sip_status_layout)
    public RelativeLayout sipStatusParentLayout;

    /**
     * 对方视频源
     */
    @BindView(R.id.remote_video_layout)
    NodePlayerView remoteVideoLayout;

    /**
     * 本地视频源
     */
    @BindView(R.id.native_video_layout)
    NodePlayerView nativeVideoLayout;

    /**
     * 挂断和拒接按钮
     */
    @BindView(R.id.sip_hangup_btn_layout)
    Button hangUpBtnLayout;

    /**
     * 显示正在与哪个哨位正在通话的布局
     */
    @BindView(R.id.current_call_number_info_layout)
    TextView displayCurrentCallSentryNameLayout;

    /**
     * 远端加载的进度条
     */
    @BindView(R.id.remote_prbar_layout)
    ProgressBar remotePrLayout;

    /**
     * 远端加载提示
     */
    @BindView(R.id.remote_display_tv_layout)
    TextView remoteTvLayout;

    /**
     * 本地加载进度条
     */
    @BindView(R.id.native_prbar_layout)
    ProgressBar nativePrLayout;

    /**
     * 本地加载提示
     */
    @BindView(R.id.native_display_tv_layout)
    TextView nativeTvLayout;

    /**
     * 远程加载提示父布局
     */
    @BindView(R.id.remote_display_layout)
    RelativeLayout remoteParentLayout;

    /**
     * 本地加载提示父布局
     */
    @BindView(R.id.native_display_layout)
    RelativeLayout nativeParentLayout;

    /**
     * 显示通话时间
     */
    @BindView(R.id.display_phone_time_tv_layout)
    public TextView displayPhoneCallTimeLayout;

    /**
     * 远端视频播放的父布局
     */
    @BindView(R.id.remote_video_parent_layout)
    public FrameLayout remoteVideoParentLayout;

    /**
     * 本地底图的父布局
     */
    @BindView(R.id.native_video_parent_layout)
    public FrameLayout nativeVideoParentLayout;

    /**
     * 通话的父布局
     */
    @BindView(R.id.phone_parent_layout)
    public LinearLayout phoneParentLayout;

    /**
     * 静音按键
     */
    @BindView(R.id.mute_btn_layout)
    public RadioButton muteRadioButton;

    /**
     * 声音拖动条
     */
    @BindView(R.id.verticalseekbar_external_sound_layout)
    public VerticalSeekBar voiceSeekbar;

    /**
     * 通话录音
     */
    @BindView(R.id.call_recording_btn_layout)
    public RadioButton callRecordingButton;

    /**
     * item选中标识
     */
    int sipItemSelected = -1;

    /**
     * 电话接通标识
     */
    boolean isCallConnected = false;

    /**
     * 语音电话标识（tue语音电话，false视频电话）
     */
    boolean isVoiceCall = true;

    /**
     * 是否来电标识
     */
    boolean isCommingCall = false;

    /**
     * 是否打电话的标识
     */
    boolean isOutCall = false;

    /**
     * 播放对方视频 的播放器
     */
    NodePlayer remotePlayer = null;

    /**
     * 播放本地视频的播放器
     */
    NodePlayer nativePlayer = null;

    /**
     * 盛放sip组数据的集合
     */
    List<SipGroupInfoBean> intercomGroupDataList = new ArrayList<>();

    /**
     * 盛放展示sip某个组内数据的集合
     */
    List<SipGroupItemInfoBean> allIntercomItemDataList = new ArrayList<>();

    /**
     * 当前页面是是否可见
     */
    boolean currentPageVisible = false;

    /**
     * 中间展示sip状态的Adapter
     */
    AllIntercomItemAdapter sipItemAdapter;

    /**
     * 时间显示线程是否正在远行
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
     * 本地视频源的播放地址
     */
    String nativePlayRtspUrl = "";

    /**
     * 声音控制对象
     */
    AudioManager mAudioManager = null;

    /**
     * 本机的sip号码
     */
    String currentNativeSipNum = "";

    /**
     * 用于远程喊话请求的Socket
     */
    Socket tcpClientSocket = null;

    /**
     * 声音采样率
     */
    public int frequency = 16000;

    /**
     * 录音时声音缓存大小
     */
    private int rBufferSize;

    /**
     * 录音对象
     */
    private AudioRecord recorder;

    /**
     * 停止标识
     */
    private boolean stopRecordingFlag = false;

    /**
     * 用udp发送声音数据的端口
     */
    int port = -1;

    /**
     * 发送声音数据的Udp
     */
    DatagramSocket udpSocket = null;

    /**
     * 用于显示远程喊话时间（布局）
     */
    TextView speaking_time = null;

    /**
     * 显示喊话时间的线程
     */
    SpeakingTimeThread thread = null;

    /**
     * 记录喊话时间
     */
    int speakingTime = 0;

    /**
     * 选中对象的远程Ip
     */
    String remoteIp = "";

    /**
     * Sip组数据列表
     */
    IntercomSipGroupListViewAdapter mIntercomSipGroupListViewAdapter;

    /**
     * 定时的线程池任务
     */
    private ScheduledExecutorService timingPoolTaskService;

    /**
     * 监听sip资源缓存完成的广播
     */
    SipDataCacheBroadcast mSipDataCacheBroadcast;

    /**
     * 本地缓存的所有的Sip数据
     */
    List<SipBean> allCacheList = null;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_intercom_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        //初始化必要参数
        initializeParamaters();
        //初始化本页面数据
        initializeSipGroupsData();
        //静音监听
        initializeMuteRadioBotton();
        //通话录音功能
        initializeRecordingRadioBotton();
        //seekBar拖动事件
        initializeVoiceSeekbar();
        //初始化视频资源
        initializeSipData();
        //注册接收报警的广播
        registerReceiveAlarmBroadcast();
    }

    /**
     * 初始化视频源数据
     */
    private void initializeSipData() {
        //取出本地缓存的所有Sip资源（如果为空或异常时，就注册广播，用来监听sip数据是否已缓存完成）
        try {
            allCacheList = App.getSipS();
            if (allCacheList == null || allCacheList.isEmpty()) {
                registerSipDataDoneBroadcast();
            } else {
                initSipCacheData();
            }
        } catch (Exception e) {
            allCacheList = null;
            allCacheList = App.getSipS();
            registerSipDataDoneBroadcast();
        }
    }

    /**
     * 注册广播监听sip数据是否缓存完成
     */
    private void registerSipDataDoneBroadcast() {
        mSipDataCacheBroadcast = new SipDataCacheBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SipDone");
        getActivity().registerReceiver(mSipDataCacheBroadcast, intentFilter);
    }

    /**
     * 显示cpu和rom使用率的广播
     */
    class SipDataCacheBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            allCacheList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_SIP).toString()), SipBean.class);
            initSipCacheData();
        }
    }

    /**
     * 初始化本地的sip数据
     */
    private void initSipCacheData() {

        //获取本机的rtsp播放地址
        if (allCacheList != null && !allCacheList.isEmpty() && !TextUtils.isEmpty(currentNativeSipNum)) {
            for (int i = 0; i < allCacheList.size(); i++) {
                SipBean mSipbean = allCacheList.get(i);
                if (mSipbean != null) {
                    String sipNumber = mSipbean.getNumber();
                    if (TextUtils.isEmpty(sipNumber)) {
                        nativePlayRtspUrl = "";
                        return;
                    }
                    if (sipNumber.equals(currentNativeSipNum)) {
                        if (mSipbean.getVideoBean() != null) {
                            String rtsp = mSipbean.getVideoBean().getRtsp();
                            if (!TextUtils.isEmpty(rtsp)) {
                                nativePlayRtspUrl = rtsp;
                                break;
                            } else {
                                nativePlayRtspUrl = "";
                            }
                        } else {
                            nativePlayRtspUrl = "";
                        }
                    } else {
                        nativePlayRtspUrl = "";
                    }
                } else {
                    nativePlayRtspUrl = "";
                }
            }
        }
    }

    /**
     * 自定义接口，实现数据回调
     */
    CallBackValue callBackValue;

    /**
     * 绑定
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callBackValue = (CallBackValue) getActivity();
    }

    /**
     * 定义接口向宿 主Activity传递数据
     */
    public interface CallBackValue {
        void SendMessageValue(String strValue);
    }

    /**
     * 初始化参数
     */
    private void initializeParamaters() {

        //本地取出sip号码
        if (SysinfoUtils.getSysinfo() != null) {
            currentNativeSipNum = SysinfoUtils.getSysinfo().getSipUsername();
        }

        //音频处理对象(控制通话声音大小)
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

    }

    /**
     * 初始化数据
     */
    private void initializeSipGroupsData() {

        //判断网络
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(2);
            return;
        }
        //请求sip分组数据的Url
        String sipGroupUrl = AppConfig.WEB_HOST + SysinfoUtils.getServerIp() + AppConfig._USIPGROUPS;

        //请求sip组数据
        HttpBasicRequest thread = new HttpBasicRequest(sipGroupUrl, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                //无数据
                if (TextUtils.isEmpty(result)) {
                    Logutil.e("请求sip组无数据");
                    handler.sendEmptyMessage(1);
                    return;
                }
                //数据异常
                if (result.contains("Execption")) {
                    Logutil.e("请求sip组数据异常" + result);
                    handler.sendEmptyMessage(1);
                    return;
                }
                //让handler去处理数据
                Message sipGroupMess = new Message();
                sipGroupMess.what = 3;
                sipGroupMess.obj = result;
                handler.sendMessage(sipGroupMess);
            }
        });
        new Thread(thread).start();
    }

    /**
     * 声音拖动处理
     */
    private void initializeVoiceSeekbar() {
        //判断声音处理对象是否为空
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        }
        //设置最大的通话声音
        voiceSeekbar.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
        //显示当前的通话声音
        voiceSeekbar.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
        //拖动事件
        voiceSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                Logutil.d("progress--->>>" + progress);
                //根据拖动大小控制音量
                if (mAudioManager != null) {
                    if (progress > 0) {
                        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, progress, AudioManager.FLAG_PLAY_SOUND);

                    } else {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                voiceSeekbar.setProgress(1);
                            }
                        });
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * 静音按键监听
     */
    private void initializeMuteRadioBotton() {
        final MuteRadioButtonValue globalValue = new MuteRadioButtonValue();
        muteRadioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isCheck = globalValue.isCheck();
                if (isCheck) {
                    if (v == muteRadioButton) muteRadioButton.setChecked(false);
                    Linphone.toggleMicro(false);
                } else {
                    if (v == muteRadioButton) muteRadioButton.setChecked(true);
                    Linphone.toggleMicro(true);
                }
                globalValue.setCheck(!isCheck);
            }
        });
    }

    /**
     * 通话录音键盘监听
     */
    private void initializeRecordingRadioBotton() {
        final RecordingRadioButtonValue gValue = new RecordingRadioButtonValue();
        callRecordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isCheck = gValue.isCheck();
                if (isCheck) {
                    if (v == callRecordingButton) callRecordingButton.setChecked(false);
                    Logutil.d("正在录音");
                    callRecordingButton.setText("正在录音");
                    callRecordingButton.setTextColor(0xffff00ff);

                } else {
                    if (v == callRecordingButton) callRecordingButton.setChecked(true);
                    Logutil.d("停止录音");
                    callRecordingButton.setText("停止录音");
                    callRecordingButton.setTextColor(0xffffffff);
                }
                gValue.setCheck(!isCheck);
            }
        });
    }

    /**
     * 处理sip分组数据
     */
    private void handlerSipGroupData(String result) {

        //先清空集合防止
        if (intercomGroupDataList != null && intercomGroupDataList.size() > 0) {
            intercomGroupDataList.clear();
        }

        try {
            JSONObject jsonObject = new JSONObject(result);
            if (!jsonObject.isNull("errorCode")) {
                Logutil.w("请求不到数据信息");
                return;
            }
            int sipCount = jsonObject.getInt("count");
            if (sipCount > 0) {
                JSONArray jsonArray = jsonObject.getJSONArray("groups");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    SipGroupInfoBean sipGroupInfoBean = new SipGroupInfoBean();
                    sipGroupInfoBean.setId(jsonItem.getInt("id"));
                    sipGroupInfoBean.setMember_count(jsonItem.getString("member_count"));
                    sipGroupInfoBean.setName(jsonItem.getString("name"));
                    intercomGroupDataList.add(sipGroupInfoBean);
                }
            }
            handler.sendEmptyMessage(4);
        } catch (Exception e) {
            Logutil.e("解析Sip分组数据异常" + e.getMessage());
            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 上部List适配数据
     */
    private void disPlayListViewAdapter() {
        //判断是否有要适配的数据
        if (intercomGroupDataList == null || intercomGroupDataList.size() == 0) {
            handler.sendEmptyMessage(1);
            Logutil.e("适配的数据时无数据");
            return;
        }
        mIntercomSipGroupListViewAdapter = new IntercomSipGroupListViewAdapter();
        //显示左侧的sip分组页面
        intercomGroupListViewLayout.setAdapter(mIntercomSipGroupListViewAdapter);
        mIntercomSipGroupListViewAdapter.setSelectedItem(0);
        mIntercomSipGroupListViewAdapter.notifyDataSetChanged();

        //默认加载第一组的数据
        Message handlerMess = new Message();
        handlerMess.arg1 = intercomGroupDataList.get(0).getId();
        handlerMess.what = 5;
        handler.sendMessage(handlerMess);

        //点击事件
        intercomGroupListViewLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mIntercomSipGroupListViewAdapter.setSelectedItem(position);
                mIntercomSipGroupListViewAdapter.notifyDataSetChanged();
                SipGroupInfoBean mSipGroupInfoBean = intercomGroupDataList.get(position);
                Logutil.i("SipGroupInfoBean-->>" + mSipGroupInfoBean.toString());
                int groupId = mSipGroupInfoBean.getId();
                disPlaySipGroupItemStatus(groupId);
            }
        });
    }

    /**
     * 当前本机的Sip号码
     */
    String nativeSipNumber;

    /**
     * 请求某个组内的sip状态数据
     */
    private void disPlaySipGroupItemStatus(int id) {

        nativeSipNumber = SysinfoUtils.getSysinfo().getSipUsername();

        //提示无网络
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(2);
            return;
        }
        if (sipItemAdapter != null) {
            sipItemAdapter = null;
            allIntercomItemDataList.clear();
        }
        //获取某个组内数据
        String sipGroupItemUrl = AppConfig.WEB_HOST + SysinfoUtils.getServerIp() + AppConfig._USIPGROUPS_GROUP;

        //子线程根据组Id请求组数据
        HttpBasicRequest httpThread = new HttpBasicRequest(sipGroupItemUrl + id, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                //无数据
                if (TextUtils.isEmpty(result)) {
                    handler.sendEmptyMessage(1);
                    return;
                }
                //数据异常
                if (result.contains("Execption")) {
                    handler.sendEmptyMessage(1);
                    return;
                }
                //清空数据集合
                if (allIntercomItemDataList != null && allIntercomItemDataList.size() > 0) {
                    allIntercomItemDataList.clear();
                }
                //解析sip资源
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    //判断json是否正常
                    if (!jsonObject.isNull("errorCode")) {
                        Logutil.w("请求不到数据信息");
                        return;
                    }
                    int count = jsonObject.getInt("count");
                    if (count > 0) {
                        JSONArray jsonArray = jsonObject.getJSONArray("resources");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonItem = jsonArray.getJSONObject(i);
                            //解析
                            SipGroupItemInfoBean groupItemInfoBean = new SipGroupItemInfoBean();
                            groupItemInfoBean.setDeviceType(jsonItem.getString("deviceType"));
                            groupItemInfoBean.setId(jsonItem.getString("id"));
                            groupItemInfoBean.setIpAddress(jsonItem.getString("ipAddress"));
                            groupItemInfoBean.setName(jsonItem.getString("name"));
                            groupItemInfoBean.setNumber(jsonItem.getString("number"));
                            groupItemInfoBean.setSentryId(jsonItem.getInt("sentryId"));
                            //判断是否有面部视频
                            if (!jsonItem.isNull("videosource")) {
                                //解析面部视频
                                JSONObject jsonItemVideo = jsonItem.getJSONObject("videosource");
                                if (jsonItemVideo != null) {
                                    //封闭面部视频
                                    VideoBean videoBean = new VideoBean(
                                            jsonItemVideo.getString("channel"),
                                            jsonItemVideo.getString("devicetype"),
                                            jsonItemVideo.getString("id"),
                                            jsonItemVideo.getString("ipaddress"),
                                            jsonItemVideo.getString("location"),
                                            jsonItemVideo.getString("name"),
                                            jsonItemVideo.getString("password"),
                                            jsonItemVideo.getInt("port"),
                                            jsonItemVideo.getString("username"), "", "", "", "", "", "");
                                    groupItemInfoBean.setBean(videoBean);
                                }
                            }
                            if (jsonItem.getString("number").equals(nativeSipNumber)) {
                                continue;
                            } else {
                                allIntercomItemDataList.add(groupItemInfoBean);
                            }
                        }
                    }
                    handler.sendEmptyMessage(6);
                } catch (JSONException e) {
                    WriteLogToFile.info("Sip组内数据解析异常::" + e.getMessage());
                    Logutil.e("Sip组内数据解析异常::" + e.getMessage());
                }
            }
        });
        new Thread(httpThread).start();
    }

    /**
     * 子线程请求数据去刷新状态
     */
    class RequestRefreshStatus implements Runnable {

        //传入的url
        String url;
        String userName;
        String userPwd;

        /**
         * 构造方法
         */
        public RequestRefreshStatus(String s, String p, String url) {
            this.url = url;
            this.userName = s;
            this.userPwd = p;
        }

        @Override
        public void run() {
            //加同步锁
            synchronized (this) {
                //可见时去刷新当前状态
                if (isVisible() && currentPageVisible) {
                    try {
                        if (!NetworkUtils.isConnected()) {
                            Logutil.e("刷新状态时网络异常");
                            //提示网络异常
                            handler.sendEmptyMessage(2);
                            //刷新适配器
                            handler.sendEmptyMessage(35);
                        } else {
                            //用HttpURLConnection请求
                            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                            con.setRequestMethod("GET");
                            con.setConnectTimeout(3000);
                            String authString = userName + ":" + userPwd;
                            //添加 basic参数
                            con.setRequestProperty("Authorization", "Basic " + new String(Base64.encode(authString.getBytes(), 0)));
                            con.connect();
                            Message message = new Message();
                            message.what = 7;
                            if (con.getResponseCode() == 200) {
                                InputStream in = con.getInputStream();
                                String result = StringUtils.readTxt(in);
                                message.obj = result;
                            } else {
                                message.obj = "";
                            }
                            handler.sendMessage(message);
                            con.disconnect();
                        }
                    } catch (Exception e) {
                        WriteLogToFile.info("请求刷新勤务页面状态数据时的异常--->>" + e.getMessage());
                        Logutil.e("请求刷新勤务页面状态数据时的异常--->>" + e.getMessage());
                        handler.sendEmptyMessage(35);
                    }
                }
            }
        }
    }

    /**
     * Sip状态刷新时异常时状态没有（取消状态显示）
     */
    private void sipStatusExecptionRefresh() {
        //遍历设置状态数据为不在线
        if (allIntercomItemDataList != null && !allIntercomItemDataList.isEmpty()) {
            for (SipGroupItemInfoBean mSipGroupItemInfoBean : allIntercomItemDataList) {
                if (mSipGroupItemInfoBean.getState() != -1)
                    mSipGroupItemInfoBean.setState(-1);
            }
        }
        if (sipItemAdapter == null) {
            sipItemAdapter = new AllIntercomItemAdapter(getActivity());
            allIntercomItemGridViewLayout.setAdapter(sipItemAdapter);
        }
        sipItemAdapter.notifyDataSetChanged();
    }

    /**
     * 刷新sip状态
     */
    private void handlerSipStatusData(String sisStatusResult) {
        List<SipStatusInfoBean> sipStatusList = new ArrayList<>();

        try {
            if (TextUtils.isEmpty(sisStatusResult)) {
                handler.sendEmptyMessage(1);
                return;
            }
            if (sisStatusResult.contains("errorCode")) {
                return;
            }

            JSONArray jsonArray = new JSONArray(sisStatusResult);
            for (int i = 0; i < jsonArray.length(); i++) {
                int state = jsonArray.getJSONObject(i).getInt("state");
                String name = jsonArray.getJSONObject(i).getString("usrname");
                SipStatusInfoBean mSipStatusInfoBean = new SipStatusInfoBean();
                mSipStatusInfoBean.setName(name);
                mSipStatusInfoBean.setState(state);
                sipStatusList.add(mSipStatusInfoBean);
            }

            for (int n = 0; n < sipStatusList.size(); n++) {
                for (int k = 0; k < allIntercomItemDataList.size(); k++) {
                    if (sipStatusList.get(n).getName().equals(allIntercomItemDataList.get(k).getNumber())) {
                        allIntercomItemDataList.get(k).setState(sipStatusList.get(n).getState());
                    }
                }
            }

            if (sipItemAdapter != null)
                sipItemAdapter.notifyDataSetChanged();
            // Logutil.e("    AppConfig.SIP_STATUS--->>"+    AppConfig.SIP_STATUS);

        } catch (Exception e) {
            Logutil.e("解析SIp状态时异常:-->>" + e.getMessage());
        }
    }

    /**
     * Sip状态类（用于刷新状态时实体类封闭）
     */
    class SipStatusInfoBean implements Serializable {
        private String name;
        private int state;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }
    }

    /**
     * 处理sip数据
     */
    private void diplayGridViewAdaperAndRefreshStatus() {
        //gridView显示数据
        if (sipItemAdapter == null)
            sipItemAdapter = new AllIntercomItemAdapter(getActivity());
        if (currentPageVisible) {
            allIntercomItemGridViewLayout.setAdapter(sipItemAdapter);
            sipItemAdapter.notifyDataSetChanged();
        }

        //item点击事件
        allIntercomItemGridViewLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (sipItemAdapter != null) {
                    if (allIntercomItemDataList != null && allIntercomItemDataList.size() > 0) {

                        //当前点击item的状态
                        int currentItemStatusValue = allIntercomItemDataList.get(position).getState();
                        //非在线空闲状态
                        if (currentItemStatusValue != 1) {
                            return;
                        }
                        //判断选中的是否是在线状态对象
                        if (currentItemStatusValue == 1) {
                            sipItemSelected = position;
                        } else {
                            sipItemSelected = -1;
                        }
                        //选中
                        sipItemAdapter.setSeclection(position);
                        //刷新适配器
                        sipItemAdapter.notifyDataSetChanged();
                        //Log
                        if (sipItemSelected != -1) {
                            Logutil.d("sipItemSelected--->>>" + sipItemSelected);
                            Logutil.d("sipItemSelected---->>>>" + allIntercomItemDataList.get(sipItemSelected).toString());
                        }
                    }
                }
            }
        });

        //定时线程任务池
        if (timingPoolTaskService == null || timingPoolTaskService.isShutdown())
            timingPoolTaskService = Executors.newSingleThreadScheduledExecutor();

        //启动定时线程池任务去刷新sip状态
        String sispStatusUrl = AppConfig.WEB_HOST + SysinfoUtils.getServerIp() + AppConfig._SIS_STATUS;
        if (!timingPoolTaskService.isShutdown()) {
            timingPoolTaskService.scheduleWithFixedDelay(new RequestRefreshStatus(SysinfoUtils.getUserName(), SysinfoUtils.getUserPwd(), sispStatusUrl), 0L, 3000, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 中間GridView数据展示
     */
    class AllIntercomItemAdapter extends BaseAdapter {
        //选中对象的标识
        private int clickTemp = -1;
        //布局加载器
        private LayoutInflater layoutInflater;

        //构造函数
        public AllIntercomItemAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return allIntercomItemDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return allIntercomItemDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setSeclection(int position) {
            clickTemp = position;
        }

        //刷新item背景
        public void refreshItemBg(int index) {
            //判断view是否空
            if (allIntercomItemGridViewLayout == null) {
                return;
            }
            if (sipItemSelected != -1) {
                //得到当前的item
                View v = allIntercomItemGridViewLayout.getChildAt(index);
                if (v != null) {
                    FrameLayout mainLayout = v.findViewById(R.id.frameLayout_item_layout);
                    //设置背景
                    mainLayout.setBackgroundColor(Color.TRANSPARENT);
                    mainLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_free_normal);
                    //重置标识
                    sipItemSelected = -1;
                    clickTemp = -1;
                    //适配器刷新
                    notifyDataSetChanged();
                }
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.activity_sip_status_item, null);
                viewHolder.itemName = (TextView) convertView.findViewById(R.id.item_name);
                viewHolder.mRelativeLayout = (FrameLayout) convertView.findViewById(R.id.frameLayout_item_layout);
                viewHolder.mainLayout = convertView.findViewById(R.id.sipstatus_main_layout);
                viewHolder.deviceType = convertView.findViewById(R.id.device_type_layout);
                viewHolder.StatusIcon = convertView.findViewById(R.id.sip_status_icon_layout);
                viewHolder.sentryId = convertView.findViewById(R.id.sentry_id_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            SipGroupItemInfoBean mSipClient = allIntercomItemDataList.get(position);
            if (mSipClient != null) {
                //显示设备名
                String deviceName = mSipClient.getName();
                if (!TextUtils.isEmpty(deviceName)) {
                    viewHolder.itemName.setText(deviceName);
                } else {
                    viewHolder.itemName.setText("");
                }
                //设备类型
                String deviceType = mSipClient.getDeviceType();
                if (!TextUtils.isEmpty(deviceType)) {
                    if (deviceType.equals("TH-C6000")) {
                        viewHolder.deviceType.setText("移动终端");
                    }
                    if (deviceType.equals("TH-S6100")) {
                        viewHolder.deviceType.setText("哨位终端");
                    }
                    if (deviceType.equals("TH-S6200")) {
                        viewHolder.deviceType.setText("值班终端");
                    }
                }
                //显示哨位Id
                viewHolder.sentryId.setText(mSipClient.getSentryId() + "号哨");

                //显示状态
                int status = mSipClient.getState();
                switch (status) {
                    case -1://未知状态
                        viewHolder.StatusIcon.setBackgroundResource(R.mipmap.intercom_call_icon_offline);
                        viewHolder.mRelativeLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_offline_normal);
                        break;
                    case 1://在线
                        viewHolder.StatusIcon.setBackgroundResource(R.mipmap.intercom_call_icon_free);
                        viewHolder.mRelativeLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_free_normal);
                        break;
                    case 2://振铃
                        viewHolder.StatusIcon.setBackgroundResource(R.mipmap.intercom_call_icon_ringing);
                        viewHolder.mRelativeLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_ringing_normal);
                        break;
                    case 3://通话
                        viewHolder.StatusIcon.setBackgroundResource(R.mipmap.intercom_call_icon_call);
                        viewHolder.mRelativeLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_call_normal);
                        break;
                }
            }

            if (clickTemp == position) {
                //默认只有在线状态对能被选中
                if (allIntercomItemDataList.get(position).getState() == 1 || allIntercomItemDataList.get(position).getState() == 3) {
                    viewHolder.mRelativeLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_free_selected);

                }
            } else {
                viewHolder.mainLayout.setBackgroundColor(Color.TRANSPARENT);
            }

            return convertView;
        }

        /**
         * 内部类
         */
        class ViewHolder {
            //显示设备名
            TextView itemName;
            //根布局
            LinearLayout mainLayout;
            //外层父布局
            FrameLayout mRelativeLayout;
            //显示设备类型
            TextView deviceType;
            //状态图标
            ImageView StatusIcon;
            //哨位Id
            TextView sentryId;
        }
    }

    /**
     * 勤务通信页面左则listview展示数据的adapter
     */
    class IntercomSipGroupListViewAdapter extends BaseAdapter {

        private int selectedItem = -1;

        @Override
        public int getCount() {
            return intercomGroupDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return intercomGroupDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setSelectedItem(int selectedItem) {
            this.selectedItem = selectedItem;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            //复用convertView
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.item_intercom_sipgroup_listview_layout, null);
                viewHolder.sipItemNameLayout = convertView.findViewById(R.id.sipgroup_item_name_layout);
                viewHolder.sipParentLayout = convertView.findViewById(R.id.intercom_sip_group_parent_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //显示组名称
            SipGroupInfoBean itemBean = intercomGroupDataList.get(position);
            viewHolder.sipItemNameLayout.setText(itemBean.getName());
            //是否选中
            if (position == selectedItem) {
                viewHolder.sipParentLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_selected);
                viewHolder.sipItemNameLayout.setTextColor(0xffffe034);
            } else {
                viewHolder.sipParentLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_normal);
                viewHolder.sipItemNameLayout.setTextColor(0xffffffff);
            }
            return convertView;
        }

        //内部类
        class ViewHolder {
            //Sip组名称
            TextView sipItemNameLayout;
            //Sip组所在的父布局
            RelativeLayout sipParentLayout;
        }
    }

    /**
     * 静音的radio值保存（来源网络，可用sharedpreference代替）
     */
    class MuteRadioButtonValue {
        public boolean isCheck() {
            return isCheck;
        }

        public void setCheck(boolean check) {
            isCheck = check;
        }

        private boolean isCheck;
    }

    /**
     * 通话录音的radio值保存（来源网络，可用sharedpreference代替）
     */
    class RecordingRadioButtonValue {
        public boolean isCheck() {
            return isCheck;
        }

        public void setCheck(boolean check) {
            isCheck = check;
        }

        private boolean isCheck;
    }

    /**
     * 按键点击事件
     */
    @OnClick({R.id.accept_btn_layout, R.id.intercom_video_btn_layout, R.id.sip_hangup_btn_layout, R.id.voice_lose_btn_layout, R.id.remote_warring_btn_layout, R.id.remote_gunshot_btn_layout, R.id.remote_speak_btn_layout})
    public void onclickEvent(View view) {
        switch (view.getId()) {
            case R.id.accept_btn_layout:
                //判断来电，并且是非接听状态下
                try {
                    acceptIncomingCall();
                } catch (LinphoneCoreException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.intercom_video_btn_layout:
                //向外打视频电话
                makeOutCall();
                break;
            case R.id.sip_hangup_btn_layout:
                hangupCurrentCall();
                break;
            case R.id.voice_lose_btn_layout:
                break;
            case R.id.remote_warring_btn_layout:
                //远程警告
                remoteWarring();
                break;
            case R.id.remote_speak_btn_layout:
                //远程喊话
                remoteSpeaking();
                break;
            case R.id.remote_gunshot_btn_layout:
                remoteGunshotWarring();
                //远程鸣枪
                break;
        }
    }

    /**
     * 向外拨打电话
     */
    private void makeOutCall() {
        //判断是否先中
        if (sipItemSelected == -1) {
            showProgressFail("请选择操作对象");
            sipItemSelected = -1;
            return;
        }
        //判断当前展示的数据集合是否为空
        if (allIntercomItemDataList == null || allIntercomItemDataList.isEmpty()) {
            Logutil.e("allIntercomItemDataList is null");
            return;
        }
        //判断当前选中对象是否为空
        SipGroupItemInfoBean bean = allIntercomItemDataList.get(sipItemSelected);
        if (bean == null) {
            Logutil.e("bean is null");
            return;
        }
        //非空闲状态禁止打电话
        if (bean.getState() != 1) {
            return;
        }
        //判断选中的对象是否有sip号码
        String number = bean.getNumber();
        if (TextUtils.isEmpty(number)) {
            showProgressFail("无号码");
            return;
        }
        //TTs播报
        App.startSpeaking("正在呼叫" + bean.getName());
        //Log
        Logutil.e("bean:" + bean.toString());
        ContentValues contentValues1 = new ContentValues();
        contentValues1.put("time", TimeUtils.getCurrentTime1());
        contentValues1.put("event", "呼叫" + bean.getName());
        new DbUtils(App.getApplication()).insert(DbHelper.EVENT_TAB_NAME, contentValues1);
        //重新加载sip字典（防止无数据出现 ）
        initializeSipData();
        //加载提示
        // showProgressDialogWithText("正在操作");
        //向外拨打视频电话
        isOutCall = true;
        //延时半秒
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //判断sip是否在线
        if (!AppConfig.SIP_STATUS) {
            Logutil.e("当前sip不在线");
            return;
        }
        if (displayCurrentCallSentryNameLayout != null) {
            displayCurrentCallSentryNameLayout.setVisibility(View.VISIBLE);
            displayCurrentCallSentryNameLayout.setText(Html.fromHtml("正在呼叫 <b><font color=\"#FF0000\">" + bean.getName() + "</font></b>..."));
        }
        //电话
        Linphone.callTo(number, false);
        //显示Ui
        handler.sendEmptyMessage(9);
        //刷新恢复选中的item背景
        if (sipItemAdapter != null)
            sipItemAdapter.refreshItemBg(sipItemSelected);
    }

    /**
     * 远程语音警告
     */
    private void remoteWarring() {
        String remoteIp = "";
        //判断远程操作对象是否选中
        if (sipItemSelected == -1) {
            showProgressFail("请选择操作对象");
            return;
        }
        //获取远程操作对象的Ip
        if (allIntercomItemDataList.get(sipItemSelected) != null) {
            remoteIp = allIntercomItemDataList.get(sipItemSelected).getIpAddress();
            if (TextUtils.isEmpty(remoteIp)) {
                showProgressFail("无号码");
                return;
            }
        }
        //判断网络是否正常
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(2);
            return;
        }
        //子线程远程警告
        RemoteVoiceRequestUtils remoteVoiceRequestUtils = new RemoteVoiceRequestUtils(2, remoteIp, new RemoteVoiceRequestUtils.RemoteCallbck() {
            @Override
            public void remoteStatus(String status) {
                Logutil.i(status);
                if (TextUtils.isEmpty(status) || status.contains("error")) {
                    handler.sendEmptyMessage(20);
                    return;
                }
                Message message = new Message();
                message.what = 21;
                message.obj = status;
                handler.sendMessage(message);
            }
        });
        new Thread(remoteVoiceRequestUtils).start();

        //刷新恢复选中的item背景
        if (sipItemAdapter != null)
            sipItemAdapter.refreshItemBg(sipItemSelected);

    }

    /**
     * 远程鸣枪警告
     */
    private void remoteGunshotWarring() {
        String remoteIp = "";
        //判断远程操作对象是否选中
        if (sipItemSelected == -1) {
            showProgressFail("请选择操作对象");
            return;
        }
        //获取远程操作对象的Ip
        if (allIntercomItemDataList.get(sipItemSelected) != null) {
            remoteIp = allIntercomItemDataList.get(sipItemSelected).getIpAddress();
            if (TextUtils.isEmpty(remoteIp)) {
                showProgressFail("无号码");
                return;
            }
        }
        //判断网络是否正常
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(2);
            return;
        }
        //子线程远程警告
        RemoteVoiceRequestUtils remoteVoiceRequestUtils = new RemoteVoiceRequestUtils(3, remoteIp, new RemoteVoiceRequestUtils.RemoteCallbck() {
            @Override
            public void remoteStatus(String status) {
                if (TextUtils.isEmpty(status) || status.contains("error")) {
                    handler.sendEmptyMessage(20);
                    return;
                }
                Message message = new Message();
                message.what = 21;
                message.obj = status;
                handler.sendMessage(message);
            }
        });
        new Thread(remoteVoiceRequestUtils).start();
        //刷新恢复选中的item背景
        if (sipItemAdapter != null)
            sipItemAdapter.refreshItemBg(sipItemSelected);
    }

    /**
     * 远程喊话
     */
    private void remoteSpeaking() {
        if (sipItemSelected == -1) {
            showProgressFail("请选择操作对象");
            return;
        }
        //获取远程操作对象的Ip
        if (allIntercomItemDataList.get(sipItemSelected) != null) {
            remoteIp = allIntercomItemDataList.get(sipItemSelected).getIpAddress();
            if (TextUtils.isEmpty(remoteIp)) {
                showProgressFail("无号码");
                return;
            }
        }
        //判断网络是否正常
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(2);
            return;
        }
        //获取此对象的设备类型
        String deviceType = allIntercomItemDataList.get(sipItemSelected).getDeviceType();
        if (!TextUtils.isEmpty(deviceType)) {
            if (deviceType.equals("TH-C6000")) {
                handler.sendEmptyMessage(22);
                return;
            }
        }
        //先去请求喊话的操作(见协议)
        requestSpeakingSocket sendSoundData = new requestSpeakingSocket(remoteIp);
        new Thread(sendSoundData).start();
    }

    /**
     * 计时线程开启
     */
    public void callTimingThreadStart() {
        isTimingThreadWork = true;
        if (timingThread != null && timingThread.isAlive()) {
        } else {
            timingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (isTimingThreadWork) {

                        try {
                            Thread.sleep(1 * 1000);
                            handler.sendEmptyMessage(11);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            });
            timingThread.start();
        }
    }

    /**
     * 计时线程停止
     */
    public void callTimingThreadStop() {
        if (isTimingThreadWork) {
            if (timingThread != null && timingThread.isAlive()) {
                timingThread.interrupt();
                timingThread = null;
            }
            timingNumber = 0;
            isTimingThreadWork = false;
        }
    }

    /**
     * 初始化播放器
     */
    private void initializePlayer() {
        //远程（对方）
        if (remotePlayer == null) {
            remotePlayer = new NodePlayer(getActivity());
            remotePlayer.setPlayerView(remoteVideoLayout);
        }
        //本地（自己）
        if (nativePlayer == null) {
            nativePlayer = new NodePlayer(getActivity());
            nativePlayer.setPlayerView(nativeVideoLayout);
        }
    }

    /**
     * 播放对方的视频源视
     */
    private void playRemoteVideo() {
        //获取当前通话
        LinphoneCall c = SipManager.getLc().getCurrentCall();
        //判断当前通话对象是否为空
        if (c == null) {
            return;
        }
        //判断是否获取到了通话远程参数
        if (c.getRemoteAddress() == null) {
            return;
        }
        //通过字典查询sip对象
        SipBean sipbean = querySipBeanFromSipNumber(c.getRemoteAddress().getUserName());
        //判断参数是否为空
        if (sipbean == null || sipbean.getVideoBean() == null) {
            remoteVideoParentLayout.setVisibility(View.VISIBLE);
            remoteTvLayout.setVisibility(View.VISIBLE);
            remoteTvLayout.setTextColor(UIUtils.getColor(R.color.red));
            remoteTvLayout.setText("未加载到视频源");
            remotePrLayout.setVisibility(View.INVISIBLE);

        } else {
            //判断对方是否有面部视频
            if (sipbean.getVideoBean() == null) {
                return;
            }
            //判断是否在播放地址
            String rtsp = sipbean.getVideoBean().getRtsp();
            Logutil.d("remotePlayer--->>>" + rtsp);
            if (TextUtils.isEmpty(rtsp)) {
                remoteTvLayout.setVisibility(View.VISIBLE);
                remoteTvLayout.setTextColor(UIUtils.getColor(R.color.red));
                remotePrLayout.setVisibility(View.INVISIBLE);
                remoteTvLayout.setText("未加载到对方的视频源!");
            } else {
                //判断播放器是否已实例化
                if (remotePlayer == null) {
                    initializePlayer();
                }
                //判断播放器是否正在播放
                if (remotePlayer != null && remotePlayer.isPlaying()) {
                    remotePlayer.stop();
                }
                //开始播放
                remotePlayer.setInputUrl(rtsp);
                remotePlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
                remotePlayer.setNodePlayerDelegate(new NodePlayerDelegate() {
                    @Override
                    public void onEventCallback(NodePlayer player, final int event, String msg) {
                        if (player == remotePlayer) {
                            if (getActivity() != null) {
                                Logutil.d("remotePlayer--->>>" + event);
                                Message remotePlayerMess = new Message();
                                remotePlayerMess.what = 33;
                                remotePlayerMess.arg1 = event;
                                handler.sendMessage(remotePlayerMess);
                            }
                        }
                    }
                });
                remotePlayer.setVideoEnable(true);
                remotePlayer.start();
            }
        }
    }

    /**
     * 播放自己的视频源
     */
    private void playNativeVideo(final String nativeRtsp) {
        //判断视频源地址是否为空
        if (TextUtils.isEmpty(nativeRtsp)) {
            nativeTvLayout.setVisibility(View.VISIBLE);
            nativeTvLayout.setTextColor(UIUtils.getColor(R.color.red));
            nativePrLayout.setVisibility(View.INVISIBLE);
            nativeTvLayout.setText("未加载到视频源");
        } else {
            //判断播放器是为空
            if (nativePlayer == null) {
                initializePlayer();
            }
            //判断播放器是否正在播放
            if (nativePlayer != null && nativePlayer.isPlaying()) {
                nativePlayer.stop();
            }
            //开始播放
            nativePlayer.setInputUrl(nativeRtsp);
            nativePlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            nativePlayer.setNodePlayerDelegate(new NodePlayerDelegate() {
                @Override
                public void onEventCallback(NodePlayer player, int event, String msg) {
                    Message nativePlayerMess = new Message();
                    nativePlayerMess.arg1 = event;
                    nativePlayerMess.what = 34;
                    handler.sendMessage(nativePlayerMess);
                }
            });
            nativePlayer.setVideoEnable(true);
            nativePlayer.start();
        }
    }

    /**
     * 用于远程喊话请求的子线程
     */
    class requestSpeakingSocket extends Thread {
        //远程喊话对象的Ip
        String remoteIp;

        //构造方法
        public requestSpeakingSocket(String remoteIp) {
            this.remoteIp = remoteIp;
        }

        @Override
        public void run() {
            try {
                if (tcpClientSocket == null) {
                    //创建tcp请求
                    tcpClientSocket = new Socket(remoteIp, AppConfig.REMOTE_PORT);
                    //设置请求超时
                    tcpClientSocket.setSoTimeout(3 * 1000);
                    //请求的总数据
                    byte[] requestData = new byte[4 + 4 + 4 + 4];
                    // flag
                    byte[] flag = new byte[4];
                    flag = "RVRD".getBytes();
                    System.arraycopy(flag, 0, requestData, 0, flag.length);

                    // action
                    byte[] action = new byte[4];
                    action[0] = 1;// 0無操作，1遠程喊話，2播放語音警告，3播放鳴槍警告，4遠程監聽，5單向廣播
                    action[1] = 0;
                    action[2] = 0;
                    action[3] = 0;
                    System.arraycopy(action, 0, requestData, 4, action.length);

                    // 接受喊话时=接收语音数据包的 UDP端口(测试)
                    byte[] parameter = new byte[4];
                    System.arraycopy(parameter, 0, requestData, 8, parameter.length);
                    // // 向服务器发消息
                    OutputStream os = tcpClientSocket.getOutputStream();// 字节输出流
                    os.write(requestData);
                    //   tcpSocket.shutdownOutput();// 关闭输出流
                    // 读取服务器返回的消息
                    InputStream in = tcpClientSocket.getInputStream();
                    byte[] data = new byte[20];
                    int read = in.read(data);
                    //   System.out.println("返回的數據" + Arrays.toString(data));
                    // 解析数据头
                    byte[] r_flag = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_flag[i] = data[i];
                    }
                    String r_DataFlag = new String(r_flag, "gb2312");
                    //     System.out.println("數據頭:" + new String(r_flag, "gb2312"));
                    // 解析返回的請求
                    byte[] r_quest = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_quest[i] = data[i + 4];
                    }
                    // 0無操作，1遠程喊話，2播放語音警告，3播放鳴槍警告，4遠程監聽，5單向廣播
                    int r_questCode = r_quest[0];
                    String r_questMess = RemoteVoiceRequestUtils.getMessage(r_questCode);

                    // 返回的状态
                    byte[] r_status = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_status[i] = data[i + 8];
                    }
                    int r_statusCode = r_status[0];
                    String r_statusMess = RemoteVoiceRequestUtils.getStatusMessage(r_statusCode);
                    Logutil.i("应答状态:" + r_statusCode + "\t" + r_statusMess);

                    // 返回参数
                    byte[] r_paramater = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        r_paramater[i] = data[i + 12];
                    }
                    Logutil.i(Arrays.toString(r_paramater));
                    int port = ByteUtil.bytesToInt(r_paramater, 0);

                    if (r_statusMess.equals("Accept")) {
                        Message message = new Message();
                        message.arg1 = port;
                        message.what = 24;
                        handler.sendMessage(message);
                    } else {
                        handler.sendEmptyMessage(23);
                        if (tcpClientSocket != null) {
                            tcpClientSocket.close();
                            tcpClientSocket = null;
                        }
                    }
                }
            } catch (Exception e) {
                if (tcpClientSocket != null) {
                    try {
                        tcpClientSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    tcpClientSocket = null;
                }
                handler.sendEmptyMessage(23);
                Logutil.e("error:" + e.getMessage());
            }
        }
    }

    /**
     * 显示通话界面
     */
    private void disPlayCallView() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //显示Ui
                sipStatusParentLayout.setVisibility(View.GONE);
                phoneCallParentLayout.setVisibility(View.VISIBLE);
                remoteParentLayout.setVisibility(View.VISIBLE);
                nativeParentLayout.setVisibility(View.VISIBLE);
                remoteVideoLayout.setVisibility(View.VISIBLE);
                nativeVideoLayout.setVisibility(View.VISIBLE);
                remotePrLayout.setVisibility(View.VISIBLE);
                remoteTvLayout.setVisibility(View.VISIBLE);
                nativePrLayout.setVisibility(View.VISIBLE);
                nativeTvLayout.setVisibility(View.VISIBLE);
                //显示接听按键
                if (isCommingCall) {
                    acceptCallBtn.setVisibility(View.VISIBLE);
                    hangUpBtnLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * 显示Sip状态页面
     */
    private void disPlaySipView() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sipStatusParentLayout.setVisibility(View.VISIBLE);
                phoneCallParentLayout.setVisibility(View.GONE);

                remotePrLayout.setVisibility(View.GONE);
                remoteTvLayout.setVisibility(View.GONE);
                nativePrLayout.setVisibility(View.GONE);
                nativeTvLayout.setVisibility(View.GONE);

                remoteParentLayout.setVisibility(View.INVISIBLE);
                nativeParentLayout.setVisibility(View.INVISIBLE);
                remoteVideoLayout.setVisibility(View.INVISIBLE);
                nativeVideoLayout.setVisibility(View.INVISIBLE);

                remoteVideoParentLayout.setVisibility(View.INVISIBLE);
                nativeVideoParentLayout.setVisibility(View.INVISIBLE);
                displayPhoneCallTimeLayout.setText("00:00");
                phoneParentLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_voice1);
            }
        });
    }

    /**
     * 根据 sip号码通过字典查询rtsp地址
     */
    public SipBean querySipBeanFromSipNumber(String from) {

        SipBean mSipBean = null;

        //获取本机的rtsp播放地址
        if (allCacheList != null && !allCacheList.isEmpty() && !TextUtils.isEmpty(from)) {
            for (int i = 0; i < allCacheList.size(); i++) {
                if (from.equals(allCacheList.get(i).getNumber())) {
                    mSipBean = allCacheList.get(i);
                    break;
                }
            }
        }
        return mSipBean;
    }

    /**
     * 初始化录音 参数
     */
    public void initializeRecordParamater() {
        try {
            //设置录音缓冲区大小
            rBufferSize = AudioRecord.getMinBufferSize(frequency,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
            //获取录音机对象
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    frequency, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, rBufferSize);
        } catch (Exception e) {
            String msg = "ERROR init: " + e.getStackTrace();
            Logutil.e("error:" + msg);
        }
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        //更改停止录音标识
        stopRecordingFlag = false;
        //开启录音线程
        mRecordingVoiceThread = new RecordingVoiceThread();
        mRecordingVoiceThread.start();
    }

    /**
     * 结束录音
     */
    public void stopRecord() throws IOException {

        //Tcp断开连接
        if (tcpClientSocket != null) {
            tcpClientSocket.close();
            tcpClientSocket = null;
        }
        //Udp断开连接
        if (udpSocket != null) {
            udpSocket.close();
            udpSocket = null;
        }
        //更改停止标识
        stopRecordingFlag = true;
    }

    /**
     * 录音线程
     */
    class RecordingVoiceThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                byte[] tempBuffer, readBuffer = new byte[rBufferSize];
                int bufResult = 0;
                recorder.startRecording();
                while (!stopRecordingFlag) {
                    bufResult = recorder.read(readBuffer, 0, rBufferSize);
                    if (bufResult > 0 && bufResult % 2 == 0) {
                        tempBuffer = new byte[bufResult];
                        System.arraycopy(readBuffer, 0, tempBuffer, 0, rBufferSize);
                        G711EncodeVoice(tempBuffer);
                    }
                }
                recorder.stop();
                Looper.prepare();
                Looper.loop();
            } catch (Exception e) {
                String msg = "ERROR AudioRecord: " + e.getMessage();
                Logutil.e(msg);
                Looper.prepare();
                Looper.loop();
            }
        }
    }

    /**
     * G711a声音压缩
     */
    private void G711EncodeVoice(byte[] tempBuffer) {
        DatagramPacket dp = null;
        try {
            dp = new DatagramPacket(G711Utils.encode(tempBuffer), G711Utils.encode(tempBuffer).length, InetAddress.getByName(remoteIp), port);
            try {
                if (udpSocket == null)
                    udpSocket = new DatagramSocket();
                udpSocket.send(dp);
                Logutil.i("正在发送...." + Arrays.toString(G711Utils.encode(tempBuffer)) + "\n长度" + G711Utils.encode(tempBuffer).length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     * 录音线程
     */
    private RecordingVoiceThread mRecordingVoiceThread;

    /**
     * 广播用来接收报警信息，中断当前的通话
     */
    ReceiveAlarmBroadcast mReceiveAlarmBroadcast;

    /**
     * 注册接收报警信息广播中断当前的通话
     */
    private void registerReceiveAlarmBroadcast() {
        mReceiveAlarmBroadcast = new ReceiveAlarmBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.ALARM_ACTION);
        getActivity().registerReceiver(mReceiveAlarmBroadcast, intentFilter);
    }

    /**
     * 收到报警中断当前通话
     */
    class ReceiveAlarmBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //  Logutil.w("收到报警了");
            callBackValue.SendMessageValue("false");
            //语音电话释放
            if (isOutCall && isVoiceCall) {
                SipManager.getLc().terminateCall(SipManager.getLc().getCurrentCall());
            }
            //向个拨打的视频电话释放
            if (isOutCall && !isVoiceCall) {
                SipManager.getLc().terminateCall(SipManager.getLc().getCurrentCall());
            }
            //来电视频电话释放
            if (isCommingCall && !isVoiceCall) {
                SipManager.getLc().terminateCall(SipManager.getLc().getCurrentCall());
            }
        }
    }

    /**
     * 显示喊话的提示框
     */
    private void showSpeakingDialog() {
        //加载dialog布局
        View view = View.inflate(getActivity(), R.layout.activity_speaking_prompt_dialog_item, null);
        speaking_time = view.findViewById(R.id.speaking_time_layout);
        //正在向谁喊话（布局）
        TextView speaking_name = view.findViewById(R.id.speaking_name_layout);
        //关闭（布局）
        TextView close_dialog = view.findViewById(R.id.seaking_close_dialog_layout);
        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        //点击外面使不消失
        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
        //此处设置位置窗体大小
        dialog.getWindow().setLayout(ContextUtils.dip2px(getActivity(), 280), ContextUtils.dip2px(getActivity(), 280));

        //判断dialog是否正在显示
        if (dialog.isShowing()) {
            //运行喊话计时线程
            if (thread == null)
                thread = new SpeakingTimeThread();
            thread.start();

            String deviceName = "";
            //根据Sip号码查询设备名称
            List<SipBean> mList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_SIP).toString()), SipBean.class);
            for (int i = 0; i < mList.size(); i++) {
                if (mList.get(i).getIpAddress().equals(remoteIp)) {
                    deviceName = mList.get(i).getName();
                    break;
                }
            }
            //显示
            String str = "正在向\t<<< <b><font color=#ff0000>" + deviceName + "</b><font/> >>>喊话";
            speaking_name.setText(Html.fromHtml(str));
        }
        //关闭按钮的点击事件
        close_dialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                speakingTime = 0;
                try {
                    stopRecord();
                    App.startSpeaking("喊话已挂断");
                    //刷新恢复选中的item背景
                    if (sipItemAdapter != null)
                        sipItemAdapter.refreshItemBg(sipItemSelected);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 显示喊话的时间
     */
    private void displaySpeakingTime() {
        speakingTime += 1;
        String speakTime = TimeUtils.getTime(speakingTime);
        if (speaking_time != null)
            speaking_time.setText(speakTime);
    }

    /**
     * 喊话计时
     */
    class SpeakingTimeThread extends Thread {
        @Override
        public void run() {
            super.run();
            do {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logutil.i("Thread error:" + e.getMessage());
                }
                handler.sendEmptyMessage(25);
            } while (!stopRecordingFlag);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        currentPageVisible = isVisibleToUser;
        //Logutil.w("Intercom--->>" + isVisibleToUser);
        if (isVisibleToUser) {
            //可见时刷新GridView的Adapter
            if (sipItemAdapter != null) {
                sipItemAdapter.notifyDataSetChanged();
            }
            //启动Sip服务
            if (!SipService.isReady() || !SipManager.isInstanceiated()) {
                Linphone.startService(App.getApplication());

            }
            //SIp注册状态和来电状态监听回调
            Linphone.addCallback(new RegistrationCallback() {
                @Override
                public void registrationOk() {
                }

                @Override
                public void registrationFailed() {
                }
            }, new PhoneCallback() {
                @Override
                public void incomingCall(LinphoneCall linphoneCall) {
                    //获取当前的来电
                    int currentCallSize = SipManager.getLc().getCalls().length;
                    Logutil.d("calls:" + currentCallSize);
                    //第一个来电
                    if (currentCallSize == 1) {
                        handler.sendEmptyMessage(12);
                    } else if (currentCallSize == 2) {
                        SipManager.getLc().terminateCall(linphoneCall);
                    }
                }

                @Override
                public void outgoingInit() {
                    AppConfig.IS_CALLING = true;
                    Logutil.i("super.outgoingInit();");
                }

                @Override
                public void callConnected() {
                    AppConfig.IS_CALLING = true;
                    Logutil.i("super.callConnected();");
                    //fragment向宿主Activity传递数据
                    callBackValue.SendMessageValue("true");

                    isCallConnected = true;
                    //向外拨打的电话接通
                    if (isOutCall) {
                        handler.sendEmptyMessage(10);
                    }
                    //来电接通
                    if (isCommingCall) {
                        handler.sendEmptyMessage(13);
                    }
                }

                @Override
                public void callEnd() {
                    AppConfig.IS_CALLING = false;
                    Logutil.i("super.callEnd();" + SipManager.getLc().getCalls().length);
                    //fragment向宿主Activity传递数据
                    callBackValue.SendMessageValue("false");
                    //拨电放挂断
                    if (isOutCall) {
                        updateOutCallUiEnd();
                    }
                    //来电挂断
                    if (isCommingCall) {
                        if (SipManager.getLc().getCalls().length == 0)
                            updateIncomingCallUiEnd();
                    }
                    isDutyRoomCall = false;

                }

                @Override
                public void callReleased() {
                    AppConfig.IS_CALLING = false;
                    Logutil.i("super.callReleased();");
                    //fragment向宿主Activity传递数据
                    callBackValue.SendMessageValue("false");
                    if (isOutCall) {
                        updateOutCallUiEnd();
                    }
                    //来电挂断
                    if (isCommingCall) {
                        if (SipManager.getLc().getCalls().length == 0)
                            updateIncomingCallUiEnd();
                    }
                    isDutyRoomCall = false;
                }

                @Override
                public void error() {
                    AppConfig.IS_CALLING = false;
                    Logutil.i("     super.error();");
                    if (isOutCall) {
                        updateOutCallUiEnd();
                    }
                    //来电挂断
                    if (isCommingCall) {
                        if (SipManager.getLc().getCalls().length == 0)
                            updateIncomingCallUiEnd();
                    }
                    isDutyRoomCall = false;
                }
            });
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    /**
     * 接收用键盘拨打电话
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void serialPortMakeCall(SipBean bean) {
        if (bean == null)
            return;
        Logutil.d(bean.toString());
        //更改Ui
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RadioGroup bottomRadioGroupLayout = getActivity().findViewById(R.id.bottom_radio_group_layout);
                CustomViewPagerSlide customViewPagerLayout = getActivity().findViewById(R.id.main_viewpager_layout);
                if (bottomRadioGroupLayout != null && customViewPagerLayout != null) {
                    bottomRadioGroupLayout.check(bottomRadioGroupLayout.getChildAt(0).getId());
                    customViewPagerLayout.setCurrentItem(0);
                }
            }
        });

        //判断选中的对象是否有sip号码
        String number = bean.getNumber();
        if (TextUtils.isEmpty(number)) {
            showProgressFail("无号码");
            return;
        }
        //TTs播报
        App.startSpeaking("正在呼叫" + bean.getName());
        //Log
        Logutil.e("bean:" + bean.toString());
        ContentValues contentValues1 = new ContentValues();
        contentValues1.put("time", TimeUtils.getCurrentTime1());
        contentValues1.put("event", "呼叫" + bean.getName());
        new DbUtils(App.getApplication()).insert(DbHelper.EVENT_TAB_NAME, contentValues1);
        //向外拨打视频电话
        isOutCall = true;
        //延时半秒
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //判断sip是否在线
        if (!AppConfig.SIP_STATUS) {
            Logutil.e("当前sip不在线");
            return;
        }
        if (displayCurrentCallSentryNameLayout != null) {
            displayCurrentCallSentryNameLayout.setVisibility(View.VISIBLE);
            displayCurrentCallSentryNameLayout.setText(Html.fromHtml("正在呼叫 <b><font color=\"#FF0000\">" + bean.getName() + "</font></b>..."));
        }
        //电话
        Linphone.callTo(number, false);
        //显示Ui
        handler.sendEmptyMessage(9);
    }

    /**
     * 是否是值班室来电标识
     */
    boolean isDutyRoomCall = false;

    /**
     * 判断是否是值班室来电
     */
    private void callIsDuryRoomIncoming(final LinphoneCall linphoneCall) {
        //判断当前来电是否为空
        if (linphoneCall == null) {
            return;
        }
        //当前来电号码
        String incomingNumber = linphoneCall.getRemoteAddress().getUserName();
        //判断当前来电号码是否为空
        if (TextUtils.isEmpty(incomingNumber)) {
            return;
        }
        //所有的sip字典
        List<SipBean> allSipList = null;
        try {
            allSipList = App.getSipS();
        } catch (Exception e) {
            allSipList = null;
        }
        //判断字典是否为空
        if (allSipList == null || allSipList.isEmpty()) {
            Logutil.e("sip字典为空");
            return;
        }
        //遍历值班室号码信息
        String dutyRoomNumber = "";
        for (SipBean sipBean : allSipList) {
            if (sipBean.getSentryId().equals("0")) {
                dutyRoomNumber = sipBean.getNumber();
                break;
            }
        }
        //判断值班室号码
        if (TextUtils.isEmpty(dutyRoomNumber)) {
            Logutil.e("dutyRoomNumber is null");
            return;
        }
        //  判断来电号码是否值班室
        if (incomingNumber.equals(dutyRoomNumber)) {
            //子线程延迟两秒并接通电话
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                        //接通电话
                        SipManager.getLc().acceptCall(linphoneCall);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (LinphoneCoreException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            //显示Ui
            acceptCallBtn.setVisibility(View.INVISIBLE);
            hangUpBtnLayout.setVisibility(View.INVISIBLE);
            isDutyRoomCall = true;
        }
    }

    @Override
    public void onDestroyView() {
        //停止定时任务
        if (timingPoolTaskService != null && !timingPoolTaskService.isShutdown()) {
            timingPoolTaskService.shutdown();
            timingPoolTaskService = null;
        }
        //移除handler监听
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        //音频管理对象
        if (mAudioManager != null) {
            mAudioManager = null;
        }
        //释放远程播放器
        if (remotePlayer != null) {
            remotePlayer.stop();
            remotePlayer.release();
        }
        //释放本地释放器
        if (nativePlayer != null) {
            nativePlayer.stop();
            nativePlayer.release();
        }
        //停止录音
        stopRecordingFlag = false;

        if (mSipDataCacheBroadcast != null)
            getActivity().unregisterReceiver(mSipDataCacheBroadcast);

        if (mReceiveAlarmBroadcast != null)
            getActivity().unregisterReceiver(mReceiveAlarmBroadcast);

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onDestroyView();
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
                    if (currentPageVisible && getActivity() != null)
                        showProgressFail(getString(R.string.str_resource_no_data));
                    break;
                case 2:
                    //提示网络异常
                    if (currentPageVisible && getActivity() != null)
                        showProgressFail(getString(R.string.str_network_error));
                    break;
                case 3:
                    //处理sip组数据
                    String result = (String) msg.obj;
                    handlerSipGroupData(result);
                    break;
                case 4:
                    //List适配显示数据
                    disPlayListViewAdapter();
                    break;
                case 5:
                    //默认加载第一组的数据
                    int id = msg.arg1;
                    disPlaySipGroupItemStatus(id);
                    break;
                case 6:
                    //处理某个组内的sip状态数据
                    diplayGridViewAdaperAndRefreshStatus();
                    break;
                case 7:
                    //刷新sip状态
                    String sisStatusResult = (String) msg.obj;
                    handlerSipStatusData(sisStatusResult);
                    break;
                case 8:
                    //去除操作加载
                    if (isOutCall)
                        dismissProgressDialog();
                    break;
                case 9:
                    //显示正在拨打电话
                    updateOutCallUiAndPlayNativeVideo();
                    break;
                case 10:
                    //向外拨打的电话接接通
                    updateOutCallUiAndPlayRemoteVideo();
                    break;
                case 11:
                    //通话计时
                    disPlayCallTime();
                    break;
                case 12:
                    //来电第一个电话
                    updateIncomingCallUiAndPlayNativeVideo();
                    break;
                case 13:
                    //来电接通
                    updateIncomingCallUiAndPlayRemoteVideo();
                    break;
                case 19:
                    //判断当前来电是否是值班室
                    if (getActivity() != null && currentPageVisible && acceptCallBtn != null && hangUpBtnLayout != null) {
                        acceptCallBtn.setVisibility(View.GONE);
                        hangUpBtnLayout.setVisibility(View.GONE);
                    }
                    break;
                case 20:
                    //提示无损操作无法连接
                    showProgressFail(getString(R.string.str_resource_no_connected));
                    break;
                case 21:
                    //提示远程操作结果
                    String status = (String) msg.obj;
                    if (status.equals("Accept")) {
                        showProgressSuccess("完成");
                    } else {
                        showProgressFail("失败");
                    }
                    break;
                case 22:
                    //提示此设备不支持
                    if (getActivity() != null && currentPageVisible)
                        showProgressFail(getString(R.string.str_resource_no_support));
                    break;
                case 23:
                    //提示远程喊话失败
                    showProgressFail("喊话请求失败");
                    App.startSpeaking("喊话请求失败");
                    break;
                case 24:
                    //请求喊话
                    port = msg.arg1;
                    //初始化录音参数
                    initializeRecordParamater();
                    //弹出对话框提示
                    showSpeakingDialog();
                    App.startSpeaking("喊话已建立");
                    //开始录音
                    startRecord();
                    break;
                case 25:
                    //显示喊话时间
                    displaySpeakingTime();
                    break;
                case 33:
                    //远端播放器状态回调
                    int remotePlayerEvent = msg.arg1;
                    remotePlayerPlayStatusCallback(remotePlayerEvent);
                    break;
                case 34:
                    //本地播放器状态回调
                    int nativePlayerEvent = msg.arg1;
                    nativePlayerPlayStatusCallback(nativePlayerEvent);
                    break;
                case 35:
                    //网络异常或cms异常sip状态刷新
                    sipStatusExecptionRefresh();
                    break;
            }
        }
    };

    /**
     * 向外拨打电话时更改UI并播放本机的视频源
     */
    private void updateOutCallUiAndPlayNativeVideo() {
        disPlayCallView();
        //向外拨电话隐藏接电话按键
        acceptCallBtn.setVisibility(View.INVISIBLE);
        //实例播放器
        initializePlayer();
        //更改视频电话的背景
        phoneParentLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_video1);
        //播放本机的视频源
        nativeVideoParentLayout.setVisibility(View.VISIBLE);
        initSipCacheData();
        //播放本机的视频源
        playNativeVideo(nativePlayRtspUrl);
        //去除加载提示
        handler.sendEmptyMessage(8);
    }

    /**
     * 向外拨打电话接通修改显示Ui并播放对方的视频源
     */
    private void updateOutCallUiAndPlayRemoteVideo() {
        //提示正在通话中
        displayCurrentCallSentryNameLayout.setText(Html.fromHtml("正在与<b><font color=\"#FF0000\">" + querySipBeanFromSipNumber(SipManager.getLc().getCurrentCall().getRemoteAddress().getUserName()).getName() + "</font></b>通话..."));
        //开始计时
        callTimingThreadStart();
        //播放对方的视频
        remoteVideoParentLayout.setVisibility(View.VISIBLE);
        //播放对象的视频源
        playRemoteVideo();
    }

    /**
     * 向外拨打的电话挂断后操作
     */
    private void updateOutCallUiEnd() {
        //远程播放器停止播放
        if (remotePlayer != null) {
            remotePlayer.stop();
        }
        //本机视频源播放器停止播放
        if (nativePlayer != null) {
            nativePlayer.stop();
        }
        //停止时计
        callTimingThreadStop();
        //显示Sip界面
        disPlaySipView();
        //重置向外拨打电话的标识
        isOutCall = false;
        //重置电话接通标识
        isCallConnected = false;
    }

    /**
     * 来电
     */
    private void updateIncomingCallUiAndPlayNativeVideo() {
        //判断来电对象是否空空
        if (SipManager.getLc().getCurrentCall() == null) {
            return;
        }
        //重新加载数据字典
        initializeSipData();
        //重置来电标识
        isCommingCall = true;
        //显示来电页面
        disPlayCallView();
        //提示
        displayCurrentCallSentryNameLayout.setText(Html.fromHtml("<b><font color=\"#ff0000\">" + querySipBeanFromSipNumber(SipManager.getLc().getCurrentCall().getRemoteAddress().getUserName()).getName() + " </font></b>来电"));
        //实例播放器
        initializePlayer();
        //更改视频电话的背景
        phoneParentLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_video1);
        //播放本机的视频源
        nativeVideoParentLayout.setVisibility(View.VISIBLE);
        //播放本机的视频源
        playNativeVideo(nativePlayRtspUrl);
        //判断是否是全班室来电
        callIsDuryRoomIncoming(SipManager.getLc().getCurrentCall());
    }

    /**
     * 来电接通
     */
    private void updateIncomingCallUiAndPlayRemoteVideo() {
        //  提示正在通话中
        if (isDutyRoomCall) {
            displayCurrentCallSentryNameLayout.setText(Html.fromHtml("正在与<b><font color=\"#ff0000\">值班室</font></b>通话..."));
        } else {
            displayCurrentCallSentryNameLayout.setText(Html.fromHtml("正在与<b><font color=\"#ff0000\">" + querySipBeanFromSipNumber(SipManager.getLc().getCurrentCall().getRemoteAddress().getUserName()).getName() + "</font></b>通话..."));
        }
        //播放对方的视频
        remoteVideoParentLayout.setVisibility(View.VISIBLE);
        acceptCallBtn.setVisibility(View.INVISIBLE);
        //开始计时
        callTimingThreadStart();
        //播放对象的视频源
        playRemoteVideo();
    }

    /**
     * 来电挂断
     */
    private void updateIncomingCallUiEnd() {

        //远程播放器停止播放
        if (remotePlayer != null) {
            remotePlayer.stop();
        }
        //本机视频源播放器停止播放
        if (nativePlayer != null) {
            nativePlayer.stop();
        }
        //停止时计
        callTimingThreadStop();
        //显示Sip界面
        disPlaySipView();
        //重置来电的标识
        isCommingCall = false;
        //重置电话接通标识
        isCallConnected = false;


    }

    /**
     * 挂断电话
     */
    private void hangupCurrentCall() {
        //判断是否向外拨打的电话
        if (isOutCall) {
            LinphoneCall[] mCalls = SipManager.getLc().getCalls();
            if (mCalls.length == 1) {
                SipManager.getLc().terminateCall(mCalls[0]);
            }
        }
        if (isCommingCall) {
            LinphoneCall[] calls = SipManager.getLc().getCalls();
            if (calls.length == 1) {
                SipManager.getLc().terminateAllCalls();
                disPlaySipView();
                callTimingThreadStop();
                phoneParentLayout.setBackgroundResource(R.mipmap.intercom_call_img_bg_voice1);
                isCommingCall = false;
            }
        }

        App.startSpeaking("通话已挂断");
    }

    /**
     * 接听来电
     */
    private void acceptIncomingCall() throws LinphoneCoreException {
        Logutil.e("isCommingCall:" + isCommingCall);
        if (isCommingCall) {
            LinphoneCall[] calls = SipManager.getLc().getCalls();
            if (calls.length == 1) {
                SipManager.getLc().acceptCall(SipManager.getLc().getCurrentCall());
            } else if (calls.length == 2) {
                SipManager.getLc().pauseCall(calls[0]);
                SipManager.getLc().acceptCall(calls[1]);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swapCall1Btn.setBackgroundResource(R.drawable.btn_pressed_select_bg);
                        swapCall2Btn.setBackgroundResource(R.mipmap.dtc_btn2_bg_pressed);
                    }
                });
            }
        }
    }

    /**
     * 显示通话时计
     */
    private void disPlayCallTime() {
        timingNumber++;
        //显示时间
        if (getActivity() != null && currentPageVisible)
            displayPhoneCallTimeLayout.setText(TimeUtils.getTime(timingNumber) + "");
    }

    /**
     * 播放本机视频源的播放器状态回调
     */
    private void nativePlayerPlayStatusCallback(int nativePlayerEvent) {
        //判断本页面是否可见
        if (getActivity() == null) {
            return;
        }
        //状态回调
        if (nativePlayerEvent == 1102) {
            nativePrLayout.setVisibility(View.GONE);
            nativeTvLayout.setVisibility(View.GONE);
        } else {
            nativePrLayout.setVisibility(View.VISIBLE);
            nativeTvLayout.setVisibility(View.VISIBLE);
            if (nativePlayerEvent == 1000) {
                nativeTvLayout.setText("正在连接...");
            } else if (nativePlayerEvent == 1001) {
                nativeTvLayout.setText("连接成功...");
            } else if (nativePlayerEvent == 1104) {
                nativeTvLayout.setText("切换视频...");
            } else {
                nativeTvLayout.setText("重新连接...");
            }
        }
    }

    /**
     * 远端播放器的状态回调
     */
    private void remotePlayerPlayStatusCallback(int event) {
        if (event == -1 || getActivity() == null) {
            return;
        }
        if (event == 1102) {
            remotePrLayout.setVisibility(View.GONE);
            remoteTvLayout.setVisibility(View.GONE);
        } else {
            remotePrLayout.setVisibility(View.VISIBLE);
            remoteTvLayout.setVisibility(View.VISIBLE);
            if (event == 1000) {
                remoteTvLayout.setText("正在连接...");
            } else if (event == 1001) {
                remoteTvLayout.setText("连接成功...");
            } else if (event == 1104) {
                remoteTvLayout.setText("切换视频...");
            } else {
                remoteTvLayout.setText("重新连接...");
            }
        }
    }
}