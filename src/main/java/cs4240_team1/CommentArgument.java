package cs4240_team1;

/**
 * [CB 11/22/14]
 * used to provide inline comments during assembly generation.
 * assembly, especially with temp vars, is hard to read -- this can be quite handy.
 * @author vancan1ty
 *
 */
public class CommentArgument implements IRCodeArgument {

	private String text;

	public CommentArgument(String text) {
		this.text = text;
	}

	@Override
	public String getTextForIR() {
		return " # " + text; 
	}

    @Override
    public void setTextForIR(String id) {
        return;
    }

    /**
	 * this has no meaning for this class.
	 */
	@Override
	public TigerType getTigerType() {
		return null;
	}
	
	public String toString() {
		return "# " + text; 
	}
}
