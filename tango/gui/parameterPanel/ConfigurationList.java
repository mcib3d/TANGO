package tango.gui.parameterPanel;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import ij.IJ;
import mcib3d.utils.exceptionPrinter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import tango.gui.Core;
import tango.gui.PanelDisplayer;
import tango.gui.util.ConfigurationListCellRenderer;
import tango.gui.util.Displayer;
import tango.gui.util.PanelElementAbstract;
import tango.gui.util.PanelElementMono;
import tango.helper.Helper;
import tango.util.ImageUtils;
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
 * @param <T>
 */
public class ConfigurationList<T extends ParameterPanelAbstract> {
    protected JList jlist;
    protected DefaultListModel listModel;
    protected JButton add, remove, test, up, down;
    protected JToggleButton edit;
    protected DBObject data;
    protected Class<T> clazz;
    protected Helper ml;
    boolean minElement, mono;
    ConfigurationList<T> template, instance=this;
    ConfigurationListMaster master;
    JPanel buttonPanel;
    Core core;
    int lastSelectedIndex=-1;
    public ConfigurationList(Core core, DBObject data, ConfigurationListMaster master_, JList jlist_, JPanel buttonPanel, boolean mono, boolean minElement, Class<T> clazz) {
        this.core=core;
        this.master=master_;
        this.minElement=minElement;
        this.mono=mono;
        this.buttonPanel=buttonPanel;
        this.clazz=clazz;
        this.data=data;
        this.jlist=jlist_;
        // create list
        this.listModel = new DefaultListModel();
        if (jlist!=null) {
            this.jlist.setModel(listModel);
            this.jlist.setCellRenderer(new ConfigurationListCellRenderer());
            this.jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.jlist.setLayoutOrientation(JList.VERTICAL);
            //listSelectionModel = this.jlist.getSelectionModel();
            jlist.addListSelectionListener(new ListSelectionListener(){
                @Override
                public void valueChanged(ListSelectionEvent lse) {
                    if (lse.getValueIsAdjusting()) return;
                    int selIdx = jlist.getSelectedIndex();
                    //int lastSelectedIndex = selIdx == lse.getFirstIndex() ? lse.getLastIndex() : lse.getFirstIndex();
                    IJ.log("last selection index = "+lastSelectedIndex);
                    IJ.log("sel idx = "+selIdx);
                    if (lastSelectedIndex>=0 && lastSelectedIndex!=selIdx && lastSelectedIndex<listModel.size()) {
                        IJ.log("deselected index = "+lastSelectedIndex);
                        if (edit.isSelected()) master.hideConfigurationPanel(false);
                        ((ConfigurationElementAbstract)listModel.get(lastSelectedIndex)).updateValidity();
                    }
                    if ( edit.isSelected() && selIdx>=0) {
                        master.showConfigurationPanel((ConfigurationElementAbstract)listModel.get(selIdx), instance);
                    } else if (selIdx>=0 && lastSelectedIndex==-1) {
                        edit.setSelected(true);
                        master.showConfigurationPanel((ConfigurationElementAbstract)listModel.get(selIdx), instance);
                    }
                    lastSelectedIndex=selIdx;
                    IJ.log("new last selected index: "+lastSelectedIndex);
                }
            });
        }
        if (buttonPanel!=null) createButtons();
        populateData();
        
    }
    
    protected void populateData() {
        if (this.data!=null) {
            if (!mono) {
                BasicDBList listData = (BasicDBList)data;
                for (Object o : listData) addElement((BasicDBObject)o);
            } else addElement((BasicDBObject)data);
        } else if (minElement) addElement(null);
    }
    
