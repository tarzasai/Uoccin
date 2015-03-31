package net.ggelardi.uoccin.serv;


public class SyncGAC {
	// @formatter:off
	/*
	private static final String TAG = "SyncGAC";
	
	private final Session session;
	private final GoogleApiClient gac;
	
	public SyncGAC(Session session) {
		this.session = session;
		this.gac = new GoogleApiClient.Builder(session.getContext()).addApi(Drive.API).addScope(Drive.SCOPE_FILE).build();
	}
	
	public SyncGAC(Context context) {
		this(Session.getInstance(context));
	}
	
	public synchronized void connect() throws Exception {
		if (gac.isConnected())
			return;
		Log.v(TAG, "Connecting GAC");
		ConnectionResult cr = gac.blockingConnect();
		if (!cr.isSuccess())
			throw new Exception(Commons.SN.CONNECT_FAIL);
		Log.v(TAG, "Connected");
	}
	
	public String checkRID(String rid) {
		if (rid.equals(session.getPrefs().getString(getPK(Commons.GD.FOLDER), "?")))
			return Commons.GD.FOLDER;
		if (rid.equals(session.getPrefs().getString(getPK(Commons.GD.MOV_WLST), "?")))
			return Commons.GD.MOV_WLST;
		if (rid.equals(session.getPrefs().getString(getPK(Commons.GD.MOV_COLL), "?")))
			return Commons.GD.MOV_COLL;
		if (rid.equals(session.getPrefs().getString(getPK(Commons.GD.MOV_SEEN), "?")))
			return Commons.GD.MOV_SEEN;
		if (rid.equals(session.getPrefs().getString(getPK(Commons.GD.SER_WLST), "?")))
			return Commons.GD.SER_WLST;
		if (rid.equals(session.getPrefs().getString(getPK(Commons.GD.SER_COLL), "?")))
			return Commons.GD.SER_COLL;
		if (rid.equals(session.getPrefs().getString(getPK(Commons.GD.SER_SEEN), "?")))
			return Commons.GD.SER_SEEN;
		return null;
	}
	
	public void saveRID(String filename, String rid) {
		Log.d(TAG, "Saving resource id for " + filename + ": " + rid);
		SharedPreferences.Editor editor = session.getPrefs().edit();
		editor.putString(getPK(filename), rid);
		editor.commit();
	}
	
	public void dropRID(String filename) {
		SharedPreferences.Editor editor = session.getPrefs().edit();
		editor.remove(getPK(filename));
		editor.commit();
	}
	
	public GoogleApiClient getClient() throws Exception {
		connect();
		return gac;
	}
	
	public synchronized DriveFolder getFolder() throws Exception {
		connect();
		DriveFolder res = null;
		String rid = session.getPrefs().getString(getPK(Commons.GD.FOLDER), null);
		if (rid != null) {
			Log.v(TAG, "Checking folder resourceID " + rid);
			DriveIdResult dr = Drive.DriveApi.fetchDriveId(gac, rid).await();
			if (dr.getStatus().isSuccess()) {
				res = Drive.DriveApi.getFolder(gac, dr.getDriveId());
				MetadataResult mr = res.getMetadata(gac).await();
				if (!mr.getStatus().isSuccess())
					throw new Exception(mr.getStatus().getStatusMessage());
				Metadata md = mr.getMetadata();
				if (!(md.getTitle().equals(Commons.GD.FOLDER) && md.isDataValid()) || md.isTrashed())
					res = null;
			}
			if (res == null) {
				rid = null;
				dropRID(Commons.GD.FOLDER);
			}
		}
		if (rid == null) {
			res = findFolder();
			if (res == null)
				res = createFolder();
		}
		return res;
	}
	
	public DriveFile getFile(DriveId driveId) throws Exception {
		connect();
		Log.v(TAG, "Checking file by driveId " + driveId.encodeToString());
		DriveFile res = Drive.DriveApi.getFile(gac, driveId);
		MetadataResult mr = res.getMetadata(gac).await();
		if (!mr.getStatus().isSuccess())
			throw new Exception(mr.getStatus().getStatusMessage());
		Metadata md = mr.getMetadata();
		if (!md.isDataValid() || md.isTrashed())
			return null;
		// check filename (for DriveService)
		String chk = md.getTitle();
		if (!(chk.equals(Commons.GD.MOV_WLST) || chk.equals(Commons.GD.MOV_COLL) ||
			chk.equals(Commons.GD.MOV_SEEN) || chk.equals(Commons.GD.SER_WLST) ||
			chk.equals(Commons.GD.SER_COLL) || chk.equals(Commons.GD.SER_SEEN)))
			return null;
		// check if it's actually in the Uoccin folder
		MetadataBufferResult br = res.listParents(gac).await();
		if (!br.getStatus().isSuccess())
			throw new Exception(br.getStatus().getStatusMessage());
		MetadataBuffer mb = br.getMetadataBuffer();
		try {
			for (int i = 0; i < mb.getCount(); i++) {
				md = mb.get(i);
				if (md.isFolder() && md.getTitle().equals(Commons.GD.FOLDER) && md.isDataValid() &&
					!md.isTrashed())
					return res;
			}
		} finally {
			mb.release();
		}
		return null;
	}
	
	public DriveFile getFile(String resourceId) throws Exception {
		connect();
		Log.v(TAG, "Checking file by resourceId " + resourceId);
		DriveIdResult dr = Drive.DriveApi.fetchDriveId(gac, resourceId).await();
		if (dr.getStatus().isSuccess())
			return getFile(dr.getDriveId());
		return null; // invalid resourceId
	}
	
	public synchronized DriveFile getFile(String filename, boolean create) throws Exception {
		connect();
		DriveFile res = null;
		String rid = session.getPrefs().getString(getPK(filename), null);
		if (rid != null) {
			res = getFile(rid);
			if (res == null) {
				rid = null;
				dropRID(filename);
			}
		}
		if (rid == null) {
			res = findFile(filename);
			if (res == null && create)
				res = createFile(filename);
		}
		return res;
	}
	
	public Metadata getMetadata(DriveFile df) throws Exception {
		connect();
		Log.v(TAG, "Reading file metadata");
		MetadataResult mr = df.getMetadata(gac).await();
		if (!mr.getStatus().isSuccess())
			throw new Exception(mr.getStatus().getStatusMessage());
		Log.v(TAG, "Metadata read");
		return mr.getMetadata();
	}
	
	public String readContent(DriveFile df) throws Exception {
		connect();
		Log.v(TAG, "Reading file content");
		DriveContentsResult cr = df.open(gac, DriveFile.MODE_READ_ONLY, null).await();
		if (!cr.getStatus().isSuccess())
			throw new Exception(cr.getStatus().getStatusMessage());
		DriveContents dc = cr.getDriveContents();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(dc.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				builder.append(line);
			return builder.toString();
		} finally {
			dc.discard(gac);
			Log.v(TAG, "Content read");
		}
	}
	
	public void writeContent(DriveFile df, String content) throws Exception {
		connect();
		Log.v(TAG, "Saving file content");
		DriveContentsResult cr = df.open(gac, DriveFile.MODE_WRITE_ONLY, null).await();
		if (!cr.getStatus().isSuccess())
			throw new Exception(cr.getStatus().getStatusMessage());
		DriveContents dc = cr.getDriveContents();
		OutputStream stream = dc.getOutputStream();
		stream.write(content.getBytes());
		Status status = dc.commit(gac, null).await();
		if (!status.getStatus().isSuccess())
			throw new Exception(status.getStatus().getStatusMessage());
		Log.v(TAG, "Content saved");
	}
	
	private static String getPK(String realName) {
		return "pk_gdrid_" + realName.replace(".", "_");
	}
	
	private DriveFolder findFolder() throws Exception {
		Log.v(TAG, "Finding Uoccin folder");
		MetadataBufferResult br = Drive.DriveApi.query(gac,
			new Query.Builder().addFilter(Filters.eq(SearchableField.TRASHED, false)).
			addFilter(Filters.eq(SearchableField.TITLE, Commons.GD.FOLDER)).build()).await();
		if (!br.getStatus().isSuccess())
			throw new Exception(br.getStatus().getStatusMessage());
		MetadataBuffer mb = br.getMetadataBuffer();
		try {
			Metadata md;
			for (int i = 0; i < mb.getCount(); i++) {
				md = mb.get(i);
				if (md.isDataValid() && !md.isTrashed() && !md.isExplicitlyTrashed()) {
					DriveId id = md.getDriveId();
					if (!TextUtils.isEmpty(id.getResourceId())) {
						Log.v(TAG, "Saving folder resourceId " + id.getResourceId());
						SharedPreferences.Editor editor = session.getPrefs().edit();
						editor.putString(getPK(Commons.GD.FOLDER), id.getResourceId());
						editor.commit();
					}
					return Drive.DriveApi.getFolder(gac, id);
				}
			}
		} finally {
			mb.release();
		}
		Log.v(TAG, "Uoccin folder is missing or invalid");
		return null;
	}
	
	private DriveFolder createFolder() throws Exception {
		Log.v(TAG, "Creating Uoccin folder (resourceId will be available later)");
		MetadataChangeSet cs = new MetadataChangeSet.Builder().setTitle(Commons.GD.FOLDER).build();
		DriveFolderResult fr = Drive.DriveApi.getRootFolder(gac).createFolder(gac, cs).await();
		if (!fr.getStatus().isSuccess())
			throw new Exception(fr.getStatus().getStatusMessage());
		return fr.getDriveFolder();
	}
	
	private DriveFile findFile(String filename) throws Exception {
		Log.v(TAG, "Finding Uoccin file " + filename);
		MetadataBufferResult br = getFolder().queryChildren(gac, new Query.Builder().
			addFilter(Filters.eq(SearchableField.TRASHED, false)).
			addFilter(Filters.eq(SearchableField.TITLE, filename)).
			build()).await();
		if (!br.getStatus().isSuccess())
			throw new Exception(br.getStatus().getStatusMessage());
		MetadataBuffer mb = br.getMetadataBuffer();
		try {
			Metadata md;
			for (int i = 0; i < mb.getCount(); i++) {
				md = mb.get(i);
				if (md.isDataValid() && !md.isTrashed()) {
					DriveId id = md.getDriveId();
					if (!TextUtils.isEmpty(id.getResourceId()))
						saveRID(filename, id.getResourceId());
					return Drive.DriveApi.getFile(gac, id);
				}
			}
		} finally {
			mb.release();
		}
		Log.v(TAG, "Uoccin file is missing or invalid");
		return null;
	}
	
	private DriveFile createFile(String filename) throws Exception {
		Log.v(TAG, "Creating Uoccin file " + filename + " (resourceId will be available later)");
		DriveContentsResult cr = Drive.DriveApi.newDriveContents(gac).await();
		if (!cr.getStatus().isSuccess())
			throw new Exception(cr.getStatus().getStatusMessage());
		MetadataChangeSet cs = new MetadataChangeSet.Builder().setTitle(filename).setMimeType("application/json").
			setPinned(session.getPrefs().getBoolean(PK.PINNGDFS, false)).build();
		DriveFileResult fr = getFolder().createFile(gac, cs, cr.getDriveContents()).await();
		if (!fr.getStatus().isSuccess())
			throw new Exception(fr.getStatus().getStatusMessage());
		return fr.getDriveFile();
	}
	*/
	// @formatter:on
}