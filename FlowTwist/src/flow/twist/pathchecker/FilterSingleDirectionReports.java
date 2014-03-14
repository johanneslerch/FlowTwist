package flow.twist.pathchecker;

import java.util.List;

import soot.SootMethod;

import com.google.common.collect.Lists;

import flow.twist.config.AnalysisDirection;
import flow.twist.reporter.Report;
import flow.twist.transformer.ResultTransformer;
import flow.twist.util.CacheMap;

public class FilterSingleDirectionReports extends ResultTransformer<Iterable<Report>, Iterable<Report>> {

	public FilterSingleDirectionReports(ResultTransformer<Iterable<Report>, ?> successor) {
		super(successor);
	}

	protected Iterable<Report> transformAnalysisResults(Iterable<Report> from) {
		CacheMap<SootMethod, ReportsPerSourceMethod> cache = new CacheMap<SootMethod, ReportsPerSourceMethod>() {
			@Override
			protected ReportsPerSourceMethod createItem(SootMethod key) {
				return new ReportsPerSourceMethod(key);
			}
		};
		for (Report r : from) {
			SootMethod method = r.context.icfg.getMethodOf(r.targetUnit);
			cache.getOrCreate(method).reports.add(r);
		}

		List<Report> result = Lists.newLinkedList();
		for (ReportsPerSourceMethod rpsm : cache.values()) {
			if (rpsm.hasReportsForBothDirections())
				result.addAll(rpsm.reports);
		}
		return result;
	}

	public static class ReportsPerSourceMethod {
		public final SootMethod method;
		public final List<Report> reports;

		public ReportsPerSourceMethod(SootMethod method) {
			this.method = method;
			reports = Lists.newLinkedList();
		}

		public boolean hasReportsForBothDirections() {
			int forward = 0;
			for (Report r : reports) {
				if (r.context.direction == AnalysisDirection.FORWARDS)
					forward++;
			}
			return forward > 0 && forward < reports.size();
		}
	}

}
