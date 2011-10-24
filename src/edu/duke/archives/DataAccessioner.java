/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.duke.archives;

import edu.duke.archives.interfaces.Adapter;
import edu.duke.archives.metadata.FileWrapper;
import edu.duke.archives.interfaces.MetadataManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.Plugin;
import org.java.plugin.PluginLifecycleException;
import org.java.plugin.PluginManager;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.standard.StandardPluginLocation;
import org.java.plugin.util.ExtendedProperties;

/**
 *
 * @author Seth Shaw
 */
public class DataAccessioner {

    private static PluginManager pluginManager;
    public ConfigManager config = new ConfigManager();
    
    private FileWrapper source;
    private File destination;
    private int toMigrate = 0;
    private int migrated = 0;
    private String currentMessage = "";
    private boolean verbose = false;
    private boolean cancelled = false;
    private boolean done = false;
    private boolean noDest = false;
    private boolean tmpDest = false;
    private ArrayList<String> errors = new ArrayList<String>();
    //Plugins
    private ArrayList<MetadataManager> availableManagers =
            new ArrayList<MetadataManager>();
    private MetadataManager metadataManager;
    private ArrayList<Adapter> availableAdapters = new ArrayList<Adapter>();
    private ArrayList<Adapter> selectedAdapters = new ArrayList<Adapter>();

    public DataAccessioner() {

        metadataManager = new DefaultMetadataManager();
        availableManagers.add(metadataManager);
        availableManagers.add(new BlankMetadataManager());
        

        //Load Plugins
        try {
            loadPlugins();
        } catch (JpfException ex) {
            Logger.getLogger(DataAccessioner.class.getName()).
                    log(Level.INFO, "Couldn't load plugins.", ex);
        }

    }

