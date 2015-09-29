/* nuxeo-video-playlist.js
 *
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaud Arguillere
 *     
 * Most of this code is based on the example in Videojs Playlist (http://belelros.github.io/videojs-playLists/)
 */

/*
 * We are running inside a fancybox opened by someone else (Nuxeo, actually), so we don't
 * handle the fancybox itself, it's main div id, events etc. This is why we must make sure
 * our NxVPL main object is initialized only once, and reset each time the box is open
 * (see video-playlist.xhtml).
 * 
 * The main issue is: We are called twice. Once before the fancybox shows itself, once after,
 * while we don't want to initialize everything 2 times, mainly because we don't handle
 * dependencies (videojs and videojs-playlist). Well, we know that initializing videojs-playlist is bad (navigating the the next video +> goes to next + next)
 * 
 * This is why, until we understand everything ;->, we are using tests and flags
 */
// The .xhtml sets ids avec our divs with this suffix
// We rebuild the IDs and get access to the divs dynamically
// => When ID_SUFFIX is used, check video-playlist.xhtlm for the perfix
var ID_SUFFIX;
var MAIN_DIV_ID, VIDEO_ID, PLAYLIST_ID, FIRST_BUTTON_ID, PREV_BUTTON_ID, NEXT_BUTTON_ID, LAST_BUTTON_ID;

var gVideos = [], gPlayer, gEls = {}, gDragSrcElt, gVideosAfterDD;

if (typeof NcVPL_INIT_COUNTER === "undefined" || NcVPL_INIT_COUNTER === null) {
    NcVPL_INIT_COUNTER = 0;
}

