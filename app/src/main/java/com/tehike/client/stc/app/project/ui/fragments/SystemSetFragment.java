package com.tehike.client.stc.app.project.ui.fragments;

import android.app.ZysjSystemManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.entity.AlarmTypeBean;
import com.tehike.client.stc.app.project.entity.SysInfoBean;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.phone.SipManager;
import com.tehike.client.stc.app.project.services.InitSystemSettingService;
import com.tehike.client.stc.app.project.services.ReceiverAlarmService;
import com.tehike.client.stc.app.project.services.RemoteVoiceOperatService;
import com.tehike.client.stc.app.project.services.RequestWebApiDataService;
import com.tehike.client.stc.app.project.services.TerminalUpdateIpService;
import com.tehike.client.stc.app.project.services.TimingAutoUpdateService;
import com.tehike.client.stc.app.project.services.TimingRefreshNetworkStatus;
import com.tehike.client.stc.app.project.services.TimingRequestAlarmTypeService;
import com.tehike.client.stc.app.project.services.TimingSendHbService;
import com.tehike.client.stc.app.project.services.UpdateSystemTimeService;
import com.tehike.client.stc.app.project.ui.BaseFragment;
import com.tehike.client.stc.app.project.update.AppUtils;
import com.tehike.client.stc.app.project.utils.ActivityUtils;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.NetworkUtils;
import com.tehike.client.stc.app.project.utils.ServiceUtil;
import com.tehike.client.stc.app.project.utils.SharedPreferencesUtils;
import com.tehike.client.stc.app.project.utils.SysinfoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

/**
 * 描述：设置页面
 * ===============================
 *
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2019/3/4 9:59
 */

public class SystemSetFragment extends BaseFragment {

    /**
     * 左侧系统信息按键
     */
    @BindView(R.id.system_setting_system_set_btn_layout)
    LinearLayout systemInforBtnLayout;

    /**
     * 左侧时间设置按键
     */
    @BindView(R.id.system_setting_time_set_btn_layout)
    LinearLayout timeSetBtnLayout;

    /**
     * 报警设置按键
     */
    @BindView(R.id.system_setting_alarm_set_btn_layout)
    LinearLayout alarmSetBtnLayout;

    /**
     * 显示设置按键
     */
    @BindView(R.id.system_setting_display_set_btn_layout)
    LinearLayout displaySetBtnLayout;

    /**
     * 声音设置按键
     */
    @BindView(R.id.system_setting_ring_set_btn_layout)
    LinearLayout ringSetBtnLayout;

    /**
     * 高级设置按键
     */
    @BindView(R.id.system_setting_advanced_set_btn_layout)
    LinearLayout advancedSetBtnLayout;

    /**
     * 显示系统信息的父布局
     */
    @BindView(R.id.system_setting_system_infor_parent_layout)
    LinearLayout systetmInforParentLayout;

    /**
     * 显示时间设置的父布局
     */
    @BindView(R.id.system_setting_time_set_parent_layout)
    LinearLayout timeSetParentLayout;

    /**
     * 显示报警设置的父布局
     */
    @BindView(R.id.system_setting_alarm_set_parent_layout)
    LinearLayout alarmSetParentLayout;

    /**
     * 显示显示设置的父布局
     */
    @BindView(R.id.system_setting_display_set_parent_layout)
    LinearLayout displaySetParentLayout;

    /**
     * 显示声音设置的父布局
     */
    @BindView(R.id.system_setting_ring_set_parent_layout)
    LinearLayout ringSetParentLayout;

    /**
     * 显示高级设置的父布局
     */
    @BindView(R.id.system_setting_advanced_set_parent_layout)
    LinearLayout advancedSetParentLayout;

    /**
     * 显示Mac信息
     */
    @BindView(R.id.system_infor_mac_tv_layout)
    TextView macTvLayout;

    /**
     * 显示Dns信息
     */
    @BindView(R.id.system_infor_dns_tv_layout)
    TextView dnsTvLayout;

    /**
     * 显示Ip信息
     */
    @BindView(R.id.system_infor_ip_tv_layout)
    TextView ipTvLayout;

    /**
     * 显示网关信息
     */
    @BindView(R.id.system_infor_gateway_tv_layout)
    TextView gatewayTvLayout;

    /**
     * 显示网关信息
     */
    @BindView(R.id.system_infor_netmask_tv_layout)
    TextView netmaskTvLayout;

    /**
     * 显示网络 类型
     */
    @BindView(R.id.system_infor_nettype_tv_layout)
    TextView netTypeTvLayout;

    /**
     * 显示当前sip号码
     */
    @BindView(R.id.system_infor_number_tv_layout)
    TextView currentSipNumberTvLayout;

    /**
     * 显示当前硬件信息
     */
    @BindView(R.id.system_infor_harware_tv_layout)
    TextView harwareTvLayout;

    @BindView(R.id.system_set_app_version_name_layout)
    TextView versionNameLayout;

    /**
     * 网络状态
     */
    @BindView(R.id.system_network_status_tv_layout)
    TextView networkStatusLayout;

    /**
     * 数据刷新间隔
     */
    @BindView(R.id.system_set_data_refresh_time_edit_layout)
    EditText dataRefreshTimeEditLayout;

    /**
     * 摇杆串口下拉显示控件
     */
    @BindView(R.id.yg_serial_port_spinner_layout)
    Spinner ygSerialPortSpinnerLayout;

    /**
     * 键盘串口下拉显示控件
     */
    @BindView(R.id.keyboard_serial_port_spinner_layout)
    Spinner keyboardSerialPortSpinnerLayout;

    /**
     * 背光亮度拖动条
     */
    @BindView(R.id.system_blacklight_seekbar_layout)
    SeekBar systemBlackLightSeekbar;

    /**
     * 显示背光亮度值
     */
    @BindView(R.id.system_blacklight_valus_tv_layout)
    TextView systemBlackLightValueLayout;

    /**
     * 系统声音拖动条
     */
    @BindView(R.id.music_ring_seekbar_layout)
    SeekBar musicRingSeekbarLayout;

    /**
     * 通话声音拖动条
     */
    @BindView(R.id.calling_ring_seekbar_layout)
    SeekBar callingRingSeekbarLayout;

    /**
     * 调节背光亮度的父布局
     */
    @BindView(R.id.blacklight_parent_layout)
    LinearLayout blacklightParentLayout;

