package com.metrocave.embedded;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import java.util.List;
import java.util.ArrayList;
import java.io.PipedInputStream;
import java.util.*;
import javax.sound.sampled.AudioFormat;

import java.io.InputStream;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import java.util.Timer;
import javax.sound.sampled.AudioFileFormat.Type;

import java.io.File;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.result.*;

import com.metrocave.FlexComponent;
import com.spokentech.speechdown.client.util.FormatUtils;
import com.spokentech.speechdown.common.SpeechEventListener;
import com.spokentech.speechdown.common.Utterance;
import com.spokentech.speechdown.client.HttpRecognizer;
import com.spokentech.speechdown.client.endpoint.S4EndPointer;
import com.spokentech.speechdown.client.endpoint.ExternalTriggerEndPointer;
import com.spokentech.speechdown.client.endpoint.StreamEndPointingInputStream;
import com.spokentech.speechdown.common.Utterance.OutputFormat;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;



public class SphinxThread extends Thread  implements Runnable, SpeechEventListener{
	public static List<Result> unitTestBuffer = new ArrayList<Result>();
	ConfigurationManager cm=null;
	Recognizer recognizer;
	PipedInputStream pis = null;
	public boolean bRunning = false;
	public StreamDataSource reader=null;
	boolean bProcessing=true;
	boolean bCalculate=false;
	public boolean bStatus=false;
	public void stopSphinx()
	{
		bKillThread=true;
		//cm=null;
		//recognizer.deallocate();
		//recognizer=null;
//		if(_automatic)
//		{
//			recog.cancel(id);			
//		}
//		else
//		{
//			triggerStop();
//		}
//		recog=null;
	}	
	public int recognizerIndex;
	SphinxThread(PipedInputStream is, Talker talker, FlexComponent decodedEvent, String streamName, Recognizer recog, ConfigurationManager c, int recogIndex)
	{
		this.recognizer=recog;
		this.pi=is;
		this.mTalker = talker;
		this.decodedEvent = decodedEvent;
		this.mStreamName = streamName;
		this.cm=c;
		this.recognizerIndex=recogIndex;
		
    	//cm = new ConfigurationManager("/home/eli/workspace32/cmusphinx/trunk/sphinx4/src/apps/edu/cmu/sphinx/demo/confidence/mc.config.xml");
//    	cm = new ConfigurationManager("speechapi.config.xml");
//
//        
//        
//        
//        
//        
//       	String zz = cm.toString();
//		recognizer = (Recognizer) cm.lookup("recognizer");
//		if (recognizer==null)
//		{
//			System.out.println("NULl recogniza");
//		}
//		String tst = recognizer.toString();
//		recognizer.allocate();
//		String test="hh";
//		System.out.println("Decoder loaded@@!!");
	}	
	SphinxThread(PipedInputStream is, Talker talker, FlexComponent decodedEvent, String streamName, HttpRecognizer recog)
	{
		this.recog=recog;
		this.pi=is;
		this.mTalker = talker;
		this.decodedEvent = decodedEvent;
		this.mStreamName = streamName;
	}	
	public void stopIt()
	{
		try
		{
			bProcessing=false;
			bStatus=false;
		}
		catch(Exception e)
		{
		}
	}
    public void setPipe(PipedInputStream apis)
    {
		mTalker.setDecoderReady(true);

    	this.pi=apis;
    }	
	public void RunIt()
	{
		bProcessing=true;
		bCalculate=true;
		bStatus=true;
	}
	boolean pleaseWait = false; // This method is called when the thread runs 
	public void run()
	{
		 try
		 {
				System.out.println("FFFFFFF");
				while (true) { // Do work // Check if should wait 
					//System.out.println("4");
					synchronized (this) 
					{ 
						while (pleaseWait) 
						{ 
							if(bKillThread){
								return;
							}

							System.out.println("5");
							try 
							{ 
								wait(); 
							} 
							catch (Exception e) 
							{ } 
							if(bKillThread){
								return;
							}
							System.out.println("7");
							calculateJSGF();
							System.out.println("6");
							

						} 
					} // Do work 
				} 
		 }
		 catch(Exception e)
		 {
			 e.printStackTrace();
		 }
		
	}
	public String sEnglish;
	public void setEnglish(String word)
	{
		sEnglish=word;
	}
    private JSGFGrammar jsgfGrammarManager;
	
