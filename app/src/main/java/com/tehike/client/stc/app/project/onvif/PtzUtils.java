package com.tehike.client.stc.app.project.onvif;

import android.text.TextUtils;

import com.tehike.client.stc.app.project.entity.VideoBean;
import com.tehike.client.stc.app.project.utils.Logutil;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 描述：$desc$
 * ===============================
 *
 * @author $user$ wpfsean@126.com
 * @version V1.0
 * @Create at:$date$ $time$
 */

public class PtzUtils extends Thread {

    /**
     * 当前云台控制对象
     */
    VideoBean videoBean ;

    /**
     * 动作
     */
    String flag;

    /**
     * 云台移动Url
     */
    String moveUrl;

    /**
     * 云台缩放Url
     */
    String zoomUrl;

    /**
     * 云台控制请求头
     */
    String ptzUrl = "";

    /**
     * 构造函数
     */
    public PtzUtils(VideoBean videoBean,String flag){
        this.videoBean = videoBean;
        this.flag = flag;
    }

    @Override
    public void run() {
        //判断云台控制对象是否为空
        if (videoBean==null)
            return;
        //获取云台控制的url
        moveUrl = videoBean.getMoveUrl();
        zoomUrl = videoBean.getZoomUrl();
        //判断是否为空
        if (TextUtils.isEmpty(moveUrl)||TextUtils.isEmpty(zoomUrl))
            return;
        //云台控制请求的http头
        ptzUrl = videoBean.getPtzUrl();
        if (TextUtils.isEmpty(ptzUrl))
            return;

        synchronized (PtzUtils.class){
            switch (flag){
                case "left":
                    ptzLeftMove();
                    break;
                case "right":
                    ptzRightMove();
                    break;
                case "top":
                    ptzTopMove();
                    break;
                case "below":
                  ptzBottomMove();
                    break;
                case "top_left":
                    ptzLeftTopMove();
                    break;
                case "top_right":
                    ptzRightTopMove();
                    break;
                case "left_below":
                    ptzLeftBottomMove();
                    break;
                case "right_below":
                    ptzRightBottomMove();
                    break;
                case "zoom_b":
                    ptzBigMove();
                    break;
                case "zoom_s":
                  ptzSmallMove();
                    break;
                case "stop":
                    ptzStop();
                    break;


            }
        }
    }

    /**
     * 停止
     */
    private void ptzStop() {
        String stopUrl = videoBean.getStopUrl();
        String result = postRequest(ptzUrl,stopUrl);
        Logutil.d(ptzUrl);
        Logutil.d(stopUrl);
        Logutil.d(videoBean.toString());
    }

    /**
     * 左上移
     */
    public void ptzLeftTopMove(){
        String paramater = String.format(moveUrl,"-0.2","0.1");
        String result = postRequest(ptzUrl,paramater);
    }

    /**
     * 左下移
     */
    public void ptzLeftBottomMove(){
        String paramater = String.format(moveUrl,"-0.2","-0.1");
        String result = postRequest(ptzUrl,paramater);
    }

    /**
     * 右上移
     */
    public void ptzRightTopMove(){
        String paramater = String.format(moveUrl,"0.2","0.1");
        String result = postRequest(ptzUrl,paramater);
    }
    /**
     * 右下移
     */
    public void ptzRightBottomMove(){
        String paramater = String.format(moveUrl,"0.2","-0.1");
        String result = postRequest(ptzUrl,paramater);
    }


    /**
     * 左移
     */
    public void ptzLeftMove(){
        String paramater = String.format(moveUrl,"-0.2","0");
        String result = postRequest(ptzUrl,paramater);
    }

    /**
     * 右移
     */
    public void ptzRightMove(){
        String paramater = String.format(moveUrl,"0.2","0");
        String result = postRequest(ptzUrl,paramater);
    }

    /**
     * 上移
     */
    public void ptzTopMove(){
        String paramater = String.format(moveUrl,"0","0.2");
        String result = postRequest(ptzUrl,paramater);
    }

    /**
     * 下移
     */
    public void ptzBottomMove(){
        String paramater = String.format(moveUrl,"0","-0.2");
        String result = postRequest(ptzUrl,paramater);
    }

    /**
     * 放大
     */
    public void ptzBigMove(){
        String paramater = String.format(zoomUrl,"0.3");
        String result = postRequest(ptzUrl,paramater);
    }

    /**
     * 缩小
     */
    public void ptzSmallMove(){
        String paramater = String.format(zoomUrl,"-0.3");
        String result = postRequest(ptzUrl,paramater);
    }



    public static String postRequest(String baseUrl, String params)  {
        try {
            String receive = "";
            // 新建一个URL对象
            URL url = new URL(baseUrl);
            // 打开一个HttpURLConnection连接
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            //设置请求允许输入 默认是true
            urlConn.setDoInput(true);
            // Post请求必须设置允许输出 默认false
            urlConn.setDoOutput(true);
            // 设置为Post请求
            urlConn.setRequestMethod("POST");
            // Post请求不能使用缓存
            urlConn.setUseCaches(false);
            //设置本次连接是否自动处理重定向
            urlConn.setInstanceFollowRedirects(true);
            // 配置请求Content-Type,application/soap+xml
            urlConn.setRequestProperty("Content-Type",
                    "application/soap+xml;charset=utf-8");
            // 开始连接
            urlConn.connect();
            // 发送请求数据
            urlConn.getOutputStream().write(params.getBytes());
            // 判断请求是否成功
            if (urlConn.getResponseCode() == 200) {
                // 获取返回的数据
                InputStream is = urlConn.getInputStream();
                byte[] data = new byte[1024];
                int n;
                while ((n = is.read(data)) != -1) {
                    receive = receive + new String(data, 0, n);
                }
            } else {
                throw new Exception("ResponseCodeError : " + urlConn.getResponseCode());
            }
            // 关闭连接
            urlConn.disconnect();
            return receive;
        }catch (Exception e){
            return null;
        }
    }
}
