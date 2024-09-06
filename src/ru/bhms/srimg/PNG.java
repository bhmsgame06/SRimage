package ru.bhms.srimg;

public class PNG {
    public static final byte[] PNG_SIGN = new byte[] {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A};
    public static final int    PNG_IHDR = 0x49484452;
    public static final int    PNG_PLTE = 0x504C5445;
    public static final int    PNG_TRNS = 0x74524E53;
    public static final int    PNG_IDAT = 0x49444154;
    public static final int    PNG_IEND = 0x49454E44;
}
