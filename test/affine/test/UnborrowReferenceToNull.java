package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class UnborrowReferenceToNull {
	private void meth(@Borrowed Object o) {}
	
	/*
	 * We should also be able to null borrowed references.
	 */
	public void test() {
		@Affine Object o1 = null;

		@Borrowed Object o2 = o1;
		
		meth(o1); //BAD
		
		o2 = null;
		
		meth(o1);
	}
}
