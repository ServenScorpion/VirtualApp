package io.virtualapp.abs.ui;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import org.jdeferred.android.AndroidDeferredManager;

import io.virtualapp.abs.BasePresenter;

/**
 * @author LodyChen
 */
public class VFragment<T extends BasePresenter> extends Fragment {

	protected T mPresenter;

	private boolean mAttach;

	public T getPresenter() {
		return mPresenter;
	}

	public void setPresenter(T presenter) {
		this.mPresenter = presenter;
	}

	protected AndroidDeferredManager defer() {
		return VUiKit.defer();
	}

	public void finishActivity() {
		Activity activity = getActivity();
		if (activity != null) {
			activity.finish();
		}
	}

    public boolean isAttach() {
        return mAttach;
    }

    @Override
    public void onAttach(Context context) {
        mAttach = true;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        mAttach = false;
        super.onDetach();
    }

    public void destroy() {
		finishActivity();
	}
}
