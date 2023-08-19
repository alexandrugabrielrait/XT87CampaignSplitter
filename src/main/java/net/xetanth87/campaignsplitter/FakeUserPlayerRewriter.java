package net.xetanth87.campaignsplitter;

import java.util.Set;

import static net.xetanth87.campaignsplitter.XT87Utils.FAKE_PLAYER_CONTROLLER;

public class FakeUserPlayerRewriter extends ScriptRewriter {
	private final Set<Integer> fakeUserPlayersSet;

	public FakeUserPlayerRewriter(MapInjector mi, Set<Integer> fakeUserPlayersSet) {
		super(mi);
		this.fakeUserPlayersSet = fakeUserPlayersSet;
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		String call = getCallFromLine(line);
		String[] params;
		String trimmedCall = null;
		if (call != null) {
			params = getParamsFromLine(line, call);
			trimmedCall = getTrimmedCallFromLine(call);
			if (trimmedCall != null && params != null && params.length == 2) {
				if (trimmedCall.equals("SetPlayerController")) {
					for (Integer id : fakeUserPlayersSet)
						if (params[0].contains("Player(" + id + ")") || line.contains("ConvertedPlayer(" + (id + 1) + ")"))
							line = "call " + trimmedCall + "(" + params[0] + "," + FAKE_PLAYER_CONTROLLER.getJassExpr() + ")";
				}
			}
		}
		append(line, sb);
	}
}
