/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.plugin.filter;

import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import tango.dataStructure.InputImages;
import tango.parameter.BooleanParameter;
import tango.parameter.Parameter;
import tango.plugin.filter.PreFilter;

/**
 *
 * @author thomasb
 */
public class ConvertToShort implements PreFilter {

    boolean debug;
    int nbCPUs = 1;

    BooleanParameter scale = new BooleanParameter("Scale values", "scale", false);
    Parameter[] parameters = new Parameter[]{scale};

    public ConvertToShort() {
    }

    @Override
    public ImageHandler runPreFilter(int currentStructureIdx, ImageHandler input, InputImages images) {
        if (input instanceof ImageFloat) {
            return ((ImageFloat) input).convertToShort(scale.getValue());
        } else {
            return input.duplicate();
        }
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public String getHelp() {
        return "Convert 32-bits image to 16-bits image.";
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.debug = verbose;
    }

    @Override
    public void setMultithread(int nCPUs) {
        this.nbCPUs = nCPUs;
    }

}
