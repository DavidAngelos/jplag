package jplag.java113;

public class JavaToken extends jplag.Token {
	private static final long serialVersionUID = -383581430479870696L;
	private int line, column, length;

	public JavaToken(int type, String file, int col,int line,int length) {
		super(type, file, col,line,length);
	}

	public int getLine() {
		return line;
	}

	public int getColumn() {
		return column;
	}

	public int getLength() {
		return length;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public static String type2string(int type) {
		if (type < 0 || type >= JavaTokenConstants.getNumTokens()) {
			return "Unkown token: " + type;
		}
		return JavaTokenConstants.valueToString(type);
	}

	@Override
	public String toString() {
		return type2string(this.type);
	}

	@Override
	public int numberOfTokens() {
		return staticNumberOfTokens();
	}

	static int staticNumberOfTokens() { return JavaTokenConstants.getNumTokens(); }
}