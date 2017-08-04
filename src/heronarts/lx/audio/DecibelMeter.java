/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
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

import heronarts.lx.LXUtils;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;

/**
 * A DecibelMeter is a modulator that returns the level of an audio signal. Gain
 * may be applied to the signal. A decibel range is given in which values are
 * normalized from 0 to 1. Raw decibel values can be accessed if desired.
 */
public class DecibelMeter extends LXModulator implements LXNormalizedParameter {

  protected static final double LOG_10 = Math.log(10);

  protected LXAudioBuffer buffer;

  /**
   * Gain of the meter, in decibels
   */
  public final BoundedParameter gain = (BoundedParameter)
    new BoundedParameter("Gain", 0, -48, 48)
    .setDescription("Sets the gain of the meter in dB")
    .setUnits(LXParameter.Units.DECIBELS);

  /**
   * Range of the meter, in decibels.
   */
  public final BoundedParameter range = (BoundedParameter)
    new BoundedParameter("Range", 48, 6, 96)
    .setDescription("Sets the range of the meter in dB")
    .setUnits(LXParameter.Units.DECIBELS);

  /**
   * Meter attack time, in milliseconds
   */
  public final BoundedParameter attack = (BoundedParameter)
    new BoundedParameter("Attack", 10, 0, 100)
    .setDescription("Sets the attack time of the meter response")
    .setUnits(LXParameter.Units.MILLISECONDS);

  /**
   * Meter release time, in milliseconds
   */
  public final BoundedParameter release = (BoundedParameter)
    new BoundedParameter("Release", 100, 0, 1000)
    .setDescription("Sets the release time of the meter response")
    .setExponent(2)
    .setUnits(LXParameter.Units.MILLISECONDS);

  private final static float PEAK_HOLD = 250;

  protected float attackGain;
  protected float releaseGain;

  private float rmsRaw = 0;
  private float rmsLevel = 0;
  private double dbLevel = -96;

  private float rmsPeak = 0;
  private double dbPeak = 0;
  private double normalizedPeak = 0;
  private long peakMillis = 0;

  /**
   * Default constructor, creates a meter with unity gain and 72dB dynamic range
   *
   * @param source Audio source to meter
   */
  public DecibelMeter(LXAudioBuffer buffer) {
    this("Meter", buffer);
  }

  /**
   * Default constructor, creates a meter with unity gain and 72dB dynamic range
   *
   * @param label Label
   * @param buffer Audio buffer to meter
   */
  public DecibelMeter(String label, LXAudioBuffer buffer) {
    super(label);
    this.buffer = buffer;
    addParameter("gain", this.gain);
    addParameter("range", this.range);
    addParameter("attack", this.attack);
    addParameter("release", this.release);
  }

  public DecibelMeter setBuffer(LXAudioBuffer buffer) {
    this.buffer = buffer;
    return this;
  }

  public double getExponent() {
    throw new UnsupportedOperationException("DecibelMeter does not support exponent");
  }

  /**
   * Return raw underlying levels, no attack/gain smoothing
   *
   * @return
   */
  public float getRaw() {
    return this.rmsRaw;
  }

  /**
   * @return Raw decibel value of the meter
   */
  public double getDecibels() {
    return this.dbLevel;
  }

  /**
   * @return Raw decibel value of the meter as a float
   */
  public float getDecibelsf() {
    return (float) getDecibels();
  }

  /**
   * @return A value for the audio meter from 0 to 1 with quadratic scaling
   */
  public double getSquare() {
    double norm = getValue();
    return norm * norm;
  }

  /**
   * @return Quadratic scaled value as a float
   */
  public float getSquaref() {
    return (float) getSquare();
  }

  @Override
  protected double computeValue(double deltaMs) {
    double releaseValue = this.release.getValue();
    this.attackGain = (float) Math.exp(-deltaMs / this.attack.getValue());
    this.releaseGain = (float) Math.exp(-deltaMs / releaseValue);

    this.rmsRaw = this.buffer.getRms();
    float rmsGain = (this.rmsRaw >= this.rmsLevel) ? this.attackGain : this.releaseGain;
    this.rmsLevel = this.rmsRaw + rmsGain * (this.rmsLevel - this.rmsRaw);

    if (this.rmsRaw > this.rmsPeak) {
      this.rmsPeak = this.rmsRaw;
      this.peakMillis = 0;
    } else {
      this.peakMillis += deltaMs;
      if (this.peakMillis > PEAK_HOLD) {
        double peakReleaseTime = Math.min(deltaMs, this.peakMillis - PEAK_HOLD);
        float releaseGain = (float) Math.exp(-peakReleaseTime / releaseValue);
        this.rmsPeak *= releaseGain;
      }
    }
    double range = this.range.getValue();
    double gain = this.gain.getValue();

    this.dbPeak = 20 * Math.log(this.rmsPeak) / LOG_10 + gain;
    this.normalizedPeak = LXUtils.constrain(1 + this.dbPeak / range, 0, 1);

    this.dbLevel = 20 * Math.log(this.rmsLevel) / LOG_10 + gain;
    return LXUtils.constrain(1 + this.dbLevel / range, 0, 1);
  }

  @Override
  public LXNormalizedParameter setNormalized(double value) {
    throw new UnsupportedOperationException("Cannot setNormalized on DecibelMeter");
  }

  public double getPeak() {
    return this.normalizedPeak;
  }

  public float getPeakf() {
    return (float) getPeak();
  }

  @Override
  public double getNormalized() {
    return getValue();
  }

  @Override
  public float getNormalizedf() {
    return (float) getNormalized();
  }
}
