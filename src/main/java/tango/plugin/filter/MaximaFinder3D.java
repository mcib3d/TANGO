/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.plugin.filter;

import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.processing.MaximaFinder;
import tango.dataStructure.InputImages;
import tango.parameter.DoubleParameter;
import tango.parameter.Parameter;

/**
 *
 * @author thomasb
 */
public class MaximaFinder3D implements PreFilter {
    
    double radX = 2;
    double radZ = 1;
    double noise = 100;
    
    boolean verbose;
    int nCPUs;
    
    DoubleParameter radX_P = new DoubleParameter("VoisXY: ", "voisXY", radX, Parameter.nfDEC1);
    DoubleParameter radZ_P = new DoubleParameter("VoisZ: ", "voisZ", radZ, Parameter.nfDEC1);
    DoubleParameter noise_P = new DoubleParameter("Noise ", "noise", noise, Parameter.nfDEC1);
    Parameter[] parameters = new Parameter[]{radX_P, radZ_P, noise_P};
    
    public MaximaFinder3D() {
        radX_P.setHelp("The radius in <em>X</em> and <em>Y</em> direction");
        radZ_P.setHelp("The radius in <em>Z</em> direction");
        noise_P.setHelp("The noise in intensity value.");
        noise_P.setHelp("The noise parameter, in intensity value. Local maxima will be sorted by highet intensities. A 3D flooding will then be peformed for each seed using the noise parameter value"
                + " and seeds inside the flooding (below noise value from another seed) will be removed.", false);
    }

    public ImageHandler runPreFilter(int currentStructureIdx, ImageHandler input, InputImages images) {
        ImageHandler img = input;
        MaximaFinder maximaFinder = new MaximaFinder(img, noise_P.getFloatValue((float) noise));
        maximaFinder.setNbCpus(nCPUs);
        maximaFinder.setVerbose(verbose);
        maximaFinder.setRadii(radX_P.getFloatValue((float) radX), radZ_P.getFloatValue((float) radZ));
        return maximaFinder.getImagePeaks();
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public String getHelp() {
        return "3D Maxima Finder filter with noise parameter.";
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setMultithread(int nCPUs) {
        this.nCPUs = nCPUs;
    }
    
}
