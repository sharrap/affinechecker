package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class BorrowImmutableReference {
	
	/*
	 * We cannot borrow a reference if it is currently shared.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Shared Object o2 = o1;
		@Borrowed Object o3 = o2; //BAD
	}
}
