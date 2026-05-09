package grtree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TreeCanvas extends JPanel {
   private Tree tree;
   private double scale = 1.0; // 1.0 means 100% (default size)
   
   private double translateX = 0;
   private double translateY = 30; 
   private int lastMouseX, lastMouseY;
   
   public TreeCanvas(Tree t) { 
      tree = t; 
      this.setBackground(Color.white);
      this.setDoubleBuffered(true); 

      // Listen for the user scrolling the mouse wheel
      this.addMouseWheelListener(new MouseWheelListener() {
          @Override
          public void mouseWheelMoved(MouseWheelEvent e) {
              // 1. Get exact mouse position for "Zoom to Cursor"
              int mouseX = e.getX();
              int mouseY = e.getY();

              // 2. Calculate world coordinates BEFORE zooming
              double worldXBefore = (mouseX / scale) - translateX;
              double worldYBefore = (mouseY / scale) - translateY;

              // 3. Use precise rotation for Mac Trackpad smooth scrolling
              double rotation = e.getPreciseWheelRotation();
              
              // NOTE: Mac trackpads use "Natural Scrolling" by default.
              // If pushing two fingers UP zooms the wrong way for you, 
              // simply remove the negative sign in front of '-rotation' below!
              double zoomFactor = Math.pow(1.05, -rotation);
              scale *= zoomFactor;

              // 4. Set limits so it doesn't zoom infinitely and crash
              if (scale < 0.1) scale = 0.1;
              if (scale > 10.0) scale = 10.0;

              // 5. Calculate world coordinates AFTER zooming
              double worldXAfter = (mouseX / scale) - translateX;
              double worldYAfter = (mouseY / scale) - translateY;

              // 6. Adjust translation so the tree stays exactly under the mouse pointer!
              translateX += (worldXAfter - worldXBefore);
              translateY += (worldYAfter - worldYBefore);

              repaint(); 
          }
      });

      this.addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
              lastMouseX = e.getX();
              lastMouseY = e.getY();
          }
      });

      this.addMouseMotionListener(new MouseMotionAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
              int dx = e.getX() - lastMouseX;
              int dy = e.getY() - lastMouseY;

              translateX += dx / scale;
              translateY += dy / scale;

              lastMouseX = e.getX();
              lastMouseY = e.getY();

              repaint();
          }
      });
   }

   public void setTree(Tree t) { 
       tree = t; 
       repaint(); 
   }

   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g); 

      // Upgrade the standard Graphics to Graphics2D to unlock scaling
      Graphics2D g2d = (Graphics2D) g;

      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Apply the zoom!
      g2d.scale(scale, scale);
      g2d.translate(translateX, translateY);

      Dimension d = this.getSize();
      int w = tree.getTreeWidth(g2d);
      
      // We must divide the width and height by the scale so the centering 
      // math doesn't break when you zoom way out
      int centerX = (int) ((d.width / scale - w) / 2);
      
      // Draw the tree!
      tree.drawTree(g2d, centerX, 0);
   }     
}

/* 
public class TreeCanvas extends JPanel {
   private Tree tree;
   private double scale = 1.0; // 1.0 means 100% (default size)
   
   private double translateX = 0;
   private double translateY = 30; 
   private int lastMouseX, lastMouseY;
   
   public TreeCanvas(Tree t) { 
      tree = t; 
      this.setBackground(Color.white);
      this.setDoubleBuffered(true); 

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

      this.addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
              lastMouseX = e.getX();
              lastMouseY = e.getY();
          }
      });

      this.addMouseMotionListener(new MouseMotionAdapter() {
          @Override
          public void mouseDragged(MouseEvent e) {
              int dx = e.getX() - lastMouseX;
              int dy = e.getY() - lastMouseY;

              translateX += dx / scale;
              translateY += dy / scale;

              lastMouseX = e.getX();
              lastMouseY = e.getY();

              repaint();
          }
      });
   }

   public void setTree(Tree t) { 
       tree = t; 
       repaint(); 
   }

   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g); 

      // Upgrade the standard Graphics to Graphics2D to unlock scaling
      Graphics2D g2d = (Graphics2D) g;

      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Apply the zoom!
      g2d.scale(scale, scale);
      g2d.translate(translateX, translateY);

      Dimension d = this.getSize();
      int w = tree.getTreeWidth(g2d);
      
      // We must divide the width and height by the scale so the centering 
      // math doesn't break when you zoom way out
      int centerX = (int) ((d.width / scale - w) / 2);
      
      // Draw the tree!
      tree.drawTree(g2d, centerX, 0);
   }     
}
*/