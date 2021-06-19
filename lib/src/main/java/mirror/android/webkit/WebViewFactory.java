package mirror.android.webkit;

import mirror.RefBoolean;
import mirror.RefClass;
import mirror.RefStaticMethod;
import mirror.RefStaticObject;

/**
 * @author CodeHz
 */

public class WebViewFactory {
	public static Class<?> TYPE = RefClass.load(WebViewFactory.class, "android.webkit.WebViewFactory");
	public static RefStaticMethod<Object> getUpdateService;
	public static RefStaticObject<Boolean> sWebViewSupported;
}
