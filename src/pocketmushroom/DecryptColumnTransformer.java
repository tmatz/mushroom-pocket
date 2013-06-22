package pocketmushroom;

import android.database.*;

public class DecryptColumnTransformer
extends SimpleColumnTransformer
{
	private PocketLock mPocketLock;

	public DecryptColumnTransformer(PocketLock lock)
	{
		mPocketLock = lock;
	}
	
	@Override
	public String getString(Cursor cursor, int column)
	{
		return mPocketLock.decrypt(cursor.getString(column));
	}
}
