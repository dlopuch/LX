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

package heronarts.lx.clip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import heronarts.lx.LX;
import heronarts.lx.LXBus;
import heronarts.lx.LXComponent;
import heronarts.lx.LXEffect;
import heronarts.lx.LXRunnableComponent;
import heronarts.lx.LXSerializable;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.MutableParameter;

public abstract class LXClip extends LXRunnableComponent implements LXComponent.Renamable, LXBus.Listener {

  public interface Listener {
    public void parameterLaneAdded(LXClip clip, ParameterClipLane lane);
    public void parameterLaneRemoved(LXClip clip, ParameterClipLane lane);
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  double cursor = 0;

  public final MutableParameter length = (MutableParameter)
    new MutableParameter("Length", 0)
    .setDescription("The length of the clip")
    .setUnits(LXParameter.Units.MILLISECONDS);

  public final BooleanParameter loop = new BooleanParameter("Loop")
  .setDescription("Whether to loop the clip");

  protected final List<LXClipLane> mutableLanes = new ArrayList<LXClipLane>();
  public final List<LXClipLane> lanes = Collections.unmodifiableList(this.mutableLanes);

  public final LXBus bus;

  private int index;

  protected final LXParameterListener parameterRecorder = new LXParameterListener() {
    public void onParameterChanged(LXParameter p) {
      if (isRunning() && bus.arm.isOn()) {
        LXListenableNormalizedParameter parameter = (LXListenableNormalizedParameter) p;
        ParameterClipLane lane = getParameterLane(parameter, true);
        lane.appendEvent(new ParameterClipEvent(lane, parameter));
      }
    }
  };

  public LXClip(LX lx, LXBus bus, int index) {
    this(lx, bus, index, true);
  }

  protected LXClip(LX lx, LXBus bus, int index, boolean registerListener) {
    super(lx);
    this.label.setDescription("The name of this clip");
    this.bus = bus;
    this.index = index;
    setParent(this.bus);
    addParameter("length", this.length);
    addParameter("loop", this.loop);

    for (LXEffect effect : bus.effects) {
      registerComponent(effect);
    }
    if (registerListener) {
      bus.addListener(this);
    }
  }

  @Override
  public void dispose() {
    for (LXEffect effect : bus.effects) {
      unregisterComponent(effect);
    }
    this.bus.removeListener(this);
    this.mutableLanes.clear();
    this.listeners.clear();
    super.dispose();
  }


  public double getLength() {
    return this.length.getValue();
  }

  private ParameterClipLane getParameterLane(LXNormalizedParameter parameter, boolean create) {
    for (LXClipLane lane : this.lanes) {
      if (lane instanceof ParameterClipLane) {
        if (((ParameterClipLane) lane).parameter == parameter) {
          return (ParameterClipLane) lane;
        }
      }
    }
    if (create) {
      ParameterClipLane lane = new ParameterClipLane(this, parameter);
      this.mutableLanes.add(lane);
      for (Listener listener : this.listeners) {
        listener.parameterLaneAdded(this, lane);
      }
      return lane;
    }
    return null;
  }

  public LXClip removeParameterLane(ParameterClipLane lane) {
    this.mutableLanes.remove(lane);
    for (Listener listener : this.listeners) {
      listener.parameterLaneRemoved(this, lane);
    }
    return this;
  }

