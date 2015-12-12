package tango.plugin.filter.mergeRegions;

import ij.IJ;
import tango.plugin.filter.*;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.AutoThresholder;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.*;
import tango.dataStructure.InputImages;
import tango.gui.Core;
import tango.parameter.*;
import tango.plugin.thresholder.AutoThreshold;
import tango.plugin.thresholder.Thresholder;

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
public class MergeRegions implements PostFilter {
    boolean debug;
    int nbCPUs=1;
    static String[] methods = new String[]{"Merge All Connected", "Merge All", "Merge Spots"};
    static String[] methodsMergeSort = new String[]{"Maximum Spotiness: Correlation Intensity-Distance", "Maximum Spotiness: Hessian"};
    static String[] sortMethods = new String[]{"Interface Integrated Intensity", "Interface Size", "Interface Mean Intensity"};
    
    //static String[] methodsTesting = new String[]{"Merge All Connected", "Merge All", "Merge Sort"};
    ChoiceParameter choice = new ChoiceParameter("Method:", "mergeMethod", methods, methods[0]);
    ChoiceParameter fusionCriterion = new ChoiceParameter("Fusion criterion", "fustionCriterion", methodsMergeSort, methodsMergeSort[1]); 
    //ChoiceParameter choice = new ChoiceParameter("Method:", "mergeMethod", Core.TESTING?methodsTesting:methods, methods[0]); 
    ConditionalParameter cond = new ConditionalParameter("Merge method", choice);
    ConditionalParameter condMergeSort = new ConditionalParameter("Criterion", fusionCriterion);
    DoubleParameter hessScale = new DoubleParameter("Integration Scale (Pix)", "scale", 1d, Parameter.nfDEC3);
    BooleanParameter hessUseScale = new BooleanParameter("Use Image Scale for Z radius", "useImageScale", true);
    SliderDoubleParameter erode = new SliderDoubleParameter("Proprotion of object erosion:", "erode", 0, 1, 0.8, 2);
    Parameter[] parameters=new Parameter[]{cond};
    
    //SliderDoubleParameter mergeCoeff = new SliderDoubleParameter("Merge Coefficient", "mergeCoeff", 0, 1, 0.75d, 3);
    //PreFilterParameter derivativeMap = new PreFilterParameter("Derivative Map:", "derivativeMap", "Image Features", new Parameter[]{new ChoiceParameter("", "", new String[]{"Gradient Magnitude"}, "Gradient Magnitude")}); 
    //ThresholdHistogramParameter derivativeThreshold = new ThresholdHistogramParameter("Derivative Limit:", "derivativeLimit", "AutoThreshold", new Parameter[]{new ChoiceParameter("", "", new String[]{"OTSU"}, "OTSU")});
    BooleanParameter filtered = new BooleanParameter("Use Filtered Intensity Image?", "useFiltered", true);
    
    public MergeRegions() {
        cond.setCondition(methods[2], new Parameter[]{filtered, condMergeSort});
        condMergeSort.setCondition(methodsMergeSort[1], new Parameter[]{hessScale, hessUseScale, erode});
    }
    
    @Override
    public ImageInt runPostFilter(int currentStructureIdx, ImageInt in, InputImages images) {
        IJ.log("merge region debug mode: "+debug);
        if (choice.getSelectedIndex()==0) mergeAllConnected(in, debug);
        else if (choice.getSelectedIndex()==1) mergeAll(in);
        else if (choice.getSelectedIndex()==2) {
            ImageHandler intensityMap = filtered.isSelected() ? images.getFilteredImage(currentStructureIdx) : images.getImage(currentStructureIdx);
            RegionCollection col = new RegionCollection(in,  intensityMap, debug, nbCPUs);
            if (fusionCriterion.getSelectedIndex()==1) {
                col.mergeSortHessianCond(hessScale.getDoubleValue(1), hessUseScale.isSelected(), erode.getValue());
            } else if (fusionCriterion.getSelectedIndex()==0) {
                col.mergeSortCorrelation();
            }
            
        }
        return in;
    }
    
    public static void mergeAllConnected(ImageInt input, boolean debug) {
        RegionCollection col = new RegionCollection(input, null, debug, 1);
        col.mergeAllConnected();
        col.shiftIndicies(false);
    }
    
    public static void mergeAll(ImageInt input) {
        for (int z = 0; z<input.sizeZ; z++) {
            for (int xy = 0; xy<input.sizeXY; xy++) {
                if (input.getPixelInt(xy, z)!=0) input.setPixel(xy, z, 1);
            }
        }
    }
    
    /*public static void mergeSort(ImageInt input, ImageHandler intensity, double derivativeLimit, boolean debug, int nbCPUs) {
        RegionCollection col = new RegionCollection(input, intensity, debug, nbCPUs);
        col.mergeSort(2, 0, derivativeLimit);
    }*/

    @Override
    public void setVerbose(boolean debug) {
        this.debug=debug;
    }

    @Override
    public void setMultithread(int nbCPUs) {
        this.nbCPUs=nbCPUs;
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public String getHelp() {
        return "Erase Objects according to their mean intensity. Implemented from a procedure designed by Philippe Andrey";
    }
    
}
