package main;

import lexer.Scanner; 
import parser.ParseTable;
import parser.Parser;
import semantic.SymbolTable;
import errors.ErrorReporter;
import interpreter.Interpreter;
import grtree.Tree;

public class Tester {
    public static void main(String[] args) {
        String codePath = "test/new.txt"; 
        String csvPath = "src/parser/LL1_PARSE_FINAL.csv"; 

        try {
            System.out.println("\n\n\nInitializing Program...");
            
            ErrorReporter reporter = new ErrorReporter();
            Scanner scanner = new Scanner(codePath);
            SymbolTable symTable = new SymbolTable(); 
            
            ParseTable table = new ParseTable();
            table.loadCSV(csvPath);
            System.out.println("\nParse Table Loaded Successfully.");

            // 1. Pass the Symbol Table into the Parser and capture the AST
            Parser parser = new Parser(scanner, table, reporter, symTable);
            Tree ast = parser.parse();

            // 2. Print the compilation summary
            reporter.printSummary();

            // 3. If there are no syntax errors, run the Interpreter!
            if (!reporter.hasErrors()) {
                symTable.reset(); // CRITICAL: Wipe parser's leftover data before running
                
                Interpreter interpreter = new Interpreter(symTable, reporter);
                interpreter.execute(ast);
            }

            // 4. Print the final memory state of the Symbol Table
            symTable.printTable();

        } catch (Exception e) {
            System.err.println("Fatal System Error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}