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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import compiler.Compiler;

import exception.TypeCheckException;
import generation.Program;

/**
 * This file contains Unit tests, and some more advanced demo tests with comparing expected results. 
 * Also checking the TypeChecker and lexer/parser by testing for 'fails' on some malformed crackl programs
 * Note that the very first launch since system boot may cause a problem if ghc is taking too long too load initially. This can be resolved by first loading up ghc, and then calling this test (or just run the test twice, where the second try must succeed!).
 * The used programs reside in the /programs directory
 */
public class CracklTest {

	public static final String ANTLR_ERROR = "code generation does not match the current";
	public static final String COMPLETE_ANTLR_ERROR = "ANTLR Tool version 4.4 used for code generation does not match the current runtime version 4.5ANTLR Tool version 4.4 used for code generation does not match the current runtime version 4.5";

	PipedOutputStream out = new PipedOutputStream();
	PipedInputStream in = null;
	Runtime rt = Runtime.getRuntime();
	
	@Before
	public void init()
	{
		// pipe the error stream
		try {
			in = new PipedInputStream(out);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// set the error stream to our own PrintStream, so we can go over it after we're done
		System.setErr(new PrintStream(out));
		System.out.println("--------------------------------------------------");
	}

	@Test(timeout = 10 * 1000)
	public void testDeclAndAssigment()
	{
		fails("WrongTypeAssignments.crk", "Expected type");
		fails("NotInitialized.crk", "not init");
		compare("SimpleDeclAssign.crk", new String[] { "2", "0" });
	}
	
	@Test(timeout = 10 * 1000)//This also tests for deadlocks
	public void testBank()
	{
		//Test consistency of a banking program using locks. 
		//Some transactions may be blocked, but this must still result in a no lost (or magically appearing) money
		compare("bank.crk", new String[] {
				"Starting ATM",
				"combined the accounts contain:", "162",
				"Performed 8 transactions. Accounts now contain combined:", "162",
				"Same! So executed correctly",
		});
	}

	@Test(timeout = 10 * 1000)
	public void testArrays()
	{
		succeeds("arrays.crk");
		fails("ArrayFailInit.crk", "Expected type integer, got boolean");
		fails("ArrayFailAssign.crk", "Expected type boolean, got text");
		// Tests three forms of arrays, and an not-immediate initialized array
		compare("arrays.crk", new String[] { "stack:", "3", "6", "9", "local heap:", "2", "4", "6", "8", "shared heap:", "21",
				"22", "23", "24", "uninitialized", "0", "4", "8", "12", "16", "20", "24", "28", "32", "36", "40", "44" });
	}

	@Test(timeout = 10 * 1000)
	public void testPeterson()
	{
		compare("peterson.crk", new String[] { "20" });
	}

	@Test(timeout = 12 * 1000)
	public void testStrings()
	{
		succeeds("strings.crk");
		compare("strings.crk", new String[] { "===========", "hello world", "===========", "hello world", "===========",
				"hello world", });
	}

	@Test(timeout = 10 * 1000)
	public void testPrint()
	{
		compare("print.crk", new String[] { "Hello world!", "42" });
	}

	@Test
	public void testCallByReference()
	{
		compare("cbr.crk", new String[] { "10", "11" });
	}

	@Test(timeout = 10 * 1000)
	public void testScopes()
	{
		fails("DeclaringTwiceInScope.crk", "'c' already declared");
		compare("ScopesTest.crk", new String[] { "2", "4" });
	}

	@Test(timeout = 10 * 1000)
	public void testIfElse()
	{
		fails("IfElseParseFail", "Expected type boolean, got integer");
		compare("IfElseTest.crk", new String[] { "33" });
	}

	@Test(timeout = 10 * 1000)
	public void testExpressions()
	{
		fails("BoolOrInt.crk", "Expected type boolean, got int");
		fails("BoolTimesBool.crk", "Expected type integer, got boolean");
		compare("ExpressionsTest.crk", new String[] { "8", "12", "3", "4", "1", "0", "0" });
	}

	@Test(timeout = 10 * 1000)
	public void testPointer()
	{
		fails("PointerFail1.crk", "Expected type boolean, got int");
		fails("PointerFail2.crk", "Expected type integer, got @int");
		compare("PointerTest.crk", new String[] { "100", "1", "100" });
		compare("pointers.crk", new String[] { "4", "5", "7", "8", "7", "9" }); // includes functions
	}

	@Test(timeout = 10 * 1000)
	public void numOperatorsTest()
	{
		compare("numOperators.crk", new String[] { "-8", "-15", "1", "8", "57", "33", "12", "-12", "16", "128" });
	}

	@Test(timeout = 10 * 1000)
	public void testStandardFunctions()
	{
		//Recursive functions test
		String[] ee = { "0", "1", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "144" };
		compare("fibonacci.crk", ee);
	}

	@Test(timeout = 10 * 1000)
	public void testBoolean()
	{
		String[] ee = { "success", "success", "0", "1", "1", "0", "1", "0", "0", "1", "1", "0", "1" };
		compare("boolean.crk", ee);
	}

	@Test(timeout = 10 * 1000)
	public void testTestje()
	{
		fails("testje.crk", "not initialized");
	}

	@Test(timeout = 5 * 1000)
	public void testFunctionsFail()
	{
		// tests some functions that are called with wrong number of arguments, or with arguments of the wrong type.
		fails("functionsFail.crk", "Invalid amount of arguments for function 'add', expected 2 but got 1. (16:2)");
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
	public void testWhile()
	{
		succeeds("nestedwhile.crk");
		compare("nestedwhile.crk", new String[] { "0", "1", "2", "3", "0", "1", "2", "3", "0", "1", "2", "3" });
	}

	@Test(timeout = 10 * 1000)
	public void testFunctions()
	{
		succeeds("functions.crk");
		compare("functions.crk", new String[] { "8 powers of 2 :)", "1", "2", "4", "8", "16", "32", "64", "128",
				"3 powers of 12!", "1", "12", "144" });
	}

	@Test(timeout = 10 * 1000)
	public void testPointers()
	{
		succeeds("pointers.crk");
	}

	@Test(timeout = 10 * 1000)
	public void testPointers2()
	{
		//nested pointers, pointers to pointers
		succeeds("pointers2.crk");
	}

	@After
	public void flush()
	{
		//Flush the streams, and wait for programs to terminate
		readIn();
		try {
			Thread.sleep(333);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/** This gave inconsistent problems, causing *some* tests to randomly fail
	@AfterClass
	public static void cleanup(){
		try{
			in.close();
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	**/

	/**
	 * Compares an expected output of a program to the actual output it gives.
	 * 
	 * @param fileName
	 *            The file of which the output should be compared.
	 * @param expected
	 *            The expected output.
	 */
	public void compare(String fileName, String[] expected)
	{
		System.out.println("\nComparing the output of " + fileName + " to the expected output " + arrayToString(expected));
		parseAndCompile(fileName);
		ArrayList<String> actual = null;
		actual = compileAndExecute(fileName);
		if (!eq(expected, actual)) {
			fail(String.format("Expected %s, got %s.\n", arrayToString(expected), actual));
		}
	}

	public String arrayToString(String[] strings)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < strings.length; i++) {
			sb.append(strings[i]);
			sb.append(", ");
		}
		sb.replace(sb.length() - 2, sb.length(), ""); // remove the last comma space
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Checks if a Crackl file succeeds the TypeChecker's standards.
	 * 
	 * @param fileName
	 */
	public void succeeds(String fileName)
	{
		System.out.println("\nTrying to see if " + fileName + " succeeds.");
		Compiler comp = new Compiler();
		try {
			comp.compile(fileName);
		} catch (TypeCheckException e) {
			readIn();
			fail("File " + fileName + " should have parsed correctly, but didnt'.");
			e.printStackTrace();
		} catch (IOException e) {
			readIn();
			fail("File " + fileName + " couldn't be opened'.");
			e.printStackTrace();
		}
	}

	/**
	 * Checks if a Crackl file fails the TypeChecker's standards.
	 * 
	 * @param fileName
	 */
	public void fails(String fileName)
	{
		System.out.println("\nTrying to see if " + fileName + " fails.");
		Compiler comp = new Compiler();
		try {
			comp.compile(fileName);
			fail("File " + fileName + " should not have been parsed correctly, but did.");
			readIn();
		} catch (TypeCheckException e) {
			readIn();
			// should happen
		} catch (IOException e) {
			readIn();
			fail("File " + fileName + " couldn't be opened'.");
			e.printStackTrace();
		}
	}

	/**
	 * Overloaded fails function, this time also checks if a specified error has been given by the TypeChecker.
	 * 
	 * @param fileName
	 *            The file to be checked.
	 * @param expectedError
	 *            The error that is expected to be returned.
	 */
	public void fails(String fileName, String expectedError)
	{
		System.out.println("\nTrying to see if " + fileName + " fails with error '" + expectedError + "'.");
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
				while (in.available() > 0) {
					nextLine = br.readLine();
					if (!nextLine.contains(ANTLR_ERROR)) {
						System.out.println("Error: " + nextLine);
					}
					if (nextLine.contains(expectedError)) {
						hasStr = true;
					}
				}
			} catch (IOException ioe) {
				readIn();
				fail("File " + fileName + " couldn't be opened'.");
				ioe.printStackTrace();
			}

			if (!hasStr) {
				fail("Error stream didn't contain the error '" + expectedError + "'.");
			}
		} catch (IOException e) {
			readIn();
			fail("File " + fileName + " couldn't be opened'.");
			e.printStackTrace();
		} finally {
			System.setErr(System.err);
		}
	}

	/**
	 * Parses and compiles a Crackl file, and creates a file called 'crk_program.hs' in the /machine directory.
	 * 
	 * @param fileName
	 */
	public void parseAndCompile(String fileName)
	{
		Compiler compiler = new Compiler();
		Program prog = null;
		try {
			prog = compiler.compile(fileName);
		} catch (TypeCheckException e) {
			// e.printStackTrace();
			fail("File " + fileName + " didn't parse.");
		} catch (IOException e) {
			readIn();
			fail("File " + fileName + " couldn't be opened'.");
			e.printStackTrace();
		}
		try {
			compiler.write("crk_program.hs", prog);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compiles a file, executes it, and returns its output.
	 * 
	 * @param fileName
	 *            The file to be compiled.
	 * @return The output of the program.
	 */
	public ArrayList<String> compileAndExecute(String fileName)
	{
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
	 * 
	 * @return
	 * @throws IOException
	 */
	public ArrayList<String> getOutput() throws IOException
	{
		final String os = System.getProperty("os.name");
		System.out.println("Build target: " + os);

		String outputFile = "machine/crk_program";
		String ghc = "ghc";
		if (os.startsWith("Windows")) {
			outputFile += ".exe";
		}
		else if (os.equals("Linux")) {
			ghc = "/opt/ghc/7.8.4/bin/ghc";
			System.out.println("Warning: using an absolute path for ghc: " + ghc);
		}

		Runtime rt = Runtime.getRuntime();

		Process p = rt.exec(ghc + " -imachine/sprockell/src machine/crk_program.hs");
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Process p2 = rt.exec(outputFile);

		try {
			p2.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		InputStream is = p2.getInputStream();
		ArrayList<String> output = new ArrayList<String>();
		Scanner s = new Scanner(is);
		while (s.hasNext()) {
			output.add(s.nextLine());
		}
		s.close();
		return output;
	}

	public boolean eq(String[] strArray, ArrayList<String> strList)
	{
		boolean res = true;
		if (strArray.length != strList.size()) {
			res = false;
		}
		else {
			for (int i = 0; i < strArray.length; i++) {
				if (!(strArray[i].equals(strList.get(i)))) {
					res = false;
				}
			}
		}
		return res;
	}

	public void readIn()
	{
		String nextLine = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(in));
			while (in.available() > 0) {
				nextLine = br.readLine();
				System.out.println("Error: " + nextLine);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
