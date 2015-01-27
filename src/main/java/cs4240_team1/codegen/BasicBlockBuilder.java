package cs4240_team1.codegen;

import cs4240_team1.IRInstruction;
import cs4240_team1.IRCodeArgument;
import cs4240_team1.LabelArgument;
import cs4240_team1.Op;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;

public class BasicBlockBuilder {
    List<IRInstruction> IRList;
    public LinkedList<BasicBlock> blockList = new LinkedList<>();

    public BasicBlockBuilder(List<IRInstruction> IRList) {
        this.IRList = IRList;
    }

    /**
     * Returns a list of BasicBlocks (appropriately wired together to form CFG)
     * in sequential program order.
     */
    public List<BasicBlock> build() {
		fuseAdjacentLabels();  // mutates IRList -- can remove labels and modify labelargs
        markLeaders();

        int blockCount = 0;
        BasicBlock currentBlock = null;
        for (IRInstruction i : IRList) {
            if (i.isLeader) {
                currentBlock = new BasicBlock();
                currentBlock.blockNumber = blockCount;
                blockCount++;
                blockList.add(currentBlock);
            }

            /* no NPE because first instruction is a leader */
            currentBlock.addInstruction(i);
        }
        CFGHelper();
        return blockList;
    }

	/**
	 * If a program looks like...
	 *
	 *    _label1
	 *    _label2
	 *    _label3
	 *    [...code...]
	 *    goto _label1
	 *    ...
	 *    breq _label2
	 *    ...
	 *    breq _label3
	 *
	 *
	 * It will be transformed like so (fuse labels)
	 *
	 *    _label1
	 *    ...
	 *    goto _label1
	 *    ...
	 *    breq _label1
	 *    ...
	 *    breq _label1
	 */
	private void fuseAdjacentLabels() {
		// will not have adjacent labels
		LinkedList<IRInstruction> finalIRList = new LinkedList<>();
		LinkedList<IRInstruction> adjLabels = new LinkedList<>();

		// in the above example, this would contain the following mappings:
		//
		// "_label1" => "_label1"
		// "_label2" => "_label1"
		// "_label3" => "_label1"
		HashMap<String, String> labelMapping = new HashMap<>();

		for (IRInstruction instr: this.IRList) {
			if (instr.getOp() == Op.LABEL) {
				String currLabel = instr.getArgs()[0].getTextForIR();

				// this label is the "primary" label that we "fuse" into
				if (adjLabels.isEmpty()) {
					labelMapping.put(currLabel, currLabel);
					finalIRList.add(instr);
				} else {
					String primaryLabel = adjLabels.get(0).getArgs()[0].getTextForIR();
					labelMapping.put(currLabel, primaryLabel);
					// non-primary adjacent labels do not make it
					// into the finalIRList
				}
				adjLabels.add(instr);
			} else {
				// if instruction is not a label, then clear the adjLabels list
				// because... there aren't any more adjLabels for that group
				adjLabels.clear();
				finalIRList.add(instr);
			}
		}

		// we now do a second pass, replacing any label arguments as needed
		for (IRInstruction instr: finalIRList) {
			for (IRCodeArgument arg: instr.getArgs()) {
				if (arg instanceof LabelArgument) {
					String currLabelText = arg.getTextForIR();
					String newLabelText = labelMapping.get(currLabelText);
					arg.setTextForIR(newLabelText);
				}
			}
		}

		this.IRList = finalIRList;
	}

    private void CFGHelper() {
        String label = null;
        IRInstruction i;
        IRInstruction i2;
        for (BasicBlock block : blockList) {/*Adding edges to blocks that jump*/
            if (block.isJumpBlock()) {
                label = block.getJumpLabel();

                /* Inefficient. Have some ideas about changing
                 * IR code to avoid some of these
                 * redundant loops, but I don't want to break things atm.
                 */
                for (BasicBlock block2 : blockList) {
                    i2 = block2.instructionList.getFirst();
                    if (i2.getOp() == Op.LABEL) {
                        if (i2.getArgs()[0].getTextForIR() == label) {
                            block.addSuccessor(block2);
                            break;
                        }
                    }
                }
            }

        }


        /* Third loop to separate logic for the moment.
         * Adding edges to sequential blocks EXCEPT for GOTOs.
         *
         * GOTO blocks are the only blocks that do not have fall-through
         * to the next sequential-order basic block.
         */
        BasicBlock prevBlock = null;
        boolean firstIter = true;

        for (BasicBlock block : blockList) {
            if (firstIter) {
                prevBlock = block;
                firstIter = false;
            } else {
                if (!prevBlock.isGotoBlock()) {
                    prevBlock.addSuccessor(block);
                }
                prevBlock = block;
            }
        }
    }

    private void markLeaders() {
        ArrayList<String> jumpToLabelList = new ArrayList<>();
        boolean previousInstructionWasJump = false;
        IRList.get(0).isLeader = true; /*First instruction is a leader*/
        
        /*First pass grabs all the labels and marks all entries after jumps as a leader*/
        for (IRInstruction i : IRList) {
            if (previousInstructionWasJump) {
                i.isLeader = true;
            }
            previousInstructionWasJump = false;
            switch (i.getOp()) {
                case BREQ:
                case BRLEQ:
                case BRGEQ:
                case BRGT:
                case BRLT:
                case BRNEQ:
                    jumpToLabelList.add(i.getArgs()[2].getTextForIR());
                    previousInstructionWasJump = true;
                    break;
                case GOTO:
                    jumpToLabelList.add(i.getArgs()[0].getTextForIR());
                    previousInstructionWasJump = true;
                    break;
            }
        }

        /* second pass marks labels that we jump to as leaders */
        for (IRInstruction i : IRList) {
            if (i.getOp() == Op.LABEL) {
                if (jumpToLabelList.contains(i.getArgs()[0].getTextForIR())) {
                    i.isLeader = true;
                }
            }
        }
    }

    public void printString() {
        System.out.println("=== START BASIC BLOCK LIST ===");
        for (BasicBlock block : blockList) {
            System.out.println("=== START BLOCK ===");
            System.out.print(block.toString());
            System.out.println("=== END BLOCK ===");
        }
        System.out.println("=== END BASIC BLOCK LIST ===");
    }

    public static void printStringStatic(List<BasicBlock> blocks) {
        System.out.println("=== START BASIC BLOCK WITH MODIFIED IR LIST ===");
        for (BasicBlock block : blocks) {
            System.out.println("=== START BLOCK ===");
            System.out.print(block.toStringModified());
            System.out.println("=== END BLOCK ===");
        }
        System.out.println("=== END BASIC BLOCK WITH MODIFIED IR LIST ===");
    }

    public void printEdges() {
        System.out.println("=== BASIC BLOCK EDGES ===");
        for (BasicBlock block : blockList) {
            System.out.println("=== START BLOCK " + block.blockNumber + " ===");
            System.out.print("EDGES:");
            for (BasicBlock successor: block.getSuccessors()) {
                System.out.print(" " + successor.blockNumber);
            }
            System.out.print("\n");
            System.out.println("=== END BLOCK " + block.blockNumber + " ===");
        }
    }
}
