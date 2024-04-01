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


// $Id: ConstChangeNotifyMap.java,v 1.6 2004/11/05 13:06:02 jesper Exp $
package net.infonode.util.collection.notifymap;

import net.infonode.util.collection.map.base.ConstMap;

public interface ConstChangeNotifyMap extends ConstMap {
  void addListener(ChangeNotifyMapListener listener);

//  void addWeakListener(ChangeNotifyMapListener listener);

  boolean removeListener(ChangeNotifyMapListener listener);
}