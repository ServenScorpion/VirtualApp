package com.xdja.zs.netstrategy;

import android.os.Parcel;

import com.lody.virtual.helper.PersistenceLayer;
import com.lody.virtual.os.VEnvironment;
import com.xdja.zs.controllerService;

public class TurnOnOffNetPersistenceLayer extends  PersistenceLayer {

    private static final int CURRENT_VERSION = 1;
    private controllerService controllerService;

    public TurnOnOffNetPersistenceLayer(controllerService controllerService) {
        super(VEnvironment.getNetEnableInfoFile());
        this.controllerService = controllerService;
    }

    @Override
    public int getCurrentVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public boolean verifyMagic(Parcel p) {
        return true;
    }

    @Override
    public void writePersistenceData(Parcel p) {
        p.writeByte(controllerService.NetworkStragegyOnorOff ? (byte) 1 : (byte) 0);
        p.writeByte(controllerService.isWhiteOrBlackFlag ? (byte) 1 : (byte) 0);
    }

    @Override
    public void readPersistenceData(Parcel p, int version) {
        controllerService.NetworkStragegyOnorOff = p.readByte() != 0;
        controllerService.isWhiteOrBlackFlag =  p.readByte() != 0;
    }
    @Override
    public void onPersistenceFileDamage() {
        getPersistenceFile().delete();
    }
}
