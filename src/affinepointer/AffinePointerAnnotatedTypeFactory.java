package affinepointer;

import javax.lang.model.element.Element;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import com.sun.source.tree.Tree;

import affinepointer.qual.Affine;
import affinepointer.qual.Bottom;
import affinepointer.qual.NonAffine;

/**
 * 
 * @author sharrap
 *
 * The AnnotatedTypeFactory for the AffineChecker.
 * 
 * The main changes from the basic ATF is that we use the dataflow analysis results even when they are less
 * specific than the default results, since Checker otherwise insists on using the most specific results since
 * dataflow is intended for refinement.
 */
public class AffinePointerAnnotatedTypeFactory extends GenericAnnotatedTypeFactory<AffinePointerValue, AffinePointerStore, AffinePointerTransfer, AffinePointerAnalysis> {	

	/*
	 * Notes to self:
	 * 
	 *   * getAnnotatedTypeLhs(Tree lhsTree) in GenericAnnotatedTypeFactory is not flow-sensitive.
	 *      Is this a problem?
	 *   * Somewhere after flow-sensitive analysis the most specific type is still being chosen.
	 *   	Where is this happening in the Checker source code?
	 *   * createTypeAnnotator and createTreeAnnotator might be the problem: try overriding them?
	 */
	
	public AffinePointerChecker checker; /* Hack */
	
	public AffinePointerAnnotatedTypeFactory(BaseTypeChecker checker) {
		super(checker);

		this.checker = (AffinePointerChecker)checker;
		
		postInit();
	}
	public AffinePointerAnnotatedTypeFactory(BaseTypeChecker checker, boolean useFlow) {
		super(checker,useFlow);

		this.checker = (AffinePointerChecker)checker;
		
		postInit();
	}
	
	/*
	 * Infers types of things in method bodies.
	 */
	@Override
	public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
		switch(tree.getKind()) {
		case NEW_CLASS:
			/*
			 * We assume that the results of "new" expressions on classes are affine.
			 * This may not be true in the rare-but-possible case of someone giving away
			 * a pointer to themself in the constructor.
			 * 
			 * TODO: Handle this edge case. Constructors should be markable as affine.
			 */
			type.addAnnotation(Affine.class);
			break;
		case NEW_ARRAY:
			/*
			 * The results of "new array" expressions are necessarily affine.
			 */
			type.addAnnotation(Affine.class);
			break;
			
			/*
			 * Null has the bottom type, to distinguish it from Affine.
			 */
			
		case NULL_LITERAL:
			type.addAnnotation(Bottom.class);
			break;
			
		case STRING_LITERAL:
			/*
			 * String literals really should be Shared.
			 * That being said, for now there is no nice way
			 * to convert them between NonAffine and Shared,
			 * so to prevent breaking things they are NonAffine.
			 * 
			 * TODO: Fix this. A hardcoded hack might be appropriate.
			 */
			
		case INT_LITERAL:
		case BOOLEAN_LITERAL:
		case FLOAT_LITERAL:
		case CHAR_LITERAL:
		case LONG_LITERAL:
		case DOUBLE_LITERAL:
			/*
			 * All other literals are non-affine
			 * since we do not support non-pointer Affine types.
			 */
			type.addAnnotation(NonAffine.class);
			break;
		default:
			/*
			 * Revert to default behaviour.
			 */
			super.addComputedTypeAnnotations(tree, type, iUseFlow);
		}
		
		//TODO: This is a hack to make the type factory actually use
		//the flow information. What is causing it to otherwise prefer
		//the most specific type even after changing the mostSpecific function?
		if (flowResult.getValue(tree) != null) {
			type.clearAnnotations();
			type.addAnnotations(flowResult.getValue(tree).getAnnotations());
			
			//System.out.println("Fetched annotation for " + tree + ": " + type.getAnnotations());
		} else {
			//System.out.println("Kept annotation for " + tree + ": " + type.getAnnotations());
		}
	}
	
	/*
	 * The default implementation of this in GenericAnnotatedTypeFactory<>
	 * does not use flow-sensitive refinement. The override turns
	 * this on but is otherwise identical for now.
	 * 
	 * TODO: Does this help in any way?
	 */
	/*public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
        AnnotatedTypeMirror res = null;
        switch (lhsTree.getKind()) {
            case VARIABLE:
            case IDENTIFIER:
            case MEMBER_SELECT:
            case ARRAY_ACCESS:
                res = getAnnotatedType(lhsTree);
                break;
            default:
                if (TreeUtils.isTypeTree(lhsTree)) {
                    // lhsTree is a type tree at the pseudo assignment of a returned expression to declared return type.
                    res = getAnnotatedType(lhsTree);
                } else {
                    ErrorReporter.errorAbort(
                            "GenericAnnotatedTypeFactory: Unexpected tree passed to getAnnotatedTypeLhs. "
                                    + "lhsTree: "
                                    + lhsTree
                                    + " Tree.Kind: "
                                    + lhsTree.getKind());
                }
        }
        return res;
    }*/
	
	/*
	 * Can be used to add computed type annotations to methods.
	 */
	public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
		super.addComputedTypeAnnotations(elt, type);
	}
}
