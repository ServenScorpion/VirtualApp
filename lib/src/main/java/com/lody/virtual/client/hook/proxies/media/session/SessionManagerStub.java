package com.lody.virtual.client.hook.proxies.media.session;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.IInterface;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import mirror.android.media.session.ISessionManager;

/**
 * @author Lody
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class SessionManagerStub extends BinderInvocationProxy {

    public SessionManagerStub() {
        super(ISessionManager.Stub.asInterface, Context.MEDIA_SESSION_SERVICE);
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new ReplaceCallingPkgMethodProxy("createSession") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                final IInterface ISession = (IInterface) super.call(who, method, args);
                return CreateProxy(ISession, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("getController".equals(method.getName())) {
                            final IInterface controller = (IInterface) method.invoke(ISession, args);

                            return CreateProxy(controller, new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                    //String getPackageName();
//                                    void adjustVolume(int direction, int flags, String packageName);
//                                    void setVolumeTo(int value, int flags, String packageName);
                                    if("setVolumeTo".equals(method.getName())){
                                        MethodParameterUtils.replaceFirstAppPkg(args);
                                        return method.invoke(controller, args);
                                    }else if("adjustVolume".equals(method.getName())){
                                        MethodParameterUtils.replaceFirstAppPkg(args);
                                        return method.invoke(controller, args);
                                    }
                                    return method.invoke(controller, args);
                                }
                            });
                        }
                        return method.invoke(ISession, args);
                    }
                });
            }
        });
        //xdja
        addMethodProxy(new ReplaceCallingPkgMethodProxy("dispatchVolumeKeyEvent"));
    }

    private static Object CreateProxy(final IInterface iInterface, final InvocationHandler proxy){
        return Proxy.newProxyInstance(iInterface.getClass().getClassLoader(), iInterface.getClass().getInterfaces(), proxy);
    }

}
