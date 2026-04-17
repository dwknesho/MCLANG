package main;

import lexer.Scanner; 
import parser.ParseTable;
import parser.Parser;
import semantic.SymbolTable; // Import the Symbol Table!
import errors.ErrorReporter;

public class Interpreter {
    public static void main(String[] args) {
        String codePath = "test/program1.txt"; 
        String csvPath = "src/parser/LL1_PARSE_FINAL.csv"; 

        try {
            System.out.println("\n\n\nInitializing Interpreter...");
            
            ErrorReporter reporter = new ErrorReporter();
            Scanner scanner = new Scanner(codePath);
            SymbolTable symTable = new SymbolTable(); // 1. Create the Symbol Table
            
            ParseTable table = new ParseTable();
            table.loadCSV(csvPath);
            System.out.println("\n\n\nParse Table Loaded Successfully.");

            // 2. Pass the Symbol Table into the Parser
            Parser parser = new Parser(scanner, table, reporter, symTable);
            parser.parse();

            // 3. Print the Errors AND the Symbol Table at the end!
            reporter.printSummary();
            symTable.printTable();

        } catch (Exception e) {
            System.err.println("Fatal System Error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}