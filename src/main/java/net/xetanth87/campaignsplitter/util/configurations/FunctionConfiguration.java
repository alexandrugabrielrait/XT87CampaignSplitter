package net.xetanth87.campaignsplitter.util.configurations;

import static net.xetanth87.campaignsplitter.util.XT87Utils.END_FUNCTION;
import static net.xetanth87.campaignsplitter.util.XT87Utils.JASS_DELIM;

public class FunctionConfiguration implements IFunctionConfiguration {
	private final String functionName;;
	private final String functionParams;
	private final String functionReturn;
	private final String functionCode;

	public FunctionConfiguration(String functionName, String functionParams, String functionReturn, String functionCode) {
		this.functionName = functionName;
		this.functionParams = functionParams;
		this.functionReturn = functionReturn;
		this.functionCode = functionCode;
	}

	@Override
	public String getDeclaration() {
		return null;
	}

	@Override
	public String getInitCall() {
		return null;
	}

	@Override
	public String getDefinitionCode() {
		return "function " + functionName + " takes " + functionParams + " returns " + functionReturn + JASS_DELIM +
				functionCode + JASS_DELIM +
				END_FUNCTION;
	}
}
