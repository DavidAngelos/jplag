package jplag.generic;

public class JavascriptParser extends GenericParser {
    public static String command = "/mnt/c/Users/davan/Downloads/code-jplag/run.sh";

    @Override
    protected GenericToken makeToken(int type, String file, int line, int column, int length) {
        return new JavascriptToken(type, file, line, column, length);
    }

    @Override
    protected String getCommandLineProgram() {
        return command;
    }
}
