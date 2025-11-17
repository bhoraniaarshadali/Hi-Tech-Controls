package com.example.hi_tech_controls.mediaControl;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.nio.ByteBuffer;

public class VideoCompressor {

    private static final String TAG = "VideoCompressor";
    private volatile boolean cancelled = false;

    /**
     * Request cancelation. The running thread will attempt to stop gracefully.
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * Compress the video pointed by uri into output file (H.264 MP4).
     * Runs on a background thread.
     */
    public void compress(Context ctx, Uri uri, File output, Callback cb) {
        new Thread(() -> {
            try {
                MediaExtractor extractor = new MediaExtractor();
                extractor.setDataSource(ctx, uri, null);

                int videoTrack = -1;
                MediaFormat inputFormat = null;

                for (int i = 0; i < extractor.getTrackCount(); i++) {
                    MediaFormat f = extractor.getTrackFormat(i);
                    String mime = f.getString(MediaFormat.KEY_MIME);
                    if (mime != null && mime.startsWith("video/")) {
                        videoTrack = i;
                        inputFormat = f;
                        break;
                    }
                }

                if (videoTrack < 0) {
                    extractor.release();
                    throw new Exception("No video track found");
                }

                extractor.selectTrack(videoTrack);

                if (cancelled) {
                    extractor.release();
                    cb.onCancelled();
                    return;
                }

                int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);

                // Create encoder format (H.264)
                MediaFormat outputFormat = MediaFormat.createVideoFormat("video/avc", width, height);
                outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1_000_000); // 1Mbps default
                outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);

                MediaCodec encoder = MediaCodec.createEncoderByType("video/avc");
                encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                Surface encoderSurface = encoder.createInputSurface();
                encoder.start();

                MediaCodec decoder = MediaCodec.createDecoderByType(
                        inputFormat.getString(MediaFormat.KEY_MIME)
                );
                // Render decoder output to the encoder surface:
                decoder.configure(inputFormat, encoderSurface, null, 0);
                decoder.start();

                MediaMuxer muxer = new MediaMuxer(output.getAbsolutePath(),
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                MediaCodec.BufferInfo encInfo = new MediaCodec.BufferInfo();
                MediaCodec.BufferInfo decInfo = new MediaCodec.BufferInfo();

                boolean doneEncoding = false;
                boolean doneDecoding = false;
                int muxerVideoTrack = -1;

                // Main transcode loop
                while (!doneEncoding && !cancelled) {

                    // Feed decoder with input from extractor
                    int decIn = decoder.dequeueInputBuffer(5000);
                    if (decIn >= 0 && !doneDecoding) {
                        ByteBuffer inBuf = decoder.getInputBuffer(decIn);
                        assert inBuf != null;
                        int size = extractor.readSampleData(inBuf, 0);
                        if (size < 0) {
                            decoder.queueInputBuffer(decIn, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            doneDecoding = true;
                        } else {
                            long ts = extractor.getSampleTime();
                            decoder.queueInputBuffer(decIn, 0, size, ts, 0);
                            extractor.advance();
                        }
                    }

                    // Drain decoder output, render to encoder surface
                    int decOut = decoder.dequeueOutputBuffer(decInfo, 5000);
                    if (decOut >= 0) {
                        boolean eos = (decInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        // render = true -> renders to encoder input surface
                        decoder.releaseOutputBuffer(decOut, true);
                        if (eos) {
                            // signal encoder input end once decoder finished
                            encoder.signalEndOfInputStream();
                        }
                    }

                    // Drain encoder output and write to muxer
                    int encOut = encoder.dequeueOutputBuffer(encInfo, 5000);
                    if (encOut >= 0) {
                        ByteBuffer encodedData = encoder.getOutputBuffer(encOut);

                        if (muxerVideoTrack == -1) {
                            MediaFormat newFmt = encoder.getOutputFormat();
                            muxerVideoTrack = muxer.addTrack(newFmt);
                            muxer.start();
                        }

                        if (encodedData != null && encInfo.size > 0) {
                            encodedData.position(encInfo.offset);
                            encodedData.limit(encInfo.offset + encInfo.size);
                            muxer.writeSampleData(muxerVideoTrack, encodedData, encInfo);
                        }

                        boolean eos = (encInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        encoder.releaseOutputBuffer(encOut, false);

                        if (eos) {
                            doneEncoding = true;
                        }
                    }
                }

                // clean up
                try {
                    if (muxerVideoTrack != -1) {
                        muxer.stop();
                    }
                } catch (Exception ignored) {
                }

                muxer.release();

                try {
                    decoder.stop();
                } catch (Exception ignored) {
                }
                try {
                    decoder.release();
                } catch (Exception ignored) {
                }

                try {
                    encoder.stop();
                } catch (Exception ignored) {
                }
                try {
                    encoder.release();
                } catch (Exception ignored) {
                }

                extractor.release();

                if (cancelled) cb.onCancelled();
                else cb.onSuccess(output);

            } catch (Exception e) {
                Log.e(TAG, "compress error", e);
                cb.onError(e);
            }
        }).start();
    }

    public interface Callback {
        void onSuccess(File output);

        void onError(Exception e);

        void onCancelled();
    }
}