	public boolean calculateJSGF()
	{
        jsgfGrammarManager = (JSGFGrammar) cm.lookup("jsgfGrammar");

        try
        {
        	URL url= new URL("file:///tmp/");
        	jsgfGrammarManager.setBaseURL(url);
            jsgfGrammarManager.loadJSGF(mStreamName);
            jsgfGrammarManager.commitChanges();               	
        }
        catch(Exception e)
        {     	
        	System.out.println(e.getMessage());
        }
        
		if(bRunning)
		{
			try
			{
				reader= (StreamDataSource) cm.lookup("streamDataSource");
				AudioFormat audioFormat = new AudioFormat(16,8000,1,true,false);
				AudioFormat.Encoding targetEncoding = AudioFormat.Encoding.PCM_SIGNED;
				AudioFormat targetFormat = new AudioFormat(8000f, 16, 1, true, false);
				reader.setInputStream(pi, "streamData"); 
	          // while (true) 
	          // {
	               System.out.println("Start speaking. Press Ctrl-C to quit.\n");
	
	               Result result = recognizer.recognize();
	               if(bKillThread)
	               {
	            	   //recognizer.deallocate();
	            	   //recognizer=null;
	               }
                   if (result != null) 
                   {
                	   decodedEvent.passRecogResults(this.mStreamName, result.getBestFinalResultNoFiller());
                       System.out.println(result.getBestFinalResultNoFiller());
                   }	
	               if (result != null) 
	               {
	                   String resultText = result.getBestResultNoFiller();
	                   System.out.println("You said: " + resultText + "\n");
	               } 
	               else 
	               {
	                   System.out.println("I can't hear what you said.\n");
	               }
			 }
			 catch(Error e)
			 {
			 	System.out.println("Cluster="+e);
			 }
		}
		return false;
	}
	

	public boolean calculate()
	{
		if(bRunning)
		{
			try
			{
				reader= (StreamDataSource) cm.lookup("streamDataSource");
				AudioFormat audioFormat = new AudioFormat(16,8000,1,true,false);
				AudioFormat.Encoding targetEncoding = AudioFormat.Encoding.PCM_SIGNED;
				AudioFormat targetFormat = new AudioFormat(8000f, 16, 1, true, false);
				reader.setInputStream(pi, "streamData"); 
	          // while (true) 
	          // {
	               System.out.println("Start speaking. Press Ctrl-C to quit.\n");
	
	               Result result = recognizer.recognize();
                   if (result != null) {
                	   
                	   Lattice lattice = new Lattice(result);
                	   SausageMaker smm = new SausageMaker(lattice);
                       LatticeOptimizer optimizer = new LatticeOptimizer(lattice);
                       optimizer.optimize();
                       lattice.dumpAllPaths();
                       String resultText = result.getBestResultNoFiller();
                       System.out.println("I heard: " + resultText + '\n');
                       Sausage sus =smm.makeSausage();
                	   
                	   System.out.println("RECOZ");
                       ConfidenceScorer cs = (ConfidenceScorer) cm.lookup
                               ("confidenceScorer");
                       ConfidenceResult cr = cs.score(result);
                       int sazz=cr.size();
                       
                       boolean found=false;
                       for (Iterator iter = cr.iterator(); iter.hasNext();) {
                    	   ConfusionSet confused= (ConfusionSet) iter.next();

                    	     Set set = confused.entrySet (  ) ; 
                    	     Iterator iterator = set.iterator (  ) ; 
                    	     while (  iterator.hasNext (  )   )   {  
                    	       Map.Entry entry =  ( Map.Entry ) iterator.next (  ) ; 
                    	       String key=entry.getKey().toString();
                    	       String value=entry.getValue().toString().substring(1, entry.getValue().toString().length()-1);
                    	       if(sEnglish.equals(value))
                    	       {
                    	    	   return true;
                    	       }
                    	       System.out.println ( entry.getKey (  )  + "/" + entry.getValue (  )  ) ; 
                    	      } 
                    	   int wow=33;
                    	}
                       //Sausage ss = (Sausage)cr;
                       SausageMaker sm = (SausageMaker) cm.lookup
                       ("sausageMaker");
                       cr.getConfusionSet(3);
                       Path best = cr.getBestHypothesis();

                       /* confidence of the best path */
                       System.out.println(best.getTranscription());
                       System.out.println
                               ("     (confidence: " +
                                       best.getLogMath().logToLinear
                                               ((float) best.getConfidence())
                                       + ')');
                       System.out.println();
                       WordResult[] words = best.getWords();
                       for (WordResult wr : words) {
                    	   String word = wr.getPronunciation().getWord().getSpelling();
                    	   double fffff=wr.getConfidence();
                           printWordConfidence(wr);
                       }
                       System.out.println();
                   }	
	               if (result != null) 
	               {
	                   String resultText = result.getBestResultNoFiller();
	                   System.out.println("You said: " + resultText + "\n");
	               } 
	               else 
	               {
	                   System.out.println("I can't hear what you said.\n");
	               }
			 }
			 catch(Error e)
			 {
			 	System.out.println("Cluster="+e);
			 }
		}
		return false;
	}
    private static void printInstructions() 
    {
        System.out.println
                ("Sample sentences:\n" +
                    '\n' +
                        "the green one right in the middle\n" +
                        "the purple one on the lower right side\n" +
                        "the closest purple one on the far left side\n" +
                        "the only one left on the left\n" +
                    '\n' +
                        "Refer to the file confidence.test for a complete list.\n");
    }


