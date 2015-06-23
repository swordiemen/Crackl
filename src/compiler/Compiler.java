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
	private TypeChecker checker;
	private Result result;

	public Compiler(){
		checker = new TypeChecker();
	}

	public void compile(String fileName){
		ParseTree tree = parse(fileName);
		System.out.println(tree);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(checker, tree);
		result = checker.getResult();
		Generator generator = new Generator(result);
		generator.visit(tree);
		ArrayList<Line> program = generator.getProgram();
		System.out.println(program);
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

	public void write(String fileName, ArrayList<Line> program) throws IOException{
		File file = new File(fileName);
		BufferedWriter bw = null;
		bw = new BufferedWriter(new FileWriter(file));

		StringBuilder sb = new StringBuilder("");
		for (Line line : program) {
			sb.append(line.toString());
		}
		bw.write(sb.toString());
		bw.close();

	}

	public static void main(String[] args){
		Compiler compiler = new Compiler();
		compiler.compile("test1.crk");
		ArrayList<Line> prog = new ArrayList<Line>();
		prog.add(new Line("1: Do et program"));
		prog.add(new Line("2: While something"));
		prog.add(new Line("3: End that"));
		try {
			compiler.write("test.sprk", prog);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
