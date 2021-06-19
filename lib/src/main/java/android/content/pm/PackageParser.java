package android.content.pm;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;

import java.lang.annotation.Target;
import java.security.cert.CertificateException;
import java.util.ArrayList;

/**
 * @author Lody
 */
public class PackageParser {

    public static final int PARSE_IS_SYSTEM = 1;

    // API 28 START
    public static final int PARSE_IS_SYSTEM_DIR = 1 << 4;
    public static final int PARSE_COLLECT_CERTIFICATES = 1 << 5;
    public static final int PARSE_ENFORCE_CODE = 1 << 6;
    // API 28 END

    @TargetApi(29)
    public interface Callback {
    }

    @TargetApi(29)
    public static final class CallbackImpl implements Callback {
        public CallbackImpl(PackageManager pm) {
            throw new RuntimeException("Stub!");
        }
    }

    @TargetApi(29)
    public void setCallback(Callback cb) {
        throw new RuntimeException("Stub!");
    }

    @TargetApi(28)
    public static final class SigningDetails {
        public Signature[] signatures;

        public static final SigningDetails UNKNOWN = null;
    }

    @TargetApi(28)
    public static class Builder {
        private Signature[] mSignatures;

        public Builder() {
        }

        public Builder setSignatures(Signature[] signatures) {
            mSignatures = signatures;
            return this;
        }

        public SigningDetails build()
                throws CertificateException {
            return new SigningDetails();
        }


    }

    public static class IntentInfo extends IntentFilter {
        public boolean hasDefault;
        public int labelRes;
        public CharSequence nonLocalizedLabel;
        public int icon;
        public int logo;
        public int banner;
    }

    public static class Component<II extends IntentInfo> {
        public Package owner;
        public ArrayList<II> intents;
        public String className;
        public Bundle metaData;

        public ComponentName getComponentName() {
            return null;
        }
    }

    public final static class Activity extends Component<ActivityIntentInfo> {
        public ActivityInfo info;
    }

    public class Package {
        public final ArrayList<Activity> activities = new ArrayList<Activity>(0);
        public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
        public final ArrayList<Provider> providers = new ArrayList<Provider>(0);
        public final ArrayList<Service> services = new ArrayList<Service>(0);
        public final ArrayList<Instrumentation> instrumentation = new ArrayList<Instrumentation>(0);
        public final ArrayList<Permission> permissions = new ArrayList<Permission>(0);
        public final ArrayList<PermissionGroup> permissionGroups = new ArrayList<PermissionGroup>(0);
        public final ArrayList<String> requestedPermissions = new ArrayList<String>();
        public Signature[] mSignatures;
        public SigningDetails mSigningDetails;
        public Bundle mAppMetaData;
        public Object mExtras;
        public String packageName;
        public int mPreferredOrder;
        public String mSharedUserId;
        public ArrayList<String> usesLibraries;
        public ArrayList<String> usesOptionalLibraries;
        public int mVersionCode;
        public ApplicationInfo applicationInfo;
        public String mVersionName;

        // Applications hardware preferences
        public ArrayList<ConfigurationInfo> configPreferences = null;

        // Applications requested features
        public ArrayList<FeatureInfo> reqFeatures = null;
        public int mSharedUserLabel;
    }

    public final class Service extends Component<ServiceIntentInfo> {
        public ServiceInfo info;
    }

    public final class Provider extends Component<ProviderIntentInfo> {
        public ProviderInfo info;
    }

    public final class Instrumentation extends Component<IntentInfo> {
        public InstrumentationInfo info;
    }

    public final class Permission extends Component<IntentInfo> {
        public PermissionInfo info;
    }

    public final class PermissionGroup extends Component<IntentInfo> {
        public PermissionGroupInfo info;
    }

    public class ActivityIntentInfo extends IntentInfo {
        public Activity activity;
    }


    public class ServiceIntentInfo extends IntentInfo {
        public Service service;
    }

    public class ProviderIntentInfo extends IntentInfo {
        public Provider provider;
    }
}
