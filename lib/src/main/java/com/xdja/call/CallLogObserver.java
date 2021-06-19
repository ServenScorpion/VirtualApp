package com.xdja.call;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.ContentProviderProxy;
import com.xdja.utils.Stirrer;

public class CallLogObserver extends android.database.ContentObserver {
    private static final CallLogObserver ourInstance = new CallLogObserver(getAsyncHandler());
    private static final String TAG = "xela-" + new Object() {
    }.getClass().getEnclosingClass().getSimpleName();

    static CallLogObserver getInstance() {
        return ourInstance;
    }

    private CallLogObserver(Handler handler) {
        super(handler);
    }

    private Context getContext() {
        return VirtualCore.get().getContext();
    }

    private void transferCallLog() {
        long currTime = System.currentTimeMillis();
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "i do not have permission to read call log.");
            return;
        }

        Cursor cursor = getContext().getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, "date DESC");

        if (cursor != null) {
            String[] columns = new String[]{"add_for_all_users", "countryiso", "data_usage", "date", "duration", "features", "formatted_number", "geocoded_location", "is_read", "last_modified", "lookup_uri", "matched_number", "name", "new", "normalized_number", "number", "numberlabel", "numbertype", "phone_account_address", "photo_id", "photo_uri", "post_dial_digits", "presentation", "subscription_component_name", "subscription_id", "transcription", "type", "via_number", "voicemail_uri"};

            if (cursor.moveToFirst()) {
                ContentValues contentValues = new ContentValues();

                long last_modified = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.LAST_MODIFIED));
                if (Math.abs(currTime - last_modified) > 500) {
                    return;
                }

                for (String column : columns) {
                    int colindex = cursor.getColumnIndex(column);
                    if (colindex != -1) {
                        int type = cursor.getType(colindex);
                        if (type == Cursor.FIELD_TYPE_INTEGER) {
                            long value = cursor.getLong(colindex);
                            contentValues.put(column, value);
                        } else if (type == Cursor.FIELD_TYPE_STRING) {
                            String value = cursor.getString(colindex);
                            contentValues.put(column, value);
                        }
                    }
                }
                {
                    getContext().getContentResolver().insert(ContentProviderProxy.buildProxyUri(0, false, CallLog.AUTHORITY, CallLog.Calls.CONTENT_URI), contentValues);
                }
//                try {
//                    Stirrer.getConentProvider(CallLog.AUTHORITY).insert(CallLog.Calls.CONTENT_URI, contentValues);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
                int id = cursor.getInt(cursor.getColumnIndex(CallLog.Calls._ID));
                cursor.close();
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, this.getClass() + " do not have permission to write call log.");
                    return;
                }
                getContext().getContentResolver().unregisterContentObserver(getInstance());
                getContext().getContentResolver().delete(CallLog.Calls.CONTENT_URI, "_id = ?", new String[]{String.valueOf(id)});
                getContext().getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, getInstance());
            } else {
                Log.d(TAG, "cursor.moveToFirst failed");
            }

        } else {
            Log.d(TAG, "get nothing from calllog ");
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        transferCallLog();
    }

    private synchronized static Handler getAsyncHandler() {
        if (sAsyncHandlerThread == null) {
            sAsyncHandlerThread = new HandlerThread("sAsyncHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sAsyncHandlerThread.start();
            sAsyncHandler = new Handler(sAsyncHandlerThread.getLooper());
        }
        return sAsyncHandler;
    }

    private static HandlerThread sAsyncHandlerThread;
    private static Handler sAsyncHandler;

    public static void observe() {
        Log.d("xela", "Observe call log");
        getInstance().listenPhoneState(getInstance().getContext());
    }

    private static void doObserve() {
        getInstance().getContext().getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, true, getInstance());
    }

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        private boolean DIALING = false;

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            TelecomManager telecomManager;

            telecomManager = (TelecomManager) getContext().getSystemService(Context.TELECOM_SERVICE);

            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (DIALING) {
                        DIALING = false;
                    }
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (!DIALING) {
                        DIALING = true;
                        if (telecomManager.getDefaultDialerPackage().equals(VirtualCore.get().getHostPkg())) {
                            CallLogObserver.doObserve();
                        } else {
                            CallLogObserver.doUnObserve();
                        }
                    }
                    break;
                default:
                    Log.d("xela", "other state: " + incomingNumber);
            }
        }

    };

    private void listenPhoneState(@NonNull Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public static void unObserve() {
        TelephonyManager telephonyManager = (TelephonyManager) getInstance().getContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE) {
            return;
        }

        TelecomManager telecomManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            telecomManager = (TelecomManager) getInstance().getContext().getSystemService(Context.TELECOM_SERVICE);
            if (!telecomManager.getDefaultDialerPackage().equals(VirtualCore.get().getHostPkg())) {
                doUnObserve();
            }
        }
        Log.d("xela", "unObserve call log");
    }

    private static void doUnObserve() {
        getInstance().getContext().getContentResolver().unregisterContentObserver(getInstance());
    }
}