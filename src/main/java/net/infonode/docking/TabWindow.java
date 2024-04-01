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


// $Id: TabWindow.java,v 1.40 2005/02/16 11:28:14 jesper Exp $
package net.infonode.docking;

import net.infonode.docking.drag.DockingWindowDragSource;
import net.infonode.docking.drag.DockingWindowDragger;
import net.infonode.docking.drag.DockingWindowDraggerProvider;
import net.infonode.docking.internal.WriteContext;
import net.infonode.docking.internalutil.*;
import net.infonode.docking.model.TabWindowItem;
import net.infonode.docking.model.ViewWriter;
import net.infonode.docking.properties.TabWindowProperties;
import net.infonode.properties.propertymap.PropertyMap;
import net.infonode.tabbedpanel.TabAdapter;
import net.infonode.tabbedpanel.TabEvent;
import net.infonode.tabbedpanel.TabRemovedEvent;
import net.infonode.util.ArrayUtil;
import net.infonode.util.Direction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * A docking window containing a tabbed panel.
 *
 * @author $Author: jesper $
 * @version $Revision: 1.40 $
 */
public class TabWindow extends AbstractTabWindow {
  private static final ButtonInfo[] buttonInfos = {
    new MinimizeButtonInfo(TabWindowProperties.MINIMIZE_BUTTON_PROPERTIES),
    new MaximizeButtonInfo(TabWindowProperties.MAXIMIZE_BUTTON_PROPERTIES),
    new RestoreButtonInfo(TabWindowProperties.RESTORE_BUTTON_PROPERTIES),
    new CloseButtonInfo(TabWindowProperties.CLOSE_BUTTON_PROPERTIES)
  };

  private AbstractButton[] buttons = new AbstractButton[buttonInfos.length];

  /**
   * Creates an empty tab window.
   */
  public TabWindow() {
    this((DockingWindow) null);
  }

  /**
   * Creates a tab window with a tab containing the child window.
   *
   * @param window the child window
   */
  public TabWindow(DockingWindow window) {
    this(window == null ? null : new DockingWindow[]{window});
  }

  /**
   * Creates a tab window with tabs for the child windows.
   *
   * @param windows the child windows
   */
  public TabWindow(DockingWindow[] windows) {
    this(windows, null);
  }

  protected TabWindow(DockingWindow[] windows, TabWindowItem windowItem) {
    super(true, windowItem == null ? new TabWindowItem() : windowItem);

    setTabWindowProperties(((TabWindowItem) getWindowItem()).getTabWindowProperties());

    new DockingWindowDragSource(getTabbedPanel(), new DockingWindowDraggerProvider() {
      public DockingWindowDragger getDragger(MouseEvent mouseEvent) {
        if (!getWindowProperties().getDragEnabled())
          return null;

        Point p = SwingUtilities.convertPoint((Component) mouseEvent.getSource(),
                                              mouseEvent.getPoint(),
                                              getTabbedPanel());

        return getTabbedPanel().tabAreaContainsPoint(p) ?
               (getChildWindowCount() == 1 ? getChildWindow(0) : TabWindow.this).startDrag(getRootWindow()) :
               null;
      }
    });

    init();

    getTabbedPanel().addTabListener(new TabAdapter() {
      public void tabAdded(TabEvent event) {
        update();
      }

      public void tabRemoved(TabRemovedEvent event) {
        update();
      }
    });

    if (windows != null) {
      for (int i = 0; i < windows.length; i++)
        addTab(windows[i]);
    }
  }

  public TabWindowProperties getTabWindowProperties() {
    return ((TabWindowItem) getWindowItem()).getTabWindowProperties();
  }

  protected void tabSelected(WindowTab tab) {
    super.tabSelected(tab);

    if (getUpdateModel()) {
      ((TabWindowItem) getWindowItem()).setSelectedItem(
          tab == null ? null : getWindowItem().getChildWindowContaining(tab.getWindow().getWindowItem()));
    }
  }

