package pocketmushroom;

import android.database.*;

public interface ColumnTransformer
{
	short getShort(Cursor cursor, int column);
	String getString(Cursor cursor, int column);
	int getInt(Cursor cursor, int column);
	long getLong(Cursor cursor, int column);
	float getFloat(Cursor cursor, int column);
	double getDouble(Cursor cursor, int column);
	boolean isNull(Cursor cursor, int column);
}
