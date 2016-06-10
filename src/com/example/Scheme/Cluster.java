package com.example.Scheme;


import android.graphics.Bitmap;
import java.io.*;

import static java.lang.Math.ceil;


public class Cluster {
    private static final long serialVersionUID = 1L;
    public static int seedNum = 5;
    private Node[] imageNodes;

    //private BufferedImage image;//
    private Bitmap image;

    private int width, height;
    private int mosaicHeight, mosaicWidth;
    public Node[][] schemeNodes;
    private Node[] seed;
    private int index;
    private int counter;

    public int getIndex() {
        return  index;
    }
    public int getSeedNum() {
        return seedNum;
    }


    public void readColors() {
        counter = 0;
        for (int i = 0; i < width; i += mosaicWidth) {
            for (int j = 0; j < height; j += mosaicHeight) {
                int pixel = image.getPixel(i, j);
                int r = (pixel & 0xff0000) >> 16;
                int g = (pixel & 0xff00) >> 8;
                int b = (pixel & 0xff);
                double c = (double)(255 - r) / 255;
                double m = (double)(255 - g) / 255;
                double y = (double)(255 - b) / 255;
                double k = (double)Math.min(c, Math.min(m, y));

//                System.out.println(k);
                if (k > 0.5) continue;
                imageNodes[counter].r = r;
                imageNodes[counter].g = g;
                imageNodes[counter].b = b;
//                System.out.println(r+" "+g+" "+b+" "+(r+g+b)/3);
                counter++;
            }
        }
    }

    public boolean setImage(Bitmap b) {
        if(b != null) {
            image = b;
            return true;
        }
        return false;
    }

    private double pow(int n, int r) {
        int ans = 1;
        for (int i = 0; i < r; i++) {
            ans *= n;
        }
        return ans;
    }

    private double distance(Node a, Node b) {
        double sum = 0;
        sum += pow((a.r - b.r), 2);
        sum += pow((a.g - b.g), 2);
        sum += pow((a.b - b.b), 2);
        sum = Math.sqrt(sum);
        return sum;
    }

    public void initSeed() {
        //read image
        //image = ImageIO.read(new File(imageName));
        //get width and height
        width = image.getWidth();
        height = image.getHeight();
        mosaicHeight = 20;
        mosaicWidth = 20;
        //initial imageRGB
        imageNodes = new Node[width*height/mosaicWidth];
        for (int i = 0; i < width*height/mosaicWidth; i++) {
            imageNodes[i] = new Node();
            imageNodes[i].rank = -1;
        }
        //initial seed
        seed = new Node[seedNum];
        for (int i = 0; i < seedNum; i++) {
            seed[i] = new Node();
        }
        //get color information in image
        readColors();
        //initial seed

        int first = (int)(width / mosaicWidth * (0.25 * height / mosaicHeight) + width / mosaicWidth * 0.25);
        int delta = (int) ceil(width / mosaicWidth / 2);
        for (int i = 0; i < seedNum; i++) {
            final double d = Math.random();
            int sele = (int)(d * counter);
//            seed[i] = imageNodes[sele];
            seed[i] = imageNodes[first];
            first += delta;
//            seed[i].print();
        }

//        System.out.println("seed");
//        for (int i = 0; i < seedNum; i++) {
//            seed[i].print();
//        }
//        System.out.println("----------");

        for (int i = 0; i < seedNum; i++) {
            seed[i].rank = i;
        }

        //initial index
        index = 0;
        //initial SchemeColor
        schemeNodes = new Node[10][seedNum];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < seedNum; j++) {
                schemeNodes[i][j] = new Node();
            }
        }
        //the first schemeColors is seed
        for (int i = 0; i < seedNum; i++) {
            schemeNodes[index][i] = seed[i];
        }
        index++;
    }

    public void updateSeed() {
        int r[] = new int[seedNum];
        int g[] = new int[seedNum];
        int b[] = new int[seedNum];
        int num[] = new int[seedNum];
        for (int i = 0; i < seedNum; i++) {
            r[i] = 0;
            g[i] = 0;
            b[i] = 0;
            num[i] = 0;
        }
        for (int i = 0; i < seedNum; i++) {
            for (int j = 0; j < counter; j++) {
                if (imageNodes[j].rank != i) continue;
                r[i] += imageNodes[j].r;
                g[i] += imageNodes[j].g;
                b[i] += imageNodes[j].b;
                num[i]++;
            }
        }
        for (int i = 0; i < seedNum; i++) {
            if (num[i] > 0) {
                r[i] /= num[i];
                g[i] /= num[i];
                b[i] /= num[i];
            }
        }

        Node newNodes[] = new Node[seedNum];
        for (int i = 0; i < seedNum; i++) {
            newNodes[i] = new Node();
        }
        for (int i = 0; i < seedNum; i++) {
            newNodes[i].r = r[i];
            newNodes[i].g = g[i];
            newNodes[i].b = b[i];
        }

        for (int i = 0; i < seedNum; i++) {
            double minD = distance(imageNodes[0], newNodes[i]);
            for (int j = 0; j < counter; j++) {
                if (imageNodes[j].rank != i) continue;
                double dis = distance(imageNodes[j], newNodes[i]);
                if (dis < minD) {
                    minD = dis;
                    seed[i] = imageNodes[j];
                }
            }
        }

        for (int i = 0; i < seedNum; i++) {
            schemeNodes[index][i] = seed[i];
        }
        index++;
    }

    public void updateCluster() {
        for (int i = 0; i < counter; i++) {
            double minD = distance(seed[0], imageNodes[i]);
            int flag = 0;
            for (int j = 1; j < seedNum; j++) {
                double dis = distance(seed[j], imageNodes[i]);
                if (dis < minD) {
                    minD = dis;
                    flag = j;
                }
            }
            imageNodes[i].rank = flag;
        }
    }

//    @SuppressWarnings("deprecation")
//    public Cluster() {
//        super("Using colors");
//        setSize(400, 400);
//    }

//    public void paint(Graphics g) {
//        for (int i = 0; i < index; i++) {
//            for (int j = 0; j < seedNum; j++) {
//                g.setColor(new Color(schemeNodes[i][j].r,
//                        schemeNodes[i][j].g,
//                        schemeNodes[i][j].b));
//                g.fillRect(50 + 50*j, 50 + 55*i, 50, 50);
//            }
//        }
   // }

    public static void main(String[] args) throws IOException {
        Cluster cluster = new Cluster();

//        cluster.addWindowListener(
//                new WindowAdapter() {
//                    public void windowClosing (WindowEvent e) {
//                        System.exit(0);
//                    }
//                });

        String imageName = "3.jpg";
        cluster.initSeed();
        cluster.updateCluster();

        for (int i = 0; i < seedNum; i++) {
            cluster.updateSeed();
            cluster.updateCluster();
        }

//        for (int k = 0; k < cluster.index; k++) {
//            System.out.println(k);
//            for (int j = 0; j < seedNum; j++) {
//                cluster.schemeNodes[k][j].print();
//            }
//            System.out.println();
//        }
        //cluster.show();
    }
}

