Updating FITS for Data Accessioner
==================================

*Last update:  Feb. 3, 2017*

Data Accessioner is basically a Java GUI wrapped around FITS 
(the Harvard *File Information Tool Set*);  FITS itself is a
wrapper around a number of open source file identification
toolkits.

Data Accessioner:  http://www.dataaccessioner.org/

FITS:  http://projects.iq.harvard.edu/fits/home

Data Accessioner (as of version 1.1) contains FITS 1.0.4, with the
following tools enabled:

* ADL Tool
* WebVTT Tool
* Droid
* JHove (version 1.14.7)
* File Utility
* ExifTool (version 10.37)
* NLNZ Metadata Extractor (version 3.6GA)
* OIS File Information
* OIS XML Information
* OIS File Information
* FFIdent
* Apache Tika

All of the tools are the versions distributed with FITS, with the 
exception of the ones with versions listed above, which have been updated
for Data Accessioner 1.1.

FITS Tools page:  http://projects.iq.harvard.edu/fits/fits/tools

## FITS background:  Technical Information

FITS separates its toolkits into two directories:

  * `lib/`:  jar files for the included tools.  
     Many of the tools are segregated into their own subdirectories, to
     avoid conflicts and allow for easier upgrade of a single tool.  
     Shared libraries and scripts are at the top level of this directory.
  * `tools/`:  configuration files for individual tools, and tools
     that are not Java-based (perl, compiled binaries, etc.)  Each tool
     will have its own subdirectory.
    
In addition, FITS has an `xml/` directory, which contains per-tool stylesheets
to convert tool output to FITS XML, as well as some other tool configuration 
files.

The file `xml/fits.xml` is the master configuration file for FITS.  It 
lists the tools that FITS has at its disposal;  the tools higher in 
the list have priority over ones lower in the list, and will be run
first.

See http://projects.iq.harvard.edu/fits/fits/fits-configuration-files 
for more information on configuring FITS.

## Data Accessioner and FITS

DataAccessioner keeps the FITS `xml/` and `tools/` subdirectories, but
does not segregate jars files in their own `lib/` directory.  Jar files
are handled by Maven;  they are downloaded, the duplicates are removed, 
and the needed jars are included in the dataaccessioner jar file when 
the application is packaged.

### Dependency Management with Maven

Building DataAccessioner requires Maven version 3 or above.

The file `pom.xml` in the source code root contains all the Maven
dependencies for both Data Accessioner and FITS.

Versions for the tools and their jar files are specified near the top
of the file, in the `<properties>` element.  

An effort has been made to retrieve as many dependencies as possible
from the Maven Central Repository.  For many tools and jars,
simply updating the dependency's version in the `pom.xml` file and 
rebuilding DataAccessioner will suffice.

However, many of the jars included in FITS are proprietary, of unknown 
provenance, or older, obsolete versions.  For these, there is a Data
Accessioner maven repository to host them: http://www.dataaccessioner.org/maven2/

**Uploading a new jar to the DataAccessioner Maven Repository**

Maven should be used to deploy jars to the DA Maven Repository.  In 
order to do that, you'll need ssh access to the dataaccessioner.org 
host;  Seth Shaw (owner of the Data Accesioner project) can help you 
get set up.

Before you deploy your first artifact, add the following server stanza 
to your maven settings file `~/.m2/settings.xml` on the host from which 
you'll be pushing up the artifacts:

     <server>
       <id>da.repo</id>
       <username>someuser_data-accessioner</username>
       <password>yourpassword</password>
       <filePermissions>664</filePermissions>
       <directoryPermissions>775</directoryPermissions>
     </server>

To upload a jar:

    mvn deploy:deploy-file -DgroupId=org.dataaccessioner \
                           -DartifactId=somejar \
                           -Dversion=1.0 \
                           -DgeneratePom=true \
                           -Dpackaging=jar \
                           -Dfile=/local/path/to/somejar.jar \
                           -DrepositoryId=da.repo \
                           -Durl=scp://ssh.phx.nearlyfreespeech.net/home/public/maven2

Some notes on deployment:

* Use `org.dataaccessioner` as the groupID
* If known, use the jar's version for the version;  if not known, use the 
  version of the tool that requires this jar as a dependency (example:  a 
  FITS main dependency would have the version 1.04, the DA FITS version)


See https://maven.apache.org/guides/mini/guide-3rd-party-jars-remote.html 
for more information.

**Rebuilding DataAccessioner**:

Run this command at the source code directory root:

    mvn clean package

The jar and distribution zip file will be placed in the `target/` subdirectory.

## Updating a Tool

To update a specific FITS tool, follow this process:

First, update FITS alone and test:

*  Download FITS to a working directory
*  Update the tool's jars in the `lib/` directory
*  Update, if needed, any tool configuration files in the `tools/`
   and `xml/` directory
*  Update, if needed, the FITS xslt files for the tool in the `xml/`
   directory
*  Run FITS against some sample files, make sure the tool works as 
   expected.
   
Then incorporate the new tool into Data Accessioner:

*  Fork the DataAccessioner git repo, then check your fork out locally
*  Update any needed config files in `src/main/resources/tools/` and
   `src/main/resources/xml/`
*  Update any changed XSLT files in `src/main/resources/xml/`
*  Look at the new jar files installed for the tool in the FITS `lib/`
   directory, then:
   *  Update the versions of any dependencies already declared in `pom.xml`
   *  Add new dependencies available in Maven Central Repository to the 
      section for the tool's dependencies in `pom.xml`
   *  OR:  Upload any new dependencies to the Data Accessioner maven
      repository and add the dependencies to `pom.xml`
*  Build dataaccessioner and test
*  Commit and push your changes to your forked git repository
*  Submit a pull request to the `seth-shaw/dataaccessioner` repository

## Updating the DROID signature file

This is simple, and does not need to be compiled into the code.  It can
be updated on any installed instance of DataAccessioner.

1.  Download the latest version of the signature file at 
    https://www.nationalarchives.gov.uk/aboutapps/pronom/droid-signature-files.htm
2.  Copy it to `DATAACCESIONER_HOME/tools/droid/DROID_SignatureFile_V##.xml`, 
    where `##` is the version number.
3.  Edit the `DATAACCESIONER_HOME/tools/droid/DROID_config.xml` file, 
    updating the `<SigFile>` and `<SigFileVersion>` elements.

## Updating FITS

Updating FITS is basically the same process as updating any given 
tool in FITS, with the following additions:

* Examine and update FITS core jar dependencies in `pom.xml`, also
* Merge changes to the FITS config file `fits.xml` with the existing
  Data Accessioner `fits.xml` file.  A few of the settings have been
  changed from the defaults for DataAccessioner purposes.
* Preserve the following files that are DataAccessioner-specific:

      tools/log4j.properties
      xml/metadataManager.xsl
      xml/dda-1-1.xsd
      
## Other Approaches

The current strategy of getting jars from Maven Central repository 
where possible, and uploading the rest to dataccessioner.org where
required, is time-consuming and can require many hours of detective
work.  It may make more sense to simply copy all the jars in any given
version of FITS wholesale up to the DataAccessioner maven repository,
then refer to those dependencies only in the POM file.