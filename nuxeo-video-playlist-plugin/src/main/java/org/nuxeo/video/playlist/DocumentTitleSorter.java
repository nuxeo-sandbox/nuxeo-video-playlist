/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */
package org.nuxeo.video.playlist;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.api.model.PropertyException;

/**
 * 
 * @since 7.3
 */
public class DocumentTitleSorter implements Sorter {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(DocumentModel o1, DocumentModel o2) {
        
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        }
        
        try {
            String t1 = (String) o1.getPropertyValue("dc:title");
            String t2 = (String) o2.getPropertyValue("dc:title");
            
            return t1.compareTo(t2);
        
        } catch (PropertyException e) {
            // Ignore
            return 0;
        }
    }
    
}
