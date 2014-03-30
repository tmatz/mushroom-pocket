package pocketmushroom;

import android.util.Log;

public final class Logger
{
	public static int i(String tag, String message)
	{
		return Log.i(tag, message);
	}

	public static int i(String tag, Object... messages)
	{
		return Log.i(tag, build(messages));
	}

	private static String build(Object[] messages)
	{
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Object m : messages)
		{
			if (!first)
			{
				builder.append(' ');
			}
			first = false;
			builder.append(m);
		}
		return builder.toString();
	}
}
