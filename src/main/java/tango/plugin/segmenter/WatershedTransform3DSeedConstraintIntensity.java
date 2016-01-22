package tango.plugin.segmenter;

import java.util.Comparator;
import java.util.TreeSet;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageShort;

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

public class WatershedTransform3DSeedConstraintIntensity extends WatershedTransform3DSeedConstraint {
    public WatershedTransform3DSeedConstraintIntensity(ImageHandler hessian, int nCPUs, boolean verbose) {
        super(hessian, nCPUs, verbose);
    }
    
    @Override
    public ImageInt runWatershed(ImageHandler input, ImageHandler watershedMap, ImageInt mask_) {
        ImageInt seg =  super.runWatershed(input, input, mask_);
        //seg = BinaryMorpho.binaryCloseMultilabel(seg, 1, 1, nCPUs);
        //seg = FillHoles2D.fillHoles2D(seg);
        return seg;
    }
    
    @Override
    protected ImageShort getLocalExtrema() {
        ImageShort sm = new ImageShort("segMap", sizeX, input.sizeY, input.sizeZ);
        //search for local extrema
        for (int z = 0; z<input.sizeZ; z++) {
            for (int y=0; y<input.sizeY; y++) {
                for (int x = 0; x<sizeX; x++) {
                    int xy=x+y*sizeX;
                    if (mask.getPixel(xy, z)!=0) {
                        if (isLocalExtremum(x, y, z, input.getPixel(xy, z))) sm.pixels[z][xy]=Short.MIN_VALUE;
                    }
                }
            }
        }
        return sm;
    }
    
    @Override
    protected boolean isLocalExtremum(int x, int y, int z, float value) {
        int xy = x+y*this.sizeX;
        if (this.input.getPixel(xy, z)<seedIntensityThld || (useSeedHessianThld && hessian.getPixel(xy, z)>seedHessianThld)) return false;
        for (int zz = z-1; zz<=z+1; zz++) {
            if (zz>=0 && zz<input.sizeZ) {
                for (int yy = y-1; yy<=y+1; yy++) {
                    if (yy>=0 && yy<input.sizeY) {
                        for (int xx = x-1; xx<=x+1; xx++) {
                            if ((xx!=x || yy!=y || zz!=z) && xx>=0 && xx<sizeX) {
                                int xxyy=xx+yy*sizeX;
                                if (mask.getPixel(xxyy, zz)!=0) {
                                    if (input.getPixel(xxyy, zz)>value) return false;
                                }  
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
    
    @Override
    protected TreeSet<Vox3D> getHeap() { // decreasing intensities
        return new TreeSet<Vox3D>(new Comparator<Vox3D>() {
            @Override
            public int compare(Vox3D o1, Vox3D o2) {
                if (o1.value<o2.value) return 1;
                else if (o1.value>o2.value) return -1;
                else return 0;
            }
        });
    }

}
