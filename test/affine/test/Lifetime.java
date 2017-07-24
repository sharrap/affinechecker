package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class Lifetime {
	/*
	 * We can only make borrows if the lifetime
	 * of the borrower is shorter than that of the borrowed.
	 */
	public void test() {
		@Shared Object o1;
		{
			@Affine Object o2 = null;
			@Shared Object o3;
			{
				@Shared Object o4;
				
				o4 = o2;
				o3 = o2;
				o1 = o2;   //BAD
			}
		}
	}
}
