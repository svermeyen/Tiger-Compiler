package cs4240_team1.codegen;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
//import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;





import cs4240_team1.Generation;
import cs4240_team1.IRCodeArgument;
import cs4240_team1.IRInstruction;
import cs4240_team1.Op;
import cs4240_team1.SymbolTable;
import cs4240_team1.TigerType;
import cs4240_team1.TypeClass;
import cs4240_team1.VarEntry;

public class CodeGenerationUtility {
	public static void generateProperIRAndPrintToStream(OutputStream out,
			SymbolTable rSymbolTable, 
			Generation g,
			ICodeGenerator registerAllocator,
			ICodeGenerator instructionSelector,
			Object ... extraArgs
			) throws IOException {

		/* platform-independent line separator */
		String lineSep = System.getProperty("line.separator");

		out.write(("#Assembly generated by TIGGER on "
				+ (new Date()).toString() + lineSep).getBytes());

		/* first, we need to output the proper data segment header */
		out.write((".data"+lineSep).getBytes());

		//write our special identifier ___linefeed which we will use to print
		//newlines.
		out.write(("___linefeed:.asciiz    \"\n\"" + lineSep).getBytes());
		
		Iterable<VarEntry> toWrite = 
				Iterables.concat(rSymbolTable.getAllVarEntriesForThisAndKids(),
				g.getTempSymbolTable().getAllVarEntriesForThisAndKids());
		
		for (VarEntry e : toWrite) {
			if (e.getTigerType().isArray()) {
				/* then let's output a .space instruction to make room */
				out.write((e.getUniqueVarId() + ":\t" 
						+ ".align\t 2" + lineSep
						+ ".space\t"
						+ e.getTotalNumberOfElementsForArrayVar()*4 + lineSep)
						.getBytes());
			} else { // let's output a .word instruction to reserve one slot
				out.write((e.getUniqueVarId() + ":\t" + ".word\t" + "0" + lineSep)
						.getBytes());
			}
		}
	
		/* second, we need to output the proper code! */
		out.write((".text"+lineSep).getBytes());
		List<IRInstruction> irCode = g.getCodeList();

		List<IRInstruction> regInstrs = registerAllocator 
				.generateCode(irCode, rSymbolTable, g, extraArgs);

		List<IRInstruction> codeInstrs = instructionSelector
				.generateCode(regInstrs, rSymbolTable, g);

		for (IRInstruction c : codeInstrs) {
			out.write((c.toString() + lineSep).getBytes());
		}
		
	}
	
	/** converts an IR operation like BREQ to its equivalent
	 * mips operation like BEQ.
	 * 
	 * in some cases, reording of instructions/other steps are
	 * also needed.
	 * 
	 * currently implemented for families of instructions which need it...
	 * branching and arithmetic.
	 */
	public static Op convertOpFromIRToMips(Op irOp) {
		switch (irOp) {
		case BREQ:
			return Op.BEQ;
		case BRNEQ:
			return Op.BNE;
		case BRLT:
			return Op.BLT;
		case BRGT:
			return Op.BGT;
		case BRGEQ:
			return Op.BGE;
		case BRLEQ:
			return Op.BLE;
		case ADD: 
			return Op.ADD;
		case SUB:
			return Op.SUB;
		case MULT:
			return Op.MUL;
		case DIV:
			return Op.DIV;
		case AND:
			return Op.AND;
		case OR:
			return Op.OR;
		default:
			throw new RuntimeException(irOp + " is not a supported"
					+ " branching operation to convert! ");
		}
	}

	/**
	 * given an IRInstruction, assert that its arguments in the given
	 * positions are of the given type. (otherwise throw an exception).
	 * 
	 * @param irLine
	 * @param indices
	 */
	public static void assertIndicatedArgsAreOfGivenType(IRInstruction irLine, 
			Class type, int ... indices){
		for (int i : indices) {
			if(!(type.isAssignableFrom(irLine.getArgs()[i].getClass()))) {
				throw new RuntimeException("non-" + type.getSimpleName() + 
						" argument: invalid IR at \n " + irLine);
			}
		}
	}
	
	public static boolean isCodeArgumentAnInt(IRCodeArgument ir) {
		if(ir.getTigerType() == null) { //by default we say that everything
			//is an 'int' for the purposes of instruction selection.
			return true;
		}
		TypeClass[] intTypes = new TypeClass[]{
				TypeClass.BASE_INT, TypeClass.ARRAY_INT, TypeClass.LITERAL_INT,
				};
		if (Arrays.asList(intTypes).contains(ir.getTigerType().getTypeClass())) {
			return true;
		} else {
			return false;
		}
	}
}