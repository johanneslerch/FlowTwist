package flow.twist.transformer;


public class StoreDataTransformer<T> extends ResultTransformer<T, Void> {

	private T data;

	public StoreDataTransformer() {
		super(null);
	}

	@Override
	protected Void transformAnalysisResults(T from) {
		this.data = from;
		return null;
	}

	public T getData() {
		return data;
	}
}
