import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/**
 * @author juan
 */
public class Acerca_De_Gmas implements PlugIn{

    @Override
    public void run(String string) {
        GenericDialog gdr = new GenericDialog("Plgin Gmas");
        gdr.addMessage("Juan Camilo Gaviria");
        gdr.addMessage("Oscar Marriga");
        gdr.addMessage("Imágenes y Vision 2016");
        gdr.showDialog();
    }
}
