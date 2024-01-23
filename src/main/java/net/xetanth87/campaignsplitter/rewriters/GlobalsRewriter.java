package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

import java.util.ArrayList;
import java.util.List;

public class GlobalsRewriter extends ScriptRewriter {
	public List<String> setList;

	public GlobalsRewriter(MapInjector mi) {
		super(mi);
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
		setList = new ArrayList<>();
		final boolean[] hasCallInGlobals = {false};
		new ScriptRewriter(mi) {
			@Override
			public void onStartRead(String text, StringBuffer sb) {
			}

			@Override
			public void onReadLine(String line, StringBuffer sb) {
				if (insideGlobals) {
					if (!line.contains("array") && line.contains("=") && line.contains("(")) {
						hasCallInGlobals[0] = true;
						stopReading();
					}
				}
			}
		}.scanScript();

		if (!hasCallInGlobals[0])
			stopReading();
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		if (insideGlobals) {
			if (!line.contains("array") && line.contains("=")) {
				if (line.startsWith(CONSTANT_DECLARATION))
					line = line.replaceFirst(CONSTANT_DECLARATION, "").trim();
				setList.add("set " + line.substring(line.indexOf(' ') + 1));
				line = line.substring(0, line.lastIndexOf('='));
			}
		}
		append(line, sb);
	}

	@Override
	public void onMainAfterLocals(StringBuffer sb) {
		for (String line : setList)
			append(line, sb);
	}
}
