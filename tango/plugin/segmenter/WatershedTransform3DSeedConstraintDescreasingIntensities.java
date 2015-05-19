package tango.plugin.segmenter;

import ij.IJ;
import java.util.Comparator;
import java.util.TreeSet;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.processing.BinaryMorpho;
import tango.plugin.filter.FillHoles2D;

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

public class WatershedTransform3DSeedConstraintDescreasingIntensities extends WatershedTransform3DSeedConstraint {
    public WatershedTransform3DSeedConstraintDescreasingIntensities(ImageHandler wsMap, int nCPUs, boolean verbose) {
        super(wsMap, nCPUs, verbose);
    }
    
    @Override
    protected boolean continuePropagation(Vox3D currentVox, Vox3D nextVox) {
        float next = input.getPixel(nextVox.xy, nextVox.z);
        if (next<backgroundLimit) return false;
        float current = input.getPixel(currentVox.xy, currentVox.z);
        return current>=next;
    }
    
    @Override
    public ImageInt runWatershed(ImageHandler input, ImageHandler watershedMap, ImageInt mask_) {
        ImageInt seg =  super.runWatershed(input, hessian, mask_);
        seg = BinaryMorpho.binaryCloseMultilabel(seg, 1, 1, nCPUs);
        seg = FillHoles2D.fillHoles2D(seg);
        return seg;
    }
    
    
    
}
