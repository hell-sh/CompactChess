package sh.hell.compactchess.game;

public enum CGNVersion
{
	/**
	 * https://web.archive.org/web/20180724201036/https://hellsh.com/CompactChess/cgn
	 */
	V1,
	/**
	 * http://web.archive.org/web/20180901071215/https://hellsh.com/CompactChess/cgn
	 */
	V2;

	public static final CGNVersion latest = CGNVersion.V2;
}
