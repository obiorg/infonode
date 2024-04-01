/*
 * Copyright (C) 2004 NNL Technology AB
 * Visit www.infonode.net for information about InfoNode(R) 
 * products and how to contact NNL Technology AB.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, 
 * MA 02111-1307, USA.
 */


// $Id: HoverManager.java,v 1.10 2005/02/16 11:28:11 jesper Exp $

package net.infonode.gui.hover.hoverable;

import net.infonode.gui.ComponentUtil;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author johan
 */
public class HoverManager {
  private static HoverManager INSTANCE = new HoverManager();

  private HierarchyListener hierarchyListener = new HierarchyListener() {
    public void hierarchyChanged(final HierarchyEvent e) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            if (((Component) e.getSource()).isShowing()) {
              addHoverable((Hoverable) e.getSource());
            }
            else {
              removeHoverable((Hoverable) e.getSource());
            }
          }
        }
      });
    }
  };

  private MouseInputAdapter mouseAdapter = new MouseInputAdapter() {
  };

  private HashSet hoverableComponents = new HashSet();
  private ArrayList enteredComponents = new ArrayList();

  private boolean enabled = true;
  private boolean hasPermission = false;

  private boolean active = true;

  private boolean gotEnterAfterExit = false;
  private boolean isDrag = false;

  private AWTEventListener eventListener = new AWTEventListener() {
    public void eventDispatched(final AWTEvent e) {
      if (active) {
//        HoverManager.this.eventDispatched(e);
      }
    }
  };

  private void eventDispatched(final AWTEvent e) {
    MouseEvent event = (MouseEvent) e;

    if (event.getID() == MouseEvent.MOUSE_PRESSED || event.getID() == MouseEvent.MOUSE_RELEASED) {
      handleButtonEvent(event);
    }
    else if (event.getID() == MouseEvent.MOUSE_ENTERED || event.getID() == MouseEvent.MOUSE_MOVED) {
      handleEnterEvent(event);
    }
    else if (event.getID() == MouseEvent.MOUSE_EXITED) {
      handleExitEvent(event);
    }
    else if (event.getID() == MouseEvent.MOUSE_DRAGGED) {
      isDrag = true;
    }
  }

  private void handleButtonEvent(MouseEvent event) {
    if (event.getID() == MouseEvent.MOUSE_PRESSED && event.getButton() == MouseEvent.BUTTON1) {
      enabled = false;
      isDrag = false;
    }
    else if (!enabled && event.getID() == MouseEvent.MOUSE_RELEASED) {
      enabled = true;

      if (isDrag) {
        final Component top = ComponentUtil.getTopLevelAncestor((Component) event.getSource());
        if (top == null)
          exitAll();
        else if (!((Component) event.getSource()).contains(event.getPoint())) {
          final Point p = SwingUtilities.convertPoint((Component) event.getSource(), event.getPoint(), top);
          if (!top.contains(p.x, p.y)) {
            exitAll();
          }
          else if (top instanceof Container) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                    Component c = ComponentUtil.findComponentUnderGlassPaneAt(p, top);

                    if (c != null) {
                      Point p2 = SwingUtilities.convertPoint(top, p, c);
                      eventDispatched(new MouseEvent(c, MouseEvent.MOUSE_ENTERED, 0, 0, p2.x, p2.y, 0, false));
                    }
                  }
                });
              }
            });
          }
        }
      }
    }
  }

  private void handleEnterEvent(MouseEvent event) {
    gotEnterAfterExit = true;

    ArrayList exitables = new ArrayList(enteredComponents);
    ArrayList enterables = new ArrayList();

    if(event.getSource().getClass() == TrayIcon.class){
        System.out.println("Source is trayIcon");
    }
    Component c = (Component) event.getSource();
    while (c != null) {
      if (hoverableComponents.contains(c)) {
        exitables.remove(c);
        enterables.add(c);
      }

      c = c.getParent();
    }

    if (enterables.size() > 0) {
      Object obj[] = enterables.toArray();
      for (int i = obj.length - 1; i >= 0; i--) {
        if (!((Hoverable) obj[i]).acceptHover(enterables)) {
          enterables.remove(obj[i]);
          exitables.add(obj[i]);
        }
      }
    }

    for (int i = exitables.size() - 1; i >= 0; i--) {
      dispatchExit((Hoverable) exitables.get(i));
    }

    for (int i = enterables.size() - 1; i >= 0; i--) {
      dispatchEnter((Hoverable) enterables.get(i));
    }
  }

  private void handleExitEvent(MouseEvent event) {
    gotEnterAfterExit = false;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (!gotEnterAfterExit)
          exitAll();
      }
    });
  }

  public static HoverManager getInstance() {
    return INSTANCE;
  }

  private HoverManager() {
  }

  private void exitAll() {
    gotEnterAfterExit = false;
    Object[] obj = enteredComponents.toArray();
    for (int i = obj.length - 1; i >= 0; i--) {
      dispatchExit((Hoverable) obj[i]);
    }
  }

  public void init() {
    gotEnterAfterExit = false;
    isDrag = false;
    enabled = true;
  }

  public void setEventListeningActive(boolean active) {
    this.active = active;
  }

  public void dispatchEvent(MouseEvent event) {
    eventDispatched(event);
  }

  public void addHoverable(Hoverable hoverable) {
    if (hoverable instanceof Component && !hoverableComponents.contains(hoverable)) {
      if (active && hoverableComponents.size() == 0) {
        try {
          Toolkit.getDefaultToolkit().addAWTEventListener(eventListener,
                                                          AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
          hasPermission = true;
        }
        catch (SecurityException e) {
          hasPermission = false;
        }
      }

      addHierarchyListener((Component) hoverable);

      ((Component) hoverable).addMouseListener(mouseAdapter);
      ((Component) hoverable).addMouseMotionListener(mouseAdapter);
      hoverableComponents.add(hoverable);
    }
  }

  public void removeHoverable(Hoverable hoverable) {
    if (hoverableComponents.contains(hoverable)) {
      ((Component) hoverable).removeMouseListener(mouseAdapter);
      ((Component) hoverable).removeMouseMotionListener(mouseAdapter);
      hoverableComponents.remove(hoverable);
      if (enteredComponents.contains(hoverable)) {
        enteredComponents.remove(hoverable);
        ((Hoverable) hoverable).hoverExit();
      }

      if (hasPermission && hoverableComponents.size() == 0) {
        Toolkit.getDefaultToolkit().removeAWTEventListener(eventListener);
      }
    }
  }

  public boolean isHovered(Hoverable c) {
    return enteredComponents.contains(c);
  }

  public boolean isEventListeningActive() {
    return active && hasPermission;
  }

  private void addHierarchyListener(Component c) {
    HierarchyListener listeners[] = c.getHierarchyListeners();
    if (listeners.length == 0) {
      c.addHierarchyListener(hierarchyListener);
    }
    else {
      for (int i = 0; i < listeners.length; i++) {
        if (listeners[i] == hierarchyListener) {
          break;
        }
        else if (i == listeners.length - 1) {
          c.addHierarchyListener(hierarchyListener);
        }
      }
    }
  }

  private void dispatchEnter(Hoverable hoverable) {
    if (enabled && !enteredComponents.contains(hoverable)) {
      enteredComponents.add(hoverable);
      ((Hoverable) hoverable).hoverEnter();
    }
  }

  private void dispatchExit(Hoverable hoverable) {
    if (enabled) {
      enteredComponents.remove(hoverable);
      ((Hoverable) hoverable).hoverExit();
    }
  }
}