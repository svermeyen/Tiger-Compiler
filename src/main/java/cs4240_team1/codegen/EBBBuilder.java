package cs4240_team1.codegen;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.HashSet;

public class EBBBuilder {

	/** 
	 * Partitions basic blocks into EBBs for use in intra-EBB allocation scheme.
	 *
	 * Note: CFG must already be generated
	 * (all predecessors/successor should be
	 * correctly set before calling this method)
	 *
	 * @param initialBlock the very first Basic Block in sequential program order.
	 * @return list of EBB's, where the first EBB is guaranteed to be the EBB
	 * whose leader block is initialBlock
	 */
	public static List<EBB> build(BasicBlock initialBlock) {
		List<EBB> ebbList = new ArrayList<>();

		Queue<BasicBlock> ebbLeaders = new LinkedList<>();

		// When processing currentEBB, ebbCandidates will hold either
		//   (a) BB's that should be added to the currentEBB
		//   (b) Leaders for subsequent EBB's
		Queue<BasicBlock> ebbCandidates = new LinkedList<>();

		// mark block as visited when we enqueue it in ebbCandidates
		//
		// exception: the initial leader is the only BB that is never enqueued
		// in ebbCandidates, so mark it as visited to start with
		HashSet<BasicBlock> visitedBlocks = new HashSet<>();

		BasicBlock currentLeader, currentEBBCandidate;
		EBB currentEBB;

		ebbLeaders.add(initialBlock);
		visitedBlocks.add(initialBlock);

		// traverses the CFG in a breadth-first manner for each EBB
		//
		// traversal begins at the EBB's leader block,
		// stopping when no blocks can be added to the current EBB
		while (!ebbLeaders.isEmpty()) {
			currentLeader = ebbLeaders.remove();
			currentEBB = new EBB(currentLeader);

			// now prepare for breadth-first traversal
			for (BasicBlock successor: currentLeader.getSuccessors()) {
				if (!visitedBlocks.contains(successor)) {
					ebbCandidates.add(successor);
					visitedBlocks.add(successor);
				}
			}

			// populate the current EBB and identify leaders of subsequent EBBs
			while (!ebbCandidates.isEmpty()) {
				currentEBBCandidate = ebbCandidates.remove();

				if (currentEBBCandidate.getPredecessorCount() <= 1) {
					currentEBB.add(currentEBBCandidate);

					for (BasicBlock successor: currentEBBCandidate.getSuccessors()) {
						if (!visitedBlocks.contains(successor)) {
							ebbCandidates.add(successor);
							visitedBlocks.add(successor);
						}
					}

				} else {
					ebbLeaders.add(currentEBBCandidate);
					// already marked as visited
					// when it was enqueued in ebbCandidates!
				}
			}

			// all BB's have been added to currentEBB!
			ebbList.add(currentEBB);
		}
		return ebbList;
	}

	/**
	 * Partitions basic blocks into EBBs of size one, essentially wrapping
	 * each basic block inside of an EBB.
	 *
	 * This method exists so that we can use the intra-EBB allocation algorithm
	 * for the intra-block allocation scheme.
	 */
	public static List<EBB> buildForIntraBlock(List<BasicBlock> blocks) {
		List<EBB> ebbList = new ArrayList<>();
		for (BasicBlock block: blocks) {
			ebbList.add(new EBB(block));
		}
		return ebbList;
	}

    public static void printString(List<EBB> ebbList) {
            System.out.println("=== START EBB WITH MODIFIED IR LIST ===");
            for (EBB ebb : ebbList) {
                System.out.println("=== START EBB ===");
                System.out.print(ebb.toString());
                System.out.println("=== END EBB ===");
            }
            System.out.println("=== END EBB WITH MODIFIED IR LIST ===");
        }
}
