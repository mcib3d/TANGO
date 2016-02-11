package tango.plugin.filter.mergeRegions;

import mcib3d.geom.Voxel3D;

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


// light voxel class
public class Vox3D implements java.lang.Comparable<Vox3D> {
        public int x, y, z, xy;
        public float value;

        public Vox3D(int x, int y, int z, int sizeX, float value) {
            this.x = x;
            this.y=y;
            this.z=z;
            this.xy = this.x+this.y * sizeX;
            this.value=value;
        }
        
        public Vox3D(int x, int y, int z, int xy) {
            this.x = x;
            this.y=y;
            this.z=z;
            this.xy = xy;
            this.value=Float.NaN;
        }
   
        @Override
        public int compareTo(Vox3D v) { //ascending order
            if (v.x==x && v.y==y && v.z==z) return 0;
            else if(value < v.value) return -1;
            else if(value > v.value) return 1;
            else return 0;
        }
        
        @Override 
        public boolean equals(Object o) {
            if (o instanceof Vox3D) {
                return x==((Vox3D)o).x && y==((Vox3D)o).y && z==((Vox3D)o).z;
            } return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + this.x;
            hash = 79 * hash + this.y;
            hash = 79 * hash + this.z;
            return hash;
        }



        @Override
        public String toString() {
            return "x:"+x + "y:"+ y+ " z:"+z+ " value:"+value;
        }
        
        public Voxel3D toVoxel3D(double value) {
            return new Voxel3D(x, y, z, value);
        }
        
        public Vox3D copy() {
            return new Vox3D(x, y, z, xy);
        }
    }
