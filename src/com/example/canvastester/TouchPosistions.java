package com.example.canvastester;

import android.graphics.PointF;

public class TouchPosistions {
	private PointF prevpoint1;
	private PointF prevpoint2;
	private PointF point1;
	private PointF point2;
	
	public void clearPoint1(){
		point1.set(-1, -1);
	}
	
	public void clearPoint2(){
		point2.set(-1, -1);
	}
	
	public void setPoint1(float x, float y){
		prevpoint1 = point1;
		point1.set(x, y);
	}
	
	public void setPoint2(float x, float y){
		prevpoint2 = point2;
		point2.set(x, y);
	}
	
	public int touchState(){
		if (point1.equals(-1, -1)){ // No touch events
			if (prevpoint1.equals(-1, -1)){ // 
				return 1;
			}
		} else if (point2.equals(-1, -1)){ // Only one finger down
			if (prevpoint2.equals(-1, -1)){ // Only been one finger down
				
			} else { // Finger 2 just released
				
				return 2;
			}
		} else { // Two fingers down, zooming?
			
		}
	return 3;
	}
	

}
