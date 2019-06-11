package com.tehike.client.stc.app.project.ui.fragments;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.db.DbHelper;
import com.tehike.client.stc.app.project.db.DbUtils;
import com.tehike.client.stc.app.project.entity.AlarmVideoSource;
import com.tehike.client.stc.app.project.entity.EventSources;
import com.tehike.client.stc.app.project.entity.OpenBoxParamater;
import com.tehike.client.stc.app.project.entity.SipBean;
import com.tehike.client.stc.app.project.entity.SipGroupInfoBean;
import com.tehike.client.stc.app.project.entity.SipGroupItemInfoBean;
import com.tehike.client.stc.app.project.entity.VideoBean;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.ui.BaseFragment;
import com.tehike.client.stc.app.project.ui.views.CustomViewPagerSlide;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.HttpBasicRequest;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.NetworkUtils;
import com.tehike.client.stc.app.project.utils.ScreenUtils;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;
import com.tehike.client.stc.app.project.utils.ToastUtils;
import com.tehike.client.stc.app.project.utils.UIUtils;
import com.tehike.client.stc.app.project.utils.WriteLogToFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerDelegate;
import cn.nodemedia.NodePlayerView;

/**
 * 描述：用于显示报警信息的Fragment页面
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2019/1/2 10:56
 */
public class AlarmFragment extends BaseFragment implements View.OnClickListener, NodePlayerDelegate {

    /**
     * 报警界面的父布局
     */
    @BindView(R.id.secondary_screen_parent_layout)
    RelativeLayout secondaryScreenParentLayout;

    /**
     * 地图背景布局(用于显示地图)
     */
    @BindView(R.id.backgroup_map_view_layout)
    ImageView backGrooupMapLayou;

    /**
     * 根布局
     */
    @BindView(R.id.sh_police_image_relative)
    RelativeLayout parentLayout;

    /**
     * 显示报警点哨兵位置的图片
     */
    @BindView(R.id.police_sentinel_image_layout)
    ImageView sentinelPointLayout;

    /**
     * 左侧功能区的父布局
     */
    @BindView(R.id.left_function_parent_layout)
    RelativeLayout leftFunctionParentLayout;

    /**
     * 右侧功能区的父布局
     */
    @BindView(R.id.right_function_parent_layout)
    RelativeLayout rightFunctionParentLayout;

    /**
     * 左侧显示隐藏的按键
     */
    @BindView(R.id.left_hide_btn_layout)
    ImageButton leftHideBtn;

    /**
     * 左侧显示隐藏的按键
     */
    @BindView(R.id.right_hide_btn_layout)
    ImageButton rightHideBtn;

    /**
     * 侧边的根布局
     */
    @BindView(R.id.side_parent_layout)
    RelativeLayout sideParentLayout;

    /**
     * 显示当前报警信息的父布局
     */
    @BindView(R.id.display_alarm_parent_layout)
    LinearLayout alarmParentLayout;

    /**
     * 播放视频源的View
     */
    @BindView(R.id.alarm_video_layout)
    NodePlayerView alarmVideoView;

    /**
     * 视频加载动画的View
     */
    @BindView(R.id.alarm_loading_icon_layout)
    ImageView alarmVideoloadingIcon;

    /**
     * 视频加载提示的View
     */
    @BindView(R.id.alarm_loading_tv_layout)
    TextView alarmVideoloadingTv;

    /**
     * 播放关联视频源一的View
     */
    @BindView(R.id.relation_first_video_layout)
    NodePlayerView relationFirstVideoView;

    /**
     * 关联视频源一加载动画的View
     */
    @BindView(R.id.relation_first_loading_icon_layout)
    ImageView relationFirstVideoLoadingIcon;

    /**
     * 视频加载提示的View
     */
    @BindView(R.id.relation_first_loading_tv_layout)
    TextView relationFirstVideoLoadingTv;

    /**
     * 播放关联视频源二的View
     */
    @BindView(R.id.relation_second_video_layout)
    NodePlayerView relationSecondVideoView;

    /**
     * 关联视频源二加载动画的View
     */
    @BindView(R.id.relation_second_loading_icon_layout)
    ImageView relationSecondVideoLoadingIcon;

    /**
     * 视频加载提示的View
     */
    @BindView(R.id.relation_second_loading_tv_layout)
    TextView relationSecondVideoLoadingTv;

    /**
     * 播放关联视频源三的View
     */
    @BindView(R.id.relation_third_video_layout)
    NodePlayerView relationThirdVideoView;

    /**
     * 关联视频源三加载动画的View
     */
    @BindView(R.id.relation_third_loading_icon_layout)
    ImageView relationThirdVideoLoadingIcon;

    /**
     * 视频加载提示的View
     */
    @BindView(R.id.relation_third_loading_tv_layout)
    TextView relationThirdVideoLoadingTv;

    /**
     * 哨位分组布局
     */
    @BindView(R.id.sentinel_group_listview_layout)
    ListView sentinelListViewLayout;

    /**
     * 哨位资源分组布局
     */
    @BindView(R.id.sentinel_resources_group_listview_layout)
    ListView sentinelResourcesListViewLayout;

    /**
     * 展示事件信息的ListView
     */
    @BindView(R.id.event_queue_listview_layout)
    ListView eventListViewLayout;

    /**
     * 已处理的报警队列
     */
    @BindView(R.id.processed_alarm_list_layout)
    ListView processedAlarmList;

    /**
     * 正在处理哪个哨位的报警信息
     */
    @BindView(R.id.alarm_handler_sentry_name_layout)
    TextView handlerSentryNameLayout;

    /**
     * 处理报警时的时间信息
     */
    @BindView(R.id.alarm_handler_sentry_time_layout)
    TextView handlerSenrtyTimeLayout;

    /**
     * 左侧功能布局是否隐藏的标识
     */
    boolean leftParentLayotHide = false;

    /**
     * 右侧功能布局是否隐藏的标识
     */
    boolean rightParentLayotHide = false;

    /**
     * 网络请求到的背景图片
     */
    Bitmap backGroupBitmap = null;

    /**
     * 事件信息的队列
     */
    LinkedList<EventSources> eventQueueList = new LinkedList<>();

    /**
     * 接收报警信息的广播
     */
    public ReceiveAlarmBroadcast mReceiveAlarmBroadcast;

    /**
     * 展示事件的适配器
     */
    EventQueueAdapter eventQueueAdapter;

    /**
     * 接收本地缓存的视频字典广播
     */
    public VideoSourcesBroadcast mVideoSourcesBroadcast;

    /**
     * 加载时的动画
     */
    Animation mLoadingAnim;

    /**
     * 报警视频源播放器
     */
    NodePlayer alarmPlayer;

    /**
     * 播放关联视频源一的播放器
     */
    NodePlayer relationFirstPlayer;

    /**
     * 播放关联视频源二的播放器
     */
    NodePlayer relationSecondPlayer;

    /**
     * 播放关联视频源三的播放器
     */
    NodePlayer relationThirdPlayer;

    /**
     * 本地缓存的所有的视频数制（视频字典）
     */
    List<VideoBean> allVideoList;

    /**
     * 本地缓存的所有的Sip数制（SIp字典）
     */
    List<SipBean> allSipList;

