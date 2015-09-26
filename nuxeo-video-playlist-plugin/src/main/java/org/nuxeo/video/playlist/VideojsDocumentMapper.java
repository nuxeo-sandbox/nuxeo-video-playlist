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
 *     Thibaud Arguillere
 */
package org.nuxeo.video.playlist;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.ecm.platform.video.VideoDocument;

/**
 * Mapp informations for a video to be played in video JS: List os sources, poster and title
 * 
 * @since 7.3
 */
public class VideojsDocumentMapper {

    private static final Log log = LogFactory.getLog(VideojsDocumentMapper.class);

    protected ArrayList<String> sources;

    protected String poster;

    protected String title;

    protected DocumentModel doc = null;

    // They will be the first in list
    protected static final ArrayList<String> MAIN_FORMATS = new ArrayList<String>(
            Arrays.asList("MP4 480p", "WebM 480p"));

    public VideojsDocumentMapper(DocumentModel inDoc) {
        doc = inDoc;

        fillInfos();
    }

    protected void fillInfos() {

        sources = new ArrayList<String>();
        poster = "";
        title = "";

        if (!doc.hasFacet(VideoConstants.VIDEO_FACET)) {
            return;
        }

        title = doc.getTitle();
        String lastModification = "" + (((Calendar) doc.getPropertyValue("dc:modified")).getTimeInMillis());
        poster = DocumentModelFunctions.fileUrl("downloadPicture", doc, "StaticPlayerView:content", lastModification);

        TranscodedVideo tv;
        String blobPropertyName;
        String url;
        VideoDocument videoDocument = doc.getAdapter(VideoDocument.class);
        // Lets first get the 2 most "popular" (2 that should be here)
        // mp4 480p and WebM 480
        for (String format : MAIN_FORMATS) {
            tv = videoDocument.getTranscodedVideo(format);
            if (tv != null) {
                blobPropertyName = tv.getBlobPropertyName();
                url = DocumentModelFunctions.bigFileUrl(doc, blobPropertyName, tv.getBlob().getFilename());
                sources.add(url);
            }
        }
        // Now, add the others
        Collection<TranscodedVideo> allTranscoded = videoDocument.getTranscodedVideos();
        for (TranscodedVideo oneTranscoded : allTranscoded) {
            if (!MAIN_FORMATS.contains(oneTranscoded.getName())) {
                blobPropertyName = oneTranscoded.getBlobPropertyName();
                url = DocumentModelFunctions.bigFileUrl(doc, blobPropertyName, oneTranscoded.getBlob().getFilename());
                sources.add(url);
            }
        }

        // And add the main video
        Blob mainBlob = (Blob) doc.getPropertyValue("file:content");
        url = DocumentModelFunctions.bigFileUrl(doc, "file:content", mainBlob.getFilename());
        sources.add(url);

    }

    public boolean canBeUsedWithVideojs() {
        return sources.size() > 0;
    }

    public String getJsonString() throws IOException {

        String json = "";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = new JsonFactory().createJsonGenerator(out, JsonEncoding.UTF8);

        jg.writeStartObject();
        jg.writeArrayFieldStart("src");
        for (String src : sources) {
            jg.writeString(src);
        }
        jg.writeEndArray();
        jg.writeStringField("poster", poster);
        jg.writeStringField("title", title);
        jg.writeEndObject();

        jg.close();
        json = out.toString(StandardCharsets.UTF_8.name());

        return json;
    }
}
