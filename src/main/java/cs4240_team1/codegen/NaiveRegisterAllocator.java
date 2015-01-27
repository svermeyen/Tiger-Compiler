package cs4240_team1.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import utility.Utility;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

import cs4240_team1.Generation;
import cs4240_team1.IRCodeArgument;
import cs4240_team1.IRComment;
import cs4240_team1.IRInstruction;
import cs4240_team1.LabelArgument;
import cs4240_team1.LiteralArgument;
import cs4240_team1.Op;
import cs4240_team1.SymbolTable;
import cs4240_team1.TigerType;
import cs4240_team1.VarEntry;
import cs4240_team1.FunctionEntry;

import static cs4240_team1.codegen.IntFPConversion.*;

public class NaiveRegisterAllocator implements ICodeGenerator {
	
	public static void assertIndicatedArgsAreVarEntries(IRInstruction irLine, 
			int ... indices){
		CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, 
				VarEntry.class, indices);
	}

	/** implements the naive register allocation strategy
	 * extraArgs is not needed.. */
	public List<IRInstruction> generateCode(
			List<IRInstruction> irCode, SymbolTable rTable, Generation g,
			Object ... extraArgs) {
		
		List<IRInstruction> outCode = new ArrayList<>();
		for (IRInstruction i : irCode) {
			outCode.addAll(
					NaiveRegisterAllocator.IROperationToCode(i,
							rTable, g));
		}
		return outCode;
	}
	

	

	/**
	 * convert an IRInstruction into a set of closer-to-the-metal IRInstructions
	 * which should be proper MIPS assembly (at least when we are done with this).
	 * 
	 * [CB 11/22/14]
	 * NOTE if someone has the wherewithal to design a proper object hierarchy
	 * to encapsulate the difference between IRInstructions and CodeInstructions
	 * that might be good.  I am just reusing IRInstructions for now because
	 * converting between the two involved all sorts of nasty type casting 
	 * and duplication of functionality!
	 * 
	 * @param irLine the instruction to convert
	 * @param rTable real symbol table from symbol table generation phase.
	 * @param tempTable temp symbol table from the ir codegen phase.
	 * @return the ir instruction converted into assembly code...
	 */
	public static List<IRInstruction> 
	IROperationToCode(IRInstruction irLine, SymbolTable rTable, Generation g) {

		/* we will append to this list of code instructions, then return it */
		List<IRInstruction> out = new ArrayList<IRInstruction>();
		out.add(new IRComment(irLine.getOp().toString()+": " + irLine.toString()));
		
		boolean isFP = false;
		if (irLine.getArgs().length > 0) {
			if(!CodeGenerationUtility.isCodeArgumentAnInt(irLine.getArgs()[0])) {
				isFP = true;
			}
		}

		switch (irLine.getOp()) {
		case ASSIGNVAR: { //load from one variable into another.
			assertIndicatedArgsAreVarEntries(irLine, 0,1);
			VarEntry varToAssignFrom = (VarEntry) irLine.getArgs()[1];
			VarEntry varToAssignTo = (VarEntry) irLine.getArgs()[0];
			Register tempStorage = IntRegister.reg_t0;
			out.addAll(
					processIntLineToFinalOutputRegAllocLevel(
					new IRInstruction(Op.LW, tempStorage, varToAssignFrom),
					isFP));

			out.addAll(
					processIntLineToFinalOutputRegAllocLevel(
					new IRInstruction(Op.SW, tempStorage, varToAssignTo),
					isFP));
					
			break;
		} case ASSIGNARR: {
			//we don't really do much for this in this phase.  Because we 
			//keeping a couple of free registers around to play with doing
			//assembly generation, we generate the assignment loop there.
			assertIndicatedArgsAreVarEntries(irLine, 0,1,2);

			//we don't use the passed in size variable as we can compute
			//the size directly at compile-time!
			Integer dim1 = ((VarEntry) irLine.getArgs()[0]).getTigerType().dimension1;
			Integer dim2 = ((VarEntry) irLine.getArgs()[0]).getTigerType().dimension2;
			int stopBound; 
			if(dim2 == null) {
				stopBound = dim1;
			} else { //account for 2d arrays.
				stopBound = dim1*dim2;
			}


			out.addAll(
					getGenericNaiveAllocInstructions(irLine, 
							new int[]{1,2}, 
							new int[]{1,2},
							new int[]{0}, 
							null, null,
							new Register[]{IntRegister.reg_t8},
							new int[]{1}));
			//^emits the modified ASSIGNARR instruction with
			//FIVE arguments this time
			//arrayBaseSymbol, loopBoundRegister, valToAssignRegister, 
			//arrayLocationRegister, counterRegister
			//[CB TODO] refactor the code generation so that only four args 
			//are neede.

			break;
		}
		case ASSIGNLIT: { //assign a literal to a variable.
			assertIndicatedArgsAreVarEntries(irLine, 0);
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, LiteralArgument.class, 1);
			if(!CodeGenerationUtility.isCodeArgumentAnInt(irLine.getArgs()[1])) {
				isFP = true;
			} 
			
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, 
					LiteralArgument.class, 1);
			out.addAll(
					processIntLineToFinalOutputRegAllocLevel(
					new IRInstruction(Op.LI, IntRegister.reg_t0, irLine.getArgs()[1]),
					isFP));

			out.addAll(
					processIntLineToFinalOutputRegAllocLevel(
					new IRInstruction(Op.SW, IntRegister.reg_t0, irLine.getArgs()[0]),
					isFP));


			break;
			/*[CB 11/23/14] NOTE THAT MIPS ASSEMBLY AND OUR IR CODE PASS
			 * ARGUMENTS IN DIFFERENT ORDER...
			 */
		} 
		case ADD: 
		case SUB:
		case MULT:
		case DIV:
		case AND:
		case OR: {
			/*
			 * all of these take in three variables, assigning the results
			 * of the operation on the first two to the last. 
			 * 
			 * we have to use different registers depending on if fp or integer.
			 */
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, 
					VarEntry.class, 0,1);
			out.addAll(
					getGenericNaiveAllocInstructions(irLine, 
							new int[]{0,1,2}, 
							new int[]{0,1},
							new int[]{}, 
							2, 2,
							null,
							null));

			/* in mips, you assign to the first variable, use the second
			 * two as operands. but we will do this translation in the next 
			 * phase!
			 */
			break;
		}
		case GOTO: //nothing to do in this phase.
			out.add(irLine);
			break;
			//the break instructions take in two variables, go to label on equal.
		case BREQ:  
		case BRNEQ:
		case BRLT:
		case BRGT:
		case BRGEQ:
		case BRLEQ:
			//all of the break statements require the same transformation.
			out.addAll(
					getGenericNaiveAllocInstructions(irLine, 
							new int[]{0,1}, 
							new int[]{0,1}, 
							new int[]{2}, 
							null, null,
							null,
							null));
			break;
		case RETURN: 
			out.add(irLine);
			break;
		case ARRAYSTORE: { //assign a variable to a given position in an array.
			//expects that both the variable and the offset are given as
			//variables, not literals.
			assertIndicatedArgsAreVarEntries(irLine, 0,1,2);
			out.addAll(
					getGenericNaiveAllocInstructions(irLine, 
							new int[]{1,2}, 
							new int[]{1,2}, 
							new int[]{0}, 
							null, null,
							new Register[]{IntRegister.reg_t9},
							new int[]{1}
					)
					);
			break;
		} case ARRAYLOAD: { //get a value out of arrayloc+index, store it to a variable.
			//once again, expects all inputs to be temp variables.
			assertIndicatedArgsAreVarEntries(irLine, 0,1,2);
			out.addAll(
					getGenericNaiveAllocInstructions(irLine, 
							new int[]{0,2}, 
							new int[]{0,2}, 
							new int[]{1}, 
							0, 0,
							new Register[]{IntRegister.reg_t9},
							new int[]{2}
					)
					);
			break;
		} case CALL: {  //only supports printi/printf
			assertIndicatedArgsAreVarEntries(irLine, 1);
			if (!irLine.getArgs()[0].getTextForIR().equals("printi")
					&& !irLine.getArgs()[0].getTextForIR().equals("printf")) {
				break; //TODO a better way of handling this than silently ignoring??
			}

			out.addAll(
					getGenericNaiveAllocInstructions(irLine,
						new int[]{1}, // val to print needs a reg
						new int[]{1}, // val to print needs a LD
						new int[]{0}, // func name is preserved
						null, null, null, // no SW or additional regs
						null)  
					);
			break;
		} case CALLR: {
			// this will be flagged as unsupported in selector
			// TODO support geti() and getf()???
			assertIndicatedArgsAreVarEntries(irLine, 0);
			if (!irLine.getArgs()[1].getTextForIR().equals("geti")
					&& !irLine.getArgs()[1].getTextForIR().equals("getf")) {
				break; //TODO a better way of handling this than silently ignoring??
			}

			out.addAll(
					getGenericNaiveAllocInstructions(irLine,
						new int[]{0}, // var to load into needs a reg
						new int[]{},  // nothing is loaded... no params
						new int[]{1}, // func name is preserved
						0,  // need to store into 0 arg
						0,  // symbol is just the var name that we want to store to
						null, // no additional regs
						null)  
					);

			break;
		} case LABEL: {
			out.add(irLine); //no alterations necesary.
			break;
		} default: {
			throw new RuntimeException("something wacky happened");
		}
		}

		return out;
	}

	public static List<IRInstruction> getGenericNaiveAllocInstructions(IRInstruction irLine,
			int[] indicesToPassAsRegs, int[]regIndicesToLoadTo, 
			int[] indicesToPassAsSymbols, Integer regIndexToUseInSW,
			 Integer symIndexToUseInSW,
			 Register[] additionalRegsToPass,
			 int[] regIndicesToLockAsIntRegs) {
		List<IRInstruction> out = new ArrayList<IRInstruction>();
		Op loadInstruction;
		Op storeInstruction;
		Register baseReg;
		int regOffset = 8;  //offset starting register to t0!

		/* Max: added the second test for printi/printf reasons --- arg[0] in that case is
		 * a function entry, and we need to test arg[1]
		 *
		 * XXX if we support geti() or getf(), this logic will need to change to avoid
		 * out of bounds errors (because these calls would have zero args)
		 */
		if(CodeGenerationUtility.isCodeArgumentAnInt(irLine.getArgs()[0])
				|| (irLine.getArgs()[0] instanceof FunctionEntry
					&& CodeGenerationUtility.isCodeArgumentAnInt(irLine.getArgs()[1]))) {
			loadInstruction = Op.LW;
			storeInstruction = Op.SW;
			baseReg = new IntRegister(0);
		} else {
			loadInstruction = Op.LWC1;
			storeInstruction = Op.SWC1;
			baseReg = new FPRegister(0);
		}
		
		//do the loads.
		for (int i : regIndicesToLoadTo) {

			if (regIndicesToLockAsIntRegs != null
					&& Ints.asList(regIndicesToLockAsIntRegs).contains(i)) {
				out.add(new IRInstruction(Op.LW, new IntRegister(i
						+ regOffset), irLine.getArgs()[i]));
			} else {
				out.add(new IRInstruction(loadInstruction, baseReg
						.newWithIndex(i + regOffset), irLine.getArgs()[i]));
			}

		}
		
		IRCodeArgument[] iArgs = irLine.getArgs();
		//build stuff to pass in core instruction.
		IRCodeArgument[] oArgs; 
		if (additionalRegsToPass != null) {
			oArgs = new IRCodeArgument[iArgs.length+additionalRegsToPass.length];
			for (int i = 0; i < additionalRegsToPass.length; i++) {
				oArgs[iArgs.length+i]=additionalRegsToPass[i];
			}
		} else {
			oArgs = new IRCodeArgument[iArgs.length];
		}
		for (int i = 0; i < iArgs.length; i++) {
			if(Ints.asList(indicesToPassAsRegs).contains(i)) {
				if (regIndicesToLockAsIntRegs != null && 
						Ints.asList(regIndicesToLockAsIntRegs).contains(i)) {
					oArgs[i] = new IntRegister(i+regOffset);
				} else {
					oArgs[i] = baseReg.newWithIndex(i+regOffset);
				}
			} else if(Ints.asList(indicesToPassAsSymbols).contains(i)) {
				oArgs[i] = iArgs[i]; //then we can pass unchanged.
			} else {
				assert false;
			}
		}
		
		
		//output the core instruction.
		out.add(new IRInstruction(irLine.getOp(), oArgs));
		
		//now, output store instructions.
		if (regIndexToUseInSW != null) {
			if (regIndicesToLockAsIntRegs != null
					&& Ints.asList(regIndicesToLockAsIntRegs).contains(
							regIndexToUseInSW)) {
				out.add(new IRInstruction(Op.SW, new IntRegister(
						regIndexToUseInSW + regOffset),
						iArgs[symIndexToUseInSW]));
			} else {

				out.add(new IRInstruction(storeInstruction, baseReg
						.newWithIndex(regIndexToUseInSW + regOffset),
						iArgs[symIndexToUseInSW]));
			}
		}

		for (IRInstruction i : out) {
			for (IRCodeArgument arg : i.getArgs()) {
				assert(arg != null);
			}
		}

	return out;
	}
}
