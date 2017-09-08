package ri_navlab.navlab;

import android.os.Environment;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fuchen on 7/24/17.
 */

public class RetroReflectivity {
            private String mImageFileName_raw;
            private String GALLERY_LOCATION_Raw = "Raw image gallery";
            private String mImageFileName_raw_txt;
            private File mGalleryFolder_raw;
            private byte[] bytes1, bytes2;
            private int start_x, start_y, height, width,height_total, width_total,dist;
            private float weightPaper = 1;
            private Mat group_1, group_2, group_3, group_4, group_1_flash, group_2_flash, group_3_flash, group_4_flash;
            private float total_pixel;
            private float Num;
             RetroReflectivity(byte[] byteBuffer1, byte[] byteBuffer2, int start_x, int start_y, int height, int width, int height_total, int width_total, int dist){
            this.bytes1 = byteBuffer1;
            this.bytes2 = byteBuffer2;
            this.start_x = start_x;
            this.start_y = start_y;
            this.height_total = width_total;
            this.width_total = height_total;
            this.dist = dist;
            this.height = height;
            this.width = width;
            dist =5;
            group_1 = new Mat(width / 2, height / 2, CvType.CV_16S);
            group_2 = new Mat(width / 2, height / 2, CvType.CV_16S);
            group_3 = new Mat(width / 2, height / 2, CvType.CV_16S);
            group_4 = new Mat(width / 2, height / 2, CvType.CV_16S);
            group_1_flash = new Mat(width / 2, height / 2, CvType.CV_16S);
            group_2_flash = new Mat(width / 2, height / 2, CvType.CV_16S);
            group_3_flash = new Mat(width / 2, height / 2, CvType.CV_16S);
            group_4_flash = new Mat(width / 2, height / 2, CvType.CV_16S);
            this.total_pixel = (height / 2) * (width / 2);
            this.Num = ((dist / 5) * (dist / 5) ) / weightPaper;

    };


