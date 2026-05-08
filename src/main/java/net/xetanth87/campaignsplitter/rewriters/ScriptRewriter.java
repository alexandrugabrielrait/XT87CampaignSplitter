package net.xetanth87.campaignsplitter.rewriters;

import net.moonlightflower.wc3libs.txt.app.jass.Jass;
import net.xetanth87.campaignsplitter.MapInjector;
import net.xetanth87.campaignsplitter.util.configurations.FunctionConfiguration;
import net.xetanth87.campaignsplitter.util.configurations.TriggerConfiguration;
import net.xetanth87.campaignsplitter.util.XT87Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static net.xetanth87.campaignsplitter.util.XT87Utils.CALL_DECLARATION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.FUNCTION_DECLARATION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;
import static net.xetanth87.campaignsplitter.util.XT87Utils.LOCAL_DECLARATION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.NATIVE_DECLARATION;

public abstract class ScriptRewriter {
	protected static final String PLAYER_FUNC = "Player(";
	public static final String TRIGGER_EXECUTE = "call ConditionalTriggerExecute(";

	private static final boolean IS_DEBUG = false;
	private static final boolean REMOVE_USELESS = true;
	public MapInjector mi;
	public boolean first = true, insideGlobals, insideMain, insideConfig, isReading;
	public String insideFunction;
	public String rewriterPrefix;
	private final List<FunctionConfiguration> functionConfig = new ArrayList<>();
	private final List<TriggerConfiguration> triggerConfig = new ArrayList<>();

	public ScriptRewriter(MapInjector mi, String rewriterPrefix) {
		this.mi = mi;
		this.rewriterPrefix = "XT87" + rewriterPrefix;
	}

	public void addFunctionConfiguration(String functionName, String functionParams, String functionReturn, String... functionCode) {
		functionConfig.add(new FunctionConfiguration(rewriterPrefix + functionName, functionParams, functionReturn, functionCode));
	}

	public void addTriggerConfiguration(String triggerName, String triggerActionCode, String triggerInitCode) {
		triggerConfig.add(new TriggerConfiguration(rewriterPrefix + triggerName, triggerActionCode, null, triggerInitCode));
	}

	public void addTriggerConfiguration(String triggerName, String triggerActionCode, String triggerLocalCode, String triggerInitCode) {
		triggerConfig.add(new TriggerConfiguration(rewriterPrefix + triggerName, triggerActionCode, triggerLocalCode, triggerInitCode));
	}

	public static String toPlayerFunc(int i) {
		return PLAYER_FUNC + i + ")";
	}

	public static String DebugOnly(String text) {
		if (IS_DEBUG)
			return text;
		return "";
	}

	public String initializationTriggerFromCall(String line) {
		String s = line.trim().replace(TRIGGER_EXECUTE, "");
		s = s.substring(0, s.indexOf(')')).trim();
		return s;
	}

	public static boolean isInitFunction(String line) {
		return line.contains("function RunInitializationTriggers takes") || line.contains("function main takes");
	}

	public void stopReading() {
		isReading = false;
	}

	public void onStartReadAux(String text, StringBuffer sb) {
		insideGlobals = false;
		isReading = true;
		onStartRead(text, sb);
	}

	public abstract void onStartRead(String text, StringBuffer sb);

	public abstract void onReadLine(String line, StringBuffer sb);

	protected void onEnterGlobals(StringBuffer sb) {
	}

	protected void onExitGlobals(StringBuffer sb) {
	}

	protected void onFunctionStart(StringBuffer sb) {
	}

	protected void onFunctionLocals(StringBuffer sb) {
	}

	protected void onFunctionEnd(StringBuffer sb) {
	}

	protected void onMainStart(StringBuffer sb) {
	}

	protected void onMainLocals(StringBuffer sb) {
	}

	protected void onMainEnd(StringBuffer sb) {
	}

	protected void onConfigStart(StringBuffer sb) {
	}

	protected void onConfigLocals(StringBuffer sb) {
	}

	protected void onConfigEnd(StringBuffer sb) {
	}

	public void append(StringBuffer sb, String... blocks) {
		if (blocks == null)
			return;
		for (String block : blocks) {
			for (String line : block.split(JASS_DELIM)) {
				if (REMOVE_USELESS) {
					line = line.trim();
					if (line.isEmpty() || line.startsWith("//"))
						continue;
				}
				if (first)
					first = false;
				else
					sb.append(JASS_DELIM);
				sb.append(line);
			}
		}
	}

	public static void readScript(MapInjector mi) throws Exception {
		String tempPath, scriptsPath = "scripts\\", luaName = "war3map.lua";
		mi.scriptPath = null;
		if (mi.mapEditor.hasFile(tempPath = Jass.GAME_PATH.getName()))
			mi.scriptPath = tempPath;
		else if (mi.mapEditor.hasFile(tempPath = scriptsPath + Jass.GAME_PATH.getName()))
			mi.scriptPath = tempPath;
		else if (mi.mapEditor.hasFile(luaName) || mi.mapEditor.hasFile(scriptsPath + luaName))
			throw new Exception("Script modifications can't be made to the map with lua script \"" + mi.mapFile.getName() + "\".");
		else
			throw new Exception("No JASS script found for map \"" + mi.mapFile.getName() + "\".");
		mi.mapEditor.extractFile(mi.scriptPath, mi.tempFile);
		Scanner sc = new Scanner(mi.tempFile);
		mi.scriptBuffer = new StringBuffer();
		boolean first = true;
		while (sc.hasNextLine()) {
			if (first)
				first = false;
			else
				mi.scriptBuffer.append(JASS_DELIM);
			mi.scriptBuffer.append(sc.nextLine());
		}
		sc.close();
	}

