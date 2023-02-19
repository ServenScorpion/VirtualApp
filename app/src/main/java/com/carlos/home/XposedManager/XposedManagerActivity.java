package com.carlos.home.XposedManager;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carlos.R;
import com.carlos.common.ui.activity.base.VActivity;
import com.carlos.common.ui.adapter.decorations.ItemOffsetDecoration;
import com.carlos.common.utils.ResponseProgram;
import com.google.android.material.appbar.AppBarLayout;
import com.carlos.home.models.AppData;
import com.carlos.home.repo.AppRepository;

import java.util.ArrayList;
import java.util.List;

public class XposedManagerActivity extends VActivity {

    SwitchCompat xposedSwitch;
    RecyclerView recyclerView;

    AppRepository appRepository;
    List<AppData> modules = new ArrayList<>();

    XposedModuleAdapter adapter;

    CoordinatorLayout coordinatorLayout;
    AppBarLayout appBarLayout;
    Toolbar toolbar;
    ConstraintLayout constraintLayout;
//    @InjectComponent
//    XposedConfig config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xposed_manager);
//        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        initXposedGlobalSettings();
        initModuleList();
    }

    private void initXposedGlobalSettings() {
        xposedSwitch = findViewById(R.id.xposed_enable_switch);
        xposedSwitch.setChecked(true);
        xposedSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            recyclerView.setEnabled(b);
            recyclerView.setAlpha(b ? 1 : 0.5f);
        });
    }

    private void initModuleList() {

        boolean xposedEnabled = true;

        recyclerView = findViewById(R.id.module_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ItemOffsetDecoration(ResponseProgram.dpToPx(getContext(), 4)));
        recyclerView.setEnabled(xposedEnabled);
        recyclerView.setAlpha(xposedEnabled ? 1 : 0.5f);

        appRepository = new AppRepository(this);
        adapter = new XposedModuleAdapter(this, appRepository, modules);
        appRepository.getVirtualXposedModules()
                .done(result -> {
                    modules.clear();
                    modules.addAll(result);
                    adapter.notifyDataSetChanged();
                });

        recyclerView.setAdapter(adapter);

    }

}
