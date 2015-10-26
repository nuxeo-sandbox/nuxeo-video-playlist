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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.core.adapter.Collection;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.video.VideoConstants;

/**
 * We build a list of Video, as JSON. We also just return the info "has video list", which means
 * "has at least one video to play".
 * <p>
 * We search videos in the current document, which is supposed to be Folderish:
 * <ul>
 * <li>First, we check the document has the "palette" facet (cf. nuxeo-patette plug-in). If yes, we use the dedicated
 * operation to get the list of ordered documents, and build the list of Videos</li>
 * <li>Second, if the document does not use nuxeo-palette, we check if is a Collection. If yes, we get the list of docs
 * with the "Video" facet in the collection</li>
 * <li>Then, if none of the above found at least one video, we try to find some in the children o the current document<br/>
 * <b>IMPORTANT</b>: When handling Folderish, the plug-in does not handle versions. The code explicitly ignore versions when checking for
 * available videos.</li>
 * </ul>
 * <p>
 * About nuxeo-palette: We don't want to link to this plug-in and create a dependency to install it. This is why we just
 * test the facet ("palette") and possibly use the operation provided by nuxeo-palette. Which means there is one
 * potential problem: If nuxeo-palette, for some reason I can't imagine ;-), change the ID of the operation or the name
 * of the facet, we will have to change it here.
 * 
 * @since 7.4
 */
