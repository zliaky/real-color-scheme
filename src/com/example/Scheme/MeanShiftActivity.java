package com.example.Scheme;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.clust4j.algo.MeanShift;
import android.graphics.Matrix;
import com.clust4j.algo.MeanShiftParameters;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by windows on 2016/5/26.
 */
public class MeanShiftActivity extends ListActivity {

    //TextView touchedXY, invertedXY, imgSize, colorRGB;
    TextView colorRGB;
    ImageView imgSource2;

    private int M_HEIGHT = 20;
    private int M_WIDTH = 20;
    int mosaicHeight;
    int mosaicWidth;
    int[] results;

    private MeanShift ms;
    private int counter;    //统计采样点的个数
    private double data[][];   //采样后的图片的数组
    private Bitmap bitmap;
    Drawable imgDrawable;
    Bitmap draw_bitmap;

    //about list view

    private List<Node> colors;
    private MyAdapter adapter;

    public final class ViewHolder{
        public ImageView color;
    }

    public class MyAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        public MyAdapter(Context context){
            this.mInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            return colors.size();
        }
        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder=new ViewHolder();
                convertView = mInflater.inflate(R.layout.list_color, null);
                final ViewHolder finalHolder = holder;
                holder.color = (ImageView)convertView.findViewById(R.id.color);
                holder.color.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View view) {
                        ColorDrawable draw = (ColorDrawable)finalHolder.color.getBackground();
                        int color = draw.getColor();



                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = (color >> 0) & 0xFF;


                        int _r = (r*100)/255;
                        int _g = (g*100)/255;
                        int _b = (b*100)/255;
                        int _max = Math.max(Math.max(_r,_g),_b);
                        int k = 100 -_max;
                        int c = (_max-_r)*100/_max;
                        int m = (_max-_g)*100/_max;
                        int y = (_max-_b)*100/_max;
                        if (k > 50) return;

                        double w = 540-5*c-2*m-5*y;
                        if(w < 0) w = 0;

                        AlertDialog.Builder normalDia=new AlertDialog.Builder(MeanShiftActivity.this);
                        normalDia.setIcon(R.drawable.ic_launcher);

                        String msg = (int)c +  "," + (int)m +  "," +(int)y + "," + (int)w + "\n";
                        normalDia.setMessage("Send Color: " + msg);

                        normalDia.setNegativeButton("取消", null);
                        normalDia.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    Global.mmOutputStream.write(msg.getBytes());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        normalDia.create().show();
                    }});

                convertView.setTag(holder);

            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            Node c = colors.get(position);
            //System.out.println("position: = " + position);
            //System.out.println("r,g,b: = " + c.r+","+c.g+","+c.b);
            holder.color.setBackgroundColor(Color.rgb(c.r,c.g,c.b));
            return convertView;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.meanshift);

        bitmap = BitmapFactory.decodeFile(Global.filename);  //读图片
        if (bitmap == null) {
            debug("bitmap null");
            return;
        }

        //touchedXY = (TextView)findViewById(R.id.xy);
        //invertedXY = (TextView)findViewById(R.id.invertedxy);
        //imgSize = (TextView)findViewById(R.id.size);
        colorRGB = (TextView)findViewById(R.id.colorrgb);
        ((ImageView)findViewById(R.id.source2)).setImageBitmap(bitmap);
        imgSource2 = (ImageView)findViewById(R.id.source2);
        imgDrawable = imgSource2.getDrawable();
        draw_bitmap = ((BitmapDrawable)imgDrawable).getBitmap();
        initMeanShift();
        imgSource2.setOnTouchListener(imgSourceOnTouchListener);

        //listview
        colors = new ArrayList<>();
        adapter = new MyAdapter(this);
        setListAdapter(adapter);
    }



    void initMeanShift() {
        //mosaic
        int width = draw_bitmap.getWidth();
        int height = draw_bitmap.getHeight();
        mosaicHeight= height/M_HEIGHT;
        mosaicWidth = width/M_WIDTH;

        data = new double[height*width/(mosaicHeight*mosaicWidth)+100][3];
        counter = 0;

        for (int i = 0; i < width; i+=mosaicWidth) {
            for (int j = 0; j < height; j+=mosaicHeight) {
                int pixel = draw_bitmap.getPixel(i, j);
                int r = (pixel & 0xff0000) >> 16;
                int g = (pixel & 0xff00) >> 8;
                int b = (pixel & 0xff);                         //计算RGB
                data[counter][0] = (double)r/256;
                data[counter][1] = (double)g/256;
                data[counter][2] = (double)b/256;                  //把采样后的像素点放进d[][]中
                counter++;
            }
        }

        //debug("count = "+String.valueOf(counter));
        //debug("data length = "+String.valueOf(data.length));
        long start = System.currentTimeMillis();
        final Array2DRowRealMatrix mat = new Array2DRowRealMatrix(data, true);
        ms = new MeanShiftParameters(0.1).fitNewModel(mat);
        long end = System.currentTimeMillis();
        debug("duration = "+String.valueOf(end - start));
        results = ms.getLabels();
    }

    View.OnTouchListener imgSourceOnTouchListener
            = new View.OnTouchListener(){

        @Override
        public boolean onTouch(View view, MotionEvent event) {

            float eventX = event.getX();
            float eventY = event.getY();
            float[] eventXY = new float[] {eventX, eventY};

            Matrix invertMatrix = new Matrix();
            ((ImageView)view).getImageMatrix().invert(invertMatrix);
            //debug(String.valueOf(view == imgSource2));

            invertMatrix.mapPoints(eventXY);
            int x = Integer.valueOf((int)eventXY[0]);
            int y = Integer.valueOf((int)eventXY[1]);

//            touchedXY.setText(
//                    "touched position: "
//                            + String.valueOf(eventX) + " / "
//                            + String.valueOf(eventY));
//            invertedXY.setText(
//                    "inverted touched position: "
//                            + String.valueOf(x) + " / "
//                            + String.valueOf(y));
//
//            imgSize.setText(
//                    "drawable size: "
//                            + String.valueOf(draw_bitmap.getWidth()) + " / "
//                            + String.valueOf(draw_bitmap.getHeight()));

            //Limit x, y range within bitmap
            if(x < 0){
                x = 0;
            }else if(x > draw_bitmap.getWidth()-1){
                x = draw_bitmap.getWidth()-1;
            }
            if(y < 0){
                y = 0;
            }else if(y > draw_bitmap.getHeight()-1){
                y = draw_bitmap.getHeight()-1;
            }

            int touchedRGB = draw_bitmap.getPixel(x, y);

            colorRGB.setText("touched color: " + "#" + Integer.toHexString(touchedRGB));
            colorRGB.setTextColor(touchedRGB);

            int x_index = x/mosaicWidth;
            int y_index = y/mosaicHeight;

            int index =x_index * M_HEIGHT + y_index;

            //debug("index: = " + String.valueOf(index));
            //System.out.println("index: = " + String.valueOf(index));
            //System.out.println("label: = " + String.valueOf(results[index]));
            refreshListView(index);
            return true;
        }};

    void refreshListView(int index) {
        colors.clear();
        for(int i = 0 ; i < counter ; i++) {
            if(results[i] == results[index])
            {
                //System.out.println("color: = " + data[i][0]*256 + "," +data[i][1]*256 + "," + data[i][2]*256);
                Node n = new Node();
                n.r = (int)(data[i][0]*256);
                n.g = (int)(data[i][1]*256);
                n.b = (int)(data[i][2]*256);
                colors.add(n);
            }
        }

        adapter.notifyDataSetChanged();
        System.out.println("color count: = " + colors.size());

    }

    void debug(String s) {
        Toast.makeText(MeanShiftActivity.this,s, Toast.LENGTH_SHORT).show();
    }   //根本没有用上的debug函数=w=


}
