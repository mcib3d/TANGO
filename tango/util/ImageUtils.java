package tango.util;

import i5d.Image5D;
import i5d.cal.ChannelDisplayProperties;
import i5d.gui.ChannelControl;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Plot;
import ij.process.ImageProcessor;
import java.awt.*;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelListener;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.ImageIcon;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;
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
public class ImageUtils {

    public static ImageIcon edit, editDiff, editError, add, remove, up, down, test;

    private static ImageIcon getIcon(String URL) {
        ImageIcon icon = new ImageIcon(ImageUtils.class.getResource(URL));
        Image img = icon.getImage();
        Image newimg = img.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(newimg);
    }

    public static void initIcons() {
        edit = getIcon("/tango/icons/edit.png");
        editDiff = getIcon("/tango/icons/edit_diff.png");
        editError = getIcon("/tango/icons/edit_error.png");
        add = getIcon("/tango/icons/add.png");
        remove = getIcon("/tango/icons/remove.png");
        up = getIcon("/tango/icons/up.png");
        down = getIcon("/tango/icons/down.png");
        test = getIcon("/tango/icons/test.png");
    }

    public static void zoom(ImagePlus image, double magnitude) {
        ImageCanvas ic = image.getCanvas();
        if (ic == null) {
            return;
        }
        ic.zoom100Percent();
        if (magnitude > 1) {
            for (int i = 0; i < (int) (magnitude + 0.5); i++) {
                ic.zoomIn(image.getWidth() / 2, image.getHeight() / 2);
            }
        } else if (magnitude > 0 && magnitude < 1) {
            for (int i = 0; i < (int) (1 / magnitude + 0.5); i++) {
                ic.zoomOut(image.getWidth() / 2, image.getHeight() / 2);
            }
        }
    }

    public static Image5D getImage5D(String title, ImageHandler[] images) {
        ImagePlus hyperstack = ImageHandler.getHyperStack(title, images);
        Image5D res = new Image5D(hyperstack.getShortTitle(), hyperstack.getStack(), images.length, images[0].sizeZ, 1);
        for (int i = 0; i < images.length; i++) {
            res.setChannelMinMax(i + 1, images[i].getMin(null), images[i].getMax(null));
            //res.setChannelCalibration(i+1, new ChannelCalibration(img.getCalibration()));

            res.setDefaultChannelNames();
        }

        for (int i = 0; i < images.length; i++) {
            Color c = tango.gui.util.Colors.colors.get(tango.gui.util.Colors.colorNames[i + 1]);
            ColorModel cm = ChannelDisplayProperties.createModelFromColor(c);
            res.setChannelColorModel(i + 1, cm);
        }
        res.setDisplayMode(ChannelControl.OVERLAY);
        return res;
    }

    public static void addScrollListener(ImagePlus img, AdjustmentListener al, MouseWheelListener ml) {
        //from Fiji code
        // TODO Find author...
        for (Component c : img.getWindow().getComponents()) {
            if (c instanceof Scrollbar) {
                ((Scrollbar) c).addAdjustmentListener(al);
            } else if (c instanceof Container) {
                for (Component c2 : ((Container) c).getComponents()) {
                    if (c2 instanceof Scrollbar) {
                        ((Scrollbar) c2).addAdjustmentListener(al);
                    }
                }
            }
        }
        img.getWindow().addMouseWheelListener(ml);
    }

    public static void removeScrollListener(ImagePlus img, AdjustmentListener al, MouseWheelListener ml) {
        //from Fiji code
        // TODO Find author...
        for (Component c : img.getWindow().getComponents()) {
            if (c instanceof Scrollbar) {
                ((Scrollbar) c).removeAdjustmentListener(al);
            } else if (c instanceof Container) {
                for (Component c2 : ((Container) c).getComponents()) {
                    if (c2 instanceof Scrollbar) {
                        ((Scrollbar) c2).removeAdjustmentListener(al);
                    }
                }
            }
        }
        img.getWindow().removeMouseWheelListener(ml);
    }

