# nuxeo-video-playlist

nuxeo-video-playlist displays a UI where a list of videos can be played automatically one after the other.

Once installed, it displays a toolbarbutton (when relevant, see below)...

!["The Playlist"](https://raw.github.com/nuxeo-sandbox/nuxeo-video-playlist/screenshots-for-readme/toolbar-button.png)

...which, once clicked by the user, opens a fancybox dialog where the videos are played:


!["The Playlist"](https://raw.github.com/nuxeo-sandbox/nuxeo-video-playlist/screenshots-for-readme/playlist-in-fancybox.jpg)


The toolbar button is displayed _only_ when the plug-in detects that at least one Document with the "Video" facet _and_ a non empty file:content binary can be used. To do so, it checks if a video can be found, in this exact order:

1. Does the current document has the "palette" facet?
  * This facet is provided by the `nuxeo-palette` plug-in, which lets the user to order Documents inside a `Folderish` container (not only videos, any kind of Document).
  * So, if the current Document has the "palette" facet _and_ has at least one valid video in the ordered list, the plug-in uses this ordered list.
2. If current document does not have the "palette" facet (or has it but the list of ordered documents does not contain a video), then nuxeo-video-playlist checks if current Document is a `Collection`.
  * If yes, the plug-in checks if there there are videos in the collection
  * If it finds at least one, it displays the Video Playlist
3. Last, if after all the previous attempts to get at least one video we still have nothing, the plug-in checks if the current document has the `Folderish` facet:
  * If yes, then it checks if there is at least one video in the children (first level in the container)
  * If it finds more than one, the list is ordered alphabetically (based on `dc:title`)
  

## Build
Assuming `maven` is installed on you computer, you can just download these sources, then `mvn clean install`. The Marketplace Package will then be ready to install:

```
# Clone this repo
cd /path/to/to/a/folder
git clone https://github.com/nuxeo-sandbox/nuxeo-video-playlist.git
# Build with maven
cd nuxeo-nuxeo-video-playlist
mvn clean install
# The MP is in nuxeo-nuxeo-video-playlist/nuxeo-video-playlist-mp/target
```


## Third-Party Tools

This plug-in uses [`videojs-playlists`](https://github.com/jgallen23/videojs-playLists), a plug-in for [`Video.js`](http://videojs.com) written by Antonio Laguna (videojs-playlists is released under the MIT license)


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
