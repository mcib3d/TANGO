package tango.gui;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageInt;
import mcib3d.image3d.processing.BinaryMorpho;
import mcib3d.utils.ThreadRunner;
import mcib3d.utils.exceptionPrinter;
import org.bson.types.ObjectId;
import sc.fiji.i5d.Image5D;
import tango.dataStructure.*;
import tango.gui.util.*;
import tango.helper.HelpManager;
import tango.plugin.measurement.MeasurementKey;
import tango.plugin.measurement.MeasurementObject;
import tango.plugin.sampler.Sampler;
import tango.spatialStatistics.StochasticProcess.RandomPoint3DGenerator;
import tango.spatialStatistics.StochasticProcess.RandomPoint3DGeneratorUniform;
import tango.util.ImageUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.*;

import static tango.gui.Connector.recordWindowsPosition;

//import i5d.Image5D;

/**
 * *
 * /**
 * Copyright (C) 2012 Jean Ollion
 * <p>
 * <p>
 * <p>
 * This file is part of tango
 * <p>
 * tango is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Jean Ollion
 */
public class CellManager implements ListSelectionListener, AdjustmentListener, MouseWheelListener {

    protected SelectionManager selectionManager;
    protected boolean showFieldRoi;
    Core core;
    int listX = 290;
    int[] structureSelection;
    JProgressBar progress;
    ThreadRunner currentRunner;
    ImageWindowPosition windowPos;
    HashMap<Integer, Roi> currentROIs;
    ImagePlus currentImage;
    private javax.swing.JList list, listChannel;
    private DefaultListModel listModel, listChannelModel;
    private ListSelectionModel listSelectionModel, listChannelSelectionModel;
    private JPanel container;
    private JToggleButton showObjects;
    private JComboBox tags;
    private boolean selectingTag, selectingCell, populatingCells, populatingChannels;
    private Experiment xp;
    private Cell currentCell;
    private ObjectManager objectManager;
    private CellManagerLayout layout;

