package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParseTable {
    // Map<NonTerminal, Map<Terminal, String[]>>
    private Map<String, Map<String, String[]>> table = new HashMap<>();
    private Set<String> terminalSet = new HashSet<>();

    public void loadCSV(String filePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine();
        if (line == null) return;

        // 1. Extract Terminals from Header
        String[] headers = line.split(",");
        for (int i = 1; i < headers.length; i++) {
            terminalSet.add(headers[i].trim());
        }

        // 2. Read the Rules
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            
            String[] cols = line.split(",", -1);
            // Clean the Non-Terminal (e.g., "<PROGRAM>" -> "PROGRAM")
            String nonTerminal = cols[0].trim().replaceAll("[<>]", ""); 

            Map<String, String[]> rowMap = new HashMap<>();
            
            for (int i = 1; i < cols.length; i++) {
                String cell = cols[i].trim();
                
                // Ignore empty cells
                if (!cell.isEmpty() && !cell.equals("-")) {
                    
                    // Split the rule by the arrow to get the Right-Hand Side
                    String[] parts = cell.split("→|->");
                    String rhs = parts.length > 1 ? parts[1].trim() : cell;
                    
                    // Handle Epsilon (Empty String)
                    if (rhs.equals("ε") || rhs.equalsIgnoreCase("epsilon")) {
                        rowMap.put(headers[i].trim(), new String[]{"ε"});
                    } else {
                        // Split by spaces and clean up brackets
                        String[] symbols = rhs.split("\\s+");
                        for (int j = 0; j < symbols.length; j++) {
                            symbols[j] = symbols[j].replaceAll("[<>]", "");
                        }
                        rowMap.put(headers[i].trim(), symbols);
                    }
                }
            }
            table.put(nonTerminal, rowMap);
        }
        br.close();
    }

    public String[] getRule(String nonTerminal, String terminal) {
        if (table.containsKey(nonTerminal)) {
            return table.get(nonTerminal).get(terminal);
        }
        return null;
    }

    public boolean isTerminal(String symbol) {
        return terminalSet.contains(symbol) || symbol.equals("ε");
    }
}