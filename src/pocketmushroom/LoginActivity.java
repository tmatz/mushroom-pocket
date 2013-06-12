package pocketmushroom;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.widget.*;
import org.tmatz.pocketmushroom.*;
import android.view.View.*;
import android.view.*;
import javax.crypto.*;

public class LoginActivity extends Activity
{
	public static final String EXTRA_PACKAGE_NAME = "package_name";
	public static final String EXTRA_HASH_DATA = "hash_data";
	public static final String EXTRA_PASSWORD = "password";
	public static final String ACTION_LOGIN = "org.tmatz.pocketmushroom.ACTION_LOGIN";

	private EditText mPasswordEdit;
	private HashData mHashData;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String callingPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME);
		mHashData = intent.getParcelableExtra(EXTRA_HASH_DATA);

		setContentView(R.layout.password);
		final ImageView appIconView = (ImageView) findViewById(R.id.applicatio_icon);
		final TextView appLabelView = (TextView) findViewById(R.id.application_label);
		mPasswordEdit = (EditText) findViewById(R.id.password_text);
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

		mPasswordEdit.setOnKeyListener(new OnKeyListener()
			{
				public boolean onKey(View v, int keyCode, KeyEvent event)
				{
					if (event.getAction() == event.ACTION_DOWN &&
						keyCode == event.KEYCODE_ENTER)
					{
						CheckPassword();
						return true;
					}
					return false;
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

	private void CheckPassword()
	{
		String password = mPasswordEdit.getText().toString();
		if (MushroomActivity.getDatabaseSecretKey(password, mHashData) != null)
		{
			Intent intent = new Intent();
			intent.putExtra(EXTRA_PASSWORD, password);
			setResult(RESULT_OK, intent);
			finish();
		}
	}
}
