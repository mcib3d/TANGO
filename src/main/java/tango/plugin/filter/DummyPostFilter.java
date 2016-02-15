/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.plugin.filter;

import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import tango.dataStructure.InputImages;
import tango.parameter.Parameter;

/**
 *
 * @author jollion
 */
public class DummyPostFilter implements PostFilter{

    @Override
    public Parameter[] getParameters() {
        return new Parameter[0];
    }

    @Override
    public String getHelp() {
        return "";
    }

    @Override
    public void setVerbose(boolean verbose) {
        
    }

    @Override
    public void setMultithread(int nCPUs) {
        
    }

    @Override
    public ImageInt runPostFilter(int currentStructureIdx, ImageInt input, InputImages images) {
        return input;
    }
    
}
