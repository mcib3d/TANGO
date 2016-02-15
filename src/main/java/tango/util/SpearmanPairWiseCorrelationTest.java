/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 *
 * @author jollion
 */


public class SpearmanPairWiseCorrelationTest {
    
    public static double[] performTest(float[] s1, float[] s2, int tail, int sampleSize) { // tail = 0 > two sided, tail<0 less, tail>0 greater
        float[] r1= getRanks(s1);
        ArrayList<Float> r2 = new ArrayList<Float>(s1.length);
        float[] r2A = getRanks(s2);
        for (float f : r2A) r2.add(f);
        double obsRho = computeRho(r1, r2);
        double[] allRhos = new double[sampleSize];
        allRhos[0]=obsRho;
        int idx=1;
        while (idx <allRhos.length) {
            Collections.shuffle(r2);
            allRhos[idx++] = computeRho(r1, r2);
        }
        int n=0; 
        int nEgual = 0; // gestion des valeures dupliquées
        if (tail==0) {
            for (double r : allRhos) {
                if (obsRho<Math.abs(r)) n++;
                else if (obsRho==r) nEgual++;
            }
        } else if (tail>0) {
            for (double r : allRhos) {
                if (obsRho<r) n++;
                else if (obsRho==r) nEgual++;
            }
        } else {
            for (double r : allRhos) {
                if (obsRho>r) n++;
                else if (obsRho==r) nEgual++;
            }
        }
        return new double[] {obsRho, ( (double)n + (double)nEgual/2.0d ) / (double)allRhos.length};
    }
    
    private static double computeRho(float[] r1, ArrayList<Float> r2) {
        double d2 = 0;
        for (int i = 0; i<r1.length;i++) d2+=Math.pow(r1[i] - r2.get(i), 2);
        return computeRho(d2, r1.length);
    }
    
    public static double computeD2(float[] r1, float[] r2) {
        double d2 = 0;
        for (int i = 0; i<r1.length;i++) d2+=Math.pow(r1[i] - r2[i], 2);
        return d2;
    }
    
    public static double computeRho(double d2, int n) {
        return 1 - 6 * d2 / (float)(n *  (n * n - 1)); 
    }
    
    public static double computeRho(float[] s1, float[] s2) {
        float[] r1= getRanks(s1);
        float[] r2= getRanks(s2);
        return computeRho(computeD2(r1, r2), r1.length);
    }
    
    private static float[] getRanks(float[] s) {
        RankObject[] ro = new RankObject[s.length];
        for (int i = 0; i<s.length; i++) ro[i] = new RankObject(s[i], i);
        Arrays.sort(ro);
        float[] res = new float[s.length]; 
        for (int i = 0; i<s.length; i++) {
            // gestion des duplicates
            if (i<s.length-1 && ro[i].value==ro[i+1].value) {
                int j = i+1;
                while(j<s.length-1 && ro[j+1].value==ro[i].value) j++;
                float value = (float)(i+j)/2f;
                for (int k = i; k<=j; k++) res[ro[k].rank] = value;
            } else res[ro[i].rank] = i;
        } 
        return res;
    }
    
}
