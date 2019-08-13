/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.plugin.filter;

import ij.IJ;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import tango.dataStructure.InputImages;
import tango.parameter.*;
import tango.plugin.filter.FeatureJ.ImageFeaturesCore;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

import static tango.util.Utils.getObjects3D;
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
public abstract class SpotLocalThresholder {
    ImageInt segMap, mask;
    //Image3D intensityMap3D;
    ImageHandler intensityMap;
    //IntImage3D segMap3D;
    Object3D[] spots;
    int limX, limY, limZ, sizeX, currentLabel;
    boolean debug;
    int nbCPUs=1;
    double globalMean, localC;
    double globalMin;
    ArrayList<Object3DVoxels> rescuedSpots;
    BooleanParameter filtered = new BooleanParameter("Use Filtered Image for intensity", "filtered", true);
    SliderDoubleParameter localCoeff = new SliderDoubleParameter("Local Coefficient", "local", 0, 1, 0.5d, 2);
    ChoiceParameter seedMethod = new ChoiceParameter("Seed computation method", "seedMethod", new String[]{"MaxGlobal(Intensity)", "MaxLocal(Intensity)", "MinGlobal(Hessian)"}, "MaxGlobal(Intensity)");
    ConditionalParameter seedCond = new ConditionalParameter(seedMethod);
    DoubleParameter scale = new DoubleParameter("Integration Scale (Pix)", "scale", 1d, Parameter.nfDEC3);
    Parameter[] coreParameters = new Parameter[] {filtered,localCoeff, seedCond};
    
    public SpotLocalThresholder() {
        localCoeff.setHelp("1 -> local threshold for each spot, computed from the gaussian fit \n0 -> same threshold used for each spot (mean of the local thresholds) \n]0-1[ - > a local threshold between the global mean and the local threshold", true);
        localCoeff.setHelp("a local threshold (thld) is computed for each spot. \nthe global mean (mean) is then computed. \nfor each spot the threshold is: (1-coefficient) * mean + coefficient * thld", false);
        seedCond.setCondition("MinGlobal(Hessian)", new Parameter[]{scale});
    }
    
    public abstract ImageInt runPostFilter(int currentStructureIdx, ImageInt image, InputImages images);
    
    public abstract String getHelp();
    
    // tango postFilter methods
    public void setVerbose(boolean debug) {
        this.debug=debug;
    }


    public void setMultithread(int nbCPUs) {
        this.nbCPUs=nbCPUs;
    }

    public Parameter[] getParameters() {
        Parameter[] parameters=getOtherParameters();
        Parameter[] p = new Parameter[parameters.length+coreParameters.length];
        System.arraycopy(coreParameters, 0, p, 0, coreParameters.length);
        System.arraycopy(parameters, 0, p, coreParameters.length, parameters.length);
        return p;
    }
    
    public Parameter[] getParametersInternal() {
        Parameter[] parameters=getOtherParameters();
        Parameter[] p = new Parameter[parameters.length+1];
        p[0]=localCoeff;
        System.arraycopy(parameters, 0, p, 1, parameters.length);
        return p;
    }
    
    protected abstract Parameter[] getOtherParameters();
    
    public abstract double getLocalThreshold(Object3D spot);
    
    protected double getLocalThresholdCorrected(Object3D spot) {
        double thld = getLocalThreshold(spot);
        if (Double.isInfinite(thld) || Double.isNaN(thld)) return Double.NaN;
        else {
            double min = spot.getPixMinValue(intensityMap);
            if (thld<min) return min; 
            return thld;
        }
    }
    
    public void initialize(ImageInt segMap, ImageHandler intensityMap, ImageInt mask) {
        this.segMap=segMap;
        limX=segMap.sizeX-1;
        limY=segMap.sizeY-1;
        limZ=segMap.sizeZ-1;
        sizeX=segMap.sizeX;
        spots=getObjects3D(segMap);
        shiftIndexes();
        currentLabel=spots.length+1;
        globalMin = intensityMap.getMin(mask);
        this.intensityMap=intensityMap;
        this.mask=mask;
        this.rescuedSpots=new ArrayList<Object3DVoxels>();
        this.localC=localCoeff.getValue();
        postInitialize();
    }
    
    protected abstract void postInitialize();
    
