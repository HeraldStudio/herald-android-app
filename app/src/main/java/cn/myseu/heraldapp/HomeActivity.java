package cn.myseu.heraldapp;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceError;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.TbsListener;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

//import android.webkit.WebView;
//import android.webkit.WebViewClient;
import cn.myseu.heraldapp.Animation.Animation;
import cn.myseu.heraldapp.Components.AuthWebView;
import pub.devrel.easypermissions.EasyPermissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
    private ImageView mRefreshButton;
    private TextView mNavigationTitle;
    private ImageView mIcon;

    private ArrayList<LinearLayout> mTabButtons = new ArrayList<>();
    private ArrayList<ImageView> mTabImageViews = new ArrayList<>();
    private ArrayList<TextView> mTabTextViews = new ArrayList<>();

    private String mToken;

    private String[] tabPath = { "/home-tab", "/activity-tab", "/notification-tab", "/personal-tab"};
    private int mTabCurrentIndex = 0;

    private Stack<ArrayList<String>> mRouteHistory;

    private String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE, };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 设置刘海屏需要的颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(Color.argb(100, 255, 255, 255));
            this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        this.requestPermisson();
        QbSdk.initX5Environment(this, new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {

            }

            @Override
            public void onViewInitFinished(boolean b) {
                if (b) {
                    Log.d("herald-x5", "x5内核加载成功");
                }
            }
        });

        // 设置ToolBar
        mToolbar = (Toolbar) findViewById(R.id.herald_toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        mBackButton = (ImageView) findViewById(R.id.back_button);
        mRefreshButton = (ImageView) findViewById(R.id.refresh_button);
        mNavigationTitle = (TextView) findViewById(R.id.navigation_text);
        mIcon = (ImageView) findViewById(R.id.toolbar_icon);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popRoute();
            }
        });
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 换成带表情的图标
                mRefreshButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.refresh_button_loading));
                // 转
                Animation.refresh(HomeActivity.this, (View) mRefreshButton, new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        // 换成不带表情的图标
                        mRefreshButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.refresh_button));
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                });
                mMainWebView.reload();
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
                mMainWebView.clearCache(false);
                mMainWebView.loadUrl("about:blank"); // clearView() should be changed to loadUrl("about:blank"), since clearView() is deprecated no
                mMainWebView.destroy(); // Note that mMainWebView.destroy() and mMainWebView = null do the exact same thing
            }
            if(mSubWebView != null) {
                mSubWebView.clearHistory();
                mSubWebView.clearCache(false);
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

        // 判断是否无缓存加载
        SharedPreferences cacheSharedPreferences = getSharedPreferences("cache", MODE_PRIVATE);
        Boolean cache = cacheSharedPreferences.getBoolean("cache", true);
        if (!cache) {
            mMainWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            mSubWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            cacheSharedPreferences.edit().putBoolean("cache", true).commit();
        }

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


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                //Log.e(TAG, "onReceivedError: ----url:" + error.getDescription());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    return;
                }
                Intent intent =  new Intent(HomeActivity.this, NotFoundActivity.class);
                startActivity(intent);
                finish();
            }

            // 新版本，只会在Android6及以上调用
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) { // 或者： if(request.getUrl().toString() .equals(getUrl()))
                    Intent intent =  new Intent(HomeActivity.this, NotFoundActivity.class);
                    startActivity(intent);
                    finish();
                }
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

        @JavascriptInterface
        public void clearCache(){
            HomeActivity.this.clearCache();
        }

        @JavascriptInterface
        public void toast(String text){
            Toast.makeText(HomeActivity.this, text, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public int getVersionCode(){
            return BuildConfig.VERSION_CODE;
        }

    }

    private void setJsInterface(AuthWebView webView) {
        webView.addJavascriptInterface(new JsInterface(), "android");
    }

    private void clearCache() {
        SharedPreferences cacheSharedPreferences = getSharedPreferences("cache", MODE_PRIVATE);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.putBoolean("cache", false);
        editor.commit();
        Intent intent =  new Intent(HomeActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void authFail() {
        // 为了实现切换登录而曲线救国
        SharedPreferences cacheSharedPreferences = getSharedPreferences("token", MODE_PRIVATE);
        SharedPreferences.Editor editor = cacheSharedPreferences.edit();
        editor.clear();
        editor.commit();
        // token不存在，启动登录界面并销毁当前活动
        Intent intent =  new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void pushRoute(String route, String title){
        
        // 一旦路由入栈，一定是在副WebView中
        mRefreshButton.setVisibility(View.GONE);
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
        Animation.slideIn(this, (View) mSubWebView);
    }

    private void popRoute(){
        mRouteHistory.pop();
        if(!mRouteHistory.empty()) {
            ArrayList<String> history = mRouteHistory.peek();
            mNavigationTitle.setText(history.get(1));
            mSubWebView.pushRoute(history.get(0));
            Animation.slideOut(this, (View) mSubWebView);
        } else {
            Animation.slideOut(this, (View) mSubWebView);
            mNavigationTitle.setVisibility(View.GONE);
            mBackButton.setVisibility(View.GONE);
            mRefreshButton.setVisibility(View.VISIBLE);
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

    private void requestPermisson() {

        if (EasyPermissions.hasPermissions(this, permissions)) {
            //已经打开权限
            //Toast.makeText(this, "已经申请相关权限", Toast.LENGTH_SHORT).show();
        } else {
            //没有打开相关权限、申请权限
            EasyPermissions.requestPermissions(this, "需要获取您的相册、照相使用权限", 1, permissions);
        }
        // Here, thisActivity is the current activity
//        if (ContextCompat.checkSelfPermission(HomeActivity.this,
//                Manifest.permission.READ_PHONE_STATE)
//                != PackageManager.PERMISSION_GRANTED) {
//
//            // Should we show an explanation?
//            if (ActivityCompat.shouldShowRequestPermissionRationale(HomeActivity.this,
//                    Manifest.permission.READ_PHONE_STATE)) {
//
//                // Show an expanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//
//            } else {
//
//                // No explanation needed, we can request the permission.
//
//                ActivityCompat.requestPermissions(HomeActivity.this,
//                        new String[]{Manifest.permission.READ_PHONE_STATE},
//                        1);
//
//                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
//                // app-defined int constant. The callback method gets the
//                // result of the request.
//            }
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //框架要求必须这么写
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    //成功打开权限
    //@Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

        //Toast.makeText(this, "相关权限获取成功", Toast.LENGTH_SHORT).show();
    }
    //用户未同意权限
    //@Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, "请同意相关权限，否则部分功能无法正常使用", Toast.LENGTH_SHORT).show();
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
