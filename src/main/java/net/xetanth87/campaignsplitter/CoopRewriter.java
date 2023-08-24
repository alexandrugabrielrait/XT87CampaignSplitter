package net.xetanth87.campaignsplitter;

import net.moonlightflower.wc3libs.bin.app.W3I;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Triple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CoopRewriter extends ScriptRewriter {
	public static final String SETPLAYERCOLOR = "call SetPlayerColor(";
	public static final String DEFINE_START_LOC = "call DefineStartLocation(";
	public static final String SETPLAYERCOLORBJ = "call SetPlayerColorBJ(";
	public static final String CONVERTPLAYERCOLOR = "ConvertPlayerColor(";
	public static final String PLAYER_FUNC = "Player(";
	public static final String TRIGGER_PREFIX = "tr_";
	public static final String ARCHON_PREFIX = "XT87AS";
	public static final String FORCE_NAME = ARCHON_PREFIX + "Force";
	public static final String TEMP_POINT_NAME = ARCHON_PREFIX + "TempPoint";
	public static final String ENFORCE_ARCHON = ARCHON_PREFIX + "EnforceArchon";
	public static final String LAST_CREATED_COOP_CACHE = ARCHON_PREFIX + "_lastCreatedGameCache";
	public static final String PLAYER_ID_ARRAY = ARCHON_PREFIX + "IDA";
	public static final String PLAYER_INDEX_ARRAY = ARCHON_PREFIX + "PIA";
	public static final String AUX_PLAYER = ARCHON_PREFIX + "AXP";
	public static final String GENERAL_HASHTABLE = ARCHON_PREFIX + "GHT";
	public static final String SELECTION_HASHTABLE = ARCHON_PREFIX + "FXHT";
	public static final String SELECTION_INDICATOR_PATH = "selectionIndicator";
	public static final String GH_HAS_ABILITY = "hasAbility";
	public static final String GH_DIALOG_BUTTON_TEXT = "dbt";
	public static final String ABILITY_ARRAY = ARCHON_PREFIX + "AbilityArray";
	public static final String ABILITY_ARRAY_SIZE = ABILITY_ARRAY + "SIZE";
	public static final String LOCAL_DECLARATION = "local ";
	public static final String DIALOG_DECLARATION = "dialog ";

	// coop cache
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

	public static final String CACHE_CAMPAIGNFILE_KEY = CACHE_OTHER_PREFIX + CACHE_CACHE_PREFIX;
	public static final String CACHE_INTEGER_KEY = CACHE_OTHER_PREFIX + CACHE_INTEGER_PREFIX;
	public static final String CACHE_REAL_KEY = CACHE_OTHER_PREFIX + CACHE_REAL_PREFIX;
	public static final String CACHE_BOOL_KEY = CACHE_OTHER_PREFIX + CACHE_BOOL_PREFIX;
	public static final String CACHE_STRING_KEY = CACHE_OTHER_PREFIX + CACHE_STRING_PREFIX;
	public static final String CACHE_UNIT_KEY = CACHE_OTHER_PREFIX + CACHE_UNIT_PREFIX;
	public static final String CACHE_FLUSH_KEY = CACHE_OTHER_PREFIX + CACHE_FLUSH_PREFIX;
	// todo make sure count doesn't count the same variable twice
	public static final String CACHE_COUNT_KEY = "count";

	public static final String CACHE_ID_SUFIX = "id";
	public static final String CACHE_IS_HERO_SUFIX = "iH";
	public static final String CACHE_XP_SUFIX = "xp";
	public static final String CACHE_STR_SUFIX = "aS";
	public static final String CACHE_AGI_SUFIX = "aA";
	public static final String CACHE_INT_SUFIX = "aI";
	public static final String CACHE_UNSPEND_POINTS_SUFIX = "uP";
	public static final String CACHE_HEALTH_MAX_SUFIX = "hp";
	public static final String CACHE_MANA_MAX_SUFIX = "mp";
	public static final String CACHE_ABILITY_ID_SUFIX = "A";
	public static final String CACHE_ABILITY_LVL_SUFIX = "L";
	public static final String CACHE_ABILITY_COUNT_SUFIX = "Ac";
	public static final String CACHE_ITEM_ID_SUFIX = "I";
	public static final String CACHE_ITEM_CHARGE_SUFIX = "C";

	// common code
	private static final String READ_LOCK_CODE = "loop" + JASS_DELIM +
			"exitwhen XT87ReadLinesWaiting==0" + JASS_DELIM +
			"call TriggerSleepAction(0.01)" + JASS_DELIM +
			"endloop";
	private static final String FLUSH_KEY_CODE =
			"I2S(" + ARCHON_PREFIX + "GetFlushIndex(q,m2,\"" + CACHE_FLUSH_PREFIX + "\"+k2))" +
					"+\"|\"+I2S(" + ARCHON_PREFIX + "GetFlushIndex(q,m2,\"" + CACHE_FLUSH_PREFIX + CACHE_MISSION_PREFIX + "\"))" +
					"+\"|\"+I2S(" + ARCHON_PREFIX + "GetFlushIndex(q,\"" + CACHE_CAMPAIGN_MKEY + "\",\"" + CACHE_FLUSH_PREFIX + CACHE_CACHE_PREFIX + "\"))";

	public static final List<String> ALL_RESOURCE_NAMES = Arrays.asList("Gold", "Lumber", "FoodUsed", "FoodCap", "FoodCeiling");
	public boolean triggersAdded, initGlobalsAdded, insideInitGlobals, insideConfig, insideCustomPlayerSlots, withCustomGameCache;
	public int endGlobalsState;
	public W3I info;
	public W3I.Player mainPlayer;
	public List<W3I.Player> secondaryPlayers;
	public List<Integer> allPlayerNumbers;
	public List<String> dialogNames;

	public CoopRewriter(MapInjector mi, W3I info, W3I.Player mainPlayer, List<W3I.Player> secondaryPlayers) {
		super(mi);
		this.info = info;
		this.mainPlayer = mainPlayer;
		this.secondaryPlayers = secondaryPlayers;
		allPlayerNumbers = new ArrayList<>(1 + secondaryPlayers.size());
		allPlayerNumbers.add(mainPlayer.getNum());
		secondaryPlayers.forEach(x -> allPlayerNumbers.add(x.getNum()));
		dialogNames = new ArrayList<>();
		withCustomGameCache = true;
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

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		line = line.replace("bj_FORCE_PLAYER[" + mainPlayer.getNum() + "]", FORCE_NAME);
		line = line.replace("GetLocalPlayer()", ARCHON_PREFIX + "GetMainPlayer()");
		line = line.replace("call SetSoundDuration(", "//call SetSoundDuration(");
		// region replace specific calls
//		for (String specificFunction : Arrays.asList(
//				"CreateSound",
//		))
//			for (String suffix : Arrays.asList("(", " ")) {
//				String specificCall = specificFunction + suffix;
//				line = line.replace(specificCall, ARCHON_PREFIX + specificCall);
//			}
		if (withCustomGameCache) {
			line = line.replace("bj_lastCreatedGameCache", LAST_CREATED_COOP_CACHE);
			line = line.replace("GetLastCreatedGameCacheBJ()", LAST_CREATED_COOP_CACHE);
			for (String prefix : Arrays.asList(",", " "))
				line = line.replace(prefix + "gamecache ", prefix + "hashtable ");
			// region replace game cache calls
			for (String gameCacheFunction : Arrays.asList(
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
						String gameCacheCall = gameCacheFunction + suffix;
						line = line.replace(prefix + gameCacheCall, prefix + ARCHON_PREFIX + gameCacheCall);
					}
			// endregion
			// region replace trigger execution calls
			if (!Arrays.asList("main", "RunInitializationTriggers").contains(insideFunction)) {
				for (String gameCacheFunction : Arrays.asList(
//					"TriggerExecute",
//					"TriggerExecuteWait",
						"TriggerEvaluate",
//					"ConditionalTriggerExecute",
						"TriggerExecuteBJ"
				))
					for (String prefix : Arrays.asList("(", " ", ",", "="))
						for (String suffix : Arrays.asList("(", " ")) {
							String gameCacheCall = gameCacheFunction + suffix;
							line = line.replace(prefix + gameCacheCall, prefix + ARCHON_PREFIX + gameCacheCall);
						}
			}
			// endregion
			// region replace button add calls
			for (String gameCacheFunction : Arrays.asList(
					"DialogAddButton",
					"DialogAddQuitButton",
					"DialogAddButtonBJ",
					"DialogAddButtonWithHotkeyBJ"
			))
				for (String prefix : Arrays.asList("(", " ", ",", "="))
					for (String suffix : Arrays.asList("(", " ")) {
						String gameCacheCall = gameCacheFunction + suffix;
						line = line.replace(prefix + gameCacheCall, prefix + ARCHON_PREFIX + gameCacheCall);
					}
			// endregion
			// region replace SyncStored calls
			for (String gameCacheFunction : Arrays.asList(
					"SyncStoredInteger",
					"SyncStoredReal",
					"SyncStoredBoolean",
					"SyncStoredUnit",
					"SyncStoredString"
			))
				for (String prefix : Collections.singletonList(" "))
					for (String suffix : Arrays.asList("(", " "))
						line = line.replace(prefix + gameCacheFunction + suffix, prefix + ARCHON_PREFIX + "SyncStoredUnused" + suffix);
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
				if (withCustomGameCache) {
					// region FileIO library
					append("        function s__File_open takes string filename returns integer" + JASS_DELIM +
							"            local integer this= s__File_List[0]" + JASS_DELIM +
							"            if ( this==0 ) then" + JASS_DELIM +
							"                set this=s__File_Counter+1" + JASS_DELIM +
							"                set s__File_Counter=this" + JASS_DELIM +
							"            else" + JASS_DELIM +
							"                set s__File_List[0]=s__File_List[this]" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"            " + JASS_DELIM +
							"            set s__File_filename[this]=filename" + JASS_DELIM +
							"            set s__File_buffer[this]=null" + JASS_DELIM +
							"            " + JASS_DELIM +
							"            " + JASS_DELIM +
							"            return this" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        // This is used to detect invalid characters which aren't supported in preload files." + JASS_DELIM +
							"        function s__File_write takes integer this,string contents returns integer" + JASS_DELIM +
							"            local integer i= 0" + JASS_DELIM +
							"            local integer c= 0" + JASS_DELIM +
							"            local integer len= StringLength(contents)" + JASS_DELIM +
							"            local integer lev= 0" + JASS_DELIM +
							"            local string prefix= \"-\"" + JASS_DELIM +
							"            local string chunk" + JASS_DELIM +
							"            set s__File_buffer[this]=null" + JASS_DELIM +
							"            // Check if the string is empty. If null,the contents will be cleared." + JASS_DELIM +
							"            if ( contents==\"\" ) then" + JASS_DELIM +
							"                set len=len+1" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"            // Begin to generate the file" + JASS_DELIM +
							"            call PreloadGenClear()" + JASS_DELIM +
							"            call PreloadGenStart()" + JASS_DELIM +
							"            loop" + JASS_DELIM +
							"                exitwhen i >= len" + JASS_DELIM +
							"                set lev=0" + JASS_DELIM +
							"                set chunk=SubString(contents,i,i+s__File_PreloadLimit)" + JASS_DELIM +
							"                call Preload(\"\\\" )\\ncall BlzSetAbilityTooltip(\"+I2S(s__File_AbilityList[c])+\",\\\"\"+prefix+chunk+\"\\\",\"+I2S(lev)+\")\\n//\")" + JASS_DELIM +
							"                set i=i+s__File_PreloadLimit" + JASS_DELIM +
							"                set c=c+1" + JASS_DELIM +
							"            endloop" + JASS_DELIM +
							"            call Preload(\"\\\" )\\nendfunction\\nfunction a takes nothing returns nothing\\n //\")" + JASS_DELIM +
							"            call PreloadGenEnd(s__File_filename[this])" + JASS_DELIM +
							"            return this" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_clear takes integer this returns integer" + JASS_DELIM +
							"            return s__File_write(this,null)" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_readPreload takes integer this returns string" + JASS_DELIM +
							"            local integer i= 0" + JASS_DELIM +
							"            local integer lev= 0" + JASS_DELIM +
							"            local string array original" + JASS_DELIM +
							"            local string chunk= \"\"" + JASS_DELIM +
							"            local string output= \"\"" + JASS_DELIM +
							"            loop" + JASS_DELIM +
							"                exitwhen i==s__File_AbilityCount" + JASS_DELIM +
							"                set original[i]=BlzGetAbilityTooltip(s__File_AbilityList[i],0)" + JASS_DELIM +
							"                set i=i+1" + JASS_DELIM +
							"            endloop" + JASS_DELIM +
							"            // Execute the preload file" + JASS_DELIM +
							"            call Preloader(s__File_filename[this])" + JASS_DELIM +
							"            // Read the output" + JASS_DELIM +
							"            set i=0" + JASS_DELIM +
							"            loop" + JASS_DELIM +
							"                exitwhen i==s__File_AbilityCount" + JASS_DELIM +
							"                " + JASS_DELIM +
							"                set lev=0" + JASS_DELIM +
							"                // Read from ability index 1 instead of 0 if " + JASS_DELIM +
							"                // backwards compatability is enabled" + JASS_DELIM +
							"                // Make sure the tooltip has changed" + JASS_DELIM +
							"                set chunk=BlzGetAbilityTooltip(s__File_AbilityList[i],lev)" + JASS_DELIM +
							"                if ( chunk==original[i] ) then" + JASS_DELIM +
							"                    if ( i==0 and output==\"\" ) then" + JASS_DELIM +
							"                        return null // empty file" + JASS_DELIM +
							"                    endif" + JASS_DELIM +
							"                    return output" + JASS_DELIM +
							"                endif" + JASS_DELIM +
							"                // Check if the file is an empty string or null" + JASS_DELIM +
							"                    if ( i==0 ) then" + JASS_DELIM +
							"                        if ( SubString(chunk,0,1) != \"-\" ) then" + JASS_DELIM +
							"                            return null // empty file" + JASS_DELIM +
							"                        endif" + JASS_DELIM +
							"                        set chunk=SubString(chunk,1,StringLength(chunk))" + JASS_DELIM +
							"                    endif" + JASS_DELIM +
							"                // Remove the prefix" + JASS_DELIM +
							"                if ( i > 0 ) then" + JASS_DELIM +
							"                    set chunk=SubString(chunk,1,StringLength(chunk))" + JASS_DELIM +
							"                endif" + JASS_DELIM +
							"                " + JASS_DELIM +
							"                // Restore the tooltip and append the chunk" + JASS_DELIM +
							"                call BlzSetAbilityTooltip(s__File_AbilityList[i],original[i],lev)" + JASS_DELIM +
							"                " + JASS_DELIM +
							"                set output=output+chunk" + JASS_DELIM +
							"                " + JASS_DELIM +
							"                set i=i+1" + JASS_DELIM +
							"            endloop" + JASS_DELIM +
							"            return output" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_close takes integer this returns nothing" + JASS_DELIM +
							"            if ( s__File_buffer[this] != null ) then" + JASS_DELIM +
							"                call s__File_write(this,s__File_readPreload(this)+s__File_buffer[this])" + JASS_DELIM +
							"                set s__File_buffer[this]=null" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"            set s__File_List[this]=s__File_List[0]" + JASS_DELIM +
							"            set s__File_List[0]=this" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_readEx takes integer this,boolean close returns string" + JASS_DELIM +
							"            local string output= s__File_readPreload(this)" + JASS_DELIM +
							"            local string buf= s__File_buffer[this]" + JASS_DELIM +
							"            if close then" + JASS_DELIM +
							"                call s__File_close(this)" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"            if output==null then" + JASS_DELIM +
							"                return buf" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"            if buf != null then" + JASS_DELIM +
							"                set output=output+buf" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"            return output" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_read takes integer this returns string" + JASS_DELIM +
							"            return s__File_readEx(this,false)" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_readAndClose takes integer this returns string" + JASS_DELIM +
							"            return s__File_readEx(this,true)" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_appendBuffer takes integer this,string contents returns integer" + JASS_DELIM +
							"            set s__File_buffer[this]=s__File_buffer[this]+contents" + JASS_DELIM +
							"            return this" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_readBuffer takes integer this returns string" + JASS_DELIM +
							"            return s__File_buffer[this]" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_writeBuffer takes integer this,string contents returns nothing" + JASS_DELIM +
							"            set s__File_buffer[this]=contents" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"        function s__File_create takes string filename returns integer" + JASS_DELIM +
							"            return s__File_write(s__File_open(filename),\"\")" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"//Implemented from module FileIO___FileInit:" + JASS_DELIM +
							"        function s__File_FileIO___FileInit__onInit takes nothing returns nothing" + JASS_DELIM +
							"            local string originalTooltip" + JASS_DELIM +
							"            // We can't use a single ability with multiple levels because" + JASS_DELIM +
							"            // tooltips return the first level's value if the value hasn't" + JASS_DELIM +
							"            // been set. This way we don't need to edit any object editor data." + JASS_DELIM +
							"            set s__File_AbilityList[0]='Amls'" + JASS_DELIM +
							"            set s__File_AbilityList[1]='Aroc'" + JASS_DELIM +
							"            set s__File_AbilityList[2]='Amic'" + JASS_DELIM +
							"            set s__File_AbilityList[3]='Amil'" + JASS_DELIM +
							"            set s__File_AbilityList[4]='Aclf'" + JASS_DELIM +
							"            set s__File_AbilityList[5]='Acmg'" + JASS_DELIM +
							"            set s__File_AbilityList[6]='Adef'" + JASS_DELIM +
							"            set s__File_AbilityList[7]='Adis'" + JASS_DELIM +
							"            set s__File_AbilityList[8]='Afbt'" + JASS_DELIM +
							"            set s__File_AbilityList[9]='Afbk'" + JASS_DELIM +
							"            set s__File_ReadEnabled=(s__File_readEx((s__File_write(s__File_open(\"FileTester.pld\"),\"FileIO_\")),true))==\"FileIO_\" // INLINED!!" + JASS_DELIM +
							"        endfunction" + JASS_DELIM +
							"    function FileIO_Write takes string filename,string contents returns nothing" + JASS_DELIM +
							"        call s__File_close(s__File_write(s__File_open(filename),contents))" + JASS_DELIM +
							"    endfunction" + JASS_DELIM +
							"    function FileIO_Read takes string filename returns string" + JASS_DELIM +
							"        return s__File_readEx(s__File_open(filename),true)" + JASS_DELIM +
							END_FUNCTION, sb);
					// endregion
					// region custom functions
					append("function " + ARCHON_PREFIX + "WatchSkill takes integer a returns boolean" + JASS_DELIM +
							"    if not(LoadBooleanBJ(StringHash(\"" + GH_HAS_ABILITY + "\"),a," + GENERAL_HASHTABLE + ")) then" + JASS_DELIM +
							"        set " + ABILITY_ARRAY + "[" + ABILITY_ARRAY_SIZE + "]=a" + JASS_DELIM +
							"        call SaveBooleanBJ(true,StringHash(\"" + GH_HAS_ABILITY + "\"),a," + GENERAL_HASHTABLE + ")" + JASS_DELIM +
							"        set " + ABILITY_ARRAY_SIZE + "=" + ABILITY_ARRAY_SIZE + "+1" + JASS_DELIM +
							"        return true" + JASS_DELIM +
							"    else" + JASS_DELIM +
							"        return false" + JASS_DELIM +
							"    " + END_IF + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function Trig_" + ARCHON_PREFIX + "WatchLearnedSkill_Actions takes nothing returns nothing" + JASS_DELIM +
							"    local integer i=0" + JASS_DELIM +
							"    local integer a=GetLearnedSkill()" + JASS_DELIM +
							"    if " + ARCHON_PREFIX + "WatchSkill(a)==true then" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),I2S(i)+\"] \"+I2S(a)+\" _\"+I2S(" + ABILITY_ARRAY_SIZE + " - 1))" + JASS_DELIM) +
							"        loop" + JASS_DELIM +
							"            exitwhen i>=" + ABILITY_ARRAY_SIZE + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),I2S(i)+\") \"+I2S(" + ABILITY_ARRAY + "[i])+\" \"+I2S(" + ABILITY_ARRAY_SIZE + " - 1))" + JASS_DELIM) +
							"            set i = i+1" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"    " + END_IF + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function InitTrig_" + ARCHON_PREFIX + "WatchLearnedSkill takes nothing returns nothing" + JASS_DELIM +
							"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + "WatchLearnedSkill = CreateTrigger()" + JASS_DELIM +
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
							"function " + ARCHON_PREFIX + "ModI takes integer n, integer b returns integer" + JASS_DELIM +
							"return n-(n/b)*b" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "IS62 takes integer n returns string" + JASS_DELIM +
							"    local string result=\"\"" + JASS_DELIM +
							"    local integer remainder" + JASS_DELIM +
							"    local integer b=62" + JASS_DELIM +
							"    local integer digit" + JASS_DELIM +
							"    if n<0 then" + JASS_DELIM +
							"    return \"-\" + " + ARCHON_PREFIX + "IS62(-n)" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen n<=0" + JASS_DELIM +
							"        set digit=" + ARCHON_PREFIX + "ModI(n,b)" + JASS_DELIM +
							"        set result=SubString(\"" + XT87Utils.BASE62_CHARS + "\",digit,digit+1)+result" + JASS_DELIM +
							"        set n=n/b" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							"    " + JASS_DELIM +
							"    return result" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "RS62 takes real r returns string" + JASS_DELIM +
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
							"function " + ARCHON_PREFIX + "BS62 takes boolean b returns string" + JASS_DELIM +
							"local integer i=GetRandomInt(0," + (int) (Math.pow(62, 2) - 2) + ")" + JASS_DELIM +
							"if (" + ARCHON_PREFIX + "ModI(i,2)==0)==b then" + JASS_DELIM +
							"set i=i+1" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"return " + ARCHON_PREFIX + "IS62(i)" + JASS_DELIM +
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
							"    local string digit" + JASS_DELIM +
							"    local integer index" + JASS_DELIM +
							"    if SubString(s,0,1)==\"-\" then" + JASS_DELIM +
							"    return -" + ARCHON_PREFIX + "S62I(SubString(s,1,StringLength(s)))" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen s==\"\"" + JASS_DELIM +
							"        " + JASS_DELIM +
							"        set digit=SubString(s,0,1)" + JASS_DELIM +
							"        set index=0" + JASS_DELIM +
							"        " + JASS_DELIM +
							"        loop" + JASS_DELIM +
							"            exitwhen SubString(\"" + XT87Utils.BASE62_CHARS + "\",index,index+1)==digit" + JASS_DELIM +
							"            set index=index+1" + JASS_DELIM +
							"            if index>=b then" + JASS_DELIM +
							"                return 0" + JASS_DELIM +
							"            endif" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"        " + JASS_DELIM +
							"        set result=result*b+index" + JASS_DELIM +
							"        set s=SubString(s,1,StringLength(s))" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							"    " + JASS_DELIM +
							"    return result" + JASS_DELIM +
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
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "S62B takes string s returns boolean" + JASS_DELIM +
							"return " + ARCHON_PREFIX + "ModI(" + ARCHON_PREFIX + "S62I(s),2)==1" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "S62S takes string s returns string" + JASS_DELIM +
							"if SubString(s,0,1)==\"s\" then" + JASS_DELIM +
							"return SubString(s,1,StringLength(s))" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"return null" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "WriteString takes string cacheName,string title,string v,string fK returns nothing" + JASS_DELIM +
							"local string p=\"XT87CampaignSplitter\\\\\"+cacheName+\"\\\\\"+title+\".pld\"" + JASS_DELIM +
							"    call FileIO_Write(p,v+\"\\n\"+" + ARCHON_PREFIX + "IS62(StringHash(v+\"|\"+p+\"|\"+fK)))" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "SyncContents takes string p,string fK returns nothing" + JASS_DELIM +
							"    local integer i=0" + JASS_DELIM +
							"    local integer j=0" + JASS_DELIM +
							"    local integer len" + JASS_DELIM +
							"    local string content=null" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen not XT87ReadLinesLock" + JASS_DELIM +
							"        call TriggerSleepAction(0.01)" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							"set XT87ReadLinesLock=true" + JASS_DELIM +
							"    if GetLocalPlayer()==" + toMainPlayerFunc() + " then" + JASS_DELIM +
							"        set content=(s__File_readEx(s__File_open((p)),true)) // INLINED!!" + JASS_DELIM +
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
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\">\"+content+\"<\")" + JASS_DELIM) +
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
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"0)\"+XT87ReadLines[0])" + JASS_DELIM) +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"1)\"+XT87ReadLines[1])" + JASS_DELIM) +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"2)\"+XT87ReadLines[2])" + JASS_DELIM) +
							"    return " + ARCHON_PREFIX + "S62I(XT87ReadLines[0])" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function Trig_" + ARCHON_PREFIX + "SyncData_Actions takes nothing returns nothing" + JASS_DELIM +
							"    local string contents=BlzGetTriggerSyncData()" + JASS_DELIM +
							"    local integer len=StringLength(contents)" + JASS_DELIM +
							"    local string char=null" + JASS_DELIM +
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
							"call SaveStr(" + GENERAL_HASHTABLE + ",StringHash(\"" + GH_DIALOG_BUTTON_TEXT + "\"),GetHandleId(b),s)" + JASS_DELIM +
							"return b" + JASS_DELIM +
							END_FUNCTION, sb);
					append("function " + ARCHON_PREFIX + "DialogAddQuitButton takes dialog d,boolean t,string s,integer h returns button" + JASS_DELIM +
							"local button b=DialogAddQuitButton(d,t,s,h)" + JASS_DELIM +
							"call SaveStr(" + GENERAL_HASHTABLE + ",StringHash(\"" + GH_DIALOG_BUTTON_TEXT + "\"),GetHandleId(b),s)" + JASS_DELIM +
							"return b" + JASS_DELIM +
							END_FUNCTION, sb);
					for (Triple<String, String, String> o : Arrays.asList(
							new Triple<>("", "", "0"),
							new Triple<>("WithHotkey", ",integer h", "h")
					))
						append("function " + ARCHON_PREFIX + "DialogAddButton" + o.a + "BJ takes dialog d,string s" + o.b + " returns button" + JASS_DELIM +
								"set bj_lastCreatedButton=DialogAddButton(d,s," + o.c + ")" + JASS_DELIM +
								"call SaveStr(" + GENERAL_HASHTABLE + ",StringHash(\"" + GH_DIALOG_BUTTON_TEXT + "\"),GetHandleId(bj_lastCreatedButton),s)" + JASS_DELIM +
								"return bj_lastCreatedButton" + JASS_DELIM +
								END_FUNCTION, sb);
					// endregion
					// region GetFlushIndex function
					// uses m2 and flush prefixed k2
					append("function " + ARCHON_PREFIX + "GetFlushIndex takes hashtable q,string m,string k returns integer" + JASS_DELIM +
							"    local string s" + JASS_DELIM +
							"    local integer i=0" + JASS_DELIM +
							"    local string cacheName=LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))" + JASS_DELIM +
							"if HaveSavedInteger(q,StringHash(m),StringHash(k)) then" + JASS_DELIM +
							"    return LoadInteger(q,StringHash(m),StringHash(k))" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"set s=" + ARCHON_PREFIX + "ReadString(cacheName,m+\"\\\\\"+k,null)" + JASS_DELIM +
							"if s!=null then" + JASS_DELIM +
							"   set i=" + ARCHON_PREFIX + "S62I(s)" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"call SaveInteger(q,StringHash(m),StringHash(k),i)" + JASS_DELIM +
							"return i" + JASS_DELIM +
							END_FUNCTION, sb);
					// endregion
					// region GameCache functions
					// region game cache basics natives
					append("function " + ARCHON_PREFIX + "InitGameCache takes string campaignFile returns hashtable" + JASS_DELIM +
							"    local hashtable q = InitHashtable()" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"InitGameCache \"+campaignFile)" + JASS_DELIM) +
							"    call SaveStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"),campaignFile)" + JASS_DELIM +
