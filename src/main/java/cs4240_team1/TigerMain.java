package cs4240_team1;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cs4240_team1.codegen.*;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import utility.Utility;

/**
 * Driver class for our Tiger Lexer/Parser
 */
public class TigerMain {
	
	public static Map<Integer, ICodeGenerator> numsToRegStrategies = 
			Utility.createMapFromArray(
			0, new NaiveRegisterAllocator(),
			1, new BasicBlockRegisterAllocator(),
			2, new EBBRegisterAllocator() 
			);

	/* OR these ints together to toggle various debug/printing functionality */
	static final int DEBUG_LEXERPARSER     = 0x0000001;
	static final int DEBUG_SYMBOLTABLES    = 0x0000002;
	static final int DEBUG_IR_GENERATION   = 0x0000004;
	static final int DEBUG_CODE_BLOCKS     = 0x0000008;
	static final int DEBUG_CODE_GENERATION = 0x0000010;

	/**
	 * by default, don't print anything extra.
	 */
	static int DEBUG_MODE = 0;

	/**
	 * pass in one of the flags, this returns true if the functionality it
	 * represents is turned on in DEBUG_MODE, otherwise false.
	 */
	public static boolean testFlag(int flag) {
		return (flag == (flag & DEBUG_MODE));
	}

	public static void main(String[] args) throws IOException {

		/*
		 * currently, this is the main variable that controls the amount of
		 * output to show.
		 */

		OptionParser oparser = new OptionParser() {
			{
				acceptsAll(Arrays.asList("h", "?"), "print help");
				accepts("v",
						"verbose (complete) output. "
								+ "If this is on, lexer output, parser output, symbol table output,"
								+ " and any additional debugging info will be printed.  Defaults to"
								+ " off.");
				accepts("r", "choose register allocation strategy")
					.withRequiredArg()
					.ofType(Integer.class)
					.describedAs("one of 0, 1, or 2")
					.defaultsTo(0);
				accepts("s", "print symbol tables.  Included within -v.");
				accepts("p", "print parser/lexer tokens. Included within -v.");
				accepts("b", "print code blocks info. Included within -v.");
				accepts("i",
						"print intermediate representation code. Included within -v.");
				accepts(
						"o",
						"In addition to specified stdout output, print final "
								+ "code to the named file.").withOptionalArg()
						.describedAs("filename").defaultsTo("out.asm");
			}
		};

		OptionSet options = oparser.parse(args);

		if (args.length == 0) {
			System.out.println("input file required!");
			System.out.println();
		}
		if (args.length == 0 || options.has("h")) {
			System.out
					.println("TIGGER - CS4400 Fall 2014 Team 1 Tiger Compiler");
			System.out
					.println("===============================================");
			System.out
					.println("Usage: java -jar TIGGER.jar [-vspibh] [-o [fname]] {inputfile}");
			System.out
					.println("Description: By default, currently prints the best\n"
							+ "assembly representation of the input file which the compiler\n"
							+ "can produce at the moment to standard out.\n"
							+ "By enabling the -v flag, you can obtain more output.\n");
			System.out.println();
			oparser.printHelpOn(System.out);
			System.out.println();
			System.out.println("Authors:");
			System.out.println("Max Virgil, Scott Vermeyen, Currell Berry");
			return;
		}

		if (options.has("v")) {
			/* then print everything out */
			DEBUG_MODE = DEBUG_MODE | DEBUG_LEXERPARSER | DEBUG_SYMBOLTABLES
					| DEBUG_IR_GENERATION | DEBUG_CODE_BLOCKS
					| DEBUG_CODE_GENERATION;
		}

		if (options.has("s")) {
			DEBUG_MODE = DEBUG_MODE | DEBUG_SYMBOLTABLES;
		}

		if (options.has("p")) {
			DEBUG_MODE = DEBUG_MODE | DEBUG_LEXERPARSER;
		}

		if (options.has("i")) {
			DEBUG_MODE = DEBUG_MODE | DEBUG_IR_GENERATION;
		}

		if (options.has("b")) {
			DEBUG_MODE = DEBUG_MODE | DEBUG_CODE_BLOCKS;
		}
		
		Integer allocatorMode = (Integer) options.valueOf("r");
		if(allocatorMode<0 || allocatorMode>2) {
			System.out.println("invalid allocator mode! " + allocatorMode);
			return;
		}
		ICodeGenerator allocator = numsToRegStrategies.get(allocatorMode);
		
		/* if an output file for assembly is requested, this is it */
		Path outputFile = null;

		if (options.has("o")) {
			String outputFileStr = (String) options.valueOf("o");
			outputFile = Paths.get(outputFileStr);
		}

		Object o = options.nonOptionArguments().get(0);
		String inputFileName = o.toString();

		InputStream nis;
		try {
			nis = new FileInputStream(inputFileName);
		} catch (FileNotFoundException e) {
			System.out.println("could not find input file specified!");
			return;
		}

		ANTLRInputStream input = null;
		TigerLexer lexer = null;
		TigerTokenStream tokens = null;
		TigerParser parser = null;
		Object ast = null;

		Utility.formatHeaderForOutput("");

		System.out.println("Reading input from file or stdin");
		try {
			input = new ANTLRInputStream(nis);
		} catch (Exception e) {
			System.err
					.println("error: unable to use input stream specified as ANTLRInputStream\nterminating...");
			System.exit(0);
		}

		lexer = new TigerLexer(input);
		tokens = testFlag(DEBUG_LEXERPARSER) ? new TigerDebugTokenStream(lexer)
				: new TigerTokenStream(lexer);
		parser = new TigerParser(tokens);

		/*
		 * for formatting purposes, add a line break between token names and
		 * error message in debug mode
		 */
		if (testFlag(DEBUG_LEXERPARSER)) {
			parser.errorPrefix = "\n\n";
		}

		System.out
				.printf("%sParsing input...%s\n",
						testFlag(DEBUG_LEXERPARSER) ? "Running in debug mode (tokens will be printed as parser consumes them)...\n"
								: "",
						testFlag(DEBUG_LEXERPARSER) ? "\n\nTokens consumed by parser:"
								: "");

		try {
			ast = parser.tiger_program().getTree();
		} catch (RecognitionException e) {
			System.err
					.println("error: parser threw RecognitionException while parsing input");
			e.printStackTrace();
			System.err.println("\nterminating...");
			System.exit(0);
		}

		if (parser.getNumberOfSyntaxErrors() == 0
				&& lexer.getNumberOfSyntaxErrors() == 0) {
			System.out.println("\n\nSuccessful parse!");
			walkAST(ast, outputFile, allocator);
		} else {
			System.out.println("\n\nUnsuccessful parse");
		}
		System.out.println(Utility.formatHeaderForOutput(""));

	}

