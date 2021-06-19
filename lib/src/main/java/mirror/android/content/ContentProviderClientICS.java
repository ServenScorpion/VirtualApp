package mirror.android.content;

import android.content.ContentProviderClient;

import mirror.MethodReflectParams;
import mirror.RefClass;
import mirror.RefConstructor;

public class ContentProviderClientICS {
    public static Class TYPE = RefClass.load(ContentProviderClientICS.class, ContentProviderClient.class);
    @MethodReflectParams({"android.content.ContentResolver", "android.content.IContentProvider"})
    public static RefConstructor<ContentProviderClient> ctor;
}