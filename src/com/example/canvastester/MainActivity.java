package com.example.canvastester;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

import com.darvds.ribbonmenu.RibbonMenuView;
import com.darvds.ribbonmenu.RibbonMenuCallback;
import com.darvds.ribbonmenu.iRibbonMenuCallback;
//import com.example.canvastester.DrawingPanel.SingleMediaScanner;
//import com.example.canvastester.DrawingPanel.RibbonMenuCallback;


import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.v4.view.GestureDetectorCompat;
import android.util.FloatMath;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.GestureDetector;


public class MainActivity extends Activity implements iRibbonMenuCallback, RibbonMenuCallback, ButtonCallback {  
	
	private final int LEFT_ANIM = 1;
	private final int RIGHT_ANIM = 2;
	private final int CHANNEL_1 = 1;
	private final int CHANNEL_2 = 2;	
	private final int MATH = 3;	
	private final int TRIGGER = 4;	
	private final int MAIN = 5;
	
	private int menutype = CHANNEL_1;
	
	private static final String DEBUG_TAG = "Gestures";
    private GestureDetectorCompat mDetector; 
	
    
    //Initialize Ribbon Menu
    private RibbonMenuView rbmView;

    private Texttospeech tts;
    
    ImageButton statusButton;
    
    @Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}
	//Register canvas for long press
	@Override
	public void openContextMenu(View view) {
		// TODO Auto-generated method stub
		super.openContextMenu(view);
	}

	private DrawingPanel mDrawingPanel;
    private DrawingThread mDrawingThread;
    private SurfaceView mSurfaceView;
    private Touch mTouch;
    
    private float totalGraphShiftx = 0;
    private float totalGraphShifty = 0;
    private float totalGraphScale = 1;
	float scale = 1;
    
    
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();


	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	   PointF start = new PointF();
	   PointF mid = new PointF();
	   float oldDist = 1f;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        Log.d("CanvasTester", "onCreate");
		
        
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);    
        
        //Creates a ribbon menu and sets left and right menus
        rbmView = (RibbonMenuView) findViewById(R.id.ribbonMenuView1);
        rbmView.setMenuClickCallback(this);
        rbmView.setMenuItems(R.menu.ribbon_menu, LEFT_ANIM);  
        rbmView.setMenuItems(R.menu.ribbon_menu_right_trigger, RIGHT_ANIM);

        mDrawingPanel = (DrawingPanel)findViewById(R.id.scopeView);
        mDrawingThread = mDrawingPanel.getThread();  
        mDrawingPanel.registerMenuCallback(this);
        mDrawingPanel.registerButtonCallback(this);
        
        tts = new Texttospeech(this);
        
        registerForContextMenu(findViewById(R.id.scopeView)); // Allow for a context menu for long click on the screen
		

        if (savedInstanceState == null) {
            Log.w(this.getClass().getName(), "SIS is null");
        } else {
            Log.w(this.getClass().getName(), "SIS is nonnull");
        }
        
        /**Create the onClick Listeners for the buttons on the app**/
  
        ImageButton buttonMenu = (ImageButton) findViewById(R.id.left_arrow_button);
        buttonMenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	  // Do something in response to button click
            	ToggleRibbonMenu(LEFT_ANIM, MAIN);
            }
        });
        
        //Play pause button that changes images
        statusButton = (ImageButton) findViewById(R.id.status_button);
        statusButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	  // Do something in response to button click
            	mDrawingPanel.setDisplayUpdating(!mDrawingPanel.getDisplayUpdating());
        		//change image depending on state
            	if(mDrawingPanel.getDisplayUpdating()){
        			statusButton.setImageResource(R.drawable.pause);
        		}else{
        			statusButton.setImageResource(R.drawable.play);
        		}
            }
        });
    }
   
    //Function to intercept button calls. Case statement determines which button is pressed
	@Override
	public void RibbonMenuItemClick(int itemId) {
		Context context = getApplicationContext();
		switch (itemId) {
			case R.id.ribbon_menu_channel_1:
				ToggleRibbonMenu(LEFT_ANIM, MAIN);
				ToggleRibbonMenu(RIGHT_ANIM, CHANNEL_1);
				menutype = CHANNEL_1;
				if(!mDrawingPanel.getChannel1()){
            		mDrawingPanel.setChannel1(context, true);
        
            	}
            	break;
			case R.id.ribbon_menu_channel_2:
				ToggleRibbonMenu(LEFT_ANIM, MAIN);
				ToggleRibbonMenu(RIGHT_ANIM, CHANNEL_2);
				menutype = CHANNEL_2;
				if(!mDrawingPanel.getChannel2()){
            		mDrawingPanel.setChannel2(context, true);
            	}
				break;
			case R.id.ribbon_menu_math_channel:
				ToggleRibbonMenu(LEFT_ANIM, MAIN);
				ToggleRibbonMenu(RIGHT_ANIM, MATH);
				menutype = MATH;
				if(!mDrawingPanel.getChannelMath()){
            		mDrawingPanel.setChannelMath(context, true);
            		tts.sayStuff("math channel on");
            	}
				break;
			case R.id.ribbon_menu_trigger:
				ToggleRibbonMenu(LEFT_ANIM, MAIN);
				ToggleRibbonMenu(RIGHT_ANIM, TRIGGER);
				menutype = TRIGGER;
				break;
			case R.id.ribbon_menu_autoset:
				ToggleRibbonMenu(LEFT_ANIM, MAIN);
				totalGraphShiftx = 0;
                totalGraphShifty = 100;
                totalGraphScale = 1;
                mDrawingPanel.setGraphScaleY(1);
                mDrawingPanel.setGraphScaleX(1);
            	mDrawingPanel.setGraphShiftChannelMath(0, 300);
            	mDrawingPanel.setGraphShiftChannel1(0, 100);
            	mDrawingPanel.setGraphShiftChannel2(0, 500);  
            	break;
			case R.id.ribbon_menu_enablechannel:
				if (menutype == CHANNEL_2){
					if(mDrawingPanel.getChannel2()){
						mDrawingPanel.setChannel2(context, false);
					} else {
						mDrawingPanel.setChannel2(context, true);
					}
				} else if (menutype == CHANNEL_1){
					if(mDrawingPanel.getChannel1()){
						mDrawingPanel.setChannel1(context, false);
					} else {
						mDrawingPanel.setChannel1(context, true);
					}	
				} else if (menutype == MATH){
					if(mDrawingPanel.getChannelMath()){
	            		mDrawingPanel.setChannelMath(context, false);
	            		
	            	} else {
	            		mDrawingPanel.setChannelMath(context, true);
	            	}	
				}
            	break;
			case R.id.ribbon_menu_measurements:
			case R.id.ribbon_menu_max:
				if (menutype == CHANNEL_2){
					mDrawingPanel.setMeasurments(2,1);
				} else if (menutype == CHANNEL_1){
					mDrawingPanel.setMeasurments(1,1);
				}  else if (menutype == MATH){
					mDrawingPanel.setMeasurments(3,1);
				}
            	break;
			case R.id.ribbon_menu_min:
				if (menutype == CHANNEL_2){
					mDrawingPanel.setMeasurments(2,2);
				} else if (menutype == CHANNEL_1){
					mDrawingPanel.setMeasurments(1,2);
				}  else if (menutype == MATH){
					mDrawingPanel.setMeasurments(3,2);
				}
            	break;
			case R.id.ribbon_menu_peak_to_peak:
				if (menutype == CHANNEL_2){
					mDrawingPanel.setMeasurments(2,3);
				} else if (menutype == CHANNEL_1){
					mDrawingPanel.setMeasurments(1,3);
				}  else if (menutype == MATH){
					mDrawingPanel.setMeasurments(3,3);
				}
            	break;
			case R.id.ribbon_menu_mean:
				if (menutype == CHANNEL_2){
					mDrawingPanel.setMeasurments(2,4);
				} else if (menutype == CHANNEL_1){
					mDrawingPanel.setMeasurments(1,4);
				}  else if (menutype == MATH){
					mDrawingPanel.setMeasurments(3,4);
				}
            	break;
			case R.id.ribbon_menu_measure_off:
				if (menutype == CHANNEL_2){
					mDrawingPanel.setMeasurments(2,0);
				} else if (menutype == CHANNEL_1){
					mDrawingPanel.setMeasurments(1,0);
				}  else if (menutype == MATH){
					mDrawingPanel.setMeasurments(3,0);
				}
            	break;           	
			case R.id.ribbon_menu_ch1_plus_ch2:
				mDrawingPanel.setMathCalculationType(1);
				break;
			case R.id.ribbon_menu_ch1_minus_ch2:
				mDrawingPanel.setMathCalculationType(2);
				break;
			case R.id.ribbon_menu_ch2_minus_ch1:
				mDrawingPanel.setMathCalculationType(3);
				break;				
			case R.id.ribbon_menu_saveimage:
				ToggleRibbonMenu(LEFT_ANIM, MAIN);
				mDrawingPanel.screenShot = true;
				while(mDrawingPanel.screenShot);
				String ourDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScholaPics";
				File[] allFiles ;
	             File folder = new File(ourDir + "/");
	             allFiles = folder.listFiles();
	             Log.d("screenCap", "Opening: " + allFiles[allFiles.length - 1].getAbsolutePath());
	             new SingleMediaScanner(this, allFiles[allFiles.length - 1]);
				
				//getScreen(R.id.scopeView);
				break;
				
			case R.id.ribbon_menu_triggerch1:
				mDrawingPanel.setTriggerChannel("A");
				break;
				
			case R.id.ribbon_menu_triggerch2:
				mDrawingPanel.setTriggerChannel("B");
				break;
				
			case R.id.ribbon_menu_triggerrising:
				mDrawingPanel.setTriggerType("F");
				break;
				
			case R.id.ribbon_menu_triggerfalling:
				mDrawingPanel.setTriggerType("F");
				break;
		      case R.id.ribbon_menu_view_saveimage:
		    	         String vourDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "ScholaPics";
		    	          File[] vallFiles ;
		    	                 File vfolder = new File(vourDir + "/");
		    	                 vallFiles = vfolder.listFiles();
		    	                 if (vallFiles.length == 0){
		    	                	 Toast.makeText(context, "No Images have been saved", Toast.LENGTH_LONG).show();
		    	                	 break;
		    	                 }
		    	                 Log.d("screenCap", "Opening: " + vallFiles[vallFiles.length - 1].getAbsolutePath());
		    	                 new SingleMediaScanner(this, vallFiles[vallFiles.length - 1]);
		    	                 break;
		    	        case R.id.ribbon_menu_exit:
		    	          Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
		    	          intentToResolve.addCategory(Intent.CATEGORY_HOME);
		    	          intentToResolve.setPackage("com.android.launcher");
		    	          ResolveInfo ri = getPackageManager().resolveActivity(intentToResolve, 0);
		    	          if (ri != null) 
		    	          {
		    	              Intent intent = new Intent(intentToResolve);
		    	              intent.setClassName(ri.activityInfo.applicationInfo.packageName, ri.activityInfo.name);
		    	              intent.setAction(Intent.ACTION_MAIN);
		    	              intent.addCategory(Intent.CATEGORY_HOME);
		    	              startActivity(intent);
		    	          }
		    	          System.exit(0);
		    	          break;	
		    	        default:
            	break;
		}
			
		
		
		
	}    
 
    /**Current implementation of the LongPress Context Menu. It is connected to the surface view does nothing (long press is handled in drawing panel)**/
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	Log.w(this.getClass().getName(), "onCreateContextMenu()");
   	
    	
    }

    @Override
    public void onPause(){
    	super.onPause();
    	Log.d("CanvasTester", "onPause");
    	//Stops thread if onpause is called
    	mDrawingThread.setRunning(false);
    }

    @Override
	public void onStart(){
    	super.onStart();
    	Log.d("CanvasTester", "onStart");
		
    	
    }
    
    @Override
	public void onResume(){
    	super.onResume();
    	Log.d("CanvasTester", "onResume");
    	//Resumes Drawing Thread
    	//mDrawingThread.onResume();
    		
    }
    
    @Override
	public void onRestart(){
    	super.onRestart();
    	Log.d("CanvasTester", "onRestart");
		
    	
    }

    @Override
 	public void onStop(){
     	super.onStop();
     	Log.d("CanvasTester", "onStop");
     }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
    	super.onSaveInstanceState(savedInstanceState);
    	Log.d("CanvasTester", "onSaveInstanceState");
    }
    
       @Override
        public void onBackPressed(){
    	   //Comment this out to disable back button
    	   super.onBackPressed();
          Log.d("CanvasTester", "OnBackPressed");
        }
    
    //Callback functions to make the ribbon menus accessible from drawing panel
