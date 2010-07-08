/*
 * Copyright (C) 2010 speechapi.com
 * 
 *   This file is part of speechapi flashspeak
 * 
 *   Speechapi flashspeak is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Speechapi flashspeak is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Speechapi flashspeak.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package org.speechapi;

import java.util.HashMap;
import java.util.Map;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.service.IServiceCapableConnection;
import org.slf4j.Logger;


//TODO: Implement (3) interfaces here (recognitionEvent, synthEvent, vxmlEvent)
public class FlexInvoker {
	final private Logger log = Red5LoggerFactory.getLogger(this.getClass());
	
	
	//TODO: find a place for this connection map (in the session?).  
    final private Map<String, IConnection> mConnections = new HashMap<String, IConnection>();
	//SpeechSessionManager is a singleton
	private static FlexInvoker instance = null;


	   private FlexInvoker() {
	   }
	   public static FlexInvoker getInstance() {
	      if(instance == null) {
	         instance = new FlexInvoker();
	      }
	      return instance;
	   }

	   
		
		public void speakResults(SpeechSession session, String filelocation)
		{
			log.info("Speaking results "+session.getId()+" "+filelocation);
				IServiceCapableConnection sc = (IServiceCapableConnection)mConnections.get(session.getId());
				if (sc != null ) 
				{ 			
					sc.invoke("speakResult", new Object[]{filelocation});
				} 
		}
		
		public void stopSpeaking(SpeechSession session)
		{
			log.debug("Stop speaking results ");
				IServiceCapableConnection sc = (IServiceCapableConnection)mConnections.get(session.getId());
				if (sc != null ) 
				{ 			
					sc.invoke("speakResult", new Object[]{});
				} 
		}
		
		public void PassResults(SpeechSession session, String result, String tags)
		{
			log.debug("Passing results "+session.getId()+" "+result+ " "+ tags);
				IServiceCapableConnection sc = (IServiceCapableConnection)mConnections.get(session.getId());
				if (sc != null ) 
				{ 			
			        //IServiceCapableConnection sc = (IServiceCapableConnection) conn2; 
			        sc.invoke("passResults", new Object[]{result, tags}); 
				} 
		}
		public void PassResults(SpeechSession session, String result, String tags, String english)
		{
			log.debug("Passing results2 "+session.getId()+" "+result+ " "+ tags);
			if (result.equalsIgnoreCase(english))
			{
				result="true";
			}
			else
			{
				result="false";
			}

			IServiceCapableConnection sc = (IServiceCapableConnection)mConnections.get(session.getId());
			if (sc != null ) 
			{ 			
		        //IServiceCapableConnection sc = (IServiceCapableConnection) conn2; 
		        sc.invoke("passResults", new Object[]{result, tags}); 
			} 
		}
	   
}
