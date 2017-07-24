package affine;

import java.util.Arrays;
import java.util.Collection;

import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

import affinepointer.AffinePointerChecker;
import lifetimebuilder.LifetimeBuilderChecker;

/**
 * 
 * @author sharrap
 *
 * First runs the LifetimeBuilderChecker, whose job it is to compute stack lifetimes.
 * Then runs the AffinePointerChecker, which performs the actual checking.
 */
public class AffineChecker extends AggregateChecker {

	@Override
	protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
		return Arrays.asList(LifetimeBuilderChecker.class, AffinePointerChecker.class);
	}

}
