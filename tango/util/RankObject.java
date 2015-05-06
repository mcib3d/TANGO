/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.util;

/**
 *
 * @author jollion
 */
public class RankObject implements Comparable<RankObject> {
    float value;
    int rank;
    public RankObject(float value, int oldRank) {
        this.value=value;
        this.rank=oldRank;
    }
    @Override
    public int compareTo(RankObject o) { //ascending order
        if(value < o.value) return -1;
        else if(value > o.value) return 1;
        else return 0;
    }
}
