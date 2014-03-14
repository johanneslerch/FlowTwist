package flow.twist.transformer;

public abstract class ResultTransformer<From, To> {

	private ResultTransformer<To, ?> successor;

	public ResultTransformer(ResultTransformer<To, ?> successor) {
		this.successor = successor;
	}

	public final void transform(From from) {
		To to = transformAnalysisResults(from);
		if (successor != null)
			successor.transform(to);
	}

	protected abstract To transformAnalysisResults(From from);

	@Override
	public String toString() {
		String debugInformation = debugInformation();
		String debugString = debugInformation == null ? "" : " (" + debugInformation + ") ";
		return getClass().getSimpleName() + debugString + (successor == null ? "" : "\n\t-> " + successor.toString());
	}

	protected String debugInformation() {
		return null;
	}
}
