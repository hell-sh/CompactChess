package sh.hell.compactchess.game;

import java.util.Comparator;

public class PGNTagComparator implements Comparator<String>
{
	@Override
	public int compare(String o1, String o2)
	{
		PGNSevenTagRoster o1str = PGNSevenTagRoster.get(o1);
		PGNSevenTagRoster o2str = PGNSevenTagRoster.get(o2);
		if(o1str != null && o2str != null)
		{
			return o1str.compareTo(o2str);
		}
		else if(o1str == null && o2str == null)
		{
			return o1.compareTo(o2);
		}
		else
		{
			return o1str == null ? 1 : -1;
		}
	}
}

