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
implements OnListItemSelectedListener, LoginFragment.DialogListener
{
	public static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	public static final String EXTRA_REPLACE_KEY = "replace_key";

	public static final String STATE_ENTRY_ID = "entry_id";
	public static final String STATE_GROUP_ID = "group_id";
	public static final String STATE_FRAGMENT_ARGUMENTS = "state_fragment_arguments";
	public static final String TAG = MushroomActivity.class.getSimpleName();

	private static final String ARG_DIGEST = "digest";
	private static final String ARG_POSITION = "position";
	private static final String ARG_TAG = "tag";
	private static final int LOADER_GROUP_LIST = 0;

	private int mGroupId = -1;
	private int mEntryId = -1;
	private String mCallingPackage;
	private ArrayList<Bundle> mFragmentArguments = new ArrayList<Bundle>();
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
		outState.putParcelableArrayList(STATE_FRAGMENT_ARGUMENTS, mFragmentArguments);
	}

	private void restoreInstanceState(Bundle savedInstanceState)
	{
		if (savedInstanceState != null)
		{
			mGroupId = savedInstanceState.getInt(STATE_GROUP_ID, -1);
			mEntryId = savedInstanceState.getInt(STATE_ENTRY_ID, -1);
			mFragmentArguments = savedInstanceState.getParcelableArrayList(STATE_FRAGMENT_ARGUMENTS);
			if (mPagerAdapter != null)
			{
				mPagerAdapter.notifyDataSetChanged();
			}
		}
		else
		{
			SharedPreferences pref = getPreferences(MODE_PRIVATE);
			mGroupId = pref.getInt(STATE_GROUP_ID, -1);
			mEntryId = pref.getInt(STATE_ENTRY_ID, -1);

			setPage(0, EntriesFragment.TAG, EntriesFragment.newArgument(mCallingPackage, mGroupId, null), false);
			if (mEntryId != -1)
			{
				setPage(1, FieldsFragment.TAG, FieldsFragment.newArgument(mCallingPackage, mEntryId, null), false);
			}
		}

		Logger.i(TAG, "restoreInstanceState", "group", mGroupId, "entry", mEntryId);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		Logger.i(TAG, "onCreate", (savedInstanceState != null) ? "with state" : null);
		super.onCreate(savedInstanceState);

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
		
		mGroupAdapter = new ArrayAdapter<GroupInfo>(this, R.layout.support_simple_spinner_dropdown_item);
		mGroupAdapter.setNotifyOnChange(true);
		
		setContentView(R.layout.mushroom_activity);
		getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
		
		actionBar.setListNavigationCallbacks(
			mGroupAdapter,
			new OnNavigationListener()
		{
			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId)
			{
				if (mGroupId != mGroupAdapter.getItem(itemPosition).id)
				{
					mGroupId = mGroupAdapter.getItem(itemPosition).id;
					setPage(0, EntriesFragment.TAG, EntriesFragment.newArgument(mCallingPackage, mGroupId, null), false);
				}
				return true;
			}});
		
		restoreInstanceState(savedInstanceState);

		if (PocketLock.getPocketLock(mCallingPackage) != null)
		{
			onGetPocketLock();
		}
		else
		{
			showLoginDialog();
		}
	}
	
	private void onGetPocketLock()
	{
		mPager.setAdapter(mPagerAdapter);
		mPager.setCurrentItem(Math.max(0, mPagerAdapter.getCount() - 1));
		getSupportLoaderManager().initLoader(LOADER_GROUP_LIST, null, mGroupInfoLoaderCallbacks);
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

	private void showLoginDialog()
	{
		Log.i(TAG, "showLoginDialog");

		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentByTag("login") == null)
		{
			LoginFragment login = LoginFragment.newInstance(mCallingPackage);
			login.show(fm, "login");
		}
	}

	@Override
	public void onListItemSelected(Fragment f, Object data)
	{
		String tag = f.getArguments().getString(ARG_TAG);
		
		if (EntriesFragment.TAG.equals(tag))
		{
			EntryInfo item = (EntryInfo)data;
			Logger.i(TAG, "onListItemSelected", tag, item.id);
			mEntryId = item.id;
			setPage(1, FieldsFragment.TAG, FieldsFragment.newArgument(mCallingPackage, mEntryId, null), false);
		}
		else if (FieldsFragment.TAG.equals(tag))
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
	
	private void setPage(int position, String tag, Bundle arg, boolean pop)
	{
		if (arg == null)
		{
			throw new IllegalArgumentException("arg must not be null.");
		}
		
		if (mFragmentArguments.size() < position)
		{
			throw new IllegalStateException();
		}

		arg.putString(ARG_TAG, tag);
		arg.putInt(ARG_POSITION, position);

		Utilities.setDigest(arg, ARG_DIGEST);

		if (position == mFragmentArguments.size())
		{
			mFragmentArguments.add(arg);
		}
		else
		{
			if (pop)
			{
				while (position + 1 < mFragmentArguments.size())
				{
					mFragmentArguments.remove(mFragmentArguments.size() - 1);
				}
			}
			mFragmentArguments.set(position, arg);
		}

		if (mPagerAdapter != null)
		{
			mPagerAdapter.notifyDataSetChanged();
		}

		if (mPager != null)
		{
			mPager.setCurrentItem(position);
		}
	}
	
	@Override
	public void onOK(LoginFragment fragment)
	{
		Logger.i(TAG, "onOK");

		if (PocketLock.getPocketLock(mCallingPackage) == null)
		{
			throw new IllegalStateException("cant unlock database.");
		}
		else
		{
			mPager.setAdapter(mPagerAdapter);
			getSupportLoaderManager().initLoader(LOADER_GROUP_LIST, null, mGroupInfoLoaderCallbacks);
		}
	}

	@Override
	public void onCancel(LoginFragment fragment)
	{
		Logger.i(TAG, "onCancel");
		finish();
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
			return mFragmentArguments.size();
		}

		@Override
		public Fragment getItem(int position)
		{
			if (position >= mFragmentArguments.size())
			{
				return null;
			}
			else
			{
				Logger.i(TAG, "PagerAdapter.getItem");

				Fragment f = null;
				Bundle arg = mFragmentArguments.get(position);
				String tag = arg.getString(ARG_TAG);

				if (EntriesFragment.TAG.equals(tag))
				{
					f = new EntriesFragment();
				}
				else if (FieldsFragment.TAG.equals(tag))
				{
					f = new FieldsFragment();
				}
				else
				{
					return null;
				}
				
				f.setArguments(arg);
				return f;
			}
		}

		@Override
		public int getItemPosition(Object object)
		{
			Fragment f = (Fragment) object;
			Bundle arg1 = f.getArguments();
			int position = arg1.getInt(ARG_POSITION);
			Bundle arg2 = mFragmentArguments.get(position);
			String digest = arg1.getString(ARG_DIGEST);
			if (digest.equals(arg2.getString(ARG_DIGEST)))
			{
				return POSITION_UNCHANGED;
			}
			else
			{
				return POSITION_NONE;
			}
		}
	}
	
	private final LoaderCallbacks<List<GroupInfo>> mGroupInfoLoaderCallbacks = new LoaderCallbacks<List<GroupInfo>>()
	{
		@Override
		public Loader<List<GroupInfo>> onCreateLoader(int id, Bundle arg)
		{
			Logger.i(TAG, "LoaderCallbacks.onCreateLoader");
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
			Logger.i(TAG, "LoaderCallbacks.onLoadFinished");
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
