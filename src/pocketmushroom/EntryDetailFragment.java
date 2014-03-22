package pocketmushroom;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.tmatz.pocketmushroom.R;

public class EntryDetailFragment extends CustomFragment
{
	public static final String ARG_PACKAGE_NAME = "package_name";
	public static final String ARG_ENTRY_ID = "entry_id";

	// Method will be invoked when field or note is clicked.
	// Activity implements this interface.
	public interface OnFieldSelectedListener
	{
		void onFieldSelected(EntryDetailFragment f, String value);
	}
	
	class RowData
	{
		public String label;
		public String value;
		public int hide;
				
		public void Add(MatrixCursor.RowBuilder builder)
		{
			builder.add(0);
			builder.add(label);
			builder.add(value);
			builder.add(hide);
		}
	}

	private SQLiteDatabase mDatabase;
	private PocketLock mPocketLock;
	private String mPackageName;
	private int mEntryId;
	private TextView mTitle;
	private ListView mListView;

	private final OnItemClickListener mOnItemClickListener = new OnItemClickListener()
	{
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			TextView value = (TextView) view.findViewById(R.id.value);
			onFieldClick(value.getText().toString());
		}
	};
	private final TransformationMethod mPasswordTransformationMethod = new PasswordTransformationMethod();
	private final TransformationMethod mNormalTransformationMethod = new HideReturnsTransformationMethod();

	@Override
	public View onCreateView(LayoutInflater inf, ViewGroup container, Bundle savedInstanceState)
	{
		Bundle args = getCustomArguments();
		View v = inf.inflate(R.layout.entry_detail_fragment, container, false);
		mTitle = (TextView) v.findViewById(R.id.title);
		mListView = (ListView) v.findViewById(R.id.field_list);
		mListView.setOnItemClickListener(mOnItemClickListener);
		mPackageName = args.getString(ARG_PACKAGE_NAME);
		mEntryId = args.getInt(ARG_ENTRY_ID, -1);
		if (mPackageName != null && mEntryId != -1)
		{
			mDatabase = PocketDatabase.openDatabase();
			mPocketLock = PocketLock.getPocketLock(mPackageName);
			showFieldList();
			Cursor c = mDatabase.rawQuery("select _id, title from entries where _id = " + mEntryId, null);
			if (c.getCount() == 1 && c.moveToFirst())
			{
				String title = mPocketLock.decrypt(c.getString(1));
				mTitle.setText(title);
			}
			c.close();
		}
		return v;
	}

	private void showFieldList()
	{
		List<RowData> rowDatas = new ArrayList<RowData>();
		
		{
			Cursor c = mDatabase.rawQuery("select _id, title, value, is_hidden from fields where entry_id = " + mEntryId, null);
			while (c.moveToNext())
			{
				RowData data = new RowData();
				data.label = mPocketLock.decrypt(c.getString(1));
				data.value = mPocketLock.decrypt(c.getString(2));
				data.hide = c.getInt(3);

				if (data.value != null && !data.value.isEmpty())

				{
					rowDatas.add(data);
				}
			}
			c.close();
		}

		Collections.sort(rowDatas, new Comparator<RowData>()
		{
			@Override
			public int compare(EntryDetailFragment.RowData p1, EntryDetailFragment.RowData p2)
			{
				return p1.label.compareTo(p2.label);
			}
		});
		
		{
			Cursor c = mDatabase.rawQuery("select _id, notes from entries where _id = " + mEntryId, null);
			if (c.moveToFirst())
			{
				RowData data = new RowData();
				data.label = getResources().getString(R.string.notes);
				data.value = mPocketLock.decrypt(c.getString(1));
				data.hide = 0;

				if (data.value != null && !data.value.isEmpty())
				{
					rowDatas.add(data);
				}
			}
			c.close();
		}

		MatrixCursor cursor = new MatrixCursor(
			new String[] {
				PocketDatabase.COL_ID,
				PocketDatabase.COL_TITLE,
				PocketDatabase.COL_VALUE,
				PocketDatabase.COL_IS_HIDDEN,
			});

		for (RowData data: rowDatas)
		{
			data.Add(cursor.newRow());
		}
				
		SimpleCursorAdapter listAdapter = new CustomCursorAdapter(
			getActivity(),
			R.layout.field_list_item,
			cursor,
			new String[] {PocketDatabase.COL_TITLE, PocketDatabase.COL_VALUE},
			new int[] {R.id.title, R.id.value});
		mListView.setAdapter(listAdapter);
		mListView.invalidateViews();
	}

	private class CustomCursorAdapter extends SimpleCursorAdapter
	{
		private int mColHidden;
		
		public CustomCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to)
		{
			super(context, layout, c, from, to);
			mColHidden = c.getColumnIndex(PocketDatabase.COL_IS_HIDDEN);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			super.bindView(view, context, cursor);
			TextView value = (TextView) view.findViewById(R.id.value);
			if (cursor.getInt(mColHidden) == 0)
			{
				value.setTransformationMethod(mNormalTransformationMethod);
			}
			else
			{
				value.setTransformationMethod(mPasswordTransformationMethod);
			}
		}
	}
	
	private void onFieldClick(String value)
	{
		if (getActivity() instanceof OnFieldSelectedListener)
		{
			((OnFieldSelectedListener) getActivity())
				.onFieldSelected(EntryDetailFragment.this, value);
		}
	}
}
