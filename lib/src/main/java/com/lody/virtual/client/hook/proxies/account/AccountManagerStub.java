package com.lody.virtual.client.hook.proxies.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.IAccountManagerResponse;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.lody.virtual.client.hook.base.BinderInvocationProxy;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.ipc.VAccountManager;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.Reflect;

import java.lang.reflect.Method;
import java.util.Map;

import mirror.android.accounts.IAccountManager;

/**
 * @author Lody
 */
public class AccountManagerStub extends BinderInvocationProxy {

	private static VAccountManager Mgr = VAccountManager.get();

	public AccountManagerStub() {
		super(IAccountManager.Stub.asInterface, Context.ACCOUNT_SERVICE);
	}

    @Override
    public void inject() throws Throwable {
        super.inject();
        //mService
        try{
            AccountManager accountManager = (AccountManager) getContext().getSystemService(Context.ACCOUNT_SERVICE);
            Reflect.on(accountManager).set("mService", this.getInvocationStub().getProxyInterface());
        }catch (Throwable e){
           e.printStackTrace();
        }
    }

    @Override
	protected void onBindMethods() {
		super.onBindMethods();
		addMethodProxy(new getPassword());
		addMethodProxy(new getUserData());
		addMethodProxy(new getAuthenticatorTypes());
		addMethodProxy(new getAccounts());
		addMethodProxy(new getAccountsForPackage());
		addMethodProxy(new getAccountsByTypeForPackage());
		addMethodProxy(new getAccountsAsUser());
		addMethodProxy(new hasFeatures());
		addMethodProxy(new getAccountsByFeatures());
		addMethodProxy(new addAccountExplicitly());
		addMethodProxy(new removeAccount());
		addMethodProxy(new removeAccountAsUser());
		addMethodProxy(new removeAccountExplicitly());
		addMethodProxy(new copyAccountToUser());
		addMethodProxy(new invalidateAuthToken());
		addMethodProxy(new peekAuthToken());
		addMethodProxy(new setAuthToken());
		addMethodProxy(new setPassword());
		addMethodProxy(new clearPassword());
		addMethodProxy(new setUserData());
		addMethodProxy(new updateAppPermission());
		addMethodProxy(new getAuthToken());
		addMethodProxy(new addAccount());
		addMethodProxy(new addAccountAsUser());
		addMethodProxy(new updateCredentials());
		addMethodProxy(new editProperties());
		addMethodProxy(new confirmCredentialsAsUser());
		addMethodProxy(new accountAuthenticated());
		addMethodProxy(new getAuthTokenLabel());
		addMethodProxy(new addSharedAccountAsUser());
		addMethodProxy(new getSharedAccountsAsUser());
		addMethodProxy(new removeSharedAccountAsUser());
		addMethodProxy(new renameAccount());
		addMethodProxy(new getPreviousName());
		addMethodProxy(new renameSharedAccountAsUser());

		if(BuildCompat.isOreo()) {
			addMethodProxy(new finishSessionAsUser());
			addMethodProxy(new getAccountVisibility());
			addMethodProxy(new addAccountExplicitlyWithVisibility());
			addMethodProxy(new getAccountsAndVisibilityForPackage());
			addMethodProxy(new getPackagesAndVisibilityForAccount());
			addMethodProxy(new setAccountVisibility());
			addMethodProxy(new startAddAccountSession());
			addMethodProxy(new startUpdateCredentialsSession());
			addMethodProxy(new registerAccountListener());
			addMethodProxy(new unregisterAccountListener());
		}
	}