    private static void printWordConfidence(WordResult wr) 
    {
        String word = wr.getPronunciation().getWord().getSpelling();

        System.out.print(word);

        /* pad spaces between the word and its score */
        int entirePadLength = 10;
        if (word.length() < entirePadLength) 
        {
            for (int i = word.length(); i < entirePadLength; i++) 
            {
                System.out.print(" ");
            }
        }

        System.out.println
                (" (confidence: " +
                        wr.getLogMath().logToLinear((float) wr.getConfidence()) + ')');
    }
	
	public String transcribe(String a)
	{
		//AudioInputStream ais = (AudioInputStream)pi;
		pis = new PipedInputStream();
        //Get the System Classloader
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();

        //Get the URLs
        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

        for(int i=0; i< urls.length; i++)
        {
            System.out.println(urls[i].getFile());
        } 
		// TODO Auto-generated method stub
		URL audioURL = null;
		try 
		{
			//audioURL = new URL("file:///home/eli/nlp/csphinx/10001-90210-01803.wav");
			//audioURL = new URL("file:///home/eli/nlp/ed/cmusphinx/trunk/pocketsphinx/test/data/wsj/n800_440c0207.wav");
			//audioURL = new URL("file:///home/eli/nlp/tmp/voxforge/Main/Tags/AudioSegmentation/AudioBook/test/audio.wav");
			//audioURL = new URL("file:///home/eli/nlp/wav/911_8khz.wav");
			//audioURL = new URL("file:///home/eli/workspace32/red5/red5/webapps/firstapp/streams/hah.wav");
			audioURL = new URL("file:///home/eli/foo1.wav");

							
			//audioURL = new URL("file:///home/eli/nlp/wav/mturk1.wav");
			
			//audioURL = new URL("file:///home/eli/nlp/wav/NC00009D.wav");
			            	//audioURL = new URL("file:///home/eli/test2.wav");
		} 
		catch (MalformedURLException e) 
		{
			// TODO Auto-generated catch block
				e.printStackTrace();

		}
		 
		
		URL configURL=null;
		try 
		{
			//configURL = new URL("file:///home/eli/nlp/cairo/34.config.xml");
			//configURL = new URL("/home/eli/workspace32/red5/red5/webapps/firstapp/WEB-INF/mcmodel.config.xml");
			//configURL = new URL("file:///home/eli/nlp/csphinx/mcmodel.config.xml");
			configURL = new URL("file:///home/eli/nlp/csphinx/mcmodel3.config.xml");

			//configURL = new URL("file:///home/eli/nlp/csphinx/mc_8khz.config.xml");
			//configURL = new URL("file:///home/eli/nlp/csphinx/31.config.xml");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
		System.out.println("555555555555 ");

		//URL url = transcription.class.getClass().getResource("edu.cmu.sphinx.model.acoustic.voxforge.Model");
		// System.out.println(getClass() + " " + url);
        
        try
        {
        	cm = new ConfigurationManager(configURL);

        }
        catch(Error e)
        {
        	
        	System.out.println(e); 
        }
        try
        {

        	String zz = cm.toString();
			
			recognizer = (Recognizer) cm.lookup("recognizer");

			if (recognizer==null)
			{
				System.out.println("NULl recogniza");
			}
			String tst = recognizer.toString();
    /* allocate the resource necessary for the recognizer */

			recognizer.allocate();
			
			//StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");
			//AudioInputStream ais  = AudioSystem.getAudioInputStream(audioFileURL);			

			if(false==false)
				return "false==false";
			String tst2 = recognizer.toString();
    // configure the audio input for the recognizer
		
			AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
			dataSource.setAudioFile(audioURL, null);

// Loop unitl last utterance in the audio file has been decoded, in which case the recognizer will return null.
			Result result;
			String results="";
			while ((result = recognizer.recognize())!= null) 
			{
	 
				String resultText = result.getBestResultNoFiller();
				results += resultText;
				System.out.println(resultText);
				unitTestBuffer.add(result);
			}
        }
        catch(Error e)
        {
        	System.out.println("Cluster="+e);
        }
        return "wtf";
	}  		
	
	public void speechStarted()
	{
		System.out.println("FFFFFFFFFFFFFFFFFFFFF");
	}
	public void speechEnded()
	{
		System.out.println("FFFFFFFFFFFFFFFFFFFFF");
		
	}

	public void noInputTimeout()
	{
		System.out.println("FFFFFFFFFFFFFFFFFFFFF");
		
	}

	/*public void recognitionComplete(Utterance r)
	{
		System.out.println("FFFFFFFFFFFFFFFFFFFFF");
	    System.out.println("grammar result: "+r.getText());	
	    String[] results = r.getText().split(" ");
	    String www=r.getText();
	    String result=r.getText();
	    //create the json object with tags for extracting semantic meaning
	    String tags = null;//toJSONString(r);
	    

	    if(english)
	    {
		    decodedEvent.ResultEvent(this.mStreamName, result, tags,englishWord);
	    }
	    else
	    {
	    	decodedEvent.ResultEvent(this.mStreamName, result, tags);
	    }
	}*/	
	PipedInputStream pi;
	private final Talker mTalker;
	private FlexComponent decodedEvent;
	private String mStreamName;
//	SphinxThread(PipedInputStream is, Talker talker, DecodedEvent decodedEvent, String streamName, HttpRecognizer reco)
//	{
//		this.recog=reco;
//		this.pi=is;
//		this.mTalker = talker;
//		this.decodedEvent = decodedEvent;
//		this.mStreamName = streamName;
//		//setUp();
//	}

	Timer timer;
	public void wtf()
	{
		int test =0;
		try
		{
		}
		catch(Exception e)
		{
		}
	}
	  

	private static String service = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";  
	//private static String service = "http://localhost:8080/speechcloud/SpeechUploadServlet";   
	//private static String service = "http://ec2-75-101-188-39.compute-1.amazonaws.com/speechcloud/SpeechUploadServlet";    
	    
	private static AudioFormat desiredFormat;
	private static int sampleRate = 8000;
	private static boolean signed = true;
	private static boolean bigEndian = true;
	private static int channels = 1;
	private static int sampleSizeInBits = 16;
	
	private static int audioBufferSize = 160000;
	private static int msecPerRead = 10;
	private static int frameSizeInBytes;
	
	//private String grammar = "file:///usr/share/tomcat5.5/example.gram"; 
	private String grammar ="file:///home/eli/workspace32/cmusphinx/trunk/sphinx4/src/apps/edu/cmu/sphinx/demo/hellodigits/digits.gram";
			
	URL grammarUrl = null;
//	HttpRecognizer recog;
	
	File soundFile1 = new File("file:///usr/share/tomcat5.5/prompts/lookupsports.wav3");	 	
	File soundFile2 = new File("file:///usr/share/tomcat5.5/prompts/get_me_a_stock_quote.wav");	 	
	File soundFile3 = new File("file:///usr/share/tomcat5.5/prompts/i_would_like_sports_news.wav");	 	
	
	String wav = "audio/x-wav";
	String s4feature = "audio/x-s4feature";
	String s4audio = "audio/x-s4audio";
	
	String audioConfigFile="/home/eli/bin/apache-tomcat-6.0.20/webapps/speechcloud/WEB-INF/sphinxfrontendonly-audio.xml";
	String featureConfigFile="file:///usr/share/tomcat5.5/sphinxfrontendonly-feature.xml";	
	boolean bKillThread=false;

	public void triggerStop()
	{
		if(etep!=null)
			etep.triggerEnd();
//		recog=null;
	}
	String id;
	protected void setUp() 
	{
//		XMLDecoder decoder = null;
//		try 
//		{
//	        decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream("metrospeech.xml")));
//	        Bean o = (Bean)decoder.readObject();
//	        decoder.close();
//	        service=o.getDecoderServer();
//	    } catch (Exception e) {
//	    }
//	    recog = new HttpRecognizer();
//		recog.setService(service);
	}
	private void CheckReady()
	{
	}
     
	private static AudioFormat getAudioFormat()
	{
		float sampleRate = 8000.0F;
		//8000,11025,16000,22050,44100
		int sampleSizeInBits = 16;
		//8,16
		int channels = 1;
		//1,2
		boolean signed = true;
		//true,false
		boolean bigEndian = false;
		//true,false
		return new AudioFormat(sampleRate,
		                       sampleSizeInBits,
		                       channels,
		                       signed,
		                       bigEndian);
	}

	public String speak(String text, String gender) 
	{
		return "";
//		String downloadService = null;    
//	    String streamsFolder = null;
//		
//		XMLDecoder decoder = null;
//		try 
//		{
//	        decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream("metrospeech.xml")));
//	        Bean o = (Bean)decoder.readObject();
//	        decoder.close();
//	        downloadService=o.getSpeechServer();
//	        streamsFolder=o.getStreamFolder();
//	    } catch (Exception e) {
//	    }
//		//String downloadService = "http://ec2-174-129-20-250.compute-1.amazonaws.com/speechcloud/SpeechDownloadServlet";    
//	
//	  	HttpSynthesizer synth;
//		synth = new HttpSynthesizer();
//		synth.setService(downloadService);	 
//
//		AudioFormat format2;
//		
//		
//		
//        format2 = new AudioFormat(
//                Encodings.getEncoding("MPEG1L3"),
//                44100,
//
//                16,
//                1,
//                2,
//                11000,
//                true);
//        
//        AudioFormat format;
//
//	    format = new AudioFormat ( 44100, 32, channels, signed, bigEndian);	    	 
//	 
//		String voice;
//		String outFileName;
//		InputStream stream;
//		
//		 
//		System.out.println("Starting Test ...");
//		
//		if(gender.toUpperCase().compareTo("MALE")==0)
//		{
//	    	voice = "jmk-arctic";
//		}
//		else
//		{
//	    	voice = "jmk-arctic";
//			
//		}
//	  	wav = "audio/mpeg";
//		text = text;
//	
//		stream = synth.synthesize(text, format2, wav, voice);
//		
//	    String rand = Math.random() + ".mp3";
//	    outFileName = streamsFolder + "/" + rand;
//	    if (stream != null) {
//	    	writeStreamToFile(stream,outFileName);
//	    }	  
//	    return rand;
	}
	
	public void writeStreamToFile(InputStream inStream, String fileName) 
	{
		try 
		{

			File f = new File(fileName);
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));
	
			BufferedInputStream in = new BufferedInputStream(inStream);
	
			byte[] buffer = new byte[256]; 
			while (true) { 
				int bytesRead = in.read(buffer);
				//_logger.trace("Read "+ bytesRead + "bytes.");
				if (bytesRead == -1) break; 
				out.write(buffer, 0, bytesRead); 
			} 
			in.close(); 
			out.close(); 
		} 
		catch (Exception e) { 
			e.printStackTrace();
		} 
	}
	


    private boolean _automatic=false;
    public void setAutomatic(boolean automatic)
    {
    	_automatic=automatic;
    }
    public void setGrammar(String name)
    {
    	String filename = "file://" + name;
    	try {
    		grammarUrl = new URL(filename);
		} catch (MalformedURLException e) {  
	         e.printStackTrace();  
		}		    	
    }
    S4EndPointer s4ep;
    ExternalTriggerEndPointer etep;
	private StreamEndPointingInputStream epStream = null;
	HttpRecognizer recog=null;
	private String userId = null;
	private String devId = null;
	private String key = null;
	public void Recognizer()
	{

		boolean lmflg = false;
		Type type = null;
		long timeout = 10000;

		if(_automatic)
		{
			System.out.println("Starting S4 EP Stream Test ...");
			if(s4ep==null)
			{
				s4ep = new S4EndPointer() ;
			}
			if(epStream==null)
			{
				epStream = new StreamEndPointingInputStream(s4ep);
			}
			epStream.setMimeType(s4audio);
			try
			{
				pi.reset();
			}
			catch (IOException e){}
			AudioFormat format = getAudioFormat();
			com.spokentech.speechdown.common.AFormat f = FormatUtils.covertToNeutral(format);
			//com.spokentech.speechdown.common.AFormat f = FormatUtils.covertToNeutral(format);
			epStream.setupStream(pi, f);
			mTalker.setDecoderReady(true);

		    try 
		    {
		    	id=recog.recognizeAsynch(devId, key, userId ,grammarUrl, epStream, false, true,OutputFormat.json, timeout, this);
		    }
		    catch (InstantiationException e) 
		    {
		        e.printStackTrace();
		    }
		    catch (IOException e) 
		    {
		        e.printStackTrace();
		    }
		    catch(Exception e)
		    {
		    	e.printStackTrace();
		    }
		    catch(ThreadDeath e)
		    {
		    	e.printStackTrace();
		    	return;
		    }		
		}
		else
		{
			System.out.println("Starting S4 EP Stream Test ...");
			AudioInputStream	audioInputStream = null;
			if(etep==null)
			{
				etep = new ExternalTriggerEndPointer();
			}
			else
			{
				//etep.triggerEnd();
			}
			if(epStream==null)
			{
				epStream = new StreamEndPointingInputStream(etep);
			}
			else
			{
				if(epStream.inUse())
				{
					triggerStop();
					while(epStream.inUse())
					{
						triggerStop();
						boolean hah=false;
					}
					boolean ress = epStream.inUse();
					boolean hah = false;
					//System.out.println("InUse");
				}

			}
		    epStream.setMimeType(wav);
			try
			{
				pi.reset();
			}
			catch (IOException e){}
			AudioFormat format = getAudioFormat();
			//com.spokentech.speechdown.common.AFormat f = FormatUtils.covertToNeutral(format);
			com.spokentech.speechdown.common.AFormat f = FormatUtils.covertToNeutral(format);	

			//AFormat af = new AFormat("PCM",8000,16,1,false,true,8,2,2);
			epStream.setupStream(pi, f);
			mTalker.setDecoderReady(true);

			
		    try 
		    {
		    	
//		        lmflg = true;
//	            boolean doEndpointing = true;
//	            boolean batchMode = true;
//	            long start = System.nanoTime();
//	            RecognitionResult r = recog.recognize(fname, grammarUrl, lmflg, doEndpointing,batchMode);
//	            long stop = System.nanoTime();
//	            long wall = (stop - start)/1000000;
//	            System.out.println("FILE TEST: Batch mode, Server Endpointing, LM result: "+r.getText() + " took "+wall+ " ms");
//	            if (r.isCflag())
//	                System.out.println("confidence is "+r.getConfidence());		
		    	
		    	//id=recog.recognizeAsynch(grammarUrl, epStream, true, true, timeout, this);
		    	
		    	id=recog.recognizeAsynch(devId, key, userId,grammarUrl, epStream, false, true, OutputFormat.json, timeout, this);
		    }
		    catch (InstantiationException e) 
		    {
		        e.printStackTrace();
		    }
		    catch (IOException e) 
		    {
		        e.printStackTrace();
		    }
		    catch(Exception e)
		    {
		    	e.printStackTrace();
		    }
		    catch(ThreadDeath e)
		    {
		    	e.printStackTrace();
		    	return;
		    }	
		    etep.triggerStart();
//		    while ( ep.triggerStart() <0)
//		    {
//		    	System.out.println("not setup yet");
//		    }
		}
		//ep.triggerStart();
//		    System.out.println("grammar result: "+r.getText());	
//		    String[] results = r.getText().split(" ");
//		    String www=r.getText();
//		    String result=r.getText();
	//
//	            
//		    //create the json object with tags for extracting semantic meaning
//		    String tags = toJSONString(r);
//		    decodedEvent.ResultEvent(this.mStreamName, result, tags);
//		    mTalker.NotReady();
//		    mTalker.JustWait();
//		    Recognizer();
			

	}

	public void StartDecodingProcess()
	{
	    mTalker.setDecoderReady(false);
	    mTalker.JustWait();
	    Recognizer();
	}


    
    public boolean english=false;
    public String englishWord=null;
    public void SetWord(String word)
    {
    	english=true;
    	englishWord=word;
    }
	@Override
    public void recognitionComplete(Utterance arg0) {
	    // TODO Auto-generated method stub
	    
    }
	

}
class MyThread extends Thread { 
	boolean pleaseWait = false; // This method is called when the thread runs 
	public void run() { 
		System.out.println("Starting thread");
		while (true) { // Do work // Check if should wait 
			//System.out.println("4");
			synchronized (this) { 
				while (pleaseWait) { 
					System.out.println("5");
					try { 
						wait(); 
					} 
					catch (Exception e) 
					{ } 
					System.out.println("6");

				} 
			} // Do work 
		} 
	} 
} 

