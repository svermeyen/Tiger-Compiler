package cs4240_team1.codegen;

import cs4240_team1.IRInstruction;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * Represents a single Extended Basic Block.
 *
 * We are not doing bonus, so we don't need to keep track of
 * an EBB's successors or predecessors.
 */
public class EBB {

	private List<BasicBlock> basicBlockList;
    public List<BlockVariable> varList = new LinkedList<>();
    public List<BlockVariable> spillVarList = new LinkedList<>();
    LinkedList<IRInstruction> instructionList = new LinkedList<>();

	/** Creates an EBB initially containing its leader block. */
	public EBB(BasicBlock leader) {
		this.basicBlockList = new ArrayList<>();
		this.basicBlockList.add(leader);
	}

	/** 
	 * Adds block to the EBB.
	 * Order and/or relationships to other blocks is not important.
	 */
	public void add(BasicBlock block) {
		basicBlockList.add(block);
	}

	/**
	 * Returns basic blocks in EBB.
	 *
	 * The first BasicBlock is guaranteed to be the leader block for the EBB.
	 */
	public List<BasicBlock> getBasicBlocks() {
		return this.basicBlockList;
	}

	public BasicBlock getLeaderBlock() {
		return basicBlockList.get(0);
	}

	public List<BasicBlock> getExitBlocks() {
		List<BasicBlock> exitBlocks = new ArrayList<>();
		for (BasicBlock block: basicBlockList) {
			if (isExitBlock(block)) {
				exitBlocks.add(block);
			}
		}

		return exitBlocks;
	}

	/** Defined as (1) having edge to block in another EBB,
	 * or (2) having no outgoing edges
	 * or (3) has edge back to own leader block
	 *
	 * [really, (1) and (3) can be combined as: has edge to leader block of an EBB]
	 */
	public boolean isExitBlock(BasicBlock block) {
		/* not very efficient -- better way is to lookup in HashSet, not List */ 
		Set<BasicBlock> successors = block.getSuccessors();
		if (successors.isEmpty()) {
			return true;
		}

		for (BasicBlock successor: successors) {
			if (!basicBlockList.contains(successor) || successor == this.getLeaderBlock()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Prints IR from all basic blocks - order of basic blocks is dictated
	 * by the return order from getBasicBlocks()
	 *
	 * (so the only guarantee is that the leader comes first)
	 */
    public String toString() {
        StringBuilder build = new StringBuilder();
		for (BasicBlock bb: this.getBasicBlocks()) {
			for (IRInstruction i : bb.modifiedIR) {
				build.append(i.toString() + "\n");
			}
		}
        return build.toString();
    }

    public int getLiveIndex(String str) {
        for (int i = 0; i < varList.size(); i++) {
            if (varList.get(i) != null && varList.get(i).getTextForIR().equals(str)) {
                return i;
            }
        }
        return -1;
    }
    public int getSpillIndex(String str) {
        for (int i = 0; i < spillVarList.size(); i++) {
            if (spillVarList.get(i) != null && spillVarList.get(i).getTextForIR().equals(str)) {
                return i;
            }
        }
        return -1;
    }

}
