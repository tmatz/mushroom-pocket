package pocketmushroom;

import android.content.*;
import android.os.*;
import android.support.v4.app.*;

public class CustomFragment extends Fragment
{
	// Method will be invoked whcn fragment requires arguments.
	// Activity implements this interface.
	public interface OnGetArgumentsListener
	{
		Bundle getCustomArguments(CustomFragment f);
	}
	
	// Method will be invoked when fragment is detached.
	// Activity implements this interface.
	public interface OnDetachListener
	{
		void onDetachFragment(CustomFragment f);
	}

	public Bundle getCustomArguments()
	{
		Bundle args = getArguments();
		if (args != null)
		{
			return args;
		}

		FragmentActivity activity = getActivity();
		if (activity instanceof OnGetArgumentsListener)
		{
			args = ((OnGetArgumentsListener) activity)
				.getCustomArguments(this);
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
	
	@Override
	public void onDetach()
	{
		if (getActivity() instanceof OnDetachListener)
		{
			((OnDetachListener) getActivity())
				.onDetachFragment(this);
		}
		super.onDetach();
	}
}
