import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageCompression {

    public static void main(String[] args) {

        int width = 512;
        int height = 512;
        float pi = 3.14159f;

        if (args.length != 2) {
            System.out.println("Expected argument count error.");
            System.exit(0);
        }

        int numOfCoeff = Integer.parseInt(args[1]);
        int firstM = (int)((double)numOfCoeff / 4096.0 + 0.5);

        BufferedImage img = new BufferedImage(width * 2 + 20, height, BufferedImage.TYPE_INT_RGB);

        byte[][] redBytes = new byte[width][height];
        byte[][] greenBytes = new byte[width][height];
        byte[][] blueBytes = new byte[width][height];

        byte[][] redBytesDCT = new byte[width][height];
        byte[][] greenBytesDCT = new byte[width][height];
        byte[][] blueBytesDCT = new byte[width][height];

        byte[][] redBytesDWT = new byte[width][height];
        byte[][] greenBytesDWT = new byte[width][height];
        byte[][] blueBytesDWT = new byte[width][height];

        try {
            File file = new File(args[0]);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int)len];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){

                    byte r = bytes[y*width+x];
                    byte g = bytes[y*width+x+height*width];
                    byte b = bytes[y*width+x+height*width*2];

                    redBytes[x][y] = r;
                    greenBytes[x][y] = g;
                    blueBytes[x][y] = b;

                    //int currentPixel = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //img.setRGB(x,y,currentPixel);

                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Discrete Cosine Transform:
        int blockedWidth = (int)((float)width / 8.0 + 0.5);
        int blockedHeight = (int)((float)height / 8.0 + 0.5);
        for (int j = 0; j < blockedHeight; j++) {
            for (int i = 0; i < blockedWidth; i++) {

                double[][] currentBlockRed = new double[8][8];
                double[][] currentBlockGreen = new double[8][8];
                double[][] currentBlockBlue = new double[8][8];

                double[][] freqCoeffRed = new double[8][8];
                double[][] freqCoeffGreen = new double[8][8];
                double[][] freqCoeffBlue = new double[8][8];

                for (int n = 0; n < 8; n++) {
                    for (int m = 0; m < 8; m++) {
                        currentBlockRed[m][n] = (double)((int)(redBytes[i*8+m][j*8+n] & 0xff));
                        currentBlockGreen[m][n] = (double)((int)(greenBytes[i*8+m][j*8+n] & 0xff));
                        currentBlockBlue[m][n] = (double)((int)(blueBytes[i*8+m][j*8+n] & 0xff));
                    }
                }

                // DCT Encoding:
                for (int v = 0; v < 8; v++) {
                    for (int u = 0; u < 8; u++) {

                        freqCoeffRed[u][v] = 0;
                        freqCoeffGreen[u][v] = 0;
                        freqCoeffBlue[u][v] = 0;

                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 8; x++) {
                                freqCoeffRed[u][v] += (currentBlockRed[x][y])
                                        * Math.cos((2.0 * (double)x + 1.0) * (double)u * Math.PI / 16.0)
                                        * Math.cos((2.0 * (double)y + 1.0) * (double)v * Math.PI / 16.0);
                                freqCoeffGreen[u][v] += (currentBlockGreen[x][y])
                                        * Math.cos((2.0 * (double)x + 1.0) * (double)u * Math.PI / 16.0)
                                        * Math.cos((2.0 * (double)y + 1.0) * (double)v * Math.PI / 16.0);
                                freqCoeffBlue[u][v] += (currentBlockBlue[x][y])
                                        * Math.cos((2.0 * (double)x + 1.0) * (double)u * Math.PI / 16.0)
                                        * Math.cos((2.0 * (double)y + 1.0) * (double)v * Math.PI / 16.0);
                            }
                        }
                        freqCoeffRed[u][v] *= 0.25;
                        freqCoeffGreen[u][v] *= 0.25;
                        freqCoeffBlue[u][v] *= 0.25;
                        if (u == 0 && v == 0) {
                            freqCoeffRed[u][v] *= 0.5;
                            freqCoeffGreen[u][v] *= 0.5;
                            freqCoeffBlue[u][v] *= 0.5;
                        }
                        /*
                        if (u == 0) {
                            freqCoeffRed[u][v] *= 1 / Math.sqrt(2.0);
                            freqCoeffGreen[u][v] *= 1 / Math.sqrt(2.0);
                            freqCoeffBlue[u][v] *= 1 / Math.sqrt(2.0);
                        }
                        if (v == 0) {
                            freqCoeffRed[u][v] *= 1 / Math.sqrt(2.0);
                            freqCoeffGreen[u][v] *= 1 / Math.sqrt(2.0);
                            freqCoeffBlue[u][v] *= 1 / Math.sqrt(2.0);
                        }
                        */
                    }
                }

                // DCT Decoding:
                for (int y = 0; y < 8; y++) {
                    for (int x = 0; x < 8; x++) {

                        currentBlockRed[x][y] = 0;
                        currentBlockGreen[x][y] = 0;
                        currentBlockBlue[x][y] = 0;

                        //int index = firstM;
                        for (int v = 0; v < 8; v++) {
                            for (int u = 0; u < 8; u++) {
                                if (u == 0 && v == 0) {
                                    currentBlockRed[x][y] += 0.5 * freqCoeffRed[u][v]
                                            * Math.cos((2.0 * (double) x + 1.0) * (double) u * Math.PI / 16.0)
                                            * Math.cos((2.0 * (double) y + 1.0) * (double) v * Math.PI / 16.0);
                                    currentBlockGreen[x][y] += 0.5 * freqCoeffGreen[u][v]
                                            * Math.cos((2.0 * (double) x + 1.0) * (double) u * Math.PI / 16.0)
                                            * Math.cos((2.0 * (double) y + 1.0) * (double) v * Math.PI / 16.0);
                                    currentBlockBlue[x][y] += 0.5 * freqCoeffBlue[u][v]
                                            * Math.cos((2.0 * (double) x + 1.0) * (double) u * Math.PI / 16.0)
                                            * Math.cos((2.0 * (double) y + 1.0) * (double) v * Math.PI / 16.0);
                                } else {
                                    currentBlockRed[x][y] += freqCoeffRed[u][v]
                                            * Math.cos((2.0 * (double) x + 1.0) * (double) u * Math.PI / 16.0)
                                            * Math.cos((2.0 * (double) y + 1.0) * (double) v * Math.PI / 16.0);
                                    currentBlockGreen[x][y] += freqCoeffGreen[u][v]
                                            * Math.cos((2.0 * (double) x + 1.0) * (double) u * Math.PI / 16.0)
                                            * Math.cos((2.0 * (double) y + 1.0) * (double) v * Math.PI / 16.0);
                                    currentBlockBlue[x][y] += freqCoeffBlue[u][v]
                                            * Math.cos((2.0 * (double) x + 1.0) * (double) u * Math.PI / 16.0)
                                            * Math.cos((2.0 * (double) y + 1.0) * (double) v * Math.PI / 16.0);
                                }
                                /*
                                if (u == 0) {
                                    currentBlockRed[x][y] *= 1 / Math.sqrt(2.0);
                                    currentBlockGreen[x][y] *= 1 / Math.sqrt(2.0);
                                    currentBlockBlue[x][y] *= 1 / Math.sqrt(2.0);
                                }
                                if (v == 0) {
                                    currentBlockRed[x][y] *= 1 / Math.sqrt(2.0);
                                    currentBlockGreen[x][y] *= 1 / Math.sqrt(2.0);
                                    currentBlockBlue[x][y] *= 1 / Math.sqrt(2.0);
                                }
                                */
                                //index--;
                            }
                            //if (index <= 0)
                            //    break;
                        }

                        currentBlockRed[x][y] *= 0.25;
                        currentBlockGreen[x][y] *= 0.25;
                        currentBlockBlue[x][y] *= 0.25;
                    }
                }

                for (int n = 0; n < 8; n++) {
                    for (int m = 0; m < 8; m++) {
                        if (currentBlockRed[m][n] < 0)
                            currentBlockRed[m][n] = 0;
                        if (currentBlockRed[m][n] > 255.0)
                            currentBlockRed[m][n] = 255.0;
                        if (currentBlockGreen[m][n] < 0)
                            currentBlockGreen[m][n] = 0;
                        if (currentBlockGreen[m][n] > 255.0)
                            currentBlockGreen[m][n] = 255.0;
                        if (currentBlockBlue[m][n] < 0)
                            currentBlockBlue[m][n] = 0;
                        if (currentBlockBlue[m][n] > 255.0)
                            currentBlockBlue[m][n] = 255.0;

                        redBytesDCT[i*8+m][j*8+n] = (byte)((int)(currentBlockRed[m][n] + 0.5));
                        greenBytesDCT[i*8+m][j*8+n] = (byte)((int)(currentBlockGreen[m][n] + 0.5));
                        blueBytesDCT[i*8+m][j*8+n] = (byte)((int)(currentBlockBlue[m][n] + 0.5));
                    }
                }
                //System.out.println(i + " and " + j + ";");
            }   // for i
        }   // for j

        // Discrete Wavelet Transform:
        int bound = Math.max(width, height);
        int numOfStage = (int)(Math.log((double)bound) / Math.log(2.0) + 0.5);
        double[][] coeffDWTRed = new double[width][height];
        double[][] coeffDWTGreen = new double[width][height];
        double[][] coeffDWTBlue = new double[width][height];

        for (int n = 0; n < height; n++) {
            for (int m = 0; m < width; m++) {
                coeffDWTRed[m][n] = (double)((int)(redBytes[m][n] & 0xff));
                coeffDWTGreen[m][n] = (double)((int)(greenBytes[m][n] & 0xff));
                coeffDWTBlue[m][n] = (double)((int)(blueBytes[m][n] & 0xff));
            }
        }

        for (int stage = 1; stage <= numOfStage; stage++) {
            int llWidth = (int)((double)width / Math.pow(2.0, (double)stage));
            // Need to perform a simple deep copy:
            double[][] tempR = new double[width][height];
            double[][] tempG = new double[width][height];
            double[][] tempB = new double[width][height];
            for (int n = 0; n < height; n++) {
                for (int m = 0; m < width; m++) {
                    tempR[m][n] = coeffDWTRed[m][n];
                    tempG[m][n] = coeffDWTGreen[m][n];
                    tempB[m][n] = coeffDWTBlue[m][n];
                }
            }
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < llWidth; i++) {
                    coeffDWTRed[i][j] = (tempR[2*i][j] + tempR[2*i+1][j]) / 2.0;
                    coeffDWTRed[llWidth+i][j] = (tempR[2*i][j] - tempR[2*i+1][j]) / 2.0;
                    coeffDWTGreen[i][j] = (tempG[2*i][j] + tempG[2*i+1][j]) / 2.0;
                    coeffDWTGreen[llWidth+i][j] = (tempG[2*i][j] - tempG[2*i+1][j]) / 2.0;
                    coeffDWTBlue[i][j] = (tempB[2*i][j] + tempB[2*i+1][j]) / 2.0;
                    coeffDWTBlue[llWidth+i][j] = (tempB[2*i][j] - tempB[2*i+1][j]) / 2.0;
                }
            }
        }

        for (int stage = 1; stage <= numOfStage; stage++) {
            int llHeight = (int)((double)height / Math.pow(2.0, (double)stage));
            double[][] tempR = new double[width][height];
            double[][] tempG = new double[width][height];
            double[][] tempB = new double[width][height];
            for (int n = 0; n < height; n++) {
                for (int m = 0; m < width; m++) {
                    tempR[m][n] = coeffDWTRed[m][n];
                    tempG[m][n] = coeffDWTGreen[m][n];
                    tempB[m][n] = coeffDWTBlue[m][n];
                }
            }
            for (int j = 0; j < llHeight; j++) {
                for (int i = 0; i < width; i++) {
                    coeffDWTRed[i][j] = (tempR[i][2*j] + tempR[i][2*j+1]) / 2.0;
                    coeffDWTRed[i][llHeight+j] = (tempR[i][2*j] - tempR[i][2*j+1]) / 2.0;
                    coeffDWTGreen[i][j] = (tempG[i][2*j] + tempG[i][2*j+1]) / 2.0;
                    coeffDWTGreen[i][llHeight+j] = (tempG[i][2*j] - tempG[i][2*j+1]) / 2.0;
                    coeffDWTBlue[i][j] = (tempB[i][2*j] + tempB[i][2*j+1]) / 2.0;
                    coeffDWTBlue[i][llHeight+j] = (tempB[i][2*j] - tempB[i][2*j+1]) / 2.0;
                }
            }
        }

        // DWT Decoding:
        double[][] coeffDWTDecRed = new double[width][height];
        double[][] coeffDWTDecGreen = new double[width][height];
        double[][] coeffDWTDecBlue = new double[width][height];

        for (int n = 0; n < height; n++) {
            for (int m = 0; m < width; m++) {
                coeffDWTDecRed[m][n] = 0;
                coeffDWTDecGreen[m][n] = 0;
                coeffDWTDecBlue[m][n] = 0;
            }
        }

        // Perform Zig-zag traverse:
        int ii= 0, jj = 0;
        int totalCount = 0;
        boolean forward = true, turning = true;
        while (totalCount < numOfCoeff) {
            if (turning && jj == 0 && ii < width - 1) {
                coeffDWTDecRed[ii][jj] = coeffDWTRed[ii][jj];
                coeffDWTDecGreen[ii][jj] = coeffDWTGreen[ii][jj];
                coeffDWTDecBlue[ii][jj] = coeffDWTBlue[ii][jj];
                totalCount++;
                ii++;
                forward = false;
                turning = false;
                continue;
            }
            else if (turning && (jj == 0 || ii == width - 1)) {
                coeffDWTDecRed[ii][jj] = coeffDWTRed[ii][jj];
                coeffDWTDecGreen[ii][jj] = coeffDWTGreen[ii][jj];
                coeffDWTDecBlue[ii][jj] = coeffDWTBlue[ii][jj];
                totalCount++;
                jj++;
                forward = false;
                turning = false;
                continue;
            }
            if (turning && ii == 0 && jj < height - 1) {
                coeffDWTDecRed[ii][jj] = coeffDWTRed[ii][jj];
                coeffDWTDecGreen[ii][jj] = coeffDWTGreen[ii][jj];
                coeffDWTDecBlue[ii][jj] = coeffDWTBlue[ii][jj];
                totalCount++;
                jj++;
                forward = true;
                turning = false;
                continue;
            }
            else if (turning && (ii == 0 || jj == height - 1)) {
                coeffDWTDecRed[ii][jj] = coeffDWTRed[ii][jj];
                coeffDWTDecGreen[ii][jj] = coeffDWTGreen[ii][jj];
                coeffDWTDecBlue[ii][jj] = coeffDWTBlue[ii][jj];
                totalCount++;
                ii++;
                forward = true;
                turning = false;
                continue;
            }
            if (forward) {
                coeffDWTDecRed[ii][jj] = coeffDWTRed[ii][jj];
                coeffDWTDecGreen[ii][jj] = coeffDWTGreen[ii][jj];
                coeffDWTDecBlue[ii][jj] = coeffDWTBlue[ii][jj];
                totalCount++;
                if (jj == 1 || ii == width - 2)
                    turning = true;
                ii++;
                jj--;
            }
            else {
                coeffDWTDecRed[ii][jj] = coeffDWTRed[ii][jj];
                coeffDWTDecGreen[ii][jj] = coeffDWTGreen[ii][jj];
                coeffDWTDecBlue[ii][jj] = coeffDWTBlue[ii][jj];
                totalCount++;
                if (ii == 1 || jj == height - 2)
                    turning = true;
                ii--;
                jj++;
            }
        }

        for (int stage = numOfStage; stage >= 1; stage--) {
            int llHeight = (int)((double)height / Math.pow(2.0, (double)stage));
            double[][] tempR = new double[width][height];
            double[][] tempG = new double[width][height];
            double[][] tempB = new double[width][height];
            for (int n = 0; n < height; n++) {
                for (int m = 0; m < width; m++) {
                    tempR[m][n] = coeffDWTDecRed[m][n];
                    tempG[m][n] = coeffDWTDecGreen[m][n];
                    tempB[m][n] = coeffDWTDecBlue[m][n];
                }
            }
            for (int j = 0; j < llHeight; j++) {
                for (int i = 0; i < width; i++) {
                    coeffDWTDecRed[i][2*j] = tempR[i][j] + tempR[i][j+llHeight];
                    coeffDWTDecRed[i][2*j+1] = tempR[i][j] - tempR[i][j+llHeight];
                    coeffDWTDecGreen[i][2*j] = tempG[i][j] + tempG[i][j+llHeight];
                    coeffDWTDecGreen[i][2*j+1] = tempG[i][j] - tempG[i][j+llHeight];
                    coeffDWTDecBlue[i][2*j] = tempB[i][j] + tempB[i][j+llHeight];
                    coeffDWTDecBlue[i][2*j+1] = tempB[i][j] - tempB[i][j+llHeight];
                }
            }
        }

        for (int stage = numOfStage; stage >= 1; stage--) {
            int llWidth = (int)((double)width / Math.pow(2.0, (double)stage));
            double[][] tempR = new double[width][height];
            double[][] tempG = new double[width][height];
            double[][] tempB = new double[width][height];
            for (int n = 0; n < height; n++) {
                for (int m = 0; m < width; m++) {
                    tempR[m][n] = coeffDWTDecRed[m][n];
                    tempG[m][n] = coeffDWTDecGreen[m][n];
                    tempB[m][n] = coeffDWTDecBlue[m][n];
                }
            }
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < llWidth; i++) {
                    coeffDWTDecRed[2*i][j] = tempR[i][j] + tempR[i+llWidth][j];
                    coeffDWTDecRed[2*i+1][j] = tempR[i][j] - tempR[i+llWidth][j];
                    coeffDWTDecGreen[2*i][j] = tempG[i][j] + tempG[i+llWidth][j];
                    coeffDWTDecGreen[2*i+1][j] = tempG[i][j] - tempG[i+llWidth][j];
                    coeffDWTDecBlue[2*i][j] = tempB[i][j] + tempB[i+llWidth][j];
                    coeffDWTDecBlue[2*i+1][j] = tempB[i][j] - tempB[i+llWidth][j];
                }
            }
        }

        for (int n = 0; n < height; n++) {
            for (int m = 0; m < width; m++) {
                if (coeffDWTDecRed[m][n] < 0)
                    coeffDWTDecRed[m][n] = 0;
                if (coeffDWTDecRed[m][n] > 255.0)
                    coeffDWTDecRed[m][n] = 255.0;
                if (coeffDWTDecGreen[m][n] < 0)
                    coeffDWTDecGreen[m][n] = 0;
                if (coeffDWTDecGreen[m][n] > 255.0)
                    coeffDWTDecGreen[m][n] = 255.0;
                if (coeffDWTDecBlue[m][n] < 0)
                    coeffDWTDecBlue[m][n] = 0;
                if (coeffDWTDecBlue[m][n] > 255.0)
                    coeffDWTDecBlue[m][n] = 255.0;

                redBytesDWT[m][n] = (byte)((int)(coeffDWTDecRed[m][n] + 0.5));
                greenBytesDWT[m][n] = (byte)((int)(coeffDWTDecGreen[m][n] + 0.5));
                blueBytesDWT[m][n] = (byte)((int)(coeffDWTDecBlue[m][n] + 0.5));
            }
        }


        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++) {
                int currentPixel = 0xff000000 | ((redBytesDCT[x][y] & 0xff) << 16) | ((greenBytesDCT[x][y] & 0xff) << 8) | (blueBytesDCT[x][y] & 0xff);
                img.setRGB(x, y, currentPixel);

                currentPixel = 0xff000000 | ((redBytesDWT[x][y] & 0xff) << 16) | ((greenBytesDWT[x][y] & 0xff) << 8) | (blueBytesDWT[x][y] & 0xff);
                img.setRGB(x + width + 20, y, currentPixel);
            }
        }



        // Use a panel and label to display the image
        JPanel  panel = new JPanel ();
        panel.add (new JLabel (new ImageIcon (img)));

        JFrame frame = new JFrame("Image Compression DCT & DWT");
        frame.getContentPane().add (panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



    }
}
