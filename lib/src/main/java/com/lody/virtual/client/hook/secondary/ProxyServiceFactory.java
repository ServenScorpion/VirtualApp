package com.lody.virtual.client.hook.secondary;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 */

public class ProxyServiceFactory {

	private static final String TAG = ProxyServiceFactory.class.getSimpleName();
	public static final String e = "androidPackageName";
	public static final String f = "clientPackageName";

	private static Map<String, ServiceFetcher> sHookSecondaryServiceMap = new HashMap<>();


	public static IBinder getProxyService(Context context, ComponentName component, IBinder binder) {
		if (context == null || binder == null) {
			return null;
		}
		try {
			String description = binder.getInterfaceDescriptor();
			ServiceFetcher fetcher = sHookSecondaryServiceMap.get(description);
			if (fetcher != null) {
				IBinder res = fetcher.getService(context, context.getClassLoader(), binder);
				if (res != null) {
					return res;
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}




	private interface ServiceFetcher {
		IBinder getService(Context context, ClassLoader classLoader, IBinder binder);
	}
}
