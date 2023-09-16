package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.xetanth87.campaignsplitter.util.XT87Utils.putWithCount;

public class CallbackRewriter extends ScriptRewriter {
	private Map<String, Integer> callbackMap;

	public CallbackRewriter(MapInjector mi) {
		super(mi);
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
//						if (params[3].startsWith("function "))
//							params[3] = putWithCount(callbackMap, params[3].replaceFirst("function ", "").trim()) + "";
//						else
//							params[3] = "-1";
						if (params[3].startsWith("function "))
							params[3] = params[3].replaceFirst("function ", "").trim();
						params[3] = "\"" + params[3] + "\"";
						line = "call " + toJassCall(call, params);
					}
				}
			}
		}
		append(line, sb);
	}

	public List<Map.Entry<String, Integer>> findCallbacksAndModifyScript()
	{
		callbackMap = new HashMap<>();
		modifyScript();
		return callbackMap.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
	}
}