/*	@Override
	public void ToggleRibbonMenu(final int direction) {
		Log.d("ToggleRibbonMenu", "Toggle " + direction);
		//if (direction == RIGHT_ANIM){
			Log.d("ToggleRibbonMenu", "Toggle Right anim");
			runOnUiThread(new Runnable() {
				public void run() {
	            rbmView.toggleMenu(direction);
	        }
	    });
//		} else if (direction == LEFT_ANIM) {
//			Log.d("ToggleRibbonMenu", "Toggle left anim");
//			runOnUiThread(new Runnable() {
//				public void run() {
//	            rbmView.toggleMenu(LEFT_ANIM);
//	        }
//	    });
//		}
	}    */
	
       @Override
       public void ToggleRibbonMenu(final int direction, final int channel) {
    	   runOnUiThread(new Runnable() {
    		   public void run() {
    			   if (!rbmView.isMenuVisible()){
    				   switch (channel){
    				   case CHANNEL_1:
    					   rbmView.setMenuItems(R.menu.ribbon_menu_right_ch1, direction);
    					   break;
    				   case CHANNEL_2:
    					   rbmView.setMenuItems(R.menu.ribbon_menu_right_ch2, direction);	
    					   break;
    				   case MATH:
    					   rbmView.setMenuItems(R.menu.ribbon_menu_right_math, direction);	
    					   break;
    				   case TRIGGER:
    					   rbmView.setMenuItems(R.menu.ribbon_menu_right_trigger, direction);	
    					   break;
    				   case MAIN:
    					   rbmView.setMenuItems(R.menu.ribbon_menu, LEFT_ANIM);  
    				   }
    			   }
    			   rbmView.toggleMenu(direction);
    			   Log.d("ToggleRibbonMenu2", "Toggle right anim run done");
    		   }
    	   });
       }
		
	public boolean onLongClick(SurfaceView v) {
		// TODO Auto-generated method stub
		Log.d("onLongClick", "Before openContextMenu");
	    openContextMenu(v);
	    return true;
	}   	
	//Callback to toggle the status button when pausing the screen
	@Override
	public void ToggleStatusButton(boolean status) {
		// TODO Auto-generated method stub
		if(status){
			runOnUiThread(new Runnable() {
				public void run() {
					statusButton.setImageResource(R.drawable.pause);
	        }
	    });
		}else{
			runOnUiThread(new Runnable() {
				public void run() {
					statusButton.setImageResource(R.drawable.play);
	        }
	    });
		}
	}
	
//Class that scans a directory then opens all of the pictures in that directory
   public class SingleMediaScanner implements MediaScannerConnectionClient {
		   	Context context;
	        private MediaScannerConnection mMs;
	        private File mFile;

	        public SingleMediaScanner(Context context, File f) {
	            mFile = f;
	            mMs = new MediaScannerConnection(context, this);
	            mMs.connect();
	            this.context = context;
	        }

	        public void onMediaScannerConnected() {
	            mMs.scanFile(mFile.getAbsolutePath(), null);
	        }

	        public void onScanCompleted(String path, Uri uri) {
	            Intent intent = new Intent(Intent.ACTION_VIEW);
	            intent.setData(uri);
	            Log.d("screenCap", "Starting activity");
	            startActivityForResult(intent, 32);
	            mMs.disconnect();
	        }


	    }


}