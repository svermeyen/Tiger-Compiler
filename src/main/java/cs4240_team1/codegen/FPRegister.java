package cs4240_team1.codegen;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cs4240_team1.IRCodeArgument;
import cs4240_team1.TigerType;

public class FPRegister extends Register implements IRCodeArgument {

	public int registerId;

	public static FPRegister reg_fp0 = new FPRegister(0);
	public static FPRegister reg_fp2 = new FPRegister(2);
	public static FPRegister reg_fp4 = new FPRegister(4);
	public static FPRegister reg_fp6 = new FPRegister(6);
	public static FPRegister reg_fp8 = new FPRegister(8);
	public static FPRegister reg_fp10 = new FPRegister(10);
	public static FPRegister reg_fp12 = new FPRegister(12);
	public static FPRegister reg_fp14 = new FPRegister(14);
	public static FPRegister reg_fp16 = new FPRegister(16);
	public static FPRegister reg_fp18 = new FPRegister(18);
	public static FPRegister reg_fp20 = new FPRegister(20);
	public static FPRegister reg_fp22 = new FPRegister(22);
	public static FPRegister reg_fp24 = new FPRegister(24);
	public static FPRegister reg_fp26 = new FPRegister(26);
	public static FPRegister reg_fp28 = new FPRegister(28);
	public static FPRegister reg_fp30 = new FPRegister(30);
	public static FPRegister reg_promote = reg_fp30;

	public FPRegister(int registerId) {
		if(registerId % 2 != 0) {
			throw new RuntimeException("cannot create an odd numbered"
					+ " fp register! (see wikipedia)");
		} 
		this.registerId = registerId;
	}

	@Override
	public String getTextForIR() {
		return mapRegNumToHumanName(registerId);
	}

	@Override
	public void setTextForIR(String id) {
		return;
	}

	@Override
	public TigerType getTigerType() {
		return TigerType.BASE_FIXEDPT_TYPE;
	}

	/**
	 * Omits reserved $f0, $f12, as well as spills.
	 *
	 * $f0, $f12 are used for input/output by qtspim.
	 */
	public static List<FPRegister> getFPRegisters() {
		List<FPRegister> registers = new LinkedList<>(
				Arrays.asList(new FPRegister[] {
				         // $f0 reserved by qtspim
						 FPRegister.reg_fp2,
						 FPRegister.reg_fp4,
						 FPRegister.reg_fp6,
						 FPRegister.reg_fp8,
						 FPRegister.reg_fp10,
					     // $f12 reserved by qtspim
						 FPRegister.reg_fp14,
						 FPRegister.reg_fp16,
						 FPRegister.reg_fp18,
						 FPRegister.reg_fp20,
						 FPRegister.reg_fp22,
					     // $f24,26,28 are spills
						 // $f30 is used for type promotion
						 }));

		return registers;
	}

	public static List<FPRegister> getSpillRegisters() {
		List<FPRegister> registers = new LinkedList<>();
		registers.add(FPRegister.reg_fp24);
		registers.add(FPRegister.reg_fp26);
		registers.add(FPRegister.reg_fp28);
		return registers;
	}

	public static String mapRegNumToHumanName(int regNum) {
		return "$f" + regNum;
	}

	/** we multiply by two so that we can create IntRegs and FPRegs with
	 * the same logic, but the FPRegs will have an even index. note that the 
	 * max input is 15!
	 */
	@Override
	public Register newWithIndex(int index) {
		assert (0 <= index && index <= 15);
		return new FPRegister(index*2);
	}

}
