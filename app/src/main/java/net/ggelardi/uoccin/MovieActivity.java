package net.ggelardi.uoccin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.serv.Session;

import java.util.Locale;

public class MovieActivity extends BaseActivity {

    private static Movie cached_movie;

    public static void start(Context context, Movie movie) {
        cached_movie = movie;
        context.startActivity(new Intent(context, MovieActivity.class)
                .putExtra("movieId", movie.imdb_id));
    }

    private String movieId;
    private Movie movie;
    private int pstWidth;
    private int pstHeight;

    private ImageView img_post;
    private TextView txt_dire;
    private TextView txt_coun;
    private TextView txt_gens;
    private TextView txt_ratd;
    private TextView txt_runt;
    private TextView txt_lang;
    private TextView txt_stat;
    private TextView txt_name;
    private TextView txt_plot;
    private TextView txt_cast;
    private LinearLayout box_wrts;
    private TextView txt_wrts;
    private TextView txt_tags;
    private LinearLayout box_awrs;
    private TextView txt_awrs;
    private LinearLayout box_rams;
    private TextView txt_rams;
    private LinearLayout box_rart;
    private TextView txt_rart;
    private LinearLayout box_raim;
    private TextView txt_raim;
    private RatingBar rat_myrt;
    private BottomSheetDialog bsd_menu;
    private BottomSheetBehavior bsb_menu;
    private TextView btn_wlst;
    private TextView btn_seen;
    private TextView btn_coll;
    private TextView btn_imdb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie);
        setUoccinActionBar(findViewById(R.id.toolbar));

        img_post = (ImageView) findViewById(R.id.img_poster);
        txt_dire = (TextView) findViewById(R.id.txt_director);
        txt_coun = (TextView) findViewById(R.id.txt_country);
        txt_gens = (TextView) findViewById(R.id.txt_genres);
        txt_ratd = (TextView) findViewById(R.id.txt_rated);
        txt_runt = (TextView) findViewById(R.id.txt_runtime);
        txt_lang = (TextView) findViewById(R.id.txt_langs);
        txt_stat = (TextView) findViewById(R.id.txt_status);
        txt_name = (TextView) findViewById(R.id.txt_title);
        txt_plot = (TextView) findViewById(R.id.txt_plot);
        txt_cast = (TextView) findViewById(R.id.txt_cast);
        box_wrts = (LinearLayout) findViewById(R.id.box_writers);
        txt_wrts = (TextView) findViewById(R.id.txt_writers);
        txt_tags = (TextView) findViewById(R.id.txt_tags);
        box_awrs = (LinearLayout) findViewById(R.id.box_awards);
        txt_awrs = (TextView) findViewById(R.id.txt_awards);
        box_rams = (LinearLayout) findViewById(R.id.box_metascore);
        txt_rams = (TextView) findViewById(R.id.txt_ratmeta);
        box_rart = (LinearLayout) findViewById(R.id.box_tomatometer);
        txt_rart = (TextView) findViewById(R.id.txt_ratrott);
        box_raim = (LinearLayout) findViewById(R.id.box_imdbscore);
        txt_raim = (TextView) findViewById(R.id.txt_ratimdb);
        rat_myrt = (RatingBar) findViewById(R.id.rat_rating);

        txt_tags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                movie.editTags(MovieActivity.this);
            }
        });

        rat_myrt.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                if (fromUser)
                    movie.setRating(Math.round(rating));
            }
        });

        View bsv = getLayoutInflater().inflate(R.layout.dialog_movie_menu, null);
        bsd_menu = new BottomSheetDialog(MovieActivity.this);
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
                movie.setWatchlist(!movie.watchlist);
                //showData();
            }
        });

        btn_coll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                movie.setCollected(!movie.collected);
                if (movie.collected)
                    movie.setWatchlist(false);
            }
        });

        btn_seen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                movie.setWatched(!movie.watched);
                if (movie.watched)
                    movie.setWatchlist(false);
            }
        });

        bsv.findViewById(R.id.btn_updt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                showMessage(R.string.movact_msg_updating);
                movie.refresh(true);
            }
        });

        btn_imdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsd_menu.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(movie.imdbUrl())));
            }
        });

        String avatar = session.driveAccountPhoto();
        if (!TextUtils.isEmpty(avatar))
            ((ImageView) findViewById(R.id.img_user)).setImageURI(Uri.parse(avatar));
    }

    @Override
    protected void onResume() {
        super.onResume();

        Point screenSize = new Point();
        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
        pstWidth = screenSize.x / 3;
        pstHeight = (pstWidth * 450) / 300;

        showData();

        if (movie.isNew() || movie.isOld())
            movie.refresh(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_movie, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_erase:
                new AlertDialog.Builder(MovieActivity.this).setTitle(movie.name())
                        .setMessage(R.string.movact_ask_delete)
                        .setIcon(R.drawable.ic_dlgico_delete)
                        .setNegativeButton(R.string.dlg_btn_cancel, null)
                        .setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SQLiteDatabase db = session.getDB();
                                if (db.inTransaction()) {
                                    showMessage(R.string.movact_msg_delbusy);
                                    return;
                                }
                                db.beginTransaction();
                                try {
                                    session.driveQueue(Session.QUEUE_MOVIE, movieId, "watchlist",
                                            Boolean.toString(false));
                                    db.delete("movie", "imdb_id = ?", new String[] { movieId });
                                    db.setTransactionSuccessful();
                                    showMessage(R.string.movact_msg_deleted);
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
    protected void updateMovie(Bundle data) {
        super.updateMovie(data);

        String mid = data.getString("movie");
        if (mid == null || mid.equals(movieId)) {
            movie.load();
            if (data.getBoolean("metadata"))
                showMessage(R.string.movact_msg_updated);
            showData();
        }
    }

    protected void showData() {
        if (movie == null) {
            if (movieId == null)
                movieId = getIntent().getStringExtra("movieId");
            if (cached_movie != null && cached_movie.imdb_id == movieId)
                movie = cached_movie;
            else {
                movie = new Movie(this, movieId);
                cached_movie = movie;
            }
        }
        if (!movie.isLoaded())
            movie.load();
        setTitle(movie.name());
        txt_dire.setText(getString(R.string.movact_fmt_director, movie.director));
        if (movie.country == null && movie.year <= 0)
            txt_coun.setVisibility(View.GONE);
        else {
            txt_coun.setVisibility(View.VISIBLE);
            txt_coun.setText(getString(R.string.movact_fmt_country, movie.country, movie.year));
        }
        txt_gens.setText(movie.genres());
        txt_ratd.setText(movie.rated());
        txt_runt.setText(movie.runtime());
        txt_lang.setText(movie.language());
        txt_stat.setVisibility(View.VISIBLE);
        if (movie.watched) {
            txt_stat.setText(R.string.movact_stat_watched);
            txt_stat.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ics_action_watched, 0, 0, 0);
        } else if (movie.collected && movie.subtitles.size() > 0) {
            txt_stat.setText(movie.subtitles().toUpperCase(Locale.getDefault()));
            txt_stat.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ics_action_subtitles, 0, 0, 0);
        } else if (movie.collected) {
            txt_stat.setText(R.string.movact_stat_collected);
            txt_stat.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ics_action_collected, 0, 0, 0);
        } else if (movie.watchlist) {
            txt_stat.setText(R.string.movact_stat_watchlist);
            txt_stat.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ics_action_watchlist, 0, 0, 0);
        } else
            txt_stat.setVisibility(View.GONE);
        txt_name.setText(movie.name());
        CharSequence plot = movie.plot();
        if (plot.length() > 300)
            plot = TextUtils.concat(plot.subSequence(0, 300), getText(R.string.movact_fmt_shortplot));
        txt_plot.setText(plot);
        txt_cast.setText(movie.actors());
        if (movie.writers == null)
            box_wrts.setVisibility(View.GONE);
        else {
            box_wrts.setVisibility(View.VISIBLE);
            txt_wrts.setText(movie.writers);
        }
        txt_tags.setText(movie.getTags());
        /*
        if (movie.awards == null)
            box_awrs.setVisibility(View.GONE);
        else {
            box_awrs.setVisibility(View.VISIBLE);
            txt_awrs.setText(movie.awards);
        }
        if (movie.metascore <= 0)
            box_rams.setVisibility(View.GONE);
        else {
            box_rams.setVisibility(View.VISIBLE);
            txt_rams.setText(getString(R.string.movact_fmt_metascore, movie.metascore));
        }
        if (movie.tomatoMeter <= 0)
            box_rart.setVisibility(View.GONE);
        else {
            box_rart.setVisibility(View.VISIBLE);
            txt_rart.setText(getString(R.string.movact_fmt_tomatometer, movie.tomatoMeter));
        }
        if (movie.imdbVotes <= 0)
            box_raim.setVisibility(View.GONE);
        else {
            box_raim.setVisibility(View.VISIBLE);
            txt_raim.setText(getString(R.string.movact_fmt_imdbscore, movie.imdbRating, movie.imdbVotes));
        }
        */
        rat_myrt.setRating(movie.rating);
        session.picasso(movie.poster, true).resize(pstWidth, pstHeight).into(img_post);
        btn_wlst.setText(movie.watchlist ? R.string.movact_btn_wlst0 : R.string.movact_btn_wlst1);
        btn_coll.setText(movie.collected ? R.string.movact_btn_coll0 : R.string.movact_btn_coll1);
        btn_seen.setText(movie.watched ? R.string.movact_btn_seen0 : R.string.movact_btn_seen1);
    }
}
