package lifetimebuilder;

import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import com.sun.source.tree.*;

import affine.ds.BlockLifetimeManager;

import affine.repn.Lifetime;

/**
 * 
 * @author sharrap
 *
 * The visitor for the LifetimeBuilder checker.
 * Determines the lifetime of each variable in the program.
 */
public class LifetimeBuilderVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {
	private BlockTree currentBlock = null;
	
	public static Map<Element, Lifetime> publicVariableLifetimeMap;
	public static Set<Lifetime> publicLifetimes;
	
	private Map<Element, Lifetime> variableLifetimeMap;
	private Set<Lifetime> lifetimes;
	/*
	 * Associates each block with a lifetime
	 */
	private BlockLifetimeManager blManager;
	
	private void initialize() {
		this.blManager = new BlockLifetimeManager();
		this.variableLifetimeMap = new java.util.HashMap<Element, Lifetime>();
		this.lifetimes = new java.util.HashSet<Lifetime>();
		
		publicVariableLifetimeMap = variableLifetimeMap;
		publicLifetimes = lifetimes;
	}
	
	public LifetimeBuilderVisitor(BaseTypeChecker checker) {
		super(checker);
		
		initialize();
	}
	
	@Override
	public Void visitBlock(BlockTree node, Void p) {
		BlockTree oldBlock = currentBlock;
		currentBlock = node;
		
		blManager.registerBlock(currentBlock, oldBlock);
		lifetimes.add(blManager.getLifetime(currentBlock));
		
		Void retn = super.visitBlock(node, p);
		
		currentBlock = oldBlock;
		return retn;
	}
	
	@Override
	public Void visitVariable(VariableTree node, Void p) {
		variableLifetimeMap.put(TreeUtils.elementFromDeclaration(node), blManager.getLifetime(currentBlock));
		
		return super.visitVariable(node, p);
	}
}