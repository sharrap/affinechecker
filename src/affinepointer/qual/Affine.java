package affinepointer.qual;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import org.checkerframework.framework.qual.SubtypeOf;

/**
 * 
 * @author sharrap
 *
 * Pointers which participate in the Affineness system should use this annotation.
 */
@SubtypeOf({Borrowed.class, NonAffine.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Affine {

}
