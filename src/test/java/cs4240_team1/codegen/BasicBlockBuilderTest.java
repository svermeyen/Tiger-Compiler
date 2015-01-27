package cs4240_team1.codegen;

import cs4240_team1.IRInstruction;
import cs4240_team1.Op;
import cs4240_team1.LabelArgument;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

/**
 * A series of simple tests to make sure our CFG gets wired correctly.
 */
public class BasicBlockBuilderTest {

	private List<IRInstruction> IRList;
	private BasicBlockBuilder TestBBBuilder;
	private List<BasicBlock> BasicBlockList;
	
	@Test
	public void noJumps_oneBasicBlock() {
		IRList = new ArrayList<>();
		IRList.add(createFakeAddInstruction());
		IRList.add(createFakeAddInstruction());

		TestBBBuilder = new BasicBlockBuilder(IRList);
		BasicBlockList = TestBBBuilder.build();
		
		Assert.assertEquals(1, BasicBlockList.size());
	}

	@Test
	public void branchInstruction_childrenAreTargetAndFallthrough() {
		IRList = new ArrayList<>();
		String targetLabel = "branchTarget";

		// branch block
		IRList.add(createFakeAddInstruction());
		IRList.add(createFakeAddInstruction());
		IRList.add(createBranchInstruction(targetLabel));
		// fallthrough block
		IRList.add(createFakeAddInstruction());
		// target block
		IRList.add(createLabelInstruction(targetLabel));
		IRList.add(createFakeAddInstruction());

		TestBBBuilder = new BasicBlockBuilder(IRList);
		BasicBlockList = TestBBBuilder.build();

		Assert.assertEquals(3, BasicBlockList.size());

		BasicBlock branchBlock = BasicBlockList.get(0);
		BasicBlock fallthroughBlock = BasicBlockList.get(1);
		BasicBlock targetBlock = BasicBlockList.get(2);

		Assert.assertEquals(2, branchBlock.getSuccessors().size());
		Assert.assertEquals(0, branchBlock.getPredecessorCount());

		Assert.assertEquals(1, fallthroughBlock.getPredecessorCount());
		Assert.assertEquals(1, fallthroughBlock.getSuccessors().size());

		Assert.assertEquals(0, targetBlock.getSuccessors().size());
		Assert.assertEquals(2, targetBlock.getPredecessorCount());
	}

	@Test
	public void gotoInstruction_targetIsOnlyChild() {
		IRList = new ArrayList<>();
		String targetLabel = "gotoTarget";

		// goto block
		IRList.add(createGotoInstruction(targetLabel));
		// fallthrough block
		IRList.add(createFakeAddInstruction());
		// target block
		IRList.add(createLabelInstruction(targetLabel));
		IRList.add(createFakeAddInstruction());

		TestBBBuilder = new BasicBlockBuilder(IRList);
		BasicBlockList = TestBBBuilder.build();

		Assert.assertEquals(3, BasicBlockList.size());

		BasicBlock gotoBlock = BasicBlockList.get(0);
		BasicBlock fallthroughBlock = BasicBlockList.get(1);
		BasicBlock targetBlock = BasicBlockList.get(2);

		Assert.assertEquals(1, gotoBlock.getSuccessors().size());
		Assert.assertEquals(0, gotoBlock.getPredecessorCount());

		Assert.assertEquals(0, fallthroughBlock.getPredecessorCount());
		Assert.assertEquals(1, fallthroughBlock.getSuccessors().size());

		Assert.assertEquals(0, targetBlock.getSuccessors().size());
		Assert.assertEquals(2, targetBlock.getPredecessorCount());

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
	 *    "exit"
	 */
	public void branchLoopCFG_isSupported() {
		IRList = new ArrayList<>();
		String beginLabel = "begin";
		String branchLabel = "branchTarget";
		String endLabel = "end";

		// begin block
		IRList.add(createLabelInstruction(beginLabel));
		IRList.add(createBranchInstruction(branchLabel));
		// fallthrough block
		IRList.add(createGotoInstruction(endLabel));
		// branch target block
		IRList.add(createLabelInstruction(branchLabel));
		IRList.add(createFakeAddInstruction());
		// end block
		IRList.add(createLabelInstruction(endLabel));
		IRList.add(createGotoInstruction(beginLabel));

		TestBBBuilder = new BasicBlockBuilder(IRList);
		BasicBlockList = TestBBBuilder.build();

		Assert.assertEquals(4, BasicBlockList.size());

		BasicBlock begin = BasicBlockList.get(0);
		BasicBlock fallthrough = BasicBlockList.get(1);
		BasicBlock branch = BasicBlockList.get(2);
		BasicBlock end = BasicBlockList.get(3);

		Assert.assertEquals(1, begin.getPredecessorCount());
		Assert.assertEquals(2, begin.getSuccessors().size());
		Assert.assertTrue(begin.getSuccessors().contains(fallthrough));
		Assert.assertTrue(begin.getSuccessors().contains(branch));

		Assert.assertEquals(1, fallthrough.getPredecessorCount());
		Assert.assertEquals(1, fallthrough.getSuccessors().size());
		Assert.assertTrue(fallthrough.getSuccessors().contains(end));

		Assert.assertEquals(1, branch.getPredecessorCount());
		Assert.assertEquals(1, branch.getSuccessors().size());
		Assert.assertTrue(branch.getSuccessors().contains(end));

		Assert.assertEquals(2, end.getPredecessorCount());
		Assert.assertEquals(1, end.getSuccessors().size());
    	Assert.assertTrue(end.getSuccessors().contains(begin));

	}

	private IRInstruction createFakeAddInstruction() {
		return new IRInstruction(Op.ADD, null, null, null);
	}

	private IRInstruction createLabelInstruction(String label) {
		return new IRInstruction(Op.LABEL, new LabelArgument(label));
	}

	private IRInstruction createBranchInstruction(String targetLabel) {
		return new IRInstruction(Op.BREQ, null, null, new LabelArgument(targetLabel));
	}

	private IRInstruction createGotoInstruction(String targetLabel) {
		return new IRInstruction(Op.GOTO, new LabelArgument(targetLabel));
	}
}
