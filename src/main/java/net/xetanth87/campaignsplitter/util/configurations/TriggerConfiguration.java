package net.xetanth87.campaignsplitter.util.configurations;

import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_ACTION_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_INIT_PREFIX;
import static net.xetanth87.campaignsplitter.util.XT87Utils.TRIGGER_VAR_REPLACE;

public class TriggerConfiguration implements IFunctionConfiguration {
	private final String triggerName;
	private final String triggerActionCode;
	private final String triggerLocalCode;
	private final String triggerInitCode;

	public TriggerConfiguration(String triggerName, String triggerActionCode, String triggerLocalCode, String triggerInitCode) {
		this.triggerName = triggerName;
		var triggerVarName = getVarName();
		this.triggerActionCode = triggerActionCode.replace(TRIGGER_VAR_REPLACE, triggerVarName);
		this.triggerLocalCode = triggerLocalCode != null ? triggerLocalCode.replace(TRIGGER_VAR_REPLACE, triggerVarName) : null;
		this.triggerInitCode = triggerInitCode.replace(TRIGGER_VAR_REPLACE, triggerVarName);
	}

	public String getVarName()
	{
		return TRIGGER_PREFIX + triggerName;
	}

	public String getActionName()
	{
		return TRIGGER_ACTION_PREFIX + triggerName;
	}

	public String getInitName()
	{
		return TRIGGER_INIT_PREFIX + triggerName;
	}

	@Override
	public String getDeclaration() {
		return "trigger " + getVarName();
	}

	@Override
	public String getInitCall() {
		return "call " + getInitName() + "()";
	}

	@Override
	public String getDefinitionCode() {
		var triggerVarName = getVarName();
		var actionFunction = getActionName();
		return "function " + actionFunction + " takes nothing returns nothing" + JASS_DELIM +
				triggerActionCode + JASS_DELIM +
				END_FUNCTION + JASS_DELIM +
				"function " + getInitName() + " takes nothing returns nothing" + JASS_DELIM +
				(triggerLocalCode != null ? (triggerLocalCode + JASS_DELIM) : "") +
				"set " + triggerVarName + "=CreateTrigger()" + JASS_DELIM +
				triggerInitCode + JASS_DELIM +
				"call TriggerAddAction(" + triggerVarName + ",function " + actionFunction + ")" + JASS_DELIM +
				END_FUNCTION;
	}
}
