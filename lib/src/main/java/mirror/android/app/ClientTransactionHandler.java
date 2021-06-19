package mirror.android.app;


import android.os.IBinder;

import mirror.MethodParams;
import mirror.RefClass;
import mirror.RefMethod;

public class ClientTransactionHandler {
    public static Class<?> TYPE = RefClass.load(ClientTransactionHandler.class, "android.app.ClientTransactionHandler");
    @MethodParams(IBinder.class)
    public static RefMethod<Object> getActivityClient;
}
