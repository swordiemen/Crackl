package generation;

import java.util.ArrayList;

public class Function {

	public Function(String text) {
		this.id = text;
	}

	public ArrayList<String> params = new ArrayList<String>();
	public int startLine = -123;
	public String id = null;

}
