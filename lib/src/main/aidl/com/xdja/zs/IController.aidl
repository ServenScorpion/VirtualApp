// IController.aidl
package com.xdja.zs;

// Declare any non-default types here with import statements
import com.xdja.zs.IControllerServiceCallback;
import com.xdja.zs.IToastCallback;

interface IController {
    boolean isNetworkEnable(String packageName);
    boolean isCameraEnable(String packageName);
    boolean isGatewayEnable(String packageName);
    boolean isChangeConnect(String packageName, int port, String ip);
    boolean isSoundRecordEnable(String packageName);
    boolean getActivitySwitch();
    void setActivitySwitch(boolean switchFlag);
    void registerCallback(IControllerServiceCallback csCallback);
    void unregisterCallback();
    void appStart(String packageName);
    void appStop(String packageName);
    void appProcessStart(String packageName, String processName, int pid);
    void appProcessStop(String packageName, String processName, int pid);
    boolean isIpV4Enable(String packageName,String ipv4);
    boolean isIpV6Enable(String packageName,String ipv6);
    boolean isDomainEnable(String packageName,String doamin);
    void OnOrOffNetworkStrategy(boolean isOnOrOff);
    void addNetworkStrategy(in Map networkStrategy,boolean isWhiteOrBlackList);
    void registerToastCallback(IToastCallback iToastCallback);
    void unregisterToastCallback();
    boolean getNetworkState();
    boolean isWhiteList();
    void addWhiteIpStrategy(String packageName,String ip);
    String[] getIpStrategy();
    String[] getDomainStrategy();
    void isNetworkControl(String packageName,String ipOrdomain,boolean isSuccessOrFail);
    boolean getOnOrOffNetworkStrategy();
    boolean getisWhiteOrBlackFlag();
    List<String> getNetworkStrategy();
}
