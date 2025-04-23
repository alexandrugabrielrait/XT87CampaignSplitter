package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;

public class TimerRewriter extends ScriptRewriter {
	private static final String TIMER_PREFIX = "XT87TS";
	public boolean mainStartAdded;
	public Map<String, Integer> elapsedTimeIndices = null, periodicTimeIndices = null;
	public static final String ELAPSED_TIME_REGISTER = "call TriggerRegisterTimerEventSingle(";
	public static final String ELAPSED_TIMER_PREFIX = TIMER_PREFIX + "ElapsedTimer_";
	public static final String PERIODIC_TIME_REGISTER = "call TriggerRegisterTimerEventPeriodic(";
	public static final String PERIODIC_TIMER_PREFIX = TIMER_PREFIX + "PeriodicTimer_";

	public TimerRewriter(MapInjector mi) {
		super(mi, "TS");
	}

	public void scan(String text) {
		Scanner sc = new Scanner(text);
		elapsedTimeIndices = new HashMap<>();
		periodicTimeIndices = new HashMap<>();
		while (sc.hasNextLine()) {
			String line = sc.nextLine();

			if (line.contains(ELAPSED_TIME_REGISTER)) {
				String[] params = getParamsFromLine(line);
				if (!elapsedTimeIndices.containsKey(params[1]))
					elapsedTimeIndices.put(params[1], elapsedTimeIndices.size());
			} else if (line.contains(PERIODIC_TIME_REGISTER)) {
				String[] params = getParamsFromLine(line);
				if (!periodicTimeIndices.containsKey(params[1]))
					periodicTimeIndices.put(params[1], periodicTimeIndices.size());
			}
//			if (insideTriggerCreation) {
//				if (line.equals(END_FUNCTION)) {
//					insideTriggerCreation = false;
//					disabledTrigger = false;
//				}
//				else if (line.contains(XT87Utils.DISABLE_TRIGGER))
//					disabledTrigger = true;
//				else if (!disabledTrigger && line.contains(PERIODIC_TIME_REGISTER)) {
//					String[] params = periodicTimeParams(line);
//					periodicTimeTriggers.add(params[0]);
//				}
//			} else if (line.contains("CreateTrigger(")) {
//				insideTriggerCreation = true;
//			}
		}
		sc.close();
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
		//scan(text);
		mainStartAdded = false;
	}

	@Override
	public void onEnterGlobals(StringBuffer sb) {
//		for (int i = 0; i < elapsedTimeIndices.size(); i++)
//			append("timer " + ELAPSED_TIMER_PREFIX + i, sb);
//		for (int i = 0; i < periodicTimeIndices.size(); i++)
//			append("timer " + PERIODIC_TIMER_PREFIX + i, sb);
//		for (String elapsedTime : elapsedTimeIndices.keySet())
//			append("call TimerStart(" + ELAPSED_TIMER_PREFIX + elapsedTimeIndices.get(elapsedTime) + "," + elapsedTime + ",false,null)", sb);
//		for (String periodicTime : periodicTimeIndices.keySet())
//			append("call TimerStart(" + PERIODIC_TIMER_PREFIX + periodicTimeIndices.get(periodicTime) + "," + periodicTime + ",true,null)", sb);
		append("timer array " + rewriterPrefix + "ETA" + JASS_DELIM + // Event Timer Array
				"integer " + TIMER_PREFIX + "ETL=0" + JASS_DELIM + // Event Timer Array Length
				"boolean " + TIMER_PREFIX + "TSP=false", sb); // Timers Start Paused
	}

	@Override
	public void onExitGlobals(StringBuffer sb) {
		append("function " + TIMER_PREFIX + "TriggerRegisterTimerEvent takes trigger t, real r, boolean b returns event" + JASS_DELIM +
				"local integer i=" + TIMER_PREFIX + "ETL" + JASS_DELIM +
				"local event e" + JASS_DELIM +
				"set " + TIMER_PREFIX + "ETL=i+1" + JASS_DELIM +
				"set " + TIMER_PREFIX + "ETA[i]=CreateTimer()" + JASS_DELIM +
				"set e=TriggerRegisterTimerExpireEvent(t," + TIMER_PREFIX + "ETA[i])" + JASS_DELIM +
				"call TimerStart(" + TIMER_PREFIX + "ETA[i],r,b,null)" + JASS_DELIM +
				"return e" + JASS_DELIM +
				END_FUNCTION, sb);
		append("function " + TIMER_PREFIX + "TriggerRegisterTimerEventPeriodic takes trigger t, real r returns event" + JASS_DELIM +
				"return " + TIMER_PREFIX + "TriggerRegisterTimerEvent(t,r,true)" + JASS_DELIM +
				END_FUNCTION, sb);
		append("function " + TIMER_PREFIX + "TriggerRegisterTimerEventSingle takes trigger t, real r returns event" + JASS_DELIM +
				"return " + TIMER_PREFIX + "TriggerRegisterTimerEvent(t,r,false)" + JASS_DELIM +
				END_FUNCTION, sb);
	}

	@Override
	public void onMainAfterLocals(StringBuffer sb) {
//		for (int i = 0; i < elapsedTimeIndices.size(); i++)
//			append("    set " + ELAPSED_TIMER_PREFIX + i + "=CreateTimer()", sb);
//		for (int i = 0; i < periodicTimeIndices.size(); i++)
//			append("    set " + PERIODIC_TIMER_PREFIX + i + "=CreateTimer()", sb);
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		// region replace specific calls
		for (String function : Arrays.asList(
				"TriggerRegisterTimerEvent",
				"TriggerRegisterTimerEventPeriodic",
				"TriggerRegisterTimerEventSingle"
		))
			for (String prefix : Arrays.asList("(", " ", ",", "="))
				for (String suffix : Arrays.asList("(", " ")) {
					String call = function + suffix;
					line = line.replace(prefix + call, prefix + TIMER_PREFIX + call);
				}
		// endregion

		String call = getTrimmedCallFromLine(line);
		String[] params;
		if (call != null) {
			params = getParamsFromLine(line);
			if (params != null) {
				boolean isBasicEvent = call.equals("TriggerRegisterTimerEvent");
				if ((isBasicEvent && "false".equals(params[2])) || call.equals("TriggerRegisterTimerEventSingle"))
					line = "call TriggerRegisterTimerExpireEvent(" + params[0] + "," + ELAPSED_TIMER_PREFIX + elapsedTimeIndices.get(params[1]) + " )";
				else if ((isBasicEvent && "true".equals(params[2])) || call.equals("TriggerRegisterTimerEventPeriodic"))
					line = "call TriggerRegisterTimerExpireEvent(" + params[0] + "," + PERIODIC_TIMER_PREFIX + periodicTimeIndices.get(params[1]) + " )";
			}
		}
		append(line, sb);
	}
}
