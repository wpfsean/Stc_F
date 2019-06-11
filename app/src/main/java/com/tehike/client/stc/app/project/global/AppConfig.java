package com.tehike.client.stc.app.project.global;


/**
 * Created by Root on 2018/8/5.
 */

public class AppConfig {

    public AppConfig() {
        throw new UnsupportedOperationException("不能被实例化");
    }

    /**
     * 是否存在报警
     */
    public static boolean IS_HAVING_ALARM = false;

    /**
     * 是否正在通话中标识
     */
    public static boolean IS_CALLING = false;

    /**
     * 键盘是否有小屏标识
     */
    public static boolean KEYBOARD_WITH_SCREEN = true;

    /**
     * 六路警灯常亮指令（开机自检用）
     */
    public static byte[] FIRST_OPEN_1 = {0x5A, 0x4B, 0x59, 0x01, (byte) 0x81, 0x00, (byte) 0xFF};
    public static byte[] SECOND_OPEN_1 = {0x5A, 0x4B, 0x59, 0x01, (byte) 0x91, 0x00, (byte) 0xFF};
    public static byte[] THIRD_OPEN_1 = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xA1, 0x00, (byte) 0xFF};
    public static byte[] FORTH_OPEN_1 = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xB1, 0x00, (byte) 0xFF};
    public static byte[] FIFTH_OPEN_1 = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xC1, 0x00, (byte) 0xFF};
    public static byte[] SIXTH_OPEN_1 = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xD1, 0x00, (byte) 0xFF};

    /**
     * 打开所有灯
     */
    public static byte[] OPEN_ALL_ALARM_LIGHT = {0x5A, 0x4B, 0x59, 0x10, 0x01, 0x11, 0x21, 0x31, 0x41, 0x51, 0x61, 0x71, (byte) 0x81, (byte) 0x91, (byte) 0xA1, (byte) 0xB1, (byte) 0xC1, (byte) 0xD1, (byte) 0xE1, (byte) 0xF1, 0x00, (byte) 0xFF};

    /**
     * 关闭所有灯
     */
    public static byte[] CLOSE_ALL_ALARM_LIGHT = {0x5A, 0x4B, 0x59, 0x10, 0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, (byte) 0x80, (byte) 0x90, (byte) 0xA0, (byte) 0xB0, (byte) 0xC0, (byte) 0xD0, (byte) 0xE0, (byte) 0xF0, 0x00, (byte) 0xFF};

    /**
     * 第一路灯开指令
     */
    public static byte[] FIRST_OPEN = {0x5A, 0x4B, 0x59, 0x01, (byte) 0x82, 0x00, (byte) 0xFF};

    /**
     * 第一路灯关指令
     */
    public static byte[] FIRST_ClOSE = {0x5A, 0x4B, 0x59, 0x01, (byte) 0x80, 0x00, (byte) 0xFF};

    /**
     * 第二路灯开指令
     */
    public static byte[] SECOND_OPEN = {0x5A, 0x4B, 0x59, 0x01, (byte) 0x92, 0x00, (byte) 0xFF};

    /**
     * 第二路灯关指令
     */
    public static byte[] SECOND_ClOSE = {0x5A, 0x4B, 0x59, 0x01, (byte) 0x90, 0x00, (byte) 0xFF};

    /**
     * 第三路灯开指令
     */
    public static byte[] THIRD_OPEN = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xA2, 0x00, (byte) 0xFF};

    /**
     * 第三路灯关指令
     */
    public static byte[] THIRD_ClOSE = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xA0, 0x00, (byte) 0xFF};

    /**
     * 第四路灯开指令
     */
    public static byte[] FORTH_OPEN = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xB2, 0x00, (byte) 0xFF};

    /**
     * 第四路灯关指令
     */
    public static byte[] FORTH_ClOSE = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xB0, 0x00, (byte) 0xFF};

    /**
     * 第五路灯开指令
     */
    public static byte[] FIFTH_OPEN = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xC2, 0x00, (byte) 0xFF};

    /**
     * 第五路灯关指令
     */
    public static byte[] FIFTH_ClOSE = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xC0, 0x00, (byte) 0xFF};

    /**
     * 第六路灯开指令
     */
    public static byte[] SIXTH_OPEN = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xD2, 0x00, (byte) 0xFF};

    /**
     * 第六路灯关指令
     */
    public static byte[] SIXTH_ClOSE = {0x5A, 0x4B, 0x59, 0x01, (byte) 0xD0, 0x00, (byte) 0xFF};

    /**
     * 会议号码
     */
    public static String DUTY_NUMBER = "0000000000";

    /**
     * webApi接口的host
     */
    public static String WEB_HOST = "http://";

    /**
     * webapi获取sysinfo
     */
    public static String _SYSINFO = ":8080/webapi/sysinfo";

    /**
     * 根据设备guid关联视频
     */
    public static String _LINKED_VIDEO = ":8080/webapi/sentryvideos?guid=";

    /**
     * 获取当前中队名称
     */
    public static String _UNITNAME = ":8080/webapi/unitname";

    /**
     * 获取服务器时间的URl
     */
    public static String SERVER_TIME = ":8080/webapi/ntp";

    /**
     * webapi获取sip分组
     */
    public static String _USIPGROUPS = ":8080/webapi/sipgroups";

    /**
     * webapi根据sip组id获取当前组数据
     */
    public static String _USIPGROUPS_GROUP = ":8080/webapi/sips?groupid=";

    /**
     * webapi获取所有的视频组
     */
    public static String _VIDEO_GROUP = ":8080/webapi/videogroups";

    /**
     * webapi根据组Id获取某个组内数据
     */
    public static String _VIDEO_GROUP_ITEM = ":8080/webapi/videos?groupid=";

    /**
     * webapi获取当前sip用户状态
     */
    public static String _SIS_STATUS = ":8080/webapi/sipstatus";

    /**
     * 报警颜色入类型对应表
     */
    public static String _ALARM_COLOR = ":8080/webapi/alertdefines";

    /**
     * 获取webapi上全部的video数据
     */
    public static String _WEBAPI_VIDEO_SOURCE = ":8080/webapi/videos?groupid=0";

    /**
     * 获取webapi上全部的Sip数据
     */
    public static String _WEBAPI_SIP_SOURCE = ":8080/webapi/sips?groupid=0";

    /**
     * 哨位地图
     */
    public static String _LOCATIONS = ":8080/webapi/locations";

    /**
     * 查询 报警时关联视频
     */
    public static String _RELATION_VIDEO = ":8080/webapi/relatingcameras?guid=";

    /**
     * 屏保计时
     */
    public static int SCREEN_SAVE_TIME = 1800;

    /**
     * 更新apk的路径(远程服务器文件夹名)
     */
    public static String UPDATE_APK_PATH = ":8080/Stc/";

    /**
     * 键盘小屏
     */
    public static String UPDATE_APK_PATH_S = ":8080/Stc_S/";

    /**
     * 更新apk的路径
     */
    public static String UPDATE_APK_FILE = "update.xml";

    /**
     * 当前登录的用户名
     */
    public static String _UNAME = "";

    /**
     * 当前登录的密码
     */
    public static String _UPWD = "";

    /**
     * 中心服務器的IP地址
     */
    public static String _USERVER = "";

    /**
     * 悬浮窗口权限是否申请成功
     */
    public static boolean ARGEE_OVERLAY_PERMISSION = false;

    /**
     * dns(第二個默認的Dns)
     */
    public static String DNS = "0.0.0.0";


    /**
     * 弹箱状态值
     * 1关
     * 0开
     */
    public static int AMMO_STATUS = -1;

    /**
     * 经纬度
     */
    public static double LOCATION_LAT = 0;

    public static double LOCATION_LOG = 0;

    /**
     * 本机Cpu信息
     */
    public static double DEVICE_CPU = 0;

    /**
     * 本机的Rom信息
     */
    public static double DEVICE_RAM = 0;

    /**
     * 上位机监听端口
     */
    public static int S_PORT = 32321;

    /**
     * 下位机的监听端口
     */
    public static int X_PORT = 32323;

    /**
     * SIp是否注册成功
     */
    public static boolean SIP_STATUS = false;

    /**
     * SD父目录
     */
    public static String SD_DIR = "tehike";

    /**
     * 存放资源的目录
     */
    public static String SOURCES_DIR = "sources";

    /**
     * 视频资源的文件名
     */
    public static String SOURCES_VIDEO = "videoResource.bin";

    /**
     * Sip资源的文件名
     */
    public static String SOURCES_SIP = "sipResource.bin";

    /**
     * Sip资源的文件名
     */
    public static String SYSINFO = "sysinfo.bin";

    /**
     * 报警类型颜色对象
     */
    public static String ALARM_COLOR = "alarmColor.bin";

    /**
     * 是否支持屏保功能
     */
    public static boolean IS_ENABLE_SCREEN_SAVE = false;

    /**
     * 主页面是否可以滑动
     */
    public static boolean IS_CAN_SLIDE = false;

    /**
     * 是否播放声音
     */
    public static boolean ISVIDEOSOUNDS = false;

    /**
     * 每隔15分钟去加载刷新一下数据
     */
    public static int REFRESH_DATA_TIME = 15 * 60 * 1000;

    /**
     * 远程 喊话
     */
    public static int REMOTE_PORT = 18720;

    /**
     * 单向广播时向喊话端发送的端口
     */
    public static int BROCAST_PORT = 8899;

    /**
     * 接收app更新action
     */
    public static String APP_UPDATE_ACTION = "android.intent.action.BOOT_COMPLETED";

    /**
     * 定时刷新网络状态广播的Action
     */
    public static String REFRESH_NETWORK_ACTION = "TimingRefrehshNetworkStatus";

    /**
     * 来电广播Action
     */
    public static String INCOMING_CALL_ACTION = "IncomingCall";

    /**
     * 接收报警的Action
     */
    public static String ALARM_ACTION = "receiveAlarm";

    /**
     * video资源解析完成广播的Action
     */
    public static String RESOLVE_VIDEO_DONE_ACTION = "refreshVideoData";

    /**
     * 接收屏保通知的广播的Action
     */
    public static String SCREEN_SAVER_ACTION = "receiveScreenSaverAction";

    /**
     * 接收屏保取消通知的广播的Action
     */
    public static String CANCEL_SCREEN_SAVER_ACTION = "receiveCancelScreenSaverAction";

    /**
     * 刷新副屏事件列表和已处理的报警列表
     */
    public static String REFRESH_ACTION = "refreshAlarmAction";

    /**
     * 分屏模式action
     */
    public static String CUSTOM_SCREEN_MODE = "customScreenMode";

    /**
     * 窗口切换action
     */
    public static String CUSTOM_WINDWN = "customWindowSelected";

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
    public static int CHECK_TYPE = 1;

    /**
     * 倒计时截图时间预设
     */
    public static int COUNT_DOWN_TIME = 5;

}
