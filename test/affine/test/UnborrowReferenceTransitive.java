package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class UnborrowReferenceTransitive {
	private void meth(@Borrowed Object o) {}
	
	/*
	 * Make sure that unborrowing also works
	 * with transitive borrows via borrowed references.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Affine Object o2 = null;
		
		@Borrowed Object o3 = o1;
		@Borrowed Object o4 = o3;
		
		meth(o1); //BAD
		meth(o2); //GOOD
		meth(o3); //BAD
		
		o3 = o2;  //GOOD
		
		meth(o1); //BAD
		meth(o2); //BAD
		meth(o3); //GOOD
	}
}
