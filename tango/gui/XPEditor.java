package tango.gui;

import tango.gui.parameterPanel.VirtualStructurePanel;
import tango.gui.parameterPanel.StructurePanel;
import tango.gui.parameterPanel.SamplerPanel;
import tango.gui.parameterPanel.ParameterPanelAbstract;
import tango.gui.parameterPanel.MeasurementPanel;
import tango.gui.parameterPanel.ChannelImagePanel;
import com.mongodb.BasicDBObject;
import ij.IJ;
import ij.Prefs;
import java.awt.FlowLayout;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import mcib3d.utils.exceptionPrinter;
import tango.dataStructure.Experiment;
import tango.gui.parameterPanel.ConfigurationList;
import tango.gui.parameterPanel.ConfigurationListMaster;
import tango.gui.util.*;
import tango.gui.util.DuplicateXPOptionPane.DuplicateXP;
import tango.helper.HelpManager;
import tango.helper.Helper;
import tango.helper.ID;
import tango.helper.RetrieveHelp;
import tango.mongo.MongoConnector;
import tango.parameter.*;
import tango.plugin.sampler.SampleRunner;
import tango.util.Utils;

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
public class XPEditor extends javax.swing.JPanel implements PanelDisplayer {

    public final static DoubleParameter scalexy = new DoubleParameter("Scale XY:", "scaleXY", null, Parameter.nfDEC5);
    public final static DoubleParameter scalez = new DoubleParameter("Scale Z:", "scaleZ", null, Parameter.nfDEC5);
    public final static TextParameter unit = new TextParameter("Unit:", "unit", "µm");
    public final static BooleanParameter useScale=new BooleanParameter("Use Global Calibration?", "globalScale", false);
    private final static HashMap<Object, Parameter[]> map = new HashMap<Object, Parameter[]>(){{
        put(true, new Parameter[]{scalexy, scalez, unit}); 
        put(false, new Parameter[0]);
    }};
    private final static ConditionalParameter globalScale = new ConditionalParameter("Image Calibration", useScale, map);
    //private final static FileParameter inputFile = new FileParameter("Input Folder", "inputFolder", null);
    private final static ChoiceParameter importFileMethod = new ChoiceParameter("Import File Method:", "importFileMethod", FieldFactory.importMethod, FieldFactory.importMethod[0]);
    public static Parameter[] xpParams = new Parameter[]{globalScale, importFileMethod};
    Core core;
    JPanel currentEditPanel;
    boolean init;
    ConfigurationList<StructurePanel> structures;
    ConfigurationList<VirtualStructurePanel> virtualStructures;
    ConfigurationList<MeasurementPanel> measurements;
    ConfigurationList<ChannelImagePanel> channelImages;
    ConfigurationList<SamplerPanel> samples;
    ConfigurationListMaster configurationListMaster;
    JPanel mainSettingsPanel;
    Helper ml;
    int selectedTab;
    JButton importImages;
    
