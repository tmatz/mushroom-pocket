package pocketmushroom;

import android.app.*;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;

public class MushroomActivity extends Activity
{
    public static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
    public static final String EXTRA_REPLACE_KEY = "replace_key";
	public static final String STATE_GROUP_ID = "group_id";
	public static final String STATE_ENTRY_ID = "entry_id";
	private static final int REQUEST_LOGIN = 1;
	private static final String BACKSTACK_ENTRY_LIST = "backstack_entry_list";
	private static final String BACKSTACK_ENTRY_DETAILS = "backstack_entry_details";
	

	private SQLiteDatabase mDatabase;
	private int mGroupId = -1;
	private int mEntryId = -1;
	private PocketLock mPocketLock;
	private String mCallingPackage;
	private int mBackStackEntryList;
	private int mBackStackEntryDetails;

	private class DecryptViewBinder implements SimpleCursorAdapter.ViewBinder
	{
		public boolean setViewValue(View v, Cursor c, int column)
		{
			String value = c.getString(column);
			try
			{
				String decrypted = mPocketLock.decrypt(value);
				((TextView) v).setText(decrypted);
				return true;
			}
			catch (Exception e)
			{
				return true;
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(STATE_GROUP_ID, mGroupId);
		outState.putInt(STATE_ENTRY_ID, mEntryId);
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

		if (savedInstanceState != null)
		{
			mGroupId = savedInstanceState.getInt(STATE_GROUP_ID, -1);
			mEntryId = savedInstanceState.getInt(STATE_ENTRY_ID, -1);
		}

		mCallingPackage = getCallingPackage();
		Intent intent = getIntent();
		if (mCallingPackage == null || !intent.getAction().equals(ACTION_INTERCEPT))
		{
			// it is not a mushroom intent.
			setContentView(R.layout.main_activity);
			return;
		}

		mDatabase = PocketDatabase.openDatabase();
		if (mDatabase == null)
		{
			Toast.makeText(this, R.string.cant_open_pocket, Toast.LENGTH_SHORT).show();
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
		else
		{
			initFragment();
		}
    }

	private void showLoginActivity()
	{
		Intent intent = new Intent();
		intent.setAction(LoginActivity.ACTION_LOGIN);
		intent.putExtra(LoginActivity.EXTRA_PACKAGE_NAME, mCallingPackage);
		startActivityForResult(intent, REQUEST_LOGIN);
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
	}
	
	private void initFragment()
	{
		FragmentManager fm = getFragmentManager();

		{
			ListFragment f = new ListFragment();
			Bundle args = new Bundle();
			args.putString(ListFragment.ARG_PACKAGE_NAME, mCallingPackage);
			f.setArguments(args);
			fm.beginTransaction()
				.replace(R.id.fragment, f)
				.commit();
		}

//		if (mGroupId != -1)
//		{
//			ListFragment f = new ListFragment();
//			Bundle args = new Bundle();
//			args.putString(ListFragment.ARG_PACKAGE_NAME, mCallingPackage);
//			args.putInt(ListFragment.ARG_GROUP_ID, mGroupId);
//			f.setArguments(args);
//			mBackStackEntryList = fm.beginTransaction()
//				.add(R.id.fragment, f)
//				.addToBackStack(BACKSTACK_ENTRY_LIST)
//				.commit();
//		}
//
//		if (mEntryId != -1)
//		{
//			EntryDetailFragment f = new EntryDetailFragment();
//			Bundle args = new Bundle();
//			args.putString(EntryDetailFragment.ARG_PACKAGE_NAME, mCallingPackage);
//			args.putInt(EntryDetailFragment.ARG_ENTRY_ID, mEntryId);
//			f.setArguments(args);
//			mBackStackEntryDetails = fm.beginTransaction()
//				.add(R.id.fragment, f)
//				.addToBackStack(BACKSTACK_ENTRY_DETAILS)
//				.commit();
//		}
	}

    /**
     * 元の文字を置き換える
     * @param result Replacing string
     */
    private void replace(String result)
	{
        Intent data = new Intent();
        data.putExtra(EXTRA_REPLACE_KEY, result);
        setResult(RESULT_OK, data);
        finish();
    }
}
