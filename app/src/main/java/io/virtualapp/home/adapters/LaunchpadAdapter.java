package io.virtualapp.home.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lody.virtual.client.core.VirtualCore;

import java.util.List;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.widgets.LabelView;
import io.virtualapp.widgets.LauncherIconView;

/**
 * @author LodyChen
 */
public class LaunchpadAdapter extends RecyclerView.Adapter<LaunchpadAdapter.ViewHolder> {

    private LayoutInflater mInflater;
    private List<AppData> mList;
    private OnAppClickListener mAppClickListener;

    public LaunchpadAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public void add(AppData data) {
        int insertPos = mList.size() - 1;
        mList.add(insertPos, data);
        notifyItemInserted(insertPos);
    }

    public void replace(int index, AppData data) {
        mList.set(index, data);
        notifyItemChanged(index);
    }

    public void remove(AppData data) {
        if (mList.remove(data)) {
            notifyDataSetChanged();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_launcher_app, null));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppData data = mList.get(position);
        holder.iconView.setImageDrawable(data.getIcon());
        holder.nameView.setText(data.getName());
        if (data.isFirstOpen() && !data.isLoading()) {
            holder.firstOpenDot.setVisibility(View.VISIBLE);
        } else {
            holder.firstOpenDot.setVisibility(View.INVISIBLE);
        }
        holder.itemView.setOnClickListener(v -> {
            if (mAppClickListener != null) {
                mAppClickListener.onAppClick(position, data);
            }
        });
        if (data instanceof MultiplePackageAppData) {
            MultiplePackageAppData multipleData = (MultiplePackageAppData) data;
            holder.spaceLabelView.setVisibility(View.VISIBLE);
            holder.spaceLabelView.setText(multipleData.userId + 1 + "");
        } else {
            holder.spaceLabelView.setVisibility(View.INVISIBLE);
        }
        if (data.isLoading()) {
            startLoadingAnimation(holder.iconView);
        } else {
            holder.iconView.setProgress(100, false);
        }

        if (VirtualCore.get().isRun64BitProcess(data.getPackageName())){
            holder.appprocess.setText("64");
        }else {
            holder.appprocess.setText("32");
        }
    }

    private void startLoadingAnimation(LauncherIconView iconView) {
        iconView.setProgress(40, true);
        VUiKit.defer().when(() -> {
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).done((res) -> iconView.setProgress(80, true));
    }


    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    public List<AppData> getList() {
        return mList;
    }

    public void setList(List<AppData> list) {
        this.mList = list;
        notifyDataSetChanged();
    }

    public void setAppClickListener(OnAppClickListener mAppClickListener) {
        this.mAppClickListener = mAppClickListener;
    }

    public void moveItem(int pos, int targetPos) {
        AppData model = mList.remove(pos);
        mList.add(targetPos, model);
        notifyItemMoved(pos, targetPos);
    }

    public void refresh(AppData model) {
        int index = mList.indexOf(model);
        if (index >= 0) {
            notifyItemChanged(index);
        }
    }

    public interface OnAppClickListener {
        void onAppClick(int position, AppData model);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        LauncherIconView iconView;
        TextView nameView;
        LabelView spaceLabelView;
        View firstOpenDot;
        LabelView appprocess;

        ViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.item_app_icon);
            nameView = itemView.findViewById(R.id.item_app_name);
            spaceLabelView = itemView.findViewById(R.id.item_app_space_idx);
            firstOpenDot = itemView.findViewById(R.id.item_first_open_dot);
            appprocess = itemView.findViewById(R.id.item_app_process);
        }
    }
}
