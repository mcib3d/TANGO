package tango.plugin.filter;

//import denoise.Denoising;
//import denoise.Operations;
import ebmoll.lipid_droplet_counter.filters.Bandpass3D;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import imageware.Builder;
import imageware.ImageWare;
import mcib3d.image3d.ImageHandler;
import tango.dataStructure.InputImages;
import tango.parameter.*;

import java.util.HashMap;

import static tango.spatialStatistics.SDIEvaluator.filter1d.Filter1d.filters;

/**
 * *
 * /**
 * Copyright (C) 2008- 2012 Thomas Boudier and others
 * <p>
 * <p>
 * <p>
 * This file is part of mcib3d
 * <p>
 * mcib3d is free software; you can redistribute it and/or modify it under the
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
 * @author thomas
 */
public class Misc_3DFilters implements PreFilter {

    static final int GAUSSIAN = 0;
    static final int LOG = 1;
    static final int DENOISE = 2;
    static final int BANDPASS = 3;
    static String gauss3DHelp = "<ul><li><strong>Gaussian 3D</strong> taken from ImageJ Process/Filters.</li></ul>";
    static String log3DHelp = "<ul><li><strong>LoG</strong>, laplacian of Gaussian, <br>taken from BIG http://bigwww.epfl.ch/sage/soft/LoG3D/ <br>. <br>When using this plugin, please cite : <br><br>D. Sage, F.R. Neumann, F. Hediger, S.M. Gasser, M. Unser, \"Automatic Tracking of Individual Fluorescence Particles: Application to the Study of Chromosome Dynamics,\" IEEE Transactions on Image Processing, vol. 14, no. 9, pp. 1372-1383, September 2005.<br> </li></ul>";
    static String denoiseHelp = "<ul><li><strong>PureDenoise</strong>, fluorescence denoising, <br>taken from BIG http://bigwww.epfl.ch/algorithms/denoise/ <br>Please make sure you have installed this plugin first</li></ul>";
    static String BPHelp = "<ul><li><strong>BandPass</strong>, filter pixels based on object size, <br>taken from Droplet Finder http://imagejdocu.tudor.lu/doku.php?id=plugin:analysis:droplet_counter:start <br>Please make sure you have installed this plugin first</li></ul>";
    boolean debug = true;
    boolean thread = false;
    int nbCPUs = 1;
    double voisx = 2;
    double voisz = 1;
    int cs = 5;
    int mins = 2;
    int maxs = 10;
    int filter = 0;
    String[] filters = {"Gaussian 3D (IJ)", "LoG 3D (BIG)", "PureDenoise (BIG)", "BandPass (Droplet)"};
    ChoiceParameter filter_P = new ChoiceParameter("Choose Filter: ", "filter", filters, null);
    DoubleParameter voisXY_P = new DoubleParameter("VoisXY: ", "voisXY", (double) voisx, Parameter.nfDEC1);
    DoubleParameter voisZ_P = new DoubleParameter("VoisZ: ", "voisZ", (double) voisz, Parameter.nfDEC1);
    BooleanParameter useScale = new BooleanParameter("Use Image Scale for Z radius: ", "useScale", true);
    ConditionalParameter condScale = new ConditionalParameter("Z-radius", useScale);
    SliderParameter iteration_P = new SliderParameter("Nb Iterations (Denoise):", "iterations", 1, 10, cs);
    IntParameter mins_P = new IntParameter("Min size (BandPass):", "minsize", mins);
    IntParameter maxs_P = new IntParameter("Max size (BandPass):", "maxsize", maxs);
    HashMap<Object, Parameter[]> map = new HashMap<Object, Parameter[]>() {
        {
            put(filters[GAUSSIAN], new Parameter[]{voisXY_P, condScale});
            put(filters[LOG], new Parameter[]{voisXY_P, condScale});
            put(filters[DENOISE], new Parameter[]{voisXY_P, condScale, iteration_P});
            put(filters[BANDPASS], new Parameter[]{mins_P, maxs_P});
        }
    };
    ConditionalParameter cond = new ConditionalParameter("Filter", filter_P, map);
    Parameter[] parameters = new Parameter[]{cond};
    // contructor for Tango

    public Misc_3DFilters() {
        filter_P.setHelp("Available filters are : " + gauss3DHelp + log3DHelp + denoiseHelp + BPHelp, true);
        voisXY_P.setHelp("The radius in <em>X</em> and <em>Y</em> direction", true);
        voisZ_P.setHelp("The radius in <em>Z</em> direction", true);
        iteration_P.setHelp("Number of iterations for PureDenoise", true);
        mins_P.setHelp("Minimum size to filter for BandPass", true);
        maxs_P.setHelp("Maximum size to filter for BandPass", true);
        condScale.setCondition(false, new Parameter[]{voisZ_P});
    }

