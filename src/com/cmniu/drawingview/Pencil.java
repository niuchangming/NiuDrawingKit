package com.cmniu.drawingview;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

public class Pencil extends Shape{
	List<Point> paths = new ArrayList<Point>();
    Paint paint = new Paint();
	
	public Pencil(Paint paint) {
		super(paint);
	}

	@Override
	void draw(Canvas canvas) {
		for (Point point : paths) {
            canvas.drawCircle(point.x, point.y, 10, paint);
        }
	}

	@Override
	void touchStart(MotionEvent event) {
		Point point = new Point();
        point.x = event.getX();
        point.y = event.getY();
        paths.add(point);
	}

	@Override
	void touchMove(MotionEvent event) {
		Point point = new Point();
        point.x = event.getX();
        point.y = event.getY();
        paths.add(point);
	}
	
	@Override
	void touchEnd(MotionEvent event) {
		Point point = new Point();
        point.x = event.getX();
        point.y = event.getY();
        paths.add(point);
	}
	
	class Point {
	    float x, y;

	    @Override
	    public String toString() {
	        return x + ", " + y;
	    }
	}

	@Override
	void redo() {
		if(paths.size() > 0){
			paths.remove(paths.size() - 1);
		}
	}

	@Override
	void undo() {
		
	}

	@Override
	void clear() {
		paths.clear();
	}
	
	@Override
	List<?> getPaths() {
		return paths;
	}

	@Override
	List<?> getUndoPaths() {
		// TODO Auto-generated method stub
		return null;
	}

}
