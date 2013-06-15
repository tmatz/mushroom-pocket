package pocketmushroom;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;
import android.content.*;

public class CustomFragment extends Fragment
{
	public interface OnGetArgumentsListener
	{
		Bundle getArguments(String tag);
	}

	public Bundle getArguments(String tag)
	{
		Bundle args = getArguments();
		if (args != null)
		{
			return args;
		}

		Activity activity = getActivity();
		if (activity instanceof OnGetArgumentsListener)
		{
			args = ((OnGetArgumentsListener) activity).getArguments(tag);
		}
		if (args != null)
		{
			return args;
		}

		Intent intent = activity.getIntent();
		if (intent != null)
		{
			args = intent.getExtras();
		}

		return args;
	}
}
