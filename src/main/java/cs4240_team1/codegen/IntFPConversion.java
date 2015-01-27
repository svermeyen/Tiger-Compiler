package cs4240_team1.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import utility.Utility;
import cs4240_team1.IRCodeArgument;
import cs4240_team1.IRInstruction;
import cs4240_team1.LabelArgument;
import cs4240_team1.LiteralArgument;
import cs4240_team1.Op;
import cs4240_team1.VarEntry;

public class IntFPConversion {

	/**
	 * for instructions which can be straight up converted, store
	 * correspondences between 
	 */
	public static Map<Op,Op> intFpCorrespondences = Utility.createMapFromArray(
		Op.ADD, Op.ADD_S,
		Op.SUB, Op.SUB_S,
		Op.MUL, Op.MUL_S,
		Op.DIV, Op.DIV_S,
		Op.LW, Op.LWC1,
		Op.SW, Op.SWC1,
		Op.LI, Op.LI_S,
		Op.BEQ, Op.C_EQ_S, //there are some weird correspondences here
		Op.BNE, Op.C_EQ_S, //because spim does not implement all fp
		Op.BLT, Op.C_LT_S, //comparators.  we make do by using condition codes
		Op.BLE, Op.C_LE_S, //in addition to comparators.
		Op.BGT, Op.C_LE_S,
		Op.BGE, Op.C_LT_S
	);
	
	/* when you convert these to fp, break on "true" */
	static Set<Op> fpConditionFlagTrueCodes = new HashSet<Op>(Arrays.asList(
			Op.BEQ, Op.BLE, Op.BLT));

	/* when you convert these to fp, break on "false" */
	static Set<Op> conditionFlagFalseCodes = new HashSet<Op>(Arrays.asList(
			Op.BNE, Op.BGE, Op.BGT));
	
	static Set<Op> intBranchOps = new HashSet<Op>(Arrays.asList(
			Op.BEQ, Op.BNE, Op.BLT, Op.BLE, Op.BGT, Op.BGE));
	
	
	public static Register regGetter(int num, boolean isFP) {
			Register baseReg; 
		if (isFP) {
			baseReg = new FPRegister(0);
		} else {
			baseReg = new IntRegister(0);
		}
		
		/* remember that FPRegisters double num because of even restriction */
		return baseReg.newWithIndex(num);
	}
	
	/**
	 * takes in a line of mips assembly for integer operations.  
	 * if isFP is true, transforms this into mips assembly for fp operations.
	 * converts int regs to fp regs
	 */
	public static List<IRInstruction> processIntLineToFinalOutputRegAllocLevel
		(IRInstruction initialOut, boolean isFP) {
		if (isFP) {
			if (intBranchOps.contains(initialOut.getOp())) {
				throw new RuntimeException("convert this in the selector phase!");
			} else {
				Op newOp = intFpCorrespondences.get(initialOut.getOp());
				List<IRCodeArgument> args = Arrays.asList(initialOut.getArgs())
						.stream()
						.map(IntFPConversion::convertIntAsmArgToFPArg)
						.collect(Collectors.toList());
				IRInstruction out = new IRInstruction(newOp, 
						args.toArray(new IRCodeArgument[0]));
				return Arrays.asList(out);
			}
		} else {
			return Arrays.asList(initialOut);
		}
	}

