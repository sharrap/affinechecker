package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class AssignBetweenUnusable {
	private void meth(@Borrowed Object o) {}
	
	/*
	 * We should not be able to assign to things that are @Unusable,
	 * even if the type system otherwise allows it.
	 */
	public void test() {
		@Affine Object o1 = new Object();
		@Affine Object o2 = new Object();
		@Affine Object o3 = null;
		
		o3 = o1; //makes o1 unusable
		o3 = o2; //makes o2 unusable
		
		o1 = o2; //BAD
	}
}
