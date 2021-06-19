package com.xdja.mms.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class SmsReceiver extends ProxyReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //如果是默认短信，并且有内部provider，外部的provider是没有记录的
//            if(VirtualCore.get().getHostPkg().equals(Telephony.Sms.getDefaultSmsPackage(context))){
//                if(SafeBoxApi.currentSpace()){
//                    //TODO 短信移到内部?
//                }
//            }
        }
    }
}
