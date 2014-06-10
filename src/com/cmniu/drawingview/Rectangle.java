package com.cmniu.drawingview;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Pair;
import android.view.MotionEvent;

public class Rectangle extends Shape{
	private ArrayList<Pair<RectPath, Paint>> paths;
	private ArrayList<Pair<RectPath, Paint>> undoPaths;
	private RectPath path;
	boolean drawingcompleted;
	
	public Rectangle(Paint paint) {
		super(paint);
		paths = new ArrayList<Pair<RectPath, Paint>>();
	}

	@Override
	void draw(Canvas canvas) {
		if(!drawingcompleted){
			canvas.drawRect(paths.get(paths.size() - 1).first.left, paths.get(paths.size() - 1).first.top, lastX, lastY, paths.get(paths.size() - 1).second);
			for(Pair<RectPath, Paint> path : paths.subList(0, paths.size() - 1)){
				canvas.drawRect(path.first.left, path.first.top, path.first.right, path.first.bottom, path.second);
			}
		}else{
			for(Pair<RectPath, Paint> path : paths){
				canvas.drawRect(path.first.left, path.first.top, path.first.right, path.first.bottom, path.second);
			}
		}
	}

	@Override
	void touchStart(MotionEvent event) {
		drawingcompleted = false;
		path = new RectPath();
		path.left = event.getX();
		path.top = event.getY();
		path.right = event.getX();
		path.bottom = event.getY();
		lastX = event.getX();
		lastY = event.getY();
		
		Paint newPaint = new Paint(paint);
		paths.add(new Pair<RectPath, Paint>(path, newPaint));
	}
	
	@Override
	void touchMove(MotionEvent event) {
		lastX = event.getX();
		lastY = event.getY();
	}

	@Override
	void touchEnd(MotionEvent event) {
		drawingcompleted = true;
		lastX = event.getX();
		lastY = event.getY();
		path.right = lastX;
		path.bottom = lastY;
	}
	
	class RectPath{
		float left;
		float top;
		float right;
		float bottom;
		boolean isRight;
		boolean isDown;
	}

	@Override
	void redo() {
		if(paths.size() > 0){
			if(undoPaths == null)
				undoPaths = new ArrayList<Pair<RectPath,Paint>>();
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
