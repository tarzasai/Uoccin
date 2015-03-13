package net.ggelardi.uoccin;

import net.ggelardi.uoccin.adapters.SeasonAdapter;
import net.ggelardi.uoccin.comp.ExpandableHeightGridView;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SeriesInfoFragment extends BaseFragment {

	private String tvdb_id;
	private Series series;
	
	private ImageView img_bann;
	private TextView txt_netw;
	private TextView txt_airt;
	private TextView txt_ratd;
	private TextView txt_wlst;
	private TextView txt_coll;
	private TextView txt_seen;
	private TextView txt_shar;
	private TextView txt_tvdb;
	private TextView txt_imdb;
	private TextView txt_refr;
	private TextView txt_plot;
	private TextView txt_acts;
	private TextView txt_gens;
	private ExpandableHeightGridView grd_seas;
	
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
		txt_shar = (TextView) view.findViewById(R.id.txt_serinf_shar);
		txt_tvdb = (TextView) view.findViewById(R.id.txt_serinf_tvdb);
		txt_imdb = (TextView) view.findViewById(R.id.txt_serinf_imdb);
		txt_refr = (TextView) view.findViewById(R.id.txt_serinf_refr);
		txt_plot = (TextView) view.findViewById(R.id.txt_serinf_plot);
		txt_acts = (TextView) view.findViewById(R.id.txt_serinf_acts);
		txt_gens = (TextView) view.findViewById(R.id.txt_serinf_gens);
		grd_seas = (ExpandableHeightGridView) view.findViewById(R.id.grd_serinf_seas);
		
		txt_wlst.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				series.setWatchlist(!series.inWatchlist());
			}
		});
		
		txt_coll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean flag = series.episodeCollected(null) < series.episodeCount(null);
				int msg = flag ? R.string.ask_set_series_coll_true : R.string.ask_set_series_coll_false;
				new AlertDialog.Builder(getActivity()).setTitle(series.name).setMessage(msg).
					setIcon(R.drawable.ic_active_storage).setNegativeButton(R.string.dlg_btn_cancel, null).
					setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							series.setCollected(flag, -1);
						}
					}).show();
			}
		});
		
		txt_seen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean flag = series.episodeWatched(null) < series.episodeCount(null);
				int msg = flag ? R.string.ask_set_series_seen_true : R.string.ask_set_series_seen_false;
				new AlertDialog.Builder(getActivity()).setTitle(series.name).setMessage(msg).
					setIcon(R.drawable.ic_active_seen).setNegativeButton(R.string.dlg_btn_cancel, null).
					setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							series.setWatched(flag, -1);
						}
					}).show();
			}
		});
		
		txt_shar.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent si = new Intent(Intent.ACTION_SEND, Uri.parse(series.tvdbUrl()));
			    si.setType("*/*");
			    si.putExtra(Intent.EXTRA_TITLE, series.name);
			    si.putExtra(Intent.EXTRA_SUBJECT, series.name);
			    si.putExtra(Intent.EXTRA_TEXT, series.plot);
			    //si.putExtra(Intent.EXTRA_STREAM, Uri.parse(series.poster));
			    startActivity(Intent.createChooser(si, "Share series info"));
			}
		});
		
		txt_refr.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				series.refresh();
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
		
		Series.addOnTitleEventListener(new OnTitleListener() {
			@Override
			public void changed(final String state, final Throwable error) {
				Log.v(logTag(), state);
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
		pstHeight = Math.round((pstWidth*140)/758);
		
		series = Series.get(getActivity(), tvdb_id);
		if (series.isNew())
			series.refresh();
		else
			showInfo();
	}
	
	private void showInfo() {
		if (img_bann == null)
			return;
		getActivity().setTitle(series.name);
		session.picasso(series.banner).resize(pstWidth, pstHeight).into(img_bann);
		txt_netw.setText(series.network());
		txt_airt.setText(series.airTime());
		txt_airt.setTextColor(getResources().getColor(series.isEnded() ? android.R.color.holo_red_dark :
			R.color.textColorNormal));
		txt_ratd.setText(series.rated());
		txt_wlst.setCompoundDrawablesWithIntrinsicBounds(0, series.inWatchlist() ?
			R.drawable.ic_active_loved : R.drawable.ic_action_loved, 0, 0);
		txt_coll.setCompoundDrawablesWithIntrinsicBounds(0, (series.episodeCount(null) > 0 &&
			series.episodeCollected(null) == series.episodeCount(null)) ? R.drawable.ic_active_storage :
			R.drawable.ic_action_storage, 0, 0);
		txt_seen.setCompoundDrawablesWithIntrinsicBounds(0, (series.episodeCount(null) > 0 &&
			series.episodeWatched(null) == series.episodeCount(null)) ? R.drawable.ic_active_seen :
			R.drawable.ic_action_seen, 0, 0);
		txt_plot.setText(series.plot());
		txt_acts.setText(series.actors());
		txt_gens.setText(series.genres());
		grd_seas.setAdapter(new SeasonAdapter(getActivity(), series));
		txt_imdb.setEnabled(!TextUtils.isEmpty(series.imdb_id));
	}
}