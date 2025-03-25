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

package studio.jkb.supermod;

import heronarts.lx.LX;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiInput;
import heronarts.lx.midi.LXMidiOutput;
import heronarts.lx.midi.MidiNote;
import heronarts.lx.midi.MidiNoteOn;
import heronarts.lx.midi.surface.LXMidiSurface;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;
import studio.jkb.supermod.SuperMod.Device.ModParameter;

@LXMidiSurface.Name("Akai APC Mini mk2 SuperMod")
@LXMidiSurface.DeviceName("APC mini mk2 Control")
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

  private boolean shiftOn = false;

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
      } else if (isSuperMod()) {
        for (int i = 0; i < this.mods.length; ++i) {
          if (parameter == this.mods[i]) {
            sendSMIndicator((ModParameter)parameter, i);
          }
        }
      }
    }

    void resend() {
      if (isSuperMod()) {
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
            if (clearButtons) {
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

  public final BooleanParameter isSuperMod =
    new BooleanParameter("SuperMod", true)
    .setDescription("Use the surface as a SuperMod controller");

  public APCminiMk2(LX lx, LXMidiInput input, LXMidiOutput output) {
    super(lx, input, output);

    // Currently just a placeholder, we will always be supermod.
    addSetting("isSuperMod", this.isSuperMod);

    registerSM();
  }

  private boolean isSuperMod() {
    return this.isSMregistered;
  }

  @Override
  protected void onEnable(boolean on) {
    if (on) {
      initialize();
      register();
    } else {
      this.deviceListener.register(null);
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
    sendChannelButtonRow();
    if (isSuperMod()) {
      sendSMTemplates();
      this.deviceListener.resend();
      return;
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
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannelPatterns(i, null, true);
    }
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
      int baseIndex = 0;
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
    /* Removed channel button row, TBD if this is permanent.
    for (int i = 0; i < NUM_CHANNELS; ++i) {
      sendChannelButton(i, getChannel(i));
    }*/
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

  private boolean isRegistered = false;

  private void register() {
    isRegistered = true;

    this.deviceListener.registerChannel(this.lx.engine.mixer.getFocusedChannel());
  }

  private void unregister() {
    isRegistered = false;

    clearGrid();
    clearChannelButtonRow();
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
          case SELECT_RIGHT:
          case SELECT_UP:
          case SELECT_DOWN:
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
        if (pitch >= SCENE_LAUNCH && pitch <= SCENE_LAUNCH_MAX) {
          this.lx.engine.clips.triggerScene(pitch - SCENE_LAUNCH);
          return;
        }

        // Grid button
        if (pitch >= CLIP_LAUNCH && pitch <= CLIP_LAUNCH_MAX) {
          int channelIndex = (pitch - CLIP_LAUNCH) % CLIP_LAUNCH_COLUMNS;
          int index = CLIP_LAUNCH_ROWS - 1 - ((pitch - CLIP_LAUNCH) / CLIP_LAUNCH_COLUMNS);
          LXAbstractChannel channel = getChannel(channelIndex);
          if (channel != null) {
            // Grid button: Pattern
            if (channel instanceof LXChannel) {
              LXChannel c = (LXChannel) channel;
              //index += c.controlSurfaceFocusIndex.getValuei();
              if (index < c.getPatterns().size()) {
                c.focusedPattern.setValue(index);
                if (!this.shiftOn) {
                  c.goPatternIndex(index);
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
