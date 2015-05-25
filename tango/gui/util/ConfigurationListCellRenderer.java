package tango.gui.util;

import tango.gui.parameterPanel.ConfigurationElementAbstract;
import tango.dataStructure.AbstractStructure;
import tango.dataStructure.Cell;
import tango.dataStructure.Field;
import tango.dataStructure.Object3DGui;
import ij.IJ;
import java.awt.*;
import javax.swing.*;
import java.util.*;
import tango.dataStructure.*;
import tango.gui.parameterPanel.ConfigurationElementPlugin;
import tango.gui.parameterPanel.ParameterPanelAbstract;
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
 * @author Jean Ollion
 */
public class ConfigurationListCellRenderer extends DefaultListCellRenderer {
    public static Map<Color, Color> oppositeColors = Collections.unmodifiableMap(new HashMap<Color, Color>() {
        {
            put(Color.WHITE, Color.BLACK);
            put(Color.BLACK, Color.WHITE);
            put(Color.RED, Color.BLACK);
            put(Color.BLUE, Color.BLACK);
        }
    });
    public ConfigurationListCellRenderer() {
        setOpaque(true);
    }
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof ConfigurationElementAbstract) {
            ConfigurationElementAbstract ppa = (ConfigurationElementAbstract) value;
            if (ppa.getLabel()!=null && ppa.getLabel().length()>0) {
                if (ppa instanceof ConfigurationElementPlugin && !((ConfigurationElementPlugin)ppa).isActivated()) label.setText("<HTML><S>"+ppa.getLabel()+"</HTML></S>");
                else label.setText(ppa.getLabel());
            }
            else label.setText("new element...");
            label.setBackground(isSelected ? ppa.getColor() : Color.WHITE);
            label.setForeground(isSelected ? oppositeColors.get(ppa.getColor()) : ppa.getColor());
        }
        return label;
    }
}