package tango.spatialStatistics.util;
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
public class Distance implements Comparable<Distance> {
        public float distance;
        public int xy, z;
        public Distance(float distance, int xy, int z) {
            this.distance=distance;
            this.xy=xy;
            this.z=z;
        }
        @Override
        public int compareTo(Distance t) {
            if (distance<t.distance) return -1;
            else if (distance>t.distance) return 1;
            else return 0;
        }
    }
