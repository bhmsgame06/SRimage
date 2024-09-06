package ru.bhms.srimg;

import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;
import java.util.zip.*;

import static ru.bhms.srimg.PNG.*;

public class SRImg {
    public static final String VERSION = "1.0";
    public static boolean verbose = false;
    public static boolean ignoreErrors = false;
    public static int compressionLevel = 2;

    public static void main(String[] args) throws Exception {
        int argc = args.length;

        if(argc <= 0) {
            showHelpPage();
            System.exit(1);
        }

        int n = 0;
        while(n < argc) {
            if(args[n].equals("-h") || args[n].equals("--help")) {

                showHelpPage();
                System.exit(0);

            } else if(args[n].equals("-v") || args[n].equals("--version")) {

                System.out.println(VERSION);
                System.exit(0);

            } else if(args[n].equals("-m") || args[n].equals("--make")) {

                int status = make(args[n + 1]);
                if(status != 0) {
                    System.exit(status);
                }
                n += 2;

            } else if(args[n].equals("-r") || args[n].equals("--reverse")) {

                int status = reverse(args[n + 1],args[n + 2]);
                if(status != 0) {
                    System.exit(status);
                }
                n += 3;

            } else if(args[n].equals("-i") || args[n].equals("--verbose")) {

                verbose = true;
                n++;

            } else if(args[n].equals("-I") || args[n].equals("--no-verbose")) {

                verbose = false;
                n++;

            } else if(args[n].equals("-e") || args[n].equals("--ignore-errors")) {

                ignoreErrors = true;
                n++;

            } else if(args[n].equals("-E") || args[n].equals("--do-not-ignore-errors")) {

                ignoreErrors = false;
                n++;

            } else if(args[n].equals("-z") || args[n].equals("--comp")) {

                compressionLevel = Integer.parseInt(args[n + 1]);
                n += 2;

            } else {

                System.err.println("Unknown option: " + args[n]);
                System.exit(1);

            }
        }
    }

    public static void showHelpPage() {
        System.out.println("SRimage (SolaRola image tool) v" + VERSION);
        System.out.println();
        System.out.println("Usage: srimg [OPTION [VALUE]...]...");
        System.out.println("Available options:");
        System.out.println("\t-h, --help                            - print this help page.");
        System.out.println("\t-v, --version                         - show version");
        System.out.println();
        System.out.println("\t-m, --make <png_file>                 - convert PNG/JPG/etc to PIM/PPL image chain.");
        System.out.println("\t-r, --reverse <pim_file> <ppl_file>   - convert PIM/PPL image chain to PNG.");
        System.out.println();
        System.out.println("\t-i, --verbose                         - print a verbose information during conversion process.");
        System.out.println("\t-I, --no-verbose                      - do not print verbose information (set by default).");
        System.out.println("\t-e, --ignore-errors                   - ignore all errors such as wrong PNG file signature error.");
        System.out.println("\t-E, --do-not-ignore-errors            - do not ignore errors (set by default).");
        System.out.println();
        System.out.println("\t-z, --comp <level>                    - set a compression level 0-9 (default is 2).");
        System.out.println();
        System.out.println("Notice that each option is read in a chain.");
        System.out.println("So, if you want to see verbose info, try that: \"srimg -v -m ./PITR.png\", instead of \"srimg -m ./PITR.png -v\".");
        System.out.println();
        System.out.println("GitHub: https://github.com/bhmsgame06/SRimage");
    }

