package com.lody.virtual.server.interfaces;

import android.os.RemoteException;

import com.lody.virtual.remote.vloc.VCell;
import com.lody.virtual.remote.vloc.VLocation;

import java.util.List;

/**
 * @author Lody
 */
interface IVirtualLocationManager{

    int getMode(int userId, String pkg);

    void setMode(int userId, String pkg, int mode);

    void setCell(int userId, String pkg,in  VCell cell);

    void setAllCell(int userId, String pkg,in  List<VCell> cell);

    void setNeighboringCell(int userId, String pkg,in  List<VCell> cell);

    void setGlobalCell(in VCell cell);

    void setGlobalAllCell(in List<VCell> cell);

    void setGlobalNeighboringCell(in List<VCell> cell);

    VCell getCell(int userId, String pkg);

    List<VCell> getAllCell(int userId, String pkg);

    List<VCell> getNeighboringCell(int userId, String pkg);

    void setLocation(int userId, String pkg,in  VLocation loc);

    VLocation getLocation(int userId, String pkg);

    void setGlobalLocation(in VLocation loc);

    VLocation getGlobalLocation();
}
