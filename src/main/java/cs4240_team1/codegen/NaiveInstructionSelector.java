package cs4240_team1.codegen;

import java.util.ArrayList;
import java.util.List;

import cs4240_team1.CommentArgument;
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
import cs4240_team1.TypeClass;

/**
 * intended to follow up NaiveRegisterAllocator, or a similar register
 * allocator.
 * 
 * @author vancan1ty
 *
 */
public class NaiveInstructionSelector implements ICodeGenerator {

	/**
	 * extraArgs is not needed.
	 */
	@Override
	public List<IRInstruction> generateCode(
			List<IRInstruction> irCode, SymbolTable rTable, Generation g, 
			Object ... extraArgs ) {
		
		List<IRInstruction> outCode = new ArrayList<>();
		for (IRInstruction i : irCode) {
			outCode.addAll(
					NaiveInstructionSelector.IROperationToCode(i,
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
		if(IRComment.class.isAssignableFrom(irLine.getClass())) { 
			//then we have a comment.  append and return.
			out.add(irLine);
			return out;
		}

		boolean isFP = false;
		if (irLine.getArgs().length > 0) {
			if(!CodeGenerationUtility.isCodeArgumentAnInt(irLine.getArgs()[0])) {
				isFP = true;
			}
		}
		

		switch (irLine.getOp()) {
		case LW:  {
			if (irLine.getArgs()[0].getTigerType().needsFPRegister()) {
				irLine = new IRInstruction(Op.LWC1, irLine.getArgs());
			}
			out.add(irLine);
			break;
		}
		case SW: {
			if (irLine.getArgs()[0].getTigerType().needsFPRegister()) {
				irLine = new IRInstruction(Op.SWC1, irLine.getArgs());
			}
			out.add(irLine);
			break;
		}
		case LI: {
			if (irLine.getArgs()[0].getTigerType().needsFPRegister()) {
				irLine = new IRInstruction(Op.LI_S, irLine.getArgs());
			}
			out.add(irLine);
			break;
		}
		case LWC1:{
			if (irLine.getArgs()[1].getTigerType().needsIntRegister()) {
				// type promote!
				// we have to load from mem into an int reg, first...
				out.add(new IRInstruction(Op.LW, IntRegister.reg_promote, irLine.getArgs()[1]));
				promote(IntRegister.reg_promote, out);
				// it's in memory now, so simply "load" using a MOVE instruction
				out.add(new IRInstruction(Op.MOV_S, irLine.getArgs()[0], FPRegister.reg_promote));
			} else {
				out.add(irLine); // pass through unharmed.
			}
			break;
		} 
		case SWC1: {
			if (irLine.getArgs()[0].getTigerType().needsIntRegister()) {
				// type promote!
				promote(irLine.getArgs()[0], out);
				out.add(new IRInstruction(irLine.getOp(), FPRegister.reg_promote, irLine.getArgs()[1]));
			} else {
				out.add(irLine); // pass through unharmed.
			}
			break;
		} 
		case LI_S: {
			out.add(irLine); // pass through unharmed.
			break;
		}
		case ASSIGNVAR: {
			if (irLine.getArgs()[0].getTigerType().needsFPRegister()) {
				if (irLine.getArgs()[1].getTigerType().needsFPRegister()) {
					out.add(new IRInstruction(Op.MOV_S, irLine.getArgs()));
				} else {
					/* type promotion!! */
					promote(irLine.getArgs()[1], out);
					out.add(new IRInstruction(Op.MOV_S, irLine.getArgs()[0], FPRegister.reg_promote));
				}
			} else {
				out.add(new IRInstruction(Op.MOVE, irLine.getArgs()));
			}
			break;
		}
		case ASSIGNLIT: {
			if (irLine.getArgs()[0].getTigerType().needsFPRegister()) {
				out.add(new IRInstruction(Op.LI_S, irLine.getArgs()));
			} else {
				out.add(new IRInstruction(Op.LI, irLine.getArgs()));
			}
			break;
		}
		case ASSIGNARR: {
			//takes in modified ASSIGNARR instruction with
			//FIVE arguments (first symbol, rest registers)
			//arrayBaseSymbol, loopBoundRegister, valToAssignRegister, 
			//arrayLocationRegister

			//is there a way to do this without a loop?
			//sample mips loop taken from
			//http://courses.cs.washington.edu/courses/cse410/99au/lectures/Lecture-09-29/sld026.htm
			//get the size of the array
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, 
					VarEntry.class, 0);
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, 
					IntRegister.class, 1,3); // only size and scratch reg must be ints

			//note that array stop bound and val to assign are
			//loaded into registers during register allocation!
			LabelArgument loopStartLabel = g.newLabel1();
			LabelArgument loopExitLabel = g.newLabel1();
			VarEntry arrayBaseSymbol = (VarEntry) irLine.getArgs()[0];
			IntRegister loopBoundRegister =  (IntRegister) irLine.getArgs()[1]; 
			Register valToAssignRegister =  (Register) irLine.getArgs()[2]; 
			IntRegister arrayLocationRegister =  (IntRegister) irLine.getArgs()[3]; 

			//zero the array indexer
			out.add(new IRInstruction(Op.MOVE, arrayLocationRegister, IntRegister.reg_zero));



			out.add(new IRInstruction(Op.LABEL, loopStartLabel, new CommentArgument("loopStartLabel")));
			out.add(new IRInstruction(Op.BEQ, IntRegister.reg_zero, loopBoundRegister, loopExitLabel));

			if (valToAssignRegister.getTigerType().needsFPRegister()) {
				out.add(new IRInstruction(Op.SWC1, valToAssignRegister, arrayBaseSymbol, arrayLocationRegister));
			} else if (arrayBaseSymbol.getTigerType().typeClass == TypeClass.ARRAY_FIXEDPT) {
				// we need to promote!  value is int, but array is fixedpt!
				promote(valToAssignRegister, out);
				out.add(new IRInstruction(Op.SWC1, FPRegister.reg_promote, arrayBaseSymbol, arrayLocationRegister));
			} else {
				out.add(new IRInstruction(Op.SW, valToAssignRegister, arrayBaseSymbol, arrayLocationRegister));
			}

			//increment the array ptr by 4.
			//all arrays hold 32-bit elements, correct?
			out.add(new IRInstruction(Op.ADDI, arrayLocationRegister, 
					arrayLocationRegister,	 
					new LiteralArgument("4", TigerType.LITERAL_INT_TYPE)));

			//decrement loopBoundRegister
			out.add(new IRInstruction(Op.ADDI, loopBoundRegister, loopBoundRegister, 
					new LiteralArgument("-1", TigerType.LITERAL_INT_TYPE)));

			//jump back to the start.
			out.add(new IRInstruction(Op.J, loopStartLabel));

			out.add(new IRInstruction(Op.LABEL, loopExitLabel, new CommentArgument("loopExitLabel")));
			//we're done!  hallelujah.

			break;
		}
		case ADD: 
		case SUB:
		case MULT:
		case DIV:
		case AND:
		case OR:
			/*
			 * must swap order of arguments. 
			 */

			out.addAll(
					IntFPConversion.processIntLineToFinalOutputSelectionLevel(
					new IRInstruction(
					CodeGenerationUtility.convertOpFromIRToMips(irLine.getOp()),
					irLine.getArgs()[2], irLine.getArgs()[0], irLine.getArgs()[1]),
					isFP));

			break;
		case GOTO: //convert from GOTO to j instruction.
			out.add(new IRInstruction(Op.J, irLine.getArgs()[0]));
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
					IntFPConversion.processIntLineToFinalOutputSelectionLevel(
					new IRInstruction(
					CodeGenerationUtility.convertOpFromIRToMips(irLine.getOp()),
					irLine.getArgs()[0], irLine.getArgs()[1], irLine.getArgs()[2]),
					isFP));

			break;
		case RETURN: //transform to jr $ra
			out.add(new IRInstruction(Op.JR, IntRegister.reg_ra));
			break;
		case ARRAYSTORE: { //assign a variable to a given position in an array.
			//takes in a fourth argument: register to use for temp calculation.
			//
			//only index and scratch reg must be int type
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, IntRegister.class,1,3);
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, VarEntry.class, 0);

			IRCodeArgument arrayLoc = irLine.getArgs()[0];
			IRCodeArgument offset = irLine.getArgs()[1];
			IRCodeArgument varToStore = irLine.getArgs()[2];
			IntRegister temp = (IntRegister) irLine.getArgs()[3];

			//using t9 for now.
			//now multiply the offset by four to account for word length.
			//temp will now hold the actual byte offset
			out.add(new IRInstruction(Op.LI, temp, 
					new LiteralArgument("4",null)));
			out.add(new IRInstruction(Op.MUL, temp, temp, offset));

			//now store into arrayloc+offset
			if (varToStore.getTigerType().needsFPRegister()) {
				out.add(new IRInstruction(Op.SWC1, varToStore , arrayLoc, temp));
			} else if (arrayLoc.getTigerType().typeClass == TypeClass.ARRAY_FIXEDPT) {
				// we need to promote!  value is int, but array is fixedpt!
				promote(varToStore, out);
				out.add(new IRInstruction(Op.SWC1, FPRegister.reg_promote, arrayLoc, temp));
			} else {
				out.add(new IRInstruction(Op.SW, varToStore , arrayLoc, temp));
			}
			break;
		} case ARRAYLOAD: { //get a value out of arrayloc+index, store it to a variable.
			//takes in a fourth argument: register to use for temp calculation.
			//
			//only index and scratch reg must be int type
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, IntRegister.class, 2,3);
			CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, VarEntry.class, 1);
			IRCodeArgument varToStoreTo = irLine.getArgs()[0];
			IRCodeArgument arrayLoc = irLine.getArgs()[1];
			IRCodeArgument offset = irLine.getArgs()[2];
			IntRegister temp = (IntRegister) irLine.getArgs()[3];
			
			//now multiply the offset by four to account for word length.
			out.add(new IRInstruction(Op.LI, temp, 
					new LiteralArgument("4",null)));
			out.add(new IRInstruction(Op.MUL, temp, 
						temp, offset));

			//now get from arrloc+offset into a register.
			if (varToStoreTo.getTigerType().needsFPRegister()) {
				out.add(new IRInstruction(Op.LWC1, varToStoreTo, arrayLoc, temp));
			} else {
				out.add(new IRInstruction(Op.LW, varToStoreTo, arrayLoc, temp));
			}

			break;
		} case CALL: {
			String funcName = irLine.getArgs()[0].getTextForIR();

			/* this switch is used for std library functions */
			switch (funcName) {
			case "printi": {
				CodeGenerationUtility.assertIndicatedArgsAreOfGivenType(irLine, IntRegister.class, 1);

				//load the print int syscall num
				out.add(new IRInstruction(Op.LI, IntRegister.reg_v0, 
						new LiteralArgument("1",TigerType.LITERAL_INT_TYPE))); 

				//load the value to print into reg_a0
				out.add(new IRInstruction(Op.MOVE, IntRegister.reg_a0, irLine.getArgs()[1]));
				out.add(new IRInstruction(Op.SYSCALL));

				//results are much more readable with newlines between them,
				//so I print newlines after printi.
				out.addAll(getInstructionsToPrintNewline());

				break;
			} case "flush": { //I am using flush to print an empty line.
				out.addAll(getInstructionsToPrintNewline());
				break;
			} case "printf": { 
				//load the print float syscall num
				out.add(new IRInstruction(Op.LI, IntRegister.reg_v0, 
						new LiteralArgument("2",TigerType.LITERAL_INT_TYPE))); 

				//load the value to print into reg_fp12
				if (irLine.getArgs()[1].getTigerType().needsFPRegister()) {
					out.add(new IRInstruction(Op.MOV_S, FPRegister.reg_fp12, irLine.getArgs()[1]));
				} else {
					// type promotion!
					promote(irLine.getArgs()[1], out);
					out.add(new IRInstruction(Op.MOV_S, FPRegister.reg_fp12, FPRegister.reg_promote));
				}
				out.add(new IRInstruction(Op.SYSCALL));

				out.addAll(getInstructionsToPrintNewline());
				break;
			}
			
			default: {
				out.add(new IRComment("CALL: function calls not supported yet!"));
				break;
			}
			}
			break;
		} case CALLR: {
			String funcName = irLine.getArgs()[1].getTextForIR();

			/* this switch is used for std library functions */
			switch (funcName) {
			case "geti": {
				//load the read int syscall num
				out.add(new IRInstruction(Op.LI, IntRegister.reg_v0, 
						new LiteralArgument("5",TigerType.LITERAL_INT_TYPE))); 

				//read from stdin -- result will be in $v0
				out.add(new IRInstruction(Op.SYSCALL));

				//move to dst
				if (irLine.getArgs()[0].getTigerType().needsFPRegister()) {
					// promote!
					promote(IntRegister.reg_v0, out);
					out.add(new IRInstruction(Op.MOV_S, irLine.getArgs()[0], FPRegister.reg_promote));
				} else {
					out.add(new IRInstruction(Op.MOVE, irLine.getArgs()[0], IntRegister.reg_v0));
				}

				break;
			} case "getf": { 
				//load the read float syscall num
				out.add(new IRInstruction(Op.LI, IntRegister.reg_v0, 
						new LiteralArgument("6",TigerType.LITERAL_INT_TYPE))); 

				//read from stdin -- result will be in $f0
				out.add(new IRInstruction(Op.SYSCALL));

				//move to dst
				out.add(new IRInstruction(Op.MOV_S, irLine.getArgs()[0], FPRegister.reg_fp0));
				break;
			}
			
			default: {
				out.add(new IRComment("CALLR: function calls not supported yet!"));
				break;
			}
			}
			break;
		} case LABEL: {
			out.add(irLine); //no alterations necesary.
			break;
		} default: {
			throw new RuntimeException("unsupported op coming into instruction"
					+ " selection phase!\nHere is the problematic line:\n"
					+ irLine);
		}
		}

		return out;
	}

	/** Read the method body if you want to use this!! */
	public static void promote(IRCodeArgument intArg, List<IRInstruction> out) {
		out.add(new IRInstruction(
					Op.MTC1,
					intArg,
					FPRegister.reg_promote)
		);
		out.add(new IRInstruction(
					Op.CVT_S_W,
					FPRegister.reg_promote,
					FPRegister.reg_promote)
		);
	}
	
	public static List<IRInstruction> getInstructionsToPrintNewline() {
		List<IRInstruction> out = new ArrayList<>();
		out.add(new IRInstruction(Op.LI, IntRegister.reg_v0,
				new LiteralArgument("4", TigerType.LITERAL_INT_TYPE)));
		out.add(new IRInstruction(Op.LA, IntRegister.reg_a0,
				new LiteralArgument("___linefeed", null)));
		out.add(new IRInstruction(Op.SYSCALL));
		return out;
	}

}
