package pocketmushroom;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.database.sqlite.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.tmatz.pocketmushroom.*;

public class MushroomActivity extends Activity implements OnClickListener
{
    private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
    private static final String REPLACE_KEY = "replace_key";
    private static final String PROVIDER = "BC";
    private static final int SALT_LENGTH = 20;
    private static final int IV_LENGTH = 16;
    private static final int PBE_ITERATION_COUNT = 100;
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String HASH_ALGORITHM = "SHA-512";
    private static final String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
	private static final String DATABASE_NAME = "wallet.db";
	private static final String HASHFILE_NAME = "hash.txt";
	private static final String COL_ID = "_id";
	private static final String COL_TITLE = "title";
	private static final String COL_NOTE = "note";
	private static final String COL_GROUP_ID = "group_id";
    private String mReplaceString;
    private Button mReplaceBtn;
    private Button mCancelBtn;
    private EditText mPasswordEdit;
	private SQLiteDatabase mDatabase;

    private class CryptoException extends RuntimeException
    {
        public CryptoException(String msg, Throwable e)
        {
            super(msg, e);
        }
    }

    private static class HexEncoder
    {
        public static byte[] toByte(String hex)
        {
            byte[] bytes = new byte[hex.length() / 2];
            for (int index = 0; index < bytes.length; ++index)
            {
                bytes[index] = (byte) Integer.parseInt(hex.substring(index * 2, (index + 1) * 2), 16);
            }
            return bytes;
        }

        public static String toHex(byte[] bytes)
        {
            StringBuilder sb = new StringBuilder();
            for (int index = 0; index < bytes.length; ++index)
            {
                sb.append(String.format("%02X", bytes[index]));
            }
            return sb.toString();
        }
    }

	private class DecryptViewBinder implements SimpleCursorAdapter.ViewBinder
	{
		private SecretKey mSecret;

		public DecryptViewBinder(SecretKey secret)
		{
			mSecret = secret;
		}

		public boolean setViewValue(View v, Cursor c, int column)
		{
			String value = c.getString(column);
			try
			{
				String decrypted = decrypt(mSecret, value);
				((TextView) v).setText(decrypted);
			}
			catch (Exception e)
			{
				((TextView) v).setText(value);
			}
			return true;
		}
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        Intent it = getIntent();
        String action = it.getAction();
		mDatabase = openDatabase();
        if (action != null && ACTION_INTERCEPT.equals(action))
		{
            /* Simejiから呼出された時 */
            mReplaceString = it.getStringExtra(REPLACE_KEY);// 置換元の文字を取得
            setContentView(R.layout.password);
            mReplaceBtn = (Button) findViewById(R.id.replace_btn);
            mReplaceBtn.setOnClickListener(this);
            mCancelBtn = (Button) findViewById(R.id.cancel_btn);
            mCancelBtn.setOnClickListener(this);
            mPasswordEdit = (EditText) findViewById(R.id.password_text);
			ImageView appIconView = (ImageView) findViewById(R.id.applicatio_icon);
			TextView appLabelView = (TextView) findViewById(R.id.application_label);
			PackageManager pm = getPackageManager();
			try
			{
				ApplicationInfo appInfo = pm.getApplicationInfo(getCallingPackage(), 0);
				appIconView.setImageDrawable(pm.getApplicationIcon(appInfo));
				appLabelView.setText(pm.getApplicationLabel(appInfo));
			}
			catch (PackageManager.NameNotFoundException e)
			{}
        }
		else
		{
            // Simeji以外から呼出された時
            setContentView(R.layout.hash);
			final TextView saltedPasswordHashView = (TextView) findViewById(R.id.salted_password_hash);
			final TextView versionView = (TextView) findViewById(R.id.version);
			final TextView newEncryptionMethodView = (TextView) findViewById(R.id.new_encryption_method);
			final TextView passwordSaltView = (TextView) findViewById(R.id.password_salt);
			final TextView encryptionSaltView = (TextView) findViewById(R.id.encryption_salt);
			if (isExternalStorageReadable())
			{
				File dir = Environment.getExternalStorageDirectory();
				File hashFile = new File(dir, HASHFILE_NAME);
				try
				{
					FileReader fr = new FileReader(hashFile);
					BufferedReader br = new BufferedReader(fr);
					saltedPasswordHashView.setText(br.readLine());
					versionView.setText(br.readLine());
					newEncryptionMethodView.setText(br.readLine());
					passwordSaltView.setText(br.readLine());
					encryptionSaltView.setText(br.readLine());
				}
				catch (FileNotFoundException e)
				{}
				catch (IOException e)
				{}
			}
			final EditText passwordEdit = (EditText) findViewById(R.id.password_text);
			final TextView genPasswordHashView = (TextView) findViewById(R.id.generated_password_hash);
			final Button okBtn = (Button) findViewById(R.id.ok_btn);
			final Button cancelBtn = (Button) findViewById(R.id.cancel_btn);
			Cursor c = mDatabase.rawQuery("select _id, title from entries", null);
			final ListView listView = (ListView) findViewById(R.id.list_view);
			final SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(
				this,
				R.layout.entry,
				c,
				new String[] {COL_TITLE},
				new int[] {R.id.title});
			listView.setAdapter(listAdapter);
			okBtn.setOnClickListener(new OnClickListener() {
					public void onClick(View p1)
					{
						String password = passwordEdit.getText().toString();
						String passwordSalt = passwordSaltView.getText().toString();
						String encryptionSalt = encryptionSaltView.getText().toString();
						String hash = getHash(password, passwordSalt);
						genPasswordHashView.setText(hash);
						SecretKey secret = getSecretKey(password, encryptionSalt);
						listAdapter.setViewBinder(new DecryptViewBinder(secret));
						listView.invalidateViews();
					}
				});
			cancelBtn.setOnClickListener(new OnClickListener() {
					public void onClick(View p1)
					{
						finish();
					}
				});
        }
    }

