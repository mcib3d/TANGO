package tango.plugin.filter.mergeRegions;

import ij.IJ;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageShort;
import tango.gui.Core;
import tango.util.SpearmanPairWiseCorrelationTest;

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

public class Region {
        HashSet<Vox3D> voxels;
        ArrayList<Interface> interfaces;
        Interface interfaceBackground;
        int label;
        RegionCollection col;
        double mergeCriterionValue;
        
        public Region(int label, Vox3D vox, RegionCollection col) {
            this.label=label;
            this.voxels=new HashSet<Vox3D>();
            if (vox!=null) voxels.add(vox);
            this.col=col;
        }
        
        public void setLabel(int label) {
            col.regions.remove(this.label);
            this.label=label;
            setVoxelLabel(label);
            col.regions.put(label, this);
        }
        
        public void setVoxelLabel(int l) {
            for (Vox3D v : voxels) col.labelMap.setPixel(v.xy, v.z, l);
        }

        public Region fusion(Region region) {
            if (region.label<label) return region.fusion(this);
            if (col.verbose) ij.IJ.log("fusion:"+label+ "+"+region.label);
            region.setVoxelLabel(label);
            this.voxels.addAll(region.voxels);
            //if (this.interactants!=null) interactants.addAll(region.interactants);
            //spots.remove(region.label);
            return region;
        }
        
        public double getArea() {
            ImageCalibrations cal = col.cal;
            ImageInt inputLabels = col.labelMap;
            double area=0;
            for (Vox3D vox: voxels) {
                if (vox.x<cal.limX && (inputLabels.getPixelInt(vox.xy+1, vox.z))!=label) {
                    area+=cal.aXZ;
                }
                if (vox.x>0 && (inputLabels.getPixelInt(vox.xy-1, vox.z))!=label) {
                    area+=cal.aXZ;
                }
                if (vox.y<cal.limY && (inputLabels.getPixelInt(vox.xy+cal.sizeX, vox.z))!=label) {
                    area+=cal.aXZ;
                }
                if (vox.y>0 && (inputLabels.getPixelInt(vox.xy-cal.sizeX, vox.z))!=label) {
                    area+=cal.aXZ;
                }
                if (vox.z<cal.limZ && (inputLabels.getPixelInt(vox.xy, vox.z+1))!=label) {
                    area+=cal.aXY;
                }
                if (vox.z>0 && (inputLabels.getPixelInt(vox.xy, vox.z-1))!=label) {
                    area+=cal.aXY;
                }
            }
            return area;
        }
        
        public boolean hasNoInteractant() {
            return interfaces==null || interfaces.isEmpty() || (interfaces.size()==1 && interfaces.get(0).r1.label==0);
        }
        
        public Object3DVoxels toObject3D() {
            ArrayList<Voxel3D> al = new ArrayList<Voxel3D>(voxels.size());
            for (Vox3D v : voxels) al.add(v.toVoxel3D(label));
            return new Object3DVoxels(al);
        }
        
        @Override 
        public boolean equals(Object o) {
            if (o instanceof Region) {
                return ((Region)o).label==label;
            } else if (o instanceof Integer) return label==(Integer)o;
            else return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + this.label;
            return hash;
        }
        
        @Override 
        public String toString() {
            return "Region:"+label;
        }
        
    public static double getRho(HashSet<Vox3D>[] voxIt, ImageHandler intensityMap, int nbCPUs) {
        int[] bb= getBoundingBox(voxIt);
        if (bb==null) return 0;
        ImageInt map = createSegImageMini(1, null, bb, voxIt );
        ImageFloat dm = map.getDistanceMapInsideMask(nbCPUs);
        /*ij.IJ.log("set dm values object:"+object.getValue());
        if (object.getValue()==7) {
            map.show();
            dm.show();
            ImageInt map2 = object.createSegImageMini(object.getValue(), 1);
            ImageFloat dm2 = map2.getDistanceMapInsideMask(nbCPUs);
            dm2.show("dm2");
        }*/
        dm.show();
        int size = 0; for (HashSet<Vox3D> h : voxIt) size+=h.size();
        float[] distances = new float[size];
        float[] values = new float[distances.length];
        int idx=0;
        for (HashSet<Vox3D> h : voxIt) {
            for (Vox3D v : h) {
                distances[idx] = dm.getPixel(v.x-bb[0], v.y-bb[1], v.z-bb[2]);
                values[idx++] = intensityMap.getPixel(v.xy, v.z);
            }   
        }
        return SpearmanPairWiseCorrelationTest.computeRho(distances, values);
    }
    
