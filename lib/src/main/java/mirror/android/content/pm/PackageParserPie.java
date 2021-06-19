package mirror.android.content.pm;

import mirror.MethodReflectParams;
import mirror.RefClass;
import mirror.RefStaticMethod;

/**
 * @author Lody
 */

public class PackageParserPie {
    public static Class<?> TYPE = RefClass.load(PackageParserPie.class, "android.content.pm.PackageParser");
    @MethodReflectParams({"android.content.pm.PackageParser$Package", "boolean"})
    public static RefStaticMethod<Void> collectCertificates;
}
