package net.ggelardi.uoccin;

import java.text.NumberFormat;
import java.util.Locale;

import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MovieInfoFragment extends BaseFragment implements OnTitleListener {

	private String imdb_id;
	private Movie movie;
	
	private ImageView img_post;
	private TextView txt_dire;
	private TextView txt_ratd;
	private TextView txt_time;
	private TextView txt_subs;
	private TextView txt_year;
	private TextView txt_coun;
	private TextView txt_meta;
	private TextView txt_rott;
	private TextView txt_imrt;
	private TextView txt_imvt;
	private TextView txt_wlst;
	private TextView txt_coll;
	private TextView txt_seen;
	private TextView txt_shar;
	private TextView txt_imdb;
	private TextView txt_refr;
	private TextView txt_plot;
	private TextView txt_acts;
	private TextView txt_lang;
	private TextView txt_gens;
	private RatingBar rat_myrt;
	private TextView txt_tags;
	
	private int pstHeight = 1;
	private int pstWidth = 1;
	
	public static MovieInfoFragment newInstance(String imdb_id) {
		MovieInfoFragment fragment = new MovieInfoFragment();
		Bundle args = new Bundle();
		args.putString("imdb_id", imdb_id);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		imdb_id = args.getString("imdb_id");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_movie_info, container, false);
		
		img_post = (ImageView) view.findViewById(R.id.img_movinf_post);
		txt_year = (TextView) view.findViewById(R.id.txt_movinf_year);
		txt_coun = (TextView) view.findViewById(R.id.txt_movinf_coun);
		txt_time = (TextView) view.findViewById(R.id.txt_movinf_time);
		txt_ratd = (TextView) view.findViewById(R.id.txt_movinf_ratd);
		txt_lang = (TextView) view.findViewById(R.id.txt_movinf_lang);
		txt_subs = (TextView) view.findViewById(R.id.txt_movinf_subs);
		txt_dire = (TextView) view.findViewById(R.id.txt_movinf_dire);
		txt_meta = (TextView) view.findViewById(R.id.txt_movinf_meta);
		txt_rott = (TextView) view.findViewById(R.id.txt_movinf_rott);
		txt_imrt = (TextView) view.findViewById(R.id.txt_movinf_imrt);
		txt_imvt = (TextView) view.findViewById(R.id.txt_movinf_imvt);
		txt_wlst = (TextView) view.findViewById(R.id.txt_movinf_wlst);
		txt_coll = (TextView) view.findViewById(R.id.txt_movinf_coll);
		txt_seen = (TextView) view.findViewById(R.id.txt_movinf_seen);
		txt_shar = (TextView) view.findViewById(R.id.txt_movinf_shar);
		txt_imdb = (TextView) view.findViewById(R.id.txt_movinf_imdb);
		txt_refr = (TextView) view.findViewById(R.id.txt_movinf_refr);
		txt_plot = (TextView) view.findViewById(R.id.txt_movinf_plot);
		txt_acts = (TextView) view.findViewById(R.id.txt_movinf_acts);
		txt_gens = (TextView) view.findViewById(R.id.txt_movinf_gens);
		rat_myrt = (RatingBar) view.findViewById(R.id.rat_movinf_myrt);
		txt_tags = (TextView) view.findViewById(R.id.txt_movinf_tags);
		
		img_post.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: show big poster
				if (!TextUtils.isEmpty(movie.poster))
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(movie.poster)));
			}
		});
		
		txt_wlst.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				movie.setWatchlist(!movie.inWatchlist());
				txt_wlst.startAnimation(blink);
			}
		});
		
		txt_coll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				movie.setCollected(!movie.inCollection());
				txt_wlst.startAnimation(blink);
			}
		});
		
		txt_seen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				movie.setWatched(!movie.isWatched());
				txt_wlst.startAnimation(blink);
			}
		});
		
		txt_shar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent si = new Intent(Intent.ACTION_SEND);
			    si.setType("text/plain");
			    si.putExtra(Intent.EXTRA_TITLE, movie.name);
			    si.putExtra(Intent.EXTRA_SUBJECT, movie.name);
			    si.putExtra(Intent.EXTRA_TEXT, movie.imdbUrl());
			    startActivity(Intent.createChooser(si, "Share movie info"));
			}
		});
		
		txt_refr.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				movie.refresh(true);
				txt_refr.startAnimation(blink);
			}
		});
		
		rat_myrt.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
				if (fromUser)
					movie.setRating(Math.round(rating));
			}
		});
		
		txt_tags.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(getActivity(), "Coming soon :)", Toast.LENGTH_SHORT).show();
			}
		});
		
		txt_imdb.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(movie.imdbUrl())));
			}
		});
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
		Point size = new Point();
		wm.getDefaultDisplay().getSize(size);
		pstWidth = Math.round((size.x*30)/100);
		pstHeight = Math.round((pstWidth*500)/320);
		
		movie = Movie.get(getActivity(), imdb_id);
		Title.addOnTitleEventListener(this);
		
		if (movie.isNew() || movie.isOld())
			movie.refresh(true);
		else
			showInfo();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Title.removeOnTitleEventListener(this);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mListener.setIcon(R.drawable.ic_action_movie);
	}
	
	@Override
	public void onTitleEvent(final String state, final Throwable error) {
		final Activity context = getActivity();
		if (context != null)
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (state.equals(OnTitleListener.NOTFOUND)) {
						showHourGlass(false);
						Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_SHORT).show();
					} else if (state.equals(OnTitleListener.WORKING)) {
						showHourGlass(true);
					} else if (state.equals(OnTitleListener.RELOAD)) {
						movie.reload();
						showInfo();
					} else if (state.equals(OnTitleListener.ERROR)) {
						showHourGlass(false);
						Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
					} else if (state.equals(OnTitleListener.READY)) {
						showHourGlass(false);
						showInfo();
					}
				}
			});
	}
	
	private void showInfo() {
		if (img_post == null)
			return;
		getActivity().setTitle(movie.name);
		session.picasso(movie.poster).resize(pstWidth, pstHeight).into(img_post);
		if (pstWidth > 1) {
			img_post.setMinimumWidth(pstWidth);
			img_post.setMaxWidth(pstWidth);
			img_post.setMinimumHeight(pstHeight);
			img_post.setMaxHeight(pstHeight);
		}
		txt_dire.setText(movie.director());
		txt_ratd.setText(movie.rated());
		txt_time.setText(movie.runtime());
		if (!movie.hasSubtitles())
			txt_subs.setVisibility(View.GONE);
		else {
			txt_subs.setVisibility(View.VISIBLE);
			txt_subs.setText(movie.subtitles());
		}
		txt_year.setText(movie.year());
		txt_coun.setText(movie.country());
		if (movie.metascore <= 0)
			txt_meta.setVisibility(View.GONE);
		else {
			txt_meta.setVisibility(View.VISIBLE);
			txt_meta.setText(Integer.toString(movie.metascore));
		}
		if (movie.tomatoMeter <= 0)
			txt_rott.setVisibility(View.GONE);
		else {
			txt_rott.setVisibility(View.VISIBLE);
			txt_rott.setText(Integer.toString(movie.tomatoMeter) + "%");
		}
		if (movie.imdbRating <= 0) {
			txt_imrt.setVisibility(View.GONE);
			txt_imvt.setVisibility(View.GONE);
		} else {
			txt_imrt.setVisibility(View.VISIBLE);
			txt_imvt.setVisibility(View.VISIBLE);
			txt_imrt.setText(String.format("%.1f", movie.imdbRating));
			txt_imvt.setText(NumberFormat.getNumberInstance(Locale.getDefault()).format(movie.imdbVotes));
			txt_imrt.setTextColor(session.getRes().getColor(movie.imdbVotes > 10000 ? R.color.textColorPrimary :
				R.color.textColorNormal));
		}
		txt_wlst.setCompoundDrawablesWithIntrinsicBounds(0, movie.inWatchlist() ?
			R.drawable.ic_active_loved : R.drawable.ic_action_loved, 0, 0);
		txt_coll.setCompoundDrawablesWithIntrinsicBounds(0, movie.inCollection() ? R.drawable.ic_active_storage :
			R.drawable.ic_action_storage, 0, 0);
		txt_seen.setCompoundDrawablesWithIntrinsicBounds(0, movie.isWatched() ? R.drawable.ic_active_seen :
			R.drawable.ic_action_seen, 0, 0);
		txt_plot.setText(Html.fromHtml(movie.plot()));
		txt_acts.setText(Html.fromHtml(movie.actors() + " ... <a href=\"" + movie.imdbCast() + "\">" +
			session.getString(R.string.lnk_movinf_acts) + "</a>"));
		txt_acts.setMovementMethod(LinkMovementMethod.getInstance());
		txt_lang.setText(movie.language());
		txt_gens.setText(movie.genres());
		rat_myrt.setRating(movie.getRating());
		txt_tags.setText(movie.tags());
	}
}