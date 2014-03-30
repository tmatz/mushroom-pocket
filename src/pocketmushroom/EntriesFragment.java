package pocketmushroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.tmatz.pocketmushroom.R;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class EntriesFragment extends CustomListFragment
	implements
		LoaderCallbacks<List<EntryInfo>>
{
	public static final String ARG_PACKAGE_NAME = "package_name";
	public static final String ARG_GROUP_ID = "group_id";

	private static final String TAG = EntriesFragment.class.getSimpleName();

	private ArrayAdapter<EntryInfo> mAdapter;

	public static EntriesFragment createInstance(
		String packageName,
		int groupId,
		Bundle extra)
	{
		if (extra == null)
		{
			extra = new Bundle();
		}

		extra.putString(ARG_PACKAGE_NAME, packageName);
		extra.putInt(ARG_GROUP_ID, groupId);

		EntriesFragment f = new EntriesFragment();
		f.setArguments(extra);

		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Logger.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		Logger.i(TAG, "onViewCreated");
		super.onViewCreated(view, savedInstanceState);

		mAdapter = new ArrayAdapter<EntryInfo>(
			getActivity(),
			android.R.layout.simple_list_item_1);
		mAdapter.setNotifyOnChange(true);

		setEmptyText(getResources().getString(R.string.no_entries));

		LoaderManager manager = getLoaderManager();
		manager.initLoader(0, getArguments(), this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		Logger.i(TAG, "onCreateOptionsMenu");
		inflater.inflate(R.menu.menu_entries_fragment, menu);

		setupSearchView(menu);

		MenuItem menuItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) MenuItemCompat
			.getActionView(menuItem);

		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
		{
			@Override
			public boolean onQueryTextSubmit(String query)
			{
				mAdapter.getFilter().filter(query);
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText)
			{
				mAdapter.getFilter().filter(newText);
				return true;
			}
		});
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		Logger.i(TAG, "onListItemClick");
		super.onListItemClick(l, v, position, id);

		if (getActivity() instanceof OnListItemSelectedListener)
		{
			EntryInfo data = mAdapter.getItem(position);
			((OnListItemSelectedListener) getActivity()).onListItemSelected(
				this,
				data);
		}
	}

	private static class ItemsLoader
		extends
			CachedAsyncTaskLoader<List<EntryInfo>>
	{
		private Bundle mArgs;

		public ItemsLoader(Context context, Bundle args)
		{
			super(context);
			mArgs = args;
		}

		@Override
		public List<EntryInfo> loadInBackground()
		{
			String packageName = mArgs.getString(ARG_PACKAGE_NAME);
			PocketLock pocketLock = PocketLock.getPocketLock(packageName);

			if (pocketLock == null)
			{
				return null;
			}

			SQLiteDatabase database = PocketDatabase.openDatabase();
			List<EntryInfo> items = new ArrayList<EntryInfo>();

			int groupId = mArgs.getInt(ARG_GROUP_ID);
			Cursor c;
			if (groupId == -1)
			{
				c = database.rawQuery("select _id, title from entries", null);
			}
			else
			{
				c = database.rawQuery(
					"select _id, title from entries where group_id = "
							+ groupId,
					null);
			}

			if (c.moveToFirst())
			{
				do
				{
					items.add(new EntryInfo(c.getInt(0), pocketLock.decrypt(c
						.getString(1))));
				} while (c.moveToNext());
			}

			c.close();

			Collections.sort(items);

			return items;
		}
	}

	@Override
	public Loader<List<EntryInfo>> onCreateLoader(int id, Bundle args)
	{
		Logger.i(TAG, "onCreateLoader");
		Loader<List<EntryInfo>> loader = new ItemsLoader(getActivity(), args);
		loader.forceLoad();
		return loader;
	}

	@Override
	public void onLoadFinished(
		Loader<List<EntryInfo>> loader,
		List<EntryInfo> items)
	{
		Logger.i(TAG, "onLoadFinished");
		for (EntryInfo i : items)
		{
			mAdapter.add(i);
		}
		setListAdapter(mAdapter);
	}

	@Override
	public void onLoaderReset(Loader<List<EntryInfo>> loader)
	{
		Logger.i(TAG, "onLoadFinished");
		setListAdapter(mAdapter);
	}
}