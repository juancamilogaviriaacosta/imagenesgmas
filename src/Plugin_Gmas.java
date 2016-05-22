import ij.*;
import ij.gui.GenericDialog;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.*;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author juan
 */
public class Plugin_Gmas implements PlugIn {

    private double[] data;
    private double porosidad;
    private int numeroObjetos;

    @Override
    public void run(String string) {
        porosidad = 0;
        numeroObjetos = 0;
        int numColumnas = 10;
        GenericDialog gd = new GenericDialog("");
        gd.addStringField("Umbral de cambio", "30", numColumnas);
        gd.addStringField("Maxima diferencia entre canales", "15", numColumnas);
        gd.addStringField("Tamaño del filtro", "10", numColumnas);
        gd.addStringField("Dimensiones minimas de los objetos", "5000", numColumnas);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        int umbral = Integer.valueOf(gd.getNextString());
        int tolerancia = Integer.valueOf(gd.getNextString());
        int filtro = Integer.valueOf(gd.getNextString());
        int dimensionMinima = Integer.valueOf(gd.getNextString());

        ImagePlus impMin = IJ.getImage();
        ImagePlus impMax = IJ.getImage();
        ImagePlus impPlanaMax = IJ.getImage();

        ZProjector zpMin = new ZProjector(impMin);
        zpMin.setStartSlice(11);
        zpMin.setStopSlice(20);
        zpMin.setMethod(ZProjector.MIN_METHOD);
        zpMin.doRGBProjection();
        ImagePlus pMin = zpMin.getProjection();
        GaussianBlur gbMin = new GaussianBlur();
        gbMin.blurGaussian(pMin.getProcessor(), 2);
        //pMin.show();

        ZProjector zpMax = new ZProjector(impMax);
        zpMax.setStartSlice(11);
        zpMax.setStopSlice(20);
        zpMax.setMethod(ZProjector.MAX_METHOD);
        zpMax.doRGBProjection();
        ImagePlus pMax = zpMax.getProjection();
        GaussianBlur gbMax = new GaussianBlur();
        gbMax.blurGaussian(pMax.getProcessor(), 2);
        ImagePlus copiaMax = pMax.duplicate();
        //pMax.show();

        ZProjector zpPlanaMax = new ZProjector(impPlanaMax);
        zpPlanaMax.setStartSlice(11);
        zpPlanaMax.setStopSlice(20);
        zpPlanaMax.setMethod(ZProjector.MAX_METHOD);
        zpPlanaMax.doRGBProjection();
        ImagePlus pPlanaMax = zpPlanaMax.getProjection();
        GaussianBlur gbPlanaMax = new GaussianBlur();
        gbPlanaMax.blurGaussian(pPlanaMax.getProcessor(), 2);
        //pPlanaMax.show();

        BufferedImage umbralizada = umbralizarPorDelta(umbral, tolerancia, pMin.getBufferedImage(), pMax.getBufferedImage());
        ImagePlus iumbral = new ImagePlus(String.valueOf(umbral), umbralizada);

        ImageCalculator ic = new ImageCalculator();
        ic.run("and", pMax, iumbral);

        WindowManager.setTempCurrentImage(iumbral);

        IJ.run("Mexican Hat Filter Gmas");

        IJ.run("Make Binary");

        RankFilters rf = new RankFilters();
        rf.rank(iumbral.getProcessor(), filtro, RankFilters.MEDIAN, 0, 0);

        IJ.run("Fill Holes");

        IJ.run("Erode");

        IJ.run("Blob Labeler Gmas");

        ImagePlus etiquetado = IJ.getImage();

        ImagePlus sinRuido = eliminarRuido(etiquetado, dimensionMinima);

        ImagePlus sobreponer = sobreponer(sinRuido, copiaMax);

        porosidad = getPorosidad(pPlanaMax, copiaMax);

        sobreponer.show();

        GenericDialog gdr = new GenericDialog("Resultados");
        gdr.addMessage("Se encontraron " + numeroObjetos + " cuarzos");
        gdr.addMessage("Existe una porosidad de " + String.format("%.2f", porosidad) + "%");
        gdr.showDialog();
    }

