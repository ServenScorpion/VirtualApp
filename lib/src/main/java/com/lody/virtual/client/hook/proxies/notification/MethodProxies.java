package com.lody.virtual.client.hook.proxies.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.annotations.SkipInject;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.VNotificationManager;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.compat.ParceledListSliceCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.ComponentUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import mirror.android.content.pm.ParceledListSlice;

/**
 * @author Lody
 */

@SuppressWarnings("unused")
class MethodProxies {
    //region channel/group
    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class CreateNotificationChannelGroups extends ReplaceCallingPkgMethodProxy {
        public CreateNotificationChannelGroups() {
            super("createNotificationChannelGroups");
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            if (args.length > 1 && ParceledListSliceCompat.isParceledListSlice(args[1])) {
                List<NotificationChannelGroup> list = (List<NotificationChannelGroup>) ParceledListSlice.getList.call(args[1]);
                List<NotificationChannelGroup> newList = new ArrayList<>();
                for (NotificationChannelGroup old : list) {
                    String id = VNotificationManager.get().dealNotificationGroup(old.getId(), getAppPkg(), getAppUserId());
                    NotificationChannelGroup group = new NotificationChannelGroup(id, old.getName());
                    newList.add(group);
                }
                args[1] = ParceledListSliceCompat.create(newList);
            }
            return super.beforeCall(who, method, args);
        }
    }

    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class DeleteNotificationChannelGroup extends ReplaceCallingPkgMethodProxy {
        public DeleteNotificationChannelGroup() {
            super("deleteNotificationChannelGroup");
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            if (args.length > 1 && args[1] instanceof String) {
                args[1] = VNotificationManager.get().dealNotificationGroup((String) args[1], getAppPkg(), getAppUserId());
            }
            return super.beforeCall(who, method, args);
        }
    }

    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class GetNotificationChannelGroup extends ReplaceCallingPkgMethodProxy{
        public GetNotificationChannelGroup() {
            super("getNotificationChannelGroup");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            if (!isAppPkg(pkg)) {
                return super.call(who, method, args);
            }
            args[0] = getHostPkg();
            if (args.length > 1 && args[1] instanceof String) {
                args[1] = VNotificationManager.get().dealNotificationGroup((String) args[1], getAppPkg(), getAppUserId());
            }
            Object res = super.call(who, method, args);
            if(res instanceof NotificationChannelGroup) {
                fixRealNotificationChannelGroup((NotificationChannelGroup)res, getAppPkg(), getAppUserId());
            }
            return res;
        }
    }

    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class GetNotificationChannelGroups extends ReplaceCallingPkgMethodProxy {
        public GetNotificationChannelGroups() {
            super("getNotificationChannelGroups");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Object ret = super.call(who, method, args);
            boolean slice = ParceledListSliceCompat.isReturnParceledListSlice(method);
            List<NotificationChannelGroup> list = slice ? ParceledListSlice.getList.call(ret)
                    : (List) ret;
            if (list != null) {
                for (int i = list.size() - 1; i >= 0; i--) {
                    NotificationChannelGroup old = list.get(i);
                    if (!VNotificationManager.get().checkNotificationGroup(old.getId(), getAppPkg(), getAppUserId())) {
                        list.remove(i);
                        continue;
                    }
                    fixRealNotificationChannelGroup(old, getAppPkg(), getAppUserId());
                }
            }
            if (slice) {
                return ParceledListSliceCompat.create(list);
            } else {
                return list;
            }
        }
    }

    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class CreateNotificationChannels extends ReplaceCallingPkgMethodProxy {
        public CreateNotificationChannels() {
            super("createNotificationChannels");
        }

        @Override
        public String getMethodName() {
            return super.getMethodName();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            if (args.length > 1 && ParceledListSliceCompat.isParceledListSlice(args[1])) {
                List<NotificationChannel> list = (List<NotificationChannel>) ParceledListSlice.getList.call(args[1]);
                List<NotificationChannel> newList = new ArrayList<>();
                for (NotificationChannel old : list) {
                    String id = VNotificationManager.get().dealNotificationChannel(old.getId(), getAppPkg(), getAppUserId());
                    if (mirror.android.app.NotificationChannel.mId == null) {
                        NotificationChannel channel = new NotificationChannel(id, old.getName(), old.getImportance());
                        if (old.getGroup() != null) {
                            String group = VNotificationManager.get().dealNotificationGroup(old.getGroup(), getAppPkg(), getAppUserId());
                            channel.setGroup(group);
                        }
                        channel.setBypassDnd(old.canBypassDnd());
                        channel.setDescription(old.getDescription());
                        channel.setLightColor(old.getLightColor());
                        channel.setLockscreenVisibility(old.getLockscreenVisibility());
                        channel.setShowBadge(old.canShowBadge());
                        channel.setSound(ComponentUtils.wrapperNotificationSoundUri(old.getSound(), getAppUserId()), old.getAudioAttributes());
                        channel.setVibrationPattern(old.getVibrationPattern());
                        newList.add(channel);
                    } else {
                        mirror.android.app.NotificationChannel.mId.set(old, id);
                        if (old.getGroup() != null) {
                            String group = VNotificationManager.get().dealNotificationGroup(old.getGroup(), getAppPkg(), getAppUserId());
                            old.setGroup(group);
                        }
                        newList.add(old);
                    }
                }
                args[1] = ParceledListSliceCompat.create(newList);
            }
            return super.beforeCall(who, method, args);
        }
    }

    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class GetNotificationChannels extends ReplaceCallingPkgMethodProxy {
        public GetNotificationChannels() {
            super("getNotificationChannels");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Object result = super.call(who, method, args);
            boolean slice = ParceledListSliceCompat.isReturnParceledListSlice(method);
            List<NotificationChannel> list = slice ? ParceledListSlice.getList.call(result)
                    : (List) result;
            if (list != null) {
                for (int i = list.size() - 1; i >= 0; i--) {
                    NotificationChannel old = list.get(i);
                    if (!VNotificationManager.get().checkNotificationChannel(old.getId(), getAppPkg(), getAppUserId())) {
                        list.remove(i);
                        continue;
                    }
                    fixRealNotificationChannel(old, getAppPkg(), getAppUserId());
                }
            }
            if (slice) {
                return ParceledListSliceCompat.create(list);
            } else {
                return list;
            }
        }
    }

    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class GetNotificationChannel extends ReplaceCallingPkgMethodProxy {
        public GetNotificationChannel() {
            super("getNotificationChannel");
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (BuildCompat.isQ() && args.length > 3) {
                //(String callingPkg, int userId, String pkg, String channelId);
                if (args[2] instanceof String && isAppPkg((String)args[2])) {
                    args[2] = getHostPkg();
                }
                if (args[3] instanceof String) {
                    args[3] = VNotificationManager.get().dealNotificationChannel((String) args[3], getAppPkg(), getAppUserId());
                }
            } else {
                if (args.length > 1 && args[1] instanceof String) {
                    args[1] = VNotificationManager.get().dealNotificationChannel((String) args[1], getAppPkg(), getAppUserId());
                }
            }
            Object object = super.call(who, method, args);
            if (object instanceof NotificationChannel) {
                fixRealNotificationChannel((NotificationChannel) object, getAppPkg(), getAppUserId());
            }
            return object;
        }
    }

