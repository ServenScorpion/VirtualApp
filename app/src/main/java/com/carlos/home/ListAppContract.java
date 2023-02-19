package com.carlos.home;

import com.carlos.home.models.AppInfo;

import java.util.List;

import com.carlos.common.ui.activity.abs.BasePresenter;
import com.carlos.common.ui.activity.abs.BaseView;

/**
 * @author LodyChen
 * @version 1.0
 */
/*package*/ class ListAppContract {
    interface ListAppView extends BaseView<ListAppPresenter> {

        void startLoading();

        void loadFinish(List infoList);
    }

    interface ListAppPresenter extends BasePresenter {

    }
}
