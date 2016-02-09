package tango.plugin.measurement.radialAnalysis;

import ij.IJ;
import ij.gui.Plot;
import mcib3d.geom.Object3DVoxels;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageShort;
import tango.dataStructure.InputCellImages;
import tango.dataStructure.ObjectQuantifications;
import tango.dataStructure.SegmentedCellImages;
import tango.parameter.BooleanParameter;
import tango.parameter.ChoiceParameter;
import tango.parameter.ConditionalParameter;
import tango.parameter.GroupKeyParameter;
import tango.parameter.KeyParameterObjectNumber;
import tango.parameter.Parameter;
import tango.parameter.PreFilterSequenceParameter;
import tango.parameter.SpinnerParameter;
import tango.parameter.StructureParameter;
import tango.plugin.measurement.MeasurementObject;

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
public class ShellAnalysis implements MeasurementObject {

    StructureParameter structure = new StructureParameter("Signal:", "structure", -1, true);
    BooleanParameter preFilter = new BooleanParameter("Use filtered image", "filtered", false);
    PreFilterSequenceParameter preFilters = new PreFilterSequenceParameter("Pre-Filters", "preFilters");
    ChoiceParameter inputSignal = new ChoiceParameter("Input signal", "inputSignal", new String[]{"Grayscale", "Segmented", "Grayscale within segmented"}, "Grayscale within segmented");
    StructureParameter structureMask = new StructureParameter("Distance from structure:", "structureMask", 0, true);
    BooleanParameter inside = new BooleanParameter("Inside structure", "inside", true);
    BooleanParameter object = new BooleanParameter("Perform around each object separately", "objects", false);
    ConditionalParameter referenceObjectCond = new ConditionalParameter("Reference for distance calculation:", structureMask);
    //IntParameter nbShells = new IntParameter("Number of Shells", "nbShells", 5);
    SpinnerParameter nbShells = new SpinnerParameter("Number of Shells", "nbShells", 2, 100, 5);
    BooleanParameter normalize = new BooleanParameter("Normalize with nucleus Signal", "normalize", false);

    Parameter[] parameters = new Parameter[]{structure, preFilter, preFilters, inputSignal, referenceObjectCond, nbShells, normalize};
    //KeyParameterObjectNumber key = new KeyParameterObjectNumber("Shell", "shell", "shell", true);
    KeyParameterObjectNumber[] keys = new KeyParameterObjectNumber[0];
    //GroupKeyParameter group = new GroupKeyParameter("Shells", "shell", "shell_", true, new KeyParameterObjectNumber[0], true);
    GroupKeyParameter group = new GroupKeyParameter("", "shells", "", true, keys, false);
    Parameter[] allKeys = new Parameter[]{group};
    int nCPUs = 1;
    boolean verbose;

    public ShellAnalysis() {
        nbShells.setFireChangeOnAction();
        referenceObjectCond.setDefaultParameter(new Parameter[]{object});
        referenceObjectCond.setCondition(0, new Parameter[]{});
        structure.setHelp("Signal to measure, either raw signal or segmented signal.", true);
        inputSignal.setHelp("Grayscale: measure percentage of fluorescence in each shell; Segmented: percentage of segmented voxels; Grayscale within segmented: idem to Grayscale but limit to the segmented volume", true);
        structureMask.setHelp("Shells are created from this reference structure, either inside if nucleus or outside if other structure.", true);
        object.setHelp("If checked shells are build around each object, if unchecked shells are build around the whole population of objects.", true);
        referenceObjectCond.setHelp("The structure around which perform shell analysis.", true);
        nbShells.setHelp("The number of shells.", true);
        normalize.setHelp("If checked shell of constant intensity, if unchecked shells of constant volume.", true);
    }

    @Override
    public int getStructure() {
        if (structureMask.getIndex() != 0 && !object.isSelected()) {
            return 0;
        } else {
            return structureMask.getIndex();
        }
    }

    @Override
    public void getMeasure(InputCellImages rawImages, SegmentedCellImages segmentedImages, ObjectQuantifications quantifications) {
        if (!group.isSelected()) {
            return;
        }
        int mode = inputSignal.getSelectedIndex();
        ImageInt segMap = mode==0? null: segmentedImages.getImage(structure.getIndex());
        ImageHandler intensity =  mode==1? null: ( preFilter.isSelected()?  rawImages.getFilteredImage(structure.getIndex()) : rawImages.getImage(structure.getIndex()) ) ;
        if (mode!=0 && segMap==null && verbose) { 
            ij.IJ.log("Measure proportion of segmented signal is selected, but no raw image found for structure: " + structure.getIndex());
            return;
        }
        
        if (object.isSelected()) {
            Object3DVoxels[] obs = segmentedImages.getObjects(this.structureMask.getIndex());
            double[][] shells = new double[nbShells.getValue()][obs.length];
            ImageInt image = (ImageInt) ImageShort.newBlankImageHandler("object", segmentedImages.getImage(this.structureMask.getIndex()));
            for (int i = 0; i < obs.length; i++) {
                Object3DVoxels o = obs[i];
                o.draw(image, o.getValue());
                double[] shellsO = getShells(rawImages.getMask(), image, intensity, segMap, rawImages.getImage(0));
                for (int s = 0; s < shellsO.length; s++) {
                    shells[s][i] = shellsO[s];
                }
                o.draw(image, 0);
            }
            for (int i = 0; i < shells.length; i++) {
                quantifications.setQuantificationObjectNumber(keys[i], shells[i]);
            }
        } else {
            double[] shells = getShells(rawImages.getMask(), segmentedImages.getImage(structureMask.getIndex()), intensity, segMap, rawImages.getImage(0));
            for (int i = 0; i < shells.length; i++) {
                quantifications.setQuantificationObjectNumber(keys[i], new double[]{shells[i]});
                //if (verbose) {
                //ij.IJ.log(keys[i].getKey() + ":" + shells[i]);
                //}
            }
        }
    }

