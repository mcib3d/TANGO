package tango.gui.util;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.text.JTextComponent;
import tango.gui.parameterPanel.ConfigurationElementPlugin;
/**
 *
 **
 * /**
 * Copyright (C) 2012 Jean Ollion
 *
 *
 *
 * This file is part of tango
 *
 * tango is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author 
 */
public class ContextMenuMouseListenerConfigutationList extends MouseAdapter {
    private JPopupMenu popup = new JPopupMenu();

    private Action activateAction;
    private Action desactivateAction;
    private Action activateAllAction;
    private Action desactivateAllAction;

    private JList jList;
    private ListModel listModel;
    private enum Actions { ACTIVATE, DESACTIVATE, ACTIVATEALL, DESACTIVATEALL};

    public ContextMenuMouseListenerConfigutationList(JList jList) {
        this.jList = jList;
        listModel = jList.getModel();
        activateAction = new AbstractAction("Activate") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setActivateSelectedElement(true);
            }
        };
        popup.add(activateAction);

        desactivateAction = new AbstractAction("Desactivate") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setActivateSelectedElement(false);
            }
        };
        popup.add(desactivateAction);
        popup.addSeparator();
        
        activateAllAction = new AbstractAction("Activate All") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setActivateAllElements(true);
            }
        };
        popup.add(activateAllAction);
        
        desactivateAllAction = new AbstractAction("Desactivate All") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setActivateAllElements(false);
            }
        };
        popup.add(desactivateAllAction);
    }

    /*@Override
    public void mouseClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            

            int nx = e.getX();

            if (nx > 500) {
                nx = nx - popup.getSize().width;
            }

            popup.show(e.getComponent(), nx, e.getY() - popup.getSize().height);
        }
    }*/
    
    public void mousePressed(MouseEvent e)  {check(e);}
    public void mouseReleased(MouseEvent e) {check(e);}

    public void check(MouseEvent e) {
        if (e.isPopupTrigger()) { //if the event shows the menu
            jList.setSelectedIndex(jList.locationToIndex(e.getPoint())); //select the item
            popup.show(jList, e.getX(), e.getY()); //and show the menu
        }
    }
    
    public void setActivateSelectedElement(boolean activate) {
        Object o = jList.getSelectedValue();
        if (o instanceof ConfigurationElementPlugin) ((ConfigurationElementPlugin)o).setActivated(activate);
        jList.updateUI();
    }
    
    public void setActivateAllElements(boolean activate) {
        for (int i = 0; i<listModel.getSize(); i++) {
            Object o = listModel.getElementAt(i);
            if (o instanceof ConfigurationElementPlugin) ((ConfigurationElementPlugin)o).setActivated(activate);
        }
        jList.updateUI();
    }
}

