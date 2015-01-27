package cs4240_team1.codegen;

import java.util.List;

import cs4240_team1.Generation;
import cs4240_team1.IRInstruction;
import cs4240_team1.SymbolTable;

/**
 * this functional interface defines the signature for a function which
 * converts the high-level IR from phase 2 to low-level assembly.
 * 
 * The idea is that we will implement this naively, then with CFGs, etc...
 * and you will be able to swap between implementations.
 * 
 * extraArgs doesn't need to be passed for most implementations.
 * 
 * @author vancan1ty
 *
 */
public interface ICodeGenerator {

	List<IRInstruction> generateCode(List<IRInstruction> ir,
			SymbolTable rTable, Generation g, Object ... extraArgs);

}
