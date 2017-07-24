package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class BorrowBorrowedReference {
	
	private void meth(@Borrowed Object o) {}
	
	/*
	 * We cannot mutably borrow the same thing twice.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Borrowed Object o2 = o1;
		@Borrowed Object o3 = o1; //BAD
		
		meth(o1); //BAD
	}
}
