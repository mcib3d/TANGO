package tango.plugin.filter;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.AutoThresholder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.*;
import mcib3d.image3d.processing.BinaryMorpho;
import tango.dataStructure.InputImages;
import tango.parameter.*;
import tango.plugin.TangoPlugin;
import tango.plugin.thresholder.AutoThreshold;
import tango.plugin.thresholder.Thresholder;
import tango.util.ImageUtils;
import tango.util.SpearmanPairWiseCorrelationTest;
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
public class EraseSpots implements PostFilter {
    boolean debug;
    int nbCPUs=1;
    final int LAYER_SIZE_LIMIT=5;
    BooleanParameter useFiltered = new BooleanParameter("Use filtered Image", "useFiltered", true);
    PreFilterSequenceParameter preFilters = new PreFilterSequenceParameter("Pre-Filters:", "filters");
    static String[] methods = new String[]{"SNR", "Absolute Intensity", "Layer Comparison", "Correlation Distance Map/Intensity"};
    static String[] methodsBackground = new String[]{"whole nucleus", "nucleus minus spots"}; //, "nucleus minus dialted spots"
    static String[] methodsSignal = new String[]{"mean intensity", "max intensity", "quantile"};
    ChoiceParameter criterionChoice = new ChoiceParameter("Criterion:", "criterionChoice", methods, methods[1]);
    ConditionalParameter criterion = new ConditionalParameter("Erase Criterion", criterionChoice);
    Parameter[] defaultParameters = new Parameter[]{criterion};
    MultiParameter criteria;
    
    public EraseSpots() {
        DoubleParameter minSNR = new DoubleParameter("Minimal SNR:", "minSNR", 2d, Parameter.nfDEC5);
        ChoiceParameter bckChoice = new ChoiceParameter("Background estimation:", "backgroundChoice", methodsBackground, methodsBackground[0]);
        ConditionalParameter dilCond = new ConditionalParameter(bckChoice);
        //dilCond.setCondition(methodsBackground[2], new Parameter[]{new IntParameter("Radius (pixels)", "radius", 1)});
        ChoiceParameter signalChoice = new ChoiceParameter("Intensity estimation:", "signalChoice", methodsSignal, methodsSignal[0]);
        ConditionalParameter signalCond = new ConditionalParameter(signalChoice);
        signalCond.setCondition(methodsSignal[2], new Parameter[]{new SliderDoubleParameter("Quantile", "quantile", 0, 1, 0.9d, 4)});
        criterion.setCondition(methods[0], new Parameter[]{minSNR, signalCond, new SliderDoubleParameter("Erode object? Proprotion of object erosion:", "erode", 0, 1, 0, 2), new BooleanParameter("Keep only eroded fraction?", "keepEroded", false), dilCond});
        criterion.setCondition(methods[1], new Parameter[]{signalCond, new SliderDoubleParameter("Erode object? Proprotion of object erosion:", "erode", 0, 1, 0, 2), new BooleanParameter("Keep only eroded fraction?", "keepEroded", false), new ThresholdParameter("Threshold estimation", "thld", "Percentage Of Bright Pixels" )}); //"AutoThreshold", new Parameter[]{new ChoiceParameter("", "", new String[]{"OTSU"}, "OTSU")})}
        criterion.setCondition(methods[2], new Parameter[]{
            new DoubleParameter("Dilate spots? Radius:", "dilate", 1d, Parameter.nfDEC2),
            new GroupParameter("Layer 1", "layer1", new Parameter[]{
                new SliderDoubleParameter("Min", "l1min", 0, 1, 0, 2),
                new SliderDoubleParameter("Miax", "l1max", 0, 1, 0, 2),
                signalCond
            }),
            new GroupParameter("Layer 2", "layer2", new Parameter[]{
                new SliderDoubleParameter("Min", "l2min", 0, 1, 0, 2),
                new SliderDoubleParameter("Miax", "l2max", 0, 1, 0, 2),
                signalCond
            }), 
            new DoubleParameter("Erase spot if I(L1)/I(L2) > threshold. Threhsold?", "thld", 1d, Parameter.nfDEC3)
        });
        ConditionalParameter corrCond = new ConditionalParameter(new ChoiceParameter("Compute (Spearman correlation test):", "compute", new String[]{"p-value", "rho"}, "p-value"));
        corrCond.setCondition("p-value", new Parameter[]{new ChoiceParameter("Expecting:", "expecting", new String[]{"Correlation", "Anti-Correlation", "Both"}, "Correlation"), new DoubleParameter("Threshold value for p-value:", "thldpvalue", 0.001d, Parameter.nfDEC5), new IntParameter("Test precision (number of permutations)", "testPrecision", 10000)});
        corrCond.setCondition("rho", new Parameter[]{new ChoiceParameter("Expecting:", "expecting", new String[]{"Correlation", "Anti-Correlation", "Both"}, "Correlation"), new DoubleParameter("Threshold value for roh:", "thldrho", 0.9d, Parameter.nfDEC3)});
        criterion.setCondition(methods[3], new Parameter[]{
            new DoubleParameter("Dilate spots? Radius:", "dilate", 1d, Parameter.nfDEC2),
            corrCond
        });
        criteria = new MultiParameter("Criteria", "criteria", defaultParameters, 1, 100, 1);
    }
    
