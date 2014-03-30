package pocketmushroom;

public class GroupInfo implements Comparable<GroupInfo>
{
	public int id;
	public String title;
	
	public GroupInfo(int id, String title)
	{
		this.id = id;
		this.title = title;
	}
	
	public String toString()
	{
		return title;
	}

	@Override
	public int compareTo(GroupInfo another)
	{
		return this.toString().compareTo(another.toString());
	}
}
