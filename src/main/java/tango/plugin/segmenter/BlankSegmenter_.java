/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.plugin.segmenter;

import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import tango.dataStructure.InputImages;
import tango.parameter.Parameter;

/**
 *
 * @author thomasb
 */
public class BlankSegmenter_ implements NucleusSegmenter {

    Parameter[] par = {};
    
    @Override
    public ImageInt runNucleus(int currentStructureIdx, ImageHandler input, InputImages rawImages) {
        ImageInt ImageBlank = new ImageByte("Blank", input.sizeX, input.sizeY, input.sizeZ);
        ImageBlank.fill(1);
        
        return ImageBlank;
    }
    
    @Override
    public Parameter[] getParameters() {
        return par;
    }
    
    @Override
    public String getHelp() {
        return "Create a blank segmented image. Whole image will be considered as one nucleus.";
    }
    
    @Override
    public void setVerbose(boolean verbose) {
        
    }
    
    @Override
    public void setMultithread(int nCPUs) {
       
    }
    
}
