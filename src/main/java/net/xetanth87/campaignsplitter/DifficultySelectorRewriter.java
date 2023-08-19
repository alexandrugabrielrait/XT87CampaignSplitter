package net.xetanth87.campaignsplitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class DifficultySelectorRewriter extends ScriptRewriter {
	public int playerId;
	public Set<String> initializationTriggers = null;//, periodicTimeTriggers = null;
	public Map<String, Integer> elapsedTimeIndices = null, periodicTimeIndices = null;
	public boolean triggersAdded, insideInitialization, insideInitGlobals;
	public static final String ELAPSED_TIME_REGISTER = "call TriggerRegisterTimerEventSingle(";
	public static final String ELAPSED_TIMER_PREFIX = "XT87CSElapsedTimer_";
	public static final String PERIODIC_TIME_REGISTER = "call TriggerRegisterTimerEventPeriodic(";
	public static final String PERIODIC_TIMER_PREFIX = "XT87CSPeriodicTimer_";

	public DifficultySelectorRewriter(MapInjector mi, int playerId) {
		super(mi);
		this.playerId = playerId;
	}

	public String[] timeParams(String line, String call) {
		String[] params = getParamsFromLine(line, call);
		return params;
	}

	public String[] elapsedTimeParams(String line) {
		return timeParams(line, ELAPSED_TIME_REGISTER);
	}

	public String[] periodicTimeParams(String line) {
		return timeParams(line, PERIODIC_TIME_REGISTER);
	}

	public void findInitializationTriggers(String text) {
		Scanner sc = new Scanner(text);
		initializationTriggers = new HashSet<>();
		elapsedTimeIndices = new HashMap<>();
		periodicTimeIndices = new HashMap<>();
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

			if (line.contains(ELAPSED_TIME_REGISTER)) {
				String[] params = elapsedTimeParams(line);
				if (!elapsedTimeIndices.containsKey(params[1]))
					elapsedTimeIndices.put(params[1], elapsedTimeIndices.size());
			} else if (line.contains(PERIODIC_TIME_REGISTER)) {
				String[] params = elapsedTimeParams(line);
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
		findInitializationTriggers(text);
		triggersAdded = false;
		insideInitialization = false;
		insideInitGlobals = false;
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		if (insideInitialization) {
			if (line.contains(END_FUNCTION)) {
				sb.append(JASS_DELIM +
						"    call CinematicFadeBJ(bj_CINEFADETYPE_FADEOUT, 0.00, \"ReplaceableTextures\\\\CameraMasks\\\\Black_mask.blp\", 0, 0, 0, 0)");
				insideInitialization = false;
			} else if (line.contains(INIT_CALL))
				return;
		} else if (insideInitGlobals) {
			if (line.contains(END_FUNCTION)) {
				insideInitGlobals = false;
				sb.append(JASS_DELIM +
						"    set XT87CSDifDialog=DialogCreate()" + JASS_DELIM +
						"    set XT87CSMapInitialization=CreateTimer()");
				for (int i = 0; i < elapsedTimeIndices.size(); i++)
					sb.append(JASS_DELIM +
							"    set " + ELAPSED_TIMER_PREFIX + i + "=CreateTimer()");
				for (int i = 0; i < periodicTimeIndices.size(); i++)
					sb.append(JASS_DELIM +
							"    set " + PERIODIC_TIMER_PREFIX + i + "=CreateTimer()");
			}
		}
		if (line.contains(ELAPSED_TIME_REGISTER)) {
			String[] params = elapsedTimeParams(line);
			line = "    call TriggerRegisterTimerExpireEventBJ(" + params[0] + "," + ELAPSED_TIMER_PREFIX + elapsedTimeIndices.get(params[1]) + " )";
		}
		else if (line.contains(PERIODIC_TIME_REGISTER)) {
			String[] params = periodicTimeParams(line);
			line = "    call TriggerRegisterTimerExpireEventBJ(" + params[0] + "," + PERIODIC_TIMER_PREFIX + periodicTimeIndices.get(params[1]) + " )";
		}
//		else if (line.contains(PERIODIC_TIME_REGISTER)) {
//			String[] params = periodicTimeParams(line);
//			if (periodicTimeTriggers.contains(params[0]))
//				sb.append(JASS_DELIM +
//						"    " + XT87Utils.DISABLE_TRIGGER + params[0] + ")");
//		}
		else if (!triggersAdded && line.contains("function Trig")) {
			sb.append(JASS_DELIM +
					"function Trig_XT87CS_Dif_Init_Actions takes nothing returns nothing" + JASS_DELIM +
					"    local string XT87CSDifDiagEasyName=GetLocalizedString(\"EASY\")" + JASS_DELIM +
					"    call PauseGameOn()" + JASS_DELIM +
					"    call DialogSetMessage(XT87CSDifDialog,GetLocalizedString(\"SELECT_DIFFICULTY_LEVEL\"))" + JASS_DELIM +
					"    if XT87CSDifDiagEasyName == \"Story\" then" + JASS_DELIM +
					"        set XT87CSDifDiagEasyName=\"Easy\"" + JASS_DELIM +
					"    endif" + JASS_DELIM +
					"    set XT87CSDifButtonEasy=DialogAddButton(XT87CSDifDialog,\"|cff00ff00\"+XT87CSDifDiagEasyName+\"|r\",0)" + JASS_DELIM +
					"    set XT87CSDifButtonNormal=DialogAddButton(XT87CSDifDialog,\"|cffffff00\"+GetLocalizedString(\"NORMAL\")+\"|r\",0)" + JASS_DELIM +
					"    set XT87CSDifButttonHard=DialogAddButton(XT87CSDifDialog,\"|cffff0000\"+GetLocalizedString(\"HARD\")+\"|r\",0)" + JASS_DELIM +
					"    call DialogDisplay(Player(" + playerId + "),XT87CSDifDialog,true)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function InitTrig_XT87CS_Dif_Init takes nothing returns nothing" + JASS_DELIM +
					"    set gg_trg_XT87CS_Dif_Init=CreateTrigger()" + JASS_DELIM +
					"    call TriggerRegisterTimerEventSingle(gg_trg_XT87CS_Dif_Init,0.01)" + JASS_DELIM +
					"    call TriggerAddAction(gg_trg_XT87CS_Dif_Init,function Trig_XT87CS_Dif_Init_Actions)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function Trig_XT87CS_Dif_Click_Actions takes nothing returns nothing" + JASS_DELIM +
					"    if GetClickedButton()==XT87CSDifButtonEasy then" + JASS_DELIM +
					"        call SetGameDifficulty(MAP_DIFFICULTY_EASY)" + JASS_DELIM +
					"    else" + JASS_DELIM +
					"        if GetClickedButton()==XT87CSDifButtonNormal then" + JASS_DELIM +
					"            call SetGameDifficulty(MAP_DIFFICULTY_NORMAL)" + JASS_DELIM +
					"        else" + JASS_DELIM +
					"            if GetClickedButton()==XT87CSDifButttonHard then" + JASS_DELIM +
					"                call SetGameDifficulty(MAP_DIFFICULTY_HARD)" + JASS_DELIM +
					"            else" + JASS_DELIM +
					"                call SetGameDifficulty(MAP_DIFFICULTY_INSANE)" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					"        endif" + JASS_DELIM +
					"    endif" + JASS_DELIM +
					"    call DialogClear(XT87CSDifDialog)" + JASS_DELIM +
					"    call DialogDestroy(XT87CSDifDialog)" + JASS_DELIM +
					"    call PauseGameOff()" + JASS_DELIM +
					"    call CinematicFadeBJ(bj_CINEFADETYPE_FADEIN,0.00,\"ReplaceableTextures\\\\CameraMasks\\\\Black_mask.blp\",0,0,0,0)" + JASS_DELIM +
					"    call StartTimerBJ(XT87CSMapInitialization,false,0.00)");
			if (!elapsedTimeIndices.isEmpty() || !periodicTimeIndices.isEmpty())
				sb.append(JASS_DELIM +
						"    call TriggerSleepAction( 0.01 )");
			for (String elapsedTime : elapsedTimeIndices.keySet())
				sb.append(JASS_DELIM +
						"    call TimerStart(" + ELAPSED_TIMER_PREFIX + elapsedTimeIndices.get(elapsedTime) + "," + elapsedTime + ",false,null)");
			for (String periodicTime : periodicTimeIndices.keySet())
				sb.append(JASS_DELIM +
						"    call TimerStart(" + PERIODIC_TIMER_PREFIX + periodicTimeIndices.get(periodicTime) + "," + periodicTime + ",true,null)");
//			for (String periodicTimeTrigger : periodicTimeTriggers)
//				sb.append(JASS_DELIM +
//						"    " + XT87Utils.ENABLE_TRIGGER + periodicTimeTrigger + ")");
			sb.append(JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function InitTrig_XT87CS_Dif_Click takes nothing returns nothing" + JASS_DELIM +
					"    set gg_trg_XT87CS_Dif_Click=CreateTrigger()" + JASS_DELIM +
					"    call TriggerRegisterDialogEventBJ(gg_trg_XT87CS_Dif_Click,XT87CSDifDialog)" + JASS_DELIM +
					"    call TriggerAddAction(gg_trg_XT87CS_Dif_Click,function Trig_XT87CS_Dif_Click_Actions)" + JASS_DELIM +
					END_FUNCTION);
			triggersAdded = true;
		}
		append(line, sb);
		if (line.equals("globals")) {
			sb.append(JASS_DELIM +
					"dialog XT87CSDifDialog" + JASS_DELIM +
					"button XT87CSDifButtonEasy" + JASS_DELIM +
					"button XT87CSDifButtonNormal" + JASS_DELIM +
					"button XT87CSDifButttonHard" + JASS_DELIM +
					"timer XT87CSMapInitialization" + JASS_DELIM +
					"trigger gg_trg_XT87CS_Dif_Init" + JASS_DELIM +
					"trigger gg_trg_XT87CS_Dif_Click");
			for (int i = 0; i < elapsedTimeIndices.size(); i++)
				sb.append(JASS_DELIM +
						"timer " + ELAPSED_TIMER_PREFIX + i);
			for (int i = 0; i < periodicTimeIndices.size(); i++)
				sb.append(JASS_DELIM +
						"timer " + PERIODIC_TIMER_PREFIX + i);
		} else if (line.contains("function InitGlobals takes")) {
			insideInitGlobals = true;
		} else if (line.contains("function InitCustomTriggers takes")) {
			sb.append(JASS_DELIM +
					"    call InitTrig_XT87CS_Dif_Init()" + JASS_DELIM +
					"    call InitTrig_XT87CS_Dif_Click()");
		} else if (line.contains("function RunInitializationTriggers takes") || line.contains("function main takes")) {
			insideInitialization = true;
		} else {
			for (String trigger : initializationTriggers) {
				if (line.contains("set " + trigger + "=") || line.contains("set " + trigger + " ")) {
					sb.append(JASS_DELIM +
							"    call TriggerRegisterTimerExpireEventBJ(" + trigger + ", XT87CSMapInitialization )");
					initializationTriggers.remove(trigger);
					break;
				}
			}
		}
	}
}
