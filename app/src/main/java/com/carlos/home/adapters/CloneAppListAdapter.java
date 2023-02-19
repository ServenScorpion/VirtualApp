package com.carlos.home.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.carlos.R;
import com.carlos.common.utils.HVLog;
import com.carlos.common.utils.InstallTools;
import com.carlos.common.utils.ResponseProgram;
import com.carlos.common.widget.LabelView;
import com.carlos.home.models.AppData;
import com.carlos.home.models.AppInfo;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import com.carlos.home.models.PackageAppData;
import com.carlos.widgets.DragSelectRecyclerViewAdapter;

/**
 * @author LodyChen
 */
public class CloneAppListAdapter extends DragSelectRecyclerViewAdapter<RecyclerView.ViewHolder> {

    private final View mFooterView;
    private LayoutInflater mInflater;
    private List<ApplicationType> mAppList;
    List<String> mCharacterList ;

    private ItemEventListener mItemEventListener;
    private ItemEventManager mItemEventManager;
    private final int HOST_APK_PROCESSS;

    Context mContext;
    public enum ITEM_TYPE {
        /**表示 这一项显示的字符*/
        ITEM_TYPE_CHARACTER,
        /**表示 这一项显示的应用信息*/
        ITEM_TYPE_APPLICATION,
        ITEM_TYPE_FOOTER
    }

    public CloneAppListAdapter(Context context) {
        mContext = context;
        this.mInflater = LayoutInflater.from(context);
        mFooterView = new View(context);
        StaggeredGridLayoutManager.LayoutParams params = new StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ResponseProgram.dpToPx(context, 60)
        );
        params.setFullSpan(true);
        mFooterView.setLayoutParams(params);


