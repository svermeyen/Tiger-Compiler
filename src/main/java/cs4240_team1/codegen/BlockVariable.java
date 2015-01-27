package cs4240_team1.codegen;

import cs4240_team1.IRCodeArgument;
import cs4240_team1.TigerType;

import java.util.Comparator;

public class BlockVariable implements Comparable<BlockVariable>, IRCodeArgument {
    public String varName;
    public Register regName;
    public boolean isArrayUse = false;
    public String offsetText;
    public int defUseCount = 1;

	/* All block variables correspond to some original IRCodeArgument.
	 *
	 * Maintain a reference, because we need to delegate to this object
	 * when getTigerType() is called */
	private IRCodeArgument originalArg;

    public BlockVariable(IRCodeArgument originalArg) {
		this.originalArg = originalArg;
        this.varName = originalArg.getTextForIR();
    }

    @Override
    public int compareTo(BlockVariable that) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        if (this.defUseCount == that.defUseCount) return EQUAL;
        if (this.defUseCount < that.defUseCount) return BEFORE;
        if (this.defUseCount > that.defUseCount) return AFTER;
        /*assert this.equals(that) : "compareTo inconsistent with equals.";*/
        return EQUAL;
    }

    @Override
    public String getTextForIR() {
        return varName;
    }

    @Override
    public void setTextForIR(String id) {
        varName = id;
    }

    @Override
    public TigerType getTigerType() {
		return originalArg.getTigerType();
    }
}
