package com.example.locationdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;

public class DrawActivity extends AppCompatActivity {

    DrawingView drawingView;
    String absoluteImgPath;
    public static INDArray output;

    private static final String TAG = "DrawActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        RelativeLayout parent = findViewById(R.id.layout2);
        drawingView = new DrawingView(this);
        parent.addView(drawingView);
    }


    private class AsyncTaskRunner extends AsyncTask<String, Integer, INDArray> {

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected INDArray doInBackground(String... params) {
            // Main background thread, this will load the model and test the input image
            int height = 28;
            int width = 28;
            int channels = 1;

            //load the model from the raw folder with a try / catch block
            try {
                // Load the pretrained network.
                InputStream inputStream = getResources().openRawResource(R.raw.mnist_model);
                MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(inputStream);

//                MultiLayerNetwork model = MultiLayerNetwork.load(new File("mnist_model"), true);

                //load the image file to test
                File f = new File(absoluteImgPath);

                //Use the nativeImageLoader to convert to numerical matrix
                NativeImageLoader loader = new NativeImageLoader(height, width, channels);

                //put image into INDArray
                INDArray image = loader.asMatrix(f);

                //values need to be scaled
                DataNormalization scalar = new ImagePreProcessingScaler(0, 1);

                //then call that scalar on the image dataset
                scalar.transform(image);

                //pass through neural net and store it in output array
                output = model.output(image.reshape(1, 784));

            } catch (IOException e) {
                e.printStackTrace();
            }
            return output;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }


        // doInBackground执行完会把result返回给这个onPostExecute方法，一般是做UI的更新，执行在UI线程
        @Override
        protected void onPostExecute(INDArray result) {
            super.onPostExecute(result);

            //used to control the number of decimals places for the output probability
            DecimalFormat df2 = new DecimalFormat(".##");

            //transfer the neural network output to an array
            double[] results = {result.getDouble(0, 0), result.getDouble(0, 1), result.getDouble(0, 2),
                    result.getDouble(0, 3), result.getDouble(0, 4), result.getDouble(0, 5), result.getDouble(0, 6),
                    result.getDouble(0, 7), result.getDouble(0, 8), result.getDouble(0, 9),};

            //find the UI tvs to display the prediction and confidence values
            TextView out1 = findViewById(R.id.prediction);
            TextView out2 = findViewById(R.id.confidence);

            //display the values using helper functions defined below

            String prediction = String.valueOf(getIndexOfLargestValue(results));
            String confidence = df2.format(arrayMaximum(results));

            Log.d(TAG, "onPostExecute: prediction: " + prediction);
            Log.d(TAG, "onPostExecute: confidence: " + confidence);

            out2.setText(confidence);
            out1.setText(prediction);

            //helper function to turn off progress test
            offProgressBar();
        }

    }

    //code for the drawing input
    public class DrawingView extends View {

        private Path mPath;
        private Paint mBitmapPaint;
        private Paint mPaint;
        private Bitmap mBitmap;
        private Canvas mCanvas;

        public DrawingView(Context c) {
            super(c);

            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(60);
            mPaint.setDither(true);
            mPaint.setColor(Color.WHITE);
            mPaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onSizeChanged(int W, int H, int oldW, int oldH) {
            super.onSizeChanged(W, H, oldW, oldH);
            mBitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_4444);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y;
        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
            }
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            mCanvas.drawPath(mPath, mPaint);
            mPath.reset();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) { // 触摸屏幕时
            float x = event.getX();
            float y = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: // 记录触摸点坐标
                    Log.d(TAG, "onTouchEvent: touch down");
                    invalidate();
                    clear();
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE: // 计算偏移量
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP: // 记录释放点坐标
                    Log.d(TAG, "onTouchEvent: touch up");
                    touch_up();
                    absoluteImgPath = saveDrawing(); // 保存手写图片
                    Log.d(TAG, "onTouchEvent: imgPath " + absoluteImgPath);
                    invalidate();
                    clear();
                    onProgressBar();
                    loadImageFromStorage(absoluteImgPath);
                    // launch the asyncTask now that the image has been saved
                    AsyncTaskRunner runner = new AsyncTaskRunner();
                    runner.execute(absoluteImgPath);
                    break;
            }
            return true;
        }

        public void clear() {
            mBitmap.eraseColor(Color.TRANSPARENT);
            invalidate();
            System.gc();
        }

    }

    public String saveDrawing() {
        drawingView.destroyDrawingCache();
        drawingView.setDrawingCacheEnabled(true);
        Bitmap b = drawingView.getDrawingCache();

        String imgName = "drawn_img_" + System.currentTimeMillis() / 1000 + ".jpg";
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // set the path to storage
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir and store the file there. Each new drawing will overwrite the previous

        String absoluteImgPath = directory + "/" + imgName;

        File mypath = new File(absoluteImgPath);

        //use a fileOutputStream to write the file to the location in a try / catch block
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            b.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return absoluteImgPath;
    }


    private void loadImageFromStorage(String absolutImgPath) {

        //use a fileInputStream to read the file in a try / catch block
        try {
            File f = new File(absolutImgPath);
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            ImageView img = (ImageView) findViewById(R.id.outputView);
            img.setImageBitmap(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void onProgressBar() {
        TextView bar = findViewById(R.id.processing);
        bar.setVisibility(View.VISIBLE);
    }

    public void offProgressBar() {
        TextView bar = findViewById(R.id.processing);
        bar.setVisibility(View.INVISIBLE);
    }

    //helper class to return the largest value in the output array
    public static double arrayMaximum(double[] arr) {
        double max = Double.NEGATIVE_INFINITY;
        for (double cur : arr)
            max = Math.max(max, cur);
        return max;
    }

    // helper class to find the index (and therefore numerical value) of the largest confidence score
    public static int getIndexOfLargestValue(double[] array) {
        if (array == null || array.length == 0) return -1;
        int largest = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[largest]) largest = i;
        }
        return largest;
    }
}