package com.cmniu.drawingview;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Pair;
import android.view.MotionEvent;

public class Circle extends Shape{
	private ArrayList<Pair<CirclePath, Paint>> paths;
	private ArrayList<Pair<CirclePath, Paint>> undoPaths;
	private CirclePath path;
	private float radius;
	boolean drawingcompleted;
	
	public Circle(Paint paint) {
		super(paint);
		paths = new ArrayList<Pair<CirclePath, Paint>>();
		path = new CirclePath();
	}

	@Override
	void draw(Canvas canvas) {
		if(!drawingcompleted){
			canvas.drawCircle(path.centerX, path.centerY, radius, paint);
			for(Pair<CirclePath, Paint> path : paths){
				canvas.drawCircle(path.first.centerX, path.first.centerY, path.first.radius, path.second);
			}
		}else{
			for(Pair<CirclePath, Paint> path : paths){
				canvas.drawCircle(path.first.centerX, path.first.centerY, path.first.radius, path.second);
			}
		}
	}

	@Override
	void touchStart(MotionEvent event) {
		drawingcompleted = false;
		path = new CirclePath();
		path.centerX = event.getX();
		path.centerY = event.getY();
		lastX = event.getX();
		lastY = event.getY();
		radius = 0;
	}

	@Override
	void touchMove(MotionEvent event) {
		lastX = event.getX();
		lastY = event.getY();
		radius = (float) Math.sqrt((float) (Math.pow(event.getX() - path.centerX, 2) + Math.pow(event.getY() - path.centerY, 2)));
	}

	@Override
	void touchEnd(MotionEvent event) {
		drawingcompleted = true;
		lastX = event.getX();
		lastY = event.getY();
		radius = (float) Math.sqrt((float) (Math.pow(event.getX() - path.centerX, 2) + Math.pow(event.getY() - path.centerY, 2)));
		path.radius = radius;
		Paint newPaint = new Paint(paint);
		paths.add(new Pair<CirclePath, Paint>(path, newPaint));
	}
	
	class CirclePath{
		public float centerX;
		public float centerY;
		public float radius;
	}

	@Override
	void redo() {
		if(paths.size() > 0){
			if(undoPaths == null)
				undoPaths = new ArrayList<Pair<CirclePath,Paint>>();
			
			undoPaths.add(paths.get(paths.size() - 1));
			paths.remove(paths.size() - 1);
		}
	}

	@Override
	void undo() {
		if(undoPaths != null && undoPaths.size() > 0){
			paths.add(undoPaths.get(undoPaths.size() - 1));
			undoPaths.remove(undoPaths.size() - 1);
		}
	}
	
	@Override
	void clear() {
		paths.clear();
		if(undoPaths != null){
			undoPaths.clear();
		}
	}

	@Override
	List<?> getPaths() {
		return paths;
	}

	@Override
	List<?> getUndoPaths() {
		return undoPaths;
	}

}
