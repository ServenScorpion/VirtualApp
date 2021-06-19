/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lody.virtual.server.content;

import android.accounts.Account;
import android.content.SyncInfo;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Information about the sync operation that is currently underway.
 */
public class VSyncInfo implements Parcelable {
    /**
     * Used when the caller receiving this object doesn't have permission to access the accounts
     * on device.
     *
     * @See Manifest.permission.GET_ACCOUNTS
     */
    private static final Account REDACTED_ACCOUNT = new Account("*****", "*****");

    /**
     * @hide
     */
    public final int authorityId;

    /**
     * The {@link Account} that is currently being synced.
     */
    public final Account account;

    /**
     * The authority of the provider that is currently being synced.
     */
    public final String authority;

    /**
     * The start time of the current sync operation in milliseconds since boot.
     * This is represented in elapsed real time.
     * See {@link android.os.SystemClock#elapsedRealtime()}.
     */
    public final long startTime;

    /**
     * Creates a SyncInfo object with an unusable Account. Used when the caller receiving this
     * object doesn't have access to the accounts on the device.
     *
     * @See Manifest.permission.GET_ACCOUNTS
     * @hide
     */
    public static VSyncInfo createAccountRedacted(
            int authorityId, String authority, long startTime) {
        return new VSyncInfo(authorityId, REDACTED_ACCOUNT, authority, startTime);
    }

    /**
     * @hide
     */
    public VSyncInfo(int authorityId, Account account, String authority, long startTime) {
        this.authorityId = authorityId;
        this.account = account;
        this.authority = authority;
        this.startTime = startTime;
    }

    /**
     * @hide
     */
    public VSyncInfo(VSyncInfo other) {
        this.authorityId = other.authorityId;
        this.account = new Account(other.account.name, other.account.type);
        this.authority = other.authority;
        this.startTime = other.startTime;
    }

    public SyncInfo toSyncInfo() {
        return mirror.android.content.SyncInfo.ctor.newInstance(authorityId, account, authority, startTime);
    }

    /**
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * @hide
     */
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(authorityId);
        parcel.writeParcelable(account, flags);
        parcel.writeString(authority);
        parcel.writeLong(startTime);
    }

    /**
     * @hide
     */
    VSyncInfo(Parcel parcel) {
        authorityId = parcel.readInt();
        account = parcel.readParcelable(Account.class.getClassLoader());
        authority = parcel.readString();
        startTime = parcel.readLong();
    }

    /**
     * @hide
     */
    public static final Creator<VSyncInfo> CREATOR = new Creator<VSyncInfo>() {
        public VSyncInfo createFromParcel(Parcel in) {
            return new VSyncInfo(in);
        }

        public VSyncInfo[] newArray(int size) {
            return new VSyncInfo[size];
        }
    };
}
