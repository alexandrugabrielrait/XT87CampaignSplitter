package net.xetanth87.campaignsplitter;

import net.moonlightflower.wc3libs.bin.app.W3I;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CoopRewriter extends ScriptRewriter {
	private static final String SETPLAYERCOLOR = "call SetPlayerColor(";
	private static final String DEFINE_START_LOC = "call DefineStartLocation(";
	private static final String SETPLAYERCOLORBJ = "call SetPlayerColorBJ(";
	private static final String CONVERTPLAYERCOLOR = "ConvertPlayerColor(";
	private static final String PLAYER_FUNC = "Player(";
	private static final String TRIGGER_PREFIX = "t_";
	private static final String ARCHON_PREFIX_BASE = "XT87A";
	private static final String ARCHON_PREFIX = ARCHON_PREFIX_BASE + "S";
	private static final String  FILE_IO_PREFIX = ARCHON_PREFIX_BASE + "I";
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
	private static final String LOCAL_DECLARATION = "local ";
	private static final String DIALOG_DECLARATION = "dialog ";

	private static final int FILE_LIBRARY_ABILITY_COUNT = 10;
	private static final int FILE_LIBRARY_PRELOAD_LIMIT = 200;

	public static final String SELECTION_INDICATOR_PATH = "selectionIndicator";

	// coop cache
	public static final boolean WITH_CUSTOM_GAMECACHE = true;
	public static final boolean WITH_FULL_ENCODING = false;
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
	public static final String CACHE_MKEY_PREFIX = CACHE_MISSION_PREFIX;

	private static final String CACHE_CAMPAIGNFILE_KEY = CACHE_OTHER_PREFIX + CACHE_CACHE_PREFIX;
	private static final String CACHE_INTEGER_KEY = CACHE_OTHER_PREFIX + CACHE_INTEGER_PREFIX;
	private static final String CACHE_REAL_KEY = CACHE_OTHER_PREFIX + CACHE_REAL_PREFIX;
	private static final String CACHE_BOOL_KEY = CACHE_OTHER_PREFIX + CACHE_BOOL_PREFIX;
	private static final String CACHE_STRING_KEY = CACHE_OTHER_PREFIX + CACHE_STRING_PREFIX;
	private static final String CACHE_UNIT_KEY = CACHE_OTHER_PREFIX + CACHE_UNIT_PREFIX;
	private static final String CACHE_FLUSH_KEY = CACHE_OTHER_PREFIX + CACHE_FLUSH_PREFIX;
	// todo make sure count doesn't count the same variable twice
	private static final String CACHE_COUNT_KEY = "count";

	private static final String GH_HAS_ABILITY_KEY = "hasAbility";
	private static final String GH_DIALOG_BUTTON_TEXT_KEY = "dbt";

	private static final String GH_INT_PREFIX = ARCHON_PREFIX + "GH";
	private static final String GH_HAS_ABILITY_INT = GH_INT_PREFIX + GH_HAS_ABILITY_KEY;
	private static final String GH_DIALOG_BUTTON_TEXT_INT = GH_INT_PREFIX + GH_DIALOG_BUTTON_TEXT_KEY;

	private static List<String> getCommonHashKeys() {
		return Arrays.asList(CACHE_CAMPAIGN_MKEY, /*CACHE_MKEY_PREFIX,*/ CACHE_CAMPAIGNFILE_KEY, CACHE_INTEGER_KEY, CACHE_REAL_KEY, CACHE_BOOL_KEY,
				CACHE_STRING_KEY, CACHE_UNIT_KEY, /*CACHE_FLUSH_KEY,*/ CACHE_COUNT_KEY, GH_DIALOG_BUTTON_TEXT_KEY);
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

	private static List<String> getCommonFieldSuffixes() {
		return Arrays.asList(CACHE_XP_SUFFIX, CACHE_STR_SUFFIX, CACHE_AGI_SUFFIX, CACHE_INT_SUFFIX, CACHE_UNSPEND_POINTS_SUFFIX,
				CACHE_HEALTH_MAX_SUFFIX, CACHE_MANA_MAX_SUFFIX, CACHE_ATTACK_DAMAGE_BASE_SUFFIX + 0, CACHE_ATTACK_DAMAGE_BASE_SUFFIX + 1,
				CACHE_ABILITY_COUNT_SUFFIX);
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
	public static final String CACHE_ATTACK_RATE_SUFFIX = "dR";
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
	private static final String READ_LOCK_CODE = "loop" + JASS_DELIM +
			"exitwhen XT87ReadLinesWaiting==0" + JASS_DELIM +
			"call TriggerSleepAction(0.01)" + JASS_DELIM +
			"endloop";
	private static final String FLUSH_KEY_CODE =
			"I2S(" + ARCHON_PREFIX + "GetFlushIndex(q,m2,\"" + CACHE_FLUSH_PREFIX + "\"+k2))" +
					"+\"|\"+I2S(" + ARCHON_PREFIX + "GetFlushIndex(q,m2,\"" + CACHE_FLUSH_PREFIX + CACHE_MISSION_PREFIX + "\"))" +
					"+\"|\"+I2S(" + ARCHON_PREFIX + "GetFlushIndex(q,\"" + CACHE_CAMPAIGN_MKEY + "\",\"" + CACHE_FLUSH_PREFIX + CACHE_CACHE_PREFIX + "\"))";

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

	public boolean triggersAdded, initGlobalsAdded, insideInitGlobals, insideConfig, insideCustomPlayerSlots;
	public int endGlobalsState;
	public W3I info;
	public W3I.Player mainPlayer;
	public List<W3I.Player> secondaryPlayers;
	public List<Integer> allPlayerNumbers;
	public List<String> dialogNames;
	public Map<String, Integer> cacheFieldMap;

	public CoopRewriter(MapInjector mi, W3I info, W3I.Player mainPlayer, List<W3I.Player> secondaryPlayers) {
		super(mi);
		this.info = info;
		this.mainPlayer = mainPlayer;
		this.secondaryPlayers = secondaryPlayers;
		allPlayerNumbers = new ArrayList<>(1 + secondaryPlayers.size());
		allPlayerNumbers.add(mainPlayer.getNum());
		secondaryPlayers.forEach(x -> allPlayerNumbers.add(x.getNum()));
		dialogNames = new ArrayList<>();
	}

	public static String getNewPath(String oldPath) {
		return XT87Utils.PATH_PREFIX + "/" + oldPath;
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
		triggersAdded = false;
		initGlobalsAdded = false;
		insideInitGlobals = false;
		insideConfig = false;
		insideCustomPlayerSlots = false;
		insideFunction = null;
		endGlobalsState = 0;
		cacheFieldMap = new HashMap<>();
	}

	private static void putWithCount(Map<String, Integer> map, String key) {
		if (!map.containsKey(key))
			map.put(key, map.size());
	}

	private int getCacheFieldIndex(String key) {
		putWithCount(cacheFieldMap, key);
		return cacheFieldMap.get(key);
	}

	public static String toPlayerFunc(int i) {
		return PLAYER_FUNC + i + ")";
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
				append("    if((" + referencedPlayer + ")==" + toMainPlayerFunc() + ")then", sb);
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
	public void onReadLine(String line, StringBuffer sb) {
		line = line.replace("bj_FORCE_PLAYER[" + mainPlayer.getNum() + "]", FORCE_NAME);
		line = line.replace("call SetSoundDuration(", "//call SetSoundDuration(");

		// region replace specific calls
		for (String function : Arrays.asList(
				"GetLocalPlayer",
				"GetTriggerPlayer",
				"GetForceOfPlayer",
				//"CreateSound",
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
				"CinematicModeBJ"
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
			String call = getCallFromLine(line);
			if (call != null) {
				String[] params = getParamsFromLine(line);
				if (call.equals("call SelectHeroSkill(")) {
					assert params != null;
					append("    call " + ARCHON_PREFIX + "WatchSkill(" + params[1] + ")", sb);
				}
			}
		}

		// variable declarations
		{
			String trimmedLine = line.trim();
			if (trimmedLine.startsWith(LOCAL_DECLARATION))
				trimmedLine = trimmedLine.substring(LOCAL_DECLARATION.length()).trim();
			if (trimmedLine.startsWith("gamecache "))
				line = line.replaceFirst("gamecache ", "hashtable ");
			else if (trimmedLine.startsWith(DIALOG_DECLARATION)) {
				String auxLine = trimmedLine.substring(DIALOG_DECLARATION.length()).trim();
				if (auxLine.contains("="))
					auxLine = auxLine.substring(0, auxLine.indexOf('='));
				dialogNames.add(auxLine.trim());
			}
		}

		if ("main".equals(insideFunction)) {
			if (line.contains(END_FUNCTION)) {
				append("    " + SETPLAYERCOLORBJ + toMainPlayerFunc() + "," + CONVERTPLAYERCOLOR + mainPlayer.getNum() + "),true)", sb);
				for (W3I.Player player : secondaryPlayers) {
					String playerFunc = toPlayerFunc(player.getNum());
					append(SETPLAYERCOLOR + playerFunc + "," + CONVERTPLAYERCOLOR + player.getNum() + "))" + JASS_DELIM +
							"call SetPlayerOnScoreScreen(" + playerFunc + ",false)", sb);
				}
				int index = 0;
				for (int playerNum : allPlayerNumbers) {
					append("set " + PLAYER_ID_ARRAY + "[" + index + "]=" + playerNum + JASS_DELIM +
							"set " + PLAYER_INDEX_ARRAY + "[" + playerNum + "]=" + index, sb);
					index++;
				}
				for (String key : getCommonHashKeys())
					append("set " + getCommonHashFunc(key) + "=StringHash(\"" + key + "\")", sb);
				append("call " + ENFORCE_ARCHON + "()" + JASS_DELIM +
						"if (GetLocalPlayer()!=" + toMainPlayerFunc() + ") then" + JASS_DELIM +
						"call SetReservedLocalHeroButtons(0)" + JASS_DELIM +
						END_IF, sb);
			}
		} else if (insideInitGlobals) {
			if (line.trim().equals(END_FUNCTION)) {
				insideInitGlobals = false;
			} else if (!initGlobalsAdded && line.contains("set ")) {
				append("    set " + FORCE_NAME + "=CreateForce()", sb);
				for (int playerNumber : allPlayerNumbers)
					append("    call ForceAddPlayer(" + FORCE_NAME + "," + toPlayerFunc(playerNumber) + ")", sb);
				initGlobalsAdded = true;
			}
		} else if (line.contains("function InitGlobals takes"))
			insideInitGlobals = true;

		if (Arrays.asList("config", "InitCustomPlayerSlots").contains(insideFunction)) {
			if (line.contains(SETPLAYERCOLOR + toMainPlayerFunc())) {
				String[] params = getParamsFromLine(line);
				// recolor players to blue and yellow
				assert params != null;
				append(line.replace(params[1], CONVERTPLAYERCOLOR + "1)"), sb);
				String secondaryColorLine = line.replace(params[1], CONVERTPLAYERCOLOR + "4)");
				for (W3I.Player player : secondaryPlayers) {
					append(secondaryColorLine.replace(toMainPlayerFunc(), toPlayerFunc(player.getNum())), sb);
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
			String call = getCallFromLine(line);
			String[] params;
			String trimmedCall = null;
			if (call != null) {
				params = getParamsFromLine(line, call);
				trimmedCall = getTrimmedCallFromLine(call);
				if (trimmedCall != null) {
					for (String playerNumberCall : Arrays.asList("SetPlayers", "SetTeams"))
						if (trimmedCall.equals(playerNumberCall)) {
							line = "call " + trimmedCall + "(" + info.getPlayers().size() + ")";
							break;
						}
				}
				if (params != null) {
					CoopCallAdjustments.AdjustmentDetails ad = CoopCallAdjustments.callParameters.get(trimmedCall);
					if (ad != null) {
						appendLine = false;

						if (ad.pointIndex != null) {
							append("set " + TEMP_POINT_NAME + "=" + params[ad.pointIndex], sb);
							params[ad.pointIndex] = TEMP_POINT_NAME;
							line = toCallString(trimmedCall, params);
						}

						if (!ad.isLocal)
							append(line, sb);

						appendCallWithReplacement(trimmedCall, params, ad, sb);
					}
				}
			}

			if (endGlobalsState == 1 && line.trim().startsWith(FUNCTION)) {
				endGlobalsState = 2;
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
							"        function " + FILE_LIBRARY_PREFIX + "" + FILE_IO_PREFIX  + "__FileInit__onInit takes nothing returns nothing" + JASS_DELIM +
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
							"            set " + FILE_LIBRARY_PREFIX + "ReadEnabled=(" + FILE_LIBRARY_PREFIX + "readEx((" + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(\"FileTester.pld\"),\"" + FILE_IO_PREFIX  + "\")),true))==\"" + FILE_IO_PREFIX  + "\" // INLINED!!" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"    function " + FILE_IO_PREFIX  + "Write takes string f,string s returns nothing" + JASS_DELIM +
							"        call " + FILE_LIBRARY_PREFIX + "close(" + FILE_LIBRARY_PREFIX + "write(" + FILE_LIBRARY_PREFIX + "open(f),s))" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"    function " + FILE_IO_PREFIX  + "Read takes string f returns string" + JASS_DELIM +
							"        return " + FILE_LIBRARY_PREFIX + "readEx(" + FILE_LIBRARY_PREFIX + "open(f),true)" + JASS_DELIM +
							END_FUNCTION, sb);
					// endregion
					// region custom functions
					append("function " + ARCHON_PREFIX + "WatchSkill takes integer a returns boolean" + JASS_DELIM +
							"    if not(LoadBoolean(" + GENERAL_HASHTABLE + ",a," + GH_HAS_ABILITY_INT + ")) then" + JASS_DELIM +
							"        set " + ABILITY_ARRAY + "[" + ABILITY_ARRAY_SIZE + "]=a" + JASS_DELIM +
							"        call SaveBoolean(" + GENERAL_HASHTABLE + ",a," + GH_HAS_ABILITY_INT + ",true)" + JASS_DELIM +
							"        set " + ABILITY_ARRAY_SIZE + "=" + ABILITY_ARRAY_SIZE + "+1" + JASS_DELIM +
							"        return true" + JASS_DELIM +
							"    else" + JASS_DELIM +
							"        return false" + JASS_DELIM +
							END_IF + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function Trig_" + ARCHON_PREFIX + "WatchLearnedSkill_Actions takes nothing returns nothing" + JASS_DELIM +
							"    local integer i=0" + JASS_DELIM +
							"    local integer a=GetLearnedSkill()" + JASS_DELIM +
							"    if " + ARCHON_PREFIX + "WatchSkill(a)==true then" + JASS_DELIM +
							DebugOnly("call BJDebugMsg(I2S(i)+\"] \"+I2S(a)+\" _\"+I2S(" + ABILITY_ARRAY_SIZE + " - 1))" + JASS_DELIM) +
							"        loop" + JASS_DELIM +
							"            exitwhen i>=" + ABILITY_ARRAY_SIZE + JASS_DELIM +
							DebugOnly("call BJDebugMsg(I2S(i)+\") \"+I2S(" + ABILITY_ARRAY + "[i])+\" \"+I2S(" + ABILITY_ARRAY_SIZE + " - 1))" + JASS_DELIM) +
							"            set i=i+1" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"    " + END_IF + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function InitTrig_" + ARCHON_PREFIX + "WatchLearnedSkill takes nothing returns nothing" + JASS_DELIM +
							"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "WatchLearnedSkill=CreateTrigger()" + JASS_DELIM +
							"    call TriggerRegisterAnyUnitEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "WatchLearnedSkill,EVENT_PLAYER_HERO_SKILL)" + JASS_DELIM +
							"    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "WatchLearnedSkill,function Trig_" + ARCHON_PREFIX + "WatchLearnedSkill_Actions)" + JASS_DELIM +
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
					append("function " + ARCHON_PREFIX + "WriteString takes string cacheName,string title,string v,string fK returns nothing" + JASS_DELIM +
							"local string p=\"XT87CampaignSplitter\\\\\"+cacheName+\"\\\\\"+title+\".pld\"" + JASS_DELIM +
							"    call " + FILE_IO_PREFIX  + "Write(p,v+\"\\n\"+" + ARCHON_PREFIX + "IS62(StringHash(v+\"|\"+p+\"|\"+fK)))" + JASS_DELIM +
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
							"    if StringHash(SubString(content,0,j)+\"|\"+p+\"|\"+fK)!=" + ARCHON_PREFIX + "S62I(SubString(content,j+1,len)) then" + JASS_DELIM +
							"        set content=\"\"" + JASS_DELIM +
							END_IF + JASS_DELIM +
							DebugOnly("call BJDebugMsg(\">\"+content+\"<\")" + JASS_DELIM) +
							"        call BlzSendSyncData(\"XTContents\",content)" + JASS_DELIM +
							"    endif" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen XT87ReadLinesAreRead" + JASS_DELIM +
							"        call TriggerSleepAction(0.01)" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "ReadString takes string cacheName,string title,string fK returns string" + JASS_DELIM +
							"local string s" + JASS_DELIM +
							"    if GetPlayerSlotState(" + toMainPlayerFunc() + ")!=PLAYER_SLOT_STATE_PLAYING then" + JASS_DELIM +
							"        return null" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"set XT87ReadLinesWaiting=XT87ReadLinesWaiting+1" + JASS_DELIM +
							"call " + ARCHON_PREFIX + "SyncContents(\"XT87CampaignSplitter\\\\\"+cacheName+\"\\\\\"+title+\".pld\",fK)" + JASS_DELIM +
							"set s=XT87ReadLines[0]" + JASS_DELIM +
							"set XT87ReadLinesAreRead=false" + JASS_DELIM +
							"set XT87ReadLinesLock=false" + JASS_DELIM +
							"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1" + JASS_DELIM +
							"return s" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "ReadUnit takes string cacheName,string title,string fK returns integer" + JASS_DELIM +
							"    local integer i" + JASS_DELIM +
							"set XT87ReadLinesWaiting=XT87ReadLinesWaiting+1" + JASS_DELIM +
							"    if GetPlayerSlotState(" + toMainPlayerFunc() + ")!=PLAYER_SLOT_STATE_PLAYING then" + JASS_DELIM +
							"        return 0" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"    call " + ARCHON_PREFIX + "SyncContents(\"XT87CampaignSplitter\\\\\"+cacheName+\"\\\\\"+title+\".pld\",fK)" + JASS_DELIM +
							"    if XT87ReadLines[0]==null then" + JASS_DELIM +
							"        return 0" + JASS_DELIM +
							END_IF + JASS_DELIM +
							DebugOnly("call BJDebugMsg(\"0)\"+XT87ReadLines[0])" + JASS_DELIM) +
							DebugOnly("call BJDebugMsg(\"1)\"+XT87ReadLines[1])" + JASS_DELIM) +
							DebugOnly("call BJDebugMsg(\"2)\"+XT87ReadLines[2])" + JASS_DELIM) +
							"    return " + ARCHON_PREFIX + "S62I(XT87ReadLines[0])" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function Trig_" + ARCHON_PREFIX + "SyncData_Actions takes nothing returns nothing" + JASS_DELIM +
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
							"set XT87ReadLinesAreRead=true" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function InitTrig_" + ARCHON_PREFIX + "SyncData takes nothing returns nothing" + JASS_DELIM +
							"    local integer i=0" + JASS_DELIM +
							"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "SyncData=CreateTrigger()" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        call BlzTriggerRegisterPlayerSyncEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "SyncData,Player(i),\"XTContents\",false)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        exitwhen i==bj_MAX_PLAYER_SLOTS" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							"    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "SyncData,function Trig_" + ARCHON_PREFIX + "SyncData_Actions)" + JASS_DELIM +
							END_FUNCTION, sb);
					// endregion
					// region button add functions
					append("function " + ARCHON_PREFIX + "DialogAddButton takes dialog d,string s,integer h returns button" + JASS_DELIM +
							"local button b=DialogAddButton(d,s,h)" + JASS_DELIM +
							"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_DIALOG_BUTTON_TEXT_INT + ",GetHandleId(b),s)" + JASS_DELIM +
							"return b" + JASS_DELIM +
							END_FUNCTION, sb);
					append("function " + ARCHON_PREFIX + "DialogAddQuitButton takes dialog d,boolean t,string s,integer h returns button" + JASS_DELIM +
							"local button b=DialogAddQuitButton(d,t,s,h)" + JASS_DELIM +
							"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_DIALOG_BUTTON_TEXT_INT + ",GetHandleId(b),s)" + JASS_DELIM +
							"return b" + JASS_DELIM +
							END_FUNCTION, sb);
					for (Triple<String, String, String> o : Arrays.asList(
							new Triple<>("", "", "0"),
							new Triple<>("WithHotkey", ",integer h", "h")
					))
						append("function " + ARCHON_PREFIX + "DialogAddButton" + o.a + "BJ takes dialog d,string s" + o.b + " returns button" + JASS_DELIM +
								"set bj_lastCreatedButton=DialogAddButton(d,s," + o.c + ")" + JASS_DELIM +
								"call SaveStr(" + GENERAL_HASHTABLE + "," + GH_DIALOG_BUTTON_TEXT_INT + ",GetHandleId(bj_lastCreatedButton),s)" + JASS_DELIM +
								"return bj_lastCreatedButton" + JASS_DELIM +
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
					// endregion
					// region GetFlushIndex function
					// uses m2 and flush prefixed k2
					append("function " + ARCHON_PREFIX + "GetFlushIndex takes hashtable q,string m,string k returns integer" + JASS_DELIM +
							"    local string s" + JASS_DELIM +
							"    local integer i=0" + JASS_DELIM +
							"local integer mh=StringHash(m)" + JASS_DELIM +
							"local integer kh=StringHash(k)" + JASS_DELIM +
							"    local string cacheName=" + getCacheNameFunc("q") + JASS_DELIM +
							"if HaveSavedInteger(q,mh,kh) then" + JASS_DELIM +
							"    return LoadInteger(q,mh,kh)" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"set s=" + ARCHON_PREFIX + "ReadString(cacheName,m+\"\\\\\"+k,null)" + JASS_DELIM +
							"if s!=null then" + JASS_DELIM +
							"   set i=" + getConversionFunc(ConversionFunctionType.STRING_TO_INT) + "(s)" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"call SaveInteger(q,mh,kh,i)" + JASS_DELIM +
							"return i" + JASS_DELIM +
							END_FUNCTION, sb);
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
							"    local string cacheName=" + getCacheNameFunc("q") + JASS_DELIM +
							DebugOnly("call BJDebugMsg(\"SaveGameCache \"+cacheName)" + JASS_DELIM) +
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
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(LoadInteger(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
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
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + getConversionFunc(ConversionFunctionType.REAL_TO_STRING) + "(LoadReal(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
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
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "B2S(LoadBoolean(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
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
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "SS62(LoadStr(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
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
							"            loop", sb);
					for (String suffix : Arrays.asList(CACHE_ABILITY_ID_SUFFIX, CACHE_ABILITY_LVL_SUFFIX))
						append("set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(LoadInteger(q,mh,StringHash(k2+\"" + suffix + "\"+I2S(j))))", sb);
					append("            set j=j+1" + JASS_DELIM +
							"            exitwhen j>=c" + JASS_DELIM +
							"            endloop" + JASS_DELIM +
							END_IF + JASS_DELIM + // endif hero
							"set c=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_INVENTORY_SIZE_SUFFIX + "\"))" + JASS_DELIM +
							"set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(c)" + JASS_DELIM +
							"        set j=0" + JASS_DELIM +
							"        loop" + JASS_DELIM +
							"        set it=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFFIX + "\"+I2S(j)))" + JASS_DELIM +
							"        set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(it)" + JASS_DELIM +
							"        if it!=0 then", sb);
					for (String suffix : Arrays.asList(CACHE_ITEM_CHARGE_SUFFIX, CACHE_ITEM_BITS_SUFFIX))
						append("set uD=uD+\"\\n\"+" + getConversionFunc(ConversionFunctionType.INT_TO_STRING) + "(LoadInteger(q,mh,StringHash(k2+\"" + suffix + "\"+I2S(j))))", sb);
					append("        endif" + JASS_DELIM +
							"        set j=j+1" + JASS_DELIM +
							"        exitwhen j>=c" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2,uD," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
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
							"            set a=" + ABILITY_ARRAY + "[i]" + JASS_DELIM +
							"            set l=GetUnitAbilityLevel(u,a)" + JASS_DELIM +
							"            if l>0 then" + JASS_DELIM +
							"               call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_ID_SUFFIX + "\"+I2S(c)),a)" + JASS_DELIM +
							"               call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_LVL_SUFFIX + "\"+I2S(c)),l)" + JASS_DELIM +
							"               set c=c+1" + JASS_DELIM +
							"            " + END_IF + JASS_DELIM +
							"            exitwhen i>" + ABILITY_ARRAY_SIZE + JASS_DELIM +
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
								"    local string cacheName=" + getCacheNameFunc("q") + JASS_DELIM +
								"if HaveSaved" + o.a + "(q,mh,kh) then" + JASS_DELIM +
								"    return true" + JASS_DELIM +
								END_IF + JASS_DELIM +
								"set s=" + ARCHON_PREFIX + "ReadString(cacheName,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
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
							"    local string cacheName=" + getCacheNameFunc("q") + JASS_DELIM +
							"if LoadInteger(q,StringHash(m2),StringHash(k2+\"" + CACHE_ID_SUFFIX + "\"))!=0 then" + JASS_DELIM +
							"    return true" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"return " + ARCHON_PREFIX + "ReadString(cacheName,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")!=null" + JASS_DELIM +
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
							"    local string cacheName=" + getCacheNameFunc("q") + JASS_DELIM +
							DebugOnly("call BJDebugMsg(\"RestoreUnit \"+cacheName+\"|\"+m+\"|\"+k+\" > \"+LoadStr(q,mh,StringHash(\"" + CACHE_STRING_PREFIX + "\"+k)))" + JASS_DELIM) +
							"    set id=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ID_SUFFIX + "\"))" + JASS_DELIM +
							"if id==0 then" + JASS_DELIM +
							"        set id=" + ARCHON_PREFIX + "ReadUnit(cacheName,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
							"    if id==0 then" + JASS_DELIM +
							"set XT87ReadLinesAreRead=false" + JASS_DELIM +
							"set XT87ReadLinesLock=false" + JASS_DELIM +
							"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1" + JASS_DELIM +
							"        return null" + JASS_DELIM +
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
							"        call SetUnitState(u,UNIT_STATE_MANA,v[" + getCacheFieldIndex(CACHE_MANA_MAX_SUFFIX) + "])" + JASS_DELIM +
							"        call BlzSetUnitArmor(u,vr[" + getCacheFieldIndex(CACHE_ARMOR_SUFFIX) + "]+BlzGetUnitArmor(u))" + JASS_DELIM +
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
						i = l.size();
						for (String s : l) {
							if (!s.startsWith("!"))
								append("call " + String.format(s, "ti", moduloFunc("v[" + getCacheFieldIndex(CACHE_ITEM_BITS_SUFFIX) + "]", "2") + "==1"), sb);
							i--;
							if (i > 0)
								append("set v[" + getCacheFieldIndex(CACHE_ITEM_BITS_SUFFIX) + "]=v[" + getCacheFieldIndex(CACHE_ITEM_BITS_SUFFIX) + "]/2", sb);
						}
					}
					append(END_IF + JASS_DELIM + // endif item exists
							"        set i=i+1" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							"if r then" + JASS_DELIM + // if read
							"set XT87ReadLinesAreRead=false" + JASS_DELIM +
							"set XT87ReadLinesLock=false" + JASS_DELIM +
							"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1" + JASS_DELIM +
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
									READ_LOCK_CODE + JASS_DELIM +
									"return " + o.a + o.c + JASS_DELIM +
									END_FUNCTION, sb);
						for (Triple<String, String, String> o : Collections.singletonList(
								new Triple<>("TriggerEvaluate", "trigger t returns boolean", "(t)")
						))
							append("function " + ARCHON_PREFIX + o.a + " takes " + o.b + JASS_DELIM +
									READ_LOCK_CODE + JASS_DELIM +
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
					append("function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions takes nothing returns nothing" + JASS_DELIM +
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
							"function InitTrig_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing" + JASS_DELIM +
							"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()", sb);
					for (int playerNumber : allPlayerNumbers)
						append("    call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toPlayerFunc(playerNumber) + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0)", sb);
					append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions)" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM, sb);
				}
				// endregion
				// region food and population cap synchronization
				resourceTypes = Arrays.asList("PLAYER_STATE_RESOURCE_FOOD_USED", "PLAYER_STATE_RESOURCE_FOOD_CAP", "PLAYER_STATE_FOOD_CAP_CEILING");
				resourceNames = getFoodResourceNames();
				for (int i = 0; i < resourceTypes.size(); i++) {
					String resourceType = resourceTypes.get(i);
					String resourceName = resourceNames.get(i);
					append("function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions takes nothing returns nothing" + JASS_DELIM +
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
							"function InitTrig_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing" + JASS_DELIM +
							"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()", sb);
					append("call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toMainPlayerFunc() + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0)", sb);
					append("call TriggerRegisterTimerEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,0,false)", sb);
					append("call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions)" + JASS_DELIM +
							END_FUNCTION, sb);
				}
				// endregion
				// region give units to main player
				append("function Trig_" + ARCHON_PREFIX + "UnitShare_Conditions takes nothing returns boolean", sb);
				for (int playerNumber : allPlayerNumbers) {
					if (playerNumber == mainPlayer.getNum())
						continue;
					append("    if GetOwningPlayer(GetEnteringUnit())==" + toPlayerFunc(playerNumber) + " then" + JASS_DELIM +
							"        return true" + JASS_DELIM +
							"    endif", sb);
				}
				append("    return false" + JASS_DELIM +
						END_FUNCTION + JASS_DELIM +
						"function Trig_" + ARCHON_PREFIX + "UnitShare_Actions takes nothing returns nothing" + JASS_DELIM +
						"    call SetUnitOwner(GetEnteringUnit()," + toMainPlayerFunc() + ",true)" + JASS_DELIM +
						END_FUNCTION + JASS_DELIM +
						"function InitTrig_" + ARCHON_PREFIX + "UnitShare takes nothing returns nothing" + JASS_DELIM +
						"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare=CreateTrigger()" + JASS_DELIM +
						"    call TriggerRegisterEnterRectSimple(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,GetPlayableMapRect())" + JASS_DELIM +
						"    call TriggerAddCondition(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,Condition(function Trig_" + ARCHON_PREFIX + "UnitShare_Conditions))" + JASS_DELIM +
						"    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare,function Trig_" + ARCHON_PREFIX + "UnitShare_Actions)" + JASS_DELIM +
						END_FUNCTION, sb);
				// endregion
				// region selection effects
				append("function " + ARCHON_PREFIX + "OnSelect takes nothing returns nothing" + JASS_DELIM +
						"local integer p=GetPlayerId(" + AUX_PLAYER + ")" + JASS_DELIM +
						"local integer c=LoadInteger(" + SELECTION_HASHTABLE + ",p,0)+1" + JASS_DELIM +
						"local effect f=AddSpecialEffectTarget(\"" + XT87Utils.PATH_PREFIX + "\\\\" + SELECTION_INDICATOR_PATH + ".mdx\",GetEnumUnit(),\"overhead\")" + JASS_DELIM +
						"call BlzSetSpecialEffectColorByPlayer(f,Player(p))" + JASS_DELIM +
						"call SaveEffectHandle(" + SELECTION_HASHTABLE + ",p,c,f)" + JASS_DELIM +
						"call SaveInteger(" + SELECTION_HASHTABLE + ",p,0,c)" + JASS_DELIM +
						END_FUNCTION, sb);
				append("function Trig_" + ARCHON_PREFIX + "Select_Actions takes nothing returns nothing" + JASS_DELIM +
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
						"set i=i+1" + JASS_DELIM +
						"exitwhen i>c" + JASS_DELIM +
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
						"function InitTrig_" + ARCHON_PREFIX + "Select takes nothing returns nothing" + JASS_DELIM +
						"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "Select=CreateTrigger()", sb);
				for (int playerNumber : allPlayerNumbers)
					for (String selectBool : Arrays.asList("false", "true"))
						append("    call TriggerRegisterPlayerSelectionEventBJ(" + TRIGGER_PREFIX + ARCHON_PREFIX + "Select," + toPlayerFunc(playerNumber) + "," + selectBool + ")", sb);
				append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "Select, function Trig_" + ARCHON_PREFIX + "Select_Actions)" + JASS_DELIM +
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
				append("function Trig_" + ARCHON_PREFIX + "DialogSync_Actions takes nothing returns nothing" + JASS_DELIM +
								"local player p=GetTriggerPlayer()" + JASS_DELIM +
								"local string s=LoadStr(" + GENERAL_HASHTABLE + "," + GH_DIALOG_BUTTON_TEXT_INT + ",GetHandleId(GetClickedButton()))"
						, sb);
				for (int playerNumber : allPlayerNumbers)
					append("call DialogDisplay(" + toPlayerFunc(playerNumber) + ",GetClickedDialog(),false)", sb);
				append("call TriggerSleepAction(0.01)" + JASS_DELIM +
						"call DisplayTimedTextFromPlayer(p,0,0,10,\"%s => \"+GetLocalizedString(s))" + JASS_DELIM +
						END_FUNCTION + JASS_DELIM +
						"function InitTrig_" + ARCHON_PREFIX + "DialogSync takes nothing returns nothing" + JASS_DELIM +
						"set " + TRIGGER_PREFIX + ARCHON_PREFIX + "DialogSync=CreateTrigger()", sb);
				for (String dialogName : dialogNames)
					append("    call TriggerRegisterDialogEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + "DialogSync," + dialogName + ")", sb);
				append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + "DialogSync,function Trig_" + ARCHON_PREFIX + "DialogSync_Actions)" + JASS_DELIM +
						END_FUNCTION, sb);
				// endregion
			}

			if (appendLine)
				append(line, sb);

			if (trimmedCall != null)
				if (!Arrays.asList("main", "RunInitializationTriggers").contains(insideFunction))
					for (String function : Arrays.asList("TriggerExecute", "TriggerExecuteWait", "ConditionalTriggerExecute"))
						if (trimmedCall.equals(function)) {
							append(READ_LOCK_CODE, sb);
							break;
						}
		}

		if (line.trim().equals("globals")) {
			for (String resourceName : getAllResourceNames())
				append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync", sb);
			append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare" + JASS_DELIM +
							"force " + FORCE_NAME + JASS_DELIM +
							"location " + TEMP_POINT_NAME + JASS_DELIM +
							"integer array " + PLAYER_ID_ARRAY + JASS_DELIM +
							"integer array " + PLAYER_INDEX_ARRAY + JASS_DELIM +
							"integer array " + HASH_ARRAY + JASS_DELIM +
							"player " + AUX_PLAYER + JASS_DELIM +
							"hashtable " + GENERAL_HASHTABLE + "=InitHashtable()" + JASS_DELIM +
							"hashtable " + SELECTION_HASHTABLE + "=InitHashtable()" + JASS_DELIM +
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "Select" + JASS_DELIM +
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "DialogSync"
//							+ JASS_DELIM +
//					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "Deselect" + JASS_DELIM +
//					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "DeselectOnDeath"
					, sb);
			if (WITH_CUSTOM_GAMECACHE)
				append("hashtable " + LAST_CREATED_COOP_CACHE + JASS_DELIM +
						"integer array " + ABILITY_ARRAY + JASS_DELIM +
						"integer " + ABILITY_ARRAY_SIZE + "=0" + JASS_DELIM +
						"integer " + GH_HAS_ABILITY_INT + "=StringHash(\"" + GH_HAS_ABILITY_KEY + "\")" + JASS_DELIM +
						"integer " + GH_DIALOG_BUTTON_TEXT_INT + "=StringHash(\"" + GH_DIALOG_BUTTON_TEXT_KEY + "\")" + JASS_DELIM +
						"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "WatchLearnedSkill" + JASS_DELIM +
						"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "SyncData" + JASS_DELIM +
						"integer " + FILE_LIBRARY_PREFIX + "CT=0" + JASS_DELIM + // Counter
						"integer array " + FILE_LIBRARY_PREFIX + "LS" + JASS_DELIM + // List
						"integer array " + FILE_LIBRARY_PREFIX + "AL" + JASS_DELIM + // AbilityList
						"boolean " + FILE_LIBRARY_PREFIX + "ReadEnabled=false" + JASS_DELIM +
						"string array " + FILE_LIBRARY_PREFIX + "FN" + JASS_DELIM +
						"string array " + FILE_LIBRARY_PREFIX + "BF" + JASS_DELIM +
						"string array XT87ReadLines" + JASS_DELIM +
						"boolean XT87ReadLinesAreRead=false" + JASS_DELIM +
						"boolean XT87ReadLinesLock=false" + JASS_DELIM +
						"integer XT87ReadLinesWaiting=0", sb);
		} else if (line.trim().equals("endglobals")) {
			if (endGlobalsState == 0)
				endGlobalsState = 1;
		} else if (line.contains("function InitCustomTriggers takes")) {
			for (String resourceName : getAllResourceNames())
				append("    call InitTrig_" + ARCHON_PREFIX + resourceName + "Sync()", sb);
			for (String triggerName : Arrays.asList("UnitShare", "Select", "DialogSync"))//, "Deselect", "DeselectOnDeath"))
				append("    call InitTrig_" + ARCHON_PREFIX + triggerName + "()", sb);
			if (WITH_CUSTOM_GAMECACHE) {
				append("    call InitTrig_" + ARCHON_PREFIX + "SyncData()" + JASS_DELIM +
						"    call InitTrig_" + ARCHON_PREFIX + "WatchLearnedSkill()" + JASS_DELIM +
						"    call ExecuteFunc(\"" + FILE_LIBRARY_PREFIX + "" + FILE_IO_PREFIX  + "__FileInit__onInit\")", sb);
			}
		} else if (line.contains("SetForceAllianceStateBJ("))
			append("call " + ENFORCE_ARCHON + "()", sb);

		if (line.contains(DEFINE_START_LOC + mainPlayer.getNum() + ","))
			for (W3I.Player player : secondaryPlayers)
				append(line.replace(DEFINE_START_LOC + mainPlayer.getNum(), DEFINE_START_LOC + player.getNum()), sb);
	}
}
