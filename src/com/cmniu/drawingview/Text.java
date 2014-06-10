package com.cmniu.drawingview;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;

public class Text extends Shape {
	private final String TAG = "Text"; 
	private TextObj textObj;
	private ArrayList<Pair<TextObj, Paint>> paths;
	private ArrayList<Pair<TextObj, Paint>> undoPaths;
	
	public Text(Paint paint) {
		super(paint);
		init();
	}
	
	private void init(){
		Paint mPaint = new Paint(paint);
		mPaint.setTextSize(16);
		mPaint.setStyle(Style.FILL);
//		mPaint.setShadowLayer(0.5f, 0.5f, 0.5f, Color.WHITE);
		textObj = new TextObj();
		textObj.text = "";
		paths = new ArrayList<Pair<TextObj, Paint>>();
		paths.add(new Pair<TextObj, Paint>(textObj, mPaint));
	}
	
	public void setInitPosituon(float x, float y){
		if(paths.size() > 0){
			paths.get(paths.size() - 1).first.x = x;
			paths.get(paths.size() - 1).first.y = y;
		}
	}
	
	public void setCurrentTypeface(Typeface typeface){
		if(paths.size() > 0){
			paths.get(paths.size() - 1).second.setTypeface(typeface);
		}
	}
	
	public void setCurrentSize(int size){
		if(paths.size() > 0){
			paths.get(paths.size() - 1).second.setTextSize(size);
		}
	}
	
	public void setCurrentText(String text){
		if(paths.size() > 0){
			paths.get(paths.size() - 1).first.text = text;
		}
	}
	
	@Override
	void draw(Canvas canvas) {
		if(paths.size() > 0){
			canvas.drawText(paths.get(0).first.text, paths.get(0).first.x, paths.get(0).first.y, paths.get(0).second);
		}
	}

	@Override
	void touchStart(MotionEvent event) {
		lastX = event.getX();
		lastY = event.getY();
		paths.get(0).first.x = lastX;
		paths.get(0).first.y = lastY;
	}

	@Override
	void touchMove(MotionEvent event) {
		lastX = event.getX();
		lastY = event.getY();
		paths.get(0).first.x = lastX;
		paths.get(0).first.y = lastY;
	}

	@Override
	void touchEnd(MotionEvent event) {
		lastX = event.getX();
		lastY = event.getY();
		paths.get(0).first.x = lastX;
		paths.get(0).first.y = lastY;
	}

	@Override
	List<?> getPaths() {
		return paths;
	}

	@Override
	List<?> getUndoPaths() {
		return undoPaths;
	}

	@Override
	void redo() {
		if(paths.size() > 0){
			if(undoPaths == null)
				undoPaths = new ArrayList<Pair<TextObj, Paint>>();
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
	
	class TextObj{
		private String text;
		private float x;
		private float y;
	}

}
