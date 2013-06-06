package pocketmushroom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import org.tmatz.pocketmushroom.R;
import android.widget.*;
import android.content.pm.*;
import android.content.pm.PackageManager.*;
import java.lang.RuntimeException;
import java.lang.Throwable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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
    private String mReplaceString;
    private Button mReplaceBtn;
    private Button mCancelBtn;
    private EditText mPasswordEdit;

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

    @Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        Intent it = getIntent();
        String action = it.getAction();
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
            setContentView(R.layout.password);
        }
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

    private byte[] generateIv() throws NoSuchAlgorithmException, NoSuchProviderException
    {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
    }
}
