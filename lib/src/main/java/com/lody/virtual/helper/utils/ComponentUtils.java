package com.lody.virtual.helper.utils;

import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.SpecialComponentList;
import com.lody.virtual.client.stub.ContentProviderProxy;
import com.lody.virtual.client.stub.ShadowPendingActivity;
import com.lody.virtual.client.stub.ShadowPendingReceiver;
import com.lody.virtual.client.stub.ShadowPendingService;
import com.lody.virtual.client.stub.StubManifest;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.IntentCompat;
import com.lody.virtual.helper.compat.ObjectsCompat;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.BroadcastIntentData;
import com.xdja.utils.Stirrer;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static android.content.ContentResolver.SCHEME_FILE;
import static android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE;
import static com.lody.virtual.client.env.SpecialComponentList.protectAction;

/**
 * @author Lody
 */
public class ComponentUtils {

    public static String getTaskAffinity(ActivityInfo info) {
        if (info.launchMode == LAUNCH_SINGLE_INSTANCE) {
            return "-SingleInstance-" + info.packageName + "/" + info.name;
        } else if (info.taskAffinity == null && info.applicationInfo.taskAffinity == null) {
            return info.packageName;
        } else if (info.taskAffinity != null) {
            return info.taskAffinity;
        }
        return info.applicationInfo.taskAffinity;
    }

    public static String getFirstAuthority(ProviderInfo info){
        if(info == null){
            return null;
        }
        String[] authorities = info.authority.split(";");
        return authorities.length == 0 ? info.authority : authorities[0];
    }

