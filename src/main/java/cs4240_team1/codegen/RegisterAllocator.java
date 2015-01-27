package cs4240_team1.codegen;

import cs4240_team1.IRInstruction;
import cs4240_team1.Op;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * To get modified IR code for a program,
 * call allocate() followed by getModifiedIRCode() */
public class RegisterAllocator {
    List<EBB> blockList;
    List<IntRegister> intRegisters;
	List<FPRegister> fpRegisters;
    List<IntRegister> intSpillRegisters;
	List<FPRegister> fpSpillRegisters;
    int index;
    boolean dirty = false;
    BlockVariable currBlockVar;
    BlockVariable dirtyBlockVar;
    String currVar;
    Register dest;
    Register src1;
    Register src2;


    public RegisterAllocator(List<EBB> blockList) {
        this.blockList = blockList;
        intRegisters = IntRegister.getIntRegisters();
        intSpillRegisters = IntRegister.getSpillRegisters();/*size 3*/

    }

    public void allocate() {
        for (EBB ebb : blockList) {
            for (BasicBlock block : ebb.getBasicBlocks()) {
                for (IRInstruction i : block.instructionList) {
                    BlockVariable temp;
                    switch (i.getOp()) {
                        case BREQ:
                        case BRLEQ:
                        case BRGEQ:
                        case BRGT:
                        case BRLT:
                        case BRNEQ:
                        case ASSIGNVAR:
                            for (int j = 0; j < 2; j++) {
                                if ((index = ebb.getLiveIndex(i.getArgs()[j].getTextForIR())) != -1) {
                                    ebb.varList.get(index).defUseCount++;
                                } else {
                                    ebb.varList.add(new BlockVariable(i.getArgs()[j]));
                                }
                            }
                            break;
                        case ADD:
                        case DIV:
                        case MULT:
                        case SUB:
                        case AND:
                        case OR:
                        case ARRAYSTORE:
                        case ARRAYLOAD:
                        case ASSIGNARR:
                            for (int j = 0; j < 3; j++) {
                                if ((index = ebb.getLiveIndex(i.getArgs()[j].getTextForIR())) != -1) {
                                    ebb.varList.get(index).defUseCount++;
                                } else {
                                    ebb.varList.add(new BlockVariable(i.getArgs()[j]));
                                }
                            }
                            break;
                        case ASSIGNLIT:
						case CALLR:  // for geti, getf (only ones we support), just look at 0 arg
                            if ((index = ebb.getLiveIndex(i.getArgs()[0].getTextForIR())) != -1) {
                                ebb.varList.get(index).defUseCount++;
                            } else {
                                ebb.varList.add(new BlockVariable(i.getArgs()[0]));
                            }
                            break;
						case CALL:
                            if ((index = ebb.getLiveIndex(i.getArgs()[1].getTextForIR())) != -1) {
                                ebb.varList.get(index).defUseCount++;
                            } else {
                                ebb.varList.add(new BlockVariable(i.getArgs()[1]));
                            }
							break;
                        default:
                            break;
                    }//switch
                }//for each instr
            }//for each bb
			removeArrays(ebb.varList);
            Collections.sort(ebb.varList, new VariableComparator());
            Collections.reverse(ebb.varList);
        }//for each ebb
        selectRegisters();
    }

	/**
	 * We reference arrays by label, so we don't need to allocate vars for them (we don't use $la)
	 *
	 * (there are errors if we don't remove them, too -- because we call LW when the live reg
	 *  isn't actually modified, even though the array may have been modified via ARRAYSTORE, resulting
	 *  in corrupted data in the array)
	 */
	private void removeArrays(List<BlockVariable> varList) {
		/* work in reverse so we can remove safely */
		for (int i = varList.size() - 1; i >= 0; i--) {
			if (varList.get(i).getTigerType().isArray()) {
				varList.remove(i);
			}
		}
	}

