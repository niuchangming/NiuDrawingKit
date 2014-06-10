package com.cmniu.drawingview;

import java.util.ArrayList;
import java.util.List;

import com.cmniu.pressure.PressureCooker;
import com.cmniu.pressure.Spot;
import com.cmniu.pressure.SpotFilter;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

public class ChineseInk extends Shape{
	private View parentView;
	private List<ArrayList<Spot>> paths;
	private List<ArrayList<Spot>> undoPaths;
	private List<Paint> paints;
	
    private final int SMOOTHING_FILTER_WLEN = 6;
    private final float SMOOTHING_FILTER_POS_DECAY = 0.65f;
    private final float SMOOTHING_FILTER_PRESSURE_DECAY = 0.9f;
    
    public final boolean ASSUME_STYLUS_CALIBRATED = true;
    
    private float mPressureExponent = 2.0f;

    private float mRadiusMin = 0.5912501f;
    private float mRadiusMax = 6.575f;

    private Bitmap mCurrentBitmap;
    private Canvas mCurrentCanvas;
    
    private PressureCooker mPressureCooker;
    
	private Spot mTmpSpot = new Spot();
	private MarkersPlotter strokePlotter;
	
    private boolean isTouchEvent;
    
	public ChineseInk(Paint paint) {
		super(paint);
		init();
	}
	
	public ChineseInk(Paint paint, View parent) {
		super(paint);
		this.parentView = parent;
		init();
	}
	
	private void init(){
		isTouchEvent = true;
		strokePlotter = new MarkersPlotter();
		mPressureCooker = new PressureCooker(parentView.getContext());
		paths = new ArrayList<ArrayList<Spot>>();
		undoPaths = new ArrayList<ArrayList<Spot>>();
		paints = new ArrayList<Paint>();
		viewConfig();
	}
	
	private void viewConfig(){
		if (mCurrentBitmap != null) return;
	       mCurrentBitmap = Bitmap.createBitmap(parentView.getMeasuredWidth(), parentView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
	       mCurrentCanvas = new Canvas();
	       mCurrentCanvas.setBitmap(mCurrentBitmap);
	}

	public void setPenSize(float min, float max) {
        mRadiusMin = min * 0.5f;
        mRadiusMax = max * 0.5f;
    }
	
	@Override
	void draw(Canvas canvas) {		
		if (mCurrentBitmap != null) {
            canvas.drawBitmap(mCurrentBitmap, 0, 0, null);
        }
	}

	@Override
	void touchStart(MotionEvent event) {
		isTouchEvent = true;
		paths.add(new ArrayList<Spot>());
		paints.add(new Paint(super.paint));
		mTmpSpot.update(
				event.getX(), event.getY(),
    			event.getSize(),
    			event.getPressure() + event.getSize(),
    			event.getEventTime(),
    			getToolTypeCompat(event, 0));
    	strokePlotter.add(mTmpSpot);
	}

	@Override
	void touchMove(MotionEvent event) {
		isTouchEvent = true;
        for (int i = 0; i < event.getHistorySize(); i++) {
        	mTmpSpot.update(
        			event.getHistoricalX(i),
        			event.getHistoricalY(i),
        			event.getHistoricalSize(i),
        			event.getHistoricalPressure(i) + event.getHistoricalSize(i),
        			event.getHistoricalEventTime(i),
                    getToolTypeCompat(event, 0)
        			);
        	strokePlotter.add(mTmpSpot);
        }
    	mTmpSpot.update(
    			event.getX(),
    			event.getY(),
    			event.getSize(),
    			event.getPressure() + event.getSize(),
    			event.getEventTime(),
                getToolTypeCompat(event, 0)
   			);
    	strokePlotter.add(mTmpSpot);
	}

	@Override
	void touchEnd(MotionEvent event) {
		isTouchEvent = true;
		mTmpSpot.update(
				event.getX(), event.getY(),
    			event.getSize(),
    			event.getPressure() + event.getSize(),
    			event.getEventTime(),
    			getToolTypeCompat(event, 0));
    	strokePlotter.add(mTmpSpot);
		strokePlotter.finish(event.getEventTime());
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
		isTouchEvent = false;
		 resetCurrentCanvas();
		if(paths.size() > 0){
			undoPaths.add(paths.get(paths.size() - 1));
			paths.remove(paths.size() - 1);
		}
		reDraw();
	}

	@Override
	void undo() {
		isTouchEvent = false;
		resetCurrentCanvas();
		if(undoPaths.size() > 0){
			paths.add(undoPaths.get(undoPaths.size() - 1));
			undoPaths.remove(undoPaths.size() - 1);
		}
		reDraw();
	}
	
	@Override
	void clear() {
		paths.clear();
		if(undoPaths != null){
			undoPaths.clear();
		}
		paints.clear();
		resetCurrentCanvas();
	}
	
	private void resetCurrentCanvas(){
		mCurrentBitmap.recycle();
		mCurrentBitmap = null;
		mCurrentBitmap = Bitmap.createBitmap(parentView.getMeasuredWidth(), parentView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
		mCurrentCanvas.setBitmap(mCurrentBitmap);
	}
	
	private void reDraw(){
		for(int i = 0; i < paths.size(); i++){
			strokePlotter.setPaint(paints.get(i));
			for(Spot spot : paths.get(i)){
				strokePlotter.plot(spot);
			}
			strokePlotter.finish(paths.get(i).get(paths.get(i).size() - 1).time);
		}
	}
	
	final static boolean hasToolType() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);
    }
	
