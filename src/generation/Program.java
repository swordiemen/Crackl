package generation;

import java.util.ArrayList;

public class Program {

	public int numberOfSprockells; //how many sprockells the program will run on
	public ArrayList<Line> programLines;
	
	public Program(ArrayList<Line> programLines, int numberOfSprockells) {
		this.programLines = programLines;
		this.numberOfSprockells = numberOfSprockells;
	}

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
