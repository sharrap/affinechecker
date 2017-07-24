package affine.ds;

import java.util.Map;
import com.sun.source.tree.BlockTree;

import affine.repn.Lifetime;

/**
 * 
 * @author sharrap
 *
 * An object whose job it is to keep track of which block is associated with which stack lifetime.
 */
public class BlockLifetimeManager {
	private Map<BlockTree, Lifetime> internalMap;
	
	public BlockLifetimeManager() {
		this.internalMap = new java.util.HashMap<BlockTree, Lifetime>();
	}
	
	public void registerBlock(BlockTree block, BlockTree lifetimeParent) {
		internalMap.put(block, new Lifetime(getLifetime(lifetimeParent)));
	}
	
	public Lifetime getLifetime(BlockTree block) {
		return block != null ? internalMap.get(block) : null;
	}
}
