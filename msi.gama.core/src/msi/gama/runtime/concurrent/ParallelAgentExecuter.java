package msi.gama.runtime.concurrent;

import java.util.Spliterator;

import msi.gama.metamodel.agent.IAgent;
import msi.gama.runtime.IScope;
import msi.gama.runtime.IScope.MutableResult;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.statements.IExecutable;

public class ParallelAgentExecuter extends ParallelAgentRunner<Object> {

	final IExecutable executable;

	public ParallelAgentExecuter(final IScope scope, final IExecutable executable, final Spliterator<IAgent> agents) {
		super(scope, agents);
		this.executable = executable;
	}

	@Override
	public Object executeOn(final IScope scope) throws GamaRuntimeException {
		final MutableResult result = new MutableResult();
		agents.forEachRemaining(each -> {
			if (result.passed())
				result.accept(scope.execute(executable, each, null));
		});
		return result.passed() ? result.getValue() : null;
	}

	@Override
	ParallelAgentExecuter subTask(final Spliterator<IAgent> sub) {
		return new ParallelAgentExecuter(originalScope, executable, sub);
	}

}