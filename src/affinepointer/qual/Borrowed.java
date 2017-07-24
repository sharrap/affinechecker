package affinepointer.qual;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * 
 * @author sharrap
 *
 * Mutable references should use this annotation.
 */
@SubtypeOf({Shared.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Borrowed {

}
