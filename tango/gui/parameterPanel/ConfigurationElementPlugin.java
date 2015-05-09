package tango.gui.parameterPanel;

import ij.IJ;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import tango.util.Utils;

public class ConfigurationElementPlugin extends ConfigurationElementAbstract {

    protected JComboBox method;
    protected ConfigurationElementPlugin template;
    protected boolean templateSet;

    public ConfigurationElementPlugin(ParameterPanelPlugin parameterPanel) {
        super(parameterPanel);
        method = new JComboBox();
        int xSize = 300;
        //if (parameterPanel instanceof MeasurementPanel) xSize = 250;
        method.setMaximumSize(new Dimension(xSize, 26));
        method.setMinimumSize(new Dimension(124, 26));
        method.setPreferredSize(new Dimension(xSize, 26));
        method.addItem("");
        for (String s : parameterPanel.getMethods()) {
            method.addItem(s);
        }
        if (parameterPanel.getMethod() != null) {
            method.setSelectedItem(parameterPanel.getMethod());
        }
        Utils.addHorizontalScrollBar(method);
        method.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setCurrentMethod();
            }
        });
        updateValidity();
    }

    public void setTemplate(ConfigurationElementPlugin template) {
        this.template = template;
        if (template != null) ((ParameterPanelPlugin) parameterPanel).setTemplate((ParameterPanelPlugin) template.parameterPanel);
        else ((ParameterPanelPlugin) parameterPanel).setTemplate(null);
        this.templateSet = true;
        updateValidity();
    }

    public void removeTemplate() {
        this.template = null;
        this.templateSet = false;
        updateValidity();
    }

    @Override
    public Color updateValidity() {
        //ij.IJ.log("edit bck color"+edit.getBackground());
        //Color col = Color.black;
        if (!parameterPanel.checkValidity()) {
            color = Color.RED;
            //edit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tango/icons/edit_error.png")));
        } else if (templateSet && ((ParameterPanelPlugin) parameterPanel).getColor()==Color.blue) {
            color = Color.BLUE;
            //edit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tango/icons/edit_diff.png")));
        } else {
            color = Color.BLACK;
            //edit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/tango/icons/edit.png")));
        }
        //edit.setBackground(col);
        return color;
    }

    @Override
    public ParameterPanelPlugin getParameterPanel() {
        return (ParameterPanelPlugin) parameterPanel;
    }

    public void setCurrentMethod() {
        ((ParameterPanelPlugin) parameterPanel).setMethod((String) method.getSelectedItem());
        if (ml != null) {
            parameterPanel.register(ml);
        }
        if (templateSet) {
            setTemplate(template);
        }
    }
    
    public JComboBox getChoice() {
        return method;
    }
    
}
