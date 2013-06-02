package com.example.canvastester;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class gridd extends Activity implements OnGestureListener
{       private LinearLayout main;       private TextView viewA;

   private static final int SWIPE_MIN_DISTANCE = 120;
   private static final int SWIPE_MAX_OFF_PATH = 250;
   private static final int SWIPE_THRESHOLD_VELOCITY = 200;


   private GestureDetector gestureScanner;
       @Override
   public void onCreate(Bundle savedInstanceState)
   {
       super.onCreate(savedInstanceState);
             gestureScanner = new GestureDetector(this);
             main = new LinearLayout(this);
       main.setBackgroundColor(Color.GRAY);
       main.setLayoutParams(new LinearLayout.LayoutParams(320,480));
             viewA = new TextView(this);
       viewA.setBackgroundColor(Color.YELLOW);
       viewA.setTextColor(Color.BLACK);
       viewA.setTextSize(16);
       viewA.setLayoutParams(new LinearLayout.LayoutParams(320,80));
       main.addView(viewA);
             setContentView(main);
   }
     @Override
   public boolean onTouchEvent(MotionEvent me)
   {
    return gestureScanner.onTouchEvent(me);
   }
     @Override
   public boolean onDown(MotionEvent e)
   {
    viewA.setText("-" + "DOWN" + "-");
    return true;
   }
     @Override
   public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
   {
       try {
           if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
               return false;
           // right to left swipe
           if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
               Toast.makeText(getApplicationContext(), "Left Swipe", Toast.LENGTH_SHORT).show();
           }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
               Toast.makeText(getApplicationContext(), "Right Swipe", Toast.LENGTH_SHORT).show();
           }
           else if(e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
               Toast.makeText(getApplicationContext(), "Swipe up", Toast.LENGTH_SHORT).show();
           }  else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
               Toast.makeText(getApplicationContext(), "Swipe down", Toast.LENGTH_SHORT).show();
           }
       } catch (Exception e) {
           // nothing
       }

               return true;
   }
     @Override
   public void onLongPress(MotionEvent e)
   {
       Toast mToast =  Toast.makeText(getApplicationContext(), "Long Press", Toast.LENGTH_SHORT);
       mToast.show();
       }
     @Override
   public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
   {
    viewA.setText("-" + "SCROLL" + "-");
    return true;
   }
     @Override
   public void onShowPress(MotionEvent e)
   {
    viewA.setText("-" + "SHOW PRESS" + "-");
   }         @Override
   public boolean onSingleTapUp(MotionEvent e)       {
       Toast mToast =  Toast.makeText(getApplicationContext(), "Single Tap", Toast.LENGTH_SHORT);
       mToast.show();
    return true;
   }
}