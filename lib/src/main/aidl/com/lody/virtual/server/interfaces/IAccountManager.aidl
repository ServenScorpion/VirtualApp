package com.lody.virtual.server.interfaces;

import android.accounts.Account;
import android.accounts.AuthenticatorDescription;
import android.accounts.IAccountManagerResponse;
import android.os.Bundle;
import android.os.RemoteException;

import java.util.Map;

/**
 * @author Lody
 */
interface IAccountManager{

    AuthenticatorDescription[] getAuthenticatorTypes(int userId);

    void getAccountsByFeatures(int userId,in IAccountManagerResponse response, String type,in  String[] features);
    String getPreviousName(int userId,in  Account account);

    Account[] getAccounts(int userId, String type);

    void getAuthToken(int userId,in  IAccountManagerResponse response,in  Account account, String authTokenType, boolean notifyOnAuthFailure, boolean expectActivityLaunch,in  Bundle loginOptions);

    void setPassword(int userId,in  Account account, String password);

    void setAuthToken(int userId,in  Account account, String authTokenType, String authToken);

    void setUserData(int userId,in  Account account, String key, String value);

    void hasFeatures(int userId,in  IAccountManagerResponse response,in  Account account,in  String[] features);

    void updateCredentials(int userId,in  IAccountManagerResponse response,in  Account account, String authTokenType, boolean expectActivityLaunch,in  Bundle loginOptions);

    void editProperties(int userId,in  IAccountManagerResponse response, String accountType, boolean expectActivityLaunch);

    void getAuthTokenLabel(int userId,in  IAccountManagerResponse response, String accountType, String authTokenType);

    String getUserData(int userId,in  Account account, String key);

    String getPassword(int userId,in  Account account);

    void confirmCredentials(int userId,in  IAccountManagerResponse response,in  Account account,in  Bundle options, boolean expectActivityLaunch);

    void addAccount(int userId,in  IAccountManagerResponse response, String accountType, String authTokenType, in String[] requiredFeatures, boolean expectActivityLaunch,in  Bundle optionsIn);

    boolean addAccountExplicitly(int userId,in  Account account, String password,in  Bundle extras);

    boolean removeAccountExplicitly(int userId,in  Account account);

    void renameAccount(int userId,in  IAccountManagerResponse response,in  Account accountToRename, String newName);

    void removeAccount(int userId,in  IAccountManagerResponse response,in  Account account, boolean expectActivityLaunch);

    void clearPassword(int userId,in  Account account);

    boolean accountAuthenticated(int userId,in  Account account);

    void invalidateAuthToken(int userId, String accountType, String authToken);

    String peekAuthToken(int userId,in  Account account, String authTokenType);

    boolean setAccountVisibility(int userId,in  Account a, String packageName, int newVisibility);

    int getAccountVisibility(int userId,in  Account a, String packageName);

    void startAddAccountSession(in IAccountManagerResponse response, String accountType,
                                String authTokenType,in  String[] requiredFeatures, boolean expectActivityLaunch,
                                in Bundle options);

    void startUpdateCredentialsSession(in IAccountManagerResponse response,in  Account account,
                                       String authTokenType, boolean expectActivityLaunch,
                                       in Bundle options);

    void registerAccountListener(in String[] accountTypes, String opPackageName);

    void unregisterAccountListener(in String[] accountTypes, String opPackageName);

    Map getPackagesAndVisibilityForAccount(int userId,in  Account account);

    Map getAccountsAndVisibilityForPackage(int userId, String packageName, String accountType);

    void finishSessionAsUser(in IAccountManagerResponse response,in  Bundle sessionBundle,
                             boolean expectActivityLaunch,in  Bundle appInfo, int userId);

    void isCredentialsUpdateSuggested(in IAccountManagerResponse response,in  Account account,
                                      String statusToken);

    boolean addAccountExplicitlyWithVisibility(int userId,in  Account account, String password,in  Bundle extras,
                                               in Map visibility);
}