    public static void overrideKeyListeners(ImagePlus img, KeyListener kl) {
        Canvas rc = img.getWindow().getCanvas();
        KeyListener[] kls = rc.getKeyListeners();
        for (KeyListener kll : kls) {
            rc.removeKeyListener(kll);
        }
        rc.addKeyListener(kl);
    }

    public static void setObjectDistancesToPeriphery(Object3DVoxels object, int nbCPUs) {
        //ImageInt map = object.createSegImageMini(object.getValue(), 0);
        ImageInt map = object.getLabelImage();
        ImageFloat dm = map.getDistanceMapInsideMask(nbCPUs);
        /*ij.IJ.log("set dm values object:"+object.getValue());
         if (object.getValue()==7) {
         map.show();
         dm.show();
         ImageInt map2 = object.createSegImageMini(object.getValue(), 1);
         ImageFloat dm2 = map2.getDistanceMapInsideMask(nbCPUs);
         dm2.show("dm2");
         }*/
        int xMin = object.getXmin();
        int yMin = object.getYmin();
        int zMin = object.getZmin();
        for (Voxel3D v : object.getVoxels()) {
            v.setValue(dm.getPixel(v.getRoundX() - xMin, v.getRoundY() - yMin, v.getRoundZ() - zMin)); // TODO enlever border 1 lorsque bug EDT résolu
        }
    }

    public static Object3DVoxels[][] getObjectLayers(Object3DVoxels[] objects, double[][] layers, boolean correctionLimit, int nbCPUs, boolean verbose) {
        /*if (objects==null && objectMap!=null) objects = objectMap.getObjects3D();
         else if (objects!=null && objectMap==null) {
         // get bounding box
         int zMax=0, yMax=0, xMax=0;
         for (Object3DVoxels o : objects) {
         if (o.getXmax()>xMax) xMax=o.getXmax();
         if (o.getYmax()>yMax) yMax=o.getYmax();
         if (o.getZmax()>zMax) zMax=o.getZmax();
         }
         ImageByte om = new ImageByte("objectMax", xMax+1, yMax+1, zMax+1);
         objectMap = om;
         for (Object3DVoxels o : objects) o.draw(om, 255);
         }
         ImageFloat dm = objectMap.getDistanceMapInsideMask(nbCPUs);
         */
        Object3DVoxels[][] res = new Object3DVoxels[objects.length][layers.length];
        for (int i = 0; i < objects.length; i++) {
            res[i] = getLayers(objects[i], layers, correctionLimit, verbose);
        }

        if (verbose) {
            // get bounding box to draw image
            int zMax = 0, yMax = 0, xMax = 0;
            for (Object3DVoxels o : objects) {
                if (o.getXmax() > xMax) {
                    xMax = o.getXmax();
                }
                if (o.getYmax() > yMax) {
                    yMax = o.getYmax();
                }
                if (o.getZmax() > zMax) {
                    zMax = o.getZmax();
                }
            }
            ImageFloat dm = new ImageFloat("distanceMap", xMax + 1, yMax + 1, zMax + 1);
            for (Object3DVoxels o : objects) {
                for (Voxel3D v : o.getVoxels()) {
                    dm.setPixel(v, (float) v.getValue());
                }
            }
            dm.show("objectLayers distance map");
            ImageShort om = new ImageShort("object Layers", dm.sizeX, dm.sizeY, dm.sizeZ);
            for (int o = 0; o < objects.length; o++) {
                objects[o].draw(om, 1);
                for (int l = 0; l < layers.length; l++) {
                    res[o][l].draw(om, l + 2);
                }
            }
            om.show();
        }
        return res;
    }

