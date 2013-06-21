package pocketmushroom;

import android.content.*;
import android.util.*;

public class ScreenUnlockBroadcastReceiver extends BroadcastReceiver
{
	private static final String TAG =
		ScreenUnlockBroadcastReceiver.class.getSimpleName();

	public void onReceive(Context c, Intent i)
	{
		Log.i(TAG, "onReceie " + i.getAction());
		PocketLock.setPocketLock(null);
	}
}
