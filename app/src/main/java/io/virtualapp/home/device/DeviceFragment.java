package io.virtualapp.home.device;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;

import java.util.ArrayList;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.home.adapters.DeviceAdapter;
import io.virtualapp.home.models.DeviceData;

/**
 * @author LodyChen
 */
public class DeviceFragment extends Fragment {
    private ListView mListView;
    private DeviceAdapter mAppLocationAdapter;

    public static DeviceFragment newInstance() {
        DeviceFragment fragment = new DeviceFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list_settings, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //
        mListView = (ListView) view.findViewById(R.id.listview);
        mAppLocationAdapter = new DeviceAdapter(getContext());
        int count = VUserManager.get().getUserCount();
        List<DeviceData> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            VUserInfo userInfo = VUserManager.get().getUserInfo(i);
            if (userInfo != null) {
                DeviceData deviceData = new DeviceData(getContext(), null, userInfo.id);
                deviceData.name = userInfo.name;
                list.add(deviceData);
            }
        }
        mAppLocationAdapter.set(list);
        mListView.setAdapter(mAppLocationAdapter);
        mListView.setOnItemClickListener((parent, view1, position, id) -> {
            DeviceDetailActiivty.open(this, mAppLocationAdapter.getDataItem(position), position);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            int pos = data.getIntExtra("pos", -1);
            if (pos >= 0) {
                mAppLocationAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAppLocationAdapter != null) {
            mAppLocationAdapter.notifyDataSetChanged();
        }
    }
}
