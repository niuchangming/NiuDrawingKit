package com.cmniu.drawingview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;

public class NiuDrawingView extends ImageView implements OnTouchListener{
	private final String TAG = "NiuDrawingView";
	private final int DEFAULT_WIDTH = 4;
	private Paint paint;
	private List<Shape> shapes = new ArrayList<Shape>();
	private List<Shape> undoShapes;
	
	public NiuDrawingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public NiuDrawingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public NiuDrawingView(Context context) {
		super(context);
		init();
	}

	private void init(){
		shapes = new ArrayList<Shape>();
		paintConfig();
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		setFocusable(true);
		setFocusableInTouchMode(true);
		setBackgroundColor(Color.WHITE);
		this.setOnTouchListener(this);
		Shape shape = new Pen(paint);
		shapes.add(shape);
	}

	public void paintConfig() {
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setColor(Color.parseColor("#000000")); 
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(DEFAULT_WIDTH);
	}
	
	public void setPaintColor(int color){
		resetEffect();
		paint.setColor(color);
	}
	
	public void setPaintWidth(float width){
		paint.setStrokeWidth(width);
	}
	
	public void resetEffect(){
		paint.setMaskFilter(null);
		paint.setShader(null);
	}
	
	public void setShape(ShapeCode code) {
		Shape shape = null;
		switch(code){
		case PEN:
			shape = new Pen(paint);
			paint.setStyle(Style.STROKE);
			break;
		case LINE:
			shape = new Line(paint);
			paint.setStyle(Style.STROKE);
			break;
		case CIRCLE:
			shape = new Circle(paint);
			paint.setStyle(Style.STROKE);
			break;
		case CIRCLE_FILL:
			shape = new Circle(paint);
			paint.setStyle(Style.FILL);
			break;
		case RECT:
			shape = new Rectangle(paint);
			paint.setStyle(Style.STROKE);
			break;
		case RECT_FILL:
			shape = new Rectangle(paint);
			paint.setStyle(Style.FILL);
			break;
		case TEXT:
			shape = new Text(paint);
			break;
		case INK:
			shape = new ChineseInk(paint, this);
			break;
		default:
			shape = new Pen(paint);
			break;
		}
		shapes.add(shape);
	}
	
	@Override
	public boolean onTouch(View arg0, MotionEvent event) {
		if(shapes.size() != 0){
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				shapes.get(shapes.size()-1).touchStart(event);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				shapes.get(shapes.size()-1).touchMove(event);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				shapes.get(shapes.size()-1).touchEnd(event);
				invalidate();
				break;
			}
		}
		return true;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		for(Shape shape : shapes){
			shape.draw(canvas);
		};
	}
	
	public enum ShapeCode {
        PEN(0), LINE(1), RECT(2), RECT_FILL(3), 
        CIRCLE(4), CIRCLE_FILL(5), INK(6), TEXT(7), NATURAL_PATH(8);
        int value;
        private ShapeCode(int value) {
            this.value = value;
        }
	}

	public void setMaskFilter(MaskFilter filter){
		this.paint.setMaskFilter(filter);
		shapes.get(shapes.size() -1).setPaint(paint);
	}
	
	public void setShader(Shader shader){
		this.paint.setShader(shader);
		shapes.get(shapes.size() -1).setPaint(paint);
	}
	
	public void setTypeface(Typeface typeface){
		if(shapes.get(shapes.size() - 1) instanceof Text){
			((Text)shapes.get(shapes.size() - 1)).setCurrentTypeface(typeface);
		}
	}
	
	public void setSize(int size){
		if(shapes.get(shapes.size() - 1) instanceof Text){
			((Text)shapes.get(shapes.size() - 1)).setCurrentSize(size);
		}
	}
	
	public void setText(String text){
		if(shapes.get(shapes.size() - 1) instanceof Text){
			((Text)shapes.get(shapes.size() - 1)).setCurrentText(text);
		}
	}
	
	public void invalidateTextView(float x, float y){
		if(shapes.get(shapes.size() - 1) instanceof Text){
			((Text)shapes.get(shapes.size() - 1)).setInitPosituon(x, y);
			invalidate();
		}
	}
	
	public void redo(){
		if(shapes.size() > 0){
			if(shapes.get(shapes.size() - 1).getPaths().size() == 0){
				if(undoShapes == null)
					undoShapes = new ArrayList<Shape>();
				undoShapes.add(shapes.get(shapes.size() - 1));
				shapes.remove(shapes.size() - 1);
			}
			
			if(shapes.size() > 0){
				shapes.get(shapes.size() - 1).redo();
			}
			invalidate();
		}
	}
	
	public void undo(){
		if(shapes.size() > 0 && shapes.get(shapes.size() - 1).getUndoPaths() != null && shapes.get(shapes.size() - 1).getUndoPaths().size() > 0){
			shapes.get(shapes.size() - 1).undo();
		}else{
			if(undoShapes != null && undoShapes.size() > 0){
				shapes.add(undoShapes.get(undoShapes.size() - 1));
				undoShapes.remove(undoShapes.size() - 1);
				shapes.get(shapes.size() - 1).undo();
			}
		}
		invalidate();
	}
	
	public void clear(){
		Shape retainShape = shapes.get(shapes.size() - 1);
		shapes.clear();
		shapes.add(retainShape);
		retainShape = null;
		if(shapes.size() == 1){
			shapes.get(0).clear();	
		}
		if(undoShapes != null){
			undoShapes.clear();
		}
		invalidate();
		System.gc();
	}

	public String save() {
		String mPath = Environment.getExternalStorageDirectory().toString();   

		Bitmap bitmap;

		OutputStream fout = null;
		File imageFile = new File(mPath, "image_" + System.currentTimeMillis() + ".png");

		try {
			this.setDrawingCacheEnabled(true);
			bitmap = Bitmap.createBitmap(this.getDrawingCache());
			this.setDrawingCacheEnabled(false);
			
		    fout = new FileOutputStream(imageFile);
		    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
		    fout.flush();
		    fout.close();

		} catch (FileNotFoundException e) {
		    Log.v(TAG, e.toString());
		} catch (IOException e) {
		    Log.v(TAG, e.toString());
		}
		
		return imageFile.getAbsolutePath();
	}
	
}
