package com.xdja.zs;

/**
 * Created by zhangsong on 18-1-23.
 */

import android.os.RemoteException;
import android.util.Log;

import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.helper.utils.Singleton;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.server.am.VActivityManagerService;
import com.xdja.zs.netstrategy.NetStrategyPersistenceLayer;
import com.xdja.zs.netstrategy.TurnOnOffNetPersistenceLayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class controllerService extends IController.Stub {
    private final String Tag = "controllerService";

    private static HashSet<String> GATEWAY_list = new HashSet<String>();
    private static HashMap<String, HashSet<String>> IPMAP = new HashMap<>();
    private static HashSet<String> JXIP_list = new HashSet<String>();
    private static boolean activitySwitchFlag = true;
    public Map<String,Integer> Network_Strategy = new HashMap<String,Integer>();
    private NetStrategyPersistenceLayer mNetStrategyPersistence = new NetStrategyPersistenceLayer(this);
    //true：开启网络过滤 false:关闭
    public static boolean NetworkStragegyOnorOff = false;
    //true:白名单 false:黑名单
    public static boolean isWhiteOrBlackFlag = false;
    private TurnOnOffNetPersistenceLayer mNetIsControlPersistence = new TurnOnOffNetPersistenceLayer(this);
    private IControllerServiceCallback mCSCallback = null;
    private IToastCallback mToastCallback = null;
    private static HashSet<String> packagenames = new HashSet<String>();
    private HashSet<String> ip_strategys = new HashSet<String>();
    private HashSet<String> domain_strategys = new HashSet<String>();
    private HashSet<String> useNetworkApps = new HashSet<String>();
    private static final int IP_STRATEGY = 1;
    private static final int DOMAIN_STRATEGY = 2;

    public controllerService() {
        init();
    }

    private void init() {
        mNetIsControlPersistence.read();
        if(NetworkStragegyOnorOff) {
            mNetStrategyPersistence.read();
            initNetStrategyList();
        }
    }

    static {

        //Gatway
        //GATEWAY_list.add("com.xdja.jxclient");
        //GATEWAY_list.add("com.tencent.mm");
        //GATEWAY_list.add("cn.wps.moffice");
        //setIPMAP();

    }

    private static final Singleton<controllerService> sService = new Singleton<controllerService>() {
        @Override
        protected controllerService create() {
            return new controllerService();
        }
    };

    public static controllerService get() {
        return sService.get();
    }

    @Override
    public boolean isNetworkEnable(String packageName) throws RemoteException {

        boolean appPermissionEnable = VAppPermissionManager.get().getAppPermissionEnable(packageName,
                VAppPermissionManager.PROHIBIT_NETWORK);
        Log.e(Tag, "isNetworkEnable getAppPermissionEnable : " + packageName + " " + appPermissionEnable);
        boolean ret = !appPermissionEnable;
        Log.e(Tag, "isNetworkEnable : " + packageName + " " + ret);
        return ret;
    }

    @Override
    public boolean isCameraEnable(String packageName) throws RemoteException {
        VAppPermissionManager vapm = VAppPermissionManager.get();
        boolean appPermissionEnable = vapm.getAppPermissionEnable(packageName,
                VAppPermissionManager.PROHIBIT_CAMERA);
        Log.e(Tag, "isCameraEnable getAppPermissionEnable : " + packageName + " " + appPermissionEnable);
        if (appPermissionEnable) {
            vapm.interceptorTriggerCallback(packageName, VAppPermissionManager.PROHIBIT_CAMERA);
        }
        boolean ret = !appPermissionEnable;
        Log.e(Tag, "isCameraEnable : " + packageName + " " + ret);
        return ret;
    }

    @Override
    public boolean isGatewayEnable(String packageName) throws RemoteException {
        boolean ret = false;

        for (String item : GATEWAY_list) {
            if (packageName.startsWith(item)) {
                ret = true;
                break;
            }
        }

        Log.e(Tag, "isGatewayEnable : " + packageName + " " + ret);
        return ret;
    }

    @Override
    public boolean isChangeConnect(String packageName, int port, String ip) throws RemoteException {
        boolean ret = false;
        Log.e(Tag, "PackageName : " + packageName + " Ip " + ip + " Port " + port);

        if (IPMAP.get(packageName) != null) {
            for (String item : IPMAP.get(packageName)) {
                String str = String.valueOf(port) + ip;
                if (str.equals(item)) {
                    ret = true;
                    break;
                }
            }
        }
        Log.e(Tag, "isChangeConnect : " + ret);
        return ret;
    }

    @Override
    public boolean isSoundRecordEnable(String packageName) throws RemoteException {
        VAppPermissionManager vapm = VAppPermissionManager.get();
        boolean appPermissionEnable = vapm.getAppPermissionEnable(packageName,
                VAppPermissionManager.PROHIBIT_SOUND_RECORD);
        Log.e(Tag, "isSoundRecordEnable getAppPermissionEnable : " + packageName + " " + appPermissionEnable);
        if (appPermissionEnable) {
            vapm.interceptorTriggerCallback(packageName, VAppPermissionManager.PROHIBIT_SOUND_RECORD);
        }
        boolean ret = !appPermissionEnable;
        Log.e(Tag, "isSoundRecordEnable : " + packageName + " " + ret);
        return ret;
    }

    @Override
    public boolean getActivitySwitch() throws RemoteException {
        Log.e(Tag, "getActivitySwitch : " + activitySwitchFlag);
        return activitySwitchFlag;
    }

    @Override
    public void setActivitySwitch(boolean switchFlag) throws RemoteException {
        Log.e(Tag, "setActivitySwitch : " + switchFlag);
        activitySwitchFlag = switchFlag;
    }

    @Override
    public void registerCallback(IControllerServiceCallback csCallback) throws RemoteException {
        VLog.e(Tag, "controllerService registerCallback ");
        if(csCallback != null){
            mCSCallback = csCallback;
        }else {
            VLog.e(Tag, "controllerService csCallback is null, registerCallback failed");
        }
    }

    @Override
    public void unregisterCallback() throws RemoteException {
        VLog.e(Tag, "controllerService unregisterCallback ");
        mCSCallback = null;
    }

    @Override
    public void appStart(String packageName) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appStart(packageName);
                VLog.e(Tag, "appStart " + packageName);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void appStop(String packageName) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appStop(packageName);
                VLog.e(Tag, "appStop " + packageName);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void appProcessStart(String packageName, String processName, int pid) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appProcessStart(packageName, processName, pid);
                VLog.e(Tag, "appProcessStart " + packageName + " process : " + processName + pid);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void appProcessStop(String packageName, String processName, int pid) throws RemoteException {
        try {
            if (mCSCallback != null) {
                mCSCallback.appProcessStop(packageName, processName, pid);
                VLog.e(Tag, "appProcessStop " + packageName + "process : " + processName + pid);
            } else {
                VLog.e(Tag, "mCSCallback is null ");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean isBinderAlive() {
        return super.isBinderAlive();
    }

    private static void setIPMAP() {
        String serverip_6 = "::ffff:120.194.4.131";
        String serverip_4 = "120.194.4.131";
        IPMAP.put("com.xdja.jxclient", JXIP_list);

        JXIP_list.add("5222" + serverip_4);
        JXIP_list.add("5060" + serverip_4);
        JXIP_list.add("2055" + serverip_6);
        JXIP_list.add("8010" + serverip_6);
        JXIP_list.add("8210" + serverip_6);
        JXIP_list.add("8040" + serverip_6);
        JXIP_list.add("8211" + serverip_6);
        JXIP_list.add("2011" + serverip_6);
        JXIP_list.add("5061" + serverip_6);
        JXIP_list.add("8030" + serverip_6);
        JXIP_list.add("9030" + serverip_6);
        JXIP_list.add("15306" + serverip_6);
    }

    @Override
    public void registerToastCallback(IToastCallback iToastCallback) throws RemoteException{
        VLog.d(Tag, "registerCallback IToastCallback");
        if(iToastCallback != null) {
            mToastCallback = iToastCallback;
        } else {
            VLog.e(Tag, "controllerService iToastCallback is null, registerCallback failed");
        }
    }

    @Override
    public void unregisterToastCallback() throws RemoteException {
        VLog.d(Tag, "controllerService unregisterToastCallback ");
        mToastCallback = null;
    }

    @Override
    public void OnOrOffNetworkStrategy(boolean isOnOrOff) throws RemoteException {
        VLog.d(Tag,"OnOrOffNetworkStrategy isOnOrOff " + isOnOrOff);
        NetworkStragegyOnorOff = isOnOrOff;
        mNetIsControlPersistence.save();
    }

    @Override
    public boolean isIpV6Enable(String packageName, String ipv6) throws RemoteException {
        if (NetworkStragegyOnorOff && ipv6 != null) {
            if (isWhiteOrBlackFlag) {//handle white list
                if (ip_strategys != null && domain_strategys != null) {
                    for (String network_strategy : ip_strategys) {
                        String splictIp = null;
                        if (ipv6.contains(".")) {
                            String[] strs = ipv6.split(":");
                            splictIp = strs[strs.length - 1];//从IPv6中提取IPv4
                            if (network_strategy.contains("-")) {
                                if (judgeIpSection(splictIp, network_strategy)) {
                                    return true;
                                }
                            } else if (network_strategy.contains("/")) {
                                if (judgeSubnet(splictIp, network_strategy)) {
                                    return true;
                                }
                            } else {
                                if (judgeIp(splictIp, network_strategy)) {
                                    return true;
                                }
                            }
                        } else {
                            if (ipv6.equals(network_strategy)) {
                                return true;
                            }
                        }
                    }
                    for (String network_strategy : domain_strategys) {
                        if (network_strategy.contains("*")) {
                            String network_strategy_domain = network_strategy.replace("*", "www");
                            if (judgeIpV6Domain(ipv6, network_strategy_domain)) {
                                return true;
                            }
                            network_strategy_domain = network_strategy.replace("*", "m");
                            if (judgeIpV6Domain(ipv6, network_strategy_domain)) {
                                return true;
                            }
                        } else {
                            if (judgeIpV6Domain(ipv6, network_strategy)) {
                                return true;
                            }
                        }
                    }
                    netControlSuccess(packageName,ipv6);
                    return false;
                }
            } else { //handle white list
                if (ip_strategys != null && domain_strategys != null) {
                    for (String network_strategy : ip_strategys) {
                        String splictIp = null;
                        if (ipv6.contains(".")) {
                            String[] strs = ipv6.split(":");
                            splictIp = strs[strs.length - 1];
                            if (network_strategy.contains("-")) {
                                if (judgeIpSection(splictIp, network_strategy)) {
                                    netControlSuccess(packageName,ipv6);
                                    return false;
                                }
                            } else if (network_strategy.contains("/")) {
                                if (judgeSubnet(splictIp, network_strategy)) {
                                    netControlSuccess(packageName,ipv6);
                                    return false;
                                }
                            } else {
                                if (judgeIp(splictIp, network_strategy)) {
                                    netControlSuccess(packageName,ipv6);
                                    return false;
                                }
                            }
                        } else {
                            if (ipv6.equals(network_strategy)) {
                                netControlSuccess(packageName,ipv6);
                                return false;
                            }
                        }
                    }
                    for (String network_strategy : domain_strategys) {
                        if (network_strategy.contains("*")) {
                            String network_strategy_domain = network_strategy.replace("*", "www");
                            if (judgeIpV6Domain(ipv6, network_strategy_domain)) {
                                netControlSuccess(packageName,ipv6);
                                return false;
                            }
                            network_strategy_domain = network_strategy.replace("*", "m");
                            if (judgeIpV6Domain(ipv6, network_strategy_domain)) {
                                netControlSuccess(packageName,ipv6);
                                return false;
                            }
                        } else {
                            if (judgeIpV6Domain(ipv6, network_strategy)) {
                                netControlSuccess(packageName,ipv6);
                                return false;
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isIpV4Enable(String packageName, String ipv4) throws RemoteException {
        if (NetworkStragegyOnorOff && ipv4 != null) {
            if (isWhiteOrBlackFlag) {//handle white list
                if (ip_strategys != null && domain_strategys != null) {
                    for (String network_strategy : ip_strategys) {
                        if (network_strategy.contains("-")) {
                            if (judgeIpSection(ipv4, network_strategy)) {
                                return true;
                            }
                        } else if (network_strategy.contains("/")) {
                            if (judgeSubnet(ipv4, network_strategy)) {
                                return true;
                            }
                        } else {
                            if (judgeIp(ipv4, network_strategy)) {
                                return true;
                            }
                        }
                    }
                    for (String network_strategy : domain_strategys) {
                        if (network_strategy.contains("*")) {
                            String network_strategy_domain = network_strategy.replace("*", "www");
                            if (judgeIpV4Domain(ipv4, network_strategy_domain)) {
                                return true;
                            }
                            network_strategy_domain = network_strategy.replace("*", "m");
                            if (judgeIpV4Domain(ipv4, network_strategy_domain)) {
                                return true;
                            }
                        } else {
                            if (judgeIpV4Domain(ipv4, network_strategy)) {
                                return true;
                            }
                        }
                    }
                    netControlSuccess(packageName,ipv4);
                    return false;
                }
            } else {// handle black list
                if (ip_strategys != null && domain_strategys != null) {
                    for (String network_strategy : ip_strategys) {
                        if (network_strategy.contains("-")) {
                            if (judgeIpSection(ipv4, network_strategy)) {
                                netControlSuccess(packageName,ipv4);
                                return false;
                            }
                        } else if (network_strategy.contains("/")) {
                            if (judgeSubnet(ipv4, network_strategy)) {
                                netControlSuccess(packageName,ipv4);
                                return false;
                            }
                        } else {
                            if (judgeIp(ipv4, network_strategy)) {
                                netControlSuccess(packageName,ipv4);
                                return false;
                            }
                        }
                    }
                    for (String network_strategy : domain_strategys) {
                        if (network_strategy.contains("*")) {
                            String network_strategy_domain = network_strategy.replace("*", "www");
                            if (judgeIpV4Domain(ipv4, network_strategy_domain)) {
                                netControlSuccess(packageName,ipv4);
                                return false;
                            }
                            network_strategy_domain = network_strategy.replace("*", "m");
                            if (judgeIpV4Domain(ipv4, network_strategy_domain)) {
                                netControlSuccess(packageName,ipv4);
                                return false;
                            }
                        } else {
                            if (judgeIpV4Domain(ipv4, network_strategy)) {
                                netControlSuccess(packageName,ipv4);
                                return false;
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isDomainEnable(String packageName, String doamin) throws RemoteException {
        //VLog.d(Tag,"packageName:" + packageName + " domain:" + doamin);
        if(NetworkStragegyOnorOff && doamin != null) {
            if (isWhiteOrBlackFlag) {//handle white list
                if (domain_strategys != null) {
                    for (String network_strategy : domain_strategys) {
                        if (network_strategy.contains("*")) {
                            network_strategy = network_strategy.replace("*.", "");
                        }
                        if (doamin.contains(network_strategy)) {
                            return true;
                        }
                    }
                    netControlSuccess(packageName,doamin);
                    return false;
                }
            } else {// handle black list
                if (domain_strategys != null) {
                    for (String network_strategy : domain_strategys) {
                        if (network_strategy.contains("*")) {
                            network_strategy = network_strategy.replace("*.", "");
                        }
                        if (doamin.contains(network_strategy)) {
                            netControlSuccess(packageName,doamin);
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean getNetworkState() {
        return NetworkStragegyOnorOff;
    }

    @Override
    public boolean isWhiteList() {
        return isWhiteOrBlackFlag;
    }

    public void addWhiteIpStrategy(String packageName, String ip) {
        if(packageName != null && ip!= null) {
            ip_strategys.add(ip);
        }
    }

    private boolean judgeIpV4Domain(String ipv4,String domain) {
        try {
            InetAddress[] ips = InetAddress.getAllByName(domain);
            for(InetAddress inetAddress:ips) {
                if(ipv4.equals(inetAddress.getHostAddress()))
                {
                    return  true;
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean judgeIpV6Domain(String ipv6,String domain) {
        try {
            InetAddress[] ips = InetAddress.getAllByName(domain);
            for(InetAddress inetAddress:ips) {
                if(ipv6.contains(inetAddress.getHostAddress()) || ipv6.equals(inetAddress.getHostAddress()))
                {
                    return  true;
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void netControlSuccess(String packagename,String domainOrIp) throws RemoteException{
        VLog.d(Tag,"packagename:" + packagename + " domainOrIp:" + domainOrIp);
        if (!packagenames.contains(packagename)) {
            showToast();
            VActivityManagerService.get().closeAllLongSocket(packagename,0);
            packagenames.add(packagename);
        }
    }

    private void showToast() throws RemoteException {
        if(mToastCallback != null) {
            mToastCallback.showToast();
        }
    }

    private boolean judgeIp(String ip, String ipStreategy) {
        return ip.equals(ipStreategy);
    }

    private boolean judgeIpSection(String ip,String ipStreategy) {
        String ipSection = ipStreategy.trim();
        int idx = ipSection.indexOf("-");
        String beginIp = ipSection.substring(0,idx);
        String endIp = ipSection.substring(idx+1);
        return (getIp2long(beginIp)<=getIp2long(ip) && getIp2long(ip)<=getIp2long(endIp));
    }

    private boolean judgeSubnet(String ip,String networkStrategy) {
        networkStrategy = networkStrategy.trim();
        int idx = networkStrategy.indexOf("/");
        String ip1 = networkStrategy.substring(0, idx);
        String subnet = networkStrategy.substring(idx + 1);
        int subnetInt = ipStrToInt(subnet);
        int a = ipStrToInt(ip1);
        int b = ipStrToInt(ip);
        //IP1与IP2属于同一子网络
        if ((a & subnetInt) == (b & subnetInt)) {
            return true;
        }
        //IP1与IP2不属于同一子网络
        else {
            return false;
        }
    }

    /**
     * 将点分十进制的IP地址转换成整数表示
     * @param ip 点分十进制的IP地址
     * @return IP地址的整数表
     */
    private int ipStrToInt(String ip) {
        String[] part = ip.split("\\.");
        int intIP = 0;
        for (int i = 0; i < part.length; i++) {
            int t = Integer.parseInt(part[i]);
            intIP += t << (24 - 8 * i);
        }
        return intIP;
    }

    private long getIp2long(String ip) {
        String[] ips = ip.split("\\.");
        long ip2long = 0L;
        for (int i = 0; i < ips.length; ++i) {
            ip2long = ip2long << 8 | Integer.parseInt(ips[i]);
        }
        return ip2long;
    }

    @Override
    public void addNetworkStrategy(Map networkStrategy, boolean isWhiteOrBlackList) {
        VLog.d(Tag,"addNetworkStrategy isWhiteOrBlackList " + isWhiteOrBlackList + " networkStrategy " + networkStrategy);
        //packagenames.clear();
        isWhiteOrBlackFlag = isWhiteOrBlackList;
        if( networkStrategy != null) {
            Network_Strategy.clear();
            Network_Strategy = (HashMap<String, Integer>)networkStrategy;
            initNetStrategyList();
            mNetStrategyPersistence.save();
        }
        mNetIsControlPersistence.save();
    }

    private void initNetStrategyList() {
        ip_strategys.clear();
        domain_strategys.clear();
        for(Map.Entry<String,Integer> entry:Network_Strategy.entrySet()) {
            String network_strategy = entry.getKey();
            int network_type = entry.getValue();
            if(network_type == IP_STRATEGY) {
                ip_strategys.add(network_strategy);
            } else if(network_type == DOMAIN_STRATEGY) {
                domain_strategys.add(network_strategy);
            }
        }
    }

    @Override
    public String[] getIpStrategy() {
        if(ip_strategys != null && ip_strategys.size() > 0) {
            return ip_strategys.toArray(new String[ip_strategys.size()]);
        }
        return null;
    }

    @Override
    public String[] getDomainStrategy() {
        if(domain_strategys != null && domain_strategys.size() > 0) {
            return domain_strategys.toArray(new String[domain_strategys.size()]);
        }
        return null;
    }

    @Override
    public void isNetworkControl(String packageName, String ipOrdomain, boolean isSuccessOrFail) throws RemoteException {
        if (isWhiteOrBlackFlag) {
            if (isSuccessOrFail) { //white list true ok
                VLog.d(Tag, "package:" + packageName + " access ipOrdomain " + ipOrdomain + " white list strategy ok");
            } else { //white list false fail
                VLog.d(Tag, "package:" + packageName + " access ipOrdomain " + ipOrdomain + " white list strategy fail");
                if (!packagenames.contains(packageName)) {
                    VLog.d(Tag,"controller service show Toast");
                    showToast();
                    packagenames.add(packageName);
                }
            }
        } else {
            if (isSuccessOrFail) { //black list true ok
                VLog.d(Tag,"package:" + packageName + " access ipOrdomain " + ipOrdomain + " black list strategy ok");
            } else { //black list false ok
                VLog.d(Tag,"package:" + packageName + " access ipOrdomain " + ipOrdomain + " black list strategy fail");
                if (!packagenames.contains(packageName)) {
                    showToast();
                    packagenames.add(packageName);
                }
            }
        }
    }

    @Override
    public boolean getOnOrOffNetworkStrategy() {
        return NetworkStragegyOnorOff;
    }

    @Override
    public boolean getisWhiteOrBlackFlag() {
        return isWhiteOrBlackFlag;
    }

    private List<String> list = new ArrayList<String>();

    @Override
    public List<String> getNetworkStrategy() {
        list.clear();
        if(ip_strategys != null && ip_strategys.size() > 0) {
            list.addAll(ip_strategys);
        }
        if(domain_strategys != null && domain_strategys.size() > 0) {
            list.addAll(domain_strategys);
        }
        return list;
    }

}
