package pocketmushroom;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.text.method.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import org.tmatz.pocketmushroom.*;

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

	private SQLiteDatabase mDatabase;
	private PocketLock mPocketLock;
	private String mPackageName;
	private int mEntryId;
	private TextView mTitle;
	private ListView mListView;
	private TextView mNotes;
	private final OnClickListener mOnClickListener = new OnClickListener()
	{
		public void onClick(View view)
		{
			String value = (String) view.getTag();
			onFieldClick(value);
		}
	};
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
		mNotes = (TextView) v.findViewById(R.id.notes);
		mNotes.setOnClickListener(mOnClickListener);
		mPackageName = args.getString(ARG_PACKAGE_NAME);
		mEntryId = args.getInt(ARG_ENTRY_ID, -1);
		if (mPackageName != null && mEntryId != -1)
		{
			mDatabase = PocketDatabase.openDatabase();
			mPocketLock = PocketLock.getPocketLock(mPackageName);
			showFieldList();
			Cursor c = mDatabase.rawQuery("select _id, title, notes from entries where _id = " + mEntryId, null);
			if (c.getCount() == 1 && c.moveToFirst())
			{
				String title = mPocketLock.decrypt(c.getString(1));
				mTitle.setText(title);
				String note = mPocketLock.decrypt(c.getString(2));
				mNotes.setText(note);
				mNotes.setTag(note);
			}
			c.close();
		}
		return v;
	}

	private void showFieldList()
	{
		Cursor c = mDatabase.rawQuery("select _id, title, value, is_hidden from fields where entry_id = " + mEntryId, null);
		FilterCursor sc = new FilterCursor(c, "title", "value", new DecryptColumnTransformer(mPocketLock));
		SimpleCursorAdapter listAdapter = new CustomCursorAdapter(
			getActivity(),
			R.layout.field_list_item,
			sc,
			new String[] {PocketDatabase.COL_TITLE, PocketDatabase.COL_VALUE},
			new int[] {R.id.title, R.id.value});
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
			TextView value = (TextView) view.findViewById(R.id.value);
			if (cursor.getInt(3) == 0)
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
