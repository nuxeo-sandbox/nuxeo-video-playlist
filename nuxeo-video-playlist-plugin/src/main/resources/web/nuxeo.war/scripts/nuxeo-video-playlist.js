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
 * dependencies (videojs and videojs-playlist). Well, we know that initializing 
 * 
 * This is why, until we understand everything ;->, we are usings tests and flags
 */
var gVideos = [], gPlayer, gEls = {}, gVideoId;

if (typeof NcVPL_INIT_COUNTER === "undefined" || NcVPL_INIT_COUNTER === null) {
    NcVPL_INIT_COUNTER = 0;
}

function NuxeoVideoPlaylist_Init(inVideoId, inPlaylistJson) {

    gVideoId = inVideoId;
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

        console.log("INIT NxVPL");
        NxVPL = {

            init : function() {
                gPlayer = vjs(document.getElementById(gVideoId));

                NxVPL.cacheElements();
                NxVPL.initVideos();
                NxVPL.createListOfVideos();
                NxVPL.bindEvents();

            },

            cacheElements : function() {
                gEls = {};
                gEls.$playlist = jQuery('div.playlist > ul');
                gEls.$prev = jQuery('#nxpvl_prev');
                gEls.$next = jQuery('#nxpvl_next');
            },

            initVideos : function() {
                gPlayer.playList(gVideos);
            },

            createListOfVideos : function() {
                var html = '';
                for (var i = 0, len = gVideos.length; i < len; i++) {
                    html += '<li data-videoplaylist="' + i + '">' + '<span class="number">' + (i + 1) + '</span>'
                            + '<span class="poster"><img src="' + gVideos[i].poster + '"></span>'
                            + '<span class="title">' + gVideos[i].title + '</span>' + '</li>';
                }
                gEls.$playlist.empty().html(html);
                NxVPL.updateActiveVideo();
            },

            updateActiveVideo : function() {
                var activeIndex = gPlayer.pl.current;

                gEls.$playlist.find('li').removeClass('active');
                gEls.$playlist.find('li[data-videoplaylist="' + activeIndex + '"]').addClass('active');
            },

            bindEvents : function() {

                gEls.$playlist.find('li').on('click', NxVPL.selectVideo);

                gEls.$prev.on('click', NxVPL.goToPrevious);
                gEls.$next.on('click', NxVPL.goToNext);

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

            goToPrevious : function(e) {
                gPlayer["prev"]();
            },

            goToNext : function(e) {
                gPlayer["next"]();

            },

            selectVideo : function(e) {
                var clicked = e.target.nodeName === 'LI' ? jQuery(e.target) : jQuery(e.target).closest('li');

                if (!clicked.hasClass('active')) {
                    //console.log('Selecting video');
                    var videoIndex = clicked.data('videoplaylist');
                    gPlayer.playList(videoIndex);
                    NxVPL.updateActiveVideo();
                }
            }
        };

        NxVPL.init();
    }
}

