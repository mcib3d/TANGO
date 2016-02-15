package tango.plugin.filter.mergeRegions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import tango.gui.Core;
import tango.plugin.filter.FeatureJ.ImageFeaturesCore;

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

public class RegionCollection {
    HashMap<Integer, Region> regions;
    ImageInt labelMap;
    ImageCalibrations cal;
    ImageHandler inputGray;
    InterfaceCollection interfaces;
    boolean setInterfaces;
    boolean verbose;
    int nCPUs;
    public RegionCollection(ImageInt labelMap, ImageHandler intensityMap, boolean verbose, int nCPUs) {
        this.verbose=verbose;
        this.nCPUs=nCPUs;
        this.labelMap=labelMap;
        this.inputGray=intensityMap;
        cal = new ImageCalibrations(labelMap);
        if (inputGray==null) this.setInterfaces=false;
        createRegions();
    }
    
    public void shiftIndicies(boolean updateRegionMap) {
        TreeMap<Integer, Region> sortedReg = new TreeMap<Integer, Region>(regions);
        HashMap<Integer, Region> newRegions = null;
        if (updateRegionMap) newRegions = new HashMap<Integer, Region> (regions.size());
        int curIdx=1;
        for (Region r : sortedReg.values()) {
            if (r.label==0) {
                if (updateRegionMap) newRegions.put(0, r);
                continue;
            }
            r.setVoxelLabel(curIdx);
            r.label=curIdx;
            if (updateRegionMap) newRegions.put(curIdx, r);
            curIdx++;
        }
        if (updateRegionMap) regions=newRegions;
    }
    
    public void initInterfaces() {
        interfaces = new InterfaceCollection(this, verbose);
        interfaces.getInterfaces();
        interfaces.initializeRegionInterfaces();
        if (verbose) interfaces.drawInterfaces();
    }
    
    public void initInterfacesLight() {
        interfaces = new InterfaceCollection(this, verbose);
        interfaces.getInterfacesLight();
        interfaces.initializeRegionInterfaces();
    }
    
    public void mergeAllConnected() {
        InterfaceCollection.mergeAllConnected(this);
    }
    
    public void mergeSortHessianCond(double hessianRadius, boolean useScale, double erode) {
        if (interfaces==null) initInterfaces();
        double scaleZ = inputGray.getScaleZ();
        double scaleXY = inputGray.getScaleXY();
        String unit = inputGray.getUnit();
        if (!useScale) inputGray.setScale(scaleXY, scaleXY, unit);
        ImageFloat hess=ImageFeaturesCore.getHessian(this.inputGray, hessianRadius, nCPUs)[0];
        if (!useScale) {
            hess.setScale(scaleXY, scaleZ, unit);
            inputGray.setScale(scaleXY, scaleZ, unit);
        }
        for (Region r : regions.values()) if (!r.interfaces.isEmpty()) r.mergeCriterionValue=Region.getHessianMeanValue(new HashSet[]{r.voxels}, hess, erode, nCPUs);
        interfaces.mergeSortHessian(hess, erode);
    }
    
    public void mergeSortCorrelation() {
        if (interfaces==null) initInterfaces();
        for (Region r : regions.values()) if (!r.interfaces.isEmpty()) r.mergeCriterionValue = Region.getRho(new HashSet[]{r.voxels}, inputGray, nCPUs);
        interfaces.mergeSortCorrelation();
    }
    
    public Region get(int label) {
        return regions.get(label);
    }
    
    protected void createRegions() {
        regions=new HashMap<Integer, Region>();
        regions.put(0, new Region(0, null, this)); // background
        for (int z = 0; z<labelMap.sizeZ; z++) {
            for (int xy = 0; xy<labelMap.sizeXY; xy++) {
                int label = labelMap.getPixelInt(xy, z);
                if (label!=0) {
                    
                    Region r = regions.get(label);
                    if (r==null) regions.put(label, new Region(label, new Vox3D(xy%cal.sizeX, xy/cal.sizeX, z, xy), this));
                    else r.voxels.add(new Vox3D(xy%cal.sizeX, xy/cal.sizeX, z, xy));
                }
            }
        }
        if (verbose) ij.IJ.log("Region collection: nb of spots:"+regions.size());
    }
    
    public void fusion(Region r1, Region r2, double newCriterion) {
        regions.remove(r1.fusion(r2, newCriterion).label);
    }
}