    private ImageHandler process(ImageHandler ih) {
        if (filter == GAUSSIAN) {
            ImageHandler img2 = ih.duplicate();
            ij.plugin.GaussianBlur3D.blur(img2.getImagePlus(), voisx, voisx, voisz);

            img2.setTitle(ih.getTitle() + "::Gauss3D");
            return img2;
        } else if (filter == LOG) {
            return LaplacianOfGaussian3D.LOG(ih, voisx, voisz);

        } else if (filter == DENOISE) {
            return pureDenoise(ih);
        } else if (filter == BANDPASS) {
            ImagePlus imp = ih.getImagePlus();
            new ImageConverter(imp).convertToGray32();
            imp.updateAndRepaintWindow();
            imp.updateImage();
            ImageStack stack = imp.getStack();
            ImageStack convert = imp.createEmptyStack();
            ImageProcessor tmp;
            ImageProcessor tmpf;
            for (int i = 1; i <= imp.getStackSize(); i++) {
                tmp = stack.getProcessor(i);
                if (tmp instanceof ByteProcessor) {
                    tmp.setMinAndMax(0, 255);
                }
                if (tmp instanceof ShortProcessor) {
                    tmp.setMinAndMax(0, 65535);
                }
                tmpf = tmp.convertToFloat();
                convert.addSlice(tmpf);
            }
            imp.setStack(convert);
            Calibration cal = imp.getCalibration();
            double ratio = cal.pixelDepth / cal.pixelWidth;

            Bandpass3D bp3d = new Bandpass3D();
            bp3d.in_hprad = maxs;
            bp3d.in_lprad = mins;
            bp3d.in_xz_ratio = ratio;
            bp3d.in_image = convert;
            bp3d.filterit();
            ImagePlus impOut = new ImagePlus("BP_", bp3d.out_result);
            if (this.debug) {
                IJ.log("finished");
            }
            return ImageHandler.wrap(impOut);
        }
        return null;
    }

    public ImageHandler runPreFilter(int currentStructureIdx, ImageHandler input, InputImages images) {
        filter = filter_P.getSelectedIndex();
        voisx = voisXY_P.getDoubleValue(voisx);
        if (useScale.isSelected()) voisz = voisx * input.getScaleXY() / input.getScaleZ();
        else voisz = voisZ_P.getDoubleValue(voisz);
        cs = iteration_P.getValue();
        mins = mins_P.getIntValue(mins);
        maxs = maxs_P.getIntValue(maxs);

        return process(input);
    }

    // From PureDenoise_
    // Florian Luisier
    // Biomedical Imaging Group (BIG)
    // Ecole Polytechnique Federale de Lausanne (EPFL)
    // Lausanne, Switzerland
    //
    // Information:
    // http://bigwww.epfl.ch/algorithms/denoise/

    private ImageHandler pureDenoise(ImageHandler input) {
        ImagePlus impSource = input.getImagePlus();

        int nx = impSource.getWidth();
        int ny = impSource.getHeight();

        ImageWare original = Builder.create(impSource);
        if (nx < 16 || ny < 16) {
            IJ.log("The size of your data is inappropriate.");
        }

        IJ.log("Denoising in progress...");
        original = original.convert(ImageWare.DOUBLE);
        int nz = original.getSizeZ();
        int[] Ext = new int[2];
        int nxe = (int) (Math.ceil((double) nx / 16) * 16);
        int nye = (int) (Math.ceil((double) ny / 16) * 16);
        if (nxe != nx || nye != ny) {
            //original = Operations.symextend2D(original, nxe, nye, Ext);
        } else {
            Ext[0] = 0;
            Ext[1] = 0;
        }
        double[] AlphaHat = new double[nz];
        double[] DeltaHat = new double[nz];
        double[] SigmaHat = new double[nz];

        // parameters
        boolean FRAMEWISE = false;
        int CS = iteration_P.getValue();
        int NBFRAME = 1;

        /*Denoising denoising = new Denoising(original, AlphaHat, DeltaHat, SigmaHat, FRAMEWISE, CS, NBFRAME);
        denoising.setLog(false);
        denoising.setFramewise(FRAMEWISE);
        denoising.estimateNoiseParameters();
        denoising.setCycleSpins(CS);
        denoising.setMultiFrame(NBFRAME);
        denoising.perform();
        ImageWare output = denoising.getOutput();
        ImageStack stack = output.buildImageStack();
        
        return ImageHandler.wrap(stack);*/
        return null;
    }

    public void setVerbose(boolean debug) {
        this.debug = debug;
    }

    public void setMultithread(int nbCPUs) {
        this.nbCPUs = nbCPUs;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public String getHelp() {
        switch (this.filter_P.getSelectedIndex()) {
            case 0:
                return gauss3DHelp;
            case 1:
                return log3DHelp;
            case 2:
                return denoiseHelp;
            case 3:
                return BPHelp;
        }
        return "Misc 3D Filters";
    }
}
