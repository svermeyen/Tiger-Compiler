package cs4240_team1.codegen;

import cs4240_team1.IRCodeArgument;

/**
 * this is used to get "algebraic data type" functionality out of
 * IntRegister and FPRegister
 * (they both extend this).
 * 
 * @author vancan1ty
 *
 */
public abstract class Register implements IRCodeArgument {

	public abstract Register newWithIndex(int index); 

}
