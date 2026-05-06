package net.xetanth87.campaignsplitter.rewriters;

import net.moonlightflower.wc3libs.bin.app.W3I;
import net.xetanth87.campaignsplitter.MapInjector;
import net.xetanth87.campaignsplitter.util.BlockUtils;
import net.xetanth87.campaignsplitter.util.CoopCallAdjustments;
import net.xetanth87.campaignsplitter.util.XT87Utils;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.xetanth87.campaignsplitter.util.BlockUtils.bCall;
import static net.xetanth87.campaignsplitter.util.BlockUtils.bFunc;
import static net.xetanth87.campaignsplitter.util.BlockUtils.bIf;
import static net.xetanth87.campaignsplitter.util.BlockUtils.bNot;
import static net.xetanth87.campaignsplitter.util.BlockUtils.bSet;
import static net.xetanth87.campaignsplitter.util.XT87Utils.DIALOG_DECLARATION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.END_IF;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;
import static net.xetanth87.campaignsplitter.util.XT87Utils.LOCAL_DECLARATION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.STRING_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_VAR_REPLACE;

public class CoopRewriter extends ScriptRewriter {
	private static final String SETPLAYERCOLOR = "call SetPlayerColor(";
	private static final String DEFINE_START_LOC = "call DefineStartLocation(";
	private static final String SETPLAYERCOLORBJ = "call SetPlayerColorBJ(";
	private static final String CONVERTPLAYERCOLOR = "ConvertPlayerColor(";
	private static final String ARCHON_PREFIX_BASE = "XT87A";
	private static final String ARCHON_PREFIX = ARCHON_PREFIX_BASE + "S";
	private static final String FILE_IO_PREFIX = ARCHON_PREFIX_BASE + "I";
	private static final String FILE_LIBRARY_PREFIX = ARCHON_PREFIX_BASE + "F";
	private static final String FORCE_NAME = ARCHON_PREFIX + "Force";
	private static final String TEMP_POINT_NAME = ARCHON_PREFIX + "TempPoint";
	private static final String ENFORCE_ARCHON = ARCHON_PREFIX + "EnforceArchon";
	//private static final String LAST_CREATED_COOP_CACHE = ARCHON_PREFIX + "_lastCreatedGameCache";
	private static final String PLAYER_ID_ARRAY = ARCHON_PREFIX + "IDA";
	private static final String PLAYER_INDEX_ARRAY = ARCHON_PREFIX + "PIA";
	private static final String AUX_PLAYER = ARCHON_PREFIX + "AXP";
	private static final String GENERAL_HASHTABLE = ARCHON_PREFIX + "GHT";
	private static final String SYNC_CACHE = ARCHON_PREFIX + "SCH";
	private static final String SELECTION_HASHTABLE = ARCHON_PREFIX + "FXT";
	private static final String ABILITY_ARRAY = ARCHON_PREFIX + "AbilityArray";
	private static final String ABILITY_ARRAY_SIZE = ABILITY_ARRAY + "SIZE";
	private static final String SYNC_FUNC_NAME = ARCHON_PREFIX + "SyncX";

	private static final int FILE_LIBRARY_ABILITY_COUNT = 10;
	private static final int FILE_LIBRARY_PRELOAD_LIMIT = 200;

	public static final String SELECTION_INDICATOR_PATH = "selectionIndicator";

	// coop cache
	public static final boolean WITH_CUSTOM_GAMECACHE = true;
	public static final boolean WITH_FULL_ENCODING = false;
	public static final boolean WITH_PAUSE_COMMAND = false;
	public static final boolean COMPARE_CHECKSUMS = false;

	public static final String CACHE_CACHE_PREFIX = "c";
	public static final String CACHE_MISSION_PREFIX = "m";
	public static final String CACHE_INTEGER_PREFIX = "i";
	public static final String CACHE_REAL_PREFIX = "r";
	public static final String CACHE_BOOL_PREFIX = "b";
	public static final String CACHE_STRING_PREFIX = "s";
	public static final String CACHE_UNIT_PREFIX = "u";
	public static final String CACHE_OTHER_PREFIX = "o";
	public static final String CACHE_FLUSH_PREFIX = "f";

	public static final String CACHE_CAMPAIGN_MKEY = CACHE_CACHE_PREFIX + "ache";
	//public static final String CACHE_MKEY_PREFIX = CACHE_MISSION_PREFIX;

	private static final String CACHE_CAMPAIGNFILE_KEY = CACHE_OTHER_PREFIX + CACHE_CACHE_PREFIX;
	private static final String CACHE_INTEGER_KEY = CACHE_OTHER_PREFIX + CACHE_INTEGER_PREFIX;
	private static final String CACHE_REAL_KEY = CACHE_OTHER_PREFIX + CACHE_REAL_PREFIX;
	private static final String CACHE_BOOL_KEY = CACHE_OTHER_PREFIX + CACHE_BOOL_PREFIX;
	private static final String CACHE_STRING_KEY = CACHE_OTHER_PREFIX + CACHE_STRING_PREFIX;
	private static final String CACHE_UNIT_KEY = CACHE_OTHER_PREFIX + CACHE_UNIT_PREFIX;
	private static final String CACHE_FLUSH_KEY = CACHE_OTHER_PREFIX + CACHE_FLUSH_PREFIX;
	// todo make sure count doesn't count the same variable twice
	private static final String CACHE_COUNT_KEY = "count";

	private static final int GH_BOOL_HAS_ABILITY = 0;
	private static final int GH_BOOL_IS_PAUSED = 1;
	private static final int GH_BOOL_IS_PAUSED_PLAYER = 2;
	private static final int GH_BOOL_TIMER_PAUSED = 3;
	private static final int GH_BOOL_TIMER_PERIODIC = 4;

	private static final int GH_STRING_DIALOG_BUTTON_TEXT = 0;
	private static final int GH_STRING_TIMER_CALLBACK = 1;
	private static final int GH_STRING_CAMPAIGN_BUTTON_NAME = 2;

	private static final int GH_REAL_PAUSED_HP = 0;
	private static final int GH_REAL_PAUSED_MP = 1;
	private static final int GH_REAL_TIMER_REMAINING = 2;
	private static final int GH_REAL_TIMER_TIMEOUT = 3;

	private static String getCommonHashFunc(String key) {
		return "\"" + key + "\"";
	}

	public static final int UNIT_CACHE_FORMAT = 0;

	public static final int CACHE_XP_INDEX = 2;
	public static final int CACHE_STR_INDEX = 3;
	public static final int CACHE_AGI_INDEX = 4;
	public static final int CACHE_INT_INDEX = 5;
	public static final int CACHE_UNSPEND_POINTS_INDEX = 6;
	public static final int CACHE_HEALTH_MAX_INDEX = 7;
	public static final int CACHE_MANA_MAX_INDEX = 8;
	public static final int CACHE_ATTACK_DAMAGE_MIN_INDEX = 9;
	public static final int CACHE_ATTACK_DAMAGE_MAX_INDEX = 10;
	public static final int CACHE_ARMOR_INDEX = 11;
	public static final int CACHE_ABILITY_COUNT_INDEX = 12;
//	public static final String CACHE_ABILITY_ID_INDEX = "A";
//	public static final String CACHE_ABILITY_LVL_INDEX = "L";
//
//	public static final String CACHE_INVENTORY_SIZE_INDEX = "S";
//	public static final String CACHE_ITEM_INFO_INDEX = "I";
	//public static final String CACHE_ITEM_CHARGE_INDEX = "G";
	//public static final String CACHE_ITEM_BITS_INDEX = "B";

	// common code
	private static final String PAUSE_LOCK_CODE = "loop" + JASS_DELIM +
			"exitwhen not " + ARCHON_PREFIX + "GIP" + JASS_DELIM +
			"call TriggerSleepAction(0.01)" + JASS_DELIM +
			"endloop";
	private static final String FLUSH_KEY_CODE =
			"I2S(" + ARCHON_PREFIX + "DoFlushIndex(q,cn,m2,\"" + CACHE_FLUSH_PREFIX + "\"+k2,0))" +
					"+\"|\"+I2S(" + ARCHON_PREFIX + "DoFlushIndex(q,cn,m2,\"" + CACHE_FLUSH_PREFIX + CACHE_MISSION_PREFIX + "\",0))" +
					"+\"|\"+I2S(" + ARCHON_PREFIX + "DoFlushIndex(q,cn,\"" + CACHE_CAMPAIGN_MKEY + "\",\"" + CACHE_FLUSH_PREFIX + CACHE_CACHE_PREFIX + "\",0))";

	private static List<String> getBaseResourceNames() {
		// Gold, Lumber
		return Arrays.asList("G", "L");
	}

	private static List<String> getFoodResourceNames() {
		// Food Used, Food Cap, Food Max
		return Arrays.asList("U", "C", "M");
	}

	private static List<String> getAllResourceNames() {
		List<String> l = new ArrayList<>(getBaseResourceNames());
		l.addAll(new ArrayList<>(getFoodResourceNames()));
		return l;
	}

	private static String unitSuffix(String suffix) {
		return "k2+\"_" + suffix + "\"";
	}

	private static String constSuffix(Object value) {
		return "k2+\"_" + value + "\"";
	}

	private static String varSuffix(String variable) {
		return "k2+\"_\"+I2S(" + variable + ")";
	}

	public boolean triggersAdded, mainStartAdded, insideConfig;
	private boolean pauseLockAdded;
	public W3I info;
	public W3I.Player mainPlayer;
	public List<W3I.Player> secondaryPlayers;
	public List<Integer> allPlayerNumbers;
	public List<String> dialogNames;
	public Map<String, Integer> cacheFieldMap;
	public int campaignKeyOffset;

	public CoopRewriter(MapInjector mi, W3I info, W3I.Player mainPlayer, List<W3I.Player> secondaryPlayers, int campaignKeyOffset) {
		super(mi, "AS");
		this.info = info;
		this.mainPlayer = mainPlayer;
		this.secondaryPlayers = secondaryPlayers;
		this.campaignKeyOffset = campaignKeyOffset;
		allPlayerNumbers = new ArrayList<>(1 + secondaryPlayers.size());
		allPlayerNumbers.add(mainPlayer.getNum());
		secondaryPlayers.forEach(x -> allPlayerNumbers.add(x.getNum()));
		dialogNames = new ArrayList<>();

		// region trigger config
		if (WITH_CUSTOM_GAMECACHE) {
			// region refresh paused units
			addTriggerConfiguration("PeriodicRefresh",
					"call " + ARCHON_PREFIX + "ForAllUnits(function " + ARCHON_PREFIX + "EnumRefreshPausedUnit)",
					"call DisableTrigger(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PeriodicRefresh)" + JASS_DELIM +
							"call TriggerRegisterTimerEventPeriodic(" + TRIGGER_VAR_REPLACE + ",0.03)");
			// endregion
			// region pause spawned units
			addTriggerConfiguration("PSU",
					"local unit u=GetEnteringUnit()" + JASS_DELIM +
							"if not HaveSavedBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED + ",GetHandleId(u)) then" + JASS_DELIM +
							"call " + ARCHON_PREFIX + "SavePausedUnit(u)" + JASS_DELIM +
							END_IF,
					"call TriggerRegisterEnterRectSimple(" + TRIGGER_VAR_REPLACE + ", GetPlayableMapRect())");
			// endregion
			// region watch learned skill
			addTriggerConfiguration("WatchLearnedSkill",
					"    local integer i=0" + JASS_DELIM +
							"    local integer a=GetLearnedSkill()" + JASS_DELIM +
							"    if " + ARCHON_PREFIX + "WatchSkill(a)==true then" + JASS_DELIM +
							DebugOnly("call BJDebugMsg(I2S(i)+\"] \"+I2S(a)+\" _\"+I2S(" + ABILITY_ARRAY_SIZE + " - 1))" + JASS_DELIM) +
							"        loop" + JASS_DELIM +
							"            exitwhen i>=" + ABILITY_ARRAY_SIZE + JASS_DELIM +
							DebugOnly("call BJDebugMsg(I2S(i)+\") \"+I2S(" + ABILITY_ARRAY + "[i])+\" \"+I2S(" + ABILITY_ARRAY_SIZE + " - 1))" + JASS_DELIM) +
							"            set i=i+1" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"    " + END_IF,
					"call TriggerRegisterAnyUnitEventBJ(" + TRIGGER_VAR_REPLACE + ", EVENT_PLAYER_HERO_SKILL)");
			// endregion
			// region sync data
			addTriggerConfiguration("SyncData",
					"set " + ARCHON_PREFIX + "Content=BlzGetTriggerSyncData()" + JASS_DELIM +
							"set " + ARCHON_PREFIX + "ContentRead=true",
					"    local integer i=0",
					"    loop" + JASS_DELIM +
							"        exitwhen i>=bj_MAX_PLAYER_SLOTS" + JASS_DELIM +
							"        call BlzTriggerRegisterPlayerSyncEvent(" + TRIGGER_VAR_REPLACE + ",Player(i),\"XTContents\",false)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"    endloop");
			// endregion
		}
		// endregion
		addFunctionConfiguration("CBF", null, null,
				"local timer t=GetExpiredTimer()" + JASS_DELIM +
						"local integer h=GetHandleId(t)" + JASS_DELIM +
						"local string c=LoadStr(" + GENERAL_HASHTABLE + "," + GH_STRING_TIMER_CALLBACK + ",h)" + JASS_DELIM +
						"local real r" + JASS_DELIM +
						"if c!=null then" + JASS_DELIM +
						"call ExecuteFunc(c)" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"if LoadBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_TIMER_PERIODIC + ",h) then" + JASS_DELIM +
						"set r=LoadReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_TIMEOUT + ",h)" + JASS_DELIM +
						"call SaveReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_REMAINING + ",h,r)" + JASS_DELIM +
						"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_TIMER_PAUSED + ",h,false)" + JASS_DELIM +
						"call TimerStart(t,r,false,function " + ARCHON_PREFIX + "CBF)" + JASS_DELIM +
						END_IF); // Callback function
		addFunctionConfiguration("CreateTimer", null, "timer",
				"local integer i=" + ARCHON_PREFIX + "ATL" + JASS_DELIM +
						"set " + ARCHON_PREFIX + "ATL=i+1" + JASS_DELIM +
						"set " + ARCHON_PREFIX + "ATA[i]=CreateTimer()" + JASS_DELIM +
						"return " + ARCHON_PREFIX + "ATA[i]");
		addFunctionConfiguration("TimerStart", "timer t,real r,boolean b,string c", null,
				"local integer h=GetHandleId(t)" + JASS_DELIM +
						"call SaveReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_TIMEOUT + ",h,r)" + JASS_DELIM +
						"call SaveReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_REMAINING + ",h,r)" + JASS_DELIM +
						"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_TIMER_PAUSED + ",h,false)" + JASS_DELIM +
						"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_TIMER_PERIODIC + ",h,b)" + JASS_DELIM +
						"if c!=null then" + JASS_DELIM +
						"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_TIMER_CALLBACK + ",h,c)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call RemoveSavedString(" + GENERAL_HASHTABLE + "," + GH_STRING_TIMER_CALLBACK + ",h)" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"call TimerStart(t,r,false,function " + ARCHON_PREFIX + "CBF)");
		addFunctionConfiguration("TimerGetRemaining", "timer t", "real",
				"local integer h=GetHandleId(t)" + JASS_DELIM +
						"if LoadBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_TIMER_PAUSED + ",h) then" + JASS_DELIM +
						"return LoadReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_REMAINING + ",h)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"return TimerGetRemaining(t)" + JASS_DELIM +
						END_IF);
		addFunctionConfiguration("TimerGetTimeout", "timer t", "real",
				"return LoadReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_TIMEOUT + ",GetHandleId(t))");
		addFunctionConfiguration("TimerGetElapsed", "timer t", "real",
				"return " + ARCHON_PREFIX + "TimerGetTimeout(t)-" + ARCHON_PREFIX + "TimerGetRemaining(t)");
		addFunctionConfiguration("PauseTimer", "timer t", null,
				"local integer h=GetHandleId(t)" + JASS_DELIM +
						"call SaveReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_REMAINING + ",h,TimerGetRemaining(t))" + JASS_DELIM +
						"call TimerStart(t,2340000,false,null)" + JASS_DELIM +
						"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_TIMER_PAUSED + ",h,true)");
		addFunctionConfiguration("ResumeTimer", "timer t", null,
				"local integer h=GetHandleId(t)" + JASS_DELIM +
						"local real r=LoadReal(" + GENERAL_HASHTABLE + "," + GH_REAL_TIMER_REMAINING + ",h)" + JASS_DELIM +
						"local code c=null" + JASS_DELIM +
						"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_TIMER_PAUSED + ",h,false)" + JASS_DELIM +
						"call TimerStart(t,r,false,function " + ARCHON_PREFIX + "CBF)");
		addFunctionConfiguration("StartTimerBJ", "timer t, boolean b, real r", "timer",
				"set bj_lastStartedTimer=t" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "TimerStart(bj_lastStartedTimer,r,b,null)" + JASS_DELIM +
						"return bj_lastStartedTimer");
		addFunctionConfiguration("CreateTimerBJ", "boolean b, real r", "timer",
				"set bj_lastStartedTimer=" + ARCHON_PREFIX + "CreateTimer()" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "TimerStart(bj_lastStartedTimer,r,b,null)" + JASS_DELIM +
						"return bj_lastStartedTimer");
