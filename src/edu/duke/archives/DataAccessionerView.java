/*
 * DataAccessionerView.java
 */
package edu.duke.archives;

import edu.duke.archives.interfaces.Adapter;
import edu.duke.archives.interfaces.MetadataManager;
import edu.duke.archives.metadata.FileWrapper;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.Task;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ButtonGroup;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * The application's main frame.
 */
public class DataAccessionerView extends FrameView {

    private JRadioButtonMenuItem rbMenuItem;

    public DataAccessionerView(SingleFrameApplication app) {
        super(app);

        this.getFrame().setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().
                getResource("resources/disk.gif")));
        initComponents();

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate =
                resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        //File Tree Icons
        excludeIcon = resourceMap.getIcon("exclude.icon");
        fileIcon = resourceMap.getIcon("page.icon");
        folderIcon = resourceMap.getIcon("folder.icon");
        excludeHiddenIcon = resourceMap.getIcon("exclude_hidden.icon");
        fileHiddenIcon = resourceMap.getIcon("page_hidden.icon");
        folderHiddenIcon = resourceMap.getIcon("folder_hidden.icon");

        //Display Size Toggle
        displaySizeBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    displaySize = true;
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    displaySize = false;
                }
                fileTree.repaint();
            }
        });
        lastModBtn.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    displayLastModified = true;
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    displayLastModified = false;
                }
                fileTree.repaint();
            }
        });
    
        enable(true);
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = DataAccessionerApp.getApplication().getMainFrame();
            aboutBox = new DataAccessionerAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        DataAccessionerApp.getApplication().show(aboutBox);
    }

    void setMigrator(DataAccessioner migrator) {
        this.migrator = migrator;
        
        //FileWrapper Managers
        mmMenu.removeAll();
        MetadataManagerMenuActionListener mmmal =
                new MetadataManagerMenuActionListener();
        ButtonGroup group = new ButtonGroup();
        for (MetadataManager mm : migrator.getAvailableManagers()) {
            rbMenuItem = new JRadioButtonMenuItem(mm.getName());
            try {
                if (mm.equals(migrator.getMetadataManager())) {
                    rbMenuItem.setSelected(true);
                }
            } catch (Exception ex) {
                Logger.getLogger(DataAccessionerView.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
            group.add(rbMenuItem);
            rbMenuItem.addActionListener(mmmal);
            mmMenu.add(rbMenuItem);
        }
        
        //Adapters
        JCheckBoxMenuItem cbmi = null;
        adaptersMenu.removeAll();
        AdapterMenuActionListener amal = new AdapterMenuActionListener();
        for(Adapter adapter : migrator.getAvailableAdapters()){
            cbmi = new JCheckBoxMenuItem(adapter.getName());
            if(migrator.getSelectedAdapters().contains(adapter)){
                cbmi.setSelected(true);
            }
            cbmi.addActionListener(amal);
            adaptersMenu.add(cbmi);
        }
    }

    private FileWrapper getSourceMetadata() {
        return (FileWrapper) fileTree.getModel().getRoot();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        accessionInfo = new javax.swing.JPanel();
        nameLbl = new javax.swing.JLabel();
        accnNoLbl = new javax.swing.JLabel();
        collTitleLbl = new javax.swing.JLabel();
        accnDirBtn = new javax.swing.JButton();
        name = new javax.swing.JTextField();
        accnNo = new javax.swing.JTextField();
        collTitle = new javax.swing.JTextField();
        accnDir = new javax.swing.JTextField();
        treePanel = new javax.swing.JPanel();
        treeToolBar = new javax.swing.JToolBar();
        loadSrcBtn = new javax.swing.JButton();
        separator1 = new javax.swing.JToolBar.Separator();
        excludeBtn = new javax.swing.JButton();
        includeBtn = new javax.swing.JButton();
        separator2 = new javax.swing.JToolBar.Separator();
        displayLabel = new javax.swing.JLabel();
        displaySizeBtn = new javax.swing.JToggleButton();
        lastModBtn = new javax.swing.JToggleButton();
        treeSP = new javax.swing.JScrollPane();
        fileTree = new javax.swing.JTree();
        buttonPanel = new javax.swing.JPanel();
        migrateBtn = new javax.swing.JButton();
        cancelBtn = new javax.swing.JButton();
        resetBtn = new javax.swing.JButton();
        resetAllBtn = new javax.swing.JButton();
        dataMetadata = new javax.swing.JPanel();
        diskNameLbl = new javax.swing.JLabel();
        diskName = new javax.swing.JTextField();
        diskNameBtn = new javax.swing.JButton();
        metadataTabs = new javax.swing.JTabbedPane();
        diskLabelSP = new javax.swing.JScrollPane();
        diskLabel = new javax.swing.JTextArea();
        notesSP = new javax.swing.JScrollPane();
        notes = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        adaptersMenu = new javax.swing.JMenu();
        mmMenu = new javax.swing.JMenu();
        mmDefaultMenuItem = new javax.swing.JRadioButtonMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("mainPanel"); // NOI18N

        accessionInfo.setName("accessionInfo"); // NOI18N

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(edu.duke.archives.DataAccessionerApp.class).getContext().getResourceMap(DataAccessionerView.class);
        nameLbl.setText(resourceMap.getString("nameLbl.text")); // NOI18N
        nameLbl.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        nameLbl.setName("nameLbl"); // NOI18N

        accnNoLbl.setText(resourceMap.getString("accnNoLbl.text")); // NOI18N
        accnNoLbl.setName("accnNoLbl"); // NOI18N

        collTitleLbl.setText(resourceMap.getString("collTitleLbl.text")); // NOI18N
        collTitleLbl.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        collTitleLbl.setName("collTitleLbl"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(edu.duke.archives.DataAccessionerApp.class).getContext().getActionMap(DataAccessionerView.class, this);
        accnDirBtn.setAction(actionMap.get("setAccessionDir")); // NOI18N
        accnDirBtn.setText(resourceMap.getString("accnDirBtn.text")); // NOI18N
        accnDirBtn.setName("accnDirBtn"); // NOI18N

        name.setName("name"); // NOI18N

        accnNo.setName("accnNo"); // NOI18N

        collTitle.setName("collTitle"); // NOI18N

        accnDir.setName("accnDir"); // NOI18N

        org.jdesktop.layout.GroupLayout accessionInfoLayout = new org.jdesktop.layout.GroupLayout(accessionInfo);
        accessionInfo.setLayout(accessionInfoLayout);
        accessionInfoLayout.setHorizontalGroup(
            accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(accessionInfoLayout.createSequentialGroup()
                .addContainerGap()
                .add(accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(collTitleLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 71, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(accnNoLbl)
                    .add(nameLbl)
                    .add(accnDirBtn))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(accnNo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                    .add(collTitle, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                    .add(name, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                    .add(accnDir, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE))
                .addContainerGap())
        );
        accessionInfoLayout.setVerticalGroup(
            accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(accessionInfoLayout.createSequentialGroup()
                .add(accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(name, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(nameLbl))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(accnNo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(accnNoLbl))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(collTitle, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(collTitleLbl))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(accessionInfoLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(accnDirBtn)
                    .add(accnDir, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );

        treePanel.setName("treePanel"); // NOI18N

        treeToolBar.setFloatable(false);
        treeToolBar.setRollover(true);
        treeToolBar.setName("treeToolBar"); // NOI18N

        loadSrcBtn.setAction(actionMap.get("loadSrcDir")); // NOI18N
        loadSrcBtn.setText(resourceMap.getString("loadSrcBtn.text")); // NOI18N
        loadSrcBtn.setFocusable(false);
        loadSrcBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        loadSrcBtn.setName("loadSrcBtn"); // NOI18N
        loadSrcBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        treeToolBar.add(loadSrcBtn);

        separator1.setName("separator1"); // NOI18N
        treeToolBar.add(separator1);

        excludeBtn.setAction(actionMap.get("excludeItem")); // NOI18N
        excludeBtn.setFocusable(false);
        excludeBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        excludeBtn.setName("excludeBtn"); // NOI18N
        excludeBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        treeToolBar.add(excludeBtn);

        includeBtn.setAction(actionMap.get("includeItem")); // NOI18N
        includeBtn.setFocusable(false);
        includeBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        includeBtn.setName("includeBtn"); // NOI18N
        includeBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        treeToolBar.add(includeBtn);

        separator2.setName("separator2"); // NOI18N
        treeToolBar.add(separator2);

        displayLabel.setText(resourceMap.getString("displayLabel.text")); // NOI18N
        displayLabel.setName("displayLabel"); // NOI18N
        treeToolBar.add(displayLabel);

        displaySizeBtn.setText(resourceMap.getString("displaySizeBtn.text")); // NOI18N
        displaySizeBtn.setFocusable(false);
        displaySizeBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        displaySizeBtn.setName("displaySizeBtn"); // NOI18N
        displaySizeBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        treeToolBar.add(displaySizeBtn);

        lastModBtn.setText(resourceMap.getString("lastModBtn.text")); // NOI18N
        lastModBtn.setFocusable(false);
        lastModBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lastModBtn.setName("lastModBtn"); // NOI18N
        lastModBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        treeToolBar.add(lastModBtn);

        treeSP.setName("treeSP"); // NOI18N

        fileTree.setModel(null);
        fileTree.setName("fileTree"); // NOI18N
        treeSP.setViewportView(fileTree);

        org.jdesktop.layout.GroupLayout treePanelLayout = new org.jdesktop.layout.GroupLayout(treePanel);
        treePanel.setLayout(treePanelLayout);
        treePanelLayout.setHorizontalGroup(
            treePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, treeToolBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
            .add(treeSP, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
        );
        treePanelLayout.setVerticalGroup(
            treePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(treePanelLayout.createSequentialGroup()
                .add(treeToolBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(treeSP, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE))
        );

        buttonPanel.setName("buttonPanel"); // NOI18N

        migrateBtn.setAction(actionMap.get("migrate")); // NOI18N
        migrateBtn.setText(resourceMap.getString("migrateBtn.text")); // NOI18N
        migrateBtn.setName("migrateBtn"); // NOI18N
        buttonPanel.add(migrateBtn);

        cancelBtn.setAction(actionMap.get("cancelMigration")); // NOI18N
        cancelBtn.setText(resourceMap.getString("cancelBtn.text")); // NOI18N
        cancelBtn.setName("cancelBtn"); // NOI18N
        buttonPanel.add(cancelBtn);

        resetBtn.setAction(actionMap.get("clearDisk")); // NOI18N
        resetBtn.setText(resourceMap.getString("resetBtn.text")); // NOI18N
        resetBtn.setName("resetBtn"); // NOI18N
        buttonPanel.add(resetBtn);

        resetAllBtn.setAction(actionMap.get("clearAll")); // NOI18N
        resetAllBtn.setText(resourceMap.getString("resetAllBtn.text")); // NOI18N
        resetAllBtn.setName("resetAllBtn"); // NOI18N
        buttonPanel.add(resetAllBtn);

        dataMetadata.setBorder(javax.swing.BorderFactory.createTitledBorder("Source Data Metadata"));
        dataMetadata.setName("dataMetadata"); // NOI18N

        diskNameLbl.setText(resourceMap.getString("diskNameLbl.text")); // NOI18N
        diskNameLbl.setName("diskNameLbl"); // NOI18N

        diskName.setName("diskName"); // NOI18N

        diskNameBtn.setAction(actionMap.get("recommendDiskName")); // NOI18N
        diskNameBtn.setText(resourceMap.getString("diskNameBtn.text")); // NOI18N
        diskNameBtn.setName("diskNameBtn"); // NOI18N

        metadataTabs.setName("metadataTabs"); // NOI18N

        diskLabelSP.setBorder(null);
        diskLabelSP.setName("diskLabelSP"); // NOI18N

        diskLabel.setColumns(20);
        diskLabel.setRows(3);
        diskLabel.setTabSize(4);
        diskLabel.setName("diskLabel"); // NOI18N
        diskLabelSP.setViewportView(diskLabel);

        metadataTabs.addTab(resourceMap.getString("diskLabelSP.TabConstraints.tabTitle"), diskLabelSP); // NOI18N

        notesSP.setBorder(null);
        notesSP.setName("notesSP"); // NOI18N

        notes.setColumns(20);
        notes.setRows(4);
        notes.setName("notes"); // NOI18N
        notesSP.setViewportView(notes);

        metadataTabs.addTab(resourceMap.getString("notesSP.TabConstraints.tabTitle"), notesSP); // NOI18N

        org.jdesktop.layout.GroupLayout dataMetadataLayout = new org.jdesktop.layout.GroupLayout(dataMetadata);
        dataMetadata.setLayout(dataMetadataLayout);
        dataMetadataLayout.setHorizontalGroup(
            dataMetadataLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(diskNameLbl)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, dataMetadataLayout.createSequentialGroup()
                .add(diskName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 224, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(diskNameBtn))
            .add(metadataTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
        );
        dataMetadataLayout.setVerticalGroup(
            dataMetadataLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(dataMetadataLayout.createSequentialGroup()
                .add(diskNameLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(dataMetadataLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(diskNameBtn)
                    .add(diskName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(metadataTabs, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 119, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        org.jdesktop.layout.GroupLayout mainPanelLayout = new org.jdesktop.layout.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, accessionInfo, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(dataMetadata, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, treePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(buttonPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(mainPanelLayout.createSequentialGroup()
                .add(accessionInfo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(treePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(dataMetadata, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(buttonPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 33, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        adaptersMenu.setText(resourceMap.getString("adaptersMenu.text")); // NOI18N
        adaptersMenu.setName("adaptersMenu"); // NOI18N
        menuBar.add(adaptersMenu);

        mmMenu.setText(resourceMap.getString("mmMenu.text")); // NOI18N
        mmMenu.setName("mmMenu"); // NOI18N

        mmDefaultMenuItem.setSelected(true);
        mmDefaultMenuItem.setText(resourceMap.getString("mmDefaultMenuItem.text")); // NOI18N
        mmDefaultMenuItem.setName("mmDefaultMenuItem"); // NOI18N
        mmMenu.add(mmDefaultMenuItem);

        menuBar.add(mmMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
            .add(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusMessageLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 219, Short.MAX_VALUE)
                .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusPanelSeparator, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(statusMessageLabel)
                    .add(statusAnimationLabel)
                    .add(progressBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(3, 3, 3))
        );

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    @Action
    public void clearDisk() {
        fileTree = null;
        treeSP.setViewportView(null);
        diskName.setText("");
        diskLabel.setText("");
        notes.setText("");
        getFrame().repaint();
    }

    private void createTree(File source) {
        fileTree = new JTree(new FileSystemModel(source.getAbsolutePath())) {

            @Override
            public String convertValueToText(Object value, boolean selected,
                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
                String returned = ((FileWrapper) value).getName();
                if (leaf) {
                    if (displaySize) {
                        returned +=
                                " (" +
                                prettySize(((FileWrapper) value).length()) + ")";
                    }
                    if (displayLastModified) {
                        returned +=
                                " [" +
                                dateFormat.format(new Date(((FileWrapper) value).lastModified())) + "]";
                    }
                }
                return returned;
            }
        };
        fileTree.setLargeModel(true);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.putClientProperty("JTree.lineStyle", "Angled");
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree,
                    Object value,
                    boolean sel,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus) {

                super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row,
                        hasFocus);

                FileWrapper nodeInfo = (FileWrapper) value;

                //Set ToolTip
                if (nodeInfo.isDirectory()) {
                    setToolTipText(null); //no tool tip
                } else {
                    setToolTipText("Size: " +
                            prettySize(nodeInfo.length()) +
                            ", Last Modified: " +
                            dateFormat.format(new Date(nodeInfo.lastModified())));
                }

                //Set Icon
                if (nodeInfo.isExcluded()) {
                    if (nodeInfo.isHidden()) {
                        setIcon(excludeHiddenIcon);
                    } else {
                        setIcon(excludeIcon);
                    }
                    setToolTipText("This file/directory is excluded.");
                } else if (nodeInfo.isHidden()) {
                    setIcon(nodeInfo.isDirectory() ? folderHiddenIcon : fileHiddenIcon);
                } else {
                    setIcon(nodeInfo.isDirectory() ? folderIcon : fileIcon);
                }

                return this;
            }
        });
        treeSP.setViewportView(fileTree);
        treeSP.repaint();
        String displayName = FileSystemView.getFileSystemView().
                getSystemDisplayName(source);
        displayName = displayName.replaceAll(" \\([A-Z]:\\)$", "");
        diskName.setText(displayName);
    }

    @Action
    public void clearAll() {
        name.setText("");
        accnNo.setText("");
        accnDir.setText("");
        collTitle.setText("");
        clearDisk();
    }

    @Action
    public Task migrate() {
        //Sanity Checks
        if (migrator == null) {
            return null;
        }
        if ((accnDir.getText().equalsIgnoreCase("")) ||
                (name.getText().equalsIgnoreCase("")) ||
                (accnNo.getText().equalsIgnoreCase("")) ||
                !(getSourceMetadata().exists())) {
            JOptionPane.showMessageDialog(getFrame(),
                    "All required fields must be entered.",
                    "Invalid Options",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        File accnDirFile = new File(accnDir.getText() + File.separator +
                accnNo.getText());
        if (getSourceMetadata().getPath().equalsIgnoreCase(accnDir.getText())) {
            JOptionPane.showMessageDialog(getFrame(),
                    "The destination cannot be the same as the source.",
                    "Invalid Options",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (diskName.getText().equals("")) {
            JOptionPane.showMessageDialog(getFrame(),
                    "Disk Name cannot be empty",
                    "Metadata Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (new File(accnDirFile, diskName.getText()).exists()) {
            JOptionPane.showMessageDialog(getFrame(),
                    "Disk with the name " + diskName.getText() +
                    " already exists.",
                    "Metadata Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        //Characters that don't play well with various file systems
        String[] illegalCharacters = {
            "<", ">", ":", "\"", "/", "\\", "|", "?", "*", ".", "!", "½"
        };
        for (String badCharacter : illegalCharacters) {
            if (diskName.getText().contains(badCharacter)) {
                JOptionPane.showMessageDialog(getFrame(),
                        "You may not use any of the following characters" +
                        " as the disk name: < > : \" / \\ | ? . ! * ½",
                        "Illegal Characters in Disk Name",
                        JOptionPane.ERROR_MESSAGE);
                return null;
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
            if (diskName.getText().equals(badName)) {
                JOptionPane.showMessageDialog(getFrame(),
                        "You may not use any of the following names " +
                        "as the disk name: " +
                        "com1, com2, com3, com4, com5, com6, com7, com8, com9, " +
                        "lpt1, lpt2, lpt3, lpt4, lpt5, lpt6, lpt7, lpt8, lpt9, " +
                        "con, nul, and prn.",
                        "Illegal Characters in Disk Name",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        if (!getSourceMetadata().exists()) {
            JOptionPane.showMessageDialog(getFrame(),
                    "The disk/folder has been removed or deleted." +
                    " Please re-insert disk or cancel migration.",
                    "Disk Missing",
                    JOptionPane.ERROR_MESSAGE);
        }

        return new MigrateTask(getApplication());
    }

    private class MigrateTask extends org.jdesktop.application.Task<Object, Void>
            implements ActionListener {

        Timer timer;

        MigrateTask(org.jdesktop.application.Application app) {
            super(app);
            currentMigration = this;
            enable(false);
        }

        @Override
        protected Object doInBackground() {
            setMessage("Preparing migration ...");
            //Add some metadata
            FileWrapper source = getSourceMetadata();
            source.addQualifiedMetadata("note", null, diskName.getText() +
                    " transfered by " + name.getText() + " on " +
                    new Date(System.currentTimeMillis()).toString());
            if (!notes.getText().equalsIgnoreCase("")) {
                source.addQualifiedMetadata("note", null,
                        notes.getText());
            }
            if (!diskLabel.getText().equalsIgnoreCase("")) {
                source.addQualifiedMetadata("description", "label",
                        diskLabel.getText());
            }
            source.addQualifiedMetadata("identifier", "accession_no",
                    accnNo.getText());
            source.addQualifiedMetadata("title", "collection",
                    collTitle.getText());
            source.setNewName(diskName.getText());
            migrator.setSource(source);
            //Set destination dir
            migrator.setDestination(new File(accnDir.getText(),
                    accnNo.getText()));

            //Run migrator
            timer = new Timer(500, this); //Prod every half second
            timer.start();
            boolean success = migrator.run();
            timer.stop();
            return success;
        }

        @Override
        protected void cancelled() {
            int answer = JOptionPane.showOptionDialog(getFrame(),
                    "You canceled the migration before it completed. " +
                    "Would you like to delete the transfered files? ",
                    "Migration Canceled",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    null,
                    null);
            if (answer == 0) {
                //Delete destination
                File toDelete = new File(migrator.getDestination(), migrator.getSource().
                        getNewName());
                if (!deleteRecursively(toDelete)) {
                    JOptionPane.showMessageDialog(getFrame(),
                            "Unable to delete " + toDelete.getAbsolutePath(),
                            "Error deleting destination",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
        @Override
        protected void finished() {
            clearDisk();
            enable(true);
            if (migrator.getErrors().size() > 0) {
                int answer = JOptionPane.showOptionDialog(getFrame(),
                        "One or more errors occured during migration. " +
                        "Details will be listed in the accession " +
                        "metadata file. " +
                        "Would you like to see a list of the errors now?",
                        "Migration Errors Occured",
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
                            ArrayList<String> errors = migrator.getErrors();
                            for (String error : errors) {
                                errorText.append(error + "\n");
                            }

                        }
                    });
                    errorDisplayThread.setName("ErrorFrame");
                    errorDisplayThread.run();
                }
            }
            migrator.getErrors().clear();
            currentMigration = null;
        }

        @Override
        protected void succeeded(Object result) {
            setMessage("Successfully migrated data.");
        }
        
        

        public void prod() {
            if (migrator != null) {
                setProgress(migrator.getPercentMigrated());
                setMessage(migrator.getCurrentMessage());
            }
        }

        public void actionPerformed(ActionEvent e) { //Catch Timer events
            prod();
        }
    }

    @Action
    public Task loadSrcDir() {
        return new LoadSrcDirTask(getApplication());
    }

    private class LoadSrcDirTask extends org.jdesktop.application.Task<Object, Void> {

        File source = null;

        LoadSrcDirTask(org.jdesktop.application.Application app) {
            super(app);
            setMessage("Trying to load a new disk or directory...");
            source = null;
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setApproveButtonText("Select Disk/Directory to Migrate");
            if (fc.showOpenDialog(getFrame()) == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                if (!file.isDirectory()) {
                    JOptionPane.showMessageDialog(getFrame(),
                            "The selected directory is invalid." +
                            " Select a valid directory.",
                            "Invalid Source Disk/Directory",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                source = file;
            }
        }

        @Override
        protected Object doInBackground() {
            setMessage("Trying to load " + source.getAbsolutePath());
            createTree(source);
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            setMessage("Done loading " + source.getAbsolutePath());
        }
    }

    @Action
    public Task excludeItem() {
        return new ExcludeItemTask(getApplication(), true);
    }

    public void setExcluded(boolean exclude) {
        TreePath[] currentSelections = fileTree.getSelectionPaths();
        for (TreePath currentSelection : currentSelections) {
            if (currentSelection != null) {
                FileWrapper currentNode =
                        (FileWrapper) currentSelection.getLastPathComponent();
                currentNode.setExcluded(exclude);
                if (exclude) {
                    fileTree.collapsePath(currentSelection);
                }
            }
        }
        fileTree.repaint();
    }

    private class ExcludeItemTask extends org.jdesktop.application.Task<Object, Void> {

        private boolean exclude;

        ExcludeItemTask(org.jdesktop.application.Application app,
                boolean exclude) {
            super(app);
            this.exclude = exclude;
        }

        @Override
        protected Object doInBackground() {
            if (exclude) {
                setMessage("Excluding selected items...");
            } else {
                setMessage("Including selected items...");
            }
            setExcluded(exclude);
            return null;  // return your result
        }

        @Override
        protected void succeeded(Object result) {
            setMessage("");
            return;
        }
    }

    @Action
    public Task includeItem() {
        return new ExcludeItemTask(getApplication(), false);
    }

    @Action
    public void setAccessionDir() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setApproveButtonText("Set as Accessions Directory");
        int returnVal = fc.showOpenDialog(this.getFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if ((!file.isDirectory()) || (!file.canWrite())) {
                JOptionPane.showMessageDialog(this.getFrame(),
                        "The selected accessions directory is invalid or you cannot write to it." +
                        " Select a valid directory.",
                        "Invalid Accessions Directory",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            accnDir.setText(file.getAbsolutePath());
        }
    }

    @Action
    public Task recommendDiskName() {
        return new RecommendDiskNameTask(getApplication());
    }

    private class RecommendDiskNameTask extends org.jdesktop.application.Task<Object, Void> {

        RecommendDiskNameTask(org.jdesktop.application.Application app) {
            super(app);
        }

        @Override
        protected Object doInBackground() {
            try {
                String newName = "Disk ";
                //List folders in accession (dest) dir matching "Disk ddd"
                Pattern pattern = Pattern.compile("(\\d+)$");
                Matcher matcher;
                File[] existingDirs = new File(accnDir.getText(), accnNo.getText()).listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        return name.matches("^[Dd]isk \\d+$");
                    }
                });
                int highest = 0;
                if ((existingDirs != null) && (existingDirs.length > 0)) {
                    for (File existing : existingDirs) {
                        matcher = pattern.matcher(existing.getName());
                        while (matcher.find()) { //Should only find one...
                            String match = matcher.group();
                            int matchInt = Integer.parseInt(match);
                            if (matchInt > highest) {
                                highest = matchInt;
                            }
                        }
                    }
                }

                //Add 1 to highest ddd & return new "Disk ddd"
                DecimalFormat myFormat = new DecimalFormat("000");
                return newName + myFormat.format(new Integer(++highest));

            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        @Override
        protected void succeeded(Object result) {
            diskName.setText(result.toString());
        }
    }

    public class FileSystemModel implements TreeModel, Serializable {

        FileWrapper root;
        private Vector<TreeModelListener> treeModelListeners =
                new Vector<TreeModelListener>();

        public FileSystemModel() {
            this(System.getProperty("user.home"));
        }

        public FileSystemModel(String startPath) {
            root = new FileWrapper(startPath);
        }

        public FileWrapper getRoot() {
            return root;
        }

        public FileWrapper getChild(Object parent, int index) {
            FileWrapper directory = (FileWrapper) parent;
            return directory.listMetadata()[index];
        }

        public int getChildCount(Object parent) {
            FileWrapper fileSysEntity = (FileWrapper) parent;
            if (fileSysEntity.isDirectory()) {
                return fileSysEntity.listMetadata().length;
            } else {
                return 0;
            }
        }

        public boolean isLeaf(Object node) {
            return ((FileWrapper) node).isFile();
        }

        public void valueForPathChanged(TreePath path, Object newValue) {
        //Do nothing?
        }

        public int getIndexOfChild(Object parent, Object child) {
            FileWrapper directory = (FileWrapper) parent;
            FileWrapper fileSysEntity = (FileWrapper) child;
            FileWrapper[] children = directory.listMetadata();

            for (int i = 0; i < children.length; ++i) {
                if (fileSysEntity.getName().equals(children[i].getName())) {
                    return i;
                }
            }
            return -1;
        }

        public void addTreeModelListener(TreeModelListener l) {
            treeModelListeners.addElement(l);

        }

        /**
         * Removes a listener previously added with addTreeModelListener().
         */
        public void removeTreeModelListener(TreeModelListener l) {
            treeModelListeners.removeElement(l);
        }
    }

    private class MetadataManagerMenuActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JMenuItem source = (JMenuItem) (e.getSource());
            for (MetadataManager mm : migrator.getAvailableManagers()) {
                if (mm.getName().equalsIgnoreCase(source.getText())) {
                    try {
                        migrator.setMetadataManager(mm);
                    } catch (Exception ex) {
                        Logger.getLogger(DataAccessionerView.class.getName()).
                                log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
    
    private class AdapterMenuActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JMenuItem source = (JMenuItem) e.getSource();
            for(Adapter adapter : migrator.getAvailableAdapters()){
                if(adapter.getName().equalsIgnoreCase(source.getText())){
                    if(source.isSelected()){
                        migrator.getSelectedAdapters().add(adapter);
                    } else {
                        migrator.getSelectedAdapters().remove(adapter);
                    }
                }
            }
        }

    }

    protected void enable(boolean enable) {
        accnDirBtn.setEnabled(enable);
        migrateBtn.setEnabled(enable);
        resetAllBtn.setEnabled(enable);
        resetBtn.setEnabled(enable);
        cancelBtn.setEnabled(!enable); //Opposite the rest
    }

    protected static String prettySize(Long size) {
        String prettySize = "";
        String[] measures = {"B", "KB", "MB", "GB", "TB", "EB", "ZB", "YB"};

        int power = measures.length - 1;
        //Cycle each measure starting with the smallest
        for (int i = 0; i < measures.length; i++) {
            //Test for best fit 
            if ((size / (Math.pow(1024, i))) < 1024) {
                power = i;
                break;
            }
        }
        DecimalFormat twoPlaces = new DecimalFormat("#,##0.##");
        Double newSize = (size / (Math.pow(1024, power)));
        prettySize = twoPlaces.format(newSize) + " " + measures[power];
        return prettySize;
    }
    
    protected static boolean deleteRecursively(File toDelete){
        if(toDelete.isDirectory()){
            for(File child : toDelete.listFiles()){
                if(!deleteRecursively(child)){
                    return false;
                }
            }
        }
        return toDelete.delete();
    }

    @Action
    public void cancelMigration() {
        if (currentMigration == null || currentMigration.isDone()) {
            return;
        }
        else {
            currentMigration.cancel(true);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel accessionInfo;
    protected javax.swing.JTextField accnDir;
    private javax.swing.JButton accnDirBtn;
    protected javax.swing.JTextField accnNo;
    private javax.swing.JLabel accnNoLbl;
    private javax.swing.JMenu adaptersMenu;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelBtn;
    protected javax.swing.JTextField collTitle;
    private javax.swing.JLabel collTitleLbl;
    private javax.swing.JPanel dataMetadata;
    protected javax.swing.JTextArea diskLabel;
    private javax.swing.JScrollPane diskLabelSP;
    protected javax.swing.JTextField diskName;
    private javax.swing.JButton diskNameBtn;
    private javax.swing.JLabel diskNameLbl;
    private javax.swing.JLabel displayLabel;
    private javax.swing.JToggleButton displaySizeBtn;
    private javax.swing.JButton excludeBtn;
    private javax.swing.JTree fileTree;
    private javax.swing.JButton includeBtn;
    private javax.swing.JToggleButton lastModBtn;
    private javax.swing.JButton loadSrcBtn;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTabbedPane metadataTabs;
    private javax.swing.JButton migrateBtn;
    private javax.swing.JRadioButtonMenuItem mmDefaultMenuItem;
    private javax.swing.JMenu mmMenu;
    protected javax.swing.JTextField name;
    private javax.swing.JLabel nameLbl;
    protected javax.swing.JTextArea notes;
    private javax.swing.JScrollPane notesSP;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton resetAllBtn;
    private javax.swing.JButton resetBtn;
    private javax.swing.JToolBar.Separator separator1;
    private javax.swing.JToolBar.Separator separator2;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JPanel treePanel;
    private javax.swing.JScrollPane treeSP;
    private javax.swing.JToolBar treeToolBar;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    private static DateFormat dateFormat =
            new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
    private boolean displaySize = false;
    private boolean displayLastModified = false;
    private JDialog aboutBox;
    private Icon excludeIcon,  fileIcon,  folderIcon,  excludeHiddenIcon,  fileHiddenIcon,  folderHiddenIcon;
    private DataAccessioner migrator = null;
    private MigrateTask currentMigration = null;
}
