package com.example.hi_tech_controls;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class VideoWebMConverter {

    public static void convertToWebM(Context ctx, Uri uri, File outFile, Callback cb) {
        new Thread(() -> {
            try {
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(ctx, uri, null);

                int trackIndex = -1;
                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video/")) {
                        trackIndex = i;
                        extractor.selectTrack(i);
                        break;
                    }
                }
                if (trackIndex < 0) {
                    cb.onError("No video track");
                    return;
                }

                MediaFormat inputFormat = extractor.getTrackFormat(trackIndex);
                MediaFormat outputFormat = MediaFormat.createVideoFormat("video/webm",
                        inputFormat.getInteger(MediaFormat.KEY_WIDTH),
                        inputFormat.getInteger(MediaFormat.KEY_HEIGHT));

                outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1_000_000);
                outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
                outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
                outputFormat.setString(MediaFormat.KEY_MIME, "video/webm");

                MediaCodec encoder = MediaCodec.createEncoderByType("video/webm");
                encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoder.start();

                FileOutputStream fos = new FileOutputStream(outFile);

                ByteBuffer[] inputBuffers = encoder.getInputBuffers();
                ByteBuffer[] outputBuffers = encoder.getOutputBuffers();

                boolean done = false;
                while (!done) {
                    int inIndex = encoder.dequeueInputBuffer(10000);
                    if (inIndex >= 0) {
                        ByteBuffer buf = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buf, 0);
                        if (sampleSize < 0) {
                            encoder.queueInputBuffer(inIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            encoder.queueInputBuffer(inIndex, 0, sampleSize,
                                    extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    int outIndex = encoder.dequeueOutputBuffer(info, 10000);
                    while (outIndex >= 0) {
                        ByteBuffer outBuf = outputBuffers[outIndex];
                        byte[] data = new byte[info.size];
                        outBuf.get(data);
                        fos.write(data);
                        encoder.releaseOutputBuffer(outIndex, false);
                        outIndex = encoder.dequeueOutputBuffer(info, 0);
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        done = true;
                    }
                }

                fos.flush();
                fos.close();
                encoder.stop();
                encoder.release();
                extractor.release();

                cb.onSuccess(outFile);

            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }

    public interface Callback {
        void onSuccess(File file);

        void onError(String msg);
    }
}
