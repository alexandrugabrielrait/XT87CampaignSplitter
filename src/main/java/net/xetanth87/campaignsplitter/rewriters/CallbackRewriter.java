package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

public class CallbackRewriter extends ScriptRewriter {
	public CallbackRewriter(MapInjector mi) {
		super(mi, "CB");
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		String call = getTrimmedCallFromLine(line);
		String[] params;
		if (call != null) {
			params = getParamsFromLine(line);
			if (params != null) {
				if (call.equals("TimerStart")) {
					if (!params[3].equals("null")) {
						if (params[3].startsWith("function "))
							params[3] = params[3].replaceFirst("function ", "").trim();
						params[3] = "\"" + params[3] + "\"";
						line = "call " + toJassCall(call, params);
					}
				}
			}
		}
		append(sb, line);
	}
}