package com.xdja.call;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.RemoteException;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.xdja.utils.Stirrer;

public class PhoneCallReceiver extends BroadcastReceiver {
    private static final PhoneCallReceiver ourInstance = new PhoneCallReceiver();
    public static final String ACTION = "com.xdja.dialer.removecall";

    private static PhoneCallReceiver getInstance() {
        return ourInstance;
    }

    private PhoneCallReceiver() {
    }

    public static void register() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION);
        VirtualCore.get().getContext().registerReceiver(getInstance(), intentFilter);
    }

    public static void unregister() {
        VirtualCore.get().getContext().unregisterReceiver(getInstance());
    }

    private void transferCallLog(String number) {
        if (ActivityCompat.checkSelfPermission(VirtualCore.get().getContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.e("xela", this.getClass() + " do not have permission to read call log.");
            return;
        }

        Cursor cursor = VirtualCore.get().getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, null, "number = ?", new String[]{number}, "date DESC");
        if (cursor != null) {
            String[] columns = new String[]{"add_for_all_users", "countryiso", "data_usage", "date", "duration", "features", "formatted_number", "geocoded_location", "is_read", "last_modified", "lookup_uri", "matched_number", "name", "new", "normalized_number", "number", "numberlabel", "numbertype", "phone_account_address", "photo_id", "photo_uri", "post_dial_digits", "presentation", "subscription_component_name", "subscription_id", "transcription", "type", "via_number", "voicemail_uri"};

            if (cursor.moveToFirst()) {
                ContentValues contentValues = new ContentValues();
                for (String column : columns) {
                    int colindex = cursor.getColumnIndex(column);
                    if (colindex != -1) {
                        int type = cursor.getType(colindex);
                        if (type == 1) {
                            long value = cursor.getLong(colindex);
                            contentValues.put(column, value);
                        } else if (type == 3) {
                            String value = cursor.getString(colindex);
                            contentValues.put(column, value);
                        }
                    }
                }
                try {
                    Stirrer.getConentProvider("call_log").insert(CallLog.Calls.CONTENT_URI, contentValues);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                int id = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                if (ActivityCompat.checkSelfPermission(VirtualCore.get().getContext(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    Log.e("xela", this.getClass() + " do not have permission to write call log.");
                    return;
                }
                VirtualCore.get().getContext().getContentResolver().delete(CallLog.Calls.CONTENT_URI, "_id = ?", new String[]{String.valueOf(id)});
            } else {
                Log.d("xela", "cursor.moveToFirst failed");
            }

        } else {
            Log.d("xela", "get nothing by calllog  number: " + number);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String callLogNumber = intent.getStringExtra(CallLog.Calls.NUMBER);
        if (callLogNumber != null) {
            transferCallLog(callLogNumber);
        } else {
            Log.d("xela", this.getClass() + " receive broadcast, number is null");
        }
    }
}