        if (mode!=1 && intensity==null && verbose ) {
            ij.IJ.log("Measure proportion of raw signal is selected, but no segmented image found for structure: " + structure.getIndex());
            return;
        }
        if (intensity!=null) intensity = preFilters.runPreFilterSequence(structure.getIndex(), intensity, rawImages, nCPUs, verbose);
        boolean in = inside.getValue();
        if (ref == nuc) {
            //IJ.log("Shell analysis inside nucleus only");
            in = true;
        }
        ShellAnalysisCore shell = new ShellAnalysisCore(nuc, ref, in, nCPUs, false);
        int nbShell = nbShells.getValue();
        int[] indexes = (normalize.isSelected()) ? shell.getShellIndexesNormalized(nbShell, intensityRef) : shell.getShellIndexes(nbShell);
        double[] shells;
        switch(mode) {
            case 0: shells = shell.getShellRepartition(intensity, indexes);
            break;
            case 1: shells =  shell.getShellRepartitionMask(objects, indexes);
            break;
            case 2: default: shells = shell.getShellRepartitionMask(objects, intensity, indexes);       
            break;
        }

        if (verbose) {
            shell.getShellMap(indexes).show();
            double[] shellIdx = new double[shells.length];
            for (int i = 0; i < shells.length; i++) {
                shellIdx[i] = i + 1;
            }
            String title = "Shell Analysis";
            if (object.isSelected()) {
                title += " for object: " + ref.getMax();
            }
            Plot p = (new Plot(title, "Shell Index", "% of fluorescence signal", shellIdx, shells));
            p.setLimits(1, shellIdx.length, 0, 1);
            p.show();
        }
        return shells;
    }

    @Override
    public Parameter[] getKeys() {
        if (nbShells.getValue() != keys.length) {
            KeyParameterObjectNumber[] newKeys = new KeyParameterObjectNumber[nbShells.getValue()];
            if (newKeys.length > 0) {
                if (nbShells.getValue() < keys.length) {
                    if (keys.length > 0) {
                        System.arraycopy(keys, 0, newKeys, 0, newKeys.length);
                    }
                } else {
                    if (keys.length > 0) {
                        System.arraycopy(keys, 0, newKeys, 0, keys.length);
                    }
                    for (int i = keys.length; i < newKeys.length; i++) {
                        newKeys[i] = new KeyParameterObjectNumber("Shell " + (i + 1) + ":", "shell" + (i + 1), "shell" + (i + 1), true);
                    }
                }
            }
            keys = newKeys;
            this.group.setKeys(keys);
        }
        return allKeys;

//          if (nbRads.getValue() != keys.length) {
//            KeyParameterObjectNumber[] newKeys = new KeyParameterObjectNumber[nbRads.getValue()];
//            if (newKeys.length > 0) {
//                if (nbRads.getValue() < keys.length) {
//                    if (keys.length > 0) {
//                        System.arraycopy(keys, 0, newKeys, 0, newKeys.length);
//                    }
//                } else {
//                    if (keys.length > 0) {
//                        System.arraycopy(keys, 0, newKeys, 0, keys.length);
//                    }
//                    for (int i = keys.length; i < newKeys.length; i++) {
//
//                        newKeys[i] = new KeyParameterObjectNumber("Rad_" + (i + 1) + ":", "rad" + (i + 1), "rad" + (i + 1), true);
//                    }
//                }
//            }
//            keys = newKeys;
//            this.group.setKeys(keys);
//        }
//        return returnKeys;
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public String getHelp() {
        return "Shell analysis. Shells of equal volume (or equal nucleus signal integrated density is Normalized is checked). % of segmented voxel in each shell if \"Use segmented objects\" is selected, % of fluorescence otherwise";
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void setMultithread(int nCPUs) {
        this.nCPUs = nCPUs;
    }

}
    protected double[] getShells(ImageInt nuc, ImageInt ref, ImageHandler intensity, ImageInt objects, ImageHandler intensityRef) {
        int mode = inputSignal.getSelectedIndex();
