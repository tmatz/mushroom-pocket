package pocketmushroom;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;
import android.content.*;
import android.database.sqlite.*;

public class EntryDetailFragment extends CustomFragment
{
	public static final String ARG_PACKAGE_NAME = "package_name";
	public static final String ARG_ENTRY_ID = "entry_id";
	
	private SQLiteDatabase mDatabase;
	private PocketLock mPocketLock;
	private String mPackageName;
	private int mEntryId;
	private TextView mTitle;
	private ListView mFields;
	private TextView mNotes;
	
	@Override
	public View onCreateView(LayoutInflater inf, ViewGroup container, Bundle savedInstanceState)
	{
		Bundle args = getArguments(getTag());
		View v = inf.inflate(R.layout.entry_detail_fragment, container, false);
		mTitle = (TextView) v.findViewById(R.id.title);
		mFields = (ListView) v.findViewById(R.id.field_list);
		mNotes = (TextView) v.findViewById(R.id.notes);
		
		mPackageName = args.getString(ARG_PACKAGE_NAME, null);
		mEntryId = args.getInt(ARG_ENTRY_ID, -1);
		if (mPackageName != null && mEntryId != -1)
		{
			mDatabase = PocketDatabase.openDatabase();
			mPocketLock = PocketLock.getPocketLock(mPackageName);
		}
		return v;
	}
}
