package affinepointer;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.source.Result;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import affine.ds.BorrowTracker;
import affine.repn.Lifetime;
import affine.repn.LifetimePoset;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.Bottom;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

/**
 * 
 * @author sharrap
 *
 * The transfer function for the Affine Pointer analysis.
 * Also in charge of reporting errors: this is discouraged by checker,
 * but we don't really have a better way of doing things.
 */
public class AffinePointerTransfer
		extends CFAbstractTransfer<AffinePointerValue, AffinePointerStore, AffinePointerTransfer> {
	
	private final AnnotationMirror UNUSABLE;
	private final AnnotationMirror AFFINE;
	private final AnnotationMirror SHARED;
	private final AnnotationMirror BORROWED;
	private final AnnotationMirror BOTTOM;
	private final AnnotationMirror NONAFFINE;
	private BorrowTracker borrowTracker;
	
	private AffinePointerChecker checker; //Hack
	
	public AffinePointerTransfer(AffinePointerAnalysis analysis) {
		super(analysis);
		
		checker = ((AffinePointerAnnotatedTypeFactory)analysis.getTypeFactory()).checker;
		
		Elements elts = analysis.getTypeFactory().getElementUtils();
		
		UNUSABLE = AnnotationUtils.fromClass(elts, Unusable.class);
		AFFINE = AnnotationUtils.fromClass(elts, Affine.class);
		SHARED = AnnotationUtils.fromClass(elts, Shared.class);
		BORROWED = AnnotationUtils.fromClass(elts, Borrowed.class);
		BOTTOM = AnnotationUtils.fromClass(elts, Bottom.class);
		NONAFFINE = AnnotationUtils.fromClass(elts, NonAffine.class);
		borrowTracker = new BorrowTracker();
	}
	
	public AffinePointerTransfer(AffinePointerAnalysis analysis, boolean forceConcurrentSemantics) {
		super(analysis,forceConcurrentSemantics);
		
		checker = ((AffinePointerAnnotatedTypeFactory)analysis.getTypeFactory()).checker;
		
		Elements elts = analysis.getTypeFactory().getElementUtils();
		
		UNUSABLE = AnnotationUtils.fromClass(elts, Unusable.class);
		AFFINE = AnnotationUtils.fromClass(elts, Affine.class);
		SHARED = AnnotationUtils.fromClass(elts, Shared.class);
		BORROWED = AnnotationUtils.fromClass(elts, Borrowed.class);
		BOTTOM = AnnotationUtils.fromClass(elts, Bottom.class);
		NONAFFINE = AnnotationUtils.fromClass(elts, NonAffine.class);
		borrowTracker = new BorrowTracker();
	}

	/*
	 * Allows us to change annotations. For this to work properly,
	 * AffinePointerValue needs to have its mostSpecific updated to
	 * just select the most recent value, or else Checker will
	 * "helpfully" observe that @Affine is more specific than @Unusable
	 * and ignore our attempt to change the annotation.
	 */
	private void changeAnnotation(AffinePointerStore store, FlowExpressions.Receiver receiver, AnnotationMirror annotation) {
		//TODO: This may break things if we carry around other information with the value
		store.clearValue(receiver);
		store.insertValue(receiver, annotation);
	}
	
	@SuppressWarnings("unused")
	private void changeAnnotation(AffinePointerStore store, Node node, AnnotationMirror annotation) {
		FlowExpressions.Receiver receiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), node);
		changeAnnotation(store, receiver, annotation);
	}

	private void changeAnnotation(
			TransferResult<?, AffinePointerStore> result,
			FlowExpressions.Receiver receiver,
			AnnotationMirror annotation) {
		if (result.containsTwoStores()) {
			changeAnnotation(result.getThenStore(), receiver, annotation);
			changeAnnotation(result.getElseStore(), receiver, annotation);
		} else {
			changeAnnotation(result.getRegularStore(), receiver, annotation);
		}
	}

	private void changeAnnotation(
			TransferResult<?, AffinePointerStore> result,
			Node node,
			AnnotationMirror annotation) {
		FlowExpressions.Receiver receiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), node);
		changeAnnotation(result, receiver, annotation);
	}
	
	/*
	 * Check if a node is currently annotated with some particular annotation
	 * according to the information held in a TransferInput, TransferResult, or AffinePointerStore.
	 */
	private boolean hasAnnotation(AffinePointerStore store, Node node, AnnotationMirror am) {
		FlowExpressions.Receiver receiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), node);
		
		return store.getValue(receiver).getAnnotations().contains(am);
	}
	
	private boolean hasAnnotation(TransferResult<?, AffinePointerStore> result, Node node, AnnotationMirror am) {
		if (result.containsTwoStores()) {
			//TODO Should we use &&?
			return hasAnnotation(result.getThenStore(), node, am) || hasAnnotation(result.getElseStore(), node, am);
		} else {
			return hasAnnotation(result.getRegularStore(), node, am);
		}
	}
	
	@SuppressWarnings("unused")
	private boolean hasAnnotation(TransferInput<?, AffinePointerStore> input, Node node, AnnotationMirror am) {
		if (input.containsTwoStores()) {
			//TODO Should we use &&?
			return hasAnnotation(input.getThenStore(), node, am) || hasAnnotation(input.getElseStore(), node, am);
		} else {
			return hasAnnotation(input.getRegularStore(), node, am);
		}
	}
	
	/*
	 * Checker is a bit too "helpful" by default and will refine @Shared and @Borrowed variables
	 * into @Affine variables, which prevents us from distinguishing them using the analysis.
	 * getValueFromFactory gets the annotation that was actually provided by the user,
	 * which is always correct in the case of @Shared and @Affine.
	 */
	private boolean hasAnnotationWithoutFlow(Node node, AnnotationMirror am) {
		return getValueFromFactory(node.getTree(), node).getAnnotations().contains(am);
	}
	
	private Element elementOf(Node n) {
		Tree t = n.getTree();
		if (t instanceof ExpressionTree) {
			return TreeUtils.elementFromUse((ExpressionTree)t);
		} else if (t instanceof MethodInvocationTree) {
			return TreeUtils.elementFromUse((MethodInvocationTree)t);
		} else if (t instanceof NewClassTree) {
			return TreeUtils.elementFromUse((NewClassTree)t);
		} else if (t instanceof VariableTree) {
			return TreeUtils.elementFromDeclaration((VariableTree)t);
		} else if (t instanceof MethodTree) {
			return TreeUtils.elementFromDeclaration((MethodTree)t);
		} else if (t instanceof ClassTree) {
			return TreeUtils.elementFromDeclaration((ClassTree)t);
		} else {
			throw new RuntimeException("Unsupported tree type " + t.getClass());
		}
	}
	
	private Lifetime getVariableLifetime(Node n) {
		Element elt = elementOf(n);
		
		return lifetimebuilder.LifetimeBuilderVisitor.publicVariableLifetimeMap.get(elt);
	}

	@Override
	public TransferResult<AffinePointerValue, AffinePointerStore>
	       visitAssignment(AssignmentNode n, TransferInput<AffinePointerValue, AffinePointerStore> in) {
		
		//TODO: Do we want to do anything before looking at this node?
		//We may want to:
		//* Observe that borrowed things are no longer borrowed (since we can't do this elsewhere right now)
		//* Make things into errors, such as assignment to borrowed variable
		
		TransferResult<AffinePointerValue, AffinePointerStore> result = super.visitAssignment(n, in);

		//The type system doesn't prevent assignments to @Unusable things, so we need to stop this explicitly.
		//We need to make sure to do this before we reset BORROWED/SHARED things to their "correct" annotations.
		if (hasAnnotation(result, n.getTarget(), UNUSABLE) && hasAnnotation(result, n.getExpression(), UNUSABLE)) {
			checker.report(Result.failure("cannot.assign.to.unusable",n.getExpression()), n.getExpression().getTree());
		}
		
		//Prevent Checker from being too "helpful" and changing the types of @Shared or @Borrowed values.
		//We will still use hasAnnotationWithoutFlow to be completely sure we get the right answers
		//for the borrow analysis, but this should help improve subtyping errors.
		
		if (hasAnnotationWithoutFlow(n.getTarget(), BORROWED)) {
			changeAnnotation(result, n.getTarget(), BORROWED);
		} else if (hasAnnotationWithoutFlow(n.getTarget(), SHARED)) {
			changeAnnotation(result, n.getTarget(), SHARED);
		}
		
		//Prevent Checker from being too "helpful" as usual...
		if (hasAnnotation(result, n.getTarget(), BOTTOM)) {
			if (hasAnnotationWithoutFlow(n.getTarget(), AFFINE)) {
				changeAnnotation(result, n.getTarget(), AFFINE);
			} else if (hasAnnotationWithoutFlow(n.getTarget(), NONAFFINE)) {
				changeAnnotation(result, n.getTarget(), NONAFFINE);
			}
		}
		
		//Update borrows if LHS is a borrow. Otherwise make sure the RHS is not currently borrowed.
		if (hasAnnotationWithoutFlow(n.getTarget(), BORROWED) || hasAnnotationWithoutFlow(n.getTarget(), SHARED)) {
			if (n.getExpression() instanceof LocalVariableNode) {
				if (!LifetimePoset.compareLT(getVariableLifetime(n.getTarget()), getVariableLifetime(n.getExpression()))) {
					checker.report(Result.failure("bad.lifetime.relationship", n.getTarget(), n.getExpression()), n.getTree());
				}
				
				boolean mutableBorrow = hasAnnotationWithoutFlow(n.getTarget(), BORROWED);
							
				if (hasAnnotationWithoutFlow(n.getExpression(), SHARED)) {
					//This should only happen if n.getTarget() is also SHARED.
					if (mutableBorrow) {
						checker.report(Result.failure("cannot.borrow.shared.with.borrowed", n.getTarget(), n.getExpression()), n.getExpression().getTree());
					} else if (!borrowTracker.addBorrowToShared(elementOf(n.getExpression()), elementOf(n.getTarget()))) {
						System.out.println(n + " This should never print and is a bug with the checker.");
					}
				} else {
					if (!borrowTracker.addBorrowToNonShared(
							elementOf(n.getExpression()),
							elementOf(n.getTarget()),
							mutableBorrow)) {
						checker.report(Result.failure(mutableBorrow ? "cannot.borrow.borrowed" : "cannot.share.borrowed", n.getExpression()),
								n.getExpression().getTree());
					}
				}
			} else if (n.getExpression() instanceof NullLiteralNode) {
				borrowTracker.removeBorrow(elementOf(n.getTarget()));
			} else {
				checker.report(Result.failure("unrecognized.borrow.target", n.getTarget(), n.getExpression()), n.getExpression().getTree());
			}
		} else {
			//The LHS was not a borrow. The RHS should not be borrowed, since if it is then it's unusable right now.
			if (borrowTracker.isBorrowed(elementOf(n.getExpression()))) {
				checker.report(Result.failure("use.of.borrowed.variable", n.getExpression()), n.getExpression().getTree());
				
				//Invalidate RHS if it has type @Affine
			} else if (n.getExpression() instanceof LocalVariableNode || n.getExpression() instanceof FieldAccessNode) {
				if (hasAnnotation(result, n.getExpression(), AFFINE)) {
					changeAnnotation(result, n.getExpression(), UNUSABLE);
				}
			}
		}
		
		return result;
	}
	
	@Override
	public TransferResult<AffinePointerValue, AffinePointerStore>
	       visitFieldAccess(FieldAccessNode n, TransferInput<AffinePointerValue, AffinePointerStore> p) {
		
		System.out.println("TODO Unsupported");
		
		return super.visitFieldAccess(n, p);
	}
	
	private boolean argumentHasAnnotation(VariableElement argument, AnnotationMirror am) {
		for (AnnotationMirror a : argument.asType().getAnnotationMirrors()) {
			if (a.getAnnotationType().toString().equals(am.getAnnotationType().toString())) return true;
		}
		return false;
	}
	
	@Override
	public TransferResult<AffinePointerValue, AffinePointerStore>
	       visitMethodInvocation(MethodInvocationNode n, TransferInput<AffinePointerValue, AffinePointerStore> in) {
		
		TransferResult<AffinePointerValue, AffinePointerStore> result = super.visitMethodInvocation(n, in);
		
		int i = 0;
		List<? extends VariableElement> argumentTypes = n.getTarget().getMethod().getParameters();
		for (Node operand : n.getArguments()) {
			//We should invalidate the variable if it and the argument both have type @Affine.
			//If the argument has type @Shared or @Borrowed, doing nothing whatsoever correctly models
			//borrowing.
			if (argumentHasAnnotation(argumentTypes.get(i), AFFINE)
					&& hasAnnotation(result, operand, AFFINE)) {
				if (borrowTracker.isBorrowed(elementOf(operand))) {
					checker.report(Result.failure("use.of.borrowed.variable", operand), operand.getTree());
				} else {
					changeAnnotation(result, operand, UNUSABLE);
				}
			} else if (argumentHasAnnotation(argumentTypes.get(i), BORROWED)) {
				if (!borrowTracker.canMutablyBorrow(elementOf(operand))) {
					checker.report(Result.failure("cannot.borrow.borrowed", operand), operand.getTree());
				}
			} else if (argumentHasAnnotation(argumentTypes.get(i), SHARED)) {
				if (!borrowTracker.canImmutablyBorrow(elementOf(operand))) {
					checker.report(Result.failure("cannot.share.borrowed", operand), operand.getTree());
				}
			}
			i++;
		}
		return result;
	}
}
