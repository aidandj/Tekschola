package com.example.canvastester;

import com.darvds.ribbonmenu.RibbonMenuCallback;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.GestureDetectorCompat;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

enum ScreenItem { NONE, CH1, CH2, TRIGGER, MATH1 }

public class DrawingPanel extends SurfaceView implements
SurfaceHolder.Callback, GestureDetector.OnGestureListener,
GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener {	

	
	private final int LEFT_ANIM = 1;
	private final int RIGHT_ANIM = 2;
	private final int CHANNEL_1 = 1;
	private final int CHANNEL_2 = 2;	
	
	
	private Texttospeech tts;
	
	private static final String DEBUG_TAG = "Gestures";
	private GestureDetectorCompat mDetector;
	private ScaleGestureDetector mScaleDetector;

//	private String serverIpAddress = "10.0.1.9";
	private String serverIpAddress = "192.168.0.2";
	Thread cThread = null;


	private DrawingThread _thread;
	private ClientThread thread;

	private float xlocation;
	private int x_dim, y_dim;
	private int mTextSize;
	private int mBoundary, mGridSize;

	/** Channel 1 Variables **/
	private Paint channel1Paint, channel1TextPaint;
	private boolean channel1Active;
	private float channel1GraphYScale, graphshiftyChannel1;
	public PointF[] channel1Points = new PointF[1200];
	private int channel1Measurements = 0;
	private float channel1Max, channel1Min, channel1Mean;

	/** Channel 2 Variables **/
	private Paint channel2Paint, channel2TextPaint;
	private boolean channel2Active;
	private float channel2GraphYScale, graphshiftyChannel2;
	public PointF[] channel2Points = new PointF[1200];
	private int channel2Measurements = 0;
	private float channel2Max, channel2Min, channel2Mean;

	/** Channel Math Variabes **/
	private Paint channelMathPaint, channelMathTextPaint;
	private boolean channelMathActive;
	private float graphshiftyMath;
	public PointF[] channelMathPoints = new PointF[1200];
	private int channelMathMeasurements = 0;
	private float channelMathMin, channelMathMax, channelMathMean;
	private int mathCalculationType = 0;

	/**Channel Variables that apply to all channels **/
	private Paint generalTextPaint, gridPaint;
	private float graphscaleX, graphshiftx;
	private boolean displayUpdating = true;
	private boolean displayPaused = false;
	public int samplerate = 0;

	private float triggerlevel = (float) 1.0;
	private String triggerchannel = "A";
	private Paint triggerPaint;

	private float channel1Scale = (float) 0.0521; //Volts per step size from Scope
	private float channel2Scale = (float) 0.0521; //Volts per step size from Scope
	private float channelMathScale = (float) 0.0521; //Volts per step size (changeable)	

	private float totalGraphShiftx = 0;
	private float totalGraphShifty = 0;
	private float totalGraphScale = 1;
	float scale = 1;

	/*We need some JSON Arrays for Communicating*/
	/*Holds ints. 0 = no recent changes (return to this after service), 1 = recently pressed, 2 = recently unpressed*/
	JSONObject switchActions; 

	/*Holds ints. 0 = no recent changes (return to this after service), negative values mean rotate left (CCW), positive value mean rotate right (CW)*/
	int encoderActions[] = new int[8];

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;

	private Bitmap toDisk;
	public boolean screenShot = false;
	Context context;
	
	Bitmap stoppedImage;
	Bitmap runningImage;
	
	// Enumerations/Arrays for knowing what 'finger one' is touching
	ScreenItem activeScreenItems = ScreenItem.NONE;
	

	/**This is the public constructor for a DrawingPanel Object. We had to implement  two constuctors in order to fully implement a SurfaceView which DrawingPanel is an implementation of **/
		
	public DrawingPanel(Context context, AttributeSet attr) {
		super(context, attr); // This is required to make sure that the basics of a surface view that need to be done are done. What follows id specific to DrawingPanel
		this.context = context;
		getHolder().addCallback(this); 
		_thread = new DrawingThread(getHolder(), this); // A Surfaceview has a built in thread object. This creates the thread.
		xlocation = 0; // Used only for autogenerated waves in testing
		mTextSize = 15; // Default size of Oscope text
		mBoundary = 10; // Default distance from the edge of the surface view
		mGridSize = 50; // Default number of pixels between grid lines on the surface view
		graphscaleX = 1; // Default scale for all signals in x dimension (time)
		channel1GraphYScale = 10; // Default scale for channel 1 signals in y dimension (volts)
		channel2GraphYScale = 10; // Default scale for channel 2 signals in y dimension (volts)
		graphshiftx = 0; // Default shift in x-axis same for both channels (time)
		graphshiftyChannel1 = 500; // Default shift in y dimension (volts) for channel 1
		graphshiftyChannel2 = 100; // Default shift in y dimension (volts) for channel 2
		graphshiftyMath = 300; // Default shift in y dimension (volts) for Math channel

		stoppedImage = BitmapFactory.decodeResource(getResources(), R.drawable.play);
		runningImage = BitmapFactory.decodeResource(getResources(), R.drawable.pause);
		
		tts = new Texttospeech(this.context);
		
		for (int i = 0; i < channel1Points.length; i++) { // Initialize the array of 1200 points to hold the waveforms.
			channel1Points[i] = new PointF();
			channel2Points[i] = new PointF();
			channelMathPoints[i] = new PointF();
		}
		/**Paint properties for the triggering line**/
		triggerPaint = new Paint();
		triggerPaint.setDither(true);
		triggerPaint.setColor(0xFFFF6600);
		triggerPaint.setStyle(Paint.Style.STROKE);
		triggerPaint.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
		triggerPaint.setStrokeWidth(3);

		/**Paint properties for Channel 1 **/
		channel1Paint = new Paint();
		channel1Paint.setDither(true);
		channel1Paint.setColor(0xFFCC0000);
		channel1Paint.setStyle(Paint.Style.STROKE);
		channel1Paint.setStrokeJoin(Paint.Join.ROUND);
		channel1Paint.setStrokeCap(Paint.Cap.ROUND);
		channel1Paint.setStrokeWidth(10);

		/**Paint properties for Channel 2 **/
		channel2Paint = new Paint();
		channel2Paint.setDither(true);
		channel2Paint.setColor(0xFFFFB400);
		channel2Paint.setStyle(Paint.Style.STROKE);
		channel2Paint.setStrokeJoin(Paint.Join.ROUND);
		channel2Paint.setStrokeCap(Paint.Cap.ROUND);
		channel2Paint.setStrokeWidth(10);

		/**Paint properties for Math Channel **/
		channelMathPaint = new Paint();
		channelMathPaint.setDither(true);
		channelMathPaint.setColor(0xFF66FF33);
		channelMathPaint.setStyle(Paint.Style.STROKE);
		channelMathPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathPaint.setStrokeWidth(10);

		/**Paint properties for Display Grid**/
		gridPaint = new Paint();
		gridPaint.setDither(true);
		gridPaint.setColor(0x7FFFFFFF);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeJoin(Paint.Join.ROUND);
		gridPaint.setStrokeCap(Paint.Cap.ROUND);
		gridPaint.setStrokeWidth(1);

		/**Paint properties for General text**/
		generalTextPaint = new Paint();
		generalTextPaint.setDither(true);
		generalTextPaint.setColor(0xFFFFFF00);
		generalTextPaint.setStyle(Paint.Style.FILL);
		generalTextPaint.setStrokeJoin(Paint.Join.ROUND);
		generalTextPaint.setStrokeCap(Paint.Cap.ROUND);
		generalTextPaint.setStrokeWidth(1);
		generalTextPaint.setTextSize((float) 30);


		/**Paint properties for Channel 1 text**/
		channel1TextPaint = new Paint();
		channel1TextPaint.setDither(true);
		channel1TextPaint.setColor(0xFFCC0000);
		channel1TextPaint.setStyle(Paint.Style.FILL);
		channel1TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel1TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel1TextPaint.setStrokeWidth(3);
		channel1TextPaint.setTextSize((float) 50);

		/**Paint properties for Channel 2 text**/
		channel2TextPaint = new Paint();
		channel2TextPaint.setDither(true);
		channel2TextPaint.setColor(0xFFFFB400);
		channel2TextPaint.setStyle(Paint.Style.FILL);
		channel2TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel2TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel2TextPaint.setStrokeWidth(3);
		channel2TextPaint.setTextSize((float) 50);

		/**Paint properties for Channel Math text**/
		channelMathTextPaint = new Paint();
		channelMathTextPaint.setDither(true);
		channelMathTextPaint.setColor(0xFF66FF33);
		channelMathTextPaint.setStyle(Paint.Style.FILL);
		channelMathTextPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathTextPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathTextPaint.setStrokeWidth(3);
		channelMathTextPaint.setTextSize((float) 50);

		// Instantiate the gesture detector with the
		// application context and an implementation of
		// GestureDetector.OnGestureListener
		mDetector = new GestureDetectorCompat(context, this);
		// Set the gesture detector as the double tap
		// listener.
		mDetector.setOnDoubleTapListener(this);

		mScaleDetector = new ScaleGestureDetector(context, this);

		//setLongClickable(true);

		/**Create the networking thread**/
		/**Commented out to create non-network test version**/

		thread = new ClientThread();
		cThread = new Thread(thread);
		cThread.start();
		Log.d("DrawingPanel", "Launching TCPIP Thread: " + String.valueOf(cThread.getId()));
		switchActions = thread.getSwitchActions();


	}

	public DrawingPanel(Context context) {
		super(context);// This is required to make sure that the basics of a surface view that need to be done are done. What follows id specific to DrawingPanel
		this.context = context;
		getHolder().addCallback(this); 
		_thread = new DrawingThread(getHolder(), this); // A Surfaceview has a built in thread object. This creates the thread.
		xlocation = 0; // Used only for autogenerated waves in testing
		mTextSize = 15; // Default size of Oscope text
		mBoundary = 10; // Default distance from the edge of the surface view
		mGridSize = 50; // Default number of pixels between grid lines on the surface view
		graphscaleX = 1; // Default scale for all signals in x dimension (time)
		channel1GraphYScale = 10; // Default scale for channel 1 signals in y dimension (volts)
		channel2GraphYScale = 10; // Default scale for channel 2 signals in y dimension (volts)
		graphshiftx = 0; // Default shift in x-axis same for both channels (time)
		graphshiftyChannel1 = 500; // Default shift in y dimension (volts) for channel 1
		graphshiftyChannel2 = 100; // Default shift in y dimension (volts) for channel 2
		graphshiftyMath = 300; // Default shift in y dimension (volts) for Math channel
		
		stoppedImage = BitmapFactory.decodeResource(getResources(), R.drawable.play);
		runningImage = BitmapFactory.decodeResource(getResources(), R.drawable.pause);
		
		tts = new Texttospeech(this.context);
		
		for (int i = 0; i < channel1Points.length; i++) { // Initialize the array of 1200 points to hold the waveforms.
			channel1Points[i] = new PointF();
			channel2Points[i] = new PointF();
			channelMathPoints[i] = new PointF();
		}
		/**Paint properties for the triggering line**/
		triggerPaint = new Paint();
		triggerPaint.setDither(true);
		triggerPaint.setColor(0xFFFF6600);
		triggerPaint.setStyle(Paint.Style.STROKE);
		triggerPaint.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
		triggerPaint.setStrokeWidth(3);

		/**Paint properties for Channel 1 **/
		channel1Paint = new Paint();
		channel1Paint.setDither(true);
		channel1Paint.setColor(0xFFCC0000);
		channel1Paint.setStyle(Paint.Style.STROKE);
		channel1Paint.setStrokeJoin(Paint.Join.ROUND);
		channel1Paint.setStrokeCap(Paint.Cap.ROUND);
		channel1Paint.setStrokeWidth(10);

		/**Paint properties for Channel 2 **/
		channel2Paint = new Paint();
		channel2Paint.setDither(true);
		channel2Paint.setColor(0xFFFFB400);
		channel2Paint.setStyle(Paint.Style.STROKE);
		channel2Paint.setStrokeJoin(Paint.Join.ROUND);
		channel2Paint.setStrokeCap(Paint.Cap.ROUND);
		channel2Paint.setStrokeWidth(10);

		/**Paint properties for Math Channel **/
		channelMathPaint = new Paint();
		channelMathPaint.setDither(true);
		channelMathPaint.setColor(0xFF66FF33);
		channelMathPaint.setStyle(Paint.Style.STROKE);
		channelMathPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathPaint.setStrokeWidth(10);

		/**Paint properties for Display Grid**/
		gridPaint = new Paint();
		gridPaint.setDither(true);
		gridPaint.setColor(0x7FFFFFFF);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeJoin(Paint.Join.ROUND);
		gridPaint.setStrokeCap(Paint.Cap.ROUND);
		gridPaint.setStrokeWidth(1);

		/**Paint properties for General text**/
		generalTextPaint = new Paint();
		generalTextPaint.setDither(true);
		generalTextPaint.setColor(0xFFFFFF00);
		generalTextPaint.setStyle(Paint.Style.FILL);
		generalTextPaint.setStrokeJoin(Paint.Join.ROUND);
		generalTextPaint.setStrokeCap(Paint.Cap.ROUND);
		generalTextPaint.setStrokeWidth(1);
		generalTextPaint.setTextSize((float) 30);


		/**Paint properties for Channel 1 text**/
		channel1TextPaint = new Paint();
		channel1TextPaint.setDither(true);
		channel1TextPaint.setColor(0xFFCC0000);
		channel1TextPaint.setStyle(Paint.Style.FILL);
		channel1TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel1TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel1TextPaint.setStrokeWidth(3);
		channel1TextPaint.setTextSize((float) 50);

		/**Paint properties for Channel 2 text**/
		channel2TextPaint = new Paint();
		channel2TextPaint.setDither(true);
		channel2TextPaint.setColor(0xFFFFB400);
		channel2TextPaint.setStyle(Paint.Style.FILL);
		channel2TextPaint.setStrokeJoin(Paint.Join.ROUND);
		channel2TextPaint.setStrokeCap(Paint.Cap.ROUND);
		channel2TextPaint.setStrokeWidth(3);
		channel2TextPaint.setTextSize((float) 50);

		/**Paint properties for Channel Math text**/
		channelMathTextPaint = new Paint();
		channelMathTextPaint.setDither(true);
		channelMathTextPaint.setColor(0xFF66FF33);
		channelMathTextPaint.setStyle(Paint.Style.FILL);
		channelMathTextPaint.setStrokeJoin(Paint.Join.ROUND);
		channelMathTextPaint.setStrokeCap(Paint.Cap.ROUND);
		channelMathTextPaint.setStrokeWidth(3);
		channelMathTextPaint.setTextSize((float) 50);

		// Instantiate the gesture detector with the
		// application context and an implementation of
		// GestureDetector.OnGestureListener
		mDetector = new GestureDetectorCompat(context, this);
		// Set the gesture detector as the double tap
		// listener.
		mDetector.setOnDoubleTapListener(this);
		mDetector.setIsLongpressEnabled(false);

		mScaleDetector = new ScaleGestureDetector(context, this);

		setLongClickable(true);

		
		
		/**Create the networking thread**/
		/**Commented out to create non-network test version**/

		thread = new ClientThread();
		cThread = new Thread(thread);
		cThread.start();
		Log.d("DrawingPanel", "Launching TCPIP Thread: " + String.valueOf(cThread.getId()));
		switchActions = thread.getSwitchActions();
		
	}

/** Callback to access ribbon menus in main activity **/
	RibbonMenuCallback RibbonMenuCallbackClass;
	
	void registerMenuCallback(RibbonMenuCallback callback){
	  RibbonMenuCallbackClass = callback;
	 }

	ButtonCallback ButtonCallbackClass;
	
	void registerButtonCallback(ButtonCallback callback) {
		ButtonCallbackClass = callback;
	}


	//Not sure what this does, i dont think it is necessary any more
	MainActivity theListener;
	
	public void setLongClickListener(MainActivity l) {
        theListener = l;
    }
	
	/** This section is for the overrides for the gesture listener */

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mScaleDetector.onTouchEvent(event);
		this.mDetector.onTouchEvent(event);
		// Be sure to call the superclass implementation
		
		
		
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDown: " );
		displayUpdating = false;
		thread.setDisplayUpdating(false);
		return true;
	}

	@Override
	public boolean onFling(MotionEvent event1, MotionEvent event2,
			float velocityX, float velocityY) {
		Log.d(DEBUG_TAG, "onFling: " + event1.getX());
		if (event1.getX() < 40)
			RibbonMenuCallbackClass.ToggleRibbonMenu(LEFT_ANIM);
		if (event1.getX() > 1180)
			RibbonMenuCallbackClass.ToggleRibbonMenu(RIGHT_ANIM);
		return true;
	}


	
	@Override
	public void onLongPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onLongPress: " + event.getX() + ", " + event.getY());
		//theListener.onLongClick(this);
		int surfWidth = this.getWidth(); 
		int surfHeight = this.getHeight(); 
		int surfX = this.getLeft(); 
		int surfY = this.getTop();
		