// addFunctionConfiguration("DestroyTimer", "timer t", null,
// 				// todo destroy from hashtable
// 				"call DestroyTimerBJ(t)");
// // todo merge with above function
// addFunctionConfiguration("DestroyTimerBJ", "timer t", null,
// 				"call " + ARCHON_PREFIX + "DestroyTimerBJ(t)");
		addFunctionConfiguration("PauseTimerBJ", "boolean b, timer t", null,
				"if b then" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "PauseTimer(t)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "ResumeTimer(t)" + JASS_DELIM +
						END_IF);
		addFunctionConfiguration("SetReservedLocalHeroButtons", "integer i", null,
				"if GetLocalPlayer()==" + toMainPlayerFunc() + " then" + JASS_DELIM +
						"call SetReservedLocalHeroButtons(i)" + JASS_DELIM +
						END_IF);
		addFunctionConfiguration("ForAllUnits", "code c", null,
				"local integer i=0" + JASS_DELIM +
						"local group g=CreateGroup()" + JASS_DELIM +
						"loop" + JASS_DELIM +
						"        exitwhen i==bj_MAX_PLAYER_SLOTS" + JASS_DELIM +
						"        call GroupEnumUnitsOfPlayer(g,Player(i),null)" + JASS_DELIM +
						"        call ForGroup(g,c)" + JASS_DELIM +
						"        call GroupClear(g)" + JASS_DELIM +
						"        set i=i+1" + JASS_DELIM +
						"endloop" + JASS_DELIM +
						"call DestroyGroup(g)");
		addFunctionConfiguration("SavePausedUnit", "unit u", null,
				"local integer h=GetHandleId(u)" + JASS_DELIM +
						"call SaveReal(" + GENERAL_HASHTABLE + "," + GH_REAL_PAUSED_HP + ",h,GetUnitState(u,UNIT_STATE_LIFE))" + JASS_DELIM +
						"call SaveReal(" + GENERAL_HASHTABLE + "," + GH_REAL_PAUSED_MP + ",h,GetUnitState(u,UNIT_STATE_MANA))" + JASS_DELIM +
						"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED + ",h,IsUnitPaused(u))" + JASS_DELIM +
						"call PauseUnit(u,true)"
		);
		addFunctionConfiguration("EnumSavePausedUnit", null, null,
				"call " + ARCHON_PREFIX + "SavePausedUnit(GetEnumUnit())"
		);
		addFunctionConfiguration("EnumRefreshPausedUnit", null, null,
				"local real r" + JASS_DELIM +
						"local unit u=GetEnumUnit()" + JASS_DELIM +
						"local integer h=GetHandleId(u)" + JASS_DELIM +
						"if HaveSavedReal(" + GENERAL_HASHTABLE + "," + GH_REAL_PAUSED_HP + ",h) then" + JASS_DELIM +
						"call SetUnitState(u,UNIT_STATE_LIFE,LoadReal(" + GENERAL_HASHTABLE + "," + GH_REAL_PAUSED_HP + ",h))" + JASS_DELIM +
						"call SetUnitState(u,UNIT_STATE_MANA,LoadReal(" + GENERAL_HASHTABLE + "," + GH_REAL_PAUSED_MP + ",h))" + JASS_DELIM +
						END_IF
		);
		addFunctionConfiguration("EnumUnpauseUnit", null, null,
				"local real r" + JASS_DELIM +
						"local unit u=GetEnumUnit()" + JASS_DELIM +
						"local integer h=GetHandleId(u)" + JASS_DELIM +
						"if HaveSavedBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED + ",h) then" + JASS_DELIM +
						"call PauseUnit(u,LoadBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED + ",h))" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"call " + ARCHON_PREFIX + "EnumRefreshPausedUnit()"
		);
		addFunctionConfiguration("PauseStartedTimer", "boolean b, timer t", null,
				"if " + ARCHON_PREFIX + "TimerGetRemaining(t)>0 then" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "PauseTimerBJ(b,t)" + JASS_DELIM +
						END_IF);
		addFunctionConfiguration("PauseGame", "boolean b", null,
				"local integer i=0" + JASS_DELIM +
						"local player p" + JASS_DELIM +
						"if " + ARCHON_PREFIX + "GIP!=b then" + JASS_DELIM +
						"if b then" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "ForAllUnits(function " + ARCHON_PREFIX + "EnumSavePausedUnit)" + JASS_DELIM +
						"call EnableTrigger(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PeriodicRefresh)" + JASS_DELIM +
						"call EnableTrigger(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PSU)" + JASS_DELIM +
						//"set " + ARCHON_PREFIX + "RGS=GetGameSpeed()" + JASS_DELIM +
						"call SuspendTimeOfDay(true)" + JASS_DELIM +
						//"call SetGameSpeed(MAP_SPEED_SLOWEST)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call SuspendTimeOfDay(" + ARCHON_PREFIX + "TDS)" + JASS_DELIM +
						//"call SetGameSpeed(" + ARCHON_PREFIX + "RGS)" + JASS_DELIM +
						"call DisableTrigger(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PeriodicRefresh)" + JASS_DELIM +
						"call DisableTrigger(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PSU)" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "ForAllUnits(function " + ARCHON_PREFIX + "EnumUnpauseUnit)" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"loop" + JASS_DELIM +
						"exitwhen i==bj_MAX_PLAYER_SLOTS" + JASS_DELIM +
						"set p=Player(i)" + JASS_DELIM +
						"if GetPlayerController(p)==MAP_CONTROL_COMPUTER then" + JASS_DELIM +
						"if b then" + JASS_DELIM +
						"call PauseCompAI(p,true)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call PauseCompAI(p,LoadBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED_PLAYER + ",i))" + JASS_DELIM +
						END_IF + JASS_DELIM +
						END_IF + JASS_DELIM +
						"set i=i+1" + JASS_DELIM +
						"endloop" + JASS_DELIM +
						"set i=0" + JASS_DELIM +
						"loop" + JASS_DELIM +
						"exitwhen i==" + ARCHON_PREFIX + "ATL" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "PauseStartedTimer(b," + ARCHON_PREFIX + "ATA[i])" + JASS_DELIM +
						"set i=i+1" + JASS_DELIM +
						"endloop" + JASS_DELIM +
						"set " + ARCHON_PREFIX + "GIP=b" + JASS_DELIM +
						END_IF);
		// pause until game load
		addFunctionConfiguration("PUGL", null, null,
				bIf(bNot(ARCHON_PREFIX + "GIP"),
						bSet(ARCHON_PREFIX + "LUP", "true") + JASS_DELIM +
								bCall(ARCHON_PREFIX + "PauseGame", "true")));
		addFunctionConfiguration("HandleSLM", "string s, string l, boolean b", null,
				"    loop" + JASS_DELIM +
						"        exitwhen " + ARCHON_PREFIX + "GCS==0" + JASS_DELIM +
						"        call TriggerSleepAction(0.01)" + JASS_DELIM +
						"    endloop" + JASS_DELIM +
						"call DisplayTimedTextToPlayer(GetLocalPlayer(),0,0,0,GetLocalizedString(s)+\": |cffffcc00\"+l+\"|r\")" + JASS_DELIM +
						"if b then" + JASS_DELIM +
						"set " + ARCHON_PREFIX + "LRD=true" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "PUGL()" + JASS_DELIM +
						END_IF);
		addFunctionConfiguration("ChangeLevel", "string s, boolean b", null,
				"local boolean h=HaveSavedString(" + GENERAL_HASHTABLE + "," + GH_STRING_CAMPAIGN_BUTTON_NAME + ",StringHash(s))" + JASS_DELIM +
						"if h then" + JASS_DELIM +
						"set s=GetLocalizedString(LoadStr(" + GENERAL_HASHTABLE + "," + GH_STRING_CAMPAIGN_BUTTON_NAME + ",StringHash(s)))" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"call " + ARCHON_PREFIX + "HandleSLM(\"CREATE\",s,true)");
		addFunctionConfiguration("LoadGame", "string s, boolean b", null,
				"call " + ARCHON_PREFIX + "HandleSLM(\"LOAD_GAME\",s,true)");
		addFunctionConfiguration("SaveGame", "string s", null,
				"call CinematicModeExBJ(false," + FORCE_NAME + ",0)" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "HandleSLM(\"SAVE_GAME\",s,false)");
		addFunctionConfiguration("LoadGameBJ", "string s, boolean b", null,
				"call " + ARCHON_PREFIX + "LoadGame(s,b)");
		addFunctionConfiguration("SaveAndChangeLevelBJ", "string s, string l, boolean b", null,
				"call " + ARCHON_PREFIX + "SaveGame(s)" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "ChangeLevel(l,b)");
		addFunctionConfiguration("SaveAndLoadGameBJ", "string s, string l, boolean b", null,
				"call " + ARCHON_PREFIX + "SaveGame(s)" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "LoadGame(l,b)");
		addFunctionConfiguration("CopySaveGame", "string s, string d", "boolean",
				"return true");
		addFunctionConfiguration("SaveGameExists", "string s", "boolean",
				"return false");
		addFunctionConfiguration("CopySaveGameBJ", "string s, string d", "boolean",
				"return true");
	}

	public static String getNewPath(String oldPath) {
		return XT87Utils.PATH_PREFIX + "/" + oldPath;
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
		triggersAdded = false;
		mainStartAdded = false;
		insideConfig = false;
		insideFunction = null;
		cacheFieldMap = new HashMap<>();
	}

