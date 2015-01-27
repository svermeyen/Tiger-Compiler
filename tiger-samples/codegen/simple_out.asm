#this is an extremely simple working mips assembly program.
#it represents the goal output for codegen/simple.tig
	.data
_t0:	.word	0 
_t1:	.word   0
_t2:	.word   0
_a0:	.word	0 
_b1:	.word	0
_c2:	.word	0 #reserve space for all of our variables. 


	.text
main:
	li $8, 2
	sw $8, _t0 #hi there everyone
	lw $8, _t0
	sw $8, _a0
	li $8, 5
	sw $8, _t1
	lw $8, _t1
	sw $8, _b1
	lw $8, _a0
	lw $9, _b1
	add $10, $8, $9
	sw $10, _t2
	lw $8, _t2
	sw $8, _c2
	jr $ra
