package pocketmushroom;
import android.database.*;

public class SimpleColumnTransformer
implements ColumnTransformer
{
	public short getShort(Cursor cursor, int column)
	{
		return cursor.getShort(column);
	}

	public String getString(Cursor cursor, int column)
	{
		return cursor.getString(column);
	}

	public int getInt(Cursor cursor, int column)
	{
		return cursor.getInt(column);
	}

	public long getLong(Cursor cursor, int column)
	{
		return cursor.getLong(column);
	}

	public float getFloat(Cursor cursor, int column)
	{
		return cursor.getFloat(column);
	}

	public double getDouble(Cursor cursor, int column)
	{
		return cursor.getDouble(column);
	}

	public boolean isNull(Cursor cursor, int column)
	{
		return cursor.isNull(column);
	}
}
