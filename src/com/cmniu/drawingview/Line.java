package com.cmniu.drawingview;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Pair;
import android.view.MotionEvent;

public class Line extends Shape{
	private static final float TOUCH_TOLERANCE = 4;
	private ArrayList<Pair<LinePath, Paint>> paths;
	private ArrayList<Pair<LinePath, Paint>> undoPaths;
	private Path path;
	private float startX;
	private float startY;
	boolean drawingcompleted;
	
	public Line(Paint paint) {
		super(paint);
		paths = new ArrayList<Pair<LinePath, Paint>>();
		path = new Path();
	}

	@Override
	void draw(Canvas canvas) {
		if(!drawingcompleted){
			canvas.drawLine(startX, startY, lastX, lastY, paint);
			for(Pair<LinePath, Paint> path : paths){
				canvas.drawLine(path.first.startX, path.first.startY, path.first.endX, path.first.endY, path.second);
			}
		}else{
			for(Pair<LinePath, Paint> path : paths){
				canvas.drawLine(path.first.startX, path.first.startY, path.first.endX, path.first.endY, path.second);
			}
		}
	}

	@Override
	void touchStart(MotionEvent event) {
		drawingcompleted = false;
		path.reset();
		path.moveTo(event.getX(), event.getY());
		
		startX = event.getX();
		startY = event.getY();
        lastX = event.getX();
        lastY = event.getY();
	}

	@Override
	void touchMove(MotionEvent event) {
		float dx = Math.abs(event.getX() - lastX);
        float dy = Math.abs(event.getY() - lastY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
        	path.lineTo(lastX, lastY);
            lastX = event.getX();
            lastY = event.getY();
        }
	}

	@Override
	void touchEnd(MotionEvent event) {
		LinePath path = new LinePath();
		path.startX = this.startX;
		path.startY = this.startY;
		path.endX = event.getX();
		path.endY = event.getY();
		
		Paint newPaint = new Paint(paint); 
		paths.add(new Pair<LinePath, Paint>(path, newPaint));
		
		lastX = event.getX();
        lastY = event.getY();
        drawingcompleted = true;
	}
	
	class LinePath{
		private float startX;
		private float startY;
		private float endX;
		private float endY;
	}

	@Override
	void redo() {
		if(paths.size() > 0){
			if(undoPaths == null)
				undoPaths = new ArrayList<Pair<LinePath,Paint>>();
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
