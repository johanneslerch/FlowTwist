package flow.twist.test.util.selectors;

import soot.SootMethod;
import soot.Unit;

public class UnitSelectorFactory {

	public static UnitSelector unitWithoutLabel(final String label, final UnitSelector selector) {
		return new UnitSelector() {
			@Override
			public boolean matches(SootMethod method, Unit unit) {
				return !unit.toString().contains(label) && selector.matches(method, unit);
			}

			@Override
			public String toString() {
				return "'" + selector.toString() + "' without '" + label + "'";
			}
		};
	}

	public static UnitSelector unitByLabel(final String label) {
		return new UnitSelector() {
			@Override
			public boolean matches(SootMethod method, Unit unit) {
				return unit.toString().contains(label);
			}

			@Override
			public String toString() {
				return label;
			}
		};
	}

	public static UnitSelector unitInMethod(final String methodString, final UnitSelector selector) {
		return new UnitSelector() {
			@Override
			public boolean matches(SootMethod method, Unit unit) {
				return method.toString().contains(methodString) && selector.matches(method, unit);
			}

			@Override
			public String toString() {
				return "'" + selector.toString() + "' in '" + methodString + "'";
			}
		};
	}

	public static UnitSelector anyUnit() {
		return new UnitSelector() {

			@Override
			public boolean matches(SootMethod method, Unit unit) {
				return true;
			}

			@Override
			public String toString() {
				return "any unit";
			}
		};
	}

	public static UnitSelector specificUnit(final Unit specificUnit) {
		return new UnitSelector() {

			@Override
			public boolean matches(SootMethod method, Unit unit) {
				return unit == specificUnit;
			}

			@Override
			public String toString() {
				return "specific unit: " + specificUnit;
			}
		};
	}
}
