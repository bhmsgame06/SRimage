package ru.bhms.srimg;

import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Utils {
    public static final String removeExtension(String file) {
        StringBuilder name = new StringBuilder();
		char ch = 0;
		int index = file.length() - 1;

		for(;(ch = file.charAt(index)) != '.';index--) {
		}
		index--;
		for(;index >= 0;index--) {
			ch = file.charAt(index);
			name.append(ch);
		}

		return name.reverse().toString();
    }

	public static final int[] getPixelArray(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();

		int[] pxArr = new int[w * h];
		image.getRGB(0,0,w,h,pxArr,0,w);

		return pxArr;
	}

	public static final Vector<Integer> getPalette(int[] pxArr,boolean isTransparent) {
		Vector<Integer> palette = new Vector<>(256);
		if(isTransparent) palette.add(0x00ff00ff);
		for(int i = 0;i < pxArr.length;i++) {
			int found = palette.indexOf(pxArr[i] & 0xffffff);
			if(found == -1 && (((pxArr[i] >> 24) & 0xff) == 255)) {
				palette.add(pxArr[i] & 0xffffff);
			}
		}

		return palette;
	}

	public static final int[] getIndexArray(int[] pxArr,Vector<Integer> palette) {
		Vector<Integer> index = new Vector<>(pxArr.length);

		for(int i = 0;i < pxArr.length;i++) {
			if(((pxArr[i] >> 24) & 0xff) != 255) {
				index.add(0);
			} else {
				index.add(palette.indexOf(pxArr[i] & 0xffffff));
			}
		}

		int[] indexOut = new int[index.size()];
		for(int i = 0;i < indexOut.length;i++) {
			indexOut[i] = index.get(i);
		}

		return indexOut;
	}

	public static final boolean isTransparent(int[] pxArr) {
		for(int i = 0;i < pxArr.length;i++) {
			if(((pxArr[i] >> 24) & 0xff) == 0) {
				return true;
			}
		}

		return false;
	}

	public static final int crc32(int type,byte[] data) {
		byte[] full = new byte[data.length + 4];
		full[0] = (byte)((type >> 24) & 0xff);
		full[1] = (byte)((type >> 16) & 0xff);
		full[2] = (byte)((type >> 8) & 0xff);
		full[3] = (byte)(type & 0xff);
		System.arraycopy(data,0,full,4,data.length);

		CRC32 crc = new CRC32();
		crc.update(full);

		return (int)crc.getValue();
	}

	public static final byte[] constructIDAT(int[] data,int width,int height) {
		byte[] byteData = new byte[data.length];
		for(int i = 0;i < data.length;i++) {
			byteData[i] = (byte)data[i];
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int x = 0;

		for(int i = 0;i < height;i++) {
			out.write(0);
			out.write(byteData,x,width);
			x += width;
		}

		return out.toByteArray();
	}
}
