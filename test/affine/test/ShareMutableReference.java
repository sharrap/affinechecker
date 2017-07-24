package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class ShareMutableReference {
	/*
	 * We can share mutable references.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Borrowed Object o2 = o1;
		@Shared Object o3 = o2;
	}
}
