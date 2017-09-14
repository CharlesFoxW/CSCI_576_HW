import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageReSampler {

    public static void main(String[] args)
    {

        if (args.length != 5) {
            System.out.println("Expected argument count error.");
            System.exit(0);
        }

        //String fileName = args[0];
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        int mode = Integer.parseInt(args[3]);
        int targetWidth = 0, targetHeight = 0;
        String res = args[4];
        if (mode != 1 && mode != 2) {
            System.out.println("Expected algorithm mode: 1 or 2.");
            System.exit(0);
        }

        if ("O1".equals(res)) {
            targetWidth = 1920;
            targetHeight = 1080;
        } else if ("O2".equals(res)) {
            targetWidth = 1280;
            targetHeight = 720;
        } else if ("O3".equals(res)) {
            targetWidth = 640;
            targetHeight = 480;
        } else {
            System.out.println("Expected resolution mode: O1, O2 or O3.");
            System.exit(0);
        }

        BufferedImage img = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

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

            byte[] redBytes = new byte[width*height];
            byte[] greenBytes = new byte[width*height];
            byte[] blueBytes = new byte[width*height];

            int index = 0;
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){

                    byte r = bytes[index];
                    byte g = bytes[index+height*width];
                    byte b = bytes[index+height*width*2];

                    redBytes[index] = r;
                    greenBytes[index] = g;
                    blueBytes[index] = b;
                    index++;
                }
            }


            if (targetWidth < width && targetHeight < height) { // Down-Sampling
                if (mode == 1) {    // Specific Sampling
                    float widthStep = (float)width / (float)targetWidth;
                    float heightStep = (float)height / (float)targetHeight;

                    for (int j = 0; j < targetHeight; j++) {
                        for (int i = 0; i < targetWidth; i++) {
                            float currentHeight = (float)j * heightStep;
                            float currentWidth = (float)i * widthStep;
                            // Project the index from target output space into the space of original image.
                            int currentIndex = (int)(currentHeight + 0.5) * width + (int)(currentWidth + 0.5);
                            int currentPixel = 0xff000000 | ((redBytes[currentIndex] & 0xff) << 16) | ((greenBytes[currentIndex] & 0xff) << 8)
                                    | (blueBytes[currentIndex] & 0xff);
                            img.setRGB(i,j,currentPixel);

                        }
                    }


                } else if (mode == 2) { // Gaussian Smoothing with sigma = inf (pixel average)
                    float widthStep = (float)width / (float)targetWidth;
                    float heightStep = (float)height / (float)targetHeight;
                    for (int j = 0; j < targetHeight; j++) {
                        for (int i = 0; i < targetWidth; i++) {
                            float currentHeight = (float)j * heightStep;
                            float currentWidth = (float)i * widthStep;
                            int currentIndex = (int)(currentHeight + 0.5) * width + (int)(currentWidth + 0.5);

                            int gridRightLimit = (int)((float)(i+1) * widthStep + 0.5);
                            int gridLeftLimit = (int)(currentWidth + 0.5);
                            int gridTopLimit = (int)(currentHeight + 0.5);
                            int gridBottomLimit = (int)((float)(j+1) * heightStep + 0.5) ;
                            int deltaX = gridRightLimit - gridLeftLimit;
                            int deltaY = gridBottomLimit - gridTopLimit;
                            int redSum = 0, greenSum = 0, blueSum = 0;
                            for (int v = 0; v < deltaY; v++) {
                                for (int u = 0; u < deltaX; u++) {
                                    int iterationIndex = currentIndex + v * width + u;
                                    // each time converts bytes to int - turn it to unsigned.
                                    redSum += redBytes[iterationIndex] & 0xff;
                                    greenSum += greenBytes[iterationIndex] & 0xff;
                                    blueSum += blueBytes[iterationIndex] & 0xff;
                                }
                            }
                            byte currentRed = (byte)(redSum / (deltaX * deltaY));
                            byte currentGreen = (byte)(greenSum / (deltaX * deltaY));
                            byte currentBlue = (byte)(blueSum / (deltaX * deltaY));
                            int currentPixel = 0xff000000 | ((currentRed & 0xff) << 16) | ((currentGreen & 0xff) << 8) | (currentBlue & 0xff);
                            img.setRGB(i,j,currentPixel);
                        }
                    }
                }


            } else if (targetWidth > width && targetHeight > height) {  // Up-Sampling

                if (mode == 1) {    // Nearest Neighbor:
                    float widthStep = (float) targetWidth / (float) width;
                    float heightStep = (float) targetHeight / (float) height;
                    for (int j = 0; j < targetHeight - heightStep / 2; j++) {
                        for (int i = 0; i < targetWidth - widthStep / 2; i++) {
                            int closestX = (int)(((float)i + widthStep / 2.0) / widthStep);
                            int closestY = (int)(((float)j + heightStep / 2.0) / heightStep);
                            int currentIndex = closestY * width + closestX;
                            int currentPixel = 0xff000000 | ((redBytes[currentIndex] & 0xff) << 16) | ((greenBytes[currentIndex] & 0xff) << 8)
                                    | (blueBytes[currentIndex] & 0xff);
                            img.setRGB(i,j,currentPixel);
                        }
                    }
                } else if (mode == 2) { //Bilinear:
                    float widthStep = (float) targetWidth / (float) width;
                    float heightStep = (float) targetHeight / (float) height;
                    for (int j = 0; j < targetHeight - heightStep; j++) {
                        for (int i = 0; i < targetWidth - widthStep; i++) {
                            int boundingX = (int)((float)i / widthStep);
                            int boundingY = (int)((float)j / heightStep);
                            float ratioX = ((float)i - (float)boundingX * widthStep) / widthStep;
                            float ratioY = ((float)j - (float)boundingY * heightStep) / heightStep;

                            int currentRedTop = (int)((float)(redBytes[boundingY*width+boundingX] & 0xff) * ratioX
                                    + (float)(redBytes[boundingY*width+boundingX+1] & 0xff) * (1.0f - ratioX));
                            int currentRedBottom = (int)((float)(redBytes[(boundingY+1)*width+boundingX] & 0xff) * ratioX
                                    + (float)(redBytes[(boundingY+1)*width+boundingX+1] & 0xff) * (1.0f - ratioX));
                            int currentRed = (int)((float)currentRedTop * ratioY + (float)currentRedBottom * (1 - ratioY));

                            int currentGreenTop = (int)((float)(greenBytes[boundingY*width+boundingX] & 0xff) * ratioX
                                    + (float)(greenBytes[boundingY*width+boundingX+1] & 0xff) * (1.0f - ratioX));
                            int currentGreenBottom = (int)((float)(greenBytes[(boundingY+1)*width+boundingX] & 0xff) * ratioX
                                    + (float)(greenBytes[(boundingY+1)*width+boundingX+1] & 0xff) * (1.0f - ratioX));
                            int currentGreen = (int)((float)currentGreenTop * ratioY + (float)currentGreenBottom * (1 - ratioY));

                            int currentBlueTop = (int)((float)(blueBytes[boundingY*width+boundingX] & 0xff) * ratioX
                                    + (float)(blueBytes[boundingY*width+boundingX+1] & 0xff) * (1.0f - ratioX));
                            int currentBlueBottom = (int)((float)(blueBytes[(boundingY+1)*width+boundingX] & 0xff) * ratioX
                                    + (float)(blueBytes[(boundingY+1)*width+boundingX+1] & 0xff) * (1.0f - ratioX));
                            int currentBlue = (int)((float)currentBlueTop * ratioY + (float)currentBlueBottom * (1 - ratioY));

                            int currentPixel = 0xff000000 | (currentRed << 16) | (currentGreen << 8) | currentBlue;
                            img.setRGB(i,j,currentPixel);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Use a panel and label to display the image
        JPanel  panel = new JPanel ();
        panel.add (new JLabel (new ImageIcon (img)));

        JFrame frame = new JFrame("Display images");

        frame.getContentPane().add (panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }



}
