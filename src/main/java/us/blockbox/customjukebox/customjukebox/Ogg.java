package us.blockbox.customjukebox.customjukebox;

import java.io.*;

class Ogg
{
    int audio_channels;
    int audio_sample_rate;
    long dataLength;
    long headerStart;
    long sampleNum;
    int vorbis_version;
    
    public Ogg(final File file) throws Exception {
        this(file.getPath());
    }
    
    public Ogg(final String file) throws Exception {
        this.dataLength = new File(file).length();
        final FileInputStream inStream = new FileInputStream(file);
        int pos = 0;
        while (true) {
            int packet_type = 0;
            final char[] capture_pattern1 = { 'v', 'o', 'r', 'b', 'i', 's' };
            for (int i = 0; i < capture_pattern1.length; ++i) {
                final int b = inStream.read();
                if (b == -1) {
                    throw new Exception("no Vorbis identification header");
                }
                ++pos;
                if (b != capture_pattern1[i]) {
                    packet_type = b;
                    i = -1;
                }
            }
            if (packet_type == 1) {
                this.vorbis_version = this.read32Bits(inStream);
                if (this.vorbis_version > 0) {
                    throw new Exception("unknown vorbis_version " + this.vorbis_version);
                }
                this.audio_channels = inStream.read();
                this.audio_sample_rate = this.read32Bits(inStream);
                pos += 9;
                this.headerStart = this.dataLength - 16384L;
                inStream.skip(this.headerStart - pos);
                int count = 0;
                while (true) {
                    int i;
                    int b;
                    char[] capture_pattern2;
                    for (capture_pattern2 = new char[] { 'O', 'g', 'g', 'S', '\0' }, i = 0; i < capture_pattern2.length; ++i) {
                        b = inStream.read();
                        if (b == -1) {
                            break;
                        }
                        if (b != capture_pattern2[i]) {
                            this.headerStart += i + 1;
                            i = -1;
                        }
                    }
                    if (i < capture_pattern2.length) {
                        break;
                    }
                    ++count;
                    final int header_type_flag = inStream.read();
                    if (header_type_flag == -1) {
                        break;
                    }
                    long absolute_granule_position = 0L;
                    for (i = 0; i < 8; ++i) {
                        final long b2 = inStream.read();
                        if (b2 == -1L) {
                            break;
                        }
                        absolute_granule_position |= b2 << 8 * i;
                    }
                    if (i < 8) {
                        break;
                    }
                    if ((header_type_flag & 0x6) == 0x0) {
                        continue;
                    }
                    this.sampleNum = absolute_granule_position;
                }
            }
        }
    }
    
    long getSeconds() {
        if (this.audio_sample_rate > 0) {
            return this.sampleNum / this.audio_sample_rate;
        }
        return 0L;
    }
    
    public static void main(final String[] args) throws Exception {
        new Ogg(args[0]).showInfo();
    }
    
    public int read32Bits(final InputStream inStream) throws Exception {
        int n = 0;
        for (int i = 0; i < 4; ++i) {
            final int b = inStream.read();
            if (b == -1) {
                throw new Exception("Unexpected end of input stream");
            }
            n |= b << 8 * i;
        }
        return n;
    }
    
    public void showInfo() {
        System.out.println("audio_channels = " + this.audio_channels);
        System.out.println("audio_sample_rate = " + this.audio_sample_rate);
        System.out.println("dataLength = " + this.dataLength);
        System.out.println("seconds = " + this.getSeconds());
        System.out.println("headerStart = " + this.headerStart);
        System.out.println("vorbis_version = " + this.vorbis_version);
    }
}
