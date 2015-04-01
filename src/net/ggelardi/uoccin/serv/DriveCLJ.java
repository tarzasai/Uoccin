package net.ggelardi.uoccin.serv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;

import net.ggelardi.uoccin.R;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.drive.DriveFolder;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

//http://stackoverflow.com/a/25871516/28852

//http://developer.android.com/google/auth/http-auth.html
//https://github.com/hanscappelle/more-android-examples/blob/master/DriveQuickstart/src/com/example/drivequickstart/MainActivity.java
//https://github.com/gabrielemariotti/androiddev/tree/master/GoogleAccount/src/it/gmariotti/android/examples/googleaccount

public class DriveCLJ {
	private static final String TAG = "DriveCLJ";
	
	private final Session session;
	private String account;
	private GoogleAccountCredential credential;
	private Drive service;
	private File folder;
	
	public DriveCLJ(Context context) throws Exception {
		session = Session.getInstance(context);
		account = session.driveAuth();
		credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE));
		credential.setSelectedAccountName(account);
		service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).
			setApplicationName(session.getString(R.string.app_name)).build();
	}
	
	public File getFolder(boolean create) throws Exception {
		if (folder != null)
			return folder;
		Log.d(TAG, "Looking for Uoccin folder...");
		FileList files = service.files().list().setQ("mimeType = '" + DriveFolder.MIME_TYPE +
			"' and title = '" + Commons.GD.FOLDER + "' and trashed = false").execute();
		if (files != null && !files.isEmpty() && files.getItems().size() > 0) {
			Log.d(TAG, "Uoccin folder found");
			folder = files.getItems().get(0);
		} else if (create) {
			Log.d(TAG, "Creating Uoccin folder...");
			File body = new File();
			body.setTitle(Commons.GD.FOLDER);
			body.setMimeType(DriveFolder.MIME_TYPE);
			folder = service.files().insert(body).execute();
		} else
			Log.d(TAG, "Uoccin folder NOT found");
		return folder;
	}
	
	public String readFile(String filename) throws Exception {
		Log.d(TAG, "Looking for file " + filename + "...");
		FileList files = service.files().list().setQ("title = '" + filename + "' and trashed = false and '" +
			getFolder(true).getId() + "' in parents").execute();
		if (files == null || files.isEmpty() || files.getItems().size() <= 0) {
			Log.d(TAG, "File " + filename + " does not exists.");
			return null;
		}
		File file = files.getItems().get(0);
		String link = file.getDownloadUrl();
		if (TextUtils.isEmpty(link)) {
			Log.d(TAG, "The file doesn't have any content stored on Drive.");
			return null;
		}
		Log.d(TAG, "Opening file at " + link);
		HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(link)).execute();
		InputStream inputStream = null;
		try {
			inputStream = resp.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				content.append(line);
			return content.toString();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
	}
	
	public void writeFile(String filename, String content) {
		//
	}
}