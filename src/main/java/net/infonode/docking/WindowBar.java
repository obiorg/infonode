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


// $Id: WindowBar.java,v 1.51 2005/02/16 11:28:14 jesper Exp $
package net.infonode.docking;

import net.infonode.docking.internal.ReadContext;
import net.infonode.docking.internal.WriteContext;
import net.infonode.docking.internalutil.DropAction;
import net.infonode.docking.model.ViewReader;
import net.infonode.docking.model.ViewWriter;
import net.infonode.docking.model.WindowBarItem;
import net.infonode.docking.properties.TabWindowProperties;
import net.infonode.docking.properties.WindowBarProperties;
import net.infonode.docking.util.DockingUtil;
import net.infonode.gui.panel.ResizablePanel;
import net.infonode.properties.propertymap.PropertyMap;
import net.infonode.tabbedpanel.TabContentPanel;
import net.infonode.tabbedpanel.TabbedPanelContentPanel;
import net.infonode.util.Direction;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A window bar is located at the edge of a root window.
 * It's a tabbed panel where the content panel is dynamically shown and hidden.
 * A window bar is enabled and disabled using the {@link Component#setEnabled} method.
 *
 * @author $Author: jesper $
 * @version $Revision: 1.51 $
 */
public class WindowBar extends AbstractTabWindow {
  private RootWindow rootWindow;
  private Direction direction;
  private TabbedPanelContentPanel contentPanel;
  private ResizablePanel edgePanel;

  WindowBar(RootWindow rootWindow, Direction direction) {
    super(false, new WindowBarItem());

    this.rootWindow = rootWindow;
    contentPanel = new TabbedPanelContentPanel(getTabbedPanel(), new TabContentPanel(getTabbedPanel()));

    this.direction = direction;

    {
      WindowBarProperties properties = new WindowBarProperties();
      properties.getTabWindowProperties().addSuperObject(rootWindow.getRootWindowProperties().getTabWindowProperties());

      ((WindowBarItem) getWindowItem()).setWindowBarProperties(new WindowBarProperties(properties));
      getWindowBarProperties().addSuperObject(rootWindow.getRootWindowProperties().getWindowBarProperties());
      getWindowBarProperties().addSuperObject(WindowBarProperties.createDefault(this.direction));
    }

    edgePanel = new ResizablePanel(this.direction.getOpposite(), contentPanel);
    edgePanel.setPreferredSize(new Dimension(200, 200));
    edgePanel.setVisible(false);
    edgePanel.setComponent(contentPanel);

    setTabWindowProperties(getWindowBarProperties().getTabWindowProperties());
    init();
  }

  public TabWindowProperties getTabWindowProperties() {
    return getWindowBarProperties().getTabWindowProperties();
  }

  /**
   * Returns the property values for this window bar.
   *
   * @return the property values for this window bar
   */
  public WindowBarProperties getWindowBarProperties() {
    return ((WindowBarItem) getWindowItem()).getWindowBarProperties();
  }

  protected int addTabNoSelect(DockingWindow window, int index) {
    index = super.addTabNoSelect(window, index);
    window.setLastMinimizedDirection(direction);
    return index;
  }

  /**
   * Sets the size of the content panel.
   * If the window bar is located on the left or right side, the panel width is set otherwise the panel height.
   *
   * @param size the content panel size
   */
  public void setContentPanelSize(int size) {
    edgePanel.setPreferredSize(direction.isHorizontal() ? new Dimension(size, 0) : new Dimension(0, size));
  }

  /**
   * Returns the size of the content panel.
   * If the window bar is located on the left or right side, the panel width is returned otherwise the panel height.
   *
   * @return the size of the content panel
   */
  public int getContentPanelSize() {
    Dimension size = edgePanel.getPreferredSize();
    return direction.isHorizontal() ? size.width : size.height;
  }

  public RootWindow getRootWindow() {
    return rootWindow;
  }

  protected void showChildWindow(DockingWindow window) {
    int index = getChildWindowIndex(window);

    if (index != -1)
      setSelectedTab(index);

    super.showChildWindow(window);
  }

  ResizablePanel getEdgePanel() {
    return edgePanel;
  }

  protected void update() {
    edgePanel.setResizeWidth(getWindowBarProperties().getContentPanelEdgeResizeDistance());
    getWindowBarProperties().getComponentProperties().applyTo(this, direction.getNextCW());
  }

  public Dimension getPreferredSize() {
    if (isEnabled()) {
      Dimension size = super.getPreferredSize();
      int minWidth = getWindowBarProperties().getMinimumWidth();
      return new Dimension(Math.max(minWidth, size.width), Math.max(minWidth, size.height));
    }
    else
      return new Dimension(0, 0);
  }

  protected void tabSelected(WindowTab tab) {
    edgePanel.setVisible(tab != null);
    super.tabSelected(tab);
  }

  protected boolean isInsideTabArea(Point p2) {
    return true;
  }

  protected void clearFocus(View view) {
    super.clearFocus(view);

    if (view != null && !DockingUtil.isAncestor(this, view)) {
      getTabbedPanel().setSelectedTab(null);
    }
  }

  public boolean isMinimized() {
    return true;
  }

  protected boolean acceptsSplitWith(DockingWindow window) {
    return false;
  }

  DropAction acceptDrop(Point p, DockingWindow window) {
    return isEnabled() ? super.acceptDrop(p, window) : null;
  }

  protected PropertyMap getPropertyObject() {
    return getWindowBarProperties().getMap();
  }

  protected PropertyMap createPropertyObject() {
    return new WindowBarProperties().getMap();
  }

  protected void write(ObjectOutputStream out, WriteContext context, ViewWriter viewWriter) throws IOException {
    out.writeInt(getContentPanelSize());
    out.writeBoolean(isEnabled());
    getWindowItem().writeSettings(out, context);
    super.write(out, context, viewWriter);
  }

  protected DockingWindow newRead(ObjectInputStream in, ReadContext context, ViewReader viewReader) throws IOException {
    setContentPanelSize(in.readInt());
    setEnabled(in.readBoolean());
    getWindowItem().readSettings(in, context);
    super.newRead(in, context, viewReader);
    return this;
  }

  protected DockingWindow oldRead(ObjectInputStream in, ReadContext context) throws IOException {
    super.oldRead(in, context);
    setContentPanelSize(in.readInt());
    setEnabled(in.readBoolean());
    return this;
  }

}
