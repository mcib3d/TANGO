package tango.plugin.segmenter;

import ij.ImagePlus;
import ij.ImageStack;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.Segment3DSpots;
import mcib3d.image3d.processing.FastFilters3D;
import tango.dataStructure.InputImages;
import tango.parameter.*;

/**
 * Description of the Class
 *
 * @author thomas
 * @created 16 avril 2006
 */
public class SeedSpots_Plus implements SpotSegmenter {

    // seeds
    ImageHandler seed3DImage;
    ImageHandler filteredSeed;
    // spots
    ImageHandler spot3DImage;
    // Segmentation algo
    Segment3DSpots seg;
    // segResults
    ImageInt segImage = null;
    int local_method = 0;
    int spot_method = 0;
    int global_background = 15;
    int local_background = 65;
    double local_diff = 100;
    // local mean
    float rad0 = 2;
    float rad1 = 4;
    float rad2 = 6;
    double we = 0.5;
    // gauss_fit
    int radmax = 10;
    double sdpc = 1.0;
    private boolean watershed = true;
    private int radiusSeeds = 2;

    String[] local_methods = {"Constant", "Local Mean", "Local Difference", "Gaussian fit"};
    String[] spot_methods = {"Classical", "Maximum", "Block"};

    // DB
    PreFilterSequenceParameter preFilters = new PreFilterSequenceParameter("Pre-Filters for seeds", "preFilters");
    IntParameter DB_radseeds = new IntParameter("Radius for seeds (pix)", "radSeeds", radiusSeeds);
    ThresholdParameter DB_global_background = new ThresholdParameter("Global seeds threshold:", "thldHigh", "Value");
    // test with gaussian method
    ChoiceParameter DB_algos = new ChoiceParameter("Choose algo : ", "algo", spot_methods, null);
    ChoiceParameter DB_lcth = new ChoiceParameter("Choose local threshold : ", "lc", local_methods, null);
    ConditionalParameter cond = new ConditionalParameter(DB_lcth);
    ThresholdParameter DB_local_background = new ThresholdParameter("Local threshold:", "thlc", "Percentage Of Bright Pixels");
    DoubleParameter diffP = new DoubleParameter("Diff value", "diff", new Double(local_diff), Parameter.nfDEC2);
    LabelParameter DB_Label_Gaussian = new LabelParameter("Gaussian Fit");
    IntParameter DB_radmax = new IntParameter("Radius for gaussian (pix)", "radGaussianFit", radmax);
    DoubleParameter DB_sdpc = new DoubleParameter("Sigma cutoff", "sigmaGaussianFit", new Double(sdpc), Parameter.nfDEC2);
    LabelParameter DB_Label_Mean = new LabelParameter("Local Means");
    SpinnerParameter DB_rad0 = new SpinnerParameter("Radius 0", "rad0", 0, 10, 2);
    SpinnerParameter DB_rad1 = new SpinnerParameter("Radius 1", "rad1", 0, 10, 4);
    SpinnerParameter DB_rad2 = new SpinnerParameter("Radius 2", "rad1", 0, 10, 6);
    Parameter[] parameters = new Parameter[]{preFilters, DB_radseeds, DB_global_background, DB_algos, cond};
    private boolean debug = true;
    private int nbCPUs = 1;

    public SeedSpots_Plus() {
        cond.setCondition(local_methods[0], new Parameter[]{DB_local_background});
        cond.setCondition(local_methods[1], new Parameter[]{DB_rad0, DB_rad1, DB_rad2});
        cond.setCondition(local_methods[2], new Parameter[]{diffP});
        cond.setCondition(local_methods[3], new Parameter[]{DB_radmax, DB_sdpc});
    }

    private void computeSeeds(int currentStructureIdx, ImageHandler input, InputImages images) {
        filteredSeed = preFilters.runPreFilterSequence(currentStructureIdx, input.duplicate(), images, nbCPUs, false);
        seed3DImage = FastFilters3D.filterImage(filteredSeed, FastFilters3D.MAXLOCAL, (float) radiusSeeds, (float) radiusSeeds, (float) radiusSeeds, 0, false);
    }

