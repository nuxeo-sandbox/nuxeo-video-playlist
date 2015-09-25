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
 */
var gVideos = [],
    gPlayer,
    gEls = {};

NxVPL = {

  init: function(videoId, playlistJson) {
    // The code is called twice: When the fancybox is initialized but not
    // yet displayed, and when it is displayed. We want to act only when everything is loaded
    var fancybox = jQuery("#fancybox-content");
    if(!fancybox || !fancybox.is(":visible")) {
      return
    }
    debugger;

    gPlayer = vjs(document.getElementById(videoId));
    gVideos = playlistJson;

    NxVPL.cacheElements();
    NxVPL.TEST_SETUP_LIST();
    NxVPL.initVideos();
    NxVPL.createListOfVideos();
    NxVPL.bindEvents();

  },

  TEST_SETUP_LIST : function() {
      /*
    gVideos = [
    {
      src : [
        'http://localhost:8080/nuxeo/nxbigfile/default/ff84837e-8bae-484e-b4dd-1db7b7e80fea/vid:transcodedVideos/0/content/Sunset Video.mp4',
        'http://localhost:8080/nuxeo/nxbigfile/default/ff84837e-8bae-484e-b4dd-1db7b7e80fea/vid:transcodedVideos/1/content/Sunset Video.webm'
      ],
      poster : 'http://localhost:8080/nuxeo/nxpicsfile/default/ff84837e-8bae-484e-b4dd-1db7b7e80fea/StaticPlayerView:content/1443132774000',
      title : 'Sunset Video'
    },
    {
      src : [
        'http://localhost:8080/nuxeo/nxbigfile/default/6730d033-c0ee-49bf-a53c-bd8681a66ae7/vid:transcodedVideos/0/content/Sabre.webm'
      ],
      poster : 'http://localhost:8080/nuxeo/nxpicsfile/default/6730d033-c0ee-49bf-a53c-bd8681a66ae7/StaticPlayerView:content/1443035373000',
      title : 'Sabre'
    },
    {
      src : [
        'http://localhost:8080/nuxeo/nxbigfile/default/21e24da6-e26a-4d08-934c-d89e27997736/vid:transcodedVideos/0/content/Toy_Story_2_trailer.mp4',
        'http://localhost:8080/nuxeo/nxbigfile/default/21e24da6-e26a-4d08-934c-d89e27997736/vid:transcodedVideos/1/content/Toy_Story_2_trailer.webm'
      ],
      poster : 'http://localhost:8080/nuxeo/nxpicsfile/default/21e24da6-e26a-4d08-934c-d89e27997736/StaticPlayerView:content/1443132752000',
      title : 'Toy_Story_2_trailer'
    }
    ];
    */
  },


  cacheElements : function() {
      gEls.$playlist = jQuery('div.playlist > ul');
      gEls.$next = jQuery('#next');
      gEls.$prev = jQuery('#prev');
      //gEls.log = $('div.panels > pre');
  },

  initVideos : function() {
    gPlayer.playList(gVideos);
  },


  createListOfVideos : function() {
    var html = '';
      for (var i = 0, len = gVideos.length; i < len; i++){
        html += '<li data-videoplaylist="'+ i +'">'+
                  '<span class="number">' + (i + 1) + '</span>'+
                  '<span class="poster"><img src="'+ gVideos[i].poster +'"></span>' +
                  '<span class="title">'+ gVideos[i].title +'</span>' +
                '</li>';
      }
      gEls.$playlist.empty().html(html);
      NxVPL.updateActiveVideo();
  },


    updateActiveVideo : function(){
      var activeIndex = gPlayer.pl.current;

      gEls.$playlist.find('li').removeClass('active');
      gEls.$playlist.find('li[data-videoplaylist="' + activeIndex +'"]').addClass('active');
    },
    

    bindEvents : function(){
      var self = this;
      gEls.$playlist.find('li').on('click', $.proxy(this.selectVideo,this));
      gEls.$next.on('click', $.proxy(this.nextOrPrev,this));
      gEls.$prev.on('click', $.proxy(this.nextOrPrev,this));
      gPlayer.on('next', function(e){
        console.log('Next video');
        self.updateActiveVideo.apply(self);
      });
      gPlayer.on('prev', function(e){
        console.log('Previous video');
        self.updateActiveVideo.apply(self);
      });
      gPlayer.on('lastVideoEnded', function(e){
        console.log('Last video has finished');
      });
    },


    nextOrPrev : function(e){
      var clicked = $(e.target);
      gPlayer[clicked.attr('id')]();
    },


    selectVideo : function(e){
      var clicked = e.target.nodeName === 'LI' ? $(e.target) : $(e.target).closest('li');

      if (!clicked.hasClass('active')){
        console.log('Selecting video');
        var videoIndex = clicked.data('videoplaylist');
        gPlayer.playList(videoIndex);
        NxVPL.updateActiveVideo();
      }
    }

}


