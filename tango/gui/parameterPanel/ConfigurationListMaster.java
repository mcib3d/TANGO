/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.gui.parameterPanel;

import ij.IJ;
import java.awt.FlowLayout;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import tango.gui.PanelDisplayer;

/**
 *
 * @author jollion
 */
public class ConfigurationListMaster {
    PanelDisplayer pd;
    JPanel choicePanel;
    JComboBox currentJCB;
    ConfigurationList currentCL;
    
    public ConfigurationListMaster(PanelDisplayer pd, JPanel choicePanel) {
        this.pd = pd;
        this.choicePanel=choicePanel;
    }
    
    
    public void flush() {
        pd.hidePanel();
        this.currentCL=null;
        this.currentJCB=null;
        if (choicePanel!=null) this.choicePanel.removeAll();
    }
    
    public void showConfigurationPanel(ConfigurationElementAbstract c, ConfigurationList list) {
        pd.hidePanel();
        if (currentCL!=null && !list.equals(currentCL)) {
            IJ.log("new != current CL");
            currentCL.editOff();
            currentCL.updateValidity();
        }
        this.currentCL = list;
        c.getParameterPanel().setDisplayer(pd);
        c.getParameterPanel().refreshDisplay();
        if (c instanceof ConfigurationElementPlugin) {
            currentJCB = ((ConfigurationElementPlugin)c).getChoice();
            if (choicePanel==null) {
                Box box = Box.createVerticalBox();
                box.add(currentJCB);
                box.add(c.getParameterPanel().getPanel());
                JPanel panel = new JPanel(new FlowLayout());
                panel.add(box);
                pd.showPanel(panel);
            } else {
                choicePanel.removeAll();
                choicePanel.add(currentJCB);
                choicePanel.repaint();
                choicePanel.revalidate();
                pd.showPanel(c.getParameterPanel().getPanel());
            }
        } else pd.showPanel(c.getParameterPanel().getPanel());
    }
    
    public void hideConfigurationPanel(boolean editOff) {
        IJ.log("hide configuration panel");
        pd.hidePanel();
        if (currentJCB!=null && choicePanel!=null) choicePanel.remove(currentJCB);
        if (editOff && this.currentCL!=null) {
            IJ.log("hide panel & edit off");
            currentCL.editOff();
        }
    }
}
