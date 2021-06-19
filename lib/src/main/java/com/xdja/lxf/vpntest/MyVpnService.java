package com.xdja.lxf.vpntest;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.provider.Settings.ACTION_VPN_SETTINGS;
import static java.lang.System.in;

/**
 * Created by xingjianqiang on 2017/3/31.
 * Project : vpntest
 * Email : xingjianqiang@xdja.com
 */

public class MyVpnService extends VpnService{

    private Builder builder;
    public static PendingIntent mConfigureIntent;
    public static ParcelFileDescriptor mInterface = null;

    public static final String COMMOND = "MockVpnService.commond";
    public static String PACKAGENAME = "io.virtualapp";
    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String cmd = null;
        if (intent != null)
            cmd = intent.getStringExtra(COMMOND);

        if ("stop".equals(cmd)) {
            if (mInterface != null) {
                try {
                    mInterface.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            builder = null;
            stopSelf();
        } else if ("start".equals(cmd)) {
            if (builder == null) {
                builder = new Builder();
                builder.setMtu(1500);
                builder.addRoute("0.0.0.0",32);
                // VPN address
                String vpn4 = "192.168.1.1";
                builder.addAddress(vpn4, 32);

                try {
                    PACKAGENAME = "io.virtualapp";
                    builder.addAllowedApplication(PACKAGENAME);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
//                mInterface = builder.setSession("120.194.4.150").setConfigureIntent(mConfigureIntent).establish();

//                Intent configure = new Intent(this, MainActivity.class);
                Intent configure = new Intent(ACTION_VPN_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                PendingIntent pi = PendingIntent.getActivity(this, 0, configure, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setConfigureIntent(pi);
                mInterface = builder.establish();

//                int sock_fd = new Socket(120.194.4.150);
//                protect(sock_fd);

                int fd = mInterface.getFd();

                // Packets received need to be written tothis output stream.

                FileOutputStream out = new FileOutputStream(mInterface.getFileDescriptor());

                // Allocate the buffer for a single packet.

                ByteBuffer packet =ByteBuffer.allocate(32767);


                try{
                    // Read packets sending to this interface
                    int length = in.read(packet.array());
                    // Write response packets back
                    out.write(packet.array(), 0, length);
                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d("MockVpnService", "onDestroy");
        builder = null;
        stopSelf();
        super.onDestroy();
    }
}
