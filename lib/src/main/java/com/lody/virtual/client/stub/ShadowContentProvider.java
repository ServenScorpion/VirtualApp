package com.lody.virtual.client.stub;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Process;

import com.lody.virtual.client.VClient;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.remote.ClientConfig;

/**
 * @author Lody
 *
 */
public class ShadowContentProvider extends ContentProvider {

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public Bundle call(String method, String arg, Bundle extras) {
		if ("_VA_|_init_process_".equals(method)) {
			return initProcess(extras);
		}
		return null;
	}

	private Bundle initProcess(Bundle extras) {
		VirtualCore.get().waitStartup();
		extras.setClassLoader(ClientConfig.class.getClassLoader());
        ClientConfig clientConfig = extras.getParcelable("_VA_|_client_config_");
		VClient client = VClient.get();
		client.initProcess(clientConfig);
		Bundle res = new Bundle();
		BundleCompat.putBinder(res, "_VA_|_client_", client.asBinder());
		res.putInt("_VA_|_pid_", Process.myPid());
		return res;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}


	public static class P0 extends ShadowContentProvider {
	}

	public static class P1 extends ShadowContentProvider {
	}

	public static class P2 extends ShadowContentProvider {
	}

	public static class P3 extends ShadowContentProvider {
	}

	public static class P4 extends ShadowContentProvider {
	}

	public static class P5 extends ShadowContentProvider {
	}

	public static class P6 extends ShadowContentProvider {
	}

	public static class P7 extends ShadowContentProvider {
	}

	public static class P8 extends ShadowContentProvider {
	}

	public static class P9 extends ShadowContentProvider {
	}

	public static class P10 extends ShadowContentProvider {
	}

	public static class P11 extends ShadowContentProvider {
	}

	public static class P12 extends ShadowContentProvider {
	}

	public static class P13 extends ShadowContentProvider {
	}

	public static class P14 extends ShadowContentProvider {
	}

	public static class P15 extends ShadowContentProvider {
	}

	public static class P16 extends ShadowContentProvider {
	}

	public static class P17 extends ShadowContentProvider {
	}

	public static class P18 extends ShadowContentProvider {
	}

	public static class P19 extends ShadowContentProvider {
	}

	public static class P20 extends ShadowContentProvider {
	}

	public static class P21 extends ShadowContentProvider {
	}

	public static class P22 extends ShadowContentProvider {
	}

	public static class P23 extends ShadowContentProvider {
	}

	public static class P24 extends ShadowContentProvider {
	}

	public static class P25 extends ShadowContentProvider {
	}

	public static class P26 extends ShadowContentProvider {
	}

	public static class P27 extends ShadowContentProvider {
	}

	public static class P28 extends ShadowContentProvider {
	}

	public static class P29 extends ShadowContentProvider {
	}

	public static class P30 extends ShadowContentProvider {
	}

	public static class P31 extends ShadowContentProvider {
	}

	public static class P32 extends ShadowContentProvider {
	}

	public static class P33 extends ShadowContentProvider {
	}

	public static class P34 extends ShadowContentProvider {
	}

	public static class P35 extends ShadowContentProvider {
	}

	public static class P36 extends ShadowContentProvider {
	}

	public static class P37 extends ShadowContentProvider {
	}

	public static class P38 extends ShadowContentProvider {
	}

	public static class P39 extends ShadowContentProvider {
	}

	public static class P40 extends ShadowContentProvider {
	}

	public static class P41 extends ShadowContentProvider {
	}

	public static class P42 extends ShadowContentProvider {
	}

	public static class P43 extends ShadowContentProvider {
	}

	public static class P44 extends ShadowContentProvider {
	}

	public static class P45 extends ShadowContentProvider {
	}

	public static class P46 extends ShadowContentProvider {
	}

	public static class P47 extends ShadowContentProvider {
	}

	public static class P48 extends ShadowContentProvider {
	}

	public static class P49 extends ShadowContentProvider {
	}

	public static class P50 extends ShadowContentProvider {
	}

	public static class P51 extends ShadowContentProvider {
	}

	public static class P52 extends ShadowContentProvider {
	}

	public static class P53 extends ShadowContentProvider {
	}

	public static class P54 extends ShadowContentProvider {
	}

	public static class P55 extends ShadowContentProvider {
	}

	public static class P56 extends ShadowContentProvider {
	}

	public static class P57 extends ShadowContentProvider {
	}

	public static class P58 extends ShadowContentProvider {
	}

	public static class P59 extends ShadowContentProvider {
	}

	public static class P60 extends ShadowContentProvider {
	}

	public static class P61 extends ShadowContentProvider {
	}

	public static class P62 extends ShadowContentProvider {
	}

	public static class P63 extends ShadowContentProvider {
	}

	public static class P64 extends ShadowContentProvider {
	}

	public static class P65 extends ShadowContentProvider {
	}

	public static class P66 extends ShadowContentProvider {
	}

	public static class P67 extends ShadowContentProvider {
	}

	public static class P68 extends ShadowContentProvider {
	}

	public static class P69 extends ShadowContentProvider {
	}

	public static class P70 extends ShadowContentProvider {
	}

	public static class P71 extends ShadowContentProvider {
	}

	public static class P72 extends ShadowContentProvider {
	}

	public static class P73 extends ShadowContentProvider {
	}

	public static class P74 extends ShadowContentProvider {
	}

	public static class P75 extends ShadowContentProvider {
	}

	public static class P76 extends ShadowContentProvider {
	}

	public static class P77 extends ShadowContentProvider {
	}

	public static class P78 extends ShadowContentProvider {
	}

	public static class P79 extends ShadowContentProvider {
	}

	public static class P80 extends ShadowContentProvider {
	}

	public static class P81 extends ShadowContentProvider {
	}

	public static class P82 extends ShadowContentProvider {
	}

	public static class P83 extends ShadowContentProvider {
	}

	public static class P84 extends ShadowContentProvider {
	}

	public static class P85 extends ShadowContentProvider {
	}

	public static class P86 extends ShadowContentProvider {
	}

	public static class P87 extends ShadowContentProvider {
	}

	public static class P88 extends ShadowContentProvider {
	}

	public static class P89 extends ShadowContentProvider {
	}

	public static class P90 extends ShadowContentProvider {
	}

	public static class P91 extends ShadowContentProvider {
	}

	public static class P92 extends ShadowContentProvider {
	}

	public static class P93 extends ShadowContentProvider {
	}

	public static class P94 extends ShadowContentProvider {
	}

	public static class P95 extends ShadowContentProvider {
	}

	public static class P96 extends ShadowContentProvider {
	}

	public static class P97 extends ShadowContentProvider {
	}

	public static class P98 extends ShadowContentProvider {
	}

	public static class P99 extends ShadowContentProvider {
	}


}
