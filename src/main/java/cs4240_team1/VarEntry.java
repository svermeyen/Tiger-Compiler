package cs4240_team1;

public class VarEntry extends SymbolTableEntry implements IRCodeArgument {

	/**
	 * this is the name of the variable
	 */
    public final String varId;

    /**
     * this is the name of the variable, with numeric index appended. 
     */
    public String uniqueVarId;

    public String typeId;

	public TigerType type;


	/**
	 * "legacy" constructor to creating VarEntries that we need to
	 * perform semantic checks on before inserting into symbol table.
	 */
	public VarEntry(String varId, String typeId) {
		this(varId, typeId, null);
	}

    public VarEntry(String varId, String typeId, String uniqueVarId) {
        super(varId);
        this.varId = varId;
        this.typeId = typeId;
        this.uniqueVarId = uniqueVarId;
    }

	public VarEntry(String varId, TigerType type, String uniqueVarId) {
        super(varId);
		this.varId = varId;
		this.type = type;
		this.typeId = type.typeId;
		this.uniqueVarId = uniqueVarId;
	}
	
	/**
	 * used for creating "trash" varEntries during code generation.
	 * @param varId
	 * @param type
	 */
	public VarEntry(String varId, TigerType type) {
		super(varId);
		this.varId = varId;
		this.type = type;
		this.typeId = type.typeId;
	}

    @Override
    public String toString() {
        return "< " + varId + " | " + type.toStringForSymbolTable() + " >";
    }

	@Override
	public String getCategoryAsString() {
		return "var";
	}

	public TigerType getType() {
		return this.type;
	}

	public void setType(TigerType newType) {
		this.type = newType;
		this.typeId = newType.typeId;
	}

	/**
	 * return the unique "id" of the varEntry.
	 * Usually is the name of the varEntry with a unique number appended.
	 * @return
	 */
	public String getUniqueVarId() {
		return uniqueVarId;
	}

    public String getTextForIR() {
        return this.uniqueVarId;
    }

    public void setTextForIR(String id) {
        this.uniqueVarId = id;
    }

    public TigerType getTigerType() {
        return this.type;
    }

	public Integer getTotalNumberOfElementsForArrayVar() {
			int totNum; 
			TigerType type = this.getTigerType();
			if(!type.isArray()) {
				return null;
			}

			if(type.dimension2 == null) {
				totNum = type.dimension1;
			} else { //account for 2d arrays.
				totNum = type.dimension1*type.dimension2;
			}

			return totNum;
	}
}