	final static int getToolTypeCompat(MotionEvent me, int index) {
        if (hasToolType()) {
            return me.getToolType(index);
        }
        if ("flyer".equals(Build.HARDWARE)) {
            if (me.getSize(index) <= 0.1f) {
                return MotionEvent.TOOL_TYPE_STYLUS;
            }
        }
        return MotionEvent.TOOL_TYPE_FINGER;
    }
	
	private class MarkersPlotter implements SpotFilter.Plotter {
        private SpotFilter mCoordBuffer;
        private SmoothStroker mRenderer;
        
        public MarkersPlotter() {
            mCoordBuffer = new SpotFilter(SMOOTHING_FILTER_WLEN, SMOOTHING_FILTER_POS_DECAY, SMOOTHING_FILTER_PRESSURE_DECAY, this);
            mRenderer = new SmoothStroker();
        }

        @Override
        public void plot(Spot s) {
            final float pressureNorm;
        
            if (ASSUME_STYLUS_CALIBRATED && s.tool == MotionEvent.TOOL_TYPE_STYLUS) {
                pressureNorm = s.pressure;
            } else {
                pressureNorm = mPressureCooker.getAdjustedPressure(s.pressure);
            }

            final float radius = lerp(mRadiusMin, mRadiusMax, (float) Math.pow(pressureNorm, mPressureExponent));
            
            mRenderer.strokeTo(s.x, s.y, radius);
            
            if(isTouchEvent){
            	 paths.get(paths.size() - 1).add(new Spot(s));
            }
        }
        
        public void finish(long time) {
            mCoordBuffer.finish();
            mRenderer.reset();
        }

        public void add(Spot s) {
            mCoordBuffer.add(s);
        }
        
        public void setPaint(Paint paint) {
            mRenderer.setPaint(paint);
        }
    }
	
	private class SmoothStroker {
        private float mLastX = 0, mLastY = 0, mLastLen = 0, mLastR = -1;
        private float mTan[] = new float[2];
        private Paint paint = ChineseInk.this.paint;
        
        public void setPaint(Paint paint){
        	this.paint = paint;
        }
        
        public void reset() {
            mLastX = mLastY = mTan[0] = mTan[1] = 0;
            mLastR = -1;
        }
        
        //好像是计算距离的
        final float dist (float x1, float y1, float x2, float y2) {
            x2-=x1;
            y2-=y1;
            return (float) Math.sqrt(x2*x2 + y2*y2);
        }
        
        final void drawStrokePoint(float x, float y, float r) {
        	mCurrentCanvas.drawCircle(x, y, r, paint);
        }
        
        public void strokeTo(float x, float y, float r) {
            if (mLastR < 0) {
                drawStrokePoint(x,y,r);
            } else { 
                mLastLen = dist(mLastX, mLastY, x, y);
                float xi, yi, ri, frac;
                float d = 0;
                while (true) {
                    if (d > mLastLen) {
                        break;
                    }
                    frac = d == 0 ? 0 : (d / mLastLen);
                    ri = lerp(mLastR, r, frac);
                    xi = lerp(mLastX, x, frac);
                    yi = lerp(mLastY, y, frac);
                    drawStrokePoint(xi,yi,ri);

                    final float MIN = 1f;
                    final float THRESH = 16f;
                    final float SLOPE = 0.1f; // asymptote: the spacing will increase as SLOPE*x
                    if (ri <= THRESH) {
                        d += MIN;
                    } else {
                        d += Math.sqrt(SLOPE * Math.pow(ri - THRESH, 2) + MIN);
                    }
                }
            }
            mLastX = x;
            mLastY = y;
            mLastR = r;
        }
    }
	
	public static float lerp(float a, float b, float f) {
	    return a + f * (b - a);
	}

}
