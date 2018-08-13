package cn.myseu.heraldapp;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothClass;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceError;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.ArrayList;

import cn.myseu.heraldapp.Animation.Animation;
import cn.myseu.heraldapp.Components.AuthWebView;

public class DebugActivity extends AppCompatActivity {

    AuthWebView mMainWebView;
    Button mGoButton;
    EditText mEditText;
    LinearLayout mWebViewContainer;
    String mToken;
    String mBaseUrl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        mWebViewContainer = (LinearLayout) findViewById(R.id.debug_webview_container);
        mGoButton = (Button) findViewById(R.id.debug_go_button);
        mEditText = (EditText) findViewById(R.id.debug_text_edit);

        // 检查token
        mToken = getToken();
        if (!mToken.equals("no-token")) {
            // 初始化webView并设置JS注入、JS接口
            initWebView();
        } else {
            // 身份认证过期
            authFail();
        }

        mGoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pushRoute(mEditText.getText().toString());
            }
        });

    }

    private String getToken() {
        SharedPreferences tokenSharedPreferences = getSharedPreferences("token", MODE_PRIVATE);
        return tokenSharedPreferences.getString("token", "no-token");
    }

    private void initWebView() {
        // 生成WebView
        mMainWebView = new AuthWebView(DebugActivity.this); // 用于主页显示的WebView

        mWebViewContainer.addView(mMainWebView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        mMainWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView webView, String s) {
                //mMainWebView.pushRoute(tabPath[0]);
                super.onPageFinished(webView, s);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String s) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(s));
                startActivity(i);
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view,
                                           SslErrorHandler handler, SslError error) {
                // TODO Auto-generated method stub
                // handler.cancel();// Android默认的处理方式
                handler.proceed();// 接受所有网站的证书
                // handleMessage(Message msg);// 进行其他处理
            }

        });
        setJsInterface(mMainWebView);
        mMainWebView.loadUrl( mEditText.getText().toString() ); // 在创建活动时即加载
    }

    private class JsInterface {
        @JavascriptInterface
        public String getToken() {
            Log.d("jsInterface", "token获取成功");
            return mToken;
        }

        @JavascriptInterface
        public void authFail() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DebugActivity.this.authFail();
                }
            });
        }

        @JavascriptInterface
        public void pushRoute(final String route, final String title) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DebugActivity.this.pushRoute(route);
                }
            });
        }

        @JavascriptInterface
        public void openURLinBrowser(String url){
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }

    }

    private void setJsInterface(AuthWebView webView) {
        webView.addJavascriptInterface(new DebugActivity.JsInterface(), "android");
    }

    private void authFail() {
        // token不存在，启动登录界面并销毁当前活动
        Intent intent =  new Intent(DebugActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void pushRoute(String route){
        if (route.contains("http")) {
            mBaseUrl = route;
            mMainWebView.loadUrl(route);
        }  else {
            mMainWebView.pushRoute(route);
        }
    }
}
