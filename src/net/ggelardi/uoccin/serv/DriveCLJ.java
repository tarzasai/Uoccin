package net.ggelardi.uoccin.serv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;

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

//https://github.com/hanscappelle/more-android-examples/blob/master/DriveQuickstart/src/com/example/drivequickstart/MainActivity.java
//https://github.com/gabrielemariotti/androiddev/tree/master/GoogleAccount/src/it/gmariotti/android/examples/googleaccount

public class DriveCLJ {
	private static final String TAG = "DriveCLJ";
	
	private final Session session;
	private String accountName;
	private GoogleAccountCredential credential;
	private Drive service;
	
	public DriveCLJ(Context context) {
		session = Session.getInstance(context);
		credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE));
		
		accountName = session.driveAccount();
		if (!TextUtils.isEmpty(accountName)) {
			credential.setSelectedAccountName(accountName);
			service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
		}
	}
	
	private File getFolder(boolean create) throws Exception {
		Log.d(TAG, "Looking for Uoccin folder...");
		FileList files = service.files().list().setQ("mimeType = '" + DriveFolder.MIME_TYPE +
			"' and title = '" + Commons.GD.FOLDER + "'").execute();
		if (files != null && !files.isEmpty()) {
			Log.d(TAG, "Uoccin folder found");
			return files.getItems().get(0);
		}
		if (create) {
			Log.d(TAG, "Creating Uoccin folder...");
			File body = new File();
			body.setTitle(Commons.GD.FOLDER);
			body.setMimeType(DriveFolder.MIME_TYPE);
			return service.files().insert(body).execute();
		}
		Log.d(TAG, "Uoccin folder NOT found");
		return null;
	}
	
	public String readFile(String filename) throws Exception {
		Log.d(TAG, "Looking for file " + filename + "...");
		FileList files = service.files().list().setQ("mimeType = 'application/json' and '" +
			getFolder(true).getId() + "' in parents").execute();
		if (files != null && !files.isEmpty()) {
			File file = files.getItems().get(0);
			if (!TextUtils.isEmpty(file.getDownloadUrl())) {
				Log.d(TAG, "Opening file at " + file.getDownloadUrl());
				GenericUrl downloadUrl = new GenericUrl(file.getDownloadUrl());
				HttpResponse resp = service.getRequestFactory().buildGetRequest(downloadUrl).execute();
				InputStream inputStream = null;
				try {
					inputStream = resp.getContent();
					BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
					StringBuilder content = new StringBuilder();
					char[] buffer = new char[1024];
					int num;
					while ((num = reader.read(buffer)) > 0)
						content.append(buffer, 0, num);
					return content.toString();
				} finally {
					if (inputStream != null)
						inputStream.close();
				}
			} else {
				Log.d(TAG, "The file doesn't have any content stored on Drive.");
				return null;
			}
		}
		Log.d(TAG, "The file does not exists.");
		return null;
	}
	
	public void writeFile(String filename, String content) {
		//
	}
}