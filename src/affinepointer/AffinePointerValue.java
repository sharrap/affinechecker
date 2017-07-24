package affinepointer;

import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.tools.javac.code.Type.WildcardType;

/**
 * 
 * @author sharrap
 *
 * A single value within the AffinePointer analysis.
 * Storage-wise it's trivial, but the mostSpecific function is overriden to
 * instead return the most recently computed value, since we don't
 * want to do type refinement!
 */
public class AffinePointerValue extends CFAbstractValue<AffinePointerValue> {
	public AffinePointerValue(
			AffinePointerAnalysis analysis,
			Set<AnnotationMirror> annotations,
			TypeMirror underlyingType) {
		super(analysis,annotations,underlyingType);
	}
	
	/*
	 * By default this method appears to be called with the receiver being
	 * the new store and "other" being the old store. For the purposes of the
	 * Affine checker we always prefer the most recent inference. This breaks
	 * the semantics of the method but is seemingly a necessary hack to make
	 * the dataflow analysis work.
	 */
	@Override
    public AffinePointerValue mostSpecific( AffinePointerValue other, AffinePointerValue backup) {
        if (other == null) {
            return this;
        }
        //Types types = analysis.getTypes();
        //TODO HACK
        TypeMirror mostSpecifTypeMirror = other.getUnderlyingType();
        
        //System.out.println("Preferring " + this + " to " + other + " with backup " + backup);
        
        Set<AnnotationMirror> mostSpecific = AnnotationUtils.createAnnotationSet();
        MostSpecificVisitor ms =
                new MostSpecificVisitor(
                        mostSpecifTypeMirror,
                        this.getUnderlyingType(),
                        other.getUnderlyingType(),
                        this.getAnnotations(),
                        other.getAnnotations(),
                        backup,
                        mostSpecific);
        ms.visit();
        if (ms.error) {
            return backup;
        }
        return analysis.createAbstractValue(mostSpecific, mostSpecifTypeMirror);
    }
	
	/*
	 * Everything below here needs to be copied from CFAbstractValue to
	 * give us the ability to actually change the implementation of mostSpecific.
	 */
    protected abstract class AnnotationSetAndTypeMirrorVisitor {
        TypeMirror result;

        private AnnotatedTypeVariable aAtv;
        private AnnotatedTypeVariable bAtv;
        private Set<AnnotationMirror> aSet;
        private Set<AnnotationMirror> bSet;

        public AnnotationSetAndTypeMirrorVisitor(
                TypeMirror result,
                TypeMirror aTypeMirror,
                TypeMirror bTypeMirror,
                Set<AnnotationMirror> aSet,
                Set<AnnotationMirror> bSet) {
            this.result = result;
            this.aSet = aSet;
            this.bSet = bSet;
            this.aAtv = getEffectTypeVar(aTypeMirror);
            this.bAtv = getEffectTypeVar(bTypeMirror);
        }

        void visit() {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            Set<? extends AnnotationMirror> tops = hierarchy.getTopAnnotations();
            for (AnnotationMirror top : tops) {
                AnnotationMirror a = hierarchy.findAnnotationInHierarchy(aSet, top);
                AnnotationMirror b = hierarchy.findAnnotationInHierarchy(bSet, top);
                if (a != null && b != null) {
                    visitAnnotationExistInBothSets(a, b, top);
                } else if (a != null) {
                    visitAnnotationExistInOneSet(a, bAtv, top);
                } else if (b != null) {
                    visitAnnotationExistInOneSet(b, aAtv, top);
                } else {
                    visitNeitherAnnotationExistsInBothSets(aAtv, bAtv, top);
                }
            }
        }

        protected abstract void visitAnnotationExistInBothSets(
                AnnotationMirror a, AnnotationMirror b, AnnotationMirror top);

        protected abstract void visitNeitherAnnotationExistsInBothSets(
                AnnotatedTypeVariable aAtv, AnnotatedTypeVariable bAtv, AnnotationMirror top);

        protected abstract void visitAnnotationExistInOneSet(
                AnnotationMirror anno, AnnotatedTypeVariable atv, AnnotationMirror top);
    }
    