@Name("videoPlaylist")
@Scope(ScopeType.EVENT)
public class VideoPlaylistBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(VideoPlaylistBean.class);

    // See above explanations about nuxeo-palette
    public static final String PALETTE_FACET = "palette";

    public static final String PALETTE_GET_ITEMS_OPERATION = "Services.GetPaletteItems";

    // PARENT_ID to be replaced
    private static final String NXQL_FOR_FOLDERISH = "SELECT * From Document WHERE ecm:parentId = 'PARENT_ID'"
            + " AND ecm:mixinType = '" + VideoConstants.VIDEO_FACET + "' AND ecm:mixinType != 'HiddenInNavigation'"
            + " AND ecm:isVersion = 0 AND ecm:currentLifeCycleState != 'deleted'"
            + " AND file:content/name IS NOT NULL";

    protected ArrayList<VideojsDocumentMapper> videos = null;

    protected static Boolean nuxeoPaletteIsInstalled = null;

    protected Boolean hasAtLeastOneVideo = null;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected AutomationService automationService;

    protected DocumentModel currentDocument = null;

    @Create
    public void initialize() {

        currentDocument = navigationContext.getCurrentDocument();
    }

    protected boolean hasNuxeoPalette() {

        if (nuxeoPaletteIsInstalled == null) {
            nuxeoPaletteIsInstalled = false;
            try {
                automationService.getOperation(PALETTE_GET_ITEMS_OPERATION);
                nuxeoPaletteIsInstalled = true;
            } catch (OperationNotFoundException e) {
                // Nothing, already set to false
            }

            if (!nuxeoPaletteIsInstalled) {
                log.warn("Operation \"" + PALETTE_GET_ITEMS_OPERATION
                        + "\" from nuxeo-palette unavailable. The video playlist will not try to read from it");
            }
        }

        return nuxeoPaletteIsInstalled;
    }

    protected boolean hasVideo(DocumentModel inDoc) {
        boolean result = false;

        if (inDoc != null && inDoc.hasFacet(VideoConstants.VIDEO_FACET)
                && inDoc.getPropertyValue("file:content") != null & !inDoc.hasFacet("HiddenInNavigation")) {
            result = true;
        }

        return result;
    }

    // Looks a bit like buildPlayList(), but the aim of hasPlaylist() is to run as
    // fast as possible, so as soon we find a Video in the list, we return true.
    // Well: Except for a "nuxeo-palette" document, because it is already
    // complex to get the values (need to parse a JSON string), so let's build
    // the list now.
    public boolean hasPlaylist() {

        // Already built once, then all is good
        if (videos != null) {
            return videos.size() > 0;
        }

        // Already called hasPlaylist()
        if (hasAtLeastOneVideo != null) {
            return hasAtLeastOneVideo;
        }

        hasAtLeastOneVideo = false;

        /* ------------------------------------------------------------------ */
        /* ---------- (1) Check if we have a "palette" on this doc ---------- */
        /* ------------------------------------------------------------------ */
        if (!hasAtLeastOneVideo && hasNuxeoPalette() && currentDocument.hasFacet(PALETTE_FACET)) {

            OperationContext ctx = new OperationContext(documentManager);
            ctx.setInput(currentDocument);

            OperationChain chain = new OperationChain("nuxeoPlayListGetPalette");
            chain.add(PALETTE_GET_ITEMS_OPERATION);

            try {
                String jsonResult = (String) automationService.run(ctx, chain);
                // For nuxeo-palette, we build the list in hasPlaylist()
                videos = new ArrayList<VideojsDocumentMapper>();
                if (StringUtils.isNotBlank(jsonResult)) {
                    // Looking at nuxeo-palette documentation, the result of this operation is a JSON string containing
                    // an array of objects. Starts with "[".
                    JSONArray jsonArray = new JSONArray(jsonResult);
                    String docId;
                    DocumentModel doc;
                    for (int i = 0, max = jsonArray.length(); i < max; ++i) {
                        JSONObject jsonObj = jsonArray.getJSONObject(i);
                        docId = jsonObj.getString("id");
                        try {
                            doc = documentManager.getDocument(new IdRef(docId));
                            addToPlayList(doc);
                        } catch (DocumentNotFoundException e) {
                            // We just ignore this exception.
                        }
                    }
                    hasAtLeastOneVideo = videos.size() > 0;
                }
            } catch (OperationException e) {
                log.error("Failed to run operation <" + PALETTE_GET_ITEMS_OPERATION + ">", e);
            } catch (JSONException e) {
                log.error("Failed to parse the JSON result. Does the stirng starts with \"[\"?", e);
            }
        }

        /* ------------------------------------------------------- */
        /* ---------- (2) Check if we have a collection ---------- */
        /* ------------------------------------------------------- */
        if (!hasAtLeastOneVideo && currentDocument.hasSchema(CollectionConstants.COLLECTION_SCHEMA_NAME)) {

            DocumentModel doc;

            @SuppressWarnings("unchecked")
            List<String> docIds = (List<String>) currentDocument.getPropertyValue(CollectionConstants.COLLECTION_DOCUMENT_IDS_PROPERTY_NAME);
            for (String id : docIds) {
                try {
                    doc = documentManager.getDocument(new IdRef(id));
                    if (hasVideo(doc)) {
                        hasAtLeastOneVideo = true;
                        break;
                    }

                } catch (DocumentNotFoundException e) {
                    // We just ignore this exception.
                }
            }
        }

        /* --------------------------------------------------- */
        /* ---------- (3) Fallback: Is it a folder? ---------- */
        /* --------------------------------------------------- */
        if (!hasAtLeastOneVideo && currentDocument.hasFacet("Folderish")) {

            // The query handles facet, existence of a file, etc...
            String nxql = NXQL_FOR_FOLDERISH.replace("PARENT_ID", currentDocument.getId());
            DocumentModelList docs = documentManager.query(nxql);
            hasAtLeastOneVideo = docs.size() > 0;
        }

        /* ------------------------------------- */
        /* ---------- Ok, we are done ---------- */
        /* ------------------------------------- */
        return hasAtLeastOneVideo;

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
            DocumentModel doc;
            for (String id : documentIds) {
                try {
                    doc = documentManager.getDocument(new IdRef(id));
                    addToPlayList(doc);
                } catch (DocumentNotFoundException e) {
                    // We just ignore this exception.
                }
            }

        }

        if (videos.size() < 1 && currentDocument.hasFacet("Folderish")) {

            String nxql = NXQL_FOR_FOLDERISH.replace("PARENT_ID", currentDocument.getId());
            nxql += " ORDER BY dc:title";
            DocumentModelList docs = documentManager.query(nxql);
            for (DocumentModel doc : docs) {
                addToPlayList(doc);
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
