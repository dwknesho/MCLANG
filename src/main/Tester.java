package main;

import errors.ErrorReporter; 
import grtree.Tree;
import interpreter.Interpreter;
import lexer.Scanner;
import parser.ParseTable;
import parser.Parser;
import runtime.SymbolTable;

public class Tester {
    public static void main(String[] args) {

        String codePath = "sourceCodes/valid1.txt"; 
        // String codePath = "sourceCodes/valid2.txt"; 
        
        // ERROR #1: WANT condition must evaluate to QUALITY
        // String codePath = "sourceErrors/err01_want_condition_quality.txt";

        // ERROR #2: Variable in MENU not declared
        // String codePath = "sourceErrors/err02_menu_var_not_declared.txt";

        // ERROR #3: Variable not declared
        // String codePath = "sourceErrors/err03_var_not_declared.txt";

        // ERROR #4: Array requires an index
        // String codePath = "sourceErrors/err04_array_requires_index.txt";

        // ERROR #5: Array index must be PRICE
        // String codePath = "sourceErrors/err05_array_index_must_be_price.txt";

        // ERROR #6: Array index out of bounds
        // String codePath = "sourceErrors/err06_array_index_out_of_bounds.txt";

        // ERROR #7: Division by zero
        // String codePath = "sourceErrors/err07_division_by_zero.txt";

        // ERROR #8: Modulo by zero
        // String codePath = "sourceErrors/err08_modulo_by_zero.txt";

        // ERROR #9: Arithmetic operator requires PRICE operands
        // String codePath = "sourceErrors/err09_arithmetic_requires_price.txt";

        // ERROR #10: Logical operator requires QUALITY operands
        // String codePath = "sourceErrors/err10_logical_requires_quality.txt";

        // ERROR #11: Equality operator requires same type on both sides
        // String codePath = "sourceErrors/err11_equality_requires_same_type.txt";

        // ERROR #12: Unary NOT requires QUALITY
        // String codePath = "sourceErrors/err12_unary_not_requires_quality.txt";

        // ERROR #13: Unary minus requires PRICE
        // String codePath = "sourceErrors/err13_unary_minus_requires_price.txt";

        // ERROR #14: Unary plus requires PRICE
        // String codePath = "sourceErrors/err14_unary_plus_requires_price.txt";

        // ERROR #15: Array size must be PRICE
        // String codePath = "sourceErrors/err15_array_size_must_be_price.txt";

        // ERROR #16: Array index must be PRICE (Assignment)
        // String codePath = "sourceErrors/err16_array_index_must_be_price_assign.txt";

        // ERROR #17: Increment or Decrement must be PRICE
        // String codePath = "sourceErrors/err17_incre_decre_must_be_price.txt";

        // ERROR #18: Compound assignment must be PRICE
        // String codePath = "sourceErrors/err18_compound_assign_must_be_price.txt";

        // ERROR #19: Division by zero (Compound Assignment)
        // String codePath = "sourceErrors/err19_compound_division_by_zero.txt";

        // ERROR #20: Modulo by zero (Compound Assignment)
        // String codePath = "sourceErrors/err20_compound_modulo_by_zero.txt";

        // ERROR #21: Variable not declared for ORDER
        // String codePath = "sourceErrors/err21_var_not_declared_for_order.txt";

        // ERROR #22: Invalid input for PRICE
        // String codePath = "sourceErrors/err22_invalid_input_for_price.txt";

        // ERROR #23: REFILL condition must evaluate to QUALITY
        // String codePath = "sourceErrors/err23_refill_condition_quality.txt";

        // ERROR #24: STIR condition must evaluate to QUALITY
        // String codePath = "sourceErrors/err24_stir_condition_quality.txt";

        // ERROR #25: PREP condition must evaluate to QUALITY
        // String codePath = "sourceErrors/err25_prep_condition_quality.txt";

        // ERROR #26: Invalid array access
        // String codePath = "sourceErrors/err26_invalid_array_access.txt";

        // ERROR #27: Task is not defined
        // String codePath = "sourceErrors/err27_task_not_defined.txt";

        // ERROR #28: Wrong number of arguments
        // String codePath = "sourceErrors/err28_wrong_number_of_args.txt";

        // ERROR #29: EMPTY Task cannot YIELD a value
        // String codePath = "sourceErrors/err29_empty_task_cannot_yield.txt";

        // ERROR #30: Task must YIELD a value
        // String codePath = "sourceErrors/err30_task_must_yield_value.txt";

        // ERROR #31: Task returned the wrong data type
        // String codePath = "sourceErrors/err31_task_returned_wrong_type.txt";

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