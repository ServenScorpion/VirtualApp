package com.carlos.home;


import com.carlos.home.models.AppData;
import com.carlos.home.models.AppInfoLite;

import java.util.List;

import com.carlos.common.ui.activity.abs.BasePresenter;
import com.carlos.common.ui.activity.abs.BaseView;

/**
 * @author LodyChen
 */
public class HomeContract {

	/* renamed from: io.busniess.va.home.HomeContract$HomePresenter */
	public interface HomePresenter extends BasePresenter {
		void addApp(AppInfoLite appInfoLite);

		boolean checkExtPackageBootPermission();

		void dataChanged();

		void deleteApp(AppData appData);

		void enterAppSetting(AppData appData);

		int getAppCount();

		String getLabel(String str);

		void launchApp(AppData appData);
	}

	/* renamed from: io.busniess.va.home.HomeContract$HomeView */
	interface HomeView extends BaseView<HomePresenter> {
		void addAppToLauncher(AppData appData);

		void askInstallGms();

		void hideBottomAction();

		void hideLoading();

		void loadError(Throwable th);

		void loadFinish(List list);

		void refreshLauncherItem(AppData appData);

		void removeAppToLauncher(AppData appData);

		void showBottomAction();

		void showGuide();

		void showLoading();

		void showOverlayPermissionDialog();

		void showPermissionDialog();
	}

	HomeContract() {
	}
}