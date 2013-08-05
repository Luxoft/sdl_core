package com.batutin.android.androidvideostreaming.activity;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.batutin.android.androidvideostreaming.R;
import com.batutin.android.androidvideostreaming.screenshot.ScreenShotUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DecodeActivity extends Activity implements SurfaceHolder.Callback {
    private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/video.mp4";
    private PlayerThread mPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        test();
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);

    }

    private void test() {

        int start = CamcorderProfile.QUALITY_HIGH;
        for (int i = 0; i< CamcorderProfile.QUALITY_QVGA; i++){
            if (CamcorderProfile.hasProfile(i) == true){
                start = i;
            }
        }

        boolean hasProf = CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH);
        if (start > -1) {
            CamcorderProfile cp = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);

            int width = 320, height = 240;
            int bitRate = 128000;
            int frameRate = 15;
            ScreenShotUtils u = new ScreenShotUtils();
            String mimeType = "video/avc";
            int numCodecs = MediaCodecList.getCodecCount();
            MediaCodecInfo codecInfo = u.getMediaCodecInfo(mimeType, numCodecs);
            //String s = CodecCapabilitiesReader.get();
            //MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
            int stride = u.calcStrideForWidth(width, codecInfo);
            int sliceHeight = u.calcSliceHeightForHeight(height, codecInfo);
            //int colorFormat = u.getCapabilities(capabilities);
            int frameSize = u.calcFrameStride(stride, sliceHeight, stride);
            MediaFormat inputFormat = MediaFormat.createVideoFormat("video/avc", width, height);
            inputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            inputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            inputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
            inputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 75);
            inputFormat.setInteger("stride", stride);
            inputFormat.setInteger("slice-height", sliceHeight);
            String name = codecInfo.getName();
            try {
                MediaCodec encoder = MediaCodec.createByCodecName(name); // need to find name in media codec list, it is chipset-specific
                encoder.configure(inputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                encoder.start();
                ByteBuffer[] encoderInputBuffers = encoder.getInputBuffers();
                ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
            }
            catch (Exception r){
                r.printStackTrace();
            }




            byte[] inputFrame = new byte[frameSize];
        }

    }

    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mPlayer == null) {
            mPlayer = new PlayerThread(holder.getSurface());
            mPlayer.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mPlayer != null) {
            mPlayer.interrupt();
        }
    }

    private class PlayerThread extends Thread {
        private MediaExtractor extractor;
        private MediaCodec decoder;
        private Surface surface;

        public PlayerThread(Surface surface) {
            this.surface = surface;
        }

        private  MediaCodecInfo selectCodec(String mimeType) {
            int numCodecs = MediaCodecList.getCodecCount();
            for (int i = 0; i < numCodecs; i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

                if (!codecInfo.isEncoder()) {
                    continue;
                }

                String[] types = codecInfo.getSupportedTypes();
                for (int j = 0; j < types.length; j++) {
                    if (types[j].equalsIgnoreCase(mimeType)) {
                        return codecInfo;
                    }
                }
            }
            return null;
        }



        @Override
        public void run() {

            //test();
        }

        public void work() {
            int width = 328, height = 328;
            int bitRate = 1000000;
            int frameRate = 15;

            extractor = new MediaExtractor();
            AssetFileDescriptor testFd = getResources().openRawResourceFd(R.raw.test_video);
            try {
                extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                        testFd.getLength());
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i);
                    decoder = MediaCodec.createDecoderByType(mime);
                    decoder.configure(format, surface, null, 0);
                    break;
                }
            }

            if (decoder == null) {
                Log.e("DecodeActivity", "Can't find video info!");
                return;
            }

            String mimeType = "video/avc";
            ScreenShotUtils u = new ScreenShotUtils();
            int numCodecs = MediaCodecList.getCodecCount();
            MediaCodecInfo codecInfo = u.getMediaCodecInfo(mimeType, numCodecs);

            MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
            int colorFormat = u.getCapabilities(capabilities);


            width = u.calcWidth(codecInfo, width);
            int stride = u.calcStrideForWidth(width, codecInfo);
            int sliceHeight = u.calcSliceHeightForHeight(height, codecInfo);

            decoder.start();

            int chromaStride = u.calcChromaStride(stride);
            int frameSize = u.calcFrameStride(stride, sliceHeight, chromaStride);
            byte[] inputFrame = u.createInputFrame(width, height, colorFormat, stride, sliceHeight, chromaStride, frameSize);

            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            BufferInfo info = new BufferInfo();
            boolean isEOS = false;
            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                if (!isEOS) {
                    int inIndex = decoder.dequeueInputBuffer(5000);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            buffer.clear();
                            buffer.put(inputFrame);
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }

                int outIndex = decoder.dequeueOutputBuffer(info, 5000);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                        outputBuffers = decoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d("DecodeActivity", "New format " + decoder.getOutputFormat());
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + buffer);

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                        decoder.releaseOutputBuffer(outIndex, true);
                        break;
                }

                // All decoded frames have been rendered, we can stop playing now
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            decoder.stop();
            decoder.release();
            extractor.release();
        }
    }
}