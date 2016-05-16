
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.plugin.*;

/**
 *
 * @author juan
 */
public class Plugin_Gmas implements PlugIn {
    
    private ImagePlus projImage = null; 
    
    @Override
    public void run(String string) {
        ImagePlus imp = IJ.getImage();
        ZProjector zp = new ZProjector(imp);
        zp.setMethod(ZProjector.MAX_METHOD);
        zp.doRGBProjection();
        projImage = zp.getProjection();
        projImage.show();
    }
}
