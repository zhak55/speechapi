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

public class SpeechSessionManager {
	
	//SpeechSessionManager is a singleton
	private static SpeechSessionManager instance = null;

	private static Map<String, SpeechSession> sessions; 
	

	   private SpeechSessionManager() {
		   sessions = new HashMap<String, SpeechSession>();
	   }
	   public static SpeechSessionManager getInstance() {
	      if(instance == null) {
	         instance = new SpeechSessionManager();
	      }
	      return instance;
	   }

	   
	   public void newSession(SpeechSession session) {
		   sessions.put(session.getId(), session);
	   }
	   
	   public SpeechSession getSession(String id) {
		   return sessions.get(id);
	   }
	   
	   public SpeechSession removeSession(String id) {
		   return sessions.remove(id);
	   }
	
	   public SpeechSession removeSession(SpeechSession session) {
		   return sessions.remove(session);
	   }
	   
}
