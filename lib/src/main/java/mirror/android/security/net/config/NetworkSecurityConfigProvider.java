package mirror.android.security.net.config;

import android.content.Context;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

public class NetworkSecurityConfigProvider {
    public static Class<?> TYPE = RefClass.load(NetworkSecurityConfigProvider.class, "android.security.net.config.NetworkSecurityConfigProvider");

    @MethodParams({Context.class})
    public static RefStaticMethod<Void> install;
}