    public void run(boolean rescueSpots) {
        double[] thlds = new double[spots.length];
        for (int i = 0; i<spots.length; i++) thlds[i]=getLocalThresholdCorrected(spots[i]);
        localThreshold(thlds, rescueSpots);
    }
    
    public double adjustThld(double thld) {
        return Double.isNaN(thld)? globalMean:(1-localC)*globalMean + (localC)*thld;
    }
    
    protected void localThreshold(double[] thlds, boolean rescueSpots) {
        //adjust // pondération par le volume des spots?? mediane??
        globalMean = 0;
        int count = 0;
        for (double d : thlds) {
            if (!Double.isNaN(d)) {
                globalMean+=d;
                count++;
            }
        }
        if (count!=0) globalMean/=count;
        if (debug) {
            System.out.println("Local Fit: global mean thld: "+globalMean+ " (rescue:"+rescueSpots+")");
            ij.IJ.log("Local Fit: global mean thld: "+globalMean);
        }
        //local threshold
        if (debug) segMap.showDuplicate("before local threshold");
        //if (true) return;
        for (int i = 0; i<thlds.length; i++) localThreshold((Object3DVoxels)spots[i], (float)adjustThld(thlds[i]), rescueSpots);
        //if (debug) segMap.showDuplicate("after local threshold");
        if (rescueSpots) {
            if (debug) segMap.showDuplicate("before rescue");
            for (int i = 0; i<rescuedSpots.size(); i++) if (rescuedSpots.get(i).getVoxels().size()<=2) {
                rescuedSpots.remove(i);
                i--;
            }
            if (!rescuedSpots.isEmpty()) {

                Object3DVoxels[] spotsToAdd = new Object3DVoxels[rescuedSpots.size()];
                spotsToAdd=rescuedSpots.toArray(spotsToAdd);
                rescuedSpots=new ArrayList<Object3DVoxels>();
                for (int i = 0; i<spotsToAdd.length; i++) {
                    double thld=getLocalThresholdCorrected(spotsToAdd[i]);
                    localThreshold(spotsToAdd[i], (float)adjustThld(thld), false);
                }
                Object3D[] newSpots=new Object3D[spots.length+spotsToAdd.length];
                System.arraycopy(spots, 0, newSpots, 0, spots.length);
                System.arraycopy(spotsToAdd, 0, newSpots, spots.length, spotsToAdd.length);
                spots=newSpots;
            }
        }
    }
    
    public Object3D[] getSpots() {
        return spots;
    }
     
    public void shiftIndexes() {
        for (int i = 0; i<spots.length; i++ ) {
            for (Voxel3D v : spots[i].getVoxels()) segMap.setPixel(v.getRoundX(), v.getRoundY(), v.getRoundZ(), i+1);
            spots[i].setValue(i+1);
        }
    }
    
    protected Vox3D getSeed(Object3DVoxels spot) {
        int method = seedMethod.getSelectedIndex();
        if (method == 1) return this.getLocalExtremum(spot);
        else if (method == 2) {
            ImageFloat hess = ImageFeaturesCore.getHessian(this.intensityMap, scale.getFloatValue(1), nbCPUs)[0];
            float min = Float.MAX_VALUE;
            Vox3D maxVox = null;
            for (Voxel3D v : spot.getVoxels()) {
                float value = hess.getPixel(v.getXYCoord(intensityMap.sizeX), v.getRoundZ()); 
                if (value<min) {
                    min = value;
                    maxVox = new Vox3D(v.getRoundX(), v.getRoundY(), v.getRoundZ(), 0);
                }
            }
            if (maxVox!=null) {
                maxVox.value=intensityMap.getPixel(maxVox.xy, maxVox.z);
                return maxVox;
            }
        }
        return getMax(spot);
    }
    
    //watershed from seed within spot with a given threshold. make it to floodfill3D?
    // TODO threshol normal puis relabel
    
