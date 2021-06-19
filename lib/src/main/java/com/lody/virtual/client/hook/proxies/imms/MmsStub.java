package com.lody.virtual.client.hook.proxies.imms;

import android.app.PendingIntent;
import android.net.Uri;

import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceSpecPkgMethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.VLog;

import java.lang.reflect.Method;

import mirror.android.app.PendingIntentJBMR2;
import mirror.com.android.internal.telephony.IMms;


/**
 * @author Lody
 * @see android.telephony.SmsManager
 * @see android.provider.Telephony.Mms
 */
public class MmsStub extends BinderInvocationProxy {

	public MmsStub() {
		super(IMms.Stub.asInterface, "imms");
	}

	@Override
	protected void onBindMethods() {
		addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendMessage", 1));
		addMethodProxy(new ReplaceSpecPkgMethodProxyEx("downloadMessage", 1));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("importTextMessage"));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("importMultimediaMessage"));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("deleteStoredMessage"));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("deleteStoredConversation"));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("updateStoredMessageStatus"));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("archiveStoredConversation"));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("addTextMessageDraft"));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("addMultimediaMessageDraft"));
		addMethodProxy(new ReplaceSpecPkgMethodProxyEx("sendStoredMessage", 1));
		addMethodProxy(new ReplaceCallingPkgMethodProxy("setAutoPersisting"));
	}

	private static class ReplaceSpecPkgMethodProxyEx extends ReplaceSpecPkgMethodProxy {
		public ReplaceSpecPkgMethodProxyEx(String name, int index) {
			super(name, index);
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			int index = MethodParameterUtils.getIndex(args, Uri.class);
			if (index != -1) {
				Uri uri = (Uri) args[index];
				args[index] = ComponentUtils.processOutsideUri(getAppUserId(), false, uri);
			}
			return super.call(who, method, args);
		}
	}
}
