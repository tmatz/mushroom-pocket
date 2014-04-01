package pocketmushroom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import org.tmatz.pocketmushroom.R;

public class MushroomActivity
extends ActionBarActivity
implements OnListItemSelectedListener
{
	public static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	public static final String EXTRA_REPLACE_KEY = "replace_key";
	public static final String STATE_GROUP_ID = "group_id";
	public static final String STATE_ENTRY_ID = "entry_id";
	private static final int REQUEST_LOGIN = 1;
	private static final String ARG_TAG = "tag";
	private static final String TAG_ENTRY_LIST = "tag_entry_list";
	private static final String TAG_ENTRY_DETAILS = "tag_entry_details";
	private static final String TAG = MushroomActivity.class.getSimpleName();

	private int mGroupId = -1;
	private int mEntryId = -1;
	private String mCallingPackage;
	private ViewPager mPager;
	private PagerAdapter mPagerAdapter;
	private ArrayAdapter<GroupInfo> mGroupAdapter;

	private void saveInstanceStateIntoPref()
	{
		int currentItem = mPager.getCurrentItem();
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		pref.edit()
			.putInt(STATE_GROUP_ID, mGroupId)
			.putInt(STATE_ENTRY_ID, (currentItem >= 1) ? mEntryId : -1)
			.commit();
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		Logger.i(TAG, "onSaveInstanceState", "group", mGroupId, "entry", mEntryId);
		super.onSaveInstanceState(outState);

		outState.putInt(STATE_GROUP_ID, mGroupId);
		outState.putInt(STATE_ENTRY_ID, mEntryId);
	}

	private void restoreInstanceState(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			mGroupId = savedInstanceState.getInt(STATE_GROUP_ID, -1);
			mEntryId = savedInstanceState.getInt(STATE_ENTRY_ID, -1);
		}
		else
		{
			SharedPreferences pref = getPreferences(MODE_PRIVATE);
			mGroupId = pref.getInt(STATE_GROUP_ID, -1);
			mEntryId = pref.getInt(STATE_ENTRY_ID, -1);
		}

		Logger.i(TAG, "restoreInstanceState", "group", mGroupId, "entry", mEntryId);
	}
	
	private class PagerAdapter extends FragmentStatePagerAdapter
	{
		public PagerAdapter(FragmentManager fm)
		{
			super(fm);
		}
		
		@Override
		public int getCount()
		{
			if (PocketLock.getPocketLock(mCallingPackage) == null)
			{
				return 0;
			}
			else if (mEntryId < 0)
			{
				return 1;
			}
			else
			{
				return 2;
			}
		}

		@Override
		public Fragment getItem(int position)
		{
			switch (position)
			{
				case 0:
					return createEntryListFragment();
				case 1:
					return createEntryDetailsFragment();
				default:
					return null;
			}
		}

		@Override
		public int getItemPosition(Object object)
		{
			Fragment f = (Fragment) object;
			Bundle arg = f.getArguments();
			if (arg == null || arg.getString(ARG_TAG) == null)
			{
				throw new IllegalStateException("fragment does not have argument.");
			}

			String tag = arg.getString(ARG_TAG);
			if (TAG_ENTRY_LIST.equals(tag))
			{
				if (mGroupId == arg.getInt(EntriesFragment.ARG_GROUP_ID))
				{
					return POSITION_UNCHANGED;
				}
			}
			else if (TAG_ENTRY_DETAILS.equals(tag))
			{
				if (mEntryId == arg.getInt(FieldsFragment.ARG_ENTRY_ID))
				{
					return POSITION_UNCHANGED;
				}
			}
			
			return POSITION_NONE;
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Logger.i(TAG, "onCreate", (savedInstanceState != null) ? "with state" : null);
		super.onCreate(savedInstanceState);
		restoreInstanceState(savedInstanceState);

		if (!PocketDatabase.isReadable())
		{
			Log.e(TAG, "pocket database is unreadable");
			Toast.makeText(this, R.string.cant_open_pocket, Toast.LENGTH_SHORT).show();
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		mCallingPackage = getCallingPackage();
		
		if (mCallingPackage == null)
		{
			Intent intent = getIntent();
			if (ACTION_INTERCEPT.equals(intent.getAction()))
			{
				Log.w(TAG, "calling package unkown");
				{
					setResult(RESULT_CANCELED);
					finish();
					return;
				}
			}
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		mGroupAdapter = new ArrayAdapter<GroupInfo>(this, android.R.layout.simple_spinner_dropdown_item);
		mGroupAdapter.setNotifyOnChange(true);
		
		setContentView(R.layout.mushroom_activity);
		getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mPagerAdapter);
		
		mPager.setCurrentItem(Math.max(0, mPagerAdapter.getCount() - 1));
		
		actionBar.setListNavigationCallbacks(
			mGroupAdapter,
			new OnNavigationListener()
		{
			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId)
			{
				mGroupId = mGroupAdapter.getItem(itemPosition).id;
				mPagerAdapter.notifyDataSetChanged();
				return true;
			}});
		
		getSupportLoaderManager().initLoader(0, null, mGroupInfoLoaderCallbacks);
		
		if (PocketLock.getPocketLock(mCallingPackage) == null)
		{
			showLoginActivity();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_mushroom_activity, menu);
		return true;
	}
	
	@Override
	public void onResume()
	{
		Log.i(TAG, "onResume");

		super.onResume();
		PocketLock.resetTimer();
	}
	
	@Override
	public void onPause()
	{
		Log.i(TAG, "onPause");

		super.onPause();		
		saveInstanceStateIntoPref();
		PocketLock.startTimer();
	}

	private void showLoginActivity()
	{
		Log.i(TAG, "showLoginActivity");

		Intent intent = new Intent();
		intent.setAction(LoginActivity.ACTION_LOGIN);
		intent.putExtra(LoginActivity.EXTRA_PACKAGE_NAME, mCallingPackage);
		startActivityForResult(intent, REQUEST_LOGIN);
	}

	@Override
	public void onActivityResult(int request, int result, Intent data)
	{
		Logger.i(TAG, "onActivityResult", request, result);

		super.onActivityResult(request, result, data);
		switch (request)
		{
			case REQUEST_LOGIN:
				if (result == RESULT_OK)
				{
					if (PocketLock.getPocketLock(mCallingPackage) == null)
					{
						showLoginActivity();
					}
					else
					{
						mPagerAdapter.notifyDataSetChanged();
					}
				}
				else
				{
					finish();
				}
				break;
		}
	}

	private Fragment createEntryDetailsFragment()
	{
		Log.i(TAG, "createEntryDetailsFragment");

		Bundle args = new Bundle();
		args.putString(ARG_TAG, TAG_ENTRY_DETAILS);

		return FieldsFragment.createInstance(mCallingPackage, mEntryId, args);
	}

	private Fragment createEntryListFragment()
	{
		Log.i(TAG, "createEntryListFragment");

		Bundle args = new Bundle();
		args.putString(ARG_TAG, TAG_ENTRY_LIST);

		return EntriesFragment.createInstance(mCallingPackage, mGroupId, args);
	}

	@Override
	public void onListItemSelected(Fragment f, Object data)
	{
		String tag = f.getArguments().getString(ARG_TAG);
		
		if (TAG_ENTRY_LIST.equals(tag))
		{
			EntryInfo item = (EntryInfo)data;
			Logger.i(TAG, "onListItemSelected", tag, item.id);
			mEntryId = item.id;
			mPagerAdapter.notifyDataSetChanged();
			mPager.setCurrentItem(1);
		}
		else if (TAG_ENTRY_DETAILS.equals(tag))
		{
			FieldInfo item = (FieldInfo)data;
			Logger.i(TAG, "onListItemSelected", tag, item.id);
			replace(item.value);
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			if (mPager.getCurrentItem() > 0)
			{
				mPager.setCurrentItem(mPager.getCurrentItem() - 1);
				return true;
			}
		}

		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int code, KeyEvent event)
	{
		// long press return button. finish app.
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			finish();
			return true;
		}

		return super.onKeyLongPress(code, event);
	}

	// send back result string to calling package.
	private void replace(String result)
	{
		Log.i(TAG, "replace");

		Intent data = new Intent();
		data.putExtra(EXTRA_REPLACE_KEY, result);
		setResult(RESULT_OK, data);
		finish();
	}

	private final LoaderCallbacks<List<GroupInfo>> mGroupInfoLoaderCallbacks = new LoaderCallbacks<List<GroupInfo>>()
	{
		@Override
		public Loader<List<GroupInfo>> onCreateLoader(int id, Bundle arg)
		{
			Logger.i(TAG, "onCreateLoader");
			switch (id)
			{
			case 0:
				return new GroupInfoLoader(MushroomActivity.this, mCallingPackage);
			}
			return null;
		}

		@Override
		public void onLoadFinished(Loader<List<GroupInfo>> loader, List<GroupInfo> data)
		{
			Logger.i(TAG, "onLoadFinished");
			mGroupAdapter.clear();
			if (data != null)
			{
				for (GroupInfo item: data)
				{
					mGroupAdapter.add(item);
				}
			}
			
			int count = mGroupAdapter.getCount();
			for (int i = 0; i < count; ++i)
			{
				if (mGroupAdapter.getItem(i).id == mGroupId)
				{
					getSupportActionBar().setSelectedNavigationItem(i);
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<List<GroupInfo>> loader)
		{
			Logger.i(TAG, "onLoaderReset");
			mGroupAdapter.clear();
		}
	};
	
	private static class GroupInfoLoader extends CachedAsyncTaskLoader<List<GroupInfo>>
	{
		private String mPackageName;

		public GroupInfoLoader(Context context, String packageName)
		{
			super(context);
			mPackageName = packageName;
		}
		
		@Override
		public List<GroupInfo> loadInBackground()
		{
			PocketLock pocketLock = PocketLock.getPocketLock(mPackageName);
			SQLiteDatabase database = PocketDatabase.openDatabase();
			if (pocketLock == null || database == null)
			{
				return null;
			}
			
			ArrayList<GroupInfo> items = new ArrayList<GroupInfo>();
			
			Cursor c = database.rawQuery("select _id, title from groups", null);
			while (c.moveToNext())
			{
				items.add(new GroupInfo(c.getInt(0), pocketLock.decrypt(c.getString(1))));
			}
			c.close();

			Collections.sort(items);
			
			items.add(0, new GroupInfo(-1, getContext().getString(R.string.all_entries)));
			return items;
		}
	}
}