    public void localThreshold(Object3DVoxels spot, float thld, boolean rescue) {
        
        TreeSet<Vox3D> heap = new TreeSet<Vox3D>();
        Vox3D seed = getSeed(spot);
        //Vox3D seed = getLocalExtremum(spot); // dans le cas de 2 spots très proches, avec 2 extremas hessien mais un seul en intensite -> fonctionne mal si la seg depend du hessien
        if (debug) {
            System.out.println("Local Threshold: spot:"+spot.getValue()+ " thld:"+thld+ " max:"+seed.toString());
            ij.IJ.log("Local Threshold: spot:"+spot.getValue()+ " thld:"+thld+ " max:"+seed.toString());
        }
        heap.add(seed);
        //HashSet<Vox3D> newVox = new HashSet<Vox3D>(spot.getVoxels().size());
        //newVox.add(seed);
        int label = spot.getValue();
        int negLabel = Short.MAX_VALUE-1;
        for (Voxel3D v : spot.getVoxels()) segMap.setPixel(v.getRoundX(), v.getRoundY(), v.getRoundZ(), negLabel);
        seed.setLabel(label);
        while (!heap.isEmpty()) {
            Vox3D v = heap.pollFirst();
            int x = v.xy%segMap.sizeX;
            int y = v.xy/segMap.sizeX;
            if (x<limX && segMap.getPixelInt(v.xy+1, v.z)==negLabel && intensityMap.getPixel(v.xy+1, v.z)>=thld) {
                Vox3D vox = new Vox3D(v.xy+1, v.z, intensityMap.getPixel(v.xy+1, v.z));
                vox.setLabel(label);
                heap.add(vox);
            }
            if (x>0 && segMap.getPixelInt(v.xy-1, v.z)==negLabel && intensityMap.getPixel(v.xy-1, v.z)>=thld) {
                Vox3D vox = new Vox3D(v.xy-1, v.z, intensityMap.getPixel(v.xy-1, v.z));
                vox.setLabel(label);
                heap.add(vox);
            }
            if (y<limY && segMap.getPixelInt(v.xy+intensityMap.sizeX, v.z)==negLabel && intensityMap.getPixel(v.xy+intensityMap.sizeX, v.z)>=thld) {
                Vox3D vox = new Vox3D(v.xy+intensityMap.sizeX, v.z, intensityMap.getPixel(v.xy+intensityMap.sizeX, v.z));
                vox.setLabel(label);
                heap.add(vox);
            }
            if (y>0 && segMap.getPixelInt(v.xy-intensityMap.sizeX, v.z)==negLabel && intensityMap.getPixel(v.xy-intensityMap.sizeX, v.z)>=thld) {
                Vox3D vox = new Vox3D(v.xy-intensityMap.sizeX, v.z, intensityMap.getPixel(v.xy-intensityMap.sizeX));
                vox.setLabel(label);
                heap.add(vox);
            }
            if (v.z<limZ && segMap.getPixelInt(v.xy, v.z+1)==negLabel && intensityMap.getPixel(v.xy, v.z+1)>=thld) {
                Vox3D vox = new Vox3D(v.xy, v.z+1, intensityMap.getPixel(v.xy, v.z+1));
                vox.setLabel(label);
                heap.add(vox);
            }
            if (v.z>0 && segMap.getPixelInt(v.xy, v.z-1)==negLabel && intensityMap.getPixel(v.xy, v.z-1)>=thld) {
                Vox3D vox = new Vox3D(v.xy, v.z-1, intensityMap.getPixel(v.xy, v.z-1));
                vox.setLabel(label);
                heap.add(vox);
            }
        }
        LinkedList<Voxel3D> voxels = spot.getVoxels();
        LinkedList<Voxel3D> newVoxels = new LinkedList<>();
        for (Voxel3D v : voxels) {
            int xy = v.getRoundX() + v.getRoundY()*sizeX;
            if (segMap.getPixelInt(xy, v.getRoundZ())==negLabel) segMap.setPixel(xy, v.getRoundZ(), 0);
            else if (segMap.getPixelInt(xy, v.getRoundZ())==label) newVoxels.add(v);
        }
        if (debug) {
            System.out.println("Local Threshold: spot:"+spot.getValue()+ " newVoxels:"+newVoxels.size()+ " oldVoxels:"+voxels.size());
        }
        spot.setVoxels(newVoxels);
        if (rescue) {
            //rescue dead spots
            boolean change = true;
            while (change) {
                change=false;
                float max =thld;
                for (Voxel3D v : voxels) {
                    int xy = v.getRoundX() + v.getRoundY()*sizeX;
                    if (segMap.getPixelInt(xy, v.getRoundZ())==0 && intensityMap.getPixel(xy, v.getRoundZ())>max) max = intensityMap.getPixel(xy, v.getRoundZ());
                }
                if (max>thld) { // && max>globalThld
                    change=true;
                    LinkedList<Voxel3D> vox = new LinkedList<Voxel3D>();
                    float min = thld;
                    for (Voxel3D v : voxels) {
                        int xy = v.getRoundX() + v.getRoundY()*sizeX;
                        if (segMap.getPixelInt(xy, v.getRoundZ())==0) {
                            vox.add(v);
                            v.setValue(currentLabel);
                            segMap.setPixel(v.getRoundX(), v.getRoundY(), v.getRoundZ(), currentLabel);
                            if (intensityMap.getPixel(xy, v.getRoundZ())<min) min = intensityMap.getPixel(xy, v.getRoundZ());
                        }
                    }
                    if (vox.size()>1) {
                        if (debug) ij.IJ.log("rescue Spot: "+currentLabel);
                        
                        Object3DVoxels o = new Object3DVoxels(vox);
                        o.setValue(currentLabel);
                        currentLabel++;
                        rescuedSpots.add(o);
                        localThreshold(o, min, false);
                    }
                } 
            }
        }
    }
    
    
    protected Vox3D getMax(Object3D s) {
        float max = 0;
        Vox3D maxVox = null;
        for (Voxel3D v : s.getVoxels()) {
            float value = intensityMap.getPixel(v.getXYCoord(intensityMap.sizeX), v.getRoundZ()); 
            if (value>max) {
                max = value;
                maxVox = new Vox3D(v.getRoundX(), v.getRoundY(), v.getRoundZ(), max);
            }
        }
        return maxVox;
    }
    
