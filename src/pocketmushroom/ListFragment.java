package pocketmushroom;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.tmatz.pocketmushroom.R;

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
	private EditText mSearch;
	private ImageButton mSearchCancel;
	private ListView mListView;

	private final OnItemClickListener mOnItemClickListener = new OnItemClickListener()
	{

		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
			
			if (getActivity() instanceof OnListItemClickListener)
			{
				ItemData data = (ItemData) view.getTag();
				((OnListItemClickListener) getActivity())
					.onListItemSelected(ListFragment.this, data);
			}
		}
	};

	private final TextWatcher mSearchTextChangedListener = new TextWatcher()
	{
		@Override
		public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4)
		{
		}

		@Override
		public void onTextChanged(CharSequence p1, int p2, int p3, int p4)
		{
		}

		@Override
		public void afterTextChanged(Editable p1)
		{
			setFilterText();
		}
	};

	private final OnClickListener mSearchCancelClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View p1)
		{
			mSearch.setText("");
			setFilterText();
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
		mSearch = (EditText) view.findViewById(R.id.search);
		mSearchCancel = (ImageButton) view.findViewById(R.id.search_cancel);
		mListView = (ListView) view.findViewById(R.id.list_view);
		mListView.setOnItemClickListener(mOnItemClickListener);	
		mListView.setTextFilterEnabled(true);
		mSearch.addTextChangedListener(mSearchTextChangedListener);
		mSearchCancel.setOnClickListener(mSearchCancelClickListener);
		View searchLayout = view.findViewById(R.id.search_layout);

		if (arguments.containsKey(ARG_GROUP_ID))
		{
			showEntryList(arguments.getInt(ARG_GROUP_ID));
		}
		else
		{
			searchLayout.setVisibility(View.GONE);
			showGroupList();
		}

		return view;
	}

	private void showGroupList()
	{
		Cursor c = mDatabase.rawQuery("select _id, title from groups", null);
		setList(c);
		c.close();
	}

	private void showEntryList(int groupId)
	{
		Cursor c = mDatabase.rawQuery("select _id, title from entries where group_id = " + groupId, null);
		setList(c);
		c.close();
	}

	private void setList(Cursor c)
	{
		final ArrayList<Object[]> list = new ArrayList<Object[]>();

		if (c.moveToFirst())
		{
			do
			{
				list.add(new Object[] {c.getInt(0), mPocketLock.decrypt(c.getString(1))});
			}
			while (c.moveToNext());
		}

		Collections.sort(list, new Comparator<Object[]>()
			{
				@Override
				public int compare(Object[] p1, Object[] p2)
				{
					return ((String) p1[1]).compareTo((String) p2[1]);
				}
			});

		SimpleCursorAdapter listAdapter = new CustomCursorAdapter(
			getActivity(),
			R.layout.entry_list_item,
			null,
			new String[] {PocketDatabase.COL_TITLE},
			new int[] {R.id.title});

		listAdapter.setFilterQueryProvider(new FilterQueryProvider()
			{
				@Override
				public Cursor runQuery(CharSequence p1)
				{
					MatrixCursor c = new MatrixCursor(new String[]{PocketDatabase.COL_ID, PocketDatabase.COL_TITLE});
					if (!TextUtils.isEmpty(p1))
					{
						String query = p1.toString().toLowerCase();
						for (Object[] item: list)
						{
							String text = (String) item[1];
							if (text.toLowerCase().contains(query))
							{
								c.addRow(item);
							}
						}
					}
					else
					{
						for (Object[] item: list)
						{
							c.addRow(item);
						}
					}
					return c;
				}
			});

		mListView.setAdapter(listAdapter);
		mListView.invalidateViews();
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState)
	{
		super.onViewStateRestored(savedInstanceState);
		setFilterText();
	}

	private void setFilterText()
	{
		String filter = mSearch.getText().toString();
		mSearchCancel.setEnabled(!TextUtils.isEmpty(filter));

		SimpleCursorAdapter adapter = (SimpleCursorAdapter) mListView.getAdapter();
		adapter.getFilter().filter(filter);
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
