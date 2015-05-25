/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tango.util;

import ij.IJ;
import ij.ImagePlus;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.DataTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.IFormatWriter;
import loci.formats.ImageWriter;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.out.APNGWriter;
import loci.formats.services.OMEXMLService;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.ImageFloat;
import mcib3d.image3d.ImageHandler;
import mcib3d.image3d.ImageShort;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;

/**
 *
 * @author jollion
 */
public class WriteImage {
    
    public static void writeAPNGToFile(ImageHandler image, String path) {
        //ImageWriter writer = new ImageWriter();
        
        //APNGWriter writer = new APNGWriter();
        try {
            IFormatWriter writer = new ImageWriter().getWriter(path);
            writer.setMetadataRetrieve(generateMetadata(image));
            writer.setSeries(0);
            IJ.log("image count: "+writer.getMetadataRetrieve().getImageCount());
            IJ.log("color model==null? "+(writer.getColorModel()==null));
            IJ.log("compression "+writer.getCompression());
            boolean littleEndian = !writer.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0);
            writer.setId(path);
            IJ.log("format: "+writer.getFormat());
            for (int z = 0; z<image.sizeZ; z++) {
                writer.saveBytes(z, getBytePlane(image, z, littleEndian));
                
            }
            writer.close();
        } catch (FormatException ex) {
            Logger.getLogger(WriteImage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WriteImage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static byte[] getBytePlane(ImageHandler image, int z, boolean littleEndian) {
        if (image instanceof ImageByte) {
            return ((ImageByte)image).pixels[z];
        } else if (image instanceof ImageShort) {
            return DataTools.shortsToBytes( (short[]) ((ImageShort)image).pixels[z], littleEndian);
        } else if (image instanceof ImageFloat) {
            return DataTools.floatsToBytes( (float[]) ((ImageFloat)image).pixels[z], littleEndian);
        } else return null;
    }
    
    public static IMetadata generateMetadata(ImageHandler image) {
        ServiceFactory factory;
        IMetadata meta=null;
        try {
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            try {
                meta = service.createOMEXMLMetadata();
                meta.setImageID("Image:0", 0);
                meta.setPixelsID("Pixels:0", 0);
                meta.setPixelsDimensionOrder(DimensionOrder.XYZCT,0);
                //meta.setPixelsBinDataBigEndian(!ij.Prefs.intelByteOrder,0,0);
                 if (meta.getPixelsBinDataCount(0) == 0 || meta.getPixelsBinDataBigEndian(0, 0) == null) {
                    meta.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
                }
                meta.setPixelsSizeX(new PositiveInteger(image.sizeX), 0);
                meta.setPixelsSizeY(new PositiveInteger(image.sizeY), 0);
                meta.setPixelsSizeZ(new PositiveInteger(image.sizeZ), 0);
                meta.setPixelsSizeT(new PositiveInteger(1), 0);
                meta.setPixelsSizeC(new PositiveInteger(1), 0);
                meta.setChannelID("Channel:0:" + 0, 0, 0);
                meta.setChannelSamplesPerPixel(new PositiveInteger(1),0,0);
                if (image instanceof ImageByte) meta.setPixelsType(PixelType.UINT8,0);
                else if (image instanceof ImageShort) meta.setPixelsType(PixelType.UINT16,0);
                else if (image instanceof ImageFloat) meta.setPixelsType(PixelType.UINT32,0);
                Unit<Length> unit = Unit.CreateBaseUnit("", "µm");
                meta.setPixelsPhysicalSizeX(new Length(image.getScaleXY(), unit), 0);
                meta.setPixelsPhysicalSizeY(new Length(image.getScaleXY(), unit), 0);
                meta.setPixelsPhysicalSizeZ(new Length(image.getScaleZ(), unit), 0);
            } catch (ServiceException ex) {
                Logger.getLogger(ImageOpener.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (DependencyException ex) {
            Logger.getLogger(ImageOpener.class.getName()).log(Level.SEVERE, null, ex);
        }
        return meta;
    }

}
