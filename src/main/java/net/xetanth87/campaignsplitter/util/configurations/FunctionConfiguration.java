package net.xetanth87.campaignsplitter.util.configurations;

import static net.xetanth87.campaignsplitter.util.BlockUtils.bDef;
import static net.xetanth87.campaignsplitter.util.BlockUtils.bFunc;
import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;
import static net.xetanth87.campaignsplitter.util.XT87Utils.forceDefaultValue;

public class FunctionConfiguration implements IFunctionConfiguration {
	private final String functionName;;
	private final String functionParams;
	private final String functionReturn;
	private final String functionCode;
	private static final String defaultValue = "nothing";

	public FunctionConfiguration(String functionName, String functionParams, String functionReturn, String... functionCode) {
		this.functionName = functionName;
		this.functionParams = functionParams;
		this.functionReturn = functionReturn;
		this.functionCode = String.join(JASS_DELIM, functionCode);
	}

	@Override
	public String getDeclaration() {
		return null;
	}

	@Override
	public String getInitCall() {
		return null;
	}

	private String getUsedValue(String value) {
		return forceDefaultValue(value, defaultValue);
	}

	@Override
	public String getDefinitionCode() {
		return bDef(functionName, functionParams, functionReturn, functionCode);
	}
}
