# Data Accessioner

The DataAccessioner is a simple Java Swing GUI to allow archivists 
and digital preservationists to migrate data off disks and onto a 
file server for basic preservation, further appraisal, arrangement, 
and description.

It provides a a wrapper around the FITS toolkit 
<http://projects.iq.harvard.edu/fits/home> to integrate common 
metadata tools to analyze and describe found content at the time 
of migration.

Home: <http://www.dataaccessioner.org>

## Build prerequisites

  * A recent version of Java (>= 1.8)
  * Perl (for exiftool)
  * Maven (>= 3.0)

## Build

Data Accessioner (>= 1.1) is built using Maven.  To build, clone the
repository, then execute the following command in the top-level
directory:

    mvn clean package

A distributable zip file will be built and placed in the `target/`
subdirectory, with the name `dataaccessioner-<version>-dist.zip`.

For testing purposes, you may run the created jar in the `target/`
folder directly, first setting the FITS_HOME environment variable:

   FITS_HOME=src/main/resources java -jar target/dataaccessioner-1.1.jar

## Install

Unzip the distribution zip file in the location of your choice.
All dependencies are included within the package.

## Run

Run `start.sh` or `start.bat` in the install directory.

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request

## History

The very first version of the tool was written in under the 
auspices of Duke University Archives in early 2008. In January 
2009 the Data Accessioner was revisited and refactored with a 
revised architecture, and the metadata tool adapters and the 
custom metadata manager were extracted to be used as plugins.

Versions 0.3.1 and 1.0 were made possible by the POWRR Project
<http://digitalpowrr.niu.edu/>;  at that point, the tool became
an open source application, unaffiliated to any institution.  
Version 1.0 is the first version to use FITS as a metadata tool 
wrapper.

Version 1.1 was made possible by the Archives of the Episcopal
Church of America.  It was refactored to use Maven to manage 
dependencies, and contains several minor enhancements.

## Credits

[Seth Shaw](https://github.com/seth-shaw)
[Scott Prater](https://github.com/sprater)

## License

Copyright © 2014, 2017 by Seth Shaw.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  
02110-1301  USA
