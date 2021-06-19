package com.lody.virtual.client.env;


class GPSStateline {
    private double mAzimuth;
    private double mElevation;
    private boolean mHasAlmanac;
    private boolean mHasEphemeris;
    private double mCarrierFrequencyHz;
    private int mPnr;
    private double mSnr;
    private boolean mUseInFix;

    public double getAzimuth() {
        return this.mAzimuth;
    }

    public double getElevation() {
        return this.mElevation;
    }

    public int getPnr() {
        return this.mPnr;
    }

    public double getSnr() {
        return this.mSnr;
    }

    public double getCarrierFrequencyHz() {
        return mCarrierFrequencyHz;
    }

    public boolean hasCarrierFrequencyHz() {
        return mCarrierFrequencyHz > 0;
    }

    public boolean isHasAlmanac() {
        return this.mHasAlmanac;
    }

    public boolean isHasEphemeris() {
        return this.mHasEphemeris;
    }

    public boolean isUseInFix() {
        return this.mUseInFix;
    }

    public void setAzimuth(double azimuth) {
        this.mAzimuth = azimuth;
    }

    public void setElevation(double elevation) {
        this.mElevation = elevation;
    }

    public void setHasAlmanac(boolean hasAlmanac) {
        this.mHasAlmanac = hasAlmanac;
    }

    public void setHasEphemeris(boolean hasEphemeris) {
        this.mHasEphemeris = hasEphemeris;
    }

    public void setPnr(int pnr) {
        this.mPnr = pnr;
    }

    public void setSnr(double snr) {
        this.mSnr = snr;
    }

    public void setUseInFix(boolean useInFix) {
        this.mUseInFix = useInFix;
    }

    public GPSStateline(int pnr, double snr, double elevation, double azimuth, boolean useInFix, boolean hasAlmanac, boolean hasEphemeris, double carrierFrequencyHz) {
        mAzimuth = azimuth;
        mElevation = elevation;
        mHasAlmanac = hasAlmanac;
        mHasEphemeris = hasEphemeris;
        mCarrierFrequencyHz = carrierFrequencyHz;
        mPnr = pnr;
        mSnr = snr;
        mUseInFix = useInFix;
    }
}