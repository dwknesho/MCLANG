package mclang.utils;

import java.io.*;

public class FileHandler {
    private BufferedReader reader;
    private int line;
    private int column;
    private Character pushedBackChar;  // For ungetChar (single character buffer)
    
    public FileHandler() {
        this.line = 1;
        this.column = 0;
        this.pushedBackChar = null;
    }
    
    /**
     * Opens a file for reading
     * @param filename Path to the input file
     * @return true if successful, false otherwise
     */
    public boolean openFile(String filename) {
        try {
            reader = new BufferedReader(new FileReader(filename));
            line = 1;
            column = 0;
            pushedBackChar = null;
            return true;
        } catch (FileNotFoundException e) {
            System.err.println("Error: File not found - " + filename);
            return false;
        }
    }
    
    /**
     * Gets the next character from the file
     * @return next character, or null if EOF
     */
    public Character getChar() {
        try {
            // Check if there's a pushed back character
            if (pushedBackChar != null) {
                char c = pushedBackChar;
                pushedBackChar = null;
                return c;
            }
            
            // Read next character from file
            int charCode = reader.read();
            
            // Check for EOF
            if (charCode == -1) {
                return null;
            }
            
            char c = (char) charCode;
            
            // Update position tracking
            if (c == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            
            return c;
            
        } catch (IOException e) {
            System.err.println("Error reading from file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Puts a character back into the input stream
     * Only supports putting back ONE character
     * @param c character to push back
     */
    public void ungetChar(char c) {
        pushedBackChar = c;
        
        // Adjust position tracking
        if (c == '\n') {
            line--;
        } else {
            column--;
        }
    }
    
    /**
     * Returns current line number
     * @return current line number
     */
    public int getCurrentLine() {
        return line;
    }
    
    /**
     * Returns current column number
     * @return current column number
     */
    public int getCurrentColumn() {
        return column;
    }
    
    /**
     * Closes the input file
     */
    public void closeFile() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing file: " + e.getMessage());
        }
    }
}
