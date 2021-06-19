package mirror.android.rms;

import java.util.ArrayList;
import java.util.Map;

import mirror.RefClass;
import mirror.RefObject;

public class HwSysResImplP {
    public static Class<?> TYPE = RefClass.load(HwSysResImplP.class, "android.rms.HwSysResImpl");
    public static RefObject<Map<Integer, ArrayList<String>>> mWhiteListMap;
}