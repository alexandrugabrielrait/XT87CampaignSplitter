package net.xetanth87.campaignsplitter.rewriters;

import net.moonlightflower.wc3libs.bin.app.W3I;
import net.xetanth87.campaignsplitter.MapInjector;
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
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_VAR_REPLACE;
import static net.xetanth87.campaignsplitter.util.XT87Utils.putWithCount;

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
	private static final String LAST_CREATED_COOP_CACHE = ARCHON_PREFIX + "_lastCreatedGameCache";
	private static final String PLAYER_ID_ARRAY = ARCHON_PREFIX + "IDA";
	private static final String PLAYER_INDEX_ARRAY = ARCHON_PREFIX + "PIA";
	private static final String AUX_PLAYER = ARCHON_PREFIX + "AXP";
	private static final String GENERAL_HASHTABLE = ARCHON_PREFIX + "GHT";
	private static final String SELECTION_HASHTABLE = ARCHON_PREFIX + "FXT";
	private static final String ABILITY_ARRAY = ARCHON_PREFIX + "AbilityArray";
	private static final String ABILITY_ARRAY_SIZE = ABILITY_ARRAY + "SIZE";
	private static final String HASH_ARRAY = ARCHON_PREFIX + "HA";

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
	private static final int GH_INT_TIMER_CALLBACK = 1;

	private static final int GH_REAL_PAUSED_HP = 0;
	private static final int GH_REAL_PAUSED_MP = 1;
	private static final int GH_REAL_TIMER_REMAINING = 2;
	private static final int GH_REAL_TIMER_TIMEOUT = 3;

	private static List<String> getCommonHashKeys() {
		return Arrays.asList(CACHE_CAMPAIGN_MKEY, /*CACHE_MKEY_PREFIX,*/ CACHE_CAMPAIGNFILE_KEY, CACHE_INTEGER_KEY, CACHE_REAL_KEY, CACHE_BOOL_KEY,
				CACHE_STRING_KEY, CACHE_UNIT_KEY, /*CACHE_FLUSH_KEY,*/ CACHE_COUNT_KEY);
	}

	private static Map<String, Integer> commonHashes = new HashMap<>() {
		{
			for (String key : getCommonHashKeys())
				putWithCount(this, key);
		}
	};

	private static int getCommonHashIndex(String key) {
		return commonHashes.get(key);
	}

	private static String getCommonHashFunc(String key) {
		return HASH_ARRAY + "[" + getCommonHashIndex(key) + "]";
	}

	public static final int UNIT_CACHE_FORMAT = 0;

	public static final String CACHE_ID_SUFFIX = "id";

	public static final String CACHE_XP_SUFFIX = "xp";
	public static final String CACHE_STR_SUFFIX = "aS";
	public static final String CACHE_AGI_SUFFIX = "aA";
	public static final String CACHE_INT_SUFFIX = "aI";
	public static final String CACHE_UNSPEND_POINTS_SUFFIX = "uP";
	public static final String CACHE_HEALTH_MAX_SUFFIX = "hp";
	public static final String CACHE_MANA_MAX_SUFFIX = "mp";
	public static final String CACHE_ATTACK_DAMAGE_BASE_SUFFIX = "dB";
	public static final String CACHE_ARMOR_SUFFIX = "ar";

	public static final String CACHE_ABILITY_COUNT_SUFFIX = "C";
	public static final String CACHE_ABILITY_ID_SUFFIX = "A";
	public static final String CACHE_ABILITY_LVL_SUFFIX = "L";

	public static final String CACHE_INVENTORY_SIZE_SUFFIX = "S";
	public static final String CACHE_ITEM_ID_SUFFIX = "I";
	public static final String CACHE_ITEM_CHARGE_SUFFIX = "G";
	public static final String CACHE_ITEM_BITS_SUFFIX = "B";

	private static List<String> getBasicFields() {
		return Arrays.asList(CACHE_XP_SUFFIX, CACHE_STR_SUFFIX, CACHE_AGI_SUFFIX, CACHE_INT_SUFFIX, CACHE_UNSPEND_POINTS_SUFFIX,
				CACHE_HEALTH_MAX_SUFFIX, CACHE_MANA_MAX_SUFFIX, CACHE_ATTACK_DAMAGE_BASE_SUFFIX + 0, CACHE_ATTACK_DAMAGE_BASE_SUFFIX + 1);
	}

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

	public boolean triggersAdded, mainStartAdded, insideConfig;
	private boolean pauseLockAdded;
	public W3I info;
	public W3I.Player mainPlayer;
	public List<W3I.Player> secondaryPlayers;
	public List<Integer> allPlayerNumbers;
	public List<String> dialogNames;
	public Map<String, Integer> cacheFieldMap;

	public CoopRewriter(MapInjector mi, W3I info, W3I.Player mainPlayer, List<W3I.Player> secondaryPlayers) {
		super(mi, "AS");
		this.info = info;
		this.mainPlayer = mainPlayer;
		this.secondaryPlayers = secondaryPlayers;
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
					"    local string contents=BlzGetTriggerSyncData()" + JASS_DELIM +
							"    local integer len=StringLength(contents)" + JASS_DELIM +
							"    local string char" + JASS_DELIM +
							"    local string buffer=\"\"" + JASS_DELIM +
							"    local integer curLine=0" + JASS_DELIM +
							"    local integer i=0" + JASS_DELIM +
							"    if len<=0 then" + JASS_DELIM +
							"        set XT87ReadLines[0]=null" + JASS_DELIM +
							"    else" + JASS_DELIM +
							"        loop" + JASS_DELIM +
							"            exitwhen i > len" + JASS_DELIM +
							"            set char=SubString(contents,i,i+1)" + JASS_DELIM +
							"            if char==\"\\n\" then" + JASS_DELIM +
							"                set XT87ReadLines[curLine]=buffer" + JASS_DELIM +
							"                set curLine=curLine+1" + JASS_DELIM +
							"                set buffer=\"\"" + JASS_DELIM +
							"            else" + JASS_DELIM +
							"                set buffer=buffer+char" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"        set XT87ReadLines[curLine]=buffer" + JASS_DELIM +
							"    endif" + JASS_DELIM +
							"set XT87ReadLinesAreRead=true",
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
						"local string c=LoadStr(" + GENERAL_HASHTABLE + "," + GH_INT_TIMER_CALLBACK + ",h)" + JASS_DELIM +
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
						"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_INT_TIMER_CALLBACK + ",h,c)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call RemoveSavedString(" + GENERAL_HASHTABLE + "," + GH_INT_TIMER_CALLBACK + ",h)" + JASS_DELIM +
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
				"call DisplayTimedTextToPlayer(GetLocalPlayer(),0,0,0,GetLocalizedString(s)+\": |cffffcc00\"+l+\"|r\")" + JASS_DELIM +
						"if b then" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "PUGL()" + JASS_DELIM +
						END_IF);
		addFunctionConfiguration("ChangeLevel", "string s, boolean b", null,
				"call " + ARCHON_PREFIX + "HandleSLM(\"CREATE\",s,true)");
		addFunctionConfiguration("LoadGame", "string s, boolean b", null,
				"call " + ARCHON_PREFIX + "HandleSLM(\"LOAD_GAME\",s,true)");
		addFunctionConfiguration("SaveGame", "string s", null,
				"call " + ARCHON_PREFIX + "HandleSLM(\"SAVE_GAME\",s,false)");
		addFunctionConfiguration("LoadGameBJ", "string s, boolean b", null,
				"call " + ARCHON_PREFIX + "LoadGame(s,b)");
		addFunctionConfiguration("SaveAndChangeLevelBJ", "string s, string l, boolean b", null,
				"call " + ARCHON_PREFIX + "SaveGame(s)" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "ChangeLevel(l,b)");
		addFunctionConfiguration("SaveAndLoadGameBJ", "string s, string l, boolean b", null,
				"call " + ARCHON_PREFIX + "SaveGame(s)" + JASS_DELIM +
						"call " + ARCHON_PREFIX + "LoadGame(l,b)");

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

	private int getCacheFieldIndex(String key) {
		return putWithCount(cacheFieldMap, key);
	}

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

	public void appendCallWithReplacement(String call, String[] params, CoopCallAdjustments.AdjustmentDetails ad, StringBuffer sb) {
		for (int index : ad.getParameterIndices()) {
			String referencedPlayer = params[index];
			Integer referencedPlayerId = getPlayerIdFromPlayerFunc(referencedPlayer);
			boolean runtimeResolve = referencedPlayerId == null;

			if (runtimeResolve)
				append("    if (" + referencedPlayer + ")==" + toMainPlayerFunc() + " then", sb);
			else if (referencedPlayerId != mainPlayer.getNum())
				continue;

			if (ad.isLocal)
				append(toCallString(call, params, index, "GetLocalPlayer()"), sb);
			else
				secondaryPlayers.stream().map(x -> toCallString(call, params, index, toPlayerFunc(x.getNum()))).forEach(s -> append(s, sb));

			if (runtimeResolve)
				append("    " + END_IF, sb);
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
		return "LoadStr(" + cache + "," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")";
	}

	private String loopOverPlayers(String var, String code) {
		return "loop" + JASS_DELIM +
				"exitwhen " + var + ">=" + allPlayerNumbers.size() + JASS_DELIM +
				code + JASS_DELIM +
				"set " + var + "=" + var + "+1" + JASS_DELIM +
				"endloop";
	}

	@Override
	public void onEnterGlobals(StringBuffer sb) {
		append("timer array " + ARCHON_PREFIX + "ATA" + JASS_DELIM + // All Timer Array
				"integer " + ARCHON_PREFIX + "ATL=0", sb); // All Timer Array Length
	}

	@Override
	public void onExitGlobals(StringBuffer sb) {
		if (WITH_CUSTOM_GAMECACHE) {
			// region FileIO library
			append("function " + FILE_LIBRARY_PREFIX + "open takes string f returns integer" + JASS_DELIM + // filename f
					"            local integer t=" + FILE_LIBRARY_PREFIX + "LS[0]" + JASS_DELIM + // this t
					"            if t==0 then" + JASS_DELIM +
					"                set t=" + FILE_LIBRARY_PREFIX + "CT+1" + JASS_DELIM +
					"                set " + FILE_LIBRARY_PREFIX + "CT=t" + JASS_DELIM +
					"            else" + JASS_DELIM +
					"                set " + FILE_LIBRARY_PREFIX + "LS[0]=" + FILE_LIBRARY_PREFIX + "LS[t]" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "FN[t]=f" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "BF[t]=null" + JASS_DELIM +
					"            return t" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					// This is used to detect invalid characters which aren't supported in preload files.
					"        function " + FILE_LIBRARY_PREFIX + "write takes integer t,string s returns integer" + JASS_DELIM + // this t, contents s
					"            local integer i=0" + JASS_DELIM +
					"            local integer c=0" + JASS_DELIM +
					"            local integer l=StringLength(s)" + JASS_DELIM +
					"            local integer v=0" + JASS_DELIM + // lev v
					"            local string p=\"-\"" + JASS_DELIM + // prefix p
					"            local string k" + JASS_DELIM + // chunk k
					"            set " + FILE_LIBRARY_PREFIX + "BF[t]=null" + JASS_DELIM +
					// Check if the string is empty. If null,the s will be cleared.
					"            if (s==\"\") then" + JASS_DELIM +
					"                set l=l+1" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					// Begin to generate the file
					"            call PreloadGenClear()" + JASS_DELIM +
					"            call PreloadGenStart()" + JASS_DELIM +
					"            loop" + JASS_DELIM +
					"                exitwhen i>=l" + JASS_DELIM +
					"                set v=0" + JASS_DELIM +
					"                set k=SubString(s,i,i+" + FILE_LIBRARY_PRELOAD_LIMIT + ")" + JASS_DELIM +
					"                call Preload(\"\\\" )\\ncall BlzSetAbilityTooltip(\"+I2S(" + FILE_LIBRARY_PREFIX + "AL[c])+\",\\\"\"+p+k+\"\\\",\"+I2S(v)+\")\\n//\")" + JASS_DELIM +
					"                set i=i+" + FILE_LIBRARY_PRELOAD_LIMIT + JASS_DELIM +
					"                set c=c+1" + JASS_DELIM +
					"            endloop" + JASS_DELIM +
					"            call Preload(\"\\\" )\\nendfunction\\nfunction a takes nothing returns nothing\\n //\")" + JASS_DELIM +
					"            call PreloadGenEnd(" + FILE_LIBRARY_PREFIX + "FN[t])" + JASS_DELIM +
					"            return t" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"        function " + FILE_LIBRARY_PREFIX + "clear takes integer t returns integer" + JASS_DELIM +
					"            return " + FILE_LIBRARY_PREFIX + "write(t,null)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"        function " + FILE_LIBRARY_PREFIX + "readPreload takes integer t returns string" + JASS_DELIM +
					"            local integer i=0" + JASS_DELIM +
					"            local integer v=0" + JASS_DELIM +
					"            local string array g" + JASS_DELIM + // original g
					"            local string k=\"\"" + JASS_DELIM +
					"            local string o=\"\"" + JASS_DELIM + // output o
					"            loop" + JASS_DELIM +
					"                exitwhen i==" + FILE_LIBRARY_ABILITY_COUNT + JASS_DELIM +
					"                set g[i]=BlzGetAbilityTooltip(" + FILE_LIBRARY_PREFIX + "AL[i],0)" + JASS_DELIM +
					"                set i=i+1" + JASS_DELIM +
					"            endloop" + JASS_DELIM +
					// Execute the preload file
					"            call Preloader(" + FILE_LIBRARY_PREFIX + "FN[t])" + JASS_DELIM +
					// Read the output"
					"            set i=0" + JASS_DELIM +
					"            loop" + JASS_DELIM +
					"                exitwhen i==" + FILE_LIBRARY_ABILITY_COUNT + JASS_DELIM +
					"                " + JASS_DELIM +
					"                set v=0" + JASS_DELIM +
					// Read from ability index 1 instead of 0 if
					// backwards compatability is enabled
					// Make sure the tooltip has changed
					"                set k=BlzGetAbilityTooltip(" + FILE_LIBRARY_PREFIX + "AL[i],v)" + JASS_DELIM +
					"                if k==g[i] then" + JASS_DELIM +
					"                    if i==0 and o==\"\" then" + JASS_DELIM +
					"                        return null" + JASS_DELIM + // empty file
					"                    endif" + JASS_DELIM +
					"                    return o" + JASS_DELIM +
					END_IF + JASS_DELIM +
					// Check if the file is an empty string or null
					"                    if i==0 then" + JASS_DELIM +
					"                        if SubString(k,0,1)!=\"-\" then" + JASS_DELIM +
					"                            return null" + JASS_DELIM + // empty file
					"                        endif" + JASS_DELIM +
					"                        set k=SubString(k,1,StringLength(k))" + JASS_DELIM +
					END_IF + JASS_DELIM +
					// Remove the prefix
					"                if i>0 then" + JASS_DELIM +
					"                    set k=SubString(k,1,StringLength(k))" + JASS_DELIM +
					"                endif" + JASS_DELIM +
					// Restore the tooltip and append the chunk
					"                call BlzSetAbilityTooltip(" + FILE_LIBRARY_PREFIX + "AL[i],g[i],v)" + JASS_DELIM +
					"                set o=o+k" + JASS_DELIM +
					"                set i=i+1" + JASS_DELIM +
					"            endloop" + JASS_DELIM +
					"            return o" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"        function " + FILE_LIBRARY_PREFIX + "close takes integer t returns nothing" + JASS_DELIM +
					"            if " + FILE_LIBRARY_PREFIX + "BF[t]!=null then" + JASS_DELIM +
					"                call " + FILE_LIBRARY_PREFIX + "write(t," + FILE_LIBRARY_PREFIX + "readPreload(t)+" + FILE_LIBRARY_PREFIX + "BF[t])" + JASS_DELIM +
					"                set " + FILE_LIBRARY_PREFIX + "BF[t]=null" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "LS[t]=" + FILE_LIBRARY_PREFIX + "LS[0]" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "LS[0]=t" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"        function " + FILE_LIBRARY_PREFIX + "readEx takes integer t,boolean c returns string" + JASS_DELIM + // close c
					"            local string o=" + FILE_LIBRARY_PREFIX + "readPreload(t)" + JASS_DELIM +
					"            local string b=" + FILE_LIBRARY_PREFIX + "BF[t]" + JASS_DELIM + // buf b
					"            if c then" + JASS_DELIM +
					"                call " + FILE_LIBRARY_PREFIX + "close(t)" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					"            if o==null then" + JASS_DELIM +
					"                return b" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					"            if b!=null then" + JASS_DELIM +
					"                set o=o+b" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					"            return o" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
//							"        function " + FILE_LIBRARY_PREFIX + "read takes integer t returns string" + JASS_DELIM +
//							"            return " + FILE_LIBRARY_PREFIX + "readEx(t,false)" + JASS_DELIM +
//							END_FUNCTION + JASS_DELIM +
//							"        function " + FILE_LIBRARY_PREFIX + "readAndClose takes integer t returns string" + JASS_DELIM +
//							"            return " + FILE_LIBRARY_PREFIX + "readEx(t,true)" + JASS_DELIM +
//							END_FUNCTION + JASS_DELIM +
//							"        function " + FILE_LIBRARY_PREFIX + "appendBuffer takes integer t,string s returns integer" + JASS_DELIM +
//							"            set " + FILE_LIBRARY_PREFIX + "BF[t]=" + FILE_LIBRARY_PREFIX + "BF[t]+s" + JASS_DELIM +
//							"            return t" + JASS_DELIM +
//							END_FUNCTION + JASS_DELIM +
//							"        function " + FILE_LIBRARY_PREFIX + "readBuffer takes integer t returns string" + JASS_DELIM +
//							"            return " + FILE_LIBRARY_PREFIX + "BF[t]" + JASS_DELIM +
//							END_FUNCTION + JASS_DELIM +
//							"        function " + FILE_LIBRARY_PREFIX + "writeBuffer takes integer t,string s returns nothing" + JASS_DELIM +
//							"            set " + FILE_LIBRARY_PREFIX + "BF[t]=s" + JASS_DELIM +
//							END_FUNCTION + JASS_DELIM +
					"        function " + FILE_LIBRARY_PREFIX + "create takes string f returns integer" + JASS_DELIM +
					"            return " + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(f),\"\")" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					//Implemented from module FileIO__FileInit:
					"        function " + FILE_LIBRARY_PREFIX + "Init takes nothing returns nothing" + JASS_DELIM +
					// We can't use a single ability with multiple levels because
					// tooltips return the first level's value if the value hasn't
					// been set. This way we don't need to edit any object editor data.
					"            set " + FILE_LIBRARY_PREFIX + "AL[0]='Amls'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[1]='Aroc'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[2]='Amic'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[3]='Amil'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[4]='Aclf'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[5]='Acmg'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[6]='Adef'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[7]='Adis'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[8]='Afbt'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "AL[9]='Afbk'" + JASS_DELIM +
					"            set " + FILE_LIBRARY_PREFIX + "ReadEnabled=(" + FILE_LIBRARY_PREFIX + "readEx((" + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(\"FileTester.pld\"),\"" + FILE_IO_PREFIX + "\")),true))==\"" + FILE_IO_PREFIX + "\"" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"    function " + FILE_IO_PREFIX + "Write takes string f,string s returns nothing" + JASS_DELIM +
					"        call " + FILE_LIBRARY_PREFIX + "close(" + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(f),s))" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"    function " + FILE_IO_PREFIX + "Read takes string f returns string" + JASS_DELIM +
					"        return " + FILE_LIBRARY_PREFIX + "readEx(" + FILE_LIBRARY_PREFIX + "open(f),true)" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region selection functions
			append("function " + ARCHON_PREFIX + "ShowSelection takes boolean b returns nothing" + JASS_DELIM +
					"local integer c" + JASS_DELIM +
					"local integer i" + JASS_DELIM +
					"local integer j=0" + JASS_DELIM +
					"local real a=0" + JASS_DELIM +
					"if b then" + JASS_DELIM +
					"set a=1" + JASS_DELIM +
					END_IF + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"ShowSelection \"+R2S(a))" + JASS_DELIM) +
					loopOverPlayers("j",
							"set c=LoadInteger(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],0)" + JASS_DELIM +
									"set i=1" + JASS_DELIM +
									"loop" + JASS_DELIM +
									"exitwhen i>c" + JASS_DELIM +
									"call BlzSetSpecialEffectScale(LoadEffectHandle(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],i),a)" + JASS_DELIM +
									"set i=i+1" + JASS_DELIM +
									"endloop" + JASS_DELIM) + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "ClearSelection takes nothing returns nothing" + JASS_DELIM +
					"local integer c" + JASS_DELIM +
					"local integer i" + JASS_DELIM +
					"local integer j=0" + JASS_DELIM +
					loopOverPlayers("j",
							"set c=LoadInteger(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],0)" + JASS_DELIM +
									"set i=1" + JASS_DELIM +
									"loop" + JASS_DELIM +
									"exitwhen i>c" + JASS_DELIM +
									"call DestroyEffect(LoadEffectHandle(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],i))" + JASS_DELIM +
									"set i=i+1" + JASS_DELIM +
									"endloop" + JASS_DELIM +
									"call SaveInteger(" + SELECTION_HASHTABLE + "," + PLAYER_ID_ARRAY + "[j],0,0)") + JASS_DELIM +
					"call ClearSelection()" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "SelectUnitSingle takes unit u returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "ClearSelection()" + JASS_DELIM +
					"call SelectUnit(u,true)" + JASS_DELIM +
					END_FUNCTION, sb);
//					append("function " + ARCHON_PREFIX + "SelectGroupBJ takes group g returns nothing" + JASS_DELIM +
//							"call " + ARCHON_PREFIX + "ClearSelection()" + JASS_DELIM +
//							"call ForGroup(g,function SelectGroupBJEnum)" + JASS_DELIM +
//							END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "ClearSelectionForPlayer takes player p returns nothing" + JASS_DELIM +
					"if p==" + toMainPlayerFunc() + " then" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "ClearSelection()" + JASS_DELIM +
					END_IF + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "EnableUserControl takes boolean b returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "ShowSelection(b)" + JASS_DELIM +
					"call EnableUserControl(b)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "SetUserControlForceOn takes force f returns nothing" + JASS_DELIM +
					"if IsPlayerInForce(" + toMainPlayerFunc() + ",f) then" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "EnableUserControl(true)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "SetUserControlForceOff takes force f returns nothing" + JASS_DELIM +
					"if IsPlayerInForce(" + toMainPlayerFunc() + ",f) then" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "EnableUserControl(false)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "CinematicModeExBJ takes boolean b, force f, real t returns nothing" + JASS_DELIM +
					"if IsPlayerInForce(" + toMainPlayerFunc() + ",f) then" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "ShowSelection(not b)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"call CinematicModeExBJ(b,f,t)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "CinematicModeBJ takes boolean b, force f returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "CinematicModeExBJ(b,f,bj_CINEMODE_INTERFACEFADE)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "PauseGameOn takes nothing returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(true)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "PauseGameOff takes nothing returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(false)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "SuspendTimeOfDay takes boolean b returns nothing" + JASS_DELIM +
					"set " + ARCHON_PREFIX + "TDS=b" + JASS_DELIM +
					"call SuspendTimeOfDay(b)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "UseTimeOfDayBJ takes boolean b returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "SuspendTimeOfDay(not b)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "PauseCompAI takes player p, boolean b returns nothing" + JASS_DELIM +
					"call PauseCompAI(p,b)" + JASS_DELIM +
					"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED_PLAYER + ",GetPlayerId(p),b)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "PauseAllUnitsBJ takes boolean b returns nothing" + JASS_DELIM +
					"local integer i=0" + JASS_DELIM +
					"call PauseAllUnitsBJ(b)" + JASS_DELIM +
					"loop" + JASS_DELIM +
					"exitwhen i==bj_MAX_PLAYER_SLOTS" + JASS_DELIM +
					"if GetPlayerController(Player(i))==MAP_CONTROL_COMPUTER then" + JASS_DELIM +
					"call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_IS_PAUSED_PLAYER + ",i,b)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set i=i+1" + JASS_DELIM +
					"endloop" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region custom functions
			append("function " + ARCHON_PREFIX + "WatchSkill takes integer a returns boolean" + JASS_DELIM +
					"    if not LoadBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_HAS_ABILITY + ",a) then" + JASS_DELIM +
					"        set " + ABILITY_ARRAY + "[" + ABILITY_ARRAY_SIZE + "]=a" + JASS_DELIM +
					"        call SaveBoolean(" + GENERAL_HASHTABLE + "," + GH_BOOL_HAS_ABILITY + ",a,true)" + JASS_DELIM +
					"        set " + ABILITY_ARRAY_SIZE + "=" + ABILITY_ARRAY_SIZE + "+1" + JASS_DELIM +
					"        return true" + JASS_DELIM +
					"    else" + JASS_DELIM +
					"        return false" + JASS_DELIM +
					END_IF + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "S2B takes string s returns boolean" + JASS_DELIM +
					"    if s==\"t\" then" + JASS_DELIM +
					"        return true" + JASS_DELIM +
					"    endif" + JASS_DELIM +
					"    return false" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "B2S takes boolean b returns string" + JASS_DELIM +
					"    if b then" + JASS_DELIM +
					"        return \"t\"" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"    return \"f\"" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "IS62 takes integer n returns string" + JASS_DELIM +
					"    local string result=\"\"" + JASS_DELIM +
					"    local integer remainder" + JASS_DELIM +
					"    local integer b=62" + JASS_DELIM +
					"    local integer d" + JASS_DELIM +
					"    local string s=\"\"" + JASS_DELIM +
					"if n<0 then" + JASS_DELIM +
					"set s=\"-\"" + JASS_DELIM +
					"set n=-n" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen n<=0" + JASS_DELIM +
					"        set d=" + moduloFunc("n", "b") + JASS_DELIM +
					"        set result=SubString(\"" + XT87Utils.BASE62_CHARS + "\",d,d+1)+result" + JASS_DELIM +
					"        set n=n/b" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					"    return s+result" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "SS62 takes string s returns string" + JASS_DELIM +
					"if s==null then" + JASS_DELIM +
					"return \"null\"" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"return \"s\"+s" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "S62I takes string s returns integer" + JASS_DELIM +
					"    local integer result=0" + JASS_DELIM +
					"    local integer b=62" + JASS_DELIM +
					"    local string d" + JASS_DELIM +
					"    local integer i" + JASS_DELIM +
					"    local integer n=1" + JASS_DELIM +
					"    if SubString(s,0,1)==\"-\" then" + JASS_DELIM +
					"set n=-1" + JASS_DELIM +
					"set s=SubString(s,1,StringLength(s))" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen s==\"\"" + JASS_DELIM +
					"        " + JASS_DELIM +
					"        set d=SubString(s,0,1)" + JASS_DELIM +
					"        set i=0" + JASS_DELIM +
					"        " + JASS_DELIM +
					"        loop" + JASS_DELIM +
					"            exitwhen SubString(\"" + XT87Utils.BASE62_CHARS + "\",i,i+1)==d" + JASS_DELIM +
					"            set i=i+1" + JASS_DELIM +
					"            if i>=b then" + JASS_DELIM +
					"                return 0" + JASS_DELIM +
					"            endif" + JASS_DELIM +
					"        endloop" + JASS_DELIM +
					"        " + JASS_DELIM +
					"        set result=result*b+i" + JASS_DELIM +
					"        set s=SubString(s,1,StringLength(s))" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					"    " + JASS_DELIM +
					"    return n*result" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "S62S takes string s returns string" + JASS_DELIM +
					"if SubString(s,0,1)==\"s\" then" + JASS_DELIM +
					"return SubString(s,1,StringLength(s))" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"return null" + JASS_DELIM +
					END_FUNCTION, sb);
			if (WITH_FULL_ENCODING)
				append("function " + ARCHON_PREFIX + "RS62 takes real r returns string" + JASS_DELIM +
						"    local string s=R2S(r)" + JASS_DELIM +
						"    local integer i=0" + JASS_DELIM +
						"    local integer len=StringLength(s)" + JASS_DELIM +
						"    loop" + JASS_DELIM +
						"    exitwhen i>=len" + JASS_DELIM +
						"    if SubString(s,i,i+1)==\".\" then" + JASS_DELIM +
						"    return " + ARCHON_PREFIX + "IS62(S2I(SubString(s,0,i)))+\".\"+" + ARCHON_PREFIX + "IS62(S2I(SubString(s,i+1,len)))" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"set i=i+1" + JASS_DELIM +
						"endloop" + JASS_DELIM +
						"    return " + ARCHON_PREFIX + "IS62(0)+\".\"+" + ARCHON_PREFIX + "IS62(0)" + JASS_DELIM +
						END_FUNCTION + JASS_DELIM +
						"function " + ARCHON_PREFIX + "S62R takes string s returns real" + JASS_DELIM +
						"    local integer i=0" + JASS_DELIM +
						"    local integer len=StringLength(s)" + JASS_DELIM +
						"    loop" + JASS_DELIM +
						"    exitwhen i==len" + JASS_DELIM +
						"    if SubString(s,i,i+1)==\".\" then" + JASS_DELIM +
						"    return S2R(I2S(" + ARCHON_PREFIX + "S62I(SubString(s,0,i)))+\".\"+I2S(" + ARCHON_PREFIX + "S62I(SubString(s,i+1,len))))" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"set i=i+1" + JASS_DELIM +
						"endloop" + JASS_DELIM +
						"    return 0.000" + JASS_DELIM +
						END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "WriteString takes string cn,string title,string v,string fK returns nothing" + JASS_DELIM +
					"local string p=\"XT87CampaignSplitter\\\\\"+cn+\"\\\\\"+title+\".pld\"" + JASS_DELIM +
					"    call " + FILE_IO_PREFIX + "Write(p,v+\"\\n\"+" + ARCHON_PREFIX + "IS62(StringHash(v+\"|\"+p+\"|\"+fK)))" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "SyncContents takes string p,string fK returns nothing" + JASS_DELIM +
					"    local integer i=0" + JASS_DELIM +
					"    local integer j=0" + JASS_DELIM +
					"    local integer len" + JASS_DELIM +
					"    local string content" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen not XT87ReadLinesLock" + JASS_DELIM +
					"        call TriggerSleepAction(0.01)" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					"if not " + ARCHON_PREFIX + "CIP then" + JASS_DELIM +
					"set " + ARCHON_PREFIX + "CIP=true" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(true)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set XT87ReadLinesLock=true" + JASS_DELIM +
					"    if GetLocalPlayer()==" + toMainPlayerFunc() + " then" + JASS_DELIM +
					"        set content=(" + FILE_LIBRARY_PREFIX + "readEx(" + FILE_LIBRARY_PREFIX + "open((p)),true)) // INLINED!!" + JASS_DELIM +
					"        if content==null then" + JASS_DELIM +
					"            set content=\"\"" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"        set len=StringLength(content)" + JASS_DELIM +
					// todo add gamecache flush index and mission key flush index to checksum
					"    loop" + JASS_DELIM +
					"    exitwhen i>=len" + JASS_DELIM +
					"    if SubString(content,i,i+1)==\"\\n\" then" + JASS_DELIM +
					"set j=i" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set i=i+1" + JASS_DELIM +
					"endloop" + JASS_DELIM +
					// compare checksums
					(COMPARE_CHECKSUMS ?
							("    if StringHash(SubString(content,0,j)+\"|\"+p+\"|\"+fK)!=" + ARCHON_PREFIX + "S62I(SubString(content,j+1,len)) then" + JASS_DELIM +
									"        set content=\"\"" + JASS_DELIM +
									END_IF + JASS_DELIM) :
							"") +
					DebugOnly("call BJDebugMsg(\">\"+content+\"<\")" + JASS_DELIM) +
					"        call BlzSendSyncData(\"XTContents\",content)" + JASS_DELIM +
					"    endif" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen XT87ReadLinesAreRead" + JASS_DELIM +
					"        call TriggerSleepAction(0.01)" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "ReadString takes string cn,string title,string fK returns string" + JASS_DELIM +
					"local string s" + JASS_DELIM +
					"    if GetPlayerSlotState(" + toMainPlayerFunc() + ")!=PLAYER_SLOT_STATE_PLAYING then" + JASS_DELIM +
					"        return null" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set XT87ReadLinesWaiting=XT87ReadLinesWaiting+1" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "SyncContents(\"XT87CampaignSplitter\\\\\"+cn+\"\\\\\"+title+\".pld\",fK)" + JASS_DELIM +
					"set s=XT87ReadLines[0]" + JASS_DELIM +
					"set XT87ReadLinesAreRead=false" + JASS_DELIM +
					"set XT87ReadLinesLock=false" + JASS_DELIM +
					"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1" + JASS_DELIM +
					"if XT87ReadLinesWaiting==0 and " + ARCHON_PREFIX + "CIP then" + JASS_DELIM +
					"set " + ARCHON_PREFIX + "CIP=false" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(false)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"return s" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "ReadUnit takes string cn,string title,string fK returns integer" + JASS_DELIM +
					"    local integer i" + JASS_DELIM +
					"set XT87ReadLinesWaiting=XT87ReadLinesWaiting+1" + JASS_DELIM +
					"    if GetPlayerSlotState(" + toMainPlayerFunc() + ")!=PLAYER_SLOT_STATE_PLAYING then" + JASS_DELIM +
					"        return 0" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"    call " + ARCHON_PREFIX + "SyncContents(\"XT87CampaignSplitter\\\\\"+cn+\"\\\\\"+title+\".pld\",fK)" + JASS_DELIM +
					"    if XT87ReadLines[0]==null then" + JASS_DELIM +
					"        return 0" + JASS_DELIM +
					END_IF + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"0)\"+XT87ReadLines[0])" + JASS_DELIM) +
					DebugOnly("call BJDebugMsg(\"1)\"+XT87ReadLines[1])" + JASS_DELIM) +
					DebugOnly("call BJDebugMsg(\"2)\"+XT87ReadLines[2])" + JASS_DELIM) +
					"    return " + ARCHON_PREFIX + "S62I(XT87ReadLines[0])" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region button add functions
			append("function " + ARCHON_PREFIX + "DialogAddButton takes dialog d,string s,integer h returns button" + JASS_DELIM +
					"local button b=DialogAddButton(d,s,h)" + JASS_DELIM +
					"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(b),s)" + JASS_DELIM +
					"return b" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "DialogAddQuitButton takes dialog d,boolean t,string s,integer h returns button" + JASS_DELIM +
					"local button b=DialogAddQuitButton(d,t,s,h)" + JASS_DELIM +
					"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(b),s)" + JASS_DELIM +
					"return b" + JASS_DELIM +
					END_FUNCTION, sb);
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("", "", "0"),
					new Triple<>("WithHotkey", ",integer h", "h")
			))
				append("function " + ARCHON_PREFIX + "DialogAddButton" + o.a + "BJ takes dialog d,string s" + o.b + " returns button" + JASS_DELIM +
						"set bj_lastCreatedButton=DialogAddButton(d,s," + o.c + ")" + JASS_DELIM +
						"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(bj_lastCreatedButton),s)" + JASS_DELIM +
						"return bj_lastCreatedButton" + JASS_DELIM +
						END_FUNCTION, sb);
			// endregion
			// region dialog display
			append("function " + ARCHON_PREFIX + "DialogDisplay takes player p, dialog d, boolean b returns nothing" + JASS_DELIM +
					"local integer i=1" + JASS_DELIM +
					"call DialogDisplay(p,d,b)" + JASS_DELIM +
					"if p==" + toMainPlayerFunc() + " then" + JASS_DELIM +
					loopOverPlayers("i", "call DialogDisplay(Player(" + PLAYER_ID_ARRAY + "[i]),d,b)") + JASS_DELIM +
					END_IF + JASS_DELIM +
					// todo dont set DIP to true if already paused
					"if " + ARCHON_PREFIX + "DIP!=b then" + JASS_DELIM +
					"set " + ARCHON_PREFIX + "DIP=b" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(b)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "DialogDisplayBJ takes boolean b, dialog d, player p returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "DialogDisplay(p,d,b)" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region DoFlushIndex function
			// uses m2 and flush prefixed k2
			append("function " + ARCHON_PREFIX + "DoFlushIndex takes hashtable q,string cn,string m,string k,integer c returns integer" + JASS_DELIM +
					"    local string s" + JASS_DELIM +
					"    local integer i=c" + JASS_DELIM +
					"local integer mh=StringHash(m)" + JASS_DELIM +
					"local integer kh=StringHash(k)" + JASS_DELIM +
					"if HaveSavedInteger(q,mh,kh) then" + JASS_DELIM +
					"    set i=LoadInteger(q,mh,kh)" + JASS_DELIM +
					"    if c!=0 then" + JASS_DELIM +
					"set i=i+c" + JASS_DELIM +
					"call SaveInteger(q,mh,kh,i)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"return i" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set s=" + ARCHON_PREFIX + "ReadString(cn,m+\"\\\\\"+k,null)" + JASS_DELIM +
					"if s!=null then" + JASS_DELIM +
					"   set i=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(s)+c" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"call SaveInteger(q,mh,kh,i)" + JASS_DELIM +
					//"if i!=0 then" + JASS_DELIM +
					//"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_INTEGER_KEY + "\",m2,k2)" + JASS_DELIM +
					//END_IF + JASS_DELIM +
					"return i" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region StoreFlushIndex function
			// uses m2 and flush prefixed k2
//					append("function " + ARCHON_PREFIX + "StoreFlushIndex takes hashtable q,string m,string k returns nothing" + JASS_DELIM +
//							"    local string s" + JASS_DELIM +
//							"    local integer i=0" + JASS_DELIM +
//							"local integer mh=StringHash(m)" + JASS_DELIM +
//							"local integer kh=StringHash(k)" + JASS_DELIM +
//							"    local string cn=" + getCacheNameFunc("q") + JASS_DELIM +
//							"if HaveSavedInteger(q,mh,kh) then" + JASS_DELIM +
//							"    set i=LoadInteger(q,mh,kh)" + JASS_DELIM +
//							"    if c!=0 then" + JASS_DELIM +
//							"set i=i+c" + JASS_DELIM +
//							"call SaveInteger(q,mh,kh,i)" + JASS_DELIM +
//							END_IF + JASS_DELIM +
//							"return i" + JASS_DELIM +
//							END_IF + JASS_DELIM +
//							"set s=" + ARCHON_PREFIX + "ReadString(cn,m+\"\\\\\"+k,null)" + JASS_DELIM +
//							"if s!=null then" + JASS_DELIM +
//							"   set i=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(s)+c" + JASS_DELIM +
//							END_IF + JASS_DELIM +
//							"call SaveInteger(q,mh,kh,i)" + JASS_DELIM +
//							"return i" + JASS_DELIM +
//							END_FUNCTION, sb);
			// endregion
			// region GameCache functions
			// region game cache basics natives
			append("function " + ARCHON_PREFIX + "InitGameCache takes string f returns hashtable" + JASS_DELIM +
					"    local hashtable q=InitHashtable()" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"InitGameCache \"+f)" + JASS_DELIM) +
					"    call SaveStr(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ",f)" + JASS_DELIM +
//		todo replace first mission key CACHE_CAMPAIGNFILE_KEY with CACHE_CAMPAIGN_MKEY
//						"    if not " + ARCHON_PREFIX + "HaveStoredInteger(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + ",StringHash(\"" + CACHE_FLUSH_KEY + "\")) then" +
//						"    call SaveInteger(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + ",StringHash(\"" + CACHE_FLUSH_KEY + "\"),0)" + JASS_DELIM +
//						END_IF + JASS_DELIM +
					"    return q" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function  " + ARCHON_PREFIX + "SaveGameCache takes hashtable q returns boolean" + JASS_DELIM +
					"    local integer i" + JASS_DELIM +
					"    local integer n" + JASS_DELIM +
					"    local integer j" + JASS_DELIM +
					"    local integer c" + JASS_DELIM +
					"    local integer id" + JASS_DELIM +
					"    local integer it" + JASS_DELIM +
					"    local integer mh" + JASS_DELIM +
					"    local boolean h" + JASS_DELIM +
					"    local string m2" + JASS_DELIM +
					"    local string k2" + JASS_DELIM +
					"    local string uD" + JASS_DELIM +
					"    local string cn=" + getCacheNameFunc("q") + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"SaveGameCache \"+cn)" + JASS_DELIM) +
					// todo simplify using foreach
					// save integers to files
					"    set i=0" + JASS_DELIM +
					"    set n=2*LoadInteger(q," + getCommonHashFunc(CACHE_INTEGER_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=n" + JASS_DELIM +
					"        set m2=LoadStr(q," + getCommonHashFunc(CACHE_INTEGER_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        set k2=LoadStr(q," + getCommonHashFunc(CACHE_INTEGER_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(LoadInteger(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					// save reals to files
					"    set i=0" + JASS_DELIM +
					"    set n=2*LoadInteger(q," + getCommonHashFunc(CACHE_REAL_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=n" + JASS_DELIM +
					"        set m2=LoadStr(q," + getCommonHashFunc(CACHE_REAL_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        set k2=LoadStr(q," + getCommonHashFunc(CACHE_REAL_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + getConversionFunc(ConversionFunctionType.REAL_TO_STRING) + "(LoadReal(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					// save bools to files
					"    set i=0" + JASS_DELIM +
					"    set n=2*LoadInteger(q," + getCommonHashFunc(CACHE_BOOL_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=n" + JASS_DELIM +
					"        set m2=LoadStr(q," + getCommonHashFunc(CACHE_BOOL_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        set k2=LoadStr(q," + getCommonHashFunc(CACHE_BOOL_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "B2S(LoadBoolean(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					// save strings to files
					"    set i=0" + JASS_DELIM +
					"    set n=2*LoadInteger(q," + getCommonHashFunc(CACHE_STRING_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=n" + JASS_DELIM +
					"        set m2=LoadStr(q," + getCommonHashFunc(CACHE_STRING_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        set k2=LoadStr(q," + getCommonHashFunc(CACHE_STRING_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "SS62(LoadStr(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					// save units to files
					"    set i=0" + JASS_DELIM +
					"    set n=2*LoadInteger(q," + getCommonHashFunc(CACHE_UNIT_KEY) + "," + getCommonHashFunc(CACHE_COUNT_KEY) + ")" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=n" + JASS_DELIM +
					"        set m2=LoadStr(q," + getCommonHashFunc(CACHE_UNIT_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        set k2=LoadStr(q," + getCommonHashFunc(CACHE_UNIT_KEY) + ",i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"        set mh=StringHash(m2)" + JASS_DELIM +
					"set id=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ID_SUFFIX + "\"))" + JASS_DELIM +
					"        set uD=" + ARCHON_PREFIX + "IS62(id)+\"\\n" + UNIT_CACHE_FORMAT + "\"" + JASS_DELIM +
					"        set h=IsHeroUnitId(id)" + JASS_DELIM +
					"        if h then", sb); // if hero
			for (String suffix : getBasicFields())
				append("            set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(LoadInteger(q,mh,StringHash(k2+\"" + suffix + "\")))", sb);
			append("            set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.REAL_TO_STRING) + "(LoadReal(q,mh,StringHash(k2+\"" + CACHE_ARMOR_SUFFIX + "\")))" + JASS_DELIM +
					"            set c=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_COUNT_SUFFIX + "\"))" + JASS_DELIM +
					"            set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(c)" + JASS_DELIM +
					"            set j=0" + JASS_DELIM +
					"            loop" + JASS_DELIM +
					"            exitwhen j>=c", sb);
			for (String suffix : Arrays.asList(CACHE_ABILITY_ID_SUFFIX, CACHE_ABILITY_LVL_SUFFIX))
				append("set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(LoadInteger(q,mh,StringHash(k2+\"" + suffix + "\"+I2S(j))))", sb);
			append("            set j=j+1" + JASS_DELIM +
					"            endloop" + JASS_DELIM +
					END_IF + JASS_DELIM + // endif hero
					"set c=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_INVENTORY_SIZE_SUFFIX + "\"))" + JASS_DELIM +
					"set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(c)" + JASS_DELIM +
					"        set j=0" + JASS_DELIM +
					"        loop" + JASS_DELIM +
					"        exitwhen j>=c" + JASS_DELIM +
					"        set it=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFFIX + "\"+I2S(j)))" + JASS_DELIM +
					"        set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(it)" + JASS_DELIM +
					"        if it!=0 then", sb);
			for (String suffix : Arrays.asList(CACHE_ITEM_CHARGE_SUFFIX, CACHE_ITEM_BITS_SUFFIX))
				append("set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(LoadInteger(q,mh,StringHash(k2+\"" + suffix + "\"+I2S(j))))", sb);
			append("        endif" + JASS_DELIM +
					"        set j=j+1" + JASS_DELIM +
					"        endloop" + JASS_DELIM +
					"        call " + ARCHON_PREFIX + "WriteString(cn,m2+\"\\\\\"+k2,uD," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					"    return false" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region Store natives
			append("function " + ARCHON_PREFIX + "StoreReference takes hashtable q,string k,string m,string k2 returns nothing" + JASS_DELIM +
					"    local integer c=LoadInteger(q,StringHash(k)," + getCommonHashFunc(CACHE_COUNT_KEY) + ")" + JASS_DELIM +
					"    call SaveStr(q,StringHash(k),2*c,m)" + JASS_DELIM +
					"    call SaveStr(q,StringHash(k),2*c+1,k2)" + JASS_DELIM +
					"    call SaveInteger(q,StringHash(k)," + getCommonHashFunc(CACHE_COUNT_KEY) + ",c+1)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "StoreInteger takes hashtable q,string m,string k,integer v returns nothing" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_INTEGER_PREFIX + "\"+k" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"StoreInteger \"+LoadStr(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
					"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_INTEGER_KEY + "\",m2,k2)" + JASS_DELIM +
					"    call SaveInteger(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM + "function " + ARCHON_PREFIX + "StoreReal takes hashtable q,string m,string k,real v returns nothing" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_REAL_PREFIX + "\"+k" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"StoreReal \"+LoadStr(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
					"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_REAL_KEY + "\",m2,k2)" + JASS_DELIM +
					"    call SaveReal(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "StoreBoolean takes hashtable q,string m,string k,boolean v returns nothing" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_BOOL_PREFIX + "\"+k" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"StoreBoolean \"+LoadStr(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
					"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_BOOL_KEY + "\",m2,k2)" + JASS_DELIM +
					"    call SaveBoolean(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "StoreString takes hashtable q,string m,string k,string v returns boolean" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_STRING_PREFIX + "\"+k" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"StoreString \"+LoadStr(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k+\" > \"+v)" + JASS_DELIM) +
					"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_STRING_KEY + "\",m2,k2)" + JASS_DELIM +
					"    return SaveStr(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region StoreUnit native
			append("function " + ARCHON_PREFIX + "StoreUnit takes hashtable q,string m,string k,unit u returns boolean" + JASS_DELIM +
					"    local integer i" + JASS_DELIM +
					"    local integer id" + JASS_DELIM +
					"    local integer c" + JASS_DELIM +
					"    local integer a" + JASS_DELIM +
					"    local integer l" + JASS_DELIM +
					"    local integer is" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
					"    local integer mh=StringHash(m2)" + JASS_DELIM +
					"    local item array ti" + JASS_DELIM +
					"    local unit tu" + JASS_DELIM +
					"    if (u==null) then" + JASS_DELIM +
					"        return false" + JASS_DELIM +
					"    endif" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"StoreUnit \"+LoadStr(q," + getCommonHashFunc(CACHE_CAMPAIGN_MKEY) + "," + getCommonHashFunc(CACHE_CAMPAIGNFILE_KEY) + ")+\"|\"+m+\"|\"+k +\" > \"+GetUnitName(u))" + JASS_DELIM) +
					"set id=GetUnitTypeId(u)" + JASS_DELIM +
					"set tu=CreateUnit(Player(PLAYER_NEUTRAL_PASSIVE),id,0,0,0)" + JASS_DELIM +
					//"call ShowUnit(tu,false)" + JASS_DELIM +
					"    call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ID_SUFFIX + "\"),id)" + JASS_DELIM +
					"set is=UnitInventorySize(u)" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_INVENTORY_SIZE_SUFFIX + "\"),is)" + JASS_DELIM +
					// region save item
					"set i=0" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=is" + JASS_DELIM +
					"set ti[i]=UnitItemInSlot(u,i)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					// endregion save items
					"    if IsHeroUnitId(id) then" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_XP_SUFFIX + "\"),GetHeroXP(u))" + JASS_DELIM +
					// region drop items
					"set i=0" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=is" + JASS_DELIM +
					"call UnitRemoveItem(u,ti[i])" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					// endregion
					"set c=GetHeroStr(u,false)" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_STR_SUFFIX + "\"),c)" + JASS_DELIM +
					"call SetHeroStr(tu,c,true)" + JASS_DELIM +
					"set c=GetHeroAgi(u,false)" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_AGI_SUFFIX + "\"),c)" + JASS_DELIM +
					"call SetHeroAgi(tu,c,true)" + JASS_DELIM +
					"set c=GetHeroInt(u,false)" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_INT_SUFFIX + "\"),c)" + JASS_DELIM +
					"call SetHeroInt(tu,c,true)" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_UNSPEND_POINTS_SUFFIX + "\"),GetHeroSkillPoints(u))" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_HEALTH_MAX_SUFFIX + "\"),BlzGetUnitMaxHP(u)-BlzGetUnitMaxHP(tu))" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_MANA_MAX_SUFFIX + "\"),BlzGetUnitMaxMana(u)-BlzGetUnitMaxMana(tu))", sb);
			for (int i = 0; i < 2; i++)
				append("        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ATTACK_DAMAGE_BASE_SUFFIX + i + "\"),BlzGetUnitBaseDamage(u," + i + ")-BlzGetUnitBaseDamage(tu," + i + "))", sb);
			append("        call SaveReal(q,mh,StringHash(k2+\"" + CACHE_ARMOR_SUFFIX + "\"),BlzGetUnitArmor(u)-BlzGetUnitArmor(tu))" + JASS_DELIM +
					"call RemoveUnit(tu)" + JASS_DELIM +
					// region readd items
					"set i=0" + JASS_DELIM +
					"loop" + JASS_DELIM +
					"exitwhen i>=is" + JASS_DELIM +
					"if ti[i]!=null then" + JASS_DELIM +
					"call UnitAddItem(u,ti[i])" + JASS_DELIM +
					"else" + JASS_DELIM +
					"call UnitAddItemById(u,'phlt')" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set i=i+1" + JASS_DELIM +
					"endloop" + JASS_DELIM +
					"set i=0" + JASS_DELIM +
					"loop" + JASS_DELIM +
					"exitwhen i>=is" + JASS_DELIM +
					"if ti[i]==null then" + JASS_DELIM +
					"call RemoveItem(UnitItemInSlot(u,i))" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set i=i+1" + JASS_DELIM +
					"endloop" + JASS_DELIM +
					// endregion
					"        set c=0" + JASS_DELIM +
					"        set i=0" + JASS_DELIM +
					"        loop" + JASS_DELIM +
					"            exitwhen i>" + ABILITY_ARRAY_SIZE + JASS_DELIM +
					"            set a=" + ABILITY_ARRAY + "[i]" + JASS_DELIM +
					"            set l=GetUnitAbilityLevel(u,a)" + JASS_DELIM +
					"            if l>0 then" + JASS_DELIM +
					"               call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_ID_SUFFIX + "\"+I2S(c)),a)" + JASS_DELIM +
					"               call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_LVL_SUFFIX + "\"+I2S(c)),l)" + JASS_DELIM +
					"               set c=c+1" + JASS_DELIM +
					"            " + END_IF + JASS_DELIM +
					"            set i=i+1" + JASS_DELIM +
					"        endloop" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_COUNT_SUFFIX + "\"),c)" + JASS_DELIM +
					"    endif" + JASS_DELIM +
					"    set i=0" + JASS_DELIM +
					"    loop" + JASS_DELIM +
					"        exitwhen i>=is" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFFIX + "\"+I2S(i)),GetItemTypeId(ti[i]))" + JASS_DELIM +
					"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_CHARGE_SUFFIX + "\"+I2S(i)),GetItemCharges(ti[i]))" + JASS_DELIM +
					"        set c=0", sb);
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
					append("if " + s + " then" + JASS_DELIM +
							"set c=c+" + i + JASS_DELIM +
							END_IF, sb);
					i = i * 2;
				}
			}
			append("        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_BITS_SUFFIX + "\"+I2S(i)),c)" + JASS_DELIM +
					"        set i=i+1" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_UNIT_KEY + "\",m2,k2)" + JASS_DELIM +
					"    return true" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region Store functions
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("Integer", "nothing", "call"),
					new Triple<>("Real", "nothing", "call"),
					new Triple<>("Boolean", "nothing", "call"),
					new Triple<>("String", "boolean", "return"),
					new Triple<>("Unit", "boolean", "return")
			))
				append("function " + ARCHON_PREFIX + "Store" + o.a + "BJ takes " + o.a.toLowerCase(Locale.ROOT) + " v,string k,string m,hashtable q returns " + o.b + JASS_DELIM +
						o.c + " " + ARCHON_PREFIX + "Store" + o.a + "(q,m,k,v)" + JASS_DELIM +
						END_FUNCTION, sb);
			// endregion
			// region SyncStored unused
			append("function " + ARCHON_PREFIX + "SyncStoredUnused takes hashtable q,string m,string k returns nothing" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region HaveStored natives
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("Integer", CACHE_INTEGER_PREFIX, getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(s)"),
					new Triple<>("Real", CACHE_REAL_PREFIX, getConversionFunc(ConversionFunctionType.STRING_TO_REAL) + "(s)"),
					new Triple<>("Boolean", CACHE_BOOL_PREFIX, ARCHON_PREFIX + "S2B(s)"),
					new Triple<>("String", CACHE_STRING_PREFIX, ARCHON_PREFIX + "S62S(s)")
			))
				append("function " + ARCHON_PREFIX + "HaveStored" + o.a + " takes hashtable q,string m,string k returns boolean" + JASS_DELIM +
						"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
						"    local string k2=\"" + o.b + "\"+k" + JASS_DELIM +
						"    local string s" + JASS_DELIM +
						"    local integer mh=StringHash(m2)" + JASS_DELIM +
						"    local integer kh=StringHash(k2)" + JASS_DELIM +
						"    local string cn=" + getCacheNameFunc("q") + JASS_DELIM +
						"if HaveSaved" + o.a + "(q,mh,kh) then" + JASS_DELIM +
						"    return true" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"set s=" + ARCHON_PREFIX + "ReadString(cn,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
						"if s==null then" + JASS_DELIM +
						"return false" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"call Save" + (o.a.equals("String") ? "Str" : o.a) + "(q,mh,kh," + o.c + ")" + JASS_DELIM +
						"return true" + JASS_DELIM +
						END_FUNCTION, sb);
			// endregion
			// region HaveStoredUnit native
			append("function " + ARCHON_PREFIX + "HaveStoredUnit takes hashtable q,string m,string k returns boolean" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
					"    local string cn=" + getCacheNameFunc("q") + JASS_DELIM +
					"if LoadInteger(q,StringHash(m2),StringHash(k2+\"" + CACHE_ID_SUFFIX + "\"))!=0 then" + JASS_DELIM +
					"    return true" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"return " + ARCHON_PREFIX + "ReadString(cn,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")!=null" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region HaveStoredValue function
			append("function " + ARCHON_PREFIX + "HaveStoredValue takes string k,integer t,string m,hashtable q returns boolean", sb);
			for (Pair<String, String> o : Arrays.asList(
					new Pair<>("Boolean", ""),
					new Pair<>("Integer", "else"),
					new Pair<>("Real", "else"),
					new Pair<>("Unit", "else"),
					new Pair<>("String", "else")
			))
				append("    " + o.b + "if t==bj_GAMECACHE_" + o.a.toUpperCase(Locale.ROOT) + " then" + JASS_DELIM +
						"        return " + ARCHON_PREFIX + "HaveStored" + o.a + "(q,m,k)", sb);
			append("    endif" + JASS_DELIM +
					"return false" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region FlushStored natives
			for (Pair<String, String> o : Arrays.asList(
					new Pair<>("Integer", CACHE_INTEGER_PREFIX),
					new Pair<>("Real", CACHE_REAL_PREFIX),
					new Pair<>("Boolean", CACHE_BOOL_PREFIX),
					new Pair<>("String", CACHE_STRING_PREFIX)
			))
				append("function " + ARCHON_PREFIX + "FlushStored" + o.a + " takes hashtable q,string m,string k returns nothing" + JASS_DELIM +
						"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
						"    local string k2=\"" + o.b + "\"+k" + JASS_DELIM +
						"    local integer mh=StringHash(m2)" + JASS_DELIM +
						"    local integer kh=StringHash(k2)" + JASS_DELIM +
						"if HaveSaved" + o.a + "(q,mh,kh) then" + JASS_DELIM +
						// todo add entry to hashtable that tells it to overwrite file upon saving
						//"call SaveInteger(q,mh,kh," + o.c + ")" + JASS_DELIM +
						"call RemoveSaved" + o.a + "(q,mh,kh)" + JASS_DELIM +
						END_IF + JASS_DELIM +
						END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "FlushStoredUnit takes hashtable q,string m,string k returns nothing" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
					"if " + ARCHON_PREFIX + "HaveStoredUnit(q,m,k) then" + JASS_DELIM +
					// todo add entry to hashtable that tells it to overwrite file upon saving
					"call RemoveSavedInteger(q,StringHash(m2),StringHash(k2+\"" + CACHE_ID_SUFFIX + "\"))" + JASS_DELIM +
					END_IF + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "FlushGameCache takes hashtable q returns nothing" + JASS_DELIM +
					// todo add entry to hashtable that tells it to overwrite file upon saving
					"call FlushParentHashtable(q)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "FlushStoredMission takes hashtable q,string m returns nothing" + JASS_DELIM +
					"local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					// todo add entry to hashtable that tells it to overwrite file upon saving
					"call FlushChildHashtable(q,StringHash(m2))" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "FlushGameCacheBJ takes hashtable q returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "FlushGameCache(q)" + JASS_DELIM +
					END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "FlushStoredMissionBJ takes string m,hashtable q returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "FlushStoredMission(q,m)" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region GetStored natives
			for (Triple<String, String, String> o : Arrays.asList(
					new Triple<>("Integer", CACHE_INTEGER_PREFIX, "0"),
					new Triple<>("Real", CACHE_REAL_PREFIX, "0.000"),
					new Triple<>("Boolean", CACHE_BOOL_PREFIX, "false"),
					new Triple<>("String", CACHE_STRING_PREFIX, "null")
			))
				append("function " + ARCHON_PREFIX + "GetStored" + o.a + " takes hashtable q,string m,string k returns " + o.a.toLowerCase(Locale.ROOT) + JASS_DELIM +
						"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
						"    local string k2=\"" + o.b + "\"+k" + JASS_DELIM +
						"if " + ARCHON_PREFIX + "HaveStored" + o.a + "(q,m,k) then" + JASS_DELIM +
						"    return Load" + (o.a.equals("String") ? "Str" : o.a) + "(q,StringHash(m2),StringHash(k2))" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"return " + o.c + JASS_DELIM +
						END_FUNCTION, sb);
			// endregion
			//region RestoreUnit native
			append("function " + ARCHON_PREFIX + "RestoreUnit takes hashtable q,string m,string k,player p,real x,real y,real facing returns unit" + JASS_DELIM +
					"    local integer i" + JASS_DELIM +
					"    local integer j" + JASS_DELIM +
					"    local integer a" + JASS_DELIM +
					"    local integer c" + JASS_DELIM +
					"    local integer fm" + JASS_DELIM +
					"    local integer l" + JASS_DELIM +
					"    local integer id" + JASS_DELIM +
					"    local unit u" + JASS_DELIM +
					"    local boolean h" + JASS_DELIM +
					"    local boolean r" + JASS_DELIM +
					"    local boolean d" + JASS_DELIM +
					"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
					"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
					"    local integer mh=StringHash(m2)" + JASS_DELIM +
					"    local integer array f" + JASS_DELIM +
					"    local integer array v" + JASS_DELIM +
					"    local real array vr" + JASS_DELIM +
					"    local integer array z" + JASS_DELIM +
					"    local item ti" + JASS_DELIM +
					"    local string cn=" + getCacheNameFunc("q") + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"RestoreUnit \"+cn+\"|\"+m+\"|\"+k+\" > \"+LoadStr(q,mh,StringHash(\"" + CACHE_STRING_PREFIX + "\"+k)))" + JASS_DELIM) +
					"    set id=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ID_SUFFIX + "\"))" + JASS_DELIM +
					"if id==0 then" + JASS_DELIM +
					"        set id=" + ARCHON_PREFIX + "ReadUnit(cn,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
					"    if id==0 then" + JASS_DELIM +
					"set XT87ReadLinesAreRead=false" + JASS_DELIM +
					"set XT87ReadLinesLock=false" + JASS_DELIM +
					"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1" + JASS_DELIM +
					"if XT87ReadLinesWaiting==0 and " + ARCHON_PREFIX + "CIP then" + JASS_DELIM +
					"set " + ARCHON_PREFIX + "CIP=false" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(false)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"return null" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set r=true" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"set u=CreateUnit(p,id,x,y,facing)" + JASS_DELIM +
					"set h=IsHeroUnitId(id)" + JASS_DELIM +
					"if h then", sb); // if hero
			for (String suffix : getBasicFields())
				append("set f[" + getCacheFieldIndex(suffix) + "]=StringHash(k2+\"" + suffix + "\")", sb);
			append("set f[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "]=StringHash(k2+\"" + CACHE_ARMOR_SUFFIX + "\")", sb);
			append("set f[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "]=StringHash(k2+\"" + CACHE_ABILITY_COUNT_SUFFIX + "\")", sb);
			append("if r then", sb); // if read
			append("set fm=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[1])", sb);
			int lineIndex = 2;
			for (String suffix : getBasicFields())
				append("set v[" + getCacheFieldIndex(suffix) + "]=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[" + (lineIndex++) + "])" + JASS_DELIM +
						"call SaveInteger(q,mh,f[" + getCacheFieldIndex(suffix) + "],v[" + getCacheFieldIndex(suffix) + "])", sb);
			append("set vr[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "]=" + getConversionFunc(ConversionFunctionType.STRING_TO_REAL) + "(XT87ReadLines[" + (lineIndex++) + "])" + JASS_DELIM +
					"call SaveReal(q,mh,f[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "],vr[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "])", sb);
			append("set v[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "]=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[" + (lineIndex++) + "])" + JASS_DELIM +
					"call SaveInteger(q,mh,f[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "],v[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "])", sb);
			append("else", sb); // else not read
			for (String suffix : getBasicFields())
				append("set v[" + getCacheFieldIndex(suffix) + "]=LoadInteger(q,mh,f[" + getCacheFieldIndex(suffix) + "])", sb);
			append("set vr[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "]=LoadReal(q,mh,f[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "])", sb);
			append("set v[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "]=LoadInteger(q,mh,f[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "])", sb);
			append(END_IF, sb); // endif read
			append("        call SetHeroXP(u,v[" + getCacheFieldIndex(CACHE_XP_SUFFIX) + "],false)" + JASS_DELIM +
					"        call SetHeroStr(u,v[" + getCacheFieldIndex(CACHE_STR_SUFFIX) + "],true)" + JASS_DELIM +
					"        call SetHeroAgi(u,v[" + getCacheFieldIndex(CACHE_AGI_SUFFIX) + "],true)" + JASS_DELIM +
					"        call SetHeroInt(u,v[" + getCacheFieldIndex(CACHE_INT_SUFFIX) + "],true)" + JASS_DELIM +
					"set v[" + getCacheFieldIndex(CACHE_HEALTH_MAX_SUFFIX) + "]=v[" + getCacheFieldIndex(CACHE_HEALTH_MAX_SUFFIX) + "]+BlzGetUnitMaxHP(u)" + JASS_DELIM +
					"set v[" + getCacheFieldIndex(CACHE_MANA_MAX_SUFFIX) + "]=v[" + getCacheFieldIndex(CACHE_MANA_MAX_SUFFIX) + "]+BlzGetUnitMaxMana(u)" + JASS_DELIM +
					"        call BlzSetUnitMaxHP(u,v[" + getCacheFieldIndex(CACHE_HEALTH_MAX_SUFFIX) + "])" + JASS_DELIM +
					"        call SetUnitState(u,UNIT_STATE_LIFE,v[" + getCacheFieldIndex(CACHE_HEALTH_MAX_SUFFIX) + "])" + JASS_DELIM +
					"        call BlzSetUnitMaxMana(u,v[" + getCacheFieldIndex(CACHE_MANA_MAX_SUFFIX) + "])" + JASS_DELIM +
					"        call SetUnitState(u,UNIT_STATE_MANA,v[" + getCacheFieldIndex(CACHE_MANA_MAX_SUFFIX) + "])", sb);
			for (int i = 0; i < 2; i++)
				append("        call BlzSetUnitBaseDamage(u,v[" + getCacheFieldIndex(CACHE_ATTACK_DAMAGE_BASE_SUFFIX + i) + "]+BlzGetUnitBaseDamage(u," + i + ")," + i + ")", sb);
			append("        call BlzSetUnitArmor(u,vr[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "]+BlzGetUnitArmor(u))" + JASS_DELIM +
					"        call ModifyHeroSkillPoints(u,bj_MODIFYMETHOD_SET,v[" + getCacheFieldIndex(CACHE_UNSPEND_POINTS_SUFFIX) + "])" + JASS_DELIM +
					"        set i=0" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" c \"+I2S(c))" + JASS_DELIM) +
					"        loop" + JASS_DELIM + // loop i = 0, i < ability_count
					"            exitwhen i>=v[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "]" + JASS_DELIM +
					"if r then", sb); // if read
			int lineIndexAfterAbilityCount = lineIndex;
			append("set a=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[" + (lineIndex++) + "+2*i])" + JASS_DELIM +
					"set l=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[" + (lineIndex++) + "+2*i])" + JASS_DELIM +
					"call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_ID_SUFFIX + "\"+I2S(i)),a)" + JASS_DELIM +
					"call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_LVL_SUFFIX + "\"+I2S(i)),l)" + JASS_DELIM +
					"else" + JASS_DELIM + // else not read
					"            set a=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_ID_SUFFIX + "\"+I2S(i)))" + JASS_DELIM +
					"            set l=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_LVL_SUFFIX + "\"+I2S(i)))" + JASS_DELIM +
					END_IF + JASS_DELIM + // endif read
					"            call ModifyHeroSkillPoints(u,bj_MODIFYMETHOD_ADD,l)" + JASS_DELIM +
					"            call " + ARCHON_PREFIX + "WatchSkill(a)" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" l \"+I2S(l))" + JASS_DELIM) +
					"            set j=0" + JASS_DELIM +
					"set d=false" + JASS_DELIM +
					"            loop" + JASS_DELIM +
					"                exitwhen j>=l" + JASS_DELIM +
					DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" j \"+I2S(j))" + JASS_DELIM) +
					DebugOnly("call BJDebugMsg(\"AUX i \"+I2S(i)+\" a \"+I2S(a))" + JASS_DELIM) +
					"                call SelectHeroSkill(u,a)" + JASS_DELIM +
					"if j==0 and GetUnitAbilityLevel(u,a)<1 then" + JASS_DELIM +
					"set d=true" + JASS_DELIM +
					"call SetPlayerAbilityAvailable(p,a,true)" + JASS_DELIM +
					"call SelectHeroSkill(u,a)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"                set j=j+1" + JASS_DELIM +
					"            endloop" + JASS_DELIM +
					"if d then" + JASS_DELIM +
					"call SetPlayerAbilityAvailable(p,a,false)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"            set i=i+1" + JASS_DELIM +
					"        endloop" + JASS_DELIM +
					"    set j=" + lineIndexAfterAbilityCount + "+2*v[" + getCacheFieldIndex(CACHE_ABILITY_COUNT_SUFFIX) + "]" + JASS_DELIM +
					"else" + JASS_DELIM + // else is unit
					"    set j=1" + JASS_DELIM +
					END_IF + JASS_DELIM + // endif hero
					"if r then" + JASS_DELIM + // if read
					"set v[" + getCacheFieldIndex(CACHE_INVENTORY_SIZE_SUFFIX) + "]=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[j])" + JASS_DELIM +
					"set j=j+1" + JASS_DELIM +
					"call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_INVENTORY_SIZE_SUFFIX + "\"),v[" + getCacheFieldIndex(CACHE_INVENTORY_SIZE_SUFFIX) + "])" + JASS_DELIM +
					"else" + JASS_DELIM + // else not read
					"set v[" + getCacheFieldIndex(CACHE_INVENTORY_SIZE_SUFFIX) + "]=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_INVENTORY_SIZE_SUFFIX + "\"))" + JASS_DELIM +
					END_IF + JASS_DELIM + // endif read
					"set i=0" + JASS_DELIM +
					"loop" + JASS_DELIM + // loop i = 0, i < inventory_size
					"exitwhen i>=v[" + getCacheFieldIndex(CACHE_INVENTORY_SIZE_SUFFIX) + "]" + JASS_DELIM +
					"if r then" + JASS_DELIM + // if read
					"set c=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[j])" + JASS_DELIM +
					"set j=j+1" + JASS_DELIM +
					"call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFFIX + "\"+I2S(i)),c)" + JASS_DELIM +
					"if c!=0 then", sb); // if item exists
			for (String suffix : Arrays.asList(CACHE_ITEM_CHARGE_SUFFIX, CACHE_ITEM_BITS_SUFFIX))
				append("set v[" + getCacheFieldIndex(suffix) + "]=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(XT87ReadLines[j])" + JASS_DELIM +
						"set j=j+1" + JASS_DELIM +
						"call SaveInteger(q,mh,StringHash(k2+\"" + suffix + "\"+I2S(i)),v[" + getCacheFieldIndex(suffix) + "])", sb);
			append(END_IF + JASS_DELIM + // endif item exists
					"else" + JASS_DELIM + // else not read
					"set c=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFFIX + "\"+I2S(i)))" + JASS_DELIM +
					"if c!=0 then", sb); // if item exists
			for (String suffix : Arrays.asList(CACHE_ITEM_CHARGE_SUFFIX, CACHE_ITEM_BITS_SUFFIX))
				append("set v[" + getCacheFieldIndex(suffix) + "]=LoadInteger(q,mh,StringHash(k2+\"" + suffix + "\"+I2S(i)))", sb);
			append(END_IF + JASS_DELIM + // endif item exists
					END_IF + JASS_DELIM + // endif read
					"if c!=0 then" + JASS_DELIM + // if item exists
					"call UnitAddItemToSlotById(u,c,i)" + JASS_DELIM +
					"set ti=UnitItemInSlot(u,i)" + JASS_DELIM +
					"        call SetItemCharges(ti,v[" + getCacheFieldIndex(CACHE_ITEM_CHARGE_SUFFIX) + "])", sb);
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
							append("call " + String.format(s, "ti", moduloFunc("v[" + getCacheFieldIndex(CACHE_ITEM_BITS_SUFFIX) + "]", "2") + "==1"), sb);
						i--;
						if (i > 0)
							append("set v[" + getCacheFieldIndex(CACHE_ITEM_BITS_SUFFIX) + "]=v[" + getCacheFieldIndex(CACHE_ITEM_BITS_SUFFIX) + "]/2", sb);
					}
				}
			}
			append(END_IF + JASS_DELIM + // endif item exists
					"        set i=i+1" + JASS_DELIM +
					"    endloop" + JASS_DELIM +
					"if r then" + JASS_DELIM + // if read
					"set XT87ReadLinesAreRead=false" + JASS_DELIM +
					"set XT87ReadLinesLock=false" + JASS_DELIM +
					"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1" + JASS_DELIM +
					"if XT87ReadLinesWaiting==0 and " + ARCHON_PREFIX + "CIP then" + JASS_DELIM +
					"set " + ARCHON_PREFIX + "CIP=false" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(false)" + JASS_DELIM +
					END_IF + JASS_DELIM +
					END_IF + JASS_DELIM + // endif read
					"    return u" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region GetStored functions
			for (String s : Arrays.asList("Integer", "Real", "Boolean"))
				append("function " + ARCHON_PREFIX + "GetStored" + s + "BJ takes string k,string m,hashtable q returns " + s.toLowerCase(Locale.ROOT) + JASS_DELIM +
						"    return " + ARCHON_PREFIX + "GetStored" + s + "(q,m,k)" + JASS_DELIM +
						END_FUNCTION, sb);
			append("function " + ARCHON_PREFIX + "GetStoredStringBJ takes string k,string m,hashtable q returns string" + JASS_DELIM +
					"    local string s=" + ARCHON_PREFIX + "GetStoredString(q,m,k)" + JASS_DELIM +
					"    if s==null then" + JASS_DELIM +
					"        return \"\"" + JASS_DELIM +
					END_IF + JASS_DELIM +
					"    return s" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "RestoreUnitLocFacingAngleBJ takes string k,string m,hashtable q,player p,location l,real f returns unit" + JASS_DELIM +
					"    set bj_lastLoadedUnit=" + ARCHON_PREFIX + "RestoreUnit(q,m,k,p,GetLocationX(l),GetLocationY(l),f)" + JASS_DELIM +
					"    return bj_lastLoadedUnit" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function " + ARCHON_PREFIX + "RestoreUnitLocFacingPointBJ takes string k,string m,hashtable q,player p,location l,location f returns unit" + JASS_DELIM +
					"    return " + ARCHON_PREFIX + "RestoreUnitLocFacingAngleBJ(k,m,q,p,l,AngleBetweenPoints(l,f))" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// region GameCache basic functions
			append("function " + ARCHON_PREFIX + "InitGameCacheBJ takes string f returns hashtable" + JASS_DELIM +
					"    set " + LAST_CREATED_COOP_CACHE + "=" + ARCHON_PREFIX + "InitGameCache(f)" + JASS_DELIM +
					"    return " + LAST_CREATED_COOP_CACHE + JASS_DELIM +
					//"    return null" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function  " + ARCHON_PREFIX + "SaveGameCacheBJ takes hashtable q returns boolean" + JASS_DELIM +
					"    return " + ARCHON_PREFIX + "SaveGameCache(q)" + JASS_DELIM +
					END_FUNCTION, sb);
			// endregion
			// endregion
			// region Trigger functions
			{
				for (Triple<String, String, String> o : Collections.singletonList(
						new Triple<>("TriggerExecuteBJ", "trigger t,boolean b returns boolean", "(t,b)")
				))
					append("function " + ARCHON_PREFIX + o.a + " takes " + o.b + JASS_DELIM +
							PAUSE_LOCK_CODE + JASS_DELIM +
							"return " + o.a + o.c + JASS_DELIM +
							END_FUNCTION, sb);
				for (Triple<String, String, String> o : Collections.singletonList(
						new Triple<>("TriggerEvaluate", "trigger t returns boolean", "(t)")
				))
					append("function " + ARCHON_PREFIX + o.a + " takes " + o.b + JASS_DELIM +
							PAUSE_LOCK_CODE + JASS_DELIM +
							"return " + o.a + o.c + JASS_DELIM +
							END_FUNCTION, sb);
			}
			// endregion
		}
		// region gold and lumber synchronization
		List<String> resourceTypes = Arrays.asList("PLAYER_STATE_RESOURCE_GOLD", "PLAYER_STATE_RESOURCE_LUMBER");
		List<String> resourceNames = getBaseResourceNames();
		for (int i = 0; i < resourceTypes.size(); i++) {
			String resourceType = resourceTypes.get(i);
			String resourceName = resourceNames.get(i);
			append("function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac takes nothing returns nothing" + JASS_DELIM +
					"    local integer v=GetPlayerState(GetTriggerPlayer()," + resourceType + ")" + JASS_DELIM +
					XT87Utils.DISABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")", sb);
			for (int playerNumber : allPlayerNumbers) {
				String playerFunc = toPlayerFunc(playerNumber);
				append("    if (GetTriggerPlayer()!=" + playerFunc + ") then", sb);
				append("        call SetPlayerState(" + playerFunc + "," + resourceType + ",v)", sb);
				append("    " + END_IF, sb);
			}
			append(XT87Utils.ENABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function it_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing" + JASS_DELIM +
					"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()", sb);
			for (int playerNumber : allPlayerNumbers)
				append("    call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toPlayerFunc(playerNumber) + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0)", sb);
			append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM, sb);
		}
		// endregion
		// region food and population cap synchronization
		resourceTypes = Arrays.asList("PLAYER_STATE_RESOURCE_FOOD_USED", "PLAYER_STATE_RESOURCE_FOOD_CAP", "PLAYER_STATE_FOOD_CAP_CEILING");
		resourceNames = getFoodResourceNames();
		for (int i = 0; i < resourceTypes.size(); i++) {
			String resourceType = resourceTypes.get(i);
			String resourceName = resourceNames.get(i);
			append("function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac takes nothing returns nothing" + JASS_DELIM +
					"    local integer v=GetPlayerState(" + toMainPlayerFunc() + "," + resourceType + ")" + JASS_DELIM +
					XT87Utils.DISABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")", sb);
			for (int playerNumber : allPlayerNumbers) {
				if (playerNumber == mainPlayer.getNum())
					continue;
				String playerFunc = toPlayerFunc(playerNumber);
				append("    call SetPlayerState(" + playerFunc + "," + resourceType + ",v)", sb);
			}
			append(XT87Utils.ENABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function it_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing" + JASS_DELIM +
					"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()", sb);
			append("call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toMainPlayerFunc() + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0)", sb);
			append("call TriggerRegisterTimerEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,0,false)", sb);
			append("call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function t_" + ARCHON_PREFIX + resourceName + "Sync_Ac)" + JASS_DELIM +
					END_FUNCTION, sb);
		}
		// endregion
		// region pause test
		if (WITH_PAUSE_COMMAND)
			append("function t_" + ARCHON_PREFIX + "PauseTest_Ac takes nothing returns nothing" + JASS_DELIM +
					"call " + ARCHON_PREFIX + "PauseGame(not " + ARCHON_PREFIX + "GIP)" + JASS_DELIM +
					END_FUNCTION + JASS_DELIM +
					"function it_" + ARCHON_PREFIX + "PauseTest takes nothing returns nothing" + JASS_DELIM +
					"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest=CreateTrigger()" + JASS_DELIM +
					"    call TriggerRegisterPlayerChatEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest," + toMainPlayerFunc() + ",\"-p\",true)" + JASS_DELIM +
					"    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest,function t_" + ARCHON_PREFIX + "PauseTest_Ac)" + JASS_DELIM +
					END_FUNCTION, sb);
		// endregion
		// region player leaves game
		append("function t_" + ARCHON_PREFIX + "PLG_Ac takes nothing returns nothing" + JASS_DELIM +
				"call RemovePlayerPreserveUnitsBJ(GetTriggerPlayer(),PLAYER_GAME_RESULT_DEFEAT,true)" + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function it_" + ARCHON_PREFIX + "PLG takes nothing returns nothing" + JASS_DELIM +
				"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG=CreateTrigger()", sb);
		for (int playerNumber : allPlayerNumbers)
			append("call TriggerRegisterPlayerEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG," + toPlayerFunc(playerNumber) + ",EVENT_PLAYER_LEAVE)", sb);
		append("call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG,function t_" + ARCHON_PREFIX + "PLG_Ac)" + JASS_DELIM +
				END_FUNCTION, sb);
		// endregion
		// region game is loaded
		append("function t_" + ARCHON_PREFIX + "GIL_Ac takes nothing returns nothing" + JASS_DELIM +
				bIf(ARCHON_PREFIX + "LUP",
						bSet(ARCHON_PREFIX + "LUP", "false") + JASS_DELIM +
								bCall(ARCHON_PREFIX + "PauseGame", "false")) + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function it_" + ARCHON_PREFIX + "GIL takes nothing returns nothing" + JASS_DELIM +
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL=CreateTrigger()" + JASS_DELIM +
				"call TriggerRegisterGameLoadedEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL)" + JASS_DELIM +
				"call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL,function t_" + ARCHON_PREFIX + "GIL_Ac)" + JASS_DELIM +
				END_FUNCTION, sb);
		// endregion
		// region give units to main player
		append("function t_" + ARCHON_PREFIX + "UnitShare_Conditions takes nothing returns boolean", sb);
		for (int playerNumber : allPlayerNumbers) {
			if (playerNumber == mainPlayer.getNum())
				continue;
			append("    if GetOwningPlayer(GetEnteringUnit())==" + toPlayerFunc(playerNumber) + " then" + JASS_DELIM +
					"        return true" + JASS_DELIM +
					"    endif", sb);
		}
		append("    return false" + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function t_" + ARCHON_PREFIX + "UnitShare_Ac takes nothing returns nothing" + JASS_DELIM +
				"    call SetUnitOwner(GetEnteringUnit()," + toMainPlayerFunc() + ",true)" + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function it_" + ARCHON_PREFIX + "UnitShare takes nothing returns nothing" + JASS_DELIM +
				"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare=CreateTrigger()" + JASS_DELIM +
				"    call TriggerRegisterEnterRectSimple(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,GetPlayableMapRect())" + JASS_DELIM +
				"    call TriggerAddCondition(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,Condition(function t_" + ARCHON_PREFIX + "UnitShare_Conditions))" + JASS_DELIM +
				"    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,function t_" + ARCHON_PREFIX + "UnitShare_Ac)" + JASS_DELIM +
				END_FUNCTION, sb);
		// endregion
		// region selection effects
		append("function " + ARCHON_PREFIX + "OnSelect takes nothing returns nothing" + JASS_DELIM +
				"local integer p=GetPlayerId(" + AUX_PLAYER + ")" + JASS_DELIM +
				"local integer c=LoadInteger(" + SELECTION_HASHTABLE + ",p,0)+1" + JASS_DELIM +
				"local effect f=AddSpecialEffectTarget(\"" + XT87Utils.PATH_PREFIX + "\\\\" + SELECTION_INDICATOR_PATH + ".mdx\",GetEnumUnit(),\"overhead\")" + JASS_DELIM +
				"local real a=1" + JASS_DELIM +
				"if bj_cineModeAlreadyIn then" + JASS_DELIM +
				"set a=0" + JASS_DELIM +
				END_IF + JASS_DELIM +
				"call BlzSetSpecialEffectColorByPlayer(f,Player(p))" + JASS_DELIM +
				"call BlzSetSpecialEffectScale(f,a)" + JASS_DELIM +
				"call SaveEffectHandle(" + SELECTION_HASHTABLE + ",p,c,f)" + JASS_DELIM +
				"call SaveInteger(" + SELECTION_HASHTABLE + ",p,0,c)" + JASS_DELIM +
				END_FUNCTION, sb);
		append("function t_" + ARCHON_PREFIX + "Select_Ac takes nothing returns nothing" + JASS_DELIM +
				"local integer i=0" + JASS_DELIM +
				"local integer p=GetPlayerId(GetTriggerPlayer())" + JASS_DELIM +
				// selection count
				"local integer c" + JASS_DELIM +
				// stop bool
				"local boolean b=LoadBoolean(" + SELECTION_HASHTABLE + ",p,0)" + JASS_DELIM +
				"local group g" + JASS_DELIM +
				"if b then" + JASS_DELIM +
				"return" + JASS_DELIM +
				END_IF + JASS_DELIM +
				"call SaveBoolean(" + SELECTION_HASHTABLE + ",p,0,true)" + JASS_DELIM +
				"call TriggerSleepAction(0.01)" + JASS_DELIM +
				"set c=LoadInteger(" + SELECTION_HASHTABLE + ",p,0)" + JASS_DELIM +
				"loop" + JASS_DELIM +
				"exitwhen i>c" + JASS_DELIM +
				"set i=i+1" + JASS_DELIM +
				"call DestroyEffect(LoadEffectHandle(" + SELECTION_HASHTABLE + ",p,i))" + JASS_DELIM +
				"endloop" + JASS_DELIM +
				"call SaveInteger(" + SELECTION_HASHTABLE + ",p,0,0)" + JASS_DELIM +
				"set g=CreateGroup()" + JASS_DELIM +
				"set " + AUX_PLAYER + "=GetTriggerPlayer()" + JASS_DELIM +
				"call GroupEnumUnitsSelected(g," + AUX_PLAYER + ",null)" + JASS_DELIM +
				"call ForGroup(g,function " + ARCHON_PREFIX + "OnSelect)" + JASS_DELIM +
				"call DestroyGroup(g)" + JASS_DELIM +
				"call SaveBoolean(" + SELECTION_HASHTABLE + ",p,0,false)" + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function it_" + ARCHON_PREFIX + "Select takes nothing returns nothing" + JASS_DELIM +
				"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "Select=CreateTrigger()", sb);
		for (int playerNumber : allPlayerNumbers)
			for (String selectBool : Arrays.asList("false", "true"))
				append("    call TriggerRegisterPlayerSelectionEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "Select," + toPlayerFunc(playerNumber) + "," + selectBool + ")", sb);
		append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "Select, function t_" + ARCHON_PREFIX + "Select_Ac)" + JASS_DELIM +
				END_FUNCTION, sb);
		// endregion
		// region show food on allied buildings
		append("function t_" + ARCHON_PREFIX + "BFP_Ac takes nothing returns nothing" + JASS_DELIM +
				//"local integer f=" + ARCHON_PREFIX + "LBF[GetPlayerId(GetLocalPlayer())]" + JASS_DELIM +
				"local framehandle array a" + JASS_DELIM +
				"local integer f=" + ARCHON_PREFIX + "LBF" + JASS_DELIM +
				"local boolean b=BlzFrameIsVisible(BlzGetFrameByName(\"AllianceDialog\",0))" + JASS_DELIM +
				"local integer i=0" + JASS_DELIM +
				"set a[0]=BlzGetFrameByName(\"AllianceAcceptButton\",0)" + JASS_DELIM +
				"set a[1]=BlzGetFrameByName(\"AllianceBackdrop\",0)" + JASS_DELIM +
				//"BlzGetFrameByName(\"AllianceCancelButton\",0)" + JASS_DELIM +
				"set a[2]=BlzGetFrameByName(\"AllianceDialogScrollBar\",0)" + JASS_DELIM +
				"set a[3]=BlzGetFrameByName(\"AllianceTitle\",0)" + JASS_DELIM +
				"set a[4]=BlzGetFrameByName(\"AllianceSlot\",0)" + JASS_DELIM +
				"set a[5]=BlzGetFrameByName(\"AlliedVictoryCheckBox\",0)" + JASS_DELIM +
				"set a[6]=BlzGetFrameByName(\"ResourceTradingTitle\",0)" + JASS_DELIM +
				"set a[7]=BlzGetFrameByName(\"PlayersHeader\",0)" + JASS_DELIM +
				"set a[8]=BlzGetFrameByName(\"AllyHeader\",0)" + JASS_DELIM +
				"set a[9]=BlzGetFrameByName(\"VisionHeader\",0)" + JASS_DELIM +
				"set a[10]=BlzGetFrameByName(\"UnitsHeader\",0)" + JASS_DELIM +
				"set a[11]=BlzGetFrameByName(\"GoldHeader\",0)" + JASS_DELIM +
				"set a[12]=BlzGetFrameByName(\"LumberHeader\",0)" + JASS_DELIM +
				"set a[13]=BlzGetFrameByName(\"AlliedVictoryLabel\",0)" + JASS_DELIM +
				"set a[14]=BlzGetFrameByName(\"LogArea\",0)" + JASS_DELIM +
				"set a[15]=BlzGetFrameByName(\"LogAreaScrollBar\",0)" + JASS_DELIM +
				"set a[16]=BlzGetFrameByName(\"LogAreaBackdrop\",0)" + JASS_DELIM +
				"set a[17]=BlzGetFrameByName(\"LogOkButton\",0)" + JASS_DELIM +
				"set a[18]=BlzGetFrameByName(\"LogTitle\",0)" + JASS_DELIM +
				"set a[19]=BlzGetFrameByName(\"LogDialog\",0)" + JASS_DELIM +
				"set a[20]=BlzGetFrameByName(\"SimpleInfoPanelIconAlly\",7)" + JASS_DELIM +
				"set a[21]=BlzGetFrameByName(\"SimpleInfoPanelIconFood\",4)" + JASS_DELIM +
				"set a[22]=BlzGetFrameByName(\"InfoPanelIconValue\",4)" + JASS_DELIM +
				"loop" + JASS_DELIM +
				"exitwhen i>=bj_MAX_PLAYERS" + JASS_DELIM +
				"call BlzFrameSetVisible(BlzGetFrameByName(\"AllianceSlot\",i),false)" + JASS_DELIM +
				"call BlzFrameSetVisible(BlzGetFrameByName(\"AlliedVictoryCheckBox\",i),false)" + JASS_DELIM +
				"set i=i+1" + JASS_DELIM +
				"endloop" + JASS_DELIM +
				DebugOnly("call BJDebugMsg(\"a\")" + JASS_DELIM) +
				"if not " + ARCHON_PREFIX + "AlliesMenuAvail and b then" + JASS_DELIM +
				"set " + ARCHON_PREFIX + "AlliesMenuAvail=true" + JASS_DELIM +
				"set i=0" + JASS_DELIM +
				"loop" + JASS_DELIM +
				"exitwhen i>3" + JASS_DELIM +
				"call BlzFrameSetVisible(a[i],false)" + JASS_DELIM +
				"set i=i+1" + JASS_DELIM +
				"endloop" + JASS_DELIM +
				END_IF + JASS_DELIM +
				"if " + ARCHON_PREFIX + "AlliesMenuAvail then" + JASS_DELIM +
				"if b then" + JASS_DELIM +
				"set i=6" + JASS_DELIM +
				"loop" + JASS_DELIM +
				"exitwhen i>13" + JASS_DELIM +
				"call BlzFrameSetVisible(a[i],false)" + JASS_DELIM +
				"set i=i+1" + JASS_DELIM +
				"endloop" + JASS_DELIM +
				"loop" + JASS_DELIM +
				"exitwhen i>18" + JASS_DELIM +
				"call BlzFrameSetVisible(a[i],true)" + JASS_DELIM +
				"set i=i+1" + JASS_DELIM +
				"endloop" + JASS_DELIM +
				END_IF + JASS_DELIM +
				"call BlzFrameSetVisible(a[19],b)" + JASS_DELIM +
				END_IF + JASS_DELIM +
				"if f>0 then" + JASS_DELIM +
				"    call BlzFrameSetVisible(a[20],false)" + JASS_DELIM +
				"    call BlzFrameSetVisible(a[21],true)" + JASS_DELIM +
				"    call BlzFrameSetText(a[22],I2S(f))" + JASS_DELIM +
				END_IF + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function it_" + ARCHON_PREFIX + "BFP takes nothing returns nothing" + JASS_DELIM +
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP=CreateTrigger()" + JASS_DELIM +
				"call TriggerRegisterTimerEventPeriodic(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP,0.0)" + JASS_DELIM +
				"call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP,function t_" + ARCHON_PREFIX + "BFP_Ac)" + JASS_DELIM +
				END_FUNCTION, sb);
		append("function t_" + ARCHON_PREFIX + "BF_Ac takes nothing returns nothing" + JASS_DELIM +
				"local unit u=GetTriggerUnit()" + JASS_DELIM +
				"local integer f=GetUnitFoodMade(u)" + JASS_DELIM +
				"local integer i" + JASS_DELIM +
				"local player p=GetTriggerPlayer()" + JASS_DELIM +
				"if p==GetLocalPlayer() then" + JASS_DELIM +
				"set i=GetPlayerId(GetLocalPlayer())" + JASS_DELIM +
				"if f>0 and IsUnitType(u,UNIT_TYPE_STRUCTURE) and not IsUnitOwnedByPlayer(u,p) and IsUnitAlly(u,p) then" + JASS_DELIM +
				"set " + ARCHON_PREFIX + "LBF=f" + JASS_DELIM +
				"call t_" + ARCHON_PREFIX + "BFP_Ac()" + JASS_DELIM +
				"else" + JASS_DELIM +
				"set " + ARCHON_PREFIX + "LBF=0" + JASS_DELIM +
				END_IF + JASS_DELIM +
				END_IF + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function it_" + ARCHON_PREFIX + "BF takes nothing returns nothing" + JASS_DELIM +
				"local integer i=0" + JASS_DELIM +
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "BF=CreateTrigger()" + JASS_DELIM +
				// todo change loop to GetLocalPlayer()
				loopOverPlayers("i",
						"call TriggerRegisterPlayerSelectionEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BF,Player(" + PLAYER_ID_ARRAY + "[i]),true)") + JASS_DELIM +
				"call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "BF, function t_" + ARCHON_PREFIX + "BF_Ac)" + JASS_DELIM +
				END_FUNCTION, sb);
		// endregion
		// region get main player
		for (String s : Arrays.asList("GetLocalPlayer", "GetTriggerPlayer"))
			append("function " + ARCHON_PREFIX + s + " takes nothing returns player" + JASS_DELIM +
					"    local player p=" + s + "()" + JASS_DELIM +
					"    if IsPlayerInForce(p," + FORCE_NAME + ") then" + JASS_DELIM +
					"        return " + toMainPlayerFunc() + JASS_DELIM +
					END_IF + JASS_DELIM +
					"    return p" + JASS_DELIM +
					END_FUNCTION, sb);
		append("function " + ARCHON_PREFIX + "GetForceOfPlayer takes player p returns force" + JASS_DELIM +
				"    local force f=CreateForce()" + JASS_DELIM +
				"    local integer i=0" + JASS_DELIM +
				"    if IsPlayerInForce(p," + FORCE_NAME + ") then" + JASS_DELIM +
				loopOverPlayers("i",
						"call ForceAddPlayer(f,Player(" + PLAYER_ID_ARRAY + "[i]))") + JASS_DELIM +
				"    else" + JASS_DELIM +
				"    call ForceAddPlayer(f,p)" + JASS_DELIM +
				END_IF + JASS_DELIM +
				"    return f" + JASS_DELIM +
				END_FUNCTION, sb);
		// endregion
		// region enforce sharing
		append("function " + ENFORCE_ARCHON + " takes nothing returns nothing", sb);
		for (W3I.Player player : secondaryPlayers) {
			String playerFunc = toPlayerFunc(player.getNum());
			for (W3I.Player otherPlayer : secondaryPlayers) {
				int otherNum = otherPlayer.getNum();
				if (otherNum == player.getNum()) {
					append("    call SetPlayerAllianceStateBJ(" + playerFunc + "," + toMainPlayerFunc() + ",bj_ALLIANCE_ALLIED_VISION)", sb);
					append("    call SetPlayerAllianceStateBJ(" + toMainPlayerFunc() + "," + playerFunc + ",bj_ALLIANCE_ALLIED_ADVUNITS)", sb);
				} else
					append("    call SetPlayerAllianceStateBJ(" + playerFunc + "," + toPlayerFunc(otherNum) + ",bj_ALLIANCE_ALLIED_VISION)", sb);
			}
		}
		append(END_FUNCTION, sb);
		// endregion
		// region dialog triggers
		append("function t_" + ARCHON_PREFIX + "DS_Ac takes nothing returns nothing" + JASS_DELIM +
						"local player p=GetTriggerPlayer()" + JASS_DELIM +
						"local dialog d=GetClickedDialog()" + JASS_DELIM +
						"local string s=LoadStr(" + GENERAL_HASHTABLE + "," + GH_STRING_DIALOG_BUTTON_TEXT + ",GetHandleId(GetClickedButton()))"
				, sb);
		for (int playerNumber : allPlayerNumbers)
			append("call DialogDisplay(" + toPlayerFunc(playerNumber) + ",d,false)", sb);
		append("call TriggerSleepAction(0.01)" + JASS_DELIM +
				"call DisplayTimedTextFromPlayer(p,0,0,10,\"%s => \"+GetLocalizedString(s))" + JASS_DELIM +
				"if " + ARCHON_PREFIX + "DIP then" + JASS_DELIM +
				"call " + ARCHON_PREFIX + "PauseGame(false)" + JASS_DELIM +
				"set " + ARCHON_PREFIX + "DIP=false" + JASS_DELIM +
				END_IF + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function it_" + ARCHON_PREFIX + "DS takes nothing returns nothing" + JASS_DELIM +
				"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "DS=CreateTrigger()", sb);
		for (String dialogName : dialogNames)
			append("    call TriggerRegisterDialogEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "DS," + dialogName + ")", sb);
		append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "DS,function t_" + ARCHON_PREFIX + "DS_Ac)" + JASS_DELIM +
				END_FUNCTION, sb);
		// endregion
	}

	@Override
	public void onMainLocals(StringBuffer sb) {
		int index = 0;
		for (int playerNum : allPlayerNumbers) {
			append("set " + PLAYER_ID_ARRAY + "[" + index + "]=" + playerNum + JASS_DELIM +
					"set " + PLAYER_INDEX_ARRAY + "[" + playerNum + "]=" + index, sb);
			index++;
		}
		for (String key : getCommonHashKeys())
			append("set " + getCommonHashFunc(key) + "=StringHash(\"" + key + "\")", sb);
		append("    " + SETPLAYERCOLORBJ + toMainPlayerFunc() + "," + CONVERTPLAYERCOLOR + mainPlayer.getNum() + "),true)", sb);
		//if (line.contains(END_FUNCTION)) {
		for (String resourceName : getAllResourceNames())
			append("call it_" + ARCHON_PREFIX + resourceName + "Sync()", sb);
		for (String triggerName : Arrays.asList("UnitShare", "Select", "PLG")) //, "Deselect", "DeselectOnDeath"))
			append("call it_" + ARCHON_PREFIX + triggerName + "()", sb);
		for (String triggerName : Arrays.asList("BF", "BFP"))
			append("call it_" + ARCHON_PREFIX + triggerName + "()", sb);
		append(bCall("it_" + ARCHON_PREFIX + "GIL"), sb);
		if (WITH_PAUSE_COMMAND)
			append(bCall("it_" + ARCHON_PREFIX + "PauseTest"), sb);
		if (WITH_CUSTOM_GAMECACHE) {
			append("call " + FILE_LIBRARY_PREFIX + "Init()", sb);
		}
		append(bSet(FORCE_NAME, bFunc("CreateForce")), sb);
		for (int playerNumber : allPlayerNumbers)
			append("    call ForceAddPlayer(" + FORCE_NAME + "," + toPlayerFunc(playerNumber) + ")", sb);
		for (W3I.Player player : secondaryPlayers) {
			String playerFunc = toPlayerFunc(player.getNum());
			append(SETPLAYERCOLOR + playerFunc + "," + CONVERTPLAYERCOLOR + player.getNum() + "))" + JASS_DELIM +
					"call SetPlayerOnScoreScreen(" + playerFunc + ",false)", sb);
		}
		append("call " + ENFORCE_ARCHON + "()" + JASS_DELIM +
				"if GetLocalPlayer()!=" + toMainPlayerFunc() + " then" + JASS_DELIM +
				"call SetReservedLocalHeroButtons(0)" + JASS_DELIM +
				END_IF, sb);
	}

	@Override
	public void onMainEnd(StringBuffer sb) {
		append("call it_" + ARCHON_PREFIX + "DS()", sb);
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
				"SaveAndLoadGameBJ"
		))
			for (String prefix : Arrays.asList("(", " ", ",", "="))
				for (String suffix : Arrays.asList("(", " ")) {
					String call = function + suffix;
					line = line.replace(prefix + call, prefix + ARCHON_PREFIX + call);
				}
		// endregion

		if (WITH_CUSTOM_GAMECACHE) {
			line = line.replace("bj_lastCreatedGameCache", LAST_CREATED_COOP_CACHE);
			line = line.replace("GetLastCreatedGameCacheBJ()", LAST_CREATED_COOP_CACHE);
			for (String prefix : Arrays.asList(",", " "))
				line = line.replace(prefix + "gamecache ", prefix + "hashtable ");
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
					append("    call " + ARCHON_PREFIX + "WatchSkill(" + params[1] + ")", sb);
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
			if (trimmedLine.startsWith("gamecache "))
				line = line.replaceFirst("gamecache ", "hashtable ");
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
					append(line.replace(params[1], CONVERTPLAYERCOLOR + "1)"), sb);
					String secondaryColorLine = line.replace(params[1], CONVERTPLAYERCOLOR + "4)");
					for (W3I.Player player : secondaryPlayers) {
						append(secondaryColorLine.replace(toMainPlayerFunc(), toPlayerFunc(player.getNum())), sb);
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
						append(PAUSE_LOCK_CODE, sb);
				}
				if (params != null) {
					CoopCallAdjustments.AdjustmentDetails ad = CoopCallAdjustments.callParameters.get(call);
					if (ad != null) {
						appendLine = false;

						if (ad.pointIndex != null) {
							append("set " + TEMP_POINT_NAME + "=" + params[ad.pointIndex], sb);
							params[ad.pointIndex] = TEMP_POINT_NAME;
							line = toCallString(call, params);
						}

						if (!ad.isLocal)
							append(line, sb);

						appendCallWithReplacement(call, params, ad, sb);
					}
				}
			}

			if (appendLine)
				append(line, sb);

			pauseLockAdded = false;

			if (call != null)
				if (!Arrays.asList("main", "RunInitializationTriggers").contains(insideFunction))
					for (String function : Arrays.asList("TriggerExecute", "TriggerExecuteWait", "ConditionalTriggerExecute"))
						if (call.equals(function)) {
							append(PAUSE_LOCK_CODE, sb);
							pauseLockAdded = true;
							break;
						}

			if ("CustomDefeatBJ".equals(call))
				append("call " + ARCHON_PREFIX + "PauseGameOn()", sb);
		}

		if (line.trim().equals("globals")) {
			for (String resourceName : getAllResourceNames())
				append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync", sb);
			append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare" + JASS_DELIM +
							"force " + FORCE_NAME + JASS_DELIM +
							"location " + TEMP_POINT_NAME + JASS_DELIM +
							//"integer array " + ARCHON_PREFIX + "LBF" + JASS_DELIM + // Last Selected Building Food
							"integer " + ARCHON_PREFIX + "LBF=0" + JASS_DELIM + // Last Selected Building Food
							"integer array " + PLAYER_ID_ARRAY + JASS_DELIM +
							"integer array " + PLAYER_INDEX_ARRAY + JASS_DELIM +
							"integer array " + HASH_ARRAY + JASS_DELIM +
							"player " + AUX_PLAYER + JASS_DELIM +
							"hashtable " + GENERAL_HASHTABLE + "=InitHashtable()" + JASS_DELIM +
							"hashtable " + SELECTION_HASHTABLE + "=InitHashtable()" + JASS_DELIM +
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "Select" + JASS_DELIM +
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "BF" + JASS_DELIM + // Building Food selection
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "BFP" + JASS_DELIM + // Building Food periodic
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "DS" + JASS_DELIM + // Dialog Sync
							"boolean " + ARCHON_PREFIX + "AlliesMenuAvail=false" + JASS_DELIM +
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "PLG" + JASS_DELIM + // player left game
							"boolean " + ARCHON_PREFIX + "GIP=false" + JASS_DELIM + // game is paused
							"boolean " + ARCHON_PREFIX + "TDS=false" + JASS_DELIM + // time of day suspended
							"boolean " + ARCHON_PREFIX + "DIP=false" + JASS_DELIM + // dialog is pausing
							"boolean " + ARCHON_PREFIX + "LUP=false" + JASS_DELIM + // load unpauses
							"boolean " + ARCHON_PREFIX + "CIP=false" // cache is pausing
					//"gamespeed " + ARCHON_PREFIX + "RGS=MAP_SPEED_NORMAL" // real game speed
					, sb);
			append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "GIL", sb);
			if (WITH_PAUSE_COMMAND)
				append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "PauseTest", sb);
			if (WITH_CUSTOM_GAMECACHE)
				append("hashtable " + LAST_CREATED_COOP_CACHE + JASS_DELIM +
						"integer array " + ABILITY_ARRAY + JASS_DELIM +
						"integer " + ABILITY_ARRAY_SIZE + "=0" + JASS_DELIM +
						"integer " + FILE_LIBRARY_PREFIX + "CT=0" + JASS_DELIM + // Counter
						"integer array " + FILE_LIBRARY_PREFIX + "LS" + JASS_DELIM + // List
						"integer array " + FILE_LIBRARY_PREFIX + "AL" + JASS_DELIM + // AbilityList
						"boolean " + FILE_LIBRARY_PREFIX + "ReadEnabled=false" + JASS_DELIM +
						"string array " + FILE_LIBRARY_PREFIX + "FN" + JASS_DELIM + // Filename
						"string array " + FILE_LIBRARY_PREFIX + "BF" + JASS_DELIM + // Buffer
						"string array XT87ReadLines" + JASS_DELIM +
						"boolean XT87ReadLinesAreRead=false" + JASS_DELIM +
						"boolean XT87ReadLinesLock=false" + JASS_DELIM +
						"integer XT87ReadLinesWaiting=0", sb);
		} else if (line.contains("SetForceAllianceStateBJ("))
			append("call " + ENFORCE_ARCHON + "()", sb);

		if (line.contains(DEFINE_START_LOC + mainPlayer.getNum() + ","))
			for (W3I.Player player : secondaryPlayers)
				append(line.replace(DEFINE_START_LOC + mainPlayer.getNum(), DEFINE_START_LOC + player.getNum()), sb);
	}
}