	private static class getPassword extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getPassword";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			return Mgr.getPassword(account);
		}
	}

	private static class getUserData extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getUserData";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			String key = (String) args[1];
			return Mgr.getUserData(account, key);
		}
	}

	private static class getAuthenticatorTypes extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAuthenticatorTypes";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			return Mgr.getAuthenticatorTypes();
		}
	}

	private static class getAccounts extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAccounts";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			String accountType = (String) args[0];
			return Mgr.getAccounts(accountType);
		}
	}

	private static class getAccountsForPackage extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAccountsForPackage";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			String packageName = (String) args[0];
			return Mgr.getAccounts(null);
		}
	}

	private static class getAccountsByTypeForPackage extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAccountsByTypeForPackage";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			String type = (String) args[0];
			String packageName = (String) args[1];
			return Mgr.getAccounts(type);
		}
	}

	private static class getAccountByTypeAndFeatures extends MethodProxy{
        @Override
        public String getMethodName() {
            return "getAccountByTypeAndFeatures";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String type = (String) args[0];
            String packageName = (String) args[1];
            return Mgr.getAccounts(type);
        }
    }

	private static class getAccountsAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAccountsAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			String accountType = (String) args[0];
			return Mgr.getAccounts(accountType);
		}
	}

	private static class hasFeatures extends MethodProxy {
		@Override
		public String getMethodName() {
			return "hasFeatures";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account account = (Account) args[1];
			String[] features = (String[]) args[2];
			Mgr.hasFeatures(response, account, features);
			return 0;
		}
	}

	private static class getAccountsByFeatures extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAccountsByFeatures";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			String accountType = (String) args[1];
			String[] features = (String[]) args[2];
			Mgr.getAccountsByFeatures(response, accountType, features);
			return 0;
		}
	}

	private static class addAccountExplicitly extends MethodProxy {
		@Override
		public String getMethodName() {
			return "addAccountExplicitly";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			String password = (String) args[1];
			Bundle extras = (Bundle) args[2];
			return Mgr.addAccountExplicitly(account, password, extras);
		}
	}

	private static class removeAccount extends MethodProxy {
		@Override
		public String getMethodName() {
			return "removeAccount";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account account = (Account) args[1];
			boolean expectActivityLaunch = (boolean) args[2];
			Mgr.removeAccount(response, account, expectActivityLaunch);
			return 0;
		}
	}

	private static class removeAccountAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "removeAccountAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account account = (Account) args[1];
			boolean expectActivityLaunch = (boolean) args[2];
			Mgr.removeAccount(response, account, expectActivityLaunch);
			return 0;
		}
	}

	private static class removeAccountExplicitly extends MethodProxy {
		@Override
		public String getMethodName() {
			return "removeAccountExplicitly";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			return Mgr.removeAccountExplicitly(account);
		}
	}

	private static class copyAccountToUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "copyAccountToUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account account = (Account) args[1];
			int userFrom = (int) args[2];
			int userTo = (int) args[3];
			method.invoke(who, args);
			return 0;
		}
	}

	private static class invalidateAuthToken extends MethodProxy {
		@Override
		public String getMethodName() {
			return "invalidateAuthToken";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			String accountType = (String) args[0];
			String authToken = (String) args[1];
			Mgr.invalidateAuthToken(accountType, authToken);
			return 0;
		}
	}

	private static class peekAuthToken extends MethodProxy {
		@Override
		public String getMethodName() {
			return "peekAuthToken";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			String authTokenType = (String) args[1];
			return Mgr.peekAuthToken(account, authTokenType);
		}
	}

	private static class setAuthToken extends MethodProxy {
		@Override
		public String getMethodName() {
			return "setAuthToken";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			String authTokenType = (String) args[1];
			String authToken = (String) args[2];
			Mgr.setAuthToken(account, authTokenType, authToken);
			return 0;
		}
	}

	private static class setPassword extends MethodProxy {
		@Override
		public String getMethodName() {
			return "setPassword";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			String password = (String) args[1];
			Mgr.setPassword(account, password);
			return 0;
		}
	}

	private static class clearPassword extends MethodProxy {
		@Override
		public String getMethodName() {
			return "clearPassword";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			Mgr.clearPassword(account);
			return 0;
		}
	}

	private static class setUserData extends MethodProxy {
		@Override
		public String getMethodName() {
			return "setUserData";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			String key = (String) args[1];
			String value = (String) args[2];
			Mgr.setUserData(account, key, value);
			return 0;
		}
	}

	private static class updateAppPermission extends MethodProxy {
		@Override
		public String getMethodName() {
			return "updateAppPermission";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			String authTokenType = (String) args[1];
			int uid = (int) args[2];
			boolean val = (boolean) args[3];
			method.invoke(who, args);
			return 0;
		}
	}

	private static class getAuthToken extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAuthToken";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account account = (Account) args[1];
			String authTokenType = (String) args[2];
			boolean notifyOnAuthFailure = (boolean) args[3];
			boolean expectActivityLaunch = (boolean) args[4];
			Bundle options = (Bundle) args[5];
			Mgr.getAuthToken(response, account, authTokenType, notifyOnAuthFailure, expectActivityLaunch, options);
			return 0;
		}
	}

	private static class addAccount extends MethodProxy {
		@Override
		public String getMethodName() {
			return "addAccount";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			String accountType = (String) args[1];
			String authTokenType = (String) args[2];
			String[] requiredFeatures = (String[]) args[3];
			boolean expectActivityLaunch = (boolean) args[4];
			Bundle options = (Bundle) args[5];
			Mgr.addAccount(response, accountType, authTokenType, requiredFeatures, expectActivityLaunch, options);
			return 0;
		}
	}

	private static class addAccountAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "addAccountAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			String accountType = (String) args[1];
			String authTokenType = (String) args[2];
			String[] requiredFeatures = (String[]) args[3];
			boolean expectActivityLaunch = (boolean) args[4];
			Bundle options = (Bundle) args[5];
			Mgr.addAccount(response, accountType, authTokenType, requiredFeatures, expectActivityLaunch, options);
			return 0;
		}
	}

	private static class updateCredentials extends MethodProxy {
		@Override
		public String getMethodName() {
			return "updateCredentials";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account account = (Account) args[1];
			String authTokenType = (String) args[2];
			boolean expectActivityLaunch = (boolean) args[3];
			Bundle options = (Bundle) args[4];
			Mgr.updateCredentials(response, account, authTokenType, expectActivityLaunch, options);
			return 0;
		}
	}

	private static class editProperties extends MethodProxy {
		@Override
		public String getMethodName() {
			return "editProperties";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			String authTokenType = (String) args[1];
			boolean expectActivityLaunch = (boolean) args[2];
			Mgr.editProperties(response, authTokenType, expectActivityLaunch);
			return 0;
		}
	}

	private static class confirmCredentialsAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "confirmCredentialsAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account account = (Account) args[1];
			Bundle options = (Bundle) args[2];
			boolean expectActivityLaunch = (boolean) args[3];
			Mgr.confirmCredentials(response, account, options, expectActivityLaunch);
			return 0;

		}
	}

	private static class accountAuthenticated extends MethodProxy {
		@Override
		public String getMethodName() {
			return "accountAuthenticated";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			return Mgr.accountAuthenticated(account);
		}
	}

	private static class getAuthTokenLabel extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getAuthTokenLabel";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			String accountType = (String) args[1];
			String authTokenType = (String) args[2];
			Mgr.getAuthTokenLabel(response, accountType, authTokenType);
			return 0;
		}
	}

	private static class addSharedAccountAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "addSharedAccountAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			int userId = (int) args[1];
			return method.invoke(who, args);
		}
	}

	private static class getSharedAccountsAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getSharedAccountsAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			int userId = (int) args[0];
			return method.invoke(who, args);
		}
	}

	private static class removeSharedAccountAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "removeSharedAccountAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			int userId = (int) args[1];
			return method.invoke(who, args);
		}
	}

	private static class renameAccount extends MethodProxy {
		@Override
		public String getMethodName() {
			return "renameAccount";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			IAccountManagerResponse response = (IAccountManagerResponse) args[0];
			Account accountToRename = (Account) args[1];
			String newName = (String) args[2];
			Mgr.renameAccount(response, accountToRename, newName);
			return 0;
		}
	}

	private static class getPreviousName extends MethodProxy {
		@Override
		public String getMethodName() {
			return "getPreviousName";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account account = (Account) args[0];
			return Mgr.getPreviousName(account);
		}
	}

	private static class renameSharedAccountAsUser extends MethodProxy {
		@Override
		public String getMethodName() {
			return "renameSharedAccountAsUser";
		}

		@Override
		public Object call(Object who, Method method, Object... args) throws Throwable {
			Account accountToRename = (Account) args[0];
			String newName = (String) args[1];
			int userId = (int) args[2];
			return method.invoke(who, args);
		}
	}
	@TargetApi(Build.VERSION_CODES.O)
	private static class isCredentialsUpdateSuggested extends MethodProxy {
		private isCredentialsUpdateSuggested() {
		}

		public String getMethodName() {
			return "isCredentialsUpdateSuggested";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			Mgr.isCredentialsUpdateSuggested((IAccountManagerResponse) objArr[0], (Account) objArr[1], (String) objArr[2]);
			return 0;
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class finishSessionAsUser extends MethodProxy {
		private finishSessionAsUser() {
		}

		public String getMethodName() {
			return "finishSessionAsUser";
		}

		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			AccountManagerStub.Mgr.finishSessionAsUser((IAccountManagerResponse) objArr[0], (Bundle) objArr[1], ((Boolean) objArr[2]).booleanValue(), (Bundle) objArr[3], ((Integer) objArr[4]).intValue());
			return Integer.valueOf(0);
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class getAccountVisibility extends MethodProxy {
		private getAccountVisibility() {
		}

		public String getMethodName() {
			return "getAccountVisibility";
		}

		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			return Integer.valueOf(AccountManagerStub.Mgr.getAccountVisibility((Account) objArr[0], (String) objArr[1]));
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class addAccountExplicitlyWithVisibility extends MethodProxy {
		private addAccountExplicitlyWithVisibility() {
		}

		public String getMethodName() {
			return "addAccountExplicitlyWithVisibility";
		}

		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			return Mgr.addAccountExplicitlyWithVisibility((Account) objArr[0], (String) objArr[1], (Bundle) objArr[2], (Map) objArr[3]);
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class getAccountsAndVisibilityForPackage extends MethodProxy {
		private getAccountsAndVisibilityForPackage() {
		}

		public String getMethodName() {
			return "getAccountsAndVisibilityForPackage";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			return Mgr.getAccountsAndVisibilityForPackage((String) objArr[0], (String) objArr[1]);
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class getPackagesAndVisibilityForAccount extends MethodProxy {
		private getPackagesAndVisibilityForAccount() {
		}

		public String getMethodName() {
			return "getPackagesAndVisibilityForAccount";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			return Mgr.getPackagesAndVisibilityForAccount((Account) objArr[0]);
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class setAccountVisibility extends MethodProxy {
		private setAccountVisibility() {
		}

		public String getMethodName() {
			return "setAccountVisibility";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			return Mgr.setAccountVisibility((Account) objArr[0], (String) objArr[1], ((Integer) objArr[2]).intValue());
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class startAddAccountSession extends MethodProxy {
		private startAddAccountSession() {
		}

		public String getMethodName() {
			return "startAddAccountSession";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			Mgr.startAddAccountSession((IAccountManagerResponse) objArr[0], (String) objArr[1], (String) objArr[2], (String[]) objArr[3], ((Boolean) objArr[4]).booleanValue(), (Bundle) objArr[5]);
			return 0;
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class startUpdateCredentialsSession extends MethodProxy {
		private startUpdateCredentialsSession() {
		}

		public String getMethodName() {
			return "startUpdateCredentialsSession";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			Mgr.startUpdateCredentialsSession((IAccountManagerResponse) objArr[0], (Account) objArr[1], (String) objArr[2], ((Boolean) objArr[3]).booleanValue(), (Bundle) objArr[4]);
			return 0;
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class registerAccountListener extends MethodProxy {
		private registerAccountListener() {
		}

		public String getMethodName() {
			return "registerAccountListener";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			Mgr.registerAccountListener((String[]) objArr[0], (String) objArr[1]);
			return 0;
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private static class unregisterAccountListener extends MethodProxy {
		private unregisterAccountListener() {
		}

		public String getMethodName() {
			return "unregisterAccountListener";
		}

		@Override
		public Object call(Object obj, Method method, Object... objArr) throws Throwable {
			Mgr.unregisterAccountListener((String[]) objArr[0], (String) objArr[1]);
			return 0;
		}
	}

}
