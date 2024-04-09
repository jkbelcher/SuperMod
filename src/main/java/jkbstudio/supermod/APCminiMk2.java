/**
 * Copyright 2024- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package jkbstudio.supermod;

import java.util.HashMap;
import java.util.Map;

import heronarts.lx.LX;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.clip.LXClip;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiControlChange;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import jkbstudio.supermod.SuperMod.Device.ModParameter;

public class APCminiMk2 extends LXMidiSurface implements LXMidiSurface.Bidirectional {

  public static final String DEVICE_NAME = "APC mini mk2 Control";

  public static final int NUM_CHANNELS = 8;
  public static final double PARAMETER_INCREMENT_AMOUNT = 0.1;

  // CCs
  public static final int CHANNEL_FADER = 48;
  public static final int CHANNEL_FADER_MAX = CHANNEL_FADER + NUM_CHANNELS - 1;
  public static final int MASTER_FADER = 56;


  // Notes
  public static final int CLIP_LAUNCH = 0;
  public static final int CLIP_LAUNCH_ROWS = 8;
  public static final int CLIP_LAUNCH_COLUMNS = NUM_CHANNELS;
  public static final int CLIP_LAUNCH_NUM = CLIP_LAUNCH_ROWS * CLIP_LAUNCH_COLUMNS;
  public static final int CLIP_LAUNCH_MAX = CLIP_LAUNCH + CLIP_LAUNCH_NUM - 1;

  public static final int SUPERMOD_NUM_MODS = 32;
  public static final int SUPERMOD_TEMPLATE_ROWS = 4;
  public static final int SUPERMOD_TEMPLATE_COLUMNS = 8;
  public static final int SUPERMOD_INDICATOR_ROWS = 4;
  public static final int SUPERMOD_INDICATOR_COLUMNS = 4;

  public static final int FADER_CTRL_VOLUME = 100;
  public static final int FADER_CTRL_PAN = 101;
  public static final int FADER_CTRL_SEND = 102;
  public static final int FADER_CTRL_DEVICE = 103;

  public static final int SELECT_UP = 104;
  public static final int SELECT_DOWN = 105;
  public static final int SELECT_LEFT = 106;
  public static final int SELECT_RIGHT = 107;

  public static final int CHANNEL_BUTTON = FADER_CTRL_VOLUME;
  public static final int CHANNEL_BUTTON_MAX = CHANNEL_BUTTON + NUM_CHANNELS - 1;

  public static final int CLIP_STOP = 112;
  public static final int SOLO = 113;
  public static final int MUTE = 114;
  public static final int REC_ARM = 115;
  public static final int SELECT = 116;
  public static final int DRUM = 117;
  public static final int NOTE = 118;
  public static final int STOP_ALL_CLIPS = 119;

  public static final int SCENE_LAUNCH = CLIP_STOP;
  public static final int SCENE_LAUNCH_NUM = 6;
  public static final int SCENE_LAUNCH_MAX = SCENE_LAUNCH + SCENE_LAUNCH_NUM - 1;

  public static final int TOGGLE_CLIPS = NOTE;
  public static final int TOGGLE_PARAMETERS = STOP_ALL_CLIPS;

  public static final int PARAMETER_COLUMNS = 8;
  public static final int PARAMETER_COLUMN_STRIDE = 1;
  public static final int PARAMETER_ROWS = 2;
  public static final int PARAMETER_ROW_STRIDE = -4;
  public static final int PARAMETER_NUM = PARAMETER_COLUMNS * PARAMETER_ROWS;
  public static final int PARAMETER_START = (CLIP_LAUNCH_ROWS - 1) * CLIP_LAUNCH_COLUMNS + CLIP_LAUNCH;

  // Notes in combination with Shift
  public static final int SHIFT = 122;

  public static final int CHANNEL_BUTTON_FOCUS = FADER_CTRL_VOLUME;
  public static final int CHANNEL_BUTTON_ENABLED = FADER_CTRL_PAN;
  public static final int CHANNEL_BUTTON_CUE = FADER_CTRL_SEND;
  public static final int CHANNEL_BUTTON_ARM = FADER_CTRL_DEVICE;
  public static final int CHANNEL_BUTTON_CROSSFADEGROUP = 0;


  // LEDs

  // Brightness and Behavior are set by MIDI Channel
  // Single color (perimeter buttons)
  public static final int MIDI_CHANNEL_SINGLE = 0;
  // Multi color (grid buttons)
  public static final int MIDI_CHANNEL_MULTI_10_PERCENT = 0;
  public static final int MIDI_CHANNEL_MULTI_25_PERCENT = 1;
  public static final int MIDI_CHANNEL_MULTI_50_PERCENT = 2;
  public static final int MIDI_CHANNEL_MULTI_65_PERCENT = 3;
  public static final int MIDI_CHANNEL_MULTI_75_PERCENT = 4;
  public static final int MIDI_CHANNEL_MULTI_90_PERCENT = 5;
  public static final int MIDI_CHANNEL_MULTI_100_PERCENT = 6;
  public static final int MIDI_CHANNEL_MULTI_PULSE_SIXTEENTH = 7;
  public static final int MIDI_CHANNEL_MULTI_PULSE_EIGTH = 8;
  public static final int MIDI_CHANNEL_MULTI_PULSE_QUARTER = 9;
  public static final int MIDI_CHANNEL_MULTI_PULSE_HALF = 10;
  public static final int MIDI_CHANNEL_MULTI_BLINK_TWENTYFOURTH = 11;
  public static final int MIDI_CHANNEL_MULTI_BLINK_SIXTEENTH = 12;
  public static final int MIDI_CHANNEL_MULTI_BLINK_EIGTH = 13;
  public static final int MIDI_CHANNEL_MULTI_BLINK_QUARTER = 14;
  public static final int MIDI_CHANNEL_MULTI_BLINK_HALF = 15;

  // Single AND multi color buttons
  public static final int LED_OFF = 0;

  // Single color buttons
  public static final int LED_ON = 1;
  public static final int LED_BLINK = 2;

  // Multi color buttons
  // TODO: There are 127 possible colors
  public static final int LED_COLOR_OFF = 0;
  public static final int LED_GRAY_50 = 1;
  public static final int LED_GRAY_75 = 2;
  public static final int LED_WHITE = 3;
  public static final int LED_RED = 5;
  public static final int LED_YELLOW = 12;
  public static final int LED_GREEN = 21;
  public static final int LED_BLUE = 67;

  // Configurable color options
  public static final int LED_PATTERN_ACTIVE_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_PATTERN_ACTIVE_COLOR = LED_RED;
  public static final int LED_PATTERN_TRANSITION_BEHAVIOR = MIDI_CHANNEL_MULTI_PULSE_SIXTEENTH;
  public static final int LED_PATTERN_TRANSITION_COLOR = LED_RED;
  public static final int LED_PATTERN_FOCUSED_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_PATTERN_FOCUSED_COLOR = LED_GREEN;
  public static final int LED_PATTERN_INACTIVE_BEHAVIOR = MIDI_CHANNEL_MULTI_50_PERCENT;
  public static final int LED_PATTERN_INACTIVE_COLOR = LED_WHITE;

  public static final int LED_CLIP_INACTIVE_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_CLIP_INACTIVE_COLOR = LED_GRAY_50;
  public static final int LED_CLIP_PLAY_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_CLIP_PLAY_COLOR = LED_GREEN;
  public static final int LED_CLIP_ARM_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_CLIP_ARM_COLOR = LED_RED;
  public static final int LED_CLIP_RECORD_BEHAVIOR = MIDI_CHANNEL_MULTI_BLINK_EIGTH;
  public static final int LED_CLIP_RECORD_COLOR = LED_RED;

  public static final int LED_PARAMETER_INCREMENT_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_PARAMETER_INCREMENT_COLOR = LED_GREEN;
  public static final int LED_PARAMETER_DECREMENT_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_PARAMETER_DECREMENT_COLOR = LED_YELLOW;
  public static final int LED_PARAMETER_RESET_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_PARAMETER_RESET_COLOR = LED_RED;
  public static final int LED_PARAMETER_ISDEFAULT_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_PARAMETER_ISDEFAULT_COLOR = LED_OFF;

  public static final int LED_SUPERMOD_TEMPLATE_ACTIVE_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_SUPERMOD_TEMPLATE_ACTIVE_COLOR = LED_GREEN;
  public static final int LED_SUPERMOD_TEMPLATE_INACTIVE_BEHAVIOR = MIDI_CHANNEL_MULTI_50_PERCENT;
  public static final int LED_SUPERMOD_TEMPLATE_INACTIVE_COLOR_LOCAL = LED_WHITE;
  public static final int LED_SUPERMOD_TEMPLATE_INACTIVE_COLOR_GLOBAL = LED_BLUE;

  public static final int LED_SUPERMOD_MODULATION_NONE_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_SUPERMOD_MODULATION_NONE_COLOR = LED_WHITE;
  public static final int LED_SUPERMOD_MODULATION_SUPERMOD_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_SUPERMOD_MODULATION_SUPERMOD_COLOR = LED_RED;
  public static final int LED_SUPERMOD_MODULATION_OTHER_BEHAVIOR = MIDI_CHANNEL_MULTI_100_PERCENT;
  public static final int LED_SUPERMOD_MODULATION_OTHER_COLOR = LED_YELLOW;

  public enum ChannelButtonMode {
    ARM,
    CROSSFADEGROUP,
    CUE,
    ENABLED,
    FOCUS
  };

  private ChannelButtonMode channelButtonMode = ChannelButtonMode.FOCUS;

  public enum GridMode {
    PATTERNS,
    PARAMETERS,
    CLIPS
  };

  private GridMode gridMode = GridMode.PATTERNS;

  private boolean shiftOn = false;

  private final Map<LXAbstractChannel, ChannelListener> channelListeners = new HashMap<LXAbstractChannel, ChannelListener>();

  private final DeviceListener deviceListener = new DeviceListener();

  private class DeviceListener implements LXParameterListener {

    private LXDeviceComponent device = null;
    private LXEffect effect = null;
    private LXPattern pattern = null;
    private LXBus channel = null;

    private final LXListenableNormalizedParameter[] knobs = new LXListenableNormalizedParameter[PARAMETER_NUM];
    private final ModParameter[] mods = new ModParameter[SUPERMOD_NUM_MODS];

    DeviceListener() {
      for (int i = 0; i < this.knobs.length; ++i) {
        this.knobs[i] = null;
      }
      for (int i = 0; i < this.mods.length; ++i) {
        this.mods[i] = null;
      }
    }

    void registerChannel(LXBus channel) {
      unregisterChannel();
      this.channel = channel;
      if (channel instanceof LXChannel) {
        ((LXChannel) channel).focusedPattern.addListener(this);
        register(((LXChannel) channel).getFocusedPattern());
      } else if (channel.effects.size() > 0) {
        register(channel.getEffect(0));
      } else {
        register(null);
      }
    }

    void registerPrevious() {
      if (this.effect != null) {
        int effectIndex = this.effect.getIndex();
        if (effectIndex > 0) {
          register(this.effect.getBus().getEffect(effectIndex - 1));
        } else if (this.channel instanceof LXChannel) {
          register(((LXChannel) this.channel).getFocusedPattern());
        }
      }
    }

    void registerNext() {
      if (this.effect != null) {
        int effectIndex = this.effect.getIndex();
        if (effectIndex < this.effect.getBus().effects.size() - 1) {
          register(this.effect.getBus().getEffect(effectIndex + 1));
        }
      } else if (this.pattern != null) {
        if (channel.effects.size() > 0) {
          register(channel.getEffect(0));
        }
      }
    }

    void register(LXDeviceComponent device) {
      if (this.device != device) {
        unregister(false);
        this.device = device;
        if (this.device instanceof LXPattern) {
          this.pattern = (LXPattern) this.device;
        }

        int i = 0;
        int m = 0;
        if (this.device != null) {
          for (LXListenableNormalizedParameter parameter : getDeviceRemoteControls()) {
            if (i >= this.knobs.length) {
              break;
            }
            this.knobs[i] = parameter;
            int patternButton = getPatternButton(i);
            if (parameter != null) {
              parameter.addListener(this);
              if (isGridModeParameters()) {
                sendNoteOn(LED_PARAMETER_INCREMENT_BEHAVIOR, patternButton, LED_PARAMETER_INCREMENT_COLOR);
                sendNoteOn(LED_PARAMETER_DECREMENT_BEHAVIOR, patternButton - CLIP_LAUNCH_COLUMNS, LED_PARAMETER_DECREMENT_COLOR);
                if (parameter.isDefault()) {
                  sendNoteOn(LED_PARAMETER_ISDEFAULT_BEHAVIOR, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_PARAMETER_ISDEFAULT_COLOR);
                } else {
                  sendNoteOn(LED_PARAMETER_RESET_BEHAVIOR, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_PARAMETER_RESET_COLOR);
                }
                sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
              }
            } else {
              if (isGridModeParameters()) {
                sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton, LED_OFF);
                sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
                sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
                sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
              }
            }
            ++i;
          }
          for (ModParameter mod : getDeviceRemoteMods()) {
            if (m >= this.mods.length) {
              break;
            }
            this.mods[m] = mod;
            if (mod != null) {
              mod.addListener(this);
            }
            if (isSuperMod()) {
              sendSMIndicator(mod, m);
            }
            ++m;
          }
          this.device.controlSurfaceSemaphore.increment();
        }
        if (isGridModeParameters()) {
          while (i < this.knobs.length) {
            int patternButton = getPatternButton(i);
            sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton, LED_OFF);
            sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
            sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
            sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
            ++i;
          }
        }
        if (isSuperMod()) {
          while (m < this.mods.length) {
            sendSMIndicator(null, m);
            ++m;
          }
        }
      }
    }

    private LXListenableNormalizedParameter[] getDeviceRemoteControls() {
      return this.device.getRemoteControls();
    }

    private ModParameter[] getDeviceRemoteMods() {
      return SuperMod.current.getRemoteMods(this.device);
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
      if ((this.channel != null) &&
          (this.channel instanceof LXChannel) &&
          (parameter == ((LXChannel)this.channel).focusedPattern)) {
        if ((this.device == null) || (this.device instanceof LXPattern)) {
          register(((LXChannel) this.channel).getFocusedPattern());
        }
      } else if (isGridModeParameters()) {
        for (int i = 0; i < this.knobs.length; ++i) {
          if (parameter == this.knobs[i]) {
            int patternButton = getPatternButton(i);
            if (((LXListenableNormalizedParameter)parameter).isDefault()) {
              sendNoteOn(LED_PARAMETER_ISDEFAULT_BEHAVIOR, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_PARAMETER_ISDEFAULT_COLOR);
            } else {
              sendNoteOn(LED_PARAMETER_RESET_BEHAVIOR, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_PARAMETER_RESET_COLOR);
            }
          }
        }
      } else if (isSuperMod()) {
        for (int i = 0; i < this.mods.length; ++i) {
          if (parameter == this.mods[i]) {
            sendSMIndicator((ModParameter)parameter, i);
          }
        }
      }
    }

    void resend() {
      if (isGridModeParameters()) {
        for (int i = 0; i < this.knobs.length; ++i) {
          LXListenableNormalizedParameter parameter = this.knobs[i];
          int patternButton = getPatternButton(i);
          if (parameter != null) {
            sendNoteOn(LED_PARAMETER_INCREMENT_BEHAVIOR, patternButton, LED_PARAMETER_INCREMENT_COLOR);
            sendNoteOn(LED_PARAMETER_DECREMENT_BEHAVIOR, patternButton - CLIP_LAUNCH_COLUMNS, LED_PARAMETER_DECREMENT_COLOR);
            if (parameter.isDefault()) {
              sendNoteOn(LED_PARAMETER_ISDEFAULT_BEHAVIOR, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_PARAMETER_ISDEFAULT_COLOR);
            } else {
              sendNoteOn(LED_PARAMETER_RESET_BEHAVIOR, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_PARAMETER_RESET_COLOR);
            }
          } else {
            sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton, LED_OFF);
            sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
            sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
          }
          sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
        }
      } else if (isSuperMod()) {
        for (int i = 0; i < this.mods.length; ++i) {
          sendSMIndicator(this.mods[i], i);
        }
      }
    }

    private void sendSMIndicator(ModParameter parameter, int i) {
      int patternButton = getSMIndicatorButton(i);
      if (parameter != null) {
        switch (parameter.getState()) {
          case SUPERMOD:
            sendNoteOn(LED_SUPERMOD_MODULATION_SUPERMOD_BEHAVIOR, patternButton, LED_SUPERMOD_MODULATION_SUPERMOD_COLOR);
            break;
          case OTHER:
            sendNoteOn(LED_SUPERMOD_MODULATION_OTHER_BEHAVIOR, patternButton, LED_SUPERMOD_MODULATION_OTHER_COLOR);
            break;
          case EMPTY:
          default:
            sendNoteOn(LED_SUPERMOD_MODULATION_NONE_BEHAVIOR, patternButton, LED_SUPERMOD_MODULATION_NONE_COLOR);
            break;
        }
      } else {
        sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, patternButton, LED_OFF);
      }
    }

    int getPatternButton(int index) {
      int row = index / PARAMETER_COLUMNS;
      int column = index % PARAMETER_COLUMNS;
      return PARAMETER_START + (row * CLIP_LAUNCH_COLUMNS * PARAMETER_ROW_STRIDE) + (column * PARAMETER_COLUMN_STRIDE);
    }

    /**
     * Returns a MIDI note for an indicator grid button, given a mod/parameter index.
     *
     * SuperMod indicators will be the bottom left quadrant of 16 buttons
     * followed by the bottom right quadrant of 16 buttons.
     * Visually this will align with two 4x4 midi surfaces.
     */
    int getSMIndicatorButton(int index) {
      int set = index / 16;
      index = index % 16;
      int row = index / SUPERMOD_INDICATOR_COLUMNS;
      int column = index % SUPERMOD_INDICATOR_COLUMNS + (set * SUPERMOD_INDICATOR_COLUMNS);
      return CLIP_LAUNCH + ((SUPERMOD_INDICATOR_ROWS - 1 - row) * CLIP_LAUNCH_COLUMNS) + column;
    }

    void onParameterButton(int columnIndex, int rowIndex) {
      int paramIndex = 0;
      int button = rowIndex;
      while (button > 3) {
        paramIndex += PARAMETER_COLUMNS;
        button -= 4;
      }
      paramIndex += columnIndex;

      LXListenableNormalizedParameter param = this.knobs[paramIndex];
      if (param != null) {
        switch (button) {
          case 0:
            if (param instanceof BooleanParameter) {
              ((BooleanParameter)param).setValue(true);
            } else if (param instanceof DiscreteParameter) {
              ((DiscreteParameter)param).increment();
            } else {
              param.setNormalized(param.getNormalized() + PARAMETER_INCREMENT_AMOUNT);
            }
            break;
          case 1:
            if (param instanceof BooleanParameter) {
              ((BooleanParameter)param).setValue(false);
            } else if (param instanceof DiscreteParameter) {
              ((DiscreteParameter)param).decrement();
            } else {
              param.setNormalized(param.getNormalized() - PARAMETER_INCREMENT_AMOUNT);
            }
            break;
          case 2:
            param.reset();
            break;
        }
      }
    }

    public void onSMIndicatorButton(int index) {
      ModParameter mod = this.mods[index];
      if (mod != null) {
        mod.clearModulation();
      }
    }

    private void unregister(boolean clearButtons) {
      if (this.device != null) {
        for (int i = 0; i < this.knobs.length; ++i) {
          if (this.knobs[i] != null) {
            this.knobs[i].removeListener(this);
            this.knobs[i] = null;
            if (isGridModeParameters() && clearButtons) {
              int patternButton = getPatternButton(i);
              sendNoteOn(MIDI_CHANNEL_SINGLE, patternButton, LED_OFF);
              sendNoteOn(MIDI_CHANNEL_SINGLE, patternButton - CLIP_LAUNCH_COLUMNS, LED_OFF);
              sendNoteOn(MIDI_CHANNEL_SINGLE, patternButton - (CLIP_LAUNCH_COLUMNS * 2), LED_OFF);
              sendNoteOn(MIDI_CHANNEL_SINGLE, patternButton - (CLIP_LAUNCH_COLUMNS * 3), LED_OFF);
            }
          }
        }
        for (int m = 0; m < this.mods.length; ++m) {
          if (this.mods[m] != null) {
            this.mods[m].removeListener(this);
            this.mods[m] = null;
            if (isSuperMod() && clearButtons) {
              sendSMIndicator(null, m);
            }
          }
        }
        this.device.controlSurfaceSemaphore.decrement();
      }
      this.pattern = null;
      this.effect = null;
      this.device = null;
    }

    private void unregisterChannel() {
      if (this.channel != null) {
        if (this.channel instanceof LXChannel) {
          ((LXChannel) this.channel).focusedPattern.removeListener(this);
        }
      }
      this.channel = null;
    }

    private void dispose() {
      unregister(true);
      unregisterChannel();
    }

  }

  private class ChannelListener implements LXChannel.Listener, LXBus.ClipListener, LXParameterListener {

    private final LXAbstractChannel channel;

    ChannelListener(LXAbstractChannel channel) {
      this.channel = channel;
      if (channel instanceof LXChannel) {
        ((LXChannel) channel).addListener(this);
      } else {
        channel.addListener(this);
      }
      channel.addClipListener(this);
      channel.cueActive.addListener(this);
      channel.enabled.addListener(this);
      channel.crossfadeGroup.addListener(this);
      channel.arm.addListener(this);
      if (channel instanceof LXChannel) {
        LXChannel c = (LXChannel) channel;
        c.focusedPattern.addListener(this);
        c.controlSurfaceFocusLength.setValue(CLIP_LAUNCH_ROWS);
        int focusedPatternIndex = c.getFocusedPatternIndex();
        c.controlSurfaceFocusIndex.setValue(focusedPatternIndex < CLIP_LAUNCH_ROWS ? 0 : (focusedPatternIndex - CLIP_LAUNCH_ROWS + 1));
      }
      for (LXClip clip : this.channel.clips) {
        if (clip != null) {
          clip.running.addListener(this);
        }
      }
    }

    public void dispose() {
      if (this.channel instanceof LXChannel) {
        ((LXChannel) this.channel).removeListener(this);
      } else {
        this.channel.removeListener(this);
      }
      this.channel.removeClipListener(this);
      this.channel.cueActive.removeListener(this);
      this.channel.enabled.removeListener(this);
      this.channel.crossfadeGroup.removeListener(this);
      this.channel.arm.removeListener(this);
      if (this.channel instanceof LXChannel) {
        LXChannel c = (LXChannel) this.channel;
        c.focusedPattern.removeListener(this);
        c.controlSurfaceFocusLength.setValue(0);
        c.controlSurfaceFocusIndex.setValue(0);
      }
      for (LXClip clip : this.channel.clips) {
        if (clip != null) {
          clip.running.removeListener(this);
        }
      }
    }

    public void onParameterChanged(LXParameter p) {
      int index = this.channel.getIndex();
      if (index >= CLIP_LAUNCH_COLUMNS) {
        return;
      }

      if (p == this.channel.cueActive) {
        if (channelButtonMode == ChannelButtonMode.CUE) {
          sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, this.channel.cueActive.isOn() ? LED_ON : LED_OFF);
        }
      } else if (p == this.channel.enabled) {
        if (channelButtonMode == ChannelButtonMode.ENABLED) {
          sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, this.channel.enabled.isOn() ? LED_ON : LED_OFF);
        }
      } else if (p == this.channel.crossfadeGroup) {
        // Button press toggles through the 3 modes. Button does not stay lit.
        sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, LED_OFF);
      } else if (p == this.channel.arm) {
        sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, channel.arm.isOn() ? LED_ON : LED_OFF);
        sendChannelClips(this.channel.getIndex(), this.channel);
      } else if (p.getParent() instanceof LXClip) {
        LXClip clip = (LXClip)p.getParent();
        sendClip(index, this.channel, clip.getIndex(), clip);
      }
      if (this.channel instanceof LXChannel) {
        LXChannel c = (LXChannel) this.channel;
        if (p == c.focusedPattern) {
          int focusedPatternIndex = c.getFocusedPatternIndex();
          int channelSurfaceIndex = c.controlSurfaceFocusIndex.getValuei();
          if (focusedPatternIndex < channelSurfaceIndex) {
            c.controlSurfaceFocusIndex.setValue(focusedPatternIndex);
          } else if (focusedPatternIndex >= channelSurfaceIndex + CLIP_LAUNCH_ROWS) {
            c.controlSurfaceFocusIndex.setValue(focusedPatternIndex - CLIP_LAUNCH_ROWS + 1);
          }
          sendChannelPatterns(index, c);
        }
      }
    }

    @Override
    public void effectAdded(LXBus channel, LXEffect effect) {
    }

    @Override
    public void effectRemoved(LXBus channel, LXEffect effect) {
    }

    @Override
    public void effectMoved(LXBus channel, LXEffect effect) {
      // TODO(mcslee): update device focus??  *JKB: Note retained from APC40mkII
    }

    @Override
    public void indexChanged(LXAbstractChannel channel) {
      // Handled by the engine channelMoved listener.
    }

    @Override
    public void groupChanged(LXChannel channel, LXGroup group) {

    }

    @Override
    public void patternAdded(LXChannel channel, LXPattern pattern) {
      if (isGridModePatterns()) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternRemoved(LXChannel channel, LXPattern pattern) {
      if (isGridModePatterns()) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternMoved(LXChannel channel, LXPattern pattern) {
      if (isGridModePatterns()) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternWillChange(LXChannel channel, LXPattern pattern, LXPattern nextPattern) {
      if (isGridModePatterns()) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void patternDidChange(LXChannel channel, LXPattern pattern) {
      if (isGridModePatterns()) {
        sendChannelPatterns(channel.getIndex(), channel);
      }
    }

    @Override
    public void clipAdded(LXBus bus, LXClip clip) {
      clip.running.addListener(this);
      sendClip(this.channel.getIndex(), this.channel, clip.getIndex(), clip);
    }

    @Override
    public void clipRemoved(LXBus bus, LXClip clip) {
      clip.running.removeListener(this);
      sendChannelClips(this.channel.getIndex(), this.channel);
    }

  }

  public final BooleanParameter masterFaderEnabled =
    new BooleanParameter("Master Fader", true)
    .setDescription("Whether the master fader is enabled");

  public final BooleanParameter channelFadersEnabled =
    new BooleanParameter("Channel Faders", true)
    .setDescription("Whether the channel faders are enabled");

  public final BooleanParameter isSuperMod =
    new BooleanParameter("SuperMod", true)
    .setDescription("Use the surface as a SuperMod controller");

  public APCminiMk2(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);
    addSetting("masterFaderEnabled", this.masterFaderEnabled);
    addSetting("channelFadersEnabled", this.channelFadersEnabled);
    addSetting("isSuperMod", this.isSuperMod);

    registerSM();
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (parameter == this.isSuperMod) {
      if (this.enabled.isOn()) {
        sendGrid();
      }
    }
  }

  private boolean isGridModePatterns() {
    return this.gridMode == GridMode.PATTERNS && !this.isSuperMod();
  }

  private boolean isGridModeClips() {
    return this.gridMode == GridMode.CLIPS && !this.isSuperMod();
  }

  private boolean isGridModeParameters() {
    return this.gridMode == GridMode.PARAMETERS && !this.isSuperMod();
  }

  private boolean isSuperMod() {
    return this.isSuperMod.getValueb() && this.isSMregistered;
  }

  @Override
  protected void onEnable(boolean on) {
    if (on) {
      initialize();
      register();
    } else {
      this.deviceListener.register(null);
      for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
        if (channel instanceof LXChannel) {
          ((LXChannel)channel).controlSurfaceFocusLength.setValue(0);
        }
      }
      if (this.isRegistered) {
        unregister();
      }
    }
  }

  @Override
  protected void onReconnect() {
    if (this.enabled.isOn()) {
      initialize();
    }
  }

  private void initialize() {
    sendGrid();
  }

  private void sendGrid() {
    sendNoteOn(MIDI_CHANNEL_SINGLE, TOGGLE_CLIPS, isGridModeClips() ? LED_ON : LED_OFF);
    sendNoteOn(MIDI_CHANNEL_SINGLE, TOGGLE_PARAMETERS, isGridModeParameters() ? LED_ON : LED_OFF);
    sendChannelButtonRow();
    if (isGridModeParameters()) {
      this.deviceListener.resend();
    } else if (isSuperMod()) {
      sendSMTemplates();
      this.deviceListener.resend();
      return;
    } else {
      for (int i = 0; i < NUM_CHANNELS; ++i) {
        LXAbstractChannel channel = getChannel(i);
        switch (this.gridMode) {
          case PATTERNS:
            sendChannelPatterns(i, channel);
            break;
          case CLIPS:
            sendChannelClips(i, channel);
            break;
          case PARAMETERS:
            break;
        }
      }
    }
  }

  private void sendSMTemplates() {
    int behavior, note, color;
    for (int index = 0; index < SUPERMOD_TEMPLATE_COLUMNS; ++index) {
      boolean global = SuperMod.current.isTemplateGlobal(index);
      for (int y = 0; y < SUPERMOD_TEMPLATE_ROWS; ++y) {
        behavior = LED_SUPERMOD_TEMPLATE_INACTIVE_BEHAVIOR;
        note = CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index;
        color = global ? LED_SUPERMOD_TEMPLATE_INACTIVE_COLOR_GLOBAL : LED_SUPERMOD_TEMPLATE_INACTIVE_COLOR_LOCAL;
        sendNoteOn(behavior, note, color);
      }
    }
  }

  private void clearGrid() {
    sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, TOGGLE_CLIPS, LED_OFF);
    sendNoteOn(MIDI_CHANNEL_MULTI_100_PERCENT, TOGGLE_PARAMETERS, LED_OFF);
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannelPatterns(i, null, true);
    }
  }

  private void sendChannelPatterns(int index, LXAbstractChannel channelBus) {
    sendChannelPatterns(index, channelBus, false);
  }

  private void sendChannelPatterns(int index, LXAbstractChannel channelBus, boolean force) {
    if (!force && isSuperMod()) {
      return;
    }
    if (index >= CLIP_LAUNCH_COLUMNS) {
      return;
    }
    if (channelBus instanceof LXChannel) {
      LXChannel channel = (LXChannel) channelBus;
      int baseIndex = channel.controlSurfaceFocusIndex.getValuei();
      int endIndex = channel.patterns.size() - baseIndex;
      int activeIndex = channel.getActivePatternIndex() - baseIndex;
      int nextIndex = channel.getNextPatternIndex() - baseIndex;
      int focusedIndex = channel.focusedPattern.getValuei() - baseIndex;
      if (channel.patterns.size() == 0) {
        focusedIndex = -1;
      }
      for (int y = 0; y < CLIP_LAUNCH_ROWS; ++y) {
        int behavior = MIDI_CHANNEL_MULTI_100_PERCENT;
        int note = CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index;
        int color = LED_OFF;
        if (y == activeIndex) {
          // This pattern is active (may also be focused)
          behavior = LED_PATTERN_ACTIVE_BEHAVIOR;
          color = LED_PATTERN_ACTIVE_COLOR;
        } else if (y == nextIndex) {
          // This pattern is being transitioned to
          behavior = LED_PATTERN_TRANSITION_BEHAVIOR;
          color = LED_PATTERN_TRANSITION_COLOR;
        } else if (y == focusedIndex) {
          // This pattern is not active, but it is focused
          behavior = LED_PATTERN_FOCUSED_BEHAVIOR;
          color = LED_PATTERN_FOCUSED_COLOR;
        } else if (y < endIndex) {
          // There is a pattern present
          behavior = LED_PATTERN_INACTIVE_BEHAVIOR;
          color = LED_PATTERN_INACTIVE_COLOR;
        }

        sendNoteOn(behavior, note, color);
      }
    } else {
      for (int y = 0; y < CLIP_LAUNCH_ROWS; ++y) {
        sendNoteOn(
          MIDI_CHANNEL_MULTI_100_PERCENT,
          CLIP_LAUNCH + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - y) + index,
          LED_OFF
        );
      }
    }
  }

  private void sendChannelClips(int index, LXAbstractChannel channel) {
    for (int i = 0; i < CLIP_LAUNCH_ROWS; ++i) {
      LXClip clip = null;
      if (channel != null) {
        clip = channel.getClip(i);
      }
      sendClip(index, channel, i, clip);
    }
  }

  private void sendClip(int channelIndex, LXAbstractChannel channel, int clipIndex, LXClip clip) {
    if (!isGridModeClips() || channelIndex >= CLIP_LAUNCH_COLUMNS || clipIndex >= CLIP_LAUNCH_ROWS) {
      return;
    }
    int behavior = MIDI_CHANNEL_MULTI_100_PERCENT;
    int color = LED_OFF;
    int pitch = CLIP_LAUNCH + channelIndex + CLIP_LAUNCH_COLUMNS * (CLIP_LAUNCH_ROWS - 1 - clipIndex);
    if (channel != null && clip != null) {
      if (channel.arm.isOn()) {
        if (clip.isRunning()) {
          behavior = LED_CLIP_RECORD_BEHAVIOR;
          color =  LED_CLIP_RECORD_COLOR;
        } else {
          behavior = LED_CLIP_ARM_BEHAVIOR;
          color =  LED_CLIP_ARM_COLOR;
        }
      } else {
        if (clip.isRunning()) {
          behavior = LED_CLIP_PLAY_BEHAVIOR;
          color =  LED_CLIP_PLAY_COLOR;
        } else {
          behavior = LED_CLIP_INACTIVE_BEHAVIOR;
          color =  LED_CLIP_INACTIVE_COLOR;
        }
      }
    }
    sendNoteOn(behavior, pitch, color);
  }

  private void sendChannelFocus() {
    if (this.channelButtonMode == ChannelButtonMode.FOCUS && !this.shiftOn) {
      sendChannelButtonRow();
    }
  }

  private void setChannelButtonMode(ChannelButtonMode mode) {
    this.channelButtonMode = mode;
    sendChannelButtonRow();
  }

  private void sendChannelButtonRow() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannelButton(i, getChannel(i));
    }
  }

  private void clearChannelButtonRow() {
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + i, LED_OFF);
    }
  }

  private void sendChannelButton(int index, LXAbstractChannel channel) {
    if (this.shiftOn) {
      // Shift
      int shiftCode = index + CHANNEL_BUTTON;
      int color = LED_OFF;
      switch (shiftCode) {
      case CHANNEL_BUTTON_FOCUS:
        color = this.channelButtonMode == ChannelButtonMode.FOCUS ? LED_ON : LED_OFF;
        break;
      case CHANNEL_BUTTON_ENABLED:
        color = this.channelButtonMode == ChannelButtonMode.ENABLED ? LED_ON : LED_OFF;
        break;
      case CHANNEL_BUTTON_CUE:
        color = this.channelButtonMode == ChannelButtonMode.CUE ? LED_ON : LED_OFF;
        break;
      case CHANNEL_BUTTON_ARM:
        color = this.channelButtonMode == ChannelButtonMode.ARM ? LED_ON : LED_OFF;
        break;
      }
      sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, color);
    } else {
      // Not shift
      if (channel != null) {
        switch (this.channelButtonMode) {
          case FOCUS:
            sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, index == this.lx.engine.mixer.focusedChannel.getValuei() ? LED_ON : LED_OFF);
            break;
          case ENABLED:
            sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, channel.enabled.isOn() ? LED_ON : LED_OFF);
            break;
          case CUE:
            sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, channel.cueActive.isOn() ? LED_ON : LED_OFF);
            break;
          case ARM:
            sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, channel.arm.isOn() ? LED_ON : LED_OFF);
            break;
          case CROSSFADEGROUP:
            // Button press toggles through the 3 modes. Button does not stay lit.
            sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, LED_OFF);
            break;
        }
      } else {
        sendNoteOn(MIDI_CHANNEL_SINGLE, CHANNEL_BUTTON + index, LED_OFF);
      }
    }
  }

  private final LXMixerEngine.Listener mixerEngineListener = new LXMixerEngine.Listener() {
    @Override
    public void channelRemoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      unregisterChannel(channel);
      if (!isGridModeParameters()) {
        sendGrid();
      }
    }

    @Override
    public void channelMoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      if (!isGridModeParameters()) {
        sendGrid();
      } else {
        sendChannelButtonRow();
      }
    }

    @Override
    public void channelAdded(LXMixerEngine mixer, LXAbstractChannel channel) {
      if (!isGridModeParameters()) {
        sendGrid();
      }
      registerChannel(channel);
    }
  };

  private final LXParameterListener focusedChannelListener = (p) -> {
    sendChannelFocus();
    this.deviceListener.registerChannel(this.lx.engine.mixer.getFocusedChannel());
  };

  private boolean isRegistered = false;

  private void register() {
    isRegistered = true;

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      registerChannel(channel);
    }

    this.lx.engine.mixer.addListener(this.mixerEngineListener);
    this.lx.engine.mixer.focusedChannel.addListener(this.focusedChannelListener);

    this.deviceListener.registerChannel(this.lx.engine.mixer.getFocusedChannel());
  }

  private void unregister() {
    isRegistered = false;

    for (LXAbstractChannel channel : this.lx.engine.mixer.channels) {
      unregisterChannel(channel);
    }

    this.lx.engine.mixer.removeListener(this.mixerEngineListener);
    this.lx.engine.mixer.focusedChannel.removeListener(this.focusedChannelListener);

    clearGrid();
    clearChannelButtonRow();
  }

  private void registerChannel(LXAbstractChannel channel) {
    ChannelListener channelListener = new ChannelListener(channel);
    this.channelListeners.put(channel, channelListener);
  }

  private void unregisterChannel(LXAbstractChannel channel) {
    ChannelListener channelListener = this.channelListeners.remove(channel);
    if (channelListener != null) {
      channelListener.dispose();
    }
  }

  private LXAbstractChannel getChannel(int index) {
    if (index < this.lx.engine.mixer.channels.size()) {
      return this.lx.engine.mixer.channels.get(index);
    }
    return null;
  }

  /*
   * SuperMod
   */

  private boolean isSMregistered = false;

  private final SuperMod.Listener superModListener = new SuperMod.Listener() {
    @Override
    public void templatesChanged() {
      if (isSuperMod()) {
        sendSMTemplates();
      }
    }

    /**
     * Handle Chromatik versions where plugin disposes prior to midi surface on shutdown.
     */
    @Override
    public void willDispose() {
      if (isSMregistered) {
        unregisterSM();
        // Unregister ModParameters before SuperMod plugin disposes.
        deviceListener.register(null);
      }
    }
  };

  private void registerSM() {
    this.isSMregistered = true;
    SuperMod.current.addListener(this.superModListener);
  }

  private void unregisterSM() {
    this.isSMregistered = false;
    SuperMod.current.removeListener(this.superModListener);
  }

  static private final int SM_INVALID = -1;
  static private final int SM_TEMPLATE_MIN = 32;
  static private final int SM_TEMPLATE_MAX = 64;

  private boolean onSMGridButton(int pitch, boolean on) {
    if (pitch == SHIFT) {
      SuperMod.current.setModState(this, on);
    } else if (SM_TEMPLATE_MIN <= pitch && pitch <= SM_TEMPLATE_MAX) {
      // Button is within template zone (top 4 rows, any column)
      SuperMod.current.setModState(this, on);
      final int templatePitch = pitch - SM_TEMPLATE_MIN;
      final int templateIndex = templatePitch % CLIP_LAUNCH_COLUMNS;
      final int row = templatePitch - templateIndex;
      final int templateVariation = (SUPERMOD_TEMPLATE_ROWS - (row / CLIP_LAUNCH_COLUMNS)) - 1;
      if (on) {
        SuperMod.current.setTemplate(this, templateIndex, templateVariation);
        // TODO: Light buttons only in response to SuperMod property change (current template/variation)
        sendNoteOn(LED_SUPERMOD_TEMPLATE_ACTIVE_BEHAVIOR, pitch, LED_SUPERMOD_TEMPLATE_ACTIVE_COLOR);
      } else {
        boolean global = SuperMod.current.isTemplateGlobal(templateIndex);
        sendNoteOn(LED_SUPERMOD_TEMPLATE_INACTIVE_BEHAVIOR, pitch,
          global ? LED_SUPERMOD_TEMPLATE_INACTIVE_COLOR_GLOBAL : LED_SUPERMOD_TEMPLATE_INACTIVE_COLOR_LOCAL);
      }
      return true;
    } else {
      // Button may be within indicator/mod zone
      if (on) {
        final int parameterButton = smGetParameterForButton(pitch);
        if (parameterButton != SM_INVALID) {
          this.deviceListener.onSMIndicatorButton(parameterButton);
          return true;
        }
      }
    }
    // Unhandled, must be channel row or scene launch
    return false;
  }

  private final int[] SM_PARAMETER_BUTTONS = { 24, 25, 26, 27,
                                               16, 17, 18, 19,
                                                8,  9, 10, 11,
                                                0,  1,  2,  3,
                                               28, 29, 30, 31,
                                               20, 21, 22, 23,
                                               12, 13, 14, 15,
                                                4,  5,  6,  7};

  private int smGetParameterForButton(int pitch) {
    for (int i = 0; i < SM_PARAMETER_BUTTONS.length; i++) {
      if (SM_PARAMETER_BUTTONS[i] == pitch) {
        return i;
      }
    }
    return SM_INVALID;
  }

  private void noteReceived(MidiNote note, boolean on) {
    int pitch = note.getPitch();

    if (isSuperMod()) {
      // Tidy edit, expecting grid behavior will evolve.
      if (onSMGridButton(pitch, on)) {
        return;
      }
    }

    // Global momentary
    if (pitch == SHIFT) {
      // Shift doesn't have an LED, odd.
      this.shiftOn = on;
      sendChannelButtonRow();
      return;
    }

    if (this.shiftOn) {
      // Shift

      // Light-up momentary buttons
      switch (pitch) {
        case CLIP_STOP:
        case SOLO:
        case REC_ARM:
        case MUTE:
        case SELECT:
        case STOP_ALL_CLIPS:
        case SELECT_UP:
        case SELECT_DOWN:
        case SELECT_LEFT:
        case SELECT_RIGHT:
          sendNoteOn(note.getChannel(), pitch, on ? LED_ON : LED_OFF);
          break;
      }

      // Button actions with Shift
      if (on) {
        LXBus bus;
        switch (pitch) {
          case SELECT_LEFT:
            if (isGridModeParameters()) {
              this.deviceListener.registerPrevious();
            } else {
              this.lx.engine.mixer.focusedChannel.decrement(false);
              lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
            }
            return;
          case SELECT_RIGHT:
            if (isGridModeParameters()) {
              this.deviceListener.registerNext();
            } else {
              this.lx.engine.mixer.focusedChannel.increment(false);
              lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
            }
            return;
          case SELECT_UP:
            bus = this.lx.engine.mixer.getFocusedChannel();
            if (bus instanceof LXChannel) {
              ((LXChannel) bus).focusedPattern.decrement(1 , false);
            }
            return;
          case SELECT_DOWN:
            bus = this.lx.engine.mixer.getFocusedChannel();
            if (bus instanceof LXChannel) {
              ((LXChannel) bus).focusedPattern.increment(1 , false);
            }
            return;
          case CHANNEL_BUTTON_FOCUS:
            setChannelButtonMode(ChannelButtonMode.FOCUS);
            return;
          case CHANNEL_BUTTON_ENABLED:
            setChannelButtonMode(ChannelButtonMode.ENABLED);
            return;
          case CHANNEL_BUTTON_CUE:
            setChannelButtonMode(ChannelButtonMode.CUE);
            return;
          case CHANNEL_BUTTON_ARM:
            setChannelButtonMode(ChannelButtonMode.ARM);
            return;
          case CHANNEL_BUTTON_CROSSFADEGROUP:
            // Not an available mode currently due to 4 button limitation
            return;
          case CLIP_STOP:
          case SOLO:
          case MUTE:
          case REC_ARM:
          case SELECT:
          case DRUM:
          case NOTE:
            // Not implemented
            return;
          case STOP_ALL_CLIPS:
            this.lx.engine.clips.stopClips();
            return;
        }
      }
    } else {
      // Not Shift

      // Light-up momentary buttons
      if (pitch >= SCENE_LAUNCH && pitch <= SCENE_LAUNCH_MAX) {
        sendNoteOn(note.getChannel(), pitch, on ? LED_ON : LED_OFF);
      }

      // Button actions without Shift
      if (on) {
        switch (pitch) {
          case TOGGLE_CLIPS:
            this.gridMode = isGridModeClips() ? GridMode.PATTERNS : GridMode.CLIPS;
            sendGrid();
            return;
          case TOGGLE_PARAMETERS:
            this.gridMode = isGridModeParameters() ? GridMode.PATTERNS : GridMode.PARAMETERS;
            sendGrid();
            return;
        }

        if (pitch >= SCENE_LAUNCH && pitch <= SCENE_LAUNCH_MAX) {
          this.lx.engine.clips.launchScene(pitch - SCENE_LAUNCH);
          return;
        }

        // Grid button
        if (pitch >= CLIP_LAUNCH && pitch <= CLIP_LAUNCH_MAX) {
          int channelIndex = (pitch - CLIP_LAUNCH) % CLIP_LAUNCH_COLUMNS;
          int index = CLIP_LAUNCH_ROWS - 1 - ((pitch - CLIP_LAUNCH) / CLIP_LAUNCH_COLUMNS);
          if (isGridModeParameters()) {
            // Grid button: Parameter
            this.deviceListener.onParameterButton(channelIndex, index);
            return;
          } else {
            LXAbstractChannel channel = getChannel(channelIndex);
            if (channel != null) {
              if (this.gridMode == GridMode.CLIPS) {
                // Grid button: Clip
                LXClip clip = channel.getClip(index);
                if (clip == null) {
                  clip = channel.addClip(index);
                } else {
                  if (clip.isRunning()) {
                    clip.stop();
                  } else {
                    clip.trigger();
                    this.lx.engine.clips.setFocusedClip(clip);
                  }
                }
              } else {
                // Grid button: Pattern
                if (channel instanceof LXChannel) {
                  LXChannel c = (LXChannel) channel;
                  index += c.controlSurfaceFocusIndex.getValuei();
                  if (index < c.getPatterns().size()) {
                    c.focusedPattern.setValue(index);
                    if (!this.shiftOn) {
                      c.goPatternIndex(index);
                    }
                  }
                }
              }
            }
          }
          return;
        }

        if (pitch >= CHANNEL_BUTTON && pitch <= CHANNEL_BUTTON_MAX) {
          LXAbstractChannel channel = getChannel(pitch - CHANNEL_BUTTON);
          if (channel != null) {
            switch (this.channelButtonMode) {
            case FOCUS:
              this.lx.engine.mixer.focusedChannel.setValue(channel.getIndex());
              lx.engine.mixer.selectChannel(lx.engine.mixer.getFocusedChannel());
              break;
            case ENABLED:
              channel.enabled.toggle();
              break;
            case CUE:
              channel.cueActive.toggle();
              break;
            case ARM:
              channel.arm.toggle();
              break;
            case CROSSFADEGROUP:
              channel.crossfadeGroup.increment();
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public void noteOnReceived(MidiNoteOn note) {
    noteReceived(note, true);
  }

  @Override
  public void noteOffReceived(MidiNote note) {
    noteReceived(note, false);
  }

  @Override
  public void controlChangeReceived(MidiControlChange cc) {
    int number = cc.getCC();

    switch (number) {
    case MASTER_FADER:
      if (this.masterFaderEnabled.isOn()) {
        this.lx.engine.mixer.masterBus.fader.setNormalized(cc.getNormalized());
      }
      return;
    }

    if (number >= CHANNEL_FADER && number <= CHANNEL_FADER_MAX) {
      if (this.channelFadersEnabled.isOn()) {
        int channel = number - CHANNEL_FADER;
        if (channel < this.lx.engine.mixer.channels.size()) {
          this.lx.engine.mixer.channels.get(channel).fader.setNormalized(cc.getNormalized());
        }
      }
      return;
    }

    LXMidiEngine.error("APC MINI unmapped control change: " + cc);
  }

  @Override
  public void dispose() {
    if (this.isRegistered) {
      unregister();
    }
    this.deviceListener.dispose();
    if (this.isSMregistered) {
      unregisterSM();
    }
    super.dispose();
  }

}