        String packageResourcePath = context.getPackageResourcePath();
        File apkPath = new File(packageResourcePath);
        HOST_APK_PROCESSS = InstallTools.checkAPKProcess(apkPath);
    }

    public void setOnItemClickListener(ItemEventListener mItemEventListener) {
        this.mItemEventListener = mItemEventListener;
    }

    public void setItemEventManager(ItemEventManager itemEventManager){
        mItemEventManager = itemEventManager;
    }


    public void setList(List<Object> models) {
        List<String> mAppPinyinList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();

        mAppList = new ArrayList<>();
        mCharacterList = new ArrayList<>();
        String defcharacter = "*";
        for (int i = 0; i < models.size(); i++) {
            Object object = models.get(i);
            if (object instanceof AppInfo){
                AppInfo appInfo = (AppInfo) models.get(i);
                if (appInfo.cloneCount > 0) {// 表示将已经多开的应用放到最前面
                    if (!mCharacterList.contains(defcharacter)){// 特殊字符 * 表示一列
                        mCharacterList.add(defcharacter);
                        mAppList.add(new ApplicationType(defcharacter, ITEM_TYPE.ITEM_TYPE_CHARACTER.ordinal()));
                    }
                    mAppList.add(new ApplicationType(appInfo, ITEM_TYPE.ITEM_TYPE_APPLICATION.ordinal()));
                    continue;
                }
                String pinyin = appInfo.namePinyin;
                map.put(pinyin, appInfo);
                mAppPinyinList.add(pinyin);
                HVLog.d("CloneAppListAdapter appInfo： "+appInfo.packageName);
            }else if (object instanceof AppData){
                PackageAppData appData = (PackageAppData) models.get(i);
                String pinyin = appData.namePinyin;
                map.put(pinyin, appData);
                mAppPinyinList.add(pinyin);
                HVLog.d("CloneAppListAdapter appData： "+appData.packageName);
            }
        }
        Collections.sort(mAppPinyinList, new ContactComparator());// 根据应用名字的拼音排序

        for (int i = 0; i < mAppPinyinList.size(); i++) {
            String name = mAppPinyinList.get(i);
            String character = (name.charAt(0) + "").toUpperCase(Locale.ENGLISH);

            if (!mCharacterList.contains(character)) {//表示 该字符不存在快速索引列表里面
                if (character.hashCode() >= "A".hashCode() && character.hashCode() <= "Z".hashCode()) { // 是字母
                    mCharacterList.add(character);
                    mAppList.add(new ApplicationType(character, ITEM_TYPE.ITEM_TYPE_CHARACTER.ordinal()));
                } else {
                    if (!mCharacterList.contains("#")) {
                        mCharacterList.add("#");
                        mAppList.add(new ApplicationType("#", ITEM_TYPE.ITEM_TYPE_CHARACTER.ordinal()));
                    }
                }
            }

            mAppList.add(new ApplicationType(map.get(name), ITEM_TYPE.ITEM_TYPE_APPLICATION.ordinal()));
        }
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE.ITEM_TYPE_FOOTER.ordinal()) {
            return new ViewHolder(mFooterView);
        }

        if (viewType == ITEM_TYPE.ITEM_TYPE_CHARACTER.ordinal()) {
            return new CharacterHolder(mInflater.inflate(R.layout.item_clone_character, parent, false));
        } else {
            return new ViewHolder(mInflater.inflate(R.layout.item_clone_app, null));
        }
    }

    @Override
    public void onBindViewHolder( RecyclerView.ViewHolder targetHolder, int position) {
        HVLog.d("onBindViewHolder");
        if (getItemViewType(position) == ITEM_TYPE.ITEM_TYPE_FOOTER.ordinal()) {
            return;
        }
        if (targetHolder instanceof CharacterHolder) {
            ((CharacterHolder) targetHolder).mTextView.setText(mAppList.get(position).getCharacter());
        } else if (targetHolder instanceof ViewHolder) {
           // ((ContactHolder) holder).mTextView.setText(resultList.get(position).getmName());
            super.onBindViewHolder(targetHolder, position);
            ApplicationType applicationType = mAppList.get(position);
            ViewHolder holder = (ViewHolder) targetHolder;
            Object typeApp = applicationType.getApp();
            if (typeApp instanceof AppInfo) {// 这个表示克隆系统安装的应用程序
                AppInfo info = (AppInfo) applicationType.getApp();
                holder.iconView.setImageDrawable(info.icon);
                holder.nameView.setText(info.name);
                holder.mManagerLayout.setVisibility(View.GONE);
                if (isIndexSelected(position)) {
                    holder.iconView.setAlpha(1f);
                    //holder.appCheckView.setImageResource(R.drawable.ic_check);
                    holder.appCheckView.setImageResource(R.drawable.ic_check_pressed);
                } else {
                    holder.iconView.setAlpha(0.65f);
                    //holder.appCheckView.setImageResource(R.drawable.ic_no_check);
                    holder.appCheckView.setImageResource(R.drawable.ic_check_normal);
                }
                if (info.cloneCount > 0) {
                    holder.labelView.setVisibility(View.VISIBLE);
                    holder.labelView.setText(info.cloneCount + 1 + "");
                } else {
                    holder.labelView.setVisibility(View.INVISIBLE);
                }
                holder.itemView.setOnClickListener(v -> {
                    if (mItemEventListener != null) {
                        mItemEventListener.onItemClick(info, position);
                    }
                });
            }else if (typeApp instanceof PackageAppData){// 这个表示安装在VA 容器里面的程序,表示程序管理
                PackageAppData packageAppData = (PackageAppData) applicationType.getApp();
                holder.iconView.setImageDrawable(packageAppData.icon);
                holder.nameView.setText(packageAppData.name);

                holder.appCheckView.setVisibility(View.GONE);
                holder.mManagerLayout.setVisibility(View.VISIBLE);
                holder.mApplication.setOnClickListener((view)->{
                    //Toast.makeText(mContext," item 点击",Toast.LENGTH_LONG).show();
                    mItemEventManager.onItemClick(packageAppData,position);
                });
            }

        /*ResponseProgram.defer().when(()->{
            return InstallTools.checkAPKProcess(new File(info.path));
        }).done((checkAPKProcess)->{
            if ((checkAPKProcess == PackageSetting.FLAG_RUN_BOTH_32BIT_64BIT) || (HOST_APK_PROCESSS == checkAPKProcess)){
                holder.appprocess.setText("正常");
                holder.appprocess.setTextColor(Color.BLUE);
            }else {
                holder.appprocess.setText("异常");
                holder.appprocess.setTextColor(Color.RED);
            }
        });*/


            /*holder.numberView.setCurrentValue(applicationType.getMultiNumber());
            holder.numberView.setOnValueChangeListener((value)->{
                applicationType.setMultiNumber(value);
            });*/
        }

    }

    public int getScrollPosition(String character) {
        if (mCharacterList.contains(character)) {
            for (int i = 0; i < mAppList.size(); i++) {
                ApplicationType applicationType = mAppList.get(i);
                String typeCharacter = applicationType.getCharacter();
                if (TextUtils.isEmpty(typeCharacter)){
                    continue;
                }
                if (mAppList.get(i).getCharacter().equals(character)) {
                    return i;
                }
            }
        }
        return -1; // -1不会滑动
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    protected boolean isIndexSelectable(int index) {
        if (mItemEventListener != null) {
            return mItemEventListener.isSelectable(index);
        }else {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return mAppList == null ? 1 : mAppList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return ITEM_TYPE.ITEM_TYPE_FOOTER.ordinal();
        }
        return mAppList.get(position).getType();//super.getItemViewType(position);
    }

    public AppInfo getItem(int index) {
        ApplicationType applicationType = mAppList.get(index);
        Object app = applicationType.getApp();
        if (app instanceof AppInfo) {
            return (AppInfo) applicationType.getApp();
        }else {
            return null;
        }
    }

    public int getItemMultiNumber(int index){
        ApplicationType applicationType = mAppList.get(index);
        return applicationType.getMultiNumber();
    }


    public interface ItemEventListener {
        void onItemClick(AppInfo appData, int position);
        boolean isSelectable(int position);
    }
    public interface ItemEventManager {
        void onItemClick(PackageAppData appData, int position);
    }

    public class CharacterHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        CharacterHolder(View view) {
            super(view);
            mTextView = (TextView) view.findViewById(R.id.character);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView nameView;
        private ImageView appCheckView;
        private LabelView labelView;
        private LabelView appprocess;
        //private NumberView numberView;
        LinearLayout mManagerLayout;
        AppCompatTextView mApplication;

        ViewHolder(View itemView) {
            super(itemView);
            if (itemView != mFooterView) {
                iconView = itemView.findViewById(R.id.item_app_icon);
                nameView = itemView.findViewById(R.id.item_app_name);
                appCheckView = itemView.findViewById(R.id.item_app_checked);
                labelView = itemView.findViewById(R.id.item_app_clone_count);
                appprocess = itemView.findViewById(R.id.item_app_process);
                mManagerLayout = itemView.findViewById(R.id.app_manager_layout);
                mApplication = itemView.findViewById(R.id.app_manager);
                //numberView = itemView.findViewById(R.id.number_view_count);
                //appprocess.setVisibility(View.GONE);
            }
        }
    }


    /**
     * 按照字母进行排序
     */
    public class ContactComparator  implements Comparator<String> {

        /**
         * 返回值大于0，会交换前后两个数位置
         * 返回值小于等于0，位置不交换
         *
         * */
        @Override
        public int compare(String o1, String o2) {

            int c1 = (o1.charAt(0) + "").toUpperCase().hashCode();
            int c2 = (o2.charAt(0) + "").toUpperCase().hashCode();

            boolean c1Flag = (c1 < "A".hashCode() || c1 > "Z".hashCode()); // 不是字母
            boolean c2Flag = (c2 < "A".hashCode() || c2 > "Z".hashCode()); // 不是字母

            if (c1Flag && !c2Flag) {
                return 1;
            } else if (!c1Flag && c2Flag) {
                return -1;
            }

            return c1 - c2;
        }
    }

    private class ApplicationType implements Serializable {
        //private AppInfo mApp;
        private Object mApp;//AppInfo 与Packageppata类型
        private String mCharacter;
        private int mType;
        private int mMultiNumber = 1;

        public ApplicationType(Object app, int type) {
            mApp = app;
            mType = type;
        }
        public ApplicationType(String character, int type) {
            mCharacter = character;
            mType = type;
        }

        public Object getApp() {
            return mApp;
        }

        public int getType() {
            return mType;
        }

        public String getCharacter(){
            return  mCharacter;
        }

        public int getMultiNumber() {
            return mMultiNumber;
        }

        public void setMultiNumber(int mMultiNumber) {
            this.mMultiNumber = mMultiNumber;
        }
    }


}
