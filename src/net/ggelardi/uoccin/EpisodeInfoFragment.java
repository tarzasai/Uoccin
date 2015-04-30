package net.ggelardi.uoccin;

import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class EpisodeInfoFragment extends BaseFragment {
	
	private String series;
	private int seasNo;
	private int episNo;
	private Episode episode;
	
	private TextView lbl_seas;
	private TextView txt_seas;
	private TextView txt_epis;
	private TextView txt_subs;
	private TextView txt_time;
	private ImageView img_scrn;
	private TextView txt_name;
	private TextView txt_prev;
	private TextView txt_coll;
	private TextView txt_seen;
	private TextView txt_shar;
	private TextView txt_refr;
	private TextView txt_next;
	private TextView txt_plot;
	private LinearLayout box_gues;
	private TextView txt_gues;
	private LinearLayout box_writ;
	private TextView txt_writ;
	private LinearLayout box_dire;
	private TextView txt_dire;
	private TextView txt_tvdb;
	private TextView txt_imdb;
	
	private int pstHeight = 1;
	private int pstWidth = 1;
	
	public static EpisodeInfoFragment newInstance(String series, int season, int episode) {
		EpisodeInfoFragment fragment = new EpisodeInfoFragment();
		Bundle args = new Bundle();
		args.putString("series", series);
		args.putInt("season", season);
		args.putInt("episode", episode);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		
		series = args.getString("series");
		seasNo = args.getInt("season");
		episNo = args.getInt("episode");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_episode_info, container, false);
		
		lbl_seas = (TextView) view.findViewById(R.id.lbl_epinf_seas);
		txt_seas = (TextView) view.findViewById(R.id.txt_epinf_seas);
		txt_epis = (TextView) view.findViewById(R.id.txt_epinf_epis);
		txt_subs = (TextView) view.findViewById(R.id.txt_epinf_subs);
		txt_time = (TextView) view.findViewById(R.id.txt_epinf_time);
		img_scrn = (ImageView) view.findViewById(R.id.img_epinf_scrn);
		txt_name = (TextView) view.findViewById(R.id.txt_epinf_name);
		txt_prev = (TextView) view.findViewById(R.id.txt_epinf_prev);
		txt_coll = (TextView) view.findViewById(R.id.txt_epinf_coll);
		txt_seen = (TextView) view.findViewById(R.id.txt_epinf_seen);
		txt_shar = (TextView) view.findViewById(R.id.txt_epinf_shar);
		txt_refr = (TextView) view.findViewById(R.id.txt_epinf_refr);
		txt_next = (TextView) view.findViewById(R.id.txt_epinf_next);
		txt_plot = (TextView) view.findViewById(R.id.txt_epinf_plot);
		box_gues = (LinearLayout) view.findViewById(R.id.box_epinf_gues);
		txt_gues = (TextView) view.findViewById(R.id.txt_epinf_gues);
		box_writ = (LinearLayout) view.findViewById(R.id.box_epinf_writ);
		txt_writ = (TextView) view.findViewById(R.id.txt_epinf_writ);
		box_dire = (LinearLayout) view.findViewById(R.id.box_epinf_dire);
		txt_dire = (TextView) view.findViewById(R.id.txt_epinf_dire);
		txt_tvdb = (TextView) view.findViewById(R.id.txt_epinf_tvdb);
		txt_imdb = (TextView) view.findViewById(R.id.txt_epinf_imdb);
		
		txt_prev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txt_prev.startAnimation(blink);
				episode = episode.getPrior();
				showInfo();
			}
		});
		
		txt_next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txt_next.startAnimation(blink);
				episode = episode.getNext();
				showInfo();
			}
		});
		
		txt_coll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				episode.setCollected(!episode.inCollection());
				txt_coll.startAnimation(blink);
			}
		});
		
		txt_seen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				episode.setWatched(!episode.isWatched());
				txt_seen.startAnimation(blink);
			}
		});
		
		txt_shar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Series ser = episode.getSeries();
				String title = ser.name + " " + episode.eid().sequence();
				StringBuilder sb = new StringBuilder();
				sb.append("*").append(title).append("*");
				if (!TextUtils.isEmpty(episode.name))
					sb.append(": ").append("_").append(episode.name).append("_");
				sb.append("\n").append(ser.network).append(", ").append(episode.firstAired());
				if (!TextUtils.isEmpty(episode.plot))
					sb.append("\n").append("\n").append(episode.plot());
				if (!TextUtils.isEmpty(episode.poster))
					sb.append("\n").append("\n").append(episode.poster);
				sb.append("\n").append(!TextUtils.isEmpty(episode.imdbUrl()) ? episode.imdbUrl() : episode.tvdbUrl());
				Intent si = new Intent(Intent.ACTION_SEND);
			    si.setType("text/plain");
			    si.putExtra(Intent.EXTRA_TITLE, title);
			    si.putExtra(Intent.EXTRA_SUBJECT, title);
			    si.putExtra(Intent.EXTRA_TEXT, sb.toString());
			    startActivity(Intent.createChooser(si, "Share episode info"));
			}
		});
		
		txt_refr.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				episode.refresh(true);
				txt_refr.startAnimation(blink);
			}
		});
		
		txt_tvdb.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(episode.tvdbUrl())));
			}
		});
		
		txt_imdb.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(episode.imdbUrl())));
			}
		});
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Episode.addOnTitleEventListener(new OnTitleListener() {
			@Override
			public void changed(final String state, final Throwable error) {
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
		});
		
		WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
		Point size = new Point();
		wm.getDefaultDisplay().getSize(size);
		pstWidth = size.x;
		pstHeight = Math.round((pstWidth*225)/400);
		
		episode = Episode.get(getActivity(), series, seasNo, episNo);
		if (episode.isNew() || episode.isOld())
			episode.refresh(true);
		else
			showInfo();
	}
	
	private void showInfo() {
		if (txt_seas == null)
			return;
		getActivity().setTitle(episode.getSeries().name);
		lbl_seas.setCompoundDrawablesWithIntrinsicBounds(episode.isPilot() ? R.drawable.ics_action_news : 0, 0, 0, 0);
		txt_seas.setText(Integer.toString(episode.season));
		txt_epis.setText(Integer.toString(episode.episode));
		txt_time.setText(episode.firstAired());
		txt_time.setCompoundDrawablesWithIntrinsicBounds(0, 0, episode.isToday() ? R.drawable.ics_action_calendar :
			0, 0);
		img_scrn.setImageBitmap(null);
		session.picasso(episode.poster).resize(pstWidth, pstHeight).into(img_scrn);
		txt_name.setText(episode.name());
		if (!episode.hasSubtitles())
			txt_subs.setVisibility(View.GONE);
		else {
			txt_subs.setVisibility(View.VISIBLE);
			txt_subs.setText(episode.subtitles());
		}
		Episode ep = episode.getPrior();
		txt_prev.setEnabled(ep != null);
		txt_prev.setText(ep != null ? ep.eid().readable() : session.getString(R.string.none_text));
		ep = episode.getNext();
		txt_next.setEnabled(ep != null);
		txt_next.setText(ep != null ? ep.eid().readable() : session.getString(R.string.none_text));
		txt_coll.setCompoundDrawablesWithIntrinsicBounds(0, episode.inCollection() ?
			R.drawable.ic_active_storage : R.drawable.ic_action_storage, 0, 0);
		txt_seen.setCompoundDrawablesWithIntrinsicBounds(0, episode.isWatched() ?
			R.drawable.ic_active_seen : R.drawable.ic_action_seen, 0, 0);
		txt_plot.setText(episode.plot());
		if (episode.guestStars.isEmpty())
			box_gues.setVisibility(View.GONE);
		else {
			box_gues.setVisibility(View.VISIBLE);
			txt_gues.setText(episode.guests());
		}
		if (episode.writers.isEmpty())
			box_writ.setVisibility(View.GONE);
		else {
			box_writ.setVisibility(View.VISIBLE);
			txt_writ.setText(episode.writers());
		}
		if (TextUtils.isEmpty(episode.director))
			box_dire.setVisibility(View.GONE);
		else {
			box_dire.setVisibility(View.VISIBLE);
			txt_dire.setText(episode.director());
		}
		txt_imdb.setEnabled(!TextUtils.isEmpty(episode.imdb_id));
	}
}