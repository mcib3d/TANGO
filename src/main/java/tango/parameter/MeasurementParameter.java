package tango.parameter;

import com.mongodb.BasicDBObject;
import ij.gui.GenericDialog;
import tango.dataStructure.InputCellImages;
import tango.dataStructure.ObjectQuantifications;
import tango.dataStructure.SegmentedCellImages;
import tango.plugin.PluginFactory;
import tango.plugin.measurement.Measurement;
import tango.plugin.measurement.MeasurementObject;
import tango.plugin.measurement.MeasurementStructure;
import tango.util.Utils;

import javax.swing.*;
import java.util.Set;

/**
 * *
 * /**
 * Copyright (C) 2012 Jean Ollion
 * <p>
 * <p>
 * <p>
 * This file is part of tango
 * <p>
 * tango is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Jean Ollion
 */
public class MeasurementParameter extends PluginParameter {

    Box keyBox;
    Parameter[] currentKeys;

    // FIXME besoin de gérer les clés?
    public MeasurementParameter(String label, String id, String defMethod) {
        super(label, id, defMethod);
        initChoice();
    }

    public MeasurementParameter(String label, String id, String defMethod, Parameter[] defParameters) {
        super(label, id, defMethod);
        initChoice();
        setContent(currentParameters, defParameters);
    }

    @Override
    protected void init(String label, String defMethod) {
        super.init(label, defMethod);
        keyBox = Box.createVerticalBox();
        keyBox.add(new JLabel("Keys:"));
        //mainBox.add(keyBox);
    }

    @Override
    public void dbGet(BasicDBObject DBO) {
        if (DBO.containsField(id)) {
            Object o = DBO.get(id);
            if (!(o instanceof BasicDBObject)) return;
            BasicDBObject subDBO = (BasicDBObject) o;
            String m = subDBO.getString("method");
            if (m != null && m.length() > 0) {
                if (plugin != null) {
                    for (Parameter p : getParameters()) p.removeFromContainer(mainBox);
                    Parameter[] keys = getKeys();
                    if (keys != null) for (Parameter p : keys) p.removeFromContainer(keyBox);
                }
                getPlug(m);
                if (plugin != null) {
                    for (Parameter p : getParameters()) p.dbGet(subDBO);
                    if (subDBO.containsField("keys")) {
                        BasicDBObject keysDBO = (BasicDBObject) subDBO.get("keys");
                        Parameter[] keys = getKeys();
                        if (keys != null) for (Parameter p : keys) p.dbGet(keysDBO);
                    }
                    displayParameters();
                }
                selecting = true;
                choice.setSelectedItem(m);
                selecting = false;
            }
        }
        setColor();
    }

    @Override
    protected void displayParameters() {
        super.displayParameters();
        if (plugin != null) {
            /*currentKeys = getKeys();
            if (currentKeys!=null) {
                for (Parameter p : currentKeys) {
                    //p.setParent(this);
                    p.addToContainer(keyBox);
                }
            }
            */
        }
    }

    public Parameter[] getKeys() {
        if (plugin != null) {
            return ((Measurement) plugin).getKeys();
        } else return null;
    }

    @Override
    public Parameter duplicate(String newLabel, String newId) {
        return new MeasurementParameter(newLabel, newId, Utils.getSelectedString(choice));
    }

    @Override
    protected void initChoice() {
        Set<String> list = getMeasurementList();
        selecting = true;
        choice.addItem(" ");
        for (String s : list) {
            choice.addItem(s);
        }
        if (defMethod != null && defMethod.length() > 0) {
            choice.setSelectedItem(defMethod);
            majPanel();
        }
        selecting = false;
    }

    protected Set<String> getMeasurementList() {
        return PluginFactory.getMeasurementList();
    }

    @Override
    protected void getPlugin(String method) {
        plugin = PluginFactory.getMeasurement(method);
    }

    public Measurement getMeasurement() {
        if (plugin != null) return (Measurement) plugin;
        else return null;
    }

    public boolean isMeasurementStructure() {
        return plugin instanceof MeasurementStructure;
    }

    public ObjectQuantifications getMeasureObject(InputCellImages rawImages, SegmentedCellImages segmentedImages) {
        if (plugin != null) {
            if (plugin instanceof MeasurementObject) {
                MeasurementObject mo = (MeasurementObject) plugin;
                int s = mo.getStructure();
                if (s >= 0) {
                    ObjectQuantifications res = new ObjectQuantifications(segmentedImages.getObjects(s).length);
                    mo.getMeasure(rawImages, segmentedImages, res);
                    return res;
                }

            }
        }
        return null;
    }
    

    
    /*@Override
    public Thresholder getPlugin(int nCPUs, boolean verbose) {
        if (plugin!=null) return (Thresholder)super.getPlugin(nCPUs, verbose);
        else return null;
    }
    * 
    */

    @Override
    public void addToGenericDialog(GenericDialog gd) {
    }
}