    /**
     * 展示已处理报警队列 的适配器
     */
    ProcessedAlarmQueueAdapter mProcessedAlarmQueueAdapter;

    /**
     * 盛放哨位分组的数据
     */
    List<SipGroupInfoBean> sentinelGroupItemList = new ArrayList<>();

    /**
     * d盛放哨位资源分组的适配器
     */
    List<SipGroupItemInfoBean> sentinelResourcesGroupItemList = new ArrayList<>();

    /**
     * 展示哨位分组的适配器
     */
    SentinelGroupAdapter mSentinelGroupAdapter;

    /**
     * 展示哨位资源分组的适配器
     */
    SentinelResourcesGroupItemAdapter mSentinelResourcesGroupItemAdapter;

    /**
     * 广播（Sip缓存完成）
     */
    SipSourcesBroadcast mSipSourcesBroadcast;

    /**
     * 用来存储所有哨位图标的集合
     */
    List<View> allView = new ArrayList<>();

    /**
     * 操作哨位时显示的Popuwindow
     */
    PopupWindow window;

    /**
     * 定时的线程池任务
     */
    private ScheduledExecutorService timingPoolTaskService;

    /**
     * 关闭报警广播
     */
    CloseAlarmBroadcast mCloseAlarmBroadcast;

    /**
     * 定时器
     */
    Timer timer = null;

