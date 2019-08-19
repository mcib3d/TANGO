package tango.plugin.segmenter;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.ZProjector;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.AutoThresholder;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.ImageShort;
import tango.dataStructure.InputImages;
import tango.parameter.Parameter;
import tango.plugin.thresholder.AutoThreshold;

public class DeepSeg_ implements NucleusSegmenter {
    //ThresholdParameter thresholdParameter = new ThresholdParameter("threshold","threshold","TRIANGLE");
    Parameter[] parameters = new Parameter[]{};

    @Override
    public ImageInt runNucleus(int currentStructureIdx, ImageHandler input, InputImages rawImages) {
        // do projection
        ZProjector zProjector = new ZProjector();
        zProjector.setMethod(ZProjector.MAX_METHOD);
        zProjector.setStartSlice(1);
        zProjector.setStopSlice(input.sizeZ);
        zProjector.setImage(input.getImagePlus());
        zProjector.doProjection();
        ImagePlus plus = zProjector.getProjection();
        // find threshold
        int threshold = (int) AutoThreshold.run(ImageInt.wrap(plus), null, AutoThresholder.Method.Triangle);
        plus.getProcessor().threshold(threshold);
        ImagePlus bin2 = duplicateImage(plus.getProcessor());
        // fill holes
        ij.plugin.filter.Binary binary = new Binary();
        binary.setup("fill", bin2);
        binary.run(bin2.getProcessor());
        // watershed IJ = separate
        ij.plugin.filter.EDM edm = new EDM();
        edm.setup("watershed", bin2);
        edm.toWatershed(bin2.getProcessor());
        // count mask
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_ROI_MASKS, ParticleAnalyzer.AREA, null, 0, 1000000, 0, 1);
        particleAnalyzer.setHideOutputImage(true);
        particleAnalyzer.analyze(bin2);
        ImagePlus seg2D = particleAnalyzer.getOutputImage();
        ImageInt seg3D = expand3D(ImageInt.wrap(seg2D), input.sizeZ);
        // perform deep segmentation
        DeepSeg deepSeg = new DeepSeg((ImageInt) input, seg3D);

        return deepSeg.getSeg();
    }

    @Override
    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public String getHelp() {
        return "Segmentation based on 2D projections";
    }

    @Override
    public void setVerbose(boolean verbose) {

    }

    @Override
    public void setMultithread(int nCPUs) {

    }

    private ImagePlus duplicateImage(ImageProcessor iProcessor) {
        int w = iProcessor.getWidth();
        int h = iProcessor.getHeight();
        ImagePlus iPlus = NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor = iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0, 0, Blitter.COPY);
        return iPlus;
    }

    private ImageInt expand3D(ImageInt imageInt, int nbZ) {
        ImageInt expand = new ImageShort("expand", imageInt.sizeX, imageInt.sizeY, nbZ);
        for (int z = 0; z < nbZ; z++)
            expand.insert(imageInt, 0, 0, z, false);

        return expand;
    }
}
