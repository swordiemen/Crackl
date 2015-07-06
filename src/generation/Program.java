package generation;

import java.util.ArrayList;

/**
 * Program simply holds an list of Lines, and models a Sprockell program.
 * 
 */
public class Program {

	/**
	 * How many sprockells the program will run on
	 */
	public int numberOfSprockells; 

	/**
	 * The instructions of the program
	 */
	public ArrayList<Line> programLines;

	public Program(ArrayList<Line> programLines, int numberOfSprockells) {
		this.programLines = programLines;
		this.numberOfSprockells = numberOfSprockells;
	}

	/**
	 * create() can turn the list of instructions into an actual Sprockell program executable by Sprockell machine running on
	 * Haskell.*
	 */
	public String create()
	{
		StringBuilder sb = new StringBuilder("import Sprockell.Sprockell\n" + "import Sprockell.System\n"
				+ "import Sprockell.TypesEtc\n" + "\n" + "prog :: [Instruction]\n" + "prog = [ \n");
		final String TAB = "\t";
		int i;
		for (i = 0; i < programLines.size() - 1; i++) {
			sb.append(TAB);
			sb.append(programLines.get(i).toString());
			sb.append(",\n");
		}
		sb.append(TAB);
		sb.append(programLines.get(i++));

		sb.append(String.format("\t]\nmain = run %d prog", this.numberOfSprockells));
		return sb.toString();

	}

}
