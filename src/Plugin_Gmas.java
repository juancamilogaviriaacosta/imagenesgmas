import gmas.Estadistica;
import ij.*;
import ij.plugin.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author juan
 */
public class Plugin_Gmas implements PlugIn {
    
    private double[] data;
    
    @Override
    public void run(String string) {
        ImagePlus impMin = IJ.getImage();
        ImagePlus impMax = IJ.getImage();
        
        ZProjector zpMin = new ZProjector(impMin);
        zpMin.setStartSlice(11);
        zpMin.setStopSlice(20);
        zpMin.setMethod(ZProjector.MIN_METHOD);
        zpMin.doRGBProjection();
        ImagePlus pMin = zpMin.getProjection();
        //pMin.show();

        ZProjector zpMax = new ZProjector(impMax);
        zpMax.setStartSlice(11);
        zpMax.setStopSlice(20);
        zpMax.setMethod(ZProjector.MAX_METHOD);
        zpMax.doRGBProjection();
        ImagePlus pMax = zpMax.getProjection();
        //pMax.show();

        int umbral = 25;
        BufferedImage umbralizada = umbralizarPorDelta(umbral, pMin.getBufferedImage(), pMax.getBufferedImage());
        ImagePlus iumbral = new ImagePlus(String.valueOf(umbral), umbralizada);
        iumbral.show();
    }

    private BufferedImage umbralizarPorDelta(int umbral, BufferedImage bImin, BufferedImage bImax) {
        int width = bImin.getWidth();
        int height = bImin.getHeight();
        int[][] result = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color c1 = new Color(bImin.getRGB(i, j));
                Color c2 = new Color(bImax.getRGB(i, j));
                int p1 = (c1.getRed() + c1.getGreen() + c1.getBlue()) / 3;
                int p2 = (c2.getRed() + c2.getGreen() + c2.getBlue()) / 3;
                if (p2 - p1 > umbral && esGris(c2)/*!esDorado(bImax.getRGB(i, j))*/) {
                    result[i][j] = bImax.getRGB(i, j);
                }
            }
        }
        
        //TODO optimizar, se podria hacer en el ciclo anterior
        BufferedImage salida = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                salida.setRGB(i, j, result[i][j]);
            }
        }
        return salida;
    }
    
    public boolean esGris(Color color) {
        prepararEstadistica(color.getRed(), color.getGreen(), color.getBlue());
        return getDesviacionEstandar() < 16;
    }

    public void prepararEstadistica(double ... data) {
        this.data = data;
    }

    public double getMedia() {
        double sum = 0.0;
        for (double a : data) {
            sum += a;
        }
        return sum / data.length;
    }

    public double getVarianza() {
        double mean = getMedia();
        double temp = 0;
        for (double a : data) {
            temp += (mean - a) * (mean - a);
        }
        return temp / data.length;
    }

    public double getDesviacionEstandar() {
        return Math.sqrt(getVarianza());
    }

    public double getMediana() {
        Arrays.sort(data);

        if (data.length % 2 == 0) {
            return (data[(data.length / 2) - 1] + data[data.length / 2]) / 2.0;
        } else {
            return data[data.length / 2];
        }
    }
    
    
}
