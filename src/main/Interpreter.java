package main;

import lexer.Scanner;
import errors.ErrorReporter;
import parser.ParseTable;
import parser.Parser;

public class Interpreter {
    public static void main(String[] args) {
        // NOTE: Make sure these paths match your actual file structure!
        String codePath = "test/program1.txt"; 
        
        // Pointing to your newly renamed V3 CSV file
        String csvPath = "src/parser/LL1 PARSING TABLE V3.csv"; 

        try {
            System.out.println("Initializing Interpreter...");
            
            // 1. Initialize the Lexer
            Scanner scanner = new Scanner(codePath);
            
            // 2. Load the LL(1) Parsing Table
            ParseTable table = new ParseTable();
            table.loadCSV(csvPath);
            System.out.println("Parse Table Loaded Successfully.");

            // 3. Initialize and Run the Parser
            Parser parser = new Parser(scanner, table);
            parser.parse();

        } catch (errors.SyntaxException se) {
            // If the parser crashes, report the syntax error!
            ErrorReporter reporter = new ErrorReporter();
            reporter.reportSyntaxError(se.line, se.col, se.getMessage());
            reporter.printSummary();
        } catch (Exception e) {
            System.err.println("Fatal System Error: " + e.getMessage());
        }
    }
}