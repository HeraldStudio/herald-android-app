package cn.myseu.heraldapp.Services;

import android.content.SharedPreferences;


import com.google.gson.Gson;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;



public class AuthService {

    public Observable<AuthResult> auth(String cardnum, String password) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://myseu.cn/ws3/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        AuthInterface authInterface = retrofit.create(AuthInterface.class);

        AuthBody authBody = new AuthBody(cardnum, password);

        Gson gs = new Gson();
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                gs.toJson(authBody)
        );

        return authInterface.auth(requestBody)
                .subscribeOn(Schedulers.io())//IO线程加载数据
                .observeOn(AndroidSchedulers.mainThread());
    }

    interface AuthInterface {
        @POST("auth")
        Observable<AuthService.AuthResult> auth(@Body RequestBody body);
    }

    public static class AuthResult {
        private Boolean success;
        private int code;
        private String result;
        private String reason;

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    public static class AuthBody {
        private String cardnum;
        private String password;
        private String platform;

        AuthBody(String cardnum, String password) {
            this.cardnum = cardnum;
            this.password = password;
            this.platform = "android";
        }

        public void setCardnum(String cardnum) {
            this.cardnum = cardnum;
        }

        public String getCardnum() {
            return cardnum;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getPlatform() {
            return platform;
        }
    }
}