	private static void walkAST(Object ast, Path outputFile, ICodeGenerator allocator) {
		CommonTreeNodeStream nodeStream = new CommonTreeNodeStream(ast);
		SymTableWalker walker = new SymTableWalker(nodeStream);

		CommonTreeNodeStream nodeStream2 = new CommonTreeNodeStream(ast);
		IRCodeWalker irCodeGenerator = new IRCodeWalker(nodeStream2);

		System.out
				.println("Building symbol table and performing semantic checks...");

		try {
			walker.walk();
			if (testFlag(DEBUG_SYMBOLTABLES)) {
				System.out.println(walker.globalSymbolTable
						.recursivePrettyPrintString());
			}

			/* do not generate IR code if semantically invalid */
			System.out.println("Successfully finished semantic checks");
			if (!walker.errors.isEmpty()) {
				System.out.println("ERRORS.");
				for (SemanticError e : walker.errors) {
					System.out.println(e.toString());
				}

				System.out.println("There were semantic errors!  Exiting.");
				return;
			} else {
				irCodeGenerator.globalSymbolTable = walker.globalSymbolTable;
				irCodeGenerator.walk();

				if (testFlag(DEBUG_SYMBOLTABLES)) {
					System.out.println(Utility
							.formatHeaderForOutput("TEMP SYMBOL TABLE"));
					System.out.println(irCodeGenerator.g.getTempSymbolTable()
							.recursivePrettyPrintString());
				}

				if (!testFlag(DEBUG_IR_GENERATION)) {
					System.out
							.println(Utility.formatHeaderForOutput("IR CODE"));
					irCodeGenerator.g.printString();
					System.out.println(Utility
							.formatHeaderForOutput("END IR CODE"));
				}
				List<IRInstruction> irInstructions = irCodeGenerator.g.codeList;
				BasicBlockBuilder blockBuilder = new BasicBlockBuilder(irInstructions);

				if (testFlag(DEBUG_CODE_BLOCKS)) {
					blockBuilder.printString();
					blockBuilder.printEdges();
				}
			
				/* if we are using the naive allocator, print to the screen.
				 * else, only file output is supported atm as the other ones
				 * currently do strange things when you use them more than once.
				 */
				if(NaiveRegisterAllocator.class.isAssignableFrom(allocator.getClass())){
					System.out
					.println(Utility.formatHeaderForOutput("MIPS CODE ?"));
					printMipsCodeToStream(System.out, irCodeGenerator, allocator, blockBuilder);
					System.out.println(Utility
							.formatHeaderForOutput("END MIPS CODE"));
				} else {
					System.out.println("if you would like to see MIPS output, please"
							+ " specify an output file!");
				}


				// now, if additional file output was requested, let's print
				// to file!
				if (outputFile != null) {

					try (OutputStream out = new BufferedOutputStream(
							Files.newOutputStream(outputFile,
									StandardOpenOption.CREATE,
									StandardOpenOption.WRITE,
									StandardOpenOption.TRUNCATE_EXISTING))) {
						printMipsCodeToStream(out, irCodeGenerator, allocator, blockBuilder);
					} catch (IOException x) {
						System.err.println(x);
					}
				}

			}

		} catch (RecognitionException e) {
			System.err
					.println("error: tree walker threw RecognitionException while walking tree");
			e.printStackTrace();
			System.err.println("\nterminating...");
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("\nterminating...");
			System.exit(0);
		}
	}

	public static void printMipsCodeToStream(OutputStream out,
			IRCodeWalker walker,  ICodeGenerator allocator, 
			BasicBlockBuilder builder) throws IOException {

		Object[] extraArgs = null;
				/* if we are using one of the allocators that needs
				 * input from the block builder, give them that input here.
				 */
				if (EBBRegisterAllocator.class.isAssignableFrom(
						allocator.getClass())
						|| BasicBlockRegisterAllocator.class.isAssignableFrom(
								allocator.getClass())) {
					extraArgs = new BasicBlockBuilder[]{builder};
				}

		NaiveInstructionSelector naiveSelector = new NaiveInstructionSelector();
		// %n is a handy syntax
		// for the platform-independent line separator
		CodeGenerationUtility.generateProperIRAndPrintToStream(out,
				walker.globalSymbolTable, walker.g,
				allocator::generateCode,
				naiveSelector::generateCode,
				extraArgs);
	}
}
