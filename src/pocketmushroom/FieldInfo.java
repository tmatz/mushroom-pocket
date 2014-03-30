package pocketmushroom;

public class FieldInfo implements Comparable<FieldInfo>
{
	public int id;
	public String title;
	public String value;
	public boolean isHidden;
	
	public FieldInfo(int id, String title, String value, boolean isHidden)
	{
		this.id = id;
		this.title = title;
		this.value = value;
		this.isHidden = isHidden;
	}
	
	public String toString()
	{
		return title;
	}

	@Override
	public int compareTo(FieldInfo another)
	{
		return this.toString().compareTo(another.toString());
	}
}
