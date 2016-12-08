package com.jkxy.notebook.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.jkxy.notebook.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.bmob.v3.BmobUser;

public class SplashActivity extends AppCompatActivity {


    @BindView(R.id.splash_view)
    View view;

    public static final String SEND_USER_NAME = "send_user_name";
    public static final String SEND_USER_ID = "send_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 取消标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);
//        mIvSplash = (ImageView) findViewById(R.id.id_iv_splash);
        // 实现渐变效果
        Animation animation = new AlphaAnimation(0.5f, 1f);
        animation.setDuration(3000);
        view.startAnimation(animation);
        // 动画结束后启动登陆界面或主界面
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
//                // FIXME 退出当前用户
//                BmobUser.logOut(SplashActivity.this);
                // 判断是否有用户登陆
                BmobUser user = BmobUser.getCurrentUser(BmobUser.class);
                // 已经登陆
                if (user != null) {
                    String userName = user.getUsername();
                    String userId = user.getObjectId();
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    intent.putExtra(SEND_USER_NAME, userName);
                    intent.putExtra(SEND_USER_ID, userId);
                    startActivity(intent);
                } else {
                    // 启动登陆界面
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
