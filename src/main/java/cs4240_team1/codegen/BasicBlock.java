package cs4240_team1.codegen;

import cs4240_team1.IRInstruction;
import cs4240_team1.Op;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class BasicBlock {

    public int blockNumber;

    public LinkedList<BlockVariable> varList = new LinkedList<>();
    public LinkedList<BlockVariable> spillVarList = new LinkedList<>();
    public LinkedList<String> stringVarList = new LinkedList<>();

	private Set<BasicBlock> predecessors;
	private Set<BasicBlock> successors; 

    LinkedList<IRInstruction> instructionList;
    LinkedList<IRInstruction> modifiedIR;

    public BasicBlock() {
        this.instructionList = new LinkedList<>();
        this.modifiedIR = new LinkedList<>();
		this.predecessors = new HashSet<>();
		this.successors = new HashSet<>();
    }

    public void addInstruction(IRInstruction i) {
        instructionList.add(i);
    }

	public int getPredecessorCount() {
		return this.predecessors.size();
	}

	public Set<BasicBlock> getSuccessors() {
		return this.successors;
	}

	/** Adds appropriate edges between blocks - use when constructing CFG. */
	public void addSuccessor(BasicBlock successor) {
		successor.predecessors.add(this);
		this.successors.add(successor);
	}

	/** Returns true if last instruction is a branch/goto -
	 * so this returns false if last instruction is RETURN
	 * (see isReturnBlock())
	 */
	public boolean isJumpBlock() {
		Op lastOp = this.instructionList.getLast().getOp();

		switch (lastOp) {
		case BREQ:
		case BRLEQ:
		case BRGEQ:
		case BRGT:
		case BRLT:
		case BRNEQ:
		case GOTO:
			return true;
		default:
			return false;
		}
	}

	/** Returns true if last instruction is a return */
	public boolean isReturnBlock() {
		Op lastOp = this.instructionList.getLast().getOp();
		return lastOp == Op.RETURN;
	}

	public boolean isGotoBlock() {
		Op lastOp = this.instructionList.getLast().getOp();
		return lastOp == Op.GOTO;
	}

	/**
	 * If this is a jump block, returns label of instruction it jumps to
	 * - null otherwise.
	 */
	public String getJumpLabel() {
		if (!this.isJumpBlock()) {
			return null;
		}

		IRInstruction lastInstr = this.instructionList.getLast();
		return lastInstr.getOp() == Op.GOTO
			? lastInstr.getArgs()[0].getTextForIR()
			: lastInstr.getArgs()[2].getTextForIR()  /* branch */
			;
	}

    public String toString() {
        StringBuilder build = new StringBuilder();
        for (IRInstruction i : instructionList) {
            build.append(i.toString() + "\n");
        }
        return build.toString();
    }

    public String toStringModified() {
        StringBuilder build = new StringBuilder();
        for (IRInstruction i : modifiedIR) {
            build.append(i.toString() + "\n");
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