    public static int make(String png) {
        try {
            BufferedImage image = ImageIO.read(new File(png));
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pxArr = Utils.getPixelArray(image);
            boolean isTransparent = Utils.isTransparent(pxArr);

            Vector<Integer> palette = Utils.getPalette(pxArr,isTransparent);
            int[] indices = Utils.getIndexArray(pxArr,palette);

            String noExt = Utils.removeExtension(png);
            DataOutputStream pimOut = new DataOutputStream(new FileOutputStream(noExt + ".pim"));
            DataOutputStream pplOut = new DataOutputStream(new FileOutputStream(noExt + ".ppl"));

            byte[] chunk;
            ByteArrayOutputStream outChunk;
            DataOutputStream dataChunk;

            { // PIM constructing           
                pimOut.writeShort(0x032E);

                outChunk = new ByteArrayOutputStream(); dataChunk = new DataOutputStream(outChunk);
                dataChunk.writeInt(width);
                dataChunk.writeInt(height);
                dataChunk.write(new byte[] {8,3,0,0,0});
                chunk = outChunk.toByteArray();
                dataChunk = null; outChunk = null;

                pimOut.writeShort(width);
                pimOut.writeShort(height);
                pimOut.writeInt(Utils.crc32(PNG_IHDR,chunk));

                Deflater zlib = new Deflater(compressionLevel);
                zlib.setInput(Utils.constructIDAT(indices,width,height));
                zlib.finish();
                byte[] idat = new byte[16384];
                int idatSize = zlib.deflate(idat);
                zlib.end();
                chunk = new byte[idatSize];
                System.arraycopy(idat,0,chunk,0,idatSize);
                idat = null;
                pimOut.writeInt(Utils.crc32(PNG_IDAT,chunk));
                
                pimOut.write(chunk,chunk.length - 4,4);
                pimOut.write(chunk,0,chunk.length - 4);

                pimOut.close();
            }

            { // PPL constructing
                int paletteSize = palette.size();
                byte[] plte = new byte[paletteSize * 3];
                for(int i = 0,n = 0;i < paletteSize;i++,n += 3) {
                    int color = palette.get(i);
                    plte[n] = (byte)(color >> 16);
                    plte[n + 1] = (byte)(color >> 8);
                    plte[n + 2] = (byte)color;
                }
                pplOut.write(isTransparent ? 1 : 0);
                pplOut.write(palette.size() - 1);
                pplOut.writeInt(Utils.crc32(PNG_PLTE,plte));
                pplOut.write(plte);
                 
                pplOut.close();
            }
        } catch(Exception e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    public static int reverse(String pim,String ppl) {
        try {
            DataInputStream pimIn = new DataInputStream(new FileInputStream(pim));
            DataInputStream pplIn = new DataInputStream(new FileInputStream(ppl));

            // PIM
            int width;
            int height;
            int ihdrCRC;
            int idatCRC;
            int idatAdler;
            byte[] idatData;

            // PPL
            boolean isTransparent;
            int colors;
            int plteCRC;
            byte[] plteData;

            // PIM parsing procedure
            int sig = pimIn.readByte();
            if(sig != 3 && !ignoreErrors) {
                System.err.printf("%s: Wrong PIM signature 0x%02x! Expected 0x03.",pim,sig);
                System.err.println();
                return 1;
            }
            pimIn.skip(1);
            width = pimIn.readUnsignedShort();
            height = pimIn.readUnsignedShort();
            ihdrCRC = pimIn.readInt();
            idatCRC = pimIn.readInt();
            idatAdler = pimIn.readInt();
            idatData = new byte[pimIn.available()];
            pimIn.read(idatData);
            if(verbose) { // PIM verbose print
                System.out.println("PIM parse summary:");
                System.out.println("\tWidth: " + width);
                System.out.println("\tHeight: " + height);
                System.out.println("\tIHDR CRC: " + String.format("0x%04x",ihdrCRC));
                System.out.println("\tIDAT CRC: " + String.format("0x%04x",idatCRC));
                System.out.println("\tIDAT Adler: " + String.format("0x%04x",idatAdler));
                System.out.println("\tIDAT image data size: " + idatData.length + " bytes");
                System.out.println();
            }

            // PPL parsing procedure
            isTransparent = pplIn.readBoolean();
            colors = pplIn.readUnsignedByte() + 1;
            plteCRC = pplIn.readInt();
            plteData = new byte[pplIn.available()];
            pplIn.read(plteData);
            if(verbose) { // PPL verbose print
                System.out.println("PPL parse summary:");
                System.out.println("\tIs transparent: " + isTransparent);
                System.out.println("\tColors count: " + colors);
                System.out.println("\tPLTE CRC: " + String.format("0x%04x",plteCRC));
                System.out.println("\tPLTE palette data size: " + plteData.length + " bytes");
                System.out.println();
            }
            
            // parsing complete
            pimIn.close();
            pplIn.close();

            // building PNG
            DataOutputStream pngOut = new DataOutputStream(new FileOutputStream(Utils.removeExtension(pim) + ".png"));
            { // Beginning of PNG file
                pngOut.write(PNG_SIGN);
            }

            { // IHDR chunk
                pngOut.writeInt(0x0D);
                pngOut.writeInt(PNG_IHDR);
                pngOut.writeInt(width);
                pngOut.writeInt(height);
                pngOut.write(new byte[] {8,3,0,0,0});
                pngOut.writeInt(ihdrCRC);
            }

            { // PLTE chunk
                pngOut.writeInt(colors * 3);
                pngOut.writeInt(PNG_PLTE);
                pngOut.write(plteData);
                pngOut.writeInt(plteCRC);
            }

            if(isTransparent)
            { // tRNS chunk (tRNS is in the PNG, if the first byte in PPL file is 1)
                pngOut.writeInt(1);
                pngOut.writeInt(PNG_TRNS);
                pngOut.writeByte(0);
                pngOut.writeInt(0x40E6D866);
            }

            { // IDAT chunk
                pngOut.writeInt(idatData.length + 4);
                pngOut.writeInt(PNG_IDAT);
                pngOut.write(idatData);
                pngOut.writeInt(idatAdler);
                pngOut.writeInt(idatCRC);
            }

            { // Ending of PNG file
                pngOut.write(new byte[] {0,0,0,0});
                pngOut.writeInt(PNG_IEND);
                pngOut.writeInt(0xAE426082);
            }

            pngOut.close();
        } catch(Exception e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }
}
