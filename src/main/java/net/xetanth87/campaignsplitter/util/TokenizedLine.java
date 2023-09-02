package net.xetanth87.campaignsplitter.util;

import net.xetanth87.campaignsplitter.rewriters.ScriptRewriter;

public class TokenizedLine {
	private static final String LOCAL_PREFIX = "local";
	private static final String CALL_PREFIX = "call";
	private static final String SET_PREFIX = "set";

	public String locality;
	public String directive;
	public String target;
	public String call;
	public String[] params;

	private TokenizedLine(String line)
	{
		if (line.startsWith(LOCAL_PREFIX + " "))
		{
			locality = LOCAL_PREFIX;
			line = line.substring(LOCAL_PREFIX.length() + 1);
		}
		else if (line.startsWith(SET_PREFIX + " "))
		{
			params = ScriptRewriter.getParamsFromLine(line);
		}
		else if (line.startsWith(CALL_PREFIX + " "))
		{
			call = ScriptRewriter.getTrimmedCallFromLine(line);
			params = ScriptRewriter.getParamsFromLine(line);
		}
	}

	public static TokenizedLine tokenizeLine(String line)
	{
		if (line == null)
			return null;
		line = line.trim();
		if (line.isBlank() || line.startsWith("//"))
			return null;
		return new TokenizedLine(line);
	}
}