    public int get_partition(){
        int i;
        Mat out_mat_nonFlash = new Mat( width_total,height_total, CvType.CV_16S);
        Mat out_mat_flash = new Mat( width_total,height_total, CvType.CV_16S);


        System.out.println("TOTAL IMAGE HEIGHT");
        System.out.println(height_total);
        System.out.println("TOTAL IMAGE WIDTH");
        System.out.println(width_total);
        System.out.println("PART IMAGE HEIGHT");
        System.out.println(height);
        System.out.println("PART IMAGE WIDTH");
        System.out.println(width);


        short[] shorts_nonFlash = new short[bytes1.length / 2];
        short[] shorts_flash = new short[bytes2.length / 2];
        Rect rect_nonFlash = new Rect(start_x ,start_y , height, width);
        Rect rect_Flash = new Rect(start_x ,start_y , height, width);
        Mat out_mat = new Mat(height, width, CvType.CV_16S);
       // System.out.println("total bytes");
       // System.out.println(bytes1.length / 2);
        for (i = 0; i < bytes1.length / 2; i++)  //changed from i = 0 to bytes1.length/2
        {
            int int_1 = toUnsignedInt(bytes1[2 * i + 1]);
            int int_2 = toUnsignedInt(bytes1[2 * i]);

            shorts_nonFlash[i] = twoBytesToShort(int_1, int_2);
            //System.out.println(shorts_nonFlash[i]);   //problems with doing this linearly, changed indices of shorts array
            int int_3 = toUnsignedInt(bytes2[2 * i + 1]);
            int int_4 = toUnsignedInt(bytes2[2 * i]);
            shorts_flash[i] = twoBytesToShort(int_3, int_4);
            //System.out.println(shorts_flash[i]);

        }
       // System.out.println("Finish byte covergence!");

        out_mat_nonFlash.put(0, 0, shorts_nonFlash);
        out_mat_flash.put(0, 0, shorts_flash);
        Mat out_mat_nonFlash1 = out_mat_nonFlash.submat(rect_nonFlash);
        Mat out_mat_Flash1 = out_mat_flash.submat(rect_Flash);
        Core.subtract(out_mat_Flash1, out_mat_nonFlash1, out_mat);


      // System.out.println("Finish get groups");

        System.out.println("out_mat.size().height");
        System.out.println(out_mat.size().height);
        System.out.println("out_mat.size().width");
        System.out.println(out_mat.size().width);
        System.out.println("group_1.size().height");
        System.out.println(group_1.size().height);
        System.out.println("group_1.size().width");
        System.out.println(group_1.size().width);
        int a = 0;
        for (int k = 0; k < out_mat.size().height; k++) {
            //System.out.println("---Traversing matrix of pixels----");
            for (int j = 0; j < out_mat.size().width; j++) {
                if (k % 2 == 0 && j % 2 == 0) {
                    group_1.put((k ) / 2, (j ) / 2, out_mat.get(k, j)[0]);
                    group_1_flash.put((k ) / 2, (j ) / 2, out_mat_Flash1.get(k, j)[0]);

                }
                if (k % 2 == 0 && j % 2 == 1) {
                    group_2.put((k ) / 2, (j ) / 2, out_mat.get(k, j)[0]);
                    group_2_flash.put((k ) / 2, (j ) / 2, out_mat_Flash1.get(k, j)[0]);

                }
                if (k % 2 == 1 && j % 2 == 0) {
                    group_3.put((k ) / 2, (j ) / 2, out_mat.get(k, j)[0]);
                    group_3_flash.put((k ) / 2, (j ) / 2, out_mat_Flash1.get(k, j)[0]);

                }
                if (k % 2 == 1 && j % 2 == 1) {
                    group_4.put((k ) / 2, (j ) / 2, out_mat.get(k, j)[0]);
                    group_4_flash.put((k ) / 2, (j ) / 2, out_mat_Flash1.get(k, j)[0]);

                }
            }
        }
      /*  createImageGallery_Raw();
        DataOutputStream fos1 = null;
        try{
            File text = this.createImageFile_Raw();
        }catch(Exception e){
            e.printStackTrace();
        }
        short[] data = new short[(height) * (width)];
        out_mat_nonFlash1.get(0, 0, data);
        try {
            System.out.println("----txt file found here------");
            System.out.println("height:" + height);
            System.out.println("width:" + width );
            fos1 = new DataOutputStream(new FileOutputStream(mImageFileName_raw_txt));
            System.out.println(mImageFileName_raw_txt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("---------------From raw image write image 1---------------------");
            for (int j = 0; j < data.length; j++) {
                fos1.writeShort(data[j]);
            }
            fos1.close();

        } catch (IOException e) {
            e.printStackTrace();
        }*/

     return 0;

    }


