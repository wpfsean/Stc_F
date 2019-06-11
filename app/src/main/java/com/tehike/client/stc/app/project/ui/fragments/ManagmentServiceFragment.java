package com.tehike.client.stc.app.project.ui.fragments;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.plandutyentity.PersionBean;
import com.tehike.client.stc.app.project.plandutyentity.PlanDutyBean;
import com.tehike.client.stc.app.project.ui.BaseFragment;
import com.tehike.client.stc.app.project.ui.views.SpaceItemDecoration;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.NetworkUtils;
import com.tehike.client.stc.app.project.utils.StringUtils;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;

/**
 * 描述：勤务管理页面
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2019/5/20 9:42
 */

public class ManagmentServiceFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    /**
     * 下拉控件
     */
    @BindView(R.id.management_fresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * recyclearview列表
     */
    @BindView(R.id.management_recyclear_layout)
    RecyclerView mRecyclerView;

    /**
     * 展示勤务排班适配器
     */
    PlanDutyAdapter mPlanDutyAdapter;

    /**
     * 当前页面是否可见
     */
    boolean isCurrentPageVisible = false;

    /**
     * 定时任务线程池
     */
    ScheduledExecutorService timingScheduledExecutorService = null;

    /**
     * 所有数据集合
     */
    List<List<PlanDutyBean>> allPlanDutyData = new ArrayList<>();

    /**
     * 数据集合
     */
    List<PlanDutyBean> planDutylistData = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_management_service_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        //下拉控件
        initializeRefreshModule();
        //实例定时线程池任务
        if (timingScheduledExecutorService == null)
            timingScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        //启动定时任务
        timingScheduledExecutorService.scheduleWithFixedDelay(new TimingRequestServerDataThread(), 0L, 15 * 60 * 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 初始化下拉控件
     */
    private void initializeRefreshModule() {
        //设置下拉颜色
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        //设置下拉刷新监听
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    /**
     * 请求勤务排班数据的子线程
     */
    class TimingRequestServerDataThread extends Thread {
        @Override
        public void run() {
            if (!NetworkUtils.isConnected()) {
                Logutil.e("网络异常");
                handler.sendEmptyMessage(4);
                return;
            }
            try {
                String url = "http://19.0.2.100:8082/schedule/getAllSchedule";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);
                connection.setRequestMethod("GET");
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = connection.getInputStream();
                    String result = StringUtils.readTxt(inputStream);
                    inputStream.close();
                    Message message = new Message();
                    message.obj = result;
                    message.what = 1;
                    handler.sendMessage(message);
                } else {
                    handler.sendEmptyMessage(3);
                }
                connection.disconnect();
            } catch (Exception e) {
                handler.sendEmptyMessage(3);
                Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "\nExecption:" + e.getMessage());
            }
        }
    }

    /**
     * 处理勤务排班数据
     */
    private void handlerPlanDutyData(String resultData) {
        if (TextUtils.isEmpty(resultData)) {
            Logutil.e("勤务排班数据为空");
            handler.sendEmptyMessage(3);
            return;
        }
        //解析
        try {
            JSONObject jsonObject = new JSONObject(resultData);
            int code = jsonObject.getInt("code");
            //判断请求数据是否正常数据
            if (code != 0) {
                Logutil.e("请求到的数据异常");
                handler.sendEmptyMessage(3);
                return;
            }
            //清空集合
            if (planDutylistData != null && !planDutylistData.isEmpty())
                planDutylistData.clear();
            if (allPlanDutyData != null && !allPlanDutyData.isEmpty())
                allPlanDutyData.clear();
            //解析
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                PlanDutyBean planDutyBean = new PlanDutyBean();
                planDutyBean.setDay(jsonObject1.getInt("day"));
                planDutyBean.setFrmtime(jsonObject1.getString("frmtime"));
                planDutyBean.setGuardpost(jsonObject1.getString("guardpost"));
                planDutyBean.setId(jsonObject1.getInt("id"));
                planDutyBean.setPersionid(jsonObject1.getString("persionid"));
                planDutyBean.setSccnt(jsonObject1.getInt("sccnt"));
                planDutyBean.setSchdate(jsonObject1.getString("schdate"));
                planDutyBean.setSchrank(jsonObject1.getInt("schrank"));
                planDutyBean.setTotime(jsonObject1.getString("totime"));
                JSONArray jsonArray1 = jsonObject1.getJSONArray("list");
                List<PersionBean> persionList = new ArrayList<>();
                for (int j = 0; j < jsonArray1.length(); j++) {
                    JSONObject jsonObject2 = jsonArray1.getJSONObject(j);
                    PersionBean persionBean = new PersionBean(jsonObject2.getString("persionid"), jsonObject2.getString("persionname"));
                    persionList.add(persionBean);
                }
                planDutyBean.setMlist(persionList);
                planDutylistData.add(planDutyBean);
            }
            handler.sendEmptyMessage(2);
        } catch (Exception e) {
            planDutylistData = null;
            allPlanDutyData = null;
            Logutil.e(Thread.currentThread().getStackTrace()[2].getClassName() + "\n解析勤务排班数据异常:" + e.getMessage());
        }
    }

    /**
     * 展示值勤列表适配器
     */
    class PlanDutyAdapter extends RecyclerView.Adapter<PlanDutyAdapter.RvHolder> {

        @Override
        public RvHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RvHolder(LayoutInflater.from(getActivity()).inflate(R.layout.fragment_management_item_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(RvHolder holder, int position) {
            holder.planDutyNameTv.setText(allPlanDutyData.get(position).get(0).getGuardpost());
            holder.planDutySentryName1.setText(allPlanDutyData.get(position).get(0).getMlist().get(0).getPersionname());
            holder.planDutySentryName2.setText(allPlanDutyData.get(position).get(1).getMlist().get(0).getPersionname());
            holder.planDutySentryName3.setText(allPlanDutyData.get(position).get(2).getMlist().get(0).getPersionname());
            holder.planDutySentryName4.setText(allPlanDutyData.get(position).get(3).getMlist().get(0).getPersionname());
            holder.planDutySentryName5.setText(allPlanDutyData.get(position).get(4).getMlist().get(0).getPersionname());
            holder.planDutySentryName6.setText(allPlanDutyData.get(position).get(5).getMlist().get(0).getPersionname());
            holder.planDutySentryName7.setText(allPlanDutyData.get(position).get(6).getMlist().get(0).getPersionname());
            holder.planDutySentryName8.setText(allPlanDutyData.get(position).get(7).getMlist().get(0).getPersionname());
            holder.planDutySentryName9.setText(allPlanDutyData.get(position).get(8).getMlist().get(0).getPersionname());
            holder.planDutySentryName10.setText(allPlanDutyData.get(position).get(9).getMlist().get(0).getPersionname());
            holder.planDutySentryName11.setText(allPlanDutyData.get(position).get(10).getMlist().get(0).getPersionname());
            holder.planDutySentryName12.setText(allPlanDutyData.get(position).get(11).getMlist().get(0).getPersionname());
        }

        @Override
        public int getItemCount() {
            return allPlanDutyData.size();
        }

        class RvHolder extends RecyclerView.ViewHolder {
            //当前哨位名
            TextView planDutyNameTv;
            TextView planDutySentryName1;
            TextView planDutySentryName2;
            TextView planDutySentryName3;
            TextView planDutySentryName4;
            TextView planDutySentryName5;
            TextView planDutySentryName6;
            TextView planDutySentryName7;
            TextView planDutySentryName8;
            TextView planDutySentryName9;
            TextView planDutySentryName10;
            TextView planDutySentryName11;
            TextView planDutySentryName12;

            public RvHolder(View itemView) {
                super(itemView);
                planDutyNameTv = itemView.findViewById(R.id.item_planduty_id_tv_layout);
                planDutySentryName1 = itemView.findViewById(R.id.item_planduty_name1_tv_layout);
                planDutySentryName2 = itemView.findViewById(R.id.item_planduty_name2_tv_layout);
                planDutySentryName3 = itemView.findViewById(R.id.item_planduty_name3_tv_layout);
                planDutySentryName4 = itemView.findViewById(R.id.item_planduty_name4_tv_layout);
                planDutySentryName5 = itemView.findViewById(R.id.item_planduty_name5_tv_layout);
                planDutySentryName6 = itemView.findViewById(R.id.item_planduty_name6_tv_layout);
                planDutySentryName7 = itemView.findViewById(R.id.item_planduty_name7_tv_layout);
                planDutySentryName8 = itemView.findViewById(R.id.item_planduty_name8_tv_layout);
                planDutySentryName9 = itemView.findViewById(R.id.item_planduty_name9_tv_layout);
                planDutySentryName10 = itemView.findViewById(R.id.item_planduty_name10_tv_layout);
                planDutySentryName11 = itemView.findViewById(R.id.item_planduty_name11_tv_layout);
                planDutySentryName12 = itemView.findViewById(R.id.item_planduty_name12_tv_layout);
            }
        }
    }

    /**
     * 处理并转换数据
     */
    private void hanlerData() {
        int n = 12;
        int listSize = planDutylistData.size();
        for (int i = 0; i < listSize / n; i++) {
            List<PlanDutyBean> newList = new ArrayList<>();
            newList = planDutylistData.subList(i * n, (i + 1) * n);
            allPlanDutyData.add(newList);
        }
    }

    /**
     * 展示
     */
    private void disPlayPlanDutyAdapter() {
        if (mPlanDutyAdapter == null)
            mPlanDutyAdapter = new PlanDutyAdapter();
        if (mRecyclerView != null) {
            mRecyclerView.addItemDecoration(new SpaceItemDecoration(0, 0));
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mRecyclerView.setAdapter(mPlanDutyAdapter);
        }

        EventBus.getDefault().post(planDutylistData);
    }

    @Override
    public void onRefresh() {
        //子线程延时两秒停止转动
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSwipeRefreshLayout != null)
                    mSwipeRefreshLayout.setRefreshing(false);
                //子线程重新去请求
                new Thread(new TimingRequestServerDataThread()).start();
            }
        }, 2 * 1000);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        isCurrentPageVisible = isVisibleToUser;
        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void onDestroy() {
        //移除handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * 处理子线程发送的消息
     */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://处理http请求的内容
                    String resultData = (String) msg.obj;
                    handlerPlanDutyData(resultData);
                    break;
                case 2:
                    //处理数据并用适配器盛现
                    hanlerData();
                    disPlayPlanDutyAdapter();
                    break;
                case 3:
                    //提示数据异常
                    if (getActivity() != null && isCurrentPageVisible)
                        showProgressFail("数据异常");
                    break;
                case 4:
                    //提示网络异常
                    if (getActivity() != null && isCurrentPageVisible)
                        showProgressFail("网络异常");
                    break;
            }
        }
    };


}
