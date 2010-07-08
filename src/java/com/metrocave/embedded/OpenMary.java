package com.metrocave.embedded;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import de.dfki.lt.mary.client.MaryClient;

//import de.dfki.lt.mary.Mary;
//import de.dfki.lt.mary.MaryDataType;
//import de.dfki.lt.mary.MaryProperties;
//import de.dfki.lt.mary.Request;
//import de.dfki.lt.mary.modules.synthesis.Voice;
//import de.dfki.lt.mary.util.MaryAudioUtils;
import javax.sound.sampled.UnsupportedAudioFileException;

public class OpenMary  {
	public OpenMary() {
		
	}
 	
//	private static AudioFormat getAudioFormat()
//	{
//		float sampleRate = 16000.0F;
//		//8000,11025,16000,22050,44100
//		int sampleSizeInBits = 16;
//		//8,16
//		int channels = 1;
//		//1,2
//		boolean signed = true;
//		//true,false
//		boolean bigEndian = false;
//		//true,false
//		return new AudioFormat(sampleRate,
//		                       sampleSizeInBits,
//		                       channels,
//		                       signed,
//		                       bigEndian);
//	}
//	private File promptDir;
//	Mary mary;
//	String prefix;
//	String maryDir;
//	private boolean recordingEnabled;
//
//    private  void addJarsToClasspath() throws Exception {
//    	//File jarDir = new File(MaryProperties.maryBase()+"\\java");
//    	System.out.println("mary base:"+maryDir+"     "+maryDir+"/java");
//    	File jarDir = new File(maryDir+"/java");
//    	File[] jarFiles = jarDir.listFiles(new FilenameFilter() {
//    		public boolean accept(File dir, String name) {
//    			return name.endsWith(".jar");
//    		}
//    	});
//    	System.out.println("jarfiles: "+jarFiles);
//    	System.out.println("# jarfiles: "+jarFiles.length);
//    	assert jarFiles != null;
//    	URLClassLoader sysloader = (URLClassLoader)ClassLoader.getSystemClassLoader();
//    	Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
//    	method.setAccessible(true);
//
//    	for (int i=0; i<jarFiles.length; i++) {
//    		URL jarURL = new URL("file:"+jarFiles[i].getPath());
//    		method.invoke(sysloader, new Object[] {jarURL});
//    	}
//    }
//    public void setMaryDir(String maryDir) {
//    	this.maryDir = maryDir;
//    	System.setProperty("mary.base", maryDir);
//    }
//    public void setPromptDir(File promptDir) {
//    	this.promptDir = promptDir;
//    }
//    public void setPrefix(String prefix) {
//    	this.prefix = prefix;
//    }
//	public void server()  throws UnsupportedAudioFileException
//	{
//		
//		try {
//			setMaryDir("/home/eli/MARY");
//			
//			addJarsToClasspath();
//	        MaryProperties.readProperties();
//			//mary = new Mary();
//	        Mary.startup();
//        } catch (Exception e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        } 		
//
//        String text="hello what time is it";
//        AudioFormat format = getAudioFormat();
//
//		//String voiceName="jmk-arctic";
//		String voiceName="male";
//		
//		
//        String  inputTypeName = "TEXT_EN";
//        String  outputTypeName = "AUDIO";
//
//        long  start = System.nanoTime();
//
//        MaryDataType inputType = MaryDataType.get(inputTypeName);
//        MaryDataType outputType = MaryDataType.get(outputTypeName);
//        
//        Voice voice = null;
//
//        if (voiceName != null)
//            voice = Voice.getVoice(voiceName);
//        else if (inputType.getLocale() != null)
//            voice = Voice.getDefaultVoice(inputType.getLocale());
//        
//        String audioTypeName = "WAVE";
//    	   //audioTypeName = "MP3";
//
//
//        AudioFileFormat audioFileFormat = null;
//
//        if (outputType.equals(MaryDataType.get("AUDIO"))) {
//
//            AudioFileFormat.Type audioType = MaryAudioUtils.getAudioFileFormatType(audioTypeName);
//            
//            //AudioFormat audioFormat = null;
//            if (audioType.toString().equals("MP3")) {
//                if (!MaryAudioUtils.canCreateMP3())
//                    throw new UnsupportedAudioFileException("Conversion to MP3 not supported.");
//                //audioFormat = MaryAudioUtils.getMP3AudioFormat();
//            //} else {
//                //Voice ref = (voice != null) ? voice : Voice.getDefaultVoice(Locale.ENGLISH);
//                //audioFormat = ref.dbAudioFormat();
//            }
//            audioFileFormat = new AudioFileFormat(audioType, format, AudioSystem.NOT_SPECIFIED);
//        }
//        Request request = new Request(inputType, outputType, voice, "", "", 1, audioFileFormat);
//boolean recordingEnabled=true;
//FileOutputStream out =null;
//try
//{
//	out= new FileOutputStream("asdfasdfasdf");;
//}
//catch(Exception e){}
//
//        try {
//        	ByteArrayInputStream bs = new ByteArrayInputStream(text.getBytes());
//	        request.readInputData( new InputStreamReader(bs, "UTF-8"));
//	        request.process();
//	        request.writeOutputData(out);
//
//System.out.println("YAAAAAA");
//
////			if (recordingEnabled) {
////		        float sr = request.getAudioFileFormat().getFormat().getSampleRate();
////		        //TODO: streamlen is always coming out to be zero.
////				float flen = request.getAudioFileFormat().getByteLength();
////				//float flen = request.getAudio().getFrameLen();
////				float sampleSize = request.getAudioFileFormat().getFormat().getSampleSizeInBits()/8;
////				int streamLen = (int) (flen/(sr * sampleSize));
////				
////				long stop = System.nanoTime();
////				long wall = (stop - start)/1000000;
////				double ratio = (double)wall/(double)streamLen;
////				
////			    hr.getSynth().setStreamLen(streamLen);
////			    hr.getSynth().setWallTime(wall);
////				ServiceLogger.logHttpRequest(hr);
////
////			}
////			
//			
//	        ///ByteArrayOutputStream baos = new ByteArrayOutputStream();
//	        // The byte array constitutes a full wave file, including the headers.
//	        // And now, play the audio data:
//	        ///request.writeOutputData(baos);
//
//	        ///AudioInputStream ais = AudioSystem.getAudioInputStream(
//	        ///    new ByteArrayInputStream(baos.toByteArray()));
//	        ///writeStreamToFile(ais,"c:/tmp/sal.mp3"); 
//	        ///AudioFormat x = request.getAudioFileFormat().getFormat();
//
//	        ///_logger.info("****: "+x);
//	        
//        } catch (FileNotFoundException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        } catch (Exception e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        }
//        
//		
//	}
//	
//	

	
	public String say(String text, String gender) throws IOException, UnknownHostException,
	        UnsupportedAudioFileException, InterruptedException {
		String serverHost = System.getProperty("server.host", "localhost");
		int serverPort = Integer.getInteger("server.port", 59125).intValue();
		System.out.println("Preparing to say" + text);
		MaryClient mary = new MaryClient(serverHost, serverPort);
		System.out.println("11111111111111");
		// String text = "Good afternoon. My name is Elias. How may I assist you today";
		String inputType = "TEXT_EN";
		String outputType = "AUDIO";
		String audioType = "MP3";
		// String defaultVoiceName = "male";
		// text="Hello eli how are you today Hello eli how are you today Hello eli how are you today";
		System.out.println("22222222");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.out.println("333333");
		System.out.println("text=" + text);
		System.out.println("inputType=" + inputType);
		System.out.println("outputType=" + outputType);
		System.out.println("audioType=" + audioType);
		System.out.println("gender=" + gender);
		if (mary == null) {
			System.out.println("MARY=NULL");
		}
		mary.process(text, inputType, outputType, audioType, gender, baos);
		String rand = Math.random() + ".mp3";
		String wavName = "/home/eli/bin/red5/dist/webapps/firstapp/streams/" + rand;
		System.out.println("444444");
		System.out.println("Saved audio file to + " + wavName);
		FileOutputStream outwav = new FileOutputStream(wavName);
		System.out.println("5555555");
		baos.writeTo(outwav);
		System.out.println("66666666666");
		return rand;
	}
	
}
