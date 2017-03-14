/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.midi;

import javax.sound.midi.ShortMessage;

public abstract class MidiNote extends LXShortMessage {

  protected MidiNote(ShortMessage message, int command) {
    super(message, command);
  }

  private final static String[] PITCHES = {
    "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
  };

  public static String getPitchString(int pitch) {
    return PITCHES[pitch % 12] +  Integer.toString(pitch/12);
  }

  public String getPitchString() {
    return getPitchString(getPitch());
  }

  public int getPitch() {
    return getData1();
  }

  public int getVelocity() {
    return getData2();
  }
}