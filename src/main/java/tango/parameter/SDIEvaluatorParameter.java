package tango.parameter;

import ij.gui.GenericDialog;
import tango.plugin.PluginFactory;
import tango.spatialStatistics.SDIEvaluator.SDIEvaluator;
import tango.spatialStatistics.SDIEvaluator.SDIEvaluatorFactory;
import tango.util.Utils;

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
public class SDIEvaluatorParameter extends PluginParameter {

    public SDIEvaluatorParameter(String label, String id, String defMethod) {
        super(label, id, defMethod);
        initChoice();
    }

    @Override
    protected void initChoice() {
        selecting = true;
        choice.addItem(" ");
        for (String s : SDIEvaluatorFactory.evaluators) {
            choice.addItem(s);
        }
        if (defMethod != null && defMethod.length() > 0) {
            choice.setSelectedItem(defMethod);
            majPanel();
        }
        selecting = false;
    }

    @Override
    protected void getPlugin(String method) {
        plugin = SDIEvaluatorFactory.getEvaluator(method);
    }

    @Override
    public Parameter duplicate(String newLabel, String newId) {
        return new SDIEvaluatorParameter(newLabel, newId, Utils.getSelectedString(choice));
    }

    @Override
    public SDIEvaluator getPlugin(int nCPUs, boolean verbose) {
        if (plugin!=null) return (SDIEvaluator)super.getPlugin(nCPUs, verbose);
        else return null;
    }

    @Override
    public void addToGenericDialog(GenericDialog gd) {
    }
}
