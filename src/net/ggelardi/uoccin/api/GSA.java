package net.ggelardi.uoccin.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.drive.DriveFolder;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

public class GSA {
	private static final String TAG = "GSA";
	
	private final Session session;
	private final Drive service;
	private File folder;
	
	public GSA(Context context) throws Exception {
		session = Session.getInstance(context);
		GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context,
			Collections.singleton(DriveScopes.DRIVE));
		credential.setSelectedAccountName(session.driveAuth());
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
	
	public String readFile(String filename, long onlyAfterUTC) throws Exception {
		Log.d(TAG, "Looking for file " + filename + "...");
		FileList files = service.files().list().setQ("title = '" + filename + "' and trashed = false and '" +
			getFolder(true).getId() + "' in parents").execute();
		if (files == null || files.isEmpty() || files.getItems().size() <= 0) {
			Log.d(TAG, "File " + filename + " does not exists.");
			return null;
		}
		File file = files.getItems().get(0);
		if (onlyAfterUTC > 0 && file.getModifiedDate().getValue() < onlyAfterUTC) {
			Log.d(TAG, "The file looks unchanged.");
			return null;
		}
		String link = file.getDownloadUrl();
		if (TextUtils.isEmpty(link)) {
			Log.w(TAG, "The file doesn't have any content stored on Drive.");
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
	
	public void writeFile(String filename, String content) throws Exception {
		Log.d(TAG, "Looking for file " + filename + "...");
		FileList files = service.files().list().setQ("title = '" + filename + "' and trashed = false and '" +
			getFolder(true).getId() + "' in parents").execute();
		String fileId = null;
		if (files != null && !files.isEmpty() && files.getItems().size() > 0)
			fileId = files.getItems().get(0).getId();
		File body = new File();
		body.setTitle(filename);
		body.setMimeType("application/json");
		body.setParents(Arrays.asList(new ParentReference().setId(getFolder(true).getId())));
		ByteArrayContent bac = ByteArrayContent.fromString("text/plain", content);
		if (TextUtils.isEmpty(fileId)) {
			File file = service.files().insert(body, bac).execute();
			Log.d(TAG, "Created file " + file.getId());
		} else {
			service.files().update(fileId, body, bac).execute();
			Log.d(TAG, "Updated file " + fileId);
		}
	}
}