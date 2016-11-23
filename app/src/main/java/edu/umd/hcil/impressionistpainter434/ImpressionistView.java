package edu.umd.hcil.impressionistpainter434;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.content.ContentResolver;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {
    private static final int BRUSH_RADIUS = 25;

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;

    private VelocityTracker mVelocityTracker = null;

    private ArrayList<DrawRect> _drawRectList = new ArrayList<DrawRect>();

    private class DrawRect {
        Rect _rect = new Rect();
        Paint _paint = new Paint();

        DrawRect(Rect rect, Paint paint) {
            _rect = rect;
            _paint = paint;
        }

        void drawDrawRect (Canvas canvas) {
            canvas.drawRect(_rect, _paint);
        }
    }

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     * Modified From:
     *  - https://github.com/jonfroehlich/CMSC434DrawTest
     */
    public void clearPainting(){
        //TODO
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
        }

        invalidate();
    }

    /**
     * Saves the painting to the Gallery
     * Modified From:
     *  - http://stackoverflow.com/questions/8560501/android-save-image-into-gallery
     *  - http://stackoverflow.com/questions/3750903/how-can-getcontentresolver-be-called-in-android
     *  - http://www.androiddesignpatterns.com/2012/06/content-resolvers-and-content-providers.html
     */
    public void savePainting(){
        // TODO
        ContentResolver cr = _imageView.getContext().getContentResolver();
        MediaStore.Images.Media.insertImage(cr, this._offScreenBitmap, "My Painting" , "A beautiful painting");
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, null);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    /*
     * Modified From:
     *  - https://github.com/jonfroehlich/CMSC434DrawTest
     *  - https://developer.android.com/training/gestures/movement.html
     */
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //TODO
        //Basically, the way this works is to listen for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location

        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();
        int brushRadius = BRUSH_RADIUS;

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Set up the velocity tracker for tracking finger speed
                if(mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                }
                else {
                    mVelocityTracker.clear();
                }
                mVelocityTracker.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                // Set the transparency according to current speed
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                int index = motionEvent.getActionIndex();
                int pointerId = motionEvent.getPointerId(index);
                float velocityX = VelocityTrackerCompat.getXVelocity(mVelocityTracker, pointerId);
                float velocityY = VelocityTrackerCompat.getYVelocity(mVelocityTracker, pointerId);
                float avgSpeed = velocityX + velocityY / 2;
                float alphaVal = Math.abs(avgSpeed / 1000 * 255);

                // Paint using color from the loaded image
                Bitmap imageViewBitmap = _imageView.getDrawingCache();
                if (imageViewBitmap != null && touchY < imageViewBitmap.getHeight() && touchX < imageViewBitmap.getWidth() && touchY > 0 && touchX > 0) {
                    int colorAtTouchPixelInImage = imageViewBitmap.getPixel((int)touchX, (int)touchY);
                    _paint.setColor(colorAtTouchPixelInImage);
                    _paint.setAlpha((int) alphaVal);

                    // Change shape based on current brush selection
                    if (_brushType == BrushType.Square) {
                        _offScreenCanvas.drawRect(touchX - brushRadius, touchY - brushRadius, touchX + brushRadius, touchY + brushRadius, _paint);
                    } else if (_brushType == BrushType.Circle) {
                        _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, _paint);
                    } else if (_brushType == BrushType.Line) {
                        _offScreenCanvas.drawLine(touchX - brushRadius, touchY - brushRadius, touchX + brushRadius, touchY + brushRadius, _paint);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        invalidate();

        return true;
    }

    /**
     * Handles Motion Event and implements the Bonus Feature
     * Modified From:
     *  - https://developer.android.com/reference/android/hardware/SensorEvent.html
     */
    public void onSensorEvent(SensorEvent sensorEvent) {
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        final float alpha = 0.8f;
        float[] gravity = new float[3];
        float[] linear_acceleration = new float[3];

        gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

        linear_acceleration[0] = sensorEvent.values[0] - gravity[0];
        linear_acceleration[1] = sensorEvent.values[1] - gravity[1];
        linear_acceleration[2] = sensorEvent.values[2] - gravity[2];

        Log.d("linear_accelertation", linear_acceleration.toString());
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

