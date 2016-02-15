package tango.gui.util;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
public class Tag {
    // TODO keys should be string ?? and add possibilty to retrieve tag names or values

    public static Map<Integer, Color> colors = Collections.unmodifiableMap(new HashMap<Integer, Color>() {

        {
            put(-1, Color.RED);
            put(0, Color.BLACK);
            put(1, new Color(166,206,227));
            put(2, new Color(31,120,180));
            put(3, new Color(178,223,138));
            put(4, new Color(51,160,44));
            put(5, new Color(251,154,153));
            put(6, new Color(227,26,28));
            put(7, new Color(253,191,111));
            put(8, new Color(255,127,0));
            put(9, new Color(202,178,214));
            put(10, new Color(106,61,154));
        }
    });
    public static Map<Integer, Color> oppositeColors = Collections.unmodifiableMap(new HashMap<Integer, Color>() {

        {
            put(-1, Color.BLACK);
            put(0, Color.WHITE);
            put(1, Color.BLACK);
            put(2, Color.BLACK);
            put(3, Color.BLACK);
            put(4, Color.BLACK);
            put(5, Color.BLACK);
            put(6, Color.BLACK);
            put(7, Color.BLACK);
            put(8, Color.BLACK);
            put(9, Color.BLACK);
            put(10, Color.BLACK);
        }
    });
    int tag;

    public Tag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public static int getNbTag() {
        return colors.size();
    }
}
