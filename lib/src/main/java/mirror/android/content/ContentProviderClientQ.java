package mirror.android.content;

import mirror.MethodReflectParams;
import mirror.RefClass;
import mirror.RefConstructor;

public class ContentProviderClientQ {
    public static Class TYPE = RefClass.load(ContentProviderClientQ.class, android.content.ContentProviderClient.class);
    @MethodReflectParams({"android.content.ContentResolver", "android.content.IContentProvider", "java.lang.String", "boolean"})
    public static RefConstructor<android.content.ContentProviderClient> ctor;
}