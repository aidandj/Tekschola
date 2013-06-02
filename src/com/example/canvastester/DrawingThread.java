package com.example.canvastester;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;


public class DrawingThread extends Thread {
	private Object mPauseLock = new Object();  
	private boolean mPaused;
	
	private SurfaceHolder _surfaceHolder;
    private DrawingPanel _panel;
    private boolean _run = false;
    public boolean isPaused = false;

    public DrawingThread(SurfaceHolder surfaceHolder, DrawingPanel panel) {
        _surfaceHolder = surfaceHolder;
        _panel = panel;
    }
    
    public void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }
    
    public void onResume() {
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
            
        }
    }

    public void setRunning(boolean run) {
        _run = run;
    }

    public SurfaceHolder getSurfaceHolder() {
        return _surfaceHolder;
    }

    @SuppressLint("WrongCall")
	@Override
    public void run() {
        Canvas c;
        while (_run) {
            c = null;
            try {
                c = _surfaceHolder.lockCanvas(null);
                synchronized (_surfaceHolder) {
                    _panel.onDraw(c);
                    //c.drawBitmap(toDisk, new Matrix(), new Paint());
            		//c.setBitmap(toDisk);
                }
                //dirtyness that threads have become for me. I'll attempt to break it down:
                //Main activity gets a button press and sets _panel.screenShot to true.
                //Because _panel.screenShot is true, it stops the drawing by calling screenCap
                //When screencap finishes it opens gallary and unlocks the drawing thread.
                synchronized(_surfaceHolder) {
                	if (_panel.screenShot){
                	_panel.screenCap();
                	}
                }
                synchronized (mPauseLock) {
                    while (mPaused) {
                        try {
                        	Log.d("ADJ", "before wait");
                            mPauseLock.wait();
                            Log.d("ADJ", "after wait");
                        } catch (InterruptedException e) {
                        }
                    }
                }	
                
            }
            catch (Exception e) {
			// we will try it again and again...
            }
            finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    _surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }
}