//		todo replace first mission key CACHE_CAMPAIGNFILE_KEY with CACHE_CAMPAIGN_MKEY
//						"    if not " + ARCHON_PREFIX + "HaveStoredInteger(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_FLUSH_KEY + "\")) then" +
//						"    call SaveInteger(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_FLUSH_KEY + "\"),0)" + JASS_DELIM +
//						END_IF + JASS_DELIM +
							"    return q" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function  " + ARCHON_PREFIX + "SaveGameCache takes hashtable q returns boolean" + JASS_DELIM +
							"    local integer i" + JASS_DELIM +
							"    local integer n" + JASS_DELIM +
							"    local integer j" + JASS_DELIM +
							"    local integer c" + JASS_DELIM +
							"    local integer it" + JASS_DELIM +
							"    local integer mh" + JASS_DELIM +
							"    local boolean h" + JASS_DELIM +
							"    local string m2" + JASS_DELIM +
							"    local string k2" + JASS_DELIM +
							"    local string uD" + JASS_DELIM +
							"    local string cacheName=LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"SaveGameCache \"+cacheName)" + JASS_DELIM) +
							// todo simplify using foreach
							// save integers to files
							"    set i=0" + JASS_DELIM +
							"    set n=2*LoadInteger(q,StringHash(\"" + CACHE_INTEGER_KEY + "\"),StringHash(\"" + CACHE_COUNT_KEY + "\"))" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen i>=n" + JASS_DELIM +
							"        set m2=LoadStr(q,StringHash(\"" + CACHE_INTEGER_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        set k2=LoadStr(q,StringHash(\"" + CACHE_INTEGER_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "IS62(LoadInteger(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							// save reals to files
							"    set i=0" + JASS_DELIM +
							"    set n=2*LoadInteger(q,StringHash(\"" + CACHE_REAL_KEY + "\"),StringHash(\"" + CACHE_COUNT_KEY + "\"))" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen i>=n" + JASS_DELIM +
							"        set m2=LoadStr(q,StringHash(\"" + CACHE_REAL_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        set k2=LoadStr(q,StringHash(\"" + CACHE_REAL_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "RS62(LoadReal(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							// save bools to files
							"    set i=0" + JASS_DELIM +
							"    set n=2*LoadInteger(q,StringHash(\"" + CACHE_BOOL_KEY + "\"),StringHash(\"" + CACHE_COUNT_KEY + "\"))" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen i>=n" + JASS_DELIM +
							"        set m2=LoadStr(q,StringHash(\"" + CACHE_BOOL_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        set k2=LoadStr(q,StringHash(\"" + CACHE_BOOL_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "BS62(LoadBoolean(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							// save strings to files
							"    set i=0" + JASS_DELIM +
							"    set n=2*LoadInteger(q,StringHash(\"" + CACHE_STRING_KEY + "\"),StringHash(\"" + CACHE_COUNT_KEY + "\"))" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen i>=n" + JASS_DELIM +
							"        set m2=LoadStr(q,StringHash(\"" + CACHE_STRING_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        set k2=LoadStr(q,StringHash(\"" + CACHE_STRING_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2," + ARCHON_PREFIX + "SS62(LoadStr(q,StringHash(m2),StringHash(k2)))," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							// save units to files
							"    set i=0" + JASS_DELIM +
							"    set n=2*LoadInteger(q,StringHash(\"" + CACHE_UNIT_KEY + "\"),StringHash(\"" + CACHE_COUNT_KEY + "\"))" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen i>=n" + JASS_DELIM +
							"        set m2=LoadStr(q,StringHash(\"" + CACHE_UNIT_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        set k2=LoadStr(q,StringHash(\"" + CACHE_UNIT_KEY + "\"),i)" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"        set mh=StringHash(m2)" + JASS_DELIM +
							"        set uD=" + ARCHON_PREFIX + "IS62(LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ID_SUFIX + "\")))" + JASS_DELIM +
							"        set h=LoadBoolean(q,mh,StringHash(k2+\"" + CACHE_IS_HERO_SUFIX + "\"))" + JASS_DELIM +
							//"        set uD=uD+\"\\n\"+" + CUSTOM_PREFIX + "B2S(h)" + JASS_DELIM +
							"        if h then", sb);
					for (String sufix : Arrays.asList(CACHE_XP_SUFIX, CACHE_STR_SUFIX, CACHE_AGI_SUFIX, CACHE_INT_SUFIX, CACHE_UNSPEND_POINTS_SUFIX, CACHE_HEALTH_MAX_SUFIX, CACHE_MANA_MAX_SUFIX))
						append("            set uD=uD+\"\\n\"+" + ARCHON_PREFIX + "IS62(LoadInteger(q,mh,StringHash(k2+\"" + sufix + "\")))", sb);
					append("            set c=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_COUNT_SUFIX + "\"))" + JASS_DELIM +
							"            set uD=uD+\"\\n\"+" + ARCHON_PREFIX + "IS62(c)" + JASS_DELIM +
							"            set j=0" + JASS_DELIM +
							"            loop" + JASS_DELIM +
							"            exitwhen j>=c", sb);
					for (String sufix : Arrays.asList(CACHE_ABILITY_ID_SUFIX, CACHE_ABILITY_LVL_SUFIX))
						append("            set uD=uD+\"\\n\"+" + ARCHON_PREFIX + "IS62(LoadInteger(q,mh,StringHash(k2+\"" + sufix + "\"+I2S(j))))", sb);
					append("            set j=j+1" + JASS_DELIM +
							"            endloop" + JASS_DELIM +
							"        endif" + JASS_DELIM +
							"        set j=0" + JASS_DELIM +
							"        loop" + JASS_DELIM +
							"        exitwhen j>5" + JASS_DELIM +
							"        set it=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFIX + "\"+I2S(j)))" + JASS_DELIM +
							"        set uD=uD+\"\\n\"+" + ARCHON_PREFIX + "IS62(it)" + JASS_DELIM +
							"        if it!=0 then" + JASS_DELIM +
							"        set uD=uD+\"\\n\"+" + ARCHON_PREFIX + "IS62(LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_CHARGE_SUFIX + "\"+I2S(j))))" + JASS_DELIM +
							"        endif" + JASS_DELIM +
							"        set j=j+1" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"        call " + ARCHON_PREFIX + "WriteString(cacheName,m2+\"\\\\\"+k2,uD," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							"    return false" + JASS_DELIM +
							END_FUNCTION, sb);
					// endregion
					// region Store natives
					append("function " + ARCHON_PREFIX + "StoreReference takes hashtable q,string k,string m,string k2 returns nothing" + JASS_DELIM +
							"    local integer c=LoadInteger(q,StringHash(k),StringHash(\"" + CACHE_COUNT_KEY + "\"))" + JASS_DELIM +
							"    call SaveStr(q,StringHash(k),2*c,m)" + JASS_DELIM +
							"    call SaveStr(q,StringHash(k),2*c+1,k2)" + JASS_DELIM +
							"    call SaveInteger(q,StringHash(k),StringHash(\"" + CACHE_COUNT_KEY + "\"),c+1)" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "StoreInteger takes hashtable q,string m,string k,integer v returns nothing" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_INTEGER_PREFIX + "\"+k" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"StoreInteger \"+LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_INTEGER_KEY + "\",m2,k2)" + JASS_DELIM +
							"    call SaveInteger(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM + "function " + ARCHON_PREFIX + "StoreReal takes hashtable q,string m,string k,real v returns nothing" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_REAL_PREFIX + "\"+k" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"StoreReal \"+LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_REAL_KEY + "\",m2,k2)" + JASS_DELIM +
							"    call SaveReal(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "StoreBoolean takes hashtable q,string m,string k,boolean v returns nothing" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_BOOL_PREFIX + "\"+k" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"StoreBoolean \"+LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))+\"|\"+m+\"|\"+k)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_BOOL_KEY + "\",m2,k2)" + JASS_DELIM +
							"    call SaveBoolean(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "StoreString takes hashtable q,string m,string k,string v returns boolean" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_STRING_PREFIX + "\"+k" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"StoreString \"+LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))+\"|\"+m+\"|\"+k+\" > \"+v)" + JASS_DELIM) +
							"    call " + ARCHON_PREFIX + "StoreReference(q,\"" + CACHE_STRING_KEY + "\",m2,k2)" + JASS_DELIM +
							"    return SaveStr(q,StringHash(m2),StringHash(k2),v)" + JASS_DELIM +
							END_FUNCTION, sb);
					// endregion
					// region StoreUnit native
					append(JASS_DELIM +
							"function " + ARCHON_PREFIX + "StoreUnit takes hashtable q,string m,string k,unit u returns boolean" + JASS_DELIM +
							"    local integer i" + JASS_DELIM +
							"    local item tempItem=null" + JASS_DELIM +
							"    local integer a" + JASS_DELIM +
							"    local integer l" + JASS_DELIM +
							"    local integer c=0" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
							"    local integer mh=StringHash(m2)" + JASS_DELIM +
							"    if (u==null) then" + JASS_DELIM +
							"        return false" + JASS_DELIM +
							"    endif" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"StoreUnit \"+LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))+\"|\"+m+\"|\"+k +\" > \"+GetUnitName(u))" + JASS_DELIM) +
							"    call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ID_SUFIX + "\"),GetUnitTypeId(u))" + JASS_DELIM +
							"    call SaveBoolean(q,mh,StringHash(k2+\"" + CACHE_IS_HERO_SUFIX + "\"),IsUnitType(u,UNIT_TYPE_HERO))" + JASS_DELIM +
							"    if (IsUnitType(u,UNIT_TYPE_HERO)) then" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_XP_SUFIX + "\"),GetHeroXP(u))" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_STR_SUFIX + "\"),GetHeroStr(u,false))" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_AGI_SUFIX + "\"),GetHeroAgi(u,false))" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_INT_SUFIX + "\"),GetHeroInt(u,false))" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_UNSPEND_POINTS_SUFIX + "\"),GetHeroSkillPoints(u))" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"hp\"),BlzGetUnitMaxHP(u))" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"mp\"),BlzGetUnitMaxMana(u))" + JASS_DELIM +
							"        set i=0" + JASS_DELIM +
							"        loop" + JASS_DELIM +
							"            exitwhen i>=" + ABILITY_ARRAY_SIZE + JASS_DELIM +
							"            set a = " + ABILITY_ARRAY + "[i]" + JASS_DELIM +
							"            set l = GetUnitAbilityLevel(u,a)" + JASS_DELIM +
							"            if l>0 then" + JASS_DELIM +
							"               call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_ID_SUFIX + "\"+I2S(c)),a)" + JASS_DELIM +
							"               call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_LVL_SUFIX + "\"+I2S(c)),l)" + JASS_DELIM +
							"               set c = c+1" + JASS_DELIM +
							"            " + END_IF + JASS_DELIM +
							"            set i = i+1" + JASS_DELIM +
							"        endloop" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_COUNT_SUFIX + "\"),c)" + JASS_DELIM +
							"    endif" + JASS_DELIM +
							"    set i=0" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen i>5" + JASS_DELIM +
							"        set tempItem = UnitItemInSlot(u,i)" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFIX + "\"+I2S(i)),GetItemTypeId(tempItem))" + JASS_DELIM +
							"        call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_CHARGE_SUFIX + "\"+I2S(i)),GetItemCharges(tempItem))" + JASS_DELIM +
							"        set i = i+1" + JASS_DELIM +
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
							new Triple<>("Integer", CACHE_INTEGER_PREFIX, ARCHON_PREFIX + "S62I(s)"),
							new Triple<>("Real", CACHE_REAL_PREFIX, ARCHON_PREFIX + "S62R(s)"),
							new Triple<>("Boolean", CACHE_BOOL_PREFIX, ARCHON_PREFIX + "S62B(s)"),
							new Triple<>("String", CACHE_STRING_PREFIX, ARCHON_PREFIX + "S62S(s)")
					))
						append("function " + ARCHON_PREFIX + "HaveStored" + o.a + " takes hashtable q,string m,string k returns boolean" + JASS_DELIM +
								"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
								"    local string k2=\"" + o.b + "\"+k" + JASS_DELIM +
								"    local string s" + JASS_DELIM +
								"    local string cacheName=LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))" + JASS_DELIM +
								"if HaveSaved" + o.a + "(q,StringHash(m2),StringHash(k2)) then" + JASS_DELIM +
								"    return true" + JASS_DELIM +
								END_IF + JASS_DELIM +
								"set s=" + ARCHON_PREFIX + "ReadString(cacheName,m2+\"\\\\\"+k2," + FLUSH_KEY_CODE + ")" + JASS_DELIM +
								"if s==null then" + JASS_DELIM +
								"return false" + JASS_DELIM +
								END_IF + JASS_DELIM +
								"call Save" + (o.a.equals("String") ? "Str" : o.a) + "(q,StringHash(m2),StringHash(k2)," + o.c + ")" + JASS_DELIM +
								"return true" + JASS_DELIM +
								END_FUNCTION, sb);
					// endregion
					// region HaveStoredUnit native
					append("function " + ARCHON_PREFIX + "HaveStoredUnit takes hashtable q,string m,string k returns boolean" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
							"    local string cacheName=LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))" + JASS_DELIM +
							"if LoadInteger(q,StringHash(m2),StringHash(k2+\"" + CACHE_ID_SUFIX + "\"))!=0 then" + JASS_DELIM +
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
						append("    " + o.b + "if t == bj_GAMECACHE_" + o.a.toUpperCase(Locale.ROOT) + " then" + JASS_DELIM +
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
								"if HaveSaved" + o.a + "(q,StringHash(m2),StringHash(k2)) then" + JASS_DELIM +
								// todo add entry to hashtable that tells it to overwrite file upon saving
								"call RemoveSaved" + o.a + "(q,StringHash(m2),StringHash(k2))" + JASS_DELIM +
								END_IF + JASS_DELIM +
								END_FUNCTION, sb);
					append("function " + ARCHON_PREFIX + "FlushStoredUnit takes hashtable q,string m,string k returns nothing" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
							"if " + ARCHON_PREFIX + "HaveStoredUnit(q,m,k) then" + JASS_DELIM +
							// todo add entry to hashtable that tells it to overwrite file upon saving
							"call RemoveSavedInteger(q,StringHash(m2),StringHash(k2+\"" + CACHE_ID_SUFIX + "\"))" + JASS_DELIM +
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
							"    local integer l" + JASS_DELIM +
							"    local integer c" + JASS_DELIM +
							"    local integer id" + JASS_DELIM +
							"    local unit u=null" + JASS_DELIM +
							"    local boolean r=false" + JASS_DELIM +
							"    local boolean d" + JASS_DELIM +
							"    local string m2=\"" + CACHE_MISSION_PREFIX + "\"+m" + JASS_DELIM +
							"    local string k2=\"" + CACHE_UNIT_PREFIX + "\"+k" + JASS_DELIM +
							"    local integer mh=StringHash(m2)" + JASS_DELIM +
							"    local string cacheName=LoadStr(q,StringHash(\"" + CACHE_CAMPAIGN_MKEY + "\"),StringHash(\"" + CACHE_CAMPAIGNFILE_KEY + "\"))" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"RestoreUnit \"+cacheName+\"|\"+m+\"|\"+k+\" > \"+LoadStr(q,StringHash(m2),StringHash(\"" + CACHE_STRING_PREFIX + "\"+k)))" + JASS_DELIM) +
							"    set id=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ID_SUFIX + "\"))" + JASS_DELIM +
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
							"if(IsUnitType(u,UNIT_TYPE_HERO))then" + JASS_DELIM + // if hero
							"if r then", sb); // if read
					int lineIndex = 1;
					for (String sufix : Arrays.asList(CACHE_XP_SUFIX, CACHE_STR_SUFIX, CACHE_AGI_SUFIX, CACHE_INT_SUFIX, CACHE_UNSPEND_POINTS_SUFIX,
							CACHE_HEALTH_MAX_SUFIX, CACHE_MANA_MAX_SUFIX))
						append("call SaveInteger(q,mh,StringHash(k2+\"" + sufix + "\")," + ARCHON_PREFIX + "S62I(XT87ReadLines[" + (lineIndex++) + "]))", sb);
					append("call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_COUNT_SUFIX + "\")," + ARCHON_PREFIX + "S62I(XT87ReadLines[" + (lineIndex++) + "]))", sb);
					append(END_IF + JASS_DELIM + // endif read
							"        call SetHeroXP(u,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_XP_SUFIX + "\")),false)" + JASS_DELIM +
							"        call SetHeroStr(u,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_STR_SUFIX + "\")),true)" + JASS_DELIM +
							"        call SetHeroAgi(u,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_AGI_SUFIX + "\")),true)" + JASS_DELIM +
							"        call SetHeroInt(u,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_INT_SUFIX + "\")),true)" + JASS_DELIM +
							"        call ModifyHeroSkillPoints(u,bj_MODIFYMETHOD_SET,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_UNSPEND_POINTS_SUFIX + "\")))" + JASS_DELIM +
							"        set c=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_COUNT_SUFIX + "\"))" + JASS_DELIM +
							"        set i=0" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"AUX i \"+I2S(i)+\" c \"+I2S(c))" + JASS_DELIM) +
							"        loop" + JASS_DELIM +
							"            exitwhen i>=c" + JASS_DELIM +
							"if r then", sb); // if read
					int lineIndexAfterAbilityCount = lineIndex;
					for (String sufix : Arrays.asList(CACHE_ABILITY_ID_SUFIX, CACHE_ABILITY_LVL_SUFIX))
						append("call SaveInteger(q,mh,StringHash(k2+\"" + sufix + "\"+I2S(i))," + ARCHON_PREFIX + "S62I(XT87ReadLines[" + (lineIndex++) + "+2*i]))", sb);
					append(END_IF + JASS_DELIM + // endif read
							"            set a=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_ID_SUFIX + "\"+I2S(i)))" + JASS_DELIM +
							"            set l=LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ABILITY_LVL_SUFIX + "\"+I2S(i)))" + JASS_DELIM +
							"            call ModifyHeroSkillPoints(u,bj_MODIFYMETHOD_ADD,l)" + JASS_DELIM +
							"            call " + ARCHON_PREFIX + "WatchSkill(a)" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"AUX i \"+I2S(i)+\" l \"+I2S(l))" + JASS_DELIM) +
							"            set j=0" + JASS_DELIM +
							"set d=false" + JASS_DELIM +
							"            loop" + JASS_DELIM +
							"                exitwhen j>=l" + JASS_DELIM +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"AUX i \"+I2S(i)+\" j \"+I2S(j))" + JASS_DELIM) +
							DebugOnly("call DisplayTextToForce(GetPlayersAll(),\"AUX i \"+I2S(i)+\" a \"+I2S(a))" + JASS_DELIM) +
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
							"    set j=" + lineIndexAfterAbilityCount + "+2*c" + JASS_DELIM +
							"else" + JASS_DELIM + //else is unit
							"    set j=1" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"    set i=0" + JASS_DELIM +
							"    loop" + JASS_DELIM +
							"        exitwhen i>5" + JASS_DELIM +
							"if r then" + JASS_DELIM +
							"set c=" + ARCHON_PREFIX + "S62I(XT87ReadLines[j])" + JASS_DELIM +
							"set j=j+1" + JASS_DELIM +
							"call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFIX + "\"+I2S(i)),c)" + JASS_DELIM +
							"if c!=0 then" + JASS_DELIM +
							"call SaveInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_CHARGE_SUFIX + "\"+I2S(i))," + ARCHON_PREFIX + "S62I(XT87ReadLines[j]))" + JASS_DELIM +
							"set j=j+1" + JASS_DELIM +
							END_IF + JASS_DELIM +
							END_IF + JASS_DELIM +
							"        call UnitAddItemToSlotById(u,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_ID_SUFIX + "\"+I2S(i))),i)" + JASS_DELIM +
							"        call SetItemCharges(UnitItemInSlot(u,i),LoadInteger(q,mh,StringHash(k2+\"" + CACHE_ITEM_CHARGE_SUFIX + "\"+I2S(i))))" + JASS_DELIM +
							"        set i=i+1" + JASS_DELIM +
							"    endloop" + JASS_DELIM +
							"        call BlzSetUnitMaxHP(u,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_HEALTH_MAX_SUFIX + "\")))" + JASS_DELIM +
							"        call SetUnitLifePercentBJ(u,100)" + JASS_DELIM +
							"        call BlzSetUnitMaxMana(u,LoadInteger(q,mh,StringHash(k2+\"" + CACHE_MANA_MAX_SUFIX + "\")))" + JASS_DELIM +
							"        call SetUnitManaPercentBJ(u,100)" + JASS_DELIM +
							"if r then" + JASS_DELIM +
							"set XT87ReadLinesAreRead=false" + JASS_DELIM +
							"set XT87ReadLinesLock=false" + JASS_DELIM +
							"set XT87ReadLinesWaiting=XT87ReadLinesWaiting-1" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"    return u" + JASS_DELIM +
							END_FUNCTION, sb);
					// endregion
					// region GetStored functions
					for (String s : Arrays.asList("Integer", "Real", "Boolean"))
						append("function " + ARCHON_PREFIX + "GetStored" + s + "BJ takes string k,string m,hashtable q returns " + s.toLowerCase(Locale.ROOT) + JASS_DELIM +
								"    return " + ARCHON_PREFIX + "GetStored" + s + "(q,m,k)" + JASS_DELIM +
								END_FUNCTION, sb);
					append("function " + ARCHON_PREFIX + "GetStoredStringBJ takes string k,string m,hashtable q returns string" + JASS_DELIM +
							"    local string s = " + ARCHON_PREFIX + "GetStoredString(q,m,k)" + JASS_DELIM +
							"    if s==null then" + JASS_DELIM +
							"        return \"\"" + JASS_DELIM +
							END_IF + JASS_DELIM +
							"    return s" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function " + ARCHON_PREFIX + "RestoreUnitLocFacingAngleBJ takes string k,string m,hashtable q,player p,location l,real f returns unit" + JASS_DELIM +
							"    set bj_lastLoadedUnit = " + ARCHON_PREFIX + "RestoreUnit(q,m,k,p,GetLocationX(l),GetLocationY(l),f)" + JASS_DELIM +
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
				List<String> resourceNames = ALL_RESOURCE_NAMES.subList(0, 2);
				for (int i = 0; i < resourceTypes.size(); i++) {
					String resourceType = resourceTypes.get(i);
					String resourceName = resourceNames.get(i);
					append("// Trigger: " + ARCHON_PREFIX + resourceName + "Sync" + JASS_DELIM +
							"function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions takes nothing returns nothing" + JASS_DELIM +
							"    local integer resourceSyncValue=GetPlayerState(GetTriggerPlayer()," + resourceType + ")" + JASS_DELIM +
							"    " + XT87Utils.DISABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")", sb);
					for (int playerNumber : allPlayerNumbers) {
						String playerFunc = toPlayerFunc(playerNumber);
						append("    if (GetTriggerPlayer()!=" + playerFunc + ") then", sb);
						append("        call SetPlayerState(" + playerFunc + "," + resourceType + ",resourceSyncValue)", sb);
						append("    " + END_IF, sb);
					}
					append("    " + XT87Utils.ENABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function InitTrig_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing" + JASS_DELIM +
							"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()", sb);
					for (int playerNumber : allPlayerNumbers)
						append("    call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toPlayerFunc(playerNumber) + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0.00)", sb);
					append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions)" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM, sb);
				}
				// endregion
				// region food and population cap synchronization
				resourceTypes = Arrays.asList("PLAYER_STATE_RESOURCE_FOOD_USED", "PLAYER_STATE_RESOURCE_FOOD_CAP", "PLAYER_STATE_FOOD_CAP_CEILING");
				resourceNames = ALL_RESOURCE_NAMES.subList(resourceNames.size(), resourceNames.size() + 3);
				for (int i = 0; i < resourceTypes.size(); i++) {
					String resourceType = resourceTypes.get(i);
					String resourceName = resourceNames.get(i);
					append("function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions takes nothing returns nothing" + JASS_DELIM +
							"    local integer resourceSyncValue=GetPlayerState(GetTriggerPlayer()," + resourceType + ")" + JASS_DELIM +
							"    " + XT87Utils.DISABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")", sb);
					for (int playerNumber : allPlayerNumbers) {
						if (playerNumber == mainPlayer.getNum())
							continue;
						String playerFunc = toPlayerFunc(playerNumber);
						append("    call SetPlayerState(" + playerFunc + "," + resourceType + ",resourceSyncValue)", sb);
					}
					append("    " + XT87Utils.ENABLE_TRIGGER + XT87Utils.THIS_TRIGGER + ")" + JASS_DELIM +
							END_FUNCTION + JASS_DELIM +
							"function InitTrig_" + ARCHON_PREFIX + resourceName + "Sync takes nothing returns nothing" + JASS_DELIM +
							"    set " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=CreateTrigger()", sb);
					append("    call TriggerRegisterPlayerStateEvent(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync," + toMainPlayerFunc() + "," + resourceType + ",GREATER_THAN_OR_EQUAL,0.00)", sb);
					append("    call TriggerAddAction(" + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync,function Trig_" + ARCHON_PREFIX + resourceName + "Sync_Actions)" + JASS_DELIM +
							END_FUNCTION, sb);
				}
				// endregion
				// region give units to main player
				append("function Trig_" + ARCHON_PREFIX + "UnitShare_Conditions takes nothing returns boolean", sb);
				for (int playerNumber : allPlayerNumbers) {
					if (playerNumber == mainPlayer.getNum())
						continue;
					append("    if (GetOwningPlayer(GetEnteringUnit())==" + toPlayerFunc(playerNumber) + ") then" + JASS_DELIM +
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
				append("function " + ARCHON_PREFIX + "SelectUnit takes nothing returns nothing" + JASS_DELIM +
						"local integer pid=GetPlayerId(" + AUX_PLAYER + ")" + JASS_DELIM +
						"local integer ct=LoadInteger(" + SELECTION_HASHTABLE + ",pid,0)+1" + JASS_DELIM +
						"local effect fc=AddSpecialEffectTarget(\"" + XT87Utils.PATH_PREFIX + "\\\\" + SELECTION_INDICATOR_PATH + ".mdx\",GetEnumUnit(),\"overhead\")" + JASS_DELIM +
						"call BlzSetSpecialEffectColorByPlayer(fc,Player(pid))" + JASS_DELIM +
						//"call BlzSetSpecialEffectYaw(fc," + PLAYER_ANGLE_ARRAY + "[pid]" + ")" + JASS_DELIM +
						"call SaveEffectHandle(" + SELECTION_HASHTABLE + ",pid,ct,fc)" + JASS_DELIM +
						"call SaveInteger(" + SELECTION_HASHTABLE + ",pid,0,ct)" + JASS_DELIM +
						END_FUNCTION, sb);
				append("function Trig_" + ARCHON_PREFIX + "Select_Actions takes nothing returns nothing" + JASS_DELIM +
						"local integer i=0" + JASS_DELIM +
						"local integer pid=GetPlayerId(GetTriggerPlayer())" + JASS_DELIM +
						"local integer ct" + JASS_DELIM +
						"local boolean stp=LoadBoolean(" + SELECTION_HASHTABLE + ",pid,-1)" + JASS_DELIM +
						"local group g" + JASS_DELIM +
						"if stp then" + JASS_DELIM +
						"return" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"call SaveBoolean(" + SELECTION_HASHTABLE + ",pid,-1,true)" + JASS_DELIM +
						"call TriggerSleepAction(0.01)" + JASS_DELIM +
						"set ct=LoadInteger(" + SELECTION_HASHTABLE + ",pid,0)" + JASS_DELIM +
						"loop" + JASS_DELIM +
						"set i=i+1" + JASS_DELIM +
						"exitwhen i>ct" + JASS_DELIM +
						"call DestroyEffect(LoadEffectHandle(" + SELECTION_HASHTABLE + ",pid,i))" + JASS_DELIM +
						"endloop" + JASS_DELIM +
						"call SaveInteger(" + SELECTION_HASHTABLE + ",pid,0,0)" + JASS_DELIM +
						"set g=CreateGroup()" + JASS_DELIM +
						"set " + AUX_PLAYER + "=GetTriggerPlayer()" + JASS_DELIM +
						"call GroupEnumUnitsSelected(g," + AUX_PLAYER + ",null)" + JASS_DELIM +
						"call ForGroup(g,function " + ARCHON_PREFIX + "SelectUnit)" + JASS_DELIM +
						"call DestroyGroup(g)" + JASS_DELIM +
						"call SaveBoolean(" + SELECTION_HASHTABLE + ",pid,-1,false)" + JASS_DELIM +
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
				append("function " + ARCHON_PREFIX + "GetMainPlayer takes nothing returns player" + JASS_DELIM +
						"    local player p=GetLocalPlayer()" + JASS_DELIM +
						"    if IsPlayerInForce(p," + FORCE_NAME + ") then" + JASS_DELIM +
						"        return " + toMainPlayerFunc() + JASS_DELIM +
						"    endif" + JASS_DELIM +
						"    return p" + JASS_DELIM +
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
				append("function Trig_" + ARCHON_PREFIX + "DialogSync_Actions takes nothing returns nothing" + JASS_DELIM +
								"local player p=GetTriggerPlayer()" + JASS_DELIM +
								"local string s=LoadStr(" + GENERAL_HASHTABLE + ",StringHash(\"" + GH_DIALOG_BUTTON_TEXT + "\"),GetHandleId(GetClickedButton()))"
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
			for (String resourceName : ALL_RESOURCE_NAMES)
				append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + resourceName + "Sync=null", sb);
			append("trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "UnitShare=null" + JASS_DELIM +
							"force " + FORCE_NAME + JASS_DELIM +
							"location " + TEMP_POINT_NAME + JASS_DELIM +
							"integer array " + PLAYER_ID_ARRAY + JASS_DELIM +
							"integer array " + PLAYER_INDEX_ARRAY + JASS_DELIM +
							"player " + AUX_PLAYER + JASS_DELIM +
							"hashtable " + GENERAL_HASHTABLE + "=InitHashtable()" + JASS_DELIM +
							"hashtable " + SELECTION_HASHTABLE + "=InitHashtable()" + JASS_DELIM +
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "Select=null" + JASS_DELIM +
							"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "DialogSync=null"
//							+ JASS_DELIM +
//					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "Deselect=null" + JASS_DELIM +
//					"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "DeselectOnDeath=null"
					, sb);
			if (withCustomGameCache)
				append("hashtable " + LAST_CREATED_COOP_CACHE + "=null" + JASS_DELIM +
						"integer array " + ABILITY_ARRAY + JASS_DELIM +
						"integer " + ABILITY_ARRAY_SIZE + "=0" + JASS_DELIM +
						"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "WatchLearnedSkill" + JASS_DELIM +
						"trigger " + TRIGGER_PREFIX + ARCHON_PREFIX + "SyncData" + JASS_DELIM +
						"constant integer s__File_AbilityCount=10" + JASS_DELIM +
						"constant integer s__File_PreloadLimit=200" + JASS_DELIM +
						"integer s__File_Counter=0" + JASS_DELIM +
						"integer array s__File_List" + JASS_DELIM +
						"integer array s__File_AbilityList" + JASS_DELIM +
						"boolean s__File_ReadEnabled=false" + JASS_DELIM +
						"string array s__File_filename" + JASS_DELIM +
						"string array s__File_buffer" + JASS_DELIM +
						"string array XT87ReadLines" + JASS_DELIM +
						"boolean XT87ReadLinesAreRead=false" + JASS_DELIM +
						"boolean XT87ReadLinesLock=false" + JASS_DELIM +
						"integer XT87ReadLinesWaiting=0", sb);
		} else if (line.trim().equals("endglobals")) {
			if (endGlobalsState == 0)
				endGlobalsState = 1;
		} else if (line.contains("function InitCustomTriggers takes")) {
			for (String resourceName : ALL_RESOURCE_NAMES)
				append("    call InitTrig_" + ARCHON_PREFIX + resourceName + "Sync()", sb);
			for (String triggerName : Arrays.asList("UnitShare", "Select", "DialogSync"))//, "Deselect", "DeselectOnDeath"))
				append("    call InitTrig_" + ARCHON_PREFIX + triggerName + "()", sb);
			if (withCustomGameCache) {
				append("    call InitTrig_" + ARCHON_PREFIX + "SyncData()" + JASS_DELIM +
						"    call InitTrig_" + ARCHON_PREFIX + "WatchLearnedSkill()" + JASS_DELIM +
						"    call ExecuteFunc(\"s__File_FileIO___FileInit__onInit\")", sb);
			}
		} else if (line.contains("SetForceAllianceStateBJ("))
			append("call " + ENFORCE_ARCHON + "()", sb);

		if (line.contains(DEFINE_START_LOC + mainPlayer.getNum() + ","))
			for (W3I.Player player : secondaryPlayers)
				append(line.replace(DEFINE_START_LOC + mainPlayer.getNum(), DEFINE_START_LOC + player.getNum()), sb);
	}
}
