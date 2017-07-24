package affine.ds;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import javax.lang.model.element.Element;

import org.checkerframework.javacutil.Pair;

/**
 * 
 * @author sharrap
 *
 * An object whose job it is to keep track of who is borrowing who.
 */
public class BorrowTracker {
	private final NoBorrowStatus NO_BORROW_STATUS = new NoBorrowStatus();
	
	//Every variable has a borrow status:
	// * No borrow status: This variable is currently unborrowed
	// * Mutable borrow status: This variable is currently borrowed by a mutable reference.
	// * Immutable borrow status: This variable is currently borrowed by one or more immutable references.
	private abstract class BorrowStatus {
		public boolean isBorrowed() {
			return isBorrowed(true) || isBorrowed(false);
		}
		public abstract boolean isBorrowed(boolean mutable);
		public abstract BorrowStatus addBorrow(Element borrower, boolean mutable);
		public abstract BorrowStatus removeBorrow(Element borrower);
		public abstract Set<Element> getBorrowers();
	}
	
	private class NoBorrowStatus extends BorrowStatus {
		public boolean isBorrowed(boolean mutable) {
			return false;
		}
		
		public BorrowStatus addBorrow(Element borrower, boolean mutable) {
			if (mutable) {
				return new MutableBorrowStatus(borrower);
			} else {
				return new ImmutableBorrowStatus(borrower);
			}
		}
		
		public BorrowStatus removeBorrow(Element borrower) {
			return this;
		}
		
		public Set<Element> getBorrowers() {
			return new HashSet<>();
		}
	}
	
	private class MutableBorrowStatus extends BorrowStatus {
		private Element borrower;
		
		public MutableBorrowStatus(Element borrower) {
			this.borrower = borrower;
		}
		
		public boolean isBorrowed(boolean mutable) {
			return mutable;
		}
		
		public BorrowStatus addBorrow(Element borrower, boolean mutable) {
			return null;
		}
		
		public BorrowStatus removeBorrow(Element borrower) {
			if (this.borrower == borrower) return NO_BORROW_STATUS;
			else return this;
		}
		
		public Set<Element> getBorrowers() {
			Set<Element> borrowers = new HashSet<>();
			borrowers.add(borrower);
			return borrowers;
		}
	}
	
	private class ImmutableBorrowStatus extends BorrowStatus {
		private Set<Element> borrowers;
		
		public ImmutableBorrowStatus(Element borrower) {
			this.borrowers = new HashSet<>();
			this.borrowers.add(borrower);
		}
		
		public boolean isBorrowed(boolean mutable) {
			return !mutable;
		}
		
		public BorrowStatus addBorrow(Element borrower, boolean mutable) {
			if (mutable) return null;
			borrowers.add(borrower);
			return this;
		}
		
		public BorrowStatus removeBorrow(Element borrower) {
			borrowers.remove(borrower);
			
			if (borrowers.isEmpty()) return NO_BORROW_STATUS;
			else return this;
		}
		
		public Set<Element> getBorrowers() {
			Set<Element> borrowersCopy = new HashSet<>();
			borrowersCopy.addAll(borrowers);
			return borrowersCopy;
		}
	}
	
	private Map<Element, BorrowStatus> borrowStatusMap = new HashMap<>();
	
	//Associates borrowers with their borrowed subject and whether the borrow is mutable.
	private Map<Element, Pair<Element, Boolean>> currentBorrowMap = new HashMap<>();
	
	private BorrowStatus getBorrowStatus(Element borrowed) {
		if (!borrowStatusMap.containsKey(borrowed)) {
			borrowStatusMap.put(borrowed, NO_BORROW_STATUS);
		}
		return borrowStatusMap.get(borrowed);
	}
	
	//Returns false if the borrow is disallowed, which will happen when getBorrowStatus(...).addBorrow(...) returns null
	private boolean updateBorrowStatus(Element borrowed, BorrowStatus update) {
		if (update == null) return false;
		borrowStatusMap.put(borrowed, update);
		return true;
	}

	/*
	 * The next two methods return true if the borrow is successful.
	 */
	public boolean addBorrowToNonShared(Element borrowed, Element borrower, boolean mutable) {
		if (updateBorrowStatus(borrowed, getBorrowStatus(borrowed).addBorrow(borrower, mutable))) {
			removeBorrow(borrower);
			currentBorrowMap.put(borrower, Pair.of(borrowed, mutable));
			
			return true;
		} else {
			return false;
		}
	}
	
	public boolean addBorrowToShared(Element borrowed, Element borrower) {
		if (!currentBorrowMap.containsKey(borrowed)) return false;
		
		return addBorrowToNonShared(currentBorrowMap.get(borrowed).first, borrower, false);
	}
	
	public boolean isBorrowed(Element borrowed) {
		return getBorrowStatus(borrowed).isBorrowed();
	}
	
	public boolean canMutablyBorrow(Element borrowed) {
		return !getBorrowStatus(borrowed).isBorrowed();
	}
	
	public boolean canImmutablyBorrow(Element borrowed) {
		return !getBorrowStatus(borrowed).isBorrowed(true);
	}
	
	/*
	 * Makes a borrower now borrow nothing.
	 */
	public void removeBorrow(Element borrower) {
		if (!currentBorrowMap.containsKey(borrower)) return;
		
		Pair<Element, Boolean> borrow = currentBorrowMap.get(borrower);
		
		updateBorrowStatus(borrow.first, getBorrowStatus(borrow.first).removeBorrow(borrower));
		
		//The borrow was mutable and the borrower was themself borrowed
		if (borrow.second && getBorrowStatus(borrower).isBorrowed()) {
			boolean mutableSource = getBorrowStatus(borrower).isBorrowed(true);
			for (Element borrowerTrans : getBorrowStatus(borrower).getBorrowers()) {
				removeBorrow(borrowerTrans);
				if (!addBorrowToNonShared(borrow.first, borrowerTrans, mutableSource)) {
					System.out.println("PANIC: This error should never happen. If it does, we have a bug in the checker");
				}
			}
		}
		
		currentBorrowMap.remove(borrower);
	}
}
