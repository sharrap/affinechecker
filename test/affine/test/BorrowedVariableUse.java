package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class BorrowedVariableUse {
	
	private void meth(@Affine Object o) {}
	
	/*
	 * We cannot use a variable if it is currently borrowed.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Shared Object o2 = o1;
		@Shared Object o3 = o1;
		@Affine Object o4 = o1; //BAD
		
		meth(o1); //BAD
	}
}
