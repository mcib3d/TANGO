package tango.plugin.filter;

import mcib3d.image3d.*;
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
public class MorphoMathFilters implements PostFilter {

    boolean debug;
    int nbCPUs = 1;
    static String[] methods = new String[]{"Binary Close", "Binary Open", "Binary Erode", "Binary Dilate", "Fill holes 2D", "Fill Holes 3D"};
    ChoiceParameter choice = new ChoiceParameter("Method:", "mergeMethod", methods, methods[0]);
    ConditionalParameter cond = new ConditionalParameter("MorphoMath Filter", choice);
    Parameter[] parameters = new Parameter[]{cond};

    BinaryClose close = new BinaryClose();
    BinaryOpen open = new BinaryOpen();
    BinaryErode erode = new BinaryErode();
    BinaryDilate dilate = new BinaryDilate();
    FillHoles2D fh2D = new FillHoles2D();
    FillHoles3D fh3D = new FillHoles3D();

    public MorphoMathFilters() {
        cond.setCondition(methods[0], close.getParameters());
        cond.setCondition(methods[1], open.getParameters());
        cond.setCondition(methods[2], erode.getParameters());
        cond.setCondition(methods[3], dilate.getParameters());
        cond.setCondition(methods[4], fh2D.getParameters());
        cond.setCondition(methods[5], fh3D.getParameters());
    }

    @Override
    public ImageInt runPostFilter(int currentStructureIdx, ImageInt in, InputImages images) {
        switch (choice.getSelectedIndex()) {
            case 0:
                close.setMultithread(nbCPUs);
                close.setVerbose(debug);
                return close.runPostFilter(currentStructureIdx, in, images);
            case 1:
                open.setMultithread(nbCPUs);
                open.setVerbose(debug);
                return open.runPostFilter(currentStructureIdx, in, images);
            case 2:
                erode.setMultithread(nbCPUs);
                erode.setVerbose(debug);
                return erode.runPostFilter(currentStructureIdx, in, images);
            case 3:
                dilate.setMultithread(nbCPUs);
                dilate.setVerbose(debug);
                return dilate.runPostFilter(currentStructureIdx, in, images);
            case 4:
                fh2D.setMultithread(nbCPUs);
                fh2D.setVerbose(debug);
                return fh2D.runPostFilter(currentStructureIdx, in, images);
            case 5:
                fh3D.setMultithread(nbCPUs);
                fh3D.setVerbose(debug);
                return fh3D.runPostFilter(currentStructureIdx, in, images);
            default:
                return in;
        }
    }

    @Override
    public void setVerbose(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void setMultithread(int nbCPUs) {
        this.nbCPUs = nbCPUs;
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public String getHelp() {
        switch (choice.getSelectedIndex()) {
            case 0:
                return close.getHelp();
            case 1:
                return open.getHelp();
            case 2:
                return erode.getHelp();
            case 3:
                return dilate.getHelp();
            case 4:
                return fh2D.getHelp();
            case 5:
                return fh3D.getHelp();
            default:
                return "Morphological Filters";
        }
    }

}
