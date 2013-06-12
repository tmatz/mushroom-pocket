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
import java.util.*;
import pocketmushroom.MushroomActivity.*;

public class MushroomActivity extends Activity
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
	private static final String STATE_GROUP_ID = "group_id";
	private static final String STATE_ENTRY_ID = "entry_id";
	private static final int REQUEST_LOGIN = 1;

    private String mReplaceString;
	private ListView mEntryList;
	private SQLiteDatabase mDatabase;
	private int mGroupId = -1;
	private int mEntryId = -1;
	private String mPasswordHash;
	private String mVersion;
	private String mEncriptionMethod;
	private String mPasswordSalt;
	private String mEncriptionSalt;
	private SecretKey mSecretKey;
	private String mPassword;
	private HashData mHashData;
	private String mCallingPackage;



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
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(STATE_GROUP_ID, mGroupId);
		outState.putInt(STATE_ENTRY_ID, mEntryId);
	}

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

		if (savedInstanceState != null)
		{
			mGroupId = savedInstanceState.getInt(STATE_GROUP_ID, -1);
			mEntryId = savedInstanceState.getInt(STATE_ENTRY_ID, -1);
		}

		mDatabase = openDatabase();
		mHashData = openHastData();

		if (mDatabase == null || mHashData == null)
		{
			Toast.makeText(this, R.string.cant_open_pocket, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		if (getCallingPackage() == null || !getCallingPackage().equals(mCallingPackage))
		{
			mSecretKey = null;
			mPassword = null;
		}
		mCallingPackage = getCallingPackage();

		if (mSecretKey == null && mPassword != null)
		{
			mSecretKey = getDatabaseSecretKey(mPassword, mHashData);
			if (mSecretKey == null)
			{
				Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
			}
		}
		if (mSecretKey == null)
		{
			showLoginActivity();
		}

		setContentView(R.layout.mushroom);
		mEntryList = (ListView) findViewById(R.id.list_view);
    }

	private void showLoginActivity()
	{
		Intent intent = new Intent();
		intent.setAction(LoginActivity.ACTION_LOGIN);
		intent.putExtra(LoginActivity.EXTRA_PACKAGE_NAME, mCallingPackage);
		intent.putExtra(LoginActivity.EXTRA_HASH_DATA, mHashData);
		startActivityForResult(intent, REQUEST_LOGIN);
	}

	private void showEntryList()
	{
		Cursor c = mDatabase.rawQuery("select _id, title from entries", null);
		SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(
			this,
			R.layout.entry,
			c,
			new String[] {COL_TITLE},
			new int[] {R.id.title});
		mEntryList.setAdapter(listAdapter);
		listAdapter.setViewBinder(new DecryptViewBinder(mSecretKey));
		mEntryList.invalidateViews();
	}

	@Override
	public void onActivityResult(int request, int result, Intent data)
	{
		super.onActivityResult(request, result, data);

		switch (request)
		{
			case REQUEST_LOGIN:
				{
					if (result == RESULT_OK)
					{
						mPassword = data.getStringExtra(LoginActivity.EXTRA_PASSWORD);
						mSecretKey = getDatabaseSecretKey(mPassword, mHashData);
						if (mSecretKey == null)
						{
							Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show();
							showLoginActivity();
						}
						showEntryList();
					}
				}
				break;
		}
	}

	public static SecretKey getDatabaseSecretKey(String password, HashData hashData) throws CryptoException
	{
		String genPasswordHash = getHash(password, hashData.passwordSalt);
		if (!genPasswordHash.equals(hashData.passwordHash))
		{
			return null;
		}
		return getSecretKey(password, hashData.encryptionSalt);
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

	public static String encrypt(SecretKey secret, String cleartext) throws CryptoException
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

    public static String decrypt(SecretKey secret, String encrypted) throws CryptoException
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

    public static SecretKey getSecretKey(String password, String salt) throws CryptoException
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

    public static String getHash(String password, String salt) throws CryptoException
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

    public static String generateSalt() throws CryptoException
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

    private static byte[] generateIv() throws NoSuchAlgorithmException
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

	private HashData openHastData()
	{
		if (!isExternalStorageReadable())
		{
			return null;
		}

		File dir = Environment.getExternalStorageDirectory();
		File hashFile = new File(dir, HASHFILE_NAME);
		try
		{
			FileReader fr = new FileReader(hashFile);
			BufferedReader br = new BufferedReader(fr);
			HashData hd = new HashData();
			hd.passwordHash = br.readLine();
			hd.version = br.readLine();
			hd.encryptionMethod = br.readLine();
			hd.passwordSalt = br.readLine();
			hd.encryptionSalt = br.readLine();
			return hd;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
