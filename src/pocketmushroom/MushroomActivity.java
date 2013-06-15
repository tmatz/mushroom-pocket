package pocketmushroom;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.tmatz.pocketmushroom.*;
import java.util.*;
import pocketmushroom.MushroomActivity.*;

public class MushroomActivity extends Activity
{
    private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
    private static final String REPLACE_KEY = "replace_key";
	private static final int REQUEST_LOGIN = 1;

	private ListView mEntryList;
	private SQLiteDatabase mDatabase;
	private int mGroupId = -1;
	private int mEntryId = -1;
	private PocketLock mPocketLock;
	private String mCallingPackage;

	private class DecryptViewBinder implements SimpleCursorAdapter.ViewBinder
	{
		public DecryptViewBinder()
		{
		}

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
		outState.putInt(PocketDatabase.STATE_GROUP_ID, mGroupId);
		outState.putInt(PocketDatabase.STATE_ENTRY_ID, mEntryId);
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

		if (savedInstanceState != null)
		{
			mGroupId = savedInstanceState.getInt(PocketDatabase.STATE_GROUP_ID, -1);
			mEntryId = savedInstanceState.getInt(PocketDatabase.STATE_ENTRY_ID, -1);
		}

		mCallingPackage = getCallingPackage();
		Intent intent = getIntent();
		if (mCallingPackage == null || !intent.getAction().equals(ACTION_INTERCEPT))
		{
			// it is not a mushroom intent.
			setContentView(R.layout.main_activity);
			return;
		}

		mDatabase = openDatabase();
		if (mDatabase == null)
		{
			Toast.makeText(this, R.string.cant_open_pocket, Toast.LENGTH_SHORT).show();
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		mPocketLock = PocketLock.getPocketLock(mCallingPackage);
		
		setContentView(R.layout.mushroom_activity);
		mEntryList = (ListView) findViewById(R.id.list_view);
		showEntryList();
		
		if (mPocketLock == null)
		{
			showLoginActivity();
		}
    }

	private void showLoginActivity()
	{
		Intent intent = new Intent();
		intent.setAction(LoginActivity.ACTION_LOGIN);
		intent.putExtra(LoginActivity.EXTRA_PACKAGE_NAME, mCallingPackage);
		startActivityForResult(intent, REQUEST_LOGIN);
	}

	private void showEntryList()
	{
		Cursor c = mDatabase.rawQuery("select _id, title from entries", null);
		SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(
			this,
			R.layout.entry_list_item,
			c,
			new String[] {PocketDatabase.COL_TITLE},
			new int[] {R.id.title});
		mEntryList.setAdapter(listAdapter);
		listAdapter.setViewBinder(new DecryptViewBinder());
		mEntryList.invalidateViews();
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
							mEntryList.invalidateViews();
						}
					}
				}
				break;
		}
	}

    /**
     * 元の文字を置き換える
     * @param result Replacing string
     */
    private void replace(String result)
	{
        Intent data = new Intent();
        data.putExtra(REPLACE_KEY, result);
        setResult(RESULT_OK, data);
        finish();
    }

	private SQLiteDatabase openDatabase()
	{
		SQLiteDatabase db = null;
		if (Utilities.isExternalStorageReadable())
		{
			File dbFile = new File(
				Environment.getExternalStorageDirectory()
				, PocketDatabase.DATABASE_NAME);
			if (dbFile.exists())
			{
				db = SQLiteDatabase.openDatabase(
				    dbFile.getAbsolutePath(),
					null,
					SQLiteDatabase.OPEN_READONLY);
			}
		}
		return db;
	}
}
