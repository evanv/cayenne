package org.objectstyle.cayenne.gui;
/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002 The ObjectStyle Group
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.*;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.*;
import org.apache.commons.collections.ExtendedProperties;
import org.objectstyle.cayenne.access.*;
import org.objectstyle.cayenne.map.*;
import org.objectstyle.cayenne.util.Preferences;
import org.objectstyle.cayenne.gui.util.*;


/** Allows adding data maps from the current domain to the data node. */
public class AddDataMapDialog extends JDialog implements ActionListener
{
	private static final int WIDTH  = 380;
	private static final int HEIGHT = 150;

	DataNode node;

	private JList list;
	private JButton add = new JButton("Add");
	private JButton cancel = new JButton("Cancel");
	
	public AddDataMapDialog(DataNode temp_node, List map_list) {
		super(Editor.getFrame(), "Add data maps to the data node", true);

		DataMap[] maps = temp_node.getDataMaps();
		if (map_list.size() == maps.length) {
			dispose();
			return;
		}

		node = temp_node;
		
		getContentPane().setLayout(new BorderLayout());
		
		list = new JList(populate(temp_node, map_list));
		getContentPane().add(list, BorderLayout.CENTER);
		
		JPanel temp = new JPanel(new FlowLayout(FlowLayout.CENTER));
		temp.add(add);
		temp.add(cancel);
		add.addActionListener(this);
		cancel.addActionListener(this);
		getContentPane().add(temp, BorderLayout.SOUTH);

		setSize(WIDTH, HEIGHT);
		JFrame frame = Editor.getFrame();
		Point point = frame.getLocationOnScreen();
		int width = frame.getWidth();
		int x = (width - WIDTH)/2;
		int height = frame.getHeight();
		int y = (height - HEIGHT)/2;
		
		point.setLocation(point.x + x, point.y + y);
		this.setLocation(point);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}
	
	private Vector populate(DataNode temp_node, List map_list) {
		DataMap[] maps = temp_node.getDataMaps();
		Vector new_maps = new Vector();
		Iterator iter = map_list.iterator();
		while(iter.hasNext()) {
			DataMap map = (DataMap)iter.next();
			boolean found = false;
			for (int i = 0; maps != null && i < maps.length; i++) {
				if (map == maps[i]) {
					found = true;
					break;
				}
			}// End for()
			if (!found)
				new_maps.add(new DataMapWrapper(map));
		}// End while()
		return new_maps;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == add) {
			Object[] sel = list.getSelectedValues();
			DataMap [] old_maps = node.getDataMaps();
			DataMap[] new_maps = new DataMap[old_maps.length + sel.length];
			for (int i = 0; i < old_maps.length; i++) {
				new_maps[i] = old_maps[i];
			}
			for (int i = 0; i < sel.length; i++) {
				new_maps[i + old_maps.length] = ((DataMapWrapper)sel[i]).getDataMap();
			}
			node.setDataMaps(new_maps);
		}// End add
		setVisible(false);
		dispose();
	}
}
	