package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class BorrowBorrowedReferenceTwice {
	private void meth(@Borrowed Object o) {}
	
	/*
	 * We cannot mutably borrow the same thing twice.
	 * This test involves borrowing borrowed references.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Borrowed Object o2 = o1;
		@Borrowed Object o3 = o2;
		@Borrowed Object o4 = o2; //BAD
		
		meth(o2); //BAD
	}
}