    public void selectRegisters() { /*This helper method is to reduce the size of allocate method and increase readability*/
        for (EBB ebb : blockList) {
			/* Setup:
			 *   intRegisters (after call: will hold any unused int registers -- not important)
			 *   fpRegisters (after call: will hold any unused fp registers -- not important)
			 *
			 *   intSpillRegisters: index 0, 1, 2 correspond to spills used for dst/src1/src2
			 *   fpSpillRegisters: index 0, 1, 2 correspond to spills used for dst/src1/src2
			 *
			 *   ebb.varList:  will only contain live vars (of both INT and FP type)
			 *   ebb.spillVarList:  will contain any spill vars (of both INT and FP type)
			 */
			initializeRegisterLists(ebb);

			/* Max: changed this to add instructions to the BB's modifiedIR, not the EBB's
			 * (so we can preserve original program order)
			 */
            for (BasicBlock block : ebb.getBasicBlocks()) {
                /*Need to transform list of variables into block variable representation*/
                for (IRInstruction i : block.instructionList) { /*begin to scan through code, looking for spills, inserting register names*/
                    Op op = i.getOp();
                    switch (op) {
                        case ADD:
                        case DIV:
                        case MULT:
                        case SUB:
                        case AND:
                        case OR:
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[2].getTextForIR())) != -1) { /*grab name of destination*/
                                dest = ebb.varList.get(index).regName;
                            } else {
                                dirty = true; /*this let's us know we are storing to a temp and thus must save immediately after*/
                                dirtyBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                dest = getSpillRegFor(0, dirtyBlockVar);
                            }
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab src names*/
                                src1 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src1 = getSpillRegFor(1, currBlockVar), currBlockVar));
                            }
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[1].getTextForIR())) != -1) {
                                src2 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src2 = getSpillRegFor(2, currBlockVar), currBlockVar));
                            }
                            block.modifiedIR.add(new IRInstruction(op, src1, src2, dest)); /*this emits the actual instruction with proper regs*/
                            if (dirty) {
                                dirty = false;
                                block.modifiedIR.add(new IRInstruction(Op.SW, dest, dirtyBlockVar));
                            }
                            break;
                        case ASSIGNVAR:
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab src names*/
                                dest = ebb.varList.get(index).regName;
                            } else {
                                dirty = true; /*this let's us know we are storing to a temp and thus must save immediately after*/
                                dirtyBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                dest = getSpillRegFor(0, dirtyBlockVar);
                            }
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[1].getTextForIR())) != -1) {
                                src1 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src1 = getSpillRegFor(1, currBlockVar), currBlockVar));
                            }
                            block.modifiedIR.add(new IRInstruction(op, dest, src1));
                            if (dirty) {
                                dirty = false;
                                block.modifiedIR.add(new IRInstruction(Op.SW, dest, dirtyBlockVar));
                            }
                            break;
                        case ASSIGNLIT:
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab src names*/
                                dest = ebb.varList.get(index).regName;
                            } else {
                                dirty = true; /*this let's us know we are storing to a temp and thus must save immediately after*/
                                dirtyBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                dest = getSpillRegFor(0, dirtyBlockVar);
                            }
                            block.modifiedIR.add(new IRInstruction(op, dest, i.getArgs()[1]));
                            if (dirty) {
                                dirty = false;
                                block.modifiedIR.add(new IRInstruction(Op.SW, dest, dirtyBlockVar));
                            }
                            break;
                        case BREQ:
                        case BRLEQ:
                        case BRGEQ:
                        case BRGT:
                        case BRLT:
                        case BRNEQ:
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab src names*/
                                src1 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src1 = getSpillRegFor(1, currBlockVar), currBlockVar));
                            }
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[1].getTextForIR())) != -1) {
                                src2 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src2 = getSpillRegFor(2, currBlockVar), currBlockVar));
                            }
							// TODO -- does label pass through as-is?
                            block.modifiedIR.add(new IRInstruction(op, src1, src2, i.getArgs()[2]));
                            break;
                        case ASSIGNARR:
                            //if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab name of destination*/
                            //    dest = ebb.varList.get(index).regName;
                            //} else {
                            //    currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                            //    block.modifiedIR.add(new IRInstruction(Op.LW, dest = intSpillRegisters.get(0), currBlockVar));
                            //}
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[1].getTextForIR())) != -1) { /*grab src names*/
                                src1 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src1 = getSpillRegFor(1, currBlockVar), currBlockVar));
                            }
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[2].getTextForIR())) != -1) {
                                src2 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src2 = getSpillRegFor(2, currBlockVar), currBlockVar));
                            }
                            block.modifiedIR.add(new IRInstruction(
										op,
										i.getArgs()[0],  // VarEntry to use text for label
										src1, // size
										src2, // val to assign
										intSpillRegisters.get(0)  // scratch reg for array offset in loop
										                       // this spill reg is free to use as scratch register
										                       // because we will never need it for dst in ASSIGNARR
										)); /*this emits the actual instruction with proper regs*/
                            break;
                        case ARRAYLOAD:
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab name of destination*/
                                dest = ebb.varList.get(index).regName;
                            } else {
                                dirty = true;
                                dirtyBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                //block.modifiedIR.add(new IRInstruction(Op.LW, dest = intSpillRegisters.get(0), dirtyBlockVar));
                                dest = getSpillRegFor(0, dirtyBlockVar);
                            }
                            //if ((index = ebb.getLiveIndex(currVar = i.getArgs()[1].getTextForIR())) != -1) { /*grab src names*/
                            //    src1 = ebb.varList.get(index).regName;
                            //} else {
                            //    index = ebb.getSpillIndex(currVar);
                            //    currBlockVar = ebb.spillVarList.get(index);
                            //    block.modifiedIR.add(new IRInstruction(Op.LW, src1 = intSpillRegisters.get(1), currBlockVar));
                            //}
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[2].getTextForIR())) != -1) {
                                src2 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src2 = getSpillRegFor(2, currBlockVar), currBlockVar));
                            }
                            block.modifiedIR.add(new IRInstruction(
										op,
										dest,
										i.getArgs()[1],  // VarEntry to use for array label
										src2,  // reg for index
										intSpillRegisters.get(1)  // scratch reg for byte offs (this has to be an INT!)
										                       // this is okay to use for a scratch reg!
															   // we don't spill src1 because we use it as a label only
										)); /*this emits the actual instruction with proper regs*/
                            if (dirty) {
                                dirty = false;
                                block.modifiedIR.add(new IRInstruction(Op.SW, dest, dirtyBlockVar));
                            }
                            break;
                        case ARRAYSTORE:
                            //if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab name of destination*/
                            //    dest = ebb.varList.get(index).regName;
                            //} else {
                            //    currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                            //    block.modifiedIR.add(new IRInstruction(Op.LW, dest = intSpillRegisters.get(0), currBlockVar));
                            //}
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[1].getTextForIR())) != -1) { /*grab src names*/
                                src1 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src1 = getSpillRegFor(1, currBlockVar), currBlockVar));
                            }
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[2].getTextForIR())) != -1) {
                                src2 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src2 = getSpillRegFor(2, currBlockVar), currBlockVar));
                            }
                            block.modifiedIR.add(new IRInstruction(
										op,
										i.getArgs()[0],  // VarEntry to use for label
										src1,  // reg with index
										src2,  // reg with value to assign
										intSpillRegisters.get(0)  // reg to use for byte offset calculation (has to be INT!)
										                       // this spill will be free!
															   // because we don't need it for "dest"
										)); /*this emits the actual instruction with proper regs*/
                            break;
						case CALL:  // only supporting printi, printf currently
							if (!i.getArgs()[0].getTextForIR().equals("printi")
									&& !i.getArgs()[0].getTextForIR().equals("printf")) {
								break; //TODO a better way of handling this than silently ignoring??
							}
							
                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[1].getTextForIR())) != -1) { /*grab src names*/
                                src1 = ebb.varList.get(index).regName;
                            } else {
                                currBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                block.modifiedIR.add(new IRInstruction(Op.LW, src1 = getSpillRegFor(1, currBlockVar), currBlockVar));
                            }
							block.modifiedIR.add(new IRInstruction(
										op,
										i.getArgs()[0],  // FunctionEntry as label
										src1));
							break;
						case CALLR:  // only supporting geti, getf
							if (!i.getArgs()[1].getTextForIR().equals("geti")
									&& !i.getArgs()[1].getTextForIR().equals("getf")) {
								break; //TODO a better way of handling this than silently ignoring??
							}

                            if ((index = ebb.getLiveIndex(currVar = i.getArgs()[0].getTextForIR())) != -1) { /*grab name of destination*/
                                dest = ebb.varList.get(index).regName;
                            } else {
                                dirty = true; /*this let's us know we are storing to a temp and thus must save immediately after*/
                                dirtyBlockVar = ebb.spillVarList.get(ebb.getSpillIndex(currVar));
                                dest = getSpillRegFor(0, dirtyBlockVar);
                            }
                            block.modifiedIR.add(new IRInstruction(
										op,
										dest,
										i.getArgs()[1]));  // preserve FunctionEntry as label
                            if (dirty) {
                                dirty = false;
                                block.modifiedIR.add(new IRInstruction(Op.SW, dest, dirtyBlockVar));
                            }
							break;
                        default:
                            block.modifiedIR.add(i);
                            break;
                    } //switch op
                } //for instruction in block
            } //for block in basicblocks
			
			loadLiveVarsAtLeaderBB(ebb);
			storeLiveVarsAtExitBBs(ebb);
        } //for ebb in BlockList
    }

	/** Call this at beginning of register selection for an EBB */
	private void initializeRegisterLists(EBB ebb) {

		// temporarily clear ebb.varList, creating two new lists based on type
		LinkedList<BlockVariable> intsFromVarList = new LinkedList<>();
		LinkedList<BlockVariable> fpsFromVarList = new LinkedList<>();
		
		for (int i = ebb.varList.size()-1; i >= 0; i--) {
			BlockVariable var = ebb.varList.remove(i);
			if (var.getTigerType().needsIntRegister()) {
				intsFromVarList.addFirst(var);
			} else {
				fpsFromVarList.addFirst(var);
			}
		}

		// assign registers, and move elements from right list to spill list if needed
		List<BlockVariable> intsForSpillVarList = assignIntRegistersToVars(intsFromVarList);
		List<BlockVariable> fpsForSpillVarList = assignFPRegistersToVars(fpsFromVarList);

		// now create the final ebb.varList of all BlockVars that are live
		ebb.varList = new LinkedList<>();
		ebb.varList.addAll(intsFromVarList);
		ebb.varList.addAll(fpsFromVarList);

		// and create the final ebb.spillVarList of all that are spills
		ebb.spillVarList = new LinkedList<>();
		ebb.spillVarList.addAll(intsForSpillVarList);
		ebb.spillVarList.addAll(fpsForSpillVarList);

		// and finally initialize spillRegs, to be safe
		intSpillRegisters = IntRegister.getSpillRegisters();
		fpSpillRegisters = FPRegister.getSpillRegisters();
	}


	// XXX copied logic from here to the method below (bad, I know)
	//
	// assign registers, and move elements from right list to spill list if needed
	private List<BlockVariable> assignIntRegistersToVars(List<BlockVariable> intsForVarList) {
		intRegisters = IntRegister.getIntRegisters();
		final int INT_REG_COUNT = intRegisters.size();
		LinkedList<BlockVariable> spillList = new LinkedList<>();

		// we have to split the list and add the remaining to spill list
		if (intsForVarList.size() > INT_REG_COUNT) {
			List<BlockVariable> split = intsForVarList.subList(INT_REG_COUNT, intsForVarList.size()); /*get all variables that can't be allocated*/
			spillList = new LinkedList<>(split); /*assign them to a new list*/
			split.clear(); /*this removes all spilled variables from the block.varList*/
		}
		
		for (BlockVariable var : intsForVarList) { /*assigning registers to the live variables*/
			var.regName = intRegisters.remove(0);
		}

		return spillList;
	}

	// XXX copied logic from up there (bad, I know)
	//
	// assign registers, and move elements from right list to spill list if needed
	private List<BlockVariable> assignFPRegistersToVars(List<BlockVariable> fpsForVarList) {
		fpRegisters = FPRegister.getFPRegisters();
		final int FP_REG_COUNT = fpRegisters.size();
		LinkedList<BlockVariable> spillList = new LinkedList<>();

		// we have to split the list and add the remaining to spill list
		if (fpsForVarList.size() > FP_REG_COUNT) {
			List<BlockVariable> split = fpsForVarList.subList(FP_REG_COUNT, fpsForVarList.size()); /*get all variables that can't be allocated*/
			spillList = new LinkedList<>(split); /*assign them to a new list*/
			split.clear(); /*this removes all spilled variables from the block.varList*/
		}
		
		for (BlockVariable var : fpsForVarList) { /*assigning registers to the live variables*/
			var.regName = fpRegisters.remove(0);
		}

		return spillList;
	}

	/** returns appropriate Register type based on the spillVar's type */
	private Register getSpillRegFor(int spillRegIndex, BlockVariable spillVar) {
		if (spillVar.getTigerType().needsIntRegister()) {
			return intSpillRegisters.get(spillRegIndex);
		}
		return fpSpillRegisters.get(spillRegIndex);
	}

	/** Insert at front of modified IR code (but not before labels!) */
	private void loadLiveVarsAtLeaderBB(EBB ebb) {
		BasicBlock leaderBB = ebb.getLeaderBlock();
		LinkedList<IRInstruction> labels = new LinkedList<>();

		// temporarily remove labels so we can add to front
		while (leaderBB.modifiedIR.getFirst().getOp() == Op.LABEL) {
			labels.addFirst(leaderBB.modifiedIR.removeFirst());
		}

		/* traverse in reverse so that the load for the first ebb.varList element
		 * will be the first instruction in leaderBB modified IR
		 *
		 * (only doing this so I can run 'diff -s' on some output to verify I didn't
		 * break anything -- a for-each loop would be logically sound here, of course)
		 */
		for (int i = ebb.varList.size() - 1; i >= 0; i--) {
			BlockVariable liveVar = ebb.varList.get(i);
			leaderBB.modifiedIR.addFirst(new IRInstruction(Op.LW, liveVar.regName, liveVar));
		}

		// push removed labels back onto list
		while (!labels.isEmpty()) {
			leaderBB.modifiedIR.addFirst(labels.removeFirst());
		}
	}

	/**
	 * Insert at end of modified IR code (but before a BR or GOTO, if present)
	 *
	 * This must be called after modifiedIR is generated for all BBs!
	 *
	 * This is to ensure that the modified jump instruction for jump blocks
	 * is the last instruction in for those BBs!
	 * (so we can properly pop() and push() it back onto the list to
	 *  insert stores before it)
	 */
	private void storeLiveVarsAtExitBBs(EBB ebb) {
		List<BasicBlock> exitBBList = ebb.getExitBlocks();
		List<IRInstruction> liveStores = getLiveVarStoreInstructions(ebb);

		for (BasicBlock exitBB: exitBBList) {
			if (exitBB.isJumpBlock() || exitBB.isReturnBlock()) {
				/* make sure the stores occur before the branch/goto! */
				IRInstruction jumpInstr = exitBB.modifiedIR.removeLast();
				exitBB.modifiedIR.addAll(liveStores);
				exitBB.modifiedIR.addLast(jumpInstr);
			} else {
				exitBB.modifiedIR.addAll(liveStores);
			}
		}
	}

	private List<IRInstruction> getLiveVarStoreInstructions(EBB ebb) {
		List<IRInstruction> liveStores = new LinkedList<>();
		for (BlockVariable liveVar: ebb.varList) {
			liveStores.add(new IRInstruction(Op.SW, liveVar.regName, liveVar));
		}
		return liveStores;
	}

	/**
	 * Call this method after allocate() to get the entire modified IR code.
	 *
	 * Input must be basic blocks as they appear in sequential program order
	 * - note that BasicBlockBuilder will already return list in this order,
	 *   so use that list!
	 */
	public List<IRInstruction> getModifiedIRCode(List<BasicBlock> bbList) {
		List<IRInstruction> allModifiedIR = new LinkedList<>();
		for (BasicBlock bb: bbList) {
			allModifiedIR.addAll(bb.modifiedIR);
		}
		return allModifiedIR;
	}
}
