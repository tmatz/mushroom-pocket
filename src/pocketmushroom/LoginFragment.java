package pocketmushroom;

import org.tmatz.pocketmushroom.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LoginFragment extends DialogFragment
{
	private static final String TAG = LoginFragment.class.getSimpleName();
	private static final String ARG_PACKAGE_NAME = "package_name";
	
	private static final Object sMutex = new Object();
	
	public interface DialogListener
	{
		void onOK(LoginFragment fragment);
		void onCancel(LoginFragment fragment);
	}
	
	private PocketLock mPocketLock;

	private DialogListener mDialogListener;
	private EditText mPasswordText;
	private ImageButton mClearButton;

	public static LoginFragment newInstance(String packageName)
	{
		LoginFragment f = new LoginFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PACKAGE_NAME, packageName);
		f.setArguments(args);
		return f;
	}
	
	public String getPackageName()
	{
		return getArguments().getString(ARG_PACKAGE_NAME);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Activity activity = getActivity();
		if (activity instanceof DialogListener)
		{
			mDialogListener = (DialogListener)activity;
		}
		
		if (!CreatePocketLock(getPackageName()))
		{
			cancel();
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		Logger.i(TAG, "onCreateDialog");
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.setTitle(R.string.login);
		dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup c, Bundle savedInstanceState)
	{
		Logger.i(TAG, "onCreateView");
		View content = inflater.inflate(R.layout.login_fragment, c);
		final ImageView appIconView = (ImageView) content.findViewById(R.id.applicatio_icon);
		final TextView appLabelView = (TextView) content.findViewById(R.id.application_label);
		mPasswordText = (EditText) content.findViewById(R.id.password_text);
		mClearButton = (ImageButton) content.findViewById(R.id.search_cancel);
		final Button okButton = (Button) content.findViewById(R.id.ok_btn);
		final Button cancelButton = (Button) content.findViewById(R.id.cancel_btn);

		PackageManager pm = getActivity().getPackageManager();
		try
		{
			ApplicationInfo appInfo = pm.getApplicationInfo(getPackageName(), 0);
			appIconView.setImageDrawable(pm.getApplicationIcon(appInfo));
			appLabelView.setText(pm.getApplicationLabel(appInfo));
		}
		catch (PackageManager.NameNotFoundException e)
		{}

		mPasswordText.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					if (keyCode == KeyEvent.KEYCODE_ENTER)
					{
						if (event.getAction() == KeyEvent.ACTION_DOWN)
						{
							CheckPassword();
						}
						return true;
					}
					return false;
				}
			});

		mClearButton.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View p1)
				{
					mPasswordText.setText("");
				}
			});

		okButton.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					CheckPassword();
				}
			});

		cancelButton.setOnClickListener(new OnClickListener()
			{
				public void onClick(View p1)
				{
					cancel();
				}
			});

		return content;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		getDialog().getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	}
	
	private void ok()
	{
		if (mDialogListener != null)
		{
			mDialogListener.onOK(this);
		}
		dismiss();
	}

	private void cancel()
	{
		if (mDialogListener != null)
		{
			mDialogListener.onCancel(this);
		}
		dismiss();
	}

	private boolean CreatePocketLock(String packageName)
	{
		synchronized (sMutex)
		{
			try
			{
				mPocketLock = new PocketLock(packageName);
				return true;
			}
			catch (CryptoException e)
			{
				Toast.makeText(getActivity(), e.id, Toast.LENGTH_SHORT).show();
				return false;
			}
		}
	}
	
	private void CheckPassword()
	{
		String password = mPasswordText.getText().toString();
		try
		{
			mPocketLock.unlock(password);
			PocketLock.setPocketLock(mPocketLock);
			ok();
		}
		catch (CryptoException e)
		{
			Toast.makeText(getActivity(), e.id, Toast.LENGTH_SHORT).show();
		}
	}
}
