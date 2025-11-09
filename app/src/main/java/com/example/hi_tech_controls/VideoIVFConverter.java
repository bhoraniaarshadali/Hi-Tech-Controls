package com.example.hi_tech_controls;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import com.example.hi_tech_controls.adapter.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class VideoIVFConverter {

    private static final String TAG = "VideoIVF";

    public static void convertToIVF(Context ctx, Uri input, File outFile, Callback cb) {
        new Thread(() -> {
            try {
                // Try VP8 hardware first
                boolean ok = encodeVP8ToIVF(ctx, input, outFile);
                if (ok && outFile.length() > 1000) {
                    cb.onSuccess(outFile);
                    return;
                }

                // Fallback → copy original MP4
                File fallback = new File(outFile.getParent(), "fallback_" + System.currentTimeMillis() + ".mp4");
                FileUtil.copyUriToFile(ctx, input, fallback);
                cb.onFallback(fallback);

            } catch (Exception e) {
                cb.onError("IVF Encode Crash: " + e.getMessage());
            }

        }).start();
    }

    private static boolean encodeVP8ToIVF(Context ctx, Uri input, File outFile) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(ctx, input, null);

            int videoTrack = selectVideoTrack(extractor);
            if (videoTrack < 0) return false;
            extractor.selectTrack(videoTrack);

            MediaFormat src = extractor.getTrackFormat(videoTrack);
            MediaCodec encoder = MediaCodec.createEncoderByType("video/x-vnd.on2.vp8");

            MediaFormat dst = MediaFormat.createVideoFormat("video/x-vnd.on2.vp8",
                    src.getInteger(MediaFormat.KEY_WIDTH),
                    src.getInteger(MediaFormat.KEY_HEIGHT));
            dst.setInteger(MediaFormat.KEY_BIT_RATE, 1_000_000);
            dst.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            dst.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

            encoder.configure(dst, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            FileOutputStream fos = new FileOutputStream(outFile);

            writeIVFHeader(fos, src.getInteger(MediaFormat.KEY_WIDTH),
                    src.getInteger(MediaFormat.KEY_HEIGHT));

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean done = false;

            while (!done) {
                int inIndex = encoder.dequeueInputBuffer(5000);
                if (inIndex >= 0) {
                    ByteBuffer inBuf = encoder.getInputBuffer(inIndex);
                    assert inBuf != null;
                    int size = extractor.readSampleData(inBuf, 0);

                    if (size < 0) {
                        encoder.queueInputBuffer(inIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    } else {
                        long ts = extractor.getSampleTime();
                        encoder.queueInputBuffer(inIndex, 0, size, ts, 0);
                        extractor.advance();
                    }
                }

                int outIndex = encoder.dequeueOutputBuffer(info, 5000);
                if (outIndex >= 0) {
                    ByteBuffer out = encoder.getOutputBuffer(outIndex);
                    writeIVFFrame(fos, out, info);
                    encoder.releaseOutputBuffer(outIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0)
                        done = true;
                }
            }

            encoder.stop();
            encoder.release();
            extractor.release();
            fos.close();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "encodeVP8ToIVF fail: " + e);
            return false;
        }
    }

    private static int selectVideoTrack(MediaExtractor ex) {
        for (int i = 0; i < ex.getTrackCount(); i++) {
            MediaFormat f = ex.getTrackFormat(i);
            String mime = f.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/")) return i;
        }
        return -1;
    }

    /**
     * IVF Header Writer
     */
    private static void writeIVFHeader(FileOutputStream fos, int w, int h) throws Exception {
        byte[] header = new byte[32];
        header[0] = 'D';
        header[1] = 'K';
        header[2] = 'I';
        header[3] = 'F';
        header[4] = 0x00;
        header[5] = 0x00;
        header[6] = 0x20;
        header[7] = 0x00;
        header[8] = 'V';
        header[9] = 'P';
        header[10] = '8';
        header[11] = '0';
        header[12] = (byte) (w & 0xFF);
        header[13] = (byte) ((w >> 8) & 0xFF);
        header[14] = (byte) (h & 0xFF);
        header[15] = (byte) ((h >> 8) & 0xFF);
        fos.write(header);
    }

    private static void writeIVFFrame(FileOutputStream fos, ByteBuffer buffer, MediaCodec.BufferInfo info) throws Exception {
        if (info.size <= 0) return;

        byte[] frameHeader = new byte[12];
        frameHeader[0] = (byte) (info.size & 0xFF);
        frameHeader[1] = (byte) ((info.size >> 8) & 0xFF);
        frameHeader[2] = (byte) ((info.size >> 16) & 0xFF);
        frameHeader[3] = (byte) ((info.size >> 24) & 0xFF);
        fos.write(frameHeader);

        byte[] frame = new byte[info.size];
        buffer.get(frame);
        fos.write(frame);
    }

    public interface Callback {
        void onSuccess(File ivfFile);

        void onFallback(File originalCopiedFile);

        void onError(String msg);
    }
}