    protected Vox3D getLocalExtremum(Object3D s) {
        float max = 0;
        Vox3D maxVox = null;
        for (Voxel3D v : s.getVoxels()) {
            float value = intensityMap.getPixel(v.getXYCoord(intensityMap.sizeX), v.getRoundZ());
            if (value>max && isLocalExtremum(v.getRoundX(), v.getRoundY(), v.getRoundZ(), value)) {
                max = value;
                maxVox = new Vox3D(v.getRoundX(), v.getRoundY(), v.getRoundZ(), max);
            }
        }
        if (maxVox==null) {
            if (debug) IJ.log("Spot: "+s.getValue()+ " no local extrema found");
            return getMax(s);
        }
        else {
            if (debug) IJ.log("Spot: "+s.getValue()+ " local extrema found : " + maxVox);
            return maxVox;
        }
    }
    
    protected boolean isLocalExtremum(int x, int y, int z, float intensity) {
        for (int zz = z-1; zz<=z+1; zz++) {
            if (zz>=0 && zz<intensityMap.sizeZ) {
                for (int yy = y-1; yy<=y+1; yy++) {
                    if (yy>=0 && yy<intensityMap.sizeY) {
                        for (int xx = x-1; xx<=x+1; xx++) {
                            if ((xx!=x || yy!=y || zz!=z) && xx>=0 && xx<sizeX) {
                                if (intensityMap.getPixel(xx+yy*sizeX, zz)>intensity) return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
    
    protected class Vox3D implements java.lang.Comparable<Vox3D> {
        public int xy, z;
        public float value;

        public Vox3D(int xy, int z, float value) {
            this.xy=xy;
            this.z=z;
            this.value=value;
        }
        
        public Vox3D(int x, int y, int z, float value) {
            this.xy=x+y*intensityMap.sizeX;
            this.z=z;
            this.value=value;
        }

        public void setLabel(int label) {
            segMap.setPixel(xy, z, label);
        }

        public int compareTo(Vox3D v) {
            if (v.xy==xy && v.z==z) return 0;
            // decreasing intensities
            else if(value < v.value) return 1;
            else if(value > v.value) return -1;
            //FIXME > l'inverse?
            else if (segMap!=null && segMap.getPixel(xy, z)<segMap.getPixel(xy, z)) return -1;
            else return 1;
        }
        
        @Override 
        public boolean equals(Object o) {
            if (o instanceof Vox3D) {
                return xy==((Vox3D)o).xy && z==((Vox3D)o).z;
            } return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 47 * hash + this.xy;
            hash = 47 * hash + this.z;
            return hash;
        }

        @Override
        public String toString() {
            return "x:"+xy%segMap.sizeX +" y:" + xy/segMap.sizeX + " z:"+z+ " value:"+value+ " label:"+segMap.getPixel(xy, z);
        }
    }
}
