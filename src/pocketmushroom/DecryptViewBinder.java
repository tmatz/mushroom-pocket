package pocketmushroom;

import android.database.*;
import android.view.*;
import android.widget.*;

public class DecryptViewBinder implements SimpleCursorAdapter.ViewBinder
{
	private PocketLock mPocketLock;

	public DecryptViewBinder(PocketLock lock)
	{
		mPocketLock = lock;
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