	/* Checks if external storage is available to at least read */
	public boolean isExternalStorageReadable()
	{
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state) ||
			Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
		{
			return true;
		}
		return false;
	}

    public void onClick(View v)
	{
        String result = null;
        if (v == mReplaceBtn)
		{
            result = getCallingPackage();
        }
		else if (v == mCancelBtn)
		{
            result = mReplaceString;
        }
        replace(result);
    }

    /**
     * 元の文字を置き換える
     * @param result Replacing string
     */
    private void replace(String result)
	{
        Intent data = new Intent();
        data.putExtra(REPLACE_KEY, result);
        setResult(RESULT_OK, data);
        finish();
    }

	public String encrypt(SecretKey secret, String cleartext) throws CryptoException
	{
		try
		{
			byte[] iv = generateIv();
			String ivHex = HexEncoder.toHex(iv);
			IvParameterSpec ivspec = new IvParameterSpec(iv);
			Cipher encryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
			encryptionCipher.init(Cipher.ENCRYPT_MODE, secret, ivspec);
			byte[] encryptedText = encryptionCipher.doFinal(cleartext.getBytes("UTF-8"));
			String encryptedHex = HexEncoder.toHex(encryptedText);
			return ivHex + encryptedHex;
		}
		catch (Exception e)
		{
			throw new CryptoException("Unable to encrypt", e);
		}
	}

    private String decrypt(SecretKey secret, String encrypted) throws CryptoException
    {
        try
        {
            Cipher decryptionCipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            String ivHex = encrypted.substring(0, IV_LENGTH * 2);
            String encryptedHex = encrypted.substring(IV_LENGTH * 2);
            IvParameterSpec ivspec = new IvParameterSpec(HexEncoder.toByte(ivHex));
            decryptionCipher.init(Cipher.DECRYPT_MODE, secret, ivspec);
            byte[] decryptedText = decryptionCipher.doFinal(HexEncoder.toByte(encryptedHex));
            String decrypted = new String(decryptedText, "UTF-8");
            return decrypted;
        }
        catch (Exception e)
        {
            throw new CryptoException("Unable to decrypt", e);
        }
    }

    private SecretKey getSecretKey(String password, String salt) throws CryptoException
    {
        try
        {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), HexEncoder.toByte(salt), PBE_ITERATION_COUNT, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM, PROVIDER);
            SecretKey tmp = factory.generateSecret(pbeKeySpec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);
            return secret;
        }
        catch (Exception e)
        {
            throw new CryptoException("Unable to get secret key", e);
        }
    }

    private String getHash(String password, String salt) throws CryptoException
    {
        try
        {
            String input = password + salt;
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM, PROVIDER);
            byte[] out = md.digest(input.getBytes("UTF-8"));
            return HexEncoder.toHex(out);
        }
        catch (Exception e)
        {
            throw new CryptoException("Unable to get hash", e);
        }
    }

    private String generateSalt() throws CryptoException
    {
        try
        {
            SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            String saltHex = HexEncoder.toHex(salt);
            return saltHex;
        }
        catch (Exception e)
        {
            throw new CryptoException("Unable to generate salt", e);
        }
    }

    private byte[] generateIv() throws NoSuchAlgorithmException
    {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }

	private SQLiteDatabase openDatabase()
	{
		SQLiteDatabase db = null;
		if (isExternalStorageReadable())
		{
			File dbFile = new File(Environment.getExternalStorageDirectory(), DATABASE_NAME);
			if (dbFile.exists())
			{
				db = SQLiteDatabase.openDatabase(
				    dbFile.getAbsolutePath(),
					null,
					SQLiteDatabase.OPEN_READONLY);
			}
		}
		return db;
	}
}
