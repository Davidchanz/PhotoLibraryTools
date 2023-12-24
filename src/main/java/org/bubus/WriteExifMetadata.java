package org.bubus;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class WriteExifMetadata {
    /**
     * Add/Update EXIF metadata in a JPEG file.
     *
     * @param jpegImageFile A source image file.
     * @param dst           The output file.
     * @throws IOException
     * @throws ImagingException
     * @throws ImagingException
     */

    static final Logger logger = Logger.getLogger(WriteExifMetadata.class);
    public boolean changeExifMetadata(final File jpegImageFile, final File dst, String location, String  dataTime) throws IOException, ImagingException, ImagingException {
        boolean success = true;

        try (FileOutputStream fos = new FileOutputStream(dst);
             OutputStream os = new BufferedOutputStream(fos)) {

            TiffOutputSet outputSet = null;

            final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                final TiffImageMetadata exif = jpegMetadata.getExif();

                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
            }

            if (null == outputSet) {
                outputSet = new TiffOutputSet();
            }

            {
                try {
                    final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
                    exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE, new RationalNumber(3, 10));

                    exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

                    SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
                    Date pictureDate = SDF.parse(dataTime.replace("T", " "));
                    Calendar cal = Calendar.getInstance();
                    cal.clear();
                    cal.setTime(pictureDate);

                    String updatedDateString = SDF.format(cal.getTime());
                    final TiffOutputField dateTimeOutputField = new TiffOutputField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, FieldType.ASCII, updatedDateString.length(), updatedDateString.getBytes());
                    exifDirectory.add(dateTimeOutputField);
                }catch (Exception e){
                    logger.error("Error to write DATETIME " + dataTime + " TO file " + dst.getName() + " FROM " + jpegImageFile.getName());
                    success = false;
                }
            }

            {
                try {
                    String[] locationData = location.split("\\+");

                    int longitudeIndex = 0;
                    int latitudeIndex = 0;
                    if(locationData.length == 4){
                        longitudeIndex = 2;
                        latitudeIndex = 1;
                    }else if(locationData.length == 3){
                        longitudeIndex = 1;
                        latitudeIndex = 0;
                    }

                    final double longitude = Double.parseDouble(locationData[longitudeIndex]);
                    final double latitude = Double.parseDouble(locationData[latitudeIndex]);

                    outputSet.setGPSInDegrees(longitude, latitude);
                }catch (Exception e){
                    logger.error("Error to write LOCATION " + location + " TO file " + dst.getName() + " FROM " + jpegImageFile.getName());
                }
            }

            new ExifRewriter().updateExifMetadataLossless(jpegImageFile, os, outputSet);
        }

        if(!success){
            dst.delete();
        }
        return success;
    }
}
