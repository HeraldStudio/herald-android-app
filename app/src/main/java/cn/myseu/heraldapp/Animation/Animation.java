package cn.myseu.heraldapp.Animation;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.View;

import cn.myseu.heraldapp.R;

public class Animation {

    public static void slideIn(Context context, View view){
        AnimatorSet slideIn = (AnimatorSet) AnimatorInflater.loadAnimator(context,
                R.animator.self_slide_in);
        slideIn.setTarget(view);
        slideIn.start();
    }

    public static void slideOut(Context context, View view){
        AnimatorSet slideOut = (AnimatorSet) AnimatorInflater.loadAnimator(context,
                R.animator.self_slide_out);
        slideOut.setTarget(view);
        slideOut.start();
    }
}
