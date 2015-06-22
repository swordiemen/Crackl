package compiler;

import grammar.CracklLexer;
import grammar.CracklParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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
		new ParseTreeWalker().walk(checker, tree);
		result = checker.getResult();
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
	
	public static void main(String[] args){
		Compiler compiler = new Compiler();
		compiler.compile("test1.crk");
	}
}
