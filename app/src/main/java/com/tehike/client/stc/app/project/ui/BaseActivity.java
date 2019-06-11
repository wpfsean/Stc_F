package com.tehike.client.stc.app.project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.tehike.client.stc.app.project.R;
import com.tehike.client.stc.app.project.utils.ActivityUtils;
import com.tehike.client.stc.app.project.utils.ProgressDialogUtils;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * 描述：Activity基类
 * ===============================
 * @author wpfse wpfsean@126.com
 * @version V1.0
 * @Create at:2018/12/3 10:08
 */

public abstract class BaseActivity extends AppCompatActivity {

    /**
     * 注释binder
     */
    private Unbinder unbinder;

    /**
     * Dialog工具类
     */
    private ProgressDialogUtils progressDialog;

    /**
     * 当前页面是否可见
     */
    public boolean isVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //清除状态栏和actionbar
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        //设置布局
        setContentView(intiLayout());
        unbinder = ButterKnife.bind(this);

        //activity加入栈中
        ActivityUtils.addActivity(this);

        //初始化dialog
        initDialog();

        afterCreate(savedInstanceState);
    }

    protected abstract int intiLayout();

    /**
     * 页面跳转
     */
    public void openActivity(Class cls) {
        startActivity(new Intent(this, cls));
    }

    /**
     * 页面跳转并结束本页面
     */
    public void openActivityAndCloseThis(Class cls) {
        startActivity(new Intent(this, cls));
        finish();
    }

    protected abstract void afterCreate(Bundle savedInstanceState);

    /**
     * 初始化dialog
     */
    private void initDialog() {
        progressDialog = new ProgressDialogUtils(this, R.style.dialog_transparent_style);
    }

    /**
     * 重新请求网络
     */
    public void onNetworkViewRefresh() {
    }

    /**
     * 显示加载的ProgressDialog
     */
    public void showProgressDialog() {
        progressDialog.showProgressDialog();
    }

    /**
     * 显示有加载文字ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param text 需要显示的文字
     */
    public void showProgressDialogWithText(String text) {
        progressDialog.showProgressDialogWithText(text);
    }

    /**
     * 显示加载成功的ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param message 加载成功需要显示的文字
     * @param time    需要显示的时间长度(以毫秒为单位)
     */
    public void showProgressSuccess(String message, long time) {
        progressDialog.showProgressSuccess(message, time);
    }

    /**
     * 显示加载成功的ProgressDialog，文字显示在ProgressDialog的下面
     * ProgressDialog默认消失时间为1秒(1000毫秒)
     *
     * @param message 加载成功需要显示的文字
     */
    public void showProgressSuccess(String message) {
        progressDialog.showProgressSuccess(message);
    }

    /**
     * 显示加载失败的ProgressDialog，文字显示在ProgressDialog的下面
     *
     * @param message 加载失败需要显示的文字
     * @param time    需要显示的时间长度(以毫秒为单位)
     */
    public void showProgressFail(String message, long time) {
        progressDialog.showProgressFail(message, time);
    }

    /**
     * 显示加载失败的ProgressDialog，文字显示在ProgressDialog的下面
     * ProgressDialog默认消失时间为1秒(1000毫秒)
     *
     * @param message 加载成功需要显示的文字
     */
    public void showProgressFail(String message) {
        progressDialog.showProgressFail(message);
    }

    /**
     * 隐藏加载的ProgressDialog
     */
    public void dismissProgressDialog() {
        progressDialog.dismissProgressDialog();
    }


    //防止点击过快
    public boolean fastClick() {
        long lastClick = 0;
        if (System.currentTimeMillis() - lastClick <= 1000) {
            return false;
        }
        lastClick = System.currentTimeMillis();
        return true;
    }


    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
        ActivityUtils.removeActivity(this);
    }
}