    @SkipInject
    @TargetApi(Build.VERSION_CODES.O)
    static class DeleteNotificationChannel extends ReplaceCallingPkgMethodProxy {
        public DeleteNotificationChannel() {
            super("deleteNotificationChannel");
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            if (args.length > 1 && args[1] instanceof String) {
                args[1] = VNotificationManager.get().dealNotificationChannel((String) args[1], getAppPkg(), getAppUserId());
            }
            return super.beforeCall(who, method, args);
        }
    }
    //endregion

    static class EnqueueNotification extends MethodProxy {

        @Override
        public String getMethodName() {
            return "enqueueNotification";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            if (getHostPkg().equals(pkg)) {
                return method.invoke(who, args);
            }
            int notificationIndex = ArrayUtils.indexOfFirst(args, Notification.class);
            int idIndex = ArrayUtils.indexOfFirst(args, Integer.class);
            int id = (int) args[idIndex];
            id = VNotificationManager.get().dealNotificationId(id, pkg, null, getAppUserId());
            args[idIndex] = id;
            Notification notification = (Notification) args[notificationIndex];

            VNotificationManager.Result result = VNotificationManager.get().dealNotification(id, notification, pkg, getAppUserId());
            if (result.mode == VNotificationManager.MODE_NONE) {
                return 0;
            } else if (result.mode == VNotificationManager.MODE_REPLACED) {
                args[notificationIndex] = result.notification;
            }

            VNotificationManager.get().addNotification(id, null, pkg, getAppUserId());
            args[0] = getHostPkg();
            return method.invoke(who, args);
        }
    }

