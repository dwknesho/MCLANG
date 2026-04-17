package grtree ;

import javax.swing.* ;
import java.awt.* ;
import java.awt.event.* ;

/**
* A TreeScrollFrame is a nice frame that paints its 
* tree.  The TreeScrollFrame uses a ScrollPane to move around 
* to view the tree.  The tree is drawn in the center of 
* a TreeCanvas which is added to the ScrollPane.
*/
public class TreeScrollFrame extends JFrame {
   private Tree tree ;
   
   public TreeScrollFrame(Tree t) { 
      super(t.data) ;  // title of frame shows root data
      tree = t ; 
      TreeCanvas tc = new TreeCanvas(tree) ;
      
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      // ScrollPane scrollpane = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED) ;
      // scrollpane.add(tc) ;
      // this.add(scrollpane) ;
      
      this.setLayout(new BorderLayout());
      this.add(tc, BorderLayout.CENTER);
      
      this.setSize(800, 600) ;
      this.setLocationRelativeTo(null);
      
      // WARNING -- THE ORDER OF THE FOLLOWING INSTRUCTIONS IS IMPORTANT!!!
      this.setVisible(true) ;  
      
      // need to get graphics for POSITIONING
      Graphics g = tc.getGraphics() ; 
      if (g != null) {
          int w = tree.getTreeWidth(g) ; 
          // tc needs be big enough to hold entire tree
          tc.repaint();
      }
      
      // Make root visible
      // scrollpane.setScrollPosition(w/2 - 200,0) ;
   }
}