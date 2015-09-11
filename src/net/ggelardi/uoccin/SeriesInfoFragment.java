package net.ggelardi.uoccin;

import java.util.Locale;

import net.ggelardi.uoccin.adapters.SeasonAdapter;
import net.ggelardi.uoccin.comp.ExpandableHeightGridView;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class SeriesInfoFragment extends BaseFragment implements OnTitleListener {
	
	private String tvdb_id;
	private Series series;
	
	private ImageView img_bann;
	private TextView txt_netw;
	private TextView txt_airt;
	private TextView txt_ratd;
	private TextView txt_wlst;
	private TextView txt_coll;
	private TextView txt_seen;
	private TextView txt_tvdb;
	private TextView txt_imdb;
	private TextView txt_refr;
	private TextView txt_plot;
	private TextView txt_acts;
	private TextView txt_gens;
	private RatingBar rat_myrt;
	private TextView txt_tags;
	private ExpandableHeightGridView grd_seas;
	private AlertDialog dlg_tags;
	private MenuItem miShare;
	private MenuItem miForget;
	
	private int pstHeight = 1;
	private int pstWidth = 1;
	
	public static SeriesInfoFragment newInstance(String tvdb_id) {
		SeriesInfoFragment fragment = new SeriesInfoFragment();
		Bundle args = new Bundle();
		args.putString("tvdb_id", tvdb_id);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		Bundle args = getArguments();
		tvdb_id = args.getString("tvdb_id");
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_series_info, container, false);
		
		img_bann = (ImageView) view.findViewById(R.id.img_serinf_bann);
		txt_netw = (TextView) view.findViewById(R.id.txt_serinf_netw);
		txt_airt = (TextView) view.findViewById(R.id.txt_serinf_airt);
		txt_ratd = (TextView) view.findViewById(R.id.txt_serinf_ratd);
		txt_wlst = (TextView) view.findViewById(R.id.txt_serinf_wlst);
		txt_coll = (TextView) view.findViewById(R.id.txt_serinf_coll);
		txt_seen = (TextView) view.findViewById(R.id.txt_serinf_seen);
		txt_tvdb = (TextView) view.findViewById(R.id.txt_serinf_tvdb);
		txt_imdb = (TextView) view.findViewById(R.id.txt_serinf_imdb);
		txt_refr = (TextView) view.findViewById(R.id.txt_serinf_refr);
		txt_plot = (TextView) view.findViewById(R.id.txt_serinf_plot);
		txt_acts = (TextView) view.findViewById(R.id.txt_serinf_acts);
		txt_gens = (TextView) view.findViewById(R.id.txt_serinf_gens);
		rat_myrt = (RatingBar) view.findViewById(R.id.rat_serinf_myrt);
		txt_tags = (TextView) view.findViewById(R.id.txt_serinf_tags);
		grd_seas = (ExpandableHeightGridView) view.findViewById(R.id.grd_serinf_seas);
		
		txt_wlst.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				series.setWatchlist(!series.inWatchlist());
				txt_wlst.startAnimation(blink);
			}
		});
		
		txt_coll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean flag = series.episodeCollected(null) < series.episodeAired(null);
				int msg = flag ? R.string.ask_set_series_coll_true : R.string.ask_set_series_coll_false;
				new AlertDialog.Builder(getActivity()).setTitle(series.name).setMessage(msg).
					setIcon(R.drawable.ic_active_storage).setNegativeButton(R.string.dlg_btn_cancel, null).
					setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							series.setCollected(flag, null);
							txt_coll.startAnimation(blink);
						}
					}).show();
			}
		});
		
		txt_seen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean flag = series.episodeWatched(null) < series.episodeAired(null);
				int msg = flag ? R.string.ask_set_series_seen_true : R.string.ask_set_series_seen_false;
				new AlertDialog.Builder(getActivity()).setTitle(series.name).setMessage(msg).
					setIcon(R.drawable.ic_active_seen).setNegativeButton(R.string.dlg_btn_cancel, null).
					setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							series.setWatched(flag, null);
							txt_seen.startAnimation(blink);
						}
					}).show();
			}
		});
		
		txt_refr.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				series.refresh(true);
				txt_refr.startAnimation(blink);
			}
		});
		
		rat_myrt.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
			@Override
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
				if (fromUser)
					series.setRating(Math.round(rating));
			}
		});
		
		txt_tags.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editTags();
			}
		});
		
		txt_tvdb.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(series.tvdbUrl())));
			}
		});
		
		txt_imdb.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(series.imdbUrl())));
			}
		});
		
		grd_seas.setExpanded(true);
		grd_seas.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				mListener.openSeriesSeason(series.tvdb_id, position+1);
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
		pstWidth = size.x;
		pstHeight = Math.round((pstWidth*140)/758);
		
		series = Series.get(getActivity(), tvdb_id);
		Title.addOnTitleEventListener(this);
		
		if (series.isNew() || series.isOld() || series.episodes.isEmpty())
			series.refresh(true);
		else
			showInfo();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Title.removeOnTitleEventListener(this);
		
		if (dlg_tags != null) {
			dlg_tags.dismiss(); // to avoid the "Activity has leaked window bla bla" error.
			dlg_tags = null;
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.series, menu);

		miShare = menu.findItem(R.id.action_share);
		miForget = menu.findItem(R.id.action_forget);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == miShare) {
			StringBuilder sb = new StringBuilder();
			sb.append("*").append(series.name.toUpperCase(Locale.getDefault())).append("*");
			sb.append("\n").append(series.network);
			if (series.isRecent())
				sb.append(" #premiere");
			sb.append(" - _").append(series.genres()).append("_");
			sb.append("\n").append("\n").append(series.plot);
			sb.append("\n").append("\n").append(series.poster);
			sb.append("\n").append(!TextUtils.isEmpty(series.imdbUrl()) ? series.imdbUrl() : series.tvdbUrl());
			Intent si = new Intent(Intent.ACTION_SEND);
		    si.setType("text/plain");
		    si.putExtra(Intent.EXTRA_TITLE, series.name);
		    si.putExtra(Intent.EXTRA_SUBJECT, series.name);
		    si.putExtra(Intent.EXTRA_TEXT, sb.toString());
		    startActivity(Intent.createChooser(si, "Share series info"));
			return true;
		}
		if (item == miForget) {
			new AlertDialog.Builder(getActivity()).setTitle(series.name).setMessage(R.string.ask_forget_series).
			setIcon(android.R.drawable.ic_dialog_alert).setNegativeButton(R.string.dlg_btn_cancel, null).
			setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					series.setWatchlist(false);
					series.setCollected(false, null);
					series.setWatched(false, null);
					series.addTag(Title.FORGET_TAG);
				}
			}).show();
			return true;
		}
		return false;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mListener.setIcon(R.drawable.ic_action_tv);
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
						series.reload();
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
		if (img_bann == null)
			return;
		getActivity().setTitle(series.name);
		session.picasso(series.banner).resize(pstWidth, pstHeight).into(img_bann);
		if (pstWidth > 1) {
			img_bann.setMinimumWidth(pstWidth);
			img_bann.setMaxWidth(pstWidth);
			img_bann.setMinimumHeight(pstHeight);
			img_bann.setMaxHeight(pstHeight);
		}
		txt_netw.setText(series.network());
		txt_airt.setText(series.airTime());
		txt_airt.setTextColor(getResources().getColor(series.isEnded() ? android.R.color.holo_red_dark :
			R.color.textColorNormal));
		txt_ratd.setText(series.rated());
		txt_wlst.setCompoundDrawablesWithIntrinsicBounds(0, series.inWatchlist() ?
			R.drawable.ic_active_loved : R.drawable.ic_action_loved, 0, 0);
		txt_coll.setCompoundDrawablesWithIntrinsicBounds(0, (series.episodeAired(null) > 0 &&
			series.episodeCollected(null) >= series.episodeAired(null)) ? R.drawable.ic_active_storage :
			R.drawable.ic_action_storage, 0, 0);
		txt_seen.setCompoundDrawablesWithIntrinsicBounds(0, (series.episodeAired(null) > 0 &&
			series.episodeWatched(null) >= series.episodeAired(null)) ? R.drawable.ic_active_seen :
			R.drawable.ic_action_seen, 0, 0);
		txt_plot.setText(series.plot());
		txt_acts.setText(series.actors());
		txt_gens.setText(series.genres());
		rat_myrt.setRating(series.getRating());
		txt_tags.setText(series.tags());
		grd_seas.setAdapter(new SeasonAdapter(getActivity(), series));
		txt_imdb.setEnabled(!TextUtils.isEmpty(series.imdb_id));
	}
	
	@SuppressLint("InflateParams")
	private void editTags() {
		final LayoutInflater inflater = getActivity().getLayoutInflater();
		final View view = inflater.inflate(R.layout.dialog_tags, null);
		final MultiAutoCompleteTextView edt = (MultiAutoCompleteTextView) view.getRootView();
		dlg_tags = new AlertDialog.Builder(getActivity()).setTitle(R.string.lbl_comm_tags).
			setView(view).setCancelable(true).setIcon(R.drawable.ic_action_tags).
			setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					series.setTags(edt.getText().toString().split(",\\s*"));
					dlg_tags = null;
				}
			}).setNegativeButton(R.string.dlg_btn_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dlg_tags = null;
				}
			}).create();
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(session.getContext(),
			android.R.layout.simple_dropdown_item_1line, session.getAllTags());
		edt.setAdapter(adapter);
		edt.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
		edt.setThreshold(1);
		edt.setDropDownBackgroundResource(R.color.textColorNormal);
		edt.setText(TextUtils.join(", ", series.getTags()));
		dlg_tags.show();
	}
}