    @Override
    public ImageInt runPostFilter(int currentStructureIdx, ImageInt in, InputImages images) {
        ImageHandler input = useFiltered.isSelected()?images.getFilteredImage(currentStructureIdx):images.getImage(currentStructureIdx);
        ImageHandler intensityMap = preFilters.runPreFilterSequence(currentStructureIdx, input, images, nbCPUs, debug);
        if (debug) intensityMap.showDuplicate("erase spot input image");
        ArrayList<Parameter[]> alCrit = criteria.getParametersArrayList();
        Object3DVoxels[] objectsArray = getObjects3D(in);
        ArrayList<Object3DVoxels> objects = new ArrayList<Object3DVoxels>(Arrays.asList(objectsArray));
        boolean isDistanceToPeripherySet=false;
        for (Parameter[] p : alCrit) {
            ConditionalParameter crit =  (ConditionalParameter)p[0];
            int idx = ((ChoiceParameter)crit.getActionnableParameter()).getSelectedIndex();
            if (idx==0) { //SNR
                DoubleParameter snr = (DoubleParameter)crit.getParameters()[0];
                ConditionalParameter signalCond = (ConditionalParameter)crit.getParameters()[1];
                int sig = ((ChoiceParameter)signalCond.getActionnableParameter()).getSelectedIndex();
                double quant = -1;
                if (sig==1) quant=1;
                else if (sig==2) quant = ((SliderDoubleParameter)signalCond.getParameters()[0]).getValue();
                double erode = -1;
                erode = ((SliderDoubleParameter)crit.getParameters()[2]).getValue();
                if (erode>0 && erode<1 && !isDistanceToPeripherySet) {
                    for (Object3DVoxels o : objects) ImageUtils.setObjectDistancesToPeriphery(o, nbCPUs);
                    isDistanceToPeripherySet=true;
                }
                ConditionalParameter dilCond = (ConditionalParameter)crit.getParameters()[4];
                int bck = ((ChoiceParameter)dilCond.getActionnableParameter()).getSelectedIndex();
                int dilate = 0;
                if (bck==0) dilate=-1;
                else if (bck==2) dilate = ((IntParameter)dilCond.getParameters()[0]).getIntValue(1);
                boolean periphery = ((BooleanParameter)crit.getParameters()[3]).isSelected();
                eraseObjectsSNR(objects, in, dilate, images.getMask(), intensityMap, quant, snr.getDoubleValue(2), erode, periphery);
            } else if (idx==1) { //intensity
                ConditionalParameter signalCond = (ConditionalParameter)crit.getParameters()[0];
                int sig = ((ChoiceParameter)signalCond.getActionnableParameter()).getSelectedIndex();
                double quant = -1;
                if (sig==1) quant=1;
                else if (sig==2) quant = ((SliderDoubleParameter)signalCond.getParameters()[0]).getValue();
                double erode = -1;
                erode = ((SliderDoubleParameter)crit.getParameters()[1]).getValue();
                if (erode>0 && erode<1 && !isDistanceToPeripherySet) {
                    for (Object3DVoxels o : objects) ImageUtils.setObjectDistancesToPeriphery(o, nbCPUs);
                    isDistanceToPeripherySet=true;
                }
                ThresholdParameter thld = (ThresholdParameter)crit.getParameters()[3];
                double thldValue = thld.getThreshold(intensityMap, images, nbCPUs, debug);
                boolean periphery = ((BooleanParameter)crit.getParameters()[2]).isSelected();
                objects = eraseObjectsIntensity(objects, in, intensityMap, quant, thldValue, erode, periphery);
            } else if (idx==2) { //layer comparison
                
                if (!isDistanceToPeripherySet) {
                    for (Object3DVoxels o : objects) ImageUtils.setObjectDistancesToPeriphery(o, nbCPUs);
                    isDistanceToPeripherySet=true;
                }
                // layer 1
                GroupParameter l1 = (GroupParameter)crit.getParameters()[1];
                double l1Min = ((SliderDoubleParameter)l1.getParameters()[0]).getValue();
                double l1Max = ((SliderDoubleParameter)l1.getParameters()[1]).getValue();
                ConditionalParameter signalCond1 = (ConditionalParameter)l1.getParameters()[2];
                int sig1 = ((ChoiceParameter)signalCond1.getActionnableParameter()).getSelectedIndex();
                double quant1 = -1;
                if (sig1==1) quant1=1;
                else if (sig1==2) quant1 = ((SliderDoubleParameter)signalCond1.getParameters()[0]).getValue();
                
                //layer 2
                GroupParameter l2 = (GroupParameter)crit.getParameters()[2];
                double l2Min = ((SliderDoubleParameter)l2.getParameters()[0]).getValue();
                double l2Max = ((SliderDoubleParameter)l2.getParameters()[1]).getValue();
                ConditionalParameter signalCond2 = (ConditionalParameter)l2.getParameters()[2];
                int sig2 = ((ChoiceParameter)signalCond2.getActionnableParameter()).getSelectedIndex();
                double quant2 = -1;
                if (sig2==1) quant2=1;
                else if (sig2==2) quant2 = ((SliderDoubleParameter)signalCond2.getParameters()[0]).getValue();
                
                double thld = ((DoubleParameter)crit.getParameters()[3]).getDoubleValue(1);
                double dilate = ((DoubleParameter)crit.getParameters()[0]).getDoubleValue(1);
                
                objects = eraseObjectsLayerComparison(objects, in, intensityMap, (float)dilate, quant1, l1Min, l1Max, quant2, l2Min, l2Max, thld);
                
            } else if (idx==3) { //correlation
                double dilate = ((DoubleParameter)crit.getParameters()[0]).getDoubleValue(1);
                ConditionalParameter corrCond = (ConditionalParameter)crit.getParameters()[1];
                boolean pValue = ((ChoiceParameter)corrCond.getActionnableParameter()).getSelectedIndex()==0;
                double thld;
                int corr;
                int expecting = ((ChoiceParameter)corrCond.getParameters()[0]).getSelectedIndex();
                thld = ((DoubleParameter)corrCond.getParameters()[1]).getDoubleValue(0.9);
                if (expecting == 0) corr=1;
                else if (expecting ==1) corr=-1;
                else corr=0;
                int permutationsNb=pValue? ((IntParameter)corrCond.getParameters()[2]).getIntValue(10000) : 0;

                objects = eraseObjectsCorrelation(objects, in, intensityMap, (float)dilate, thld, pValue, corr, permutationsNb);
            }
        }
        return in;
    }
    
    
    public void eraseObjectsSNR(ArrayList<Object3DVoxels> objects, ImageInt in, int dilate, ImageInt mask, ImageHandler intensity, double quantile, double thld, double erode, boolean periphery) {
        if (objects.isEmpty()) return;
        // get noise
        Object3DVoxels bcg;
        if (dilate>0) {
            ImageByte dil = BinaryMorpho.binaryDilate(in, dilate, Math.max((int)(dilate * in.getScaleXY()/in.getScaleZ()+0.5), 1), nbCPUs);
            bcg  = dil.getObject3DBackground(mask);
            if (debug) dil.show("Dilated Image - radius:"+dilate);
            if (bcg.getVolumePixels()==0) {
                ij.IJ.log("eraseSpots error: no background after dilate");
                bcg = in.getObject3DBackground(mask);
            }
        } else if (dilate==0) bcg = in.getObject3DBackground(mask);
        else bcg=getObjects3D(mask)[0];
        if (bcg.getVolumePixels()==0) {
            ij.IJ.log("eraseSpots error: no background");
            bcg=getObjects3D(mask)[0];
        }
        double sigma = bcg.getPixStdDevValue(intensity);
        double mean = bcg.getPixMeanValue(intensity);
        int count = 0;
        int nbInit=objects.size();
        Object3DVoxels[][] eroded=null;
        if (erode>0 && erode<1) {
            double[][] layers = periphery? new double[][]{{0, erode}} : new double[][]{{erode, 1}};
            eroded = ImageUtils.getObjectLayers(objects.toArray(new Object3DVoxels[objects.size()]), layers, true, nbCPUs, debug);
        }
        Iterator<Object3DVoxels> it = objects.iterator();
        while(it.hasNext()) {
            Object3DVoxels o = it.next();
            double I;
            if (quantile<0) {
                if (eroded!=null) I= eroded[count][periphery?1:0].getPixMeanValue(intensity);
                else I= o.getPixMeanValue(intensity);
            }
            else {
                if (eroded!=null) I= eroded[count][periphery?1:0].getQuantilePixValue(intensity, quantile);
                else I=o.getQuantilePixValue(intensity, quantile);
            }
            double snrValue = (I-mean) / sigma;
            if (debug) ij.IJ.log("EraseSpots::SNR::spot:"+o.getValue()+ " snr:"+snrValue+ " thld:"+thld+ (snrValue<thld?" erased":""));
            if (snrValue<thld) {
                o.draw(in, 0);
                it.remove();
            }
            count++;
        }
        if (debug) ij.IJ.log("erase spots: remaining objects: "+objects.size()+"/"+nbInit);
    }
    