//	private int getCacheFieldIndex(String key) {
//		switch (key) {
//			case CACHE_XP_SUFFIX:
//				return 2;
//			case CACHE_STR_SUFFIX:
//				return 3;
//			case CACHE_AGI_SUFFIX:
//				return 4;
//			case CACHE_INT_SUFFIX:
//				return 5;
//			case CACHE_UNSPEND_POINTS_SUFFIX:
//				return 6;
//			case CACHE_HEALTH_MAX_SUFFIX:
//				return 7;
//			case CACHE_MANA_MAX_SUFFIX:
//				return 8;
//			case CACHE_ATTACK_DAMAGE_MIN_SUFFIX:
//				return 9;
//			case CACHE_ATTACK_DAMAGE_MAX_SUFFIX:
//				return 10;
//			case CACHE_ARMOR_SUFFIX:
//				return 11;
//			case CACHE_ABILITY_COUNT_SUFFIX:
//				return 12;
//			default:
//				return -1;
//		}
//	}

	public String toMainPlayerFunc() {
		return toPlayerFunc(mainPlayer.getNum());
	}

	public Integer getPlayerIdFromPlayerFunc(String s) {
		if (!s.contains(PLAYER_FUNC))
			return null;
		s = s.replace(PLAYER_FUNC, "").replace("(", "").replace(")", "");
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public String toCallString(String call, String[] params, int replacedParamIndex, String replacementParam) {
		StringBuilder result = new StringBuilder("call " + call + "(");
		for (int i = 0; i < params.length; i++) {
			if (i != 0)
				result.append(",");
			if (replacementParam != null && i == replacedParamIndex)
				result.append(replacementParam);
			else
				result.append(params[i]);
		}
		return result + ")";
	}

	public String toCallString(String call, String[] params) {
		return toCallString(call, params, 0, null);
	}

	public void appendCallWithReplacement(StringBuffer sb, String call, String[] params, CoopCallAdjustments.AdjustmentDetails ad) {
		for (int index : ad.getParameterIndices()) {
			String referencedPlayer = params[index];
			Integer referencedPlayerId = getPlayerIdFromPlayerFunc(referencedPlayer);
			boolean runtimeResolve = referencedPlayerId == null;

			if (runtimeResolve)
				append(sb, "    if (" + referencedPlayer + ")==" + toMainPlayerFunc() + " then");
			else if (referencedPlayerId != mainPlayer.getNum())
				continue;

			if (ad.isLocal)
				append(sb, toCallString(call, params, index, "GetLocalPlayer()"));
			else
				secondaryPlayers.stream().map(x -> toCallString(call, params, index, toPlayerFunc(x.getNum()))).forEach(s -> append(sb, s));

			if (runtimeResolve)
				append(sb, "    " + END_IF);
		}
	}

	private enum ConversionFunctionType {
		STRING_TO_INT,
		STRING_TO_REAL,
		INT_TO_STRING,
		REAL_TO_STRING
	}

	private static String getConversionFunc(ConversionFunctionType funcType) {
		return switch (funcType) {
			case STRING_TO_INT -> WITH_FULL_ENCODING ? (ARCHON_PREFIX + "S62I") : "S2I";
			case STRING_TO_REAL -> WITH_FULL_ENCODING ? (ARCHON_PREFIX + "S62R") : "S2R";
			case INT_TO_STRING -> WITH_FULL_ENCODING ? (ARCHON_PREFIX + "IS62") : "I2S";
			case REAL_TO_STRING -> WITH_FULL_ENCODING ? (ARCHON_PREFIX + "RS62") : "R2S";
		};
	}

	private static String moduloFunc(String n, String b) {
		return n + "-(" + n + "/" + b + ")*" + b;
	}

	private static String getCacheNameFunc(String cache) {
		return "GetStoredString(" + cache + "," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")";
	}

	private String loopOverPlayers(String var, String... code) {
		return "loop" + JASS_DELIM +
				"exitwhen " + var + ">=" + allPlayerNumbers.size() + JASS_DELIM +
				String.join(JASS_DELIM, code) + JASS_DELIM +
				"set " + var + "=" + var + "+1" + JASS_DELIM +
				"endloop";
	}

	@Override
	public void onEnterGlobals(StringBuffer sb) {
		append(sb, "timer array " + ARCHON_PREFIX + "ATA", // All Timer Array
				"integer " + ARCHON_PREFIX + "ATL=0"); // All Timer Array Length
	}

	@Override
	public void onExitGlobals(StringBuffer sb) {
		if (WITH_CUSTOM_GAMECACHE) {
			// region FileIO library
			append(sb, "function " + FILE_LIBRARY_PREFIX + "open takes string f returns integer", // filename f
					"            local integer t=" + FILE_LIBRARY_PREFIX + "LS[0]", // this t
					"            if t==0 then",
					"                set t=" + FILE_LIBRARY_PREFIX + "CT+1",
					"                set " + FILE_LIBRARY_PREFIX + "CT=t",
					"            else",
					"                set " + FILE_LIBRARY_PREFIX + "LS[0]=" + FILE_LIBRARY_PREFIX + "LS[t]",
					"            endif",
					"            set " + FILE_LIBRARY_PREFIX + "FN[t]=f",
					"            set " + FILE_LIBRARY_PREFIX + "BF[t]=null",
					"            return t",
					END_FUNCTION,
					// This is used to detect invalid characters which aren't supported in preload files.
					"        function " + FILE_LIBRARY_PREFIX + "write takes integer t,string s returns integer", // this t, contents s
					"            local integer i=0",
					"            local integer c=0",
					"            local integer l=StringLength(s)",
					"            local integer v=0", // lev v
					"            local string p=\"-\"", // prefix p
					"            local string k", // chunk k
					"            set " + FILE_LIBRARY_PREFIX + "BF[t]=null",
					// Check if the string is empty. If null,the s will be cleared.
					"            if (s==\"\") then",
					"                set l=l+1",
					"            endif",
					// Begin to generate the file
					"            call PreloadGenClear()",
					"            call PreloadGenStart()",
					"            loop",
					"                exitwhen i>=l",
					"                set v=0",
					"                set k=SubString(s,i,i+" + FILE_LIBRARY_PRELOAD_LIMIT + ")",
					"                call Preload(\"\\\" )\\ncall BlzSetAbilityTooltip(\"+I2S(" + FILE_LIBRARY_PREFIX + "AL[c])+\",\\\"\"+p+k+\"\\\",\"+I2S(v)+\")\\n//\")",
					"                set i=i+" + FILE_LIBRARY_PRELOAD_LIMIT,
					"                set c=c+1",
					"            endloop",
					"            call Preload(\"\\\" )\\nendfunction\\nfunction a takes nothing returns nothing\\n //\")",
					"            call PreloadGenEnd(" + FILE_LIBRARY_PREFIX + "FN[t])",
					"            return t",
					END_FUNCTION,
					"        function " + FILE_LIBRARY_PREFIX + "clear takes integer t returns integer",
					"            return " + FILE_LIBRARY_PREFIX + "write(t,null)",
					END_FUNCTION,
					"        function " + FILE_LIBRARY_PREFIX + "readPreload takes integer t returns string",
					"            local integer i=0",
					"            local integer v=0",
					"            local string array g", // original g
					"            local string k=\"\"",
					"            local string o=\"\"", // output o
					"            loop",
					"                exitwhen i==" + FILE_LIBRARY_ABILITY_COUNT,
					"                set g[i]=BlzGetAbilityTooltip(" + FILE_LIBRARY_PREFIX + "AL[i],0)",
					"                set i=i+1",
					"            endloop",
					// Execute the preload file
					"            call Preloader(" + FILE_LIBRARY_PREFIX + "FN[t])",
					// Read the output"
					"            set i=0",
					"            loop",
					"                exitwhen i==" + FILE_LIBRARY_ABILITY_COUNT,
					"                ",
					"                set v=0",
					// Read from ability index 1 instead of 0 if
					// backwards compatability is enabled
					// Make sure the tooltip has changed
					"                set k=BlzGetAbilityTooltip(" + FILE_LIBRARY_PREFIX + "AL[i],v)",
					"                if k==g[i] then",
					"                    if i==0 and o==\"\" then",
					"                        return null", // empty file
					"                    endif",
					"                    return o",
					END_IF,
					// Check if the file is an empty string or null
					"                    if i==0 then",
					"                        if SubString(k,0,1)!=\"-\" then",
					"                            return null", // empty file
					"                        endif",
					"                        set k=SubString(k,1,StringLength(k))",
					END_IF,
					// Remove the prefix
					"                if i>0 then",
					"                    set k=SubString(k,1,StringLength(k))",
					"                endif",
					// Restore the tooltip and append the chunk
					"                call BlzSetAbilityTooltip(" + FILE_LIBRARY_PREFIX + "AL[i],g[i],v)",
					"                set o=o+k",
					"                set i=i+1",
					"            endloop",
					"            return o",
					END_FUNCTION,
					"        function " + FILE_LIBRARY_PREFIX + "close takes integer t returns nothing",
					"            if " + FILE_LIBRARY_PREFIX + "BF[t]!=null then",
					"                call " + FILE_LIBRARY_PREFIX + "write(t," + FILE_LIBRARY_PREFIX + "readPreload(t)+" + FILE_LIBRARY_PREFIX + "BF[t])",
					"                set " + FILE_LIBRARY_PREFIX + "BF[t]=null",
					"            endif",
					"            set " + FILE_LIBRARY_PREFIX + "LS[t]=" + FILE_LIBRARY_PREFIX + "LS[0]",
					"            set " + FILE_LIBRARY_PREFIX + "LS[0]=t",
					END_FUNCTION,
					"        function " + FILE_LIBRARY_PREFIX + "readEx takes integer t,boolean c returns string", // close c
					"            local string o=" + FILE_LIBRARY_PREFIX + "readPreload(t)",
					"            local string b=" + FILE_LIBRARY_PREFIX + "BF[t]", // buf b
					"            if c then",
					"                call " + FILE_LIBRARY_PREFIX + "close(t)",
					"            endif",
					"            if o==null then",
					"                return b",
					"            endif",
					"            if b!=null then",
					"                set o=o+b",
					"            endif",
					"            return o",
					END_FUNCTION,
//							"        function " + FILE_LIBRARY_PREFIX + "read takes integer t returns string",
//							"            return " + FILE_LIBRARY_PREFIX + "readEx(t,false)",
//							END_FUNCTION,
//							"        function " + FILE_LIBRARY_PREFIX + "readAndClose takes integer t returns string",
//							"            return " + FILE_LIBRARY_PREFIX + "readEx(t,true)",
//							END_FUNCTION,
//							"        function " + FILE_LIBRARY_PREFIX + "appendBuffer takes integer t,string s returns integer",
//							"            set " + FILE_LIBRARY_PREFIX + "BF[t]=" + FILE_LIBRARY_PREFIX + "BF[t]+s",
//							"            return t",
//							END_FUNCTION,
//							"        function " + FILE_LIBRARY_PREFIX + "readBuffer takes integer t returns string",
//							"            return " + FILE_LIBRARY_PREFIX + "BF[t]",
//							END_FUNCTION,
//							"        function " + FILE_LIBRARY_PREFIX + "writeBuffer takes integer t,string s returns nothing",
//							"            set " + FILE_LIBRARY_PREFIX + "BF[t]=s",
//							END_FUNCTION,
					"        function " + FILE_LIBRARY_PREFIX + "create takes string f returns integer",
					"            return " + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(f),\"\")",
					END_FUNCTION,
					//Implemented from module FileIO__FileInit:
					"        function " + FILE_LIBRARY_PREFIX + "Init takes nothing returns nothing",
					// We can't use a single ability with multiple levels because
					// tooltips return the first level's value if the value hasn't
					// been set. This way we don't need to edit any object editor data.
					"            set " + FILE_LIBRARY_PREFIX + "AL[0]='Amls'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[1]='Aroc'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[2]='Amic'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[3]='Amil'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[4]='Aclf'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[5]='Acmg'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[6]='Adef'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[7]='Adis'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[8]='Afbt'",
					"            set " + FILE_LIBRARY_PREFIX + "AL[9]='Afbk'",
					"            set " + FILE_LIBRARY_PREFIX + "ReadEnabled=(" + FILE_LIBRARY_PREFIX + "readEx((" + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(\"FileTester.pld\"),\"" + FILE_IO_PREFIX + "\")),true))==\"" + FILE_IO_PREFIX + "\"",
					END_FUNCTION,
					"    function " + FILE_IO_PREFIX + "Write takes string f,string s returns nothing",
					"        call " + FILE_LIBRARY_PREFIX + "close(" + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(f),s))",
					END_FUNCTION,
					"    function " + FILE_IO_PREFIX + "Read takes string f returns string",
					"        return " + FILE_LIBRARY_PREFIX + "readEx(" + FILE_LIBRARY_PREFIX + "open(f),true)",
					END_FUNCTION);
			// endregion
			// region selection functions
			append(sb, "function " + ARCHON_PREFIX + "ShowSelection takes boolean b returns nothing",
					"local integer c",
					"local integer i",
					"local integer j=0",
					"local real a=0",
					"if b then",
					"set a=1",
					END_IF,
					DebugOnly("call BJDebugMsg(\"ShowSelection \"+R2S(a))" + JASS_DELIM) +
							loopOverPlayers("j",
									"set c=LoadInteger(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],0)" + JASS_DELIM +
											"set i=1",
									"loop",
									"exitwhen i>c",
									"call BlzSetSpecialEffectScale(LoadEffectHandle(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],i),a)",
									"set i=i+1",
									"endloop" + JASS_DELIM),
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "ClearSelection takes nothing returns nothing",
					"local integer c",
					"local integer i",
					"local integer j=0",
					loopOverPlayers("j",
							"set c=LoadInteger(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],0)",
							"set i=1",
							"loop",
							"exitwhen i>c",
							"call DestroyEffect(LoadEffectHandle(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],i))",
							"set i=i+1",
							"endloop",
							"call SaveInteger(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],0,0)"),
					"call ClearSelection()",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "SelectUnitSingle takes unit u returns nothing",
					"call " + ARCHON_PREFIX + "ClearSelection()",
					"call SelectUnit(u,true)",
					END_FUNCTION);
//					append(sb, "function " + ARCHON_PREFIX + "SelectGroupBJ takes group g returns nothing",
//							"call " + ARCHON_PREFIX + "ClearSelection()",
//							"call ForGroup(g,function SelectGroupBJEnum)",
//							END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "ClearSelectionForPlayer takes player p returns nothing",
					"if p==" + toMainPlayerFunc() + " then",
					"call " + ARCHON_PREFIX + "ClearSelection()",
					END_IF,
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "EnableUserControl takes boolean b returns nothing",
					"call " + ARCHON_PREFIX + "ShowSelection(b)",
					"call EnableUserControl(b)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "SetUserControlForceOn takes force f returns nothing",
					"if IsPlayerInForce(" + toMainPlayerFunc() + ",f) then",
					"call " + ARCHON_PREFIX + "EnableUserControl(true)",
					END_IF,
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "SetUserControlForceOff takes force f returns nothing",
					"if IsPlayerInForce(" + toMainPlayerFunc() + ",f) then",
					"call " + ARCHON_PREFIX + "EnableUserControl(false)",
					END_IF,
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "CinematicModeExBJ takes boolean b, force f, real t returns nothing",
					"if IsPlayerInForce(" + toMainPlayerFunc() + ",f) then",
					"call " + ARCHON_PREFIX + "ShowSelection(not b)",
					END_IF,
					"call CinematicModeExBJ(b,f,t)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "CinematicModeBJ takes boolean b, force f returns nothing",
					"call " + ARCHON_PREFIX + "CinematicModeExBJ(b,f,bj_CINEMODE_INTERFACEFADE)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "PauseGameOn takes nothing returns nothing",
					"call " + ARCHON_PREFIX + "PauseGame(true)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "PauseGameOff takes nothing returns nothing",
					"call " + ARCHON_PREFIX + "PauseGame(false)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "SuspendTimeOfDay takes boolean b returns nothing",
					"set " + ARCHON_PREFIX + "TDS=b",
					"call SuspendTimeOfDay(b)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "UseTimeOfDayBJ takes boolean b returns nothing",
					"call " + ARCHON_PREFIX + "SuspendTimeOfDay(not b)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "PauseCompAI takes player p, boolean b returns nothing",
					"call PauseCompAI(p,b)",
					"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED_PLAYER + ",GetPlayerId(p),b)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "PauseAllUnitsBJ takes boolean b returns nothing",
					"local integer i=0",
					"call PauseAllUnitsBJ(b)",
					"loop",
					"exitwhen i==bj_MAX_PLAYER_SLOTS",
					"if GetPlayerController(Player(i))==MAP_CONTROL_COMPUTER then",
					"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED_PLAYER + ",i,b)",
					END_IF,
					"set i=i+1",
					"endloop",
					END_FUNCTION);
			// endregion
			// region custom functions
			append(sb, "function " + ARCHON_PREFIX + "WatchSkill takes integer a returns boolean",
					"    if not LoadBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_HAS_ABILITY + ",a) then",
					"        set " + ABILITY_ARRAY + "[" + ABILITY_ARRAY_SIZE + "]=a",
					"        call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_HAS_ABILITY + ",a,true)",
					"        set " + ABILITY_ARRAY_SIZE + "=" + ABILITY_ARRAY_SIZE + "+1",
					"        return true",
					"    else",
					"        return false",
					END_IF,
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "S2B takes string s returns boolean",
					"    if s==\"t\" then",
					"        return true",
					"    endif",
					"    return false",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "B2S takes boolean b returns string",
					"    if b then",
					"        return \"t\"",
					END_IF,
					"    return \"f\"",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "IS62 takes integer n returns string",
					"    local string result=\"\"",
					"    local integer remainder",
					"    local integer b=62",
					"    local integer d",
					"    local string s=\"\"",
					"if n<0 then",
					"set s=\"-\"",
					"set n=-n",
					END_IF,
					"    loop",
					"        exitwhen n<=0",
					"        set d=" + moduloFunc("n", "b"),
					"        set result=SubString(\"" + XT87Utils.BASE62_CHARS + "\",d,d+1)+result",
					"        set n=n/b",
					"    endloop",
					"    return s+result",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "SS62 takes string s returns string",
					"if s==null then",
					"return \"null\"",
					END_IF,
					"return \"s\"+s",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "S62I takes string s returns integer",
					"    local integer result=0",
					"    local integer b=62",
					"    local string d",
					"    local integer i",
					"    local integer n=1",
					"    if SubString(s,0,1)==\"-\" then",
					"set n=-1",
					"set s=SubString(s,1,StringLength(s))",
					END_IF,
					"    loop",
					"        exitwhen s==\"\"",
					"        set d=SubString(s,0,1)",
					"        set i=0",
					"        loop",
					"            exitwhen SubString(\"" + XT87Utils.BASE62_CHARS + "\",i,i+1)==d",
					"            set i=i+1",
					"            if i>=b then",
					"                return 0",
					"            endif",
					"        endloop",
					"        set result=result*b+i",
					"        set s=SubString(s,1,StringLength(s))",
					"    endloop",
					"    return n*result",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "S62S takes string s returns string",
					"if SubString(s,0,1)==\"s\" then",
					"return SubString(s,1,StringLength(s))",
					END_IF,
					"return null",
					END_FUNCTION);
			if (WITH_FULL_ENCODING)
				append(sb, "function " + ARCHON_PREFIX + "RS62 takes real r returns string",
						"    local string s=R2S(r)",
						"    local integer i=0",
						"    local integer len=StringLength(s)",
						"    loop",
						"    exitwhen i>=len",
						"    if SubString(s,i,i+1)==\".\" then",
						"    return " + ARCHON_PREFIX + "IS62(S2I(SubString(s,0,i)))+\".\"+" + ARCHON_PREFIX + "IS62(S2I(SubString(s,i+1,len)))",
						END_IF,
						"set i=i+1",
						"endloop",
						"    return " + ARCHON_PREFIX + "IS62(0)+\".\"+" + ARCHON_PREFIX + "IS62(0)",
						END_FUNCTION,
						"function " + ARCHON_PREFIX + "S62R takes string s returns real",
						"    local integer i=0",
						"    local integer len=StringLength(s)",
						"    loop",
						"    exitwhen i==len",
						"    if SubString(s,i,i+1)==\".\" then",
						"    return S2R(I2S(" + ARCHON_PREFIX + "S62I(SubString(s,0,i)))+\".\"+I2S(" + ARCHON_PREFIX + "S62I(SubString(s,i+1,len))))",
						END_IF,
						"set i=i+1",
						"endloop",
						"    return 0.000",
						END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "WriteString takes string cn,string title,string v,string fK returns nothing",
					"local string p=\"XT87CampaignSplitter\\\\\"+cn+\"\\\\\"+title+\".pld\"",
					"    call " + FILE_IO_PREFIX + "Write(p,v+\"\\n\"+" + ARCHON_PREFIX + "IS62(StringHash(v+\"|\"+p+\"|\"+fK)))",
					END_FUNCTION);
			// remove last line
			append(sb, "function " + ARCHON_PREFIX + "RLL takes string content returns string",
					"    local integer i = StringLength(content) - 1",
					"    loop",
					"        exitwhen i < 0",
					"        if SubString(content, i, i + 1) == \"\\n\" then",
					"            return SubString(content, 0, i)",
					"        endif",
					"        set i = i - 1",
					"    endloop",
					"    return content",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "GetContent takes string cn,string title,string fK returns string",
					//"    local integer i=0",
					//"    local integer j=0",
					"    local integer len",
					"    local string content",
					"    local string char",
					"    local string buffer=\"\"",
					"local string p=\"XT87CampaignSplitter\\\\\"+cn+\"\\\\\"+title+\".pld\"",
					"    local unit u",
					"    loop",
					"        exitwhen not " + ARCHON_PREFIX + "RdLock",
					"        call TriggerSleepAction(0.01)",
					"    endloop",
//					"if not " + ARCHON_PREFIX + "CIP then",
//					"set " + ARCHON_PREFIX + "CIP=true",
//					"call " + ARCHON_PREFIX + "PauseGame(true)",
//					END_IF,
					"set " + ARCHON_PREFIX + "RdLock=true",
					//"    if GetLocalPlayer()==" + toMainPlayerFunc() + " then",
					"set content=" + FILE_LIBRARY_PREFIX + "readEx(" + FILE_LIBRARY_PREFIX + "open(p),true)",
					"set content=" + ARCHON_PREFIX + "RLL(content)",
					"if content==null then",
					"set content=\"\"",
					END_IF,
					//"        set len=StringLength(content)",
					// todo add gamecache flush index and mission key flush index to checksum
//					"    loop",
//					"    exitwhen i>=len",
//					"    if SubString(content,i,i+1)==\"\\n\" then",
//					"set j=i",
//					END_IF,
//					"set i=i+1",
//					"endloop",
					// compare checksums
//					(COMPARE_CHECKSUMS ?
//							("    if StringHash(SubString(content,0,j)+\"|\"+p+\"|\"+fK)!=" + ARCHON_PREFIX + "S62I(SubString(content,j+1,len)) then",
//									"        set content=\"\"",
//									END_IF + JASS_DELIM) :
//							"") +
//					DebugOnly("call BJDebugMsg(\">\"+content+\"<\")" + JASS_DELIM) +
//					"set u=CreateUnit(Player(PLAYER_NEUTRAL_PASSIVE),'hfoo',0,0,0)",
//					"call BlzSetUnitName(u,\"c\"+content)",
//					"call StoreUnit(" + SYNC_CACHE + ",\"m\",\"k\",u)",
//					"call RemoveUnit(u)",
//					"call SyncStoredUnit("+ SYNC_CACHE + ",\"m\",\"k\")",
					//END_IF,
					//"call TriggerSyncReady()",
					//"set u=RestoreUnit("+ SYNC_CACHE + ",\"m\",\"k\",Player(PLAYER_NEUTRAL_PASSIVE),0,0,0)",
					//"set content=GetUnitName(u)",
					//"set content=SubString(content,1,StringLength(content))",
					//"call RemoveUnit(u)",
//					"set len=StringLength(content)",
//					"set j=0", // j is now current line index
//					"set i=0",
//					"    if len<=0 then",
//					"        set XT87ReadLines[0]=null",
//					"    else",
//					"        loop",
//					"            exitwhen i>len",
//					"            set char=SubString(content,i,i+1)",
//					"            if char==\"\\n\" then",
//					"                set XT87ReadLines[j]=buffer",
//					"                set j=j+1",
//					"                set buffer=\"\"",
//					"            else",
//					"                set buffer=buffer+char",
//					"            endif",
//					"        set i=i+1",
//					"        endloop",
//					"        set XT87ReadLines[j]=buffer",
					"set " + ARCHON_PREFIX + "RdLock=false",
					"return content",
//					"set XT87ReadLinesAreRead=true",
//					"    loop",
//					"        exitwhen XT87ReadLinesAreRead",
//					"        call TriggerSleepAction(0.01)",
//					"    endloop",
					END_FUNCTION);

			append(sb, "function " + ARCHON_PREFIX + "RdWt takes boolean b returns nothing",
					"loop",
					"exitwhen not XT87ASRdWaitLock",
					"call TriggerSleepAction(0.01)",
					"endloop",
					"set " + ARCHON_PREFIX + "RdWaitLock=true",
					"if b then",
					"set " + ARCHON_PREFIX + "RdWait=" + ARCHON_PREFIX + "RdWait+1",
					"set " + ARCHON_PREFIX + "RdWaitLock=false",
					"if not " + ARCHON_PREFIX + "CIP then",
					"set " + ARCHON_PREFIX + "CIP=true",
					XT87Utils.fadeCall(true),
					"if not " + ARCHON_PREFIX + "GIP then",
					"call " + ARCHON_PREFIX + "PauseGame(true)",
					END_IF,
					END_IF,
					"else",
					"set " + ARCHON_PREFIX + "RdWait=" + ARCHON_PREFIX + "RdWait-1",
					"set " + ARCHON_PREFIX + "RdWaitLock=false",
					"if " + ARCHON_PREFIX + "RdWait==0 and " + ARCHON_PREFIX + "CIP then\n" +
							"set " + ARCHON_PREFIX + "CIP=false\n" +
							XT87Utils.fadeCall(false),
					"call " + ARCHON_PREFIX + "PauseGame(false)",
					END_IF,
					END_IF,
					END_FUNCTION);
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("Integer", getConversionFunc(ConversionFunctionType.STRING_TO_INT), "0"),
					new Triple<>("Real", getConversionFunc(ConversionFunctionType.STRING_TO_REAL), "0"),
					new Triple<>("Boolean", ARCHON_PREFIX + "S2B", "false")
			)) {
				String type = o.a.toLowerCase(Locale.ROOT);
				append(sb, BlockUtils.bDef(SYNC_FUNC_NAME + o.a, type + " v", type,
						"local string c=\"c\"",
						"local string t=\"t\"",
						"call Store" + o.a + "(" + SYNC_CACHE + ",c,t,v)",
						"call TriggerSyncStart()",
						"call SyncStored" + o.a + "(" + SYNC_CACHE + ",c,t)",
						"call TriggerSyncReady()",
						"return GetStored" + o.a + "(" + SYNC_CACHE + ",c,t)"));
				append(sb, "function " + ARCHON_PREFIX + "Read" + o.a + " takes string cn,string t,string fK returns " + type,
						"local " + type + " v",
						"if GetPlayerSlotState(" + toMainPlayerFunc() + ")!=PLAYER_SLOT_STATE_PLAYING then",
						"return " + o.c,
						END_IF,
						"call " + ARCHON_PREFIX + "RdWt(true)",
						"set v=" + SYNC_FUNC_NAME + o.a + "(" + o.b + "(" + ARCHON_PREFIX + "GetContent(cn,t,fK)))",
						"call " + ARCHON_PREFIX + "RdWt(false)",
						"return v",
						END_FUNCTION);
			}
			append(sb, "function " + ARCHON_PREFIX + "ReadString takes string cn,string t,string fK returns string",
					"local string s",
					"    if GetPlayerSlotState(" + toMainPlayerFunc() + ")!=PLAYER_SLOT_STATE_PLAYING then",
					"        return null",
					END_IF,
					"call " + ARCHON_PREFIX + "RdWt(true)",
					"set " + ARCHON_PREFIX + "ContentRead=false",
					"if GetLocalPlayer()==" + toMainPlayerFunc() + " then",
					"set s=" + ARCHON_PREFIX + "S62S(" + ARCHON_PREFIX + "GetContent(cn,t,fK))",
					"if s==null then",
					"set s=\"\"",
					END_IF,
					"call BlzSendSyncData(\"XTContents\",s)",
					END_IF,
					"loop",
					"exitwhen " + ARCHON_PREFIX + "ContentRead",
					"call TriggerSleepAction(0.01)",
					"endloop",
					//"set XT87ReadLinesLock=false",
					//"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1",
//					"if XT87ReadLinesWaiting==0 and " + ARCHON_PREFIX + "CIP then",
//					"set " + ARCHON_PREFIX + "CIP=false",
//					"call " + ARCHON_PREFIX + "PauseGame(false)",
//					END_IF,
					"set s=" + ARCHON_PREFIX + "Content",
					"call " + ARCHON_PREFIX + "RdWt(false)",
					"return s",
					END_FUNCTION);
			// endregion
			// region button add functions
			append(sb, "function " + ARCHON_PREFIX + "DialogAddButton takes dialog d,string s,integer h returns button",
					"local button b=DialogAddButton(d,s,h)",
					"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(b),s)",
					"return b",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "DialogAddQuitButton takes dialog d,boolean t,string s,integer h returns button",
					"local button b=DialogAddQuitButton(d,t,s,h)",
					"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(b),s)",
					"return b",
					END_FUNCTION);
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("", "", "0"),
					new Triple<>("WithHotkey", ",integer h", "h")
			))
				append(sb, "function " + ARCHON_PREFIX + "DialogAddButton" + o.a + "BJ takes dialog d,string s" + o.b + " returns button",
						"set bj_lastCreatedButton=DialogAddButton(d,s," + o.c + ")",
						"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(bj_lastCreatedButton),s)",
						"return bj_lastCreatedButton",
						END_FUNCTION);
			// endregion
			// region dialog display
			append(sb, "function " + ARCHON_PREFIX + "DialogDisplay takes player p, dialog d, boolean b returns nothing",
					"local integer i=1",
					"call DialogDisplay(p,d,b)",
					"if p==" + toMainPlayerFunc() + " then",
					loopOverPlayers("i", "call DialogDisplay(Player(" + PLAYER_ID_ARRAY + "[i]),d,b)"),
					END_IF,
					// todo dont set DIP to true if already paused
					"if " + ARCHON_PREFIX + "DIP!=b then",
					"set " + ARCHON_PREFIX + "DIP=b",
					"call " + ARCHON_PREFIX + "PauseGame(b)",
					END_IF,
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "DialogDisplayBJ takes boolean b, dialog d, player p returns nothing",
					"call " + ARCHON_PREFIX + "DialogDisplay(p,d,b)",
					END_FUNCTION);
			// endregion
			// region DoFlushIndex function
			// uses m2 and flush prefixed k2
			append(sb, "function " + ARCHON_PREFIX + "DoFlushIndex takes gamecache q,string cn,string m,string k,integer c returns integer",
					"return 0",
//					"    local integer i=c",
//					"if HaveSavedInteger(q,m,k) then",
//					"    set i=GetStoredInteger(q,m,k)",
//					"    if c!=0 then",
//					"set i=i+c",
//					"call StoreInteger(q,m,k,i)",
//					END_IF,
//					"return i",
//					END_IF,
//					"set i=" + ARCHON_PREFIX + "ReadInteger(cn,m+\"\\\\\"+k,null)+c",
//					"call StoreInteger(q,m,k,i)",
					//"if i!=0 then",
					//"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_INTEGER_KEY + "\",m2,k2)",
					//END_IF,
//					"return i",
					END_FUNCTION);
			// endregion
			// region StoreFlushIndex function
			// uses m2 and flush prefixed k2
