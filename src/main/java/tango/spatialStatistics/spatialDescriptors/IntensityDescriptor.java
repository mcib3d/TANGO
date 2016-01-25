package tango.spatialStatistics.spatialDescriptors;


import ij.ImagePlus;
import java.util.Arrays;
import java.util.HashMap;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageHandler;
import tango.dataStructure.InputCellImages;
import tango.dataStructure.SegmentedCellImages;
import tango.dataStructure.StructureQuantifications;
import tango.parameter.FilteredStructureParameter;
import tango.parameter.Parameter;
import tango.parameter.SamplerParameter;
import tango.parameter.StructureParameter;
import tango.spatialStatistics.CumulativeCurves;
import tango.spatialStatistics.spatialDescriptors.SpatialDescriptor;
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
public class IntensityDescriptor implements SpatialDescriptor {
    CumulativeCurves curves = new CumulativeCurves();
    StructureParameter structure = new StructureParameter("Observed Objetcts:", "structure", -1, true);
    SamplerParameter sampler = new SamplerParameter("Sampled Objects:", "sample", -1);
    FilteredStructureParameter structureIntensity = new FilteredStructureParameter("Intensity map:", "intensityMap");
    ImageHandler intensityMap;
    Parameter[] parameters = new Parameter[]{structure, sampler, structureIntensity, curves.getParameters()};
    
    double[] observedDescriptor;
    double[][] sampleDescriptor;
    int nCPUs=1;
    boolean verbose;
    @Override
    public void setMultithread(int nbCPUs) {
        this.nCPUs=nbCPUs;
    }
    
    @Override
    public void setVerbose(boolean verbose) {
        this.verbose=verbose;
    }
    @Override
    public void run(int nbSamples, InputCellImages raw, SegmentedCellImages seg) {
        intensityMap = structureIntensity.getImage(raw, verbose, nCPUs);
        sampler.initSampler(raw, seg, nCPUs, verbose);
        Object3D[] observedO = structure.getObjects(seg);
        observedDescriptor = eval(observedO);
        sampleDescriptor = new double[nbSamples][];
        for (int i = 0; i < nbSamples; i++) {
            sampleDescriptor[i] = eval(sampler.getSample());
            if (sampleDescriptor[i]==null) {
                sampleDescriptor=null;
                return;
            }
        }
    }

    protected double[] eval(Object3D[] objects) {
        if (objects==null || objects.length==0) return null;
        double[] res = new double[objects.length];
        for (int i = 0; i<objects.length; i++) res[i] = objects[i].getPixMeanValue(intensityMap);
        return res;
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public int[] getStructures() {
        return new int[] {structure.getIndex()};
    }

    @Override
    public String getHelp() {
        return "Mean intensity within each spot. To use with shuffle sampler";
    }

    @Override
    public Parameter[] getKeyParameters() {
        return new Parameter[]{curves.getKeys()};
    }

    @Override
    public double[] getObservedDescriptor() {
        return observedDescriptor;
    }

    @Override
    public double[][] getSampleDescriptor() {
        return sampleDescriptor;
    }

    @Override
    public void getCurves(StructureQuantifications quantifs) {
        curves.getCurves(observedDescriptor, sampleDescriptor, quantifs, verbose);
    }
    
    @Override
    public boolean performCurves() {
        return curves.performCurves();
    }

}