    public static boolean intentFilterEquals(Intent a, Intent b) {
        if (a != null && b != null) {
            if (!ObjectsCompat.equals(a.getAction(), b.getAction())) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getData(), b.getData())) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getType(), b.getType())) {
                return false;
            }
            Object pkgA = a.getPackage();
            if (pkgA == null && a.getComponent() != null) {
                pkgA = a.getComponent().getPackageName();
            }
            String pkgB = b.getPackage();
            if (pkgB == null && b.getComponent() != null) {
                pkgB = b.getComponent().getPackageName();
            }
            if (!ObjectsCompat.equals(pkgA, pkgB)) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getComponent(), b.getComponent())) {
                return false;
            }
            if (!ObjectsCompat.equals(a.getCategories(), b.getCategories())) {
                return false;
            }
        }
        return true;
    }

    public static String getProcessName(ComponentInfo componentInfo) {
        String processName = componentInfo.processName;
        if (processName == null) {
            processName = componentInfo.packageName;
            componentInfo.processName = processName;
        }
        return processName;
    }

    public static boolean isSameComponent(ComponentInfo first, ComponentInfo second) {

        if (first != null && second != null) {
            String pkg1 = first.packageName + "";
            String pkg2 = second.packageName + "";
            String name1 = first.name + "";
            String name2 = second.name + "";
            return pkg1.equals(pkg2) && name1.equals(name2);
        }
        return false;
    }

    public static ComponentName toComponentName(ComponentInfo componentInfo) {
        return new ComponentName(componentInfo.packageName, componentInfo.name);
    }

    public static boolean isSystemApp(String pkgName) {
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = VirtualCore.get().getUnHookPackageManager().getApplicationInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (applicationInfo == null) {
            return false;
        }
        return pkgName.equals("android") || (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    public static boolean isSystemApp(ApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return false;
        }
        if (GmsSupport.isGoogleAppOrService(applicationInfo.packageName)) {
            return false;
        } else if (SpecialComponentList.isSpecSystemPackage(applicationInfo.packageName)) {
            return true;
        } else if (applicationInfo.uid >= Process.FIRST_APPLICATION_UID && (applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return false;
        } else if (SpecialComponentList.isSpecSystemPackage(applicationInfo.packageName)) {
            return true;
        } else if (applicationInfo.uid >= Process.FIRST_APPLICATION_UID) {
            return (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        }
        return true;
    }

    public static String getComponentAction(ActivityInfo info) {
        return getComponentAction(info.packageName, info.name);
    }

    public static String getComponentAction(ComponentName component) {
        return getComponentAction(component.getPackageName(), component.getClassName());
    }

    public static String getComponentAction(String packageName, String name) {
        return String.format(VirtualCore.get().getHostPkg()+"_VA_%s_%s", packageName, name);
    }

    public static Intent redirectBroadcastIntent(Intent intent, int userId){
        return redirectBroadcastIntent(intent, userId, BroadcastIntentData.TYPE_APP);
    }

    /**
     * @see BroadcastIntentData#TYPE_APP
     * @see BroadcastIntentData#TYPE_FROM_SYSTEM
     * @see BroadcastIntentData#TYPE_FROM_INTENT_SENDER
     */
    public static Intent redirectBroadcastIntent(Intent intent, int userId, int flags) {
        Intent newIntent = new Intent();
        newIntent.setDataAndType(intent.getData(), intent.getType());
        Set<String> categories = intent.getCategories();
        if (categories != null) {
            for (String category : categories) {
                newIntent.addCategory(category);
            }
        }
        ComponentName component = intent.getComponent();
        String targetPackage = intent.getPackage();
        // for static Receiver
        if (component != null) {
            String componentAction = getComponentAction(component);
            newIntent.setAction(componentAction);
            //指定组件的时候不能含有uri
            newIntent.setDataAndType(null, null);
            if (targetPackage == null) {
                targetPackage = component.getPackageName();
            }
        } else {
            String action = protectAction(intent.getAction());
            if(action != null) {
                newIntent.setAction(action);
            }
        }

//        Bundle bundle = intent.getExtras();
//        if(bundle != null && intent.getAction() != null){
//            newIntent.putExtras(bundle);
//        }
        //TODO intent的数据已经在BroadcastIntentData里面了，这里应该是历史遗留代码

        BroadcastIntentData data = new BroadcastIntentData(userId, intent, targetPackage, flags);
        newIntent.putExtra("_VA_|_data_", data);

        return newIntent;
    }

    public static Intent redirectIntentSender(int type, String creator, Intent intent) {
        if (type == ActivityManagerCompat.INTENT_SENDER_ACTIVITY_RESULT) {
            return null;
        }
        Intent newIntent = new Intent();
        newIntent.setSourceBounds(intent.getSourceBounds());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            newIntent.setClipData(intent.getClipData());
        }
        newIntent.addFlags(intent.getFlags() & IntentCompat.IMMUTABLE_FLAGS);
        String originType = newIntent.getType();
        ComponentName component = newIntent.getComponent();
        String newType = originType != null ? originType + ":" + creator : creator;
        if (component != null) {
            newType = newType + ":" + component.flattenToString();
        }
        if (intent.getAction() != null) {
            newIntent.setAction(intent.getAction());
        }
        if (intent.getCategories() != null) {
            for (String g : intent.getCategories()) {
                newIntent.addCategory(g);
            }
        }
        //PendingIntent的send方法，最终是低矮用PMS的下面几个方法，cmp是优先查询，
        // 所以action，category，data这些任意设置都可以找到组件
        //queryIntentReceiversInternal
        //queryIntentServicesInternal
        //resolveIntentInternal
        //queryIntentActivitiesInternal
        //Fix: 修复uri传递错，导致通知栏跳转不对
        newIntent.setDataAndType(intent.getData(), newType);

        String packageName32bit = VirtualCore.getConfig().getHostPackageName();
        switch (type) {
            case ActivityManagerCompat.INTENT_SENDER_ACTIVITY: {
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.setClassName(packageName32bit, ShadowPendingActivity.class.getName());
                break;
            }
            case ActivityManagerCompat.INTENT_SENDER_SERVICE: {
                newIntent.setClassName(packageName32bit, ShadowPendingService.class.getName());
                break;
            }
            case ActivityManagerCompat.INTENT_SENDER_BROADCAST: {
                newIntent.setClassName(packageName32bit, ShadowPendingReceiver.class.getName());
                break;
            }
            default:
                return null;
        }
        newIntent.putExtra("_VA_|_intent_", intent);
        newIntent.putExtra("_VA_|_userId_", VUserHandle.myUserId());
        return newIntent;
    }

    public static Intent getIntentForIntentSender(Intent sender){
        return sender.getParcelableExtra("_VA_|_intent_");
    }

    public static int getUserIdForIntentSender(Intent sender){
        return sender.getIntExtra("_VA_|_userId_", -1);
    }

    public static void clearVAData(Intent sender) {
        if (sender.hasExtra("_VA_|_intent_")) {
            sender.removeExtra("_VA_|_intent_");
        }
        if (sender.hasExtra("_VA_|_userId_")) {
            sender.removeExtra("_VA_|_userId_");
        }
    }

    public static Intent processOutsideIntent(int userId, boolean is64bit, Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            intent.setDataAndType(processOutsideUri(userId, is64bit, data), intent.getType());
        }
        if (Build.VERSION.SDK_INT >= 16 && intent.getClipData() != null) {
            ClipData clipData = intent.getClipData();
            if (clipData.getItemCount() >= 0) {
                ClipData.Item item = clipData.getItemAt(0);
                Uri uri = item.getUri();
                if (uri != null) {
                    Uri processedUri = processOutsideUri(userId, is64bit, uri);
                    if (processedUri != uri) {
                        ClipData processedClipData = new ClipData(clipData.getDescription(), new ClipData.Item(item.getText(), item.getHtmlText(), item.getIntent(), processedUri));
                        for (int i = 1; i < clipData.getItemCount(); i++) {
                            ClipData.Item processedItem = clipData.getItemAt(i);
                            uri = processedItem.getUri();
                            if (uri != null) {
                                uri = processOutsideUri(userId, is64bit, uri);
                            }
                            processedClipData.addItem(new ClipData.Item(processedItem.getText(), processedItem.getHtmlText(), processedItem.getIntent(), uri));
                        }
                        intent.setClipData(processedClipData);
                    }
                }
            }
        }
        if (intent.hasExtra("output")) {
            Object output = intent.getParcelableExtra("output");
            if (output instanceof Uri) {
                intent.putExtra("output", processOutsideUri(userId, is64bit, (Uri) output));
            } else if (output instanceof ArrayList) {
                ArrayList list = (ArrayList) output;
                ArrayList<Uri> newList = new ArrayList<>();
                for (Object o : list) {
                    if (!(o instanceof Uri)) {
                        break;
                    }
                    newList.add(processOutsideUri(userId, is64bit, (Uri) o));
                }
                if (!newList.isEmpty()) {
                    intent.putExtra("output", newList);
                }
            }
        }
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            Object output = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (output instanceof Uri) {
                intent.putExtra(Intent.EXTRA_STREAM, processOutsideUri(userId, is64bit, (Uri) output));
            } else if (output instanceof ArrayList) {
                ArrayList list = (ArrayList) output;
                ArrayList<Uri> newList = new ArrayList<>();
                for (Object o : list) {
                    if (!(o instanceof Uri)) {
                        break;
                    }
                    newList.add(processOutsideUri(userId, is64bit, (Uri) o));
                }
                if (!newList.isEmpty()) {
                    intent.putExtra(Intent.EXTRA_STREAM, newList);
                }
            }
        }

        if (intent.hasExtra(Intent.EXTRA_INTENT)) {
           Object extraintent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (extraintent instanceof Intent) {
                Intent outIntent = processOutsideIntent(userId, is64bit, (Intent) extraintent);
                intent.putExtra(Intent.EXTRA_INTENT, outIntent);
            }
        }
        return intent;
    }

    private static String getInSideDataColumn(Uri uri, String where, String[] args) {
        //_data
        ContentProviderClient client = Stirrer.getConentProvider(uri.getAuthority());
        if (client == null) {
            VLog.e("kk-test", "not found client by %s", uri.getAuthority());
        } else {
            Cursor cursor = null;
            try {
                cursor = client.query(uri, null, where, args, null);
                if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex("_data");
                    if (index >= 0) {
                        return cursor.getString(index);
                    }
                }
            } catch (RemoteException e) {
                VLog.e("kk-test", "getInSideDataColumn", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return null;
    }

    public static Uri processOutsideUri(int userId, boolean is64bit, Uri uri) {
        if (isDownloadsDocument(uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            String path;
            if (documentId.startsWith("raw:")) {
                path = documentId.replaceFirst("raw:", "");
            } else {
                path = getInSideDataColumn(ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(documentId).longValue()), null, null);
            }
            if (path != null) {
                return new Uri.Builder().scheme(SCHEME_CONTENT)
                        .authority(StubManifest.getProxyAuthority(is64bit))
                        .appendPath("external")
                        .appendPath(path)
                        .appendQueryParameter("__va_scheme", SCHEME_FILE)
                        .build();
            }
        } else if (isExternalStorageDocument(uri)) {
            String[] split = DocumentsContract.getDocumentId(uri).split(":");
            if ("primary".equalsIgnoreCase(split[0])) {
                return new Uri.Builder().scheme(SCHEME_CONTENT)
                        .authority(StubManifest.getProxyAuthority(is64bit))
                        .appendPath("external")
                        .appendPath(split[1])
                        .appendQueryParameter("__va_scheme", SCHEME_FILE)
                        .build();
            }
        } else if (isMediaDocument(uri)) {
            String[] split2 = DocumentsContract.getDocumentId(uri).split(":");
            String str = split2[0];
            Uri uri2;
            if ("image".equals(str)) {
                uri2 = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            } else if ("video".equals(str)) {
                uri2 = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            } else if ("audio".equals(str)) {
                uri2 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            } else {
                uri2 = null;
            }
            if (uri2 != null) {
                String path = getInSideDataColumn(uri2, "_id=?", new String[]{split2[1]});
                if (path != null) {
                    return new Uri.Builder()
                            .scheme(SCHEME_CONTENT)
                            .authority(StubManifest.getProxyAuthority(is64bit))
                            .appendPath("external")
                            .appendPath(path)
                            .appendQueryParameter("__va_scheme", SCHEME_FILE)
                            .build();
                }
            }
        }
        // add & change by lml@xdja.com
        if (SCHEME_FILE.equals(uri.getScheme())) {
            //content://io.busniess.va.provider_proxy/external/1/2.txt?__va_scheme=file
            String path = uri.getPath();
            String external_path = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (path.startsWith(external_path)) {
                String split_path = path.substring(external_path.length());
                Uri fake_uri = uri.buildUpon().scheme(SCHEME_CONTENT)
                        .path("/external" + split_path)
                        .authority(StubManifest.getProxyAuthority(is64bit))
                        .appendQueryParameter("__va_scheme", SCHEME_FILE).build();
                return fake_uri;
            } else {
                return Uri.fromFile(new File(NativeEngine.resverseRedirectedPath(path)));
            }
        }

        if (!TextUtils.equals(uri.getScheme(), "content")) {
            return uri;
        }
        String authority = uri.getAuthority();
        if (authority == null) {
            return uri;
        }
        // comment by lml@xdja.com
//        ProviderInfo info = VirtualCore.get().getUnHookPackageManager().resolveContentProvider(authority, 0);
//        if (info == null) {
//            return uri;
//        }
        uri = ContentProviderProxy.buildProxyUri(userId, is64bit, authority, uri);
        return uri;
    }

    public static ComponentName getAppComponent(Intent realIntent){
        if(realIntent == null || realIntent.getComponent() == null){
            return null;
        }
        String pkg = realIntent.getComponent().getPackageName();
        String type = realIntent.getType();
        if(TextUtils.equals(pkg, StubManifest.PACKAGE_NAME) && type != null && type.contains("/")){
            String[] ws = type.split("/");
            return new ComponentName(ws[0], ws[1]);
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    public static Uri wrapperNotificationSoundUri(Uri uri, int userId){
        if(uri != null){
            //如果内部MediaProvider实现铃声的uri，则需要处理content://media/internal/audio/，返回false
            //目前是使用外部铃声设置
            if(VirtualCore.getConfig().useOutsideNotificationSound(uri)){
                return uri;
            }
            return processOutsideUri(userId, VirtualCore.get().isPluginEngine(), uri);
        }
        return uri;
    }
}
