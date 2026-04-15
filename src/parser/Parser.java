package parser;

import lexer.Scanner; 
import lexer.Token;
import semantic.SymbolTable;
import errors.SyntaxException;
import errors.LexicalException;
import errors.ErrorReporter;
import grtree.Tree;
import grtree.TreeScrollFrame;
import java.util.Stack;

public class Parser {
    private Scanner scanner;
    private ParseTable table;
    private ErrorReporter reporter;
    private SymbolTable symTable; // Added Symbol Table
    private int currentPrintLine = -1;

    // Updated Constructor
    public Parser(Scanner scanner, ParseTable table, ErrorReporter reporter, SymbolTable symTable) {
        this.scanner = scanner;
        this.table = table;
        this.reporter = reporter;
        this.symTable = symTable;
    }

    // UPDATED: Now prints tokens IMMEDIATELY and captures Identifiers!
    private Token fetchNextToken() {
        while (true) {
            try {
                Token t = scanner.getNextToken();
                
                // 1. Print the token exactly as it is scanned!
                if (t != null && !t.tokenName.equals("[EOF]")) {
                    printToken(t);
                }

                // 2. TEMPORARY PHASE 3 HACK: Add every <id> to the Symbol Table
                if (t != null && t.tokenName.equals("<id>")) {
                    if (symTable.getAttributes(t.lexeme) == null) {
                        symTable.declareVariable(t.lexeme, "Pending Phase 4");
                    }
                }
                
                return t;
            } catch (LexicalException le) {
                reporter.reportLexicalError(le.line, le.col, le.getMessage(), le.lexeme);
            } catch (Exception e) {
                return new Token("eof", "", null, -1, -1);
            }
        }
    }

    public void parse() throws Exception {
        Stack<Tree> stack = new Stack<>();
        Tree root = new Tree("PROGRAM"); 
        
        stack.push(new Tree("eof")); 
        stack.push(root);            

        System.out.println("\n\n\nStarting Syntax Analysis...\n");
        Token currentToken = fetchNextToken();

        while (!stack.isEmpty()) {
            Tree topNode = stack.peek(); 
            String topSymbol = topNode.data;

            if (topSymbol.equals("ε")) {
                stack.pop();
                continue; 
            }

            String tokenSymbol = normalizeToken(currentToken.tokenName);

            if (table.isTerminal(topSymbol)) {
                if (topSymbol.equals(tokenSymbol)) {
                    stack.pop(); 
                    
                    // NOTE: I removed printToken() from here! It is now safely in fetchNextToken()
                    
                    topNode.data = topSymbol + " (" + currentToken.lexeme + ")";
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = fetchNextToken(); 
                    }
                } else {
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Expected '" + topSymbol + "' but found '" + currentToken.lexeme + "'");
                    stack.pop(); 
                }
            } 
            else {
                String[] rhs = table.getRule(topSymbol, tokenSymbol);
                
                if (rhs == null) {
                    reporter.reportSyntaxError(currentToken.line, currentToken.col, 
                        "Unexpected token '" + currentToken.lexeme + "' while parsing " + topSymbol);
                    
                    if (!tokenSymbol.equals("eof")) {
                        currentToken = fetchNextToken();
                    } else {
                        stack.pop(); 
                    }
                } else {
                    stack.pop(); 
                    Tree[] children = new Tree[rhs.length];
                    for (int i = 0; i < rhs.length; i++) {
                        children[i] = new Tree(rhs[i]);
                        topNode.addChild(children[i]); 
                    }

                    for (int i = rhs.length - 1; i >= 0; i--) {
                        stack.push(children[i]);
                    }
                }
            }
        }
        
        System.out.println("\n\n\n\n\nParsing Complete!\n\n\n\n");
        new TreeScrollFrame(root);
    }

    private String normalizeToken(String tokenName) {
        if (tokenName.equals("[EOF]")) return "eof";
        return tokenName.replaceAll("[<>]", ""); 
    }

    private void printToken(Token token) {
        // Enforces the exact line breaks of the source file
        if (currentPrintLine != -1 && token.line > currentPrintLine) {
            System.out.println(); 
        }
        currentPrintLine = token.line;

        // Strip the brackets from the token name
        String name = token.tokenName.replaceAll("[<>]", ""); 
        
        // If it is an ID, Number, or String, print it in the "value (type)" format!
        if (name.equals("id") || name.equals("numlit") || name.equals("stringlit")) {
            
            // Map the token names to the prettier labels you requested
            String displayLabel = name;
            if (name.equals("id")) displayLabel = "identifier";
            if (name.equals("numlit")) displayLabel = "num_lit";
            if (name.equals("stringlit")) displayLabel = "string_lit";
            
            System.out.print(token.lexeme + " (" + displayLabel + ") ");
        } 
        // For everything else (keywords, operators), just print the token name
        else {
            System.out.print(name + " ");
        }
    }
}