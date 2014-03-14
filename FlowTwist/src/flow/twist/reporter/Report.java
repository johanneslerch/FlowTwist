package flow.twist.reporter;

import soot.Unit;
import flow.twist.config.AnalysisContext;
import flow.twist.trackable.Trackable;

public class Report {

	public final AnalysisContext context;
	public final Trackable trackable;
	public final Unit targetUnit;

	public Report(AnalysisContext context, Trackable trackable, Unit unit) {
		this.context = context;
		this.trackable = trackable;
		this.targetUnit = unit;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + ((trackable == null) ? 0 : System.identityHashCode(trackable));
		result = prime * result + ((targetUnit == null) ? 0 : targetUnit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Report other = (Report) obj;
		if (context == null) {
			if (other.context != null)
				return false;
		} else if (!context.equals(other.context))
			return false;
		if (trackable != other.trackable)
			return false;
		if (targetUnit == null) {
			if (other.targetUnit != null)
				return false;
		} else if (!targetUnit.equals(other.targetUnit))
			return false;
		return true;
	}

}