    public float[] retoreFlectivityGreen(){
        float[] reto_green = new float[2];
        double max_green = 0;
        int count_Green = 0;
        Mat temp_mat1 = new Mat(height / 2, width / 2, CvType.CV_16S);
        Mat temp_mat2 = new Mat(height / 2, width / 2, CvType.CV_16S);
        Mat temp_mat1_flash = new Mat(height / 2, width / 2, CvType.CV_16S);
        Mat temp_mat2_flash = new Mat(height / 2, width / 2, CvType.CV_16S);
        float temp_sum_green = 0;
        if( ( (start_x % 2 == 0) && (start_y % 2 == 0) ) || ( (start_x % 2 == 1) && (start_y % 2 == 1) )){
            temp_mat1 = group_1;
            temp_mat2 = group_4;
            temp_mat1_flash = group_1_flash;
            temp_mat2_flash = group_4_flash;
            System.out.println("Green group_14_flash");
        }

        if(  (start_x % 2 == 1) && (start_y % 2 == 0) ){
            temp_mat1 = group_2;
            temp_mat2 = group_3;
            temp_mat1_flash = group_2_flash;
            temp_mat2_flash = group_3_flash;
            System.out.println("Green group_23_flash");
        }


        if(  (start_x % 2 == 0) && (start_y % 2 == 1) ){

            temp_mat1 = group_2;
            temp_mat2 = group_3;
            temp_mat1_flash = group_2_flash;
            temp_mat2_flash = group_3_flash;
            System.out.println("Green group_23_flash");
        }
        for (int k = 0; k < temp_mat1.size().height; k++) {
            //System.out.println("---Traversing matrix of pixels----");
            for (int j = 0; j < temp_mat1.size().width; j++) {
                temp_sum_green += temp_mat1.get(k, j)[0];
                if(temp_mat1_flash.get(k, j)[0] == 1023) {
                    max_green = temp_mat1_flash.get(k, j)[0];
                }
                //System.out.println(temp_mat1_flash.get(k, j)[0]);

            }

        }


        for (int k = 0; k < temp_mat2.size().height; k++) {
            //System.out.println("---Traversing matrix of pixels----");
            for (int j = 0; j < temp_mat2.size().width; j++) {
                if(temp_mat2.get(k, j)[0] == 0){//||(temp_mat2.get(k, j)[0] >1023) ||(temp_mat2.get(k, j)[0] < -1023)){
                    count_Green ++;
                }
                temp_sum_green += temp_mat2.get(k, j)[0];

                if(temp_mat2_flash.get(k, j)[0] == 1023) {
                    max_green = temp_mat2_flash.get(k, j)[0];
                }

            }

        }



        reto_green[0] = temp_sum_green * Num / (total_pixel * 2 - count_Green);

        if(max_green >= 1023)
            reto_green[1] = 1;
        else
            reto_green[1] = 0;

        return reto_green;


    }