    public ArrayList<Object3DVoxels> eraseObjectsIntensity(ArrayList<Object3DVoxels> objects, ImageInt in, ImageHandler intensity, double quantile, double thld, double erode, boolean periphery) {
        if (objects.isEmpty()) return objects;
        Object3DVoxels[][] eroded=null;
        if (erode>0 && erode<1) {
            double[][] layers = periphery? new double[][]{{0, erode}} : new double[][]{{erode, 1}};
            eroded = ImageUtils.getObjectLayers(objects.toArray(new Object3DVoxels[objects.size()]), layers, true, nbCPUs, debug);
        }
        if (debug) ij.IJ.log("EraseSpots::nb objects:"+objects.size());
        Iterator<Object3DVoxels> it = objects.iterator();
        int count = 0;
        while(it.hasNext()) {
            Object3DVoxels o = it.next();
            double I;
            if (quantile<0) {
                if (eroded!=null) I = eroded[count][0].getPixMeanValue(intensity);
                else I= o.getPixMeanValue(intensity);
            }
            else {
                if (eroded!=null) I=eroded[count][0].getQuantilePixValue(intensity, quantile);
                else I=o.getQuantilePixValue(intensity, quantile);
            }
            if (debug) ij.IJ.log("EraseSpots::Intensity::spot:"+o.getValue()+ " intensity: "+I+ " thld:"+thld+ (I<thld?"erased":"") + "count:"+count);
            if (I<thld) {
                o.draw(in, 0);
                it.remove();
            }
            count++;
        }
        return objects;
    }
    
