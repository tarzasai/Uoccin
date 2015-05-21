package net.ggelardi.uoccin;

import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Session;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;

public class SettingsFragment extends PreferenceFragment {
	private static final String[] ACCOUNT_TYPE = new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE };
	
	/*private static final int STATE_INITIAL = 0;
	private static final int STATE_CHOOSING_ACCOUNT = 1;
	private static final int STATE_DONE = 3;*/
	
	private static final int CHOOSE_ACCOUNT = 0;
	
	private Session session;
	private GoogleAccountManager gaccman;
	private Preference accpref;
	//private int frstate;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		session = Session.getInstance(getActivity());
		gaccman = new GoogleAccountManager(getActivity());
		// Initialize the preferred account setting.
		accpref = this.findPreference(PK.GDRVAUTH);
		accpref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				chooseAccount();
				return true;
			}
		});
		//frstate = STATE_INITIAL;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Account preferenceAccount = getPreferenceAccount();
		if (preferenceAccount != null) {
			accpref.setSummary(preferenceAccount.name);
			//frstate = STATE_DONE;
			/*
		} else if (frstate == STATE_INITIAL) {
			chooseAccount();
			*/
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CHOOSE_ACCOUNT) {
			if (data != null) {
				String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				if (!TextUtils.isEmpty(accountName)) {
					Account account = gaccman.getAccountByName(accountName);
					setAccount(account);
				}
			} //else
				//frstate = STATE_INITIAL;
		}
	}
	
	private Account getPreferenceAccount() {
		//return session.driveAccountSet() ? gaccman.getAccountByName(session.driveAccountName()) : null;
		return gaccman.getAccountByName(session.driveAccountName());
	}
	
	private void chooseAccount() {
		//frstate = STATE_CHOOSING_ACCOUNT;
		Intent intent = AccountPicker.newChooseAccountIntent(getPreferenceAccount(), null, ACCOUNT_TYPE, false, null,
			null, null, null);
		startActivityForResult(intent, CHOOSE_ACCOUNT);
	}
	
	private void setAccount(Account account) {
		if (account != null) {
			session.setDriveUserAccount(account.name);
			accpref.setSummary(account.name);
			//frstate = STATE_DONE;
		}
	}
}