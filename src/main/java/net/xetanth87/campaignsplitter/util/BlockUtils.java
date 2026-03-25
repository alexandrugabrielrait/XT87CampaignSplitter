package net.xetanth87.campaignsplitter.util;

import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.END_IF;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;
import static net.xetanth87.campaignsplitter.util.XT87Utils.forceDefaultValue;

public class BlockUtils {
	private static final String nothing = "nothing";

	private static boolean isEmpty(String s)
	{
		return s == null || s.isEmpty();
	}

	public static String bFunc(String functionName, String functionParams) {
		return functionName + "(" + forceDefaultValue(functionParams, "") + ")";
	}

	public static String bFunc(String functionName) {
		return bFunc(functionName, null);
	}

	public static String bDef(String functionName, String functionParams, String functionReturn, String functionCode) {
		return "function " + functionName + " takes " + forceDefaultValue(functionParams, nothing)
				+ " returns " + forceDefaultValue(functionReturn, nothing) + JASS_DELIM
				+ functionCode + JASS_DELIM
				+ END_FUNCTION;
	}

	public static String bCall(String functionName, String funtionParams) {
		return "call " + bFunc(functionName, funtionParams);
	}

	public static String bCall(String functionName) {
		return bCall(functionName, null);
	}

	public static String bSet(String varName, String value) {
		return "set " + varName + "=" + value;
	}

	public static String bLocal(String varName, String value) {
		return "local " + varName + (isEmpty(value) ? "" : ("=" + value));
	}

	public static String bLocal(String varName) {
		return bLocal(varName, null);
	}

	public static String bIf(String condition, String thenCode, String elseCode) {
		String elseSection = isEmpty(elseCode) ? "" : ("else" + JASS_DELIM + elseCode + JASS_DELIM);
		return "if " + condition + " then" + JASS_DELIM
				+ thenCode + JASS_DELIM
				+ elseSection
				+ END_IF;
	}

	public static String bIf(String condition, String thenCode) {
		return bIf(condition, thenCode, null);
	}

	public static String bNot(String condition) {
		return "not " + condition;
	}
}
