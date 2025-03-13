package com.szymm.chat;


import java.util.Arrays;

public class Pixmap {
    private final byte x;
    private final byte y;
    private final byte[] pixels;
    private final LUT lut;

    public Pixmap(byte x, byte y, byte[] pixels, LUT lut) {
        this.x = x;
        this.y = y;
        this.pixels = pixels;
        this.lut = lut;
    }

    public static Pixmap of(String text) {
        String[] lines = text.replace('_', '0')
                .split("\n");
        LUT lut = Pixmap.getLUT(lines[0]);
        byte x = (byte) (lines[1].length() / 2);
        byte y = (byte) (lines.length - 1);
        byte[] pixels = new byte[x * y];
        short i = 0;
        for (byte yi = 0; yi < y; yi++) {
            for (byte xi = 0; xi < x; xi++) {
                int start = xi * 2;
                int end = (xi + 1) * 2;
                String pixelText = lines[yi + 1].substring(start, end);
                pixels[i++] = Byte.parseByte(pixelText, 16);
            }
        }
        return new Pixmap(x, y, pixels, lut);
    }

    public static Pixmap decode(byte[] pixmapBytes, LUT lut) {
        byte x = pixmapBytes[0];
        byte y = pixmapBytes[1];
        int size = x * y;
        byte[] pixels = Arrays.copyOfRange(pixmapBytes, 2, 2 + size);
        return new Pixmap(x, y, pixels, lut);
    }

    public byte[] encode() {
        int size = this.x * this.y;
        byte[] pixmapBytes = new byte[size + 2];
        pixmapBytes[0] = this.x;
        pixmapBytes[1] = this.y;
        for (int i = 0; i < size; i++) {
            pixmapBytes[i + 2] = this.pixels[i];
        }
        return pixmapBytes;
    }

    public String display() {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (byte yi = 0; yi < this.y; yi++) {
            for (byte xi = 0; xi < this.x; xi++) {
                byte pixel = this.pixels[i++];
                builder.append(this.lut.lookup(pixel));
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public static LUT getLUT(String id) {
        return Pixmap.LUT_4W;
    }

    public static final LUT LUT_4W = new LUT(new String[]{"  ", "░░", "▓▓", "██"});

    public static class LUT {
        private final String[] glyphs;

        public LUT(String[] glyphs) {
            this.glyphs = glyphs;
        }

        public String lookup(byte pixel) {
            return this.glyphs[pixel];
        }
    }
}