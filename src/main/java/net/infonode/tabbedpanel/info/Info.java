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


// $Id: Info.java,v 1.7 2005/02/16 11:28:14 jesper Exp $

package net.infonode.tabbedpanel.info;

import net.infonode.gui.ReleaseInfoDialog;
import net.infonode.gui.laf.InfoNodeLookAndFeelReleaseInfo;
import net.infonode.tabbedpanel.TabbedPanelReleaseInfo;
import net.infonode.util.ReleaseInfo;

/**
 * Program that shows release information in a dialog
 *
 * @author $Author: jesper $
 * @version $Revision: 1.7 $
 */
public class Info {
  private Info() {
  }

  public static final void main(String[] args) {
    ReleaseInfoDialog.showDialog(
        new ReleaseInfo[]{TabbedPanelReleaseInfo.getReleaseInfo(), InfoNodeLookAndFeelReleaseInfo.getReleaseInfo()},
        null);
    System.exit(0);
  }
}