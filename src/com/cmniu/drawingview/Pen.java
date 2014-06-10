package com.cmniu.drawingview;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Pair;
import android.view.MotionEvent;

public class Pen extends Shape{
	private static final float TOUCH_TOLERANCE = 4;
	private ArrayList<Pair<Path, Paint>> paths;
	private ArrayList<Pair<Path, Paint>> undoPaths;
	private Path path;
	boolean drawingcompleted;
	
	public Pen(Paint paint) {
		super(paint);
		paths = new ArrayList<Pair<Path, Paint>>();
		path = new Path();
	}

	@Override
	void draw(Canvas canvas) {
		if(!drawingcompleted){
			canvas.drawPath(path, paint);
			if(paths.size() > 0){
				for (Pair<Path, Paint> p : paths.subList(0, paths.size() - 1)) {
					canvas.drawPath(p.first, p.second);
				}
			}
		}else{
			for (Pair<Path, Paint> p : paths) {
				canvas.drawPath(p.first, p.second);
			}
		}
	}

	@Override
	void touchStart(MotionEvent event) {
		drawingcompleted = false;
		Paint newPaint = new Paint(paint);
		path = new Path();
		paths.add(new Pair<Path, Paint>(path, newPaint));
		path.moveTo(event.getX(), event.getY());
		lastX = event.getX();
		lastY = event.getY();
	}

	@Override
	void touchMove(MotionEvent event) {
		float dx = Math.abs(event.getX() - lastX);
		float dy = Math.abs(event.getY() - lastY);
		if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
			path.quadTo(lastX, lastY, (event.getX() + lastX) / 2, (event.getY() + lastY) / 2);
			lastX = event.getX();
			lastY = event.getY();
		}
	}

	@Override
	void touchEnd(MotionEvent event) {
		path.lineTo(lastX, lastY);
		drawingcompleted = true;
	}

	@Override
	void redo() {
		if(paths.size() > 0){
			if(undoPaths == null)
				undoPaths = new ArrayList<Pair<Path,Paint>>();
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

//p.second.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));
//newPaint.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.OUTER));
//newPaint.setShader(new SweepGradient(5, 5, Color.rgb(33, 133, 299), Color.BLUE));
