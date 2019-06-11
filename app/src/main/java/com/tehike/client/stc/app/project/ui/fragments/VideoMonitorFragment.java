package com.tehike.client.stc.app.project.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kongqw.serialportlibrary.SerialPortManager;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.entity.VideoBean;
import com.tehike.client.stc.app.project.entity.VideoGroupInfoBean;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.onvif.PtzUtils;
import com.tehike.client.stc.app.project.ui.BaseFragment;
import com.tehike.client.stc.app.project.ui.views.OnMultiTouchListener;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.HttpBasicRequest;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.NetworkUtils;
import com.tehike.client.stc.app.project.utils.PageModel;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;
import com.tehike.client.stc.app.project.utils.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTouch;
import cn.nodemedia.NodePlayer;
import cn.nodemedia.NodePlayerDelegate;
import cn.nodemedia.NodePlayerView;

/**
 * 描述：视频监控页面
 * ===============================
 * <p>
 * 思路：先获取组列表,点击每个组展示不同的数据（默认第一组），然后去解析Onvif
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/12/10 9:35
 */
public class VideoMonitorFragment extends BaseFragment implements NodePlayerDelegate {

    /**
     * 展示video分组的ListView
     */
    @BindView(R.id.video_group_listview_layout)
    ListView videoGroupListView;

    /**
     * 展示组内数据的ListView
     */
    @BindView(R.id.group_item_listview_layout)
    ListView groupItemListView;

    /**
     * 第一个播放器所在的父布局
     */
    @BindView(R.id.first_fra_parent_layout)
    RelativeLayout firstParentLayout;
    /**
     * 第一个播放器的VIew
     */
    @BindView(R.id.first_player_view_layout)
    NodePlayerView firstPlayView;

    /**
     * 第一个Pr
     */
    @BindView(R.id.first_player_pr_layout)
    ProgressBar firstPr;

    /**
     * 第一个loadTV
     */
    @BindView(R.id.first_load_tv_layout)
    TextView firstLoadTv;

    /**
     * 第二个播放器所在的父布局
     */
    @BindView(R.id.second_fra_parent_layout)
    RelativeLayout secondParentLayout;

    /**
     * 第二个播放器的VIew
     */
    @BindView(R.id.second_player_view_layout)
    NodePlayerView secondPlayView;

    /**
     * 第二个Pr
     */
    @BindView(R.id.second_player_pr_layout)
    ProgressBar secondPr;

    /**
     * 第二个loadTV
     */
    @BindView(R.id.second_load_tv_layout)
    TextView secondLoadTv;

    /**
     * 第三个播放器所在的父布局
     */
    @BindView(R.id.third_fra_parent_layout)
    RelativeLayout thirdParentLayout;

    /**
     * 第三个播放器的VIew
     */
    @BindView(R.id.third_player_view_layout)
    NodePlayerView thirdPlayView;

    /**
     * 第三个Pr
     */
    @BindView(R.id.third_player_pr_layout)
    ProgressBar thirdPr;

    /**
     * 第三个loadTV
     */
    @BindView(R.id.third_load_tv_layout)
    TextView thirdLoadTv;

    /**
     * 第四个播放器所在的父布局
     */
    @BindView(R.id.fourth_fra_parent_layout)
    RelativeLayout fourthParentLayout;

    /**
     * 第四个播放器的VIew
     */
    @BindView(R.id.fourth_player_view_layout)
    NodePlayerView fourthPlayView;

    /**
     * 第四个Pr
     */
    @BindView(R.id.fourth_player_pr_layout)
    ProgressBar fourthPr;

    /**
     * 第四个loadTV
     */
    @BindView(R.id.fourth_load_tv_layout)
    TextView fourthLoadTv;

    /**
     * 四分屏所在的父布局
     */
    @BindView(R.id.many_play_parent_layout)
    LinearLayout multiScreenLayout;

    /**
     * 单屏播放器的View
     */
    @BindView(R.id.single_player_layout)
    NodePlayerView singlePlayView;

    /**
     * 单屏所在的父布局
     */
    @BindView(R.id.single_play_parent_layout)
    FrameLayout singleScreenLayout;

    /**
     * 当前页面是否可见
     */
    boolean isCurrentPageVisible = false;

    /**
     * 单屏加载条
     */
    @BindView(R.id.single_player_load_image_layout)
    ImageView singleLoadingImg;

    /**
     * 单屏播放提示
     */
    @BindView(R.id.single_loadtv_layout)
    TextView singleLoadTv;

    /**
     * 资源切换
     */
    @BindView(R.id.source_control_layout)
    TextView sourceTvLayout;

    /**
     * 云台控制
     */
    @BindView(R.id.ptz_control_layout)
    TextView ptzTvLayout;

    /**
     * 资源显示的父布局
     */
    @BindView(R.id.source_parent_layout)
    LinearLayout sourceParentLayout;

    /**
     * 云台控制显示的父布局
     */
    @BindView(R.id.ptz_parent_layout)
    LinearLayout ptzParentLayout;

    /**
     * 显示摇杆指令
     */
    @BindView(R.id.display_order_tv_layout)
    TextView displayOrderTvLayout;

    /**
     * 盛放video视频组数据的集合(上部列表)
     */
    List<VideoGroupInfoBean> videoGroupList = new ArrayList<>();

    /**
     * 盛放video视频数据的集合（下部列表 ）
     */
    List<VideoBean> videoGroupItemList = new ArrayList<>();

    /**
     * 当前点击时存放数据的列表（通过字典查询后的视频组列表数据）
     */
    List<VideoBean> videoDataSources = new ArrayList<>();

    /**
     * 当前四屏的数据集合
     */
    List<VideoBean> currentList = new ArrayList<>();

    /**
     * 本地缓存的所有的视频数制（视频字典）
     */
    List<VideoBean> allVideoList;

    /**
     * 展示某个组内数据的ada
     */
    GroupItemAdapter mGroupItemAdapter;

    /**
     * 展示所有组数据的Ada
     */
    VideoGroupAdapter videoGroupAdapter;

    /**
     * 单屏播放器
     */
    NodePlayer singlePlayer;

    /**
     * 第一个播放器
     */
    NodePlayer firstPlayer;

    /**
     * 第二个播放器
     */
    NodePlayer secondPlayer;

    /**
     * 第三个播放器
     */
    NodePlayer thirdPlayer;

    /**
     * 第四个播放器
     */
    NodePlayer fourthPlayer;

    /**
     * 第一个view是否选 中
     */
    boolean firstViewSelected = true;

    /**
     * 第二个view是否选 中
     */
    boolean secondViewSelected = false;

    /**
     * 第三个view是否选 中
     */
    boolean thirdViewSelected = false;

    /**
     * 第四个view是否选 中
     */
    boolean fourthViewSelected = false;

    /**
     * 四分屏分页加载器
     */
    PageModel pm;

    /**
     * 当前页码
     */
    int videoCurrentPage = 1;

    /**
     * 加载时的动画
     */
    Animation mLoadingAnim;

    /**
     * 单屏时画面的对象
     */
    VideoBean currentDevice = null;

    /**
     * 当前单屏视频对象的位置下标
     */
    int currentDevicePosition = 0;

    /**
     * 当前 是否是四分屏状态
     */
    boolean isCurrentFourScreen = false;

    /**
     * 当前是否是单屏状态
     */
    boolean isCurrentSingleScreen = true;

    /**
     * 广播接收video资源缓存完成
     */
    RefreshVideoDataBroadcast mRefreshVideoDataBroadcast;

    /**
     * 盛放每个视频组的集合
     */
    Map<String, List<VideoBean>> map = new HashMap<>();

    /**
     * 用来标识是哪个视频组的标识
     */
    String groupIdFlag = "";