    public ArrayList<Object3DVoxels> eraseObjectsLayerComparison(ArrayList<Object3DVoxels> objects, ImageInt in, ImageHandler intensity, float dilate, double quantileL1, double l1Min, double l1Max, double quantileL2, double l2Min, double l2Max, double thld) {
        if (objects.isEmpty()) return objects;
        Object3DVoxels[] dilObjects=null;
        if (dilate>0) {
            dilObjects = new Object3DVoxels[objects.size()];
            int idx = 0;
            for (Object3DVoxels o : objects) {
                dilObjects[idx] = o.dilate(dilate, in, nbCPUs);
                ImageUtils.setObjectDistancesToPeriphery(dilObjects[idx], nbCPUs);
                idx++;
            }
            if (debug) {
                ImageInt objDilate = (ImageInt) ImageInt.newBlankImageHandler("dilated objects", in);
                for (Object3DVoxels o : dilObjects) o.draw(objDilate);
                objDilate.show();
            }
        }
        
        double[][] layers = new double[][]{{1-l1Max, 1-l1Min}, {1-l2Max, 1-l2Min}};
        Object3DVoxels[][] objectLayers = ImageUtils.getObjectLayers(dilate>0?dilObjects:objects.toArray(new Object3DVoxels[objects.size()]), layers, true, nbCPUs, debug);
        
        if (debug) ij.IJ.log("EraseSpots::nb objects:"+objects.size());
        Iterator<Object3DVoxels> it = objects.iterator();
        int count = 0;
        while(it.hasNext()) {
            Object3DVoxels o = it.next();
            double I1;
            if (quantileL1<0) I1 = objectLayers[count][0].getPixMeanValue(intensity);
            else I1=objectLayers[count][0].getQuantilePixValue(intensity, quantileL1);
            double I2;
            if (quantileL2<0) I2 = objectLayers[count][1].getPixMeanValue(intensity);
            else I2=objectLayers[count][1].getQuantilePixValue(intensity, quantileL2);
            
            if (debug) ij.IJ.log("EraseSpots::Intensity::spot:"+o.getValue()+ " intensity layer 1: "+I1+ " intensity Layer 2: "+I2+ "I1/I2: "+I1/I2+ " threshold: "+thld);
            if (I2>0 && I1/I2>thld) {
                o.draw(in, 0);
                it.remove();
            }
            count++;
        }
        return objects;
    }

