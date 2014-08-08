/*
 * Copyright (C) 2014 Seth Shaw.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package org.dataaccessioner;

import java.awt.Dimension;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreeModel;
import org.apache.tika.metadata.Property;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.netbeans.swing.outline.RenderDataProvider;
import org.netbeans.swing.outline.RowModel;

/**
 *
 * @author Seth Shaw
 */
public class DASwingView extends javax.swing.JFrame {
    
    //Components
    private JPanel mainPanel = new JPanel();
    
    private JMenuBar menuBar = new JMenuBar();
    private JMenu fileMenu = new JMenu("File");
    private JMenuItem clearSourceMI = new JMenuItem("Clear Source Information");
    private JMenuItem clearAllMI = new JMenuItem("Clear All");
    private JMenuItem aboutMI = new JMenuItem("About...");
    private JMenuItem exitMI = new JMenuItem("Exit");
    
    private JMenu fitsMenu = new JMenu("FITS Tools");
    
    private JPanel accnPanel = new JPanel();
    private JLabel nameLbl = new JLabel("Your Name");
    private JTextField nameTxt = new JTextField();
    private JLabel accnNumLbl = new JLabel("Accession Number");
    private JTextField accnNumTxt = new JTextField();
    private JLabel collTitleLbl = new JLabel("Collection Title");
    private JTextField collTitleTxt = new JTextField();
    private JButton accnDirBtn = new JButton("Accession to Directory");
    private JTextField accnDirTxt = new JTextField();
        
    private JSplitPane srcItmSP = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    
    private JPanel srcPanel = new JPanel();
    private JToolBar srcTlBr = new JToolBar(JToolBar.HORIZONTAL);
    private JButton srcSelectBtn = new JButton("Source/Directory");
    private JButton excludeItmBtn = new JButton("Exclude");
    private JButton includeItmBtn = new JButton("Include");
    private JSeparator srcTlBrDiv = new JSeparator(SwingConstants.VERTICAL);
    private JLabel srcTlBrLbl = new JLabel("Display:");
    private JToggleButton dispItmSizeTBtn = new JToggleButton("Size");
    private JToggleButton dispItmModTBtn = new JToggleButton("Last Modified");
    
    private JLabel srcIdLbl = new JLabel("Source Name/Identifier");
    private JTextField srcIdTxt = new JTextField();
    private Outline srcOutline = new Outline();
    private JScrollPane srcViewSC = new JScrollPane(srcOutline);
    
    private JPanel dcPanel = new JPanel();
    private JLabel dcPanelLbl = new JLabel("File/Folder Dublin Core Metadata");
    private JLabel dcElementsLbl = new JLabel("Dublin Core Element");
    private JComboBox dcElementsCB = new JComboBox();
    private JLabel dcValueLbl = new JLabel("Metadata Value");
    private JTextArea dcValueTxtA = new JTextArea(5, 20);
    private JScrollPane dcValueSP = new JScrollPane(dcValueTxtA);
    private JButton addDCBtn = new JButton("Add New");
    private JButton rmvDCBtn = new JButton("Remove Selected");
    private final String[] colHeadings = {"Element","Value"};
    private DefaultTableModel dcEntriesTblModel = new DefaultTableModel(colHeadings, 0);
    private JTable dcEntriesTbl = new JTable(dcEntriesTblModel);
    private JScrollPane dcEntriesSP = new JScrollPane(dcEntriesTbl);
    
    private JPanel cmdBtnPanel = new JPanel();
    private JButton migrateBtn = new JButton("Migrate");
    private JButton cancelBtn = new JButton("Cancel");
    private JButton clearSrcBtn = new JButton("Clear Source Information");
    private JButton clearAllBtn = new JButton("Clear All");
    
    private JPanel statusPanel = new JPanel();
    private JLabel statusLbl = new JLabel();
    private JProgressBar statusPB = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
    
    private DataAccessioner da;
    
    public DASwingView() {
        super();
        this.da = new DataAccessioner();
        da.startFits();
        initComponents();
        //TEST Set Source
        setSource(new File("C:\\Users\\sshaw6\\Dropbox\\Family"));
    }
    
    public DASwingView(DataAccessioner da){
        super();
        this.da = da;
        initComponents();
    }
    
