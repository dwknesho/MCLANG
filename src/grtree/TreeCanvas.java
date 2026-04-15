package grtree;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
* Canvas on which to paint the tree.
* Now includes interactive Mouse-Wheel Zooming!
*/
public class TreeCanvas extends Canvas {
   private Tree tree;
   private double scale = 1.0; // 1.0 means 100% (default size)
   
   public TreeCanvas(Tree t) { 
      tree = t; 
      this.setBackground(Color.white);

      // Listen for the user scrolling the mouse wheel
      this.addMouseWheelListener(new MouseWheelListener() {
          @Override
          public void mouseWheelMoved(MouseWheelEvent e) {
              if (e.getWheelRotation() < 0) {
                  // Scrolled up -> Zoom In
                  scale *= 1.1; 
              } else {
                  // Scrolled down -> Zoom Out
                  scale /= 1.1; 
              }
              // Force the canvas to redraw itself with the new zoom!
              repaint(); 
          }
      });
   }

   public void setTree(Tree t) { tree = t; }

   public void paint(Graphics g) {
      // Upgrade the standard Graphics to Graphics2D to unlock scaling
      Graphics2D g2d = (Graphics2D) g;

      // Apply the zoom!
      g2d.scale(scale, scale);

      Dimension d = this.getSize();
      int w = tree.getTreeWidth(g2d);
      
      // We must divide the width and height by the scale so the centering 
      // math doesn't break when you zoom way out
      int centerX = (int) ((d.width / scale - w) / 2);
      int topY = (int) (30 / scale);

      // Draw the tree!
      tree.drawTree(g2d, centerX, topY);
   }     
}