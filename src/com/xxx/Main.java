package com.xxx;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.Random;

public class Main {

    //跳跃速度参数
    private final static double SPEED = 1.425;
    //上传图片存储路径
    private final static String PATH = "C:/logs/screen.png";

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * 手机投影到电脑，对电脑进行截屏;(或者使用adb命令截屏后发送到电脑)
     * @param position  投影到电脑后的投影位置(相对于电脑屏幕)
     * @return  Mat类型的图像矩阵
     */
    private static Mat capture(Rectangle position){
        BufferedImage screenImg = null;
        try {
            screenImg = new Robot().createScreenCapture(position);
        } catch (AWTException e) {
            e.printStackTrace();
        }
        BufferedImage image = new BufferedImage(position.width,position.height,BufferedImage.TYPE_3BYTE_BGR);
        image.setData(screenImg.getData());
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(),image.getWidth(), CvType.CV_8UC3);
        mat.put(0,0,data);
        return mat;
    }

    public static void main(String[] args) {
        Runtime runtime = Runtime.getRuntime();
        while (true) {
            try {
                Process process = runtime.exec("cmd /c adb shell screencap -p /sdcard/screen.png");
                process.waitFor();
                Process process1 = runtime.exec("cmd /c adb pull /sdcard/screen.png " + PATH  );
                process1.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            Mat image = Imgcodecs.imread(PATH);
            if(image.dataAddr() != 0){
                Point player_Point = findPlayer(image);
                System.out.println("player_Point: " + player_Point);
                if(player_Point != null) {
                    Point stage_point = findStage(image, player_Point);
                    System.out.println("stage_point: " + stage_point);
                    double distance = Math.hypot(player_Point.x - stage_point.x, player_Point.y - stage_point.y);
                    System.out.println("distance: "+distance);
                    click(distance);
                }
            }
        }
    }

    //模拟点击
    private static void click(double distance)  {
        long time = (long)(distance*SPEED);
        Random random = new Random();
        // 模拟点击位置
        int x = 400+random.nextInt(500);
        int y = 1700+random.nextInt(300);

        Process exec;
        try {
            exec = Runtime.getRuntime().exec("cmd /c adb shell input swipe "+ x +" " + y + " " + x+100 + " " + y+100 + " " + time);
            exec.waitFor();
            // 等待人物落到方块上，跳至中心点时光圈特效结束，时间不能太短
            Thread.sleep(time+2000+random.nextInt(100));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    //寻找下一个方块
    private static Point findStage(Mat image,Point player_Point){
        // 背景色置黑
        for (int row = 0; row < image.rows(); row++) {
            double[] stage_gray = image.get(row,0);
            for (int col = 0; col < image.cols(); col++) {
                if((Math.abs(image.get(row,col)[0] - stage_gray[0]) < 4) &&
                   (Math.abs(image.get(row,col)[1] - stage_gray[1]) < 4) &&
                   (Math.abs(image.get(row,col)[2] - stage_gray[2]) < 4)){
                    image.put(row,col,0,0,0);
                }
            }
        }
        //转为灰度图
        Imgproc.cvtColor(image,image,Imgproc.COLOR_BGR2GRAY);

        //拉普拉斯锐化，凸显边缘
        Mat dest = new Mat();
        Imgproc.Laplacian(image,dest,image.depth(),3);
        Core.add(image,dest,image);
        Imgcodecs.imwrite("C:/logs/ruihua.png",image);

        // 边缘检测
        Imgproc.Canny(image,image,50,128);
        Imgcodecs.imwrite("C:/logs/stage_edge.png",image);

        boolean isBreak = false;
        Point point = new Point();

        for (int row = 420; row < image.rows(); row++) {
            for (int col = 10; col < image.cols(); col++) {

                //对于下一个方块位置低于操控人物的位置时，不进行比较
                if(col > player_Point.x-70 && col < player_Point.x+70){
                    continue;
                }

                //颜色发生突变，黑色背景与白色方块的界限
                if(image.get(row,col)[0] > 0 ){
                    point.x = col;
                    point.y = row;
                    isBreak = true;
                    break;
                }
            }
            if(isBreak){
                break;
            }
        }
        int sum = 0;
        int count = 0;
        int row3;
        //垂直查找方块位置
        for (int i = point.y; i < player_Point.y; i++) {
            if(image.get(i, point.x)[0] > 0){
                sum+=i;
                count++;
            }
        }
        //平均求出中点
        row3 = sum/count;
        point.y = point.y+(row3-point.y)/2;
        return point;
    }

    // 寻找玩家位置
    private static Point findPlayer(Mat image){
        double[] player_bgr = new double[]{100,58,56};
        boolean isFind = false;
        Point point = new Point();
        for (int row = image.height()-50; row >= 420 ; row--) {

            for (int col = 0; col < image.width(); col++) {
                double[] bgr = image.get(row,col);
                if(Math.abs(bgr[0]-player_bgr[0]) < 3 &&
                   Math.abs(bgr[1]-player_bgr[1]) < 3 &&
                   Math.abs(bgr[2]-player_bgr[2]) < 3){
                    point.x = col;
                    point.y = row;
                    isFind = true;
                    break ;
                }
            }
            if(isFind){
                point.y = point.y - 15;
                break;
            }
        }
        return isFind ? point : null;
    }
}


