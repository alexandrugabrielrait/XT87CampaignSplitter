package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

public class NeutralPlayerRewriter extends ScriptRewriter {
	public NeutralPlayerRewriter(MapInjector mi) {
		super(mi, "NP");
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		line = line
				.replaceAll("([\\s(=])Player\\((12|\\$C)\\)", "$1Player(PLAYER_NEUTRAL_AGGRESSIVE)")
				.replaceAll("([\\s(=])Player\\((13|\\$D)\\)", "$1Player(GetBJPlayerNeutralVictim())")
				.replaceAll("([\\s(=])Player\\((14|\\$E)\\)", "$1Player(GetBJPlayerNeutralExtra())")
				.replaceAll("([\\s(=])Player\\((15|\\$F)\\)", "$1Player(PLAYER_NEUTRAL_PASSIVE)")
				.replaceAll("([\\s(=])ConvertedPlayer\\((13|\\$D)\\)", "$1Player(PLAYER_NEUTRAL_AGGRESSIVE)")
				.replaceAll("([\\s(=])ConvertedPlayer\\((14|\\$E)\\)", "$1Player(GetBJPlayerNeutralVictim())")
				.replaceAll("([\\s(=])ConvertedPlayer\\((15|\\$F)\\)", "$1Player(GetBJPlayerNeutralExtra())")
				.replaceAll("([\\s(=])ConvertedPlayer\\((16|\\$10)\\)", "$1Player(PLAYER_NEUTRAL_PASSIVE)");
		append(sb, line);
	}
}
