package affine.test;
import affinepointer.qual.Affine;
import affinepointer.qual.Borrowed;
import affinepointer.qual.NonAffine;
import affinepointer.qual.Shared;
import affinepointer.qual.Unusable;

@SuppressWarnings({"unused"})
public class UnborrowReference {
	private void meth(@Borrowed Object o) {}
	
	/*
	 * If a variable stops being referred to it should be usable again.
	 */
	public void test() {
		@Affine Object o1 = null;
		@Affine Object o2 = null;
		
		@Borrowed Object o3 = o1;
		
		meth(o1); //BAD
		meth(o2);
		
		o3 = o2;
		
		meth(o1);
		meth(o2); //BAD
	}
}
