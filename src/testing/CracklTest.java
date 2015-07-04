package testing;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import compiler.Compiler;
import exception.TypeCheckException;
import generation.Program;

public class CracklTest {

	public static final String ANTLR_ERROR = "code generation does not match the current";
	public static final String COMPLETE_ANTLR_ERROR = "ANTLR Tool version 4.4 used for code generation does not match the current runtime version 4.5ANTLR Tool version 4.4 used for code generation does not match the current runtime version 4.5";

	PipedOutputStream out = new PipedOutputStream();
	PipedInputStream in = null;	
	Runtime rt = Runtime.getRuntime();
	
	@Before
	public void init(){
		// pipe the error stream
		try {
			in = new PipedInputStream(out);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// set the error stream to our own PrintStream, so we can go over it after we're done
		System.setErr(new PrintStream(out));
	}

	@Test(timeout = 20 * 1000)
	public void testStandardFunctions()
	{
		String[] ee = { "0", "1", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "144" };
		compare("fibonacci.crk", ee);
	}

	@Test(timeout = 10 * 1000)
	public void testTestje()
	{
		fails("testje.crk", "not initialized");
	}

	@Test(timeout = 10 * 1000)
	public void testParsefail()
	{
		fails("parsefail.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testArrays()
	{
		succeeds("arrays.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testFibonacci()
	{
		succeeds("fibonacci.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testFunctions()
	{
		succeeds("functions.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testIfelse()
	{
		succeeds("ifelse.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testLocks()
	{
		succeeds("locks.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testNestedwhile()
	{
		succeeds("nestedwhile.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testPeterson()
	{
		succeeds("peterson.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testPointers()
	{
		succeeds("pointers.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testPointers2()
	{
		succeeds("pointers2.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testStrings()
	{
		succeeds("strings.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testWhile()
	{
		succeeds("while.crk");
		String[] whileArr = { "0", "1" };
		// compare("while.crk", whileArr);
	}

	@Test(timeout = 10 * 1000)
	public void testPrint()
	{
		compare("print.crk", new String[] { "hey" });
	}

	/**
	 * Compares an expected output of a program to the actual output it gives.
	 * @param fileName The file of which the output should be compared.
	 * @param expected The expected output.
	 */
	public void compare(String fileName, String[] expected){
		System.out.println("\nComparing the output of " + fileName + " to the expected output " + expected);
		parseAndCompile(fileName);
		ArrayList<String> actual = null;
		actual = compileAndExecute(fileName);
		if(!eq(expected, actual)){
			fail(String.format("Expected %s, got %s.\n", arrayToString(expected),actual ));
		}
	}
	
	
	public String arrayToString(String[] strings){
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for(int i = 0; i< strings.length; i++){
				sb.append(strings[i]);
				sb.append(" ");
			}
			sb.append("]");
			return sb.toString();
	}

	/**
	 * Checks if a Crackl file succeeds the TypeChecker's standards.
	 * @param fileName
	 */
	public void succeeds(String fileName){
		System.out.println("\nTrying to see if " + fileName + " succeeds.");
		Compiler comp = new Compiler();
		try {
			comp.compile(fileName);
		} catch (TypeCheckException e) {
			readIn();
			fail("File " + fileName + " should have parsed correctly, but didnt'.");
			e.printStackTrace();
		}
	}

	/**
	 * Checks if a Crackl file fails the TypeChecker's standards.
	 * @param fileName
	 */
	public void fails(String fileName){
		System.out.println("\nTrying to see if " + fileName + " fails.");
		Compiler comp = new Compiler();
		try {
			comp.compile(fileName);
			fail("File " + fileName + " should not have been parsed correctly, but did.");
			readIn();
		} catch (TypeCheckException e) {
			readIn();			
			// should happen
		}
	}

	/**
	 * Overloaded fails function, this time also checks if a specified error has been given by the TypeChecker.
	 * @param fileName The file to be checked.
	 * @param expectedError The error that is expected to be returned.
	 */
	public void fails(String fileName, String expectedError){
		System.out.println("\nTrying to see if " + fileName + " succeeds with error " + expectedError);
		Compiler comp = new Compiler();
		try {
			comp.compile(fileName);
			fail("File " + fileName + " should not have been parsed correctly, but did.");
			readIn();
		} catch (TypeCheckException e) {
			// supposed to happen, now check if our error stream contains the expected error.
			boolean hasStr = false;
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String nextLine = null;
			try {
				while(in.available() > 0){
					nextLine = br.readLine();
					if(!nextLine.contains(ANTLR_ERROR)){
						System.out.println("Error: " + nextLine);
					}
					if(nextLine.contains(expectedError)){
						hasStr = true;
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if(!hasStr){
				fail("Error stream didn't contain the error '" + expectedError + "'.");
			}
		}
		finally{
			System.setErr(System.err);
		}
	}

	/**
	 * Parses and compiles a Crackl file, and creates a file called 'crk_program.hs' in the /machine directory.
	 * @param fileName
	 */
	public void parseAndCompile(String fileName){
		Compiler compiler = new Compiler();
		Program prog = null;
		try {
			prog = compiler.compile(fileName);
		} catch (TypeCheckException e) {
			//e.printStackTrace();
			fail("File " + fileName + " didn't parse.");
		}
		try {
			compiler.write("crk_program.hs", prog);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compiles a file, executes it, and returns its output.
	 * @param fileName The file to be compiled.
	 * @return The output of the program.
	 */
	public ArrayList<String> compileAndExecute(String fileName){
		parseAndCompile(fileName);
		ArrayList<String> res = null;
		try {
			res = getOutput();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return res;
	}

	/**
	 * Returns the output of file that has been compiled last.
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> getOutput() throws IOException{
		final String os = System.getProperty("os.name");
		System.out.println("Build target: "+os);

		String outputFile = "machine/crk_program";
		String ghc = "ghc";
		if(os.startsWith("Windows")){
			outputFile+=".exe";
		}else if(os.equals("Linux")){
			ghc = "/opt/ghc/7.8.4/bin/ghc";
			System.out.println("Warning: using an absolute path for ghc: "+ghc);
		}

		Runtime rt = Runtime.getRuntime();

		Process p = rt.exec(ghc+" -imachine/sprockell/src machine/crk_program.hs");
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Writing executable: "+outputFile);
		Process p2 = rt.exec(outputFile);

		try {
			p2.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		InputStream is = p2.getInputStream();
		ArrayList<String> output = new ArrayList<String>();
		Scanner s = new Scanner(is);
		while(s.hasNext()){
			output.add(s.nextLine());
		}
		s.close();
		return output;
	}

	public boolean eq(String[] strArray, ArrayList<String> strList){
		boolean res = true;
		if(strArray.length != strList.size()){
			res = false;
		}else{
			for(int i = 0; i < strArray.length; i++){
				if(!(strArray[i].equals(strList.get(i)))){
					res = false;
				}
			}
		}
		return res;
	}

	public void readIn(){
		String nextLine = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while(in.available() > 0){
				nextLine = br.readLine();
				System.out.println("Error: " + nextLine);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	
}
