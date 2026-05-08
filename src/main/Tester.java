package main;

import lexer.Scanner; 
import parser.ParseTable;
import parser.Parser;
import semantic.SymbolTable;
import errors.ErrorReporter;
import semantic.Interpreter;
import grtree.Tree;

public class Tester {
    public static void main(String[] args) {
        String codePath = "test/program1.txt"; 
        String csvPath = "src/parser/LL1_PARSE_FINAL.csv"; 

        try {
            System.out.println("\nInitializing Program...");
            ErrorReporter reporter = new ErrorReporter();
            Scanner scanner = new Scanner(codePath);
            SymbolTable symTable = new SymbolTable(); 
            
            ParseTable table = new ParseTable();
            table.loadCSV(csvPath);

            Parser parser = new Parser(scanner, table, reporter, symTable);
            
            // 1. RUN PARSER
            Tree ast = parser.parse(); // IMPORTANT: Update your Parser's parse() method to RETURN the ast instead of void!

            // 2. CHECK FOR ERRORS BEFORE EXECUTING
            reporter.printSummary();

            if (!reporter.hasErrors() && ast != null) {
                // 3. RUN INTERPRETER & TYPE CHECKER
                Interpreter interpreter = new Interpreter(symTable);
                interpreter.execute(ast);
                
                // 4. PRINT FINAL SYMBOL TABLE
                symTable.printTable();
            }

        } catch (Exception e) {
            System.err.println("Fatal System Error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}