function NuxeoVideoPlaylist_Init(inIdSuffix, inPlaylistJson) {

    ID_SUFFIX = inIdSuffix;

    MAIN_DIV_ID = "vpl-mainDiv-" + ID_SUFFIX;
    VIDEO_ID = "vpl-video-" + ID_SUFFIX;
    PLAYLIST_ID = "vpl-playlist-" + ID_SUFFIX;

    // These one are hard-coded, also in the css
    FIRST_BUTTON_ID = "vpl-first";
    PREV_BUTTON_ID = "vpl-prev";
    NEXT_BUTTON_ID = "vpl-next";
    LAST_BUTTON_ID = "vpl-last";

    gVideos = inPlaylistJson;

    var fancybox = jQuery("#fancybox-content");
    // "Real" init. must occur when the fancybox is visible. NxVPL.init() is called at
    // the end of these declarations
    if ((typeof NxVPL === "undefined" || NxVPL === null) && fancybox && fancybox.is(":visible")) {

        NcVPL_INIT_COUNTER += 1;
        if ((NcVPL_INIT_COUNTER % 2) !== 0) {
            console.log("Called for re-init => not doing it");
            return;
        }

        //console.log("INIT NxVPL");
        NxVPL = {

            init : function() {
                gPlayer = vjs(document.getElementById(VIDEO_ID));
                
                NxVPL.cacheElements();
                NxVPL.initVideos();
                NxVPL.createListOfVideos();
                NxVPL.bindEvents();

            },

            cacheElements : function() {
                gEls = {};
                gEls.$playlist = jQuery('div.playlist > ul');
                gEls.$first = jQuery(document.getElementById(FIRST_BUTTON_ID));
                gEls.$prev = jQuery(document.getElementById(PREV_BUTTON_ID));
                gEls.$next = jQuery(document.getElementById(NEXT_BUTTON_ID));
                gEls.$last = jQuery(document.getElementById(LAST_BUTTON_ID));
            },

            initVideos : function() {
                // The array received by videojs-playlist is kept by reference (playlist doesn not do a deep copy).
                // And as it modifies the array, to add some custom infos, we must pass a deep-copy, because later,
                // if the user drag-drop, we reuse the original gVideos
                var deepCopy = JSON.parse(JSON.stringify(gVideos));; //jQuery.extend(true, {}, gVideos);
                gPlayer.playList(deepCopy);
                //gPlayer.playList(gVideos);
            },

            createListOfVideos : function() {
                var html = '';
                for (var i = 0, len = gVideos.length; i < len; i++) {
                    /*
                    html += '<li id="vpl-video-' + i + '" data-videoplaylist="' + i + '">' + '<span class="number">' + (i + 1) + '</span>'
                            + '<span class="poster"><img src="' + gVideos[i].poster + '"></span>'
                            + '<span class="title">' + gVideos[i].title + '</span>' + '</li>';*/
                    html += '<li data-videoplaylist="' + i + '">' + '<div id="vpl-video-' + i + '" draggable=true>'
                            + '<span class="number">' + (i + 1) + '</span>' + '<span class="poster"><img src="'
                            + gVideos[i].poster + '"></span>' + '<span class="title">' + gVideos[i].title + '</span>'
                            + '</div></li>';
                }
                gEls.$playlist.empty().html(html);
                NxVPL.updateActiveVideo();

                // Workaround for FF, which needs a minimum of data bound to a drag handler
                // Anyway, we must setup the drag handlers for all items
                setTimeout(function() {

                    var theItem, i, len;
                    len = gVideos.length;
                    for (i = 0; i < len; i++) {
                        theItem = document.getElementById("vpl-video-" + i);

                        theItem.addEventListener('dragstart', function(e) {

                            gDragSrcElt = this;
                            gDragSrcElt.vpl_originaIndex = parseInt(gDragSrcElt.id.replace("vpl-video-", ""));

                            // FireFox _wants_ something in the drag-data.
                            e.dataTransfer.setData('text', this.id);
                        });

                        theItem.addEventListener('dragenter', function(e) {

                            if (gDragSrcElt != this) {
                                this.classList.add("vpl_dragOver");
                            }
                        }, false);

                        theItem.addEventListener('dragover', function(e) {

                            if (e.preventDefault) {
                                e.preventDefault(); // Necessary. Allows us to drop.
                            }

                            return false;

                        }, false);

                        theItem.addEventListener('dragleave', function(e) {
                            this.classList.remove("vpl_dragOver");
                        }, false);

                        theItem.addEventListener('drop',
                                function(e) {

                                    var destIdx, srcIdx, count;
                                    var theIdx, theLen; // do not use i nor len, they are in the outer loop already;

                                    if (e.stopPropagation) {
                                        e.stopPropagation(); // stops the browser from redirecting.
                                    }

                                    if (gDragSrcElt != this) {
                                        srcIdx = gDragSrcElt.vpl_originaIndex;
                                        destIdx = parseInt(this.id.replace("vpl-video-", ""));

                                        gVideosAfterDD = [];
                                        theLen = gVideos.length;
                                        for (theIdx = 0; theIdx < theLen; ++theIdx) {
                                            gVideosAfterDD.push(gVideos[theIdx]);
                                        }
                                        
                                        vplp_utils_move(gVideosAfterDD, srcIdx, destIdx);

                                    }
                                    gDragSrcElt = null;

                                    setTimeout(function() {
                                        
                                        gVideos = [];
                                        theLen = gVideosAfterDD.length;
                                        for (theIdx = 0; theIdx < theLen; ++theIdx) {
                                            gVideos.push(gVideosAfterDD[theIdx]);
                                        }
                                        
                                        NxVPL.initVideos();
                                        NxVPL.createListOfVideos();
                                        NxVPL.bindEvents();
                                        NxVPL.goToFirst();
                                        
                                    }, 200);

                                    return false;

                                }, false);

                        theItem.addEventListener('dragend', function(e) {
                            for (var i = 0, len = gVideos.length; i < len; i++) {
                                theItem = document.getElementById("vpl-video-" + i);
                                theItem.classList.remove("vpl_dragOver");
                            }

                            return false;
                        }, false);
                    }
                }, 500);
            },

            updateActiveVideo : function() {
                var activeIndex = gPlayer.pl.current;

                gEls.$playlist.find('li').removeClass('active');
                gEls.$playlist.find('li[data-videoplaylist="' + activeIndex + '"]').addClass('active');
            },

            bindEvents : function() {

                gEls.$playlist.find('li').on('click', NxVPL.selectVideo);

                gEls.$first.on('click', NxVPL.goToFirst);
                gEls.$prev.on('click', NxVPL.goToPrevious);
                gEls.$next.on('click', NxVPL.goToNext);
                gEls.$last.on('click', NxVPL.goToLast);

                gPlayer.on('next', function(e) {
                    //console.log('Next video');
                    NxVPL.updateActiveVideo();
                });

                gPlayer.on('prev', function(e) {
                    //console.log('Previous video');
                    NxVPL.updateActiveVideo();
                });

                gPlayer.on('lastVideoEnded', function(e) {
                    //console.log('Last video has finished');
                });
            },

            goToFirst : function(e) {
                gPlayer.playList(0);
                NxVPL.updateActiveVideo();
            },

            goToPrevious : function(e) {
                gPlayer["prev"]();
            },

            goToNext : function(e) {
                gPlayer["next"]();

            },

            goToLast : function(e) {
                gPlayer.playList(gVideos.length - 1);
                NxVPL.updateActiveVideo();
            },

            selectVideo : function(e) {
                var clicked = e.target.nodeName === 'LI' ? jQuery(e.target) : jQuery(e.target).closest('li');

                if (!clicked.hasClass('active')) {
                    //console.log('Selecting video');
                    var videoIndex = clicked.data('videoplaylist');
                    gPlayer.playList(videoIndex);
                    NxVPL.updateActiveVideo();
                }
            },

            alignElements : function() {
                // Still this "2 init calls" problem (see header of this script)
                // But here, we'd like to be in the second call...
                // So, let's do the usual workaround, with a timeout :-<
                setTimeout(
                        function() {
                            var playlistObj = jQuery(document.getElementById(PLAYLIST_ID));
                            if (playlistObj.width() > 700) {
                                setTimeout(alignElements, 50);
                            } else {
                                var fancyboxContainerObj, mainDivObj, videoObj, playlistObj, mainWidth, mainHeight, marginH, marginV;

                                fancyboxContainerObj = jQuery("#fancybox-content");
                                mainDivObj = jQuery(document.getElementById(MAIN_DIV_ID));
                                videoObj = jQuery(document.getElementById(VIDEO_ID));
                                playlistObj = jQuery(document.getElementById(PLAYLIST_ID));

                                mainWidth = fancyboxContainerObj.width();
                                mainDivObj.width(videoObj.width() + playlistObj.width() + 4);
                                marginH = (fancyboxContainerObj.width() - mainDivObj.width()) / 2;

                                marginV = ((fancyboxContainerObj.height() - mainDivObj.position().top) - mainDivObj
                                        .height()) / 2;

                                mainDivObj.css({
                                    "margin-top" : marginV,
                                    "margin-left" : marginH
                                });
                            }
                        }, 50);
            }
        };

        NxVPL.init();
        NxVPL.alignElements();
    }
}

function vplp_utils_move(inArray, inFrom, inTo) {
    
    if (inTo === inFrom) {
        return;
    }

    var target = inArray[inFrom];
    var increment = inTo < inFrom ? -1 : 1;

    for (var k = inFrom; k != inTo; k += increment) {
        inArray[k] = inArray[k + increment];
    }
    inArray[inTo] = target;
}
