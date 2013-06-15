package pocketmushroom;

import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;
import android.content.*;

public class ListFragment extends CustomFragment
{
	public static final String ARG_PACKAGE_NAME = "package_name";
	public static final String ARG_GROUP_ID = "group_id";
	
	private String mPackageName;
	private PocketLock mPocketLock;
	private SQLiteDatabase mDatabase;
	private ListView mListView;
	
	@Override
	public View onCreateView(
		LayoutInflater inflater
		, ViewGroup container
		, Bundle savedInstanceState)
	{
		Bundle arguments = getArguments(getTag());
		mPackageName = arguments.getString(ARG_PACKAGE_NAME);
		mDatabase = PocketDatabase.openDatabase();
		mPocketLock = PocketLock.getPocketLock(mPackageName);
		View view = inflater.inflate(R.layout.list_fragment, container, false);
		mListView = (ListView) view.findViewById(R.id.list_view);
		if (arguments.containsKey(ARG_GROUP_ID))
		{
			showEntryList(arguments.getInt(ARG_GROUP_ID));
		}
		else
		{
			showGroupList();
		}
		return view;
	}
	
	private void showGroupList()
	{
		Cursor c = mDatabase.rawQuery("select _id, title from groups", null);
		SimpleCursorAdapter listAdapter = new CustomCursorAdapter(
			getActivity(),
			R.layout.entry_list_item,
			c,
			new String[] {PocketDatabase.COL_TITLE},
			new int[] {R.id.title},
			0);
		mListView.setAdapter(listAdapter);
		listAdapter.setViewBinder(new DecryptViewBinder());
		mListView.invalidateViews();
	}

	private void showEntryList(int groupId)
	{
		Cursor c = mDatabase.rawQuery("select _id, title from entries where group_id = " + groupId, null);
		SimpleCursorAdapter listAdapter = new CustomCursorAdapter(
			getActivity(),
			R.layout.entry_list_item,
			c,
			new String[] {PocketDatabase.COL_TITLE},
			new int[] {R.id.title},
			0);
		mListView.setAdapter(listAdapter);
		listAdapter.setViewBinder(new DecryptViewBinder());
		mListView.invalidateViews();
	}
	
	private class CustomCursorAdapter extends SimpleCursorAdapter
	{
		public CustomCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags)
		{
			super(context, layout, c, from, to, flags);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			super.bindView(view, context, cursor);
		}
	}
	
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
}
