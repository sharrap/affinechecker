package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class BadBorrowTarget {
	/*
	 * We do not support borrowing or sharing things
	 * other than local variables and null
	 */
	public void test() {
		@Borrowed Object o1 = new Object(); //BAD
		@Shared Object o2 = new Object();   //BAD
	}
}
