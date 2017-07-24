package affinepointer.qual;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * 
 * @author sharrap
 *
 * This annotation is currently unused but
 * exists largely for debugging purposes.
 */
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Top {

}