    /* package */ static class EnqueueNotificationWithTag extends MethodProxy {

        @Override
        public String getMethodName() {
            return "enqueueNotificationWithTag";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            //TODO geyao
            String pkg = (String) args[0];
            if (getHostPkg().equals(pkg)) {
                return method.invoke(who, args);
            }
            int notificationIndex = ArrayUtils.indexOfFirst(args, Notification.class);
            int idIndex = ArrayUtils.indexOfFirst(args, Integer.class);
            int tagIndex = (Build.VERSION.SDK_INT >= 18 ? 2 : 1);
            int id = (int) args[idIndex];
            String tag = (String) args[tagIndex];

            id = VNotificationManager.get().dealNotificationId(id, pkg, tag, getAppUserId());
            tag = VNotificationManager.get().dealNotificationTag(id, pkg, tag, getAppUserId());
            args[idIndex] = id;
            args[tagIndex] = tag;
            //key(tag,id)
            Notification notification = (Notification) args[notificationIndex];
            VNotificationManager.Result result = VNotificationManager.get().dealNotification(id, notification, pkg, getAppUserId());
            if (result.mode == VNotificationManager.MODE_NONE) {
                return 0;
            } else if (result.mode == VNotificationManager.MODE_REPLACED) {
                args[notificationIndex] = result.notification;
            }

            VNotificationManager.get().addNotification(id, tag, pkg, getAppUserId());
            args[0] = getHostPkg();
            if (Build.VERSION.SDK_INT >= 18 && args[1] instanceof String) {
                args[1] = getHostPkg();
            }
            return method.invoke(who, args);
        }
    }

    /* package */ static class EnqueueNotificationWithTagPriority extends EnqueueNotificationWithTag {

        @Override
        public String getMethodName() {
            return "enqueueNotificationWithTagPriority";
        }
    }

    /* package */ static class CancelNotificationWithTag extends MethodProxy {

        @Override
        public String getMethodName() {
            return "cancelNotificationWithTag";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String packageName = (String) args[0];
            String pkg = MethodParameterUtils.replaceFirstAppPkg(args);
            if (getHostPkg().equals(pkg)) {
                return method.invoke(who, args);
            }
            String tag = (String) args[1];
            int id = (int) args[2];
            id = VNotificationManager.get().dealNotificationId(id, pkg, tag, getAppUserId());
            tag = VNotificationManager.get().dealNotificationTag(id, pkg, tag, getAppUserId());
            args[1] = tag;
            args[2] = id;
            if (VirtualCore.get().isAppInstalled(pkg)) {
                VNotificationManager.get().cancelNotification(packageName, tag, id, getAppUserId());
                return 0;
            }
            return method.invoke(who, args);
        }
    }

    /**
     * @author Lody
     */
    /* package */ static class CancelAllNotifications extends MethodProxy {

        @Override
        public String getMethodName() {
            return "cancelAllNotifications";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = MethodParameterUtils.replaceFirstAppPkg(args);
            if (VirtualCore.get().isAppInstalled(pkg)) {
                VNotificationManager.get().cancelAllNotification(pkg, getAppUserId());
                return 0;
            }
            return method.invoke(who, args);
        }
    }

