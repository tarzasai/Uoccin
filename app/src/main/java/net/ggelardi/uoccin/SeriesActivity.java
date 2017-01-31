package net.ggelardi.uoccin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;

import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.serv.Session;

import java.util.List;
import java.util.Locale;

public class SeriesActivity extends BaseSeriesActivity implements AppBarLayout.OnOffsetChangedListener {

    public static void start(Context context, Series series) {
        cached_series = series;
        context.startActivity(new Intent(context, SeriesActivity.class)
                .putExtra("seriesId", series.tvdb_id));
    }

    private int pstWidth;

    private CollapsingToolbarLayout ctl_this;
    private AppBarLayout abl_this;
    private ImageView img_post;
    private TextView txt_term;
    private TextView txt_airs;
    private TextView txt_name;
    private TextView txt_plot;
    private TextView txt_cast;
    private TextView txt_gens;
    private TextView txt_sinc;
    private TextView txt_ratd;
    private TextView txt_tvsc;
    private TextView txt_aird;
    private TextView txt_seen;
    private TextView txt_coll;
    private TextView txt_miss;
    private TextView txt_tags;
    private RatingBar rat_myrt;
    private RecyclerView grd_seas;
    private BottomSheetDialog bsd_menu;
    private BottomSheetBehavior bsb_menu;
    private TextView btn_wlst;
    private TextView btn_seen;
    private TextView btn_coll;
    private TextView btn_imdb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series);
        setUoccinActionBar(findViewById(R.id.toolbar));

        ctl_this = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        abl_this = (AppBarLayout) findViewById(R.id.appbar_layout);
        img_post = (ImageView) findViewById(R.id.img_fanart);
        txt_term = (TextView) findViewById(R.id.txt_term);
        txt_airs = (TextView) findViewById(R.id.txt_airs);
        txt_name = (TextView) findViewById(R.id.txt_name);
        txt_plot = (TextView) findViewById(R.id.txt_plot);
        txt_cast = (TextView) findViewById(R.id.txt_cast);
        txt_gens = (TextView) findViewById(R.id.txt_genres);
        txt_sinc = (TextView) findViewById(R.id.txt_aired);
        txt_ratd = (TextView) findViewById(R.id.txt_rated);
        txt_tvsc = (TextView) findViewById(R.id.txt_ratings);
        txt_aird = (TextView) findViewById(R.id.txt_count);
        txt_seen = (TextView) findViewById(R.id.txt_watched);
        txt_coll = (TextView) findViewById(R.id.txt_collected);
        txt_miss = (TextView) findViewById(R.id.txt_missing);
        txt_tags = (TextView) findViewById(R.id.txt_tags);
        grd_seas = (RecyclerView) findViewById(R.id.grd_seasons);
        rat_myrt = (RatingBar) findViewById(R.id.rat_rating);

        txt_tags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                series.editTags(SeriesActivity.this);
            }
        });

        rat_myrt.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser)
                    series.setRating(Math.round(rating));
            }
        });

        View bsv = getLayoutInflater().inflate(R.layout.dialog_series_menu, null);
        bsd_menu = new BottomSheetDialog(SeriesActivity.this);
        bsd_menu.setContentView(bsv);
        bsd_menu.setCancelable(true);
        bsd_menu.setCanceledOnTouchOutside(true);
        bsb_menu = BottomSheetBehavior.from((View) bsv.getParent());

        btn_wlst = (TextView) bsv.findViewById(R.id.btn_wlst);
        btn_seen = (TextView) bsv.findViewById(R.id.btn_seen);
        btn_coll = (TextView) bsv.findViewById(R.id.btn_coll);
        btn_imdb = (TextView) bsv.findViewById(R.id.btn_imdb);

        bsd_menu.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                bsb_menu.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        findViewById(R.id.fab_menu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsb_menu.setState(BottomSheetBehavior.STATE_EXPANDED);
                bsd_menu.show();
            }
        });

        btn_wlst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                series.setWatchlist(!series.watchlist);
                showMessage(series.watchlist ? R.string.seract_msg_wlst1 : R.string.seract_msg_wlst0);
                showData();
            }
        });

        btn_coll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                final boolean coll = series.collectedEpisodes() < series.airedEpisodes();
                new AlertDialog.Builder(SeriesActivity.this).setTitle(series.name())
                        .setMessage(coll ? R.string.seract_ask_coll1 : R.string.seract_ask_coll0)
                        .setIcon(R.drawable.ic_dlgico_massedit).setNegativeButton(R.string.dlg_btn_cancel, null)
                        .setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                series.setCollected(coll, null);
                                showMessage(coll ? R.string.seract_msg_coll1 : R.string.seract_msg_coll0);
                                showData();
                            }
                        }).show();
            }
        });

        btn_seen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                final boolean seen = series.watchedEpisodes() < series.airedEpisodes();
                new AlertDialog.Builder(SeriesActivity.this).setTitle(series.name())
                        .setMessage(seen ? R.string.seract_ask_seen1 : R.string.seract_ask_seen0)
                        .setIcon(R.drawable.ic_dlgico_massedit).setNegativeButton(R.string.dlg_btn_cancel, null).
                        setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                series.setWatched(seen, null);
                                showMessage(seen ? R.string.seract_msg_seen1 : R.string.seract_msg_seen0);
                                showData();
                            }
                        }).show();
            }
        });

        bsv.findViewById(R.id.btn_updt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                grd_seas.getAdapter().notifyDataSetChanged();
                showMessage(R.string.seract_msg_updating);
                updating = true;
                series.refresh(true);
            }
        });

        bsv.findViewById(R.id.btn_tvdb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsd_menu.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(series.tvdbUrl())));
            }
        });

        btn_imdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsd_menu.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(series.imdbUrl())));
            }
        });

        String avatar = session.driveAccountPhoto();
        if (!TextUtils.isEmpty(avatar))
            ((ImageView) findViewById(R.id.img_user)).setImageURI(Uri.parse(avatar));
    }

    @Override
    protected void onResume() {
        Point screenSize = new Point();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
        pstWidth = screenSize.x;

        ctl_this.setTitleEnabled(true);

        super.onResume();

        abl_this.addOnOffsetChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        abl_this.removeOnOffsetChangedListener(this);
    }

    @Override
    public void setTitle(CharSequence title) {
        ctl_this.setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_series, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_erase:
                new AlertDialog.Builder(SeriesActivity.this).setTitle(series.name())
                        .setMessage(R.string.seract_ask_delete)
                        .setIcon(R.drawable.ic_dlgico_delete)
                        .setNegativeButton(R.string.dlg_btn_cancel, null)
                        .setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SQLiteDatabase db = session.getDB();
                                if (db.inTransaction()) {
                                    showMessage(R.string.seract_msg_delbusy);
                                    return;
                                }
                                db.beginTransaction();
                                try {
                                    session.driveQueue(Session.QUEUE_SERIES, seriesId, "watchlist",
                                            Boolean.toString(false));
                                    db.delete("series", "tvdb_id = ?",
                                            new String[] { Integer.toString(seriesId) });
                                    db.setTransactionSuccessful();
                                    showMessage(R.string.seract_msg_deleted);
                                    finish();
                                } catch (Exception err) {
                                    Log.e(tag(), "delete", err);
                                    showMessage(err.getLocalizedMessage());
                                } finally {
                                    db.endTransaction();
                                }
                            }
                        }).show();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return false;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        setTitle(series != null && verticalOffset < -50 ? series.name() : "");
    }

    @Override
    protected void updateSeries(Bundle data) {
        super.updateSeries(data);

        int sid = data.getInt("series");
        if (sid == 0 || sid == seriesId)
            grd_seas.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void showData() {
        super.showData();

        setTitle(series.name());
        if (series.isEnded()) {
            txt_airs.setVisibility(View.GONE);
            txt_term.setVisibility(View.VISIBLE);
        } else {
            txt_term.setVisibility(View.GONE);
            txt_airs.setVisibility(View.VISIBLE);
            String info = series.network() + ", " + series.airsTime();
            if (series.runtime > 0)
                info += String.format(Locale.getDefault(), " (%dm)", series.runtime);
            txt_airs.setText(info);
        }

        txt_name.setText(series.name());
        txt_plot.setText(series.plot());
        txt_cast.setText(series.actors());
        txt_gens.setText(series.genres());
        txt_ratd.setText(series.rated());
        txt_sinc.setText(series.firstAired());
        txt_tvsc.setText(String.format(getString(R.string.seract_lbl_votes), series.tvdbRating, series.tvdbVotes));
        txt_aird.setText(Long.toString(series.airedEpisodes()));
        txt_seen.setText(Long.toString(series.watchedEpisodes()));
        txt_coll.setText(Long.toString(series.collectedEpisodes()));
        txt_miss.setText(Long.toString(series.missingEpisodes()));
        txt_tags.setText(series.getTags());
        rat_myrt.setRating(series.rating);

        LinearLayoutManager hlm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        grd_seas.setLayoutManager(hlm);
        grd_seas.setAdapter(new SeasonAdapter());
        grd_seas.setFocusable(false);

        btn_wlst.setText(series.watchlist ? R.string.seract_btn_wlst0 : R.string.seract_btn_wlst1);

        long eaird = series.airedEpisodes();

        btn_coll.setEnabled(eaird > 0);
        btn_coll.setText(series.collectedEpisodes() < eaird ? R.string.seract_btn_coll1 : R.string.seract_btn_coll0);
        btn_coll.setAlpha((float) (btn_coll.isEnabled() ? 1 : 0.3));

        btn_seen.setEnabled(eaird > 0);
        btn_seen.setText(series.watchedEpisodes() < eaird ? R.string.seract_btn_seen1 : R.string.seract_btn_seen0);
        btn_seen.setAlpha((float) (btn_seen.isEnabled() ? 1 : 0.3));

        btn_imdb.setEnabled(!TextUtils.isEmpty(series.imdb_id));
        btn_imdb.setAlpha((float) (btn_imdb.isEnabled() ? 1 : 0.3));

        if (!series.images.isEmpty()) {
            final Series.FanArt pic = series.getLastImage();
            int pstHeight = Math.round((pstWidth * pic.height) / pic.width);
            session.picasso(pic.fullres, R.drawable.series_fanart)
                    .resize(pstWidth, pstHeight)
                    .into(img_post, new Callback() {
                        @Override
                        public void onSuccess() {
                            //
                        }
                        @Override
                        public void onError() {
                            series.images.remove(pic);
                            series.save(true); // will issue onTitleUpdate()
                        }
                    });
        }
    }

    private class SeasonAdapter extends RecyclerView.Adapter<SeasonAdapter.SeasonViewHolder> {

        List<Integer> seasons = series.episodes.getSeasons();

        @Override
        public SeasonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_season, parent, false);
            return new SeasonViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final SeasonViewHolder holder, final int position) {
            holder.season = seasons.get(position);
            holder.btn.setText(Integer.toString(holder.season));
            holder.btn.setEnabled(!updating);
            holder.btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!updating)
                        SeasonActivity.start(SeriesActivity.this, series, holder.season);
                }
            });
        }

        @Override
        public int getItemCount() {
            return seasons.size();
        }

        class SeasonViewHolder extends RecyclerView.ViewHolder {
            public int season;
            Button btn;

            SeasonViewHolder(View view) {
                super(view);
                btn = (Button) view.getRootView();
            }
        }
    }
}
