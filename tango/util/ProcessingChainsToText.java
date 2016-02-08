/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.util;

import com.mongodb.BasicDBObject;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author thomasb
 */
public class ProcessingChainsToText {
    
    //BasicDBObject chain = null;
    JSONObject json = null;
    

    public ProcessingChainsToText(BasicDBObject chain) {
        if (chain != null) {
            json = new JSONObject(chain.toString());
        }
    }

    public String getName() {
        if (json == null) {
            return null;
        }
        return json.getString("name");
    }

    public String getPreFilters() {
        String res = "";
        if (json == null) {
            return null;
        }
        JSONArray pre = json.getJSONArray("preFilters");
        int nb = pre.length();
        //res = res.concat("prefilters " + pre.length());
        for (int pf = 0; pf < nb; pf++) {
            JSONObject pre0 = pre.getJSONObject(pf);
            res = res.concat("* Prefilter " + (pf + 1) + "\n");
            res = res.concat(getString(pre0));
        }
        return res;
    }

    public String getPostFilters() {
        String res = "";
        if (json == null) {
            return null;
        }
        JSONArray pre = json.getJSONArray("postFilters");
        int nb = pre.length();
        //res = res.concat("prefilters " + pre.length());
        for (int pf = 0; pf < nb; pf++) {
            JSONObject pre0 = pre.getJSONObject(pf);
            res = res.concat("* PostFilter " + (pf + 1) + "\n");
            res = res.concat(getString(pre0));
        }
        return res;
    }

    private String getString(JSONObject js) {
        if (js == null) {
            return "";
        }
        String res = "";
        for (String key : (Set<String>)js.keySet()) {
            Object O = js.get(key);
            if (O instanceof JSONObject) {
                res = res.concat(key + "\n");
                res = res.concat(getString((JSONObject) O));
            } else if (O instanceof JSONArray) {
                res = res.concat(key + "\n");
                int nb = ((JSONArray) O).length();
                for (int pf = 0; pf < nb; pf++) {
                    res = res.concat("* " + key + " " + (pf + 1) + "\n");
                    JSONObject pre0 = ((JSONArray) O).getJSONObject(pf);
                    res = res.concat(getString(pre0));
                }
            } else {
                res = res.concat("\t" + key + " : " + js.get(key) + "\n");
            }
        }

        return res;
    }

    public String getSegmentation() {
        if (json == null) {
            return null;
        }

        JSONObject seg = json.getJSONObject("segmentation");

        return getString(seg);
    }
    
}