    private AnnotatedTypeVariable getEffectTypeVar(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return null;
        } else if (typeMirror.getKind() == TypeKind.WILDCARD) {
            return getEffectTypeVar(((WildcardType) typeMirror).getExtendsBound());

        } else if (typeMirror.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typevar = ((TypeVariable) typeMirror);
            AnnotatedTypeMirror atm =
                    analysis.getTypeFactory().getAnnotatedType(typevar.asElement());
            return (AnnotatedTypeVariable) atm;
        } else {
            return null;
        }
    }
	
    private class MostSpecificVisitor extends AnnotationSetAndTypeMirrorVisitor {
    	TypeMirror result;
        boolean error = false;
        // TypeMirror backupTypeMirror;
        Set<AnnotationMirror> backupSet;
        // AnnotatedTypeVariable backupAtv;
        Set<AnnotationMirror> mostSpecific;

        public MostSpecificVisitor(
                TypeMirror result,
                TypeMirror aTypeMirror,
                TypeMirror bTypeMirror,
                Set<AnnotationMirror> aSet,
                Set<AnnotationMirror> bSet,
                AffinePointerValue backup,
                Set<AnnotationMirror> mostSpecific) {
            super(result, aTypeMirror, bTypeMirror, aSet, bSet);
            this.result = result;
            this.mostSpecific = mostSpecific;
            if (backup != null) {
                this.backupSet = backup.getAnnotations();
                // this.backupTypeMirror = backup.getUnderlyingType();
                // this.backupAtv = getEffectTypeVar(backupTypeMirror);
            } else {
                // this.backupAtv = null;
                // this.backupTypeMirror = null;
                this.backupSet = null;
            }
        }

        private AnnotationMirror getBackUpAnnoIn(AnnotationMirror top) {
            if (backupSet == null) {
                // If there is no back up value, but on is required then the resulting set will
                // not be the most specific.  Indicate this with the error.
                error = true;
                return null;
            }
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            return hierarchy.findAnnotationInHierarchy(backupSet, top);
        }

        @Override
        protected void visitAnnotationExistInBothSets(
                AnnotationMirror a, AnnotationMirror b, AnnotationMirror top) {
            /*QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            if (hierarchy.isSubtype(a, b)) {
                mostSpecific.add(a);
            } else if (hierarchy.isSubtype(b, a)) {
                mostSpecific.add(b);
            } else {
                AnnotationMirror backup = getBackUpAnnoIn(top);
                if (backup != null) {
                    mostSpecific.add(backup);
                }
            }*/
            //TODO HACK
            //It looks like "other" always contains the value we want?
            mostSpecific.add(b);
        }

        @Override
        protected void visitNeitherAnnotationExistsInBothSets(
                AnnotatedTypeVariable aAtv, AnnotatedTypeVariable bAtv, AnnotationMirror top) {
            if (canBeMissingAnnotations(result)) {
                // don't add an annotation
            } else {
                AnnotationMirror aUB = aAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                AnnotationMirror bUB = bAtv.getUpperBound().getEffectiveAnnotationInHierarchy(top);
                visitAnnotationExistInBothSets(aUB, bUB, top);
            }
        }

        @Override
        protected void visitAnnotationExistInOneSet(
                AnnotationMirror anno, AnnotatedTypeVariable atv, AnnotationMirror top) {
            QualifierHierarchy hierarchy = analysis.getTypeFactory().getQualifierHierarchy();
            AnnotationMirror upperBound = atv.getEffectiveAnnotationInHierarchy(top);

            if (!canBeMissingAnnotations(result)) {
                visitAnnotationExistInBothSets(anno, upperBound, top);
                return;
            }
            Set<AnnotationMirror> lBSet =
                    AnnotatedTypes.findEffectiveLowerBoundAnnotations(hierarchy, atv);
            AnnotationMirror lowerBound = hierarchy.findAnnotationInHierarchy(lBSet, top);
            if (hierarchy.isSubtype(upperBound, anno)) {
                // no anno is more specific than anno
            } else if (hierarchy.isSubtype(anno, lowerBound)) {
                mostSpecific.add(anno);
            } else {
                AnnotationMirror backup = getBackUpAnnoIn(top);
                if (backup != null) {
                    mostSpecific.add(backup);
                }
            }
        }
    }
    
    private static boolean canBeMissingAnnotations(TypeMirror typeMirror) {
        if (typeMirror == null) {
            return false;
        }
        if (typeMirror.getKind() == TypeKind.VOID
                || typeMirror.getKind() == TypeKind.NONE
                || typeMirror.getKind() == TypeKind.PACKAGE) {
            return true;
        }
        if (typeMirror.getKind() == TypeKind.WILDCARD) {
            return canBeMissingAnnotations(((WildcardType) typeMirror).getExtendsBound());
        }
        return typeMirror.getKind() == TypeKind.TYPEVAR;
    }
}
