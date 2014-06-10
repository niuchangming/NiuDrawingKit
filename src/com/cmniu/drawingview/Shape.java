package com.cmniu.drawingview;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

public abstract class Shape {
	protected Paint paint;
	protected float lastX;
	protected float lastY;
	
	public Shape(Paint paint) {
		this.paint = paint;
	}
	
	public Paint getPaint() {
		return paint;
	}

	public void setPaint(Paint paint) {
		this.paint = paint;
	}

	abstract void draw(Canvas canvas);
	abstract void touchStart(MotionEvent event);
	abstract void touchMove(MotionEvent event);
	abstract void touchEnd(MotionEvent event);
	abstract List<?> getPaths();
	abstract List<?> getUndoPaths();
	abstract void redo();
	abstract void undo();
	abstract void clear();
}
