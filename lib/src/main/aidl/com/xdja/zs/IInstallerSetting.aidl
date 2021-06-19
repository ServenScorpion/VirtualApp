// IInstallerSetting.aidl
package com.xdja.zs;

interface IInstallerSetting {
     List<String> getSystemApps();
     void setSystemApps(in List<String> list);
     void addSystemApp(String packagename);
     void removeSystemApp(String packagename);
     boolean isSystemApp(String pkg);
}