	/**
	 * takes in a line of mips assembly for integer operations.  
	 * if isFP is true, transforms this into mips assembly for fp operations.
	 * does not convert register types.
	 */
	public static List<IRInstruction> processIntLineToFinalOutputSelectionLevel
		(IRInstruction initialOut, boolean isFP) {
		/*
		 * HACK
		 *
		 * Caller doesn't always properly identify FP for conditionals!
		 * Misses when src1 is int, but src2 is float
		 *
		 * Set FP here....
		 */
		if (intBranchOps.contains(initialOut.getOp())
				&& initialOut.getArgs()[1].getTigerType().needsFPRegister()) {
			isFP = true;
		}

		if (isFP) {
			if (intBranchOps.contains(initialOut.getOp())) {
				return getBranchInstructionFromBase(initialOut, true);
			} else {
				/* recall: these will be arithmetic ops, in form of 'op dst, src1, src2' */
				Op newOp = intFpCorrespondences.get(initialOut.getOp());
				List<IRInstruction> outStream = new ArrayList<>();

				//type promote if either source is INT!
				if (initialOut.getArgs()[2].getTigerType().needsIntRegister()) {
					NaiveInstructionSelector.promote(initialOut.getArgs()[2], outStream);
					outStream.add(new IRInstruction(
								newOp,
								initialOut.getArgs()[0],
								initialOut.getArgs()[1],
								FPRegister.reg_promote)
					);
					return outStream;
				} else if (initialOut.getArgs()[1].getTigerType().needsIntRegister()) {
					NaiveInstructionSelector.promote(initialOut.getArgs()[1], outStream);
					outStream.add(new IRInstruction(
								newOp,
								initialOut.getArgs()[0],
								FPRegister.reg_promote,
								initialOut.getArgs()[2])
					);
					return outStream;
				} else {
					IRInstruction out = new IRInstruction(newOp, initialOut.getArgs());
					return Arrays.asList(out);
				}
			}
		} else {
			return Arrays.asList(initialOut);
		}
	}
	
	public static IRCodeArgument convertIntAsmArgToFPArg(IRCodeArgument intArg) {
		if (VarEntry.class.isAssignableFrom(intArg.getClass())) {
			return intArg; //no change needed.
		} else if (IntRegister.class.isAssignableFrom(intArg.getClass())) {
			IntRegister in = (IntRegister) intArg;
			FPRegister baseReg = new FPRegister(0);
			return baseReg.newWithIndex(in.registerId);
		} else if (LiteralArgument.class.isAssignableFrom(intArg.getClass())) {
			return intArg;
		} else if (LabelArgument.class.isAssignableFrom(intArg.getClass())) {
			return intArg;
		} else {
			throw new RuntimeException("something wacky happened.");
		}
	}

	/**
	 * if isFP=true
	 * converts an operation with the int operator
	 * but fp allocated registers
	 * to pure fp operation and arguments.
	 */
	public static List<IRInstruction> getBranchInstructionFromBase
			(IRInstruction intBranchInstr, boolean isFP) {
		List<IRInstruction> out = new ArrayList<>();
		if (isFP) {
			Op conditionOp;
			if(fpConditionFlagTrueCodes.contains(intBranchInstr.getOp())) {
				conditionOp = Op.BC1T;
			} else {
				conditionOp = Op.BC1F;
			}
			Op newOp = intFpCorrespondences.get(intBranchInstr.getOp());

			// promote src1 if needed
			FPRegister cmp1;
			if (intBranchInstr.getArgs()[0] instanceof FPRegister) {
				cmp1 = (FPRegister) intBranchInstr.getArgs()[0];
			} else {
				// PROMOTE
				NaiveInstructionSelector.promote(intBranchInstr.getArgs()[0], out);
				cmp1 = FPRegister.reg_promote;
			}

			// promote src2 if needed
			FPRegister cmp2;
		    if (intBranchInstr.getArgs()[1] instanceof FPRegister) {
				cmp2 = (FPRegister) intBranchInstr.getArgs()[1];
			} else {
				// PROMOTE
				NaiveInstructionSelector.promote(intBranchInstr.getArgs()[1], out);
				cmp2 = FPRegister.reg_promote;
			}

			LabelArgument jmp = (LabelArgument) intBranchInstr.getArgs()[2];
			out.add(new IRInstruction(newOp,cmp1,cmp2));
			out.add(new IRInstruction(conditionOp,jmp));
		} else {
			out.add(intBranchInstr);
		}
		return out;
	}

}
