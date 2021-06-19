package mirror.android.app.servertransaction;

import java.util.List;

import mirror.RefClass;
import mirror.RefObject;

public class ActivityResultItem {
    public static Class<?> TYPE = RefClass.load(ActivityResultItem.class, "android.app.servertransaction.ActivityResultItem");
    public static RefObject<List> mResultInfoList;
}
