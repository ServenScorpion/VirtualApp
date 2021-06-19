package com.lody.virtual.client.hook.proxies.role;

import android.annotation.TargetApi;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.ReplaceCallingPkgMethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastPkgMethodProxy;
import com.lody.virtual.client.hook.base.StaticMethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.stub.InstallerSetting;
import com.lody.virtual.os.VUserHandle;

import java.lang.reflect.Method;

import mirror.android.app.role.IRoleManager;


@TargetApi(29)
public class RoleStub extends BinderInvocationProxy {
    public RoleStub() {
        super(IRoleManager.Stub.asInterface,  "role");
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
        addMethodProxy(new StaticMethodProxy("getDefaultSmsPackage") {
            @Override
            public Object call(Object who, Method method, Object... args) throws Throwable {
                int userIndex = MethodParameterUtils.getIndex(args, int.class);
                if (userIndex >= 0) {
                    args[userIndex] = VUserHandle.realUserId();
                }
                String pkg = (String) super.call(who, method, args);
                if(VirtualCore.get().getHostPkg().equals(pkg)){
                    return InstallerSetting.MESSAGING_PKG;
                }
                return pkg;
            }
        });
        addMethodProxy(new ReplaceLastPkgMethodProxy("isRoleHeld"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("addRoleHolderAsUser"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("removeRoleHolderAsUser"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("addRoleHolderFromController"));
        addMethodProxy(new ReplaceLastPkgMethodProxy("removeRoleHolderFromController"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getHeldRolesFromController"));
        addMethodProxy(new ReplaceCallingPkgMethodProxy("getSmsMessagesForFinancialApp"));
    }
}
