package edu.duke.archives.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.jdom.Element;
import org.jdom.Namespace;

/**
 *
 * @author Seth Shaw
 */
public class Metadata extends File {

    private Metadata parentMetadata;
    private String newName = null;
    private TreeMap<String, Metadata> childrenMetadata;
    private String md5;
    private ArrayList<Element> qualifiedMetadata =
            new ArrayList<Element>();
    private static final Namespace DC_NAMESPACE = Namespace.getNamespace("dc",
            "http://purl.org/dc/elements/1.1/");
    private static final Namespace DEFAULT_NAMESPACE = Namespace.getNamespace("http://dataaccessioner.org/schema/dda-0-3-1");

    private boolean excluded = false; //Include by default

    public Metadata(String pathname) {
        super(pathname);
    }

    public Metadata(String pathname, boolean excluded) {
        super(pathname);
        setExcluded(excluded);
    }

    public Metadata(Metadata metadata, String child) {
        super(metadata, child);
        this.parentMetadata = metadata;
        //By default set child to parent's exclude setting
        this.setExcluded(metadata.excluded);
    }

    public Metadata(File parent, String child) {
        super(parent, child);
        this.parentMetadata = (Metadata) parent;
    }

    public void addQualifiedMetadata(Namespace namespace, String element, String qualifier, String value) {
        if(element == null){ return; }
        Element qm = new Element(element, namespace);
        if(qualifier != null){
        qm.setAttribute("qualifier", qualifier);
        }
        if(value != null){
        qm.addContent(value);
        }
        qualifiedMetadata.add(qm);
    }

    public void addQualifiedMetadata(String element, String qualifier, String value) {
        addQualifiedMetadata(DEFAULT_NAMESPACE, element, qualifier, value);
    }

    /**
     * @return the MD5
     */
    public String getMD5() {
        return md5;
    }

    /**
     * @param md5 the MD5 to set
     */
    public void setMD5(String md5) {
        this.md5 = md5;
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

    protected void setParentMetadata(Metadata parentMetadata) {
        this.parentMetadata = parentMetadata;
    }

    public Metadata[] listMetadata() {
        String[] childrenList = list();
        if (childrenList.length != getChildrenMetadata().values().size()) {
            //Add any children unaccounted for
            TreeMap<String, Metadata> newChildMetadata = new TreeMap<String, Metadata>();
            for (int i = 0; i < childrenList.length; i++) {
                if (childrenMetadata.containsKey(childrenList[i])) {
                    newChildMetadata.put(childrenList[i], childrenMetadata.get(childrenList[i]));
                } else {
                    Metadata childM = new Metadata(this, childrenList[i]);
                    newChildMetadata.put(childM.getName(), childM);
                }
            }
            //Remove any qualifiedMetadata children not listed
            childrenMetadata = newChildMetadata;
        }
        return childrenMetadata.values().toArray(new Metadata[childrenMetadata.size()]);
    }

    protected TreeMap<String, Metadata> getChildrenMetadata() {
        if (childrenMetadata == null){
            childrenMetadata = new TreeMap<String, Metadata>();
        }
        return childrenMetadata;
    }

    protected void setChildrenMetadata(TreeMap<String, Metadata> childrenMetadata) {
        this.childrenMetadata = childrenMetadata;
    }

    public List<Element> getQualifiedMetadata() {
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
        md5 = null;
        if (this.isDirectory()) {
            for (String path : childrenMetadata.keySet()) {
                Metadata child = childrenMetadata.remove(path);
                if (!child.deleteRecursively()) {
                    return false;
                }
            }
            childrenMetadata = null;
        }
        return this.delete();
    }
}