	public static void insertScript(MapInjector mi) throws IOException {
		FileWriter writer = new FileWriter(mi.tempFile);
		writer.append(mi.scriptBuffer.toString());
		writer.close();
		XT87Utils.overwriteFile(mi.mapEditor, mi.scriptPath, mi.tempFile, true);
	}

	public void handleScript(boolean modify) {
		String text = mi.scriptBuffer.toString();
		Scanner sc = new Scanner(text);
		StringBuffer sb = new StringBuffer();
		onStartReadAux(text, sb);
		first = true;
		boolean doGlobals = false;
		boolean afterLocals = false;
		int doExitGlobals = 0;
		while (isReading && sc.hasNextLine()) {
			String line = sc.nextLine();
			line = line.trim();
			if (doGlobals) {
				for (var configuration : triggerConfig) {
					append(sb, configuration.getDeclaration());
				}
				onEnterGlobals(sb);
				doGlobals = false;
			} else if (line.equals("globals"))
				doGlobals = true;

			if (line.startsWith(FUNCTION_DECLARATION)) {
				insideFunction = line.substring(FUNCTION_DECLARATION.length()).trim();
				insideFunction = insideFunction.substring(0, insideFunction.indexOf(' '));
				afterLocals = false;
				insideMain = "main".equals(insideFunction);
				insideConfig = "config".equals(insideFunction);
				if (insideMain)
					onMainStart(sb);
				else if (insideConfig)
					onConfigStart(sb);
				else
					onFunctionStart(sb);
			}
			else if (insideFunction != null) {
				if (!afterLocals && !line.startsWith(LOCAL_DECLARATION))
				{
					afterLocals = true;
					if (insideMain) {
						onMainLocals(sb);
						for (var configuration : triggerConfig)
							append(sb, configuration.getInitCall());
					}
					else if (insideConfig)
						onFunctionLocals(sb);
					else
						onFunctionLocals(sb);
				}
				if (line.equals(END_FUNCTION))
				{
					if (insideMain)
						onMainEnd(sb);
					else if (insideConfig)
						onConfigEnd(sb);
					else
						onFunctionEnd(sb);
				}
			}

			if (doExitGlobals == 1 && !line.startsWith(NATIVE_DECLARATION)) {
				for (var configuration : functionConfig) {
					append(sb, configuration.getDefinitionCode());
				}
				onExitGlobals(sb);
				for (var configuration : triggerConfig) {
					append(sb, configuration.getDefinitionCode());
				}
				doExitGlobals = 2;
			}
			if (line.equals("endglobals"))
				insideGlobals = false;
			onReadLine(line, sb);
			if (line.equals("globals"))
				insideGlobals = true;
			else if (line.equals(END_FUNCTION)) {
				insideFunction = null;
				insideMain = false;
				insideConfig = false;
			}
			else if (doExitGlobals == 0 && line.equals("endglobals"))
				doExitGlobals = 1;
		}
		if (isReading && modify)
			mi.scriptBuffer = sb;
		isReading = false;
	}

	public void scanScript() {
		handleScript(false);
	}

	public void modifyScript() {
		handleScript(true);
	}

	public static String[] splitWithParentheses(String input) {
		List<String> result = new ArrayList<>();
		StringBuilder currentElement = new StringBuilder();
		int openParentheses = 0;
		for (char c : input.toCharArray()) {
			if (c == ',' && openParentheses == 0) {
				result.add(currentElement.toString().trim());
				currentElement.setLength(0);
			} else {
				currentElement.append(c);
				if (c == '(') {
					openParentheses++;
				} else if (c == ')') {
					openParentheses--;
				}
			}
		}

		if (currentElement.length() > 0) {
			result.add(currentElement.toString().trim());
		}

		return result.toArray(new String[0]);
	}

	private static String[] getParamsAux(String s) {
		try {
			String[] params = splitWithParentheses(s);
			for (int i = 0; i < params.length; i++)
				params[i] = params[i].trim();
			return params;
		} catch (Exception e) {
			return null;
		}
	}

	public static String[] getParamsFromLine(String line, String call) {
		try {
			String s = line.trim().replace(call, "");
			return getParamsAux(s.substring(0, s.lastIndexOf(')')));
		} catch (Exception e) {
			return null;
		}
	}

	public static String[] getParamsFromLine(String line) {
		try {
			return getParamsAux(line.substring(line.indexOf('(') + 1, line.lastIndexOf(')')));
		} catch (Exception e) {
			return null;
		}
	}

	public static String getCallFromLine(String line) {
		try {
			return line.substring(line.indexOf(CALL_DECLARATION), line.indexOf("(") + 1);
		} catch (Exception e) {
			return null;
		}
	}

	public static String getTrimmedCallFromLine(String line) {
		try {
			String newLine = getCallFromLine(line).substring(CALL_DECLARATION.length());
			return newLine.substring(0, newLine.length() - 1).trim();
		} catch (Exception e) {
			return null;
		}
	}

	public static String toJassCall(String call, String[] params) {
		StringBuilder paramText = null;
		for (String param : params) {
			if (paramText == null)
				paramText = new StringBuilder(param);
			else
				paramText.append(",").append(param);
		}
		if (paramText == null)
			paramText = new StringBuilder();
		return call + "(" + paramText + ")";
	}
}
