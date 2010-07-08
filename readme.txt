To Build
---------
You can use ant to build flash.  You should setup properties in username.properties file (where usrename is your login name).

There is a dependency on as3corelib for json parser. (http://code.google.com/p/as3corelib/)

This command will create the swf file (speechapi.swf)
  ant flash 

This command will create the red5asr flashspeak war
  ant war

To configure the server set the properties in /etc/red5-web.properties

To run the servver, place this file in red5 webapps dir and start red5

Js files
--------
Flash is loades with swfobject.  Your will need swfobject.js for that purpose.  Speechapi.js 
contains speechapi javascript code.  speechapi-all.js contains both files concatentated together, 
if you would like a single javascript include.

you can generate the concatenated js file with this command:
  ant concat-js 

The file is placed in teh gen directory.

Samples
-------
Two samples are provided parrot.html and parrot2.html.
Parrot.html creates uses swfobject in the html header and then uses the speechapi setup method.
Parrot2.html uses the setup2 method, which creates the swfobject and also sets up the speechapi in 
a single method.
