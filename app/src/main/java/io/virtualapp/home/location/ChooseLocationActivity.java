package io.virtualapp.home.location;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VirtualLocationManager;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.vloc.VLocation;
import com.tencent.lbssearch.TencentSearch;
import com.tencent.lbssearch.httpresponse.BaseObject;
import com.tencent.lbssearch.httpresponse.HttpResponseListener;
import com.tencent.lbssearch.object.Location;
import com.tencent.lbssearch.object.param.Geo2AddressParam;
import com.tencent.lbssearch.object.param.SearchParam;
import com.tencent.lbssearch.object.result.Geo2AddressResultObject;
import com.tencent.lbssearch.object.result.SearchResultObject;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.CameraPosition;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.tencentmap.mapsdk.map.CameraUpdateFactory;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;

import java.util.ArrayList;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.utils.StringUtils;

import static io.virtualapp.VCommends.EXTRA_LOCATION;

public class ChooseLocationActivity extends VActivity implements TencentLocationListener {
    private TencentMap mMap;
    private MapView mapView;
    private MenuItem mSearchMenuItem;
    private TencentSearch mSearcher;
    private View mSearchLayout;
    private TextView mLatText, mLngText, mAddressText;
    private ArrayAdapter<MapSearchResult> mSearchAdapter;
    private View mMockImg, mMockingView, mMockBtn, mSearchTip;
    private TextView mMockText;
    private String mCity;
    private String mCurPkg;
    private int mCurUserId;
    private VLocation mLocation;
    private boolean isFindLocation;
    private boolean mMocking;
    private String mAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.activity_mock_location);
        Toolbar toolbar = bind(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        enableBackHome();
        ListView mSearchResult = bind(R.id.search_results);
        mapView = findViewById(R.id.map);
        mLatText = bind(R.id.tv_lat);
        mLngText = bind(R.id.tv_lng);
        mMockImg = bind(R.id.img_mock);
        mMockText = bind(R.id.tv_mock);
        mSearchLayout = bind(R.id.search_layout);
        mAddressText = bind(R.id.tv_address);
        mMockingView = bind(R.id.img_stop);
        mMockBtn = findViewById(R.id.img_go_mock);
        mSearchTip = findViewById(R.id.tv_tip_search);
        mapView.onCreate(savedInstanceState); // 此方法必须重写
        mMap = mapView.getMap();
        mSearchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        mSearchResult.setAdapter(mSearchAdapter);
        mSearcher = new TencentSearch(this);
        MapSearchResult.NULL.setAddress(getString(R.string.tip_no_find_points));
        mSearchResult.setOnItemClickListener((adapterView, view, position, id) -> {
            MapSearchResult searchResult = mSearchAdapter.getItem(position);
            if (searchResult != null && searchResult != MapSearchResult.NULL) {
                mSearchMenuItem.collapseActionView();
                gotoLocation(searchResult.address, searchResult.lat, searchResult.lng, true);
            }
        });
        mMap.setOnMapCameraChangeListener(new TencentMap.OnMapCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
                gotoLocation(null, cameraPosition.getTarget().getLatitude(), cameraPosition.getTarget().getLongitude(), false);
            }
        });

        findViewById(R.id.img_stop_mock).setOnClickListener((v) -> {
            VirtualLocationManager.get().setMode(mCurUserId, mCurPkg, VirtualLocationManager.MODE_CLOSE);
            updateMock(false);
            Intent intent = new Intent();
            mLocation.latitude = 0;
            mLocation.longitude = 0;
            intent.putExtra(VCommends.EXTRA_LOCATION, mLocation);
            intent.putExtra(VCommends.EXTRA_PACKAGE, mCurPkg);
            intent.putExtra(VCommends.EXTRA_USERID, mCurUserId);
            intent.putExtra(VCommends.EXTRA_LOCATION_ADDRESS, mAddress);
            setResult(Activity.RESULT_OK, intent);
        });

        mMockBtn.setOnClickListener((v) -> {
            VirtualCore.get().killApp(mCurPkg, mCurUserId);
            VirtualLocationManager.get().setMode(mCurUserId, mCurPkg, VirtualLocationManager.MODE_USE_SELF);
            VirtualLocationManager.get().setLocation(mCurUserId, mCurPkg, mLocation);
            updateMock(true);
            Intent intent = new Intent();
            intent.putExtra(VCommends.EXTRA_LOCATION, mLocation);
            intent.putExtra(VCommends.EXTRA_PACKAGE, mCurPkg);
            intent.putExtra(VCommends.EXTRA_USERID, mCurUserId);
            intent.putExtra(VCommends.EXTRA_LOCATION_ADDRESS, mAddress);
            setResult(Activity.RESULT_OK, intent);
        });
        findViewById(R.id.img_loc).setOnClickListener((v) -> startLocation());
        ((CheckBox) findViewById(R.id.checkbox)).setOnCheckedChangeListener((v, b) -> showInputWindow());
        mMockingView.setOnClickListener((v) -> {
        });

        //data
        mCurPkg = getIntent().getStringExtra(VCommends.EXTRA_PACKAGE);
        mCurUserId = getIntent().getIntExtra(VCommends.EXTRA_USERID, 0);
        VLocation vLocation = getIntent().hasExtra(EXTRA_LOCATION) ? getIntent().getParcelableExtra(EXTRA_LOCATION) : null;
        if (vLocation != null) {
            mLocation = vLocation;
            updateMock(VirtualLocationManager.get().isUseVirtualLocation(mCurUserId, mCurPkg));
            gotoLocation(null, vLocation.getLatitude(), vLocation.getLongitude(), true);
        } else {
            mLocation = new VLocation();
            LatLng latLng = mMap.getMapCenter();
            gotoLocation(null, latLng.getLatitude(), latLng.getLongitude(), false);
            startLocation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        mSearchMenuItem = menuItem;
        mSearchMenuItem.setEnabled(!mMocking);
        SearchView mSearchView = (SearchView) menuItem.getActionView();
//        mSearchView.setSubmitButtonEnabled(true);
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchView.setQueryHint(getString(R.string.tip_input_keywords));
        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchLayout.setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchLayout.setVisibility(View.GONE);
                return true;
            }
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    searchLocation(newText);
                } else {
                    mSearchAdapter.clear();
                    mSearchAdapter.notifyDataSetChanged();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (item.getItemId()) {
            case R.id.action_search:
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void gotoLocation(String address, double lat, double lng, boolean move) {
        if (move) {
            int level = Math.max(mMap.getZoomLevel(), mMap.getMaxZoomLevel() / 3 * 2);
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(lat, lng), level)));
        } else {
            mLocation.latitude = StringUtils.doubleFor8(lat);
            mLocation.longitude = StringUtils.doubleFor8(lng);
            mLatText.setText(String.valueOf(mLocation.latitude));
            mLngText.setText(String.valueOf(mLocation.longitude));
        }
        //更新收藏状态
        if (TextUtils.isEmpty(address)) {
            Geo2AddressParam param = new Geo2AddressParam()
                    .location(new Location()
                            .lat((float) lat)
                            .lng((float) lng));
            mSearcher.geo2address(param, new HttpResponseListener() {
                @Override
                public void onSuccess(int i, BaseObject object) {
                    Geo2AddressResultObject oj = (Geo2AddressResultObject) object;
                    if (oj.result != null) {
                        mCity = oj.result.address_component.city;
                        setAddress(oj.result.address);
                    }
                }

                @Override
                public void onFailure(int i, String s, Throwable throwable) {
                    setAddress(getString(R.string.unknown_location));
                    Log.e("kk", "no find address:" + s, throwable);
                }
            });
        } else {
            setAddress(address);
        }
    }

    private void startLocation() {
        if (isFindLocation) {
            Toast.makeText(this, R.string.tip_find_location, Toast.LENGTH_SHORT).show();
            return;
        }
        isFindLocation = true;
        Toast.makeText(this, R.string.tip_start_location, Toast.LENGTH_SHORT).show();
        TencentLocationRequest request = TencentLocationRequest.create()
                .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_GEO)
                .setAllowGPS(true);
        int error = TencentLocationManager.getInstance(this)
                .requestLocationUpdates(request, this);
        if (error != 0) {
            isFindLocation = false;
            VLog.w("TMap", "startLocation:error=" + error);
        }
    }

    private void updateMock(boolean mock) {
        mMocking = mock;
        mMockImg.setSelected(mock);
        if (mock) {
            mMockText.setText(R.string.mocking);
            mMockingView.setVisibility(View.VISIBLE);
            mMockBtn.setVisibility(View.GONE);
            if (mSearchMenuItem != null) {
                mSearchMenuItem.setEnabled(false);
            }
        } else {
            mMockText.setText(R.string.no_mock);
            mMockingView.setVisibility(View.GONE);
            mMockBtn.setVisibility(View.VISIBLE);
            if (mSearchMenuItem != null) {
                mSearchMenuItem.setEnabled(true);
            }
        }
        mMockText.setSelected(mock);
    }

    private void setAddress(String text) {
        runOnUiThread(() -> {
            mAddress = text;
            mAddressText.setText(text);
        });
    }

    private void searchLocation(String keyword) {
        String target = null;
        int pos = keyword.indexOf("@");
        if (pos > 0) {
            target = keyword.substring(0, pos);
            keyword = keyword.substring(pos + 1);
        }
        final String city = !TextUtils.isEmpty(target) ? target : (TextUtils.isEmpty(mCity) ? "中国" : mCity);

        SearchParam.Region r = new SearchParam.Region().poi(city);
        SearchParam param = new SearchParam().keyword(keyword).boundary(r).page_size(50);
        mSearcher.search(param, new HttpResponseListener() {
            @Override
            public void onSuccess(int i, BaseObject baseObject) {
                if (mSearchTip.getVisibility() != View.GONE) {
                    runOnUiThread(() -> mSearchTip.setVisibility(View.GONE));
                }
                SearchResultObject oj = (SearchResultObject) baseObject;
                mSearchAdapter.clear();
                if (oj.count == 0) {
                    mSearchAdapter.add(MapSearchResult.NULL);
                } else {
                    for (SearchResultObject.SearchResultData item : oj.data) {
                        MapSearchResult result = new MapSearchResult(item.address, item.location.lat, item.location.lng);
                        result.setCity(city);
                        mSearchAdapter.add(result);
                    }
                }
                mSearchAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int i, String s, Throwable throwable) {
                Log.e("kk", "error:" + s, throwable);
                mSearchAdapter.clear();
                mSearchAdapter.add(MapSearchResult.NULL);
                mSearchAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onLocationChanged(TencentLocation location, int error, String msg) {
        isFindLocation = false;
        if (location != null) {
            TencentLocationManager.getInstance(this).removeUpdates(this);
            mLocation.accuracy = location.getAccuracy();
            mLocation.bearing = location.getBearing();
            mLocation.altitude = location.getAltitude();
            mLocation.latitude = location.getLatitude();
            mLocation.longitude = location.getLongitude();
            gotoLocation(null, mLocation.getLatitude(), mLocation.getLongitude(), true);
        } else {
            VLog.e("TMap", "定位失败," + error + ": " + msg);
        }
    }

    @Override
    public void onStatusUpdate(String s, int i, String s1) {

    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    //region 更多功能
    private void showInputWindow() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.VACustomTheme);
        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.dialog_change_loc, null);
        builder.setView(view1);
        Dialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(false);
        EditText editText1 = view1.findViewById(R.id.edt_lat);
        editText1.setText(StringUtils.doubleFor8String(mLocation.getLatitude()));

        EditText editText2 = view1.findViewById(R.id.edt_lon);
        editText2.setText(StringUtils.doubleFor8String(mLocation.getLongitude()));

        dialog.setCancelable(false);
        view1.findViewById(R.id.btn_cancel).setOnClickListener((v2) -> dialog.dismiss());
        view1.findViewById(R.id.btn_ok).setOnClickListener((v2) -> {
            dialog.dismiss();
            try {
                double lat = Double.parseDouble(editText1.getText().toString());
                double lon = Double.parseDouble(editText2.getText().toString());
                gotoLocation(null, lat, lon, true);
            } catch (Exception e) {
                Toast.makeText(this, R.string.input_loc_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressWarnings("unused")
    private static class MapSearchResult {
        private static final MapSearchResult NULL = new MapSearchResult();
        private String address;
        private double lat;
        private double lng;
        private String city;

        private MapSearchResult() {
        }

        public MapSearchResult(String address) {
            this.address = address;
        }

        private MapSearchResult(String address, double lat, double lng) {
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }

        private void setAddress(String address) {
            this.address = address;
        }

        private void setCity(String city) {
            this.city = city;
        }

        @Override
        public String toString() {
            return address;
        }
    }
    //endregion
}
