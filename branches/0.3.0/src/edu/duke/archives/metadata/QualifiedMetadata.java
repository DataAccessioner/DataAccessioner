package edu.duke.archives.metadata;

/**
 *
 * @author Seth Shaw
 */
public class QualifiedMetadata {

    String nameSpace;
    String element;
    String qualifier;
    String value = "";

    /**
     * @return the nameSpace
     */
    public String getNameSpace() {
        return nameSpace;
    }

    /**
     * @param nameSpace the nameSpace to set
     */
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    /**
     * @return the element
     */
    public String getElement() {
        return element;
    }

    /**
     * @param element the element to set
     */
    public void setElement(String element) {
        this.element = element;
    }

    /**
     * @return the qualifier
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * @param qualifier the qualifier to set
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @param nameSpace
     * @param element
     * @param qualifier
     * @param value
     */
    public QualifiedMetadata(String nameSpace, String element,
            String qualifier, String value) {
        this.nameSpace = nameSpace;
        this.element = element;
        this.qualifier = qualifier;
        this.value = value;
    }
}

