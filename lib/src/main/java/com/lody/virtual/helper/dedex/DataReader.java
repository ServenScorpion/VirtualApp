package com.lody.virtual.helper.dedex;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DataReader implements Closeable {

    private final RandomAccessFile mRaf;
    private final File mFile;
    private final MappedByteBuffer mMappedBuffer;
    private ArrayList<DataReader> mAssociatedReaders;

    public DataReader(String file) throws Exception {
        this(new File(file));
    }

    public DataReader(File file) throws Exception {
        mFile = file;
        mRaf = new RandomAccessFile(mFile, "r");
        mMappedBuffer = mRaf.getChannel().map(
                FileChannel.MapMode.READ_ONLY, 0, file.length());
        mMappedBuffer.rewind();
        setLittleEndian(true);
    }

    public void setLittleEndian(boolean isLittleEndian) {
        mMappedBuffer.order(isLittleEndian
                ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
    }

    public void seek(long offset) {
        position((int) offset);
    }

    public void position(int newPosition) {
        mMappedBuffer.position(newPosition);
    }

    public int position() {
        return mMappedBuffer.position();
    }

    public int readByte() {
        return mMappedBuffer.get() & 0xff;
    }

    public void readBytes(byte[] b) {
        mMappedBuffer.get(b, 0, b.length);
    }

    public void readBytes(char[] b) {
        final byte[] bs = new byte[b.length];
        readBytes(bs);
        for (int i = 0; i < b.length; i++) {
            b[i] = (char) bs[i];
        }
    }

    public short readShort() {
        return mMappedBuffer.getShort();
    }

    public int readInt() {
        return mMappedBuffer.getInt();
    }

    public int previewInt() {
        mMappedBuffer.mark();
        final int value = readInt();
        mMappedBuffer.reset();
        return value;
    }

    public final long readLong() {
        return mMappedBuffer.getLong();
    }

    public int readUleb128() {
        int result = readByte();
        if (result > 0x7f) {
            int curVal = readByte();
            result = (result & 0x7f) | ((curVal & 0x7f) << 7);
            if (curVal > 0x7f) {
                curVal = readByte();
                result |= (curVal & 0x7f) << 14;
                if (curVal > 0x7f) {
                    curVal = readByte();
                    result |= (curVal & 0x7f) << 21;
                    if (curVal > 0x7f) {
                        curVal = readByte();
                        result |= curVal << 28;
                    }
                }
            }
        }
        return result;
    }

    public File getFile() {
        return mFile;
    }

    public FileChannel getChannel() {
        return mRaf.getChannel();
    }

    public void addAssociatedReader(DataReader reader) {
        if (mAssociatedReaders == null) {
            mAssociatedReaders = new ArrayList<>();
        }
        mAssociatedReaders.add(reader);
    }

    @Override
    public void close() {
        try {
            mRaf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mAssociatedReaders != null) {
            for (DataReader r : mAssociatedReaders) {
                r.close();
            }
        }
    }

    public static int toInt(String str) {
        int len = str.length(), p = 0;
        char[] sb = new char[len];
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if ((c >= '0' && c <= '9') || c == '-') {
                sb[p++] = c;
            }
        }
        return p == 0 ? 0 : Integer.parseInt(new String(sb, 0, p));
    }

}