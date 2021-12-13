package io.quarkiverse.renarde.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.BitSet;

public class UriUtils {

    private static BitSet ALPHA = new BitSet();
    static {
        for (char c = 'a'; c <= 'z'; c++)
            ALPHA.set(c);
        for (char c = 'A'; c <= 'Z'; c++)
            ALPHA.set(c);
    }

    private static BitSet DIGIT = new BitSet();
    static {
        for (char c = '0'; c <= '9'; c++)
            DIGIT.set(c);
    }

    private static BitSet UNRESERVED = new BitSet();
    static {
        UNRESERVED.or(ALPHA);
        UNRESERVED.or(DIGIT);
        UNRESERVED.set('-');
        UNRESERVED.set('.');
        UNRESERVED.set('_');
        UNRESERVED.set('~');
    }

    private static BitSet SUB_DELIMS = new BitSet();
    static {
        SUB_DELIMS.set('!');
        SUB_DELIMS.set('$');
        SUB_DELIMS.set('&');
        SUB_DELIMS.set('\'');
        SUB_DELIMS.set('(');
        SUB_DELIMS.set(')');
        SUB_DELIMS.set('*');
        SUB_DELIMS.set('+');
        SUB_DELIMS.set(',');
        SUB_DELIMS.set(';');
        SUB_DELIMS.set('=');
    }

    private static BitSet PCHAR = new BitSet();
    static {
        PCHAR.or(UNRESERVED);
        PCHAR.or(SUB_DELIMS);
        PCHAR.set(':');
        PCHAR.set('@');
    }

    public static String encodeSegment(String segment) {
        int length = segment.codePointCount(0, segment.length());
        StringBuffer sb = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            int c = segment.codePointAt(i);
            if (PCHAR.get(c))
                sb.append((char) c);
            else
                percentEncode(c, sb);
        }
        return sb.toString();
    }

    private static void percentEncode(int c, StringBuffer sb) {
        CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
        ByteBuffer out = ByteBuffer.allocate(4);
        CharBuffer in = CharBuffer.allocate(2);
        if (Character.isSupplementaryCodePoint(c)) {
            in.append(Character.highSurrogate(c));
            in.append(Character.lowSurrogate(c));
        } else
            in.append((char) c);
        in.flip();
        if (encoder.encode(in, out, true) != CoderResult.UNDERFLOW)
            throw new RuntimeException("Illegal UTF-8 encoding for codepoint " + c);
        out.flip();
        while (out.hasRemaining()) {
            sb.append("%");
            toHexa(out.get(), sb);
        }
    }

    private static void toHexa(byte b, StringBuffer sb) {
        byte h = (byte) ((b & 0b11110000) >> 4);
        byte l = (byte) (b & 0b1111);
        toHexa2(h, sb);
        toHexa2(l, sb);
    }

    private static void toHexa2(byte l, StringBuffer sb) {
        if (l < 10)
            sb.append(l);
        else
            sb.append((char) ((l - 10) + 'A'));
    }
}
