package com.lody.virtual.server.am;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.collection.SparseArray;
import com.lody.virtual.helper.compat.ObjectsCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.ClassUtils;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.AppTaskInfo;
import com.lody.virtual.remote.StubActivityRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import mirror.android.app.ActivityManagerNative;
import mirror.com.android.internal.R_Hide;

import static android.content.pm.ActivityInfo.LAUNCH_MULTIPLE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_TOP;

/**
 * @author Lody
 */

/* package */ class ActivityStack {
    private static final String TAG = "ActivityStack";
    private final ActivityManager mAM;
    private final VActivityManagerService mService;

    /**
     * [Key] = TaskId [Value] = TaskRecord
     */
    private final SparseArray<TaskRecord> mHistory = new SparseArray<>();
    private final List<ActivityRecord> mLaunchingActivities = new ArrayList<>();
    private final Map<ActivityInfo, IBinder> mExcludeRecentActivityRecord = new HashMap<>();
    private final Map<ActivityRecord, LaunchingActivity> pendingNewIntents = new ConcurrentHashMap<>();

    ActivityStack(VActivityManagerService mService) {
        this.mService = mService;
        mAM = (ActivityManager) VirtualCore.get().getContext().getSystemService(Context.ACTIVITY_SERVICE);
    }

    private static void removeFlags(Intent intent, int flags) {
        intent.setFlags(intent.getFlags() & ~flags);
    }

    private static boolean containFlags(Intent intent, int flags) {
        return (intent.getFlags() & flags) != 0;
    }

    private void deliverNewIntentLocked(int userId, ActivityRecord sourceRecord, ActivityRecord targetRecord, Intent intent) {
        if (targetRecord == null) {
            return;
        }
        String creator = getCallingPackage(userId, sourceRecord);
        if (creator == null) {
            creator = "android";
        }
        try {
            targetRecord.process.client.scheduleNewIntent(creator, targetRecord.token, intent);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    private TaskRecord findTaskByAffinityLocked(int userId, String affinity) {
        for (int i = 0; i < this.mHistory.size(); i++) {
            TaskRecord r = this.mHistory.valueAt(i);
            if (userId == r.userId && affinity.equals(r.affinity)) {
                return r;
            }
        }
        return null;
    }

    private TaskRecord findTaskByIntentLocked(int userId, Intent intent) {
        for (int i = 0; i < this.mHistory.size(); i++) {
            TaskRecord r = this.mHistory.valueAt(i);
            if (userId == r.userId && r.taskRoot != null
                    && ObjectsCompat.equals(intent.getComponent(), r.taskRoot.getComponent())) {
                return r;
            }
        }
        return null;
    }

    private ActivityRecord findActivityByToken(int userId, IBinder token) {
        ActivityRecord target = null;
        if (token != null) {
            for (int i = 0; i < this.mHistory.size(); i++) {
                TaskRecord task = this.mHistory.valueAt(i);
                if (task.userId != userId) {
                    continue;
                }
                synchronized (task.activities) {
                    for (ActivityRecord r : task.activities) {
                        if (r.token == token) {
                            target = r;
                        }
                    }
                }
            }
        }
        return target;
    }

    /**
     * App started in VA may be removed in OverView screen, then AMS.removeTask
     * will be invoked, all data struct about the task in AMS are released,
     * while the client's process is still alive. So remove related data in VA
     * as well. A new TaskRecord will be recreated in `onActivityCreated`
     */
    private void optimizeTasksLocked() {
        List<ActivityManager.RecentTaskInfo> recentTask = VirtualCore.get().getAppTasksEx();
        int N = mHistory.size();
        while (N-- > 0) {
            TaskRecord task = mHistory.valueAt(N);
            ListIterator<ActivityManager.RecentTaskInfo> iterator = recentTask.listIterator();
            boolean taskAlive = false;
            while (iterator.hasNext()) {
                ActivityManager.RecentTaskInfo info = iterator.next();
                if (info.id == task.taskId) {
                    taskAlive = true;
                    iterator.remove();
                    break;
                }
            }
            if (!taskAlive) {
                mHistory.removeAt(N);
            }
        }
    }

    void finishAllActivities(){
        synchronized (mHistory) {
            int N = mHistory.size();
            while (N-- > 0) {
                TaskRecord task = mHistory.valueAt(N);
                synchronized (task.activities) {
                    for (ActivityRecord r : task.activities) {
                        Log.e("wxd", " finishActivity : " + r.component);
                        try {
                            r.process.client.finishActivity(r.token);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    //xdja
    void finishAllActivity(ProcessRecord record) {
        synchronized (mHistory) {
            int N = mHistory.size();
            while (N-- > 0) {
                TaskRecord task = mHistory.valueAt(N);
                synchronized (task.activities) {
                    for (ActivityRecord r : task.activities) {
                        if (r.process.pid == record.pid) {
                            Log.e("wxd", " finishActivity : " + r.component);
                            try {
                                r.process.client.finishActivity(r.token);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }


    int startActivitiesLocked(int userId, Intent[] intents, ActivityInfo[] infos, String[] resolvedTypes, IBinder resultTo, Bundle options, int callingUid, int callingPid) {
        for (int i = 0; i < intents.length; i++) {
            startActivityLocked(userId, intents[i], infos[i], resultTo, options, null, 0, callingUid, callingPid);
        }
        return 0;
    }


    private boolean isAllowUseSourceTask(ActivityRecord source, ActivityInfo info, int userId, String affinity) {
        if (source == null) {
            return false;
        }
        if (source.info.launchMode == ActivityInfo.LAUNCH_SINGLE_INSTANCE) {
            return false;
        }
        //xdja LAUNCH_SINGLE_TASK模式需要对比taskAffinity，如果affinity不同需要newTask，例如wps编辑界面
        if(info.launchMode == LAUNCH_SINGLE_TASK){
            if(findTaskByAffinityLocked(userId, affinity) == null){
                return false;
            }
        }
        return true;
    }

    int startActivityLocked(int userId, Intent intent, ActivityInfo info, IBinder resultTo, Bundle options,
                            String resultWho, int requestCode, int callingUid, int callingPid) {
        synchronized (mHistory) {
            optimizeTasksLocked();
        }
        ActivityRecord sourceRecord = findActivityByToken(userId, resultTo);
        if (sourceRecord == null) {
            resultTo = null;
        }
        String affinity = ComponentUtils.getTaskAffinity(info);
        int mLauncherFlags = 0;
        boolean newTask = containFlags(intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        boolean clearTop = containFlags(intent, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        boolean clearTask = containFlags(intent, Intent.FLAG_ACTIVITY_CLEAR_TASK);
        boolean multipleTask = newTask && containFlags(intent, Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        boolean reorderToFront = containFlags(intent, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        boolean singleTop = containFlags(intent, Intent.FLAG_ACTIVITY_SINGLE_TOP);
        boolean alwaysRetainTaskState = (info.flags & ActivityInfo.FLAG_ALWAYS_RETAIN_TASK_STATE) != 0;

        if ((info.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) != 0
                || containFlags(intent, Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)) {
            if (info.packageName.equals(InstallerSetting.MDM_CLIENT_PKG) &&
            info.name.contains("ForceControlActivity")) {
                synchronized (mExcludeRecentActivityRecord) {
                    if (!mExcludeRecentActivityRecord.containsKey(info)) {
                        mExcludeRecentActivityRecord.put(info, null);
                    }
                }
            } else {
                mLauncherFlags |= Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
            }
        }

        boolean notStartToFront = false;
        if (clearTop || singleTop || clearTask) {
            notStartToFront = true;
        }
        if (!newTask) {
            clearTask = false;
        }
        TaskRecord sourceTask = null;
        if (sourceRecord != null) {
            sourceTask = sourceRecord.task;
        }

        TaskRecord reuseTask = null;
        if (!multipleTask) {
            switch (info.launchMode) {
                case LAUNCH_SINGLE_INSTANCE: {
                    reuseTask = findTaskByAffinityLocked(userId, affinity);
                    break;
                }
                case LAUNCH_SINGLE_TASK:
                case LAUNCH_MULTIPLE:
                case LAUNCH_SINGLE_TOP: {
                    if (newTask || sourceTask == null) {
                        reuseTask = findTaskByAffinityLocked(userId, affinity);
                    } else if (isAllowUseSourceTask(sourceRecord, info, userId, affinity)) {
                        if ((info.launchMode == LAUNCH_SINGLE_TASK) &&
                                (sourceRecord.task != null) &&
                                (!sourceRecord.task.affinity.equals(affinity))) {
                            reuseTask = findTaskByAffinityLocked(userId, affinity);
                        } else {
                            reuseTask = sourceTask;
                        }
                    }
                    break;
                }

            }
        }
        if (reuseTask == null || reuseTask.isFinishing()) {
            return startActivityInNewTaskLocked(mLauncherFlags, userId, intent, info, options, callingUid, callingPid);
        }
        mAM.moveTaskToFront(reuseTask.taskId, 0);

        /*
         * 一个APP的界面已经打开，我们按Home，再从桌面打开App，不会重新启动App的界面，
         * 而是直接仅仅把界面切到前台。
         *
         */
        boolean startTaskToFront = !notStartToFront
                && ComponentUtils.intentFilterEquals(reuseTask.taskRoot, intent)
                && reuseTask.taskRoot.getFlags() == intent.getFlags();

        ActivityRecord notifyNewIntentActivityRecord = null;
        boolean marked = false;
        ComponentName component = ComponentUtils.toComponentName(info);
        if (info.launchMode == LAUNCH_SINGLE_INSTANCE) {
            synchronized (reuseTask.activities) {
                for (ActivityRecord r : reuseTask.activities) {
                    if (r.component.equals(component)) {
                        notifyNewIntentActivityRecord = r;
                        break;
                    }
                }
            }
        }
        boolean notReorderToFront = false;
        if (info.launchMode == LAUNCH_SINGLE_TASK || clearTop) {
            synchronized (reuseTask.activities) {
                notReorderToFront = true;
                /*
                 * (1）如果当前task包含这个Activity，这个Activity以上的Activity出栈，这个Activity到达栈顶。
                 */
                int N = reuseTask.activities.size();
                while (N-- > 0) {
                    ActivityRecord r = reuseTask.activities.get(N);
                    if (!r.marked && r.component.equals(component)) {
                        notifyNewIntentActivityRecord = r;
                        marked = true;
                        break;
                    }
                }

                if (marked) {
                    while (N++ < reuseTask.activities.size() - 1) {
                        reuseTask.activities.get(N).marked = true;
                    }
                    /*
                     *  处理 ClearTop:
                     * （2）如果这个Activity是standard模式，这个Activity也出栈，并且重新实例化到达栈顶。
                     */
                    if (clearTop && info.launchMode == LAUNCH_MULTIPLE) {
                        notifyNewIntentActivityRecord.marked = true;
                        notifyNewIntentActivityRecord = null;
                    }
                }
            }
        }
        ActivityRecord pendingActivityRecorder = null;
        if (info.launchMode == LAUNCH_SINGLE_TOP || singleTop) {
            notReorderToFront = true;
            /*
             * 打开的Activity如果在栈顶，则不创建新的实例，并且会触发onNewIntent事件。
             */
            ActivityRecord top = reuseTask.getTopActivityRecord();
            if (top != null && !top.marked && top.component.equals(component)) {
                notifyNewIntentActivityRecord = top;
            } else {
                // mark starting activity
                synchronized (mLaunchingActivities) {
                    for (ActivityRecord launchingActivity : mLaunchingActivities) {
                        if (!launchingActivity.marked && launchingActivity.component.equals(component)) {
                            pendingActivityRecorder = launchingActivity;
                            break;
                        }
                    }
                }
            }
        }
        if (reorderToFront) {
            ActivityRecord top = reuseTask.getTopActivityRecord();
            if (top.component.equals(component)) {
                notifyNewIntentActivityRecord = top;
            } else {
                /*
                 * 由于无法直接实现将要启动的Activity从栈中拉到栈顶，
                 * 我们直接将它finish掉，并在栈顶重新启动。
                 * 然而，某些Activity不能这样做（典例：网易新闻分享到微博然后点取消）
                 * 好在还可以workaround之。
                 */
                synchronized (reuseTask.activities) {
                    int N = reuseTask.activities.size();
                    while (N-- > 0) {
                        ActivityRecord r = reuseTask.activities.get(N);
                        if (r.component.equals(component)) {
                            if (notReorderToFront) {
                                notifyNewIntentActivityRecord = r;
                            } else {
                                r.marked = true;
                                marked = true;
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (clearTask) {
            synchronized (reuseTask.activities) {
                for (ActivityRecord r : reuseTask.activities) {
                    r.marked = true;
                }
            }
            marked = true;
        }
        if (marked) {
            finishMarkedActivity();
        }
        if (notifyNewIntentActivityRecord != null) {
            deliverNewIntentLocked(userId, sourceRecord, notifyNewIntentActivityRecord, intent);
            if (!notifyNewIntentActivityRecord.marked) {
                return 0;
            }
        } else if (pendingActivityRecorder != null) {
            if (sourceRecord == null) {
                sourceRecord = pendingActivityRecorder;
            }
            synchronized (mLaunchingActivities) {
                LaunchingActivity launchingActivity = pendingNewIntents.get(sourceRecord);
                if (launchingActivity == null) {
                    launchingActivity = new LaunchingActivity(pendingActivityRecorder.component);
                    pendingNewIntents.put(sourceRecord, launchingActivity);
                }
                launchingActivity.pendingNewIntents.add(new PendingNewIntent(userId, sourceRecord, intent));
            }
            return 0;
        }
        if (startTaskToFront) {
            return 0;
        }
        ActivityRecord targetRecord = newActivityRecord(intent, info, resultTo, userId);
        Intent destIntent = startActivityProcess(userId, targetRecord, intent, info, callingUid, callingPid);

        if (destIntent != null) {
            destIntent.addFlags(mLauncherFlags);
            ActivityRecord startFrom;
            if (sourceTask == reuseTask) {
                startFrom = sourceRecord;
            } else {
                startFrom = reuseTask.getTopActivityRecord(true);
            }
            startActivityFromSourceTask(startFrom.process, startFrom.token, destIntent, resultWho, requestCode, options);
            return 0;
        } else {
            synchronized (mLaunchingActivities) {
                if (mLaunchingActivities.remove(targetRecord)) {
                    pendingNewIntents.remove(targetRecord);
                }
            }
            return -1;
        }
    }

    ActivityRecord newActivityRecord(Intent intent, ActivityInfo info, IBinder resultTo, int userId) {
        ActivityRecord targetRecord = new ActivityRecord(intent, info, resultTo, userId);
        synchronized (mLaunchingActivities) {
            mLaunchingActivities.add(targetRecord);
        }
        return targetRecord;
    }


    private int startActivityInNewTaskLocked(int launcherFlags, final int userId, Intent intent, final ActivityInfo info, final Bundle options, int callingUid, int callingPid) {
        ActivityRecord targetRecord = newActivityRecord(intent, info, null, userId);
        final Intent destIntent = startActivityProcess(userId, targetRecord, intent, info, callingUid, callingPid);
        if (destIntent != null) {
            destIntent.addFlags(launcherFlags);
            destIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            destIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            destIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // noinspection deprecation
                destIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            } else {
                destIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            }
            if (options != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                VirtualCore.get().getContext().startActivity(destIntent, options);
            } else {
                VirtualCore.get().getContext().startActivity(destIntent);
            }
            return 0;
        } else {
            if (mLaunchingActivities.remove(targetRecord)) {
                pendingNewIntents.remove(targetRecord);
            }
            return -1;
        }

    }

    private void finishMarkedActivity() {
        synchronized (mHistory) {
            int N = mHistory.size();
            while (N-- > 0) {
                final TaskRecord task = mHistory.valueAt(N);
                synchronized (task.activities) {
                    for (ActivityRecord r : task.activities) {
                        if (!r.marked) {
                            continue;
                        }
                        try {
                            r.process.client.finishActivity(r.token);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public boolean finishActivityAffinity(int userId, IBinder token) {
        synchronized (mHistory) {
            ActivityRecord r = findActivityByToken(userId, token);
            if (r == null) {
                return false;
            }
            String taskAffinity = ComponentUtils.getTaskAffinity(r.info);
            synchronized (r.task.activities) {
                for (int index = r.task.activities.indexOf(r); index >= 0; --index) {
                    ActivityRecord cur = r.task.activities.get(index);
                    if (!ComponentUtils.getTaskAffinity(cur.info).equals(taskAffinity)) {
                        break;
                    }
                    cur.marked = true;
                }
            }
        }
        finishMarkedActivity();
        return false;
    }

    private void startActivityFromSourceTask(ProcessRecord r, final IBinder resultTo, final Intent intent, final String resultWho,
                                             final int requestCode, final Bundle options) {
        realStartActivityLocked(r.appThread, resultTo, intent, resultWho, requestCode, options);
    }


    private void realStartActivityLocked(IInterface appThread, IBinder resultTo, Intent intent, String resultWho, int requestCode,
                                         Bundle options) {
        Class<?>[] types = mirror.android.app.IActivityManager.startActivity.paramList();
        Object[] args = new Object[types.length];
        args[0] = appThread;
        int intentIndex = ArrayUtils.protoIndexOf(types, Intent.class);
        int resultToIndex = ArrayUtils.protoIndexOf(types, IBinder.class, 2);
        int optionsIndex = ArrayUtils.protoIndexOf(types, Bundle.class);
        int resolvedTypeIndex = intentIndex + 1;
        int resultWhoIndex = resultToIndex + 1;
        int requestCodeIndex = resultToIndex + 2;

        args[intentIndex] = intent;
        args[resultToIndex] = resultTo;
        args[resultWhoIndex] = resultWho;
        args[requestCodeIndex] = requestCode;
        if (optionsIndex != -1) {
            args[optionsIndex] = options;
        }
        args[resolvedTypeIndex] = intent.getType();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            args[intentIndex - 1] = VirtualCore.get().getHostPkg();
        }
        ClassUtils.fixArgs(types, args);

        try {
            mirror.android.app.IActivityManager.startActivity.call(ActivityManagerNative.getDefault.call(),
                    (Object[]) args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String fetchStubActivity(int vpid, ActivityInfo targetInfo) {

        boolean isFloating = false;
        boolean isTranslucent = false;
        boolean showWallpaper = false;
        try {
            int[] R_Styleable_Window = R_Hide.styleable.Window.get();
            int R_Styleable_Window_windowIsTranslucent = R_Hide.styleable.Window_windowIsTranslucent.get();
            int R_Styleable_Window_windowIsFloating = R_Hide.styleable.Window_windowIsFloating.get();
            int R_Styleable_Window_windowShowWallpaper = R_Hide.styleable.Window_windowShowWallpaper.get();

            AttributeCache.Entry ent = AttributeCache.instance().get(targetInfo.packageName, targetInfo.theme,
                    R_Styleable_Window);
            if (ent != null && ent.array != null) {
                showWallpaper = ent.array.getBoolean(R_Styleable_Window_windowShowWallpaper, false);
                isTranslucent = ent.array.getBoolean(R_Styleable_Window_windowIsTranslucent, false);
                isFloating = ent.array.getBoolean(R_Styleable_Window_windowIsFloating, false);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        boolean isDialogStyle = isFloating || isTranslucent || showWallpaper;
        if (isDialogStyle) {
            return StubManifest.getStubDialogName(vpid);
        } else {
            return StubManifest.getStubActivityName(vpid);
        }
    }

    private Intent startActivityProcess(int userId, ActivityRecord targetRecord, Intent intent, ActivityInfo info, int callingUid, int callingPid) {
        //TODO 以前是为了实现：A调用B，B结束后是返回桌面，不是返回A

        ProcessRecord targetApp = mService.startProcessIfNeedLocked(info.processName, userId, info.packageName, -1, callingUid, VActivityManager.PROCESS_TYPE_ACTIVITY);
        if (targetApp == null) {
            return null;
        }
        return getStartStubActivityIntentInner(intent, targetApp.is64bit, targetApp.vpid, userId, targetRecord, info);
    }

    Intent getStartStubActivityIntentInner(Intent intent, boolean is64bit, int vpid, int userId, ActivityRecord targetRecord, ActivityInfo info) {
        intent = new Intent(intent);
        Intent targetIntent = new Intent();
        targetIntent.setClassName(StubManifest.getStubPackageName(is64bit), fetchStubActivity(vpid, info));
        ComponentName component = intent.getComponent();
        if (component == null) {
            component = ComponentUtils.toComponentName(info);
        }
        targetIntent.setType(component.flattenToString());
        StubActivityRecord saveInstance = new StubActivityRecord(intent, info, userId, targetRecord);
        saveInstance.saveToIntent(targetIntent);
        return targetIntent;
    }


    void onActivityCreated(ProcessRecord targetApp, IBinder token, int taskId, ActivityRecord record) {
        LaunchingActivity launchingActivity = null;
        synchronized (mLaunchingActivities) {
            if (mLaunchingActivities.remove(record)) {
                launchingActivity = pendingNewIntents.remove(record);
            }
        }
        synchronized (mHistory) {
            optimizeTasksLocked();
            TaskRecord task = mHistory.get(taskId);
            if (task == null) {
                boolean excludeRecent = (record.info.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) != 0;
                task = new TaskRecord(taskId, record.userId, ComponentUtils.getTaskAffinity(record.info), record.intent, excludeRecent);
                mHistory.put(taskId, task);
                Intent intent = new Intent(Constants.ACTION_NEW_TASK_CREATED);
                intent.putExtra(Constants.EXTRA_USER_HANDLE, record.userId);
                intent.putExtra(Constants.EXTRA_PACKAGE_NAME, record.info.packageName);
                VirtualCore.get().getContext().sendBroadcast(intent);
            }


            record.init(task, targetApp, token);

            synchronized (mExcludeRecentActivityRecord) {
                if (mExcludeRecentActivityRecord.containsKey(record.info)) {
                    mExcludeRecentActivityRecord.put(record.info, token);
                }
            }

            synchronized (task.activities) {
                task.activities.add(record);
            }
        }
        if (launchingActivity != null && launchingActivity.Match(record.component)) {
            for (PendingNewIntent pendingNewIntent : launchingActivity.pendingNewIntents) {
                deliverNewIntentLocked(pendingNewIntent.userId, pendingNewIntent.sourceRecord, record, pendingNewIntent.intent);
            }
        }
    }

    void onActivityResumed(int userId, IBinder token) {
        synchronized (mHistory) {
            optimizeTasksLocked();
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                synchronized (r.task.activities) {
                    r.task.activities.remove(r);
                    r.task.activities.add(r);
                }
            }
        }
    }


    void onActivityFinish(int userId, IBinder token) {
        synchronized (mHistory) {
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                r.marked = true;
            }
        }
    }

    ActivityRecord onActivityDestroyed(int userId, IBinder token) {
        synchronized (mHistory) {
            optimizeTasksLocked();
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                r.marked = true;
                synchronized (r.task.activities) {
                    // We shouldn't remove task at this point,
                    // it will be removed by optimizeTasksLocked().
                    r.task.activities.remove(r);
                }
            }
            return r;
        }
    }

    void processDied(ProcessRecord record) {
        synchronized (mHistory) {
            optimizeTasksLocked();
            int N = mHistory.size();
            while (N-- > 0) {
                TaskRecord task = mHistory.valueAt(N);
                synchronized (task.activities) {
                    Iterator<ActivityRecord> iterator = task.activities.iterator();
                    while (iterator.hasNext()) {
                        ActivityRecord r = iterator.next();
                        if (r.process.pid != record.pid) {
                            continue;
                        }
                        iterator.remove();
                        if (task.activities.isEmpty()) {
                            mHistory.remove(task.taskId);
                        }
                    }
                }
            }

        }
    }

    String getPackageForToken(int userId, IBinder token) {
        synchronized (mHistory) {
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                return r.info.packageName;
            }
            return null;
        }
    }

    private ActivityRecord getCallingRecordLocked(int userId, IBinder token) {
        ActivityRecord r = findActivityByToken(userId, token);
        if (r == null) {
            return null;
        }
        return findActivityByToken(userId, r.resultTo);
    }

    ComponentName getCallingActivity(int userId, IBinder token) {
        ActivityRecord r = getCallingRecordLocked(userId, token);
        return r != null ? r.intent.getComponent() : null;
    }

    String getCallingPackage(int userId, IBinder token) {
        ActivityRecord r = getCallingRecordLocked(userId, token);
        return r != null ? r.info.packageName : null;
    }

    AppTaskInfo getTaskInfo(int taskId) {
        synchronized (mHistory) {
            TaskRecord task = mHistory.get(taskId);
            if (task != null) {
                return task.getAppTaskInfo();
            }
            return null;
        }
    }

    ComponentName getActivityClassForToken(int userId, IBinder token) {
        synchronized (mHistory) {
            ActivityRecord r = findActivityByToken(userId, token);
            if (r != null) {
                return r.component;
            }
            return null;
        }
    }

    boolean includeExcludeFromRecentsFlag(IBinder token){
        synchronized (mExcludeRecentActivityRecord) {
            Iterator<Map.Entry<ActivityInfo, IBinder>> entries = mExcludeRecentActivityRecord.entrySet().iterator();
            while(entries.hasNext()) {
                Map.Entry<ActivityInfo, IBinder> entry = entries.next();
                if (entry.getValue() != null && entry.getValue().equals(token)) {
                    VLog.d(TAG, entry.getKey().taskAffinity + " has excludeFromRecentTask flag");
                    entries.remove();
                    return true;
                }
            }
        }
        return false;
    }

}