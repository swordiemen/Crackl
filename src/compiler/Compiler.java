package compiler;

import exception.TypeCheckException;
import generation.Generator;
import generation.Line;
import generation.Program;
import grammar.CracklLexer;
import grammar.CracklParser;
import grammar.CracklParser.ProgramContext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import analysis.ParseErrorListener;
import analysis.Result;
import analysis.TypeChecker;

public class Compiler {
	public static final String PROGRAMS_PATH = "./programs/";
	public static final String OUTPUT_PATH = "./machine/";

	private TypeChecker checker;
	ParseErrorListener errorListener;

	/**
	 * Creates a new Compiler class.
	 */
	public Compiler() {
		checker = new TypeChecker();
		errorListener = new ParseErrorListener();
	}

	/**
	 * Takes the name of a file, and compiles it into a program that can be read by Sprockll.
	 * 
	 * @param fileName
	 *            The name of the file where the code is written.
	 * @return program The program created by the generator.
	 */
	public Program compile(String fileName) throws TypeCheckException, FileNotFoundException, IOException
	{
		errorListener = new ParseErrorListener();
		ParseTree tree = parse(fileName);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(checker, tree);
		if (errorListener.hasErrors()) {
			List<String> errors = errorListener.getErrors();
			for (String error : errors) {
				System.out.println("Error: " + error);
			}
			throw new TypeCheckException("File " + fileName + " has not been parsed correctly.");
		}
		if (!checker.hasErrors()) {
			Result result = checker.getResult();

			Generator generator = new Generator(result, checker.functions);
			generator.visit(tree);
			ArrayList<Line> programLines = generator.program;
			Program program = new Program(programLines, result.numberOfSprockells);
			return program;
		}
		else {
			for (String err : checker.getErrors()) {
				System.out.println(err);
			}
			throw new TypeCheckException("Build failed (TypeChecker)");
		}
	}

	/**
	 * Takes a file, and returns a parseTree of the code in the file.
	 * 
	 * @param fileName
	 *            Name of the file to be parsed.
	 * @return <b>parseTree</b> The resulting parseTree.
	 */
	public ParseTree parse(String fileName) throws TypeCheckException, FileNotFoundException, IOException
	{
		ProgramContext tree = null;
		CharStream chars = null;

		File inputFile = new File(PROGRAMS_PATH + fileName);
		if (inputFile.exists()) {
			chars = new ANTLRInputStream(new FileReader(inputFile));
			Lexer lexer = new CracklLexer(chars);
			TokenStream tokens = new CommonTokenStream(lexer);
			CracklParser parser = new CracklParser(tokens);
			parser.addErrorListener(errorListener);
			tree = parser.program();
		}
		else {
			throw new FileNotFoundException(PROGRAMS_PATH + fileName);
		}
		// if(errorListener.hasErrors()){
		// throw new TypeCheckException("Parsing file " + fileName + " has failed.");
		// }
		return tree;
	}

	/**
	 * Takes a program generated by the generator, and writes it to a file.
	 * 
	 * @param fileName
	 *            The name of the file
	 * @param program
	 *            The program generated by the generator.
	 * @throws IOException
	 */
	public void write(String fileName, Program program) throws IOException
	{
		if (!new File(OUTPUT_PATH).mkdirs()) {
		}
		File file = new File(OUTPUT_PATH + fileName);
		BufferedWriter bw = null;
		bw = new BufferedWriter(new FileWriter(file));

		bw.write(program.create());
		bw.close();
	}

	public static void main(String[] args)
	{
		Compiler compiler = new Compiler();
		try {
			String program_name = "fibonacci.crk";
			Program program = compiler.compile(program_name);
			compiler.write("crk_program.hs", program);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TypeCheckException e1) {
			e1.printStackTrace();
		}
	}
}
