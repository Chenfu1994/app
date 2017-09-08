package ri_navlab.navlab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by fuchen on 1/30/17.
 */

public class Box extends View {
    private int x_d, y_d,x,y,dx,dy;
    private int x0, y0;

    private Paint paint = new Paint();
    Box(Context context, int x, int y) {

        super(context);
        this.x = x;
        this.y = y;
    }



    @Override
    protected void onDraw(Canvas canvas) { // Override the onDraw() Method
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(10);
        float size = 0;

        //center
         x0 = canvas.getWidth()/2;
         System.out.println("x0" + x0);
         y0 = canvas.getHeight()/2;
         System.out.println("y0" + y0);
         dx = canvas.getHeight()/24;
         dy = canvas.getHeight()/24;

         canvas.drawRect(x0 - dx , y0 - dy, x0 + dx, y0+ dy, paint);





            //canvas.drawRect(x0 - Math.abs(x0 - x), y0 - Math.abs(y0 - y), x0+Math.abs(x0 - x), y0+Math.abs(y0 - y), paint);


    }

   /* public boolean onTouchEvent(MotionEvent event) {


        int eventaction = event.getAction();
        switch (eventaction) {

            case MotionEvent.ACTION_DOWN:
                 x = (int)event.getX();
                 y = (int) event.getY();
                System.out.println(x);
                System.out.println(y);
                x_d = Math.abs(x0 - x);
                y_d= Math.abs(y0 - y);
                invalidate();
                break;


        }



      return true;
    }*/

    public int[] getValue(){
        //float portion_height, portion_width;
        /*double portion_height,portion_width;
        System.out.println("half image_height in box");
        System.out.println(y_d);
        System.out.println("half image_weight in box");
        System.out.println(x_d);
        portion_height = (float)(image_height ) / (1080);
        portion_width = (float)image_width / (1500);
        System.out.println("portion_height");
        System.out.println(portion_height);
        System.out.println("portion_width");
        System.out.println(portion_width);*/
        int[] output = new int[4];


        //output[0] = (int)(x0 * portion_height * 0.75);
        //output[1] = (int)(y0 * portion_width );
        //output[2] = (int)(2 * dx * portion_height * 0.75);
        //output[3] = (int)(2 * dy * portion_width);
        output[0] = x0;
        output[1] = y0;
        output[2] = dx;
        output[3] = dy;


        return output;

    }


}
