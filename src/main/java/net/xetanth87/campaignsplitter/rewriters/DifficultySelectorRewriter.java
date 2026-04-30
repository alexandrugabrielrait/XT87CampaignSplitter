package net.xetanth87.campaignsplitter.rewriters;

import net.xetanth87.campaignsplitter.MapInjector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.END_IF;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_VAR_REPLACE;

public class DifficultySelectorRewriter extends ScriptRewriter {
	public int playerId;
	public Set<String> initializationTriggers = null;//, periodicTimeTriggers = null;
	//	public Map<String, Integer> elapsedTimeIndices = null, periodicTimeIndices = null;
	public boolean triggersAdded, insideInitialization, insideInitGlobals;
	private final String MAP_INIT = "M";
	private final String DIALOG = "D";
	private final String BUTTON_EASY = "BE";
	private final String BUTTON_NORMAL = "BN";
	private final String BUTTON_HARD = "BH";
//	public static final String ELAPSED_TIME_REGISTER = "call TriggerRegisterTimerEventSingle(";
//	public static final String ELAPSED_TIMER_PREFIX = rewriterPrefix + "ElapsedTimer_";
//	public static final String PERIODIC_TIME_REGISTER = "call TriggerRegisterTimerEventPeriodic(";
//	public static final String PERIODIC_TIMER_PREFIX = rewriterPrefix + "PeriodicTimer_";

	public DifficultySelectorRewriter(MapInjector mi, int playerId) {
		super(mi, "DS");
		this.playerId = playerId;

		addTriggerConfiguration("I", // init
				"local string e=GetLocalizedString(\"EASY\")" + JASS_DELIM +
						"call PauseGameOn()" + JASS_DELIM +
						"call DialogSetMessage(" + rewriterPrefix + DIALOG + ",GetLocalizedString(\"SELECT_DIFFICULTY_LEVEL\"))" + JASS_DELIM +
						"if e==\"Story\" then" + JASS_DELIM +
						"set e=\"Easy\"" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"set " + rewriterPrefix + BUTTON_EASY + "=DialogAddButton(" + rewriterPrefix + DIALOG + ",\"|cff00ff00\"+e+\"|r\",0)" + JASS_DELIM +
						"set " + rewriterPrefix + BUTTON_NORMAL + "=DialogAddButton(" + rewriterPrefix + DIALOG + ",\"|cffffff00\"+GetLocalizedString(\"NORMAL\")+\"|r\",0)" + JASS_DELIM +
						"set " + rewriterPrefix + BUTTON_HARD + "=DialogAddButton(" + rewriterPrefix + DIALOG + ",\"|cffff0000\"+GetLocalizedString(\"HARD\")+\"|r\",0)" + JASS_DELIM +
						"call DialogDisplay(Player(" + playerId + ")," + rewriterPrefix + DIALOG + ",true)",
				"call TriggerRegisterTimerEventSingle(" + TRIGGER_VAR_REPLACE + ",0.01)");

		addTriggerConfiguration("C", // clicked
				"local button b=GetClickedButton()" + JASS_DELIM +
						"if b==" + rewriterPrefix + BUTTON_EASY + " then" + JASS_DELIM +
						"call SetGameDifficulty(MAP_DIFFICULTY_EASY)" + JASS_DELIM +
						"elseif b==" + rewriterPrefix + BUTTON_NORMAL + " then" + JASS_DELIM +
						"call SetGameDifficulty(MAP_DIFFICULTY_NORMAL)" + JASS_DELIM +
						"elseif b==" + rewriterPrefix + BUTTON_HARD + " then" + JASS_DELIM +
						"call SetGameDifficulty(MAP_DIFFICULTY_HARD)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call SetGameDifficulty(MAP_DIFFICULTY_INSANE)" + JASS_DELIM +
						"endif" + JASS_DELIM +
						//"call DialogClear(" + rewriterPrefix + "DifDialog)" + JASS_DELIM +
						//"call DialogDestroy(" + rewriterPrefix + "DifDialog)" + JASS_DELIM +
						"call PauseGameOff()" + JASS_DELIM +
						"call CinematicFadeBJ(bj_CINEFADETYPE_FADEIN,0.00,\"ReplaceableTextures\\\\CameraMasks\\\\Black_mask.blp\",0,0,0,0)" + JASS_DELIM +
						"call StartTimerBJ(" + rewriterPrefix + MAP_INIT + ",false,0.00)",
				"call TriggerRegisterDialogEvent(" + TRIGGER_VAR_REPLACE + "," + rewriterPrefix + DIALOG + ")");
//			if (!elapsedTimeIndices.isEmpty() || !periodicTimeIndices.isEmpty())
//				append(JASS_DELIM +
//						"call TriggerSleepAction( 0.01 )", sb);
//			for (String elapsedTime : elapsedTimeIndices.keySet())
//				append(JASS_DELIM +
//						"call TimerStart(" + ELAPSED_TIMER_PREFIX + elapsedTimeIndices.get(elapsedTime) + "," + elapsedTime + ",false,null)", sb);
//			for (String periodicTime : periodicTimeIndices.keySet())
//				append(JASS_DELIM +
//						"call TimerStart(" + PERIODIC_TIMER_PREFIX + periodicTimeIndices.get(periodicTime) + "," + periodicTime + ",true,null)", sb);
//			for (String periodicTimeTrigger : periodicTimeTriggers)
//				append(JASS_DELIM +
//						"" + XT87Utils.ENABLE_TRIGGER + periodicTimeTrigger + ")", sb);
	}

