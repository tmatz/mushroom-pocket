package pocketmushroom;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;
import org.tmatz.pocketmushroom.R;

public class MushroomActivity
extends FragmentActivity
implements ListFragment.OnListItemClickListener
, EntryDetailFragment.OnFieldSelectedListener
, CustomFragment.OnDetachListener
{
	public static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	public static final String EXTRA_REPLACE_KEY = "replace_key";
	public static final String STATE_GROUP_ID = "group_id";
	public static final String STATE_ENTRY_ID = "entry_id";
	private static final int REQUEST_LOGIN = 1;
	private static final String ARG_TAG = "tag";
	private static final String TAG_GROUP_LIST = "tag_group_list";
	private static final String TAG_ENTRY_LIST = "tag_entry_list";
	private static final String TAG_ENTRY_DETAILS = "tag_entry_details";
	private static final String TAG = "MushroomActivity";

	private int mGroupId = -1;
	private int mEntryId = -1;
	private PocketLock mPocketLock;
	private String mCallingPackage;
	private final StringBuilder mLogBuilder = new StringBuilder();
	private ViewPager mPager;
	private PagerAdapter mPagerAdapter;

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("onSaveInstanceState group = ")
			  .append(mGroupId)
			  .append(", entry = ")
			  .append(mEntryId)
			  .toString());

		super.onSaveInstanceState(outState);

		outState.putInt(STATE_GROUP_ID, mGroupId);
		outState.putInt(STATE_ENTRY_ID, mEntryId);
	}

	private void restoreInstanceState(Bundle savedInstanceState)
	{
		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("restoreInstanceState group = ")
			  .append(mGroupId)
			  .append(", entry = ")
			  .append(mEntryId).toString());

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
			if (mPocketLock == null)
			{
				return 0;
			}
			else if (mGroupId < 0)
			{
				return 1;
			}
			else if (mEntryId < 0)
			{
				return 2;
			}
			else
			{
				return 3;
			}
		}

		@Override
		public Fragment getItem(int position)
		{
			switch (position)
			{
				case 0:
					return createGroupListFragment();
				case 1:
					return createEntryListFragment();
				case 2:
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
			
			String tag = (arg != null) ? arg.getString(ARG_TAG) : null;
			if (tag == null)
			{
				return POSITION_NONE;
			}
			
			if (TAG_GROUP_LIST.equals(tag))
			{
				return POSITION_UNCHANGED;
			}
			else if (TAG_ENTRY_LIST.equals(tag))
			{
				if (mGroupId == arg.getInt(ListFragment.ARG_GROUP_ID))
				{
					return POSITION_UNCHANGED;
				}
			}
			else if (TAG_ENTRY_DETAILS.equals(tag))
			{
				if (mEntryId == arg.getInt(EntryDetailFragment.ARG_ENTRY_ID))
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
		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("onCreate")
			  .append((savedInstanceState != null) ? "with state" : "")
			  .toString());

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
			Log.e(TAG, "calling package unkown");
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		mPocketLock = PocketLock.getPocketLock(mCallingPackage);
		setContentView(R.layout.mushroom_activity);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		
		mPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(mPagerAdapter);
		
		if (mPocketLock == null)
		{
			showLoginActivity();
		}
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
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		pref.edit()
			.putInt(STATE_GROUP_ID, mGroupId)
			.putInt(STATE_ENTRY_ID, mEntryId)
			.commit();

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
		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("onActivityResult request = ")
			  .append(request)
			  .append(", result = ")
			  .append(result)
			  .toString());

		super.onActivityResult(request, result, data);
		switch (request)
		{
			case REQUEST_LOGIN:
				{
					if (result == RESULT_OK)
					{
						mPocketLock = PocketLock.getPocketLock(mCallingPackage);
						if (mPocketLock == null)
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
				}
				break;
		}
	}

	private Fragment createEntryDetailsFragment()
	{
		Log.i(TAG, "createEntryDetailsFragment");

		EntryDetailFragment f = new EntryDetailFragment();
		Bundle args = new Bundle();
		args.putString(ARG_TAG, TAG_ENTRY_DETAILS);
		args.putString(EntryDetailFragment.ARG_PACKAGE_NAME, mCallingPackage);
		args.putInt(EntryDetailFragment.ARG_ENTRY_ID, mEntryId);
		f.setArguments(args);
		
		return f;
	}

	private Fragment createEntryListFragment()
	{
		Log.i(TAG, "createEntryListFragment");

		Fragment f = new ListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_TAG, TAG_ENTRY_LIST);
		args.putString(ListFragment.ARG_PACKAGE_NAME, mCallingPackage);
		args.putInt(ListFragment.ARG_GROUP_ID, mGroupId);
		f.setArguments(args);
		
		return f;
	}

	private Fragment createGroupListFragment()
	{
		Log.i(TAG, "createGroupListFragment");

		Fragment f = new ListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_TAG, TAG_GROUP_LIST);
		args.putString(ListFragment.ARG_PACKAGE_NAME, mCallingPackage);
		f.setArguments(args);
		
		return f;
	}

	public void onDetachFragment(CustomFragment f)
	{
		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("onDetachFragment tag = ")
			  .append(f.getTag())
			  .toString());

		if (TAG_ENTRY_LIST.equals(f.getTag()))
		{
			mGroupId = -1;
			mEntryId = -1;
		}
		else if (TAG_ENTRY_DETAILS.equals(f.getTag()))
		{
			mEntryId = -1;
		}
	}

	public void onListItemSelected(ListFragment f, ListFragment.ItemData data)
	{
		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("onListItemSelected tag = ")
			  .append(f.getTag())
			  .append(", id = ")
			  .append(data.id)
			  .toString());

		String tag = f.getArguments().getString(ARG_TAG);
		if (TAG_GROUP_LIST.equals(tag))
		{
			mGroupId = data.id;
			mPager.setCurrentItem(1);
		}
		if (TAG_ENTRY_LIST.equals(tag))
		{
			mEntryId = data.id;
			mPager.setCurrentItem(2);
		}
		
		mPagerAdapter.notifyDataSetChanged();
	}

	public void onFieldSelected(EntryDetailFragment f, String value)
	{
		Log.i(TAG, "onFieldSelected");

		replace(value);
	}

	// long press return button. finish app.
	@Override
	public boolean onKeyLongPress(int code, KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
		{
			this.finish();
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
}
