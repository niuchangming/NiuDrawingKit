package com.cmniu.pressure;

import com.cmniu.niudrawingkit.R;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class Slate extends View {
    static final String TAG = "Slate";
    
    public static final boolean HWLAYER = true;
    public static final boolean SWLAYER = false;
    public static final boolean FANCY_INVALIDATES = false;

    public static final int MAX_POINTERS = 10;

    private static final int SMOOTHING_FILTER_WLEN = 6;
    private static final float SMOOTHING_FILTER_POS_DECAY = 0.65f;
    private static final float SMOOTHING_FILTER_PRESSURE_DECAY = 0.9f;

    private static final float INVALIDATE_PADDING = 4.0f;
    public static final boolean ASSUME_STYLUS_CALIBRATED = true;
    
    public static final int TYPE_WHITEBOARD = 0;
    public static final int TYPE_FELTTIP = 1;
    public static final int TYPE_AIRBRUSH = 2;
    public static final int TYPE_FOUNTAIN_PEN = 3;
    
    public static final int SHAPE_CIRCLE = 0;
    public static final int SHAPE_SQUARE = 1;
    public static final int SHAPE_BITMAP_AIRBRUSH = 3;
    public static final int SHAPE_FOUNTAIN_PEN = 4;

    private float mPressureExponent = 2.0f;

    private float mRadiusMin;
    private float mRadiusMax;

    private Bitmap mPreviousBitmap, mCurrentBitmap;
    private Canvas mPreviousCanvas, mCurrentCanvas;
    
    private Bitmap mPendingPaintBitmap;
    
    private Bitmap mAirbrushBits;
    private Rect mAirbrushBitsFrame;
    private Bitmap mFountainPenBits;
    private Rect mFountainPenBitsFrame;
        
    private PressureCooker mPressureCooker;
    
    private boolean mEmpty;
    
    private int mBackgroundColor = Color.TRANSPARENT;
    
    private Region mDirtyRegion = new Region();

    public interface SlateListener {
        void strokeStarted();
        void strokeEnded();
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

            final float radius = lerp(mRadiusMin, mRadiusMax,
                    (float) Math.pow(pressureNorm, mPressureExponent));
            
            final RectF dirtyF = mRenderer.strokeTo(mCurrentCanvas, s.x, s.y, radius);
            dirty(dirtyF);
        }
        
        public void setPenColor(int color) {
            mRenderer.setPenColor(color);
        }
        
        public void finish(long time) {
            mCoordBuffer.finish();
            mRenderer.reset();
        }

        public void add(Spot s) {
            mCoordBuffer.add(s);
        }
        
        public void setPenType(int shape) {
            mRenderer.setPenType(shape);
        }
    }
    
    private class SmoothStroker {
        
        private float mLastX = 0, mLastY = 0, mLastLen = 0, mLastR = -1;
        private float mTan[] = new float[2];

        private int mPenColor;

        private int mShape = SHAPE_CIRCLE; // SHAPE_BITMAP_AIRBRUSH;

        private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        int mInkDensity = 0xff; // set to 0x20 or so for a felt-tip look, 0xff for traditional Markers
        
        public void setPenColor(int color) {
            mPenColor = color;
            if (color == 0) {
                mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                mPaint.setColor(Color.BLACK);
            } else {
                mPaint.setXfermode(null);
                
                mPaint.setColor(Color.BLACK); // or collor? or color & (mInkDensity << 24)?
                mPaint.setAlpha(mInkDensity);
                
                mPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            }
        }
        
        public void setPenType(int type) {
            switch (type) {
            case TYPE_WHITEBOARD:
                mShape = SHAPE_CIRCLE;
                mInkDensity = 0xff;
                break;
            case TYPE_FELTTIP:
                mShape = SHAPE_CIRCLE;
                mInkDensity = 0x10;
                break;
            case TYPE_AIRBRUSH:
                mShape = SHAPE_BITMAP_AIRBRUSH;
                mInkDensity = 0x80;
                break;
            case TYPE_FOUNTAIN_PEN:
                mShape = SHAPE_FOUNTAIN_PEN;
                mInkDensity = 0xff;
                break;
            }
            setPenColor(mPenColor);
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
        
        private final RectF tmpRF = new RectF();
        final void drawStrokePoint(Canvas c, float x, float y, float r, RectF dirty) {
            switch (mShape) {
            case SHAPE_SQUARE:
                c.drawRect(x-r,y-r,x+r,y+r, mPaint);
                break;
            case SHAPE_BITMAP_AIRBRUSH:
                tmpRF.set(x-r,y-r,x+r,y+r);
                if (mAirbrushBits == null || mAirbrushBitsFrame == null) {
                    throw new RuntimeException("Slate.drawStrokePoint: no airbrush bitmap - frame=" + mAirbrushBitsFrame);
                }
                c.drawBitmap(mAirbrushBits, mAirbrushBitsFrame, tmpRF, mPaint);
                break;
            case SHAPE_FOUNTAIN_PEN:
                tmpRF.set(x-r,y-r,x+r,y+r);
                if (mFountainPenBits == null || mFountainPenBitsFrame == null) {
                    throw new RuntimeException("Slate.drawStrokePoint: no fountainpen bitmap - frame=" + mFountainPenBitsFrame);
                }
                c.drawBitmap(mFountainPenBits, mFountainPenBitsFrame, tmpRF, mPaint);
                break;
            case SHAPE_CIRCLE:
            default:
                c.drawCircle(x, y, r, mPaint);
                break;
            }
            dirty.union(x-r, y-r, x+r, y+r);//跟新这个矩形和在这个矩形里面四个点围成的矩形
        }
        
        private final RectF tmpDirtyRectF = new RectF();
        public RectF strokeTo(Canvas c, float x, float y, float r) {
            final RectF dirty = tmpDirtyRectF;
            dirty.setEmpty();
            if (mLastR < 0) {
                drawStrokePoint(c,x,y,r,dirty);
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
                    drawStrokePoint(c,xi,yi,ri,dirty);

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
            
            return dirty;
        }
    }

    private MarkersPlotter[] mStrokes;

    Spot mTmpSpot = new Spot();
    
    private static Paint sBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    
    public Slate(Context c, AttributeSet as) {
        super(c, as);
        init();
    }
    
    public Slate(Context c) {
    	super(c);
    	init();
    }
    
    @SuppressLint("NewApi")
	private void init() {
        mEmpty = true;

        final int memClass;
        final ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            memClass = am.getLargeMemoryClass();
        } else {
            memClass = am.getMemoryClass();
        }
        final boolean lowMem = (memClass <= 16);

        final Resources res = getContext().getResources();

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ALPHA_8;
        if (lowMem) { // let's see how this works in practice
            opts.inSampleSize = 4;
        }
        mAirbrushBits = BitmapFactory.decodeResource(res, R.drawable.airbrush_light, opts);
        if (mAirbrushBits == null) { Log.e(TAG, "SmoothStroker: Couldn't load airbrush bitmap"); }
        mAirbrushBitsFrame = new Rect(0, 0, mAirbrushBits.getWidth(), mAirbrushBits.getHeight());
        
        mFountainPenBits = BitmapFactory.decodeResource(res, R.drawable.fountainpen, opts);
        if (mFountainPenBits == null) { Log.e(TAG, "SmoothStroker: Couldn't load fountainpen bitmap"); }
        mFountainPenBitsFrame = new Rect(0, 0, mFountainPenBits.getWidth(), mFountainPenBits.getHeight());

        // set up individual strokers for each pointer
        mStrokes = new MarkersPlotter[MAX_POINTERS]; // TODO: don't bother unless hasSystemFeature(MULTITOUCH_DISTINCT)
        for (int i=0; i<mStrokes.length; i++) {
            mStrokes[i] = new MarkersPlotter();
        }
        
        mPressureCooker = new PressureCooker(getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (HWLAYER) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
            } else if (SWLAYER) {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            } else {
                setLayerType(View.LAYER_TYPE_NONE, null);
            }
        }
    }

    public boolean isEmpty() { return mEmpty; }
    
    public void setPenSize(float min, float max) {
        mRadiusMin = min * 0.5f;
        mRadiusMax = max * 0.5f;
    }

    public void recycle() {
    	// WARNING: the slate will not be usable until you call load() or clear() or something
    	if (mPreviousBitmap != null) {
	    	mPreviousBitmap.recycle(); 
	    	mPreviousBitmap = null;
    	}
    	if (mCurrentBitmap != null) {
	    	mCurrentBitmap.recycle();
	        mCurrentBitmap = null;
    	}
    }

    public void clear() {
        if (mCurrentBitmap != null) {
            commitStroke();
            mCurrentCanvas.drawColor(0xffffffff, PorterDuff.Mode.SRC);
            invalidate();
        } else if (mPendingPaintBitmap != null) {
            mPendingPaintBitmap.recycle();
            mPendingPaintBitmap = null;
        }
        mEmpty = true;
    }

    public void commitStroke() {
        if (mPreviousCanvas == null) return;

        Canvas swapCanvas = mPreviousCanvas;
        Bitmap swapBitmap = mPreviousBitmap;

        mPreviousCanvas = mCurrentCanvas;
        mPreviousBitmap = mCurrentBitmap;

        swapCanvas.save();
        swapCanvas.setMatrix(null);
        swapCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        swapCanvas.drawBitmap(mPreviousBitmap, 0, 0, null);
        swapCanvas.restore();

        mCurrentCanvas = swapCanvas;
        mCurrentBitmap = swapBitmap;
    }

    public void undo() {
        mCurrentCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        mCurrentCanvas.drawBitmap(mPreviousBitmap, 0, 0, null);

        invalidate();
    }

    public void paintBitmap(Bitmap b) {
        if (mCurrentBitmap == null) {
            mPendingPaintBitmap = b;
            return;
        }

        commitStroke();

        Matrix m = new Matrix();
        RectF s = new RectF(0, 0, b.getWidth(), b.getHeight());
        RectF d = new RectF(0, 0, mCurrentBitmap.getWidth(), mCurrentBitmap.getHeight());
        m.setRectToRect(s, d, Matrix.ScaleToFit.CENTER);
        
        mCurrentCanvas.drawBitmap(b, m, sBitmapPaint);
        invalidate();
    }

    public void setDrawingBackground(int color) {
        mBackgroundColor  = color;
        setBackgroundColor(color);
        invalidate();
    }

    public Bitmap getBitmap() {
        commitStroke();
        return mPreviousBitmap;
    }

    public Bitmap copyBitmap(boolean withBackground) {
        Bitmap b = getBitmap();
        Bitmap newb = Bitmap.createBitmap(b.getWidth(), b.getHeight(), b.getConfig());
        if (newb != null) {
            Canvas newc = new Canvas(newb);
            if (mBackgroundColor != Color.TRANSPARENT && withBackground) {
                newc.drawColor(mBackgroundColor);
            }
            newc.drawBitmap(b, 0, 0, null);
        }
        return newb;
    }

    public void setPenColor(int color) {
        for (MarkersPlotter plotter : mStrokes) {
            plotter.setPenColor(color);
        }
    }
    
    public void setPenType(int shape) {
        for (MarkersPlotter plotter : mStrokes) {
            plotter.setPenType(shape);
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw,
            int oldh) {
        if (mCurrentBitmap != null) return;
        
        mCurrentBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        if (mCurrentBitmap == null) {
            throw new RuntimeException("onSizeChanged: Unable to allocate main buffer (" + w + "x" + h + ")");
        }
        mCurrentCanvas = new Canvas();
        mCurrentCanvas.setBitmap(mCurrentBitmap);

        mPreviousBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        if (mCurrentBitmap == null) {
            throw new RuntimeException("onSizeChanged: Unable to allocate undo buffer (" + w + "x" + h + ")");
        }
        mPreviousCanvas = new Canvas();
        mPreviousCanvas.setBitmap(mPreviousBitmap);

        final Bitmap b = mPendingPaintBitmap; 
        if (b != null) {
            mPendingPaintBitmap = null;
            paintBitmap(b);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCurrentBitmap != null) {
            if (!mDirtyRegion.isEmpty()) {
                canvas.clipRegion(mDirtyRegion);
                mDirtyRegion.setEmpty();
            }
            canvas.drawBitmap(mCurrentBitmap, 0, 0, null);
        }
    }

    float dbgX = -1, dbgY = -1;
    RectF dbgRect = new RectF();
    
    final static boolean hasPointerCoords() {
    	return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1);
    }

    final static boolean hasToolType() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);
    }
    
    @SuppressLint("NewApi")
	final static int getToolTypeCompat(MotionEvent me, int index) {
        if (hasToolType()) {
            return me.getToolType(index);
        }
        
        // dirty hack for the HTC Flyer
        if ("flyer".equals(Build.HARDWARE)) {
            if (me.getSize(index) <= 0.1f) {
                // with very high probability this is the stylus
                return MotionEvent.TOOL_TYPE_STYLUS;
            }
        }
        
        return MotionEvent.TOOL_TYPE_FINGER;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int N = event.getHistorySize();
        int P = event.getPointerCount();
        long time = event.getEventTime();

        mEmpty = false;

        // starting a new touch? commit the previous state of the canvas
        if (action == MotionEvent.ACTION_DOWN) {
            commitStroke();
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN
        		|| action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int j = event.getActionIndex();
            
        	mTmpSpot.update(
        	        event.getX(j),
        			event.getY(j),
        			event.getSize(j),
        			event.getPressure(j) + event.getSize(j),
        			time,
        			getToolTypeCompat(event, j)
        			);
            mStrokes[event.getPointerId(j)].add(mTmpSpot);
        	if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
	            mStrokes[event.getPointerId(j)].finish(time);
        	}
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (dbgX >= 0) {
                dbgRect.set(dbgX-1,dbgY-1,dbgX+1,dbgY+1);
            }

            for (int i = 0; i < N; i++) {
                for (int j = 0; j < P; j++) {
                	mTmpSpot.update(
                			event.getHistoricalX(j, i),
                			event.getHistoricalY(j, i),
                			event.getHistoricalSize(j, i),
                			event.getHistoricalPressure(j, i)
                                + event.getHistoricalSize(j, i),
                			event.getHistoricalEventTime(i),
                            getToolTypeCompat(event, j)
                			);
                    mStrokes[event.getPointerId(j)].add(mTmpSpot);
                }
            }
            for (int j = 0; j < P; j++) {
            	mTmpSpot.update(
            			event.getX(j),
            			event.getY(j),
            			event.getSize(j),
            			event.getPressure(j) + event.getSize(j),
            			time,
                        getToolTypeCompat(event, j)
           			);
                mStrokes[event.getPointerId(j)].add(mTmpSpot);
            }
        }
        
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            for (int j = 0; j < P; j++) {
                mStrokes[event.getPointerId(j)].finish(time);
            }
            dbgX = dbgY = -1;
        }
        return true;
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }
    
    @Override
    public void invalidate(Rect r) {
        if (r.isEmpty()) {
            Log.w(TAG, "invalidating empty rect!");
        }
        super.invalidate(r);
    }

    final Rect tmpDirtyRect = new Rect();
    private void dirty(RectF r) {
        r.roundOut(tmpDirtyRect);
        tmpDirtyRect.inset((int)-INVALIDATE_PADDING,(int)-INVALIDATE_PADDING);
        if (FANCY_INVALIDATES) {
            mDirtyRegion.union(tmpDirtyRect);
            invalidate(); // enqueue invalidation
        } else {
            invalidate(tmpDirtyRect);
        }
    }
}