    public float[] retoreFlectivityRed(){

        float[]  reto_red = new float[2];
        float temp_sum_red = 0;
        double max_red = 0;
        int count_Red = 0;
        Mat temp_mat1 = new Mat(height / 2, width / 2, CvType.CV_16S);
        Mat temp_mat1_flash = new Mat(height / 2, width / 2, CvType.CV_16S);
        if( (start_x % 2 == 0) && (start_y % 2 == 0) ){
            temp_mat1 =  group_2;
            temp_mat1_flash = group_2_flash;
            System.out.println("Red group_3_flash");

        }

        if(  (start_x % 2 == 1) && (start_y % 2 == 0) ){
            temp_mat1 = group_1;
            temp_mat1_flash = group_1_flash;
            System.out.println("Red group_1_flash");
        }


        if(  (start_x % 2 == 0) && (start_y % 2 == 1) ){
            temp_mat1 = group_4;
            temp_mat1_flash = group_4_flash;
            System.out.println("Red group_4_flash");
        }

        if(  (start_x % 2 == 1) && (start_y % 2 == 1) ){
            temp_mat1 = group_3;
            temp_mat1_flash = group_3_flash;
            System.out.println("Red group_2_flash");
        }

        for (int k = 0; k < temp_mat1.size().height; k++) {
            //System.out.println("---Traversing matrix of pixels----");
            for (int j = 0; j < temp_mat1.size().width; j++) {
                //System.out.println(temp_mat1.get(k, j)[0]);

                if (temp_mat1.get(k, j)[0] == 0) { //|| (temp_mat1.get(k, j)[0] >1023) ||(temp_mat1.get(k, j)[0] < -1023)){
                    count_Red++;

                }
                temp_sum_red += temp_mat1.get(k, j)[0];


                if (temp_mat1_flash.get(k, j)[0] == 1023) {
                    max_red = temp_mat1_flash.get(k, j)[0];
                }


            }
        }

     /*       short[] data = new short[(height / 2) * (width / 2)];
            temp_mat1_flash.get(0, 0, data);
            DataOutputStream fos1 = null;
        try {
            System.out.println("----txt file found here------");
            System.out.println("height:" + height / 2);
            System.out.println("width:" + width / 2);
            fos1 = new DataOutputStream(new FileOutputStream(mImageFileName_raw_txt));
            System.out.println(mImageFileName_raw_txt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            System.out.println("---------------From raw image write image 1---------------------");
            for (int j = 0; j < data.length; j++) {
                fos1.writeShort(data[j]);
            }
            fos1.close();

        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //System.out.println(max_red);
        System.out.println(count_Red);
        reto_red[0] = temp_sum_red * Num / (total_pixel); //- count_Red);
        if(max_red >= 1023)
            reto_red[1] = 1;
        else
            reto_red[1] = 0;
        return reto_red;
    }


    public float[] retoreFlectivityBlue(){
        /*try{
            File text = this.createImageFile_Raw();
        }catch(Exception e){
            e.printStackTrace();
        }*/
        //DataOutputStream fos1 = null;

        float[] reto_blue = new float[2];
        float temp_sum_blue = 0;
        double max_blue = 0;
        int count_Blue = 0;
        Mat temp_mat1 = new Mat(height / 2, width / 2, CvType.CV_16S);
        Mat temp_mat1_flash = new Mat(height / 2, width / 2, CvType.CV_16S);
        if( (start_x % 2 == 0) && (start_y % 2 == 0) ){
            System.out.println("Blue group_2_flash");
            temp_mat1 = group_3;
            temp_mat1_flash = group_3_flash;
        }

        if(  (start_x % 2 == 1) && (start_y % 2 == 0) ){
            temp_mat1 = group_4;
            temp_mat1_flash = group_4_flash;
            System.out.println("Blue group_4_flash");
        }


        if(  (start_x % 2 == 0) && (start_y % 2 == 1) ){
            temp_mat1 = group_1;
            temp_mat1_flash = group_1_flash;
            System.out.println("Blue group_1_flash");
        }

        if(  (start_x % 2 == 1) && (start_y % 2 == 1) ){
            temp_mat1 = group_2;
            temp_mat1_flash = group_2_flash;
            System.out.println("Blue group_3_flash");
        }
        for (int k = 0; k < temp_mat1.size().height; k++) {
            //System.out.println("---Traversing matrix of pixels----");
            for (int j = 0; j < temp_mat1.size().width; j++) {

                temp_sum_blue += temp_mat1.get(k, j)[0];

                if(temp_mat1_flash.get(k, j)[0] == 1023){
                    max_blue = temp_mat1_flash.get(k, j)[0];
                }

              }
            }
        reto_blue[0] = temp_sum_blue * Num / (total_pixel - count_Blue);
        if(max_blue >= 1023)
            reto_blue[1] = 1;
        else
            reto_blue[1] = 0;


        return reto_blue;
    }


    private short twoBytesToShort(int b1, int b2) {
        return (short) ((b1 << 8) | b2);
    }

    public int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    public void clearall(){
        group_1= null;
        group_2= null;
        group_3 = null;
        group_4 = null;
        group_1_flash = null;
        group_2_flash = null;
        group_3_flash =null;
        group_4_flash = null;
        bytes1 = null;
        bytes2 = null;





    }


    File createImageFile_Raw() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String timeStamp =generateTimestamp();
        String imageFileName = "IMAGE_" + timeStamp + "_RAW";
        //System.out.println(imageFileName);
        //System.out.println(mGalleryFolder_raw);
        File text = File.createTempFile(imageFileName, ".txt", mGalleryFolder_raw);
        //System.out.println("in create image");
        mImageFileName_raw_txt = text.getAbsolutePath();
        //System.out.println(mImageFileName_raw);

        return text;


    }


    private void createImageGallery_Raw() {
        String state = Environment.getExternalStorageState();
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // mGalleryFolder= new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) +"/image gallery new/");
        mGalleryFolder_raw = new File(storageDirectory, GALLERY_LOCATION_Raw);
        //System.out.println(mGalleryFolder_raw);
        if (!mGalleryFolder_raw.exists()) {
            //  System.out.println("not created");
            if (mGalleryFolder_raw.mkdirs()) {
                //Toast.makeText(getApplicationContext(), "new file dir made", Toast.LENGTH_SHORT).show();
            }
        }

    }
}

