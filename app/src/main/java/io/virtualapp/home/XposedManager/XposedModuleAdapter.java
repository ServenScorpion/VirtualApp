package io.virtualapp.home.XposedManager;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.compat.PermissionCompat;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.sandxposed.XposedConfig;
import com.lody.virtual.server.bit64.V64BitHelper;

import java.util.List;

import io.virtualapp.R;
import io.virtualapp.home.PermissionRequestActivity;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.AppRepository;

import static io.virtualapp.VCommends.REQUEST_PERMISSION;

public class XposedModuleAdapter extends RecyclerView.Adapter<XposedModuleAdapter.ViewHolder> {


    private Context context;
    private LayoutInflater mInflater;
    private List<AppData> modules;
    private AppRepository repository;

//    @InjectComponent
//    XposedConfig config;

    public XposedModuleAdapter(Context context, AppRepository repository, List<AppData> modules) {
        this.context = context;
        mInflater = LayoutInflater.from(context);
        this.modules = modules;
        this.repository = repository;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(mInflater.inflate(R.layout.list_item_module, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(modules.get(i));
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;
        TextView desc;
        TextView version;
        CheckBox enable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            desc = itemView.findViewById(R.id.description);
            version = itemView.findViewById(R.id.version_name);
            enable = itemView.findViewById(R.id.checkbox);
        }

        public void bind(AppData data) {
            icon.setImageDrawable(data.getIcon());
            title.setText(data.getName());
            version.setText(data.getVersionName());
            enable.setChecked(true);
//            enable.setOnCheckedChangeListener((compoundButton, b) -> config.enableModule(data.getPackageName(), b));
            if (data.getXposedModule() != null) {
                desc.setText(data.getXposedModule().desc);
            }
            if (data.canLaunch()) {
                itemView.setOnClickListener(view -> launchModule(data));
            } else {
                itemView.setOnClickListener(null);
            }
            if (data.canDelete()) {
                itemView.setOnLongClickListener(view -> deleteModule(data));
            } else {
                itemView.setOnLongClickListener(null);
            }
        }

        void launchModule(AppData data) {
            try {
                if (data instanceof PackageAppData) {
                    PackageAppData appData = (PackageAppData) data;
                    appData.isFirstOpen = false;
//                    LoadingActivity.launch(context, appData.packageName, 0);
                    launchApp(data);
                } else if (data instanceof MultiplePackageAppData) {
                    MultiplePackageAppData multipleData = (MultiplePackageAppData) data;
                    multipleData.isFirstOpen = false;
//                    LoadingActivity.launch(context, multipleData.appInfo.packageName, ((MultiplePackageAppData) data).userId);
                    launchApp(data);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        boolean deleteModule(AppData data) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Module")
                    .setMessage("Do you want to delete " + data.getName() + "?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        try {
                            if (data instanceof PackageAppData) {
                                repository.removeVirtualApp(((PackageAppData) data).packageName, 0);
                            } else {
                                MultiplePackageAppData appData = (MultiplePackageAppData) data;
                                repository.removeVirtualApp(appData.appInfo.packageName, appData.userId);
                            }
                            modules.remove(data);
                            notifyDataSetChanged();
                        } catch (Throwable throwable) {

                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return false;
        }

    }
    public void launchApp(AppData data) {
        try {
            int userId = data.getUserId();
            String packageName = data.getPackageName();
            if (userId != -1 && packageName != null) {
                boolean runAppNow = true;
                if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    InstalledAppInfo info = VirtualCore.get().getInstalledAppInfo(packageName, userId);
                    ApplicationInfo applicationInfo = info.getApplicationInfo(userId);
                    boolean is64bit = VirtualCore.get().isRun64BitProcess(info.packageName);
                    if (is64bit) {
                        if (check64bitEnginePermission()) {
                            return;
                        }
                    }
                    if (PermissionCompat.isCheckPermissionRequired(applicationInfo)) {
                        String[] permissions = VPackageManager.get().getDangrousPermissions(info.packageName);
                        if (!PermissionCompat.checkPermissions(permissions, is64bit)) {
                            runAppNow = false;
                            PermissionRequestActivity.requestPermission((Activity) context, permissions, data.getName(), userId, packageName, REQUEST_PERMISSION);
                        }
                    }
                }
                if (runAppNow) {
                    data.isFirstOpen = false;
                    launchApp(userId, packageName);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean check64bitEnginePermission() {
        if (VirtualCore.get().is64BitEngineInstalled()) {
            if (!V64BitHelper.has64BitEngineStartPermission()) {
                return true;
            }
        }
        return false;
    }
    private void launchApp(int userId, String packageName) {
        if (VirtualCore.get().isRun64BitProcess(packageName)) {
            if (!VirtualCore.get().is64BitEngineInstalled()) {
                Toast.makeText(context, "Please install 64bit engine.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!V64BitHelper.has64BitEngineStartPermission()) {
                Toast.makeText(context, "No Permission to start 64bit engine.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        VActivityManager.get().launchApp(userId, packageName);
    }
}