    public CellManager(Core core, JPanel container) {
        try {
            this.core = core;
            this.container = container;
            layout = new CellManagerLayout(this);
            this.showObjects = layout.viewObjects;
            tags = layout.getTagChoice();
            for (int i = 0; i < Tag.getNbTag(); i++) {
                tags.addItem(i - 1);
            }
            tags.setRenderer(new TagCellRenderer());

            this.listModel = new DefaultListModel();
            this.list = layout.cellList;
            list.setModel(listModel);
            this.list.setCellRenderer(new LCRenderer());
            this.list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            this.list.setLayoutOrientation(JList.VERTICAL);
            listSelectionModel = list.getSelectionModel();
            listSelectionModel.addListSelectionListener(this);

            this.listChannelModel = new DefaultListModel();
            this.listChannel = layout.structureList;
            listChannel.setModel(listChannelModel);
            listChannelSelectionModel = listChannel.getSelectionModel();
            listChannelSelectionModel.addListSelectionListener(this);
            this.listChannel.setCellRenderer(new LCRenderer());
            this.listChannel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            this.listChannel.setLayoutOrientation(JList.VERTICAL);
            selectingTag = false;
            selectingCell = false;
            objectManager = new ObjectManager(core, container);
            selectionManager = new SelectionManager(this);
            objectManager.setShowSelection(selectionManager.selectObjects);
            if (Core.helper != null) registerComponents(Core.helper.getHelpManager());
        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
    }

    public static void measureCells(final Cell[] cells, final boolean override) {
        for (Cell c : cells) c.close();
        if (Core.GUIMode) Core.getProgressor().setAction("Measuring cells");
        if (Core.GUIMode) Core.getProgressor().resetProgress(cells.length);
        final ThreadRunner tr = new ThreadRunner(0, cells.length, Core.getMaxCellMeasurement());
        for (int i = 0; i < tr.threads.length; i++) {
            tr.threads[i] = new Thread(
                    new Runnable() {
                        public void run() {
                            for (int cidx = tr.ai.getAndIncrement(); cidx < tr.end; cidx = tr.ai.getAndIncrement()) {
                                try {
                                    System.out.println("Measurements: " + (cidx + 1) + "/" + tr.end + " nbThreads:" + tr.threads.length);
                                    if (cells[cidx] != null && cells[cidx].getTag().getTag() >= 0) {
                                        cells[cidx].setVerbose(false);
                                        cells[cidx].setNbCPUs(1); // TODO more CPUs if less cells at the same time
                                        cells[cidx].mesure(override);
                                        cells[cidx].close();
                                    }
                                    if (Core.GUIMode) Core.getProgressor().incrementStep();
                                    System.out.println("Measurements: " + (cidx + 1) + "/" + tr.end + " done.");
                                    //close field images if no cell is open anymore
                                    Field f = cells[cidx].getField();
                                    if (!f.hasOpenedCellImages()) {
                                        f.closeInputImages();
                                        f.closeOutputImages();
                                    }
                                } catch (Exception e) {
                                    exceptionPrinter.print(e, "measure:" + cidx + " " + ((cells[cidx] != null) ? cells[cidx].getFieldName() + "::" + cells[cidx].getName() : "cell==null"), Core.GUIMode);
                                }
                            }
                        }
                    }
            );
        }
        tr.startAndJoin();
    }

    public static void processCells(final Cell[] cells, final boolean[] processChannels) {
        for (Cell c : cells) c.close(); //hide is sufficient
        if (Core.GUIMode) {
            Core.getProgressor().setAction("Processing cells");
            System.out.println("Processing cells");
        }
        if (Core.GUIMode) {
            Core.getProgressor().resetProgress(cells.length);
            System.out.println("nb cells:" + cells.length);
        }
        final ThreadRunner tr = new ThreadRunner(0, cells.length, Core.getMaxCellProcess());
        for (int i = 0; i < tr.threads.length; i++) {
            tr.threads[i] = new Thread(
                    new Runnable() {
                        public void run() {
                            for (int cidx = tr.ai.getAndIncrement(); cidx < tr.end; cidx = tr.ai.getAndIncrement()) {
                                //if (progressMonitor!=null && progressMonitor.isCanceled()) return;
                                try {
                                    System.out.println("Structure Process: " + (cidx + 1) + "/" + tr.end + " nbThreads:" + tr.threads.length);
                                    if (cells[cidx] != null && cells[cidx].getTag().getTag() >= 0) {
                                        cells[cidx].setVerbose(false);
                                        cells[cidx].setNbCPUs(1); // TODO more CPUs if less cells at the same time
                                        cells[cidx].process(processChannels);
                                        cells[cidx].close();
                                    }
                                    if (Core.GUIMode) Core.getProgressor().incrementStep();
                                    System.out.println("Structure Process: " + (cidx + 1) + "/" + tr.end + " done.");
                                    Field f = cells[cidx].getField();
                                    if (!f.hasOpenedCellImages()) {
                                        f.closeInputImages();
                                        f.closeOutputImages();
                                    }
                                } catch (Exception e) {
                                    exceptionPrinter.print(e, "process cell:" + cidx + " " + ((cells[cidx] != null) ? cells[cidx].getFieldName() + "::" + cells[cidx].getName() : "cell==null"), Core.GUIMode);
                                }
                            }
                        }
                    }
            );
        }
        tr.startAndJoin();
    }

    public void registerComponents(HelpManager hm) {
        objectManager.registerComponents(hm);
        layout.registerComponents(hm);
    }

    public void setXP(Experiment xp) {
        this.xp = xp;
        //ChannelParameter.setStructures(core.getChannels(false));
        //listChannel.setVisibleRowCount(xp.getNBStructures());
        removeCells();
        setSortKeys();
        layout.setStructures(xp.getStructureNames(false));
        if (selectionManager != null) selectionManager.update();
        this.windowPos = new ImageWindowPosition(xp.getNBFiles(), xp.getNBStructures(true));
        //for (String c : core.getChannels(false)) listChannelModel.addElement(c);
    }

    public void updateXP() {
        this.setSortKeys();
        String structure = layout.getThumbnailStructure();
        layout.setStructures(xp.getStructureNames(false));
        layout.setStructure(structure);
    }

    private void setSortKeys() {
        MeasurementKey mkey = new MeasurementKey(new int[]{0}, MeasurementObject.Number);
        if (Core.getExperiment() != null) {
            ArrayList<String> keys = Core.getExperiment().getKeys().get(mkey);
            layout.setKeys(keys);
        }
    }

    private void sort(String key, ArrayList<Cell> cells) {
        if (key.equals("idx")) return;
        ij.IJ.log("sort by:" + key);
        Cell.setAscendingOrger(layout.getAscendingOrder());
        boolean notFound = false;
        HashMap<ObjectId, BasicDBObject> objects = null;
        if (!key.equals("tag"))
            objects = Core.getExperiment().getConnector().getNucleiObjects(Core.getExperiment().getId());
        for (Cell c : cells) {
            if (key.equals("tag")) c.setValue(c.getTag().getTag());
            else {
                //HashMap<Integer, BasicDBObject> objects = Core.getExperiment().getConnector().getObjects(c.getId(), 0);
                //ij.IJ.log("nb of objects:"+objects.size());
                //BasicDBObject dbo = objects.get(1);
                BasicDBObject dbo = objects.get(c.getId());
                if (dbo != null) {
                    if (dbo.containsField(key)) c.setValue(dbo.getDouble(key));
                    else {
                        c.setValue(-1);
                        notFound = true;
                    }
                }
            }

        }
        if (notFound) ij.IJ.log("Warning measurement: " + key + " not found for one or several nuclei");
        Collections.sort(cells);
    }

    private void moveContainer() {
        if (core != null) {
            JScrollBar bar = core.getScrollPane().getHorizontalScrollBar();
            bar.setValue(bar.getMaximum());
            //bar = core.getScrollPane().getVerticalScrollBar();
            //bar.setValue(bar.getMinimum());
        }
    }

    public void populateCells() {
        FieldManager fm = core.getFieldManager();
        if (fm != null) populateCells(fm.getSelectedCells());
    }

    public void addCell(Cell c) {
        try {
            this.populatingCells = true;
            this.listModel.addElement(c);
            this.populatingCells = false;
        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
    }

    protected HashMap<Integer, ArrayList<Integer>> getSelectedObjects() {
        return objectManager.getSplitSelectionIndexes();
    }

    protected Cell getSelectedCell() {
        Object o = list.getSelectedValue();
        if (o == null) return null;
        else return (Cell) o;
    }

    public void populateCells(ArrayList<Cell> cells) {
        try {
            List selection = list.getSelectedValuesList();
            HashSet<Cell> selectedIds = new HashSet<Cell>(selection.size());
            for (Object o : selection) selectedIds.add(((Cell) o));
            int nb = listModel.getSize();
            this.populatingCells = true;
            this.listModel.removeAllElements();
            removeChannels();
            Set<String> nuclei = null;
            if (selectionManager != null) {
                for (Cell c : cells) c.setInSelection(false);
                Selection sel = selectionManager.getSelection();
                if (sel != null) {
                    nuclei = sel.getNuclei();
                    if (selectionManager.isShowOnly()) {
                        cells.retainAll(nuclei);
                        for (Cell c : cells) c.setInSelection(true);
                    }
                }
            }
            sort(layout.getSortKey(), cells);
            for (Cell c : cells) this.listModel.addElement(c);
            // set selected cells
            if (selectedIds.size() > 0) {
                ArrayList<Integer> selectedIndices = new ArrayList<Integer>(selectedIds.size());
                for (int i = 0; i < cells.size(); i++) if (selectedIds.contains(cells.get(i))) selectedIndices.add(i);
                int[] sel = new int[selectedIndices.size()];
                int j = 0;
                for (int i : selectedIndices) sel[j++] = i;
                list.setSelectedIndices(sel);
                populateStructures();
            }
            // set selection highlight
            if (nuclei != null && !selectionManager.isShowOnly()) {
                cells.retainAll(nuclei);
                for (Cell c : cells) c.setInSelection(true);
            }
            /*if (listModel.getSize()==nb) {
                list.setSelectedIndices(selection);
                if (selection.length==1) populateStructures();
            }
            *
            */

            this.populatingCells = false;
        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
    }

    protected void toggleShowOnlySelection() {
        // get selection


    }

    private void setTag(int tag) {
        List cells = this.list.getSelectedValuesList();
        for (Object o : cells) {
            ((Cell) o).setTag(tag);
        }
    }

    private void removeCells() {
        this.populatingCells = true;
        if (currentCell != null) {
            currentCell.close();
            currentCell = null;
        }
        try {
            for (Object o : this.list.getSelectedValuesList()) {
                listModel.removeElement(o);
            }
        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
        removeChannels();
        this.populatingCells = false;
    }

    public void removeChannels() {
        this.populatingChannels = true;
        try {
            listChannelModel.removeAllElements();
        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
        this.populatingChannels = false;
    }

    private void deleteSelectedCells() {
        populatingCells = true;
        try {
            for (Object o : this.list.getSelectedValuesList()) {
                listModel.removeElement(o);
                ((Cell) o).delete();
            }
        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
        populatingCells = false;
    }

    public JPanel getPanel() {
        return this.layout;
    }

    public void showCell5d(int idx) {
        try {
            Cell c = (Cell) listModel.get(idx);
            Image5D raw = c.getImage5D(true);

            raw.show();
            ImageUtils.zoom(raw, core.getConnector().magnitude.getDoubleValue(2));

            if (Connector.openSegmentedImages.isSelected()) {
                Image5D seg = c.getImage5D(false);
                seg.show();
                ImageUtils.zoom(seg, core.getConnector().magnitude.getDoubleValue(2));
            }

        } catch (Exception e) {
            exceptionPrinter.print(e, "show channel", Core.GUIMode);
        }
    }

    public void openStructures() {
        if (listChannel.getSelectedIndex() == -1) {
            // opens thumbnail structure
            AbstractStructure ass = (AbstractStructure) listChannelModel.getElementAt(Cell.structureThumbnail);
            openStructure(ass);
        } else {
            for (Object o : this.listChannel.getSelectedValuesList()) {
                AbstractStructure ass = (AbstractStructure) o;
                openStructure(ass);
            }
        }
    }

    private void openStructure(AbstractStructure ass) {
        try {
            if (Connector.openSegmentedImages.isSelected()) {
                ImageHandler S = ass.getSegmented();
                if (S != null) {
                    S.show();
                    ImageUtils.zoom(S.getImagePlus(), core.getConnector().magnitude.getDoubleValue(2));
                }
                if (ass instanceof Structure) {
                    ImageHandler sp = ((Structure) ass).getProbabilityMap();
                    if (sp != null) {
                        sp.show();
                        ImageUtils.zoom(sp.getImagePlus(), core.getConnector().magnitude.getDoubleValue(2));
                    }
                }
            }
            if (!(ass instanceof VirtualStructure)) {
                ImageHandler raw = ass.getRaw();
                if (raw != null) {
                    raw.show();
                    ImageUtils.zoom(raw.getImagePlus(), core.getConnector().magnitude.getDoubleValue(2));
                }
            }
            if (recordWindowsPosition.isSelected()) windowPos.setWindowPosition(ass);
        } catch (Exception e) {
            exceptionPrinter.print(e, "show channel", Core.GUIMode);
        }
    }

    public void show3DCell() {
        try {
            Cell c = (Cell) list.getSelectedValue();
            c.show3D();
        } catch (Exception e) {
            exceptionPrinter.print(e, "show 3D", Core.GUIMode);
        }
    }

    public void closeCell(int idx) {
        try {
            ((Cell) listModel.get(idx)).close();
        } catch (Exception e) {
            exceptionPrinter.print(e, "close cell", Core.GUIMode);
        }
    }

    public void run(final boolean process, final boolean measure, final boolean override) {
        Core.debug = false;
        if (!process && !measure || list.getSelectedIndex() < 0) return;
        if (!layout.run.isEnabled()) {
            IJ.error("retry run");
            return;
        }
        core.toggleIsRunning(true);
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (process) {
                    process();
                }
                if (measure) {
                    mesure(override);
                }
                //core.toggleIsRunning(false);
                Thread t2 = new Thread(new Runnable() {
                    public void run() {
                        core.toggleIsRunning(false);
                        populateObjects(); // FIXME doesnt populate objects..
                    }
                }
                );
                t2.start();
            }
        }
        );
        //SwingUtilities.invokeLater(t); ///freezes the progressor
        t.start();

    }

    public void mesure(final boolean override) {
        final Cell[] cells = getSelectedCells(true);
        if (cells == null) return;
        measureCells(cells, override);
    }

    public void process() {
        final Cell[] cells = getSelectedCells(true);
        if (cells == null) return;
        final boolean[] processChannels = new boolean[this.xp.getNBStructures(true)];
        if (this.listChannel.getSelectedIndex() == -1)
            for (int i = 0; i < processChannels.length; i++) processChannels[i] = true;
        else for (int i : this.listChannel.getSelectedIndices()) processChannels[i] = true;
        processCells(cells, processChannels);
    }

    protected Cell[] getSelectedCells(boolean excludeTag) {
        List cells = list.getSelectedValuesList();
        if (cells != null && cells.size() > 0) {
            ArrayList<Cell> c = new ArrayList<Cell>(cells.size());
            for (int i = 0; i < cells.size(); i++)
                if (!excludeTag || (excludeTag && ((Cell) cells.get(i)).getTag().getTag() >= 0))
                    c.add((Cell) cells.get(i));
            Cell[] res = new Cell[c.size()];
            res = c.toArray(res);
            return res;
        }
        return new Cell[0];
    }

    public void selectCellsFromDB() {
        this.selectingCell = true;
        this.list.clearSelection();
        BasicDBList selectedCells = Core.mongoConnector.getSelectedCells(xp.getId());
        if (selectedCells != null && selectedCells.size() > 0) {
            ArrayList<Integer> selectedIndices = new ArrayList<Integer>(selectedCells.size());
            for (int i = 0; i < listModel.getSize(); i++) {
                if (selectedCells.contains(((Cell) listModel.get(i)).getId().toHexString())) selectedIndices.add(i);
            }
            if (!selectedIndices.isEmpty()) {
                int[] selectedIdx = new int[selectedIndices.size()];
                for (int i = 0; i < selectedIdx.length; i++) selectedIdx[i] = selectedIndices.get(i);
                this.list.setSelectedIndices(selectedIdx);
            }
        }
        this.selectingCell = false;
    }

    public void displayPointPattern3D() {
        Cell cell = (Cell) this.list.getSelectedValue();
        AbstractStructure s = (AbstractStructure) this.listChannel.getSelectedValue();
        RandomPoint3DGenerator rpg = new RandomPoint3DGeneratorUniform(cell.getMask(), s.getObjects().length, Core.getMaxCPUs(), true);
        rpg.drawPoints(1000, 100);
        rpg.showPoint3D(s, 2, 1);
    }

    public void testProcess(int structure, int step, int subStep) {
        System.out.println("Test:" + step + " " + subStep + " Structure:" + structure);
        if (list.getSelectedIndex() < 0) {
            ij.IJ.error("Select a cell");
            return;
        }
        if (structure <= 0) {
            ij.IJ.error("Select a Structure");
            return;
        }
        Core.debug = false;
        Cell c = (Cell) list.getSelectedValue();
        c.close();
        c.setVerbose(false);
        c.setNbCPUs(Core.getMaxCPUs());
        try {
            AbstractStructure s = c.getStructure(structure);
            if (s instanceof Structure) {
                ((Structure) s).testProcess(step, subStep);
            }
        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
        Core.debug = false;
        c.setVerbose(false);
        c.setNbCPUs(1);
    }

    public void testMeasure(int measurementIdx) {
        core.getXPEditor().save(false);
        if (list.getSelectedIndex() < 0) {
            ij.IJ.error("Select a cell");
            return;
        }
        Core.debug = true;
        Cell c = (Cell) list.getSelectedValue();
        c.close();
        c.setVerbose(true);
        c.setNbCPUs(Core.getMaxCPUs());
        try {
            c.testMeasure(measurementIdx);

        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
        c.setVerbose(false);
        c.setNbCPUs(1);
        Core.debug = false;
    }

    public void testSampler(Sampler sampler) {
        core.getXPEditor().save(false);
        if (list.getSelectedIndex() < 0) {
            ij.IJ.error("Select a cell");
            return;
        }
        Core.debug = true;
        Cell c = (Cell) list.getSelectedValue();
        c.close();
        c.setVerbose(true);
        c.setNbCPUs(Core.getMaxCPUs());
        sampler.setVerbose(true);
        try {
            c.testSampler(sampler);

        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
        c.setVerbose(false);
        c.setNbCPUs(1);
        Core.debug = false;
    }

    private void erodeNucleus(Cell c) {
        ImageInt i = c.getMask();
        ImageInt res = BinaryMorpho.binaryErode(i, .5f, .5f);
        res.setTitle(i.getTitle());
        res.setOffset(i);
        res.setScale(i);
        c.getSegmentedImages().setSegmentedImage(res, 0);
        c.getNucleus().saveOutput();
    }

    public void test(boolean process, boolean measure, boolean override, int structure) {
        System.out.println("Test:" + process + " " + measure + " " + override + " Structure:" + structure);
        if (structure < 0) structure = this.listChannel.getSelectedIndex();
        if (list.getSelectedIndex() < 0) {
            ij.IJ.error("Select a cell");
            return;
        }
        /*if (Core.TESTING) {
            Object[] o = list.getSelectedValues();
            for (Object oo:o) {
                erodeNucleus((Cell)oo);
            }
        }
        * 
        */
        
        /*GeodesicMap map = new GeodesicMap();
        map.getStructure().setIndex(1);
        map.getSaturation().setValue(new Double(0.01));
        Cell cell = (Cell)this.list.getSelectedValue();
        map.init(cell.getRawImages(), cell.getSegmentedImages());
        map.getIntensityMap().show("intensityMap");
        AbstractStructure s = (AbstractStructure)this.listChannel.getSelectedValue();
        map.run(s.getObjects(), true, false);
        map.getDistanceMap().showDuplicate("distanceMap");
        map.getLabelMap().showDuplicate("labelMap");
        * 
        */
        //displayPointPattern3D():
        Core.debug = true;
        Cell c = (Cell) list.getSelectedValue();
        c.close();
        c.setVerbose(true);
        c.setNbCPUs(Core.getMaxCPUs());
        try {
            if (process) {
                if (structure >= 0) {
                    boolean[] processChannels = new boolean[this.xp.getNBStructures(true)];
                    processChannels[structure] = true;
                    c.process(processChannels);
                } else IJ.error("Select A Structure");
            }
            if (measure) {
                c.mesure(override);
            }

        } catch (Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
        c.setVerbose(false);
        c.setNbCPUs(1);
        Core.debug = false;
    }


    public void populateObjects() {
        if (this.list.getSelectedIndex() == -1) {
            //objectManager.setStructures(null, new Object[0]);
            objectManager.setStructures(null, new ArrayList() {
            });
        } else
            objectManager.setStructures(((Cell) this.list.getSelectedValue()).getId(), listChannel.getSelectedValuesList());
    }

    public void hide() {
        objectManager.hide(false);
        this.showObjects.setSelected(false);
        container.remove(this.getPanel());
        core.refreshDisplay();
    }

    public void toggleShowObjects() {
        if (showObjects.isSelected()) {
            objectManager.show(false);
            populateObjects();
            moveContainer();
            container.revalidate();
            core.refreshDisplay();
        } else {
            objectManager.hide(true);
        }
    }

    public void tagAction() {
        if (selectingCell || tags.getSelectedIndex() < 0 || list == null) return;
        selectingTag = true;
        this.setTag(this.tags.getSelectedIndex() - 1);
        this.list.validate();
        this.list.repaint();
        selectingTag = false;
    }

    public void selectAll() {
        //this.selectingCell=true;
        list.setSelectionInterval(0, list.getModel().getSize() - 1);
        //this.selectingCell=false;
    }

    public void selectNone() {
        //this.selectingCell=true;
        this.list.clearSelection();
        //this.selectingCell=false;
    }

    public void viewOverlay() {
        this.showCell5d(list.getSelectedIndex());
    }

    public void deleteCells() {
        if (JOptionPane.showConfirmDialog(layout, "Remove selected Cells From DB and Disk?", "ij3DM", JOptionPane.OK_CANCEL_OPTION) == 0) {
            try {
                deleteSelectedCells();
            } catch (Exception e) {
                exceptionPrinter.print(e, "", Core.GUIMode);
            }
        }
    }

    protected void clearCellSelection() {
        this.selectingCell = true;
        this.list.clearSelection();
        this.selectingCell = false;
    }

    public void valueChanged(ListSelectionEvent lse) {
        if (lse.getValueIsAdjusting()) {
            return;
        }
        if (populatingCells || populatingChannels || selectingCell || selectingTag) {
            return;
        }
        if (lse.getSource() == this.listSelectionModel) {
            selectingCell = true;
            //change current tag
            if (list.getSelectedIndex() >= 0) {
                Cell c = (Cell) list.getSelectedValue();

                int tag = c.getTag().getTag() + 1;
                if (tag < Tag.getNbTag()) {
                    tags.setSelectedIndex(tag);
                } else tags.setSelectedIndex(1);

                //changeChannels
                if (currentCell == null || currentCell != c) {

                    if (currentCell != null) {
                        if (recordWindowsPosition.isSelected()) this.windowPos.recordWindowPosition(currentCell);
                        currentCell.close(); //close to free memory
                    }
                    currentCell = c;
                    structureSelection = listChannel.getSelectedIndices();
                    populateStructures();


                    if (this.showFieldRoi) this.showRois3D();


                }
            } else {
                currentCell = null;
                populatingChannels = true;
                this.listChannelModel.removeAllElements();
                populatingChannels = false;
                populateObjects();
                this.hideRois();
            }
            layout.setSelectionLength(list.getSelectedIndices().length);
            selectingCell = false;
        } else if (lse.getSource() == this.listChannelSelectionModel) {
            structureSelection = listChannel.getSelectedIndices();
            if (this.showObjects.isSelected()) this.populateObjects();
        }
    }

    protected void populateStructures() {
        populatingChannels = true;
        for (int i = 0; i < listChannelModel.getSize(); i++) {
            ((AbstractStructure) listChannelModel.getElementAt(i)).setSelectedIndicies(null);
        }
        this.listChannelModel.removeAllElements();
        if (currentCell != null) {
            for (int i = 0; i < currentCell.getNbStructures(true); i++) {
                listChannelModel.addElement(currentCell.getStructure(i));
            }
            if (selectionManager != null && selectionManager.getSelection() != null) {
                Selection s = selectionManager.getSelection();
                int[] selectedStructures = s.getSelectedStructures(currentCell.getId());
                if (selectedStructures.length > 0) {
                    listChannel.setSelectedIndices(selectedStructures);
                    for (int i : selectedStructures) {
                        ((AbstractStructure) listChannelModel.getElementAt(i)).setSelectedIndicies(s.getSelectedObjects(currentCell.getId(), i));
                    }
                } else if (structureSelection != null) listChannel.setSelectedIndices(structureSelection);
            } else if (structureSelection != null) listChannel.setSelectedIndices(structureSelection);
            if (this.showObjects.isSelected()) {
                populateObjects();
            }
        }
        populatingChannels = false;
    }

    public void toggleIsRunning(boolean isRunning) {
        this.layout.toggleIsRunning(isRunning);
        this.list.setEnabled(!isRunning);
        this.listChannel.setEnabled(!isRunning);
        if (objectManager != null) objectManager.toggleIsRunning(isRunning);
    }

    protected void registerActiveImage() {
        ImagePlus activeImage = WindowManager.getCurrentImage();
        if (activeImage != null && activeImage.getProcessor() != null) { // && activeImage.getImageStackSize() > 1
            if (currentImage != null && currentImage.getWindow() != null && currentImage != activeImage) {
                //System.out.println("remove listener:"+currentImage.getTitle());
                ImageUtils.removeScrollListener(currentImage, this, this);
                currentImage.killRoi();
                currentImage.updateAndDraw();
                currentImage = null;
            }
            if (currentImage != activeImage) {
                //System.out.println("add listener:"+activeImage.getTitle());
                ImageUtils.addScrollListener(activeImage, this, this);
                this.currentImage = activeImage;
            }
        }
    }

    public void toggleShowROIs(boolean show) {
        this.showFieldRoi = show;
        if (show) {
            showRois3D();
            if (this.objectManager != null) this.objectManager.toggleShowROIs(false);
        } else {
            this.hideRois();
            CellManagerLayout lay = layout;
            if (lay.showROIs.isSelected()) lay.showROIs.setSelected(false);
        }
    }

    public void showRois3D() {
        registerActiveImage();
        if (currentImage == null) return;
        ImageHandler mask = currentCell.getMask();
        // set Slice
        currentImage.setSlice(mask.offsetZ + mask.sizeZ / 2 + 1);

        int nSlices = currentImage.getNSlices();

        currentROIs = new HashMap<Integer, Roi>(nSlices);

        ImageStack maskstack = mask.getImageStack();

        for (int i = 1; i <= mask.sizeZ; i++) {
            ImagePlus im = new ImagePlus("mask", maskstack.getProcessor(i));
            im.getProcessor().setThreshold(1, 255, ImageProcessor.NO_LUT_UPDATE);
            ThresholdToSelection tts = new ThresholdToSelection();
            tts.setup("", im);
            tts.run(im.getProcessor());
            Roi r = im.getRoi();
            if (r != null) {
                Rectangle rect = r.getBounds();

                r.setLocation(mask.offsetX + rect.x, mask.offsetY + rect.y);
                currentROIs.put(i + mask.offsetZ, r);
            }
        }
        updateRoi();
    }

    public void adjustmentValueChanged(AdjustmentEvent ae) {
        if (this.showFieldRoi) updateRoi();
    }

    public void mouseWheelMoved(MouseWheelEvent mwe) {
        if (this.showFieldRoi) updateRoi();
    }

    protected void updateRoi() {
        //System.out.println("image:"+currentImage.getTitle()+ " slice:"+currentImage.getSlice());
        Roi r = currentROIs.get(currentImage.getSlice());
        if (r != null) {
            currentImage.setRoi(r);
        } else {
            currentImage.killRoi();
        }
        currentImage.updateAndDraw();
    }

    protected void hideRois() {
        if (currentImage == null) return;
        currentImage.killRoi();
        if (currentImage.isVisible()) {
            currentImage.updateAndDraw();
            ImageUtils.removeScrollListener(currentImage, this, this);
        }
        currentImage = null;
    }

    public void plotMeanZProfiles() {
        Cell[] cells = getSelectedCells(false);
        for (Cell c : cells) {
            int sIdx = (listChannel.getSelectedIndex() == -1) ? Cell.structureThumbnail : this.listChannel.getSelectedIndex();
            ImageUtils.plotMeanZprofile(c.getName(), c.getMask(), c.getRawImages().getImage(sIdx));
        }
    }
}
