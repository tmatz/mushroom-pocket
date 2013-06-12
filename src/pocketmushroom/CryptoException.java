package pocketmushroom;

public class CryptoException extends RuntimeException
{
	public CryptoException(String msg, Throwable e)
	{
		super(msg, e);
	}

	public CryptoException(String msg)
	{
		super(msg);
	}
}
