package cs4240_team1;

public enum Op {
	ASSIGNVAR,
	ASSIGNARR,
	ASSIGNLIT,
	ADD,
	SUB,
	MULT,
	DIV,
	AND,
	OR,
	GOTO,
	BREQ,
	BRNEQ,
	BRLT,
	BRGT,
	BRGEQ,
	BRLEQ,
	RETURN,
	ARRAYSTORE,
	ARRAYLOAD,
	CALL,
	CALLR,
	LABEL,

	//THE BELOW OPS ARE MEANT TO BE USED FOR LOW-LEVEL REPRESENTATION!
	//MIPS INSTRUCTIONS
	LW,  //$t, C($s)
	SW,  //$t, C($s)
	BEQ, //beq $s,$t,C  equivalent to BREQ in high-level representation
	LA,  //$rd, LabelAddr  load label/variable into register.
	LI,  // register_destination, value load immediate.
	MOVE,// copy a value from one register to another.
	BGE,  // $t0, $t1, exitLabel   break if greater than or equal to
	ADDI, // $t0, $t1, val   add immediate
	J,    // C goto label (jump to offset).
	JR,   // jump to register. 
	SYSCALL, //implemented by SPIM, is how you print to console...
	BNE,  //equivalent to BRNEQ. 
	BLT,  //equivalent to BRLEQ
	BGT,  //equivalent to BRGEQ
	BLE,  //equivalent to BRLEQ
	MUL,  //Rd=Rs*Rt 
	
	//floating point stuff
	LWC1, //same as LW but for fp
	SWC1, //same as SW but for fp
	LI_S,
	MOV_S,
	S_S,
	ADD_S,
	SUB_S,
	MUL_S,
	DIV_S,
	AND_S,
	OR_S,
	
	C_EQ_S, //note breq equiv instructions get translated into TWO
	//C_NE_S, //instructions for fp breaks.
	C_LT_S,
	C_LE_S,
	//C_GT_S,
	//C_GE_S,
	
	//type promotion stuff
	//to promote int to fp:
	//
	// mtc1 $int $fp
	// cvt.s.w $fp $fp
	//
	// $fp will hold promoted value
	MTC1,
	CVT_S_W,
	
	BC1T,
	BC1F
	;

	@Override
	public String toString() {
		if (this == Op.ASSIGNVAR || this == Op.ASSIGNARR || this == ASSIGNLIT) {
			return "assign";
		} else if (this == Op.ARRAYSTORE) {
			return "array_store";
		} else if (this == Op.ARRAYLOAD) {
			return "array_load";
		} 
		return super.toString().toLowerCase().replace("_", ".");
	}
}

/* BELOW ARE MIPS INSTRUCTIONS FROM WIKIPEDIA WHICH WE MAY ADD TO THIS ENUM */

//INTEGER

//ADD, ADDU, SUB, SUBU, ADDI, ADDIU, MULTU, DIV, DIVU, LW, LH, LHU, LB, LBU, 
//SW, SH, SB, LUI, MFHI, MFLO, MFCZ, MTCZ, AND, ANDI, OR, ORI, XOR, NOR, SLT,
//SLTI, SLL, SRL, SRA, SILV, SRLV, SRAV, BEQ, BNE, J, JR, JAL


//FP
/*
add.s, // $x,$y,$z	$x = $y + $z				Floating-Point add (single precision)
sub.s, // $x,$y,$z	$x = $y - $z				Floating-Point subtract (single precision)
mul.s, // $x,$y,$z	$x = $y * $z				Floating-Point multiply (single precision)
div.s, // $x,$y,$z	$x = $y / $z				Floating-Point divide (single precision)
add.d, // $x,$y,$z	$x = $y + $z				Floating-Point add (double precision)
sub.d, // $x,$y,$z	$x = $y - $z				Floating-Point subtract (double precision)
mul.d, // $x,$y,$z	$x = $y * $z				Floating-Point multiply (double precision)
div.d, // $x,$y,$z	$x = $y / $z				Floating-Point divide (double precision)
lwcZ, // $x,CONST ($y)	Coprocessor[Z].DataRegister[$x] = Memory[$y + CONST]	I			Loads the 4 byte word stored from: MEM[$y+CONST] into a Coprocessor data register. Sign extension.
swcZ, // $x,CONST ($y)	Memory[$y + CONST] = Coprocessor[Z].DataRegister[$x]	I			Stores the 4 byte word held by a Coprocessor data register into: MEM[$y+CONST]. Sign extension.
c.lt.s, // $f2,$f4	if ($f2 < $f4) cond=1; else cond=0				Floating-point compare less than single precision 
	    // there are also other things you can substitute for lt.
c.lt.d, // $f2,$f4	if ($f2 < $f4) cond=1; else cond=0				Floating-point compare less than double precision 
        // there are also other things you can substitute for lt
bc1t, // 100	if (cond == 1) go to PC+4+100				PC relative branch if FP condition
bc1f, // 100	if (cond == 0) go to PC+4+100				PC relative branch if not condition
*/

//pseudoinstructions
/* 
Move	move $rt,$rs	add $rt,$rs,$zero	R[rt]=R[rs]
Clear	clear $rt	add $rt,$zero,$zero	R[rt]=0
Not	not $rt, $rs	nor $rt, $rs, $zero	R[rt]=~R[rs]
Load Address	la $rd, LabelAddr	lui $rd, LabelAddr[31:16]; ori $rd,$rd, LabelAddr[15:0]	$rd = Label Address
Load Immediate	li $rd, IMMED[31:0]	lui $rd, IMMED[31:16]; ori $rd,$rd, IMMED[15:0]	$rd = 32 bit Immediate value
Branch unconditionally	b Label	beq $zero,$zero,Label	PC=Label
Branch and link	bal Label	bgezal $zero,Label	R[31]=PC+8; PC=Label
Branch if greater than	bgt $rs,$rt,Label	slt $at,$rt,$rs; bne $at,$zero,Label	if(R[rs]>R[rt]) PC=Label
Branch if less than	blt $rs,$rt,Label	slt $at,$rs,$rt; bne $at,$zero,Label	if(R[rs]<R[rt]) PC=Label
Branch if greater than or equal	bge $rs,$rt,Label	slt $at,$rs,$rt; beq $at,$zero,Label	if(R[rs]>=R[rt]) PC=Label
Branch if less than or equal	ble $rs,$rt,Label	slt $at,$rt,$rs; beq $at,$zero,Label	if(R[rs]<=R[rt]) PC=Label
Branch if greater than unsigned	bgtu $rs,$rt,Label	sltu $at,$rt,$rs; bne $at,$zero,Label	if(R[rs]>R[rt]) PC=Label
Branch if greater than zero	bgtz $rs,Label	slt $at,$zero,$rs; bne $at,$zero,Label	if(R[rs]>0) PC=Label
Branch if equal to zero	beqz $rs,Label	beq $rs,$zero,Label	if(R[rs]==0) PC=Label
Multiplies and returns only first 32 bits	mul $d, $s, $t	mult $s, $t; mflo $d	$d = $s * $t
Divides and returns quotient	div $d, $s, $t	div $s, $t; mflo $d	$d = $s / $t
Divides and returns remainder	rem $d, $s, $t	div $s, $t; mfhi $d	$d = $s % $t 
 */

