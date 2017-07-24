package affinepointer;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.javacutil.Pair;

import affine.repn.LifetimePoset;
import lifetimebuilder.LifetimeBuilderVisitor;

/**
 * 
 * @author sharrap
 *
 * A dataflow analysis object for the AffinePointerChecker.
 * The implementation is trivial other than that it initializes the lifetime poset.
 */
public class AffinePointerAnalysis
		extends CFAbstractAnalysis<AffinePointerValue, AffinePointerStore, AffinePointerTransfer> {
	
	public AffinePointerAnalysis(
			BaseTypeChecker checker,
			AffinePointerAnnotatedTypeFactory factory,
			List<Pair<VariableElement, AffinePointerValue>> fieldValues,
			int maxCountBeforeWidening) {
		super(checker,factory,fieldValues,maxCountBeforeWidening);
		
		LifetimePoset.initializeLifetimePoset(LifetimeBuilderVisitor.publicLifetimes);
	}
	public AffinePointerAnalysis(
			BaseTypeChecker checker,
			AffinePointerAnnotatedTypeFactory factory,
			List<Pair<VariableElement, AffinePointerValue>> fieldValues) {
		super(checker,factory,fieldValues);
		
		LifetimePoset.initializeLifetimePoset(LifetimeBuilderVisitor.publicLifetimes);
	}

	@Override
	public AffinePointerValue createAbstractValue(Set<AnnotationMirror> annotations, TypeMirror underlyingType) {
		return new AffinePointerValue(this, annotations, underlyingType);
	}

	@Override
	public AffinePointerStore createCopiedStore(AffinePointerStore store) {
		return new AffinePointerStore(store);
	}

	@Override
	public AffinePointerStore createEmptyStore(boolean sequentialSemantics) {
		return new AffinePointerStore(this, sequentialSemantics);
	}

}
