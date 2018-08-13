package cn.myseu.heraldapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.reactivestreams.Subscription;

import cn.myseu.heraldapp.Services.AuthService;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class LoginActivity extends AppCompatActivity {

    private ImageView mLoginFaceImageView;
    private EditText mCardnumEditText;
    private EditText mPasswordEditText;
    private Button mLoginButton;

    private int debugCounter = 0;

    Disposable mAuthSubscription;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.getWindow().setStatusBarColor(Color.argb(100, 255, 255, 255));
            this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        mLoginFaceImageView = (ImageView) findViewById(R.id.login_face);
        mCardnumEditText = (EditText) findViewById(R.id.cardnum_edit);
        mPasswordEditText = (EditText) findViewById(R.id.password_edit);
        mLoginButton = (Button) findViewById(R.id.login_button);

        // 模仿Bilibili的变脸效果
        mCardnumEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    mLoginFaceImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.icon_normal));
                }
            }
        });
        mPasswordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    mLoginFaceImageView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.icon_password));
                }
            }
        });

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLoginButton.setClickable(false);
                mLoginButton.setText("请稍候...");
                login(mCardnumEditText.getText().toString(), mPasswordEditText.getText().toString());
            }
        });

        mLoginFaceImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                debugCounter++;
                if (debugCounter > 5 && debugCounter < 10) {
                    Toast.makeText(LoginActivity.this, String.format("再点击%d次进入调试模式", 10 - debugCounter), Toast.LENGTH_SHORT).show();
                }
                if (debugCounter >= 10) {
                    mLoginButton.setText("进入调试");
                }
            }
        });

    }

    private void login(String cardnum, String password) {

        Log.i("login-cardnum", cardnum);
        AuthService authService = new AuthService();
        authService.auth(cardnum, password).subscribe(new Observer<AuthService.AuthResult>() {
            @Override
            public void onSubscribe(Disposable d) {
                mAuthSubscription = d;
            }

            @Override
            public void onNext(AuthService.AuthResult authResult) {
                if (authResult.getSuccess()) {
                    Log.d("token", authResult.getResult());
                    SharedPreferences tokenSharedPreferences = getSharedPreferences("token", MODE_PRIVATE);
                    SharedPreferences.Editor tokenEditor = tokenSharedPreferences.edit();
                    tokenEditor.clear();
                    tokenEditor.putString("token", authResult.getResult());
                    tokenEditor.commit();
                    if ( debugCounter < 10) {
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Intent intent = new Intent(LoginActivity.this, DebugActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    mLoginButton.setClickable(true);
                    mLoginButton.setText("登录");
                    debugCounter = 0;
                    Log.e("login-fail", authResult.getReason());
                    Toast.makeText(LoginActivity.this, authResult.getReason(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {
                mLoginButton.setClickable(true);
                mLoginButton.setText("登录");
                debugCounter = 0;
                Log.e("login-fail", "网络失败");
                Toast.makeText(LoginActivity.this, "先检查下网络吧！", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {

            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            return true;
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
