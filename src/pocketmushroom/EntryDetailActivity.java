package pocketmushroom;

import android.app.*;
import android.os.*;
import org.tmatz.pocketmushroom.*;

public class EntryDetailActivity extends Activity
{
	public static final String ACTION_ENTRY_DETAIL = "org.tmatz.pocketmushroom.ACTION_ENTRY_DETAIL";
	@Override
	public void onCreate(Bundle savedIanstanceState)
	{
		super.onCreate(savedIanstanceState);
		setContentView(R.layout.entry_detail_activity);
	}
}
