package net.xetanth87.campaignsplitter.rewriters;

import net.moonlightflower.wc3libs.txt.app.jass.Jass;
import net.xetanth87.campaignsplitter.MapInjector;
import net.xetanth87.campaignsplitter.util.XT87Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public abstract class ScriptRewriter {
	public static final String JASS_DELIM = "\r\n";
	public static final String LOCAL_DECLARATION = "local ";
	public static final String FUNCTION_DECLARATION = "function ";
	public static final String CALL_DECLARATION = "call ";
	public static final String END_FUNCTION = "endfunction";
	public static final String END_IF = "endif";
	public static final String INIT_CALL = "call ConditionalTriggerExecute(";
	private static final boolean IS_DEBUG = false;
	private static final boolean REMOVE_USELESS = true;
	public MapInjector mi;
	public boolean first = true;
	public String insideFunction;

	public ScriptRewriter(MapInjector mi) {
		this.mi = mi;
	}

	public static String DebugOnly(String text) {
		if (IS_DEBUG)
			return text;
		return "";
	}

	public String initializationTriggerFromCall(String line) {
		String s = line.trim().replace(INIT_CALL, "");
		s = s.substring(0, s.indexOf(')')).trim();
		return s;
	}

	public static boolean isInitFunction(String line) {
		return line.contains("function RunInitializationTriggers takes") || line.contains("function main takes");
	}

	public abstract void onStartRead(String text, StringBuffer sb);

	public abstract void onReadLine(String line, StringBuffer sb);

	public void onEnterGlobals(StringBuffer sb) {
	}

	public void onExitGlobals(StringBuffer sb) {
	}

	public void onMainAfterLocals(StringBuffer sb) {
	}

	public void append(String block, StringBuffer sb) {
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

	public static void readScript(MapInjector mi) throws Exception {
		String tempPath, scriptsPath = "scripts\\", luaName = "war3map.lua";
		mi.scriptPath = null;
		if (mi.mapEditor.hasFile(tempPath = Jass.GAME_PATH.getName()))
			mi.scriptPath = tempPath;
		else if (mi.mapEditor.hasFile(tempPath = scriptsPath + Jass.GAME_PATH.getName()))
			mi.scriptPath = tempPath;
		else if (mi.mapEditor.hasFile(luaName) || mi.mapEditor.hasFile(scriptsPath + luaName))
			throw new Exception("Difficulty selection won't be added to the map with lua script \"" + mi.mapFile.getName() + "\".");
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

	public void modifyScript() {
		String text = mi.scriptBuffer.toString();
		Scanner sc = new Scanner(text);
		StringBuffer sb = new StringBuffer();
		onStartRead(text, sb);
		first = true;
		boolean inGlobals = false;
		boolean inMain = false;
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			line = line.trim();
			if (inGlobals) {
				onEnterGlobals(sb);
				inGlobals = false;
			}
			else if (line.equals("globals"))
				inGlobals = true;

			if (line.startsWith(FUNCTION_DECLARATION)) {
				insideFunction = line.substring(FUNCTION_DECLARATION.length()).trim();
				insideFunction = insideFunction.substring(0, insideFunction.indexOf(' '));
				if ("main".equals(insideFunction))
					inMain = true;
			} else if (inMain && !line.startsWith(LOCAL_DECLARATION)) {
				onMainAfterLocals(sb);
				inMain = false;
			}
			onReadLine(line, sb);
			if (line.equals(END_FUNCTION))
				insideFunction = null;
			else if (line.equals("endglobals"))
				onExitGlobals(sb);
		}

		mi.scriptBuffer = sb;
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
}
