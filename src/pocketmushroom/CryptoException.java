package pocketmushroom;

public class CryptoException extends RuntimeException
{
	private static final long serialVersionUID = 30950614149791578L;

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