//		showPopup(findViewById(R.id.scopeView));
		
		
		for (int i = 0; i < this.channel1Points.length ; i++){ //
			//Log.d(DEBUG_TAG, "XY: " + (int)(event.getX()) + ", " + (int)(event.getY()) +" current Channel1 XY: " + (int)((((this.channel1Points[i].x*graphscaleX)+graphshiftx))) + "," + (int)(((this.channel1Points[i].y+graphshiftyChannel1))) ); 
			if ((int)((event.getX())/30) == (int)((((this.channel1Points[i].x*graphscaleX)+graphshiftx))/(30*graphscaleX))){ //channel1Points[i].x * graphscaleX + graphshiftx
				//Log.d(DEBUG_TAG, "Channel 1 X Match");
				if ((int)((event.getY())/30) == (int)(((this.channel1Points[i].y * channel1GraphYScale)+graphshiftyChannel1)/30)){ 
					Log.d(DEBUG_TAG, "Channel 1 Touch"); 
					activeScreenItems = ScreenItem.CH1;
					//channel1Active = false;
					RibbonMenuCallbackClass.ToggleRibbonWaveformMenu(CHANNEL_1);
					break;
				}
			}

		}
		for (int i = 0; i < this.channel2Points.length ; i++){ //
			//Log.d(DEBUG_TAG, "XY: " + (int)(event.getX()) + ", " + (int)(event.getY()) +" current Channel2 XY: " + (int)((((this.channel2Points[i].x*graphscaleX)+graphshiftx))) + "," + (int)(((this.channel2Points[i].y+graphshiftyChannel2))) ); 
			if ((int)((event.getX())/30) == (int)((((this.channel2Points[i].x*graphscaleX)+graphshiftx))/(30*graphscaleX))){ //channel1Points[i].x * graphscaleX + graphshiftx
				//Log.d(DEBUG_TAG, "Channel 1 X Match");
				if ((int)((event.getY())/30) == (int)(((this.channel2Points[i].y* channel2GraphYScale)+graphshiftyChannel2)/30)){ 
					Log.d(DEBUG_TAG, "Channel 2 Touch"); 
					activeScreenItems = ScreenItem.CH2;
					//channel2Active = false;
					RibbonMenuCallbackClass.ToggleRibbonWaveformMenu(CHANNEL_2);
					break;
				}
			}

		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		Log.d(DEBUG_TAG, "onScroll: ");
		graphshiftx = -distanceX + graphshiftx;
		graphshiftyChannel1 = distanceY + graphshiftyChannel1;
		return true;
	}

	@Override
	public void onShowPress(MotionEvent event) {
		Log.d(DEBUG_TAG, "onShowPress: " );
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		Log.d(DEBUG_TAG, "onSingleTapUp: ");
		displayUpdating = true;
		displayPaused = true;
		thread.setDisplayUpdating(true);
		/*if (displayPaused){
			displayUpdating = false;
			displayPaused = false;
			thread.setDisplayUpdating(false);
		}
		else{
			displayUpdating = true;
			displayPaused = true;
			thread.setDisplayUpdating(true);
		}*/
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDoubleTap: ");
		//RibbonMenuCallbackClass.ToggleRibbonMenu(LEFT_ANIM);
		//RibbonMenuCallbackClass.ToggleRibbonMenu(RIGHT_ANIM);
		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent event) {
		Log.d(DEBUG_TAG, "onDoubleTapEvent: ");
		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent event) {
		Log.d(DEBUG_TAG, "onSingleTapConfirmed: " );
		return true;
	}
