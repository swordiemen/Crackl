package compiler;

import generation.Generator;
import generation.Line;
import grammar.CracklLexer;
import grammar.CracklParser;
import grammar.CracklParser.ProgramContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import analysis.Result;
import analysis.TypeChecker;

import com.sun.org.apache.xalan.internal.xsltc.compiler.CompilerException;

public class Compiler {
	public static final String PROGRAMS_PATH = "./machine";
	private TypeChecker checker;

	/**
	 * Creates a new Compiler class.
	 */
	public Compiler(){
		checker = new TypeChecker();
	}

	/**
	 * Takes the name of a file, and compiles it into a program that can be read by Sprockll.
	 * @param fileName The name of the file where the code is written.
	 * @throws CompilerException 
	 */
	public ArrayList<Line> compile(String fileName) throws CompilerException{
		ParseTree tree = parse(fileName);
		System.out.println(tree);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(checker, tree);
		if(!checker.hasErrors()){
			Result result = checker.getResult();
			Generator generator = new Generator(result);
			generator.visit(tree);
			ArrayList<Line> program = generator.getProgram();
			System.out.println(program);
			return program;
		}else{
			throw new CompilerException("Build failed (TypeChecker)");
		}
	}

	/**
	 * Takes a file, and returns a parseTree of the code in the file.
	 * @param fileName Name of the file to be parsed.
	 * @return <b>parseTree</b> The resulting parseTree.
	 */
	public ParseTree parse(String fileName){		
		CharStream chars = null;
		try {
			chars = new ANTLRInputStream(new FileReader(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Lexer lexer = new CracklLexer(chars);
		TokenStream tokens = new CommonTokenStream(lexer);
		CracklParser parser = new CracklParser(tokens);
		ProgramContext tree = parser.program();
		return tree;
	}
	
	public String formatProgram(ArrayList<Line> program)
	{
		StringBuilder sb = new StringBuilder("import Sprockell.Sprockell\n"+
                                                "import Sprockell.System\n"+
                                                "import Sprockell.TypesEtc\n"+
                                                "\n"+
                                                "prog :: [Instruction]\n"+
                                                "prog = [ \n"
                                            );
		final String TAB = "\t";
		int i;
		for (i = 0; i<program.size()-1; i++) {
			sb.append(TAB);
			sb.append(program.get(i).toString());
			sb.append(",\n");
		}
			sb.append(TAB);
			sb.append(program.get(i++));
			
			sb.append("\t]\n"+
					"main = run 1 prog");
		return sb.toString();
		
	}

	/**
	 * Takes a program generated by the generator, and writes it to a file.
	 * @param fileName The name of the file
	 * @param program The program generated by the generator.
	 * @throws IOException
	 */
	public void write(String fileName, ArrayList<Line> program) throws IOException{
		if(!new File(PROGRAMS_PATH).mkdirs()){
//			System.out.println("Failed t>>>>>>>o make directory: "+PROGRAMS_PATH);
//			return;
		}
		File file = new File(PROGRAMS_PATH+"/"+fileName);
		BufferedWriter bw = null;
		bw = new BufferedWriter(new FileWriter(file));

		bw.write(formatProgram(program));
		bw.close();

	}

	public static void main(String[] args){
		Compiler compiler = new Compiler();
		try {
			ArrayList<Line> prog = compiler.compile("ifelse.crk");
			compiler.write("ptest.hs", prog);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CompilerException e1) {
			e1.printStackTrace();
		}
	}
}
