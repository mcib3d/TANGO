/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.plugin.filter;

import ij.IJ;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.CannyEdge3D;
import tango.dataStructure.InputImages;
import tango.parameter.BooleanParameter;
import tango.parameter.DoubleParameter;
import tango.parameter.LabelParameter;
import tango.parameter.Parameter;
import tango.parameter.SliderParameter;

/**
 *
 * @author thomasb
 */
public class SymmetryFilter implements PreFilter {

    BooleanParameter smooth = new BooleanParameter("Use smooth version :", "smooth", true);
    DoubleParameter alpha = new DoubleParameter("Alpha for edge :", "alpha", 1.0, Parameter.nfDEC2);
    LabelParameter option = new LabelParameter("Options");
    SliderParameter radius = new SliderParameter("Radius :", "radius", 1, 100, 10);
    DoubleParameter scaling = new DoubleParameter("Scaling :", "scaling", 2.0, Parameter.nfDEC2);
    DoubleParameter normalize = new DoubleParameter("Normalize :", "normalize", 10.0, Parameter.nfDEC2);
    BooleanParameter improved = new BooleanParameter("Improved seed detection :", "improved", true);
    Parameter[] parameters = new Parameter[]{radius, alpha, improved, smooth, option, normalize, scaling};

    boolean verbose;

    public SymmetryFilter() {
        smooth.setHelp("Performs a 3D gaussian smoothing of the result", true);
        alpha.setHelp("The alpha value of the Canny edge detector, the smaller the value, the smoother the detection", true);
        option.setHelp("Optional values, do not change if you do not know", true);
        radius.setHelp("Radius of the object, in pixel", true);
        improved.setHelp("Improved scheme for seed detection, deselect for object detection", true);
    }

    @Override
    public ImageHandler runPreFilter(int currentStructureIdx, ImageHandler input, InputImages images) {
        CannyEdge3D edges = new CannyEdge3D(input, alpha.getDoubleValue(1.0));
        if (verbose) {
            IJ.log("Computing gradients ... ");
        }
        ImageHandler[] gg = edges.getGradientsXYZ();
        mcib3d.image3d.processing.SymmetryFilter sy = new mcib3d.image3d.processing.SymmetryFilter(gg, radius.getValue(), improved.getValue());
        if (verbose) {
            IJ.log("Computing symmetry ... ");
        }
        return sy.getSymmetry(smooth.getValue());
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public String getHelp() {
        return "Symmetry filter based on edge detection (experimental)";
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setMultithread(int nCPUs) {

    }

}
