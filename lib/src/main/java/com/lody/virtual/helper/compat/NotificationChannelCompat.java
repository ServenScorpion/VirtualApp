package com.lody.virtual.helper.compat;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.helper.utils.VLog;

import java.util.List;

public class NotificationChannelCompat {
    /**
     * va服务进程
     */
    public static final String DAEMON_ID = Constants.NOTIFICATION_DAEMON_CHANNEL;
    /**
     * 内部应用通知栏
     */
    public static final String DEFAULT_ID = Constants.NOTIFICATION_CHANNEL;
    /**
     * 来电，短信通知
     */
    public static final String LIGHT_ID = Constants.NOTIFICATION_LIGHT_CHANNEL;
    /**
     * 闹钟等内部系统应用通知
     */
    public static final String SYSTEM_ID = Constants.NOTIFICATION_SYSTEM_CHANNEL;
    /**
     * 内部应用通知栏
     */
    public static final String GROUP_APP = Constants.NOTIFICATION_GROUP_APP;
    /**
     * 闹钟等内部系统应用通知
     */
    public static final String GROUP_SYSTEM = Constants.NOTIFICATION_GROUP_SYSTEM;
    /**
     * 来电，短信通知
     */
    public static final String GROUP_PHONE = Constants.NOTIFICATION_GROUP_PHONE;
    /**
     * va服务进程
     */
    public static final String GROUP_DAEMON = Constants.NOTIFICATION_GROUP_DAEMON;

    public static boolean enable(){
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
            return VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.O;
        }
        return false;
    }

    public static boolean isVAChannel(String name){
        return DAEMON_ID.equals(name) || DEFAULT_ID.equals(name) || LIGHT_ID.equals(name) || SYSTEM_ID.equals(name);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void checkOrCreateGroup(Context context, String groupId, String name) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            List<NotificationChannelGroup> groups = manager.getNotificationChannelGroups();
            NotificationChannelGroup old = null;
            if (groups != null) {
                for (NotificationChannelGroup group : groups) {
                    if (TextUtils.equals(group.getId(), groupId)) {
                        old = group;
                        break;
                    }
                }
            }
            if (old == null) {
                old = new NotificationChannelGroup(groupId, name);
                try {
                    manager.createNotificationChannelGroup(old);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void checkOrCreateChannel(Context context, String channelId, String name) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = manager.getNotificationChannel(channelId);
            if (channel == null) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                if (LIGHT_ID.equals(channelId) || SYSTEM_ID.equals(channelId)) {
                    //通话，短信，闹钟
                    //通知发出的时候，会弹小窗口
                    importance = NotificationManager.IMPORTANCE_HIGH;
                } else if (channelId.equals(DAEMON_ID)) {
                    //不需要弹小窗口
                    importance = NotificationManager.IMPORTANCE_LOW;
                }
                channel = new NotificationChannel(channelId, name, importance);
                //尽量默认设置
                channel.setDescription("Compatibility of old versions");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                if (channelId.equals(LIGHT_ID)) {
                    //通知栏使用默认铃声
                    Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    channel.setSound(sound, Notification.AUDIO_ATTRIBUTES_DEFAULT);
                    channel.enableVibration(true);
                    channel.enableLights(true);
                    channel.setGroup(GROUP_PHONE);
                } else if (channelId.equals(DAEMON_ID)) {
                    channel.setGroup(GROUP_DAEMON);
                    //静音
                    channel.setSound(null, null);
                    channel.setShowBadge(false);
                    channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                    channel.setImportance(NotificationManager.IMPORTANCE_LOW);
                } else if (channelId.equals(DEFAULT_ID)) {
                    channel.setGroup(GROUP_APP);
                } else if (channelId.equals(SYSTEM_ID)) {
                    channel.setGroup(GROUP_SYSTEM);
                }
                try {
                    VLog.d("kk", "create channel %s ", channelId);
                    manager.createNotificationChannel(channel);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            } else {
                //更新已经存在的channel
                if (channelId.equals(LIGHT_ID)) {
                    if (GROUP_PHONE.equals(channel.getGroup())) {
                        return;
                    }
                    channel.setGroup(GROUP_PHONE);
                } else if (channelId.equals(DAEMON_ID)) {
                    if (GROUP_DAEMON.equals(channel.getGroup()) && channel.getImportance() == NotificationManager.IMPORTANCE_LOW) {
                        return;
                    }
                    channel.setGroup(GROUP_DAEMON);
                    channel.setImportance(NotificationManager.IMPORTANCE_LOW);
                } else if (channelId.equals(DEFAULT_ID)) {
                    if (GROUP_APP.equals(channel.getGroup()) && channel.getImportance() == NotificationManager.IMPORTANCE_DEFAULT) {
                        return;
                    }
                    channel.setGroup(GROUP_APP);
                    channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
                } else if (channelId.equals(SYSTEM_ID)) {
                    if (GROUP_SYSTEM.equals(channel.getGroup())) {
                        return;
                    }
                    //NotificationManager.IMPORTANCE_HIGH
                    channel.setGroup(GROUP_SYSTEM);
                } else {
                    return;
                }
                VLog.i("kk", "update channel %s ", channelId);
                try {
                    manager.deleteNotificationChannel(channelId);
                    manager.createNotificationChannel(channel);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Notification.Builder createBuilder(Context context, String channelId){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                 && VirtualCore.get().getTargetSdkVersion() >= android.os.Build.VERSION_CODES.O) {
            return new Notification.Builder(context, channelId);
        }else{
            return new Notification.Builder(context);
        }
    }
}
