package com.lody.virtual.server.notification;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.lody.virtual.R;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.VLog;

import java.util.ArrayList;
import java.util.HashMap;


/* package */ class RemoteViewsFixer {
    private static final String TAG = NotificationCompat.TAG;
    private static final boolean DEBUG = false;
    private final WidthCompat mWidthCompat;
    private int notification_min_height, notification_max_height, notification_mid_height;
    private int notification_panel_width;
    private int notification_side_padding;
    private int notification_padding;

    private final HashMap<String, Bitmap> mImages = new HashMap<>();
    private NotificationCompat mNotificationCompat;

    RemoteViewsFixer(NotificationCompat notificationCompat) {
        mWidthCompat = new WidthCompat();
        mNotificationCompat = notificationCompat;
    }

    View toView(final Context context, RemoteViews remoteViews, boolean isBig) {
        View mCache = null;
        try {
            //创建通知栏的根View
            View layout = LayoutInflater.from(context).inflate(remoteViews.getLayoutId(), null);
            mCache = layout;//如果下个步骤出错，就显示这个
            mCache = createView(context, remoteViews, layout, isBig);
        } catch (Throwable throwable) {
            VLog.w(TAG, "toView", throwable);
        }
        return mCache;
    }

    Bitmap createBitmap(View mCache) {
        if (mCache == null) {
            return null;
        }
        mCache.setDrawingCacheEnabled(true);
        mCache.buildDrawingCache();
        return mCache.getDrawingCache();
    }

    private void apply(Context context, View view, RemoteViews remoteViews) {
        try {
            Reflect.on(view).call("setTagInternal", Reflect.on("com.android.internal.R$id").get("widget_frame"), remoteViews.getLayoutId());
        } catch (Exception e2) {
            VLog.w(TAG, "setTagInternal", e2);
        }
        ArrayList<Object> mActions = Reflect.on(remoteViews).get("mActions");
        if (mActions != null) {
            if (DEBUG) {
                VLog.d(TAG, "apply actions:" + mActions.size());
            }
            for (Object action : mActions) {
                try {
                    //把action应用到view上面
                    Reflect.on(action).call("apply", view, null, null);
                    if (DEBUG) {
                        if (action.getClass().getName().contains("ReflectionAction")) {
                            VLog.d(TAG, "apply action:%s, methodName=%s", action, Reflect.on(action).get("methodName"));
                        } else {
                            VLog.d(TAG, "apply action:%s", action);
                        }
                    }
                } catch (Exception e) {
                    VLog.w(TAG, "apply action", e);
                }
            }
        }
    }

    private View createView(final Context context, RemoteViews remoteViews, View layout, boolean isBig) {
        if (remoteViews == null)
            return null;
        Context base = mNotificationCompat.getHostContext();
        init(base);
        if(DEBUG){
            VLog.v(TAG, "createView:big=" + isBig);
        }

        //通知栏高度适配
        int height = isBig ? notification_max_height : notification_min_height;
        //宽度
        int width = mWidthCompat.getNotificationWidth(base, notification_panel_width, height,
                notification_side_padding);
        if(DEBUG){
            VLog.v(TAG, "createView:getNotificationWidth=" + width);
        }
        //放置通知栏的容器
        ViewGroup frameLayout = new FrameLayout(context);
        if(DEBUG){
            VLog.v(TAG, "createView:apply");
        }
        //应用RemoteViews的mActions
        apply(context, layout, remoteViews);

        //通知栏view的宽高处理
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        frameLayout.addView(layout, params);
        if (layout instanceof ViewGroup) {
            if (DEBUG) {
                VLog.v(TAG, "createView:fixTextView");
            }
            fixTextView((ViewGroup) layout);
        }
        int mode = View.MeasureSpec.AT_MOST;
        //TODO need adaptation
        if (BuildCompat.isOreo() && !isBig) {
            mode = View.MeasureSpec.EXACTLY;
        }
        if(DEBUG){
            VLog.v(TAG, "createView:layout");
        }
        View mCache = frameLayout;
        mCache.layout(0, 0, width, height);
        mCache.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, mode));
        int height2 = mCache.getMeasuredHeight();
        if(height2 == 0){
            height2 = height;
        }
        mCache.layout(0, 0, width, height2);
        if(DEBUG){
            VLog.v(TAG, "notification:max=%d/%d, size=%d/%d", width, height,
                    mCache.getMeasuredWidth(), mCache.getMeasuredHeight());
        }
        return mCache;
    }

    private void fixTextView(ViewGroup viewGroup) {
        int count = viewGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = viewGroup.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                if (isSingleLine(tv)) {
                    tv.setSingleLine(false);
                    tv.setMaxLines(1);
                }
            } else if (v instanceof ViewGroup) {
                fixTextView((ViewGroup) v);
            }
        }
    }

    private boolean isSingleLine(TextView textView) {
        boolean singleLine;
        try {
            singleLine = Reflect.on(textView).get("mSingleLine");
        } catch (Exception e) {
            singleLine = (textView.getInputType() & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
        }
        return singleLine;
    }

    public RemoteViews makeRemoteViews(String key, Context pluginContext, RemoteViews contentView, boolean isBig, boolean click) {
        if (contentView == null) {
            return null;
        }
        //获取全部点击事件，VA是添加32个透明按钮，把某个view的区域计算出来占几个按钮，这些按钮都触发这个view的点击事件
        final PendIntentCompat pendIntentCompat = new PendIntentCompat(contentView);
        final int layoutId;
        if (!click || pendIntentCompat.findPendIntents() <= 0) {
            //没点击事件
            layoutId = R.layout.custom_notification_lite;
        } else {
            layoutId = R.layout.custom_notification;
        }
        if(DEBUG){
            VLog.v(TAG, "createviews id = " + layoutId);
        }
        //VA的静态通知栏
        RemoteViews remoteViews = new RemoteViews(mNotificationCompat.getHostContext().getPackageName(), layoutId);
        if(DEBUG){
            VLog.v(TAG, "remoteViews to view");
        }
        //把目标通知栏生成View对象
        View cache = toView(pluginContext, contentView, isBig);
        // remoteViews to bitmap
        if(DEBUG){
            VLog.v(TAG, "start createBitmap");
        }
        //把View对象绘制成bitmap
        final Bitmap bmp = createBitmap(cache);
        if(DEBUG){
            if (bmp == null) {
                VLog.e(TAG, "bmp is null,contentView=" + contentView);
                // return null; //ignore notification
            } else {
                VLog.v(TAG, "bmp w=" + bmp.getWidth() + ",h=" + bmp.getHeight());
            }
        }
        Bitmap old;
        synchronized (mImages) {
            old = mImages.get(key);
        }
        if (old != null && !old.isRecycled()) {
            if(DEBUG){
                VLog.v(TAG, "recycle " + key);
            }
            old.recycle();
        }
        remoteViews.setImageViewBitmap(R.id.im_main, bmp);
        if(DEBUG){
            VLog.v(TAG, "createview " + key);
        }
        synchronized (mImages) {
            mImages.put(key, bmp);
        }
        //notification's click
        if (click) {
            if (layoutId == R.layout.custom_notification) {
                if(DEBUG){
                    VLog.v(TAG, "start setPendIntent");
                }
                try {
                    pendIntentCompat.setPendIntent(remoteViews,
                            toView(mNotificationCompat.getHostContext(), remoteViews, isBig),
                            cache);
                } catch (Exception e) {
                    VLog.e(TAG, "setPendIntent error", e);
                }
            }
        }
        return remoteViews;
    }

    private boolean init = false;

    private void init(Context context) {
        if (init) return;
        init = true;
        if (notification_panel_width == 0) {
            Context systemUi = null;
            try {
                systemUi = context.createPackageContext(NotificationCompat.SYSTEM_UI_PKG, Context.CONTEXT_IGNORE_SECURITY);
            } catch (PackageManager.NameNotFoundException e) {
            }
            if (Build.VERSION.SDK_INT <= 19) {
                notification_side_padding = 0;
            } else {
                notification_side_padding = getDimem(context, systemUi, "notification_side_padding",
                        R.dimen.notification_side_padding);
            }
            notification_panel_width = getDimem(context, systemUi, "notification_panel_width",
                    R.dimen.notification_panel_width);
            if (notification_panel_width <= 0) {
                notification_panel_width = context.getResources().getDisplayMetrics().widthPixels;
            }
            notification_min_height = getDimem(context, systemUi, "notification_min_height",
                    R.dimen.notification_min_height);
            if(BuildCompat.isPie() && BuildCompat.isEMUI()){
                int height = VirtualCore.get().getContext().getResources().getDimensionPixelSize(R.dimen.notification_min_height);
                if(height > 0){
                    if (DEBUG) {
                        VLog.i(TAG, "notification_min_height2=" + height);
                    }
                    notification_min_height = height;
                }
            }
            if (DEBUG) {
                VLog.i(TAG, "notification_min_height=" + notification_min_height);
            }
            // getDimem(context, systemUi, "notification_row_min_height", 0);
            // if (notification_min_height == 0) {
            // notification_min_height =
            // }
            notification_max_height = getDimem(context, systemUi, "notification_max_height",
                    R.dimen.notification_max_height);
            notification_mid_height = getDimem(context, systemUi, "notification_mid_height",
                    R.dimen.notification_mid_height);
            notification_padding = getDimem(context, systemUi, "notification_padding", R.dimen.notification_padding);
            // notification_collapse_second_card_padding
        }
    }

    private int getDimem(Context context, Context sysContext, String name, int defId) {
        if (sysContext != null) {
            int id = sysContext.getResources().getIdentifier(name, "dimen", NotificationCompat.SYSTEM_UI_PKG);
            if (id != 0) {
                try {
                    float v = sysContext.getResources().getDimension(id);
                    VLog.v(TAG, "getDimension="+v);
                    return Math.round(v);
                } catch (Exception e) {

                }
            }
        }
        // VLog.w(TAG, "use my dimen:" + name);
        return defId == 0 ? 0 : Math.round(context.getResources().getDimension(defId));
    }

}
