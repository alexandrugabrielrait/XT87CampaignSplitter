package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

import java.util.Set;

public class FakeUserPlayerRewriter extends ScriptRewriter {
	private final Set<Integer> fakeUserPlayersSet;

	public FakeUserPlayerRewriter(MapInjector mi, Set<Integer> fakeUserPlayersSet) {
		super(mi, "FK");
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
						if (params[0].equals("Player(" + id + ")") || params[0].equals("ConvertedPlayer(" + (id + 1) + ")"))
							//line = "call " + trimmedCall + "(" + params[0] + "," + FAKE_PLAYER_CONTROLLER.getJassExpr() + ")";
							return;
				}
			}
		}
		append(line, sb);
	}
}
