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

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.netbeans.swing.outline.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 *
 * @author Seth Shaw
 */
public class DASwingView extends javax.swing.JFrame {
    
    //For building the migration
    private DataAccessioner da;
    private Set<File> excludedFiles = new HashSet<File>();
    private Map<File,List<Pair>> fileMetadata = new HashMap<File,List<Pair>>();
    private MigrationTask migration;
    private List<String> warnings = new ArrayList<String>();
    
    //GUI Components
    private JPanel mainPanel = new JPanel();
    
    private JMenuBar menuBar = new JMenuBar();
    private JMenu fileMenu = new JMenu("File");
    private JMenuItem clearSrcMI = new JMenuItem("Clear Source Information");
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
    
    private JLabel srcIdLbl = new JLabel("Source Name/Identifier");
    private File currentSrc = null;
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
    private File dcCurrentFile = null;
    private DCTableModel dcEntriesTblModel = new DCTableModel(new ArrayList<Pair>());
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
       
    public DASwingView() {
        super();
        this.da = new DataAccessioner();
        da.startFits();
        initComponents();
    }
    
    public DASwingView(DataAccessioner da){
        super();
        this.da = da;
        initComponents();
    }
    
    private void setSource(File file){
        if (file == null){
            return;
        }
        currentSrc = file;
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
        srcOutline.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        srcOutline.setModel(mdl);
        srcOutline.setRenderDataProvider(new RenderData());
        srcOutline.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            //Modify to update the Item DC panel
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = srcOutline.getSelectedRow();
                File f = (File) srcOutline.getValueAt(row, 0);
                if (!e.getValueIsAdjusting()) {
                    updateDCPane(f);
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

    private void initComponents() {
        this.setTitle(da.getName()+" v. "+da.getVersion());
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        Dimension preferredSize = new Dimension(500,768);
        this.setPreferredSize(preferredSize);
        this.setSize(preferredSize);
        
        //Stand-alone Action Listeners
        ActionListener clearAllListen = new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearAll();
            }
        };
        clearAllBtn.addActionListener(clearAllListen);
        clearAllMI.addActionListener(clearAllListen);
        
        ActionListener clearSrcListen = new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearSource(true);
            }
        };
        clearSrcMI.addActionListener(clearSrcListen);
        clearSrcBtn.addActionListener(clearSrcListen);
        
        //Menus
        aboutMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AboutBox().setVisible(true); 
            }
        });
        fileMenu.add(aboutMI);
        fileMenu.add(new JSeparator());
        fileMenu.add(clearSrcMI);
        fileMenu.add(clearAllMI);
        fileMenu.add(new JSeparator());
        exitMI.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        
        });
        fileMenu.add(exitMI);
        menuBar.add(fileMenu);
        for (edu.harvard.hul.ois.fits.tools.Tool tool : da.getFits().getToolbelt().getTools()) {
            ItemListener toolsListener = new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    int state = e.getStateChange();
                    boolean selected = (state == ItemEvent.SELECTED);
                    JMenuItem source = (JMenuItem) e.getSource();
                    for (edu.harvard.hul.ois.fits.tools.Tool tool : da.getFits().getToolbelt().getTools()) {
                        if (source.getText().equals(tool.getName())) {
                            tool.setEnabled(selected);
                        }
                    }
                }
            };
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(tool.getName());
            item.setSelected(true);
            item.setToolTipText(tool.getName()
                    +" v. "+tool.getToolInfo().getVersion()
                    +" ("+tool.getToolInfo().getDate()
                    +") "+tool.getToolInfo().getNote());
            item.addItemListener(toolsListener);
            fitsMenu.add(item);
        }
        fitsMenu.add(new JSeparator());
        JMenuItem selectAllToolsMI = new JMenuItem("Select all");
        selectAllToolsMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(java.awt.Component comp: fitsMenu.getMenuComponents()){
                    if(comp instanceof JCheckBoxMenuItem){
                        JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) comp;
                        checkbox.setSelected(true);
                    }
                }
            }
        });
        fitsMenu.add(selectAllToolsMI);
        JMenuItem selectNoToolsMI = new JMenuItem("De-select all");
        selectNoToolsMI.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(java.awt.Component comp: fitsMenu.getMenuComponents()){
                    if(comp instanceof JCheckBoxMenuItem){
                        JCheckBoxMenuItem checkbox = (JCheckBoxMenuItem) comp;
                        checkbox.setSelected(false);
                    }
                }
            }
        });
        fitsMenu.add(selectNoToolsMI);
        menuBar.add(fitsMenu);
        setJMenuBar(menuBar);
        
        //Accession Panel
        accnDirBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setAccessionDir();
            }
        });
        
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
        srcSelectBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectSrc();
            }
        });
        excludeItmBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                excludedFiles.add(dcCurrentFile);
                srcOutline.repaint();
            }
        });
        includeItmBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                excludedFiles.remove(dcCurrentFile);
                srcOutline.repaint();
            }
        });
        srcSelectBtn.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
        excludeItmBtn.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
        includeItmBtn.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
        srcTlBr.setBorder(null);
        srcTlBr.setRollover(true);
        srcTlBr.add(srcSelectBtn);
        srcTlBr.add(excludeItmBtn);
        srcTlBr.add(includeItmBtn);
        
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
        
        //File Tree Icons
        UIManager.put("exclude.icon", new ImageIcon(getClass().getResource("/exclude.gif")));
        UIManager.put("exclude_hidden.icon", new ImageIcon(getClass().getResource("/exclude_hidden.gif")));
        UIManager.put("folder.icon", new ImageIcon(getClass().getResource("/folder.gif")));
        UIManager.put("folder_hidden.icon", new ImageIcon(getClass().getResource("/folder_hidden.gif")));
        UIManager.put("file.icon", new ImageIcon(getClass().getResource("/file.gif")));
        UIManager.put("file_hidden.icon", new ImageIcon(getClass().getResource("/file_hidden.gif")));
        
        for(Property prop: MetadataManager.DC_ELEMENTS){
            dcElementsCB.addItem(prop.getName());
        }
        //Add DC to existing item
        addDCBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String value = dcValueTxtA.getText();
                if(value == null || value.isEmpty()){ //No empties!
                    return;
                }
                
                dcEntriesTblModel.addRow(new Pair((String)dcElementsCB.getSelectedItem(),value));
                dcValueTxtA.setText(""); //Clear box for new value
            }
        });
        //Remove selected DC from existing item
        rmvDCBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dcEntriesTblModel.removePairs(dcEntriesTbl.getSelectedRows());
            }
        });
        
        
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
        migrateBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDCPane(null);
                if(migrationSanityCheck()){
                    migration = new MigrationTask();
                    migration.execute();
                } else{
                    setStatusMsg("Couldn't run the migration.");
                }
            }
        });
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (migration == null || migration.isDone()) {
                    return;
                } else {
                    migration.cancel(true);
                }
            }
        });
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
        
        pack();

    }

    private void setStatusMsg(String message) {
        statusLbl.setText(message);
    }

    public void clearAll() {
        int response = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear the source and accession information?\n"
                    + "This will remove all the Dublin Core Metadata and \n"
                    + "exclusion settings you have entered!",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            //Clear top portion
            nameTxt.setText("");
            accnNumTxt.setText("");
            accnDirTxt.setText("");
            collTitleTxt.setText("");
            //Clear Source
            clearSource(false);
        }
    }
    
    public void clearSource(boolean confirmation) {
        int response = JOptionPane.YES_OPTION; //Yes, we want to do it unless confirmed otherwise.

        if (confirmation) { //Check if we really should be getting a confirmation.
            response = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to clear the source information?\n"
                    + "This will remove all the Dublin Core Metadata and \n"
                    + "exclusion settings you have entered!",
                    "Confirm Clear Source",
                    JOptionPane.YES_NO_OPTION);
        }

        if (response == JOptionPane.YES_OPTION) {
            fileMetadata.clear();
            excludedFiles.clear();
            srcViewSC.setViewportView(null);
            srcIdTxt.setText("");
            updateDCPane(null);
            this.repaint();
        }
    }
    
    private void updateDCPane(File file){
        //Clear entry
        dcValueTxtA.setText("");
        //Save away
        fileMetadata.put(dcCurrentFile, dcEntriesTblModel.getMetadata());
        //Load new
        dcCurrentFile = file;
        List<Pair> metadata = fileMetadata.get(file);
        if(metadata == null){
            metadata = new ArrayList<Pair>();
        }
        dcEntriesTblModel = new DCTableModel(metadata);
        dcEntriesTbl.setModel(dcEntriesTblModel);
        dcEntriesSP.setViewportView(dcEntriesTbl);
    }
    
    private void selectSrc() {
            setStatusMsg("Trying to load a new disk or directory...");
            statusPB.setIndeterminate(true);
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setApproveButtonText("Select Disk/Directory to Migrate");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (!file.isDirectory()) {
                    JOptionPane.showMessageDialog(this,
                            "The selected directory is invalid.\n" +
                            "Select a valid directory.",
                            "Invalid Source Disk/Directory",
                            JOptionPane.ERROR_MESSAGE);
                    setStatusMsg("Failed to load "+file.getName());
                    statusPB.setIndeterminate(false);
                    return;
                }
                setSource(file);
                setStatusMsg(file.getName()+" is loaded.");
                statusPB.setIndeterminate(false);
            }
    }
    
    private void setAccessionDir() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setApproveButtonText("Set as Accessions Directory");
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(!file.isDirectory() || !file.canWrite()){
                JOptionPane.showMessageDialog(this,
                        "The selected accessions directory is invalid or you cannot write to it." +
                        " Select a valid directory.",
                        "Invalid Accessions Directory",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            accnDirTxt.setText(file.getAbsolutePath());
        }
    }
    
    private boolean migrationSanityCheck() {
        //Sanity Checks
        if ((accnDirTxt.getText().equalsIgnoreCase(""))
                || (nameTxt.getText().equalsIgnoreCase(""))
                || (accnNumTxt.getText().equalsIgnoreCase(""))
                || !(currentSrc.canRead())) {
            JOptionPane.showMessageDialog(this,
                    "All required fields must be entered.",
                    "Invalid Options",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        File accnDirRoot = new File(accnDirTxt.getText());
        File accnNumDir = new File(accnDirRoot, accnNumTxt.getText());
        if (currentSrc.getAbsolutePath().equalsIgnoreCase(accnNumDir.getAbsolutePath())) {
            JOptionPane.showMessageDialog(this,
                    "The destination cannot be the same as the source.",
                    "Invalid Options",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (srcIdTxt.getText().equals("")) {
            JOptionPane.showMessageDialog(this,
                    "Source Name/Identifier cannot be empty",
                    "Metadata Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (new File(accnNumDir, srcIdTxt.getText()).exists()) {
            JOptionPane.showMessageDialog(this,
                    "A source with the name or identifier " + srcIdTxt.getText()
                    + " already exists.",
                    "Metadata Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        //Characters that don't play well with various file systems
        String[] illegalCharacters = {
            "<", ">", ":", "\"", "/", "\\", "|", "?", "*", ".", "!", "½"
        };
        for (String badCharacter : illegalCharacters) {
            if (srcIdTxt.getText().contains(badCharacter)) {
                JOptionPane.showMessageDialog(this,
                        "You may not use any of the following characters"
                        + " in the source name/identifier:\n"
                        + "< > : \" / \\ | ? . ! * ½",
                        "Illegal Characters in Source Name/Identifier",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        //These are protected names in Windows
        String[] illegalNames = {
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8",
            "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8",
            "lpt9",
            "con", "nul", "prn"
        };
        for (String badName : illegalNames) {
            if (srcIdTxt.getText().equals(badName)) {
                JOptionPane.showMessageDialog(this,
                        "You may not use any of the following names "
                        + "as the source name/identifier: \n"
                        + "com1, com2, com3, com4, com5, com6, com7, com8, com9, \n"
                        + "lpt1, lpt2, lpt3, lpt4, lpt5, lpt6, lpt7, lpt8, lpt9, \n"
                        + "con, nul, and prn.",
                        "Illegal Characters in Source Name/Identifier",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        if (!currentSrc.canRead()) {
            JOptionPane.showMessageDialog(this,
                    "The source cannot be read, or has been removed or deleted.\n"
                    + " Please re-attach the source (e.g. insert disk) or cancel migration.",
                    "Disk Missing",
                    JOptionPane.ERROR_MESSAGE);
        }

        return true; //If you got this far, you pass.
    }

    private void clearCancel(File destination) {
        int answer = JOptionPane.showOptionDialog(this,
                "You cancelled the migration before it completed.\n"
                + "Would you like to delete the transferred files? ",
                "Migration Cancelled",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                null,
                null);
        if (answer == 0) {
            //Delete destination
            if (!deleteRecursively(destination)) {
                JOptionPane.showMessageDialog(this,
                        "Unable to delete " + destination.getAbsolutePath(),
                        "Error deleting destination",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        setStatusMsg("Migration Cancelled!");
    }

    protected static boolean deleteRecursively(File toDelete) {
        if (toDelete.isDirectory()) {
            for (File child : toDelete.listFiles()) {
                if (!deleteRecursively(child)) {
                    return false;
                }
            }
        }
        return toDelete.delete();
    }

    private void displayWarnings(){
        int answer = JOptionPane.showOptionDialog(this,
                        "One or more errors occurred during migration.\n" +
                        "Details will be listed in the accession " +
                        "metadata file.\n" +
                        "Would you like to see a list of the errors now?",
                        "Migration Errors Occurred",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        null,
                        null);
                if (answer == 0) {
                    Thread errorDisplayThread = new Thread(new Runnable() {

                        JFrame errorFrame;

                        public void run() {
                            errorFrame = new JFrame("Migration Errors");
                            JPanel errorPanel = new JPanel(new BorderLayout());
                            errorFrame.add(errorPanel);
                            JTextArea errorText = new JTextArea();
                            JScrollPane errorScroll = new JScrollPane(errorText);
                            errorScroll.setPreferredSize(new Dimension(450, 110));
                            errorScroll.setMaximumSize(errorFrame.getMaximumSize());
                            errorPanel.add(errorScroll, "Center");
                            errorFrame.pack();
                            errorFrame.setVisible(true);
                            errorFrame.toFront();
                            for (String warning : warnings) {
                                errorText.append(warning + "\n");
                            }

                        }
                    });
                    errorDisplayThread.setName("ErrorFrame");
                    errorDisplayThread.run();
                }
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
            File f = (File) o;
            if (excludedFiles.contains(f)){
                if(f.isHidden()){
                    return UIManager.getIcon("exclude_hidden.icon");
                } else {
                    return UIManager.getIcon("exclude.icon");
                }
            } else if (f.isDirectory()) {
                if(f.isHidden()){
                    return UIManager.getIcon("folder_hidden.icon");
                } else {
                    return UIManager.getIcon("folder.icon");
                }
            } else {
                if(f.isHidden()){
                    return UIManager.getIcon("file_hidden.icon");
                } else {
                    return UIManager.getIcon("file.icon");
                }
            }
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

    private class DCTableModel extends AbstractTableModel {
        private String[] columnNames = {"Element","Value"};
        private List<Pair> metadata;

        public DCTableModel(List<Pair> listOfPairs) {
            if(listOfPairs == null){
                metadata = new ArrayList<Pair>();
            } else {
                metadata = listOfPairs;
            }
        }
        
        @Override
        public int getRowCount() {
            return metadata.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return metadata.get(rowIndex).getValueAt(columnIndex);
        }
        
        public void addRow(Pair pair){
            metadata.add(pair);
            int row = metadata.indexOf(pair);
            fireTableCellUpdated(row, 0);
            fireTableCellUpdated(row, 1);
            fireTableRowsInserted(row, row);
        }
        
        public void removeRow(int row) {
            metadata.remove(row);
            this.fireTableDataChanged();
        }
  
        @Override
        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        private List<Pair> getMetadata() {
            return metadata;
        }

        private void removePairs(int[] selectedRows) {
            //Need to collect all the pairs before attempting to remove them
            //because the indicies may not match after the first removal.
            List<Pair> toRemove = new ArrayList<Pair>();
            for(int row: selectedRows){
                toRemove.add(metadata.get(row));
            }
            //Now remove them one by one, using the *current* index
            for(Pair pair: toRemove){
                removeRow(metadata.indexOf(pair));
            }
        }
    }
    class Pair {
        private final String element;
        private final String value;
        
        public Pair(String anElement, String aValue){
            element = anElement;
            value = aValue;
        }
        
        public String element(){return element;}
        public String value()  {return value;}
        public Object getValueAt(int index){
            switch (index){
                case 0: return element;
                case 1: return value;
                default: return null;
            }
        }
    }
    
    class MigrationTask extends SwingWorker<String, Object> implements ActionListener{

        Timer timer;
        Migrator migrator = da.getMigrator();
        File destination;
        
        @Override
        protected String doInBackground() throws Exception {
            HashMap<String, String> daSwingMetadata = new HashMap<>();
            setStatusMsg("Preparing migration ...");
            statusPB.setIndeterminate(true);
            enable(false);
            //Run migrator
            timer = new Timer(500, this); //Prod every half second
            timer.start();
            File accnRoot = new File(accnDirTxt.getText());
            File accnDir = new File(accnRoot, accnNumTxt.getText());
            File accnMetadataFile = new File(accnRoot, accnNumTxt.getText()+".xml");
            destination = new File(accnDir, srcIdTxt.getText());
            destination.mkdirs();
            daSwingMetadata.put("collectionName", collTitleTxt.getText());
            daSwingMetadata.put("accessionNumber", accnNumTxt.getText());
            daSwingMetadata.put("submitterName", nameTxt.getText());
            MetadataManager mm = new MetadataManager(accnMetadataFile, daSwingMetadata );
            for(File annotatedFile: fileMetadata.keySet()){
                if(annotatedFile == null){ //Oddly, the first key is always null
                    continue;
                }
                Metadata annotations = new Metadata();
                for(Pair pair: fileMetadata.get(annotatedFile)){
                    annotations.add(pair.element, pair.value());
                }
                if(annotations.size() >0){
                    //Not all annotatedFile listings have entries... TODO FIX in updateDC rather than here
                    mm.setFileAnnotation(annotatedFile, annotations);
                }
            }
            try{
                da.run(currentSrc, destination, mm, excludedFiles);
            }
            catch(Exception e){
                cancelled();
                setStatusMsg("ERROR! "+e.getLocalizedMessage());
                return "FAIL!";
            }
            finished();

            setStatusMsg("Success!");
            return "Success!";
        }

        protected void cancelled() {
            finished();
            clearCancel(destination);
        }
        
        protected void finished() {
            timer.stop();
            enable(true);
            statusPB.setIndeterminate(false);
            clearSource(false);
            warnings = migrator.getWarnings();
            if (warnings.size() > 0) {
                displayWarnings();
            }
            migrator.getWarnings().clear();
        }

        public void actionPerformed(ActionEvent e) {
            if (migrator != null) {
                //Eventually get a new progress monitor update
                setStatusMsg(migrator.getStatusMessage());
            }
        }

        protected void enable(boolean enable) {
            accnDirBtn.setEnabled(enable);
            migrateBtn.setEnabled(enable);
            clearAllBtn.setEnabled(enable);
            clearAllMI.setEnabled(enable);
            clearSrcBtn.setEnabled(enable);
            clearSrcMI.setEnabled(enable);
            cancelBtn.setEnabled(!enable); //Opposite the rest
        }
    }

    public class AboutBox extends javax.swing.JFrame {

        private javax.swing.JLabel devLbl;
        private javax.swing.JLabel devVal;
        private javax.swing.JLabel title;
        private javax.swing.JLabel urlLbl;
        private javax.swing.JLabel urlVal;
        private javax.swing.JLabel versionLbl;
        private javax.swing.JLabel versionVal;

        public AboutBox() {
            initComponents();
        }

        public void closeAboutBox() {
            setVisible(false);
        }

        private void initComponents() {
        title = new javax.swing.JLabel();
        versionLbl = new javax.swing.JLabel();
        devLbl = new javax.swing.JLabel();
        urlLbl = new javax.swing.JLabel();
        versionVal = new javax.swing.JLabel();
        devVal = new javax.swing.JLabel();
        urlVal = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setTitle(da.getName()+" v. "+da.getVersion());

        title.setFont(title.getFont().deriveFont(title.getFont().getStyle() | java.awt.Font.BOLD, title.getFont().getSize()+9));
        title.setText(da.getName());

        versionLbl.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        versionLbl.setText("Version:");

        devLbl.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        devLbl.setText("Developer:");

        urlLbl.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        urlLbl.setText("Website:");

        versionVal.setText(da.getVersion());

        devVal.setText("Seth Shaw");

        urlVal.setText("http://www.dataaccessioner.org");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(title)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(devLbl)
                            .addComponent(versionLbl)
                            .addComponent(urlLbl))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(versionVal)
                            .addComponent(devVal)
                            .addComponent(urlVal))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(title)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(versionLbl)
                    .addComponent(versionVal))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(devLbl)
                    .addComponent(devVal))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(urlLbl)
                    .addComponent(urlVal))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        pack();
        }

    }
}