    /**
     * 刷新已处理的报警列表和事件列表
     */
    ReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast mReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_alarm_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        //初始化viw
        initializeView();
        //加载背景地图
        initLocationMapUrl();
        //注册广播接收报警信息
        registerReceiveAlarmBroadcast();
        //加载已处理的报警信息
        initProcessedAlarmData();
        //加载事件信息
        initEventData();
        //加载本地的所有的视频资源
        initVideoSources();
        //加载本地的Sip资源
        initSipSources();
        //初始化哨位分组数据
        initSentinelGroupData();
        //初始化哨位状态刷新
        initTimingRefreshSentinelStatus();
        //注册刷新右侧列表的广播
        registerReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast();
        //注册广播，监听关闭报警
        registerCloseAlarmBroadcast();
    }

    /**
     * 加载背景地图的url
     */
    private void initLocationMapUrl() {
        //测试
        final String requestUrl = AppConfig.WEB_HOST + SysinfoUtils.getSysinfo().getWebresourceServer() + AppConfig._LOCATIONS;
        //请求Locations资源
        HttpBasicRequest httpBasicRequest = new HttpBasicRequest(requestUrl, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                //WriteLogToFile.info("Locations--->>>" + result);
                Message message = new Message();
                message.obj = result;
                message.what = 8;
                handler.sendMessage(message);
            }
        });
        new Thread(httpBasicRequest).start();
    }

    /**
     * 加载事件信息
     */
    private void initEventData() {
        //清空事件队列
        if (eventQueueList != null && eventQueueList.size() > 0) {
            eventQueueList.clear();
        }
        Cursor c = null;
        try {
            //查询数据库
            c = new DbUtils(App.getApplication()).query(DbHelper.EVENT_TAB_NAME, null, null, null, null, null, null, null);
            if (c == null) {
                Logutil.e("c is null");
                return;
            }
            //遍历Cursor
            if (c.moveToFirst()) {
                do {
                    EventSources mEventSources = new EventSources();
                    String time = c.getString(c.getColumnIndex("time"));
                    String event = c.getString(c.getColumnIndex("event"));
                    mEventSources.setEvent(event);
                    mEventSources.setTime(time);
                    eventQueueList.add(mEventSources);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        //把事件队列反转一下，最新的放在上面
        if (eventQueueList != null && eventQueueList.size() > 0)
            eventQueueList = reverseLinkedList(eventQueueList);
        //适配器展示
        if (eventQueueAdapter != null) {
            eventQueueAdapter = null;
        }
        eventQueueAdapter = new EventQueueAdapter();
        eventListViewLayout.setAdapter(eventQueueAdapter);
        eventQueueAdapter.notifyDataSetChanged();

    }

    /**
     * 反转linkedlist
     */
    private LinkedList reverseLinkedList(LinkedList linkedList) {
        LinkedList<Object> newLinkedList = new LinkedList<>();
        for (Object object : linkedList) {
            newLinkedList.add(0, object);
        }
        return newLinkedList;
    }

    /**
     * 加载哨位分组数据
     */
    private void initSentinelGroupData() {
        //判断网络
        if (!NetworkUtils.isConnected()) {
            handler.sendEmptyMessage(11);
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
                    handler.sendEmptyMessage(12);
                    return;
                }
                //数据异常
                if (result.contains("Execption")) {
                    Logutil.e("请求sip组数据异常" + result);
                    handler.sendEmptyMessage(12);
                    return;
                }
                //让handler去处理数据
                Message sipGroupMess = new Message();
                sipGroupMess.what = 13;
                sipGroupMess.obj = result;
                handler.sendMessage(sipGroupMess);
            }
        });
        new Thread(thread).start();
    }

    /**
     * 加载所有的本地视频资源
     */
    private void initVideoSources() {
        try {
            allVideoList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_VIDEO).toString()), VideoBean.class);
        } catch (Exception e) {
            Logutil.e("取video字典广播异常---->>>" + e.getMessage());
            registerAllVideoSourceDoneBroadcast();
        }
    }

    /**
     * 加载本地的已缓存完成的Sip
     */
    private void initSipSources() {
        try {
            allSipList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_SIP).toString()), SipBean.class);
        } catch (Exception e) {
            //异常后注册广播用来接收sip缓存完成的通知
            registerAllSipSourceDoneBroadcast();
        }

    }

    /**
     * 初始化所有的已处理的报警信息数据
     */
    private void initProcessedAlarmData() {
        //存放已处理的报警事件
        LinkedList<AlarmVideoSource> mlist = new LinkedList<>();
        //清空队列
        mlist.clear();
        Cursor c = null;
        try {
            //查询数据库
            c = new DbUtils(App.getApplication()).query(DbHelper.TAB_NAME, null, null, null, null, null, null, null);
            if (c == null) {
                Logutil.e("c is null");
                return;
            }
            //遍历
            if (c.moveToFirst()) {
                do {
                    AlarmVideoSource alarmVideoSource = new AlarmVideoSource();
                    alarmVideoSource.setSenderIp(c.getString(c.getColumnIndex("senderIp")));
                    alarmVideoSource.setFaceVideoId(c.getString(c.getColumnIndex("faceVideoId")));
                    alarmVideoSource.setAlarmType(c.getString(c.getColumnIndex("alarmType")));
                    alarmVideoSource.setFaceVideoName(c.getString(c.getColumnIndex("faceVideoName")));
                    alarmVideoSource.setTime(c.getString(c.getColumnIndex("time")));
                    mlist.add(alarmVideoSource);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
        } finally {
            //关闭游标
            if (c != null) {
                c.close();
            }
        }
        if (mlist != null && !mlist.isEmpty())
            mlist = reverseLinkedList(mlist);
        //适配数据
        if (mProcessedAlarmQueueAdapter != null) {
            mProcessedAlarmQueueAdapter = null;
        }
        mProcessedAlarmQueueAdapter = new ProcessedAlarmQueueAdapter(mlist);
        processedAlarmList.setAdapter(mProcessedAlarmQueueAdapter);
        //刷新适配器
        mProcessedAlarmQueueAdapter.notifyDataSetChanged();
    }

    /**
     * 初始化View
     */
    private void initializeView() {
        //加载动画
        mLoadingAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.loading);
        //左侧按键监听
        leftHideBtn.setOnClickListener(this);
        //右侧按键监听
        rightHideBtn.setOnClickListener(this);
        //报警视频播放器
        alarmPlayer = new NodePlayer(getActivity());
        alarmPlayer.setPlayerView(alarmVideoView);
        alarmPlayer.setVideoEnable(true);
        alarmPlayer.setAudioEnable(false);
        alarmPlayer.setNodePlayerDelegate(this);

        //报警视频播放器
        relationFirstPlayer = new NodePlayer(getActivity());
        relationFirstPlayer.setPlayerView(relationFirstVideoView);
        relationFirstPlayer.setVideoEnable(true);
        relationFirstPlayer.setAudioEnable(false);
        relationFirstPlayer.setNodePlayerDelegate(this);

        //报警视频播放器
        relationSecondPlayer = new NodePlayer(getActivity());
        relationSecondPlayer.setPlayerView(relationSecondVideoView);
        relationSecondPlayer.setVideoEnable(true);
        relationSecondPlayer.setAudioEnable(false);
        relationSecondPlayer.setNodePlayerDelegate(this);

        //报警视频播放器
        relationThirdPlayer = new NodePlayer(getActivity());
        relationThirdPlayer.setPlayerView(relationThirdVideoView);
        relationThirdPlayer.setVideoEnable(true);
        relationThirdPlayer.setAudioEnable(false);
        relationThirdPlayer.setNodePlayerDelegate(this);

    }

    /**
     * 注册接收刷新事件列表和已处理的报警列表
     */
    private void registerReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast() {
        mReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast = new ReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.REFRESH_ACTION);
        getActivity().registerReceiver(mReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast, intentFilter);
    }

    /**
     * 报警源视频播放器状态回调
     */
    private void alarmPlayerStatusCallback(int event) {
        //状态回调
        if (event == 1102) {
            //连接成功
            alarmVideoloadingTv.setVisibility(View.GONE);
            alarmVideoloadingIcon.clearAnimation();
            alarmVideoloadingIcon.setVisibility(View.GONE);
        } else {
            alarmVideoloadingIcon.setVisibility(View.VISIBLE);
            alarmVideoloadingIcon.startAnimation(mLoadingAnim);
            alarmVideoloadingTv.setVisibility(View.VISIBLE);
            alarmVideoloadingTv.setTextSize(12);
            alarmVideoloadingTv.setTextColor(UIUtils.getColor(R.color.red));
            if (event == 1000) {
                alarmVideoloadingTv.setText("正在连接...");
            } else if (event == 1001) {
                alarmVideoloadingTv.setText("连接成功...");
            } else if (event == 1104) {
                alarmVideoloadingTv.setText("切换视频...");
            } else {
                alarmVideoloadingTv.setText("重新连接...");
            }
        }
    }

    /**
     * 关联视频源一播放器状态回调
     */
    private void relationFirstPlayerStatusCallback(int event) {
        //状态回调
        if (event == 1102) {
            //连接成功
            relationFirstVideoLoadingTv.setVisibility(View.GONE);
            relationFirstVideoLoadingIcon.clearAnimation();
            relationFirstVideoLoadingIcon.setVisibility(View.GONE);
        } else {
            relationFirstVideoLoadingIcon.setVisibility(View.VISIBLE);
            relationFirstVideoLoadingIcon.startAnimation(mLoadingAnim);
            relationFirstVideoLoadingTv.setVisibility(View.VISIBLE);
            relationFirstVideoLoadingTv.setTextSize(12);
            relationFirstVideoLoadingTv.setTextColor(UIUtils.getColor(R.color.red));
            if (event == 1000) {
                relationFirstVideoLoadingTv.setText("正在连接...");
            } else if (event == 1001) {
                relationFirstVideoLoadingTv.setText("连接成功...");
            } else if (event == 1104) {
                relationFirstVideoLoadingTv.setText("切换视频...");
            } else {
                relationFirstVideoLoadingTv.setText("重新连接...");
            }
        }
    }

    /**
     * 关联视频源二播放器状态回调
     */
    private void relationSecondPlayerStatusCallback(int event) {
        //状态回调
        if (event == 1102) {
            //连接成功
            relationSecondVideoLoadingTv.setVisibility(View.GONE);
            relationSecondVideoLoadingIcon.clearAnimation();
            relationSecondVideoLoadingIcon.setVisibility(View.GONE);
        } else {
            relationSecondVideoLoadingIcon.setVisibility(View.VISIBLE);
            relationSecondVideoLoadingIcon.startAnimation(mLoadingAnim);
            relationSecondVideoLoadingTv.setVisibility(View.VISIBLE);
            relationSecondVideoLoadingTv.setTextSize(12);
            relationSecondVideoLoadingTv.setTextColor(UIUtils.getColor(R.color.red));
            if (event == 1000) {
                relationSecondVideoLoadingTv.setText("正在连接...");
            } else if (event == 1001) {
                relationSecondVideoLoadingTv.setText("连接成功...");
            } else if (event == 1104) {
                relationSecondVideoLoadingTv.setText("切换视频...");
            } else {
                relationSecondVideoLoadingTv.setText("重新连接...");
            }
        }
    }

    /**
     * 关联视频源三播放器状态回调
     */
    private void relationThirdPlayerStatusCallback(int event) {
        //状态回调
        if (event == 1102) {
            //连接成功
            relationThirdVideoLoadingTv.setVisibility(View.GONE);
            relationThirdVideoLoadingIcon.clearAnimation();
            relationThirdVideoLoadingIcon.setVisibility(View.GONE);
        } else {
            relationThirdVideoLoadingIcon.setVisibility(View.VISIBLE);
            relationThirdVideoLoadingIcon.startAnimation(mLoadingAnim);
            relationThirdVideoLoadingTv.setVisibility(View.VISIBLE);
            relationThirdVideoLoadingTv.setTextSize(12);
            relationThirdVideoLoadingTv.setTextColor(UIUtils.getColor(R.color.red));
            if (event == 1000) {
                relationThirdVideoLoadingTv.setText("正在连接...");
            } else if (event == 1001) {
                relationThirdVideoLoadingTv.setText("连接成功...");
            } else if (event == 1104) {
                relationThirdVideoLoadingTv.setText("切换视频...");
            } else {
                relationThirdVideoLoadingTv.setText("重新连接...");
            }
        }
    }

    /**
     * 广播刷新已处理的报警队列和事件队列
     */
    class ReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.sendEmptyMessage(16);
        }
    }

    /**
     * 加载背景地图
     */
    private void initBackgroupBitmap(final String s) {

        //开始子线程去请求背景地图
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL iconUrl = new URL(s);
                    URLConnection conn = iconUrl.openConnection();
                    HttpURLConnection http = (HttpURLConnection) conn;
                    int length = http.getContentLength();
                    conn.connect();
                    //获得图像的字符流
                    InputStream is = conn.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is, length);
                    Bitmap bm = BitmapFactory.decodeStream(bis);
                    bis.close();
                    is.close();
                    if (bm != null) {
                        Message message = new Message();
                        message.what = 1;
                        message.obj = bm;
                        handler.sendMessage(message);
                    } else {
                        WriteLogToFile.info("背景地图加载转换异常");
                    }
                } catch (Exception e) {
                    Log.e("TAG", "请求图片异常--" + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 显示地图背景
     */
    private void disPlayBackGroupBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            backGrooupMapLayou.setImageBitmap(bitmap);
            backGroupBitmap = bitmap;
            handler.sendEmptyMessage(3);
        } else {
            Logutil.e("请求到的背景图片为空---");
        }
    }

    /**
     * 计算所有布防点的位置
     */
    private void disPlayAllSentinelPoints() {
        if (backGroupBitmap == null) {
            return;
        }
        //计算网络加载的背景图片的宽高
        int netBitmapWidth = backGroupBitmap.getWidth();
        int netBitmapHeight = backGroupBitmap.getHeight();
        //计算本身背景布局的宽高
        int nativeLayoutwidth = backGrooupMapLayou.getWidth();
        int nativeLayoutHeight = backGrooupMapLayou.getHeight();
        //算出宽高比例
        float percent_width = (float) netBitmapWidth / nativeLayoutwidth;
        float percent_height = (float) netBitmapHeight / nativeLayoutHeight;

        //宽高比例保留两位小数
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        String width_format = decimalFormat.format(percent_width);
        String height_format = decimalFormat.format(percent_height);

        //最终的宽高比例
        float final_format_width = Float.parseFloat(width_format);
        float final_format_height = Float.parseFloat(height_format);


        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(sentinelPointLayout.getLayoutParams());

        //清除上次的所有的图标
        if (parentLayout != null) {
            if (allView != null && allView.size() > 0) {
                ViewGroup viewGroup = (ViewGroup) allView.get(0).getParent();
                for (int n = 0; n < allView.size(); n++) {
                    View view = allView.get(n);
                    if (view != null) {
                        viewGroup.removeView(view);
                    }
                }
                viewGroup.invalidate();
                allView.clear();
            }
        }
        //遍历显示所有的哨位图标
        if (sentinelResourcesGroupItemList != null && sentinelResourcesGroupItemList.size() > 0) {
            for (int i = 0; i < sentinelResourcesGroupItemList.size(); i++) {
                String location = sentinelResourcesGroupItemList.get(i).getLocation();
                if (!TextUtils.isEmpty(location)) {
                    String locationArry[] = location.split(",");
                    int x = Integer.parseInt(locationArry[0]);
                    int y = Integer.parseInt(locationArry[1]);
                    // Logutil.d("x-->>" + x + "\n y---->>" + y);
                    float sentinel_width = Float.parseFloat(decimalFormat.format(x / final_format_width)) - 15;
                    float sentinel_height = Float.parseFloat(decimalFormat.format(y / final_format_height)) - 48;
                    //定义显示其他哨兵的ImageView
                    ImageView other_image = new ImageView(App.getApplication());
                    allView.add(other_image);
                    displaySentinel(other_image, layoutParams, (int) sentinel_width, (int) sentinel_height);
                }
            }
        }
    }

    /**
     * 展示所有的布防点
     */
    private void displaySentinel(ImageView imageView, final ViewGroup.MarginLayoutParams layoutParams, final int sentinel_width, final int sentinel_height) {
        if (layoutParams != null) {
            imageView.setImageResource(R.mipmap.sentinel);

            //设置其他哨兵哨位点的位置
            layoutParams.setMargins(sentinel_width, sentinel_height, 0, 0);
            //将哨位点位置设置到RelativeLayout.LayoutParams
            RelativeLayout.LayoutParams rllps = new RelativeLayout.LayoutParams(layoutParams);
            //设置显示其他哨兵位置图片的宽高
            rllps.width = 60;
            rllps.height = 60;
            //显示图片
            parentLayout.addView(imageView, rllps);

        }
    }

    @Override
    public void onClick(View v) {
        if (window != null && window.isShowing()) {
            window.dismiss();
        }
        switch (v.getId()) {
            case R.id.left_hide_btn_layout:
                //隐藏或显示左侧的功能布局
                hideLeftParentLayout();
                break;
            case R.id.right_hide_btn_layout:
                //隐藏或显示右侧的功能布局
                hideRightParentLayout();
                break;
        }
    }

    /**
     * 注册接收报警信息广播
     */
    private void registerReceiveAlarmBroadcast() {
        mReceiveAlarmBroadcast = new ReceiveAlarmBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.ALARM_ACTION);
        getActivity().registerReceiver(mReceiveAlarmBroadcast, intentFilter);
    }

    /**
     * 当前接收到的报警
     */
    AlarmVideoSource alarm = null;

    /**
     * TTs播报内容
     */
    String ttsContent = "";

    /**
     * 报警显示内容
     */
    String disPlayAlarmContent = "";

    /**
     * 广播接收报警信息
     */
    class ReceiveAlarmBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //接收报警对象
            alarm = (AlarmVideoSource) intent.getSerializableExtra("alarm");
            //判断报警
            if (alarm == null) {
                return;
            }
            Logutil.d("AAAA:" + alarm.toString());
            //报警点的哨位名称
            String sentryName = "";
            //报警类型
            String alarmType = alarm.getAlarmType();
            //报警发送者IP
            String alarmSendIp = alarm.getSenderIp();
            //遍历比对
            if (!TextUtils.isEmpty(alarmType) && !TextUtils.isEmpty(alarmSendIp)) {
                if (allSipList != null && !allSipList.isEmpty()) {
                    for (SipBean s : allSipList) {
                        if (s.getIpAddress().equals(alarm.getSenderIp())) {
                            sentryName = s.getName();
                            break;
                        }
                    }
                }
            }
            //判断内容是否空
            if (!TextUtils.isEmpty(sentryName)) {
                //显示报警信息
                disPlayAlarmContent = sentryName + "发生" + "<b><font color=\"#FF0000\">" + alarmType + "</font></b>报警";
                ttsContent = sentryName + "发生" + alarmType + "报警";
            }
            //显示报警信息
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handlerSentryNameLayout.setText(Html.fromHtml(disPlayAlarmContent));
                }
            });
            //TTs播报报警内容
            alarmTTs(ttsContent);
            //加载已处理的报警数据
            initProcessedAlarmData();
            //加载所有的事件信息数据
            initEventData();
            //播放器停止播放
            playerStop();
            //刷新当前页面可见
            handler.sendEmptyMessage(5);
        }
    }

    /**
     * TTs播报报警信息
     */
    private void alarmTTs(final String disPlayInfor) {
        //取消定时器
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        //实例新的定时器
        timer = new Timer();
        //开启定时任务
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                App.startSpeaking(disPlayInfor);
            }
        }, 0, 4500);
    }

    /**
     * 注册广播监听所有的视频数据是否解析完成
     */
    private void registerAllVideoSourceDoneBroadcast() {
        mVideoSourcesBroadcast = new VideoSourcesBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.RESOLVE_VIDEO_DONE_ACTION);
        getActivity().registerReceiver(mVideoSourcesBroadcast, intentFilter);
    }

    /**
     * Video字典广播
     */
    class VideoSourcesBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //取出本地缓存的所有的Video数据
                allVideoList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_VIDEO).toString()), VideoBean.class);
            } catch (Exception e) {
                Logutil.e("取video字典广播异常---->>>" + e.getMessage());
            }
        }
    }

    /**
     * 注册广播，监听关闭报警
     */
    private void registerCloseAlarmBroadcast() {
        mCloseAlarmBroadcast = new CloseAlarmBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("closeAlarm");
        getActivity().registerReceiver(mCloseAlarmBroadcast, intentFilter);
    }

    /**
     * 关闭报警广播
     */
    class CloseAlarmBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            handler.sendEmptyMessage(7);
        }
    }

    /**
     * 注册广播监听Sip资源缓存完成
     */
    private void registerAllSipSourceDoneBroadcast() {
        mSipSourcesBroadcast = new SipSourcesBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("SipDone");
        getActivity().registerReceiver(mSipSourcesBroadcast, intentFilter);
    }

    /**
     * Sip字典广播
     */
    class SipSourcesBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                allSipList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_SIP).toString()), SipBean.class);
            } catch (Exception e) {
                Logutil.e("取allSipList字典广播异常---->>>" + e.getMessage());
            }
        }
    }

    /**
     * 展示事件信息的适配器
     */
    class EventQueueAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return eventQueueList.size();
        }

        @Override
        public Object getItem(int position) {
            return eventQueueList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_processed_event_item_layout, null);
                viewHolder.eventName = convertView.findViewById(R.id.processed_event_name_layout);
                viewHolder.eventTime = convertView.findViewById(R.id.processed_event_time_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.eventName.setText(eventQueueList.get(position).getEvent());
            viewHolder.eventTime.setText(eventQueueList.get(position).getTime());

            return convertView;
        }

        //内部类
        class ViewHolder {

            //事件名称
            TextView eventName;
            //事件发生时间
            TextView eventTime;
        }
    }

    /**
     * 已处理的的报警队列的适配器
     */
    class ProcessedAlarmQueueAdapter extends BaseAdapter {

        LinkedList<AlarmVideoSource> mlist;


        public ProcessedAlarmQueueAdapter(LinkedList<AlarmVideoSource> mlist) {
            this.mlist = mlist;
        }

        @Override
        public int getCount() {
            return mlist.size();
        }

        @Override
        public Object getItem(int position) {
            return mlist.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_alarm_processed_event_item_layout, null);
                viewHolder.alarmEventName = convertView.findViewById(R.id.alarm_processed_event_name_layout);
                viewHolder.alarmType = convertView.findViewById(R.id.alarm_processed_event_type_layout);
                viewHolder.alarmTime = convertView.findViewById(R.id.alarm_processed_event_time_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //发送报警者的哨位名称
            String alarmSendSentryName = "";
            //报警发送Ip
            String alarmSendIp = mlist.get(position).getSenderIp();
            //遍历
            if (!TextUtils.isEmpty(alarmSendIp) && allSipList != null && !allSipList.isEmpty()) {
                for (SipBean s : allSipList) {
                    if (s.getIpAddress().equals(alarmSendIp)) {
                        alarmSendSentryName = s.getName();
                        break;
                    }
                }
                //显示报警哨位名称
                viewHolder.alarmEventName.setText(Html.fromHtml("<b><font color=\"#ff0000\">" + alarmSendSentryName + "</font></b>"));
            } else {
                //显示报警源视频名称
                viewHolder.alarmEventName.setText(alarmSendIp);
            }
            //显示报警类型
            viewHolder.alarmType.setText(mlist.get(position).getAlarmType());
            //显示报警时间
            viewHolder.alarmTime.setText(mlist.get(position).getTime());
            return convertView;
        }

        //内部类
        class ViewHolder {
            //报警地点
            TextView alarmEventName;
            //报警类型
            TextView alarmType;
            //报警发生时间
            TextView alarmTime;

        }
    }

    /**
     * 隐藏或显示右侧的功能布局
     */
    private void hideRightParentLayout() {
        if (!rightParentLayotHide) {
            rightParentLayotHide = true;
            rightFunctionParentLayout.setVisibility(View.GONE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) rightHideBtn.getLayoutParams();
            layoutParams.setMargins(200, 0, 0, 0);
            rightHideBtn.setLayoutParams(layoutParams);
        } else {
            rightParentLayotHide = false;
            rightFunctionParentLayout.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) rightHideBtn.getLayoutParams();
            layoutParams.setMargins(0, 0, 290, 0);
            rightHideBtn.setLayoutParams(layoutParams);
        }
    }

    /**
     * 隐藏或显示左侧的功能布局
     */
    private void hideLeftParentLayout() {
        if (!leftParentLayotHide) {
            leftParentLayotHide = true;
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) leftHideBtn.getLayoutParams();
            layoutParams.leftMargin = leftHideBtn.getLeft() - 290;
            leftHideBtn.setLayoutParams(layoutParams);
            TranslateAnimation animation = new TranslateAnimation(290, 0, 0, 0);
            animation.setDuration(2000);
            animation.setFillAfter(false);
            leftFunctionParentLayout.startAnimation(animation);
            leftFunctionParentLayout.clearAnimation();
            leftFunctionParentLayout.setVisibility(View.GONE);
        } else {
            leftParentLayotHide = false;
            leftFunctionParentLayout.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) leftHideBtn.getLayoutParams();
            layoutParams.leftMargin = leftHideBtn.getLeft() + 290;
            leftHideBtn.setLayoutParams(layoutParams);
        }
    }

    /**
     * 处理哨位分组数据
     */
    private void handlerSentinelGroupData(String sentinelGroupDataResult) {
        //先清空集合防止
        if (sentinelGroupItemList != null && sentinelGroupItemList.size() > 0) {
            sentinelGroupItemList.clear();
        }

        try {
            JSONObject jsonObject = new JSONObject(sentinelGroupDataResult);
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
                    sentinelGroupItemList.add(sipGroupInfoBean);
                }
            }
            handler.sendEmptyMessage(14);
        } catch (Exception e) {
            Logutil.e("解析Sip分组数据异常" + e.getMessage());
            handler.sendEmptyMessage(12);
        }
    }

    /**
     * 展示哨位分组
     */
    private void displaySentinelListAdapter() {
        //判断是否有要适配的数据
        if (sentinelGroupItemList == null || sentinelGroupItemList.size() == 0) {
            handler.sendEmptyMessage(12);
            Logutil.e("适配的数据时无数据");
            return;
        }
        mSentinelGroupAdapter = new SentinelGroupAdapter(getActivity());
        //显示左侧的sip分组页面
        sentinelListViewLayout.setAdapter(mSentinelGroupAdapter);
        mSentinelGroupAdapter.setSeclection(0);
        mSentinelGroupAdapter.notifyDataSetChanged();

        //默认加载第一组的数据

        String groupId = sentinelGroupItemList.get(0).getId() + "";
        loadVideoGroupItemData(groupId);

        //点击事件
        sentinelListViewLayout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSentinelGroupAdapter.setSeclection(position);
                mSentinelGroupAdapter.notifyDataSetChanged();
                SipGroupInfoBean mSipGroupInfoBean = sentinelGroupItemList.get(position);
                Logutil.i("SipGroupInfoBean-->>" + mSipGroupInfoBean.toString());
                int groupId = mSipGroupInfoBean.getId();
                loadVideoGroupItemData(groupId + "");

            }
        });
    }

    /**
     * 哨位分组适配器
     */
    class SentinelGroupAdapter extends BaseAdapter {
        //选中对象的标识
        private int clickTemp = -1;
        //布局加载器
        private LayoutInflater layoutInflater;

        //构造函数
        public SentinelGroupAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return sentinelGroupItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return sentinelGroupItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setSeclection(int position) {
            clickTemp = position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;

            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.item_video_group_monifor_layout, null);
                viewHolder.videoGroupName = (TextView) convertView.findViewById(R.id.video_group_name_layout);
                viewHolder.videoGroupParentLayout = (RelativeLayout) convertView.findViewById(R.id.video_group_parent_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            SipGroupInfoBean videoGroupInfoBean = sentinelGroupItemList.get(position);

            if (videoGroupInfoBean != null)
                viewHolder.videoGroupName.setText(videoGroupInfoBean.getName());

            //选中状态
            if (clickTemp == position) {
                viewHolder.videoGroupName.setTextColor(0xffffffff);
                viewHolder.videoGroupParentLayout.setBackgroundResource(R.mipmap.dtc_bg_list_group_selected);
            } else {
                viewHolder.videoGroupName.setTextColor(0xff6adeff);
                viewHolder.videoGroupParentLayout.setBackgroundResource(R.mipmap.dtc_bg_list_group_normal);
            }
            return convertView;
        }

        /**
         * 内部类
         */
        class ViewHolder {
            //显示分组名
            TextView videoGroupName;
            //分组item的父布局
            RelativeLayout videoGroupParentLayout;
        }
    }

    /**
     * 加载哨位资源分组数据
     */
    private void loadVideoGroupItemData(final String id) {
        //判断组Id是否为空
        if (TextUtils.isEmpty(id)) {
            return;
        }
        String sipGroupItemUrl = AppConfig.WEB_HOST + SysinfoUtils.getServerIp() + AppConfig._USIPGROUPS_GROUP;

        //子线程根据组Id请求组数据
        HttpBasicRequest httpThread = new HttpBasicRequest(sipGroupItemUrl + id, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                //无数据
                if (TextUtils.isEmpty(result)) {
                    handler.sendEmptyMessage(12);
                    return;
                }
                //数据异常
                if (result.contains("Execption")) {
                    handler.sendEmptyMessage(12);
                    return;
                }

                if (sentinelResourcesGroupItemList != null && sentinelResourcesGroupItemList.size() > 0) {
                    sentinelResourcesGroupItemList.clear();
                }
                //解析sip资源
                try {
                    JSONObject jsonObject = new JSONObject(result);

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
                            groupItemInfoBean.setLocation(jsonItem.getString("location"));
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
                                            jsonItemVideo.getString("name"),
                                            jsonItemVideo.getString("location"),
                                            jsonItemVideo.getString("password"),
                                            jsonItemVideo.getInt("port"),
                                            jsonItemVideo.getString("username"), "", "", "", "", "", "");
                                    groupItemInfoBean.setBean(videoBean);
                                }
                            }
                            sentinelResourcesGroupItemList.add(groupItemInfoBean);
                        }
                    }
                    handler.sendEmptyMessage(15);
                } catch (JSONException e) {
                    WriteLogToFile.info("报警列表组内数据解析异常::" + e.getMessage());
                    Logutil.e("报警列表组内数据解析异常::" + e.getMessage());
                }
            }
        });
        new Thread(httpThread).start();
    }

    /**
     * 展示哨位资源分组
     */
    private void disPlaySentinelResourcesGroupItemAdapter() {
        /**
         *保证数据的填充和适配器刷新在同一线程中
         * 否则：
         *解决ListView的The content of the adapter has changed but ListView did not receive a notification崩溃问题
         */
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSentinelResourcesGroupItemAdapter == null) {
                    mSentinelResourcesGroupItemAdapter = new SentinelResourcesGroupItemAdapter();
                    sentinelResourcesListViewLayout.setAdapter(mSentinelResourcesGroupItemAdapter);
                }
                mSentinelResourcesGroupItemAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 展示哨位资源分组的适配器
     */
    class SentinelResourcesGroupItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return sentinelResourcesGroupItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return sentinelResourcesGroupItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(getActivity()).inflate(R.layout.item_video_group_item_monifor_layout, null);
                viewHolder.sipItemName = convertView.findViewById(R.id.video_group_item_name_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            SipGroupItemInfoBean mDevice = sentinelResourcesGroupItemList.get(position);
            viewHolder.sipItemName.setText(mDevice.getName());
            return convertView;
        }

        class ViewHolder {
            TextView sipItemName;
        }
    }

    /**
     * 开启定时请求服务，用于请求哨位的状态信息（是否在线）
     */
    private void initTimingRefreshSentinelStatus() {
        //定时线程任务池
        if (timingPoolTaskService == null || timingPoolTaskService.isShutdown())
            timingPoolTaskService = Executors.newSingleThreadScheduledExecutor();
        //开户定时的线程滠
        if (!timingPoolTaskService.isShutdown()) {
            timingPoolTaskService.scheduleWithFixedDelay(new TimingRefreshSentinelStatus(), 0L, 10 * 1000, TimeUnit.MILLISECONDS);
        }

    }

    /**
     * 收到报警时修改UiuI效果
     */
    private void updateCurrentUi() {
        //使alarmFragemt可见
        RadioGroup bottomRadioGroupLayout = getActivity().findViewById(R.id.bottom_radio_group_layout);
        CustomViewPagerSlide customViewPagerLayout = getActivity().findViewById(R.id.main_viewpager_layout);
        if (bottomRadioGroupLayout != null && customViewPagerLayout != null) {
            bottomRadioGroupLayout.check(bottomRadioGroupLayout.getChildAt(3).getId());
            customViewPagerLayout.setCurrentItem(3);
        }
        //显示报警弹窗
        if (alarmParentLayout != null)
            alarmParentLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 播放报警源视频
     */
    private void playAlarmVideo() {
        String alarmVideoRtsp = "this is alarm video rtsp";
        if (alarm == null) {
            Logutil.e("alarm is null");
            return;
        }
        String alarmId = alarm.getFaceVideoId();
        if (TextUtils.isEmpty(alarmId)) {
            Logutil.e("alarmId is null");
            return;
        }
        if (allVideoList == null || allVideoList.size() == 0) {
            Logutil.e("allVideoList is null");
            return;
        }

        for (VideoBean v : allVideoList) {
            if (v.getId().equals(alarmId)) {
                alarmVideoRtsp = v.getRtsp();
                break;
            }
        }
        Logutil.e("alarmRtsp--->>" + alarmVideoRtsp);
        alarmPlayer.setInputUrl(alarmVideoRtsp);
        alarmPlayer.start();
    }

    /**
     * 加载关联视频资源
     */
    private void initRelationVideoSources() {
        //判断报警是否为空
        if (alarm == null) {
            Logutil.e("alarm is null");
            return;
        }
        //报警源id
        String alarmId = alarm.getFaceVideoId();
        if (TextUtils.isEmpty(alarmId)) {
            Logutil.e("alarmId is null");
            return;
        }
        //webapi接口查询报警源关联视频
        String relatingUrl = AppConfig.WEB_HOST + SysinfoUtils.getSysinfo().getWebresourceServer() + AppConfig._RELATION_VIDEO + alarmId;
        //请求
        HttpBasicRequest httpBasicRequest = new HttpBasicRequest(relatingUrl, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {

                Message message = new Message();
                message.obj = result;
                message.what = 6;
                handler.sendMessage(message);
            }
        });
        new Thread(httpBasicRequest).start();
    }

    /**
     * 播放友邻哨视频
     */
    private void playRelationVideo(String result) {
        //判断返回的关联视频中是否有数据
        if (result.contains("errorCode")) {
            promptRelationFristPlayerNoRtsp();
            promptRelationSecondPlayerNoRtsp();
            promptRelationThirdPlayerNoRtsp();
            return;
        }
        //解析
        try {
            JSONArray jsonArray = new JSONArray(result);
            //判断关联视频中是否有数据
            if (jsonArray.length() == 0) {
                Logutil.e("jsonArray lenght is 0");
                promptRelationFristPlayerNoRtsp();
                promptRelationSecondPlayerNoRtsp();
                promptRelationThirdPlayerNoRtsp();
                return;
            }
            //获取所有到的关联视频的Id并添加到集合
            List<String> relationVideoIdList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                relationVideoIdList.add(jsonObject.getString("id"));
            }
            //判断本机缓存的所有video字典是否为空
            if (relationVideoIdList == null || relationVideoIdList.size() == 0) {
                Logutil.e("relationVideoIdList length is 0");
                return;
            }
            //遍历关联到的视频rtsp并存在到集合中
            List<String> relationVideoRtspList = new ArrayList<>();
            if (allVideoList != null && allVideoList.size() > 0) {
                for (int i = 0; i < allVideoList.size(); i++) {
                    for (int j = 0; j < relationVideoIdList.size(); j++) {
                        if (allVideoList.get(i).getId().equals(relationVideoIdList.get(j))) {
                            relationVideoRtspList.add(allVideoList.get(i).getRtsp());
                        }
                    }
                }
            }
            //Log
            Logutil.e("关联视频" + relationVideoRtspList.size() + "\n" + relationVideoRtspList.toString());

            //根据最终数据播放
            if (relationVideoRtspList.size() == 1) {
                relationFirstPlayer.setInputUrl(relationVideoRtspList.get(0));
                relationFirstPlayer.start();
                promptRelationSecondPlayerNoRtsp();
                promptRelationThirdPlayerNoRtsp();
            } else if (relationVideoRtspList.size() == 2) {
                relationFirstPlayer.setInputUrl(relationVideoRtspList.get(0));
                relationFirstPlayer.start();
                relationSecondPlayer.setInputUrl(relationVideoRtspList.get(1));
                relationSecondPlayer.start();
                promptRelationThirdPlayerNoRtsp();
            } else if (relationVideoRtspList.size() == 3) {
                relationFirstPlayer.setInputUrl(relationVideoRtspList.get(0));
                relationFirstPlayer.start();
                relationSecondPlayer.setInputUrl(relationVideoRtspList.get(1));
                relationSecondPlayer.start();
                relationThirdPlayer.setInputUrl(relationVideoRtspList.get(2));
                relationThirdPlayer.start();
            } else if (relationVideoRtspList.size() == 4) {
                relationFirstPlayer.setInputUrl(relationVideoRtspList.get(0));
                relationFirstPlayer.start();
                relationSecondPlayer.setInputUrl(relationVideoRtspList.get(1));
                relationSecondPlayer.start();
                relationThirdPlayer.setInputUrl(relationVideoRtspList.get(2));
                relationThirdPlayer.start();
            }

        } catch (Exception e) {
            Logutil.e("解析友邻哨信息异常:" + e.getMessage());
        }
    }

    /**
     * 关闭报警
     */
    private void closeAlarm() {
        //关闭tts语音播报
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (AppConfig.IS_HAVING_ALARM) {
            //发送关闭警灯指使
            if (!AppConfig.KEYBOARD_WITH_SCREEN) {
                boolean closeAlarmLightSuccess = App.getSerialPortManager().sendBytes(AppConfig.CLOSE_ALL_ALARM_LIGHT);
                Logutil.v("closeAlarmLightSuccess:" + closeAlarmLightSuccess);
            }
            //报警布局不可见
            alarmParentLayout.setVisibility(View.GONE);
            App.startSpeaking("值班室关闭报警");
            //重置是否存在报警标识
            AppConfig.IS_HAVING_ALARM = false;
        }

        //播放器停止播放
        playerStop();

    }

    /**
     * 播放器停止播放
     */
    private void playerStop() {
        if (alarmPlayer != null) {
            alarmPlayer.stop();
        }
        if (relationFirstPlayer != null) {
            relationFirstPlayer.stop();
        }
        if (relationSecondPlayer != null) {
            relationSecondPlayer.stop();
        }
        if (relationThirdPlayer != null) {
            relationThirdPlayer.stop();
        }
    }

    /**
     * 提示关联视频一无资源
     */
    private void promptRelationFristPlayerNoRtsp() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                relationFirstVideoLoadingIcon.clearAnimation();
                relationFirstVideoLoadingIcon.setVisibility(View.INVISIBLE);
                relationFirstVideoLoadingTv.setVisibility(View.VISIBLE);
                relationFirstVideoLoadingTv.setText("无视频资源");
                relationFirstVideoLoadingTv.setTextSize(12);
                relationFirstVideoLoadingTv.setTextColor(UIUtils.getColor(R.color.red));
            }
        });
    }

    /**
     * 提示关联视频二无资源
     */
    private void promptRelationSecondPlayerNoRtsp() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                relationSecondVideoLoadingIcon.clearAnimation();
                relationSecondVideoLoadingIcon.setVisibility(View.INVISIBLE);
                relationSecondVideoLoadingTv.setVisibility(View.VISIBLE);
                relationSecondVideoLoadingTv.setText("无视频资源");
                relationSecondVideoLoadingTv.setTextSize(12);
                relationSecondVideoLoadingTv.setTextColor(UIUtils.getColor(R.color.red));
            }
        });
    }

    /**
     * 提示关联视频三无资源
     */
    private void promptRelationThirdPlayerNoRtsp() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                relationThirdVideoLoadingIcon.clearAnimation();
                relationThirdVideoLoadingIcon.setVisibility(View.INVISIBLE);
                relationThirdVideoLoadingTv.setText("无视频资源");
                relationThirdVideoLoadingTv.setVisibility(View.VISIBLE);
                relationThirdVideoLoadingTv.setTextSize(12);
                relationThirdVideoLoadingTv.setTextColor(UIUtils.getColor(R.color.red));
            }
        });
    }

    /**
     * 定时请求哨位状态的子线程
     */
    class TimingRefreshSentinelStatus extends Thread {
        @Override
        public void run() {

//            /**
//             * 定时刷新哨位状态
//             */
//            if (BoxFragment.boxStatusList.size() > 0 && sentinelResourcesGroupItemList.size() > 0) {
//
//                if (BoxFragment.boxStatusList.size() >= sentinelResourcesGroupItemList.size()) {
//
//                    for (int i = 0; i < BoxFragment.boxStatusList.size(); i++) {
//                        for (int j = 0; j < sentinelResourcesGroupItemList.size(); j++) {
//                            if (BoxFragment.boxStatusList.get(i).getID().equals(sentinelResourcesGroupItemList.get(j).getId())) {
//                                Logutil.d("哈哈，大于>>>" + sentinelResourcesGroupItemList.get(j).getName());
//                            }
//                        }
//                    }
//                } else {
//                    for (int i = 0; i < sentinelResourcesGroupItemList.size(); i++) {
//                        for (int j = 0; j < BoxFragment.boxStatusList.size(); j++) {
//                            if (sentinelResourcesGroupItemList.get(i).getId().equals(BoxFragment.boxStatusList.get(j).getID())) {
//                                Logutil.d("哈哈，小于>>>" + sentinelResourcesGroupItemList.get(i).toString());
//                            }
//                        }
//                    }
//                }
//                //
//                // Logutil.d( BoxFragment.boxStatusList.size()+"AAAAAAAAA");
//            }
        }
    }

    /**
     * 刷新右侧表列
     */
    private void refreshQueue() {
        //重新加载事件队列
        initEventData();
        //重新加载已处理的报警队列
        initProcessedAlarmData();
    }

    @Override
    public void onEventCallback(final NodePlayer player, final int event, String msg) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (alarmPlayer == player) {
                    alarmPlayerStatusCallback(event);
                } else if (relationFirstPlayer == player) {
                    relationFirstPlayerStatusCallback(event);
                } else if (relationSecondPlayer == player) {
                    relationSecondPlayerStatusCallback(event);
                } else if (relationThirdPlayer == player) {
                    relationThirdPlayerStatusCallback(event);
                }
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        //页面可见时
        if (isVisibleToUser) {
            //刷新事件列表
            initEventData();
            //刷新已处理的报警事件
            initProcessedAlarmData();

            handler.sendEmptyMessage(15);
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onDestroyView() {
        //关闭定时器
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        //销毁接收报警的广播
        if (mReceiveAlarmBroadcast != null) {
            getActivity().unregisterReceiver(mReceiveAlarmBroadcast);
        }
        //销毁video资源缓存完成的广播
        if (mVideoSourcesBroadcast != null) {
            getActivity().unregisterReceiver(mVideoSourcesBroadcast);
        }
        //销毁sip资源缓存完成的广播
        if (mSipSourcesBroadcast != null) {
            getActivity().unregisterReceiver(mSipSourcesBroadcast);
        }
        //销毁关闭报警的广播
        if (mCloseAlarmBroadcast != null)
            getActivity().unregisterReceiver(mCloseAlarmBroadcast);
        //注销刷新右侧list列表的广播
        if (mReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast != null) {
            getActivity().unregisterReceiver(mReceiveRefreshProcessedAlarmQueueAndEventQueueBroadcast);
        }
        //移除handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        //图片背景释放
        if (backGroupBitmap != null && !backGroupBitmap.isRecycled()) {
            backGroupBitmap.recycle();
            backGroupBitmap = null;
        }
        System.gc();
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {

        //图片背景释放
        if (backGroupBitmap != null && !backGroupBitmap.isRecycled()) {
            backGroupBitmap.recycle();
            backGroupBitmap = null;
        }
        System.gc();

        super.onLowMemory();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //显示报警地图
                    Bitmap bitmap = (Bitmap) msg.obj;
                    disPlayBackGroupBitmap(bitmap);
                    break;
                case 3:
                    //显示所有的哨位点
                    disPlayAllSentinelPoints();
                    break;
                case 5:
                    Logutil.e("isHavingAlarm" + AppConfig.IS_HAVING_ALARM);
                    //接收到报警时修改选中的效果
                    if (!AppConfig.IS_HAVING_ALARM)
                        updateCurrentUi();
                    AppConfig.IS_HAVING_ALARM = true;
                    //播放报警源的视频
                    playAlarmVideo();
                    //加载报警源的关联视频数据
                    initRelationVideoSources();
                    break;
                case 6:
                    //处理关联视频源数据
                    String result = (String) msg.obj;
                    if (!TextUtils.isEmpty(result) && !result.contains("Execption")) {
                        playRelationVideo(result);
                    } else {
                        //异常
                        promptRelationFristPlayerNoRtsp();
                        promptRelationSecondPlayerNoRtsp();
                        promptRelationThirdPlayerNoRtsp();
                    }
                    break;
                case 7:
                    //关闭报警
                    closeAlarm();
                    break;
                case 8:
                    //加载 locations资源数据
                    String locationsData = (String) msg.obj;
                    handlerLocationsData(locationsData);
                    break;
                case 11:
                    if (isVisible() && getActivity() != null)
                        //提示网络异常
                        ToastUtils.showShort("网络异常!");
                    break;
                case 12:
                    if (isVisible() && getActivity() != null)
                        //提示未加载到哨位分组数据
                        //ToastUtils.showShort("未获取到数据!");
                        break;
                case 13:
                    //处理哨位分组数据
                    String sentinelGroupDataResult = (String) msg.obj;
                    handlerSentinelGroupData(sentinelGroupDataResult);
                    break;
                case 14:
                    //展示哨位分组的适配器
                    displaySentinelListAdapter();
                    break;
                case 15:
                    //加载哨位资源分组数据
                    disPlaySentinelResourcesGroupItemAdapter();
                    disPlayAllSentinelPoints();
                    break;
                case 16:
                    //刷新事件队列
                    refreshQueue();
                    break;
            }
        }
    };

    /**
     * 处理locations数据
     */
    private void handlerLocationsData(String locationsData) {
        //本机的Guid
        String nativeGuid = SysinfoUtils.getSysinfo().getDeviceGuid();
        String mapUrl = "";
        //判断本机的Guid
        if (TextUtils.isEmpty(nativeGuid)) {
            WriteLogToFile.info("nativeGuid is null");
            Logutil.e("nativeGuid is null");
            return;
        }
        //判断locations数据是否异常
        if (!TextUtils.isEmpty(locationsData) && !locationsData.contains("Execption") && !locationsData.contains("errorCode")) {
            try {
                JSONObject jsonObject = new JSONObject(locationsData);
                JSONArray jsonArray = jsonObject.getJSONArray("terminals");
                if (jsonArray.length() == 0) {
                    return;
                }
                //遍历terminal获取背景Map
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonItem = jsonArray.getJSONObject(i);
                    String guid = jsonItem.getString("guid");
                    if (guid.equals(nativeGuid)) {
                        mapUrl = jsonItem.getString("mapUrl");
                    }
                }
                //Log
                WriteLogToFile.info("mapurl:" + mapUrl);
               // Logutil.e("mapurl:" + mapUrl);
                //判断背景地图url是否为空
                if (TextUtils.isEmpty(mapUrl)) {
                    Logutil.e("mapUrl is null");
                    return;
                }
                //从网络加载背景地图的图片
                initBackgroupBitmap(mapUrl);
            } catch (Exception e) {
                Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "解析locations异常:" + e.getMessage());
                WriteLogToFile.info(Thread.currentThread().getStackTrace()[2].getClassName() + "解析locations异常:" + e.getMessage());
            }
        }
    }
}
