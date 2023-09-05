package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static net.xetanth87.campaignsplitter.util.XT87Utils.STRING_PREFIX;

public class VictoryRewriter extends ScriptRewriter {
	public int campaignKeyOffset;
	public boolean insideInitGlobals;
	public final boolean addNextLevelMessage;
	public static final String SET_LEVEL_CALL = "call SetNextLevelBJ(";
	public static final String VICTORY_CALL = "call CustomVictoryBJ(";

	public VictoryRewriter(MapInjector mi, int campaignKeyOffset, boolean addNextLevelMessage) {
		super(mi);
		this.campaignKeyOffset = campaignKeyOffset;
		this.addNextLevelMessage = addNextLevelMessage;
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
		insideInitGlobals = false;
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		if (addNextLevelMessage)
			if (insideInitGlobals) {
				if (line.equals(END_FUNCTION)) {
					insideInitGlobals = false;
					sb.append(JASS_DELIM +
							"    set XT87VN=\"\"");
				}
			}
		if (line.contains(VICTORY_CALL)) {
			line = line.substring(line.indexOf(VICTORY_CALL) + VICTORY_CALL.length());
			List<String> words = Arrays.stream(line.substring(0, line.lastIndexOf(')')).split(",")).map(String::trim).collect(Collectors.toList());
			//append(//VICTORY_CALL + words[0] + ", true," + words[2] +  ")"
			//		VICTORY_CALL + words.get(0) + ", true,true)", sb);
			if (addNextLevelMessage)
				append(VICTORY_CALL + words.get(0) + ",true,true)" + JASS_DELIM +
						"    if XT87VN!=\"\" then" + JASS_DELIM +
						"        call DisplayTimedTextToPlayer(" + words.get(0) + ",0,0,0,GetLocalizedString(\"CONTINUE_PLAYING\")+\":\\n|cffffcc00\"+XT87VN+\"|r\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\")" + JASS_DELIM +
						"    endif", sb);
			else
				// force scorescreens
				append(VICTORY_CALL + words.get(0) + "," + words.get(0) +  ",true)", sb);
			return;
		}
		append(line, sb);
		if (addNextLevelMessage)
			if (line.equals("globals")) {
				sb.append(JASS_DELIM +
						"string XT87VN"); // Victory Next level path
			} else if (line.contains("function InitGlobals takes")) {
				insideInitGlobals = true;
			} else if (line.contains(SET_LEVEL_CALL)) {
				String mapPath = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")).toLowerCase(Locale.ROOT);
				sb.append(JASS_DELIM +
						"    set XT87VN=GetLocalizedString(\"" + STRING_PREFIX + (campaignKeyOffset + mi.cs.chapterNameKeysMap.get(mapPath)) + "\")");
			}
	}
}
