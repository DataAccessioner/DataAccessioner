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

import de.schlichtherle.io.swing.JFileTree;
import java.awt.Dimension;
import javax.swing.GroupLayout;
import javax.swing.JButton;
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
import javax.swing.JTree;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.tree.TreeModel;

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
    private JScrollPane srcViewSC = new JScrollPane();
    
    private JPanel dcPanel = new JPanel();
    private JLabel dcPanelLbl = new JLabel("File/Folder Dublin Core Metadata");
    private JLabel dcElementsLbl = new JLabel("Dublin Core Element");
    private JComboBox dcElementsCB = new JComboBox(MetadataManager.DC_ELEMENTS.toArray());
    private JLabel dcValueLbl = new JLabel("Metadata Value");
    private JTextArea dcValueTxtA = new JTextArea(5, 20);
    private JScrollPane dcValueSP = new JScrollPane(dcValueTxtA);
    private JButton addDCBtn = new JButton("Add New");
    private JButton rmvDCBtn = new JButton("Remove Selected");
    private JScrollPane dcEntriesSP = new JScrollPane();
    private JTable dcEntriesTbl = new JTable(4, 2);
    
    private JPanel cmdBtnPanel = new JPanel();
    private JButton migrateBtn = new JButton("Migrate");
    private JButton cancelBtn = new JButton("Cancel");
    private JButton clearSrcBtn = new JButton("Clear Source Information");
    private JButton clearAllBtn = new JButton("Clear All");
    
    private JPanel statusPanel = new JPanel();
    private JLabel statusLbl = new JLabel();
    private JProgressBar statusPB = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
    
    
    public DASwingView() {
        super();
        
        initComponents();
        
        // status bar initialization - message timeout, idle icon and busy animation, etc
        
        // connecting action tasks to status bar via TaskMonitor
        
        //File Tree Icons
        
        //Add Size & Last modified listeners
        
        //
    }
    
    //ACTIONS
    
    //Setup FITS tool menu
    
    
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
        menuBar.add(fitsMenu); //Initialized elsewhere
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
        
        JTree blankTree = new JTree();
        blankTree.setModel(null);
        srcViewSC.setViewportView(blankTree); //Add Option window pane!!
        
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
        
        //Item/Dublin Core Pane Layout
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

    }
}
