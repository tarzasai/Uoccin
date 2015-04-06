package net.ggelardi.uoccin.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import com.google.api.services.drive.Drive.Changes;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
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
		credential.setSelectedAccountName(session.driveUserAccount());
		service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).
			setApplicationName(session.getString(R.string.app_name)).build();
	}
	
	public List<Change> getChanges() throws Exception {
		Log.d(TAG, "Looking for changes in Drive files...");
		long lcid = session.driveLastChangeID();
		Changes.List request = service.changes().list().setIncludeDeleted(false).setIncludeSubscribed(
			false).setFields("items/file,largestChangeId,nextPageToken").setStartChangeId(lcid + 1);
		List<Change> result = new ArrayList<Change>();
		ChangeList changes;
		do {
			try {
				changes = request.execute();
				result.addAll(changes.getItems());
				lcid = changes.getLargestChangeId();
				request.setPageToken(changes.getNextPageToken());
			} catch (Exception err) {
				Log.e(TAG, "getChanges", err);
				request.setPageToken(null);
			}
		} while (request.getPageToken() != null && request.getPageToken().length() > 0);
		session.setDriveLastChangeID(lcid);
		if (result.isEmpty())
			Log.d(TAG, "Zero changes found.");
		else
			Log.i(TAG, "Found " + Integer.toString(result.size()) + " new changes since last check.");
		return result;
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
			Log.i(TAG, "Creating Uoccin folder...");
			File body = new File();
			body.setTitle(Commons.GD.FOLDER);
			body.setMimeType(DriveFolder.MIME_TYPE);
			folder = service.files().insert(body).execute();
		} else
			Log.w(TAG, "Uoccin folder NOT found");
		return folder;
	}
	
	public String readFile(String filename, long onlyAfterUTC) throws Exception {
		Log.d(TAG, "Looking for file " + filename + "...");
		FileList files = service.files().list().setQ("title = '" + filename + "' and trashed = false and '" +
			getFolder(true).getId() + "' in parents").execute();
		if (files == null || files.isEmpty() || files.getItems().size() <= 0) {
			Log.i(TAG, "File " + filename + " does not exists.");
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
			Log.i(TAG, filename + " downloaded, " + content.length() + " chars.");
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
			Log.i(TAG, filename + " created, id " + file.getId());
		} else {
			service.files().update(fileId, body, bac).execute();
			Log.i(TAG, filename + " updated, id " + fileId);
		}
	}
}