package pocketmushroom;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import org.tmatz.pocketmushroom.*;

public class ListFragment extends CustomFragment
{
	public static final String ARG_PACKAGE_NAME = "package_name";
	public static final String ARG_GROUP_ID = "group_id";
	
	// Method will be ivoked when list item is clicked.
	// Activity implements this interface.
	public interface OnListItemClickListener
	{
		void onListItemSelected(ListFragment f, ItemData data);
	}
	
	public class ItemData
	{
		public int id;
	}
	
	private String mPackageName;
	private PocketLock mPocketLock;
	private SQLiteDatabase mDatabase;
	private ListView mListView;
	private final OnItemClickListener mOnItemClickListener = new OnItemClickListener()
	{

		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			if (getActivity() instanceof OnListItemClickListener)
			{
				ItemData data = (ItemData) view.getTag();
				((OnListItemClickListener) getActivity())
					.onListItemSelected(ListFragment.this, data);
			}
		}
	};
	
	@Override
	public View onCreateView(
		LayoutInflater inflater
		, ViewGroup container
		, Bundle savedInstanceState)
	{
		Bundle arguments = getCustomArguments();
		mPackageName = arguments.getString(ARG_PACKAGE_NAME);
		mDatabase = PocketDatabase.openDatabase();
		mPocketLock = PocketLock.getPocketLock(mPackageName);
		View view = inflater.inflate(R.layout.list_fragment, container, false);
		mListView = (ListView) view.findViewById(R.id.list_view);
		mListView.setOnItemClickListener(mOnItemClickListener);
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
		FilterCursor sc = new FilterCursor(c, "title", new DecryptColumnTransformer(mPocketLock));
		SimpleCursorAdapter listAdapter = new CustomCursorAdapter(
			getActivity(),
			R.layout.entry_list_item,
			sc,
			new String[] {PocketDatabase.COL_TITLE},
			new int[] {R.id.title});
		mListView.setAdapter(listAdapter);
		mListView.invalidateViews();
	}

	private void showEntryList(int groupId)
	{
		Cursor c = mDatabase.rawQuery("select _id, title from entries where group_id = " + groupId, null);
		FilterCursor sc = new FilterCursor(c, "title", new DecryptColumnTransformer(mPocketLock));
		SimpleCursorAdapter listAdapter = new CustomCursorAdapter(
			getActivity(),
			R.layout.entry_list_item,
			sc,
			new String[] {PocketDatabase.COL_TITLE},
			new int[] {R.id.title});
		mListView.setAdapter(listAdapter);
		mListView.invalidateViews();
	}

	private class CustomCursorAdapter extends SimpleCursorAdapter
	{
		public CustomCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to)
		{
			super(context, layout, c, from, to);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			super.bindView(view, context, cursor);
			ItemData data = new ItemData();
			data.id = cursor.getInt(0);
			view.setTag(data);
		}
	}
}
