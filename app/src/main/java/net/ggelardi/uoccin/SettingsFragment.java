package net.ggelardi.uoccin;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;

import net.ggelardi.uoccin.comp.IntListPreference;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;

public class SettingsFragment extends PreferenceFragment {
	private static final String[] ACCOUNT_TYPE = new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE };
	private static final int CHOOSE_ACCOUNT = 0;

	private Session session;
	private GoogleAccountManager gaccman;
	private Preference cpdauth;
	private ListPreference lpstart;
	private ListPreference lplocal;
	private IntListPreference lpsyint;
	private EditTextPreference epduuid;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		session = Session.getInstance(getActivity());
		gaccman = new GoogleAccountManager(getActivity());

		cpdauth = findPreference(Commons.PK.GDRVAUTH);
		cpdauth.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
						new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null);
				startActivityForResult(googlePicker, CHOOSE_ACCOUNT);
				return true;
			}
		});

		lpstart = (ListPreference) findPreference(Commons.PK.STARTUPV);
		lpstart.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary(getStartupDescr(newValue.toString()));
				return true;
			}
		});

		lplocal = (ListPreference) findPreference(Commons.PK.LANGUAGE);
		lplocal.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary(getLanguageDescr(newValue.toString()));
				return true;
			}
		});

		lpsyint = (IntListPreference) findPreference(Commons.PK.GDRVINTV);
		lpsyint.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary(getIntervalDescr(newValue.toString()));
				return true;
			}
		});

		epduuid = (EditTextPreference) findPreference(Commons.PK.GDRVUUID);
		epduuid.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary(newValue.toString());
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		lpstart.setSummary(getStartupDescr(null));
		lplocal.setSummary(getLanguageDescr(null));
		lpsyint.setSummary(getIntervalDescr(null));
		epduuid.setSummary(session.driveDeviceID());

		Account preferenceAccount = getPreferenceAccount();
		if (preferenceAccount != null)
			cpdauth.setSummary(preferenceAccount.name);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CHOOSE_ACCOUNT && data != null) {
			Account accsel = gaccman.getAccountByName(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
			if (accsel != null) {
				session.setDriveUserAccount(accsel.name);
				cpdauth.setSummary(accsel.name);
			}
		}
	}

	private String getStartupDescr(String id) {
		if (TextUtils.isEmpty(id))
			id = Integer.toString(session.getPrefs().getInt(Commons.PK.STARTUPV, 0));
		String[] keys = session.getRes().getStringArray(R.array.view_defser_ids);
		String[] vals = session.getRes().getStringArray(R.array.view_defser_titles);
		for (int i = 0; i < keys.length; i++)
			if (keys[i].equals(id))
				return vals[i];
		return "";
	}

	private String getLanguageDescr(String id) {
		if (TextUtils.isEmpty(id))
			id = session.getPrefs().getString(Commons.PK.LANGUAGE, "en");
		String[] keys = session.getRes().getStringArray(R.array.pk_language_values);
		String[] vals = session.getRes().getStringArray(R.array.pk_language_names);
		for (int i = 0; i < keys.length; i++)
			if (keys[i].equals(id))
				return vals[i];
		return "";
	}

	private String getIntervalDescr(String id) {
		if (TextUtils.isEmpty(id))
			id = Integer.toString(session.getPrefs().getInt(Commons.PK.GDRVINTV, 30));
		String[] keys = session.getRes().getStringArray(R.array.pk_gdrvintv_values);
		String[] vals = session.getRes().getStringArray(R.array.pk_gdrvintv_names);
		for (int i = 0; i < keys.length; i++)
			if (keys[i].equals(id))
				return vals[i];
		return "";
	}

	private Account getPreferenceAccount() {
		return gaccman.getAccountByName(session.driveAccountName());
	}
}
