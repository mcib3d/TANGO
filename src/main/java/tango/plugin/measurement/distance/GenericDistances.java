package tango.plugin.measurement.distance;

import tango.dataStructure.InputCellImages;
import tango.dataStructure.SegmentedCellImages;
import tango.dataStructure.StructureQuantifications;
import tango.parameter.DistanceParameter;
import tango.parameter.KeyParameterStructureArrayO2O;
import tango.parameter.Parameter;
import tango.parameter.StructureParameter;
import tango.plugin.measurement.MeasurementObject2Object;

/**
 * *
 * /**
 * Copyright (C) 2012 Jean Ollion
 * <p>
 * <p>
 * <p>
 * This file is part of tango
 * <p>
 * tango is free software; you can redistribute it and/or modify it under the
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
 * @author Jean Ollion
 */
public class GenericDistances implements MeasurementObject2Object {
    boolean verbose;
    int nbCPUs = 1;
    StructureParameter structure1 = new StructureParameter("Structure 1:", "structure1", -1, true);
    StructureParameter structure2 = new StructureParameter("Structure 2:", "structure2", -1, true);
    DistanceParameter distance = new DistanceParameter("Distance:", "distance", Distance.distances[0]);
    Parameter[] parameters = new Parameter[]{structure1, structure2, distance};
    KeyParameterStructureArrayO2O dist = new KeyParameterStructureArrayO2O("Distance", "dist", "dist", true);
    KeyParameterStructureArrayO2O[] keys = new KeyParameterStructureArrayO2O[]{dist};

    @Override
    public int[] getStructures() {
        return new int[]{structure1.getIndex(), structure2.getIndex()};
    }


    @Override
    public void setMultithread(int nbCPUs) {
        this.nbCPUs = nbCPUs;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public void getMeasure(InputCellImages raw, SegmentedCellImages seg, StructureQuantifications quantifs) {
        Distance d = distance.getDistance(raw, seg);
        //System.out.println("generic distance:"+d.getClass());
        if (structure1.getIndex() == structure2.getIndex())
            quantifs.setQuantificationStructureArrayO2O(dist, d.getAllInterDistances(structure1.getObjects(seg)));
        else
            quantifs.setQuantificationStructureArrayO2O(dist, d.getAllInterDistances(structure1.getObjects(seg), structure2.getObjects(seg)));
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }


    @Override
    public KeyParameterStructureArrayO2O[] getKeys() {
        return keys;
    }

    @Override
    public String getHelp() {
        return "Computes all inter distance between segmented objects of the two selected Structure. Result is an array of length n1 * n2 or n * (n-1) if the same structure is selected twice";
    }


}
