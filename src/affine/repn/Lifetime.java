package affine.repn;

/**
 * 
 * @author sharrap
 *
 * A very simple implementation of a variable's lifetime on the stack.
 * The parent field is used to compute a poset for faster less than comparisons,
 * and should not be used afterwards.
 */
public class Lifetime {
	private Lifetime parentLifetime;
	
	public Lifetime(Lifetime parentLifetime) {
		this.parentLifetime = parentLifetime;
	}
	public Lifetime() {
		this(null);
	}
	
	public Lifetime getParent() {
		return parentLifetime;
	}
	
	public void setParent(Lifetime lifetime) {
		parentLifetime = lifetime;
	}
	
	public boolean lessThan(Lifetime other) {
		return parentLifetime != null &&
				(parentLifetime.equals(other) || parentLifetime.lessThan(other));
	}
}
