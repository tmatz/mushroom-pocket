package pocketmushroom;

public class CryptoException extends RuntimeException
{
	public int id;
	
	public CryptoException(int id, Throwable e)
	{
		super(e);
		this.id = id;
	}
	public CryptoException(int id)
	{
		this.id = id;
	}
}
