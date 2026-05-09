package main;

import lexer.Scanner; 
import parser.ParseTable;
import parser.Parser;
import semantic.SymbolTable;
import semantic.Interpreter; 
import grtree.Tree;          
import errors.ErrorReporter;

public class Tester {
    public static void main(String[] args) {
        String codePath = "test/program1.txt";  // <-- Change this to test other files!
        String csvPath = "src/parser/LL1_PARSE_FINAL.csv"; 

        try {
            System.out.println("\n\n\n=======================================================");
            System.out.println(">>> ACTIVELY RUNNING FILE: " + codePath + " <<<");
            System.out.println("=======================================================\n");
            
            System.out.println("Initializing Program...");
            ErrorReporter reporter = new ErrorReporter();
            Scanner scanner = new Scanner(codePath);
            
            SymbolTable symTable = new SymbolTable(); 
            
            ParseTable table = new ParseTable();
            table.loadCSV(csvPath);
            System.out.println("Parse Table Loaded Successfully.");

            Parser parser = new Parser(scanner, table, reporter);
            Tree ast = parser.parse(); 

            reporter.printSummary();

            // --- PHASE 4: EXECUTE THE PROGRAM ---
            if (!reporter.hasErrors()) {
                Interpreter interpreter = new Interpreter(symTable);
                interpreter.execute(ast); 
            } else {
                System.out.println("\n[!] Execution skipped due to syntax errors.");
            }

            symTable.printTable();

        } catch (Exception e) {
            System.err.println("Fatal System Error: " + e.getMessage());
            e.printStackTrace(); 
        }
    }
}