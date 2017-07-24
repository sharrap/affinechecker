package affinepointer;

import org.checkerframework.framework.flow.CFAbstractStore;

/**
 * 
 * @author sharrap
 *
 * A trivial dataflow store for the AffinePointer analysis.
 * Exists in case we want to add things to it.
 */
public class AffinePointerStore extends CFAbstractStore<AffinePointerValue, AffinePointerStore> {
	public AffinePointerStore(AffinePointerAnalysis analysis, boolean sequentialSemantics) {
		super(analysis,sequentialSemantics);
	}
	
	public AffinePointerStore(AffinePointerStore other) {
		super(other);
	}
	
	@Override
	public Object clone() {
		return new AffinePointerStore(this);
	}
}
