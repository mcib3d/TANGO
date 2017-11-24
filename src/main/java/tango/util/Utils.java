
package tango.util;

import ij.IJ;
import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DFactory;
import mcib3d.geom.Object3DVoxels;
import mcib3d.image3d.ImageInt;

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
public class Utils {
    static Pattern p = Pattern.compile("[^a-z0-9_-]", Pattern.CASE_INSENSITIVE);
    public static Object3DVoxels[] getObjects3D(ImageInt im) {
        try {
            Object3DFactory oc = new Object3DFactory(im);
            return oc.getObjects(false);
        } catch (Exception e) { 
            return null;
        }
    }
    public static Color compareColor(Color oldC, Color newC) {
        if (oldC==null) oldC=Color.black;
        if (newC==Color.red) oldC=newC;
        else if (newC==Color.blue && oldC!=Color.red) oldC=newC;
        return oldC;
    }
    
    public static File chooseDir(String label, File curDir) {
        final JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (curDir != null) {
            fc.setCurrentDirectory(curDir);
        }
        if (label!=null) fc.setDialogTitle(label);
        int returnval = fc.showOpenDialog(IJ.getInstance());
        File file = null;
        if (returnval == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
        }
        return file;
    }
    
    public static String getPath(File f) {
        if (f==null) return null;
        String s;
        try{ 
            s=f.getCanonicalPath();
        } catch (Exception e) {
            s=f.getAbsolutePath();
        }
        //System.out.println("path:"+s.replace(" ", "\\ "));
        //s=s.replace(" ", "\\ ");
        //s = "\"" + s + "\"";
        return s; 
    }
    
    public static String join(String[] array, String separator) {
        String res = "";
        for (int i = 0; i<(array.length-1);i++) res+=array[i]+separator;
        if (array.length>=1) res+=array[array.length-1];
        return res;
    }
    
    public static String join(int[] array, String separator) {
        String res = "";
        for (int i = 0; i<(array.length-1);i++) res+=array[i]+separator;
        if (array.length>=1) res+=array[array.length-1];
        return res;
    }
    
    public static boolean contains(JComboBox jcb, Object o, boolean caseSensitive) {
        if (o==null) return false;
        if (caseSensitive || !(o instanceof String)) {
            for (int i = 0; i<jcb.getItemCount(); i++) if (jcb.getItemAt(i).equals(o)) return true;
        } else {
            String s = ((String)o).toLowerCase();
            for (int i = 0; i<jcb.getItemCount(); i++) if (((String)jcb.getItemAt(i)).toLowerCase().equals(s)) return true;
        }
        return false;
    }
    
    public static String getSelectedString(JComboBox jcb) {
        return (jcb.getSelectedIndex()==-1)?null : (String)jcb.getSelectedItem();
    }
    
    public static void addHorizontalScrollBar(JComboBox box) {
        if (box.getItemCount() == 0) return;
        Object comp = box.getUI().getAccessibleChild(box, 0);
        if (!(comp instanceof JPopupMenu)) {
            return;
        }
        JPopupMenu popup = (JPopupMenu) comp;
        int n = popup.getComponentCount();
        int i = 0;
        while (i<n) {
            if (popup.getComponent(i) instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) popup.getComponent(i);
                scrollPane.setHorizontalScrollBar(new JScrollBar(JScrollBar.HORIZONTAL));
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            }
            i++;
        }
    }
    
    public static boolean isValid(String s, boolean allowSpecialCharacters) {
        if (s==null || s.length()==0) return false;
        if (allowSpecialCharacters) return true;
        Matcher m = p.matcher(s);
        return !m.find();
    }
    
    public static int getIdxSameStructure(int idx1, int idx2, int size) {
        return idx1*size-(idx1*(idx1+1))/2+idx2-idx1-1;
    }
    
    public static int getIdx(int idx1, int idx2, int size2) {
        return idx1*size2 + idx2;
    }
    
    public static float getMedianIndex(float[] sorted, float value) {
        int idxMin, idxMax;
        idxMin = Arrays.binarySearch(sorted, value);
        //duplicate => median value
        idxMax=idxMin;
        while(idxMin>0 && sorted[idxMin-1]==sorted[idxMax]) idxMin--;
        while(idxMax<sorted.length-1 && sorted[idxMax+1]==sorted[idxMin]) idxMax++;
        if (idxMax!=idxMin) return (idxMin+idxMax)/2.0f;
        else return idxMin;
    }
    
    public static float getMedianIndex(double[] sorted, double value) {
        int idxMin, idxMax;
        idxMin = Arrays.binarySearch(sorted, value);
        //duplicate => median value
        idxMax=idxMin;
        while(idxMin>0 && sorted[idxMin-1]==sorted[idxMax]) idxMin--;
        while(idxMax<sorted.length-1 && sorted[idxMax+1]==sorted[idxMin]) idxMax++;
        if (idxMax!=idxMin) return (idxMin+idxMax)/2.0f;
        else return idxMin;
    }
    
    public static int getLeftIndex(float[] sorted, float value) {
        int idx = Arrays.binarySearch(sorted, value);
        int idxMin=idx;
        while(idxMin>0 && sorted[idxMin-1]==sorted[idx]) idxMin--;
        return idxMin;
    }
    
    public static int getRightIndex(float[] sorted, float value) {
        int idx = Arrays.binarySearch(sorted, value);
        int idxMax = idx;
        while(idxMax<sorted.length-1 && sorted[idxMax+1]==sorted[idx]) idxMax++;
        return idxMax;
    }
    
    public static int factorial(int n) {
        int fact = 1; // this  will be the result
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }
    
    
}
