package flow.twist.ifds;

import flow.twist.config.AnalysisContext;

public interface PropagatorProvider {

	Propagator[][] provide(AnalysisContext context);

}
