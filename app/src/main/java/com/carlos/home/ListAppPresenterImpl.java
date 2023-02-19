package com.carlos.home;

import android.app.Activity;

import com.carlos.common.utils.HVLog;
import com.carlos.home.repo.AppDataSource;
import com.carlos.home.repo.AppRepository;

import java.io.File;

/**
 * @author LodyChen
 */
class ListAppPresenterImpl implements ListAppContract.ListAppPresenter {

	private ListAppContract.ListAppView mView;
	private AppDataSource mRepository;
	ListAppActivity mListAppActivity;

	ListAppPresenterImpl(Activity activity, ListAppContract.ListAppView view) {
		HVLog.d(getClass());
		if (activity instanceof ListAppActivity){
			mListAppActivity = (ListAppActivity) activity;
		}
		mView = view;
		mRepository = new AppRepository(activity);
		mView.setPresenter(this);
	}

	@Override
	public void start() {
		mView.setPresenter(this);
		mView.startLoading();
		if (mListAppActivity.getCurrentAction().equals(ListAppActivity.ACTION_CLONE_APP)) {
			mRepository.getInstalledApps(mListAppActivity, false).done(mView::loadFinish);
		}else if (mListAppActivity.getCurrentAction().equals(ListAppActivity.ACTION_APP_MANAGER)){// 这里需要去从外部存储选择
			mRepository.getVirtualApps(false,true).done(mView::loadFinish);
			//mRepository.getInstalledApps(mListAppActivity,true).done(mView::loadFinish);
		}else if (mListAppActivity.getCurrentAction().equals(ListAppActivity.ACTION_CLONE_APP_EXTERNAL_STORAGE)){// 这里需要去从外部存储选择

		}
	}
}