    /**
     * 云台摇杆单条指令
     */
    String mSingleInstruct = "";

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_videomonotor_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        //初始化
        initializeVideoData();
        //初始化播放器
        initializePlayer();
        //播放器双击或单击事件
        videoClickEvent();
        //分屏模式广播
        registerCustomScreenBroadcast();
        //窗口选中广播
        registerWindowSelectedBroadcast();
    }

    /**
     * 初始化串口
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void initializeSerialPort(String str) {
        if (getActivity() != null && isCurrentPageVisible) {
            Message message = new Message();
            message.what = 24;
            message.obj = str;
            handler.sendMessage(message);
        }
    }

    /**
     * 串口指令控制
     */
    private void handlerPTZControl(final String singleInstruct) {
        //摇杆晃动时判断当前视频页面是否可见
        if (!isCurrentPageVisible || getActivity() == null) {
            return;
        }
        //子线程发送更改指令UI
        new Thread() {
            public void run() {
                mSingleInstruct = singleInstruct;
                handler.post(udateOrderRunnableUi);
            }
        }.start();

        //判断当前视频对象是否为空
        if (currentDevice == null) {
            Logutil.d("当前视频对象为空");
            return;
        }

        //判断是否结束的指令
        if (!TextUtils.isEmpty(singleInstruct) && singleInstruct.endsWith("06")) {
            //子线程去执行停止操作
            new Thread(new PtzUtils(currentDevice, "stop")).start();
            Log.d("TAG", "云台停止///////////");
        } else {
            //计算云台控制的范围
            String y = singleInstruct.substring(2, 6);
            String x = singleInstruct.substring(6, 10);
            String z = singleInstruct.substring(10, 14);
            //向下的最小值
            int downMin = Integer.valueOf("0020", 16);
            //向下的最大值
            int downMax = Integer.valueOf("01ff", 16);
            //向上的最小值
            int upMin = Integer.valueOf("0201", 16);
            //向上的最大值
            int upMax = Integer.valueOf("03df", 16);

            if (!x.equals("0200") && !y.equals("0200") && !z.equals("0200")) {
                //xyz同时转动
                Log.d("TAG", "xyz同时转动");
            } else if (x.equals("0200") && !y.equals("0200") && !z.equals("0200")) {
                //x不动，y和z转动
                Log.d("TAG", "x不动，y和z转动");
            } else if (y.equals("0200") && !x.equals("0200") && !z.equals("0200")) {
                //y不动，x和z转动
                Log.d("TAG", "y不动，x和z转动");
            } else if (z.equals("0200") && !x.equals("0200") && !y.equals("0200")) {
                //z不动，x和y转动
                Log.e("TAG", "z不动，x和y转动");

                int yy = Integer.valueOf(y, 16);
                int xx = Integer.valueOf(x, 16);
                //左下
                if ((downMin < xx && xx < downMax) && (downMin < yy && yy < downMax)) {
                    Log.w("TAG", "左下");
                    new Thread(new PtzUtils(currentDevice, "left_below")).start();
                }
                //左上
                if ((downMin < xx && xx < downMax) && (upMin < yy && yy < upMax)) {
                    Log.w("TAG", "左上");
                    new Thread(new PtzUtils(currentDevice, "top_left")).start();
                }
                //右上
                if ((upMin < xx && xx < upMax) && (upMin < yy && yy < upMax)) {
                    Log.w("TAG", "右上");
                    new Thread(new PtzUtils(currentDevice, "top_right")).start();
                }
                //右下
                if ((upMin < xx && xx < upMax) && (downMin < yy && yy < downMax)) {
                    Log.w("TAG", "右下");
                    new Thread(new PtzUtils(currentDevice, "right_below")).start();
                }
            } else if (!z.equals("0200") && x.equals("0200") && y.equals("0200")) {
                //xy不动，z转动
                Log.d("TAG", "xy不动，z转动");
                int zz = Integer.valueOf(z, 16);
                //放大
                if (downMin < zz && zz < downMax) {
                    Log.d("TAG", "ZoomBig");
                    new Thread(new PtzUtils(currentDevice, "zoom_b")).start();
                }
                //缩小
                if (upMin < zz && zz < upMax) {
                    Log.d("TAG", "ZoomSmall");
                    new Thread(new PtzUtils(currentDevice, "zoom_s")).start();
                }
            } else if (z.equals("0200") && x.equals("0200") && !y.equals("0200")) {
                //zx不动，y动
                Log.e("TAG", "zx不动，y动");
                int yy = Integer.valueOf(y, 16);
                if (downMin < yy && yy < downMax) {
                    Log.w("TAG", "向下移动");
                    new Thread(new PtzUtils(currentDevice, "below")).start();
                } else if (upMin < yy && yy < upMax) {
                    Log.w("TAG", "向上移动");
                    new Thread(new PtzUtils(currentDevice, "top")).start();
                }
            } else if (z.equals("0200") && !x.equals("0200") && y.equals("0200")) {
                //zy不动，x动
                Log.e("TAG", "zy不动，x动");
                int xx = Integer.valueOf(x, 16);
                if (downMin < xx && xx < downMax) {
                    Log.w("TAG", "向左移动");
                    new Thread(new PtzUtils(currentDevice, "left")).start();
                } else if (upMin < xx && xx < upMax) {
                    Log.w("TAG", "向右移动");
                    new Thread(new PtzUtils(currentDevice, "right")).start();
                }
            }
        }
    }

    /**
     * 云台控制按键
     */
    @OnTouch({R.id.ptz_control_reset_btn_layout, R.id.ptz_control_top_btn_layout, R.id.ptz_control_below_btn_layout, R.id.ptz_control_left_btn_layout, R.id.ptz_control_right_btn_layout, R.id.ptz_control_top_left_btn_layout, R.id.ptz_control_top_right_btn_layout, R.id.ptz_control_left_below_btn_layout, R.id.ptz_control_right_below_btn_layout, R.id.ptz_control_big_btn_layout, R.id.ptz_control_small_btn_layout})
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                switch (v.getId()) {
                    case R.id.ptz_control_reset_btn_layout:
                        Logutil.d("reset");
                        break;
                    case R.id.ptz_control_top_btn_layout:
                        Logutil.d("top");
                        new Thread(new PtzUtils(currentDevice, "top")).start();
                        break;
                    case R.id.ptz_control_below_btn_layout:
                        Logutil.d("below");
                        new Thread(new PtzUtils(currentDevice, "below")).start();
                        break;
                    case R.id.ptz_control_left_btn_layout:
                        Logutil.d("left");
                        new Thread(new PtzUtils(currentDevice, "left")).start();
                        break;
                    case R.id.ptz_control_right_btn_layout:
                        Logutil.d("right");
                        new Thread(new PtzUtils(currentDevice, "right")).start();
                        break;
                    case R.id.ptz_control_top_left_btn_layout:
                        Logutil.d("top_left");
                        new Thread(new PtzUtils(currentDevice, "top_left")).start();
                        break;
                    case R.id.ptz_control_top_right_btn_layout:
                        Logutil.d("top_right");
                        new Thread(new PtzUtils(currentDevice, "top_right")).start();
                        break;
                    case R.id.ptz_control_left_below_btn_layout:
                        Logutil.d("left_below");
                        new Thread(new PtzUtils(currentDevice, "left_below")).start();
                        break;
                    case R.id.ptz_control_right_below_btn_layout:
                        Logutil.d("right_below");
                        new Thread(new PtzUtils(currentDevice, "right_below")).start();
                        break;
                    case R.id.ptz_control_big_btn_layout:
                        Logutil.d("zoom_b");
                        new Thread(new PtzUtils(currentDevice, "zoom_b")).start();
                        break;
                    case R.id.ptz_control_small_btn_layout:
                        Logutil.d("small");
                        new Thread(new PtzUtils(currentDevice, "zoom_s")).start();
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                Logutil.d("松开");
                new Thread(new PtzUtils(currentDevice, "stop")).start();
                break;
        }
        return false;
    }

    /**
     * 初始化视频数据
     */
    private void initializeVideoData() {
        //先判断本地的视频源数据是否存在(报异常)
        try {
            String videoSourceStr = FileUtil.readFile(AppConfig.SOURCES_VIDEO).toString();
            allVideoList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(videoSourceStr), VideoBean.class);
            initializeVideoGroupData();
        } catch (Exception e) {
            //异常后，注册广播监听videoSource数据是否初始化成功
            registerRefreshVideoDataBroadcast();
        }
    }

    /**
     * 广播（用于接收视频字典缓存成功后，适配本页面数据）
     */
    private void registerRefreshVideoDataBroadcast() {
        mRefreshVideoDataBroadcast = new RefreshVideoDataBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.RESOLVE_VIDEO_DONE_ACTION);
        getActivity().registerReceiver(mRefreshVideoDataBroadcast, intentFilter);
    }

    /**
     * 广播接收视频资源缓存完成后获取视频数据
     */
    class RefreshVideoDataBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                //取出本地缓存的所有的Video数据
                allVideoList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.SOURCES_VIDEO).toString()), VideoBean.class);
            } catch (Exception e) {
                Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "\n取video字典广播异常---->>>" + e.getMessage());
            }
            //初始化数据
            initializeVideoGroupData();
        }
    }

    /**
     * 初始化视频组数据
     */
    private void initializeVideoGroupData() {
        //加载动画
        mLoadingAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.loading);
        //判断网络
        if (!NetworkUtils.isConnected()) {
            Logutil.e("无网络");
            handler.sendEmptyMessage(1);
            return;
        }
        //视频组Url
        String videoGroupUrl = AppConfig.WEB_HOST + SysinfoUtils.getServerIp() + AppConfig._VIDEO_GROUP;
        //子线程请求视频分组
        HttpBasicRequest thread = new HttpBasicRequest(videoGroupUrl, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                //判断是否有数据
                if (TextUtils.isEmpty(result)) {
                    Logutil.e("视频组无数据");
                    handler.sendEmptyMessage(2);
                    return;
                }
                //判断获取的数据是否异常
                if (result.contains("Execption")) {
                    Logutil.e("视频组数据异常");
                    handler.sendEmptyMessage(2);
                    return;
                }
                if (videoGroupList != null && videoGroupList.size() > 0) {
                    videoGroupList.clear();
                }
                //解析videoGroup数据
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    //判断是否有异常
                    if (!jsonObject.isNull("errorCode")) {
                        Logutil.e("请求数据异常--->>" + result);
                        return;
                    }
                    int count = jsonObject.getInt("count");
                    //判断是否有数据
                    if (count > 0) {
                        JSONArray jsonArray = jsonObject.getJSONArray("groups");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonItem = jsonArray.getJSONObject(i);
                            VideoGroupInfoBean videoGroupInfoBean = new VideoGroupInfoBean(jsonItem.getString("id"), jsonItem.getInt("member_count"), jsonItem.getString("name"));
                            videoGroupList.add(videoGroupInfoBean);
                        }
                    }
                    //拿到视频组数据后去适配展示
                    if (videoGroupList != null && videoGroupList.size() > 0) {
                        handler.sendEmptyMessage(4);
                    }
                } catch (Exception e) {
                    Logutil.e("解析videoGroup数据异常！");
                    handler.sendEmptyMessage(3);
                    return;
                }
            }
        });
        new Thread(thread).start();
    }

    /**
     * 初始化播放器
     */
    private void initializePlayer() {
        //实例第一个播放器
        firstPlayer = new NodePlayer(getActivity());
        firstPlayer.setVideoEnable(true);
        firstPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
        firstPlayer.setPlayerView(firstPlayView);
        firstPlayer.setNodePlayerDelegate(this);
        //实例第二个播放器
        secondPlayer = new NodePlayer(getActivity());
        secondPlayer.setVideoEnable(true);
        secondPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
        secondPlayer.setPlayerView(secondPlayView);
        secondPlayer.setNodePlayerDelegate(this);
        //实例第三个播放器
        thirdPlayer = new NodePlayer(getActivity());
        thirdPlayer.setVideoEnable(true);
        thirdPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
        thirdPlayer.setPlayerView(thirdPlayView);
        thirdPlayer.setNodePlayerDelegate(this);
        //实例第四个播放器
        fourthPlayer = new NodePlayer(getActivity());
        fourthPlayer.setVideoEnable(true);
        fourthPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
        fourthPlayer.setPlayerView(fourthPlayView);
        fourthPlayer.setNodePlayerDelegate(this);
        //实例单屏的播放器
        singlePlayer = new NodePlayer(getActivity());
        singlePlayer.setVideoEnable(true);
        singlePlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
        singlePlayer.setPlayerView(singlePlayView);
        singlePlayer.setNodePlayerDelegate(this);
    }

    /**
     * 单屏播放
     */
    private void initSinglePlayer(String url) {

        //判断单屏播放器是否正在播放
        if (singlePlayer != null && singlePlayer.isPlaying()) {
            //停止播放
            singlePlayer.stop();
        }
        if (!TextUtils.isEmpty(url)) {
            singlePlayer.setInputUrl(url);
            singlePlayer.setNodePlayerDelegate(this);
            singlePlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            singlePlayer.setVideoEnable(true);
            singlePlayer.start();
        } else {
            singleLoadTv.setVisibility(View.VISIBLE);
            singleLoadTv.setText("未加载到视频源");
        }
    }

    /**
     * 播放器的点击或双击事件
     */
    private void videoClickEvent() {
        //第一个播放器单击或双击事件
        firstPlayView.setOnTouchListener(new OnMultiTouchListener(new OnMultiTouchListener.MultiClickCallback() {
            @Override
            public void onDoubleClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        multiScreenLayout.setVisibility(View.GONE);
                        singleScreenLayout.setVisibility(View.VISIBLE);
                        firstParentLayout.setVisibility(View.GONE);
                        secondParentLayout.setVisibility(View.GONE);
                        thirdParentLayout.setVisibility(View.GONE);
                        fourthParentLayout.setVisibility(View.GONE);
                        firstPlayView.setVisibility(View.GONE);
                        secondPlayView.setVisibility(View.GONE);
                        thirdPlayView.setVisibility(View.GONE);
                        fourthPlayView.setVisibility(View.GONE);
                        firstPlayer.stop();
                        secondPlayer.stop();
                        thirdPlayer.stop();
                        fourthPlayer.stop();
                        initDoubleClickFourScreenPlay();
                    }
                });
            }
        }, new OnMultiTouchListener.ClickCallback() {
            @Override
            public void onClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        selectFirstWindow();
                        Logutil.i("first--->>>" + currentDevicePosition + "\n" + currentDevice.getName());
                    }
                });

            }
        }));
        //第二个播放器单击或双击事件
        secondPlayView.setOnTouchListener(new OnMultiTouchListener(new OnMultiTouchListener.MultiClickCallback() {
            @Override
            public void onDoubleClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        multiScreenLayout.setVisibility(View.GONE);
                        singleScreenLayout.setVisibility(View.VISIBLE);
                        firstParentLayout.setVisibility(View.GONE);
                        secondParentLayout.setVisibility(View.GONE);
                        thirdParentLayout.setVisibility(View.GONE);
                        fourthParentLayout.setVisibility(View.GONE);
                        firstViewSelected = false;
                        secondViewSelected = true;
                        thirdViewSelected = false;
                        fourthViewSelected = false;
                        firstPlayer.stop();
                        secondPlayer.stop();
                        thirdPlayer.stop();
                        fourthPlayer.stop();

                        initDoubleClickFourScreenPlay();
                    }
                });


            }
        }, new OnMultiTouchListener.ClickCallback() {
            @Override
            public void onClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        selectSecondWindow();
                        Logutil.i("second--->>>" + currentDevicePosition + "\n" + currentDevice.getName());
                    }
                });
            }
        }));
        //第三个播放器单击或双击事件
        thirdPlayView.setOnTouchListener(new OnMultiTouchListener(new OnMultiTouchListener.MultiClickCallback() {
            @Override
            public void onDoubleClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        multiScreenLayout.setVisibility(View.GONE);
                        singleScreenLayout.setVisibility(View.VISIBLE);
                        firstParentLayout.setVisibility(View.GONE);
                        secondParentLayout.setVisibility(View.GONE);
                        thirdParentLayout.setVisibility(View.GONE);
                        fourthParentLayout.setVisibility(View.GONE);
                        firstViewSelected = false;
                        secondViewSelected = false;
                        thirdViewSelected = true;
                        fourthViewSelected = false;
                        firstPlayer.stop();
                        secondPlayer.stop();
                        thirdPlayer.stop();
                        fourthPlayer.stop();
                        initDoubleClickFourScreenPlay();


                    }
                });


            }
        }, new OnMultiTouchListener.ClickCallback() {
            @Override
            public void onClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        selectThirdWindow();
                        Logutil.i("third--->>>" + currentDevicePosition + "\n" + currentDevice.getName());
                    }
                });
            }
        }));
        //第四个播放器单击或双击事件
        fourthPlayView.setOnTouchListener(new OnMultiTouchListener(new OnMultiTouchListener.MultiClickCallback() {
            @Override
            public void onDoubleClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        multiScreenLayout.setVisibility(View.GONE);
                        singleScreenLayout.setVisibility(View.VISIBLE);
                        firstParentLayout.setVisibility(View.GONE);
                        secondParentLayout.setVisibility(View.GONE);
                        thirdParentLayout.setVisibility(View.GONE);
                        fourthParentLayout.setVisibility(View.GONE);
                        firstViewSelected = false;
                        secondViewSelected = false;
                        thirdViewSelected = false;
                        fourthViewSelected = true;
                        firstPlayer.stop();
                        secondPlayer.stop();
                        thirdPlayer.stop();
                        fourthPlayer.stop();

                        initDoubleClickFourScreenPlay();
                    }
                });


            }
        }, new OnMultiTouchListener.ClickCallback() {
            @Override
            public void onClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        selectFourthWindow();
                        Logutil.i("fourth--->>>" + currentDevicePosition + "\n" + currentDevice.getName());


                    }
                });
            }
        }));
        //单屏播放器的双击或单击事件
        singlePlayView.setOnTouchListener(new OnMultiTouchListener(new OnMultiTouchListener.MultiClickCallback() {
            @Override
            public void onDoubleClick() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        multiScreenLayout.setVisibility(View.VISIBLE);
                        singleScreenLayout.setVisibility(View.GONE);
                        firstParentLayout.setVisibility(View.VISIBLE);
                        secondParentLayout.setVisibility(View.VISIBLE);
                        thirdParentLayout.setVisibility(View.VISIBLE);
                        fourthParentLayout.setVisibility(View.VISIBLE);
                        firstPlayView.setVisibility(View.VISIBLE);
                        secondPlayView.setVisibility(View.VISIBLE);
                        thirdPlayView.setVisibility(View.VISIBLE);
                        fourthPlayView.setVisibility(View.VISIBLE);
                        firstPlayer.start();
                        secondPlayer.start();
                        thirdPlayer.start();
                        fourthPlayer.start();
                        singlePlayer.stop();

                        isCurrentFourScreen = true;
                        isCurrentSingleScreen = false;
                        if (singlePlayerIsRounding)
                            singlePlayerIsRounding = false;
                        initFourScreenPlay();


                    }
                });
            }
        }, new OnMultiTouchListener.ClickCallback() {
            @Override
            public void onClick() {
            }
        }));
    }

    /**
     * 双击四分屏播放视频
     */
    private void initDoubleClickFourScreenPlay() {
        isCurrentSingleScreen = true;
        isCurrentFourScreen = false;
        if (currentDevicePosition != -1) {
            if (!TextUtils.isEmpty(videoDataSources.get(currentDevicePosition).getRtsp()))
                initSinglePlayer(videoDataSources.get(currentDevicePosition).getRtsp());
        }
    }

    /**
     * 展示VideoGroup数据分组
     */
    private void disPlayVideoGroupAdater() {
        //显示视频分组
        videoGroupAdapter = new VideoGroupAdapter(getActivity());
        videoGroupListView.setAdapter(videoGroupAdapter);
        videoGroupAdapter.setSeclection(0);
        videoGroupAdapter.notifyDataSetChanged();
        //设置点击事件
        videoGroupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String groupItemId = videoGroupList.get(position).getId();
                Logutil.i("是单击");
                videoGroupAdapter.setSeclection(position);
                videoGroupAdapter.notifyDataSetChanged();
                //点击组时展示组内数据
                loadVideoGroupItemData(groupItemId);
                if (singlePlayerIsRounding)
                    singlePlayerIsRounding = false;
            }
        });
        //默认展示第一个组内的数据
        String groupId = videoGroupList.get(0).getId();
        loadVideoGroupItemData(groupId);
    }

    /**
     * 根据组id展示某个组内的数据
     */
    private void loadVideoGroupItemData(final String id) {
        //判断组Id是否为空
        if (TextUtils.isEmpty(id)) {
            handler.sendEmptyMessage(5);
            return;
        }
        //请求数据地址
        String url = AppConfig.WEB_HOST + SysinfoUtils.getServerIp() + AppConfig._VIDEO_GROUP_ITEM + id;
        //子线程去请求某个组内数据
        HttpBasicRequest httpBasicRequest = new HttpBasicRequest(url, new HttpBasicRequest.GetHttpData() {
            @Override
            public void httpData(String result) {
                //判断是否有数据
                if (TextUtils.isEmpty(result)) {
                    Logutil.e(id + "组无数据");
                    handler.sendEmptyMessage(2);
                    return;
                }
                //判断获取的数据是否异常
                if (result.contains("Execption")) {
                    Logutil.e(id + "数据异常");
                    handler.sendEmptyMessage(2);
                    return;
                }
                //Log
                try {
                    videoGroupItemList.clear();
                    JSONObject jsonObject = new JSONObject(result);
                    int count = jsonObject.getInt("count");
                    if (count > 0) {
                        JSONArray jsonArray = jsonObject.getJSONArray("sources");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonItem = jsonArray.getJSONObject(i);
                            VideoBean videoBean = new VideoBean();
                            videoBean.setChannel(jsonItem.getString("channel"));
                            videoBean.setDevicetype(jsonItem.getString("devicetype"));
                            videoBean.setId(jsonItem.getString("id"));
                            videoBean.setIpaddress(jsonItem.getString("ipaddress"));
                            videoBean.setName(jsonItem.getString("name"));
                            videoBean.setLocation(jsonItem.getString("location"));
                            videoBean.setPassword(jsonItem.getString("password"));
                            videoBean.setPort(jsonItem.getInt("port"));
                            videoBean.setUsername(jsonItem.getString("username"));
                            videoGroupItemList.add(videoBean);
                        }
                    }

                    //清除上个组内的数据
                    if (videoDataSources != null && videoDataSources.size() > 0) {
                        videoDataSources.clear();
                    }
                    //视频源字典匹配数据（比对组Id，以便获取当前视频源对象播放的Rtsp地址）
                    for (int n = 0; n < allVideoList.size(); n++) {
                        for (int k = 0; k < videoGroupItemList.size(); k++) {
                            if (allVideoList.get(n).getId().equals(videoGroupItemList.get(k).getId())) {
                                videoDataSources.add(allVideoList.get(n));
                            }
                        }
                    }
                    //把请求到的每组视频资源放到大的集合中
                    if (map.containsKey(id)) {
                        map.remove(id);
                        map.put(id, videoDataSources);
                    } else {
                        groupIdFlag = id;
                        map.put(id, videoDataSources);
                    }
                    //展示底部的list列表数据
                    handler.sendEmptyMessage(6);
                } catch (Exception e) {
                    Logutil.e("解析video组内数据异常！--->" + e.getMessage());
                    handler.sendEmptyMessage(3);
                    return;
                }
            }
        });
        new Thread(httpBasicRequest).start();
    }

    /**
     * 展示某个组内数据的Adapter
     */
    class GroupItemAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return videoDataSources.size();
        }

        @Override
        public Object getItem(int position) {
            return videoDataSources.get(position);
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
            VideoBean mDevice = videoDataSources.get(position);
            viewHolder.sipItemName.setText(mDevice.getName());
            return convertView;
        }

        class ViewHolder {
            TextView sipItemName;
        }
    }

    /**
     * Video所有的组展示
     */
    class VideoGroupAdapter extends BaseAdapter {
        //选中对象的标识
        private int clickTemp = -1;
        //布局加载器
        private LayoutInflater layoutInflater;

        //构造函数
        public VideoGroupAdapter(Context context) {
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return videoGroupList.size();
        }

        @Override
        public Object getItem(int position) {
            return videoGroupList.get(position);
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

            VideoGroupInfoBean videoGroupInfoBean = videoGroupList.get(position);

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
     * 展示底部的List列表数据
     */
    private void disPlayVideoGroupItemAdapter() {
        //adapter展示数据
        if (mGroupItemAdapter == null) {
            mGroupItemAdapter = new GroupItemAdapter();
            groupItemListView.setAdapter(mGroupItemAdapter);
        }
        mGroupItemAdapter.notifyDataSetChanged();

        //item点击播放
        groupItemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clickListItem(position);
            }
        });

        //默认当前下标为0
        currentDevicePosition = 0;
        //判断列表是否有数据
        if (videoDataSources == null || videoDataSources.isEmpty()) {
            return;
        }
        //默认第一个选中
        currentDevice = videoDataSources.get(currentDevicePosition);

        //分页加载器，加载前四个
        if (pm == null)
            pm = new PageModel(videoDataSources, 4);
        //四分屏当前列表
        if (pm.getList().size() >= 4)
            currentList = pm.getObjects(videoCurrentPage);
        //如果当前状态是单屏
        if (isCurrentSingleScreen) {
            //默认加载第一个视频源图像
            if (currentDevice != null) {
                //获取第一个视频的播放地址
                String rtsp = currentDevice.getRtsp();
                if (!TextUtils.isEmpty(rtsp)) {
                    //标识当前是几分屏
                    isCurrentSingleScreen = true;
                    isCurrentFourScreen = false;
                    //单屏播放
                    initSinglePlayer(rtsp);

                }
            }
        }
        //如果当前状态是四分屏
        if (isCurrentFourScreen) {
            //单屏播放器停止播放
            if (singlePlayer != null && singlePlayer.isPlaying()) {
                singlePlayer.stop();
            }
            //改变当前的状态标识
            isCurrentSingleScreen = false;
            isCurrentFourScreen = true;

            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            if (!firstPlayer.isPlaying() && !secondPlayer.isPlaying() || !thirdPlayer.isPlaying() || !fourthPlayer.isPlaying()) {
                //显示加载进度条
                displayLoadingAndPr();
                //播放
                initFourScreenPlay();
            }
        }
    }

    /**
     * 视频列表的点击事件
     */
    private void clickListItem(int position) {
        //当前视频点击的对象
        VideoBean mDevice = videoDataSources.get(position);
        currentDevicePosition = position;
        currentDevice = videoDataSources.get(position);
        //判断视频对象是否为空
        if (currentDevice == null) {
            return;
        }
        //当前的播放地址
        String rtsp = mDevice.getRtsp();
        //如果是单屏状态下
        if (isCurrentSingleScreen) {
            //重置轮巡标识
            singlePlayerIsRounding = false;

            if (!TextUtils.isEmpty(rtsp)) {
                initSinglePlayer(rtsp);
            }
        }
        //如果是四分屏状态下
        if (isCurrentFourScreen) {
            //判断是否在轮巡
            if (firstViewSelected && isFirstPlayerRounding) {
                isFirstPlayerRounding = false;
            }
            if (secondViewSelected && isSecondPlayerRounding) {
                isSecondPlayerRounding = false;
            }
            if (thirdViewSelected && isThridPlayerRounding) {
                isThridPlayerRounding = false;
            }
            if (fourthViewSelected && isFourthPlayerRounding) {
                isFourthPlayerRounding = false;
            }

            //如果选中的是第一个播放器
            if (firstViewSelected) {
                if (firstPlayer != null && firstPlayer.isPlaying()) {
                    firstPlayer.stop();
                }
                firstPlayer.setInputUrl(rtsp);
                firstPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
                firstPlayer.setVideoEnable(true);
                firstPlayer.start();
            }
            if (secondViewSelected) {
                if (secondPlayer != null && secondPlayer.isPlaying()) {
                    secondPlayer.stop();
                }
                secondPlayer.setInputUrl(rtsp);
                secondPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
                secondPlayer.setVideoEnable(true);
                secondPlayer.start();
            }

            if (thirdViewSelected) {
                if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                    thirdPlayer.stop();
                }
                thirdPlayer.setInputUrl(rtsp);
                thirdPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
                thirdPlayer.setVideoEnable(true);
                thirdPlayer.start();
            }

            if (fourthViewSelected) {
                if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                    fourthPlayer.stop();
                }
                fourthPlayer.setInputUrl(rtsp);
                fourthPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
                fourthPlayer.setVideoEnable(true);
                fourthPlayer.start();
            }
        }
    }

    /**
     * 十六分屏功能
     */
    @OnClick(R.id.sixteen_screen_btn_layout)
    public void test(View view) {
        //Test
        if (currentDevice != null) {
        } else {
            Logutil.d("is null");
        }
    }

    /**
     * 资源控制
     */
    @OnClick(R.id.source_control_layout)
    public void sourceControl(View view) {
        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }
        //设置资源和云台颜色
        sourceTvLayout.setTextColor(0xffffffff);
        ptzTvLayout.setTextColor(0xff6adeff);

        sourceParentLayout.setVisibility(View.VISIBLE);
        ptzParentLayout.setVisibility(View.GONE);
    }

    /**
     * 云台控制
     */
    @OnClick(R.id.ptz_control_layout)
    public void PtzControl(View view) {
        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }
        //设置资源和云台颜色
        sourceTvLayout.setTextColor(0xff6adeff);
        ptzTvLayout.setTextColor(0xffffffff);

        sourceParentLayout.setVisibility(View.GONE);
        ptzParentLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 下一页
     */
    @OnClick(R.id.monitor_video_nextpage_btn_layout)
    public void nextPageVideo(View view) {
        //判断当前是否是单屏播放
        if (isCurrentSingleScreen) {
            //判断当前视频源的下标是否超过当前集合的总长度
            if (currentDevicePosition > videoDataSources.size()) {
                Logutil.e("当前下标超了");
                return;
            }
            //判断是否是当前集合的最后一个
            if (currentDevicePosition == videoDataSources.size() - 1) {
                currentDevicePosition = videoDataSources.size() - 1;
                return;
            }
            if (getActivity() != null) {
                //显示加载进度条
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        singleLoadingImg.setVisibility(View.VISIBLE);
                        singleLoadTv.setVisibility(View.VISIBLE);
                    }
                });
            }
            currentDevicePosition += 1;
            Logutil.d("单屏下一页" + currentDevicePosition);
            currentDevice = videoDataSources.get(currentDevicePosition);
            initSinglePlayer(currentDevice.getRtsp());
        }

        if (isCurrentFourScreen) {
            if (pm != null && pm.isHasNextPage()) {
                videoCurrentPage++;
                Logutil.i("当前页面:" + videoCurrentPage);
                currentList = pm.getObjects(videoCurrentPage);
                initFourScreenPlay();
            } else {
                Logutil.d("没有下一页了");
            }
        }
    }

    /**
     * 上一页
     */
    @OnClick(R.id.monitor_video_preview_btn_layout)
    public void previewPageVideo(View view) {
        //判断当前是单屏
        if (isCurrentSingleScreen) {
            //如果是第一个就赋值0
            if (currentDevicePosition == 0) {
                currentDevicePosition = 0;
                return;
            }
            currentDevicePosition -= 1;

            //显示加载进度条
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        singleLoadingImg.setVisibility(View.VISIBLE);
                        singleLoadTv.setVisibility(View.VISIBLE);
                    }
                });
            }
            Logutil.d("单屏下一页-->>" + currentDevicePosition);
            currentDevice = videoDataSources.get(currentDevicePosition);
            initSinglePlayer(currentDevice.getRtsp());
        }
        //四分屏时
        if (isCurrentFourScreen) {
            if (pm != null && pm.isHasPreviousPage()) {
                videoCurrentPage--;
                Logutil.i("当前页面:" + videoCurrentPage);
                currentList = pm.getObjects(videoCurrentPage);
                initFourScreenPlay();
            } else {
                videoCurrentPage = 0;
            }
        }
    }

    /**
     * 切换四屏
     */
    @OnClick(R.id.four_screen_btn_layout)
    public void customFourScreen(View view) {
        if (isCurrentSingleScreen && !isCurrentFourScreen) {
            isCurrentSingleScreen = false;
            isCurrentFourScreen = true;
            Logutil.i("单屏切换多屏");
            //            //单屏播放器停止播放
            if (singlePlayer != null && singlePlayer.isPlaying()) {
                singlePlayer.stop();
            }
            if (isCurrentPageVisible) {
                //多屏显示，单屏隐藏
                multiScreenLayout.setVisibility(View.VISIBLE);
                singleScreenLayout.setVisibility(View.GONE);
                firstParentLayout.setVisibility(View.VISIBLE);
                secondParentLayout.setVisibility(View.VISIBLE);
                thirdParentLayout.setVisibility(View.VISIBLE);
                fourthParentLayout.setVisibility(View.VISIBLE);
                //播放器view可见
                firstPlayView.setVisibility(View.VISIBLE);
                secondPlayView.setVisibility(View.VISIBLE);
                thirdPlayView.setVisibility(View.VISIBLE);
                fourthPlayView.setVisibility(View.VISIBLE);
                //第一个播放器默认是选中状态
                firstParentLayout.setBackgroundResource(R.drawable.video_select_bg);
                secondParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
                thirdParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
                fourthParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
                initFourScreenPlay();
            }

        }
    }

    /**
     * 切换单屏
     */
    @OnClick(R.id.single_screen_btn_layout)
    public void customSingleScreen(View view) {

        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }

        if (!isCurrentSingleScreen && isCurrentFourScreen) {
            isCurrentSingleScreen = true;
            isCurrentFourScreen = false;
            Logutil.i("多屏切换单屏");
            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            if (secondPlayer != null && secondPlayer.isPlaying()) {
                secondPlayer.stop();
            }
            if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                thirdPlayer.stop();
            }
            if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                fourthPlayer.stop();
            }
            if (isCurrentPageVisible) {
                //多屏显示，单屏隐藏
                multiScreenLayout.setVisibility(View.GONE);
                singleScreenLayout.setVisibility(View.VISIBLE);
                firstParentLayout.setVisibility(View.GONE);
                secondParentLayout.setVisibility(View.GONE);
                thirdParentLayout.setVisibility(View.GONE);
                fourthParentLayout.setVisibility(View.GONE);
                //播放器view可见
                firstPlayView.setVisibility(View.GONE);
                secondPlayView.setVisibility(View.GONE);
                thirdPlayView.setVisibility(View.GONE);
                fourthPlayView.setVisibility(View.GONE);

                initDoubleClickFourScreenPlay();
            }
        }
    }

    /**
     * 停止某个视频的播放
     */
    @OnClick(R.id.stop_video_btn_layout)
    public void stopVideoPlay(View view) {
        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }

        //单屏情况下
        if (isCurrentSingleScreen) {
            if (singlePlayer != null && singlePlayer.isPlaying()) {
                singlePlayer.stop();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        singleLoadTv.setVisibility(View.VISIBLE);
                        singleLoadTv.setText("已停止播放...");
                    }
                });
            }
        }
        //四分屏情况下
        if (isCurrentFourScreen) {
            //第一个选中
            if (firstViewSelected) {
                if (firstPlayer != null && firstPlayer.isPlaying()) {
                    firstPlayer.stop();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            firstLoadTv.setVisibility(View.VISIBLE);
                            firstLoadTv.setText("已停止播放...");
                        }
                    });
                }
            }
            //第二个选中
            if (secondViewSelected) {
                if (secondPlayer != null && secondPlayer.isPlaying()) {
                    secondPlayer.stop();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            secondLoadTv.setVisibility(View.VISIBLE);
                            secondLoadTv.setText("已停止播放...");
                        }
                    });
                }
            }
            //第三个选中
            if (thirdViewSelected) {
                if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                    firstPlayer.stop();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            thirdLoadTv.setVisibility(View.VISIBLE);
                            thirdLoadTv.setText("已停止播放...");
                        }
                    });
                }
            }
            //第四个选中
            if (fourthViewSelected) {
                if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                    fourthPlayer.stop();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fourthLoadTv.setVisibility(View.VISIBLE);
                            fourthLoadTv.setText("已停止播放...");
                        }
                    });
                }
            }
        }
    }

    /**
     * 停止播放所有视频
     */
    @OnClick(R.id.all_stop_video_btn_layout)
    public void stopAllVideoPlay(View view) {
        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }
        //单屏情况下
        if (isCurrentSingleScreen) {
            if (singlePlayer != null && singlePlayer.isPlaying()) {
                singlePlayer.stop();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        singleLoadTv.setVisibility(View.VISIBLE);
                        singleLoadTv.setText("已停止播放...");
                    }
                });
            }
        }
        //四分屏
        if (isCurrentFourScreen) {
            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        firstLoadTv.setVisibility(View.VISIBLE);
                        firstLoadTv.setText("已停止播放...");
                    }
                });
            }
            if (secondPlayer != null && secondPlayer.isPlaying()) {
                secondPlayer.stop();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        secondLoadTv.setVisibility(View.VISIBLE);
                        secondLoadTv.setText("已停止播放...");
                    }
                });
            }
            if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                thirdPlayer.stop();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        thirdLoadTv.setVisibility(View.VISIBLE);
                        thirdLoadTv.setText("已停止播放...");
                    }
                });
            }
            if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                fourthPlayer.stop();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fourthLoadTv.setVisibility(View.VISIBLE);
                        fourthLoadTv.setText("已停止播放...");
                    }
                });
            }
        }

    }

    /**
     * 刷新显示
     */
    @OnClick(R.id.fresh_video_btn_layout)
    public void refrshVideoPlay(View view) {
        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }

        if (isCurrentSingleScreen) {
            if (singlePlayer != null) {
                singlePlayer.start();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        singleLoadTv.setVisibility(View.GONE);
                    }
                });
            }
        }

        if (isCurrentFourScreen) {
            if (firstPlayer != null) {
                firstPlayer.start();
            }
            if (secondPlayer != null) {
                secondPlayer.start();
            }
            if (thirdPlayer != null) {
                thirdPlayer.start();
            }
            if (fourthPlayer != null) {
                fourthPlayer.start();
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    firstLoadTv.setVisibility(View.GONE);
                    secondLoadTv.setVisibility(View.GONE);
                    thirdLoadTv.setVisibility(View.GONE);
                    fourthLoadTv.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * 全屏显示
     */
    @OnClick(R.id.full_screen_btn_layout)
    public void fullVideoPlay(View view) {
        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }
        multiScreenLayout.setVisibility(View.GONE);
        singleScreenLayout.setVisibility(View.VISIBLE);
        firstParentLayout.setVisibility(View.GONE);
        secondParentLayout.setVisibility(View.GONE);
        thirdParentLayout.setVisibility(View.GONE);
        fourthParentLayout.setVisibility(View.GONE);
        firstViewSelected = false;
        secondViewSelected = false;
        thirdViewSelected = false;
        fourthViewSelected = true;
        firstPlayer.stop();
        secondPlayer.stop();
        thirdPlayer.stop();
        fourthPlayer.stop();

        isCurrentSingleScreen = true;
        isCurrentFourScreen = false;

        if (firstViewSelected) {
            currentDevicePosition = (videoCurrentPage - 1) * 4 + 0;
        }
        if (secondViewSelected) {
            currentDevicePosition = (videoCurrentPage - 1) * 4 + 1;
        }
        if (thirdViewSelected) {
            currentDevicePosition = (videoCurrentPage - 1) * 4 + 2;
        }
        if (fourthViewSelected) {
            currentDevicePosition = (videoCurrentPage - 1) * 4 + 3;
        }
        if (currentDevice != null) {
            if (!TextUtils.isEmpty(currentDevice.getRtsp()))
                initSinglePlayer(currentDevice.getRtsp());
        }
    }

    /**
     * 第一个窗口选中
     */
    private void selectFirstWindow() {
        firstParentLayout.setBackgroundResource(R.drawable.video_select_bg);
        secondParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        thirdParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        fourthParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        firstViewSelected = true;
        secondViewSelected = false;
        thirdViewSelected = false;
        fourthViewSelected = false;
        currentDevicePosition = (videoCurrentPage - 1) * 4 + 0;
        //防止下标越界
        if (currentDevicePosition > videoDataSources.size()) {
            currentDevicePosition = 0;
        }
        if (videoDataSources.size() > 0 && currentDevicePosition <= videoDataSources.size() - 1)
            currentDevice = videoDataSources.get(currentDevicePosition);

    }

    /**
     * 第二个窗口选中
     */
    private void selectSecondWindow() {
        firstParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        secondParentLayout.setBackgroundResource(R.drawable.video_select_bg);
        thirdParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        fourthParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        firstViewSelected = false;
        secondViewSelected = true;
        thirdViewSelected = false;
        fourthViewSelected = false;
        currentDevicePosition = (videoCurrentPage - 1) * 4 + 1;
        //防止下标越界
        if (currentDevicePosition > videoDataSources.size()) {
            currentDevicePosition = 0;
        }
        currentDevice = videoDataSources.get(currentDevicePosition);
    }

    /**
     * 第三个窗口选中
     */
    private void selectThirdWindow() {
        firstParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        secondParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        thirdParentLayout.setBackgroundResource(R.drawable.video_select_bg);
        fourthParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        firstViewSelected = false;
        secondViewSelected = false;
        thirdViewSelected = true;
        fourthViewSelected = false;
        currentDevicePosition = (videoCurrentPage - 1) * 4 + 2;
        //防止下标越界
        if (currentDevicePosition > videoDataSources.size() - 1) {
            currentDevicePosition = 0;
        }
        currentDevice = videoDataSources.get(currentDevicePosition);
    }

    /**
     * 第四个窗口选中
     */
    private void selectFourthWindow() {
        firstParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        secondParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        thirdParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
        fourthParentLayout.setBackgroundResource(R.drawable.video_select_bg);
        firstViewSelected = false;
        secondViewSelected = false;
        thirdViewSelected = false;
        fourthViewSelected = true;
        currentDevicePosition = (videoCurrentPage - 1) * 4 + 3;
        //防止下标越界
        if (currentDevicePosition > videoDataSources.size()) {
            currentDevicePosition = 0;
        }
        currentDevice = videoDataSources.get(currentDevicePosition);
    }

    /**
     * 监听分屏模式的广播
     */
    ScreenModeBroadcast mScreenModeBroadcast;

    /**
     * 注册广播监听分屏模式
     */
    private void registerCustomScreenBroadcast() {
        mScreenModeBroadcast = new ScreenModeBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.CUSTOM_SCREEN_MODE);
        getActivity().registerReceiver(mScreenModeBroadcast, intentFilter);
    }

    /**
     * 广播监听键盘关闭报警
     */
    class ScreenModeBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.sendEmptyMessage(27);
        }
    }

    /**
     * 自定义分屏模式
     */
    private void customScreenMode() {
        //判断监控页面是否可见
        if (getActivity() != null && !isCurrentPageVisible) {
            Logutil.e("监控页面不可见");
            return;
        }
        //停止轮巡
        if (firstViewSelected && isFirstPlayerRounding) {
            isFirstPlayerRounding = false;
        }
        if (secondViewSelected && isSecondPlayerRounding) {
            isSecondPlayerRounding = false;
        }
        if (thirdViewSelected && isThridPlayerRounding) {
            isThridPlayerRounding = false;
        }
        if (fourthViewSelected && isFourthPlayerRounding) {
            isFourthPlayerRounding = false;
        }


        if (isCurrentSingleScreen && !isCurrentFourScreen) {
            isCurrentSingleScreen = false;
            isCurrentFourScreen = true;
            Logutil.i("单屏切换多屏");
            //            //单屏播放器停止播放
            if (singlePlayer != null && singlePlayer.isPlaying()) {
                singlePlayer.stop();
            }
            if (isCurrentPageVisible) {
                //多屏显示，单屏隐藏
                multiScreenLayout.setVisibility(View.VISIBLE);
                singleScreenLayout.setVisibility(View.GONE);
                firstParentLayout.setVisibility(View.VISIBLE);
                secondParentLayout.setVisibility(View.VISIBLE);
                thirdParentLayout.setVisibility(View.VISIBLE);
                fourthParentLayout.setVisibility(View.VISIBLE);
                //播放器view可见
                firstPlayView.setVisibility(View.VISIBLE);
                secondPlayView.setVisibility(View.VISIBLE);
                thirdPlayView.setVisibility(View.VISIBLE);
                fourthPlayView.setVisibility(View.VISIBLE);
                //第一个播放器默认是选中状态
                firstParentLayout.setBackgroundResource(R.drawable.video_select_bg);
                secondParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
                thirdParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
                fourthParentLayout.setBackgroundResource(R.drawable.video_normal_bg);
                initFourScreenPlay();
            }
        } else if (!isCurrentSingleScreen && isCurrentFourScreen) {
            isCurrentSingleScreen = true;
            isCurrentFourScreen = false;
            Logutil.i("多屏切换单屏");
            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            if (secondPlayer != null && secondPlayer.isPlaying()) {
                secondPlayer.stop();
            }
            if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                thirdPlayer.stop();
            }
            if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                fourthPlayer.stop();
            }
            if (isCurrentPageVisible) {
                //多屏显示，单屏隐藏
                multiScreenLayout.setVisibility(View.GONE);
                singleScreenLayout.setVisibility(View.VISIBLE);
                firstParentLayout.setVisibility(View.GONE);
                secondParentLayout.setVisibility(View.GONE);
                thirdParentLayout.setVisibility(View.GONE);
                fourthParentLayout.setVisibility(View.GONE);
                //播放器view可见
                firstPlayView.setVisibility(View.GONE);
                secondPlayView.setVisibility(View.GONE);
                thirdPlayView.setVisibility(View.GONE);
                fourthPlayView.setVisibility(View.GONE);

                initDoubleClickFourScreenPlay();
            }
        }


    }

    /**
     * 窗口选中的广播
     */
    WindowSelectedBroadcast mWindowSelectedBroadcast;

    /**
     * 注册广播监听窗口选中事件
     */
    private void registerWindowSelectedBroadcast() {
        mWindowSelectedBroadcast = new WindowSelectedBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.CUSTOM_WINDWN);
        getActivity().registerReceiver(mWindowSelectedBroadcast, intentFilter);
    }

    /**
     * 广播监听窗口选中事件
     */
    class WindowSelectedBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.sendEmptyMessage(28);
        }
    }

    /**
     * 当前四分屏的哪个窗口被选中标识
     */
    int currentWindowSelectedPosition = 1;

    /**
     * 窗口选中事件
     */
    private void customWindownSelected() {
        //判断监控页面是否可见
        if (getActivity() != null && !isCurrentPageVisible) {
            Logutil.e("监控页面不可见");
            return;
        }
        if (isCurrentSingleScreen) {
            Logutil.e("单屏不支持");
            showProgressFail("单屏不支持!");
            return;
        }
        currentWindowSelectedPosition += 1;
        Logutil.d("currentWindowSelectedPosition--->>" + currentWindowSelectedPosition);
        if (currentWindowSelectedPosition <= 4) {
            switch (currentWindowSelectedPosition) {
                case 1:
                    selectFirstWindow();
                    break;
                case 2:
                    selectSecondWindow();
                    break;
                case 3:
                    selectThirdWindow();
                    break;
                case 4:
                    selectFourthWindow();
                    break;
            }
        } else {
            currentWindowSelectedPosition = 1;
            selectFirstWindow();
        }
    }

    /**
     * 单屏轮回时的每个视频源的下标
     */
    int singlePlayerIsRoundSubNum = 0;

    /**
     * 单屏是否正在轮回
     */
    boolean singlePlayerIsRounding = false;

    /**
     * 窗口轮巡
     */
    @OnClick(R.id.video_round_btn_layout)
    public void videoRound(View view) {

        //单屏轮播
        if (isCurrentSingleScreen) {
            singlePlayerIsRounding = true;
            handler.sendEmptyMessage(15);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (singlePlayerIsRounding) {
                        try {
                            Thread.sleep(10 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        handler.sendEmptyMessage(14);
                    }
                }
            }).start();
        }


        if (isCurrentFourScreen) {
            handler.sendEmptyMessage(16);
            if (firstViewSelected) {
                isFirstPlayerRounding = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isFirstPlayerRounding) {
                            try {
                                Thread.sleep(10 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            handler.sendEmptyMessage(20);
                        }
                    }
                }).start();
            }

            if (secondViewSelected) {
                isSecondPlayerRounding = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isSecondPlayerRounding) {
                            try {
                                Thread.sleep(10 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            handler.sendEmptyMessage(21);
                        }
                    }
                }).start();
            }

            if (thirdViewSelected) {
                isThridPlayerRounding = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isThridPlayerRounding) {
                            try {
                                Thread.sleep(10 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            handler.sendEmptyMessage(22);
                        }
                    }
                }).start();
            }

            if (fourthViewSelected) {
                isFourthPlayerRounding = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isFourthPlayerRounding) {
                            try {
                                Thread.sleep(10 * 1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            handler.sendEmptyMessage(23);
                        }
                    }
                }).start();
            }

        }

    }

    /**
     * 第一个窗口轮巡时的初始下标
     */
    int firstPlayerRoundSubNum = 0;

    /**
     * 第一个窗口是否正在轮巡的标识
     */
    boolean isFirstPlayerRounding = false;

    /**
     * 第一个窗口轮巡
     */
    private void startFirstPlayerRound() {

        firstPlayerRoundSubNum += 1;
        Logutil.d("firstNum" + firstPlayerRoundSubNum);
        if (firstPlayer != null && firstPlayer.isPlaying()) {
            firstPlayer.stop();
        }
        if (getActivity() == null) {
            return;
        }
        firstPr.setVisibility(View.VISIBLE);
        firstLoadTv.setVisibility(View.VISIBLE);
        firstLoadTv.setText("正在加载...");

        List<VideoBean> roundList = map.get(groupIdFlag);
        if (roundList == null || roundList.isEmpty()) {
            Logutil.e("当前轮巡组数据为空");
            return;
        }

        if (firstPlayerRoundSubNum == roundList.size()) {
            firstPlayerRoundSubNum = 0;
        }
        if (firstPlayerRoundSubNum <= roundList.size()) {

            Logutil.d("roundList.get(singlePlayerIsRoundSubNum).getRtsp()" + roundList.get(singlePlayerIsRoundSubNum).getRtsp() + "\n" + roundList.get(singlePlayerIsRoundSubNum).getName());
            firstPlayer.setInputUrl(roundList.get(firstPlayerRoundSubNum).getRtsp());
            firstPlayer.setNodePlayerDelegate(this);
            firstPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            firstPlayer.setVideoEnable(true);
            firstPlayer.start();
        }

    }

    /**
     * 第二个窗口轮巡时的初始下标
     */
    int secondPlayerRoundSubNum = 0;

    /**
     * 第二个窗口是否正在轮巡的标识
     */
    boolean isSecondPlayerRounding = false;

    /**
     * 第二个窗口轮巡
     */
    private void startSecondPlayerRound() {

        secondPlayerRoundSubNum += 1;
        if (secondPlayer != null && secondPlayer.isPlaying()) {
            secondPlayer.stop();
        }
        if (getActivity() == null) {
            return;
        }
        secondPr.setVisibility(View.VISIBLE);
        secondLoadTv.setVisibility(View.VISIBLE);
        secondLoadTv.setText("正在加载...");

        List<VideoBean> roundList = map.get(groupIdFlag);
        if (roundList == null || roundList.isEmpty()) {
            Logutil.e("当前轮巡组数据为空");
            return;
        }

        if (secondPlayerRoundSubNum == roundList.size()) {
            secondPlayerRoundSubNum = 0;
        }
        if (secondPlayerRoundSubNum <= roundList.size()) {
            secondPlayer.setInputUrl(roundList.get(secondPlayerRoundSubNum).getRtsp());
            secondPlayer.setNodePlayerDelegate(this);
            secondPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            secondPlayer.setVideoEnable(true);
            secondPlayer.start();
        }

    }

    /**
     * 第三个窗口轮巡时的初始下标
     */
    int thirdPlayerRoundSubNum = 0;

    /**
     * 第三个窗口是否正在轮巡的标识
     */
    boolean isThridPlayerRounding = false;

    /**
     * 第三窗口轮巡
     */
    private void startThirdPlayerRound() {

        thirdPlayerRoundSubNum += 1;
        if (thirdPlayer != null && thirdPlayer.isPlaying()) {
            thirdPlayer.stop();
        }
        if (getActivity() == null) {
            return;
        }
        thirdPr.setVisibility(View.VISIBLE);
        thirdLoadTv.setVisibility(View.VISIBLE);
        thirdLoadTv.setText("正在加载...");

        List<VideoBean> roundList = map.get(groupIdFlag);
        if (roundList == null || roundList.isEmpty()) {
            Logutil.e("当前轮巡组数据为空");
            return;
        }

        if (thirdPlayerRoundSubNum == roundList.size()) {
            thirdPlayerRoundSubNum = 0;
        }
        if (thirdPlayerRoundSubNum <= roundList.size()) {
            thirdPlayer.setInputUrl(roundList.get(thirdPlayerRoundSubNum).getRtsp());
            thirdPlayer.setNodePlayerDelegate(this);
            thirdPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            thirdPlayer.setVideoEnable(true);
            thirdPlayer.start();
        }

    }

    /**
     * 第四个窗口轮巡时的初始下标
     */
    int fourthPlayerRoundSubNum = 0;

    /**
     * 第四个窗口是否正在轮巡的标识
     */
    boolean isFourthPlayerRounding = false;

    /**
     * 第四个窗口轮巡
     */
    private void startFourthPlayerRound() {

        fourthPlayerRoundSubNum += 1;
        if (fourthPlayer != null && fourthPlayer.isPlaying()) {
            fourthPlayer.stop();
        }
        if (getActivity() == null) {
            return;
        }
        fourthPr.setVisibility(View.VISIBLE);
        fourthLoadTv.setVisibility(View.VISIBLE);
        fourthLoadTv.setText("正在加载...");

        List<VideoBean> roundList = map.get(groupIdFlag);
        if (roundList == null || roundList.isEmpty()) {
            Logutil.e("当前轮巡组数据为空");
            return;
        }

        if (fourthPlayerRoundSubNum == roundList.size()) {
            fourthPlayerRoundSubNum = 0;
        }
        if (fourthPlayerRoundSubNum <= roundList.size()) {
            fourthPlayer.setInputUrl(roundList.get(fourthPlayerRoundSubNum).getRtsp());
            fourthPlayer.setNodePlayerDelegate(this);
            fourthPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            fourthPlayer.setVideoEnable(true);
            fourthPlayer.start();
        }

    }

    /**
     * 初始化四屏播放
     */
    private void initFourScreenPlay() {

        //如果当前分页内有4条数据
        if (currentList.size() == 4) {
            //显示加载进度条
            handler.sendEmptyMessage(13);
            //获取第一个rtsp播放地址
            String rtsp1 = "";
            if (!TextUtils.isEmpty(currentList.get(0).getRtsp())) {
                rtsp1 = currentList.get(0).getRtsp();
            } else {
                rtsp1 = "";
            }
            //获取第二个rtsp播放地址
            String rtsp2 = "";
            if (!TextUtils.isEmpty(currentList.get(1).getRtsp())) {
                rtsp2 = currentList.get(1).getRtsp();
            } else {
                rtsp2 = "";
            }
            //获取第三个rtsp播放地址
            String rtsp3 = "";
            if (!TextUtils.isEmpty(currentList.get(2).getRtsp())) {
                rtsp3 = currentList.get(2).getRtsp();
            } else {
                rtsp3 = "";
            }
            //获取第四个rtsp播放地址
            String rtsp4 = "";
            if (!TextUtils.isEmpty(currentList.get(3).getRtsp())) {
                rtsp4 = currentList.get(3).getRtsp();
            } else {
                rtsp4 = "";
            }
            //暂停四个播放器
            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            if (secondPlayer != null && secondPlayer.isPlaying()) {
                secondPlayer.stop();
            }
            if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                thirdPlayer.stop();
            }
            if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                fourthPlayer.stop();
            }
            //第一个播放器播放
            firstPlayer.setInputUrl(rtsp1);
            firstPlayer.setNodePlayerDelegate(this);
            firstPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            firstPlayer.setVideoEnable(true);
            firstPlayer.start();
            //第二个播放器播放
            secondPlayer.setInputUrl(rtsp2);
            secondPlayer.setNodePlayerDelegate(this);
            secondPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            secondPlayer.setVideoEnable(true);
            secondPlayer.start();
            //第三个播放器播放
            thirdPlayer.setInputUrl(rtsp3);
            thirdPlayer.setNodePlayerDelegate(this);
            thirdPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            thirdPlayer.setVideoEnable(true);
            thirdPlayer.start();
            //第四个播放器播放
            fourthPlayer.setInputUrl(rtsp4);
            fourthPlayer.setNodePlayerDelegate(this);
            fourthPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            fourthPlayer.setVideoEnable(true);
            fourthPlayer.start();
        }
        //如果当前分页内有3条数据
        if (currentList.size() == 3) {
            //显示加载进度条
            handler.sendEmptyMessage(13);
            //获取第一个rtsp播放地址
            String rtsp1 = "";
            if (!TextUtils.isEmpty(currentList.get(0).getRtsp())) {
                rtsp1 = currentList.get(0).getRtsp();
            } else {
                rtsp1 = "";
            }
            //获取第二个rtsp播放地址
            String rtsp2 = "";
            if (!TextUtils.isEmpty(currentList.get(1).getRtsp())) {
                rtsp2 = currentList.get(1).getRtsp();
            } else {
                rtsp2 = "";
            }
            //获取第三个rtsp播放地址
            String rtsp3 = "";
            if (!TextUtils.isEmpty(currentList.get(2).getRtsp())) {
                rtsp3 = currentList.get(2).getRtsp();
            } else {
                rtsp3 = "";
            }
            //暂停四个播放器
            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            if (secondPlayer != null && secondPlayer.isPlaying()) {
                secondPlayer.stop();
            }
            if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                thirdPlayer.stop();
            }
            if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                fourthPlayer.stop();
            }
            //第一个播放器播放
            firstPlayer.setInputUrl(rtsp1);
            firstPlayer.setNodePlayerDelegate(this);
            firstPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            firstPlayer.setVideoEnable(true);
            firstPlayer.start();
            //第二个播放器播放
            secondPlayer.setInputUrl(rtsp2);
            secondPlayer.setNodePlayerDelegate(this);
            secondPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            secondPlayer.setVideoEnable(true);
            secondPlayer.start();
            //第三个播放器播放
            thirdPlayer.setInputUrl(rtsp3);
            thirdPlayer.setNodePlayerDelegate(this);
            thirdPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            thirdPlayer.setVideoEnable(true);
            thirdPlayer.start();
        }
        //如果当前分页内有2条数据
        if (currentList.size() == 2) {
            //显示加载进度条
            handler.sendEmptyMessage(13);
            //获取第一个rtsp播放地址
            String rtsp1 = "";
            if (!TextUtils.isEmpty(currentList.get(0).getRtsp())) {
                rtsp1 = currentList.get(0).getRtsp();
            } else {
                rtsp1 = "";
            }
            //获取第一个rtsp播放地址
            String rtsp2 = "";
            if (!TextUtils.isEmpty(currentList.get(1).getRtsp())) {
                rtsp2 = currentList.get(1).getRtsp();
            } else {
                rtsp2 = "";
            }
            //暂停四个播放器
            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            if (secondPlayer != null && secondPlayer.isPlaying()) {
                secondPlayer.stop();
            }
            if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                thirdPlayer.stop();
            }
            if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                fourthPlayer.stop();
            }
            //第一个播放器播放
            firstPlayer.setInputUrl(rtsp1);
            firstPlayer.setNodePlayerDelegate(this);
            firstPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            firstPlayer.setVideoEnable(true);
            firstPlayer.start();
            //第二个播放器播放
            secondPlayer.setInputUrl(rtsp2);
            secondPlayer.setNodePlayerDelegate(this);
            secondPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            secondPlayer.setVideoEnable(true);
            secondPlayer.start();
        }
        //如果当前分页内有1条数据
        if (currentList.size() == 1) {
            //显示加载进度条
            handler.sendEmptyMessage(13);
            //获取第一个rtsp播放地址
            String rtsp1 = "";
            if (!TextUtils.isEmpty(currentList.get(0).getRtsp())) {
                rtsp1 = currentList.get(0).getRtsp();
            } else {
                rtsp1 = "";
            }
            //暂停四个播放器
            if (firstPlayer != null && firstPlayer.isPlaying()) {
                firstPlayer.stop();
            }
            if (secondPlayer != null && secondPlayer.isPlaying()) {
                secondPlayer.stop();
            }
            if (thirdPlayer != null && thirdPlayer.isPlaying()) {
                thirdPlayer.stop();
            }
            if (fourthPlayer != null && fourthPlayer.isPlaying()) {
                fourthPlayer.stop();
            }
            firstPlayer.setInputUrl(rtsp1);
            firstPlayer.setNodePlayerDelegate(this);
            firstPlayer.setAudioEnable(AppConfig.ISVIDEOSOUNDS);
            firstPlayer.setVideoEnable(true);
            firstPlayer.start();
        }
    }

    /**
     * 显示加载的进度 条和提示
     */
    private void displayLoadingAndPr() {
        if (isCurrentPageVisible) {
            firstPr.setVisibility(View.VISIBLE);
            firstLoadTv.setVisibility(View.VISIBLE);
            firstLoadTv.setText("正在加载...");

            secondPr.setVisibility(View.VISIBLE);
            secondLoadTv.setVisibility(View.VISIBLE);
            secondLoadTv.setText("正在加载...");

            thirdPr.setVisibility(View.VISIBLE);
            thirdLoadTv.setVisibility(View.VISIBLE);
            thirdLoadTv.setText("正在加载...");

            fourthPr.setVisibility(View.VISIBLE);
            fourthLoadTv.setVisibility(View.VISIBLE);
            fourthLoadTv.setText("正在加载...");
        }

    }

    /**
     * 单屏开始轮巡播放
     */
    private void startSinglePlayerRound() {
        singlePlayerIsRoundSubNum += 1;
        if (singlePlayer != null && singlePlayer.isPlaying()) {
            singlePlayer.stop();
        }
        singleLoadingImg.setVisibility(View.VISIBLE);
        singleLoadingImg.startAnimation(mLoadingAnim);
        singleLoadTv.setVisibility(View.VISIBLE);
        singleLoadTv.setText("正在加载...");

        List<VideoBean> roundList = map.get(groupIdFlag);
        if (roundList == null || roundList.isEmpty()) {
            Logutil.e("当前轮巡组数据为空");
            return;
        }

        if (singlePlayerIsRoundSubNum == roundList.size()) {
            singlePlayerIsRoundSubNum = 0;
        }
        if (singlePlayerIsRoundSubNum <= roundList.size()) {
            initSinglePlayer(roundList.get(singlePlayerIsRoundSubNum).getRtsp());
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        isCurrentPageVisible = isVisibleToUser;
        if (isVisibleToUser) {
            //可见时刷新GridView的Adapter
            if (videoGroupAdapter != null) {
                videoGroupAdapter.notifyDataSetChanged();
            }
            //判断当前是否是单屏并播放
            if (isCurrentSingleScreen) {
                singlePlyaerRestartPlay();
            }
            //判断当前是否是四分屏并播放
            if (isCurrentFourScreen) {
                multiPlayerRestartPlay();
            }
        } else {
            //不可见时停止播放
            currentPlayerStopPlay();
        }
        super.setUserVisibleHint(isVisibleToUser);
    }

    /**
     * 停止播放
     */
    private void currentPlayerStopPlay() {

        isFirstPlayerRounding = false;
        isSecondPlayerRounding = false;
        isThridPlayerRounding = false;
        isFourthPlayerRounding = false;
        singlePlayerIsRounding = false;
        if (isCurrentSingleScreen) {
            isCurrentSingleScreen = true;
            isCurrentFourScreen = false;
            if (singlePlayer != null) {
                singlePlayer.stop();
            }
        }
        if (isCurrentFourScreen) {
            isCurrentSingleScreen = false;
            isCurrentFourScreen = true;
            if (firstPlayer != null) {
                firstPlayer.stop();
            }
            if (secondPlayer != null) {
                secondPlayer.stop();
            }
            if (thirdPlayer != null) {
                thirdPlayer.stop();
            }
            if (fourthPlayer != null) {
                fourthPlayer.stop();
            }
        }
    }

    /**
     * 单屏播放器可见时重新播放
     */
    private void singlePlyaerRestartPlay() {
        isCurrentSingleScreen = true;
        isCurrentFourScreen = false;
        if (singlePlayer != null) {
            singlePlayer.setNodePlayerDelegate(new NodePlayerDelegate() {
                @Override
                public void onEventCallback(NodePlayer player, int event, String msg) {
                    Message singlePlayerMess = new Message();
                    singlePlayerMess.arg1 = event;
                    singlePlayerMess.what = 7;
                    handler.sendMessage(singlePlayerMess);
                }
            });
            singlePlayer.start();
        }
    }

    /**
     * 多屏播放器可见时重新播放
     */
    private void multiPlayerRestartPlay() {
        isCurrentSingleScreen = false;
        isCurrentFourScreen = true;
        if (firstPlayer != null) {
            firstPlayer.setNodePlayerDelegate(this);
            firstPlayer.start();
        }
        if (secondPlayer != null) {
            secondPlayer.setNodePlayerDelegate(this);
            secondPlayer.start();
        }
        if (thirdPlayer != null) {
            thirdPlayer.setNodePlayerDelegate(this);
            thirdPlayer.start();
        }
        if (fourthPlayer != null) {
            fourthPlayer.setNodePlayerDelegate(this);
            fourthPlayer.start();
        }
    }

    /**
     * 播放器信息回调
     */
    @Override
    public void onEventCallback(NodePlayer player, final int event, final String msg) {
        //单屏播放器
        if (singlePlayer == player) {
            Message singlePlayerMess = new Message();
            singlePlayerMess.arg1 = event;
            singlePlayerMess.what = 7;
            handler.sendMessage(singlePlayerMess);
        }
        //第一个播放状态回调
        if (firstPlayer == player) {
            Message firstPlayerMess = new Message();
            firstPlayerMess.arg1 = event;
            firstPlayerMess.what = 8;
            handler.sendMessage(firstPlayerMess);
        }
        //第二个播放状态回调
        if (secondPlayer == player) {
            Message secondPlayerMess = new Message();
            secondPlayerMess.arg1 = event;
            secondPlayerMess.what = 9;
            handler.sendMessage(secondPlayerMess);
        }
        //第三个播放状态回调
        if (thirdPlayer == player) {
            Message thirdPlayerMess = new Message();
            thirdPlayerMess.arg1 = event;
            thirdPlayerMess.what = 10;
            handler.sendMessage(thirdPlayerMess);
        }
        //第四个播放状态回调
        if (fourthPlayer == player) {
            Message fourthPlayerMess = new Message();
            fourthPlayerMess.arg1 = event;
            fourthPlayerMess.what = 11;
            handler.sendMessage(fourthPlayerMess);
        }
    }

    @Override
    public void onDestroyView() {

        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);


        //注销广播
        if (mRefreshVideoDataBroadcast != null) {
            getActivity().unregisterReceiver(mRefreshVideoDataBroadcast);
        }
        if (mScreenModeBroadcast != null)
            getActivity().unregisterReceiver(mScreenModeBroadcast);
        if (mWindowSelectedBroadcast != null)
            getActivity().unregisterReceiver(mWindowSelectedBroadcast);
        //移除handler监听
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
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
                    //提示网络异常
                    if (isCurrentPageVisible && getActivity() != null)
                        showProgressFail(UIUtils.getString(R.string.str_no_network));
                    break;
                case 2:
                    //提示未加载 到数据
                    if (isCurrentPageVisible && getActivity() != null)
                        showProgressFail(UIUtils.getString(R.string.str_resource_no_data));
                    break;
                case 3:
                    //提示解析json数据异常
                    if (isCurrentPageVisible && getActivity() != null)
                        showProgressFail("数据异常");
                    break;
                case 4:
                    //展示videoGroup数据
                    disPlayVideoGroupAdater();
                    break;
                case 5:
                    //提示未获取到组ID
                    if (isCurrentPageVisible && getActivity() != null)
                        showProgressFail("未获取到关联数据");
                    break;
                case 6:
                    //显示listView
                    disPlayVideoGroupItemAdapter();
                    break;
                case 7:
                    //单屏播放器的状态回调
                    int singlePlayerStatusEvent = msg.arg1;
                    singlePlayerStatusCallback(singlePlayerStatusEvent);
                    break;
                case 8:
                    //第一个播放器播放状态回调
                    int firstPlayerStatusEvent = msg.arg1;
                    firstPlayerStatusCallback(firstPlayerStatusEvent);
                    break;
                case 9:
                    //第二个播放器播放状态回调
                    int secondPlayerStatusEvent = msg.arg1;
                    secondPlayerStatusCallback(secondPlayerStatusEvent);
                    break;
                case 10:
                    //第三个播放器播放状态回调
                    int thridPlayerStatusEvent = msg.arg1;
                    thirdPlayerStatusCallback(thridPlayerStatusEvent);
                    break;
                case 11:
                    //第四个播放器播放状态回调
                    int fouthPlayerStatusEvent = msg.arg1;
                    fourthPlayerStatusCallback(fouthPlayerStatusEvent);
                    break;
                case 13:
                    //加载进度条和加载信息
                    displayLoadingAndPr();
                    break;
                case 14:
                    //单屏轮播
                    startSinglePlayerRound();
                    break;
                case 15:
                    //单屏开始轮播提示
                    if (isCurrentPageVisible && getActivity() != null) {
                        showProgressSuccess("单屏开始轮巡");
                    }
                    break;
                case 16:
                    //四分屏开始轮播提示
                    promptPlayerPolling();
                    break;
                case 20:
                    //第一个窗口开始轮巡
                    startFirstPlayerRound();
                    break;
                case 21:
                    //第二个窗口开始轮巡
                    startSecondPlayerRound();
                    break;
                case 22:
                    //第三个窗口开始轮巡
                    startThirdPlayerRound();
                    break;
                case 23:
                    //第四个窗口开始轮巡
                    startFourthPlayerRound();
                    break;
                case 24:
                    //处理摇杆控制云台功能
                    String singleInstruct = (String) msg.obj;
                    handlerPTZControl(singleInstruct);
                    break;
                case 25:
                    //提示不支持云台功能
                    if (getActivity() != null && isCurrentPageVisible)
                        showProgressFail("不支持云台功能!");
                    break;
                case 26:
                    //提示云台控制异常
                    if (getActivity() != null && isCurrentPageVisible)
                        showProgressFail("不支持云台功能!");
                    break;
                case 27:
                    //自定义分屏模式
                    customScreenMode();
                    break;
                case 28:
                    customWindownSelected();
                    break;

            }
        }
    };


    /**
     * 显示摇杆指令更新Ui
     */
    Runnable udateOrderRunnableUi = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null && isCurrentPageVisible) {
                displayOrderTvLayout.setText(mSingleInstruct);
                //两秒后消失
                Timer timer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayOrderTvLayout.setText("");
                            }
                        });
                    }
                };
                timer.schedule(timerTask, 1500);
            }
        }
    };

    /**
     * 提示播放器开始轮巡
     */
    private void promptPlayerPolling() {
        Logutil.i(firstViewSelected + "\n" + secondViewSelected + "\n" + thirdViewSelected + "\n" + fourthViewSelected);
        if (firstViewSelected) {
            showProgressSuccess("第一个窗口开始轮播");
        }
        if (secondViewSelected) {
            showProgressSuccess("第二个窗口开始轮播");
        }
        if (thirdViewSelected) {
            showProgressSuccess("第三个窗口开始轮播");
        }
        if (fourthViewSelected) {
            showProgressSuccess("第四个窗口开始轮播");
        }
    }

    /**
     * 单屏播放器播放状态的回调
     */
    private void singlePlayerStatusCallback(final int singlePlayerStatusEvent) {
        //当前页面是否可见
        if (getActivity() == null || !isCurrentPageVisible) {
            return;
        }
        //状态回调
        if (singlePlayerStatusEvent == 1102) {
            //连接成功
            singleLoadingImg.setVisibility(View.GONE);
            singleLoadingImg.clearAnimation();
            singleLoadTv.setVisibility(View.GONE);
        } else {
            singleLoadingImg.setVisibility(View.VISIBLE);
            singleLoadingImg.startAnimation(mLoadingAnim);
            singleLoadTv.setVisibility(View.VISIBLE);
            singleLoadTv.setTextSize(12);
            singleLoadTv.setTextColor(UIUtils.getColor(R.color.red));
            if (singlePlayerStatusEvent == 1000) {
                singleLoadTv.setText("正在连接...");
            } else if (singlePlayerStatusEvent == 1001) {
                singleLoadTv.setText("连接成功...");
            } else if (singlePlayerStatusEvent == 1104) {
                singleLoadTv.setText("切换视频...");
            } else {
                singleLoadTv.setText("重新连接...");
            }
        }
    }

    /**
     * 第一个播放器播放状态的回调
     */
    private void firstPlayerStatusCallback(final int firstPlayerStatusEvent) {
        //当前页面是否可见
        if (getActivity() == null || !isCurrentPageVisible) {
            return;
        }
        //状态回调判断
        if (firstPlayerStatusEvent == 1102) {
            firstPr.setVisibility(View.GONE);
            firstLoadTv.setVisibility(View.GONE);
        } else {
            firstPr.setVisibility(View.VISIBLE);
            firstLoadTv.setVisibility(View.VISIBLE);
            firstLoadTv.setTextSize(12);
            firstLoadTv.setTextColor(UIUtils.getColor(R.color.red));
            if (firstPlayerStatusEvent == 1000) {
                firstLoadTv.setText("正在连接...");
            } else if (firstPlayerStatusEvent == 1001) {
                firstLoadTv.setText("连接成功...");
            } else if (firstPlayerStatusEvent == 1104) {
                firstLoadTv.setText("切换视频...");
            } else {
                firstLoadTv.setText("重新连接...");
            }
        }
    }

    /**
     * 第二个播放器播放状态的回调
     */
    private void secondPlayerStatusCallback(final int secondPlayerStatusEvent) {
        //当前页面是否可见
        if (getActivity() == null || !isCurrentPageVisible) {
            return;
        }
        //状态回调判断
        if (secondPlayerStatusEvent == 1102) {
            secondPr.setVisibility(View.GONE);
            secondLoadTv.setVisibility(View.GONE);
        } else {
            secondPr.setVisibility(View.VISIBLE);
            secondLoadTv.setVisibility(View.VISIBLE);
            secondLoadTv.setTextSize(12);
            secondLoadTv.setTextColor(UIUtils.getColor(R.color.red));
            if (secondPlayerStatusEvent == 1000) {
                secondLoadTv.setText("正在连接...");
            } else if (secondPlayerStatusEvent == 1001) {
                secondLoadTv.setText("连接成功...");
            } else if (secondPlayerStatusEvent == 1104) {
                secondLoadTv.setText("切换视频...");
            } else {
                secondLoadTv.setText("重新连接...");
            }
        }
    }

    /**
     * 第三个播放器播放状态的回调
     */
    private void thirdPlayerStatusCallback(final int thirdPlayerStatusEvent) {
        //当前页面是否可见
        if (getActivity() == null || !isCurrentPageVisible) {
            return;
        }
        //状态回调判断
        if (thirdPlayerStatusEvent == 1102) {
            thirdPr.setVisibility(View.GONE);
            thirdLoadTv.setVisibility(View.GONE);
        } else {
            thirdPr.setVisibility(View.VISIBLE);
            thirdLoadTv.setVisibility(View.VISIBLE);
            thirdLoadTv.setTextSize(12);
            thirdLoadTv.setTextColor(UIUtils.getColor(R.color.red));
            if (thirdPlayerStatusEvent == 1000) {
                thirdLoadTv.setText("正在连接...");
            } else if (thirdPlayerStatusEvent == 1001) {
                thirdLoadTv.setText("连接成功...");
            } else if (thirdPlayerStatusEvent == 1104) {
                thirdLoadTv.setText("切换视频...");
            } else {
                thirdLoadTv.setText("重新连接...");
            }
        }
    }

    /**
     * 第四个播放器播放状态的回调
     */
    private void fourthPlayerStatusCallback(final int fourthPlayerStatusEvent) {
        //当前页面是否可见
        if (getActivity() == null || !isCurrentPageVisible) {
            return;
        }
        //状态回调判断
        if (fourthPlayerStatusEvent == 1102) {
            fourthPr.setVisibility(View.GONE);
            fourthLoadTv.setVisibility(View.GONE);
        } else {
            fourthPr.setVisibility(View.VISIBLE);
            fourthLoadTv.setVisibility(View.VISIBLE);
            fourthLoadTv.setTextSize(12);
            fourthLoadTv.setTextColor(UIUtils.getColor(R.color.red));
            if (fourthPlayerStatusEvent == 1000) {
                fourthLoadTv.setText("正在连接...");
            } else if (fourthPlayerStatusEvent == 1001) {
                fourthLoadTv.setText("连接成功...");
            } else if (fourthPlayerStatusEvent == 1104) {
                fourthLoadTv.setText("切换视频...");
            } else {
                fourthLoadTv.setText("重新连接...");
            }
        }
    }
}
