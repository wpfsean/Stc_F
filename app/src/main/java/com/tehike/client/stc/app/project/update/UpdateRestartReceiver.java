package com.tehike.client.stc.app.project.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tehike.client.stc.app.project.App;
import com.tehike.client.stc.app.project.ui.StcDutyLoginActivity;


/**
 * 描述：用来接收更新后自启的广播
 * ===============================
 * @author wpfse wpfsean@126.com
 * @Create at:2019/1/2 14:42
 * @version V1.0
 */
public class UpdateRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ( intent.getAction().equals("android.intent.action.PACKAGE_REPLACED") ) {
            Intent intent2 = new Intent(context, StcDutyLoginActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent2);
        }
    }
}
