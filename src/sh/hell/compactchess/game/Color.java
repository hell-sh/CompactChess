package sh.hell.compactchess.game;

public enum Color
{
	WHITE,
	BLACK;

	public Color opposite()
	{
		return this == Color.WHITE ? Color.BLACK : Color.WHITE;
	}
}
