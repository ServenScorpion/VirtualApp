package io.virtualapp.home.adapters;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.lody.virtual.helper.utils.Reflect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.virtualapp.App;
import io.virtualapp.R;
import io.virtualapp.home.ListAppFragment;

/**
 * @author LodyChen
 */
public class AppPagerAdapter extends FragmentPagerAdapter {
    private List<String> titles = new ArrayList<>();
    private List<File> dirs = new ArrayList<>();

    public AppPagerAdapter(FragmentManager fm) {
        super(fm);
        titles.add(App.getApp().getResources().getString(R.string.clone_apps));
        dirs.add(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Context ctx = App.getApp();
            StorageManager storage = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
            for (StorageVolume volume : storage.getStorageVolumes()) {
                //Why the fuck are getPathFile and getUserLabel hidden?!
                //StorageVolume is kinda useless without those...
                File dir = Reflect.on(volume).call("getPathFile").get();
                String label = Reflect.on(volume).call("getUserLabel").get();
                Log.i("文件路径N", dir.getAbsolutePath() + "，label=" + label);
                Log.i("文件路径N", "dir.listFiles()=" + dir.listFiles());
                if (dir.listFiles() != null) {
                    titles.add(label);
                    dirs.add(dir);
                } else {
                    File storageFir = Environment.getExternalStorageDirectory();
                    Log.i("文件路径N", storageFir.getAbsolutePath());
                    if (storageFir != null && storageFir.isDirectory()) {
                        //此处为查找外部存储的APP
                        titles.add(App.getApp().getResources().getString(R.string.external_storage));
                        dirs.add(storageFir);
                    }
                }
                titles.add("APP的files");
                dirs.add(new File(ctx.getFilesDir()+"/apps"));
            }
        } else {
            // Fallback: only support the default storage sources
            File storageFir = Environment.getExternalStorageDirectory();
            Log.i("文件路径", storageFir.getAbsolutePath());
            if (storageFir != null && storageFir.isDirectory()) {
                //此处为查找外部存储的APP
                titles.add(App.getApp().getResources().getString(R.string.external_storage));
                dirs.add(storageFir);
            }
        }
    }

    @Override
    public Fragment getItem(int position) {
        return ListAppFragment.newInstance(dirs.get(position));
    }

    @Override
    public int getCount() {
        return titles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles.get(position);
    }
}