    public static void main(String[] args) {
        //Command-line processing (GNU parsing is similar to Java's.
        CommandLineParser parser = new GnuParser();
        try {
            
            Options options = new Options();
            
            options.addOption(new Option("help", "print this message"));
            options.addOption(new Option("version",
                    "print the version information and exit"));
            options.addOption(new Option("quiet", "be extra quiet"));
            options.addOption(new Option("verbose", "be extra verbose"));
            options.addOption(new Option("debug", "print debugging information"));
            options.addOption(new Option("cli",
                    "use without the graphic user interface"));
            @SuppressWarnings("static-access")
            Option sourceOptn = OptionBuilder.withArgName("sourcePath").hasArg().
                    withDescription("root directory or file to be migrated").
                    create("source");
            options.addOption(sourceOptn);
            @SuppressWarnings("static-access")
            Option destOptn = OptionBuilder.withArgName("destPath").hasArg().
                    withDescription("location to place the migrated copy.\n" +
                    "\"dest=nodest\" will only use plugins if the source is " +
                    "unwrittable.\n" +
                    "\"dest=nodest_force\" will create a temporary copy of " +
                    "writable source files to run plugins.").
                    create("dest");
            options.addOption(destOptn);
            @SuppressWarnings("static-access")
            Option plugins = OptionBuilder.withArgName("pluginId").hasArgs().
                    withDescription("plugins to turn on by default").
                    withValueSeparator(',').
                    create("plugins");
            options.addOption(plugins);
            @SuppressWarnings("static-access")
            Option configFile = OptionBuilder.withArgName("configFile").hasArg().
                    withDescription("path to config file").create("config");
            options.addOption(configFile);
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar DataAccessioner.jar", options);
                return;
            } else if (line.hasOption("version")) {
                System.out.println("DataAccessioner version 0.4");
                return;
            }

            DataAccessioner dataAccessioner = new DataAccessioner();
            dataAccessioner.config = (line.hasOption("config"))
                    ? new ConfigManager(line.getOptionValue("config"))
                    : new ConfigManager("config.xml"); //Default place to look

            //Change logging verbosity
            if (line.hasOption("quiet")) {
                Logger.getLogger(DataAccessioner.class.getName()).
                        setLevel(Level.SEVERE);
            } else if (line.hasOption("verbose")) {
                Logger.getLogger(DataAccessioner.class.getName()).
                        setLevel(Level.FINE);
            } else if (line.hasOption("debug")) {
                Logger.getLogger(DataAccessioner.class.getName()).
                        setLevel(Level.FINEST);
            } else {
                Logger.getLogger(DataAccessioner.class.getName()).
                        setLevel(Level.INFO);
            }

            //source
            if (line.hasOption("source")) {
                String sourcePath = line.getOptionValue("source");
                FileWrapper source = new FileWrapper(sourcePath);
                if (source == null || !source.exists()) {
                    System.out.println(sourcePath +
                            " is not a valid source to migrate.");
                    return;
                } else if (!source.canRead()) {
                    System.out.println("Cannot read the source path: " +
                            sourcePath);
                    return;
                }
                dataAccessioner.setSource(source);
            }
            //destination
            if (line.hasOption("dest")) {
                String destPath = line.getOptionValue("dest");
                if (destPath.equalsIgnoreCase("nodest")) { // FileWrapper only running
                    dataAccessioner.noDest = true;
                } else if (destPath.equalsIgnoreCase("nodest_force")) {
                    dataAccessioner.tmpDest = true;
                } else {
                    dataAccessioner.noDest = false;
                    File dest = new File(destPath);
                    dest.mkdirs();
                    if (dest == null) {
                        System.out.println(destPath +
                                " is not a valid destination.");
                        return;
                    } else if (!dest.canWrite()) {
                        System.out.println("Cannot write to the destination path: " +
                                destPath);
                        return;
                    }
                    dataAccessioner.setDestination(dest);
                }
            }

            //Plugin selection
            if (line.hasOption("plugins")) {
                String[] pluginIDs = line.getOptionValues("plugins");
                //Get available adapters
                List<Adapter> adapters = dataAccessioner.getAvailableAdapters();
                //Get available managers
                List<MetadataManager> managers =
                        dataAccessioner.getAvailableManagers();
                for ( String pluginID : pluginIDs) {
                    //Check registry for plugin matching id
                    Plugin plugin = null;
                    try {
                        plugin =
                                pluginManager.getPlugin(pluginID);
                    } catch ( PluginLifecycleException ple) {
                        Logger.getLogger(DataAccessioner.class.getName()).
                                log(Level.WARNING, "Couldn't load plugin: " +
                                pluginID, ple);
                        continue;
                    } catch ( IllegalArgumentException iae) {
                        Logger.getLogger(DataAccessioner.class.getName()).
                                log(Level.WARNING, "Couldn't load plugin: " +
                                pluginID, iae);
                        continue;
                    }

                    //retrieve the plugin class parameter
                    String className = "";
                    //Extensions define what adapters & managers this plugin adds.
                    for (Extension extension : plugin.getDescriptor().
                            getExtensions()) {
                        className = extension.getParameter("class").
                                valueAsString();
                        //cycle through list of adapaters
                        for (Adapter adapter : adapters) {
                            //If matching class
                            if (className.equalsIgnoreCase(adapter.getClass().
                                    getCanonicalName())) {
                                dataAccessioner.getSelectedAdapters().add(adapter);
                                break;
                            }
                        }


                        //activate manager if manager
                        for ( MetadataManager manager : managers) {
                            //If matching class
                            if (className.equalsIgnoreCase(manager.getClass().
                                    getCanonicalName())) {
                                try {
                                    dataAccessioner.setMetadataManager(manager);
                                    break;
                                } catch ( Exception e) {
                                    Logger.getLogger(DataAccessioner.class.getName()).
                                            log(Level.INFO, "Couldn't load plugin: " +
                                            pluginID, e);
                                    continue;
                                }
                            }
                        }
                    }//End for each extension
                }//End for each plugin id
            }//End Plugin option

            //Command-line mode
            if (line.hasOption("cli")) {
                //Check Source
                if (dataAccessioner.source == null ||
                        !dataAccessioner.source.canRead()) {
                    System.out.println("Must have a valid readable source");
                    return;
                }
                System.out.println("Source: " + dataAccessioner.source.getPath());
                //Check Destination
                if (dataAccessioner.destination == null ||
                        !dataAccessioner.destination.canWrite()) {
                    System.out.println("Must have a valid writable destination");
                    return;
                }
                System.out.println("Destination: " +
                        dataAccessioner.destination.getPath());
                //Give run information
                System.out.println("Selected Adapters: ");
                List adapters = dataAccessioner.selectedAdapters;
                if (adapters.isEmpty()) {
                    System.out.println("\t<none>");
                } else {
                    for ( Adapter adapter : dataAccessioner.selectedAdapters) {
                        System.out.println("\t" + adapter.getName());
                    }
                }
                System.out.println("Metadata Manager: " +
                        dataAccessioner.metadataManager.getName());
                dataAccessioner.run();
            }
            
            //Launch GUI
            DataAccessionerApp.launch(DataAccessionerApp.class,
                    dataAccessioner, args);
            return;

        } catch ( ParseException exp) {
            Logger.getLogger(DataAccessioner.class.getName()).log(Level.SEVERE,
                    "Parsing failed.  Reason: " + exp.getMessage());
            return;
        }

    }

        public File getDestination() {
        return destination;
    }

    public void setDestination(File destination) {
        this.destination = destination;
    }

    public FileWrapper getSource() {
        return source;
    }

    public void setSource(FileWrapper source) {
        this.source = source;
    }

    String getCurrentMessage() {
        return currentMessage;
    }

    private void loadPlugins() throws JpfException {
        pluginManager = ObjectFactory.newInstance().createManager();

        ExtendedProperties ep = new ExtendedProperties();
        ep.put("org.java.plugin.PathResolver",
                "org.java.plugin.standard.ShadingPathResolver");
        ep.put("unpackMode", "smart");
        ep.put("org.java.plugin.standard.ShadingPathResolver.unpackMode",
                "smart");

        pluginManager = ObjectFactory.newInstance(ep).createManager(
                ObjectFactory.newInstance(ep).createRegistry(),
                ObjectFactory.newInstance(ep).createPathResolver());

        File pluginsDir = new File("plugins");
        File[] plugins = pluginsDir.listFiles(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".zip");
            }
        });
        if (plugins == null || plugins.length == 0) {
            return;
        }
        PluginLocation[] locations = new PluginLocation[plugins.length];

        for (int i = 0; i < plugins.length; i++) {
            try {
                locations[i] = StandardPluginLocation.create(plugins[i]);
            } catch (MalformedURLException me) {
                Logger.getLogger(DataAccessioner.class.getName()).
                        log(Level.WARNING, plugins[i].getPath() +
                        " is malformed.\nReason: " + me.getLocalizedMessage());
            }
        }

        pluginManager.publishPlugins(locations);

        PluginDescriptor core =
                pluginManager.getRegistry().getPluginDescriptor("edu.duke.archives.migrator.plugins.core");

        //Load MetadataManagers
        ExtensionPoint point =
                pluginManager.getRegistry().getExtensionPoint(core.getId(),
                "MetadataManager");

        for (Iterator it = point.getConnectedExtensions().iterator();
                it.hasNext();) {
            try {
                Extension ext = (Extension) it.next();
                PluginDescriptor descr = ext.getDeclaringPluginDescriptor();
                pluginManager.activatePlugin(descr.getId());
                ClassLoader classLoader =
                        pluginManager.getPluginClassLoader(descr);
                Class pluginCls = classLoader.loadClass(ext.getParameter("class").
                        valueAsString());
                availableManagers.add((MetadataManager) pluginCls.newInstance());
            } catch (Exception ex) {
                Logger.getLogger(DataAccessioner.class.getName()).
                        log(Level.WARNING, null, ex);
            }
        }
        //Load Adapters
        point = pluginManager.getRegistry().getExtensionPoint(core.getId(),
                "Adapter");

        for (Iterator it = point.getConnectedExtensions().iterator();
                it.hasNext();) {
            try {
                Extension ext = (Extension) it.next();
                PluginDescriptor descr = ext.getDeclaringPluginDescriptor();
                pluginManager.activatePlugin(descr.getId());
                ClassLoader classLoader =
                        pluginManager.getPluginClassLoader(descr);
                Class pluginCls = classLoader.loadClass(ext.getParameter("class").
                        valueAsString());
                availableAdapters.add((Adapter) pluginCls.newInstance());
            } catch (Exception ex) {
                Logger.getLogger(DataAccessioner.class.getName()).
                        log(Level.WARNING, null, ex);
            }
        }
    }

    private void setCurrentMessage(String message) {
        currentMessage = message;
        if (verbose) {
            System.out.println(message);
        }
    }

    int getPercentMigrated() {
        if (done) {
            return 100;
        }
        int percent = (int) (100.0 * ((double) migrated / (double) toMigrate));
        //Never go above 99 unless actually done (counters rounding errors).
        if (percent > 99) {
            return 99;
        }
        return percent;
    }

    /**
     * Sets the Migrator's FileWrapper Manager.
     * Not required to run the migrator
     * 
     * @param metadataManager is in charge of organizing & formating metadata
     */
    public void setMetadataManager(MetadataManager metadataManager) throws Exception {
        this.metadataManager = metadataManager;
    }

    public ArrayList<MetadataManager> getAvailableManagers() {
        return availableManagers;
    }

    public ArrayList<Adapter> getSelectedAdapters() {
        return selectedAdapters;
    }

    public ArrayList<Adapter> getAvailableAdapters() {
        return availableAdapters;
    }

    public boolean run() {
        try {
            cancelled = false;
            done = false;
            if (destination.getAbsolutePath().
                    equalsIgnoreCase(source.getAbsolutePath())) {
                addError("Destination was the same as the source.");
                return false;
            }
            destination.getParentFile().mkdirs();
            if (getMetadataManager() != null) {
                getMetadataManager().init(this);
            }
            //Start
            recurseDirectories(source,
                    new File(destination.getAbsolutePath() +
                    File.separator + source.getNewName()));
            //Done
            if (getMetadataManager() != null) {
                setCurrentMessage("Writting out metadata...");
                getMetadataManager().close();
            }
            setCurrentMessage("Done migrating...");
            done = true;
            return true;
        } catch (Exception ex) {
            addError(ex.getLocalizedMessage());
            Logger.getLogger(DataAccessioner.class.getName()).
                    log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            return false;
        } catch (OutOfMemoryError oome) {
            destination = null;
            source.deleteRecursively();
            source = null;
            getMetadataManager().cancel();
            metadataManager = null;
            addError("Java ran out of memory. " +
                    "Use the -Xmx option to increase available memory.");
            return false;
        } catch (Error err) {
            addError(err.getLocalizedMessage());
            Logger.getLogger(DataAccessioner.class.getName()).
                    log(Level.SEVERE, null, err);
            err.printStackTrace();
            return false;
        }
    }

    /**
     * Recursively walk a directory tree and return a List of all
     * Files found; the List is sorted using File.compareTo.
     *
     * @param src is a valid directory, which can be read.
     * @param destDir is the new directory to be created
     */
    private void recurseDirectories(FileWrapper src, File destDir)
            throws Exception {
        if (cancelled) {
            throw new Exception("Migration Cancelled");
        }
        setCurrentMessage("Migrating " + src.getName());
        if (getMetadataManager() != null) {
            getMetadataManager().startDirectory(src);
        }
        try {
            /**
             * I know it is odd to call the directory by the destination 
             * directory, but that is where processors will find it. 
             * The only time this causes a difference is then the 
             * DiskMigrator passes us the disk root directory but we
             * want to call it by the label we give it.
             */
            validateDirectory(src);
            FileWrapper[] filesAndDirs = src.listMetadata();
            toMigrate += filesAndDirs.length;
            for (int i = 0; i < filesAndDirs.length; i++) {

                if (Thread.interrupted()) {
                    String message = "Migration of " +
                            filesAndDirs[i].getAbsolutePath() +
                            " interupted";
                    System.out.println(message);
                    throw new InterruptedException(message);
                }

                if (filesAndDirs[i].isExcluded()) {
                //Ignore excluded files and folders
                } else if (!filesAndDirs[i].isFile()) {
                    //Find new destination directory
                    File newDestDir = new File(destDir.getAbsolutePath() +
                            File.separator + filesAndDirs[i].getName());
                    newDestDir.mkdirs();
                    try {
                        newDestDir.setLastModified(filesAndDirs[i].lastModified());
                    } catch (Exception e) {
                        String message =
                                "Unable to set last modified date (" +
                                filesAndDirs[i] + "): " +
                                e.getLocalizedMessage();
                        errors.add(message);
                        filesAndDirs[i].addQualifiedMetadata("error", "",
                                message);
                    }
                    recurseDirectories(filesAndDirs[i], newDestDir);
                } else if(filesAndDirs[i].isFile()) {
                    destDir.mkdirs();
                    String dest = destDir.getAbsolutePath() + File.separator +
                            filesAndDirs[i].getNewName();
                    copyFile(filesAndDirs[i], dest);
                } else {
                    String note =
                            filesAndDirs[i].getPath() +
                            " does not appear to be either a file or a directory and will be skipped.";
                    Logger.getLogger(DataAccessioner.class.getName()).log(Level.WARNING,
                            note);
                    if (metadataManager != null) {
                        metadataManager.addNote(note);
                    }
                    addError(note);
                }
                destDir.setLastModified(src.lastModified());
                migrated++;
            }
        } catch (FileNotFoundException fnfe) {
            String note = "The directory " + src.getName() + " was not found";
            if (getMetadataManager() != null) {
                if (!(src == null)) {
                    getMetadataManager().addNote(note);
                }
            }
            addError(note);
            Logger.getLogger(DataAccessioner.class.getName()).log(Level.WARNING,
                    note, fnfe);
        } finally {
            if (getMetadataManager() != null) {
                getMetadataManager().endDirectory();
            }
        }
    }

    private void copyFile(FileWrapper src, String dest)
            throws Exception {
        if (cancelled) {
            throw new Exception("Migration Cancelled");
        }
        try {
            setCurrentMessage("Migrating: " + src.getName());

            //Generate & record Hash
			/*
             * MD5 code from R.J. Lorimer, "Getting MD5 Sums in Java", 
             * Javalobby.org (accessed 9 May 2008)
             * 
             * My original approach of using 
             * org.apache.commons.codec.digest.DigestUtils.md5Hex() was
             * choking on some large files leaving me with an OutOfMemoryError.
             */
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            MessageDigest sha2Digest = MessageDigest.getInstance("SHA-256");
            CRC32 crc32Digest = new CRC32();
            
            InputStream is = new FileInputStream(src);
            byte[] buffer = new byte[8192];
            int read = 0;
            String inMD5 = "";
            setCurrentMessage("Creating initial MD5: " + src.getName());
            try {
                while ((read = is.read(buffer)) > -1) {
                    md5Digest.update(buffer, 0, read);
                    sha2Digest.update(buffer, 0, read);
                    crc32Digest.update(buffer, 0, read);
                }
                inMD5 = digest2String(md5Digest.digest());

                src.setChecksum("MD5", inMD5);
                src.setChecksum("CRC32", String.format("%08x", crc32Digest.getValue()));
                src.setChecksum("SHA-256", digest2String(sha2Digest.digest()));
                
            } catch (IOException e) {
                throw new RuntimeException("Unable to process source file " +
                        src.getName() + " for MD5", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close input stream for MD5 calculation",
                            e);
                }
                md5Digest.reset();
                buffer = new byte[8192];
            }

            if (!noDest) {
                //Copy File
                setCurrentMessage("Copying: " + src.getName());
                FileChannel in = new FileInputStream(src).getChannel();
                File newFile = new File(dest);
                FileChannel out = new FileOutputStream(newFile).getChannel();
                try {
//                in.transferTo(0, in.size(), out); //Often gives memory map errors.
                    // magic number for Windows, 64Mb - 32Kb)
                    int maxCount = (64 * 1024 * 1024) - (32 * 1024);
                    long size = in.size();
                    long position = 0;
                    while (position < size) {
                        position += in.transferTo(position, maxCount, out);
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                }
                try {
                    newFile.setLastModified(src.lastModified());
                } catch (Exception e) {
                    String message =
                            "Unable to set last modified date (" +
                            newFile + "): " +
                            e.getLocalizedMessage();
                    errors.add(message);
                    src.addQualifiedMetadata("error", "",
                            message);
                }

                //Generate hash for transfered file and check
                setCurrentMessage("Creating migrated MD5: " + src.getName());
                is = new FileInputStream(newFile);
                String outMD5 = "";
                try {
                    while ((read = is.read(buffer)) > -1) {
                        md5Digest.update(buffer, 0, read);
                    }
                    outMD5 = digest2String(md5Digest.digest());
                } catch (IOException e) {
                    throw new RuntimeException("Unable to process migrated " +
                            newFile.getName() + " for MD5", e);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to close input stream for MD5 calculation",
                                e);
                    }
                }

                if (inMD5.compareTo(outMD5) != 0) {
                    String message = "Transfer failed, the copied file MD5" +
                            " did not match: " + inMD5 + " != " + outMD5;   
                    throw new Exception(message);
                }
            }
            if (getMetadataManager() != null) {
                if (src != null) {
                    getMetadataManager().startFile(src);
                    if (verbose) {
                        System.out.println("Recording " + src.getName());
                    }
                }
            }

            //File-level plugins run here.
            for (Adapter adapter : selectedAdapters) {

                /* 
                 * NoDest means no copy is made. No adapters are run to prevent
                 * the risk of adapters accidently (or maliciously) ruining
                 * the source media.
                 * 
                 * TmpDest allows us to make a temporary copy for the purpose of
                 * running adapters without harming the source media. Copying
                 * is slightly different than the above method and we don't
                 * force MD5 checking.
                 * 
                 **/

                if (tmpDest) {
                    try {
                        File temp = File.createTempFile("tmp_", null);
                        temp.deleteOnExit();
                        FileChannel in =
                                new FileInputStream(src).getChannel();
                        FileChannel out =
                                new FileOutputStream(temp).getChannel();
                        in.transferTo(0, in.size(), out);
                        in.close();
                        out.close();
                        if (src.lastModified() > 0) {
                            temp.setLastModified(src.lastModified());
                        }
                        getMetadataManager().addXML(adapter.runFile(temp.getPath()));
                    } catch (IOException ioe) {
                        getMetadataManager().addNote("ERROR " +
                                "(Temporary Destination Mode): Could not create " +
                                "temp file in to run adapters.");
                    }
                } else if (!noDest) {
                    getMetadataManager().addXML(adapter.runFile(dest));
                }
            }
            getMetadataManager().endFile();
        } catch (FileNotFoundException fnfe) {
            String note = "The file " + src.getName() + " was not found";
            if (getMetadataManager() != null) {
                getMetadataManager().addNote(note);
            }
            addError(note);
            return;
        } catch (OutOfMemoryError bounded) {
            src = null;
            dest = null;
            System.gc();
            String note =
                    "The JVM ran out of memory while migrating or creating/checking metadata" +
                    " (" + bounded.getMessage() + ")";
            if (getMetadataManager() != null) {
                getMetadataManager().addNote(note);
            }
            addError(note);
            System.err.println("The JVM ran out of memory while migrating or creating/checking metadata for " +
                    src.getAbsoluteFile());
            System.err.println("\t" + bounded.getMessage());
            System.err.println("\tJVM upper level is " + Runtime.getRuntime().
                    totalMemory());
            bounded.printStackTrace();
        } catch (Exception e) {
            String note = "An error occured while migrating " + src.getName() +
                    ". " + e.getMessage();
            if (getMetadataManager() != null) {
                getMetadataManager().addNote(note);
            }
            addError(note);
            System.err.println(note);
        }

        return;
    }

    static private void validateDirectory(
            File aDirectory) throws FileNotFoundException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: " +
                    aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " +
                    aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " +
                    aDirectory);
        }
    }

    private void addError(String note) {
        errors.add(note);
    }

    public ArrayList<String> getErrors() {
        return errors;
    }

    public MetadataManager getMetadataManager() {
        return metadataManager;
    }
    
    private String digest2String(byte[] digest) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
