package cn.myseu.heraldapp.Components;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebViewClient;


//import android.webkit.WebView;
//import android.webkit.WebSettings;
//import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.LinearLayout;

import java.text.AttributedCharacterIterator;
import io.reactivex.Observable;

import cn.myseu.heraldapp.R;
import io.reactivex.functions.Action;

public class AuthWebView extends WebView {

    private  WebSettings webSettings;
    private String token;

    public AuthWebView(Context context) {

        super(context);
        webSettings = getSettings();
        // 启用Javascript *默认不启用，你是魔鬼吗？
        webSettings.setJavaScriptEnabled(true);

        // 启用localStorage
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheMaxSize(1024*1024*8);
        String appCachePath = context.getCacheDir().getAbsolutePath();
        webSettings.setAppCachePath(appCachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        //启用调试
        setWebContentsDebuggingEnabled(true);

        // 不显示滚动条
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        // 不显示X5内核自带的快速滚动条
        if(getX5WebViewExtension() != null) {
            Log.d("herald-x5","正在使用X5内核");
            getX5WebViewExtension().setScrollBarFadingEnabled(false);
            getX5WebViewExtension().setHorizontalScrollBarEnabled(false);
            getX5WebViewExtension().setVerticalScrollBarEnabled(false);
        }
    }

    public void setToken(String token){
        this.token = token;
    }

    public void injectToken(String token) {
        evaluateJavascript("window.injectToken('"+token+"')", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) { }
        });
    }

    public void pushRoute(String route) {
        evaluateJavascript("window.goto('" + route + "')", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {

            }
        });
    }


}
