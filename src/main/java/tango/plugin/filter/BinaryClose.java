package tango.plugin.filter;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.processing.BinaryMorpho;
import mcib3d.utils.exceptionPrinter;
import tango.dataStructure.InputImages;
import tango.parameter.BooleanParameter;
import tango.parameter.ConditionalParameter;
import tango.parameter.DoubleParameter;
import tango.parameter.Parameter;

import java.util.HashMap;
import java.util.TreeMap;

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
public class BinaryClose implements PostFilter, PlugIn {
    boolean debug;
    int nbCPUs = 1;
    // TODO utiliser une methode classique si le rayon est petit
    DoubleParameter radiusXY = new DoubleParameter("XY-radius: ", "radiusXY", 1d, Parameter.nfDEC1);
    DoubleParameter radiusZ = new DoubleParameter("Z-radius: ", "radiusZ", 1d, Parameter.nfDEC1);
    BooleanParameter useScale = new BooleanParameter("Use Image Scale for Z radius: ", "useScale", true);
    BooleanParameter keepHoles = new BooleanParameter("Keep holes", "keepHoles", false);
    //BooleanParameter dilate = new BooleanParameter("Dilate: ", "dilate", false);
    HashMap<Object, Parameter[]> map = new HashMap<Object, Parameter[]>() {
        {
            put(false, new Parameter[]{radiusZ});
            put(true, new Parameter[0]);
        }
    };
    ConditionalParameter cond = new ConditionalParameter("Z-Radius", useScale, map);
    Parameter[] parameters = new Parameter[]{radiusXY, cond, keepHoles};

    public BinaryClose() {
        radiusXY.setHelp("Radius in XY direction (pixels)", true);
        radiusZ.setHelp("Radius in Z direction (pixels)", true);
        useScale.setHelp("If selected, the radius in Z direction will be computed according to the image anisotropy", true);
        useScale.setHelp("If selected, radiusZ = radiusXY * scaleXY / scaleZ", false);
        keepHoles.setHelp("Keep holes (if any) intacts. Inside holes may disappear due to closing.", true);
        //dilate.setHelp("If selected a dilatation of one voxel is applied to the nucleus mask (after operation)", true);
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public void setVerbose(boolean debug) {
        this.debug = debug;
    }

    @Override
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
                IJ.log("binaryClose: radius XY:" + radXY + " radZ:" + radZ + " nbCPUs:" + nbCPUs);
            }
            ImageInt res = BinaryMorpho.binaryCloseMultilabel(input, radXY, radZ, nbCPUs);

            // Keep holes
            if (keepHoles.isSelected()) {
                ImageInt holes=null;
                // copied from FillHoles2D --> do a class ?
                ImageInt fill;
                TreeMap<Integer, int[]> bounds = input.getBounds(true);
                if (bounds.size() > 1) {
                    ImageByte[] masks = input.crop3DBinary(bounds);
                    for (ImageByte mask : masks) {
                        tango.util.FillHoles2D.fill(mask, 255, 0);
                    }
                    fill = ImageHandler.merge3DBinary(masks, input.sizeX, input.sizeY, input.sizeZ);
                    holes = fill.subtractImage(input);
                    holes.invertBackground(0, 1);
                } else if (bounds.size() == 1) {
                    ImageByte ib = new ImageByte(input, true);
                    tango.util.FillHoles2D.fill(ib, 255, 0);
                    fill = ib;
                    holes = fill.subtractImage(input.thresholdAboveExclusive(0));
                    holes.invertBackground(0, 1);                    
                }                
                // intersect with close
                res.intersectMask(holes);
            }

            return res;
            // option dilate enlevée car plus d'interêt de l'inclure si on ne le fait pas en même temps que la fermeture, ça complique le plugin.
            /*if (dilate.isSelected()) {
             return mcib3d.image3d.processing.BinaryMorpho.binaryDilateMultilabel(input, 1, 1, nbCPUs);
             } else {
             return close;
             }
             */

        } catch (Exception e) {
            exceptionPrinter.print(e, "", true);
        }
        return null;
    }

    @Override
    public void setMultithread(int nbCPUs) {
        this.nbCPUs = nbCPUs;
    }

    @Override
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        if (imp == null || !(imp.getBitDepth() == 8 || imp.getBitDepth() == 16)) {
            IJ.log("needs 16-bit or 8-bit labelled image");
            return;
        }
        IJ.showStatus("binaryClose");
        GenericDialog gd = new GenericDialog("BinaryClose");
        gd.addNumericField("radiusXY:", 5, 1);
        gd.addNumericField("radiusZ:", 3, 1);
        gd.showDialog();
        if (gd.wasOKed()) {
            double radXY = gd.getNextNumber();
            double radZ = gd.getNextNumber();
            this.radiusXY.setValue(radXY);
            this.radiusZ.setValue(radZ);
            ImageHandler res = runPostFilter(0, (ImageInt) ImageHandler.wrap(imp), null);
            res.setMinAndMax(null);
            res.show(imp.getTitle() + "::close");
        }
    }

    @Override
    public String getHelp() {
        return "Morphological close using distance maps, optimized for large radius \nWorks on binary masks (no fusion between adjacent objects)";
    }
}
