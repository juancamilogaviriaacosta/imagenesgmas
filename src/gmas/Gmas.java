/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gmas;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 *
 * @author juan
 */
public class Gmas {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            File f1 = new File("/home/juan/Escritorio/stacks-sedimentaria/pila-fd/MIN2.jpg");
            File f2 = new File("/home/juan/Escritorio/stacks-sedimentaria/pila-fd/MAX2.jpg");
            BufferedImage i1 = ImageIO.read(f1.toURL());
            BufferedImage i2 = ImageIO.read(f2.toURL());
            int[][] resp = convertTo2DUsingGetRGB(i1, i2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int[][] convertTo2DUsingGetRGB(BufferedImage i1, BufferedImage i2) throws Exception {
        int width = i1.getWidth();
        int height = i2.getHeight();
        int[][] result = new int[width][height];
        int umbral = 12;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color c1 = new Color(i1.getRGB(i, j));
                Color c2 = new Color(i2.getRGB(i, j));
                int p1 = (c1.getRed() + c1.getGreen() + c1.getBlue()) / 3;
                int p2 = (c2.getRed() + c2.getGreen() + c2.getBlue()) / 3;
                if (p2 - p1 > umbral && !esDorado(c2)) {
                    result[i][j] = i2.getRGB(i, j);
                }
            }
        }

        BufferedImage salida = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                salida.setRGB(i, j, result[i][j]);
            }
        }
        ImageIO.write(salida, "jpg", new File("/home/juan/Escritorio/" + umbral + ".jpg"));
        return result;
    }

    private static boolean esDorado(Color c1) {
        int abajo = 100;
        int arriba = 100;
        return ((213-abajo) <= c1.getRed() && c1.getRed() <= (213 + arriba)) && ((192-abajo) <= c1.getGreen() && c1.getGreen() <= (192 + arriba)) && ((136-abajo) <= c1.getBlue() && c1.getBlue() <= (136 + arriba));
    }
}