    //ACTIONS
        //Migrate
        //Cancel
        //Clear Source
        //Clear All
        //Exit
        //About
        //(Un)select tool from menu
        //Add DC to existing item
            // Best strategy right now is to maintain the model & actual data separately. Creating a custom model was getting too unweildy
        //Remove selected DC from existing item
        //Update Item Pane when tree node is selected
        // Select accession directory 
        // Select source to migrate
    private void setSource(File file){
        srcIdTxt.setText(file.getName());
        /**
         * Thanks to both the posters @
         * http://stackoverflow.com/questions/2841183/access-tree-object-in-netbeans-outline/2841582
         * 
         * & Geertjan Wielenga @
         * https://blogs.oracle.com/geertjan/entry/swing_outline_component
         */
        TreeModel srcTreeMdl = new FileTreeModel(file);
        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(
                srcTreeMdl, new FileRowModel(), true, file.getName());

        srcOutline = new Outline();
        srcOutline.setRootVisible(true);
        srcOutline.setModel(mdl);
        srcOutline.setRenderDataProvider(new RenderData());
        srcOutline.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            //Modify to update the Item DC panel
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = srcOutline.getSelectedRow();
                File f = (File) srcOutline.getValueAt(row, 0);
                if (!e.getValueIsAdjusting()) {
                    System.out.println(row + ": " + f);
                }
            }
        });
        srcViewSC.setViewportView(srcOutline);
    }
        //Add Fits menu item tool tip (to rid us of null info)
    
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DASwingView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DASwingView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DASwingView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DASwingView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DASwingView().setVisible(true);
            }
        });
    }

    private void initComponents() { //ADD Actions!!
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        Dimension preferredSize = new Dimension(450,768);
        this.setPreferredSize(preferredSize);
        this.setSize(preferredSize);
        
        //Menus
        fileMenu.add(aboutMI);
        fileMenu.add(new JSeparator());
        fileMenu.add(clearSourceMI);
        fileMenu.add(clearAllMI);
        fileMenu.add(new JSeparator());
        fileMenu.add(exitMI);
        menuBar.add(fileMenu);
        for(edu.harvard.hul.ois.fits.tools.Tool tool: da.getFits().getToolbelt().getTools()){
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(tool.getName());
            item.setSelected(true);
            item.setToolTipText(tool.getName()
                    +" v. "+tool.getToolInfo().getVersion()
                    +" ("+tool.getToolInfo().getDate()
                    +") "+tool.getToolInfo().getNote());
            fitsMenu.add(item);
        }
        menuBar.add(fitsMenu);
        setJMenuBar(menuBar);
        
        //Accession Panel
        GroupLayout accnLayout = new GroupLayout(accnPanel);
        accnPanel.setLayout(accnLayout);
        accnLayout.setHorizontalGroup(accnLayout.createSequentialGroup()
                .addGroup(accnLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(nameLbl)
                        .addComponent(accnNumLbl)
                        .addComponent(collTitleLbl)
                        .addComponent(accnDirBtn))
                .addGroup(accnLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(nameTxt)
                        .addComponent(accnNumTxt)
                        .addComponent(collTitleTxt)
                        .addComponent(accnDirTxt)
                )
        );
        accnLayout.setVerticalGroup(accnLayout.createSequentialGroup()
                .addGroup(accnLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(nameLbl)
                        .addComponent(nameTxt))
                .addGroup(accnLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(accnNumLbl)
                        .addComponent(accnNumTxt))
                .addGroup(accnLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(collTitleLbl)
                        .addComponent(collTitleTxt))
                .addGroup(accnLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(accnDirBtn)
                        .addComponent(accnDirTxt))
        );
               
        //Source Pane w/ Layout
        srcSelectBtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        excludeItmBtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        includeItmBtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        dispItmSizeTBtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        dispItmModTBtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        srcTlBr.setBorder(null);
        srcTlBr.setRollover(true);
        srcTlBr.add(srcSelectBtn);
        srcTlBr.add(excludeItmBtn);
        srcTlBr.add(includeItmBtn);
        srcTlBr.add(srcTlBrDiv);
        srcTlBr.add(srcTlBrLbl);
        srcTlBr.add(dispItmSizeTBtn);
        srcTlBr.add(dispItmModTBtn);
        
//        JTree blankTree = new JTree();
//        blankTree.setModel(null);
//        srcViewSC.setViewportView(blankTree); //Add Option window pane!!
        
        GroupLayout srcLayout = new GroupLayout(srcPanel);
        srcPanel.setLayout(srcLayout);
        srcLayout.setHorizontalGroup(srcLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(srcTlBr, GroupLayout.DEFAULT_SIZE, 428, Short.MAX_VALUE)
                .addGroup(srcLayout.createSequentialGroup()
                        .addComponent(srcIdLbl)
                        .addComponent(srcIdTxt)
                )
                .addComponent(srcViewSC)
        );
        srcLayout.setVerticalGroup(srcLayout.createSequentialGroup()
                .addComponent(srcTlBr, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(srcLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(srcIdLbl)
                        .addComponent(srcIdTxt))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(srcViewSC));
        
        //Item/Dublin Core Pane & Layout
        for(Property prop: MetadataManager.DC_ELEMENTS){
            dcElementsCB.addItem(prop.getName());
        }
        
        GroupLayout dcLayout = new GroupLayout(dcPanel);
        dcPanel.setLayout(dcLayout);
        dcLayout.setHorizontalGroup(dcLayout.createParallelGroup()
                .addComponent(dcPanelLbl)
                .addGroup(dcLayout.createSequentialGroup()
                        .addGroup(dcLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addComponent(dcElementsLbl)
                                .addComponent(dcValueLbl))
                        .addGroup(dcLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(dcElementsCB)
                                .addComponent(dcValueSP)))
                .addGroup(dcLayout.createSequentialGroup()
                        .addComponent(addDCBtn)
                        .addComponent(rmvDCBtn))
                .addComponent(dcEntriesSP)
        );
        dcLayout.setVerticalGroup(dcLayout.createSequentialGroup()
                .addComponent(dcPanelLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(dcLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(dcElementsLbl)
                        .addComponent(dcElementsCB))
                .addGroup(dcLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(dcValueLbl)
                        .addComponent(dcValueSP, GroupLayout.PREFERRED_SIZE, 63, GroupLayout.PREFERRED_SIZE))
                .addGroup(dcLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(addDCBtn)
                        .addComponent(rmvDCBtn))
                .addComponent(dcEntriesSP)
        );

        
        //Source/Item Split Pane (variable size, expand to fill)
        srcItmSP.setTopComponent(srcPanel);
        srcItmSP.setBottomComponent(dcPanel);
        srcItmSP.setDividerLocation(300);
        srcItmSP.setOrientation(JSplitPane.VERTICAL_SPLIT);
        
        //Command Button Pane (fixed size, stuck to bottom)
        GroupLayout cmdLayout = new GroupLayout(cmdBtnPanel);
        cmdBtnPanel.setLayout(cmdLayout);
        cmdLayout.setHorizontalGroup(cmdLayout.createSequentialGroup()
                .addComponent(migrateBtn)
                .addComponent(cancelBtn)
                .addComponent(clearSrcBtn)
                .addComponent(clearAllBtn)
        );
        cmdLayout.setVerticalGroup(cmdLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(migrateBtn)
                .addComponent(cancelBtn)
                .addComponent(clearSrcBtn)
                .addComponent(clearAllBtn)
        );
        
        //Status Pane (fixed size, stuck to bottom)
        GroupLayout statusLayout = new GroupLayout(statusPanel);
        statusPanel.setLayout(statusLayout);
        statusLayout.setHorizontalGroup(statusLayout.createSequentialGroup()
                .addComponent(statusLbl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusPB, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );
        statusLayout.setVerticalGroup(statusLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(statusLbl, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(statusPB)
        );
        
        //Master Layout
        GroupLayout mainLayout = new GroupLayout(getContentPane());
        getContentPane().setLayout(mainLayout);
        mainLayout.setHorizontalGroup(mainLayout.createParallelGroup()
                .addComponent(accnPanel)
                .addComponent(srcItmSP)
                .addComponent(cmdBtnPanel)
                .addComponent(statusPanel)
        );
        mainLayout.setVerticalGroup(mainLayout.createSequentialGroup()
                .addComponent(accnPanel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(srcItmSP)
                .addComponent(cmdBtnPanel)
                .addComponent(statusPanel)
        );
        
        // status bar initialization - message timeout, idle icon and busy animation, etc
        
        // connecting action tasks to status bar via TaskMonitor
        
        //File Tree Icons
        
        //Add Size & Last modified listeners
        
        pack();

    }

    private void setStatusMsg(String message) {
        statusLbl.setText(message);
    }

    private static class FileTreeModel implements TreeModel {

        private File root;

        public FileTreeModel(File root) {
            this.root = root;
        }

        @Override
        public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
            //do nothing
        }

        @Override
        public Object getChild(Object parent, int index) {
            File f = (File) parent;
            return f.listFiles()[index];
        }

        @Override
        public int getChildCount(Object parent) {
            File f = (File) parent;
            if (!f.isDirectory()) {
                return 0;
            } else {
                return f.list().length;
            }
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            File par = (File) parent;
            File ch = (File) child;
            return Arrays.asList(par.listFiles()).indexOf(ch);
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public boolean isLeaf(Object node) {
            File f = (File) node;
            return !f.isDirectory();
        }

        @Override
        public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
            //do nothing
        }

        @Override
        public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
            //do nothing
        }

    }
 
    private class FileRowModel implements RowModel {

        @Override
        public Class getColumnClass(int column) {
            switch (column) {
                case 0:
                    return Date.class;
                case 1:
                    return Long.class;
                default:
                    assert false;
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Date" : "Size (bytes)";
        }

        @Override
        public Object getValueFor(Object node, int column) {
            File f = (File) node;
            switch (column) {
                case 0:
                    return new Date(f.lastModified()); //UPDATE to ISO date/time display
                case 1:
                    return new Long(f.length()); //Possible UPDATE to pretty file sizes?
                default:
                    assert false;
            }
            return null;
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return false;
        }

        @Override
        public void setValueFor(Object node, int column, Object value) {
            //do nothing for now
        }

    }

    private class RenderData implements RenderDataProvider {

        @Override
        public java.awt.Color getBackground(Object o) {
            return null;
        }

        @Override
        public String getDisplayName(Object o) {
            return ((File) o).getName();
        }

        @Override
        public java.awt.Color getForeground(Object o) {
            File f = (File) o;
            if (!f.isDirectory() && !f.canWrite()) {
                return UIManager.getColor("controlShadow");
            }
            return null;
        }

        @Override
        public javax.swing.Icon getIcon(Object o) {
            return null;

        }

        @Override
        public String getTooltipText(Object o) {
            File f = (File) o;
            return f.getAbsolutePath();
        }

        @Override
        public boolean isHtmlDisplayName(Object o) {
            return false;
        }

    }
}
