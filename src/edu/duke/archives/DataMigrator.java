package edu.duke.archives;

import edu.duke.archives.interfaces.Adapter;
import edu.duke.archives.metadata.Metadata;
import edu.duke.archives.interfaces.MetadataManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java.plugin.ObjectFactory;
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
public class DataMigrator {

    private Metadata source;
    private PluginManager pluginManager;

    public File getDestination() {
        return destination;
    }

    public void setDestination(File destination) {
        this.destination = destination;
    }

    public Metadata getSource() {
        return source;
    }

    public void setSource(Metadata source) {
        this.source = source;
    }
    private File destination;
    private int toMigrate = 0;
    private int migrated = 0;
    private String currentMessage = "";
    private boolean verbose = false;
    private boolean cancelled = false;
    private boolean done = false;
    private ArrayList<String> errors = new ArrayList<String>();
    //Plugins
    private ArrayList<MetadataManager> availableManagers =
            new ArrayList<MetadataManager>();
    private MetadataManager metadataManager;
    private ArrayList<Adapter> availableAdapters = new ArrayList<Adapter>();
    private ArrayList<Adapter> selectedAdapters = new ArrayList<Adapter>();

    public DataMigrator() {

        availableManagers.add(new DefaultMetadataManager());
        availableManagers.add(new BlankMetadataManager());
        //Load Plugins
        try {
            loadPlugins();
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
                    Logger.getLogger(DataMigrator.class.getName()).
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
                    Logger.getLogger(DataMigrator.class.getName()).
                            log(Level.WARNING, null, ex);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(DataMigrator.class.getName()).
                    log(Level.WARNING, "Could not load plugins.", e);
        }
    }

    public static void main(String[] args) {
        //TODO: Add options to start GUI via switch, select a metadata manager, use plugins, or simply to copy from one disk to another.
        DataAccessioner.launch(DataAccessioner.class, args);
    }

    void cancel(boolean cancelled) {
        if (cancelled) {
            cancelled = true;
            if (metadataManager != null) {
                metadataManager.cancel();
            }
        }
    }

    String getCurrentMessage() {
        return currentMessage;
    }

    void setDestinationDir(File file) {
        this.destination = file;
    }

    void setSourceDirectory(Metadata source) {
        this.source = source;
    }

