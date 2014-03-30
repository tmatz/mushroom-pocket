package pocketmushroom;

import org.tmatz.pocketmushroom.R;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class CustomListFragment extends ListFragment
{
	private static final String TAG = CustomListFragment.class.getSimpleName();
	private static final String KEY_QUERY = "query";

	private CharSequence mQueryText;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Logger.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		// setHasOptionsMenu(true);
		if (savedInstanceState != null)
		{
			mQueryText = savedInstanceState.getCharSequence(KEY_QUERY);
			Logger.i(TAG, "restore Qeury", mQueryText);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		Logger.i(TAG, "onViewCreated");
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		Logger.i(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState);
		if (!TextUtils.isEmpty(mQueryText))
		{
			outState.putCharSequence(KEY_QUERY, mQueryText);
		}
	}

	protected void setupSearchView(Menu menu)
	{
		final MenuItem menuItem = menu.findItem(R.id.action_search);
		final SearchView searchView = (SearchView) MenuItemCompat
			.getActionView(menuItem);

		searchView
			.setOnQueryTextFocusChangeListener(new OnFocusChangeListener()
			{
				@Override
				public void onFocusChange(View v, boolean hasFocus)
				{
					Logger.i(TAG, "SearchView.onFocusChange", hasFocus);

					if (hasFocus)
					{
						if (!TextUtils.isEmpty(mQueryText))
						{
							Logger.i(TAG, "restore QueryText", mQueryText);
							searchView.setQuery(mQueryText, false);
						}
					}
					else
					{
						mQueryText = searchView.getQuery();
						Logger.i(TAG, "save QueryText ", mQueryText);
						MenuItemCompat.collapseActionView(menuItem);
					}
				}
			});
	}
	
	@Override
	public void setListAdapter(ListAdapter adapter)
	{
		super.setListAdapter(adapter);
		if (adapter instanceof ArrayAdapter)
		{
			((ArrayAdapter<?>)adapter).getFilter().filter(mQueryText);			
		}
	}
}