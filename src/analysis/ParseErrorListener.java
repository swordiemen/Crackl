package analysis;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

/** Antlr error listener to collect errors rather than send them to stderr. */
public class ParseErrorListener extends BaseErrorListener {
	/** Errors collected by the listener. */
	private final List<String> errors = new ArrayList<>();

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer,
			Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		this.errors.add(String.format("Line %d:%d - unexpected token %s", line,
				charPositionInLine, offendingSymbol.toString(), msg));
	}

	/** Adds an error message during the tree visit stage. */
	public void visitError(Token token, String msg, Object... args) {
		int line = token.getLine();
		int charPositionInLine = token.getCharPositionInLine();
		msg = String.format(msg, args);
		msg = String.format("Line %d:%d - %s", line, charPositionInLine, msg);
		this.errors.add(msg);
	}

	/** Indicates if the listener has collected any errors. */
	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	/** Returns the (possibly empty) list of errors collected by the listener. */
	public List<String> getErrors() {
		return this.errors;
	}
}