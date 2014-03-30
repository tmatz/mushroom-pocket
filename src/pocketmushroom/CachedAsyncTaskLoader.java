package pocketmushroom;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public abstract class CachedAsyncTaskLoader<T> extends AsyncTaskLoader<T>
{
	protected T mCachedData;

	public CachedAsyncTaskLoader(Context context)
	{
		super(context);
	}

	@Override
	public void deliverResult(T data)
	{
		if (isReset())
		{
			if (mCachedData != null)
			{
				mCachedData = null;
			}
			return;
		}

		mCachedData = data;

		if (isStarted())
		{
			super.deliverResult(data);
		}
	}

	@Override
	protected void onStartLoading()
	{
		if (mCachedData != null)
		{
			deliverResult(mCachedData);
		}

		if (takeContentChanged() || mCachedData == null)
		{
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading()
	{
		cancelLoad();
		super.onStopLoading();
	}

	@Override
	protected void onReset()
	{
		cancelLoad();
		super.onReset();
	}
}