    private void Segmentation() {
        seg = new Segment3DSpots(spot3DImage, seed3DImage);
        seg.show = debug;
        // set parameter
        seg.setSeedsThreshold(global_background);
        seg.setWatershed(watershed);
        switch (local_method) {
            case 0:
                seg.setMethodLocal(Segment3DSpots.LOCAL_CONSTANT);
                seg.setLocalThreshold(local_background);
                break;
            case 1:
                seg.setMethodLocal(Segment3DSpots.LOCAL_MEAN);
                seg.setRadiusLocalMean(rad0, rad1, rad2, we);
                break;
            case 2:
                seg.setMethodLocal(Segment3DSpots.LOCAL_DIFF);
                seg.setLocalDiff((int) local_diff);
                break;
            case 3:
                seg.setMethodLocal(Segment3DSpots.LOCAL_GAUSS);
                seg.setGaussPc(sdpc);
                seg.setGaussMaxr(radmax);
                break;
        }
        switch (spot_method) {
            case 0:
                seg.setMethodSeg(Segment3DSpots.SEG_CLASSICAL);
                break;
            case 1:
                seg.setMethodSeg(Segment3DSpots.SEG_MAX);
                break;
            case 2:
                seg.setMethodSeg(Segment3DSpots.SEG_BLOCK);
                break;
        }
        seg.segmentAll();
        // output 
        segImage = (ImageInt) seg.getLabelImage();
    }

    @Override
    public Parameter[] getParameters() {
        preFilters.setHelp("Prefilters to detect the seeds, note that local maxima will be used on the filtered image.", true);
        preFilters.setHelp("Prefilters to detect the seeds, note that local maxima will be used on the filtered image. "
                + "A robust seed detector is the Image Features Hessian or the symmetry filter.", false);
        DB_radseeds.setHelp("Radius to compute max local for seeds", true);
        DB_global_background.setHelp("Threshold for seeds", true);
        DB_radmax.setHelp("Radius max to compute values for gaussian fitting", true);
        DB_sdpc.setHelp("The multiplication factor with sigma value to compute the threshold based on gaussian fitting", true);
        DB_algos.setHelp("Algorithms for spot segmentation", true);
        DB_algos.setHelp("Three segmentation algorithms:\nClassical, segment all voxels with value greater than local threshold"
                + "\nMaximum, stop segmentation if voxel value greater than seed"
                + "\nBlock, stop segmentation process if one voxel grreater that seed", false);
        return parameters;
    }

    @Override
    public void setVerbose(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void setMultithread(int nbCPUs) {
        this.nbCPUs = nbCPUs;
    }

    @Override
    public ImageInt runSpot(int currentStructureIdx, ImageHandler input, InputImages images) {
        spot3DImage = input;

        // get info from parameters        
        local_background = DB_local_background.getThreshold(input, images, nbCPUs, debug).intValue();
        rad0 = DB_rad0.getValue();
        rad1 = DB_rad1.getValue();
        rad2 = DB_rad2.getValue();
        local_diff = diffP.getDoubleValue(local_diff);
        radmax = DB_radmax.getIntValue(radmax);
        sdpc = DB_sdpc.getDoubleValue(sdpc);

        // seeds
        radiusSeeds = DB_radseeds.getIntValue(radiusSeeds);
        computeSeeds(currentStructureIdx, input, images);
        global_background = (DB_global_background.getThreshold(filteredSeed, images, nbCPUs, debug)).intValue();
        if (debug) {
            filteredSeed.show("Seeds");
        }

        watershed = true;
        local_method = DB_lcth.getSelectedIndex();
        spot_method = DB_algos.getSelectedIndex();
        Segmentation();

        return segImage;
    }

    @Override
    public ImageFloat getProbabilityMap() {
        return null;
    }

    @Override
    public String getHelp() {
        return "3D Spot segmentation";
    }
}
