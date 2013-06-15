package pocketmushroom;

import android.database.sqlite.*;
import android.os.*;
import java.io.*;

public class PocketDatabase
{
	public static final String DATABASE_NAME = "wallet.db";
	public static final String TABLE_ENTRIES = "entries";
	public static final String TABLE_FIELDS = "fields";
	public static final String TABLE_GROUPS = "groups";
	public static final String TABLE_GROUPFIELDS = "groupfields";
	public static final String COL_ID = "_id";
	public static final String COL_TITLE = "title";
	public static final String COL_NOTE = "note";
	public static final String COL_ENTRY_ID = "emtry_id";
	public static final String COL_GROUP_ID = "group_id";
	public static final String COL_VALUE = "value";
	public static final String COL_GROUPFIELD_ID = "groupfield_id";
	public static final String COL_ICON = "icon";
	public static final String COL_IS_HIDDEN = "is_hodden";
	
	private static SQLiteDatabase sDatabase;
	
	public synchronized static SQLiteDatabase openDatabase()
	{
		if (sDatabase != null)
		{
			return sDatabase;
		}
		
		if (Utilities.isExternalStorageReadable())
		{
			File dbFile = new File(
				Environment.getExternalStorageDirectory()
				, PocketDatabase.DATABASE_NAME);
			if (dbFile.exists())
			{
				sDatabase = SQLiteDatabase.openDatabase(
				    dbFile.getAbsolutePath(),
					null,
					SQLiteDatabase.OPEN_READONLY);
			}
		}
		return sDatabase;
	}
}
