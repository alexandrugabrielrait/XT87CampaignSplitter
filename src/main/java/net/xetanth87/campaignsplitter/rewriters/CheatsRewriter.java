package net.xetanth87.campaignsplitter.rewriters;

import net.moonlightflower.wc3libs.bin.app.W3I;
import net.xetanth87.campaignsplitter.MapInjector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.END_IF;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_VAR_REPLACE;

public class CheatsRewriter extends ScriptRewriter {
	public W3I.Player mainPlayer;
	protected String CheatModeVar;
	protected String PauseVar;
	protected String FogVar;

	public CheatsRewriter(MapInjector mi, W3I.Player mainPlayer) {
		super(mi, "CH");
		this.mainPlayer = mainPlayer;
		CheatModeVar = rewriterPrefix + "C";
		PauseVar = rewriterPrefix + "P";
		FogVar = rewriterPrefix + "F";

		var checkCheatMode = "if not " + CheatModeVar + " then" + JASS_DELIM + "return" + JASS_DELIM + "endif";

		addFunctionConfiguration("M", "string s", "nothing",
				"call DisplayTextToPlayer(GetLocalPlayer(),0,0,s)"); // message
		addFunctionConfiguration("E", "boolean b", "nothing",
				"if b then " + JASS_DELIM +
						"call " + rewriterPrefix + "M(\"XT87CS Cheat enabled!\")" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call " + rewriterPrefix + "M(\"XT87CS Cheat disabled!\")" + JASS_DELIM +
						"endif"); // enable message
		addTriggerConfiguration("T", // toggle cheats enabled
				"set " + CheatModeVar + "=not " + CheatModeVar + JASS_DELIM +
						"if " + CheatModeVar + " then " + JASS_DELIM +
						"call " + rewriterPrefix + "M(\"XT87CS Cheats ON!\")" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call " + rewriterPrefix + "M(\"XT87CS Cheats OFF!\")" + JASS_DELIM +
						"endif",
				"call TriggerRegisterPlayerChatEvent(" + TRIGGER_VAR_REPLACE + "," + toMainPlayerFunc() + ",\"xetanthcheats\",true)");
		addTriggerConfiguration("P", // pause
				checkCheatMode + JASS_DELIM +
						"set " + PauseVar + "=not " + PauseVar + JASS_DELIM +
						"call " + rewriterPrefix + "E(" + PauseVar + ")" + JASS_DELIM +
						"call PauseGame(" + PauseVar + ")",
				"call TriggerRegisterPlayerChatEvent(" + TRIGGER_VAR_REPLACE + "," + toMainPlayerFunc() + ",\"-xp\",true)");
		addTriggerConfiguration("I", // iseedeadpeople
				checkCheatMode + JASS_DELIM +
						"call " + rewriterPrefix + "E(" + FogVar + ")" + JASS_DELIM +
						"set " + FogVar + "=not " + FogVar + JASS_DELIM +
						"call FogMaskEnable(" + FogVar + ")" + JASS_DELIM +
						"call FogEnable(" + FogVar + ")",
				"call TriggerRegisterPlayerChatEvent(" + TRIGGER_VAR_REPLACE + "," + toMainPlayerFunc() + ",\"-xi\",true)");
		addTriggerConfiguration("WE", // whosyourdaddy effect
				"local real d=GetEventDamage()" + JASS_DELIM +
						"local player p=GetOwningPlayer(GetEventDamageSource())" + JASS_DELIM +
						"local player t=GetOwningPlayer(GetTriggerUnit())" + JASS_DELIM +
						"local player a=" + toMainPlayerFunc() + JASS_DELIM +
						"if d<=0 or p==t then" + JASS_DELIM +
						"return" + JASS_DELIM +
						"elseif p == a then" + JASS_DELIM +
						"call BlzSetEventDamage(d * 1000)" + JASS_DELIM +
						"elseif t == a then" + JASS_DELIM +
						"call BlzSetEventDamage(0)" + JASS_DELIM +
						"endif",
				"call DisableTrigger(" + TRIGGER_VAR_REPLACE + ")" + JASS_DELIM +
						"call TriggerRegisterAnyUnitEventBJ(" + TRIGGER_VAR_REPLACE + ",EVENT_PLAYER_UNIT_DAMAGED)");
		addTriggerConfiguration("W", // whosyourdaddy
				"local boolean b=not IsTriggerEnabled(" + TRIGGER_PREFIX + rewriterPrefix + "WE)" + JASS_DELIM +
						checkCheatMode + JASS_DELIM +
						"call " + rewriterPrefix + "E(" + FogVar + ")" + JASS_DELIM +
						"if b then" + JASS_DELIM +
						"call EnableTrigger(" + TRIGGER_PREFIX + rewriterPrefix + "WE)" + JASS_DELIM +
						"else" + JASS_DELIM +
						"call DisableTrigger(" + TRIGGER_PREFIX + rewriterPrefix + "WE)" + JASS_DELIM +
						"endif",
				"call TriggerRegisterPlayerChatEvent(" + TRIGGER_VAR_REPLACE + "," + toMainPlayerFunc() + ",\"-xw\",true)");
		addTriggerConfiguration("GG", // greed gold
				checkCheatMode + JASS_DELIM +
						"call AdjustPlayerStateSimpleBJ(" + toMainPlayerFunc() + ",PLAYER_STATE_RESOURCE_GOLD,1000)",
				"call TriggerRegisterPlayerChatEvent(" + TRIGGER_VAR_REPLACE + "," + toMainPlayerFunc() + ",\"-xg\",true)");
		addTriggerConfiguration("GL", // greed lumber
				checkCheatMode + JASS_DELIM +
						"call AdjustPlayerStateSimpleBJ(" + toMainPlayerFunc() + ",PLAYER_STATE_RESOURCE_LUMBER,1000)",
				"call TriggerRegisterPlayerChatEvent(" + TRIGGER_VAR_REPLACE + "," + toMainPlayerFunc() + ",\"-xl\",true)");
		addTriggerConfiguration("B", // blink selected units to camera
				"local real x" + JASS_DELIM +
						"local real y" + JASS_DELIM +
						"local group g=CreateGroup()" + JASS_DELIM +
						"local unit u" + JASS_DELIM +
						"local player p=GetTriggerPlayer()" + JASS_DELIM +
						"if GetLocalPlayer()==p then" + JASS_DELIM +
						"set x=GetCameraTargetPositionX()" + JASS_DELIM +
						"set y=GetCameraTargetPositionY()" + JASS_DELIM +
						"endif" + JASS_DELIM +
						"set x=x+0.00" + JASS_DELIM +
						"set y=y+0.00" + JASS_DELIM +
						"call GroupEnumUnitsOfPlayer(g,p,null)" + JASS_DELIM +
						"loop" + JASS_DELIM +
						"set u=FirstOfGroup(g)" + JASS_DELIM +
						"exitwhen u==null" + JASS_DELIM +
						"call GroupRemoveUnit(g,u)" + JASS_DELIM +
						"if IsUnitSelected(u,p) then" + JASS_DELIM +
						"call SetUnitPosition(u,x,y)" + JASS_DELIM +
						END_IF + JASS_DELIM +
						"endloop" + JASS_DELIM +
						"call DestroyGroup(g)",
				"call TriggerRegisterPlayerChatEvent(" + TRIGGER_VAR_REPLACE + "," + toMainPlayerFunc() + ",\"-xb\",true)"
		);
	}

	public String toMainPlayerFunc() {
		return toPlayerFunc(mainPlayer.getNum());
	}

	@Override
	public void onStartRead(String text, StringBuffer sb) {
	}

	@Override
	public void onEnterGlobals(StringBuffer sb) {
		append(sb, "boolean " + CheatModeVar + "=false",
				"boolean " + PauseVar + "=false",
				"boolean " + FogVar + "=true"); // cheat mode
	}

	@Override
	public void onReadLine(String line, StringBuffer sb) {
		append(sb, line);
	}
}
