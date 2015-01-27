package cs4240_team1.codegen;

import cs4240_team1.IRInstruction;
import cs4240_team1.Op;
import cs4240_team1.LabelArgument;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

public class EBBBuilderTest {

	private List<EBB> EBBList;

	@Test
	public void twoChildren_ChildrenHaveOneParent_createOneEBB() {
		BasicBlock parent = new BasicBlock();
		BasicBlock child1 = new BasicBlock();
		BasicBlock child2 = new BasicBlock();

		parent.addSuccessor(child1);
		parent.addSuccessor(child2);

		EBBList = EBBBuilder.build(parent);

		Assert.assertEquals(1, EBBList.size());
		Assert.assertEquals(3, EBBList.get(0).getBasicBlocks().size());
	}

	@Test
	public void onlyOneLeaderBlock_CFGHasLoop_createOneEBB() {
		BasicBlock leader = new BasicBlock();
		BasicBlock loopBranch = new BasicBlock();
		BasicBlock loopExit = new BasicBlock();

		leader.addSuccessor(loopBranch);
		loopBranch.addSuccessor(loopExit);
		loopBranch.addSuccessor(leader);  // loop back edge

		EBBList = EBBBuilder.build(leader);

		Assert.assertEquals(1, EBBList.size());
		Assert.assertEquals(3, EBBList.get(0).getBasicBlocks().size());
	}

	@Test
	public void secondBlockHasTwoPredecessors_createTwoEBBs() {
		BasicBlock block1 = new BasicBlock();
		BasicBlock block2 = new BasicBlock();
		BasicBlock branchToBlock2 = new BasicBlock();

		block1.addSuccessor(block2);
		block2.addSuccessor(branchToBlock2);
		branchToBlock2.addSuccessor(block2);

		EBBList = EBBBuilder.build(block1);

		Assert.assertEquals(2, EBBList.size());
		Assert.assertEquals(1, EBBList.get(0).getBasicBlocks().size());
		Assert.assertEquals(2, EBBList.get(1).getBasicBlocks().size());

		Assert.assertSame(block2, EBBList.get(1).getBasicBlocks().get(0));
	}

	@Test
	/**
	 *   "enter"
	 *     |   _____
	 *     |  |     |
	 *     [BB]     |
	 *     / \      |
	 *  [BB] [BB]   |
	 *    \   /     |
	 *     [BB]     |
	 *      |\______|
	 *      |
	 *     [BB]
	 */
	public void branchLoopCFG_createTwoEBBs() {
		BasicBlock branch = new BasicBlock();
		BasicBlock fallthrough = new BasicBlock();
		BasicBlock target = new BasicBlock();
		BasicBlock looper = new BasicBlock();
		BasicBlock exit = new BasicBlock();

		branch.addSuccessor(fallthrough);
		branch.addSuccessor(target);
		fallthrough.addSuccessor(looper);
		target.addSuccessor(looper);
		looper.addSuccessor(branch);
		looper.addSuccessor(exit);

		EBBList = EBBBuilder.build(branch);

		Assert.assertEquals(2, EBBList.size());
		Assert.assertEquals(3, EBBList.get(0).getBasicBlocks().size());
		Assert.assertEquals(2, EBBList.get(1).getBasicBlocks().size());

		Assert.assertSame(looper, EBBList.get(1).getBasicBlocks().get(0));
	}
}
