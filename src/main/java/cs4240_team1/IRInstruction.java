package cs4240_team1;

import java.util.List;

public class IRInstruction {
    Op op = null;
    IRCodeArgument[] args;
    int originalLineNumber; // records the line number in the original program.
    StringBuilder build;
    public Boolean isLeader = false;

    /**no args constructor.  This was added for IRComment support.
     */
    public IRInstruction() {}

    public IRInstruction(Op op, IRCodeArgument ... args) {
        this.op = op;
        this.args = args;
    }

   public IRInstruction(Op op, List<IRCodeArgument> args) {
       this.op = op;
       this.args = args.toArray(new IRCodeArgument[1]);
   }

    @Override
    public String toString() {
        build = new StringBuilder();
        switch (op) {
            case LABEL:
                for (IRCodeArgument codeArg : args) {
                    build.append(codeArg.getTextForIR());
					build.append(":");
                }
				break;

			/* in PDF, these do not need three commas -- may have more or fewer commas */
			case CALL:
			case CALLR:
                build.append(op.toString());
                for (IRCodeArgument codeArg : args) {
                    build.append(", " + codeArg.getTextForIR());
                }
				break;
			case SW: //these need the special parentheses syntax.
			case LW:
			case SWC1:
			case LWC1:
                build.append(op.toString());
                build.append(", " + this.args[0].getTextForIR());
                build.append(", " + this.args[1].getTextForIR());
                if(args.length == 3) { //then we are indexing into the mem location.
                	build.append("("+args[2].getTextForIR()+")");
                }
				break;

			/* for all other ops, always have three commas (they are quadruplets) */
            default:
				int commasLeft = 3;
                build.append(op.toString());
                for (IRCodeArgument codeArg : args) {
                    build.append(", " + codeArg.getTextForIR());
					commasLeft--;
                }
				while (commasLeft > 0) {
					build.append(",");
					commasLeft--;
					if (commasLeft > 0) {
						build.append(" ");  /* no trailing space */
					}
				}
				break;
        }
        return build.toString();
    }

	public Op getOp() {
		return op;
	}

	public IRCodeArgument[] getArgs() {
		return args;
	}
}

