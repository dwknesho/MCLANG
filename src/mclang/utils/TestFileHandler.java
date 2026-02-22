package mclang.utils;

/**
 * Simple test program for FileHandler
 */
public class TestFileHandler {
    public static void main(String[] args) {
        FileHandler fh = new FileHandler();
        
        // Test 1: Open file
        System.out.println("=== Test 1: Open File ===");
        if (fh.openFile("test/simple.txt")) {
            System.out.println("✓ File opened successfully");
        } else {
            System.out.println("✗ Failed to open file");
            return;
        }
        
        // Test 2: Read all characters
        System.out.println("\n=== Test 2: Read Characters ===");
        int count = 0;
        while (true) {
            Character c = fh.getChar();
            if (c == null) {
                System.out.println("\n✓ Reached EOF");
                break;
            }
            
            // Print character (show newlines as \\n)
            if (c == '\n') {
                System.out.print("\\n");
            } else {
                System.out.print(c);
            }
            count++;
        }
        System.out.println("\nTotal characters read: " + count);
        
        fh.closeFile();
        
        // Test 3: Read with line/column tracking
        System.out.println("\n=== Test 3: Line/Column Tracking ===");
        fh = new FileHandler();
        fh.openFile("test/simple.txt");
        
        for (int i = 0; i < 15; i++) {  // Read first 15 chars
            Character c = fh.getChar();
            if (c == null) break;
            
            System.out.printf("Char: '%c' | Line: %d | Column: %d\n", 
                            c == '\n' ? 'N' : c, 
                            fh.getCurrentLine(), 
                            fh.getCurrentColumn());
        }
        
        fh.closeFile();
        
        // Test 4: ungetChar
        System.out.println("\n=== Test 4: UngetChar ===");
        fh = new FileHandler();
        fh.openFile("test/simple.txt");
        
        Character c1 = fh.getChar();  // Read 'H'
        System.out.println("Read: " + c1);
        
        fh.ungetChar(c1);  // Push back 'H'
        System.out.println("Pushed back: " + c1);
        
        Character c2 = fh.getChar();  // Read 'H' again
        System.out.println("Read again: " + c2);
        
        if (c1.equals(c2)) {
            System.out.println("✓ UngetChar works correctly!");
        } else {
            System.out.println("✗ UngetChar failed");
        }
        
        fh.closeFile();
        
        System.out.println("\n=== All Tests Complete ===");
    }
}