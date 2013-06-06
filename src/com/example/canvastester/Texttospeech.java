package com.example.canvastester;

import java.util.Locale;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.content.Context;

public class Texttospeech extends Activity implements TextToSpeech.OnInitListener{

	TextToSpeech tts;
	boolean tts_ready = false;
	
	public Texttospeech (Context context){
		tts= new TextToSpeech(context,this);
	}
	
	public void sayStuff(String stuff){
		if (tts_ready){		
			
			
					tts.speak(stuff, TextToSpeech.QUEUE_FLUSH, null);
			
		} else {
			Log.e("TTS", "tryed to say somthing and tts wasnt ready");
		}
		
	}
	
	public void onInit(int status) {
		// TODO Auto-generated method stub
		
		
		if (status == TextToSpeech.SUCCESS) {
			tts_ready = true;
			int result = tts.setLanguage(Locale.US);

			tts.setPitch((float) 1); // set pitch level

			 tts.setSpeechRate((float) 1.25); // set speech speed rate			
			 if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
					Log.e("TTS", "Language is not supported");
				}	
		}

		 else {
			tts_ready = false;
			Log.e("TTS", "Initilization Failed");
		}

	}
}