    protected void createButtons() {
        // modifier en fonction des types...
        
        if (!mono) {
            add=new JButton("");
            add.setActionCommand("Add");
            
            add.setIcon(ImageUtils.add);
            add.setToolTipText("Remove");
            add.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    addElement(null);
                }
            });
            buttonPanel.add(add);
            buttonPanel.add(Box.createHorizontalStrut(2));
            remove = new JButton("");
            remove.setActionCommand("Remove");
            remove.setIcon(ImageUtils.remove);
            remove.setToolTipText("Remove");
            remove.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (edit.isSelected()) {
                        master.hideConfigurationPanel(false);
                    }
                    int idx = jlist.getSelectedIndex();
                    if (idx>=0) {
                        if (ml!=null) ((ConfigurationElementAbstract)listModel.get(idx)).unRegister(ml);
                        listModel.remove(idx);
                        if (idx<listModel.size()) jlist.setSelectedIndex(idx);
                        else lastSelectedIndex=-1;
                        updateValidity();
                    }
                }
            });
            buttonPanel.add(remove);
            buttonPanel.add(Box.createHorizontalStrut(2));
        }    
        edit = new JToggleButton("");
        edit.setActionCommand("Edit");
        edit.setSize(26, 26);
        edit.setIcon(ImageUtils.edit);
        edit.setToolTipText("Edit");
        edit.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (jlist.getSelectedIndex()<0) return;
                if (edit.isSelected()) master.showConfigurationPanel((ConfigurationElementAbstract)jlist.getSelectedValue(), instance);
                else {
                    ((ConfigurationElementAbstract)jlist.getSelectedValue()).updateValidity();
                    jlist.updateUI();
                    master.hideConfigurationPanel(false);
                }
            }
        });
        buttonPanel.add(edit);
        buttonPanel.add(Box.createHorizontalStrut(2));
        
        test = new JButton("");
        test.setActionCommand("Test");
        test.setSize(26, 26);
        test.setIcon(ImageUtils.test);
        test.setToolTipText("Test");
        if (clazz.isAssignableFrom(ParameterPanelPlugin.class)) {
            buttonPanel.add(test);
            buttonPanel.add(Box.createHorizontalStrut(2));
            test.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    test();
                }
            });
        }
        if (!mono) {
            up=new JButton("");
            up.setActionCommand("Up");
            up.setSize(26, 26);
            up.setIcon(ImageUtils.up);
            up.setToolTipText("Up");
            up.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    int i = jlist.getSelectedIndex();
                    if (i>0) {
                        Object o = listModel.remove(i);
                        listModel.add(i-1, o);
                        jlist.setSelectedIndex(i-1);
                    }
                }
            });
            buttonPanel.add(up);
            buttonPanel.add(Box.createHorizontalStrut(2));
            
            down=new JButton("");
            down.setActionCommand("Down");
            down.setSize(26, 26);
            //down.setPreferredSize(new Dimension(35, 28));
            down.setIcon(ImageUtils.down);
            down.setToolTipText("Down");
            down.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent ae) {
                    int i = jlist.getSelectedIndex();
                    if (i<0) return;
                    if (i<listModel.size()-2) {
                        Object o = listModel.remove(i);
                        listModel.add(i+1, o);
                        jlist.setSelectedIndex(i+1);
                    } else if (i==listModel.size()-2) {
                        Object o = listModel.remove(i);
                        listModel.addElement(o);
                        jlist.setSelectedIndex(i+1);
                    }
                }
            });
            
            buttonPanel.add(down);
            //buttonPanel.add(Box.createHorizontalGlue());
            //buttonBox.setMinimumSize(buttonBox.getPreferredSize());
        }
        
    }
    
    protected void updateValidity() {
        if (edit==null) return;
        Color c = Color.BLACK;
        for (int i = 0; i<listModel.size(); i++) {
            c = Utils.compareColor(c, ((ConfigurationElementAbstract)listModel.get(i)).updateValidity());
            if (c.equals(Color.RED)) {
                edit.setIcon(ImageUtils.editError);
                return;
            }
        }
        if ((template!=null && this.listModel.size()!=template.listModel.size()) || c.equals(Color.BLUE)) edit.setIcon(ImageUtils.editDiff);
        else edit.setIcon(ImageUtils.edit);
        this.jlist.updateUI();
    }
    
    public DBObject save() {
        if (mono) return saveMono();
        else return saveMulti();
    }
    
    public BasicDBList saveMulti() { // TODO to extended classes
        BasicDBList list = new BasicDBList();
        for (Object o : listModel.toArray()) {
            if (o instanceof ConfigurationElementPlugin) {
                if (((ConfigurationElementPlugin)o).getParameterPanel().getMethod()==null)  ((ConfigurationElementPlugin)o).setCurrentMethod();
            } else {
                ((ConfigurationElement)o).getParameterPanel().checkValidity();
            }
            list.add(((ConfigurationElementAbstract)o).getParameterPanel().save());
        }
        return list;
    }
    
    public BasicDBObject saveMono () {
        if (listModel.size()>0) return ((ConfigurationElementAbstract)listModel.get(0)).getParameterPanel().save();
        else return null;
    }
    
    protected void addElement(BasicDBObject DBO) {
        try{
            int idx;
            if (jlist!=null) idx = jlist.getSelectedIndex();
            else idx = -1;
            if (idx==-1) idx = listModel.size();
            ConfigurationElementAbstract b = createConfigurationElement(DBO, idx);
            if (ml!=null) b.register(ml);
            if (idx==listModel.size()) {
                IJ.log("add element: "+b.getLabel());
                listModel.addElement(b);
            }
            else {
                IJ.log("add element:"+b.getLabel()+" " + (idx+1));
                listModel.add(idx+1, b);
            } // TODO décaler les autres indices?
            if (jlist!=null) jlist.setSelectedIndex(idx+1);
            if (template!=null) {
                if (template.listModel.size()>idx) {
                    if (b instanceof ConfigurationElementPlugin) ((ConfigurationElementPlugin)b).setTemplate((ConfigurationElementPlugin)template.listModel.get(idx));
                }
            }
            if (b instanceof ConfigurationElementPlugin) {
                ((ConfigurationElementPlugin)b).method.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //instance.updateValidity();
                        jlist.updateUI();
                    }
                });
            }
            //panelDisplayer.refreshDisplay();
            this.updateValidity();
        } catch(Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        }
    }
    
    protected ConfigurationElementAbstract createConfigurationElement(BasicDBObject DBO, int idx) {
        T t;
        try{
            t = clazz.newInstance();
            t.setIdx(idx);
            t.setData(DBO);
            t.initPanel();
            ConfigurationElementAbstract b;
            if (t instanceof ParameterPanel) {
                b=new ConfigurationElement((ParameterPanel)t, idx);
            } else {
                b = new ConfigurationElementPlugin((ParameterPanelPlugin)t);
                if (template!=null && idx<template.listModel.size()) ((ConfigurationElementPlugin)b).setTemplate((ConfigurationElementPlugin)template.listModel.get(idx));
            }
            //System.out.println("panelElement null:"+(b==null)+ " idx:"+idx+ " "+DBO);
            return b;
        } catch(Exception e) {
            exceptionPrinter.print(e, "", Core.GUIMode);
        } return null;
    }
    
    public void removeElement(ConfigurationElementAbstract c) {
        listModel.removeElement(c);
        updateValidity();
    }
    
    public void editOff() {
        IJ.log("edit off");
        edit.setSelected(false);
        jlist.clearSelection();
        lastSelectedIndex=-1;
    }
    
    public void showPanel(ConfigurationElementAbstract c) {
        edit.setSelected(true);
        master.showConfigurationPanel(c, this);
    }
    
    public void test() {
        int subStep = this.jlist.getSelectedIndex();
        if (clazz.equals(PreFilterPanel.class)) {    
            core.getProcessingSequenceEditor().test(0, subStep);
        } else if (clazz.equals(PostFilterPanel.class)) {
            core.getProcessingSequenceEditor().test(2, subStep);
        } else if (clazz.equals(NucleiSegmenterPanel.class) || clazz.equals(ChannelSegmenterPanel.class)) {
            core.getProcessingSequenceEditor().test(1, 0);
        } else if (clazz.equals(MeasurementPanel.class)) {
            core.getCellManager().testMeasure(subStep);
        } else if (clazz.equals(SamplerPanel.class)) {
            core.getCellManager().testSampler(((SamplerPanel)listModel.get(subStep)).getSampler());
        }
    }
    
    public void setTemplate(ConfigurationList<T> mpp) {
        this.template=mpp;
        for (int i = 0; i<this.listModel.size(); i++) {
            if (listModel.get(i) instanceof PanelElementPlugin) {
                if (mpp!=null) {
                    if (i<mpp.listModel.size()) {
                        if (mpp.listModel.get(i) instanceof PanelElementPlugin) {
                            ((PanelElementPlugin)listModel.get(i)).setTemplate((PanelElementPlugin)mpp.listModel.get(i));
                        }
                    } else ((PanelElementPlugin)listModel.get(i)).setTemplate(null);
                } else ((PanelElementPlugin)listModel.get(i)).removeTemplate();
            }
        }
        updateValidity();
    }
    
    public void refreshParameters() {
        for (int i = 0; i<listModel.size(); i++) ((ConfigurationElementAbstract)listModel.get(i)).getParameterPanel().refreshParameters();
    }
    
    public void register(Helper ml) {
        this.ml=ml;
        for (int i = 0; i<listModel.size(); i++) ((ConfigurationElementAbstract)listModel.get(i)).register(ml);
    }
    
    public void unRegister(Helper ml) {
        for (int i = 0; i<listModel.size(); i++) ((ConfigurationElementAbstract)listModel.get(i)).unRegister(ml);
    }
    
    public ParameterPanelAbstract[] getParameterPanels() {
        ParameterPanelAbstract[] res = new ParameterPanelAbstract[listModel.size()];
        for (int i = 0; i<res.length; i++) res[i]=((ConfigurationElementAbstract)listModel.get(i)).getParameterPanel();
        return res;
    }
}