    public static double getHessianMeanValue(HashSet<Vox3D>[] voxIt, ImageFloat hessian, double erode, int nbCPUs) {
        int[] bb= getBoundingBox(voxIt);
        if (bb==null) return 0;
        ImageInt map = createSegImageMini(1, null, bb, voxIt );
        ImageFloat dm = map.getDistanceMapInsideMask(nbCPUs);
        // set voxel value to distanceMap
        int size = 0;
        for (HashSet<Vox3D> h : voxIt) {
            size+=h.size();
            for (Vox3D v : h) v.value=dm.getPixel(v.x-bb[0], v.y-bb[1], v.z-bb[2]);
        }
        ArrayList<Vox3D> al;
        if (voxIt.length==1) al = new ArrayList(voxIt[0]);
        else {
            al = new ArrayList<Vox3D>(size);
            for (HashSet<Vox3D> h : voxIt) al.addAll(h);
        }
        Collections.sort(al);
        int idx = (int)((size-1) * erode + 0.5);
        
        double mean = 0;
        for (int i = idx; i<size; i++) {
            Vox3D v = al.get(i);
            mean+=hessian.pixels[v.z][v.xy];
        }
        if (idx<size) mean/=(double)(size-idx);
        //IJ.log("getHessMeanVal Sort: 0:"+al.get(0).value+" "+(size-1)+":"+al.get(size-1).value + "idx: "+idx+ " : "+al.get(idx).value + " mean value: "+mean);
        return mean;
    }
        
    protected static int[] getBoundingBox(HashSet<Vox3D>[] voxIt) {
        int xMin=Integer.MAX_VALUE, yMin=Integer.MAX_VALUE, zMin=Integer.MAX_VALUE;
        int xMax=0, yMax=0, zMax=0;
        int size = 0;
        for (HashSet<Vox3D> it : voxIt) {
            for (Vox3D v : it) {
                if (xMin>v.x) xMin=v.x;
                if (yMin>v.y) yMin=v.y;
                if (zMin>v.z) zMin=v.z;
                if (xMax<v.x) xMax=v.x;
                if (yMax<v.y) yMax=v.y;
                if (zMax<v.z) zMax=v.z;
            }
            size+=it.size();
        }
        //IJ.log("bounding box: xMin:"+xMin+" xMax:"+xMax+" yMin:"+yMin+" yMax:"+yMax+" zMin:"+zMin+" zMax:"+zMax+ " size:"+size + " nb of sets: "+voxIt.length);
        if (size==0) return null;
        else return new int[]{xMin, yMin, zMin, xMax, yMax, zMax};
    }
        
    public static ImageInt createSegImageMini(int val, int[] border, int[] boundingBox, HashSet<Vox3D>[] voxIt) {
        if (boundingBox==null) boundingBox= getBoundingBox(voxIt);
        if (border==null) border = new int[3];
        int xm = boundingBox[0] - border[0];
        if (xm < -border[0]) {
            xm = -border[0];
        }
        int ym = boundingBox[1] - border[1];
        if (ym < -border[1]) {
            ym = -border[1];
        }
        int zm = boundingBox[2] - border[2];
        if (zm < -border[2]) {
            zm = -border[2];
        }

        int w = boundingBox[3] - xm + 1 + border[0];
        int h = boundingBox[4] - ym + 1 + border[1];
        int d = boundingBox[5] - zm + 1 + border[2];
        ImageShort miniLabelImage = new ImageShort("Object_" + val, w, h, d);
        int xx, yy, zz;
        for (HashSet<Vox3D> it : voxIt) {
            for (Vox3D vox : it) {
                xx = vox.x - xm;
                yy = vox.y - ym;
                zz = vox.z - zm;
                // TODO suface vertices may have coordinates < 0 if touching edges 
                if (miniLabelImage.contains(xx, yy, zz)) {
                    miniLabelImage.setPixel(xx, yy, zz, val);
                }
            }
        }
        //miniLabelImage.show();
        // set the offsets
        miniLabelImage.offsetX = xm;
        miniLabelImage.offsetY = ym;
        miniLabelImage.offsetZ = zm;
        //miniLabelImage.setScale((float) this.getResXY(), (float) this.getResZ(), this.getUnits());
        //miniLabelImage.show("obj:"+this);
        return miniLabelImage;
    }
    
}