    /**
     * 屏保设置的父布局
     */
    @BindView(R.id.screensaver_parent_layout)
    LinearLayout screensaverParentLayout;

    /**
     * 设置调节亮度的按键
     */
    @BindView(R.id.set_blacklight_parent_btn_layout)
    Button setBlackLightBtnLayout;

    /**
     * 设置调节屏保设置的按键
     */
    @BindView(R.id.set_screensaver_parent_btn_layout)
    Button setScreentSaverBtnLayout;

    /**
     * 选择屏保是否开户的spinner
     */
    @BindView(R.id.whether_enable_screen_save_spinner)
    Spinner whetherEnableScreenSaveSpinnerLayout;

    /**
     * 屏保时间的spinner
     */
    @BindView(R.id.screen_save_time_select_spinner)
    Spinner screenSaveTimeSpinnerLayout;

    /**
     * 设置报警一的Spinner
     */
    @BindView(R.id.alarm_type_spinner1_layout)
    Spinner alarmTypeSpinner1Layout;

    /**
     * 设置报警二的Spinner
     */
    @BindView(R.id.alarm_type_spinner2_layout)
    Spinner alarmTypeSpinner2Layout;

    /**
     * 设置报警三的Spinner
     */
    @BindView(R.id.alarm_type_spinner3_layout)
    Spinner alarmTypeSpinner3Layout;

    /**
     * 设置报警四的Spinner
     */
    @BindView(R.id.alarm_type_spinner4_layout)
    Spinner alarmTypeSpinner4Layout;

    /**
     * 设置报警五的Spinner
     */
    @BindView(R.id.alarm_type_spinner5_layout)
    Spinner alarmTypeSpinner5Layout;

    /**
     * 设置报警六的Spinner
     */
    @BindView(R.id.alarm_type_spinner6_layout)
    Spinner alarmTypeSpinner6Layout;

    /**
     * 报警类型重置
     */
    @BindView(R.id.alarm_type_reset_checkbox_layout)
    CheckBox alarmTypeResetBtn;

    /**
     * 报警时鸣枪
     */
    @BindView(R.id.send_alarm_with_shotgun_checkbox_layout)
    CheckBox alarmShotgunBtn;

    /**
     * 报警时供弹
     */
    @BindView(R.id.send_alarm_with_open_ammobox_checkbox_layout)
    CheckBox alarmOpenAmmoBoxBtn;

    /**
     * 是否支持串口屏标识
     */
    @BindView(R.id.is_support_serial_port_screen_layout)
    TextView supportSerialPortFlagLayout;

    /**
     * 串口管理类
     */
    SerialPortManager mSerialPortManager;

    /**
     * 摇杆spinner适配器
     */
    SerialPortAdapter spinnerAdapter;

    /**
     * 声音管理类
     */
    AudioManager audioManager;

    /**
     * 是否开启屏保的适配器
     */
    ArrayAdapter<String> whetherEnableScreenSaveSpinnerAdaper;

    /**
     * 屏保时间选择的适器
     */
    ArrayAdapter<String> screenSaveTimeAdapter;

    /**
     * 刷新网络状态广播
     */
    NetworkStatusBroadcast mFreshNetworkStatusBroadcast;

    /**
     * 屏保功能
     */
    String[] screenSavaFunction = new String[]{"关闭屏保", "开启屏保"};

    /**
     * 屏保时间
     */
    String[] screenSaveTime = new String[]{"30分钟", "1小时", "2小时", "4小时", "8小时", "10小时", "12小时", "24小时"};

    /**
     * 屏保功能下标
     */
    int screenSaveFuntionItemPosition = -1;

    /**
     * 屏保时间选择下标
     */
    int screenSaveTimeItemPosition = -1;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_systemset_layout;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {

        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        registerNetworkChangedBroadcast();

        initSystemInfor();

        disPlayScreenSaver();
    }

