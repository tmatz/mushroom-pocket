package pocketmushroom;

import android.database.*;
import android.os.*;
import android.util.*;

import java.util.*;

public class FilterCursor
extends AbstractCursor
{
	private class ValueIndexPair extends Pair<String, Integer>
	{
		public ValueIndexPair(String first, Integer second) {
			super(first, second);
		}
	}

	private class PairComparator<T extends Comparable<T>>
	implements Comparator<Pair<T, ?>>
	{
		@Override
		public int compare(Pair<T, ?> lhs, Pair<T, ?> rhs)
		{
			return lhs.first.compareTo(rhs.first);
		}
	}

	private Cursor mCursor;
	private int[] mSortedOrder;
	private int mSortColumn;
	private ColumnTransformer mColumnTransformer;

	private DataSetObserver mDataSetObserver = new DataSetObserver()
	{
		@Override
		public void onChanged()
		{
			mPos = -1;
			mSortedOrder = null;
		}

		@Override
		public void onInvalidated()
		{
			mPos = -1;
			mSortedOrder = null;
		}
	};

	public FilterCursor(Cursor c, String sort, ColumnTransformer trans)
	{
		mCursor = c;
		mSortedOrder = null;
		mSortColumn = c.getColumnIndexOrThrow(sort);
		if (trans != null)
		{
			mColumnTransformer = trans;
		}
		else
		{
			mColumnTransformer = new SimpleColumnTransformer();
		}
		c.registerDataSetObserver(mDataSetObserver);
	}
	
	private void sort()
	{
		int count = mCursor.getCount();
		if (count == 0)
		{
			mSortedOrder = new int[0];
			return;
		}

		ValueIndexPair[] values = new ValueIndexPair[count];
		int numValues = 0;
		mCursor.moveToFirst();

		for (int i = 0; i < values.length; ++i)
		{
			String value = mColumnTransformer.getString(mCursor, mSortColumn);
			values[numValues++] = new ValueIndexPair(value, i);
			mCursor.moveToNext();
		}
		Arrays.sort(values, 0, numValues, new PairComparator<String>());

		mSortedOrder = new int[numValues];
		for (int i = 0; i < numValues; ++i)
		{
			mSortedOrder[i] = ((Pair<?, Integer>) values[i]).second;
		}
	}

	@Override
	public boolean onMove(int oldPosition, int newPosition)
	{
		if (mSortedOrder == null)
		{
			sort();
		}

		if (newPosition < mSortedOrder.length)
		{
			return mCursor.moveToPosition(mSortedOrder[newPosition]);
		}
		else
		{
			return false;
		}
	}

	public String[] getColumnNames()
	{
		return mCursor.getColumnNames();
	}

	public String getString(int p1)
	{
		return mColumnTransformer.getString(mCursor, p1);
	}

	public short getShort(int p1)
	{
		return mColumnTransformer.getShort(mCursor, p1);
	}

	public int getInt(int p1)
	{
		return mColumnTransformer.getInt(mCursor, p1);
	}

	public long getLong(int p1)
	{
		return mColumnTransformer.getLong(mCursor, p1);
	}

	public float getFloat(int p1)
	{
		return mColumnTransformer.getFloat(mCursor, p1);
	}

	public double getDouble(int p1)
	{
		return mColumnTransformer.getDouble(mCursor, p1);
	}

	public boolean isNull(int p1)
	{
		return mColumnTransformer.isNull(mCursor, p1);
	}

	public int getCount()
	{
		if (mSortedOrder == null)
		{
			sort();
		}
		return mSortedOrder.length;
	}

	public int getColumnIndex(String p1)
	{
		return mCursor.getColumnIndex(p1);
	}

	public int getColumnIndexOrThrow(String p1) throws IllegalArgumentException
	{
		return mCursor.getColumnIndexOrThrow(p1);
	}

	public String getColumnName(int p1)
	{
		return mCursor.getColumnName(p1);
	}

	public int getColumnCount()
	{
		return mCursor.getColumnCount();
	}

	public byte[] getBlob(int p1)
	{
		return mCursor.getBlob(p1);
	}

	public void copyStringToBuffer(int p1, CharArrayBuffer p2)
	{
		mCursor.copyStringToBuffer(p1, p2);
	}

	public void deactivate()
	{
		mCursor.deactivate();
		super.deactivate();
	}

	public boolean requery()
	{
		super.requery();
		return mCursor.requery();
	}

	public void close()
	{
		super.close();
		mCursor.close();
	}

	public boolean getWantsAllOnMoveCalls()
	{
		return mCursor.getWantsAllOnMoveCalls();
	}

	public Bundle getExtras()
	{
		return mCursor.getExtras();
	}

	public Bundle respond(Bundle p1)
	{
		return mCursor.respond(p1);
	}
}
