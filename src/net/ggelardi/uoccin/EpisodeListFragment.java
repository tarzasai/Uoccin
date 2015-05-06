package net.ggelardi.uoccin;

import net.ggelardi.uoccin.adapters.EpisodeAdapter;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.photos.views.HeaderGridView;

public class EpisodeListFragment extends BaseFragment implements AbsListView.OnItemClickListener, OnTitleListener {
	
	private String series;
	private int season;
	
	private boolean forceReload = false;
	
	private AbsListView mListView;
	private EpisodeAdapter mAdapter;
	
	private TextView txtShdrPrev;
	private TextView txtShdrNext;
	private TextView lblShdrSeas;
	private LinearLayout boxShdrFlgs;
	private ImageView imgShdrColl;
	private ImageView imgShdrSeen;
	
	public static EpisodeListFragment newList(String series, int season) {
		EpisodeListFragment fragment = new EpisodeListFragment();
		Bundle args = new Bundle();
		args.putString("series", series);
		args.putInt("season", season);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		series = args.getString("series");
		season = args.getInt("season");
		
		mAdapter = new EpisodeAdapter(getActivity(), this);
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_season_items, container, false);
		
		mListView = (AbsListView) view.findViewById(android.R.id.list);
		mListView.setOnItemClickListener(this);
		
		if (mListView instanceof HeaderGridView)
			((HeaderGridView) mListView).addHeaderView(inflater.inflate(R.layout.header_space, null));
		else if (mListView instanceof ListView) {
			((ListView) mListView).addHeaderView(inflater.inflate(R.layout.header_space, null));
			((ListView) mListView).addFooterView(inflater.inflate(R.layout.header_space, null));
		}
		
		txtShdrPrev = (TextView) view.findViewById(R.id.txt_sehdr_prev);
		txtShdrNext = (TextView) view.findViewById(R.id.txt_sehdr_next);
		lblShdrSeas = (TextView) view.findViewById(R.id.txt_sehdr_seas);
		boxShdrFlgs = (LinearLayout) view.findViewById(R.id.box_sehdr_flgs);
		imgShdrColl = (ImageView) view.findViewById(R.id.img_sehdr_coll);
		imgShdrSeen = (ImageView) view.findViewById(R.id.img_sehdr_seen);
		
		final Series ser = Series.get(getActivity(), series);
		
		txtShdrPrev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				season = ser.seasons().get(ser.seasons().indexOf(season) - 1);
				reload();
			}
		});
		
		txtShdrNext.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				season = ser.seasons().get(ser.seasons().indexOf(season) + 1);
				reload();
			}
		});
		
		ImageView flgs = (ImageView) view.findViewById(R.id.img_sehdr_flgs);
		flgs.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mAdapter.setSwitches(!mAdapter.getSwitches());
				boxShdrFlgs.setVisibility(mAdapter.getSwitches() ? View.VISIBLE : View.GONE);
			}
		});
		
		imgShdrColl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean flag = ser.episodeCollected(season) < ser.episodeAired(season);
				int msg = flag ? R.string.ask_set_season_coll_true : R.string.ask_set_season_coll_false;
				new AlertDialog.Builder(getActivity()).setTitle(ser.name).setMessage(msg).
					setIcon(R.drawable.ic_active_storage).setNegativeButton(R.string.dlg_btn_cancel, null).
					setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ser.setCollected(flag, season);
							imgShdrColl.setImageResource(ser.episodeCollected(season) >= ser.episodeAired(season) ?
								R.drawable.ic_active_storage : R.drawable.ic_action_storage);
						}
					}).show();
			}
		});
		
		imgShdrSeen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean flag = ser.episodeWatched(season) < ser.episodeAired(season);
				int msg = flag ? R.string.ask_set_season_seen_true : R.string.ask_set_season_seen_false;
				new AlertDialog.Builder(getActivity()).setTitle(ser.name).setMessage(msg).
					setIcon(R.drawable.ic_active_seen).setNegativeButton(R.string.dlg_btn_cancel, null).
					setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							ser.setWatched(flag, season);
							imgShdrSeen.setImageResource(ser.episodeWatched(season) >= ser.episodeAired(season) ?
								R.drawable.ic_active_seen : R.drawable.ic_action_seen);
						}
					}).show();
			}
		});
		
		((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Title.addOnTitleEventListener(this);
		reload();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Title.removeOnTitleEventListener(this);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mListener.setIcon(R.drawable.ic_action_tv);
	}
	
	@Override
	public void onClick(View v) {
		int pos;
		try {
			pos = (Integer) v.getTag();
		} catch (Exception err) {
			return; // wtf?
		}
		if (v.getId() == R.id.img_epitm_coll) {
			Episode ep = mAdapter.getItem(pos);
			ep.setCollected(!ep.inCollection());
			return;
		}
		if (v.getId() == R.id.img_epitm_seen) {
			Episode ep = mAdapter.getItem(pos);
			ep.setWatched(!ep.isWatched());
			return;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int pos = mListView instanceof GridView ? position - 2 : position - 1;
		Episode ep = mAdapter.getItem(pos);
		mListener.openSeriesEpisode(ep.series, ep.season, ep.episode);
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
						mAdapter.notifyDataSetChanged();
						Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_SHORT).show();
					} else if (state.equals(OnTitleListener.WORKING)) {
						showHourGlass(true);
					} else if (state.equals(OnTitleListener.RELOAD)) {
						forceReload = true;
						reload();
					} else if (state.equals(OnTitleListener.ERROR)) {
						showHourGlass(false);
						mAdapter.notifyDataSetChanged();
						Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
					} else if (state.equals(OnTitleListener.READY)) {
						showHourGlass(false);
						mAdapter.notifyDataSetChanged();
					}
				}
			});
	}
	
	private void reload() {
		Log.v(getTag(), "reload()");
		Series ser = Series.get(getActivity(), series);
		getActivity().setTitle(ser.name);
		lblShdrSeas.setText(Integer.toString(season));
		int sid = ser.seasons().indexOf(season);
		if (sid <= 0)
			txtShdrPrev.setVisibility(View.GONE);
		else {
			txtShdrPrev.setVisibility(View.VISIBLE);
			txtShdrPrev.setText(Integer.toString(ser.seasons().get(sid - 1)));
		}
		if (sid >= ser.seasons().size() - 1)
			txtShdrNext.setVisibility(View.GONE);
		else {
			txtShdrNext.setVisibility(View.VISIBLE);
			txtShdrNext.setText(Integer.toString(ser.seasons().get(sid + 1)));
		}
		boxShdrFlgs.setVisibility(View.GONE);
		imgShdrColl.setImageResource(ser.episodeCollected(season) >= ser.episodeAired(season) ?
			R.drawable.ic_active_storage : R.drawable.ic_action_storage);
		imgShdrSeen.setImageResource(ser.episodeWatched(season) >= ser.episodeAired(season) ?
			R.drawable.ic_active_seen : R.drawable.ic_action_seen);
		mAdapter.setTitles(ser.episodes(season), forceReload);
		forceReload = false;
	}
}