    private void loadPlugins() {
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
        try {
            PluginLocation[] locations = new PluginLocation[plugins.length];

            for (int i = 0; i < plugins.length; i++) {
                locations[i] = StandardPluginLocation.create(plugins[i]);
            }

            pluginManager.publishPlugins(locations);
        } catch (Exception e) {
            System.err.println("Could not load plugins: "
                    + e.getLocalizedMessage());
            e.printStackTrace();
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

    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    /**
     * Sets the Migrator's Metadata Manager.
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

    public void setSelectedAdapters(ArrayList<Adapter> selectedAdapters) {
        this.selectedAdapters = selectedAdapters;
    }

    public ArrayList<Adapter> getAvailableAdapters() {
        return availableAdapters;
    }

    public void setAvailableAdapters(ArrayList<Adapter> availableAdapters) {
        this.availableAdapters = availableAdapters;
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
            if (metadataManager != null) {
                metadataManager.init(this);
            }
            //Start
            recurseDirectories(source,
                    new File(destination.getAbsolutePath()
                    + File.separator + source.getNewName()));
            //Done
            if (metadataManager != null) {
                setCurrentMessage("Writting out metadata...");
                metadataManager.close();
            }
            setCurrentMessage("Done migrating...");
            done = true;
            return true;
        } catch (Exception ex) {
            addError(ex.getLocalizedMessage());
            Logger.getLogger(DataMigrator.class.getName()).
                    log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            return false;
        } catch (OutOfMemoryError oome) {
            destination = null;
            source.deleteRecursively();
            source = null;
            metadataManager.cancel();
            metadataManager = null;
            addError("Java ran out of memory. "
                    + "Use the -Xmx option to increase available memory.");
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
    public void recurseDirectories(Metadata src, File destDir)
            throws Exception {

        if (cancelled) {
            throw new Exception("Migration Cancelled");
        }
        setCurrentMessage("Migrating " + src.getName());
        if (metadataManager != null) {
            metadataManager.startDirectory(src);
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
            Metadata[] filesAndDirs = src.listMetadata();
            toMigrate += filesAndDirs.length;
            for (int i = 0; i < filesAndDirs.length; i++) {

                if (Thread.interrupted()) {
                    String message = "Migration of "
                            + filesAndDirs[i].getAbsolutePath()
                            + " interupted";
                    System.out.println(message);
                    throw new InterruptedException(message);
                }

                if (filesAndDirs[i].isExcluded()) {
                    //Ignore excluded files and folders
                } else if (filesAndDirs[i].isDirectory()) {
                    //Find new destination directory
                    File newDestDir = new File(destDir.getAbsolutePath()
                            + File.separator + filesAndDirs[i].getName());
                    newDestDir.mkdirs();
                    recurseDirectories(filesAndDirs[i], newDestDir);
                } else if (filesAndDirs[i].isFile()) {
                    destDir.mkdirs();
                    String dest = destDir.getAbsolutePath() + File.separator
                            + filesAndDirs[i].getNewName();
                    copyFile(filesAndDirs[i], dest);
                } else {
                    String note =
                            filesAndDirs[i].getPath()
                            + " does not appear to be either a file or a"
                            + " directory and will be skipped.";
                    addError(note);
                }
                migrated++;
            }
            try {
                destDir.setLastModified(src.lastModified());
            } catch (Exception e) {
                String message = "Unable to set last modified date ("
                        + destDir + "): " + e.getLocalizedMessage();
                addError(message);
            }
        } catch (FileNotFoundException fnfe) {
            String note = "The directory " + src.getName() + " was not found";
            addError(note);
        } finally {
            if (metadataManager != null) {
                metadataManager.endDirectory();
            }
        }
    }

    public void copyFile(Metadata src, String dest)
            throws Exception {
        if (cancelled) {
            throw new Exception("Migration Cancelled");
        }
        if (!src.isFile()) {
            System.err.println(src.getPath() + " is not a file.");
        }
        if (!src.canRead()) {
            System.err.println("Cannot read " + src.getPath());
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
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream is = new FileInputStream(src);
            byte[] buffer = new byte[8192];
            int read = 0;
            String inMD5 = "";
            setCurrentMessage("Creating initial MD5: " + src.getName());
            try {
                while ((read = is.read(buffer)) > -1) {
                    digest.update(buffer, 0, read);
                }
                byte[] md5sum = digest.digest();
                inMD5 = "";
                for (int i = 0; i < md5sum.length; i++) {
                    inMD5 += Integer.toString((md5sum[i] & 0xff) + 0x100, 16).
                            substring(1);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to process source file "
                        + src.getName() + " for MD5", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close input stream for MD5 calculation",
                            e);
                }
                digest.reset();
                buffer = new byte[8192];
                read = 0;
            }
            src.setMD5(inMD5);

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
                        "Unable to set last modified date ("
                        + newFile + "): "
                        + e.getLocalizedMessage();
                addError(message);
                src.addQualifiedMetadata("error", "", message);
            }

            //Release some resources
            in = null;
            out = null;

            //Generate hash for transfered file and check
            setCurrentMessage("Creating migrated MD5: " + src.getName());
            is = new FileInputStream(newFile);
            String outMD5 = "";
            try {
                while ((read = is.read(buffer)) > -1) {
                    digest.update(buffer, 0, read);
                }
                byte[] md5sum = digest.digest();
                outMD5 = "";
                for (int i = 0; i < md5sum.length; i++) {
                    outMD5 += Integer.toString((md5sum[i] & 0xff) + 0x100, 16).
                            substring(1);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to process migrated "
                        + newFile.getName() + " for MD5", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close input stream for MD5 calculation",
                            e);
                }
            }

            if (inMD5.compareTo(outMD5) != 0) {
                String message = "Transfer failed, the copied file MD5"
                        + " did not match: " + inMD5 + " != " + outMD5;
                throw new Exception(message);
            } else {
                src.setMD5(inMD5);
            }

            if (metadataManager != null) {
                if (src != null) {
                    metadataManager.startFile(src);
                    if (verbose) {
                        System.out.println("Recording " + src.getName());
                    }
                }
            }

            //File-level plugins run here.
            for (Adapter adapter : selectedAdapters) {
                metadataManager.addXML(adapter.runFile(dest));
            }
            metadataManager.endFile();
        } catch (FileNotFoundException fnfe) {
            String note = "The file " + src.getPath() + " was not found";
            addError(note);
            return;
        } catch (OutOfMemoryError bounded) {
            src = null;
            dest = null;
            System.gc();
            String note =
                    "The JVM ran out of memory while migrating or creating/checking metadata"
                    + " (" + bounded.getMessage() + ")";
            addError(note);
            System.err.println("The JVM ran out of memory while migrating or creating/checking metadata for "
                    + src.getAbsoluteFile());
            System.err.println("\t" + bounded.getMessage());
            System.err.println("\tJVM upper level is " + Runtime.getRuntime().
                    totalMemory());
            bounded.printStackTrace();
        } catch (Exception e) {
            String note = "An error occured while migrating " + src.getName()
                    + ". " + e.getMessage();
            if (metadataManager != null) {
                if (!(src == null)) {
                    metadataManager.addXML(note);
                }
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
            throw new FileNotFoundException("Directory does not exist: "
                    + aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: "
                    + aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: "
                    + aDirectory);
        }
    }

    private void addError(String note) {
        if (note == null){
            note = "Unkown (null) Error.";
        }
        Logger.getLogger(DataMigrator.class.getName()).log(Level.WARNING, note);
        errors.add(note);
        if (metadataManager != null) {
            metadataManager.addNote(note);
        }
    }

    public ArrayList<String> getErrors() {
        return errors;
    }
}