    /**
     * 展示屏保功能
     */
    private void disPlayScreenSaver() {

        //是否屏保的spinner选择
        whetherEnableScreenSaveSpinnerAdaper = new ArrayAdapter<String>(getActivity()
                , R.layout.dialog_screen_save_enable_item, R.id.screen_save_item_name_layout,
                screenSavaFunction);
        whetherEnableScreenSaveSpinnerLayout.setAdapter(whetherEnableScreenSaveSpinnerAdaper);

        int p1 = (int) SharedPreferencesUtils.getObject(App.getApplication(), "screenSaveFuntionItemPosition", -1);
        if (p1 == -1) {
            whetherEnableScreenSaveSpinnerLayout.setSelection(0, true);
        } else {
            whetherEnableScreenSaveSpinnerLayout.setSelection(p1, true);
        }
        whetherEnableScreenSaveSpinnerAdaper.notifyDataSetChanged();
        //屏保功能选择监听
        whetherEnableScreenSaveSpinnerLayout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                screenSaveFuntionItemPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //屏保时间的选择
        screenSaveTimeAdapter = new ArrayAdapter<String>(getActivity()
                , R.layout.dialog_screen_save_select_item, R.id.screen_save_item_name_layout,
                screenSaveTime);
        screenSaveTimeSpinnerLayout.setAdapter(screenSaveTimeAdapter);
        int p2 = (int) SharedPreferencesUtils.getObject(App.getApplication(), "screenSaveTimeItemPosition", -1);
        if (p2 == -1) {
            screenSaveTimeSpinnerLayout.setSelection(0, true);
        } else {
            screenSaveTimeSpinnerLayout.setSelection(p2, true);
        }
        screenSaveTimeAdapter.notifyDataSetChanged();
        //屏保时间选择监听
        screenSaveTimeSpinnerLayout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                screenSaveTimeItemPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 显示系统信息
     */
    private void initSystemInfor() {
        //当前用户的sip密码
        String currentSipNumber = "";
        //当前的硬件信息
        String harwareInfor = "";
        //获取当前的mack
        String mac = "";
        //当前的dns
        String dns = "";
        //当前的ip
        String ip = "";
        //当前的gataway
        String gateway = "";
        //子网掩码
        String netmask = "";
        //获取当前的网络类型
        int netWorkType = NetworkUtils.getNetMode(App.getApplication());
        if (netWorkType == 1) {
            netTypeTvLayout.setText("有线");
        } else if (netWorkType == 2) {
            netTypeTvLayout.setText("Wifi");
        } else if (netWorkType == 0) {
            netTypeTvLayout.setText("无网络");
        }
        //众云对象
        ZysjSystemManager mZysjSystemManager = App.mZysjSystemManager;
        if (mZysjSystemManager != null) {
            //获取mac
            if (netWorkType == 1) {
                mac = App.getSystemManager().ZYgetEthMacAddress();
            } else {
                mac = App.getSystemManager().ZYgetWifiMacAddress();
            }
            //获取dns
            if (netWorkType == 1) {
                dns = App.getSystemManager().ZYgetEthDns1();
            } else {
                dns = App.getSystemManager().ZYgetEthDns1();
            }
            //获取Ip
            if (netWorkType == 1) {
                ip = App.getSystemManager().ZYgetEthIp();
            } else {
                ip = App.getSystemManager().ZYgetWifiIp();
            }
            //获取网关
            if (netWorkType == 1) {
                gateway = App.getSystemManager().ZYgetEthGatWay();
            } else {
                gateway = App.getSystemManager().ZYgetWifiGatWay();
            }
            //获取子网掩码
            if (netWorkType == 1) {
                netmask = App.getSystemManager().ZYgetEthNetMask();
            } else {
                netmask = App.getSystemManager().ZYgetWifiNetMask();
            }
        }
        //显示mac
        if (TextUtils.isEmpty(mac)) {
            macTvLayout.setText("未知");
        } else {
            macTvLayout.setText(mac.toUpperCase().replace(":", "-"));
        }
        //显示dns
        if (TextUtils.isEmpty(dns)) {
            dnsTvLayout.setText("未知");
        } else {
            dnsTvLayout.setText(dns);
        }
        //显示ip
        if (TextUtils.isEmpty(ip)) {
            ipTvLayout.setText("未知");
        } else {
            ipTvLayout.setText(ip);
        }
        //显示网关
        if (TextUtils.isEmpty(gateway)) {
            gatewayTvLayout.setText("未知");
        } else {
            gatewayTvLayout.setText(gateway);
        }
        //显示子网掩码
        if (TextUtils.isEmpty(netmask)) {
            netmaskTvLayout.setText("未知");
        } else {
            netmaskTvLayout.setText(netmask);
        }
        //获取当前的sip号码
        SysInfoBean mSysInfoBean = SysinfoUtils.getSysinfo();
        if (mSysInfoBean != null) {
            currentSipNumber = mSysInfoBean.getSipUsername();
        }
        //显示sip号码
        if (TextUtils.isEmpty(currentSipNumber)) {
            currentSipNumberTvLayout.setText("未知");
        } else {
            currentSipNumberTvLayout.setText(currentSipNumber);
        }
        Map<String, String> collectDeviceInfo = AppUtils.collectDeviceInfo(App.getApplication());
        if (collectDeviceInfo != null) {
            harwareInfor = collectDeviceInfo.get("PRODUCT");
        }
        //显示当前硬件信息
        if (TextUtils.isEmpty(harwareInfor)) {
            harwareTvLayout.setText("未知");
        } else {
            harwareTvLayout.setText(harwareInfor);
        }
        //显示当前app版本名称
        String versionName = AppUtils.getVersionName(App.getApplication());
        if (!TextUtils.isEmpty(versionName)) {
            versionNameLayout.setText("Version " + versionName);
        } else {
            versionNameLayout.setText("未知");
        }
        //显示是否支持串口屏
        if (AppConfig.KEYBOARD_WITH_SCREEN)
            supportSerialPortFlagLayout.setText("支持");
        else
            supportSerialPortFlagLayout.setText("不支持");
    }

    /**
     * 初始化摇杆下拉控件信息
     */
    private void initYgSerialPortSpinner() {
        //摇杆串口选择适配器
        spinnerAdapter = new SerialPortAdapter(initSerialPortData());
        // 把定义好的Adapter设定到spinner中
        ygSerialPortSpinnerLayout.setAdapter(spinnerAdapter);
        //取出本地保存的
        String ygSelected = (String) SharedPreferencesUtils.getObject(App.getApplication(), "ygserialport", "");
        if (!TextUtils.isEmpty(ygSelected)) {
            Device d = GsonUtils.GsonToBean(ygSelected, Device.class);
            setSpinnerDefaultValue(ygSerialPortSpinnerLayout, d.toString());
            spinnerAdapter.notifyDataSetChanged();
        }
        ygSerialPortSpinnerLayout.setSelection(2, true);

        //监听
        ygSerialPortSpinnerLayout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Device device = (Device) spinnerAdapter.getItem(pos);
                if (device != null) {
                    String str = GsonUtils.GsonString(device);
                    if (!TextUtils.isEmpty(str)) {
                        SharedPreferencesUtils.putObject(App.getApplication(), "ygserialport", str);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * 初始化键盘下拉控件信息
     */
    private void initKeyBoardSerialPortSpinner() {

        //摇杆串口选择适配器
        spinnerAdapter = new SerialPortAdapter(initSerialPortData());
        // 把定义好的Adapter设定到spinner中
        keyboardSerialPortSpinnerLayout.setAdapter(spinnerAdapter);
        //取出本地保存的
        String keyboardSelected = (String) SharedPreferencesUtils.getObject(App.getApplication(), "keyboardserialport", "");
        if (!TextUtils.isEmpty(keyboardSelected)) {
            Device keyboardDevice = GsonUtils.GsonToBean(keyboardSelected, Device.class);
            setSpinnerDefaultValue(keyboardSerialPortSpinnerLayout, keyboardDevice.toString());
            spinnerAdapter.notifyDataSetChanged();
        }
        keyboardSerialPortSpinnerLayout.setSelection(1, true);
        //监听
        keyboardSerialPortSpinnerLayout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Device device = (Device) spinnerAdapter.getItem(pos);
                if (device != null) {
                    String str = GsonUtils.GsonString(device);
                    if (!TextUtils.isEmpty(str)) {
                        SharedPreferencesUtils.putObject(App.getApplication(), "keyboardserialport", str);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * 初始化串口数据
     */
    private ArrayList<Device> initSerialPortData() {
        mSerialPortManager = new SerialPortManager();
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        //遍历本设备的所有的串口
        ArrayList<Device> devices = serialPortFinder.getDevices();
        if (devices != null && devices.size() > 0) {
            return devices;
        }
        return null;
    }

    /**
     * 设置spinner的默认选项
     */
    private void setSpinnerDefaultValue(Spinner spinner, String value) {
        SpinnerAdapter apsAdapter = spinner.getAdapter();
        int size = apsAdapter.getCount();
        for (int i = 0; i < size; i++) {
            if (TextUtils.equals(value, apsAdapter.getItem(i).toString())) {
                spinner.setSelection(i, true);
                break;
            }
        }
    }

    /**
     * 摇杆串口选择适配器
     */
    public class SerialPortAdapter extends BaseAdapter {
        //串口列表
        ArrayList<Device> list;
        //布局加载器
        private LayoutInflater layoutInflater;

        public SerialPortAdapter(ArrayList<Device> list) {
            layoutInflater = LayoutInflater.from(getActivity());
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            //利用view
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = layoutInflater.inflate(R.layout.serial_port_select_item_layout, null);
                viewHolder.serialPortname = convertView.findViewById(R.id.serial_port_item_name_layout);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //显示串口名
            viewHolder.serialPortname.setText(list.get(position).getName());
            return convertView;
        }

        //内部类
        class ViewHolder {
            //显示串口名称
            TextView serialPortname;
        }
    }

    /**
     * 按键的点击事件
     */
    @OnClick({R.id.set_blacklight_parent_btn_layout, R.id.set_screensaver_parent_btn_layout, R.id.system_setting_system_set_btn_layout, R.id.system_setting_time_set_btn_layout, R.id.system_setting_alarm_set_btn_layout, R.id.system_setting_display_set_btn_layout, R.id.system_setting_ring_set_btn_layout, R.id.system_setting_advanced_set_btn_layout, R.id.system_setting_cancel_btn_layout, R.id.system_setting_save_btn_layout, R.id.system_setting_loginout_btn_layout})
    public void btnClickEvent(View view) {
        switch (view.getId()) {
            case R.id.set_blacklight_parent_btn_layout:

                displayBlackLightSet();
                break;
            case R.id.set_screensaver_parent_btn_layout:
                displayScreenSaverSet();
                break;
            case R.id.system_setting_system_set_btn_layout:
                //系统信息
                disPlaySystemInforView();
                break;
            case R.id.system_setting_time_set_btn_layout:
                //时间设置
                disPlayTimeSetView();
                break;
            case R.id.system_setting_alarm_set_btn_layout:
                //报警设置
                disPlayAlarmSetView();
                break;
            case R.id.system_setting_display_set_btn_layout:
                //显示设置
                disPlayShowSetView();
                break;
            case R.id.system_setting_ring_set_btn_layout:
                //声音设置
                disPlayRingSetView();
                break;
            case R.id.system_setting_advanced_set_btn_layout:
                //高级设置
                disPlayAdvancedSetView();
                break;
            case R.id.system_setting_cancel_btn_layout:
                //取消
                break;
            case R.id.system_setting_save_btn_layout:
                //保存
                saveSetting();
                break;
            case R.id.system_setting_loginout_btn_layout:
                //退出登录
                appLoginOut();
                android.os.Process.killProcess(android.os.Process.myPid());
                break;
        }
    }

    /**
     * 展示屏保设置
     */
    private void displayScreenSaverSet() {
        setBlackLightBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_label_normal);
        setScreentSaverBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_label_selected);
        blacklightParentLayout.setVisibility(View.GONE);
        screensaverParentLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 展示背光设置
     */
    private void displayBlackLightSet() {
        setBlackLightBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_label_selected);
        setScreentSaverBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_label_normal);
        blacklightParentLayout.setVisibility(View.VISIBLE);
        screensaverParentLayout.setVisibility(View.GONE);
    }

    /**
     * 显示系统信息View
     */
    private void disPlaySystemInforView() {
        systemInforBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_selected);
        timeSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        alarmSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        displaySetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        ringSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        advancedSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        systetmInforParentLayout.setVisibility(View.VISIBLE);
        timeSetParentLayout.setVisibility(View.GONE);
        alarmSetParentLayout.setVisibility(View.GONE);
        displaySetParentLayout.setVisibility(View.GONE);
        ringSetParentLayout.setVisibility(View.GONE);
        advancedSetParentLayout.setVisibility(View.GONE);

        initSystemInfor();
    }

    /**
     * 显示时间设置View
     */
    private void disPlayTimeSetView() {
        systemInforBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        timeSetBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_selected);
        alarmSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        displaySetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        ringSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        advancedSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        systetmInforParentLayout.setVisibility(View.GONE);
        timeSetParentLayout.setVisibility(View.VISIBLE);
        alarmSetParentLayout.setVisibility(View.GONE);
        displaySetParentLayout.setVisibility(View.GONE);
        ringSetParentLayout.setVisibility(View.GONE);
        advancedSetParentLayout.setVisibility(View.GONE);
    }

    /**
     * 显示报警设置View
     */
    private void disPlayAlarmSetView() {
        systemInforBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        timeSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        alarmSetBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_selected);
        displaySetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        ringSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        advancedSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        systetmInforParentLayout.setVisibility(View.GONE);
        timeSetParentLayout.setVisibility(View.GONE);
        alarmSetParentLayout.setVisibility(View.VISIBLE);
        displaySetParentLayout.setVisibility(View.GONE);
        ringSetParentLayout.setVisibility(View.GONE);
        advancedSetParentLayout.setVisibility(View.GONE);
        //报警类型定义
        alarmTypeDefine();
    }

    /**
     * 报警类型定义
     */
    private void alarmTypeDefine() {
        //获取所有的警情定义集合
        List<AlarmTypeBean> mAlarmTypeBeanList = GsonUtils.GsonToList(CryptoUtil.decodeBASE64(FileUtil.readFile(AppConfig.ALARM_COLOR).toString()), AlarmTypeBean.class);
        //判断是否有数据
        if (mAlarmTypeBeanList == null || mAlarmTypeBeanList.isEmpty()) {
            return;
        }
        //警情定义集合
        final List<String> allAlarmTypeList = new ArrayList<>();
        //添加数据
        for (AlarmTypeBean alarmTypeBean : mAlarmTypeBeanList) {
            allAlarmTypeList.add(alarmTypeBean.getTypeName());
        }
        //判断警情类型集合是否为空
        if (allAlarmTypeList == null || allAlarmTypeList.isEmpty()) {
            return;
        }

        boolean ck1 = (boolean) SharedPreferencesUtils.getObject(getActivity(), "alarmTypeReset", false);
        if (ck1) {
            alarmTypeResetBtn.setChecked(true);
        } else {
            alarmTypeResetBtn.setChecked(false);
        }
        boolean ck2 = (boolean) SharedPreferencesUtils.getObject(getActivity(), "alarmWithShotgun", false);
        if (ck2) {
            alarmShotgunBtn.setChecked(true);
        } else {
            alarmShotgunBtn.setChecked(false);
        }
        boolean ck3 = (boolean) SharedPreferencesUtils.getObject(getActivity(), "alarmWithBox", false);
        if (ck3) {
            alarmOpenAmmoBoxBtn.setChecked(true);
        } else {
            alarmOpenAmmoBoxBtn.setChecked(false);
        }

        setFirstAlarmType(allAlarmTypeList);
        setSecondAlarmType(allAlarmTypeList);
        setThirdAlarmType(allAlarmTypeList);
        setFourthAlarmType(allAlarmTypeList);
        setFifththAlarmType(allAlarmTypeList);
        setSixAlarmType(allAlarmTypeList);

    }

    /**
     * 设置报警一类型
     */
    private void setFirstAlarmType(final List<String> allAlarmTypeList) {
        //报警一选项适配器
        ArrayAdapter<String> alarmTypeAdapter1 = new ArrayAdapter<String>(getActivity(), R.layout.item_alarm_type_define_layout, allAlarmTypeList);
        alarmTypeSpinner1Layout.setAdapter(alarmTypeAdapter1);
        //本地保存的数据
        String type = (String) SharedPreferencesUtils.getObject(getActivity(), "alarmType1", "");
        if (!TextUtils.isEmpty(type)) {
            setSpinnerDefaultValue(alarmTypeSpinner1Layout, type);
        } else {
            setSpinnerDefaultValue(alarmTypeSpinner1Layout, "脱逃");
        }
        alarmTypeAdapter1.notifyDataSetChanged();
        alarmTypeSpinner1Layout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String alarmType = allAlarmTypeList.get(position);
                Logutil.d("当前报警:" + alarmType);
                SharedPreferencesUtils.putObject(getActivity(), "alarmType1", alarmType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 设置报警二类型
     */
    private void setSecondAlarmType(final List<String> allAlarmTypeList) {
        //报警一选项适配器
        ArrayAdapter<String> alarmTypeAdapter1 = new ArrayAdapter<String>(getActivity(), R.layout.item_alarm_type_define_layout, allAlarmTypeList);
        alarmTypeSpinner2Layout.setAdapter(alarmTypeAdapter1);
        //本地保存的数据
        String type = (String) SharedPreferencesUtils.getObject(getActivity(), "alarmType2", "");
        if (!TextUtils.isEmpty(type)) {
            setSpinnerDefaultValue(alarmTypeSpinner2Layout, type);
        } else {
            setSpinnerDefaultValue(alarmTypeSpinner2Layout, "暴狱");
        }
        alarmTypeAdapter1.notifyDataSetChanged();
        alarmTypeSpinner2Layout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String alarmType = allAlarmTypeList.get(position);
                Logutil.d("当前报警:" + alarmType);
                SharedPreferencesUtils.putObject(getActivity(), "alarmType2", alarmType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 设置报警三类型
     */
    private void setThirdAlarmType(final List<String> allAlarmTypeList) {
        //报警一选项适配器
        ArrayAdapter<String> alarmTypeAdapter1 = new ArrayAdapter<String>(getActivity(), R.layout.item_alarm_type_define_layout, allAlarmTypeList);
        alarmTypeSpinner3Layout.setAdapter(alarmTypeAdapter1);
        //本地保存的数据
        String type = (String) SharedPreferencesUtils.getObject(getActivity(), "alarmType3", "");
        if (!TextUtils.isEmpty(type)) {
            setSpinnerDefaultValue(alarmTypeSpinner3Layout, type);
        } else {
            setSpinnerDefaultValue(alarmTypeSpinner3Layout, "袭击");
        }
        alarmTypeAdapter1.notifyDataSetChanged();
        alarmTypeSpinner3Layout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String alarmType = allAlarmTypeList.get(position);
                Logutil.d("当前报警:" + alarmType);
                SharedPreferencesUtils.putObject(getActivity(), "alarmType3", alarmType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 设置报警四类型
     */
    private void setFourthAlarmType(final List<String> allAlarmTypeList) {
        //报警一选项适配器
        ArrayAdapter<String> alarmTypeAdapter1 = new ArrayAdapter<String>(getActivity(), R.layout.item_alarm_type_define_layout, allAlarmTypeList);
        alarmTypeSpinner4Layout.setAdapter(alarmTypeAdapter1);
        //本地保存的数据
        String type = (String) SharedPreferencesUtils.getObject(getActivity(), "alarmType4", "");
        if (!TextUtils.isEmpty(type)) {
            setSpinnerDefaultValue(alarmTypeSpinner4Layout, type);
        } else {
            setSpinnerDefaultValue(alarmTypeSpinner4Layout, "自然");
        }
        alarmTypeAdapter1.notifyDataSetChanged();
        alarmTypeSpinner4Layout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String alarmType = allAlarmTypeList.get(position);
                Logutil.d("当前报警:" + alarmType);
                SharedPreferencesUtils.putObject(getActivity(), "alarmType4", alarmType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 设置报警五类型
     */
    private void setFifththAlarmType(final List<String> allAlarmTypeList) {
        //报警一选项适配器
        ArrayAdapter<String> alarmTypeAdapter1 = new ArrayAdapter<String>(getActivity(), R.layout.item_alarm_type_define_layout, allAlarmTypeList);
        alarmTypeSpinner5Layout.setAdapter(alarmTypeAdapter1);
        //本地保存的数据
        String type = (String) SharedPreferencesUtils.getObject(getActivity(), "alarmType5", "");
        if (!TextUtils.isEmpty(type)) {
            setSpinnerDefaultValue(alarmTypeSpinner5Layout, type);
        } else {
            setSpinnerDefaultValue(alarmTypeSpinner5Layout, "挟持");
        }
        alarmTypeAdapter1.notifyDataSetChanged();
        alarmTypeSpinner5Layout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String alarmType = allAlarmTypeList.get(position);
                Logutil.d("当前报警:" + alarmType);
                SharedPreferencesUtils.putObject(getActivity(), "alarmType5", alarmType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * 设置报警六类型
     */
    private void setSixAlarmType(final List<String> allAlarmTypeList) {
        //报警一选项适配器
        ArrayAdapter<String> alarmTypeAdapter1 = new ArrayAdapter<String>(getActivity(), R.layout.item_alarm_type_define_layout, allAlarmTypeList);
        alarmTypeSpinner6Layout.setAdapter(alarmTypeAdapter1);
        //本地保存的数据
        String type = (String) SharedPreferencesUtils.getObject(getActivity(), "alarmType6", "");
        if (!TextUtils.isEmpty(type)) {
            setSpinnerDefaultValue(alarmTypeSpinner6Layout, type);
        } else {
            setSpinnerDefaultValue(alarmTypeSpinner6Layout, "突发");
        }
        alarmTypeAdapter1.notifyDataSetChanged();
        alarmTypeSpinner6Layout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String alarmType = allAlarmTypeList.get(position);
                Logutil.d("当前报警:" + alarmType);
                SharedPreferencesUtils.putObject(getActivity(), "alarmType6", alarmType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @OnCheckedChanged({R.id.alarm_type_reset_checkbox_layout, R.id.send_alarm_with_shotgun_checkbox_layout, R.id.send_alarm_with_open_ammobox_checkbox_layout})
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        switch (buttonView.getId()) {
            case R.id.alarm_type_reset_checkbox_layout://是否重置报警类型
                if (isChecked) {
                    //清除数据
                    SharedPreferencesUtils.clear(getActivity(), "alarmType1");
                    SharedPreferencesUtils.clear(getActivity(), "alarmType2");
                    SharedPreferencesUtils.clear(getActivity(), "alarmType3");
                    SharedPreferencesUtils.clear(getActivity(), "alarmType4");
                    SharedPreferencesUtils.clear(getActivity(), "alarmType5");
                    SharedPreferencesUtils.clear(getActivity(), "alarmType6");
                    //标记
                    SharedPreferencesUtils.putObject(getActivity(), "alarmTypeReset", true);
                    //重新显示报警view
                    disPlayAlarmSetView();
                } else {
                    //重置标识
                    SharedPreferencesUtils.putObject(getActivity(), "alarmTypeReset", false);
                }
                break;
            case R.id.send_alarm_with_shotgun_checkbox_layout://报警是否鸣松
                if (isChecked) {
                    SharedPreferencesUtils.putObject(getActivity(), "alarmWithShotgun", true);
                } else {
                    SharedPreferencesUtils.putObject(getActivity(), "alarmWithShotgun", false);
                }
                break;
            case R.id.send_alarm_with_open_ammobox_checkbox_layout://报警是否申请供弹
                if (isChecked) {
                    SharedPreferencesUtils.putObject(getActivity(), "alarmWithBox", true);
                } else {
                    SharedPreferencesUtils.putObject(getActivity(), "alarmWithBox", false);
                }
                break;
        }
    }

    /**
     * 显示显示设置View
     */
    private void disPlayShowSetView() {
        systemInforBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        timeSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        alarmSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        displaySetBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_selected);
        ringSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        advancedSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        systetmInforParentLayout.setVisibility(View.GONE);
        timeSetParentLayout.setVisibility(View.GONE);
        alarmSetParentLayout.setVisibility(View.GONE);
        displaySetParentLayout.setVisibility(View.VISIBLE);
        ringSetParentLayout.setVisibility(View.GONE);
        advancedSetParentLayout.setVisibility(View.GONE);
        setDeviceBlackLight();
    }

    /**
     * 显示声音设置View
     */
    private void disPlayRingSetView() {
        systemInforBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        timeSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        alarmSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        displaySetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        ringSetBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_selected);
        advancedSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        systetmInforParentLayout.setVisibility(View.GONE);
        timeSetParentLayout.setVisibility(View.GONE);
        alarmSetParentLayout.setVisibility(View.GONE);
        displaySetParentLayout.setVisibility(View.GONE);
        ringSetParentLayout.setVisibility(View.VISIBLE);
        advancedSetParentLayout.setVisibility(View.GONE);

        setSystemRing();

        setCallingRing();
    }

    /**
     * 显示高级设置的View
     */
    private void disPlayAdvancedSetView() {
        systemInforBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        timeSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        alarmSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        displaySetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        ringSetBtnLayout.setBackgroundResource(R.drawable.system_set_btn_bg);
        advancedSetBtnLayout.setBackgroundResource(R.mipmap.dtc_btn1_bg_selected);
        systetmInforParentLayout.setVisibility(View.GONE);
        timeSetParentLayout.setVisibility(View.GONE);
        alarmSetParentLayout.setVisibility(View.GONE);
        displaySetParentLayout.setVisibility(View.GONE);
        ringSetParentLayout.setVisibility(View.GONE);
        advancedSetParentLayout.setVisibility(View.VISIBLE);
        //初始代摇杆串口
        initYgSerialPortSpinner();
        //初始化键盘串口
        initKeyBoardSerialPortSpinner();

        //生物验证类型判断
        initCheckType();
    }

    @BindView(R.id.check_type_box1)
    CheckBox checkType1Box;


    @BindView(R.id.check_type_box2)
    CheckBox checkType2Box;


    @BindView(R.id.check_type_box3)
    CheckBox checkType3Box;


    @BindView(R.id.check_type_box4)
    CheckBox checkType4Box;


    @BindView(R.id.check_type_box5)
    CheckBox checkType5Box;


    @BindView(R.id.check_type_box6)
    CheckBox checkType6Box;


    @BindView(R.id.check_type_box7)
    CheckBox checkType7Box;


    /**
     * 生物验证类型
     * 1 无+拍照
     * 2 指纹+拍照
     * 3指静脉+拍照
     * 4人脸+拍照
     * 5指静脉+人脸+拍照
     * 6指纹+人脸+拍照
     * 7指纹+指静脉+拍照
     */


    /**
     * 初始化生物验证类型
     */
    private void initCheckType() {

        String checkType = (String) SharedPreferencesUtils.getObject(App.getApplication(), "checkType", "0");
        if (TextUtils.isEmpty(checkType)) {
            SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "1");
            checkType1Box.setChecked(true);
            checkType2Box.setChecked(false);
            checkType3Box.setChecked(false);
            checkType4Box.setChecked(false);
            checkType5Box.setChecked(false);
            checkType6Box.setChecked(false);
            checkType7Box.setChecked(false);
        } else {
            if (checkType.equals("1")) {
                checkType1Box.setChecked(true);
                checkType2Box.setChecked(false);
                checkType3Box.setChecked(false);
                checkType4Box.setChecked(false);
                checkType5Box.setChecked(false);
                checkType6Box.setChecked(false);
                checkType7Box.setChecked(false);
            } else if (checkType.equals("2")) {
                checkType1Box.setChecked(false);
                checkType2Box.setChecked(true);
                checkType3Box.setChecked(false);
                checkType4Box.setChecked(false);
                checkType5Box.setChecked(false);
                checkType6Box.setChecked(false);
                checkType7Box.setChecked(false);
            } else if (checkType.equals("3")) {
                checkType1Box.setChecked(false);
                checkType2Box.setChecked(false);
                checkType3Box.setChecked(true);
                checkType4Box.setChecked(false);
                checkType5Box.setChecked(false);
                checkType6Box.setChecked(false);
                checkType7Box.setChecked(false);
            } else if (checkType.equals("4")) {
                checkType1Box.setChecked(false);
                checkType2Box.setChecked(false);
                checkType3Box.setChecked(false);
                checkType4Box.setChecked(true);
                checkType5Box.setChecked(false);
                checkType6Box.setChecked(false);
                checkType7Box.setChecked(false);
            } else if (checkType.equals("5")) {
                checkType1Box.setChecked(false);
                checkType2Box.setChecked(false);
                checkType3Box.setChecked(false);
                checkType4Box.setChecked(false);
                checkType5Box.setChecked(true);
                checkType6Box.setChecked(false);
                checkType7Box.setChecked(false);
            } else if (checkType.equals("6")) {
                checkType1Box.setChecked(false);
                checkType2Box.setChecked(false);
                checkType3Box.setChecked(false);
                checkType4Box.setChecked(false);
                checkType5Box.setChecked(false);
                checkType6Box.setChecked(true);
                checkType7Box.setChecked(false);
            } else if (checkType.equals("7")) {
                checkType1Box.setChecked(false);
                checkType2Box.setChecked(false);
                checkType3Box.setChecked(false);
                checkType4Box.setChecked(false);
                checkType5Box.setChecked(false);
                checkType6Box.setChecked(false);
                checkType7Box.setChecked(true);
            }

        }


        checkType1Box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkType2Box.setChecked(false);
                    checkType3Box.setChecked(false);
                    checkType4Box.setChecked(false);
                    checkType5Box.setChecked(false);
                    checkType6Box.setChecked(false);
                    checkType7Box.setChecked(false);
                    SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "1");
                }
            }
        });

        checkType2Box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkType1Box.setChecked(false);
                    checkType3Box.setChecked(false);
                    checkType4Box.setChecked(false);
                    checkType5Box.setChecked(false);
                    checkType6Box.setChecked(false);
                    checkType7Box.setChecked(false);
                    SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "2");
                }
            }
        });
        checkType3Box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkType1Box.setChecked(false);
                    checkType2Box.setChecked(false);
                    checkType4Box.setChecked(false);
                    checkType5Box.setChecked(false);
                    checkType6Box.setChecked(false);
                    checkType7Box.setChecked(false);

