package analysis;

import java.util.ArrayList;

/**
 * Function holds some informatation regarding generating and TypeChecking functions, such as it’s arguments, name and “program line”, which indicates at which instruction number the function starts.
 *
 */
public class Function {

	public Function(String text) {
		this.id = text;
	}

	public ArrayList<String> params = new ArrayList<String>();
	public int startLine = -123;
	public String id = null;

}