    static class AreNotificationsEnabledForPackage extends MethodProxy {
        @Override
        public String getMethodName() {
            return "areNotificationsEnabledForPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            if (getHostPkg().equals(pkg)) {
                return method.invoke(who, args);
            }
            return VNotificationManager.get().areNotificationsEnabledForPackage(pkg, getAppUserId());
        }
    }

    static class SetNotificationsEnabledForPackage extends MethodProxy {
        @Override
        public String getMethodName() {
            return "setNotificationsEnabledForPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            if (getHostPkg().equals(pkg)) {
                return method.invoke(who, args);
            }
            int enableIndex = ArrayUtils.indexOfFirst(args, Boolean.class);
            boolean enable = (boolean) args[enableIndex];
            VNotificationManager.get().setNotificationsEnabledForPackage(pkg, enable, getAppUserId());
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    static class GetAppActiveNotifications extends MethodProxy {
        @Override
        public String getMethodName() {
            return "getAppActiveNotifications";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            args[0] = getHostPkg();
            if (args.length > 1) {
                args[1] = 0;
            }
            Object list = super.call(who, method, args);
            boolean slice = ParceledListSliceCompat.isReturnParceledListSlice(method);
            List<StatusBarNotification> resultList = slice ? ParceledListSlice.getList.call(list)
                    : (List) list;
            if (resultList != null) {
                for (int i = resultList.size() - 1; i >= 0; i--) {
                    StatusBarNotification value = resultList.get(i);
                    if (!VNotificationManager.get().checkNotificationTag(value.getTag(), getAppPkg(), getAppUserId())) {
                        resultList.remove(i);
                    } else {
                        fixRealStatusBarNotification(value, getAppPkg(), getAppUserId());
                    }
                }
            }
            if (slice) {
                return ParceledListSliceCompat.create(resultList);
            } else {
                return resultList;
            }
        }
    }

    //region fix real
    private static void fixRealStatusBarNotification(StatusBarNotification value, String packageName, int userId) {
        if (mirror.android.service.notification.StatusBarNotification.pkg != null) {
            mirror.android.service.notification.StatusBarNotification.pkg.set(value, packageName);
        }
        if (mirror.android.service.notification.StatusBarNotification.opPkg != null) {
            mirror.android.service.notification.StatusBarNotification.opPkg.set(value, packageName);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (mirror.android.service.notification.StatusBarNotification.id != null) {
                int id = VNotificationManager.get().dealNotificationId(value.getId(), packageName, value.getTag(), userId);
                mirror.android.service.notification.StatusBarNotification.id.set(value, id);
            }
            if (mirror.android.service.notification.StatusBarNotification.tag != null) {
                String tag = VNotificationManager.get().getRealNotificationTag(value.getTag(), packageName, userId);
                mirror.android.service.notification.StatusBarNotification.tag.set(value, tag);
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Notification notification = value.getNotification();
            fixRealNotification(notification, packageName, userId);
        }
    }

    private static void fixRealNotification(Notification notification, String packageName, int userId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (mirror.android.app.NotificationO.mChannelId != null) {
                String channel = VNotificationManager.get().getRealNotificationChannel(notification.getChannelId(), packageName, userId);
                mirror.android.app.NotificationO.mChannelId.set(notification, channel);
            }
            if(notification.getGroup() != null) {
                if (mirror.android.app.NotificationO.mGroupKey != null) {
                    String group = VNotificationManager.get().getRealNotificationGroup(notification.getGroup(), packageName, userId);
                    mirror.android.app.NotificationO.mGroupKey.set(notification, group);
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static void fixRealNotificationChannelGroup(NotificationChannelGroup group, String packageName, int userId) {
        if (mirror.android.app.NotificationChannelGroup.mId != null) {
            String id = VNotificationManager.get().getRealNotificationGroup(group.getId(), packageName, userId);
            mirror.android.app.NotificationChannelGroup.mId.set(group, id);
        }
        if (mirror.android.app.NotificationChannelGroup.mChannels != null) {
            List<NotificationChannel> channels = mirror.android.app.NotificationChannelGroup.mChannels.get(group);
            if (channels != null) {
                for (NotificationChannel channel : channels) {
                    fixRealNotificationChannel(channel, packageName, userId);
                }
            }
        }
    }

    private static void fixRealNotificationChannel(NotificationChannel channel, String packageName, int userId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (mirror.android.app.NotificationChannel.mId != null) {
                String id = VNotificationManager.get().getRealNotificationChannel(channel.getId(), packageName, userId);
                mirror.android.app.NotificationChannel.mId.set(channel, id);
            }
            if(channel.getGroup() != null) {
                String group = VNotificationManager.get().getRealNotificationGroup(channel.getGroup(), packageName, userId);
                channel.setGroup(group);
            }
        }
    }
    //endregion
}
