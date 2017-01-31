package net.ggelardi.uoccin.serv;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.services.drive.model.File;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

import net.ggelardi.uoccin.MainActivity;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.GSA;
import net.ggelardi.uoccin.api.OMDB;
import net.ggelardi.uoccin.api.TVDB;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Series;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class Service extends IntentService {
    private static final String TAG = "Service";

    private static final int NOTIF_BAK = 1980;
    private static final int NOTIF_MOV = 1990;
    private static final int NOTIF_SER = 1991;
    private static final int NOTIF_ERR = 1999;
    private static final Uri NOTIF_SND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    private Session session;
    private GSA drive;
    private Genson genson;

    private List<String> mov_list = new ArrayList<>();
    private List<String> mov_coll = new ArrayList<>();
    private List<String> ser_list = new ArrayList<>();
    private List<String> ser_coll = new ArrayList<>();

    public Service() {
        super("Service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.v(TAG, "onStartCommand");

        return android.app.Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(TAG, "onDestroy");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, intent.toString());
        session = Session.getInstance(this);
        Bundle extra;
        try {
            switch (intent.getAction()) {
                case Intent.ACTION_BOOT_COMPLETED:
                    session.checkAlarms(false);
                    break;
                case Commons.SR.REFRESH_MOVIE:
                    extra = intent.getExtras();
                    updateMovieInfo(extra.getString("imdb_id"), extra.getBoolean("forced", false));
                    break;
                case Commons.SR.REFRESH_SERIES:
                    extra = intent.getExtras();
                    updateSeriesInfo(extra.getInt("tvdb_id"), extra.getBoolean("forced", false));
                    break;
                case Commons.SR.REFRESH_EPISODE:
                    extra = intent.getExtras();
                    updateEpisodeInfo(extra.getInt("tvdb_id"), extra.getBoolean("forced", false));
                    break;
                case Commons.SR.GDRIVE_SYNCNOW:
                    driveSync();
                    break;
                case Commons.SR.GDRIVE_BACKUP:
                    driveBackup();
                    break;
                case Commons.SR.GDRIVE_RESTORE:
                    driveRestore();
                    break;
                case Commons.SR.CLEAN_DB_CACHE:
                    dbCleanUp();
                    break;
            }
        } catch (Exception err) {
            Log.e(TAG, "onHandleIntent", err);
            if (GoogleAuthIOException.class.isInstance(err))
                notifyError(getString(R.string.notif_err_gdrv), err.getLocalizedMessage(), Commons.SN.CONNECT_FAIL);
            else
                notifyError(getString(R.string.notif_err_serv), err.getLocalizedMessage(), null);
        }
    }

    private void sendToast(final String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void notifyError(String title, String text, String action) {
        NotificationCompat.Builder ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
        ncb.setLargeIcon(BitmapFactory.decodeResource(session.getRes(), R.drawable.ic_uoccin));
        ncb.setSmallIcon(R.drawable.ic_notif_error);
        ncb.setContentTitle(title);
        ncb.setContentText(text);
        if (action != null)
            ncb.setContentIntent(PendingIntent.getActivity(session.getContext(), 1998,
                    new Intent(session.getContext(), MainActivity.class).setAction(action),
                    PendingIntent.FLAG_UPDATE_CURRENT));
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ERR, ncb.build());
    }

    private void updateMovieInfo(String imdb_id, boolean forceRefresh) throws Exception {
        if (!session.isConnected(!forceRefresh))
            return;
        Movie movie = new Movie(this, imdb_id).load();
        if (forceRefresh || movie.isNew() || movie.isOld())
            movie.update(new OMDB(this).getMovie(imdb_id)).save(true);
    }

    private Series updateSeriesInfo(Series series, boolean forceRefresh) throws Exception {
        if (!session.isConnected(!forceRefresh))
            return series;
        int tvdb_id = series.tvdb_id;
        if (forceRefresh || series.isNew() || series.isOld()) {
            TVDB tvdb = new TVDB(this);
            TVDB.TVDBSeries tser = tvdb.getSeries(tvdb_id);
            TVDB.TVDBActors tact = tvdb.getActors(tvdb_id);
            TVDB.TVDBImages tart = tvdb.getImages(tvdb_id, "fanart", null);
            TVDB.TVDBImages tpst = tvdb.getImages(tvdb_id, "poster", null);
            //
            SQLiteDatabase db = session.getDB();
            boolean trans = !db.inTransaction();
            if (trans)
                db.beginTransaction();
            try {
                series.update(tser.data, tart != null ? tart.data : null,
                        tpst != null ? tpst.data : null, tact != null ? tact.data : null).save(true);
                // episodes
                if (TextUtils.isEmpty(tser.data.firstAired))
                    db.delete("episode", "series = ?", new String[]{Integer.toString(tvdb_id)});
                else {
                    ArrayList<TVDB.EpisodeData> teps = new ArrayList<>();
                    TVDB.TVDBEpisodes temp;
                    Integer page = 1;
                    while (true) {
                        try {
                            temp = tvdb.getEpisodes(tvdb_id, null, null, Integer.toString(page));
                        } catch (Exception fail) {
                            break;
                        }
                        if (temp == null || temp.data == null)
                            break;
                        teps.addAll(Arrays.asList(temp.data));
                        if (temp.links.next == null)
                            break;
                        page++;
                    }
                    List<Episode> episodes = new ArrayList<>();
                    for (TVDB.EpisodeData tep : teps) {
                        if (tep.special() && !session.specials())
                            continue;
                        episodes.add(new Episode(this, tep.id).load().update(tep));
                    }
                    if (episodes.isEmpty())
                        db.delete("episode", "series = ?", new String[]{Integer.toString(tvdb_id)});
                    else {
                        Collections.sort(episodes, new Comparator<Episode>() {
                            @Override
                            public int compare(Episode e1, Episode e2) {
                                return e1.eid().compareTo(e2.eid());
                            }
                        });
                        List<String> ids = new ArrayList<>();
                        for (Episode ep : episodes) {
                            ep.save(false);
                            ids.add(Integer.toString(ep.tvdb_id));
                        }
                        db.execSQL("delete from episode where series = " +
                                Integer.toString(tvdb_id) + " and not tvdb_id in (" +
                                TextUtils.join(", ", ids) + ")");
                    }
                }
                if (trans) {
                    db.setTransactionSuccessful();
                    Series.broadcast(session.getContext(), series.tvdb_id, true);
                }
            } catch (Exception err) {
                Log.e(TAG, "updateSeriesInfo", err);
                throw err;
            } finally {
                if (trans)
                    db.endTransaction();
            }
        }
        return series;
    }

    private Series updateSeriesInfo(int tvdb_id, boolean forceRefresh) throws Exception {
        Series series = new Series(this, tvdb_id).load(false);
        if (!session.isConnected(!forceRefresh))
            return series;
        return updateSeriesInfo(series, forceRefresh);
    }

    private void updateEpisodeInfo(int tvdb_id, boolean forceRefresh) throws Exception {
        if (!session.isConnected(!forceRefresh))
            return;
        Episode episode = new Episode(this, tvdb_id).load();
        if (forceRefresh || episode.isNew() || episode.isOld()) {
            TVDB.TVDBEpisode data = new TVDB(this).getEpisode(tvdb_id);
            if (data != null) {
                episode.update(data.data).save(true);
                if (!session.getDB().inTransaction())
                    Episode.broadcast(session.getContext(), episode.series, episode.tvdb_id, episode.season,
                            episode.episode, true);
            } else if (episode.series > 0) {
                // the episode does not exists
                session.getDB().delete("episode", "tvdb_id = ?", new String[] {Integer.toString(tvdb_id)});
                if (!session.getDB().inTransaction())
                    Series.broadcast(session.getContext(), episode.series, true);
            }
        }
    }

    private boolean applyMovieDiff(String imdb_id, Boolean watchlist, Boolean collected, Boolean watched, Integer rating,
                                String tags, String subs) throws Exception {
        Movie movie = new Movie(this, imdb_id).load();
        if (movie.isNew()) {
            if ((watchlist == null || !watchlist) && (collected == null || !collected) &&
                    (watched == null || !watched) && (rating == null || rating <= 0) &&
                    TextUtils.isEmpty(subs) && TextUtils.isEmpty(tags))
                return false; // can ignore it
            updateMovieInfo(imdb_id, true);
            movie.load();
        }
        boolean updated = false;
        if (watchlist != null && watchlist != movie.watchlist) {
            movie.watchlist = watchlist;
            updated = true;
            if (watchlist)
                mov_list.add(movie.name);
        }
        if (collected != null && collected != movie.collected) {
            movie.collected = collected;
            updated = true;
            if (collected)
                mov_coll.add(movie.name);
        }
        if (watched != null && watched != movie.watched) {
            movie.watched = watched;
            updated = true;
        }
        if (rating != null) {
            movie.rating = rating;
            updated = true;
        }
        if (tags != null) {
            movie.tags = new ArrayList<>(Arrays.asList(tags.split(",")));
            updated = true;
        }
        if (subs != null) {
            movie.subtitles = new ArrayList<>(Arrays.asList(subs.split(",")));
            updated = true;
        }
        if (updated)
            movie.save(false);
        return true;
    }

    private boolean applySeriesDiff(int tvdb_id, Boolean watchlist, Integer rating, String tags) throws Exception {
        Series series = new Series(this, tvdb_id).load(false);
        if (series.isNew()) {
            if ((watchlist == null || !watchlist) && (rating == null || rating <= 0) && TextUtils.isEmpty(tags))
                return false; // can ignore it
            updateSeriesInfo(series, true);
        }
        boolean updated = false;
        if (watchlist != null && watchlist != series.watchlist) {
            series.watchlist = watchlist;
            updated = true;
            if (watchlist)
                ser_list.add(series.name);
        }
        if (rating != null) {
            series.rating = rating;
            updated = true;
        }
        if (tags != null) {
            series.tags = new ArrayList<>(Arrays.asList(tags.split(",")));
            updated = true;
        }
        if (updated)
            series.save(false);
        return true;
    }

    private void applyEpisodeDiff(String diffcode, Boolean collected, Boolean watched, String subs) throws Exception {
        Episode episode = new Episode(this, new Episode.EID(diffcode)).load();
        Series series = new Series(this, episode.series).load(false);
        if (episode.isNew()) {
            if ((collected == null || !collected) && (watched == null || !watched) && TextUtils.isEmpty(subs))
                return; // can ignore it
            updateSeriesInfo(series, true);
            episode.load();
            if (episode.isNew())
                return; // the episode does not exists (anymore?), skipping...
        }
        boolean updated = false;
        if (collected != null && collected != episode.collected) {
            episode.collected = collected;
            updated = true;
            if (collected)
                ser_coll.add(series.name + " " + episode.eid().readable());
            else
                episode.subtitles.clear();
        }
        if (watched != null && watched != episode.watched) {
            episode.watched = watched;
            updated = true;
        }
        if (subs != null && episode.collected) {
            episode.subtitles = new ArrayList<>(Arrays.asList(subs.split(",")));
            updated = true;
        }
        if (updated)
            episode.save(false);
    }

    private void applyDiffLine(long cid, String type, String target, String field, String value) throws Exception {
        Boolean wlst = null;
        Boolean coll = null;
        Boolean seen = null;
        Integer rats = null;
        String tags = null;
        String subs = null;
        if (type.equals(Session.QUEUE_MOVIE)) {
            if (field.equals("watchlist"))
                wlst = Boolean.parseBoolean(value);
            else if (field.equals("collected"))
                coll = Boolean.parseBoolean(value);
            else if (field.equals("watched"))
                seen = Boolean.parseBoolean(value);
            else if (field.equals("rating"))
                rats = Integer.parseInt(value);
            else if (field.equals("subtitles"))
                subs = value;
            else if (field.equals("tags"))
                tags = value;
            else
                throw new IOException("Invalid field " + field);
            applyMovieDiff(target, wlst, coll, seen, rats, tags, subs);
        } else if (type.equals(Session.QUEUE_SERIES)) {
            if (!target.contains(".")) {
                if (field.equals("watchlist"))
                    wlst = Boolean.parseBoolean(value);
                else if (field.equals("rating"))
                    rats = Integer.parseInt(value);
                else if (field.equals("tags"))
                    tags = value;
                else
                    throw new IOException("Invalid field " + field);
                applySeriesDiff(Integer.parseInt(target), wlst, rats, tags);//, false);
            } else {
                if (field.equals("collected"))
                    coll = Boolean.parseBoolean(value);
                else if (field.equals("watched"))
                    seen = Boolean.parseBoolean(value);
                else if (field.equals("subtitles"))
                    subs = value;
                else
                    throw new IOException("Invalid field '" + field + "'");
                applyEpisodeDiff(target, coll, seen, subs);
            }
        } else
            throw new IOException("Invalid diff type '" + type + "'");
    }

    private NotificationCompat.Builder buildNotif() {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
        nb.setCategory(NotificationCompat.CATEGORY_SOCIAL);
        nb.setSmallIcon(R.drawable.ic_notif_main);
        nb.setAutoCancel(true);
        if (session.notificationSound())
            nb.setSound(NOTIF_SND);
        return nb;
    }

    private void checkDrive() throws Exception {
        if (drive == null)
            drive = new GSA(this);
    }

    private void checkGenson() {
        if (genson == null)
            genson = new GensonBuilder().setSkipNull(true)
                    .useIndentation(true) // DEBUG ONLY!
                    .create();
    }

    private int loadDiffFile(String fileId) {
        String[] lines = null;
        try {
            lines = drive.readFile(fileId).split("\n");
        } catch (Exception err) {
            Log.e(TAG, "Error loading file " + fileId, err);
            return 0;
        }
        ContentValues cv;
        String[] fields;
        int res = 0;
        for (String line : lines)
            try {
                fields = line.split("\\x7C");
                cv = new ContentValues();
                cv.put("timestamp", Long.parseLong(fields[0]));
                cv.put("target", fields[1]);
                cv.put("title", fields[2]);
                cv.put("field", fields[3]);
                cv.put("value", fields[4]);
                Log.d(TAG, Commons.logContentValue("Adding command", cv));
                session.getDB().insertOrThrow("queue_in", null, cv);
                res++;
            } catch (Exception err) {
                Log.e(TAG, "Error loading line: " + line, err);
            }
        return res;
    }

    private void driveSync() throws Exception {
        if (!(session.driveSyncEnabled() && session.isConnected(session.driveSyncWifiOnly())))
            return;
        if (session.driveSyncRunning(true))
        try {
            mov_list = new ArrayList<>();
            mov_coll = new ArrayList<>();
            ser_list = new ArrayList<>();
            ser_coll = new ArrayList<>();
            SQLiteDatabase db = session.getDB();
            try {
                checkDrive();
                // write diff
                List<String> others = drive.getOtherFoldersIds();
                if (others.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    try (Cursor qo = db.query("queue_out", null, null, null, null, null, "timestamp")) {
                        while (qo.moveToNext()) {
                            sb.append(qo.getLong(0)).append('|');
                            sb.append(qo.getString(1)).append('|');
                            sb.append(qo.getString(2)).append('|');
                            sb.append(qo.getString(3)).append('|');
                            sb.append(qo.getString(4)).append("\n");
                        }
                    }
                    if (sb.length() > 0) {
                        String fn = Long.toString(System.currentTimeMillis()) + "." + session.driveDeviceID() + ".diff";
                        for (String fid : others)
                            drive.writeFile(null, fid, fn, Commons.MT.TEXT, sb.toString());
                    }
                }
                db.delete("queue_out", null, null);
                // delete old failed queue_in changes
                db.execSQL("delete from queue_in where datetime(timestamp/1000, 'unixepoch') <= datetime('now', '-2 days')");
                // load other devices' diffs
                for (String fid : drive.getNewDiffs()) {
                    loadDiffFile(fid); // ignore errors
                    try {
                        drive.deleteFile(fid);
                    } catch (Exception err) {
                        Log.e(TAG, "drive.deleteFile() failed", err); // ignore errors
                    }
                }
                try (Cursor qi = db.query("queue_in", new String[]{"rowid", "*"}, null, null, null, null, "timestamp")) {
                    int r = qi.getColumnIndex("rowid");
                    int t = qi.getColumnIndex("target");
                    int i = qi.getColumnIndex("title");
                    int f = qi.getColumnIndex("field");
                    int v = qi.getColumnIndex("value");
                    long rowId;
                    while (qi.moveToNext()) {
                        Log.d(TAG, Commons.logCursor("apply change", qi));
                        rowId = qi.getLong(r);
                        db.beginTransaction();
                        try {
                            applyDiffLine(rowId, qi.getString(t), qi.getString(i), qi.getString(f), qi.getString(v));
                            db.delete("queue_in", "rowid = ?", new String[]{Long.toString(rowId)});
                            db.setTransactionSuccessful();
                        } catch (Exception err) {
                            Log.e(TAG, "driveSync", err); // ignore, maybe next time will succeed
                        } finally {
                            db.endTransaction();
                        }
                    }
                }
                // notifications
                if (!(mov_list.isEmpty() && mov_coll.isEmpty())) {
                    NotificationCompat.Builder nb = buildNotif();
                    nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_movies));
                    nb.setContentTitle(getString(R.string.notif_mov_title));
                    nb.setContentText(getString(R.string.notif_com_text));
                    nb.setNumber(mov_list.size() + mov_coll.size());
                    NotificationCompat.InboxStyle is = new NotificationCompat.InboxStyle();
                    is.setBigContentTitle(getString(R.string.notif_mov_summ));
                    if (!mov_list.isEmpty()) {
                        is.addLine(getString(R.string.notif_hdr_list));
                        for (String title : mov_list)
                            is.addLine("- " + title);
                        Intent ri = new Intent(this, MainActivity.class).setAction(Commons.SN.MOV_WLST);
                        PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
                        nb.addAction(R.drawable.ic_action_watchlist, getString(R.string.notif_act_list), pi);
                    }
                    if (!mov_coll.isEmpty()) {
                        is.addLine(getString(R.string.notif_hdr_coll));
                        for (String title : mov_coll)
                            is.addLine("- " + title);
                        Intent ri = new Intent(this, MainActivity.class).setAction(Commons.SN.MOV_COLL);
                        PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
                        nb.addAction(R.drawable.ic_action_storage, getString(R.string.notif_act_coll), pi);
                    }
                    nb.setStyle(is);
                    Intent ri = new Intent(this, MainActivity.class);
                    PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
                    nb.setContentIntent(pi);
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify("uoccin", NOTIF_MOV, nb.build());
                }
                if (!(ser_list.isEmpty() && ser_coll.isEmpty())) {
                    NotificationCompat.Builder nb = buildNotif();
                    nb.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_series));
                    nb.setContentTitle(getString(R.string.notif_ser_title));
                    nb.setContentText(getString(R.string.notif_com_text));
                    nb.setNumber(ser_list.size() + ser_coll.size());
                    NotificationCompat.InboxStyle is = new NotificationCompat.InboxStyle();
                    is.setBigContentTitle(getString(R.string.notif_ser_summ));
                    if (!ser_list.isEmpty()) {
                        is.addLine(getString(R.string.notif_hdr_list));
                        for (String title : ser_list)
                            is.addLine("- " + title);
                        Intent ri = new Intent(this, MainActivity.class).setAction(Commons.SN.SER_WLST);
                        PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
                        nb.addAction(R.drawable.ic_action_watchlist, getString(R.string.notif_act_list), pi);
                    }
                    if (!ser_coll.isEmpty()) {
                        is.addLine(getString(R.string.notif_hdr_coll));
                        for (String title : ser_coll)
                            is.addLine("- " + title);
                        Intent ri = new Intent(this, MainActivity.class).setAction(Commons.SN.SER_COLL);
                        PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
                        nb.addAction(R.drawable.ic_action_storage, getString(R.string.notif_act_coll), pi);
                    }
                    nb.setStyle(is);
                    Intent ri = new Intent(this, MainActivity.class);
                    PendingIntent pi = PendingIntent.getActivity(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
                    nb.setContentIntent(pi);
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify("uoccin", NOTIF_SER, nb.build());
                }
                //TODO: http://stackoverflow.com/questions/12551908/how-to-update-notification-number
            } catch (Exception err) {
                Log.e(TAG, "driveSync", err);
                throw err;
            }
        } finally {
            session.driveSyncRunning(false);
        }
    }

    private void driveBackup() {
        if (!(session.driveAccountSet() && session.isConnected(false)))
            return;
        Long totm;
        Long tots;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notif_main)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_backup))
                .setContentTitle(getString(R.string.notif_bak_title))
                .setContentText(getString(R.string.notif_bak_starting));
        SQLiteDatabase db = session.getDB();
        try {
            totm = DatabaseUtils.queryNumEntries(db, "movie");
            tots = DatabaseUtils.queryNumEntries(db, "series");
            int count = totm.intValue() + tots.intValue();
            nb.setContentText(String.format(getString(R.string.notif_bak_details), totm, tots));
            nb.setProgress(count, 0, false);
            nm.notify(NOTIF_BAK, nb.build());
            int prog = 0;
            checkDrive();
            UFile file = new UFile();
            file.movies = new HashMap<>();
            file.series = new HashMap<>();
            // movies
            try (Cursor cur = db.query("movie", new String[]{"imdb_id", "name", "watchlist", "collected", "watched",
                    "rating", "subtitles"}, "watchlist = 1 or collected = 1 or watched = 1", null, null, null,
                    "name collate nocase", null)
            ) {
                UFile.UMovie mov;
                String mid;
                while (cur.moveToNext()) {
                    mid = cur.getString(0);
                    mov = new UFile.UMovie();
                    mov.read(db, mid, cur);
                    file.movies.put(mid, mov);
                    prog++;
                    nm.notify(NOTIF_BAK, nb.setProgress(count, prog, false).build());
                }
            }
            // series
            try (Cursor cur = db.query("series", new String[]{"tvdb_id", "name", "watchlist", "rating"}, null, null,
                    null, null, "name collate nocase", null)
            ) {
                UFile.USeries ser;
                int sid;
                while (cur.moveToNext()) {
                    sid = cur.getInt(0);
                    ser = new UFile.USeries();
                    ser.read(db, sid, cur);
                    if (ser.watchlist || !ser.collected.isEmpty() || !ser.watched.isEmpty())
                        file.series.put(sid, ser);
                    prog++;
                    nm.notify(NOTIF_BAK, nb.setProgress(count, prog, false).build());
                }
            }
            checkGenson();
            String content = genson.serialize(file);
            File bak = drive.getFile(GSA.BACKUP, drive.getRootFolderId(true), null);
            drive.writeFile(bak != null ? bak.getId() : null, drive.getRootFolderId(true),
                    GSA.BACKUP, Commons.MT.JSON, content);
            nb.setContentTitle(getString(R.string.notif_bak_title_done)).setProgress(0, 0, false)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_complete))
                    .setContentText(String.format(getString(R.string.notif_bak_success), totm, tots));
            nm.notify(NOTIF_BAK, nb.build());
        } catch (Exception err) {
            Log.e(TAG, "driveBackup", err);
            nb.setContentTitle(getString(R.string.notif_bak_title_fail)).setProgress(0, 0, false)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_error))
                    .setContentText(err.getLocalizedMessage());
            nm.notify(NOTIF_BAK, nb.build());
        }
    }

    private void driveRestore() {
        if (!(session.driveAccountSet() && session.isConnected(false)))
            return;
        int movCount;
        int serCount;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notif_main)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_restore))
                .setContentTitle(getString(R.string.notif_res_title))
                .setContentText(getString(R.string.notif_res_loading));
        nm.notify(NOTIF_BAK, nb.build());
        try {
            checkDrive();
            File bak = drive.getFile(GSA.BACKUP, drive.getRootFolderId(true), null);
            if (bak == null)
                throw new Exception(getString(R.string.notif_res_notfound));
            String content = drive.readFile(bak);
            if (TextUtils.isEmpty(content))
                throw new Exception(getString(R.string.nm_restore_isempty));
            checkGenson();
            UFile file = genson.deserialize(content, UFile.class);
            movCount = file.movies.keySet().size();
            serCount = file.series.keySet().size();
            int movDone = 0;
            int serDone = 0;
            if ((movCount + serCount) <= 0)
                throw new Exception(getString(R.string.nm_restore_isempty));
            nb.setContentText(String.format(getString(R.string.nm_restore_details), movCount, serCount));
            nb.setProgress(movCount + serCount, 0, false);
            nm.notify(NOTIF_BAK, nb.build());
            SQLiteDatabase db = session.getDB();
            db.beginTransaction();
            try {
                int prog = 0;
                String contentText = getString(R.string.nm_restore_running);
                // set abort action
                SharedPreferences.Editor editor = session.getPrefs().edit();
                editor.remove(Commons.PK.BAKABORT);
                editor.commit();
                Intent ri = new Intent(Commons.SN.ABORT_RESTOR);
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, ri, PendingIntent.FLAG_UPDATE_CURRENT);
                nb.addAction(R.drawable.ic_action_cancel, getString(R.string.nm_restore_abort), pi);
                // movies
                db.delete("movie", null, null);
                UFile.UMovie um;
                for (String imdb_id : file.movies.keySet()) {
                    if (session.getPrefs().getBoolean(Commons.PK.BAKABORT, false))
                        throw new Exception(getString(R.string.nm_restore_aborted));
                    um = file.movies.get(imdb_id);
                    if (applyMovieDiff(imdb_id, um.watchlist, um.collected, um.watched, um.rating,
                            um.tags != null ? TextUtils.join(",", um.tags) : null,
                            um.subtitles != null ? TextUtils.join(",", um.subtitles) : null))
                        movDone++;
                    prog++;
                    nb.setContentText(String.format(contentText, movDone, movCount, serDone, serCount))
                            .setProgress(movCount + serCount, prog, false);
                    nm.notify(NOTIF_BAK, nb.build());
                }
                // series
                db.delete("series", null, null);
                Series chkSeries;
                List<Integer> chkSeasons;
                UFile.USeries us;
                for (int tvdb_id : file.series.keySet()) {
                    if (session.getPrefs().getBoolean(Commons.PK.BAKABORT, false))
                        throw new Exception(getString(R.string.nm_restore_aborted));
                    us = file.series.get(tvdb_id);
                    if (applySeriesDiff(tvdb_id, us.watchlist, us.rating,
                            us.tags != null ? TextUtils.join(",", us.tags) : null)) {
                        serDone++;
                        chkSeries = new Series(session.getContext(), tvdb_id).load(true);
                        chkSeasons = chkSeries.episodes.getSeasons();
                        // collected episodes
                        Map<Integer, String[]> seasonMap;
                        for (int season : us.collected.keySet()) {
                            if (!chkSeasons.contains(season)) {
                                Log.i(TAG, String.format(Locale.getDefault(), "driveRestore(): " +
                                        "series \"%s\": looks like the season %d does not exists " +
                                        "(anymore?), skipping...", chkSeries.name, season));
                                continue;
                            }
                            if (session.getPrefs().getBoolean(Commons.PK.BAKABORT, false))
                                throw new Exception(getString(R.string.nm_restore_aborted));
                            seasonMap = us.collected.get(season);
                            for (int episode : seasonMap.keySet()) {
                                Object[] tmp = seasonMap.get(episode);
                                applyEpisodeDiff(new Episode.EID(tvdb_id, season, episode).toString(), true, null,
                                        tmp != null && tmp.length > 0 ? TextUtils.join(",", seasonMap.get(episode)) : null);
                            }
                        }
                        // watched episodes
                        for (int season : us.watched.keySet()) {
                            if (!chkSeasons.contains(season)) {
                                Log.i(TAG, String.format(Locale.getDefault(), "driveRestore(): " +
                                        "series \"%s\": looks like the season %d does not exists " +
                                        "(anymore?), skipping...", chkSeries.name, season));
                                continue;
                            }
                            for (Integer episode : us.watched.get(season)) {
                                if (session.getPrefs().getBoolean(Commons.PK.BAKABORT, false))
                                    throw new Exception(getString(R.string.nm_restore_aborted));
                                applyEpisodeDiff(new Episode.EID(tvdb_id, season, episode).toString(), null, true, null);
                            }
                        }
                    }
                    prog++;
                    nb.setContentText(String.format(contentText, movDone, movCount, serDone, serCount))
                            .setProgress(movCount + serCount, prog, false);
                    nm.notify(NOTIF_BAK, nb.build());
                }
                db.delete("queue_out", null, null);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            // notifications
            nb.setContentTitle(getString(R.string.notif_res_title_done)).setProgress(0, 0, false)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_complete))
                    .setContentText(String.format(getString(R.string.nm_restore_success), movDone, serDone))
                    .setSound(NOTIF_SND).setAutoCancel(true).mActions.clear();
            nm.notify(NOTIF_BAK, nb.build());
            Movie.broadcast(session.getContext(), null, true);
            Series.broadcast(session.getContext(), null, true);
        } catch (Exception err) {
            Log.e(TAG, "driveRestore", err);
            nb.setContentTitle(getString(R.string.notif_res_title_fail)).setProgress(0, 0, false)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_li_error))
                    .setContentText(err.getLocalizedMessage())
                    .setSound(NOTIF_SND).setAutoCancel(true).mActions.clear();
            nm.notify(NOTIF_BAK, nb.build());
        }
    }

    private void dbCleanUp() throws Exception {
        Log.d(TAG, "cleaning database...");
        SQLiteDatabase db = session.getDB();
        db.beginTransaction();
        try {
            // specials
            if (!session.specials())
                db.execSQL("delete from episode where season <= 0 or episode <= 0");
            // old stuff
            String age = "createtime < " + Long.toString(System.currentTimeMillis() - Commons.days(7));
            db.execSQL("delete from movie where " + age + " and watchlist = 0 and collected = 0 and watched = 0");
            db.execSQL("delete from series where " + age + " and watchlist = 0 and not tvdb_id in " +
                    "(select distinct series from episode where collected = 1 or watched = 1)");
            // cleaning
            db.execSQL("update movie set subtitles = null where subtitles = ''");
            db.execSQL("update episode set subtitles = null where subtitles = '' or collected = 0");
            // done
            db.setTransactionSuccessful();
        } catch (Exception err) {
            Log.e(TAG, "dbCleanUp", err);
        } finally {
            db.endTransaction();
            if (!db.isDatabaseIntegrityOk())
                throw new Exception("Database integrity error check FAILED!");
            Movie.broadcast(session.getContext(), null, true);
            Series.broadcast(session.getContext(), null, true);
        }
    }

    public static class UFile {
        public Map<String, UMovie> movies;
        public Map<Integer, USeries> series;

        static class UMovie {
            public String name;
            public boolean watchlist;
            public boolean collected;
            public boolean watched;
            public int rating;
            public String[] tags;
            public String[] subtitles;

            void read(SQLiteDatabase db, String imdb_id, Cursor cur) {
                name = cur.getString(1);
                watchlist = cur.getInt(2) == 1;
                collected = cur.getInt(3) == 1;
                watched = cur.getInt(4) == 1;
                if (!cur.isNull(5))
                    rating = cur.getInt(5);
                if (!cur.isNull(6))
                    subtitles = cur.getString(6).split(",\\s*");
                // tags
                List<String> list = new ArrayList<String>();
                try (Cursor cr = db.query(true, "movtag", new String[]{"tag"}, "movie = ?", new String[]{imdb_id},
                        null, null, "tag", null)
                ) {
                    while (cr.moveToNext())
                        list.add(cr.getString(0));
                }
                tags = list.isEmpty() ? null : list.toArray(new String[list.size()]);
            }
        }

        static class USeries {
            public String name;
            public boolean watchlist;
            public int rating;
            public String[] tags;
            public Map<Integer, Map<Integer, String[]>> collected;
            public Map<Integer, List<Integer>> watched;

            void read(SQLiteDatabase db, int tvdb_id, Cursor cur) {
                name = cur.getString(1);
                watchlist = cur.getInt(2) == 1;
                if (!cur.isNull(3))
                    rating = cur.getInt(3);
                // episodes
                collected = new HashMap<>();
                watched = new HashMap<>();
                try (Cursor eps = db.query("episode", new String[]{"season", "episode", "collected", "watched", "subtitles"},
                        "series = ? and (collected = 1 or watched = 1)", new String[]{Integer.toString(tvdb_id)}, null, null,
                        "season, episode")
                ) {
                    int season;
                    int episode;
                    boolean coll;
                    boolean seen;
                    List<Integer> intlst;
                    while (eps.moveToNext()) {
                        season = eps.getInt(0);
                        episode = eps.getInt(1);
                        coll = eps.getInt(2) == 1;
                        seen = eps.getInt(3) == 1;
                        if (coll) {
                            if (!collected.containsKey(season))
                                collected.put(season, new HashMap<Integer, String[]>());
                            collected.get(season).put(episode,
                                    (eps.isNull(4) ? new String[]{} : eps.getString(4).split(",\\s*")));
                        }
                        if (seen) {
                            if (!watched.containsKey(season))
                                watched.put(season, new ArrayList<Integer>());
                            intlst = watched.get(season);
                            if (!intlst.contains(episode))
                                intlst.add(episode);
                        }
                    }
                }
                // tags
                if (watchlist || !collected.isEmpty() || !watched.isEmpty()) {
                    List<String> list = new ArrayList<String>();
                    try (Cursor cr = db.query(true, "sertag", new String[]{"tag"}, "series = ?",
                            new String[]{Integer.toString(tvdb_id)}, null, null, "tag", null)
                    ) {
                        while (cr.moveToNext())
                            list.add(cr.getString(0));
                    }
                    tags = list.isEmpty() ? null : list.toArray(new String[list.size()]);
                }
            }
        }
    }
}
