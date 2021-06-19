package com.lody.virtual.client.hook.proxies.content;

import com.lody.virtual.client.hook.annotations.Inject;
import com.lody.virtual.client.hook.annotations.LogInvocation;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;

import mirror.android.content.ContentResolver;
import mirror.android.content.IContentService;

/**
 * @author Lody
 * @see IContentService
 */
@Inject(MethodProxies.class)
public class ContentServiceStub extends BinderInvocationProxy {
    private static final String TAG = ContentServiceStub.class.getSimpleName();

    public ContentServiceStub() {
        super(IContentService.Stub.asInterface, "content");
    }

    @Override
    public void inject() throws Throwable {
        super.inject();
        ContentResolver.sContentService.set(getInvocationStub().getProxyInterface());
    }


}
