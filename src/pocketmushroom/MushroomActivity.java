package pocketmushroom;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;

public class MushroomActivity extends FragmentActivity
implements ListFragment.OnListItemClickListener
, EntryDetailFragment.OnFieldSelectedListener
, CustomFragment.OnDetachListener
{

	public void onDetach(CustomFragment f)
	{
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

    public static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
    public static final String EXTRA_REPLACE_KEY = "replace_key";
	public static final String STATE_GROUP_ID = "group_id";
	public static final String STATE_ENTRY_ID = "entry_id";
	private static final int REQUEST_LOGIN = 1;
	private static final String TAG_GROUP_LIST = "tag_group_list";
	private static final String TAG_ENTRY_LIST = "tag_entry_list";
	private static final String TAG_ENTRY_DETAILS = "tag_entry_details";
	private static final String TAG = MushroomActivity.class.getSimpleName();

	private int mGroupId = -1;
	private int mEntryId = -1;
	private PocketLock mPocketLock;
	private String mCallingPackage;
	private final StringBuilder mLogBuilder = new StringBuilder();

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		outState.putInt(STATE_GROUP_ID, mGroupId);
		outState.putInt(STATE_ENTRY_ID, mEntryId);

		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("onSaveInstanceState group = ")
			  .append(mGroupId)
			  .append(", entry = ")
			  .append(mEntryId)
			  .toString());
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

		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("restoreInstanceState group = ")
			  .append(mGroupId)
			  .append(", entry = ")
			  .append(mEntryId).toString());
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

		restoreInstanceState(savedInstanceState);

		if (!PocketDatabase.isReadable())
		{
			Toast.makeText(this, R.string.cant_open_pocket, Toast.LENGTH_SHORT).show();
			Log.e(TAG, "pocket database is unreadable");
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

		if (mPocketLock == null)
		{
			showLoginActivity();
		}
		else if (savedInstanceState == null)
		{
			initFragment();
		}

		Log.i(TAG, "onCreate");
    }

	@Override
	public void onStop()
	{
		SharedPreferences pref = getPreferences(MODE_PRIVATE);
		pref.edit()
			.putInt(STATE_GROUP_ID, mGroupId)
			.putInt(STATE_ENTRY_ID, mEntryId)
			.commit();

		super.onStop();

		Log.i(TAG, "onStop");
	}

	private void showLoginActivity()
	{
		Intent intent = new Intent();
		intent.setAction(LoginActivity.ACTION_LOGIN);
		intent.putExtra(LoginActivity.EXTRA_PACKAGE_NAME, mCallingPackage);
		startActivityForResult(intent, REQUEST_LOGIN);

		Log.i(TAG, "showLoginActivity");
	}

	@Override
	public void onActivityResult(int request, int result, Intent data)
	{
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
							initFragment();
						}
					}
				}
				break;
		}

		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			  .append("onActivityResult request = ")
			  .append(request)
			  .append(", result = ")
			  .append(result)
			  .toString());
	}

	private void initFragment()
	{
		addGroupListFragment();

		if (mGroupId != -1)
		{
			replaceEntryListFragment();
		}

		if (mEntryId != -1)
		{
			replaceEntryDetailsFragment();
		}

		Log.i(TAG, "initFragment");
	}

	private void replaceEntryDetailsFragment()
	{
		FragmentManager fm = getSupportFragmentManager();
		EntryDetailFragment f = new EntryDetailFragment();
		Bundle args = new Bundle();
		args.putString(EntryDetailFragment.ARG_PACKAGE_NAME, mCallingPackage);
		args.putInt(EntryDetailFragment.ARG_ENTRY_ID, mEntryId);
		f.setArguments(args);
		fm.beginTransaction()
			.replace(R.id.fragment, f, TAG_ENTRY_DETAILS)
			.addToBackStack(TAG_ENTRY_DETAILS)
			.commit();

		Log.i(TAG, "replaceEntryDetailsFragment");
	}

	private void replaceEntryListFragment()
	{
		FragmentManager fm = getSupportFragmentManager();
		ListFragment f = new ListFragment();
		Bundle args = new Bundle();
		args.putString(ListFragment.ARG_PACKAGE_NAME, mCallingPackage);
		args.putInt(ListFragment.ARG_GROUP_ID, mGroupId);
		f.setArguments(args);
		fm.beginTransaction()
			.replace(R.id.fragment, f, TAG_ENTRY_LIST)
			.addToBackStack(TAG_ENTRY_LIST)
			.commit();

		Log.i(TAG, "replaceEntryListFragment");
	}

	private void addGroupListFragment()
	{
		FragmentManager fm = getSupportFragmentManager();
		ListFragment f = new ListFragment();
		Bundle args = new Bundle();
		args.putString(ListFragment.ARG_PACKAGE_NAME, mCallingPackage);
		f.setArguments(args);
		fm.beginTransaction()
			.replace(R.id.fragment, f, TAG_GROUP_LIST)
			.commit();

		Log.i(TAG, "addGroupListFragment");
	}

	public void onListItemSelected(ListFragment f, ListFragment.ItemData data)
	{
		if (TAG_GROUP_LIST.equals(f.getTag()))
		{
			mGroupId = data.id;
			replaceEntryListFragment();
		}
		if (TAG_ENTRY_LIST.equals(f.getTag()))
		{
			mEntryId = data.id;
			replaceEntryDetailsFragment();
		}
		
		mLogBuilder.setLength(0);
		Log.i(TAG, mLogBuilder
			.append("onListItemSelected tag = ")
			.append(f.getTag())
			.append(", id = ")
			.append(data.id)
			.toString());
	}

	public void onFieldSelected(EntryDetailFragment f, String value)
	{
		replace(value);
		
		Log.i(TAG, "onFieldSelected");
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

    // send result string to calling package.
    private void replace(String result)
	{
        Intent data = new Intent();
        data.putExtra(EXTRA_REPLACE_KEY, result);
        setResult(RESULT_OK, data);
        finish();
		
		Log.i(TAG, "replace");
    }
}