    private BufferedImage umbralizarPorDelta(int umbral, int tolerancia, BufferedImage bImin, BufferedImage bImax) {
        int width = bImin.getWidth();
        int height = bImin.getHeight();
        int[][] result = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color c1 = new Color(bImin.getRGB(i, j));
                Color c2 = new Color(bImax.getRGB(i, j));
                int p1 = (c1.getRed() + c1.getGreen() + c1.getBlue()) / 3;
                int p2 = (c2.getRed() + c2.getGreen() + c2.getBlue()) / 3;
                if (p2 - p1 > umbral && esGris(c2, tolerancia)/*!esDorado(bImax.getRGB(i, j))*/) {
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

    public boolean esGris(Color color, int tolerancia) {
        prepararEstadistica(color.getRed(), color.getGreen(), color.getBlue());
        return getDesviacionEstandar() < tolerancia;
    }

    public void prepararEstadistica(double... data) {
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

    private ImagePlus eliminarRuido(ImagePlus imagen, int dimensionMinima) {
        ImageProcessor bImin = imagen.getProcessor();
        int width = bImin.getWidth();
        int height = bImin.getHeight();
        Map<Integer, Integer> mapaConteo = new HashMap();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = bImin.getPixel(i, j);
                if (!mapaConteo.containsKey(rgb)) {
                    mapaConteo.put(rgb, 0);
                }
                mapaConteo.put(rgb, mapaConteo.get(rgb) + 1);
            }
        }

        Set<Integer> setObjetos = new HashSet();
        ShortProcessor map = new ShortProcessor(width, height);
        ImagePlus respuestaShort = new ImagePlus("sinRuido", map);
        ImageProcessor pr = respuestaShort.getProcessor();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = bImin.getPixel(i, j);
                if (mapaConteo.get(rgb) > dimensionMinima) {
                    pr.set(i, j, rgb);
                    setObjetos.add(rgb);
                }
            }
        }

        numeroObjetos = setObjetos.size();
        map.setColorModel(Blob_Labeler_Gmas.makeLut(0));
        WindowManager.setTempCurrentImage(respuestaShort);
        return respuestaShort;
    }

    private ImagePlus sobreponer(ImagePlus abajo, ImagePlus arriba) {
        Overlay overlayList = abajo.getOverlay();
        if (overlayList == null) {
            overlayList = new Overlay();
        }
        Roi roi = new ImageRoi(0, 0, arriba.getProcessor());
        ((ImageRoi) roi).setOpacity(0.5);
        overlayList.add(roi);
        abajo.setOverlay(overlayList);
        Undo.setup(Undo.OVERLAY_ADDITION, abajo);
        WindowManager.setTempCurrentImage(abajo);
        return abajo;
    }

    private double getPorosidad(ImagePlus pPlanaMax, ImagePlus pMax) {
        BufferedImage pPlanaMaxBi = pPlanaMax.getBufferedImage();
        BufferedImage pMaxBi = pMax.getBufferedImage();
        int width = pPlanaMax.getWidth();
        int height = pPlanaMax.getHeight();
        double respuesta = 0;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Color cPla = new Color(pPlanaMaxBi.getRGB(i, j));
                Color cPer = new Color(pMaxBi.getRGB(i, j));
                int superior = 255;
                int inferior = 0;
                int holgura = 20;
                if (superior - holgura <= cPla.getRed() && cPla.getRed() <= superior
                        && superior - holgura <= cPla.getGreen() && cPla.getGreen() <= superior
                        && superior - holgura <= cPla.getBlue() && cPla.getBlue() <= superior
                        && inferior <= cPer.getRed() && cPer.getRed() <= inferior + holgura
                        && inferior <= cPer.getGreen() && cPer.getGreen() <= inferior + holgura
                        && inferior <= cPer.getBlue() && cPer.getBlue() <= inferior + holgura) {
                    respuesta++;
                }
            }
        }
        System.out.println("RESPUESTAAAAAAA: " + respuesta);
        return (respuesta / (width * height) * 100);
    }
}