//Function that captures a screenshot and opens the gallery. It is dirty and uses some global variables connected to main activity and drawing thread.
	public void screenCap(){ 
		
		Calendar calendar = Calendar.getInstance();
		 int date = calendar.get(Calendar.DATE);
		 int month = calendar.get(Calendar.MONTH);
		 int year = calendar.get(Calendar.YEAR);
		 int hour = calendar.get(Calendar.HOUR_OF_DAY);
		 int minute = calendar.get(Calendar.MINUTE);
		 int second = calendar.get(Calendar.SECOND);
		 
		 
		 OutputStream fOut = null;
		 String ourDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScholaPics";
		 String imgname = Integer.toString(year) + "." + Integer.toString(month + 1) + "." +  Integer.toString(date) + "-" +  Integer.toString(hour) + ":" +  Integer.toString(minute) + "." +  Integer.toString(second) +  ".jpg";
         File myDir=new File(ourDir);
         myDir.mkdirs();
         Log.d("screenCap", "File saving to: " + ourDir + imgname);
		 File file = new File(ourDir, imgname);
             try {
				fOut = new FileOutputStream(file);
		         toDisk.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
		         Log.d("screenCap", "File saved to: " + ourDir + "/" + imgname);
	             fOut.flush();
	             fOut.close();
	             //MediaStore.Images.Media.insertImage(context.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
	             screenShot = false;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
             //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    
             //String SCAN_PATH;
             //File[] allFiles ;
             //File folder = new File(ourDir + "/");
             //allFiles = folder.listFiles();
             //Log.d("screenCap", "Opening: " + allFiles[allFiles.length - 1].getAbsolutePath());
             //new SingleMediaScanner(context, allFiles[0]);
             
             // tells your intent to get the contents
          // opens the URI for your image directory on your sdcard
          //intent.setType(ourDir); 
          //((Activity) RibbonMenuCallbackClass).startActivityForResult(intent, 1);


            
	}
	
	 

	/** This section is for the overrides for the SurfaceView */
//Creates a bitmap that onDraw can draw to and be saved
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.x_dim = w;
    	this.y_dim = h;
        toDisk = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        super.onSizeChanged(w, h, oldw, oldh);
    }
	
	@Override
	public void onDraw(Canvas canvas) {
		int i = 0;
		int j = 0;
		int pointCount = 1000;//this.getWidth();
		
		x_dim = this.getWidth();
		y_dim = this.getHeight();
		float smallTextPaintSize = (float) ((.5)*(y_dim / mTextSize));
		float largeTextPaintSize = (float) ((y_dim / mTextSize));
		generalTextPaint.setTextSize(smallTextPaintSize);
		channel1TextPaint.setTextSize(largeTextPaintSize);
		channel2TextPaint.setTextSize(largeTextPaintSize);
		generalTextPaint.setColor(0xFFFFFF00);
		channelMathTextPaint.setTextSize(largeTextPaintSize);
		mGridSize = (int) (y_dim - 2*mBoundary)/8;
		
		/** This section creates example waveforms.Comment out when examining live data**/
	
		serviceButtons();
		serviceEncoders();

		//Send all draw commands to this canvas also
		canvas.drawBitmap(toDisk, new Matrix(), new Paint());
		canvas.setBitmap(toDisk);
	
		
		canvas.drawColor(Color.BLACK);

		//Prepare to Draw horizontal lines
		j = (this.getWidth() % mGridSize) / 2; 
		for (i = (y_dim % mGridSize) / 2; i < y_dim; i = i + mGridSize) {
			canvas.drawLine((float) j, (float) i, (float) (this.getWidth() - j),
					(float) i, gridPaint);
		}
		
		canvas.drawLine((float) j, (float) i - mGridSize-5-smallTextPaintSize, (float) (this.getWidth() - j),
				(float) i - mGridSize-5-smallTextPaintSize, gridPaint);
		
		//Prepare to Draw vertical lines
		i = (y_dim % mGridSize) / 2;
		canvas.drawLine((float) (this.getWidth() % mGridSize) / 2, (float) i, (float) ((this.getWidth() % mGridSize) / 2),
				(float) (y_dim - i), gridPaint);
		for (j = (this.getWidth() % mGridSize) / 2; j < this.getWidth(); j = j + mGridSize) {
			canvas.drawLine((float) j, (float) i, (float) (j),
					(float) (y_dim - i - 5-smallTextPaintSize), gridPaint);
		}
		canvas.drawLine((float) j - mGridSize, (float) i, (float) (j - mGridSize),
				(float) (y_dim - i), gridPaint);

		//triggerlevel = thread.getTriggerLevel();
		triggerlevel = (float)3.3;
		//Drawing trigger level
		if (triggerchannel == "A"){
			canvas.drawText(formatVolts(triggerlevel), (float) x_dim,
					(float) y_dim - ((triggerlevel* channel1GraphYScale) + graphshiftyChannel1), generalTextPaint);
			canvas.drawLine((float) mBoundary, (float) y_dim - ((triggerlevel* channel1GraphYScale) + graphshiftyChannel1), (float) (this.getWidth()-mBoundary), (float) (float) y_dim - (triggerlevel* channel1GraphYScale + graphshiftyChannel1), triggerPaint);
		}else{
			canvas.drawText(formatVolts(triggerlevel), (float) x_dim,
					(float) y_dim - ((triggerlevel* channel2GraphYScale) + graphshiftyChannel2), generalTextPaint);
			canvas.drawLine((float) mBoundary, (float) y_dim - (triggerlevel* channel2GraphYScale + graphshiftyChannel2), (float) (this.getWidth()-mBoundary), (float) (float) y_dim - (triggerlevel* channel2GraphYScale + graphshiftyChannel2), triggerPaint);
		}
		if (channel1Active) {
			channel1Max = channel1Points[0].y;
			channel1Min = channel1Points[0].y;
			for (i = 0; i < (pointCount - 1); i++) {
				canvas.drawLine(channel1Points[i].x * graphscaleX
						+ graphshiftx, y_dim - (channel1Points[i].y * channel1GraphYScale + graphshiftyChannel1),
						channel1Points[i + 1].x * graphscaleX + graphshiftx,
						y_dim - (channel1Points[i + 1].y * channel1GraphYScale + graphshiftyChannel1), channel1Paint);
				if (channel1Points[i].y > channel1Max)
					channel1Max = channel1Points[i].y;
				if (channel1Points[i].y < channel1Min)
					channel1Min = channel1Points[i].y;
				channel1Mean += channel1Points[i].y;
			}
			channel1Mean = channel1Mean/(pointCount - 1);
			channel1TextPaint.setTextAlign(Paint.Align.RIGHT);
			generalTextPaint.setColor(0xFFCC0000);
			generalTextPaint.setTextAlign(Paint.Align.CENTER);
			canvas.drawText(
					"CH1: ".concat(formatVolts((mGridSize)/channel1GraphYScale)).concat("/DIV"), (float) mBoundary+(x_dim/5),
					(float) y_dim - mBoundary-5, generalTextPaint);
			if (channel1Measurements != 0){
				canvas.drawText(getMeasurements(1, channel1Measurements), (float) mBoundary+(x_dim/5),
						(float) y_dim - mBoundary-5 - smallTextPaintSize, generalTextPaint);
			}
		}

		if (channel2Active) {
			channel2Max = channel2Points[0].y;
			channel2Min = channel2Points[0].y;
			for (i = 0; i < pointCount - 1; i++) {
				canvas.drawLine(channel2Points[i].x * graphscaleX
						+ graphshiftx, y_dim - (channel2Points[i].y * channel2GraphYScale  + graphshiftyChannel2),
						channel2Points[i + 1].x * graphscaleX + graphshiftx,
						y_dim - (channel2Points[i + 1].y * channel2GraphYScale + graphshiftyChannel2), channel2Paint);
				

				if (channel2Points[i].y > channel2Max)
					channel2Max = channel2Points[i].y;
				if (channel2Points[i].y < channel2Min)
					channel2Min = channel2Points[i].y;	
				channel2Mean += channel2Points[i].y;
			}
			channel2Mean = channel2Mean/(pointCount - 1);
			channel2TextPaint.setTextAlign(Paint.Align.RIGHT);
			generalTextPaint.setColor(0xFFFFB400);
			generalTextPaint.setTextAlign(Paint.Align.CENTER);
			canvas.drawText(
					"CH2: ".concat(formatVolts((mGridSize)/channel2GraphYScale)).concat("/DIV"), (float) mBoundary+2*(x_dim/5),
					(float) y_dim - mBoundary-5, generalTextPaint);
			if (channel2Measurements != 0){
				canvas.drawText(getMeasurements(2, channel2Measurements), (float) mBoundary+2*(x_dim/5),
						(float) y_dim - mBoundary-5 - smallTextPaintSize, generalTextPaint);
			}
		}

		if (channelMathActive) {
			channelMathMax = channelMathPoints[0].y;
			channelMathMin = channelMathPoints[0].y;
			for (i = 0; i < pointCount - 1; i++) {
				canvas.drawLine(channelMathPoints[i].x * graphscaleX
						+ graphshiftx, y_dim - (channelMathPoints[i].y + graphshiftyMath),
						channelMathPoints[i + 1].x * graphscaleX + graphshiftx,
						y_dim - (channelMathPoints[i + 1].y + graphshiftyMath), channelMathPaint);
				if (channelMathPoints[i].y > channelMathMax)
					channelMathMax = channelMathPoints[i].y;
				if (channelMathPoints[i].y < channelMathMin)
					channelMathMin = channelMathPoints[i].y;
				channelMathMean += channelMathPoints[i].y;
			}
			channelMathMean = channelMathMean/(pointCount);
			channelMathTextPaint.setTextAlign(Paint.Align.RIGHT);
			generalTextPaint.setColor(0xFF66FF33);
			generalTextPaint.setTextAlign(Paint.Align.CENTER);
			if (channelMathMeasurements != 0){
				canvas.drawText(
						getMeasurements(3, channelMathMeasurements), (float) mBoundary+3*(x_dim/5),
						(float) y_dim - mBoundary - 2*(smallTextPaintSize), generalTextPaint);
			}
		}

		samplerate = thread.getSampleRate();
		
		generalTextPaint.setTextAlign(Paint.Align.CENTER);
		canvas.drawText(
				"Time: ".concat(formatTime((float)(mGridSize*((float)Math.pow(2,samplerate)/(float)20000000)/graphscaleX))).concat("/DIV"), (float) mBoundary+4*(x_dim/5),
				(float) y_dim - mBoundary-5, generalTextPaint);	
		
		generalTextPaint.setTextAlign(Paint.Align.LEFT);
		if (displayUpdating){
			
			generalTextPaint.setColor(0xFF66FF33);
		//	canvas.drawBitmap(runningImage, 0, 0, generalTextPaint);
	       canvas.drawText(
					"Running", (float) mBoundary+55,
					(float) mBoundary+smallTextPaintSize+5, generalTextPaint);	
		} else {
			generalTextPaint.setColor(0xFFCC0000);
		//	canvas.drawBitmap(stoppedImage, 0, 0, generalTextPaint);
	       canvas.drawText(
					"Stopped", (float) mBoundary+60,
					(float) mBoundary+smallTextPaintSize+5, generalTextPaint);	

		}
			
		if (!thread.getIsConnected()){
			canvas.drawText(
					"Demo", (float) (x_dim - (2* mBoundary))/2,
					(float) (y_dim - (2* mBoundary))/2, generalTextPaint);	
		}
			
		
	}

	private void serviceButtons(){
		try{
			switchActions = thread.getSwitchActions();
		if (switchActions.getInt("A") == 1){
			setChannel1(null, !(getChannel1()));
			switchActions.put("A", 0);
		} else if (switchActions.getInt("B") == 1){
			setChannel2(null, !(getChannel2()));
			switchActions.put("B", 0);
		}  else if (switchActions.getInt("C") == 1){
			setChannelMath(null, !(getChannelMath()));
			switchActions.put("C", 0);
		}
		thread.setSwitchActions(switchActions);
		} catch (Exception e) {
			
		}
	}
	
	private void serviceEncoders(){
		try{
//			encoderActions = thread.getEncoderActions();
			if (encoderActions[1] != 0){
				if (encoderActions[1] == 1){ //CW Motion
					graphshiftyChannel1 += 10;
				} else{
					graphshiftyChannel1 -= 10;
				}
			encoderActions[1] = 0;
			} else if (encoderActions[2] != 0){
				if (encoderActions[2] == 1){ //CW Motion
					channel1GraphYScale *= 1.1;
				} else{
					channel1GraphYScale *= .9;
				}
			encoderActions[2] = 0;
			} else if (encoderActions[3] != 0){
				if (encoderActions[3] == 1){ //CW Motion
					graphshiftyChannel2 += 10;
				} else{
					graphshiftyChannel2 -= 10;
				}
			encoderActions[3] = 0;
			} else if (encoderActions[4] != 0){
				if (encoderActions[4] == 1){ //CW Motion
					channel2GraphYScale *= 1.1;
				} else{
					channel2GraphYScale *= .9;
				}
			encoderActions[4] = 0;
			} else if (encoderActions[5] != 0){
				if (encoderActions[5] == 1){ //CW Motion
					graphshiftx += 10;
				} else{
					graphshiftx -= 10;
				}
			encoderActions[5] = 0;
			} else if (encoderActions[6] != 0){
				if (encoderActions[6] == 1){ //CW Motion
					graphscaleX *= 1.1;
				} else{
					graphscaleX *= .9;
				}
			encoderActions[6] = 0;
			} else if (encoderActions[7] != 0){
				if (encoderActions[7] == 1){ //CW Motion
					
				} else{
					
				}
			encoderActions[7] = 0;
			}
			thread.setEncoderActions(encoderActions);
		} catch (Exception e) {
			
		}
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	
//Im not sure why all this works, its the final product that works for me even though i confused the heck out of myself setting it up
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("test", "surfaceCreated");
		if(_thread.getState() != Thread.State.NEW)
        {
			_thread = new DrawingThread(getHolder(), this);
			//Log.d("test", "before onresume");
            _thread.setRunning(true);
            _thread.start();

        }else{
		_thread.setRunning(true);
		_thread.start();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.d("test", "surfaceDestroyed");
		boolean retry = true;
		_thread.setRunning(false);
		while (retry) {
			try {
				_thread.interrupt();
				retry = false;
			} catch (Exception e) {
				// we will try it again and again...
			}
		}
	}



	/** This section is for the getter and setters */

	public void setDisplayUpdating(boolean value){
		thread.displayUpdating = value;
		displayUpdating = value;
	}
	
	public boolean getDisplayUpdating(){
		return thread.displayUpdating;
	}
	
	public void setMeasurments(int channel, int measurement){
		if (channel == 1){ //Channel 1
			channel1Measurements = measurement;
			tts.sayStuff("channel one measurement changed");
		} else if (channel == 2){ //Channel 1
			channel2Measurements = measurement;
			tts.sayStuff("channel two measurement changed");
		} else if (channel == 3){ //Channel 1
			channelMathMeasurements = measurement;
			tts.sayStuff("math channel measurement changed");
		} else {
			
		}
	}
	
	public String getMeasurements(int channel, int measurement){

		if (channel == 1){
			if (measurement == 1)
				return "Max: ".concat(formatVolts(channel1Max));
			else if (measurement == 2)
				return "Min: ".concat(formatVolts(channel1Min));
			else if (measurement == 3)
				return "Vpp: ".concat(formatVolts((channel1Max-channel1Min)));
			else if (measurement == 4)
				return "Mean: ".concat(formatVolts((channel1Mean)));
			else
				return "";
		} else if (channel == 2) {
			if (measurement == 1)
				return "Max: ".concat(formatVolts(channel2Max));
			else if (measurement == 2)
				return "Min: ".concat(formatVolts(channel2Min));
			else if (measurement == 3)
				return "Vpp: ".concat(formatVolts((channel2Max-channel2Min)));
			else if (measurement == 4)
				return "Mean: ".concat(formatVolts((channel2Mean)));
			else
				return "";
		} else if (channel == 3) {
			if (measurement == 1)
				return "Max: ".concat(formatVolts(channelMathMax));
			else if (measurement == 2)
				return "Min: ".concat(formatVolts(channelMathMin));
			else if (measurement == 3)
				return "Vpp: ".concat(formatVolts((channelMathMax-channelMathMin)));
			else if (measurement == 4)
				return "Mean: ".concat(formatVolts((channelMathMean)));
			else
				return "";
		} else
				return "";
	}

	public float channelMathCalculate(float ch1, float ch2, int mathMode){
		if (mathMode == 0){
			return ((float) 0);
		} else if (mathMode == 1){
			return ((float) ch1 + ch2);
		} else if (mathMode == 2){
			return ((float) ch1 - ch2);
		} else if (mathMode == 3){
			return ((float) ch2 - ch1);
		} else
			return ((float) 0);
	}

	public void setGraphScaleY(float scale) {
		channel1GraphYScale= scale;
		channel2GraphYScale= scale;

	}

	public void setMathCalculationType(int value){
		mathCalculationType = value;
	}
	
	public int getMathCalculationType(){
		return mathCalculationType;
	}
	
	public void setGraphScaleX(float scale) {
		graphscaleX= scale;
	}

	public void setGraphShiftChannel1(float x, float y) {
		graphshiftx = x;
		graphshiftyChannel1 = y;
	}

	public void setGraphShiftChannel2(float x, float y) {
		graphshiftx = x;
		graphshiftyChannel2 = y;
	}	

	public void setGraphShiftChannelMath(float x, float y) {
		graphshiftx = x;
		graphshiftyMath = y;
	}

	public DrawingThread getThread() {
		return _thread;
	}

	public void setChannel1(Context context, boolean mode) {
		channel1Active = mode;
		try
		{
			JSONObject mainObj = new JSONObject();
			JSONObject led = new JSONObject();
			led.put("id", "A");

			if (mode){
				if (context != null)
					Toast.makeText(context, "Channel 1 On", Toast.LENGTH_SHORT).show();
				led.put("value", true);
				tts.sayStuff("channel one on");

			} else{
				if (context != null)
					Toast.makeText(context, "Channel 1 Off", Toast.LENGTH_SHORT).show();
				led.put("value", false);
				tts.sayStuff("channel one off");
			}

			mainObj.put("led", led);
			thread.send(mainObj.toString());
			Log.d("DrawingPanel", "mainObj: " + mainObj.toString());
		}
		catch (JSONException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void setTriggerChannel(String triggerChannel) {
		try
		{
			triggerchannel = triggerChannel;
			JSONObject channel = new JSONObject();

			channel.put("trigger-channel", triggerChannel);
	
			thread.send(channel.toString());
		}
		catch (JSONException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void setTriggerType(String triggerType) {
		try
		{
			JSONObject type = new JSONObject();

			type.put("trigger-type", triggerType);
	
			thread.send(type.toString());

		}
		catch (JSONException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public boolean getChannel1() {
		return channel1Active;
	}

	public void setChannel2(Context context, boolean mode) {
		channel2Active = mode;
		try
		{
			JSONObject mainObj = new JSONObject();
			JSONObject led = new JSONObject();
			led.put("id", "B");

			if (mode){
				if (context != null)
					Toast.makeText(context, "Channel 2 On", Toast.LENGTH_SHORT).show();
				led.put("value", true);
				tts.sayStuff("channel two on");

			} else{
				if (context != null)
					Toast.makeText(context, "Channel 2 Off", Toast.LENGTH_SHORT).show();
				led.put("value", false);
				tts.sayStuff("channel two off");
			}

			mainObj.put("led", led);
			thread.send(mainObj.toString());
			Log.d("DrawingPanel", "mainObj: " + mainObj.toString());
		}
		catch (JSONException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public boolean getChannel2() {
		return channel2Active;
	}

	public void setChannelMath(Context context, boolean mode) {
		channelMathActive = mode;
		try
		{
			JSONObject mainObj = new JSONObject();
			JSONObject led = new JSONObject();
			led.put("id", "C");

			if (mode){
				if (context != null)
					Toast.makeText(context, "Math Channel On", Toast.LENGTH_SHORT).show();
				led.put("value", true);

			} else{
				if (context != null)
					Toast.makeText(context, "Math Channel Off", Toast.LENGTH_SHORT).show();
				led.put("value", false);
			}

			mainObj.put("led", led);
			thread.send(mainObj.toString());
			Log.d("DrawingPanel", "mainObj: " + mainObj.toString());
		}
		catch (JSONException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	
	public boolean getChannelMath() {
		return channelMathActive;
	}

	public ScreenItem getActiveScreenItems(){
		return activeScreenItems;
	}
	
	/** This section is for utility functions **/
	private String formatVolts(Float input){
		if ((input >= 1) || (input <= -1))
			return String.format("%.2f V", input);
		else if (((1000*input) >= 1) || ((1000*input) <= -1))
			return String.format("%.2f mV", (1000*input));
		else if (((1000000*input) >= 1) || ((1000000*input) <= -1))
			return String.format("%.2f uV", (1000000*input));
		else if (((1000000000*input) >= 1) || ((1000000000*input) <= -1))
			return String.format("%.2f nV", (1000000000*input));
		else
			return "< 1nV";
	}

	private String formatTime(Float input){
		if ((input >= 1) || (input <= -1))
			return String.format("%.2f S", input);
		else if (((1000*input) >= 1) || ((1000*input) <= -1))
			return String.format("%.2f mS", (1000*input));
		else if (((1000000*input) >= 1) || ((1000000*input) <= -1))
			return String.format("%.2f uS", (1000000*input));
		else if (((1000000000*input) >= 1) || ((1000000000*input) <= -1))
			return String.format("%.2f nS", (1000000000*input));
		else
			return "< 1pS";
	}

	
	
	/** This section is for the overrides for the ScaleListener */

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		Log.d("Scaling", "Scaling: " + channel1GraphYScale);
		if (detector.getCurrentSpanX()*2 < detector.getCurrentSpanY()) // Y bigger than X
			channel1GraphYScale *= detector.getScaleFactor();
		else if (detector.getCurrentSpanY()*2 < detector.getCurrentSpanX()) // X bigger than Y
			graphscaleX *= detector.getScaleFactor();
		else {
			channel1GraphYScale *= detector.getScaleFactor();
			graphscaleX *= detector.getScaleFactor();
		}

		// Don't let the object get too small or too large.
		channel1GraphYScale = Math.max(0.1f, Math.min(channel1GraphYScale, 50.0f));
		graphscaleX = Math.max(0.1f, Math.min(graphscaleX, 5.0f));

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		Log.d("Scaling", "Beginning Scale: " + channel1GraphYScale);

		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {
		// TODO Auto-generated method stub
		Log.d("Scaling", "Ending Scale: " + channel1GraphYScale);
				/*		float temp = (float)(mGridSize*((float)Math.pow(2,samplerate)/(float)20000000)/graphscaleX);
		int posiston = 0;
		int temp2 = 1000;
		if (samplerate <= 2)
			temp2 = 100;
		else if (samplerate <= 5)
			temp2 = 10;
		else if (samplerate <= 8)
			temp2 = 1;
		while (temp > 0){
			temp = temp - (float)(.0001)*temp2;
			posiston++;
		}
		graphscaleX = (float)(mGridSize*((float)Math.pow(2,samplerate)/(float)20000000))/(float)(.0001 * temp2 * posiston);
		 */


	}


	/** This section deals with the network connection **/

	public class ClientThread implements Runnable {

		private BufferedReader in;
		private BufferedWriter out;
		private float triggerlevel;
		private boolean displayUpdating;
		private int samplerate;
		private boolean isConnected = false;
		
		String triggerType;
		String triggerChannel;

		JSONObject oldswitches;
		JSONObject switchActions;
		
		int encoderActions[] = new int[8];

		public ClientThread(){
			switchActions = new JSONObject();
			try {
				switchActions.put("A", 0);
				switchActions.put("B", 0);
				switchActions.put("C", 0);
				switchActions.put("D", 0);
				switchActions.put("E", 0);
				switchActions.put("F", 0);
				switchActions.put("G", 0);
				switchActions.put("H", 0);
				switchActions.put("I", 0);
				switchActions.put("J", 0);
				switchActions.put("P", 0);
				
				oldswitches = new JSONObject();

				oldswitches.put("A", new JSONObject("{\"value\": false}"));
				oldswitches.put("B", new JSONObject("{\"value\": false}"));
				oldswitches.put("C", new JSONObject("{\"value\": false}"));
				oldswitches.put("D", new JSONObject("{\"value\": false}"));
				oldswitches.put("E", new JSONObject("{\"value\": false}"));
				oldswitches.put("F", new JSONObject("{\"value\": false}"));
				oldswitches.put("G", new JSONObject("{\"value\": false}"));
				oldswitches.put("H", new JSONObject("{\"value\": false}"));
				oldswitches.put("I", new JSONObject("{\"value\": false}"));
				oldswitches.put("J", new JSONObject("{\"value\": false}"));
				oldswitches.put("P", new JSONObject("{\"value\": false}"));			
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
			
			displayUpdating = true;
			samplerate = 1;
					
		}
		
		public void setDisplayUpdating(boolean value){
			displayUpdating = value;
		}
		
		public JSONObject getSwitchActions(){
			return switchActions;
		}
		
		public void setSwitchActions(JSONObject JSONinput){
			switchActions = JSONinput;
		}
		
		public boolean getIsConnected(){
			return isConnected;
		}
		
		
		public void setEncoderActions(int[] encoderInput){
			encoderActions = encoderInput;
		}
		
		public int[] getEncoderActions(){
			return encoderActions;
		}
		
		public void setTrigger(String inputTriggerType, String inputTriggerChannel){
			triggerType = inputTriggerType;
			triggerChannel = inputTriggerChannel;
		}
		
		public String getTriggerType(){
			return triggerType;
		}
		
		public String getTriggerChannel(){
			return triggerChannel;
		}
		public float getTriggerLevel(){
			return triggerlevel;
		}
		
		public int getSampleRate(){
			return samplerate;
		}

		public void send(String data) throws IOException {
			if (isConnected){
				if (data != null){ //Time to send a message to server
				Log.d("TekScope-ClientThread", "Sending JSON Object");
				Log.d("TekScope-ClientThread", data);
				out.write(data.concat("\n"));
				out.flush();
			} else {
				throw new InvalidParameterException();
			}
			}
		}

		private int braceCounter(String json){
			int count = 0;
			for (int i = 0; i < json.length(); i++){
				if (json.charAt(i) == '{')
					count ++;
				if (json.charAt(i) == '}')
					count --;
			}
			//Log.d("TekScope-ClientThread", "Brace Count: ".concat(String.valueOf(count)));
			return count;
		}

		public void run() {
			try {
				InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
				Log.d("TekScope-ClientThread", "C: Connecting...");
				Socket socket = new Socket(serverAddr, 15151);             

				if (socket != null){
					isConnected = true;
				} else {
					isConnected = false;
				}



				String jsonRaw;

				JSONArray channelA = null;
				JSONArray channelB = null;

				JSONObject switches = null;

				JSONObject encoders = null;
				int oldencoders[] = new int[8];

				if (isConnected){

					in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));   
					out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));

					if (out == null){
						Log.d("TekScope-ClientThread", "Can not open output!");
						return;
					}

					if (in == null){
						Log.d("TekScope-ClientThread", "Can not open input!");
						return;
					}

					while (true){    
						try {
							//Log.d("TekScope-ClientThread", "C: Sending command.");

							//while (!(in.ready()))
							//	Log.d("TekScope-ClientThread", "Not Ready");

							jsonRaw = in.readLine();
							//Log.d("TekScope-ClientThread", "Read a Line");
							//Log.d("TekScope-ClientThread", jsonRaw);

							while ((braceCounter(jsonRaw) > 0) && jsonRaw.charAt(0) == '{'){
								Log.d("TekScope-ClientThread", "Incomplete JSON Object");
								while (!(in.ready()));
								jsonRaw = jsonRaw.concat(in.readLine());
								//Log.d("TekScope-ClientThread", "Read a Line");
								//Log.d("TekScope-ClientThread", jsonRaw);
							}

							if (braceCounter(jsonRaw) == 0){

								/*This object can contain keys: switch, encoder, sample-rate, trigger-level, trigger-channel, trigger-type, A, and B*/
								JSONObject jsonIncoming = new JSONObject(jsonRaw);

								channelA = jsonIncoming.optJSONArray("A");
								channelB = jsonIncoming.optJSONArray("B");

								switches = jsonIncoming.optJSONObject("switch");
								encoders = jsonIncoming.optJSONObject("encoder");

								int tempint  = jsonIncoming.optInt("sample-rate", -1);
								if (tempint >= 0 && tempint <= 15){
									samplerate = tempint;
									Log.d("TekScope-ClientThread", "Sample Rate: ".concat(String.valueOf(samplerate)));
								}


								double tempdouble = jsonIncoming.optDouble("trigger-level");
								if (!(Double.isNaN(tempdouble))){
									triggerlevel = (float)tempdouble;
									Log.d("TekScope-ClientThread", "Trigger Level: ".concat(String.valueOf(triggerlevel)));
								}

								String temptype = jsonIncoming.optString("trigger-type");
								if (!(temptype.isEmpty()))
									Log.d("TekScope-ClientThread", "Trigger Type: ".concat(temptype));

								String tempchannel = jsonIncoming.optString("trigger-channel");
								if (!(tempchannel.isEmpty()))
									Log.d("TekScope-ClientThread", "Trigger Channel: ".concat(tempchannel));


								if (channelA != null){
									Log.d("TekScope-ClientThread", "Processing New Channel Data");
									for (int i = 0; i < channelA.length(); i++){
										if (displayUpdating) {
											channel1Points[i].y = (float)channelA.getDouble(i);
											channel1Points[i].x = i;
											if (channelB != null){
												channel2Points[i].y = (float)channelB.getDouble(i);
												channel2Points[i].x = i;
											}
											if (channelA != null && channelB != null){
												channelMathPoints[i].y = channelMathCalculate(channel1Points[i].y, channel2Points[i].y, mathCalculationType);
												channelMathPoints[i].x = i;
											}
										}
									}
								}
								// Handle Switches
								if (switches != null){
									try{
										String i = switches.getString("id");

										boolean value = switches.getBoolean("value");
										Log.d("TekScope-ClientThread", "New switch: ".concat(switches.toString()));
										boolean oldvalue = oldswitches.getJSONObject(i).getBoolean("value");									

										if (value == true && oldvalue == false)
											switchActions.put(i,1); //Just pressed
										else if (value == false && oldvalue == true)
											switchActions.put(i,2); //Just released
										else 
											switchActions.put(i,0); //Stable

										JSONObject switchtemp = new JSONObject();
										switchtemp.put("value", value);
										switchtemp.put("id", i);
										oldswitches.put(i, switchtemp);
									} catch (Exception e){

									}
									//Log.d("TekScope-ClientThread", "switches Object: ".concat(switchActions.toString()));
								}

								// Handle Encoders
								if (encoders != null){
									int i = encoders.getInt("id");
									if ((encoders.getInt("value") - oldencoders[i]) > 0)
										encoderActions[i] = 1; //CW motion
									else if ((encoders.getInt("value") - oldencoders[i]) < 0)
										encoderActions[i] = 2; //CCW Motion
									else 
										encoderActions[i] = 0; //Stable

									oldencoders[i] = encoders.getInt("value");
									//Log.d("TekScope-ClientThread", "encoders Object: ".concat(encoders.toString()));
								}

							}



						} catch (Exception e) {
							Log.e("TekScope-ClientThread", "S: Error", e);
							break;
						}
					}
					socket.close();
					Log.d("TekScope-ClientThread", "C: Closed.");
				} else {
					while(true){
						wait(100);
						if (displayUpdating) {
							for (int i = 0; i < 1200; i++) {
								channel1Points[i]
										.set((float) i, (float) ((100) * (Math
												.sin(((10*i + xlocation) * (.02))))));
								channel2Points[i].set((float) i,
										(float) ((50) * (Math.sin(((i) * (.02))))));
								channelMathPoints[i].set((float) i, (float) (channel2Points[i].y-channel1Points[i].y));
							}
						}
					}
				}
			} catch (Exception e) {
				Log.e("TekScope-ClientThread", "C: Error", e);
			}
		}

		private float getFloat(BufferedReader in){
			String temp = null;

			try {
				while (!(in.ready()));
				temp = in.readLine();

				while ((temp == null) || (temp.equalsIgnoreCase("")))
					temp = in.readLine();
			} catch (Exception e) {
			}

			return Float.valueOf(temp);
		}

	}
	
	
	
	public class SelectedItems {
		boolean NONE;
		boolean CH1;
		boolean CH2;
		boolean MTH1;
		boolean TRGR;
		
		public void wipe(){
			NONE = CH1 = CH2 = MTH1 = TRGR = false;
		}
		//setters
		public void setNONE(){
			NONE = true;
		}
		public void setCH1(){
			CH1 = true;
		}
		public void setCH2(){
			CH2 = true;
		}
		public void setMTH1(){
			MTH1 = true;
		}
		public void setTRGR(){
			TRGR = true;
		}
		//getters
		public boolean getNONE(){
			return NONE;
		}
		public boolean getCH1(){
			return CH1;
		}
		public boolean getCH2(){
			return CH2;
		}
		public boolean getMTH1(){
			return MTH1;
		}
		public boolean getTRGR(){
			return TRGR;
		}
	}




}



/*
@Override 
public boolean onTouchEvent(MotionEvent event) {


	int surfWidth = this.getWidth(); 
	int surfHeight = this.getHeight(); 
	int surfX = this.getLeft(); 
	int surfY = this.getTop();

	// Handle touch events here... 
	switch (event.getAction() & MotionEvent.ACTION_MASK) { 
	case MotionEvent.ACTION_DOWN:
		start.set(event.getX(), event.getY()); 
		Log.w(this.getClass().getName(), "mode=DRAG"); mode = DRAG;

		for (int i = 0; i < this.channel1Points.length ; i++){ //
			Log.w(this.getClass().getName(), "XY: " + (int)((event.getX()-surfX)/30)
					+ ", " + (int)((event.getY()-surfY-50)/30) +" current Channel1 XY: " + (int)((this.channel1Points[i].x)/30) + "," + (int)((this.channel1Points[i].y)/30) ); 
			if ((int)((event.getX()-surfX)/30) == (int)((this.channel1Points[i].x)/30)){ 
				if ((int)((event.getY()-surfY-50)/30) == (int)((this.channel1Points[i].y)/30)){ 
					Log.w(this.getClass().getName(), "Channel 1 Touch"); }
			}

		}

		break; 
	case MotionEvent.ACTION_POINTER_DOWN: 
		oldDist = spacing(event);
		Log.w(this.getClass().getName(), "oldDist=" + oldDist); if (oldDist > 10f) { //midPoint(mid, event); mode = ZOOM;
			Log.w(this.getClass().getName(), "mode=ZOOM"); 
		} 
		return false; 
		//break;
	case MotionEvent.ACTION_UP: 
		if (mode == DRAG){ 
			totalGraphShiftx = (event.getX() - start.x)+totalGraphShiftx; 
			totalGraphShifty = (event.getY() - start.y)+totalGraphShifty; 
		} 
	case MotionEvent.ACTION_POINTER_UP: 
		if (mode == ZOOM){ 
			Log.w(this.getClass().getName(), "Storing Scale"); 
			totalGraphScale = scale;
			Log.w(this.getClass().getName(), "totalGraphScale= " + totalGraphScale); } mode = NONE;

			Log.w(this.getClass().getName(), "mode=NONE"); 
			break; 
	case MotionEvent.ACTION_MOVE: 
		if (event.getEdgeFlags()==MotionEvent.EDGE_LEFT){
			Log.w(this.getClass().getName(), "Left Edge (Lauch Menu)");
			break; 
		} 
		if (mode == DRAG) { // ... 
			this.setGraphShift((event.getX() - start.x)+totalGraphShiftx, (event.getY() - start.y)+totalGraphShifty); }
		else if (mode == ZOOM) { 
			float newDist = spacing(event);
			Log.w(this.getClass().getName(), "newDist=" + newDist); 
			if (newDist > 10f) { 
				scale = newDist / oldDist;
				this.setGraphScale(scale*totalGraphScale);
				Log.w(this.getClass().getName(), "Scale= " + scale);
				Log.w(this.getClass().getName(), "totalGraphScale= " + totalGraphScale);
			} 
		} 
		break; 
	} 
	return super.onTouchEvent(event);
}
 */