    public XPEditor(Core core) {
        this.core = core;
        init = true;
        initComponents();
        
        editTab.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                selectTab(editTab.getSelectedIndex());
            }
        });
        toggleEnableTabs(false);
        mainSettingsPanel = new JPanel(new FlowLayout()); //new GridLayout(0, 1, 0, 0)
        globalScale.addToContainer(mainSettingsPanel);
        unit.allowSpecialCharacter(true);

        this.settingsPanel.add(mainSettingsPanel);
        importFileMethod.addToContainer(mainSettingsPanel);
        importImages=new JButton("Import Images");
        mainSettingsPanel.add(importImages);
        importImages.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importImages();
            }
        });
        
        getFolders();
        String folder = Prefs.get(MongoConnector.getPrefix() + "_" + Core.mongoConnector.getUserName() + "_folder.String", "");
        if (folders.getItemCount() > 0 && Utils.contains(folders, folder, true)) {
            folders.setSelectedItem(folder);
            setProject(folder);
        }
        
        
        init = false;
    }

    public void registerComponents(HelpManager hm) {
        // projects
        hm.objectIDs.put(this.sfLabel, new ID(RetrieveHelp.editXPPage, "Project"));
        hm.objectIDs.put(this.folders, new ID(RetrieveHelp.editXPPage, "Project"));
        hm.objectIDs.put(this.newFolder, new ID(RetrieveHelp.editXPPage, "New_Project"));
        hm.objectIDs.put(this.removeFolder, new ID(RetrieveHelp.editXPPage, "Delete_Project"));
        
        // experiments
        hm.objectIDs.put(this.eLabel, new ID(RetrieveHelp.editXPPage, "Experiment_2"));
        hm.objectIDs.put(this.experiments, new ID(RetrieveHelp.editXPPage, "Experiment_2"));
        hm.objectIDs.put(this.newExperiment, new ID(RetrieveHelp.editXPPage, "New_Experiment"));
        hm.objectIDs.put(this.deleteExperiment, new ID(RetrieveHelp.editXPPage, "Delete_Experiment"));
        hm.objectIDs.put(this.duplicateExperiment, new ID(RetrieveHelp.editXPPage, "Duplicate"));
        hm.objectIDs.put(this.override, new ID(RetrieveHelp.editXPPage, "Override"));
        hm.objectIDs.put(this.renameExperiment, new ID(RetrieveHelp.editXPPage, "Rename_Experiment"));
        hm.objectIDs.put(scalexy.label(), new ID(RetrieveHelp.editXPPage, "Calibration"));
        hm.objectIDs.put(scalez.label(), new ID(RetrieveHelp.editXPPage, "Calibration"));
        hm.objectIDs.put(unit.label(), new ID(RetrieveHelp.editXPPage, "Calibration"));
        hm.objectIDs.put(save, new ID(RetrieveHelp.editXPPage, "Save"));
        hm.objectIDs.put(importFileMethod.getChoice(), new ID(RetrieveHelp.editXPPage, "Import_File_Method"));
        hm.objectIDs.put(importFileMethod.label(), new ID(RetrieveHelp.editXPPage, "Import_File_Method"));
        hm.objectIDs.put(importImages, new ID(RetrieveHelp.editXPPage, "Import_Images"));
        registerXPComponents(hm);
        
    }
    
    public void registerXPComponents(HelpManager hm) {
        hm.objectIDs.put(channelImageJSP, new ID(RetrieveHelp.editXPPage, "Channel_Images"));
        hm.objectIDs.put(channelImageButtonPanel, new ID(RetrieveHelp.editXPPage, "Channel_Images"));
        hm.objectIDs.put(structureJSP, new ID(RetrieveHelp.editXPPage, "Structures"));
        hm.objectIDs.put(structureButtonPanel, new ID(RetrieveHelp.editXPPage, "Structures"));
        hm.objectIDs.put(virtualStructureJSP, new ID(RetrieveHelp.editXPPage, "Virtual_Structures"));
        hm.objectIDs.put(virtualStructureButtonPanel, new ID(RetrieveHelp.editXPPage, "Virtual_Structures"));
        hm.objectIDs.put(mesurementJSP, new ID(RetrieveHelp.editXPPage, "Quantitative_Image_Analysis"));
        hm.objectIDs.put(mesurementButtonPanel, new ID(RetrieveHelp.editXPPage, "Quantitative_Image_Analysis"));
        hm.objectIDs.put(samplerJSP, new ID(RetrieveHelp.editXPPage, "Samples"));
        hm.objectIDs.put(samplerButtonPanel, new ID(RetrieveHelp.editXPPage, "Samples"));
    }

    public void register(Helper ml) {
        if (this.ml != null && ml != this.ml && ml != null) {
            unRegister(ml);
        }
        this.ml = ml;
        
        register();
    }

    private void register() {
        if (ml == null) {
            return;
        }
        if (structures != null) {
            structures.register(ml);
        }
        if (virtualStructures != null) {
            virtualStructures.register(ml);
        }
        if (measurements != null) {
            measurements.register(ml);
        }
        if (channelImages != null) {
            channelImages.register(ml);
        }
        if (samples != null) {
            samples.register(ml);
        }
    }

    public void unRegister(Helper ml) {
        if (this.ml == ml) {
            this.ml = null;
        }
        if (structures != null) {
            structures.unRegister(ml);
        }
        if (virtualStructures != null) {
            virtualStructures.unRegister(ml);
        }
        if (measurements != null) {
            measurements.unRegister(ml);
        }
        if (channelImages != null) {
            channelImages.unRegister(ml);
        }
        if (samples != null) {
            samples.unRegister(ml);
        }
    }
    
    protected void toggleIsRunning(boolean isRunning) {
        newFolder.setEnabled(!isRunning);
        folders.setEnabled(!isRunning);
        this.experiments.setEnabled(!isRunning);
        toggleEnableButtons(!isRunning, !isRunning);
        
    }
    
    protected void toggleEnableButtons(boolean projectSet, boolean xpSet) {
        if (!projectSet) {
            removeFolder.setEnabled(false);
            newExperiment.setEnabled(false);
            
        }
        if (projectSet) {
            removeFolder.setEnabled(true);
            newExperiment.setEnabled(true);
        }
        if (!xpSet) {
            renameExperiment.setEnabled(false);
            deleteExperiment.setEnabled(false);
            duplicateExperiment.setEnabled(false);
            save.setEnabled(false);
            importImages.setEnabled(false);
            toggleEnableTabs(false);
            this.override.setEnabled(false);
        }
        if (xpSet) {
            deleteExperiment.setEnabled(true);
            renameExperiment.setEnabled(true);
            duplicateExperiment.setEnabled(true);
            this.override.setEnabled(true);
            save.setEnabled(true);
            importImages.setEnabled(true);
            toggleEnableTabs(true);
        }
    }
    
    private void importImages() {
        this.save(true);
        this.core.getFieldManager().importImages();
    }

    private void getFolders() {
        folders.removeAllItems();
        folders.addItem("");
        for (String key : Core.mongoConnector.getProjects()) {
            folders.addItem(key);
        }
    }

    private void getXPs() {
        experiments.removeAllItems();
        experiments.addItem("");
        for (String key : Core.mongoConnector.getExperiments()) {
            experiments.addItem(key);
        }
    }

    private void setProject(String name) {
        if (name == null || name.length() == 0) {
            core.toggleEnableTabs(false);
            toggleEnableButtons(false, false);
            return;
        }
        Core.mongoConnector.setProject(name);
        
        getXPs();
        if (experiments.getItemCount() > 0 && init) {
            String defaultXP = (String) Prefs.get(MongoConnector.getPrefix() + "_" + Core.mongoConnector.getUserName() + "_xp.String", "");
            if (defaultXP.length() > 0 && Utils.contains(experiments, defaultXP, true)) {
                experiments.setSelectedItem(defaultXP);
                setXP(defaultXP);
            } else toggleEnableButtons(true, false);
        }
        Prefs.set(MongoConnector.getPrefix() + "_" + Core.mongoConnector.getUserName() + "_folder.String", name);
    }

    private void flushLists() {
        if (measurements!=null) measurements.flushList();
        if (channelImages!=null) channelImages.flushList();
        if (structures!=null) structures.flushList();
        if (virtualStructures!=null) virtualStructures.flushList();
        if (samples!=null) samples.flushList();
    }
    
    private void setXP(String name) {
        if (name == null || name.length() == 0) {
            core.toggleEnableTabs(false);
            toggleEnableTabs(false);
            toggleEnableButtons(this.folders.getSelectedIndex()>=0, false);
            return;
        }
        try {
            core.setExperiment(new Experiment(name, Core.mongoConnector));
            for (Parameter p : xpParams) {
                p.dbGet(Core.getExperiment().getData());
            }
            flushLists();
            configurationListMaster = new ConfigurationListMaster(this, null);
            measurements = new ConfigurationList<MeasurementPanel>(core, Core.getExperiment().getMeasurementSettings(), this.configurationListMaster, this.mesurementList, this.mesurementButtonPanel, false, false, MeasurementPanel.class);
            structures = new ConfigurationList<StructurePanel>(core, Core.getExperiment().getStructures(), configurationListMaster, this.structureList, this.structureButtonPanel, false, true, StructurePanel.class);
            virtualStructures = new ConfigurationList<VirtualStructurePanel>(core, Core.getExperiment().getVirtualStructures(), this.configurationListMaster, this.virtualStructureList, this.virtualStructureButtonPanel, false, false, VirtualStructurePanel.class);
            samples = new ConfigurationList<SamplerPanel>(core, Core.getExperiment().getSampleChannels(), configurationListMaster, this.samplerList, this.samplerButtonPanel, false, false, SamplerPanel.class);
            channelImages = new ConfigurationList<ChannelImagePanel>(core, Core.getExperiment().getChannelImages(), configurationListMaster, this.channelImageList, this.channelImageButtonPanel ,false, true, ChannelImagePanel.class);
            register();
            if (ml!=null && ml instanceof Helper) registerXPComponents(((Helper)ml).getHelpManager());
            core.toggleEnableTabs(true);
            toggleEnableTabs(true);
            //editTab.setSelectedIndex(0);
            //selectTab(0);
            Prefs.set(MongoConnector.getPrefix() + "_" + Core.mongoConnector.getUserName() + "_xp.String", name);
            toggleEnableButtons(true, true);
            IJ.log("xp:" + name + " set");
        } catch (Exception e) {
            toggleEnableButtons(this.folders.getSelectedIndex()>=0, false);
            exceptionPrinter.print(e, "", Core.GUIMode);
        }

    }

    public void toggleEnableTabs(boolean enable) {
        for (int i = 0; i<editTab.getTabCount(); i++) {
            editTab.setEnabledAt(i, enable);
            if (enable && editTab.getSelectedIndex()==i) selectTab(i);
        }
        if (!enable) editTab.setEnabled(false);
        else editTab.setEnabled(true);
        if (!Core.SPATIALSTATS) editTab.setEnabledAt(4, false);
        if (!Core.VIRTUALSTRUCTURES) editTab.setEnabledAt(2, false); //virtual structures
    }
    

    @Override
    public void showPanel(JPanel panel) {
        hidePanel();
        currentEditPanel = panel;
        panel.setMinimumSize(editPanel.getMinimumSize());
        this.editScroll.setViewportView(panel);
        //this.editPanel.add(panel);
        //this.editScrollPane.setViewportView(panel);
        refreshDisplay();
    }

    @Override
    public void hidePanel() {
        this.editScroll.setViewportView(editPanel);
        //if (currentEditPanel!=null) this.editPanel.remove(currentEditPanel);
        currentEditPanel = null;
        refreshDisplay();
    }

    @Override
    public void refreshDisplay() {
        this.editScroll.repaint();
        this.editScroll.revalidate();
        this.editPanel.repaint();
        this.editPanel.revalidate();
        this.settingsPanel.repaint();
        this.settingsPanel.revalidate();
        core.refreshDisplay();
    }

    public void refreshParameters() {
        if (Core.getExperiment() == null) {
            return;
        }
        if (selectedTab==4) {
            ParameterPanelAbstract[] panels = samples.getParameterPanels();
            String[] names = new String[panels.length];
            for (int i = 0; i<names.length; i++) names[i]=((SamplerPanel)panels[i]).getName();
            SamplerParameter.setChannels(names);
        }
        else if (selectedTab==0)  {
            ParameterPanelAbstract[] panels = channelImages.getParameterPanels();
            String[] names = new String[panels.length];
            for (int i = 0; i<names.length; i++) names[i]=((ChannelImagePanel)panels[i]).getName();
            ChannelFileParameter.setChannels(names);
        }
        else if (selectedTab==1 || selectedTab==2) {
            ParameterPanelAbstract[] panels = structures.getParameterPanels();
            String[] names = new String[panels.length];
            for (int i = 0; i<names.length; i++) names[i]=((StructurePanel)panels[i]).getName();
            panels = virtualStructures.getParameterPanels();
            String[] namesV = new String[panels.length];
            for (int i = 0; i<namesV.length; i++) namesV[i]=((VirtualStructurePanel)panels[i]).getName();
            StructureParameter.setStructures(names, namesV);
        }
        
        measurements.refreshParameters();
        channelImages.refreshParameters();
        structures.refreshParameters();
        virtualStructures.refreshParameters();
        samples.refreshParameters();
        register();
    }
    
    public void refreshDispayMPP() {
        if (Core.getExperiment() == null) {
            return;
        }
        mesurementList.updateUI();
        channelImageList.updateUI();
        structureList.updateUI();
        virtualStructureList.updateUI();
        samplerList.updateUI();
        // TODO Validate??
    }
    
    

    private void allOff() {
        if (Core.getExperiment() == null) {
            return;
        }
        this.configurationListMaster.hideConfigurationPanel(true);
    }
    
    public void selectTab(int newTab) {
        if (Core.getExperiment() == null) {
            return;
        }
        refreshParameters();
        allOff();
        
        selectedTab=newTab;
        this.hidePanel();
    }

    public void save(boolean record) {
        if (Core.getExperiment() == null) {
            return;
        }
        //parameters in mainPanel
        for (Parameter p : xpParams) {
            p.dbPut(Core.getExperiment().getData());
        }
        Core.getExperiment().setChannelImages(this.channelImages.saveMulti());
        Core.getExperiment().setStructures(this.structures.saveMulti(), true);
        Core.getExperiment().setVirtualStructures(this.virtualStructures.saveMulti());
        Core.getExperiment().setMeasurements(this.measurements.saveMulti());
        Core.getExperiment().setSamples(this.samples.saveMulti());
        if (record) {
            Core.getExperiment().save();
            Core.setExperimentModifiedFromAnalyzer(true);
        }
        this.core.updateSettings();
        IJ.log("xp saved!!");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        settingsPanel = new javax.swing.JPanel();
        connectPanel = new javax.swing.JPanel();
        sfLabel = new javax.swing.JLabel();
        newFolder = new javax.swing.JButton();
        folders = new javax.swing.JComboBox();
        removeFolder = new javax.swing.JButton();
        eLabel = new javax.swing.JLabel();
        experiments = new javax.swing.JComboBox();
        newExperiment = new javax.swing.JButton();
        renameExperiment = new javax.swing.JButton();
        duplicateExperiment = new javax.swing.JButton();
        deleteExperiment = new javax.swing.JButton();
        save = new javax.swing.JButton();
        override = new javax.swing.JButton();
        editTab = new javax.swing.JTabbedPane();
        channelImagePanel = new javax.swing.JPanel();
        channelImageButtonPanel = new javax.swing.JPanel();
        channelImageJSP = new javax.swing.JScrollPane();
        channelImageList = new javax.swing.JList();
        structurePanel = new javax.swing.JPanel();
        structureButtonPanel = new javax.swing.JPanel();
        structureJSP = new javax.swing.JScrollPane();
        structureList = new javax.swing.JList();
        virtualStructurePanel = new javax.swing.JPanel();
        virtualStructureButtonPanel = new javax.swing.JPanel();
        virtualStructureJSP = new javax.swing.JScrollPane();
        virtualStructureList = new javax.swing.JList();
        mesurementPanel = new javax.swing.JPanel();
        mesurementJSP = new javax.swing.JScrollPane();
        mesurementList = new javax.swing.JList();
        mesurementButtonPanel = new javax.swing.JPanel();
        samplerPanel = new javax.swing.JPanel();
        samplerButtonPanel = new javax.swing.JPanel();
        samplerJSP = new javax.swing.JScrollPane();
        samplerList = new javax.swing.JList();
        editScroll = new javax.swing.JScrollPane();
        editPanel = new javax.swing.JPanel();

        setMaximumSize(new java.awt.Dimension(1024, 600));
        setMinimumSize(new java.awt.Dimension(1024, 600));
        setPreferredSize(new java.awt.Dimension(1024, 600));

        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("General Options"));
        settingsPanel.setMaximumSize(new java.awt.Dimension(362, 243));
        settingsPanel.setMinimumSize(new java.awt.Dimension(362, 243));
        settingsPanel.setPreferredSize(new java.awt.Dimension(362, 243));
        settingsPanel.setLayout(new java.awt.GridLayout(1, 0));

        connectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Select Experiment"));
        connectPanel.setMaximumSize(new java.awt.Dimension(362, 321));
        connectPanel.setMinimumSize(new java.awt.Dimension(362, 321));

        sfLabel.setText("Project:");

        newFolder.setText("New");
        newFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newFolderActionPerformed(evt);
            }
        });

        folders.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "" }));
        folders.setMaximumSize(new java.awt.Dimension(200, 28));
        folders.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                foldersItemStateChanged(evt);
            }
        });

        removeFolder.setText("Delete");
        removeFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeFolderActionPerformed(evt);
            }
        });

        eLabel.setText("Experiment:");

        experiments.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "" }));
        experiments.setMaximumSize(new java.awt.Dimension(200, 28));
        experiments.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                experimentsItemStateChanged(evt);
            }
        });

        newExperiment.setText("New");
        newExperiment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newExperimentActionPerformed(evt);
            }
        });

        renameExperiment.setText("Rename");
        renameExperiment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                renameExperimentActionPerformed(evt);
            }
        });

        duplicateExperiment.setText("Duplicate");
        duplicateExperiment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                duplicateExperimentActionPerformed(evt);
            }
        });

        deleteExperiment.setText("Delete");
        deleteExperiment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteExperimentActionPerformed(evt);
            }
        });

        save.setText("Save Changes");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });

        override.setText("Override");
        override.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                overrideActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout connectPanelLayout = new javax.swing.GroupLayout(connectPanel);
        connectPanel.setLayout(connectPanelLayout);
        connectPanelLayout.setHorizontalGroup(
            connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(connectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, connectPanelLayout.createSequentialGroup()
                        .addComponent(newFolder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeFolder, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(folders, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(experiments, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(connectPanelLayout.createSequentialGroup()
                        .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(eLabel)
                            .addComponent(sfLabel))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, connectPanelLayout.createSequentialGroup()
                        .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(duplicateExperiment, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(newExperiment, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                            .addComponent(override, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(save, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(renameExperiment, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE)
                            .addComponent(deleteExperiment, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE))))
                .addContainerGap())
        );
        connectPanelLayout.setVerticalGroup(
            connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(connectPanelLayout.createSequentialGroup()
                .addGap(0, 4, Short.MAX_VALUE)
                .addComponent(sfLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(folders, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(removeFolder)
                    .addComponent(newFolder))
                .addGap(24, 24, 24)
                .addComponent(eLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(experiments, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(renameExperiment)
                    .addComponent(newExperiment))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(duplicateExperiment)
                    .addComponent(deleteExperiment))
                .addGap(25, 25, 25)
                .addGroup(connectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(save)
                    .addComponent(override))
                .addContainerGap())
        );

        editTab.setMinimumSize(new java.awt.Dimension(644, 280));
        editTab.setPreferredSize(new java.awt.Dimension(644, 280));

        channelImageButtonPanel.setLayout(new javax.swing.BoxLayout(channelImageButtonPanel, javax.swing.BoxLayout.LINE_AXIS));

        channelImageList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        channelImageJSP.setViewportView(channelImageList);

        javax.swing.GroupLayout channelImagePanelLayout = new javax.swing.GroupLayout(channelImagePanel);
        channelImagePanel.setLayout(channelImagePanelLayout);
        channelImagePanelLayout.setHorizontalGroup(
            channelImagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(channelImagePanelLayout.createSequentialGroup()
                .addComponent(channelImageButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 372, Short.MAX_VALUE))
            .addGroup(channelImagePanelLayout.createSequentialGroup()
                .addComponent(channelImageJSP)
                .addContainerGap())
        );
        channelImagePanelLayout.setVerticalGroup(
            channelImagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(channelImagePanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(channelImageButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(channelImageJSP, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))
        );

        editTab.addTab("Channel Image", channelImagePanel);

        structureButtonPanel.setLayout(new javax.swing.BoxLayout(structureButtonPanel, javax.swing.BoxLayout.LINE_AXIS));

        structureList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        structureJSP.setViewportView(structureList);

        javax.swing.GroupLayout structurePanelLayout = new javax.swing.GroupLayout(structurePanel);
        structurePanel.setLayout(structurePanelLayout);
        structurePanelLayout.setHorizontalGroup(
            structurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(structurePanelLayout.createSequentialGroup()
                .addComponent(structureButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(structurePanelLayout.createSequentialGroup()
                .addComponent(structureJSP, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
                .addContainerGap())
        );
        structurePanelLayout.setVerticalGroup(
            structurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(structurePanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(structureButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(structureJSP, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))
        );

        editTab.addTab("Structures", structurePanel);

        virtualStructureButtonPanel.setLayout(new javax.swing.BoxLayout(virtualStructureButtonPanel, javax.swing.BoxLayout.LINE_AXIS));

        virtualStructureList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        virtualStructureJSP.setViewportView(virtualStructureList);

        javax.swing.GroupLayout virtualStructurePanelLayout = new javax.swing.GroupLayout(virtualStructurePanel);
        virtualStructurePanel.setLayout(virtualStructurePanelLayout);
        virtualStructurePanelLayout.setHorizontalGroup(
            virtualStructurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(virtualStructurePanelLayout.createSequentialGroup()
                .addComponent(virtualStructureButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 372, Short.MAX_VALUE))
            .addComponent(virtualStructureJSP)
        );
        virtualStructurePanelLayout.setVerticalGroup(
            virtualStructurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(virtualStructurePanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(virtualStructureButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(virtualStructureJSP, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))
        );

        editTab.addTab("Virtual Structures", virtualStructurePanel);

        mesurementList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        mesurementJSP.setViewportView(mesurementList);

        mesurementButtonPanel.setAlignmentX(0.0F);
        mesurementButtonPanel.setPreferredSize(new java.awt.Dimension(260, 26));
        mesurementButtonPanel.setLayout(new javax.swing.BoxLayout(mesurementButtonPanel, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout mesurementPanelLayout = new javax.swing.GroupLayout(mesurementPanel);
        mesurementPanel.setLayout(mesurementPanelLayout);
        mesurementPanelLayout.setHorizontalGroup(
            mesurementPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mesurementJSP, javax.swing.GroupLayout.DEFAULT_SIZE, 632, Short.MAX_VALUE)
            .addGroup(mesurementPanelLayout.createSequentialGroup()
                .addComponent(mesurementButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        mesurementPanelLayout.setVerticalGroup(
            mesurementPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mesurementPanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(mesurementButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(mesurementJSP, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))
        );

        editTab.addTab("Measurements", mesurementPanel);

        samplerButtonPanel.setLayout(new javax.swing.BoxLayout(samplerButtonPanel, javax.swing.BoxLayout.LINE_AXIS));

        samplerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        samplerJSP.setViewportView(samplerList);

        javax.swing.GroupLayout samplerPanelLayout = new javax.swing.GroupLayout(samplerPanel);
        samplerPanel.setLayout(samplerPanelLayout);
        samplerPanelLayout.setHorizontalGroup(
            samplerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(samplerPanelLayout.createSequentialGroup()
                .addComponent(samplerButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 372, Short.MAX_VALUE))
            .addComponent(samplerJSP)
        );
        samplerPanelLayout.setVerticalGroup(
            samplerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(samplerPanelLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addComponent(samplerButtonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(samplerJSP, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))
        );

        editTab.addTab("Samplers", samplerPanel);

        editPanel.setMinimumSize(new java.awt.Dimension(625, 250));
        editPanel.setPreferredSize(new java.awt.Dimension(625, 250));

        javax.swing.GroupLayout editPanelLayout = new javax.swing.GroupLayout(editPanel);
        editPanel.setLayout(editPanelLayout);
        editPanelLayout.setHorizontalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 642, Short.MAX_VALUE)
        );
        editPanelLayout.setVerticalGroup(
            editPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 274, Short.MAX_VALUE)
        );

        editScroll.setViewportView(editPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(connectPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(editScroll)
                    .addComponent(editTab, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(editTab, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 276, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addComponent(connectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void newExperimentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newExperimentActionPerformed
        String name = JOptionPane.showInputDialog("Experiment Name");
        if (name==null) return;
        if (Utils.isValid(name, false) && !Utils.contains(experiments, name, false)) {
            Core.mongoConnector.createExperiment(name);
            getXPs();
            experiments.setSelectedItem(name);
            setXP(name);
        } else {
            IJ.error("Invalid Name/Experiment already exists");
        }
    }//GEN-LAST:event_newExperimentActionPerformed

    private void renameExperimentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_renameExperimentActionPerformed
        if (experiments.getSelectedIndex() >= 0) {
            String old_name = Utils.getSelectedString(experiments);
            String name = JOptionPane.showInputDialog("Rename Experiment from:" + old_name + " to:", old_name);
            //IJ.log("rename XP debug:: xp source:"+old_name+ " destination :"+name);
            if (name==null || name.equals(old_name)) return;
            if (Utils.isValid(name, false) && !Utils.contains(experiments, name, false)) {
                //IJ.log("rename XP debug::process...");
                Core.mongoConnector.renameExperiment(old_name, name);
                //IJ.log("rename XP debug::OK");
                getXPs();
                experiments.setSelectedItem(name);
                setXP(name);
                //IJ.log("rename XP debug::SET XP");
            } else {
                IJ.error("Invalid Name/Experiment already exists");
            }
        } else {
            IJ.error("Select XP first");
        }
    }//GEN-LAST:event_renameExperimentActionPerformed

    private void removeFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeFolderActionPerformed
        Object name = folders.getSelectedItem();
        if (name != null && JOptionPane.showConfirmDialog(this, "Remove Folder:" + name + " and Experiments associated?", "TANGO", JOptionPane.OK_CANCEL_OPTION) == 0) {
            Core.mongoConnector.removeProject((String) name);
            folders.removeItem(name);
            experiments.removeAllItems();
        }
    }//GEN-LAST:event_removeFolderActionPerformed

    private void newFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newFolderActionPerformed
        String name = JOptionPane.showInputDialog("Project Name (no special chars)");
        if (name==null) return;
        if (!Utils.isValid(name, false)) {
            IJ.error("Invalid name (no speical chars allowed)");
            return;
        }
        if (Core.mongoConnector.getUserName().length()+name.length()>50) {
            IJ.error("Name is too long");
            return;
        }
        if (!Utils.contains(folders, name, false)) {
            Core.mongoConnector.createProject(name);
            getFolders();
            folders.setSelectedItem(name);
            setProject(name);
        } else {
            IJ.error("Project already exists");
        }
    }//GEN-LAST:event_newFolderActionPerformed

    private void deleteExperimentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteExperimentActionPerformed
        if (experiments.getSelectedIndex() >= 0) {
            String name = (String) experiments.getSelectedItem();
            if (JOptionPane.showConfirmDialog(this, "Remove Experiment:" + name + " and mesures associated?", "TANGO", JOptionPane.OK_CANCEL_OPTION) == 0) {
                //IJ.log("obj:"+mc.experiment.findOne(new BasicDBObject("outputFileName", outputFileName)).toString());
                Core.mongoConnector.removeExperiment(name);
                experiments.removeItem(name);
            }
        } else {
            IJ.error("Select XP first");
        }
    }//GEN-LAST:event_deleteExperimentActionPerformed

    private void duplicateExperimentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_duplicateExperimentActionPerformed
        if (experiments.getSelectedIndex() >= 0) {
            //String[] dest = TextChoiceJOptionPane.showInputDialog("Enter Name and Destination Set:", Core.mongoConnector.getProjects());
            DuplicateXP d = DuplicateXPOptionPane.showInputDialog(core,"Duplicate Experiment:", Core.mongoConnector.getProjects(), Utils.getSelectedString(experiments));
            if (d == null) {
                return;
            }
            String source = (String) experiments.getSelectedItem();
            if (d.set.equals(folders.getSelectedItem())) { 
                if (d.xp != null && d.xp.length() > 0 && !Core.mongoConnector.getExperiments().contains(d.xp)) {
                    Core.mongoConnector.duplicateExperiment(Core.mongoConnector, source, d.xp);
                    getXPs();
                    experiments.setSelectedItem(d.xp);
                    setXP(d.xp);
                } else {
                    IJ.error("name must be different");
                }
            } else {
                MongoConnector mc2 = Core.mongoConnector.duplicate(false);
                mc2.setProject(d.set);
                if (d.xp != null && d.xp.length() > 0 && !mc2.getExperiments().contains(d.xp)) {
                    mc2.duplicateExperiment(Core.mongoConnector, source, d.xp);
                    folders.setSelectedItem(d.set);
                    setProject(d.set);
                    experiments.setSelectedItem(d.xp);
                    setXP(d.xp);
                } else {
                    IJ.error("XP already existing in " + d.set);
                }
                mc2.close();
            }
        }
    }//GEN-LAST:event_duplicateExperimentActionPerformed

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
        if (Core.getExperiment() == null) {
            return;
        }
        save(true);
        //refreshParameters();
        // FIXME save
        //save(); //in case modif on structures / virtualStructure would shift indexes
    }//GEN-LAST:event_saveActionPerformed

    private void foldersItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_foldersItemStateChanged
        if (init) {
            return;
        }
        if (evt.getStateChange() == 1) {
            String name = (String) folders.getSelectedItem();
            setProject(name);
        }
    }//GEN-LAST:event_foldersItemStateChanged

    private void experimentsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_experimentsItemStateChanged
        if (init) {
            return;
        }
        if (evt.getStateChange() == 1) {
            String name = (String) experiments.getSelectedItem();
            setXP(name);
        }
    }//GEN-LAST:event_experimentsItemStateChanged

    private void overrideActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_overrideActionPerformed
        OverrideXPElement[] xps = OverrideXPOptionPane.showInputDialog(5, "", "");
        if (xps!=null) {
            MongoConnector mc2 = Core.mongoConnector.duplicate(false);  
            for (OverrideXPElement o : xps) {
                mc2.setProject(o.project);
                BasicDBObject xp = mc2.getExperiment(o.xp);
                if (xp != null) {
                    if (o.channelImages) {
                        xp.append("channelFiles", Core.getExperiment().getChannelImages());
                    }
                    if (o.structures) {
                        xp.append("structures", Core.getExperiment().getStructures());
                    }
                    if (o.virtualStructures) {
                        xp.append("virtualStructures", Core.getExperiment().getVirtualStructures());
                    }
                    if (o.measurements) {
                        xp.append("measurements", Core.getExperiment().getMeasurementSettings());
                    }
                    if (o.samplers) {
                        xp.append("sampleChannels", Core.getExperiment().getSampleChannels());
                    }
                    mc2.saveExperiment(xp);
                    IJ.log("Override: "+o);
                }
                    
            }
            mc2.close();
        }
    }//GEN-LAST:event_overrideActionPerformed

    private void testSampleActionPerformed(java.awt.event.ActionEvent evt) {
        SampleRunner.test();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel channelImageButtonPanel;
    private javax.swing.JScrollPane channelImageJSP;
    private javax.swing.JList channelImageList;
    private javax.swing.JPanel channelImagePanel;
    private javax.swing.JPanel connectPanel;
    private javax.swing.JButton deleteExperiment;
    private javax.swing.JButton duplicateExperiment;
    private javax.swing.JLabel eLabel;
    private javax.swing.JPanel editPanel;
    private javax.swing.JScrollPane editScroll;
    private javax.swing.JTabbedPane editTab;
    private javax.swing.JComboBox experiments;
    private javax.swing.JComboBox folders;
    private javax.swing.JPanel mesurementButtonPanel;
    private javax.swing.JScrollPane mesurementJSP;
    private javax.swing.JList mesurementList;
    private javax.swing.JPanel mesurementPanel;
    private javax.swing.JButton newExperiment;
    private javax.swing.JButton newFolder;
    private javax.swing.JButton override;
    private javax.swing.JButton removeFolder;
    private javax.swing.JButton renameExperiment;
    private javax.swing.JPanel samplerButtonPanel;
    private javax.swing.JScrollPane samplerJSP;
    private javax.swing.JList samplerList;
    private javax.swing.JPanel samplerPanel;
    private javax.swing.JButton save;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JLabel sfLabel;
    private javax.swing.JPanel structureButtonPanel;
    private javax.swing.JScrollPane structureJSP;
    private javax.swing.JList structureList;
    private javax.swing.JPanel structurePanel;
    private javax.swing.JPanel virtualStructureButtonPanel;
    private javax.swing.JScrollPane virtualStructureJSP;
    private javax.swing.JList virtualStructureList;
    private javax.swing.JPanel virtualStructurePanel;
    // End of variables declaration//GEN-END:variables
}
