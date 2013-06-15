package pocketmushroom;

import android.os.*;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.tmatz.pocketmushroom.*;
import java.security.spec.*;

public class PocketLock
{
    private static final String PROVIDER = "BC";
    private static final int SALT_LENGTH = 20;
    private static final int IV_LENGTH = 16;
    private static final int PBE_ITERATION_COUNT = 100;
    private static final String RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String HASH_ALGORITHM = "SHA-512";
    private static final String PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";
	private static final String HASHFILE_NAME = "hash.txt";

	private static PocketLock sPocketLock;

	private String mPasswordHash;
	private String mVersion;
	private String mEncryptionMethod;
	private String mPasswordSalt;
	private String mEncryptionSalt;
	private String mPackageName;
	private SecretKey mSecretKey;

	public boolean isUnlocked()
	{
		// TODO: Implement this method
		return false;
	}

	public static synchronized PocketLock getPocketLock(String packageName)
	{
		if (sPocketLock != null && sPocketLock.mPackageName.equals(packageName))
		{
			return sPocketLock;
		}
		else
		{
			return null;
		}
	}

	public static synchronized void setPocketLock(PocketLock lock)
	{
		sPocketLock = lock;
	}

	public PocketLock(String packageName) throws CryptoException
	{
		if (!Utilities.isExternalStorageReadable())
		{
			throw new CryptoException(R.string.exception_storage_not_readable);
		}

		File dir = Environment.getExternalStorageDirectory();
		File hashFile = new File(dir, HASHFILE_NAME);
		if (!hashFile.exists())
		{
			throw new CryptoException(R.string.exception_file_not_found);
		}

		try
		{
			FileReader fr = new FileReader(hashFile);
			BufferedReader br = new BufferedReader(fr);
			mPasswordHash = br.readLine();
			mVersion = br.readLine();
			mEncryptionMethod = br.readLine();
			mPasswordSalt = br.readLine();
			mEncryptionSalt = br.readLine();
			mPackageName = packageName;
		}
		catch (Exception e)
		{
			throw new CryptoException(R.string.exception_file_can_not_read);
		}
	}
	
	public String getPackageName()
	{
		return mPackageName;
	}

	public void unlock(String password) throws CryptoException
	{
		mSecretKey = getDatabaseSecretKey(password);
	}

	public String decrypt(String encrypted) throws CryptoException
	{
		try
		{
			return decrypt(mSecretKey, encrypted);
		}
		catch (Exception e)
		{
			throw new CryptoException(R.string.exception_decryption_failed, e);
		}
	}

	private SecretKey getDatabaseSecretKey(String password) throws CryptoException
	{
		String genPasswordHash = null;
		try
		{
			genPasswordHash = getHash(password, mPasswordSalt);
		}
		catch (Exception e)
		{
			throw new CryptoException(R.string.exception_decryption_failed, e);
		}

		if (!genPasswordHash.equals(mPasswordHash))
		{
			throw new CryptoException(R.string.exception_wrong_password);
		}

		try
		{
			return getSecretKey(password, mEncryptionSalt);
		}
		catch (Exception e)
		{
			throw new CryptoException(R.string.exception_decryption_failed, e);
		}
	}

	public static String encrypt(SecretKey secret, String cleartext) throws Exception
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

    public static String decrypt(SecretKey secret, String encrypted) throws Exception
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

    public static SecretKey getSecretKey(String password, String salt) throws Exception
    {
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), HexEncoder.toByte(salt), PBE_ITERATION_COUNT, 256);
		SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM, PROVIDER);
		SecretKey tmp = factory.generateSecret(pbeKeySpec);
		SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);
		return secret;
    }

    public static String getHash(String password, String salt) throws Exception
    {
		String input = password + salt;
		MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM, PROVIDER);
		byte[] out = md.digest(input.getBytes("UTF-8"));
		return HexEncoder.toHex(out);
    }

    public static String generateSalt() throws Exception
    {
		SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
		byte[] salt = new byte[SALT_LENGTH];
		random.nextBytes(salt);
		String saltHex = HexEncoder.toHex(salt);
		return saltHex;
    }

    private static byte[] generateIv() throws Exception
    {
        SecureRandom random = SecureRandom.getInstance(RANDOM_ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        return iv;
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
}
