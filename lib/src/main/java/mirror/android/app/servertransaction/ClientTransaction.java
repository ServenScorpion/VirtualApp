package mirror.android.app.servertransaction;


import android.os.IBinder;

import java.util.List;

import mirror.RefClass;
import mirror.RefObject;

public class ClientTransaction {
    public static Class<?> TYPE = RefClass.load(ClientTransaction.class, "android.app.servertransaction.ClientTransaction");
    public static RefObject<IBinder> mActivityToken;
    public static RefObject<Object> mLifecycleStateRequest;
    public static RefObject<List<Object>> mActivityCallbacks;
}
