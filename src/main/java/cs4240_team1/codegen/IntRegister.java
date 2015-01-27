package cs4240_team1.codegen;

import cs4240_team1.IRCodeArgument;
import cs4240_team1.TigerType;

import java.util.LinkedList;
import java.util.List;

public class IntRegister extends Register implements IRCodeArgument {

	public int registerId;

	/* all of the registers along with their MIPS names */
	public static IntRegister reg_zero = new IntRegister(0);
	public static IntRegister reg_at = new IntRegister(1);
	public static IntRegister reg_v0 = new IntRegister(2);
	public static IntRegister reg_v1 = new IntRegister(3);
	public static IntRegister reg_a0 = new IntRegister(4);
	public static IntRegister reg_a1 = new IntRegister(5);
	public static IntRegister reg_a2 = new IntRegister(6);
	public static IntRegister reg_a3 = new IntRegister(7);
	public static IntRegister reg_t0 = new IntRegister(8);
	public static IntRegister reg_t1 = new IntRegister(9);
	public static IntRegister reg_t2 = new IntRegister(10);
	public static IntRegister reg_t3 = new IntRegister(11);
	public static IntRegister reg_t4 = new IntRegister(12);
	public static IntRegister reg_t5 = new IntRegister(13);
	public static IntRegister reg_t6 = new IntRegister(14);
	public static IntRegister reg_t7 = new IntRegister(15);
	public static IntRegister reg_s0 = new IntRegister(16);
	public static IntRegister reg_s1 = new IntRegister(17);
	public static IntRegister reg_s2 = new IntRegister(18);
	public static IntRegister reg_s3 = new IntRegister(19);
	public static IntRegister reg_s4 = new IntRegister(20);
	public static IntRegister reg_s5 = new IntRegister(21);
	public static IntRegister reg_s6 = new IntRegister(22);
	public static IntRegister reg_s7 = new IntRegister(23);
	public static IntRegister reg_t8 = new IntRegister(24);
	public static IntRegister reg_t9 = new IntRegister(25);
	public static IntRegister reg_k0 = new IntRegister(26);
	public static IntRegister reg_k1 = new IntRegister(27);
	public static IntRegister reg_gp = new IntRegister(28);
	public static IntRegister reg_sp = new IntRegister(29);
	public static IntRegister reg_fp = new IntRegister(30);
	public static IntRegister reg_ra = new IntRegister(31);

	/* scratch register for handling type promotion from memory
	 * this is okay because we aren't implementing func calls
	 */
	public static IntRegister reg_promote = reg_a3;


	public IntRegister(int registerId) {
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
		return TigerType.BASE_INT_TYPE;
	}

    public static List<IntRegister> getIntRegisters() {
        List<IntRegister> registers = new LinkedList<>();
        registers.add(IntRegister.reg_t0);
        registers.add(IntRegister.reg_t1);
        registers.add(IntRegister.reg_t2);
        registers.add(IntRegister.reg_t3);
        registers.add(IntRegister.reg_t4);
        registers.add(IntRegister.reg_t5);
        registers.add(IntRegister.reg_t6);
        registers.add(IntRegister.reg_s0);
        registers.add(IntRegister.reg_s1);
        registers.add(IntRegister.reg_s2);
        registers.add(IntRegister.reg_s3);
        registers.add(IntRegister.reg_s4);
        registers.add(IntRegister.reg_s5);
        registers.add(IntRegister.reg_s6);
        registers.add(IntRegister.reg_s7);
        return registers;
    }

    public static List<IntRegister> getSpillRegisters() {
        List<IntRegister> registers = new LinkedList<>();
        registers.add(IntRegister.reg_t7);
        registers.add(IntRegister.reg_t8);
        registers.add(IntRegister.reg_t9);
        return registers;
    }
	
	public static String mapRegNumToHumanName(int regNum) {
		switch (regNum) {
		case(0): return "$zero";
		case(1): return "$at";
		case(2): return "$v0";
		case(3): return "$v1";
		case(4): return "$a0";
		case(5): return "$a1";
		case(6): return "$a2";
		case(7): return "$a3";
		case(8): return "$t0";
		case(9): return "$t1";
		case(10): return "$t2";
		case(11): return "$t3";
		case(12): return "$t4";
		case(13): return "$t5";
		case(14): return "$t6";
		case(15): return "$t7";
		case(16): return "$s0";
		case(17): return "$s1";
		case(18): return "$s2";
		case(19): return "$s3";
		case(20): return "$s4";
		case(21): return "$s5";
		case(22): return "$s6";
		case(23): return "$s7";
		case(24): return "$t8";
		case(25): return "$t9";
		case(26): return "$k0";
		case(27): return "$k1";
		case(28): return "$gp";
		case(29): return "$sp";
		case(30): return "$fp";
		case(31): return "$ra";
		default:
			throw new RuntimeException("invalid register number!");
		}
	}

	@Override
	public Register newWithIndex(int index) {
		return new IntRegister(index);
	}
}
