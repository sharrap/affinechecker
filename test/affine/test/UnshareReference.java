package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class UnshareReference {
	private void meth(@Borrowed Object o) {}
	
	/*
	 * Make sure that unshared references can be used again,
	 * but only after every alias has been removed.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Affine Object o2 = null;
		
		@Shared Object o3 = o1;
		@Shared Object o4 = o1;
		@Shared Object o5 = o1;
		
		meth(o1); //BAD
		meth(o2);
		
		o3 = o2;
		
		meth(o1); //BAD
		meth(o2); //BAD
		
		o4 = o2;
		
		meth(o1); //BAD
		meth(o2); //BAD
		
		o5 = o2;
		
		meth(o1);
		meth(o2); //BAD
	}
}
