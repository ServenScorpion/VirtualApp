package io.virtualapp.utils;

import android.animation.Animator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class AnimUtils {
    public static void hide(View view) {
        view.clearAnimation();
        AlphaAnimation mHideAnimation = new AlphaAnimation(1.0f, 0.0f);
        mHideAnimation.setDuration(500);
        mHideAnimation.setFillAfter(true);
        mHideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(mHideAnimation);
    }

    public static void show(View view) {
        if(view.getVisibility() == View.VISIBLE){
            return;
        }
        view.clearAnimation();
        AlphaAnimation mHideAnimation = new AlphaAnimation(0.0f, 1.0f);
        mHideAnimation.setDuration(500);
        mHideAnimation.setFillAfter(true);
        mHideAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(mHideAnimation);
    }

    private static void upAnim(View view, int vis, int h) {
        if (vis == View.VISIBLE) {
            view.setTranslationY(h);
            view.setVisibility(View.VISIBLE);
        }
        view.animate().translationY(0)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (vis != View.VISIBLE) {
                            view.setVisibility(vis);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        if (vis != View.VISIBLE) {
                            view.setVisibility(vis);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                }).setDuration(500L).start();
    }

    private static void downAnim(View view, int vis, int h) {
        if(view.getVisibility() == vis){
            return;
        }
        view.setTranslationY(0);
        if (vis == View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
        view.animate().translationY(h)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (vis != View.VISIBLE) {
                            view.setVisibility(vis);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        if (vis != View.VISIBLE) {
                            view.setVisibility(vis);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .setDuration(500L).start();
    }

    /**
     * 顶部上移
     */
    public static void topUpAnim(View view, int vis) {
        downAnim(view, vis, -view.getHeight());
    }

    /**
     * 顶部下移
     */
    public static void topDownAnim(View view, int vis) {
        upAnim(view, vis, -view.getHeight());
    }

    /**
     * 底部上移
     */
    public static void bottomUpAnim(View view, int vis) {
        upAnim(view, vis, view.getHeight());
    }

    /**
     * 底部下移
     */
    public static void bottomDownAnim(View view, int vis) {
        downAnim(view, vis, view.getHeight());
    }
}
