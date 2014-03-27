package pocketmushroom;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;
import android.widget.RadioGroup.LayoutParams;

public class LoginActivity extends Activity
{
	public static final String ACTION_LOGIN = "org.tmatz.pocketmushroom.ACTION_LOGIN";
	public static final String EXTRA_PACKAGE_NAME = "calling_package_name";

	private static final Object sMutex = new Object();
	
	private PocketLock mPocketLock;
	private EditText mPasswordText;
	private ImageButton mClearButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String callingPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME);

		if (callingPackage == null)
		{
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		if (!CreatePocketLock(callingPackage))
		{
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		setContentView(R.layout.login_activity);
		getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

		final ImageView appIconView = (ImageView) findViewById(R.id.applicatio_icon);
		final TextView appLabelView = (TextView) findViewById(R.id.application_label);
		mPasswordText = (EditText) findViewById(R.id.password_text);
		mClearButton = (ImageButton) findViewById(R.id.search_cancel);
		final Button okButton = (Button) findViewById(R.id.ok_btn);
		final Button cancelButton = (Button) findViewById(R.id.cancel_btn);
		PackageManager pm = getPackageManager();
		try
		{
			ApplicationInfo appInfo = pm.getApplicationInfo(callingPackage, 0);
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
					setResult(RESULT_CANCELED);
					finish();
				}
			});
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
				Toast.makeText(this, e.id, Toast.LENGTH_SHORT).show();
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
			setResult(RESULT_OK);
			finish();
		}
		catch (CryptoException e)
		{
			Toast.makeText(this, e.id, Toast.LENGTH_SHORT).show();
		}
	}
}