    public ArrayList<Object3DVoxels> eraseObjectsCorrelation(ArrayList<Object3DVoxels> objects, ImageInt in, ImageHandler intensity, float dilate, double thld, boolean pValue, int tail, int permutationNb) {
        if (objects.isEmpty()) return objects;
        Object3DVoxels[] dilObjects=null;
        if (dilate>0) {
            dilObjects = new Object3DVoxels[objects.size()];
            int idx = 0;
            for (Object3DVoxels o : objects) {
                dilObjects[idx] = o.dilate(dilate, in, nbCPUs);
                ImageUtils.setObjectDistancesToPeriphery(dilObjects[idx], nbCPUs);
                idx++;
            }
            if (debug) {
                ImageInt objDilate = (ImageInt) ImageInt.newBlankImageHandler("dilated objects", in);
                for (Object3DVoxels o : dilObjects) o.draw(objDilate);
                objDilate.show();
            }
        }if (debug) ij.IJ.log("EraseSpots::nb objects:"+objects.size());
        Iterator<Object3DVoxels> it = objects.iterator();
        int count = 0;
        while(it.hasNext()) {
            Object3DVoxels o = it.next();
            Object3DVoxels dilO = dilate>0?dilObjects[count]:o;
            float[] values = dilO.getValueArray(intensity);
            float[] distances = new float[values.length];
            int idx=0;
            for (Voxel3D v : dilO.getVoxels()) distances[idx++] = (float)v.getValue();
            if (pValue) {
                double[] res = SpearmanPairWiseCorrelationTest.performTest(values, distances, tail, permutationNb);
                if (res[1]>thld) {
                    o.draw(in, 0);
                    it.remove();
                }
                if (debug) ij.IJ.log("EraseSpots::Correlation::spot:"+o.getValue()+ " rho: "+res[0]+ "p-value: "+ res[1]*1000 +"*10-3 thld: "+thld+ (res[1]>thld?"erased":"") + "count:"+count);
                
            } else {
                double rho = SpearmanPairWiseCorrelationTest.computeRho(SpearmanPairWiseCorrelationTest.computeD2(values, distances), distances.length);
                boolean erase=false;
                if ((tail>0 && rho>=thld) || (tail<0&& rho<=thld) || (tail==0 && rho>=Math.abs(rho) ) ) {
                    o.draw(in, 0);
                    it.remove();
                    erase=true;
                } 
                if (debug) ij.IJ.log("EraseSpots::Correlation::spot:"+o.getValue()+ " rho: "+rho+" thld: "+thld+ (erase?"erased":"") + "count:"+count);
            }
            count++;
        }
        return objects;
    }
    
    @Override
    public void setVerbose(boolean debug) {
        this.debug=debug;
    }

    @Override
    public void setMultithread(int nbCPUs) {
        this.nbCPUs=nbCPUs;
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[]{useFiltered, preFilters, criteria};
    }

    @Override
    public String getHelp() {
        return "Erase Objects according to their signal-to-noise ratio or their mean intensity. If the eroded value is positive, spots will be eroded of user-defined proportion for signal calculation";
    }
    
}
