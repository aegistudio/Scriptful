package net.aegistudio.scriptful.execution;

import javax.script.Invocable;

public class FunctionExecutor implements Executor<FunctionExecutor> {
	public final Invocable script;
	public final String function;
	
	public FunctionExecutor(Invocable script, String function) {
		this.script = script;
		this.function = function;
	}
	
	public void execute(Object... arguments) throws Exception {
		script.invokeFunction(function, arguments);
	}

	@Override
	public int compareTo(FunctionExecutor another) {
		int compareFirst = script.hashCode() - another.script.hashCode();
		if(compareFirst != 0) return compareFirst;
		
		return function.compareTo(another.function);
	}
}
