package net.ggelardi.uoccin.serv;

public class DriveService /*extends DriveEventService*/ {
	// @formatter:off
	/*
	private static final String TAG = "DriveService";
	
	@Override
	public void onChange(ChangeEvent event) {
		Log.d(TAG, event.toString());
		DriveId id = event.getDriveId();
		if (id.getResourceType() != DriveId.RESOURCE_TYPE_FILE || TextUtils.isEmpty(id.getResourceId()))
			return;
		try {
			Session session = Session.getInstance(this);
			SyncGAC gdw = session.getGAC();
			DriveFile df = gdw.getFile(id);
			if (df != null) {
				String filename = gdw.getMetadata(df).getTitle();
				Log.d(TAG, "Something changed in " + filename);
				if (event.hasBeenDeleted())
					gdw.dropRID(filename);
				else {
					gdw.saveRID(filename, id.getResourceId());
					if (event.hasContentChanged()) {
						Intent si = new Intent(this, Service.class);
						si.setAction(Service.GDRIVE_RESTORE);
						si.putExtra("what", filename);
						session.getContext().startService(si);
					}
				}
			}
		} catch (Exception err) {
			Intent si = new Intent(Commons.SN.GENERAL_FAIL);
			si.putExtra("what", err.getLocalizedMessage());
			sendBroadcast(si);
		}
	}
	*/
	// @formatter:on
}