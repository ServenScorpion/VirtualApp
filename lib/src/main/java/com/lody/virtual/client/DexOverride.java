package com.lody.virtual.client;

/**
 * @author Lody
 */
public class DexOverride {
    public String originDexPath;
    public String newDexPath;
    public String originOdexPath;
    public String newOdexPath;

    public DexOverride(String originDexPath, String newDexPath, String originOdexPath, String newOdexPath) {
        this.originDexPath = originDexPath;
        this.newDexPath = newDexPath;
        this.originOdexPath = originOdexPath;
        this.newOdexPath = newOdexPath;
    }

}
