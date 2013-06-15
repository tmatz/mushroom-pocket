package pocketmushroom;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;
import android.content.*;

public class EntryDetailFragment extends CustomFragment
{
	private TextView mTitle;
	private ListView mFields;
	private TextView mNotes;
	
	@Override
	public View onCreateView(LayoutInflater inf, ViewGroup container, Bundle savedInstanceState)
	{
		Bundle args = getArguments(getTag());
		View v = inf.inflate(R.layout.entry_detail_fragment, null);
		mTitle = (TextView) v.findViewById(R.id.title);
		mFields = (ListView) v.findViewById(R.id.field_list);
		mNotes = (TextView) v.findViewById(R.id.notes);
		return v;
	}
}
