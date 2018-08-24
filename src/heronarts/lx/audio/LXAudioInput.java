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
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.audio;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import heronarts.lx.LX;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;

public class LXAudioInput extends LXAudioComponent implements LXOscComponent, LineListener {

  private AudioFormat format = STEREO;

  private final LX lx;

  private TargetDataLine line;

  public final DiscreteParameter device;

  private boolean closed = true;
  private boolean stopped = false;

  private InputThread inputThread = null;

  private class InputThread extends Thread {

    private final AudioFormat format;

    private final byte[] rawBytes;

    private InputThread(AudioFormat format) {
      super("LXAudioEngine Input Thread");
      this.format = format;
      this.rawBytes = new byte[this.format == MONO ? MONO_BUFFER_SIZE : STEREO_BUFFER_SIZE];
    }

    @Override
    public void run() {
      while (!closed) {
        while (stopped) {
          if (closed) {
            return;
          }
          try {
            synchronized (this) {
              wait();
            }
          } catch (InterruptedException ix) {}
        }

        // Read from the audio line
        line.read(rawBytes, 0, rawBytes.length);

        if (this.format == MONO) {
          mix.putSamples(rawBytes, 0, MONO_BUFFER_SIZE, MONO_FRAME_SIZE);
        } else {
          left.putSamples(rawBytes, 0, STEREO_BUFFER_SIZE, STEREO_FRAME_SIZE);
          right.putSamples(rawBytes, 2, STEREO_BUFFER_SIZE, STEREO_FRAME_SIZE);
          mix.computeMix(left, right);
        }
      }
    }

  };

  public static class Device {
    public final Mixer.Info info;
    public final Mixer mixer;
    public final DataLine.Info line;

    Device(Mixer.Info info, Mixer mixer, DataLine.Info dataLine) {
      this.info = info;
      this.mixer = mixer;
      this.line = dataLine;
    }

    @Override
    public String toString() {
      return this.info.getName();
    }

    public boolean isAvailable() {
      return true;
    }

    public static class Unavailable extends Device {
      Unavailable() {
        super(null, null, null);
      }

      @Override
      public String toString() {
        return "No Input";
      }

      @Override
      public boolean isAvailable() {
        return false;
      }

    }
  }

  LXAudioInput(LX lx) {
    super(lx, "Audio Input");
    this.lx = lx;

    // Find system input devices...
    List<Device> devices = new ArrayList<Device>();
    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
    for (Mixer.Info mixerInfo : mixers) {
      Mixer mixer = AudioSystem.getMixer(mixerInfo);
      Line.Info[] targetLines = mixer.getTargetLineInfo();
      for (Line.Info lineInfo : targetLines) {
        if (lineInfo instanceof DataLine.Info) {
          devices.add(new Device(mixerInfo, mixer, (DataLine.Info) lineInfo));
          break;
        }
      }
    }
    if (devices.size() == 0) {
      devices.add(new Device.Unavailable());
    }

    this.device = new DiscreteParameter("Device", devices.toArray(new Device[] {}));
    addParameter("device", this.device);

  }

  public String getOscAddress() {
    return "/lx/audio/input";
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);
    if (p == this.device) {
      close();
      if (this.lx.engine.audio.enabled.isOn()) {
        open();
      }
    }
  }

  public AudioFormat getFormat() {
    return this.format;
  }

  void open() {
    if (this.line == null) {
      Device device = (Device) this.device.getObject();
      if (!device.isAvailable()) {
        return;
      }

      DataLine.Info info = null;
      if (device.mixer.isLineSupported(STEREO_TARGET_LINE)) {
        this.format = STEREO;
        info = STEREO_TARGET_LINE;
      } else if (device.mixer.isLineSupported(MONO_TARGET_LINE)) {
        this.format = MONO;
        info = MONO_TARGET_LINE;
      } else {
        System.err.println("Device " + device + " does not support mono/stereo 16-bit input");
        return;
      }
      try {
        this.line = (TargetDataLine) device.mixer.getLine(info);
        this.line.addLineListener(this);
        this.line.open(this.format, 2 * (this.format == MONO ? MONO_BUFFER_SIZE : STEREO_BUFFER_SIZE));
        this.line.start();
        this.stopped = false;
        this.closed = false;
        this.inputThread = new InputThread(this.format);
        this.inputThread.start();
      } catch (Exception x) {
        System.err.println(x.getLocalizedMessage());
        return;
      }
    }
  }

  void start() {
    if (this.line == null) {
      throw new IllegalStateException("Cannot start() LXAudioInput before open()");
    }
    this.stopped = false;
    this.line.start();
    synchronized (this.inputThread) {
      this.inputThread.notify();
    }
  }

  void stop() {
    if (this.line == null) {
      throw new IllegalStateException("Cannot stop() LXAudioInput before open()");
    }
    this.stopped = true;
    this.line.stop();
  }

  void close() {
    if (this.line != null) {
      this.line.flush();
      stop();
      this.closed = true;
      this.line.close();
      this.line = null;
      synchronized (this.inputThread) {
        this.inputThread.notify();
      }
      try {
        this.inputThread.join();
      } catch (InterruptedException ix) {
        ix.printStackTrace();
      }
      this.inputThread = null;
    }
  }

  @Override
  public void update(LineEvent event) {
    LineEvent.Type type = event.getType();
    if (type == LineEvent.Type.OPEN) {
    } else if (type == LineEvent.Type.START) {
    } else if (type == LineEvent.Type.STOP) {
      if (this.line == event.getLine()) {
        this.stopped = true;
      }
    } else if (type == LineEvent.Type.CLOSE) {
      if (this.line == event.getLine()) {
        this.closed = true;
      }
    }
  }

}
