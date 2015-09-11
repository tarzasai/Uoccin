package net.ggelardi.uoccin.comp;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;

public class GenresDialogPreference extends DialogPreference {
	
	private final Session session;
	private MultiAutoCompleteTextView edt;
	
	public GenresDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		session = Session.getInstance(context);
		
		setPersistent(false);
		
		setDialogLayoutResource(R.layout.dialog_genres);
	}
	
	@Override
	public void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		edt = (MultiAutoCompleteTextView) view.getRootView();
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(session.getContext(),
			android.R.layout.simple_dropdown_item_1line, session.getRes().getStringArray(R.array.def_tvdb_genres));
		edt.setAdapter(adapter);
		edt.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
		edt.setThreshold(1);
		edt.setDropDownBackgroundResource(R.color.textColorNormal);
		edt.setText(TextUtils.join(", ", session.tvdbGenreFilter()));
	}
	
	@Override
	protected Parcelable onSaveInstanceState() {
		final Parcelable superState = super.onSaveInstanceState();
		
		// Check whether this Preference is persistent (continually saved)
		if (isPersistent())
			return superState;
		
		if (edt == null)
			return null;
		
		// Create instance of custom BaseSavedState
		final SavedState ss = new SavedState(superState);
		ss.value = edt.getText().toString();
		return ss;
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		// Check whether we saved the state in onSaveInstanceState
		if (state == null || !state.getClass().equals(SavedState.class)) {
			// Didn't save the state, so call superclass
			super.onRestoreInstanceState(state);
			return;
		}
		
		// Cast state to custom BaseSavedState and pass to superclass
		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		
		// Set this Preference's widget to reflect the restored state
		edt.setText(myState.value);
	}
	
	@Override
	public void onDialogClosed(boolean positiveResult) {
		// super.onDialogClosed(positiveResult);
		
		if (positiveResult) {
			SharedPreferences.Editor editor = session.getPrefs().edit();
			String[] vals = edt.getText().toString().split(",\\s*");
			if (vals.length > 0)
				editor.putString(PK.TVDBGFLT, TextUtils.join(",", vals));
			else
				editor.remove(PK.TVDBGFLT);
			editor.commit();
		}
	}
	
	private static class SavedState extends BaseSavedState {
		// Member that holds the setting's value
		String value;
		
		public SavedState(Parcelable superState) {
			super(superState);
		}
		
		public SavedState(Parcel source) {
			super(source);
			// Get the current preference's value
			value = source.readString();
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			// Write the preference's value
			dest.writeString(value);
		}
		/*
		// Standard creator object using an instance of this class
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}
			
			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
		*/
	}
}