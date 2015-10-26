# nuxeo-video-playlist

nuxeo-video-playlist displays a UI where a list of videos can be played automatically one after the other.

Once installed, it displays a toolbarbutton which, once clicked by the user, opens a fancybox dialog where the videos are played.

The toolbar button is displayed only when the plug-in detects that at least one Document with the "Video" facet _and_ a non empty file:content binary can be used. To do so, it checks if a video can be found, in this exact order:

1. Does the current document has the "palette" facet?
  * This is provided by the `nuxeo-palette` plug-in, which lets the user to order documents inside a `Folderish` container.
  * So, if current document has the "paette" facet _and_ has at least one valid video in the ordered list, then it uses this ordered list
2. If current document does not have the "palette" facet (or has it but the list of ordered documents does not contain a video), then nuxeo-video-playlist checks if current document is a `Collection`.
  * If yes, then the plug-in check if there there are video documents in the collection
  * If it finds at least one, it displays the Video Playlist
3. Last, if none of the above were succesful in getting at least one video, the plug-in checks if the current document has the `Folderish` facet
  * If yes, then it checks if the children (first level in the container) cntain at leats one video
  * If it finds more than one, the list is ordered alphabetically based on `dc:title`)
  
  TEST:
  
![Custom-Thumbnails](https://intranet.nuxeo.com/nuxeo/site/easyshare/9d2c9e28-e097-4a19-8962-de3e597c7b31/a0f4f0db-aa55-42ea-8420-d4ed3811a5ed/VIDEO-PLAYLIST-2.jpg)


## Third-Party Tools



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
