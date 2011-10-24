package edu.duke.archives.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Seth Shaw
 */
public class FileWrapper extends File {

    private FileWrapper parentMetadata;
    private String newName = null;
    private TreeMap<String, FileWrapper> childrenMetadata;
    private Map<String, String> checksums = new TreeMap<String, String>();
    private ArrayList<QualifiedMetadata> qualifiedMetadata =
            new ArrayList<QualifiedMetadata>();
    

    private boolean excluded = false; //Include by default

    public FileWrapper(String pathname) {
        super(pathname);
    }

    public FileWrapper(String pathname, boolean excluded) {
        super(pathname);
        setExcluded(excluded);
    }

    public FileWrapper(FileWrapper metadata, String child) {
        super(metadata, child);
        this.parentMetadata = metadata;
        //By default set child to parent's exclude setting
        this.setExcluded(metadata.excluded);
    }

    public FileWrapper(File parent, String child) {
        super(parent, child);
        this.parentMetadata = (FileWrapper) parent;
    }

    public void addQualifiedMetadata(String namespace, String element, String qualifier, String value) {
        qualifiedMetadata.add(new QualifiedMetadata(namespace, element, qualifier, value));
    }

    public void addQualifiedMetadata(String element, String qualifier, String value) {
        qualifiedMetadata.add(new QualifiedMetadata(null, element, qualifier, value));
    }

    public Map getChecksums(){
        return checksums;
    }
    
    public String getChecksum(String algorithm){
        return (checksums.get(algorithm) == null) ? "" : (String) checksums.get(algorithm);
    }
    
    /**
     * @return the MD5
     */
    @Deprecated
    public String getMD5() {
        return (checksums.get("MD5") == null)? "" : (String) checksums.get("MD5");
    }

    public void setChecksum(String algorithm, String value){
        checksums.put(algorithm, value);
    }
    
    /**
     * @param md5 the MD5 to set
     */
    @Deprecated
    public void setMD5(String md5) {
        checksums.put("MD5", md5);
    }


    public void setExcluded(boolean excluded) {
        if (excluded) {
            this.excluded = excluded;
        // No need to recurse down: children will never be seen anyway.
        } else {
            this.excluded = false;
            // This will recurse up and set necessary directories to "include"
            if ((this.parentMetadata != null) && this.parentMetadata.excluded) {
                this.parentMetadata.setExcluded(false);
            }
        }
    }

    public boolean isExcluded() {
        return this.excluded;
    }

    protected void setParentMetadata(FileWrapper parentMetadata) {
        this.parentMetadata = parentMetadata;
    }

    public FileWrapper[] listMetadata() {
        String[] childrenList = list();
        if (childrenList.length != getChildrenMetadata().values().size()) {
            //Add any children unaccounted for
            TreeMap<String, FileWrapper> newChildMetadata = new TreeMap<String, FileWrapper>();
            for (int i = 0; i < childrenList.length; i++) {
                if (childrenMetadata.containsKey(childrenList[i])) {
                    newChildMetadata.put(childrenList[i], childrenMetadata.get(childrenList[i]));
                } else {
                    FileWrapper childM = new FileWrapper(this, childrenList[i]);
                    newChildMetadata.put(childM.getName(), childM);
                }
            }
            //Remove any qualifiedMetadata children not listed
            childrenMetadata = newChildMetadata;
        }
        return childrenMetadata.values().toArray(new FileWrapper[childrenMetadata.size()]);
    }

    protected TreeMap<String, FileWrapper> getChildrenMetadata() {
        if (childrenMetadata == null){
            childrenMetadata = new TreeMap<String, FileWrapper>();
        }
        return childrenMetadata;
    }

    protected void setChildrenMetadata(TreeMap<String, FileWrapper> childrenMetadata) {
        this.childrenMetadata = childrenMetadata;
    }

    public List<QualifiedMetadata> getQualifiedMetadata() {
        return this.qualifiedMetadata;
    }
    
    /**
     * @return the newName
     */
    public String getNewName() {
        if (newName == null) {
            return this.getName();
        } else {
            return newName;
        }
    }

    /**
     * @param newName the newName to set
     */
    public void setNewName(String newName) {
        this.newName = newName;
    }

    public boolean getAllowsChildren() {
        if (this.isFile()) {
            return false;
        }
        return true;
    }

    public int getChildCount() {
        return list().length;
    }

    public boolean isLeaf() {
        if (this.isFile()) {
            return true;
        }
        return false;
    }
    
    public synchronized boolean deleteRecursively() {
        parentMetadata = null;
        qualifiedMetadata = null;
        checksums.remove("MD5");
        if (this.isDirectory()) {
            for (String path : childrenMetadata.keySet()) {
                FileWrapper child = childrenMetadata.remove(path);
                if (!child.deleteRecursively()) {
                    return false;
                }
            }
            childrenMetadata = null;
        }
        return this.delete();
    }
}
