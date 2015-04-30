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
import net.ggelardi.uoccin.serv.Commons.MIME;
import net.ggelardi.uoccin.serv.Commons.SDF;
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
		credential.setSelectedAccountName(session.driveAccountName());
		service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(),
			credential).setApplicationName(session.getString(R.string.app_name)).build();
	}
	
	public List<Change> getChanges() throws Exception {
		Log.d(TAG, "Looking for changes in Drive's Uoccin folder...");
		long lcid = session.driveLastChangeID();
		Changes.List request = service.changes().list().setIncludeDeleted(false).setIncludeSubscribed(
			false).setFields("items/file,largestChangeId,nextPageToken").setStartChangeId(lcid + 1);
		List<Change> result = new ArrayList<Change>();
		ChangeList changes;
		do {
			try {
				changes = request.execute();
				for (Change change: changes.getItems()) {
					// looks like List.setIncludeDeleted() has no effects. btw Change.getDeleted() always returns null
					Boolean deleted = change.getDeleted();
					if (deleted != null && deleted)
						continue;
					deleted = change.getFile().getExplicitlyTrashed();
					if (deleted != null && deleted)
						continue;
					for (ParentReference parent: change.getFile().getParents())
						if (parent.getId().equals(getFolder(false).getId())) {
							result.add(change);
							break;
						}
				}
				lcid = changes.getLargestChangeId();
				request.setPageToken(changes.getNextPageToken());
			} catch (Exception err) {
				Log.e(TAG, "getChanges", err);
				request.setPageToken(null);
			}
		} while (request.getPageToken() != null && request.getPageToken().length() > 0);
		session.setDriveLastChangeID(lcid);
		Log.i(TAG, "Found " + Integer.toString(result.size()) + " changes since last check.");
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
	
	public File getFile(String filename, Long newerThanUTC) throws Exception {
		Log.d(TAG, "Looking for file " + filename + "...");
		String query = "title = '" + filename + "' and trashed = false and '" +
			getFolder(true).getId() + "' in parents";
		if (newerThanUTC != null && newerThanUTC > 0)
			query += " and modifiedDate > '" + SDF.rfc3339(newerThanUTC) + "'";
		FileList fl = service.files().list().setQ(query).execute();
		return fl != null && !fl.isEmpty() && fl.getItems().size() > 0 ? fl.getItems().get(0) : null;
	}
	
	public String readFile(File file) throws Exception {
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
	
	public void writeFile(String fileId, String title, String mime, String content) throws Exception {
		File body = new File();
		body.setTitle(title);
		body.setMimeType(mime);
		body.setParents(Arrays.asList(new ParentReference().setId(getFolder(true).getId())));
		ByteArrayContent bac = ByteArrayContent.fromString(MIME.TEXT, content);
		if (TextUtils.isEmpty(fileId)) {
			File file = service.files().insert(body, bac).execute();
			Log.i(TAG, title + " created, id " + file.getId());
		} else {
			service.files().update(fileId, body, bac).execute();
			Log.i(TAG, title + " updated, id " + fileId);
		}
	}
}