package cs4240_team1.codegen;

import java.util.List;

import cs4240_team1.Generation;
import cs4240_team1.IRInstruction;
import cs4240_team1.SymbolTable;

public class BasicBlockRegisterAllocator implements ICodeGenerator {

	public BasicBlockRegisterAllocator() {
	}

	@Override
	public List<IRInstruction> generateCode(List<IRInstruction> ir,
			SymbolTable rTable, Generation g, Object... extraArgs) {

		assert (extraArgs != null);
		assert (extraArgs.length == 1);
		assert (BasicBlockBuilder.class.isAssignableFrom(extraArgs[0]
				.getClass()));

		/* mark everything not leader before generation */
		for (IRInstruction i : ir) {
			i.isLeader = false;
		}
		BasicBlockBuilder blockBuilder = (BasicBlockBuilder) extraArgs[0];

		List<BasicBlock> needFirstBlock = blockBuilder.build();
		List<EBB> ebbList = EBBBuilder.buildForIntraBlock(needFirstBlock);

		RegisterAllocator allocator = new RegisterAllocator((ebbList));
		allocator.allocate();
		// EBBBuilder.printString(ebbList);
		List<IRInstruction> modifiedIR = allocator
				.getModifiedIRCode(needFirstBlock);
		return modifiedIR;
	}

}
