package data;

public interface ValueListener
{
	public void updatedValue(String key, Object oldVal, Object newVal);
}
