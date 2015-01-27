package cs4240_team1.codegen;

import java.util.Comparator;

public class VariableComparator implements Comparator<BlockVariable> {

    @Override
    public int compare(BlockVariable o1, BlockVariable o2) {
        return o1.compareTo(o2);
    }
}