                    SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "3");
                }
            }
        });
        checkType4Box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkType1Box.setChecked(false);
                    checkType2Box.setChecked(false);
                    checkType3Box.setChecked(false);
                    checkType5Box.setChecked(false);
                    checkType6Box.setChecked(false);
                    checkType7Box.setChecked(false);
                    SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "4");
                }
            }
        });
        checkType5Box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkType1Box.setChecked(false);
                    checkType2Box.setChecked(false);
                    checkType3Box.setChecked(false);
                    checkType4Box.setChecked(false);
                    SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "5");
                }
            }
        });
        checkType6Box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkType1Box.setChecked(false);
                    checkType2Box.setChecked(false);
                    checkType3Box.setChecked(false);
                    checkType4Box.setChecked(false);
                    checkType5Box.setChecked(false);
                    checkType7Box.setChecked(false);
                    SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "5");
                }
            }
        });
        checkType7Box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkType1Box.setChecked(false);
                    checkType2Box.setChecked(false);
                    checkType3Box.setChecked(false);
                    checkType4Box.setChecked(false);
                    checkType6Box.setChecked(false);
                    checkType5Box.setChecked(false);
                    SharedPreferencesUtils.putObject(App.getApplication(), "checkType", "5");
                }
            }
        });
    }

    /**
     * 退出登录
     */
    private void appLoginOut() {

        //关闭被动接收声音的服务
        if (ServiceUtil.isServiceRunning(RemoteVoiceOperatService.class)) {
            ServiceUtil.stopService(RemoteVoiceOperatService.class);
        }
        //关闭接收报警的服务
        if (ServiceUtil.isServiceRunning(ReceiverAlarmService.class)) {
            ServiceUtil.stopService(ReceiverAlarmService.class);
        }
        //关闭被动修改Ip的服务
        if (ServiceUtil.isServiceRunning(TerminalUpdateIpService.class)) {
            ServiceUtil.stopService(TerminalUpdateIpService.class);
        }
        //关闭自动更新的服务
        if (ServiceUtil.isServiceRunning(TimingAutoUpdateService.class)) {
            ServiceUtil.stopService(TimingAutoUpdateService.class);
        }
        //关闭定时刷新网络状态的服务
        if (ServiceUtil.isServiceRunning(TimingRefreshNetworkStatus.class)) {
            ServiceUtil.stopService(TimingRefreshNetworkStatus.class);
        }
        //关闭定时请求报警类型和警灯颜色对应表的服务
        if (ServiceUtil.isServiceRunning(TimingRequestAlarmTypeService.class)) {
            ServiceUtil.stopService(TimingRequestAlarmTypeService.class);
        }
        //关闭定时刷新数据的服务
        if (ServiceUtil.isServiceRunning(RequestWebApiDataService.class)) {
            ServiceUtil.stopService(RequestWebApiDataService.class);
        }
        //关闭校时服务
        if (ServiceUtil.isServiceRunning(InitSystemSettingService.class)) {
            ServiceUtil.stopService(InitSystemSettingService.class);
        }
        if (ServiceUtil.isServiceRunning(TimingSendHbService.class)) {
            ServiceUtil.stopService(TimingSendHbService.class);
        }
        if (ServiceUtil.isServiceRunning(UpdateSystemTimeService.class)) {
            ServiceUtil.stopService(UpdateSystemTimeService.class);
        }
        //清除所有的activity
        ActivityUtils.removeAllActivity();
        //清除本界面
        if (getActivity() != null)
            getActivity().finish();

        //挂断电话
        if (SipManager.getLc().getCalls().length > 0) {
            SipManager.getLc().terminateAllCalls();
        }

        AppConfig.SIP_STATUS = false;
        //清除sip代理
        SipManager.getLc().clearProxyConfigs();

    }

    /**
     * 保存设置
     */
    private void saveSetting() {

        //点击保存按键隐藏软键盘
//        InputMethodManager mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (mInputMethodManager != null)
//            mInputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);

        //设置屏保事件
        setScreenEvent();

        setDataRefreshTime();

        App.startSpeaking("配置成功");

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //退出登录，重新打开应用
        appLoginOut();
        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("com.tehike.client.stc.app.project");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * 设置屏保功能操作
     */
    private void setScreenEvent() {
        //设置屏保时间
        if (screenSaveTimeItemPosition != -1) {
            String time = screenSaveTime[screenSaveTimeItemPosition];
            SharedPreferencesUtils.putObject(App.getApplication(), "screenSaveTimeItemPosition", screenSaveTimeItemPosition);
            if (time.equals("30分钟")) {
                AppConfig.SCREEN_SAVE_TIME = 30 * 60;
            } else if (time.equals("1小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60;
            } else if (time.equals("2小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60 * 2;
            } else if (time.equals("4小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60 * 4;
            } else if (time.equals("6小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60 * 6;
            } else if (time.equals("8小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60 * 8;
            } else if (time.equals("10小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60 * 10;
            } else if (time.equals("12小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60 * 12;
            } else if (time.equals("24小时")) {
                AppConfig.SCREEN_SAVE_TIME = 60 * 60 * 24;
            }
        }
        //设置屏保是否开启
        if (screenSaveFuntionItemPosition != -1) {
            String function = screenSavaFunction[screenSaveFuntionItemPosition];
            SharedPreferencesUtils.putObject(App.getApplication(), "screenSaveFuntionItemPosition", screenSaveFuntionItemPosition);
            if (function.equals("关闭屏保")) {
                AppConfig.IS_ENABLE_SCREEN_SAVE = false;
            } else if (function.equals("开启屏保")) {
                AppConfig.IS_ENABLE_SCREEN_SAVE = true;
            }
            Logutil.d("function" + function);
        }
    }

    /**
     * 设置数据刷新间隔
     */
    private void setDataRefreshTime() {
        //获取屏保时间
        String dataRefreshTime = dataRefreshTimeEditLayout.getText().toString();
        //设置屏保
        if (!TextUtils.isEmpty(dataRefreshTime)) {
            int time = Integer.parseInt(dataRefreshTime);
            if (time > 0) {
                AppConfig.REFRESH_DATA_TIME = time * 1000;
            }
        }
    }

    /**
     * 设置设备背光亮度
     */
    private void setDeviceBlackLight() {

        //当前背光亮度
        int currentBrightness = App.getSystemManager().ZYgetBackLight();
        //设置该度
        systemBlackLightSeekbar.setProgress(currentBrightness);
        //提示
        systemBlackLightValueLayout.setText("当前设备亮度:" + currentBrightness);
        //增加监听
        systemBlackLightSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        systemBlackLightValueLayout.setText("当前设备亮度:" + progress);
                    }
                });
                int result = App.getSystemManager().ZYsetBackLight(progress);
                Logutil.d("setDeviceBlackLight--->>" + result);
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
     * 设置系统声音
     */
    private void setSystemRing() {
        //判断音频管理对象是否为空
        if (audioManager == null) {
            return;
        }
        //最大的系统音量
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //当前的系统音量
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //seekbar设置最大的进度条
        musicRingSeekbarLayout.setMax(max);
        //显示当前的进度条
        musicRingSeekbarLayout.setProgress(current);
        //监听
        musicRingSeekbarLayout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                Logutil.d("values:" + progress);
                //拖动改变音量大小
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
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
     * 设置通话声音
     */
    private void setCallingRing() {

        //当前最大的通话音量
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        //当前音量
        int current = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        //seekbar设置最大的进度条
        callingRingSeekbarLayout.setMax(max);
        //显示当前的进度 条
        callingRingSeekbarLayout.setProgress(current);
        //拖动监听
        callingRingSeekbarLayout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Logutil.d("progress" + progress);
                //设置声音
                if (progress > 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, progress, 0);
                } else {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callingRingSeekbarLayout.setProgress(1);
                        }
                    });
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
     * 注册刷新网络状态的广播
     */
    private void registerNetworkChangedBroadcast() {
        mFreshNetworkStatusBroadcast = new NetworkStatusBroadcast();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppConfig.REFRESH_NETWORK_ACTION);
        getActivity().registerReceiver(mFreshNetworkStatusBroadcast, intentFilter);

    }

    /**
     * 广播接收网络状态变化（判断网线是否拨出）
     */
    public class NetworkStatusBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isNormal = intent.getBooleanExtra("isNormal", false);
            if (isNormal) {
                handler.sendEmptyMessage(1);
            } else {
                handler.sendEmptyMessage(2);
            }
        }
    }

    @Override
    public void onDestroyView() {
        //注销广播
        if (mFreshNetworkStatusBroadcast != null)
            getActivity().unregisterReceiver(mFreshNetworkStatusBroadcast);
        //移除 handler监听
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    networkStatusLayout.setText("连接正常");
                    networkStatusLayout.setTextColor(getResources().getColor(R.color.white));
                    break;
                case 2:
                    networkStatusLayout.setText("已断开");
                    networkStatusLayout.setTextColor(getResources().getColor(R.color.red));
                    break;
            }
        }
    };
}
