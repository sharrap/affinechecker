package affine.repn;

import java.util.Collection;
import java.util.Set;

import org.checkerframework.javacutil.Pair;

/**
 * 
 * @author sharrap
 * 
 * A fairly naive class which takes a partially ordered set
 * implemented as a simple DAG and returns the same poset
 * represented as a map for more efficient queries.
 *
 * Everything is static as a hack to get around the difficulty of passing
 * data between independent checker modules.
 */
public class LifetimePoset {
	private static Set<Pair<Lifetime, Lifetime>> comparisonSet;
	
	public static void initializeLifetimePoset(Collection<Lifetime> lifetimes) {
		comparisonSet = new java.util.HashSet<Pair<Lifetime, Lifetime>>();
		
		for (Lifetime l : lifetimes) {
			comparisonSet.add(Pair.of(l, l));
			Lifetime p;
			while ((p = l.getParent()) != null) {
				comparisonSet.add(Pair.of(l, p));
				l = p;
			}
		}
	}
	
	public static boolean compareLT(Lifetime l, Lifetime r) {
		return comparisonSet.contains(Pair.of(l, r));
	}
	
	public static boolean compareGT(Lifetime l, Lifetime r) {
		return comparisonSet.contains(Pair.of(r, l));
	}
}