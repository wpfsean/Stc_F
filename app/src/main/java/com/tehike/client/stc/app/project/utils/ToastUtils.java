package com.tehike.client.stc.app.project.utils;

import android.widget.Toast;
import com.tehike.client.stc.app.project.App;


public class ToastUtils {

	private static Toast mToast;

	public static void showLong(String text) {
		if (mToast == null) {
			mToast = Toast.makeText(App.getApplication(), text, Toast.LENGTH_LONG);
		} else {
			mToast.setText(text);
		}
		mToast.show();
	}

	public static void showShort(String text) {
		if (mToast == null) {
			mToast = Toast.makeText(App.getApplication(), text, Toast.LENGTH_SHORT);
		} else {
			mToast.setText(text);
		}
		mToast.show();
	}

}
