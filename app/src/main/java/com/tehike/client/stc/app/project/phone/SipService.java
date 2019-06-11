package com.tehike.client.stc.app.project.phone;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.db.DbHelper;
import com.tehike.client.stc.app.project.db.DbUtils;
import com.tehike.client.stc.app.project.entity.SipBean;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.utils.ActivityUtils;
import com.tehike.client.stc.app.project.utils.CryptoUtil;
import com.tehike.client.stc.app.project.utils.FileUtil;
import com.tehike.client.stc.app.project.utils.GsonUtils;
import com.tehike.client.stc.app.project.utils.Logutil;
import com.tehike.client.stc.app.project.utils.TimeUtils;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactoryImpl;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SipService extends Service implements LinphoneCoreListener {
    private static final String TAG = "SipService";
    private PendingIntent mKeepAlivePendingIntent;
    private static SipService instance;
    private static PhoneCallback sPhoneCallback;
    private static RegistrationCallback sRegistrationCallback;
    private static MessageCallback sMessageCallback;

    public static boolean isReady() {
        return instance != null;
    }

    /**
     * 当前电话是否接通
     */
    boolean isConnected = false;


    @Override
    public void onCreate() {
        super.onCreate();
        LinphoneCoreFactoryImpl.instance();
        SipManager.createAndStart(SipService.this);
        instance = this;
        Intent intent = new Intent(this, KeepAliveHandler.class);
        mKeepAlivePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ((AlarmManager) this.getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60000, 60000, mKeepAlivePendingIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeAllCallback();
        SipManager.getLc().destroy();
        SipManager.destroy();
        ((AlarmManager) this.getSystemService(Context.ALARM_SERVICE)).cancel(mKeepAlivePendingIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void addPhoneCallback(PhoneCallback phoneCallback) {
        sPhoneCallback = phoneCallback;
    }

    public static void removePhoneCallback() {
        if (sPhoneCallback != null) {
            sPhoneCallback = null;
        }
    }

    public static void addMessageCallback(MessageCallback messageCallback) {
        sMessageCallback = messageCallback;
    }

    public static void removeMessageCallback() {
        if (sMessageCallback != null) {
            sMessageCallback = null;
        }

    }

    public static void addRegistrationCallback(RegistrationCallback registrationCallback) {
        sRegistrationCallback = registrationCallback;
    }

    public static void removeRegistrationCallback() {
        if (sRegistrationCallback != null) {
            sRegistrationCallback = null;
        }
    }

    public void removeAllCallback() {
        removePhoneCallback();
        removeRegistrationCallback();
        removeMessageCallback();
    }

    @Override
    public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig,
                                  LinphoneCore.RegistrationState registrationState, String s) {
        String state = registrationState.toString();
        if (sRegistrationCallback != null && state.equals(LinphoneCore.RegistrationState.RegistrationNone.toString())) {
            sRegistrationCallback.registrationNone();
            AppConfig.SIP_STATUS = false;
        } else if (sRegistrationCallback != null && state.equals(LinphoneCore.RegistrationState.RegistrationProgress.toString())) {
            AppConfig.SIP_STATUS = false;
        } else if (sRegistrationCallback != null && state.equals(LinphoneCore.RegistrationState.RegistrationOk.toString())) {
            sRegistrationCallback.registrationOk();
            AppConfig.SIP_STATUS = true;
        } else if (sRegistrationCallback != null && state.equals(LinphoneCore.RegistrationState.RegistrationCleared.toString())) {
            sRegistrationCallback.registrationCleared();
            AppConfig.SIP_STATUS = false;
        } else if (sRegistrationCallback != null && state.equals(LinphoneCore.RegistrationState.RegistrationFailed.toString())) {
            sRegistrationCallback.registrationFailed();
            AppConfig.SIP_STATUS = false;
        }
    }

    /**
     * 数据字典
     */
    List<SipBean> allSipList;

    /**
     * 来电用户名
     */
    String inComingCallUserName = "";

    @Override
    public void callState(final LinphoneCore linphoneCore, final LinphoneCall linphoneCall, LinphoneCall.State state, String s) {
        if (state == LinphoneCall.State.IncomingReceived && sPhoneCallback != null) {
            //来电号码
            String inComingCallNumber = linphoneCall.getRemoteAddress().getUserName();
            //会议号码默认接
            if (inComingCallNumber.equals(AppConfig.DUTY_NUMBER)) {
                try {
                    SipManager.getLc().acceptCall(linphoneCall);
                } catch (LinphoneCoreException e) {
                    e.printStackTrace();
                }
            } else {
                //清除屏保
                if (ActivityUtils.getTopActivity().getClass().getName().equals("com.tehike.client.stc.app.project.ui.ScreenSaverActivity")) {
                    ActivityUtils.getTopActivity().finish();
                }
                //获取当前的本机缓存的所有的sip信息
                allSipList = App.getSipS();
                if (allSipList != null && !allSipList.isEmpty()) {
                    //遍历得到来电者名称
                    for (SipBean sipBean : allSipList) {
                        if (sipBean != null && !TextUtils.isEmpty(sipBean.getNumber())) {
                            if (sipBean.getNumber().equals(inComingCallNumber)) {
                                inComingCallUserName = sipBean.getName();
                                break;
                            }
                        }
                    }
                    //数据存入数据库
                    ContentValues mContentValues = new ContentValues();
                    mContentValues.put("time", TimeUtils.getCurrentTime1());
                    //判断来电名称是否存在
                    if (!TextUtils.isEmpty(inComingCallUserName)) {
                        mContentValues.put("event", inComingCallUserName + "来电");
                    } else {
                        mContentValues.put("event", inComingCallNumber + "来电");
                    }
                    //存入
                    new DbUtils(App.getApplication()).insert(DbHelper.EVENT_TAB_NAME, mContentValues);
                } else {
                    //来电信息保存到数据库
                    ContentValues contentValues1 = new ContentValues();
                    contentValues1.put("time", TimeUtils.getCurrentTime1());
                    contentValues1.put("event", inComingCallNumber + "来电");
                    new DbUtils(App.getApplication()).insert(DbHelper.EVENT_TAB_NAME, contentValues1);
                }
                Logutil.d("=========inComingCallNumber=========:" + inComingCallNumber);
                //当前通话时，第二个来电不播报
                if (!AppConfig.IS_CALLING) {
                    if (!TextUtils.isEmpty(inComingCallUserName)) {
                        App.startSpeaking(inComingCallUserName + "来电");
                    } else
                        App.startSpeaking(inComingCallNumber + "来电");
                }
            }
            Logutil.d("==================电话回调=====================");
            //回调
            sPhoneCallback.incomingCall(linphoneCall);
            //发送来电广播
            App.getApplication().sendBroadcast(new Intent(AppConfig.INCOMING_CALL_ACTION));
        }
        if (state == LinphoneCall.State.OutgoingInit && sPhoneCallback != null) {
            sPhoneCallback.outgoingInit();
        }

        if (state == LinphoneCall.State.Connected && sPhoneCallback != null) {
            sPhoneCallback.callConnected();
        }

        if (state == LinphoneCall.State.Error && sPhoneCallback != null) {
            sPhoneCallback.error();
        }

        if (state == LinphoneCall.State.CallEnd && sPhoneCallback != null) {
            sPhoneCallback.callEnd();
        }

        if (state == LinphoneCall.State.CallReleased && sPhoneCallback != null) {
            sPhoneCallback.callReleased();
            isConnected = false;
        }
    }

    @Override
    public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {

    }

    @Override
    public void authenticationRequested(LinphoneCore linphoneCore, LinphoneAuthInfo linphoneAuthInfo, LinphoneCore.AuthMethod authMethod) {

    }

    @Override
    public void callStatsUpdated(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCallStats linphoneCallStats) {

    }

    @Override
    public void newSubscriptionRequest(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend, String s) {

    }

    @Override
    public void notifyPresenceReceived(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend) {

    }

    @Override
    public void dtmfReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, int i) {

    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneAddress linphoneAddress, byte[] bytes) {

    }

    @Override
    public void transferState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state) {

    }

    @Override
    public void infoReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneInfoMessage linphoneInfoMessage) {

    }

    @Override
    public void subscriptionStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, SubscriptionState subscriptionState) {

    }

    @Override
    public void publishStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, PublishState publishState) {

    }

    @Override
    public void show(LinphoneCore linphoneCore) {

    }

    @Override
    public void displayStatus(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void displayMessage(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void displayWarning(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {

    }

    @Override
    public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {

    }

    @Override
    public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
        return 0;
    }

    @Override
    public void callEncryptionChanged(LinphoneCore linphoneCore, LinphoneCall linphoneCall, boolean b, String s) {

    }

    @Override
    public void isComposingReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom) {

    }

    @Override
    public void ecCalibrationStatus(LinphoneCore linphoneCore, LinphoneCore.EcCalibratorStatus ecCalibratorStatus, int i, Object o) {

    }

    @Override
    public void globalState(LinphoneCore linphoneCore, LinphoneCore.GlobalState globalState, String s) {

    }

    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {

    }

    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {

    }

    @Override
    public void friendListCreated(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {

    }

    @Override
    public void friendListRemoved(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {

    }

    @Override
    public void networkReachableChanged(LinphoneCore linphoneCore, boolean b) {

    }

    @Override
    public void messageReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {

        //接收短消息 的回调
        if (sMessageCallback != null) {
            sMessageCallback.receiverMessage(linphoneChatMessage);
        }

        initSoundPrompt();
        //消息来源
        String from = linphoneChatMessage.getFrom().getUserName();
        //短消息
        String mess = linphoneChatMessage.getText();
        //提示短消息
        disPlayShortMess(from, mess);
    }


    @Override
    public void messageReceivedUnableToDecrypted(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {

    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, String s, LinphoneContent linphoneContent) {

    }

    @Override
    public void configuringStatus(LinphoneCore linphoneCore, LinphoneCore.RemoteProvisioningState remoteProvisioningState, String s) {

    }

    /**
     * 短消息声音提示
     */
    private void initSoundPrompt() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SoundPool soundPool = new SoundPool(100, AudioManager.STREAM_MUSIC, 5);//构建对象
                soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                        soundPool.play(sampleId, 1, 1, 1, 0, 1);//播放
                    }
                });
                soundPool.load(App.getApplication(), R.raw.incoming_chat, 1);//加载资源
            }
        }).start();
    }

    /**
     * 提示sip的短消息
     */
    private void disPlayShortMess(final String from, String mess) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(App.getApplication());
        builder.setTitle("新消息");
        View view = View.inflate(App.getApplication(), R.layout.receive_sip_message_item_layout, null);
        TextView showInformation = view.findViewById(R.id.prompt_sip_message_tv_layout);
        showInformation.setText("\u3000\u3000类型:短消息\n" + "\u3000\u3000消息来源:" + from + "\n" + "\u3000\u3000内容:" + mess);
        builder.setView(view);
        final Dialog dialog = builder.create();
        dialog.getWindow().setLayout(300, 60);
        dialog.getWindow().setGravity(Gravity.TOP);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog.isShowing())
                    dialog.dismiss();
            }
        });
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (dialog.isShowing())
                    dialog.dismiss();
            }
        };
        timer.schedule(timerTask, 3000);
    }
}
