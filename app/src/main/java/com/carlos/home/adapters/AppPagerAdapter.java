package com.carlos.home.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.HashMap;
import java.util.Map;

import com.carlos.R;
import com.carlos.common.App;
import com.carlos.common.utils.HVLog;
import com.carlos.home.ListAppActivity;
import com.carlos.home.ListAppFragment;

/**
 * @author LodyChen
 */
public class AppPagerAdapter extends FragmentPagerAdapter {
    private Map<Integer,ActionBean> titlesTab = new HashMap<>();

    ListAppActivity mListAppActivity;
    public AppPagerAdapter(ListAppActivity activity, FragmentManager fm) {
        super(fm);
        mListAppActivity = activity;
        titlesTab.put(ListAppActivity.STATUS_CLONE_APP,new ActionBean(App.getApp().getResources().getString(R.string.clone_apps),ListAppActivity.ACTION_CLONE_APP));
        titlesTab.put(ListAppActivity.STATUS_APPS_MANAGER,new ActionBean(App.getApp().getResources().getString(R.string.apps_manager),ListAppActivity.ACTION_APP_MANAGER));
        //titles.put(ListAppActivity.STATUS_CLONE_EXTERNAL_STORAGE,new ActionBean(App.getApp().getResources().getString(R.string.external_storage),ListAppActivity.ACTION_CLONE_APP_EXTERNAL_STORAGE));
    }

    @Override
    public Fragment getItem(int position) {
        if (position == ListAppActivity.STATUS_CLONE_APP) {
            return ListAppFragment.newInstance();
        }else if (position == ListAppActivity.STATUS_APPS_MANAGER){
            return ListAppFragment.newInstance();
            //return ListAppManagerFragment.newInstance();
        }else if (position == ListAppActivity.STATUS_CLONE_EXTERNAL_STORAGE){
            return ListAppFragment.newInstance();
        }
        return null;
    }

    public int getItemIndexByAction(){
        for(Map.Entry<Integer, ActionBean> entry:titlesTab.entrySet()){
            ActionBean value = entry.getValue();
            String action = value.getActionName();
            int key = entry.getKey();
            HVLog.d("value:"+value+"    ");
            if (action.equals(mListAppActivity.getCurrentAction())){
                return key;
            }
        }
        throw new RuntimeException();
    }


    @Override
    public int getCount() {
        return titlesTab.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        ActionBean actionBean = titlesTab.get(position);
        return actionBean.getTitleName();
    }

    public String getPageTitle() {
        int index = getItemIndexByAction();
        ActionBean actionBean = titlesTab.get(index);
        return actionBean.getTitleName();
    }

    public class ActionBean{
        String titleName;
        String actionName;
        public ActionBean(String titleName,String actionName){
            this.titleName = titleName;
            this.actionName = actionName;
        }

        public String getTitleName() {
            return titleName;
        }

        public void setTitleName(String titleName) {
            this.titleName = titleName;
        }

        public String getActionName() {
            return actionName;
        }

        public void setActionName(String actionName) {
            this.actionName = actionName;
        }
    }
}
