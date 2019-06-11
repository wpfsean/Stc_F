package com.tehike.client.stc.app.project.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.global.AppConfig;
import com.tehike.client.stc.app.project.ui.StcDutyLoginActivity;
import com.tehike.client.stc.app.project.utils.UIUtils;

/**
 * 描述：开机自启动（测试用）
 * ===============================
 * @author wpfse wpfsean@126.com
 * @Create at:2018/12/16 19:55
 * @version V1.0
 */

public class BootBroadcast extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(AppConfig.APP_UPDATE_ACTION)) {
            Intent i = new Intent(context, StcDutyLoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            App.startSpeaking(UIUtils.getString(R.string.str_device_start_sucuess));
        }
    }
}