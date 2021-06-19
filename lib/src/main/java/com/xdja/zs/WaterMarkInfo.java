package com.xdja.zs;

import android.os.Parcel;
import android.os.Parcelable;

public class WaterMarkInfo implements Parcelable {
    /**
     * ID
     */
    private long id;
    /**
     * 水印内容
     * 1-企业名称 2-企业LOGO 3-用户名 4-设备IMEI 5-IP 6-日期 7-GPS位置
     * 支持1~3种，英文逗号(,)分割
     */
    private String waterMarkContent;
    /**
     * 文字大小    1-小号  2-标准  3-大号   默认2
     */
    private float textSize;
    /**
     * 文字颜色
     */
    private String textColor;
    /**
     * 文字透明度
     */
    private double textAlpha;
    /**
     * 企业名称
     */
    private String companyName;
    /**
     * 企业LOGO本地已下载图片存储URL
     */
    private String logoFilePath;
    /**
     * 企业ID
     */
    private long companyId;
    /**
     * 旋转角度
     */
    private int rotate = -20;
    /**
     * 文字间隔
     */
    private float distance;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWaterMarkContent() {
        return waterMarkContent;
    }

    public void setWaterMarkContent(String waterMarkContent) {
        this.waterMarkContent = waterMarkContent;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public double getTextAlpha() {
        return textAlpha;
    }

    public void setTextAlpha(double textAlpha) {
        this.textAlpha = textAlpha;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLogoFilePath() {
        return logoFilePath;
    }

    public void setLogoFilePath(String logoFilePath) {
        this.logoFilePath = logoFilePath;
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }
    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "WaterMarkInfo{" +
                "id=" + id +
                ", waterMarkContent='" + waterMarkContent + '\'' +
                ", textSize=" + textSize +
                ", textColor='" + textColor + '\'' +
                ", textAlpha=" + textAlpha +
                ", companyName='" + companyName + '\'' +
                ", logoFilePath='" + logoFilePath + '\'' +
                ", companyId=" + companyId +
                ", distance=" + distance +
                ", rotate=" + rotate +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.waterMarkContent);
        dest.writeFloat(this.textSize);
        dest.writeString(this.textColor);
        dest.writeDouble(this.textAlpha);
        dest.writeString(this.companyName);
        dest.writeString(this.logoFilePath);
        dest.writeLong(this.companyId);
        dest.writeFloat(this.distance);
        dest.writeInt(this.rotate);
    }

    public WaterMarkInfo() {
    }

    protected WaterMarkInfo(Parcel in) {
        this.id = in.readLong();
        this.waterMarkContent = in.readString();
        this.textSize = in.readFloat();
        this.textColor = in.readString();
        this.textAlpha = in.readDouble();
        this.companyName = in.readString();
        this.logoFilePath = in.readString();
        this.companyId = in.readLong();
        this.distance = in.readFloat();
        this.rotate = in.readInt();
    }

    public static final Parcelable.Creator<WaterMarkInfo> CREATOR = new Parcelable.Creator<WaterMarkInfo>() {
        @Override
        public WaterMarkInfo createFromParcel(Parcel source) {
            return new WaterMarkInfo(source);
        }

        @Override
        public WaterMarkInfo[] newArray(int size) {
            return new WaterMarkInfo[size];
        }
    };
}
