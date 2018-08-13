package cn.myseu.heraldapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

//import android.webkit.WebView;
//import android.webkit.WebViewClient;
import cn.myseu.heraldapp.Components.AuthWebView;

import java.util.ArrayList;
import java.util.Stack;

public class HomeActivity extends AppCompatActivity {

    public static String BASE_URL = "https://hybrid.myseu.cn/index.html";
    private AuthWebView mMainWebView;
    private AuthWebView mSubWebView;
    
    private LinearLayout mWebViewContainer;
    private LinearLayout mSubWebViewContainer;
    private ConstraintLayout mTabBar;
    private Toolbar mToolbar;
    private ImageView mBackButton;
    private TextView mNavigationTitle;
    private ImageView mIcon;

    private ArrayList<LinearLayout> mTabButtons = new ArrayList<>();
    private ArrayList<ImageView> mTabImageViews = new ArrayList<>();
    private ArrayList<TextView> mTabTextViews = new ArrayList<>();

    private String mToken;

    private String[] tabPath = { "/home-tab", "/activity-tab", "/notification-tab", "/personal-tab"};
    private int mTabCurrentIndex = 0;

    private Stack<ArrayList<String>> mRouteHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 设置ToolBar
        mToolbar = (Toolbar) findViewById(R.id.herald_toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        mBackButton = (ImageView) findViewById(R.id.back_button);
        mNavigationTitle = (TextView) findViewById(R.id.navigation_text);
        mIcon = (ImageView) findViewById(R.id.toolbar_icon);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popRoute();
            }
        });

        // 初始化Tab
        mTabBar = (ConstraintLayout) findViewById(R.id.herald_tabbar);
        findTabViews();

        // 检查token
        mToken = getToken();
        if (!mToken.equals("no-token")) {
            // 初始化webView并设置JS注入、JS接口
            initWebView();
        } else {
            // 身份认证过期
            authFail();
        }
        

        // 生成路由栈，初始为空
        mRouteHistory = new Stack<>();
    }

    @Override
    protected void onDestroy() {
        if (mMainWebView != null) {
            mWebViewContainer.removeAllViews();
            if(mMainWebView != null) {
                mMainWebView.clearHistory();
                mMainWebView.clearCache(true);
                mMainWebView.loadUrl("about:blank"); // clearView() should be changed to loadUrl("about:blank"), since clearView() is deprecated no
                mMainWebView.destroy(); // Note that mMainWebView.destroy() and mMainWebView = null do the exact same thing
            }
            if(mSubWebView != null) {
                mSubWebView.clearHistory();
                mSubWebView.clearCache(true);
                mSubWebView.loadUrl("about:blank"); // clearView() should be changed to loadUrl("about:blank"), since clearView() is deprecated no
                mSubWebView.destroy(); // Note that mMainWebView.destroy() and mMainWebView = null do the exact same thing
            }
        }
        super.onDestroy();
    }
    
    private void initWebView() {
        // 生成WebView
        mMainWebView = new AuthWebView(HomeActivity.this); // 用于主页显示的WebView
        mSubWebView = new AuthWebView(HomeActivity.this); // 用于其他页面显示的WebView
        
        mWebViewContainer = (LinearLayout) findViewById(R.id.auth_web_view_container);
        mWebViewContainer.addView(mMainWebView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mSubWebViewContainer = (LinearLayout) findViewById(R.id.auth_sub_web_view_container);
        mSubWebViewContainer.addView(mSubWebView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        mSubWebViewContainer.setVisibility(View.GONE);

        mMainWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView webView, String s) {
                //mMainWebView.pushRoute(tabPath[0]);
                super.onPageFinished(webView, s);
                setTabListener();
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
        mMainWebView.loadUrl( HomeActivity.BASE_URL ); // 在创建活动时即加载
        //mMainWebView.loadUrl("https://myseu.cn/#/"); // 在创建活动时即加载
        // 同时初始化副WebView并加载页面
        mSubWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView webView, String s) {
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
        setJsInterface(mSubWebView);
        mSubWebView.loadUrl( HomeActivity.BASE_URL ); // 在创建活动时即加载
        //mSubWebView.loadUrl("https://myseu.cn/#/"); // 在创建活动时即加载
    }

    private String getToken() {
        SharedPreferences tokenSharedPreferences = getSharedPreferences("token", MODE_PRIVATE);
        return tokenSharedPreferences.getString("token", "no-token");
    }

    private void findTabViews() {
        // 获取TabButtons
        mTabButtons.add( (LinearLayout) findViewById(R.id.tab_home_button));
        mTabButtons.add( (LinearLayout) findViewById(R.id.tab_activity_button));
        mTabButtons.add( (LinearLayout) findViewById(R.id.tab_notification_button));
        mTabButtons.add( (LinearLayout) findViewById(R.id.tab_personal_button));
        // 获取TabImages
        mTabImageViews.add( (ImageView) findViewById(R.id.tab_home_image));
        mTabImageViews.add( (ImageView) findViewById(R.id.tab_activity_image));
        mTabImageViews.add( (ImageView) findViewById(R.id.tab_notification_image));
        mTabImageViews.add( (ImageView) findViewById(R.id.tab_personal_image));
        // 获取tabTexts
        mTabTextViews.add( (TextView) findViewById(R.id.tab_home_text));
        mTabTextViews.add( (TextView) findViewById(R.id.tab_activity_text));
        mTabTextViews.add( (TextView) findViewById(R.id.tab_notification_text));
        mTabTextViews.add( (TextView) findViewById(R.id.tab_personal_text));
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
                    HomeActivity.this.authFail();
                }
            });
        }

        @JavascriptInterface
        public void pushRoute(final String route, final String title) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    HomeActivity.this.pushRoute(route, title);
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
        webView.addJavascriptInterface(new JsInterface(), "android");
    }

    private void authFail() {
        // token不存在，启动登录界面并销毁当前活动
        Intent intent =  new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void pushRoute(String route, String title){
        
        // 一旦路由入栈，一定是在副WebView中

        mSubWebView.pushRoute(route);
        ArrayList<String> history = new ArrayList<>();
        history.add(route);
        history.add(title);
        mRouteHistory.push(history);
        mNavigationTitle.setText(title);
        mNavigationTitle.setVisibility(View.VISIBLE);
        mBackButton.setVisibility(View.VISIBLE);
        mTabBar.setVisibility(View.GONE);
        mIcon.setVisibility(View.GONE);
        mSubWebViewContainer.setVisibility(View.VISIBLE);
        mWebViewContainer.setVisibility(View.GONE);
    }

    private void popRoute(){
        mRouteHistory.pop();
        if(!mRouteHistory.empty()) {
            ArrayList<String> history = mRouteHistory.peek();
            mNavigationTitle.setText(history.get(1));
            mSubWebView.pushRoute(history.get(0));
        } else {
            mNavigationTitle.setVisibility(View.GONE);
            mBackButton.setVisibility(View.GONE);
            mIcon.setVisibility(View.VISIBLE);
            mTabBar.setVisibility(View.VISIBLE);
            mSubWebViewContainer.setVisibility(View.GONE);
            mWebViewContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setTabListener() {
        for ( LinearLayout button: mTabButtons) {
            final int index = mTabButtons.indexOf(button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (index != mTabCurrentIndex) {
                        mTabCurrentIndex = index;
                        int[] selectedIcons = {R.drawable.home_tab_icon_selected,
                                R.drawable.activity_tab_icon_selected,
                                R.drawable.notification_tab_icon_selected,
                                R.drawable.personal_tab_icon_selected};

                        int[] icons = {R.drawable.home_tab_icon,
                                R.drawable.activity_tab_icon,
                                R.drawable.notification_tab_icon,
                                R.drawable.personal_tab_icon};
                        mMainWebView.pushRoute(tabPath[index]);
                        for (int buttonIndex = 0; buttonIndex < mTabButtons.size(); buttonIndex++) {
                            if(buttonIndex == index){
                                mTabImageViews.get(buttonIndex).setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),selectedIcons[buttonIndex]));
                                mTabTextViews.get(buttonIndex).setTextColor(getResources().getColor(R.color.colorPrimary));
                            } else {
                                mTabImageViews.get(buttonIndex).setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),icons[buttonIndex]));
                                mTabTextViews.get(buttonIndex).setTextColor(getResources().getColor(R.color.colorUnfocused));
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mRouteHistory.empty()) {
                // 如果历史栈是空的，则返回桌面，但不退出程序
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                home.addCategory(Intent.CATEGORY_HOME);
                startActivity(home);
                return true;
            } else {
                // 历史栈不为空则回退历史栈
                popRoute();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

}
