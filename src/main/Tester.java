package main;

import lexer.Scanner;
import parser.ParseTable;
import parser.Parser;
import semantic.Interpreter;
import semantic.SymbolTable;
import errors.ErrorReporter;

// Tester is the entry point for Phase 4.
// It runs the full pipeline: scan -> parse -> print tree -> interpret
public class Tester {
    public static void main(String[] args) {

        String codePath = "test/josetest.txt";
        String csvPath  = "src/parser/LL1_PARSE_FINAL.csv";

        try {
            System.out.println("\n===================================================");
            System.out.println("         MCLANG INTERPRETER  -  Phase 4            ");
            System.out.println("===================================================\n");

            ErrorReporter reporter = new ErrorReporter();
            Scanner       scanner  = new Scanner(codePath);
            SymbolTable   symTable = new SymbolTable();
            ParseTable    table    = new ParseTable();

            table.loadCSV(csvPath);
            System.out.println("Parse table loaded: " + csvPath);
            System.out.println("Source file:        " + codePath + "\n");

            // --- Phase 3: Parse and build AST ---
            Parser parser = new Parser(scanner, table, reporter, symTable);
            grtree.Tree ast = parser.parseAndReturn(); // returns the AST root

            // Print the tree in text form (requirement: tester prints the tree)
            System.out.println("=== PARSE TREE (text form) ===");
            System.out.println(ast.toString());

            // Print any lexical / syntax errors collected during parsing
            reporter.printSummary();

            // Stop here if there were parse errors — the tree may be malformed
            if (reporter.hasErrors()) {
                System.out.println("[!] Parse errors found. Interpretation skipped.\n");
                symTable.printTable();
                return;
            }

            // --- Phase 4: Interpret the AST ---
            System.out.println("=== EXECUTION OUTPUT ===\n");
            Interpreter interpreter = new Interpreter(symTable);
            interpreter.interpret(ast);

            // Show the final state of the symbol table
            symTable.printTable();

        } catch (Exception e) {
            System.err.println("Fatal Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}