//					append(sb, "function " + ARCHON_PREFIX + "StoreFlushIndex takes gamecache q,string m,string k returns nothing",
//							"    local string s",
//							"    local integer i=0",
//							"    local string cn=" + getCacheNameFunc("q"),
//							"if HaveSavedInteger(q,m,k) then",
//							"    set i=GetStoredInteger(q,m,k)",
//							"    if c!=0 then",
//							"set i=i+c",
//							"call StoreInteger(q,m,k,i)",
//							END_IF,
//							"return i",
//							END_IF,
//							"set s=" + ARCHON_PREFIX + "ReadString(cn,m+\"\\\\\"+k,null)",
//							"if s!=null then",
//							"   set i=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(s)+c",
//							END_IF,
//							"call StoreInteger(q,m,k,i)",
//							"return i",
//							END_FUNCTION);
			// endregion
			// region GameCache functions
			// region game cache basics natives
			append(sb, "function " + ARCHON_PREFIX + "InitGameCache takes string f returns gamecache",
					"    local gamecache q=InitGameCache(\"xt87\"+f)",
					DebugOnly("call BJDebugMsg(\"InitGameCache \"+f)" + JASS_DELIM) +
							"    call StoreString(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ",f)",
//		todo replace first mission key CACHE_CAMPAIGNFILE_KEY with CACHE_CAMPAIGN_MKEY
//						"    if not " + ARCHON_PREFIX + "HaveStoredInteger(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + ",StringHash(\"" + CACHE_FLUSH_KEY + "\")) then" +
//						"    call StoreInteger(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + ",StringHash(\"" + CACHE_FLUSH_KEY + "\"),0)",
//						END_IF,
					"    return q",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "SaveGameCache takes gamecache q returns boolean",
					"    local integer i",
					"    local integer n",
					"    local integer j",
					"    local integer l",
					"    local integer id",
					"    local integer it",
					"    local boolean h",
					"    local string m2",
					"    local string k2",
					"    local string uD",
					"    local string cn=" + getCacheNameFunc("q"),
					"    set " + ARCHON_PREFIX + "GCS=" + ARCHON_PREFIX + "GCS+1",
					DebugOnly("call BJDebugMsg(\"SaveGameCache \"+cn)" + JASS_DELIM) +
							// todo simplify using foreach
							// save integers to files
							"    set i=0",
					"    set n=2*GetStoredInteger(q," + getCommonHashFunc(CACHE_INTEGER_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")",
					"    loop",
					"        exitwhen i>=n",
					"        set m2=GetStoredString(q," + getCommonHashFunc(CACHE_INTEGER_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        set k2=GetStoredString(q," + getCommonHashFunc(CACHE_INTEGER_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(GetStoredInteger(q,m2,k2))," + FLUSH_KEY_CODE + ")",
					"    endloop",
					// save reals to files
					"    set i=0",
					"    set n=2*GetStoredInteger(q," + getCommonHashFunc(CACHE_REAL_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")",
					"    loop",
					"        exitwhen i>=n",
					"        set m2=GetStoredString(q," + getCommonHashFunc(CACHE_REAL_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        set k2=GetStoredString(q," + getCommonHashFunc(CACHE_REAL_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + getConversionFunc(ConversionFunctionType.REAL_TO_STRING) + "(GetStoredReal(q,m2,k2))," + FLUSH_KEY_CODE + ")",
					"    endloop",
					// save bools to files
					"    set i=0",
					"    set n=2*GetStoredInteger(q," + getCommonHashFunc(CACHE_BOOL_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")",
					"    loop",
					"        exitwhen i>=n",
					"        set m2=GetStoredString(q," + getCommonHashFunc(CACHE_BOOL_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        set k2=GetStoredString(q," + getCommonHashFunc(CACHE_BOOL_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "B2S(GetStoredBoolean(q,m2,k2))," + FLUSH_KEY_CODE + ")",
					"    endloop",
					// save strings to files
					"    set i=0",
					"    set n=2*GetStoredInteger(q," + getCommonHashFunc(CACHE_STRING_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")",
					"    loop",
					"        exitwhen i>=n",
					"        set m2=GetStoredString(q," + getCommonHashFunc(CACHE_STRING_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        set k2=GetStoredString(q," + getCommonHashFunc(CACHE_STRING_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "SS62(GetStoredString(q,m2,k2))," + FLUSH_KEY_CODE + ")",
					"    endloop",
					// save units to files
					"    set i=0",
					"    set n=2*GetStoredInteger(q," + getCommonHashFunc(CACHE_UNIT_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")",
					"    loop",
					"        exitwhen i>=n",
					"        set m2=GetStoredString(q," + getCommonHashFunc(CACHE_UNIT_KEY) + ",I2S(i))",
					"        set i=i+1",
					"        set k2=GetStoredString(q," + getCommonHashFunc(CACHE_UNIT_KEY) + ",I2S(i))",
					"        set i=i+1",
					"set id=GetStoredInteger(q,m2," + constSuffix(0) + ")",
					"set uD=" + ARCHON_PREFIX + "IS62(id)+\"\\n" + UNIT_CACHE_FORMAT + "\"",
					"set h=IsHeroUnitId(id)",
					"set l=GetStoredInteger(q,m2," + constSuffix(1) + ")",
					"set j=0",
					"loop",
					"exitwhen j>l",
					"if h and j==" + CACHE_ARMOR_INDEX + " then",
					"set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.REAL_TO_STRING) + "(GetStoredInteger(q,m2," + constSuffix(CACHE_ARMOR_INDEX) + "))",
					"else", // not hero armor
					"set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(GetStoredInteger(q,m2," + varSuffix("j") + "))",
					END_IF, // hero armor
					"endloop", // endloop vector
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2,uD," + FLUSH_KEY_CODE + ")",
					"endloop", // endloop units
					"    set " + ARCHON_PREFIX + "GCS=" + ARCHON_PREFIX + "GCS-1",
					"    return false",
					END_FUNCTION);
			// endregion
			// region Store natives
			append(sb, "function " + ARCHON_PREFIX + "StoreReference takes gamecache q,string k,string m,string k2 returns nothing",
					"local integer c=GetStoredInteger(q,k," + getCommonHashFunc(CACHE_COUNT_KEY) + ")",
					"call StoreString(q,k,I2S(2*c),m)",
					"call StoreString(q,k,I2S(2*c+1),k2)",
					"call StoreInteger(q,k," + getCommonHashFunc(CACHE_COUNT_KEY) + ",c+1)",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "StoreInteger takes gamecache q,string m,string k,integer v returns nothing",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_INTEGER_PREFIX + "\"+k",
					DebugOnly("call BJDebugMsg(\"StoreInteger \"+GetStoredString(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_INTEGER_KEY + "\",m2,k2)",
					"    call StoreInteger(q,m2,k2,v)",
					END_FUNCTION, "function " + ARCHON_PREFIX + "StoreReal takes gamecache q,string m,string k,real v returns nothing",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_REAL_PREFIX + "\"+k",
					DebugOnly("call BJDebugMsg(\"StoreReal \"+GetStoredString(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_REAL_KEY + "\",m2,k2)",
					"    call StoreReal(q,m2,k2,v)",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "StoreBoolean takes gamecache q,string m,string k,boolean v returns nothing",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_BOOL_PREFIX + "\"+k",
					DebugOnly("call BJDebugMsg(\"StoreBoolean \"+GetStoredString(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_BOOL_KEY + "\",m2,k2)",
					"    call StoreBoolean(q,m2,k2,v)",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "StoreString takes gamecache q,string m,string k,string v returns boolean",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_STRING_PREFIX + "\"+k",
					DebugOnly("call BJDebugMsg(\"StoreString \"+GetStoredString(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k+\" > \"+v)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_STRING_KEY + "\",m2,k2)",
					"    return StoreString(q,m2,k2,v)",
					END_FUNCTION);
			// endregion
			// region StoreUnit native
			append(sb, "function " + ARCHON_PREFIX + "StoreUnit takes gamecache q,string m,string k,unit u returns boolean",
					"    local integer i",
					"    local integer j",
					"    local integer id",
					"    local integer c",
					"    local integer l",
					"    local integer is",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k",
					"    local item array ti",
					"    local unit tu",
					"    if (u==null) then",
					"        return false",
					"    endif",
					DebugOnly("call BJDebugMsg(\"StoreUnit \"+GetStoredString(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k +\" > \"+GetUnitName(u))" + JASS_DELIM) +
							"set id=GetUnitTypeId(u)",
					"set tu=CreateUnit(Player(PLAYER_NEUTRAL_PASSIVE),id,0,0,0)");
			//"call ShowUnit(tu,false)",
			append(sb, "    call StoreInteger(q,m2," + constSuffix(0) + ",id)",
					"set is=UnitInventorySize(u)",
					// region save item
					"set i=0",
					"    loop",
					"        exitwhen i>=is",
					"set ti[i]=UnitItemInSlot(u,i)",
					"        set i=i+1",
					"    endloop",
					// endregion save items
					"    if IsHeroUnitId(id) then",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_XP_INDEX) + ",GetHeroXP(u))",
					// region drop items
					"set i=0",
					"    loop",
					"        exitwhen i>=is",
					"call UnitRemoveItem(u,ti[i])",
					"        set i=i+1",
					"    endloop",
					// endregion
					"set c=GetHeroStr(u,false)",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_STR_INDEX) + ",c)",
					"call SetHeroStr(tu,c,true)",
					"set c=GetHeroAgi(u,false)",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_AGI_INDEX) + ",c)",
					"call SetHeroAgi(tu,c,true)",
					"set c=GetHeroInt(u,false)",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_INT_INDEX) + ",c)",
					"call SetHeroInt(tu,c,true)",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_UNSPEND_POINTS_INDEX) + ",GetHeroSkillPoints(u))",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_MANA_MAX_INDEX) + ",BlzGetUnitMaxHP(u)-BlzGetUnitMaxHP(tu))",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_HEALTH_MAX_INDEX) + ",BlzGetUnitMaxMana(u)-BlzGetUnitMaxMana(tu))");
			for (int i = 0; i < 2; i++)
				append(sb, "        call StoreInteger(q,m2," + constSuffix(CACHE_ATTACK_DAMAGE_MIN_INDEX + i) + ",BlzGetUnitBaseDamage(u," + i + ")-BlzGetUnitBaseDamage(tu," + i + "))");
			append(sb, "        call StoreReal(q,m2," + constSuffix(CACHE_ARMOR_INDEX) + ",BlzGetUnitArmor(u)-BlzGetUnitArmor(tu))",
					"call RemoveUnit(tu)",
					// region readd items
					"set i=0",
					"loop",
					"exitwhen i>=is",
					"if ti[i]!=null then",
					"call UnitAddItem(u,ti[i])",
					"else",
					"call UnitAddItemById(u,'phlt')",
					END_IF,
					"set i=i+1",
					"endloop",
					"set i=0",
					"loop",
					"exitwhen i>=is",
					"if ti[i]==null then",
					"call RemoveItem(UnitItemInSlot(u,i))",
					END_IF,
					"set i=i+1",
					"endloop",
					// endregion
					"        set c=0",
					"        set i=0",
					"        loop",
					"            exitwhen i>" + ABILITY_ARRAY_SIZE,
					"            set j=" + ABILITY_ARRAY + "[i]",
					"            set l=GetUnitAbilityLevel(u,j)",
					"if l>0 then",
					"               call StoreInteger(q,m2,"
							+ varSuffix((CACHE_ABILITY_COUNT_INDEX + 1) + "+2*i") + ",j)",
					"               call StoreInteger(q,m2,"
							+ varSuffix((CACHE_ABILITY_COUNT_INDEX + 2) + "+2*i") + ",l)",
					"               set c=c+1",
					END_IF, // endif leveled
					"            set i=i+1",
					"        endloop",
					"        call StoreInteger(q,m2," + constSuffix(CACHE_ABILITY_COUNT_INDEX) + ",c)",
					"set j=" + (CACHE_ABILITY_COUNT_INDEX + 1) + "+2*c",
					"else", // not hero
					"set j=2",
					END_IF, // endif hero
					"call StoreInteger(q,m2," + varSuffix("j") + ",is)",
					"set i=0",
					"set j=j+1",
					"    loop",
					"        exitwhen i>=is",
					"set id=GetItemTypeId(ti[i])",
					"        call StoreInteger(q,m2," + varSuffix("i+j") + ",id)",
					"if id!=0 then",
					"        call StoreInteger(q,m2," + varSuffix("i+j+1") + ",GetItemCharges(ti[i]))",
					"set j=j+2",
					"        set c=0");
			{
				int i = 1;
				for (String s : Arrays.asList(
						"BlzGetItemBooleanField(ti[i],ITEM_BF_DROPPED_WHEN_CARRIER_DIES)", // 1
						"IsItemInvulnerable(ti[i])", // 2
						"IsItemPawnable(ti[i])", // 4
						"BlzGetItemBooleanField(ti[i],ITEM_BF_CAN_BE_DROPPED)", // 8
						"BlzGetItemBooleanField(ti[i],ITEM_BF_PERISHABLE)", // 16
						"BlzGetItemBooleanField(ti[i],ITEM_BF_ACTIVELY_USED)", // 32
						"IsItemPowerup(ti[i])" // 64
				)) {
					append(sb, "if " + s + " then",
							"set c=c+" + i,
							END_IF);
					i = i * 2;
				}
			}
			append(sb, "call StoreInteger(q,m2," + varSuffix("i+j") + ",c)",
					END_IF, // has item
					"set i=i+1",
					"endloop",
					"call StoreInteger(q,m2," + constSuffix(1) + ",i+j)", // len
					"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_UNIT_KEY + "\",m2,k2)",
					"    return true",
					END_FUNCTION);
			// endregion
			// region Store functions
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("Integer", "nothing", "call"),
					new Triple<>("Real", "nothing", "call"),
					new Triple<>("Boolean", "nothing", "call"),
					new Triple<>("String", "boolean", "return"),
					new Triple<>("Unit", "boolean", "return")
			))
				append(sb, "function " + ARCHON_PREFIX + "Store" + o.a + "BJ takes " + o.a.toLowerCase(Locale.ROOT) + " v,string k,string m,gamecache q returns " + o.b,
						o.c + " " + ARCHON_PREFIX + "Store" + o.a + "(q,m,k,v)",
						END_FUNCTION);
			// endregion
			// region SyncStored unused
			append(sb, "function " + ARCHON_PREFIX + "SyncStoredUnused takes gamecache q,string m,string k returns nothing",
					END_FUNCTION);
			// endregion
			// region HaveStored natives
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("Integer", CACHE_INTEGER_PREFIX, "0"),
					new Triple<>("Real", CACHE_REAL_PREFIX, "0.000"),
					new Triple<>("Boolean", CACHE_BOOL_PREFIX, "false"),
					new Triple<>("String", CACHE_STRING_PREFIX, "null")
			))
				append(sb, "function " + ARCHON_PREFIX + "HaveStored" + o.a + " takes gamecache q,string m,string k returns boolean",
						"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
						"    local string k2=\"" + o.b + "\"+k",
						"    local " + o.a.toLowerCase(Locale.ROOT) + " v",
						"    local string cn=" + getCacheNameFunc("q"),
						"if HaveStored" + o.a + "(q,m2,k2) then",
						"    return true",
						END_IF,
						"set v=" + ARCHON_PREFIX + "Read" + o.a + "(cn,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")",
						"if v==" + o.c + " then",
						"return false",
						END_IF,
						"call Store" + o.a + "(q,m2,k2,v)",
						"return true",
						END_FUNCTION);
			// endregion
			// region HaveStoredUnit native
			append(sb, "function " + ARCHON_PREFIX + "HaveStoredUnit takes gamecache q,string m,string k returns boolean",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k",
					"    local string cn=" + getCacheNameFunc("q"),
					"if GetStoredInteger(q,m2," + constSuffix(0) + ")!=0 then",
					"    return true",
					END_IF,
					"return " + ARCHON_PREFIX + "ReadString(cn,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")!=null",
					END_FUNCTION);
			// endregion
			// region HaveStoredValue function
			append(sb, "function " + ARCHON_PREFIX + "HaveStoredValue takes string k,integer t,string m,gamecache q returns boolean");
			for (Pair<String, String> o : Arrays.asList(
					new Pair<>("Boolean", ""),
					new Pair<>("Integer", "else"),
					new Pair<>("Real", "else"),
					new Pair<>("Unit", "else"),
					new Pair<>("String", "else")
			))
				append(sb, "    " + o.b + "if t==bj_GAMECACHE_" + o.a.toUpperCase(Locale.ROOT) + " then",
						"        return " + ARCHON_PREFIX + "HaveStored" + o.a + "(q,m,k)");
			append(sb, "    endif",
					"return false",
					END_FUNCTION);
			// endregion
			// region FlushStored natives
			for (Pair<String, String> o : Arrays.asList(
					new Pair<>("Integer", CACHE_INTEGER_PREFIX),
					new Pair<>("Real", CACHE_REAL_PREFIX),
					new Pair<>("Boolean", CACHE_BOOL_PREFIX),
					new Pair<>("String", CACHE_STRING_PREFIX)
			))
				append(sb, "function " + ARCHON_PREFIX + "FlushStored" + o.a + " takes gamecache q,string m,string k returns nothing",
						"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
						"    local string k2=\"" + o.b + "\"+k",
						"if HaveStored" + o.a + "(q,m2,k2) then",
						// todo add entry to hashtable that tells it to overwrite file upon saving
						//"call StoreInteger(q,m2,k2," + o.c + ")",
						"call FlushStored" + o.a + "(q,m2,k2)",
						END_IF,
						END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "FlushStoredUnit takes gamecache q,string m,string k returns nothing",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k",
					"if " + ARCHON_PREFIX + "HaveStoredUnit(q,m,k) then",
					// todo add entry to hashtable that tells it to overwrite file upon saving
					"call FlushStoredInteger(q,m2," + constSuffix(0) + ")",
					END_IF,
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "FlushGameCache takes gamecache q returns nothing",
					// todo add entry to hashtable that tells it to overwrite file upon saving
					"call FlushGameCache(q)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "FlushStoredMission takes gamecache q,string m returns nothing",
					"local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					// todo add entry to hashtable that tells it to overwrite file upon saving
					"call FlushStoredMission(q,m2)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "FlushGameCacheBJ takes gamecache q returns nothing",
					"call " + ARCHON_PREFIX + "FlushGameCache(q)",
					END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "FlushStoredMissionBJ takes string m,gamecache q returns nothing",
					"call " + ARCHON_PREFIX + "FlushStoredMission(q,m)",
					END_FUNCTION);
			// endregion
			// region GetStored natives
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("Integer", CACHE_INTEGER_PREFIX, "0"),
					new Triple<>("Real", CACHE_REAL_PREFIX, "0.000"),
					new Triple<>("Boolean", CACHE_BOOL_PREFIX, "false"),
					new Triple<>("String", CACHE_STRING_PREFIX, "null")
			))
				append(sb, "function " + ARCHON_PREFIX + "GetStored" + o.a + " takes gamecache q,string m,string k returns " + o.a.toLowerCase(Locale.ROOT),
						"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
						"    local string k2=\"" + o.b + "\"+k",
						"if " + ARCHON_PREFIX + "HaveStored" + o.a + "(q,m,k) then",
						"    return GetStored" + o.a + "(q,m2,k2)",
						END_IF,
						"return " + o.c,
						END_FUNCTION);
			// endregion
			//region RestoreUnit native
			append(sb, "function " + ARCHON_PREFIX + "RestoreUnit takes gamecache q,string m,string k,player p,real x,real y,real f returns unit",
					"    local integer i",
					"    local integer j",
					"    local integer a",
					"    local integer c",
					"    local integer fm",
					"    local integer l",
					"    local integer len",
					"    local unit u=null",
					"    local boolean r",
					"    local boolean d",
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m",
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k",
					"    local integer array v", // ints from all lines
					"    local real vr", // armor as real
					"    local integer array z",
					"    local item ti",
					"local string ct", // content
					"local string t=m2+\"\\\\\"+k2", // title
					"    local string cn=" + getCacheNameFunc("q"), // cache name
					"    local string char",
					"    local string bf=\"\"", // buffer
					DebugOnly("call BJDebugMsg(\"RestoreUnit \"+cn+\"|\"+m+\"|\"+k+\" > \"+GetStoredString(q,m2,\"" + CACHE_STRING_PREFIX + "\"+k))" + JASS_DELIM) +
							"set v[0]=GetStoredInteger(q,m2," + constSuffix(0) + ")",
					"if v[0]==0 then",
					"set r=true",

					"if GetPlayerSlotState(" + toMainPlayerFunc() + ")!=PLAYER_SLOT_STATE_PLAYING then",
					"return null",
					END_IF,

					"call " + ARCHON_PREFIX + "RdWt(true)",
					"set ct=" + ARCHON_PREFIX + "GetContent(cn,t," + FLUSH_KEY_CODE + ")",

					"set len=StringLength(ct)",
					"set j=0", // curr line
					"set i=0",
					"if len<=0 then",
					"set v[0]=0",
					"else", // has content
					"loop",
					"exitwhen i>len",
					"set char=SubString(ct,i,i+1)",
					"if char==\"\\n\" then",
					"if j==" + CACHE_ARMOR_INDEX + " then",
					"set vr=S2R(bf)",
					END_IF, // armor
					"if j==0 then",
					"set v[j]=" + ARCHON_PREFIX + "S62I(bf)",
					"else",
					"set v[j]=S2I(bf)",
					END_IF, // 0
					"set j=j+1",
					"set bf=\"\"",
					"else", // not endline
					"set bf=bf+char",
					"endif", // endline
					"set i=i+1",
					"endloop",
					"if j==" + CACHE_ARMOR_INDEX + " then",
					"set vr=S2R(bf)",
					"endif", // armor
					"set v[j]=S2I(bf)",
					END_IF); // content
			append(sb, "call StoreInteger(q,m2," + constSuffix(0) + ",v[0])",
					"call StoreInteger(q,m2," + constSuffix(1) + ",j)",
					"call TriggerSyncStart()",
					"call SyncStoredInteger(q,m2," + constSuffix(0) + ")",
					"call SyncStoredInteger(q,m2," + constSuffix(1) + ")",
					"call TriggerSyncReady()",
					// len is now vector length
					"set v[0]=GetStoredInteger(q,m2," + constSuffix(0) + ")",
					"set v[1]=GetStoredInteger(q,m2," + constSuffix(1) + ")",
					"if v[0]==0 then",
					"set u=null",
					"else", // has id
					"if r then",
					"set i=2",
					"loop", // loop i = 2, i <= len
					"exitwhen i>=v[1]",
					"call StoreInteger(q,m2," + varSuffix("i") + ",v[i])",
					"set i=i+1",
					"endloop",
					"call StoreReal(q,m2," + constSuffix(CACHE_ARMOR_INDEX) + ",vr)");
			append(sb, "call TriggerSyncStart()");
			append(sb, "set i=2",
					"loop", // loop i = 2, i <= len
					"exitwhen i>=v[1]",
					"call SyncStoredInteger(q,m2," + varSuffix("i") + ")",
					"set i=i+1",
					"endloop",
					"call SyncStoredReal(q,m2," + constSuffix(CACHE_ARMOR_INDEX) + ")");
			append(sb, "call TriggerSyncReady()");
			append(sb, "else", // not read
					"set v[0]=GetStoredInteger(q,m2," + constSuffix(0) + ")");
			append(sb, END_IF); // endif read

			append(sb, "set u=CreateUnit(p,v[0],x,y,f)");

			append(sb, "if IsHeroUnitId(v[0]) then",
					"call SetHeroXP(u,GetStoredInteger(q,m2," + constSuffix(CACHE_XP_INDEX) + "),false)",
					"call SetHeroStr(u,GetStoredInteger(q,m2," + constSuffix(CACHE_STR_INDEX) + "),true)",
					"call SetHeroAgi(u,GetStoredInteger(q,m2," + constSuffix(CACHE_AGI_INDEX) + "),true)",
					"call SetHeroInt(u,GetStoredInteger(q,m2," + constSuffix(CACHE_INT_INDEX) + "),true)",
					"set v[" + CACHE_HEALTH_MAX_INDEX + "]=GetStoredInteger(q,m2," + constSuffix(CACHE_HEALTH_MAX_INDEX) + ")+BlzGetUnitMaxHP(u)",
					"set v[" + CACHE_MANA_MAX_INDEX + "]=GetStoredInteger(q,m2," + constSuffix(CACHE_MANA_MAX_INDEX) + ")+BlzGetUnitMaxMana(u)",
					"call BlzSetUnitMaxHP(u,v[" + CACHE_HEALTH_MAX_INDEX + "])",
					"call SetUnitState(u,UNIT_STATE_LIFE,v[" + CACHE_HEALTH_MAX_INDEX + "])",
					"call BlzSetUnitMaxMana(u,v[" + CACHE_MANA_MAX_INDEX + "])",
					"call SetUnitState(u,UNIT_STATE_MANA,v[" + CACHE_MANA_MAX_INDEX + "])");
			for (int i = 0; i < 2; i++)
				append(sb, "call BlzSetUnitBaseDamage(u,GetStoredInteger(q,m2," + constSuffix(CACHE_ATTACK_DAMAGE_MIN_INDEX + i) + ")+BlzGetUnitBaseDamage(u," + i + ")," + i + ")");
			append(sb, "call BlzSetUnitArmor(u,GetStoredReal(q,m2," + constSuffix(CACHE_ARMOR_INDEX) + ")+BlzGetUnitArmor(u))",
					"call ModifyHeroSkillPoints(u,bj_MODIFYMETHOD_SET,GetStoredInteger(q,m2," + constSuffix(CACHE_STR_INDEX) + "))",
					"set i=0",
					DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" c \"+I2S(c))" + JASS_DELIM) +
							"set i=0",
					"set v[" + CACHE_ABILITY_COUNT_INDEX + "]=GetStoredInteger(q,m2," + constSuffix(CACHE_ABILITY_COUNT_INDEX) + ")",
					"loop", // loop i = 0, i < ability_count
					"exitwhen i>=v[" + CACHE_ABILITY_COUNT_INDEX + "]",
					"set a=GetStoredInteger(q,m2," + varSuffix((CACHE_ABILITY_COUNT_INDEX + 1) + "+2*i") + ")",
					"set l=GetStoredInteger(q,m2," + varSuffix((CACHE_ABILITY_COUNT_INDEX + 2) + "+2*i") + ")",
					"call ModifyHeroSkillPoints(u,bj_MODIFYMETHOD_ADD,l)",
					"call " + ARCHON_PREFIX + "WatchSkill(a)",
					DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" l \"+I2S(l))" + JASS_DELIM) +
							"            set j=0",
					"set d=false",
					"loop", // loop levels
					"exitwhen j>=l",
					DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" j \"+I2S(j))" + JASS_DELIM) +
							DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" a \"+I2S(a))" + JASS_DELIM) +
							"call SelectHeroSkill(u,a)",
					"if j==0 and GetUnitAbilityLevel(u,a)<1 then",
					"set d=true",
					"call SetPlayerAbilityAvailable(Player(PLAYER_NEUTRAL_PASSIVE),a,true)",
					"call SelectHeroSkill(u,a)",
					END_IF, // force level
					"set j=j+1",
					"endloop", // level loop
					"if d then", // if forced level
					"call SetPlayerAbilityAvailable(Player(PLAYER_NEUTRAL_PASSIVE),a,false)",
					END_IF, // endif forced level
					"set i=i+1",
					"endloop",
					"set j=" + (CACHE_ABILITY_COUNT_INDEX + 1) + "+2*v[" + CACHE_ABILITY_COUNT_INDEX + "]",
					"else", // not hero
					"set j=2",
					END_IF, // endif hero
					"set len=GetStoredInteger(q,m2," + varSuffix("j") + ")",
					"set i=0",
					"set j=j+1",
					"loop",
					"exitwhen i>len",
					"set c=GetStoredInteger(q,m2," + varSuffix("j+i") + ")",
					"if c!=0 then"); // if item exists
			// 2 is charge, 3 is bits
			append(sb, "set v[2]=GetStoredInteger(q,m2," + varSuffix("i+j+1") + ")",
					"set j=j+2",
					"set v[3]=GetStoredInteger(q,m2," + varSuffix("i+j") + ")",
					"call UnitAddItemToSlotById(u,c,i)",
					"set ti=UnitItemInSlot(u,i)",
					"call SetItemCharges(ti,v[2])");
			{
				List<String> l = Arrays.asList(
						"SetItemDropOnDeath(%s,%s)", // 1
						"SetItemInvulnerable(%s,%s)", // 2
						"SetItemPawnable(%s,%s)", // 4
						"SetItemDroppable(%s,%s)" // 8
						//"!BlzSetItemBooleanField(%s,ITEM_BF_PERISHABLE,%s)", // 16
						//"!BlzSetItemBooleanField(%s,ITEM_BF_ACTIVELY_USED,%s)", // 32
						//"!BlzSetItemBooleanField(%s,ITEM_BF_USE_AUTOMATICALLY_WHEN_ACQUIRED,%s)" // 64
				);
				{
					int i = l.size();
					for (String s : l) {
						if (!s.startsWith("!"))
							append(sb, "call " + String.format(s, "ti", moduloFunc("v[3]", "2") + "==1"));
						i--;
						if (i > 0)
							append(sb, "set v[3]=v[3]/2");
					}
				}
			}
			append(sb, END_IF, // end item exists
					"set i=i+1",
					"endloop",
					END_IF, // end read
					END_IF, // end unit id not 0
					"call SetUnitOwner(u,p,true)",
					"call ShowUnit(u,true)",
					"call " + ARCHON_PREFIX + "RdWt(false)",
					"return u",
					END_FUNCTION);
			// endregion
			// region GetStored functions
			for (String s : Arrays.asList("Integer", "Real", "Boolean"))
				append(sb, "function " + ARCHON_PREFIX + "GetStored" + s + "BJ takes string k,string m,gamecache q returns " + s.toLowerCase(Locale.ROOT),
						"    return " + ARCHON_PREFIX + "GetStored" + s + "(q,m,k)",
						END_FUNCTION);
			append(sb, "function " + ARCHON_PREFIX + "GetStoredStringBJ takes string k,string m,gamecache q returns string",
					"    local string s=" + ARCHON_PREFIX + "GetStoredString(q,m,k)",
					"    if s==null then",
					"        return \"\"",
					END_IF,
					"    return s",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "RestoreUnitLocFacingAngleBJ takes string k,string m,gamecache q,player p,location l,real f returns unit",
					"    set bj_lastLoadedUnit=" + ARCHON_PREFIX + "RestoreUnit(q,m,k,p,GetLocationX(l),GetLocationY(l),f)",
					"    return bj_lastLoadedUnit",
					END_FUNCTION,
					"function " + ARCHON_PREFIX + "RestoreUnitLocFacingPointBJ takes string k,string m,gamecache q,player p,location l,location f returns unit",
					"    return " + ARCHON_PREFIX + "RestoreUnitLocFacingAngleBJ(k,m,q,p,l,AngleBetweenPoints(l,f))",
					END_FUNCTION);
			// endregion
			// region GameCache basic functions
			append(sb, "function " + ARCHON_PREFIX + "InitGameCacheBJ takes string f returns gamecache",
					"set bj_lastCreatedGameCache=" + ARCHON_PREFIX + "InitGameCache(f)",
					"return bj_lastCreatedGameCache",
					//"    return null",
					END_FUNCTION,
					"function  " + ARCHON_PREFIX + "SaveGameCacheBJ takes gamecache q returns boolean",
					"    return " + ARCHON_PREFIX + "SaveGameCache(q)",
					END_FUNCTION);
			// endregion
			// endregion
			// region Trigger functions
			{
				for (Triple<String, String, String> o : Collections.singletonList(
						new Triple<>("TriggerExecuteBJ", "trigger t,boolean b returns boolean", "(t,b)")
				))
					append(sb, "function " + ARCHON_PREFIX + o.a + " takes " + o.b,
							PAUSE_LOCK_CODE,
							"return " + o.a + o.c,
							END_FUNCTION);
				for (Triple<String, String, String> o : Collections.singletonList(
						new Triple<>("TriggerEvaluate", "trigger t returns boolean", "(t)")
				))
					append(sb, "function " + ARCHON_PREFIX + o.a + " takes " + o.b,
							PAUSE_LOCK_CODE,
							"return " + o.a + o.c,
							END_FUNCTION);
			}
			// endregion
		}
		// region gold and lumber synchronization
		List<String> resourceTypes = Arrays.asList("PLAYER_STATE_RESOURCE_GOLD", "PLAYER_STATE_RESOURCE_LUMBER");
		List<String> resourceNames = getBaseResourceNames();
		for (int i = 0; i < resourceTypes.size(); i++) {
			String resourceType = resourceTypes.get(i);
			String resourceName = resourceNames.get(i);
			append(sb, "function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac takes nothing returns nothing",
					"    local integer v=GetPlayerState(GetTriggerPlayer()," + resourceType + ")",
					XT87Utils.DISABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")");
			for (int playerNumber : allPlayerNumbers) {
				String playerFunc = toPlayerFunc(playerNumber);
				append(sb, "    if (GetTriggerPlayer()!=" + playerFunc + ") then");
				append(sb, "        call SetPlayerState(" + playerFunc + "," + resourceType + ",v)");
				append(sb, "    " + END_IF);
			}
			append(sb, XT87Utils.ENABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")",
					END_FUNCTION,
					"function it_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing",
					"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()");
			for (int playerNumber : allPlayerNumbers)
				append(sb, "    call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toPlayerFunc(playerNumber) + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0)");
			append(sb, "    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac)",
					END_FUNCTION + JASS_DELIM);
		}
		// endregion
		// region food and population cap synchronization
		resourceTypes = Arrays.asList("PLAYER_STATE_RESOURCE_FOOD_USED", "PLAYER_STATE_RESOURCE_FOOD_CAP", "PLAYER_STATE_FOOD_CAP_CEILING");
		resourceNames = getFoodResourceNames();
		for (int i = 0; i < resourceTypes.size(); i++) {
			String resourceType = resourceTypes.get(i);
			String resourceName = resourceNames.get(i);
			append(sb, "function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac takes nothing returns nothing",
					"    local integer v=GetPlayerState(" + toMainPlayerFunc() + "," + resourceType + ")",
					XT87Utils.DISABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")");
			for (int playerNumber : allPlayerNumbers) {
				if (playerNumber == mainPlayer.getNum())
					continue;
				String playerFunc = toPlayerFunc(playerNumber);
				append(sb, "    call SetPlayerState(" + playerFunc + "," + resourceType + ",v)");
			}
			append(sb, XT87Utils.ENABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")",
					END_FUNCTION,
					"function it_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing",
					"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()");
			append(sb, "call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toMainPlayerFunc() + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0)");
			append(sb, "call TriggerRegisterTimerEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,0,false)");
			append(sb, "call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac)",
					END_FUNCTION);
		}
		// endregion
		// region pause test
		if (WITH_PAUSE_COMMAND)
			append(sb, "function t_" + ARCHON_PREFIX + "PauseTest_Ac takes nothing returns nothing",
					"call " + ARCHON_PREFIX + "PauseGame(not " + ARCHON_PREFIX + "GIP)",
					END_FUNCTION,
					"function it_" + ARCHON_PREFIX + "PauseTest takes nothing returns nothing",
					"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest=CreateTrigger()",
					"    call TriggerRegisterPlayerChatEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest," + toMainPlayerFunc() + ",\"-p\",true)",
					"    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest,function t_" + ARCHON_PREFIX + "PauseTest_Ac)",
					END_FUNCTION);
		// endregion
		// region player leaves game
		append(sb, "function t_" + ARCHON_PREFIX + "PLG_Ac takes nothing returns nothing",
				"call RemovePlayerPreserveUnitsBJ(GetTriggerPlayer(),PLAYER_GAME_RESULT_DEFEAT,true)",
				END_FUNCTION,
				"function it_" + ARCHON_PREFIX + "PLG takes nothing returns nothing",
				"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG=CreateTrigger()");
		for (int playerNumber : allPlayerNumbers)
			append(sb, "call TriggerRegisterPlayerEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG," + toPlayerFunc(playerNumber) + ",EVENT_PLAYER_LEAVE)");
		append(sb, "call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG,function t_" + ARCHON_PREFIX + "PLG_Ac)",
				END_FUNCTION);
		// endregion
		// region game is loaded
		append(sb, "function t_" + ARCHON_PREFIX + "GIL_Ac takes nothing returns nothing",
				bCall("SetMapFlag", "MAP_RELOADED," + ARCHON_PREFIX + "LRD"),
				bSet(ARCHON_PREFIX + "LRD", "false"),
				bIf(ARCHON_PREFIX + "LUP",
						bSet(ARCHON_PREFIX + "LUP", "false"),
						bCall(ARCHON_PREFIX + "PauseGame", "false")),
				END_FUNCTION,
				"function it_" + ARCHON_PREFIX + "GIL takes nothing returns nothing",
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL=CreateTrigger()",
				"call TriggerRegisterGameLoadedEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL)",
				"call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL,function t_" + ARCHON_PREFIX + "GIL_Ac)",
				END_FUNCTION);
		// endregion
		// region give units to main player
		append(sb, "function t_" + ARCHON_PREFIX + "UnitShare_Conditions takes nothing returns boolean");
		for (int playerNumber : allPlayerNumbers) {
			if (playerNumber == mainPlayer.getNum())
				continue;
			append(sb, "    if GetOwningPlayer(GetEnteringUnit())==" + toPlayerFunc(playerNumber) + " then",
					"        return true",
					"    endif");
		}
		append(sb, "    return false",
				END_FUNCTION,
				"function t_" + ARCHON_PREFIX + "UnitShare_Ac takes nothing returns nothing",
				"    call SetUnitOwner(GetEnteringUnit()," + toMainPlayerFunc() + ",true)",
				END_FUNCTION,
				"function it_" + ARCHON_PREFIX + "UnitShare takes nothing returns nothing",
				"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare=CreateTrigger()",
				"    call TriggerRegisterEnterRectSimple(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,GetPlayableMapRect())",
				"    call TriggerAddCondition(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,Condition(function t_" + ARCHON_PREFIX + "UnitShare_Conditions))",
				"    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,function t_" + ARCHON_PREFIX + "UnitShare_Ac)",
				END_FUNCTION);
		// endregion
		// region selection effects
		append(sb, "function " + ARCHON_PREFIX + "OnSelect takes nothing returns nothing",
				"local integer p=GetPlayerId(" + AUX_PLAYER + ")",
				"local integer c=LoadInteger(" + SELECTION_HASHTABLE + ",p,0)+1",
				"local effect f=AddSpecialEffectTarget(\"" + XT87Utils.PATH_PREFIX + "\\\\" + SELECTION_INDICATOR_PATH + ".mdx\",GetEnumUnit(),\"overhead\")",
				"local real a=1",
				"if bj_cineModeAlreadyIn then",
				"set a=0",
				END_IF,
				"call BlzSetSpecialEffectColorByPlayer(f,Player(p))",
				"call BlzSetSpecialEffectScale(f,a)",
				"call SaveEffectHandle(" + SELECTION_HASHTABLE + ",p,c,f)",
				"call SaveInteger(" + SELECTION_HASHTABLE + ",p,0,c)",
				END_FUNCTION);
		append(sb, "function t_" + ARCHON_PREFIX + "Select_Ac takes nothing returns nothing",
				"local integer i=0",
				"local integer p=GetPlayerId(GetTriggerPlayer())",
				// selection count
				"local integer c",
				// stop bool
				"local boolean b=LoadBoolean(" + SELECTION_HASHTABLE + ",p,0)",
				"local group g",
				"if b then",
				"return",
				END_IF,
				"call SaveBoolean(" + SELECTION_HASHTABLE + ",p,0,true)",
				"call TriggerSleepAction(0.01)",
				"set c=LoadInteger(" + SELECTION_HASHTABLE + ",p,0)",
				"loop",
				"exitwhen i>c",
				"set i=i+1",
				"call DestroyEffect(LoadEffectHandle(" + SELECTION_HASHTABLE + ",p,i))",
				"endloop",
				"call SaveInteger(" + SELECTION_HASHTABLE + ",p,0,0)",
				"set g=CreateGroup()",
				"set " + AUX_PLAYER + "=GetTriggerPlayer()",
				"call GroupEnumUnitsSelected(g," + AUX_PLAYER + ",null)",
				"call ForGroup(g,function " + ARCHON_PREFIX + "OnSelect)",
				"call DestroyGroup(g)",
				"call SaveBoolean(" + SELECTION_HASHTABLE + ",p,0,false)",
				END_FUNCTION,
				"function it_" + ARCHON_PREFIX + "Select takes nothing returns nothing",
				"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "Select=CreateTrigger()");
		for (int playerNumber : allPlayerNumbers)
			for (String selectBool : Arrays.asList("false", "true"))
				append(sb, "    call TriggerRegisterPlayerSelectionEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "Select," + toPlayerFunc(playerNumber) + "," + selectBool + ")");
		append(sb, "    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "Select, function t_" + ARCHON_PREFIX + "Select_Ac)",
				END_FUNCTION);
		// endregion
		// region show food on allied buildings
		append(sb, "function t_" + ARCHON_PREFIX + "BFP_Ac takes nothing returns nothing",
				//"local integer f=" + ARCHON_PREFIX + "LBF[GetPlayerId(GetLocalPlayer())]",
				"local framehandle array a",
				"local integer f=" + ARCHON_PREFIX + "LBF",
				"local boolean b=BlzFrameIsVisible(BlzGetFrameByName(\"AllianceDialog\",0))",
				"local integer i=0",
				"set a[0]=BlzGetFrameByName(\"AllianceAcceptButton\",0)",
				"set a[1]=BlzGetFrameByName(\"AllianceBackdrop\",0)",
				//"BlzGetFrameByName(\"AllianceCancelButton\",0)",
				"set a[2]=BlzGetFrameByName(\"AllianceDialogScrollBar\",0)",
				"set a[3]=BlzGetFrameByName(\"AllianceTitle\",0)",
				"set a[4]=BlzGetFrameByName(\"AllianceSlot\",0)",
				"set a[5]=BlzGetFrameByName(\"AlliedVictoryCheckBox\",0)",
				"set a[6]=BlzGetFrameByName(\"ResourceTradingTitle\",0)",
				"set a[7]=BlzGetFrameByName(\"PlayersHeader\",0)",
				"set a[8]=BlzGetFrameByName(\"AllyHeader\",0)",
				"set a[9]=BlzGetFrameByName(\"VisionHeader\",0)",
				"set a[10]=BlzGetFrameByName(\"UnitsHeader\",0)",
				"set a[11]=BlzGetFrameByName(\"GoldHeader\",0)",
				"set a[12]=BlzGetFrameByName(\"LumberHeader\",0)",
				"set a[13]=BlzGetFrameByName(\"AlliedVictoryLabel\",0)",
				"set a[14]=BlzGetFrameByName(\"LogArea\",0)",
				"set a[15]=BlzGetFrameByName(\"LogAreaScrollBar\",0)",
				"set a[16]=BlzGetFrameByName(\"LogAreaBackdrop\",0)",
				"set a[17]=BlzGetFrameByName(\"LogOkButton\",0)",
				"set a[18]=BlzGetFrameByName(\"LogTitle\",0)",
				"set a[19]=BlzGetFrameByName(\"LogDialog\",0)",
				"set a[20]=BlzGetFrameByName(\"SimpleInfoPanelIconAlly\",7)",
				"set a[21]=BlzGetFrameByName(\"SimpleInfoPanelIconFood\",4)",
				"set a[22]=BlzGetFrameByName(\"InfoPanelIconValue\",4)",
				"loop",
				"exitwhen i>=bj_MAX_PLAYERS",
				"call BlzFrameSetVisible(BlzGetFrameByName(\"AllianceSlot\",i),false)",
				"call BlzFrameSetVisible(BlzGetFrameByName(\"AlliedVictoryCheckBox\",i),false)",
				"set i=i+1",
				"endloop",
				DebugOnly("call BJDebugMsg(\"a\")" + JASS_DELIM) +
						"if not " + ARCHON_PREFIX + "AlliesMenuAvail and b then",
				"set " + ARCHON_PREFIX + "AlliesMenuAvail=true",
				"set i=0",
				"loop",
				"exitwhen i>3",
				"call BlzFrameSetVisible(a[i],false)",
				"set i=i+1",
				"endloop",
				END_IF,
				"if " + ARCHON_PREFIX + "AlliesMenuAvail then",
				"if b then",
				"set i=6",
				"loop",
				"exitwhen i>13",
				"call BlzFrameSetVisible(a[i],false)",
				"set i=i+1",
				"endloop",
				"loop",
				"exitwhen i>18",
				"call BlzFrameSetVisible(a[i],true)",
				"set i=i+1",
				"endloop",
				END_IF,
				"call BlzFrameSetVisible(a[19],b)",
				END_IF,
				"if f>0 then",
				"    call BlzFrameSetVisible(a[20],false)",
				"    call BlzFrameSetVisible(a[21],true)",
				"    call BlzFrameSetText(a[22],I2S(f))",
				END_IF,
				END_FUNCTION,
				"function it_" + ARCHON_PREFIX + "BFP takes nothing returns nothing",
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP=CreateTrigger()",
				"call TriggerRegisterTimerEventPeriodic(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP,0.0)",
				"call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP,function t_" + ARCHON_PREFIX + "BFP_Ac)",
				END_FUNCTION);
		append(sb, "function t_" + ARCHON_PREFIX + "BF_Ac takes nothing returns nothing",
				"local unit u=GetTriggerUnit()",
				"local integer f=GetUnitFoodMade(u)",
				"local integer i",
				"local player p=GetTriggerPlayer()",
				"if p==GetLocalPlayer() then",
				"set i=GetPlayerId(GetLocalPlayer())",
				"if f>0 and IsUnitType(u,UNIT_TYPE_STRUCTURE) and not IsUnitOwnedByPlayer(u,p) and IsUnitAlly(u,p) then",
				"set " + ARCHON_PREFIX + "LBF=f",
				"call t_" + ARCHON_PREFIX + "BFP_Ac()",
				"else",
				"set " + ARCHON_PREFIX + "LBF=0",
				END_IF,
				END_IF,
				END_FUNCTION,
				"function it_" + ARCHON_PREFIX + "BF takes nothing returns nothing",
				"local integer i=0",
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "BF=CreateTrigger()",
				// todo change loop to GetLocalPlayer()
				loopOverPlayers("i",
						"call TriggerRegisterPlayerSelectionEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BF,Player(" + PLAYER_ID_ARRAY + "[i]),true)"),
				"call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BF, function t_" + ARCHON_PREFIX + "BF_Ac)",
				END_FUNCTION);
		// endregion
		// region get main player
		for (String s : Arrays.asList("GetLocalPlayer", "GetTriggerPlayer"))
			append(sb, "function " + ARCHON_PREFIX + s + " takes nothing returns player",
					"    local player p=" + s + "()",
					"    if IsPlayerInForce(p," + FORCE_NAME + ") then",
					"        return " + toMainPlayerFunc(),
					END_IF,
					"    return p",
					END_FUNCTION);
		append(sb, "function " + ARCHON_PREFIX + "GetForceOfPlayer takes player p returns force",
				"    local force f=CreateForce()",
				"    local integer i=0",
				"    if IsPlayerInForce(p," + FORCE_NAME + ") then",
				loopOverPlayers("i",
						"call ForceAddPlayer(f,Player(" + PLAYER_ID_ARRAY + "[i]))"),
				"    else",
				"    call ForceAddPlayer(f,p)",
				END_IF,
				"    return f",
				END_FUNCTION);
		// endregion
		// region enforce sharing
		append(sb, "function " + ENFORCE_ARCHON + " takes nothing returns nothing");
		for (W3I.Player player : secondaryPlayers) {
			String playerFunc = toPlayerFunc(player.getNum());
			for (W3I.Player otherPlayer : secondaryPlayers) {
				int otherNum = otherPlayer.getNum();
				if (otherNum == player.getNum()) {
					append(sb, "    call SetPlayerAllianceStateBJ(" + playerFunc + "," + toMainPlayerFunc() + ",bj_ALLIANCE_ALLIED_VISION)");
					append(sb, "    call SetPlayerAllianceStateBJ(" + toMainPlayerFunc() + "," + playerFunc + ",bj_ALLIANCE_ALLIED_ADVUNITS)");
				} else
					append(sb, "    call SetPlayerAllianceStateBJ(" + playerFunc + "," + toPlayerFunc(otherNum) + ",bj_ALLIANCE_ALLIED_VISION)");
			}
		}
		append(sb, END_FUNCTION);
		// endregion
		// region dialog triggers
		append(sb, "function t_" + ARCHON_PREFIX + "DS_Ac takes nothing returns nothing",
				"local player p=GetTriggerPlayer()",
				"local dialog d=GetClickedDialog()",
				"local string s=LoadStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(GetClickedButton()))"
		);
		for (int playerNumber : allPlayerNumbers)
			append(sb, "call DialogDisplay(" + toPlayerFunc(playerNumber) + ",d,false)");
		append(sb, "call TriggerSleepAction(0.01)",
				"call DisplayTimedTextFromPlayer(p,0,0,10,\"%s => \"+GetLocalizedString(s))",
				"if " + ARCHON_PREFIX + "DIP then",
				"call " + ARCHON_PREFIX + "PauseGame(false)",
				"set " + ARCHON_PREFIX + "DIP=false",
				END_IF,
				END_FUNCTION,
				"function it_" + ARCHON_PREFIX + "DS takes nothing returns nothing",
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "DS=CreateTrigger()");
		for (String dialogName : dialogNames)
			append(sb, "    call TriggerRegisterDialogEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "DS," + dialogName + ")");
		append(sb, "    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "DS,function t_" + ARCHON_PREFIX + "DS_Ac)",
				END_FUNCTION);
		// endregion
	}

	@Override
	public void onMainLocals(StringBuffer sb) {
		int index = 0;
		for (int playerNum : allPlayerNumbers) {
			append(sb, "set " + PLAYER_ID_ARRAY + "[" + index + "]=" + playerNum,
					"set " + PLAYER_INDEX_ARRAY + "[" + playerNum + "]=" + index);
			index++;
		}
		append(sb, "    " + SETPLAYERCOLORBJ + toMainPlayerFunc() + "," + CONVERTPLAYERCOLOR + mainPlayer.getNum() + "),true)");
		//if (line.contains(END_FUNCTION)) {
		for (String resourceName : getAllResourceNames())
			append(sb, "call it_" + ARCHON_PREFIX + resourceName + "Sync()");
		for (String triggerName : Arrays.asList("UnitShare", "Select", "PLG")) //, "Deselect", "DeselectOnDeath"))
			append(sb, "call it_" + ARCHON_PREFIX + triggerName + "()");
		for (String triggerName : Arrays.asList("BF", "BFP"))
			append(sb, "call it_" + ARCHON_PREFIX + triggerName + "()");
		append(sb, bCall("it_" + ARCHON_PREFIX + "GIL"));
		if (WITH_PAUSE_COMMAND)
			append(sb, bCall("it_" + ARCHON_PREFIX + "PauseTest"));
		if (WITH_CUSTOM_GAMECACHE) {
			append(sb, "call " + FILE_LIBRARY_PREFIX + "Init()");
		}
		append(sb, bSet(FORCE_NAME, bFunc("CreateForce")));
		for (int playerNumber : allPlayerNumbers)
			append(sb, "    call ForceAddPlayer(" + FORCE_NAME + "," + toPlayerFunc(playerNumber) + ")");
		for (W3I.Player player : secondaryPlayers) {
			String playerFunc = toPlayerFunc(player.getNum());
			append(sb, SETPLAYERCOLOR + playerFunc + "," + CONVERTPLAYERCOLOR + player.getNum() + "))",
					"call SetPlayerOnScoreScreen(" + playerFunc + ",false)");
		}
		append(sb, "call " + ENFORCE_ARCHON + "()",
				"if GetLocalPlayer()!=" + toMainPlayerFunc() + " then",
				"call SetReservedLocalHeroButtons(0)",
				END_IF);

		List<String> mapPaths = new ArrayList<>(mi.cs.chapterNameKeysMap.keySet());
		Collections.sort(mapPaths);

		for (String mapPath : mapPaths) {
			String buttonName = STRING_PREFIX + (campaignKeyOffset + mi.cs.chapterNameKeysMap.get(mapPath));
			append(sb, "call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_CAMPAIGN_BUTTON_NAME +
					",StringHash(\"" + mapPath + "\"),\"" + buttonName + "\")");
		}
	}

	@Override
	public void onMainEnd(StringBuffer sb) {
		append(sb, "call it_" + ARCHON_PREFIX + "DS()");
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		line = line.replace("bj_FORCE_PLAYER[" + mainPlayer.getNum() + "]", FORCE_NAME);
		line = line.replace("call SetSoundDuration(", "//call SetSoundDuration(");

		// region replace specific calls
		for (String function : Arrays.asList(
				"GetLocalPlayer",
				"GetTriggerPlayer",
				"GetForceOfPlayer",
				//"CreateSound",
				"DialogDisplay",
				"DialogDisplayBJ",
				"DialogAddButton",
				"DialogAddQuitButton",
				"DialogAddButtonBJ",
				"DialogAddButtonWithHotkeyBJ",
				"ClearSelection",
				"SelectUnitSingle",
				//"SelectGroupBJ",
				"ClearSelectionForPlayer",
				"EnableUserControl",
				"SetUserControlForceOn",
				"SetUserControlForceOff",
				"CinematicModeExBJ",
				"CinematicModeBJ",
				"PauseGame",
				"PauseGameOn",
				"PauseGameOff",
				"SuspendTimeOfDay",
				"UseTimeOfDayBJ",
				"PauseCompAI",
				"PauseAllUnitsBJ",
				"CreateTimer",
				"CreateTimerBJ",
//				"DestroyTimer",
//				"DestroyTimerBJ",
				"TimerStart",
				"StartTimerBJ",
				"TimerGetElapsed",
				"TimerGetRemaining",
				"TimerGetTimeout",
				"PauseTimer",
				"ResumeTimer",
				"PauseTimerBJ",
				"SetReservedLocalHeroButtons",
				"ChangeLevel",
				"LoadGame",
				"SaveGame",
				"LoadGameBJ",
				"SaveAndChangeLevelBJ",
				"SaveAndLoadGameBJ",
				"CopySaveGame",
				"SaveGameExists",
				"CopySaveGameBJ"
		))
			for (String prefix : Arrays.asList("(", " ", ",", "="))
				for (String suffix : Arrays.asList("(", " ")) {
					String call = function + suffix;
					line = line.replace(prefix + call, prefix + ARCHON_PREFIX + call);
				}
		// endregion

		if (WITH_CUSTOM_GAMECACHE) {
			//line = line.replace("bj_lastCreatedGameCache", LAST_CREATED_COOP_CACHE);
			//line = line.replace("GetLastCreatedGameCacheBJ()", LAST_CREATED_COOP_CACHE);
			//for (String prefix : Arrays.asList(",", " "))
			//	line = line.replace(prefix + "gamecache ", prefix + "hashtable ");
			// region replace game cache calls
			for (String function : Arrays.asList(
					"InitGameCache",
					"SaveGameCache",

					"StoreInteger",
					"StoreReal",
					"StoreBoolean",
					"StoreUnit",
					"StoreString",

					"HaveStoredInteger",
					"HaveStoredReal",
					"HaveStoredBoolean",
					"HaveStoredUnit",
					"HaveStoredString",

					"FlushGameCache",
					"FlushStoredMission",
					"FlushStoredInteger",
					"FlushStoredReal",
					"FlushStoredBoolean",
					"FlushStoredUnit",
					"FlushStoredString",

					"GetStoredInteger",
					"GetStoredReal",
					"GetStoredBoolean",
					"GetStoredString",
					"RestoreUnit",

					"InitGameCacheBJ",
					"SaveGameCacheBJ",

					"StoreIntegerBJ",
					"StoreRealBJ",
					"StoreBooleanBJ",
					"StoreStringBJ",
					"StoreUnitBJ",

					"GetStoredIntegerBJ",
					"GetStoredRealBJ",
					"GetStoredBooleanBJ",
					"GetStoredStringBJ",
					"RestoreUnitLocFacingAngleBJ",
					"RestoreUnitLocFacingPointBJ",

					"FlushGameCacheBJ",
					"FlushStoredMissionBJ",
					"HaveStoredValue"
			))
				for (String prefix : Arrays.asList("(", " ", ",", "="))
					for (String suffix : Arrays.asList("(", " ")) {
						String call = function + suffix;
						line = line.replace(prefix + call, prefix + ARCHON_PREFIX + call);
					}
			// endregion
			// region replace trigger execution calls
			if (!Arrays.asList("main", "RunInitializationTriggers").contains(insideFunction)) {
				for (String function : Arrays.asList(
//					"TriggerExecute",
//					"TriggerExecuteWait",
						"TriggerEvaluate",
//					"ConditionalTriggerExecute",
						"TriggerExecuteBJ"
				))
					for (String prefix : Arrays.asList("(", " ", ",", "="))
						for (String suffix : Arrays.asList("(", " ")) {
							String call = function + suffix;
							line = line.replace(prefix + call, prefix + ARCHON_PREFIX + call);
						}
			}
			// endregion
			// region replace SyncStored calls
			for (String function : Arrays.asList(
					"SyncStoredInteger",
					"SyncStoredReal",
					"SyncStoredBoolean",
					"SyncStoredUnit",
					"SyncStoredString"
			))
				for (String prefix : Collections.singletonList(" "))
					for (String suffix : Arrays.asList("(", " "))
						line = line.replace(prefix + function + suffix, prefix + ARCHON_PREFIX + "SyncStoredUnused" + suffix);
			// endregion
			String call = getTrimmedCallFromLine(line);
			if (call != null) {
				String[] params = getParamsFromLine(line);
				if (call.equals("SelectHeroSkill")) {
					assert params != null;
					append(sb, "    call " + ARCHON_PREFIX + "WatchSkill(" + params[1] + ")");
				}
			}
		}

		// variable declarations
		{
			boolean local = false;
			String trimmedLine = line.trim();
			if (trimmedLine.startsWith(LOCAL_DECLARATION)) {
				local = true;
				trimmedLine = trimmedLine.substring(LOCAL_DECLARATION.length()).trim();
			}
			String declaredObject = trimmedLine.substring(trimmedLine.indexOf(' ') + 1).trim();
			if (declaredObject.contains("="))
				declaredObject = declaredObject.substring(0, declaredObject.indexOf('=')).trim();
				// todo replace this with a function that works with dialog arrays
			else if (trimmedLine.startsWith(DIALOG_DECLARATION)) {
				if (!local)
					dialogNames.add(declaredObject);
			}
		}

		// Lobby configuration
		if (Arrays.asList("config", "InitCustomPlayerSlots").contains(insideFunction)) {
			if (line.contains(SETPLAYERCOLOR)) {
				String[] params = getParamsFromLine(line);
				if (params != null && params.length == 2 && params[0].equals(toMainPlayerFunc())) {
					// recolor players to blue and yellow
					assert params != null;
					append(sb, line.replace(params[1], CONVERTPLAYERCOLOR + "1)"));
					String secondaryColorLine = line.replace(params[1], CONVERTPLAYERCOLOR + "4)");
					for (W3I.Player player : secondaryPlayers) {
						append(sb, secondaryColorLine.replace(toMainPlayerFunc(), toPlayerFunc(player.getNum())));
					}
				}
				return;
			}
		}
		//if (!triggersAdded && line.contains("function Trig")) {
		// define functions
		//triggersAdded = true;
		//}

		boolean appendLine = true;

		{
			String call = getTrimmedCallFromLine(line);
			String[] params;
			if (call != null) {
				params = getParamsFromLine(line);
				call = getTrimmedCallFromLine(line);
				if (call != null) {
					for (String playerNumberCall : Arrays.asList("SetPlayers", "SetTeams"))
						if (call.equals(playerNumberCall)) {
							line = "call " + call + "(" + info.getPlayers().size() + ")";
							break;
						}
					if (!pauseLockAdded && call.equals("TriggerSleepAction"))
						append(sb, PAUSE_LOCK_CODE);

					if (params != null) {
						CoopCallAdjustments.AdjustmentDetails ad = CoopCallAdjustments.callParameters.get(call);
						if (ad != null) {
							appendLine = false;

							if (ad.pointIndex != null) {
								append(sb, "set " + TEMP_POINT_NAME + "=" + params[ad.pointIndex]);
								params[ad.pointIndex] = TEMP_POINT_NAME;
								line = toCallString(call, params);
							}

							if (!ad.isLocal)
								append(sb, line);

							appendCallWithReplacement(sb, call, params, ad);
						}
					}
				}
			}

			if (appendLine)
				append(sb, line);

			pauseLockAdded = false;

			if (call != null)
				if (!Arrays.asList("main", "RunInitializationTriggers").contains(insideFunction))
					for (String function : Arrays.asList("TriggerExecute", "TriggerExecuteWait", "ConditionalTriggerExecute"))
						if (call.equals(function)) {
							append(sb, PAUSE_LOCK_CODE);
							pauseLockAdded = true;
							break;
						}

			if ("CustomDefeatBJ".equals(call))
				append(sb, "call " + ARCHON_PREFIX + "PauseGameOn()");
		}

		if (line.trim().equals("globals")) {
			for (String resourceName : getAllResourceNames())
				append(sb, "trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync");
			append(sb, "trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare",
					"force " + FORCE_NAME,
					"location " + TEMP_POINT_NAME,
					//"integer array " + ARCHON_PREFIX + "LBF", // Last Selected Building Food
					"integer " + ARCHON_PREFIX + "LBF=0", // Last Selected Building Food
					"integer array " + PLAYER_ID_ARRAY,
					"integer array " + PLAYER_INDEX_ARRAY,
					"player " + AUX_PLAYER,
					"hashtable " + GENERAL_HASHTABLE + "=InitHashtable()",
					"hashtable " + SELECTION_HASHTABLE + "=InitHashtable()",
					"gamecache " + SYNC_CACHE + "=InitGameCache(\"" + SYNC_CACHE + "\")",
					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "Select",
					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "BF", // Building Food selection
					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP", // Building Food periodic
					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "DS", // Dialog Sync
					"boolean " + ARCHON_PREFIX + "AlliesMenuAvail=false",
					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG", // player left game
					"boolean " + ARCHON_PREFIX + "GIP=false", // game is paused
					"integer " + ARCHON_PREFIX + "GCS=0", // game cache is saving (count)
					"boolean " + ARCHON_PREFIX + "TDS=false", // time of day suspended
					"boolean " + ARCHON_PREFIX + "DIP=false", // dialog is pausing
					"boolean " + ARCHON_PREFIX + "LUP=false", // load unpauses
					"boolean " + ARCHON_PREFIX + "LRD=false", // load reloads
					"boolean " + ARCHON_PREFIX + "CIP=false" // cache is pausing
					//"gamespeed " + ARCHON_PREFIX + "RGS=MAP_SPEED_NORMAL" // real game speed
			);
			append(sb, "trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL");
			if (WITH_PAUSE_COMMAND)
				append(sb, "trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest");
			if (WITH_CUSTOM_GAMECACHE)
				append(sb, "integer array " + ABILITY_ARRAY,
						"integer " + ABILITY_ARRAY_SIZE + "=0",
						"integer " + FILE_LIBRARY_PREFIX + "CT=0", // Counter
						"integer array " + FILE_LIBRARY_PREFIX + "LS", // List
						"integer array " + FILE_LIBRARY_PREFIX + "AL", // AbilityList
						"boolean " + FILE_LIBRARY_PREFIX + "ReadEnabled=false",
						"string array " + FILE_LIBRARY_PREFIX + "FN", // Filename
						"string array " + FILE_LIBRARY_PREFIX + "BF", // Buffer
						"string " + ARCHON_PREFIX + "Content",
						"boolean " + ARCHON_PREFIX + "ContentRead=false",
						//"string array XT87ReadLines",
						//"boolean XT87ReadLinesAreRead=false",
						"boolean " + ARCHON_PREFIX + "RdLock=false", // Read lock
						"integer " + ARCHON_PREFIX + "RdWait=0", // Read waiting count
						"boolean " + ARCHON_PREFIX + "RdWaitLock=false" // Read waiting count
				);
		} else if (line.contains("SetForceAllianceStateBJ("))
			append(sb, "call " + ENFORCE_ARCHON + "()");

		if (line.contains(DEFINE_START_LOC + mainPlayer.getNum() + ","))
			for (W3I.Player player : secondaryPlayers)
				append(sb, line.replace(DEFINE_START_LOC + mainPlayer.getNum(), DEFINE_START_LOC + player.getNum()));
	}
}
