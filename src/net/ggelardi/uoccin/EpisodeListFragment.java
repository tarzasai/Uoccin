package net.ggelardi.uoccin;

import java.util.List;

import net.ggelardi.uoccin.adapters.EpisodeAdapter;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.EpisodeTask;
import net.ggelardi.uoccin.serv.EpisodeTask.EpisodeTaskContainer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.photos.views.HeaderGridView;

public class EpisodeListFragment extends BaseFragment implements EpisodeTaskContainer, AbsListView.OnItemClickListener {
	
	private String type;
	private String title;
	private String query;
	private String[] params;
	private String series;
	private int season;
	
	private AbsListView mListView;
	private EpisodeAdapter mAdapter;
	private EpisodeTask mTask;
	
	private RelativeLayout rlSHeader;
	private TextView txtShdrPrev;
	private TextView txtShdrNext;
	private TextView lblShdrSeas;
	private LinearLayout boxShdrFlgs;
	private ImageView imgShdrColl;
	private ImageView imgShdrSeen;
	
	public static EpisodeListFragment newQuery(String title, String query, String ... params) {
		EpisodeListFragment fragment = new EpisodeListFragment();
		Bundle args = new Bundle();
		args.putString("type", EpisodeTask.QUERY);
		args.putString("title", title);
		args.putString("query", query);
		args.putStringArray("params", params);
		fragment.setArguments(args);
		return fragment;
	}
	
	public static EpisodeListFragment newList(String series, int season) {
		EpisodeListFragment fragment = new EpisodeListFragment();
		Bundle args = new Bundle();
		args.putString("type", EpisodeTask.LIST);
		args.putString("series", series);
		args.putInt("season", season);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getString("type");
		if (type.equals(EpisodeTask.QUERY)) {
			title = args.getString("title");
			query = args.getString("query");
			params = args.getStringArray("params");
		} else {
			series = args.getString("series");
			season = args.getInt("season");
		}
		
		mAdapter = new EpisodeAdapter(getActivity(), this);
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(type.equals(EpisodeTask.QUERY) ? R.layout.fragment_episode_items :
			R.layout.fragment_season_items, container, false);
		
		mListView = (AbsListView) view.findViewById(android.R.id.list);
		mListView.setOnItemClickListener(this);
		
		if (mListView instanceof HeaderGridView)
			((HeaderGridView) mListView).addHeaderView(inflater.inflate(R.layout.header_space, null));
		else if (mListView instanceof ListView) {
			((ListView) mListView).addHeaderView(inflater.inflate(R.layout.header_space, null));
			((ListView) mListView).addFooterView(inflater.inflate(R.layout.header_space, null));
		}
		
		if (type.equals(EpisodeTask.LIST)) {
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
					load();
				}
			});
			
			txtShdrNext.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					season = ser.seasons().get(ser.seasons().indexOf(season) + 1);
					load();
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
		}
		
		((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Series.addOnTitleEventListener(new OnTitleListener() {
			@Override
			public void changed(final String state, final Throwable error) {
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
		});
		
		load();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (mTask != null)
			mTask.cancel(true);
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
	public Context getContext() {
		return getActivity();
	}
	
	@Override
	public void preExecuteTask() {
		showHourGlass(true);
	}
	
	@Override
	public void postExecuteTask(List<Episode> result) {
		mAdapter.setTitles(result);
		showHourGlass(false);
		mTask = null;
	}
	
	private void load() {
		if (type.equals(EpisodeTask.LIST)) {
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
		} else
			getActivity().setTitle(title);
		mTask = new EpisodeTask(this, type);
		if (type.equals(EpisodeTask.LIST))
			mTask.execute(new String[] { series, Integer.toString(season) });
		else {
			String[] args = new String[params.length + 1];
			args[0] = query;
			System.arraycopy(params, 0, args, 1, params.length);
			mTask.execute(args);
		}
	}
}