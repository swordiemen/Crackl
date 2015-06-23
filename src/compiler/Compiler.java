package compiler;

import generation.Generator;
import generation.Line;
import grammar.CracklLexer;
import grammar.CracklParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import analysis.Result;
import analysis.TypeChecker;
import grammar.CracklParser.*;

public class Compiler {
	public static final String PROGRAMS_PATH = "./machine";
	private TypeChecker checker;
	private Result result;

	public Compiler(){
		checker = new TypeChecker();
	}

	public ArrayList<Line> compile(String fileName){
		ParseTree tree = parse(fileName);
		System.out.println(tree);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(checker, tree);
		result = checker.getResult();
		Generator generator = new Generator(result);
		generator.visit(tree);
		ArrayList<Line> program = generator.getProgram();
		System.out.println(program);
		return program;
	}

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

	public void write(String fileName, ArrayList<Line> program) throws IOException{
		if(!new File(PROGRAMS_PATH).mkdirs()){
//			System.out.println("Failed to make directory: "+PROGRAMS_PATH);
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
		ArrayList<Line> prog = compiler.compile("test1.crk");
		try {
			compiler.write("ptest.hs", prog);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}