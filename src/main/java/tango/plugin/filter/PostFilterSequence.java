package tango.plugin.filter;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import mcib3d.image3d.ImageInt;
import tango.dataStructure.InputImages;
import tango.parameter.Parameter;
import tango.plugin.PluginFactory;

import java.util.ArrayList;

/**
 * *
 * /**
 * Copyright (C) 2012 Jean Ollion
 * <p>
 * <p>
 * <p>
 * This file is part of tango
 * <p>
 * tango is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Jean Ollion
 */
public class PostFilterSequence {
    ArrayList<PostFilter> filters;

    // TB
    public String imageName;

    public PostFilterSequence(BasicDBObject type, int nbCPUs, boolean verbose) {
        if (type == null || !type.containsField("postFilters")) return;
        BasicDBList prefilters = (BasicDBList) type.get("postFilters");
        filters = new ArrayList<>(prefilters.size());
        for (int i = 0; i < prefilters.size(); i++) {
            Object o = prefilters.get(i);
            if (o != null) {
                BasicDBObject data = (BasicDBObject) o;
                if (!data.getBoolean("isActivated", true)) {
                    filters.add(new DummyPostFilter());
                    continue;
                }
                PostFilter f = PluginFactory.getPostFilter(data.getString("method"));
                if (f != null) {
                    Parameter[] parameters = f.getParameters();
                    for (Parameter p : parameters) p.dbGet(data);
                    filters.add(f);
                    f.setMultithread(nbCPUs);
                    f.setVerbose(verbose);
                }
            }
        }
    }

    public ImageInt run(int currentStructureIdx, ImageInt in, InputImages images) {
        ImageInt currentImage = in;
        if (isEmpty()) return in;
        images.imageName = this.imageName;
        while (!filters.isEmpty()) {
            currentImage = filters.remove(0).runPostFilter(currentStructureIdx, currentImage, images);
            currentImage.setScale(in);
            currentImage.setOffset(in);
        }
        currentImage.setTitle(in.getTitle() + "::postFiltered");

        return currentImage;
    }

    public void test(int currentStructureIdx, ImageInt in, InputImages images, int step, boolean onlyStep) {
        ImageInt currentImage = in;
        if (isEmpty() || step > filters.size()) return;
        int idx = 0;
        while (!filters.isEmpty()) {
            PostFilter f = filters.remove(0);
            if (step == idx) {
                currentImage.set332RGBLut();
                if (!onlyStep) currentImage.showDuplicate("Image Before selected Step");
                images.setVerbose(true);
                f.setVerbose(true);
            } else {
                if (onlyStep) {
                    idx++;
                    continue;
                }
                images.setVerbose(false);
                f.setVerbose(false);
            }
            currentImage = f.runPostFilter(currentStructureIdx, currentImage, images);
            currentImage.setScale(in);
            currentImage.setOffset(in);
            if (step == idx) {
                currentImage.set332RGBLut();
                currentImage.showDuplicate("Image After selected Step");
                images.setVerbose(false);
                return;
            }
            idx++;
        }
    }


    public boolean isEmpty() {
        return (filters == null || filters.isEmpty());
    }
}