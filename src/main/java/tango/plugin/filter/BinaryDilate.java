package tango.plugin.filter;

import mcib3d.utils.exceptionPrinter;
import ij.IJ;
import java.util.HashMap;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageLabeller;
import mcib3d.image3d.processing.BinaryMorpho;
import tango.dataStructure.InputImages;
import tango.parameter.*;

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
public class BinaryDilate implements PostFilter {

    // TODO utiliser une methode classique si le rayon est petit
    boolean debug;
    int nbCPUs = 1;
    DoubleParameter radiusXY = new DoubleParameter("XY-radius: ", "radiusXY", 1d, Parameter.nfDEC1);
    DoubleParameter radiusZ = new DoubleParameter("Z-radius: ", "radiusZ", 1d, Parameter.nfDEC1);
    BooleanParameter useScale = new BooleanParameter("Use Image Scale for Z radius: ", "useScale", true);
    HashMap<Object, Parameter[]> map = new HashMap<Object, Parameter[]>() {
        {
            put(false, new Parameter[]{radiusZ});
            put(true, new Parameter[0]);
        }
    };
    ConditionalParameter cond = new ConditionalParameter("Z-radius", useScale, map);
    Parameter[] parameters = new Parameter[]{radiusXY, cond};

    public BinaryDilate() {
        radiusXY.setHelp("Radius in XY direction (pixels)", true);
        radiusZ.setHelp("Radius in Z direction (pixels)", true);
        useScale.setHelp("If selected, the radius in Z direction will be computed according to the image anisotropy", true);
        useScale.setHelp("If selected, radiusZ = radiusXY * scaleXY / scaleZ", false);
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public void setVerbose(boolean debug) {
        this.debug = debug;
    }

    public ImageInt runPostFilter(int currentStructureIdx, ImageInt input, InputImages images) {
        try {
            float radXY = Math.max(radiusXY.getFloatValue(1), 1);
            float radZ;
            if (useScale.isSelected()) {
                radZ = Math.max(radXY * (float) (input.getScaleXY() / input.getScaleZ()), 1);
            } else {
                radZ = radiusZ.getFloatValue(1);
            }
            if (debug) {
                IJ.log("binaryDilate: radius XY" + radXY + " radZ:" + radZ);
            }
            ImageLabeller label = new ImageLabeller(debug);
            return label.getLabels(BinaryMorpho.binaryDilate(input.thresholdAboveExclusive(0), radXY, radZ, nbCPUs));

        } catch (Exception e) {
            exceptionPrinter.print(e, "", true);
        }
        return null;
    }

    public void setMultithread(int nbCPUs) {
        this.nbCPUs = nbCPUs;
    }

    public String getHelp() {
        return "Morphological dilate using distance maps, optimized for large radius.\n Note thay the number of objects may change due to some mergings.";
    }

}
