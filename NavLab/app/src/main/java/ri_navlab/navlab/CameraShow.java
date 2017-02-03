package ri_navlab.navlab;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by fuchen on 1/30/17.
 */

public class CameraShow extends SurfaceView implements Callback, PictureCallback {

    private SurfaceHolder   holder;
    private Camera          camera;
    @SuppressWarnings("unused")
    private Context context;
    private Paint           paint       = new Paint();

    private long            firstPoint;
    private long            secondPoint;

    @SuppressWarnings("deprecation")
    CameraShow(Context context)
    {
        super(context);
        this.setWillNotDraw(false);

        this.context = context;

        holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try
        {
            camera = Camera.open(0);
            camera.setDisplayOrientation(90); // Rotates Camera's preview 90 degrees
            camera.setPreviewDisplay(holder);
        }
        catch (IOException e)
        {
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        camera.setPreviewCallback(null);
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            camera.takePicture(null, null, this);

        return true;
    }

    public void onPictureTaken(byte[] data, Camera camera)
    {
        camera.startPreview();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        //  Find Screen size first
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        int screenWidth = metrics.widthPixels / 2;
        int screenHeight = metrics.heightPixels / 2;

        //  Set paint options
        paint.setAntiAlias(true);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(255, 237, 28, 36));

        //canvas.drawLine(50,10,50,10+80,paint);
        canvas.drawRect(screenWidth - 20, screenHeight - 20, screenWidth + 20, screenHeight + 20, paint);

        //super.onDraw(canvas);
    }
}
