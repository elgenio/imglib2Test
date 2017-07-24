import ij.IJ;
import ij.ImageJ;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.gauss.Gauss;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.io.File;
import java.util.Iterator;

public class imgLib2Test {

    public <T extends RealType<T> & NativeType<T>> imgLib2Test()
            throws ImgIOException {

        // define the file to open
        File file = new File("/Volumes/Data/imgLib2Project/img/img.tif");
        String path = file.getAbsolutePath();
        ImgOpener imgOpener = new ImgOpener();

        // open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg) is
        Img<T> image = (Img<T>) imgOpener.openImg(path);

        // display it via ImgLib using ImageJ
        ImageJFunctions.show(image,"First Image");
        getMinMax(image, "First Image");

        final double[] sigma = new double[]{2, 0.75};
        // compute with float precision, and output a FloatType Img
        final Img<FloatType> gaussImage = Gauss.toFloat(sigma, image);

        ImageJFunctions.show(gaussImage,"Gauss Image");
        getMinMax(gaussImage, "Gauss Image");

        final FloatType type = new FloatType();
        final ArrayImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();

        final Img<FloatType> dX = factory.create(gaussImage, type);
        gradient(Views.extendBorder(gaussImage), dX, 0);
        ImageJFunctions.show(dX, "gradient X");
        getMinMax(gaussImage, "gradient X");

        final Img<FloatType> dY = factory.create(gaussImage, type);
        gradient(Views.extendBorder(gaussImage), dY, 1);
        ImageJFunctions.show(dY, "gradient Y");
        getMinMax(gaussImage, "gradient Y");

        RandomAccessible<FloatType> input = Views.extendValue(dY, new FloatType(128));
        RealRandomAccessible<FloatType> interpolated = Views.interpolate(input, new NLinearInterpolatorFactory<FloatType>());

        AffineTransform2D affine = new AffineTransform2D();
        affine.scale(1.3);

        RealRandomAccessible<FloatType> realview = RealViews.affineReal(interpolated, affine);

        RandomAccessibleInterval<FloatType> view = Views.interval(Views.raster(realview), dY);

        ImageJFunctions.show(view,"Scale Image");

    }




    public static void main(String[] args) throws ImgIOException {
        // open an ImageJ window
        new ImageJ();

        // run the example
        new imgLib2Test();
    }

    //Created by Me
    private <T extends RealType<T> & NativeType<T>> void getMinMax(Img<T> image, String title) {
        // create two empty variables
        T min = image.firstElement().createVariable();
        T max = image.firstElement().createVariable();

        // compute min and max of the Image
        computeMinMax(image, min, max);
        IJ.log(title+"-minimum Value (img): " + min);
        IJ.log(title+"-maximum Value (img): " + max);
        IJ.log("-----------------------");
    }

    public static <T extends NumericType<T>> void gradient(
            final RandomAccessible<T> source,
            final RandomAccessibleInterval<T> target,
            final int dimension) {
        final Cursor<T> front = Views.flatIterable(
                Views.interval(source,
                        Intervals.translate(target, 1, dimension))).cursor();
        final Cursor<T> back = Views.flatIterable(
                Views.interval(source,
                        Intervals.translate(target, -1, dimension))).cursor();
        for (final T t : Views.flatIterable(target)) {
            t.set(front.next());
            t.sub(back.next());
            t.mul(0.5);
        }
    }

    public <T extends Comparable<T> & Type<T>> void computeMinMax(
            final Iterable<T> input, final T min, final T max) {
        // create a cursor for the image (the order does not matter)
        final Iterator<T> iterator = input.iterator();

        // initialize min and max with the first image value
        T type = iterator.next();

        min.set(type);
        max.set(type);

        // loop over the rest of the data and determine min and max value
        while (iterator.hasNext()) {
            // we need this type more than once
            type = iterator.next();

            if (type.compareTo(min) < 0)
                min.set(type);

            if (type.compareTo(max) > 0)
                max.set(type);
        }

    }
}