    public static Object3DVoxels[] getLayers(Object3DVoxels object, double[][] layers, boolean correctionLimit, boolean verbose) {
        Object3DVoxels[] objectLayers = new Object3DVoxels[layers.length];
        ArrayList<Voxel3D> vox = object.getVoxels();
        Collections.sort(vox, new Comparator<Voxel3D>() {
            @Override
            public int compare(Voxel3D v1, Voxel3D v2) {
                if (v1.value < v2.value) {
                    return -1;
                } else if (v1.value > v2.value) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        for (int i = 0; i < layers.length; i++) {
            int idxStart = (int) (layers[i][0] * (vox.size() - 1) + 0.5);
            int idxStop = (int) (layers[i][1] * (vox.size() - 1) + 0.5);
            if (verbose) {
                IJ.log("object: " + object.getValue() + " idxStart: " + idxStart + " value: " + vox.get(idxStart).getValue() + "idxStop: " + idxStop + " value:" + vox.get(idxStop).getValue() + " value 0 : " + vox.get(0).getValue() + " value " + (vox.size() - 1) + " : " + vox.get(vox.size() - 1).getValue());
            }
            // Correction valeurs limites
            if (correctionLimit) {
                //int idxStartL = getLeftIndex(vox, idxStart);
                //int idxStartR = getRightIndex(vox, idxStart);
                //int idxStopL = getLeftIndex(vox, idxStop);
                //int idxStopR = getRightIndex(vox, idxStop);

                //if (idxStopL-idxStartR<=1)
                //if (idxStart>0 && vox.get(0).getValue()==vox.get(idxStart).getValue()) idxStart = 0;
                /*if (vox.get(0).getValue()==vox.get(idxStop).getValue()) { // cas où idxStop est dans la couche exterieure
                 if (verbose) IJ.log("object: "+object.getValue()+" correction limit ext: idxStart: "+idxStart+ " idxStop: "+idxStop+" value: "+vox.get(0).getValue());
                 idxStop = getRightIndex(vox, idxStop);
                 idxStart=0;
                 if (verbose) IJ.log("object: "+object.getValue()+" correction limit ext: new idxStop: "+idxStop);
                 }*/
                // cas où il y a une seule couche
                /*if (vox.get(vox.size()-1).getValue()==vox.get(idxStop).getValue() && vox.get(0).getValue()==vox.get(idxStart).getValue() && vox.get(vox.size()-1).getValue()!=vox.get(0).getValue()) {
                 if (verbose) IJ.log("object: "+object.getValue()+" correction limit int: idxStart: "+idxStart+ " idxStop: "+idxStop+" value: "+vox.get(vox.size()-1).getValue());
                 idxStart = getRightIndex(vox, idxStart);
                 if (idxStart<vox.size()-2) idxStart++;
                 if (verbose) IJ.log("object: "+object.getValue()+" correction limit int: new idxStart: "+idxStart+" value: "+vox.get(idxStart).getValue());
                 } //else idxStart = getLeftIndex(vox, idxStart);
                 */
                if (layers[i][1] == 1 && layers[i][0] > 0 && vox.get(idxStart).value == vox.get(0).value) {
                    if (verbose) {
                        IJ.log("object: " + object.getValue() + " correction limit start: idxStart: " + idxStart + " idxStop: " + idxStop + " value: " + vox.get(vox.size() - 1).getValue());
                    }
                    idxStart = getRightIndex(vox, idxStart);
                    if (idxStart < vox.size() - 2) {
                        ++idxStart;
                    }
                    if (verbose) {
                        IJ.log("object: " + object.getValue() + " correction limit start: new idxStart: " + idxStart + " value: " + vox.get(idxStart).getValue());
                    }
                } else if (layers[i][0] == 0 && layers[i][1] < 1 && vox.get(idxStop).value == vox.get(0).value) {
                    if (verbose) {
                        IJ.log("object: " + object.getValue() + " correction limit stop: idxStart: " + idxStart + " idxStop: " + idxStop + " value: " + vox.get(0).getValue());
                    }
                    idxStop = getRightIndex(vox, idxStop);
                    if (verbose) {
                        IJ.log("object: " + object.getValue() + " correction limit stop: new idxStop: " + idxStop);
                    }
                }

            }

            ArrayList<Voxel3D> voxObj = new ArrayList<Voxel3D>(idxStop - idxStart + 1);
            for (int j = idxStart; j <= idxStop; j++) {
                voxObj.add(vox.get(j));
            }
            if (voxObj.isEmpty()) {
                if (idxStop < (vox.size() - 1)) {
                    voxObj.add(vox.get(idxStop + 1));
                } else if (idxStart > 0) {
                    voxObj.add(vox.get(idxStart - 1));
                } else {
                    voxObj = vox;
                }
            }
            objectLayers[i] = new Object3DVoxels(voxObj);
            objectLayers[i].setValue(object.getValue());
        }
        return objectLayers;
    }

    private static int getLeftIndex(ArrayList<Voxel3D> sorted, int idx) {
        int idxMin = idx;
        double ref = sorted.get(idx).getValue();
        while (idxMin > 0 && sorted.get(idxMin - 1).getValue() == ref) {
            idxMin--;
        }
        return idxMin;
    }

    private static int getRightIndex(ArrayList<Voxel3D> sorted, int idx) {
        int idxMax = idx;
        double ref = sorted.get(idx).getValue();
        int limit = sorted.size() - 1;
        while (idxMax < limit && sorted.get(idxMax + 1).getValue() == ref) {
            idxMax++;
        }
        return idxMax;
    }

    public static int[][] getNeighbourhood(float radius, float radiusZ) {
        float r = (float) radius / radiusZ;
        int rad = (int) (radius + 0.5f);
        int radZ = (int) (radiusZ + 0.5f);
        int[][] temp = new int[3][(2 * rad + 1) * (2 * rad + 1) * (2 * radZ + 1)];
        //float[] tempDist = new float[temp[0].length];
        int count = 0;
        float rad2 = radius * radius;
        for (int zz = -radZ; zz <= radZ; zz++) {
            for (int yy = -rad; yy <= rad; yy++) {
                for (int xx = -rad; xx <= rad; xx++) {
                    float d2 = zz * r * zz * r + yy * yy + xx * xx;
                    if (d2 <= rad2 && d2 > 0) {	//exclusion du point central
                        temp[0][count] = xx;
                        temp[1][count] = yy;
                        temp[2][count] = zz;
                        //tempDist[count] = (float) Math.sqrt(d2);
                        count++;
                    }
                }
            }
        }

        //distances = new float[count];
        //System.arraycopy(tempDist, 0, distances, 0, count);

        /*
         * Integer[] order = new Integer[distances.length]; for (int i = 0; i <
         * order.length; i++) order[i]=i; Arrays.sort(order, new
         * ComparatorDistances()); Arrays.sort(distances); for (int i = 0;
         * i<count; i++) { vois[0][i]=temp[0][order[i]];
         * vois[1][i]=temp[1][order[i]]; vois[2][i]=temp[2][order[i]]; }
         *
         */
        int[][] res = new int[3][count];
        System.arraycopy(temp[0], 0, res[0], 0, count);
        System.arraycopy(temp[1], 0, res[1], 0, count);
        System.arraycopy(temp[2], 0, res[2], 0, count);
        return res;
    }

    public static int[][] getNeighbourhood(float radius, float radiusZ, float thickness) {
        if (radiusZ > 0 && radiusZ < 1) {
            radiusZ = 1;
        }
        //IJ.log("neigh: XY:"+radius+" Z:"+radiusZ+ " Thickness:"+thickness);
        float r = (float) radius / radiusZ;
        int rad = (int) (radius + 0.5f);
        int radZ = (int) (radiusZ + 0.5f);
        int[][] temp = new int[3][(2 * rad + 1) * (2 * rad + 1) * (2 * radZ + 1)];
        //float[] tempDist = new float[temp[0].length];
        int count = 0;
        float rad2 = radius * radius;
        float radMin = (radius >= thickness) ? (float) Math.pow(radius - thickness, 2) : Float.MIN_VALUE; //:exclusion du point central
        for (int zz = -radZ; zz <= radZ; zz++) {
            for (int yy = -rad; yy <= rad; yy++) {
                for (int xx = -rad; xx <= rad; xx++) {
                    float d2 = zz * r * zz * r + yy * yy + xx * xx;
                    if (d2 <= rad2 && d2 > radMin && !((xx == 0) && (yy == 0) && (zz == 0))) {
                        temp[0][count] = xx;
                        temp[1][count] = yy;
                        temp[2][count] = zz;
                        //tempDist[count] = (float) Math.sqrt(d2);
                        //IJ.log("neigh: X:"+xx+" Y:"+yy+ " Z:"+zz);
                        count++;
                    }
                }
            }
        }
        int[][] res = new int[3][count];
        System.arraycopy(temp[0], 0, res[0], 0, count);
        System.arraycopy(temp[1], 0, res[1], 0, count);
        System.arraycopy(temp[2], 0, res[2], 0, count);
        //ij.IJ.log("Neigbor: Radius:"+radius+ " radiusZ"+radiusZ+ " count:"+count);
        return res;
    }

    public static int[][] getHalfNeighbourhood(float radius, float radiusZ, float thickness) {
        if (radiusZ > 0 && radiusZ < 1) {
            radiusZ = 1;
        }
        //IJ.log("neigh: XY:"+radius+" Z:"+radiusZ+ " Thickness:"+thickness);
        float r = (float) radius / radiusZ;
        int rad = (int) (radius + 0.5f);
        int radZ = (int) (radiusZ + 0.5f);
        int[][] temp = new int[3][(2 * rad + 1) * (2 * rad + 1) * (2 * radZ + 1)];
        //float[] tempDist = new float[temp[0].length];
        int count = 0;
        float rad2 = radius * radius;
        float radMin = (radius >= thickness) ? (float) Math.pow(radius - thickness, 2) : Float.MIN_VALUE; //exclusion du point central
        for (int zz = 0; zz <= radZ; zz++) {
            for (int yy = (zz == 0) ? 0 : -rad; yy <= rad; yy++) {
                for (int xx = (zz == 0 && yy == 0) ? -rad + 1 : -rad; xx <= rad; xx++) {
                    float d2 = zz * r * zz * r + yy * yy + xx * xx;
                    if (d2 <= rad2 && d2 > radMin) {
                        temp[0][count] = xx;
                        temp[1][count] = yy;
                        temp[2][count] = zz;
                        //tempDist[count] = (float) Math.sqrt(d2);
                        //IJ.log("neigh: X:"+xx+" Y:"+yy+ " Z:"+zz);
                        count++;
                    }
                }
            }
        }
        int[][] res = new int[3][count];
        System.arraycopy(temp[0], 0, res[0], 0, count);
        System.arraycopy(temp[1], 0, res[1], 0, count);
        System.arraycopy(temp[2], 0, res[2], 0, count);
        //ij.IJ.log("Neigbor: Radius:"+radius+ " radiusZ"+radiusZ+ " count:"+count);
        return res;
    }

    public static void plotMeanZprofile(String title, ImageInt mask, ImageHandler intensity) {
        if (title == null) {
            title = intensity.getTitle();
        }
        double[] mean = new double[mask.sizeZ];
        double[] slice = new double[mask.sizeZ];
        int count;
        for (int z = 0; z < mask.sizeZ; z++) {
            count = 0;
            for (int xy = 0; xy < mask.sizeXY; xy++) {
                if (mask.getPixel(xy, z) != 0) {
                    mean[z] += intensity.getPixel(xy, z);
                    count++;
                }
            }
            if (count != 0) {
                mean[z] /= count;
            }
            slice[z] = z + 1;
        }
        Plot plot = new Plot(title, "Slice", "Mean Value", slice, mean);
        plot.show();
    }

    static public boolean containsValue(ImageProcessor img, int value) {
        int s = img.getWidth() * img.getHeight();
        for (int i = 0; i < s; i++) {
            if (img.get(i) == value) {
                return true;
            }
        }

        return false;
    }
}
