# nuxeo-plugintemplate

Structure of a plug-in with its Nuxeo Marketplace Package, useful when starting a new project.

**IMPORTANT**: This is a template building a static Nuxeo Marketplace Package, with no dependencies on external libraries, etc. For information about Marketplace Packages, their structure, how to build them etc., please see [this documentation](https://doc.nuxeo.com/x/CwIz).

### Usage
Duplicate this template.

Replace `plugintemplate` everywhere you can find it, with you own value (including in the names of dolers of course), then build the plug-in and import it in Eclipse.

For example, say you are building `nuxeo-super-stuff`

1. Replace everywhere (but, like, really everywhere) `plugintemplate` with `super-stuff`
2. Update the `.pom` files to change:
  * The version dependencies, if needed
  * The verison of your plug-in
3. Then:
    
    cd /path/to/the/nuxeo-super-stuff
    # Build to check there is no error
    mvn clean install
    # Possibly, fix errors (most of the time, you forgot to replace one "pluginsample")
    # Build for exlipse
    mvn eclipse:clean eclipse:eclipse
    # Now, you can import your project in Eclipse, add your classes, etc.
    


## License
(C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
Thibaud Arguillere (https://github.com/ThibArg)

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com) and packaged applications for Document Management, Digital Asset Management and Case Management. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.
