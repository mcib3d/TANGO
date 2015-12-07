/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.util;

import com.mongodb.BasicDBObject;
import ij.IJ;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author thomasb
 */
public class ProcessingChainsToText {
    
    BasicDBObject chain;
    JSONObject json;

    public ProcessingChainsToText(BasicDBObject chain) {
        this.chain = chain;
        json=new JSONObject(chain.toString());
    }
    
    public String getName(){
        if(json==null) return null;
        return json.getString("name");
    }
    
    public String getPreFilters(){
         if(json==null) return null;
         JSONArray pre=json.getJSONArray("preFilters");
         IJ.log("prefilters "+pre.length());
         JSONObject pre0=pre.getJSONObject(0);
         return pre0.toString();
    }
    
}
