/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.lx.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;

public class LXAudioComponent extends LXComponent {

  protected static final int SAMPLE_RATE = 44100;
  protected static final int SAMPLE_BUFFER_SIZE = 512;

  protected static final int BYTES_PER_SAMPLE = 2;
  protected static final int BITS_PER_SAMPLE = BYTES_PER_SAMPLE * Byte.SIZE;

  protected static final int MONO_FRAME_SIZE = BYTES_PER_SAMPLE;
  protected static final int STEREO_FRAME_SIZE = BYTES_PER_SAMPLE * 2;
  protected static final int MONO_BUFFER_SIZE = SAMPLE_BUFFER_SIZE * MONO_FRAME_SIZE;
  protected static final int STEREO_BUFFER_SIZE = SAMPLE_BUFFER_SIZE * STEREO_FRAME_SIZE;

  protected static final AudioFormat MONO = new AudioFormat(SAMPLE_RATE, 8*BYTES_PER_SAMPLE, 1, true, false);
  protected static final AudioFormat STEREO = new AudioFormat(SAMPLE_RATE, 8*BYTES_PER_SAMPLE, 2, true, false);

  protected static final DataLine.Info MONO_SOURCE_LINE = new DataLine.Info(SourceDataLine.class, MONO);
  protected static final DataLine.Info STEREO_SOURCE_LINE = new DataLine.Info(SourceDataLine.class, STEREO);

  protected static final DataLine.Info MONO_TARGET_LINE = new DataLine.Info(TargetDataLine.class, MONO);
  protected static final DataLine.Info STEREO_TARGET_LINE = new DataLine.Info(TargetDataLine.class, STEREO);

  public final LXAudioBuffer left = new LXAudioBuffer(SAMPLE_BUFFER_SIZE, SAMPLE_RATE);
  public final LXAudioBuffer right = new LXAudioBuffer(SAMPLE_BUFFER_SIZE, SAMPLE_RATE);
  public final LXAudioBuffer mix = new LXAudioBuffer(SAMPLE_BUFFER_SIZE, SAMPLE_RATE);

  LXAudioComponent(LX lx, String label) {
    super(lx, label);
  }
}
