package tango.gui.parameterPanel;

import java.awt.Color;
import tango.gui.parameterPanel.MultiParameterPanel;
import tango.gui.parameterPanel.ParameterPanelAbstract;
import java.awt.Container;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import tango.gui.parameterPanel.ConfigurationList;
import tango.helper.Helper;

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
public abstract class ConfigurationElementAbstract {
    protected ParameterPanelAbstract parameterPanel;
    protected Helper ml;
    protected JLabel label;
    protected Color color;

    public ConfigurationElementAbstract(ParameterPanelAbstract pp) {
        this.parameterPanel=pp;
        this.label = new JLabel(parameterPanel.getMPPLabel());
        parameterPanel.setMPPLabel(label);
        color=Color.BLACK;
    }
    
    public ParameterPanelAbstract getParameterPanel() {
        return parameterPanel;
    }
    
    /*public void off() {
        if (edit.isSelected()) updateValidity();
        edit.setSelected(false);
    }*/
    
    public Color updateValidity() {
        if (parameterPanel.checkValidity()) color = Color.BLACK;
        else color = Color.RED;
        return color;
    }
    
    public void setIdx(int idx) {
        this.parameterPanel.setIdx(idx);
    }
    public void register(Helper ml) {
        if (parameterPanel!=null && ml!=this.ml) parameterPanel.register(ml);
        this.ml=ml;
    }
    public void unRegister(Helper ml) {
        this.ml=null;
        if (parameterPanel!=null) parameterPanel.unRegister(ml);
    }
    
    public Color getColor() {
        return color;
    }
    
    public String getLabel() {
        return this.label.getText();
    }
}
