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

import org.junit.Test;

import compiler.Compiler;

import exception.TypeCheckException;
import generation.Program;

public class CracklTest {

	Runtime rt = Runtime.getRuntime();

	@Test
	public void testStandardFunctions(){
		String[] ee = {};
		compare("functions_rec.crk", ee);
		fails("testje.crk", "not initialized");
	}

	/**
	 * Compares an expected output of a program to the actual output it gives.
	 * @param fileName The file of which the output should be compared.
	 * @param expected The expected output.
	 */
	public void compare(String fileName, String[] expected){
		parseAndCompile(fileName);
		ArrayList<String> actual = null;
		try {
			actual = getOutput();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(!eq(expected, actual)){
			fail("Expected " + expected + ", got " + actual + ".");
		}
	}

	/**
	 * Checks if a Crackl file succeeds the TypeChecker's standards.
	 * @param fileName
	 */
	public void succeed(String fileName){
		Compiler comp = new Compiler();
		try {
			comp.compile(fileName);
		} catch (TypeCheckException e) {
			fail("File " + fileName + " should have parsed correctly, but didnt'.");
			e.printStackTrace();
		}
	}

	/**
	 * Checks if a Crackl file fails the TypeChecker's standards.
	 * @param filename
	 */
	public void fails(String filename){
		Compiler comp = new Compiler();
		try {
			comp.compile(filename);
			fail("File " + filename + " should not have been parsed correctly, but did.");
		} catch (TypeCheckException e) {
			// should happen
		}
	}

	/**
	 * Overloaded fails function, this time also checks if a specified error has been given by the TypeChecker.
	 * @param filename The file to be checked.
	 * @param expectedError The error that is expected to be returned.
	 */
	public void fails(String filename, String expectedError){
		// pipe the error stream
		PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in = null;
		try {
			in = new PipedInputStream(out);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		// set the error stream to our own PrintStream, so we can go over it after we're done
		System.setErr(new PrintStream(out));
		Compiler comp = new Compiler();
		try {
			comp.compile(filename);
			fail("File " + filename + " should not have been parsed correctly, but did.");
		} catch (TypeCheckException e) {
			// supposed to happen, now check if our error stream contains the expected error.
			boolean hasStr = false;
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String nextLine = null;
			try {
				nextLine = br.readLine();

				while(nextLine != null){
					if(nextLine.contains(expectedError)){
						hasStr = true;
					}
					if(!(in.available() > 0)){ 
						// stackoverflow.com/questions/804951/is-it-possible-to-read-from-a-inputstream-with-a-timeout
						break;
					}
					nextLine = br.readLine();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			if(!hasStr){
				fail("Error stream didn't contain " + expectedError);
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
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec("ghc -imachine/sprockell/src machine/crk_program.hs");
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Process p2 = rt.exec("machine/crk_program.exe");

		try {
			p2.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		InputStream is = p2.getInputStream();
		ArrayList<String> output = new ArrayList<String>();
		Scanner s = new Scanner(is);
		while(s.hasNext()){
			output.add(s.next());
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
}
