/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 */
package org.nuxeo.video.playlist;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.video.VideoConstants;

@Name("videoPlaylist")
@Scope(ScopeType.EVENT)
public class VideoPlaylistBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VideoPlaylistBean.class);

    protected ArrayList<VideojsDocumentMapper> videos = null;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    protected DocumentModel currentDocument = null;

    @Create
    public void initialize() throws ClientException {

        currentDocument = navigationContext.getCurrentDocument();
    }

    protected boolean hasVideo(DocumentModel inDoc) {
        boolean result = false;

        if (inDoc != null && inDoc.hasFacet(VideoConstants.VIDEO_FACET)
                && inDoc.getPropertyValue("file:content") != null & !inDoc.hasFacet("HiddenInNavigation")
                && !inDoc.isVersion()) {
            result = true;
        }

        return result;
    }

    /*
     * Looks a bit like buildPlayList(), but the aim of hasPlaylist() is to run as fast as possible, so as soone we find
     * a Video in the list, we return true.
     */
    public boolean hasPlaylist() {

        boolean looksOk = false;

        // Already built once, then all is good
        if (videos != null && videos.size() > 0) {
            return true;
        }

        // We just check without creating the VideojsDocumentMapper objects etc..
        // The final list is build only when we really need it, not when a caller just checks.
        if (currentDocument.hasSchema(CollectionConstants.COLLECTION_SCHEMA_NAME)) {

            DocumentModel doc;

            @SuppressWarnings("unchecked")
            List<String> docIds = (List<String>) currentDocument.getPropertyValue(CollectionConstants.COLLECTION_DOCUMENT_IDS_PROPERTY_NAME);
            for (String id : docIds) {
                try {
                    doc = documentManager.getDocument(new IdRef(id));
                    if (hasVideo(doc)) {
                        return true;
                    }

                } catch (Exception e) {
                    // TODO
                    // When moving to a version > 7.4, remove Exception, use the correct exception
                }
            }

        } else if (currentDocument.hasSchema("HERE THE SCHEMA FROM FRED's WORK")) {
            
            // =================================================
            // Other fields?...
            // =================================================
            
        } else if (currentDocument.hasFacet("Folderish")) {

            DocumentModelList docs = documentManager.getChildren(currentDocument.getRef());
            for (DocumentModel doc : docs) {
                if (hasVideo(doc)) {
                    return true;
                }
            }
        }

        return looksOk;

    }

    /*
     * Looks a bit like hasPlaylist(), but buildPlayList() actually builds the list, while the aim of hasPlaylist() is
     * to run as fast as possible
     */
    protected void buildPlayList() {

        if (videos != null) {
            return;
        }

        videos = new ArrayList<VideojsDocumentMapper>();
        if (currentDocument.hasSchema(CollectionConstants.COLLECTION_SCHEMA_NAME)) {

            Collection collectionAdapter = currentDocument.getAdapter(Collection.class);
            List<String> documentIds = collectionAdapter.getCollectedDocumentIds();
            VideojsDocumentMapper vjsMapper;
            DocumentModel doc;
            for (String id : documentIds) {
                try {
                    doc = documentManager.getDocument(new IdRef(id));
                    addToPlayList(doc);
                } catch (Exception e) {
                    // TODO
                    // When moving to a version > 7.4, remove Exception, use the correct exception
                }
            }

        } else if (currentDocument.hasSchema("HERE THE SCHEMA FROM FRED's WORK")) {
            
            // =================================================
            // Other fields?...
            // =================================================
            
        } else if (currentDocument.hasFacet("Folderish")) {

            DocumentModelList docs = documentManager.getChildren(currentDocument.getRef());
            for (DocumentModel doc : docs) {
                if (hasVideo(doc)) {
                    addToPlayList(doc);
                }
            }
        }
    }
    
    
    protected void addToPlayList(DocumentModel inDoc) {
        
        VideojsDocumentMapper vjsMapper = new VideojsDocumentMapper(inDoc);
        if (vjsMapper.canBeUsedWithVideojs()) {
            videos.add(vjsMapper);
        }
        
    }

    public String getPlaylistAsJsonString() throws IOException {

        buildPlayList();

        String json = "[";

        int max = videos.size();
        if (max > 0) {
            // First element of the array
            json += videos.get(0).getJsonString();
            // Other elements start with a comma
            for (int i = 1; i < max; ++i) {
                json += "," + videos.get(i).getJsonString();
            }
        }

        json += "]";

        return json;
    }

}
