package tango.plugin.filter;

import ij.IJ;
import mcib3d.image3d.ImageInt;
import tango.dataStructure.InputImages;
import tango.parameter.FileParameter;
import tango.parameter.Parameter;
import tango.parameter.TextParameter;

import java.io.File;

public class SaveNucleusLabelImage implements PostFilter {
    FileParameter fileParameter = new FileParameter("Directory: ", "dir", new File(IJ.getDirectory("home")));
    TextParameter textParameter = new TextParameter("Name", "name", "project");

    Parameter[] parameters = new Parameter[]{fileParameter, textParameter};


    @Override
    public ImageInt runPostFilter(int currentStructureIdx, ImageInt input, InputImages images) {
        File parent = fileParameter.getDir();
        File dir = new File(parent.getAbsoluteFile() + File.separator + textParameter.getText());
        if (!dir.exists()) dir.mkdir();
        //IJ.log("dir " + dir.getPath());
        //IJ.log("image title : " + images.imageName);
        String title = input.getTitle();
        input.setTitle(images.imageName);
        input.save(dir.getPath(), true);
        input.setTitle(title);

        return input.duplicate();
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public String getHelp() {
        return "Save the segmented image of the nucleus.";
    }

    @Override
    public void setVerbose(boolean verbose) {

    }

    @Override
    public void setMultithread(int nCPUs) {

    }
}
