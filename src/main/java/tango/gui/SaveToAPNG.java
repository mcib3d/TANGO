/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tango.gui;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import java.io.File;
import mcib3d.image3d.ImageHandler;
import tango.util.WriteImage;
import tango.util.Utils;
/**
 *
 * @author jean
 */
public class SaveToAPNG implements PlugIn {
    
    @Override
    public void run(String arg){
        ImagePlus ip = ij.IJ.getImage();
        if (ip==null) return;
        File f = Utils.chooseDir("choose export dir", null);
       
        String path = f.getAbsolutePath()+"/testAPNG.png";
        IJ.log("saving image:"+ip.getTitle()+ " to path:"+path);
        WriteImage.writeAPNGToFile(ImageHandler.wrap(ip), path);
        //new ImageExporter(ip).run(path);
    }
}