  public LXClip addListener(Listener listener) {
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Already registered listener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  public LXClip removeListener(Listener listener) {
    this.listeners.remove(listener);
    return this;
  }

  public double getCursor() {
    return this.cursor;
  }

  public double getBasis() {
    double lengthValue = this.length.getValue();
    if (lengthValue > 0) {
      return this.cursor / lengthValue;
    }
    return 0;
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    super.onParameterChanged(p);
    if (p == this.trigger) {
      this.cursor = 0;
    } else if (p == this.running) {
      if (this.running.isOn()) {
        for (LXClip clip : this.bus.clips) {
          if (clip != null && clip != this) {
            clip.stop();
          }
        }
        if (this.bus.arm.isOn()) {
          // Start recording a new clip.
          // TODO(mcslee): toggle an overdub / replace recording mode
          this.cursor = 0;
          this.length.setValue(0);
          clearLanes();
          onStartRecording();
        }
      } else {
        // Finished recording
        if (this.bus.arm.isOn()) {
          this.length.setValue(this.cursor);
          this.bus.arm.setValue(false);
        }
      }
    }
  }

  protected void onStartRecording() {
    // Subclasses may override
  }

  private void clearLanes() {
    Iterator<LXClipLane> iter = this.mutableLanes.iterator();
    while (iter.hasNext()) {
      LXClipLane lane = iter.next();
      if (lane instanceof ParameterClipLane) {
        iter.remove();
        for (Listener listener : this.listeners) {
          listener.parameterLaneRemoved(this, (ParameterClipLane) lane);
        }
      } else {
        lane.clear();
      }
    }
  }

  protected void registerComponent(LXComponent component) {
    for (LXParameter p : component.getParameters()) {
      if (p instanceof LXListenableNormalizedParameter) {
        ((LXListenableNormalizedParameter) p).addListener(this.parameterRecorder);
      }
    }
  }

  protected void unregisterComponent(LXComponent component) {
    for (LXParameter p : component.getParameters()) {
      if (p instanceof LXListenableNormalizedParameter) {
        ((LXListenableNormalizedParameter) p).removeListener(this.parameterRecorder);
        ParameterClipLane lane = getParameterLane((LXNormalizedParameter) p, false);
        if (lane != null) {
          this.mutableLanes.remove(lane);
          for (Listener listener : this.listeners) {
            listener.parameterLaneRemoved(this, lane);
          }
        }
      }
    }
  }

  public int getIndex() {
    return this.index;
  }

  public LXClip setIndex(int index) {
    this.index = index;
    return this;
  }


  private void advanceCursor(double from, double to) {
    for (LXClipLane lane : this.lanes) {
      lane.advanceCursor(from, to);
    }
  }

  @Override
  protected void run(double deltaMs) {
    double nextCursor = this.cursor + deltaMs;
    double lengthValue = this.length.getValue();
    if (!this.bus.arm.isOn()) {
      // TODO(mcslee): make this more efficient, keep track of our indices...
      advanceCursor(this.cursor, nextCursor);
      while (nextCursor > lengthValue) {
        if (!this.loop.isOn() || (lengthValue == 0)) {
          this.cursor = nextCursor = lengthValue;
          stop();
          break;
        } else {
          nextCursor -= lengthValue;
          advanceCursor(0, nextCursor);
        }
      }
    } else {
      this.length.setValue(nextCursor);
    }
    this.cursor = nextCursor;
  }

  @Override
  public void effectAdded(LXBus channel, LXEffect effect) {
    registerComponent(effect);
  }

  @Override
  public void effectRemoved(LXBus channel, LXEffect effect) {
    unregisterComponent(effect);
  }

  @Override
  public void effectMoved(LXBus channel, LXEffect effect) {}

  private static final String KEY_LANES = "parameterLanes";
  public static final String KEY_INDEX = "index";

  @Override
  public void load(LX lx, JsonObject obj) {
    clearLanes();
    if (obj.has(KEY_LANES)) {
      JsonArray lanesArr = obj.get(KEY_LANES).getAsJsonArray();
      for (JsonElement laneElement : lanesArr) {
        JsonObject laneObj = laneElement.getAsJsonObject();
        String laneType = laneObj.get(LXClipLane.KEY_LANE_TYPE).getAsString();
        loadLane(lx, laneType, laneObj);
      }
    }
    super.load(lx, obj);
  }

  protected void loadLane(LX lx, String laneType, JsonObject laneObj) {
    if (laneType.equals(LXClipLane.VALUE_LANE_TYPE_PARAMETER)) {
      LXComponent component = lx.getComponent(laneObj.get(KEY_COMPONENT_ID).getAsInt());
      String path = laneObj.get(KEY_PARAMETER_PATH).getAsString();
      LXParameter parameter = component.getParameter(path);
      LXClipLane lane = getParameterLane((LXNormalizedParameter) parameter, true);
      lane.load(lx, laneObj);
    }
  }

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
    obj.addProperty(KEY_INDEX, this.index);
    obj.add(KEY_LANES, LXSerializable.Utils.toArray(lx, this.lanes));
  }
}