	public void findInitializationTriggers(String text) {
		Scanner sc = new Scanner(text);
		initializationTriggers = new HashSet<>();
//		elapsedTimeIndices = new HashMap<>();
//		periodicTimeIndices = new HashMap<>();
		//periodicTimeTriggers = new HashSet<>();
		boolean insideInitialization = false, insideTriggerCreation = false;//, disabledTrigger = false;
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (insideInitialization) {
				if (line.equals(END_FUNCTION))
					insideInitialization = false;
				else if (line.contains(INIT_CALL))
					initializationTriggers.add(initializationTriggerFromCall(line));
			} else if (isInitFunction(line))
				insideInitialization = true;

//			if (line.contains(ELAPSED_TIME_REGISTER)) {
//				String[] params = getParamsFromLine(line);
//				if (!elapsedTimeIndices.containsKey(params[1]))
//					elapsedTimeIndices.put(params[1], elapsedTimeIndices.size());
//			} else if (line.contains(PERIODIC_TIME_REGISTER)) {
//				String[] params = getParamsFromLine(line);
//				if (!periodicTimeIndices.containsKey(params[1]))
//					periodicTimeIndices.put(params[1], periodicTimeIndices.size());
//			}
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
	protected void onMainLocals(StringBuffer sb) {
		append(JASS_DELIM +
				"set " + rewriterPrefix + DIALOG + "=DialogCreate()" + JASS_DELIM +
				"set " + rewriterPrefix + MAP_INIT + "=CreateTimer()", sb);
//				for (int i = 0; i < elapsedTimeIndices.size(); i++)
//					append(JASS_DELIM +
//							"set " + ELAPSED_TIMER_PREFIX + i + "=CreateTimer()", sb);
//				for (int i = 0; i < periodicTimeIndices.size(); i++)
//					append(JASS_DELIM +
//							"set " + PERIODIC_TIMER_PREFIX + i + "=CreateTimer()", sb);
	}

	@Override
	protected void onMainEnd(StringBuffer sb) {
		append(JASS_DELIM +
				"call CinematicFadeBJ(bj_CINEFADETYPE_FADEOUT, 0.00, \"ReplaceableTextures\\\\CameraMasks\\\\Black_mask.blp\", 0, 0, 0, 0)", sb);
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
		findInitializationTriggers(text);
		triggersAdded = false;
		insideInitialization = false;
		insideInitGlobals = false;
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		if (insideInitialization) {
			if (line.contains(END_FUNCTION)) {
				insideInitialization = false;
			} else if (line.contains(INIT_CALL))
				return;
		} else if (insideInitGlobals) {
			if (line.contains(END_FUNCTION)) {
				insideInitGlobals = false;
			}
		}
//		if (line.contains(ELAPSED_TIME_REGISTER)) {
//			String[] params = getParamsFromLine(line);
//			line = "call TriggerRegisterTimerExpireEventBJ(" + params[0] + "," + ELAPSED_TIMER_PREFIX + elapsedTimeIndices.get(params[1]) + " )";
//		} else if (line.contains(PERIODIC_TIME_REGISTER)) {
//			String[] params = getParamsFromLine(line);
//			line = "call TriggerRegisterTimerExpireEventBJ(" + params[0] + "," + PERIODIC_TIMER_PREFIX + periodicTimeIndices.get(params[1]) + " )";
//		}
//		else if (line.contains(PERIODIC_TIME_REGISTER)) {
//			String[] params = periodicTimeParams(line);
//			if (periodicTimeTriggers.contains(params[0]))
//				sb.append(JASS_DELIM +
//						"" + XT87Utils.DISABLE_TRIGGER + params[0] + ")");
//		}
		else if (!triggersAdded && line.contains("function Trig")) {
			triggersAdded = true;
		}
		append(line, sb);
		if (line.equals("globals")) {
			append(JASS_DELIM +
					"dialog " + rewriterPrefix + DIALOG + JASS_DELIM +
					"button " + rewriterPrefix + BUTTON_EASY + JASS_DELIM +
					"button " + rewriterPrefix + BUTTON_NORMAL + JASS_DELIM +
					"button " + rewriterPrefix + BUTTON_HARD + JASS_DELIM +
					"timer " + rewriterPrefix + MAP_INIT, sb);
//			for (int i = 0; i < elapsedTimeIndices.size(); i++)
//				append(JASS_DELIM +
//						"timer " + ELAPSED_TIMER_PREFIX + i, sb);
//			for (int i = 0; i < periodicTimeIndices.size(); i++)
//				append(JASS_DELIM +
//						"timer " + PERIODIC_TIMER_PREFIX + i, sb);
		} else if (line.contains("function InitGlobals takes")) {
			insideInitGlobals = true;
		} else if (line.contains("function RunInitializationTriggers takes") || line.contains("function main takes")) {
			insideInitialization = true;
		} else {
			for (String trigger : initializationTriggers) {
				if (line.contains("set " + trigger + "=") || line.contains("set " + trigger + " ")) {
					append(JASS_DELIM +
							"call TriggerRegisterTimerExpireEventBJ(" + trigger + ", " + rewriterPrefix + MAP_INIT + ")", sb);
					initializationTriggers.remove(trigger);
					break;
				}
			}
		}
	}
}