  protected void maximized(boolean maximized) {
    super.maximized(maximized);
    update();
  }

  protected void update() {
    if (InternalDockingUtil.updateButtons(buttonInfos, buttons, null, this, getTabWindowProperties().getMap())) {
      updateTabAreaComponents();
    }
  }

  protected int getTabAreaComponentCount() {
    return ArrayUtil.countNotNull(buttons);
  }

  protected void getTabAreaComponents(int index, JComponent[] components) {
    for (int i = 0; i < buttons.length; i++)
      if (buttons[i] != null)
        components[index++] = buttons[i];
  }

  protected void optimizeWindowLayout() {
    if (getWindowParent() == null)
      return;

    if (getTabbedPanel().getTabCount() == 0)
      internalClose();
    else if (getTabbedPanel().getTabCount() == 1 &&
             (getWindowParent().showsWindowTitle() || !getChildWindow(0).needsTitleWindow())) {
      getWindowParent().internalReplaceChildWindow(this, getChildWindow(0).getBestFittedWindow(getWindowParent()));
    }
  }

  public int addTab(DockingWindow w, int index) {
    int actualIndex = super.addTab(w, index);
    setSelectedTab(actualIndex);
    return actualIndex;
  }

  protected int addTabNoSelect(DockingWindow window, int index) {
    DockingWindow beforeWindow = index == getChildWindowCount() ? null : getChildWindow(index);

    int i = super.addTabNoSelect(window, index);

    if (getUpdateModel()) {
      if (beforeWindow == null)
        getWindowItem().addWindow(window.getWindowItem());
      else {
        getWindowItem().addWindow(window.getWindowItem(),
                                  getWindowItem().getWindowIndex(
                                      getWindowItem().getChildWindowContaining(beforeWindow.getWindowItem())));
      }
    }

    return i;
  }

  protected void updateWindowItem(RootWindow rootWindow) {
    super.updateWindowItem(rootWindow);
    ((TabWindowItem) getWindowItem()).setParentTabWindowProperties(rootWindow == null ?
                                                                   TabWindowItem.emptyProperties :
                                                                   rootWindow.getRootWindowProperties()
                                                                   .getTabWindowProperties());
  }

  protected PropertyMap getPropertyObject() {
    return getTabWindowProperties().getMap();
  }

  protected PropertyMap createPropertyObject() {
    return new TabWindowProperties().getMap();
  }

  protected void updateMinimizable() {
    update();
    super.updateMinimizable();
  }

  protected int getEdgeDepth(Direction dir) {
    return dir == getTabbedPanel().getProperties().getTabAreaOrientation() ?
           1 :
           super.getEdgeDepth(dir);
  }

  protected int getChildEdgeDepth(DockingWindow window, Direction dir) {
    return dir == getTabbedPanel().getProperties().getTabAreaOrientation() ?
           0 :
           1 + super.getChildEdgeDepth(window, dir);
  }

  protected DockingWindow getOptimizedWindow() {
    return getChildWindowCount() == 1 ? getChildWindow(0).getOptimizedWindow() : super.getOptimizedWindow();
  }

  protected boolean acceptsSplitWith(DockingWindow window) {
    return super.acceptsSplitWith(window) && (getChildWindowCount() != 1 || getChildWindow(0) != window);
  }

  protected DockingWindow getBestFittedWindow(DockingWindow parentWindow) {
    return getChildWindowCount() == 1 && (!getChildWindow(0).needsTitleWindow() || parentWindow.showsWindowTitle()) ?
           getChildWindow(0).getBestFittedWindow(parentWindow) : this;
  }

  protected void write(ObjectOutputStream out, WriteContext context, ViewWriter viewWriter) throws IOException {
    out.writeInt(WindowIds.TAB);
    viewWriter.writeWindowItem(getWindowItem(), out, context);
    super.write(out, context, viewWriter);
  }


}
