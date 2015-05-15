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
import com.google.api.services.drive.Drive.Children;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

public class GSA {
	private static final String TAG = "GSA";
	
	private final Session session;
	private final Drive service;
	private String rootId;
	private String deviceId;
	
	public GSA(Context context) throws Exception {
		session = Session.getInstance(context);
		GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context,
			Collections.singleton(DriveScopes.DRIVE));
		credential.setSelectedAccountName(session.driveAccountName());
		service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(),
			credential).setApplicationName(session.getString(R.string.app_name)).build();
	}
	
	public String getRootFolder(boolean create) throws Exception {
		if (!TextUtils.isEmpty(rootId))
			return rootId;
		Log.d(TAG, "Looking for Uoccin folder...");
		FileList files = service.files().list().setQ("mimeType = '" + DriveFolder.MIME_TYPE +
			"' and title = '" + Commons.GD.FOLDER + "' and trashed = false").execute();
		if (files != null && !files.isEmpty() && files.getItems().size() > 0) {
			Log.d(TAG, "Uoccin folder found");
			rootId = files.getItems().get(0).getId();
		} else if (create) {
			Log.i(TAG, "Creating Uoccin folder...");
			File body = new File();
			body.setTitle(Commons.GD.FOLDER);
			body.setMimeType(DriveFolder.MIME_TYPE);
			rootId = service.files().insert(body).execute().getId();
		} else
			Log.w(TAG, "Uoccin folder NOT found");
		return rootId;
	}
	
	public String getDeviceFolder(boolean create) throws Exception {
		if (!TextUtils.isEmpty(deviceId))
			return deviceId;
		if (!TextUtils.isEmpty(getRootFolder(create))) {
			Log.d(TAG, "Looking for device folder...");
			String dfName = "device." + session.driveDeviceID();
			ChildList children = service.children().list(rootId).setQ("mimeType = '" +
				DriveFolder.MIME_TYPE + "' and title = '" + dfName + "' and trashed = false").execute();
			if (children != null && !children.isEmpty() && children.getItems().size() > 0) {
				Log.d(TAG, "Device folder found");
				deviceId = children.getItems().get(0).getId();
			} else if (create) {
				Log.i(TAG, "Creating device folder...");
				File body = new File();
				body.setTitle(dfName);
				body.setMimeType(DriveFolder.MIME_TYPE);
				body.setParents(Arrays.asList(new ParentReference().setId(rootId)));
				deviceId = service.files().insert(body).execute().getId();
			} else
				Log.w(TAG, "Device folder NOT found");
		}
		return deviceId;
	}
	
	public List<String> getOtherFoldersIds() throws Exception {
		getDeviceFolder(true); // required
		Log.d(TAG, "Looking for other devices folders...");
		List<String> res = new ArrayList<String>();
		ChildList children;
		Children.List request = service.children().list(rootId).setQ("mimeType = '" +
			DriveFolder.MIME_TYPE + "' and title contains 'device.' and trashed = false");
		do {
			try {
				children = request.execute();
				for (ChildReference child : children.getItems())
					if (!deviceId.equals(child.getId()))
						res.add(child.getId());
				request.setPageToken(children.getNextPageToken());
			} catch (Exception err) {
				Log.e(TAG, "getDeviceFolders", err);
				request.setPageToken(null);
			}
		} while (request.getPageToken() != null && request.getPageToken().length() > 0);
		return res;
	}
	
	public List<String> getNewDiffs() throws Exception {
		getDeviceFolder(true); // required
		Log.d(TAG, "Looking for updates in the device folder...");
		List<String> result = new ArrayList<String>();
		long lcid = session.driveLastChangeID();
		ChangeList changes;
		File file;
		Changes.List request = service.changes().list().setIncludeDeleted(false).setIncludeSubscribed(
			false).setFields("items/file,largestChangeId,nextPageToken").setStartChangeId(lcid + 1);
		do {
			try {
				changes = request.execute();
				for (Change change: changes.getItems()) {
					file = change.getFile();
					if (!file.getTitle().endsWith(".diff"))
						continue;
					// check for deleted (looks like List.setIncludeDeleted(false) has no effects)
					Boolean deleted = change.getDeleted();
					if (deleted != null && deleted)
						continue;
					deleted = file.getExplicitlyTrashed();
					if (deleted != null && deleted)
						continue;
					for (ParentReference parent: file.getParents())
						if (parent.getId().equals(deviceId)) {
							result.add(file.getId());
							break;
						}
				}
				lcid = changes.getLargestChangeId();
				request.setPageToken(changes.getNextPageToken());
			} catch (Exception err) {
				Log.e(TAG, "getNewDiffs", err);
				request.setPageToken(null);
			}
		} while (request.getPageToken() != null && request.getPageToken().length() > 0);
		session.setDriveLastChangeID(lcid);
		Log.i(TAG, "Found " + Integer.toString(result.size()) + " diffs since last check.");
		return result;
	}
	
	public File getFile(String filename, String folderId, Long newerThanUTC) throws Exception {
		Log.d(TAG, "Looking for file " + filename + "...");
		String query = "title = '" + filename + "' and '" + folderId + "' in parents and trashed = false";
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
		Log.d(TAG, "Opening file " + file.getTitle());
		HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(link)).execute();
		InputStream inputStream = null;
		try {
			inputStream = resp.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder content = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				content.append(line).append("\n");
			return content.toString();
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
	}
	
	public String readFile(String fileId) throws Exception {
		return readFile(service.files().get(fileId).execute());
	}
	
	public void writeFile(String fileId, String folderId, String title, String mime, String content) throws Exception {
		File body = new File();
		body.setTitle(title);
		body.setMimeType(mime);
		body.setParents(Arrays.asList(new ParentReference().setId(folderId)));
		ByteArrayContent bac = ByteArrayContent.fromString(MIME.TEXT, content);
		if (TextUtils.isEmpty(fileId)) {
			File file = service.files().insert(body, bac).execute();
			Log.i(TAG, title + " created, id " + file.getId());
		} else {
			service.files().update(fileId, body, bac).execute();
			Log.i(TAG, title + " updated, id " + fileId);
		}
	}
	
	public void deleteFile(String fileId) throws Exception {
		service.files().delete(fileId).execute();
	}
}