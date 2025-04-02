/**
 * Copyright 2024- Justin Belcher
 *
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package studio.jkb.supermod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.LXPlugin;
import heronarts.lx.Tempo;
import heronarts.lx.modulation.LXCompoundModulation;
import heronarts.lx.modulation.LXCompoundModulation.Target;
import heronarts.lx.modulation.LXModulationEngine;
import heronarts.lx.modulation.LXParameterModulation.ModulationException;
import heronarts.lx.modulation.LXTriggerModulation;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.VariableLFO;
import heronarts.lx.modulator.LXVariablePeriodModulator.ClockMode;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.utils.LXUtils;

/**
 * Super Modulator Engine
 *
 * Allows creation of modulators on the fly from midi surfaces.
 */
@LXPlugin.Name("SuperMod")
public class SuperMod extends LXComponent implements LXStudio.Plugin {

  static public final String VERSION = "0.1.5-SNAPSHOT";
  static public final String SUPERMOD_PREFIX = "SM_";

  static public SuperMod current;

  static public enum ModulationState {
    EMPTY,
    SUPERMOD,
    OTHER
  }

  public interface Listener {
    /**
     * Indicates when MIDI surfaces controlling device parameters
     * should enter a modulation control state.
     */
    public default void stateChanged(boolean isMod) {};
    /**
     * Templates changed, which could indicate a global target
     * modulator was added or removed.
     */
    public default void templatesChanged() {};
    /**
     * Plugin dispose warning, necessary for Chromatik versions
     * where plugins dispose prior to midi surfaces.
     */
    public default void willDispose() {};
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  public final EnumParameter<Tempo.Division> tempo1 =
    new EnumParameter<Tempo.Division>("Tempo 1", Tempo.Division.SIXTEENTH);

  public final EnumParameter<Tempo.Division> tempo2 =
    new EnumParameter<Tempo.Division>("Tempo 2", Tempo.Division.QUARTER);

  public final EnumParameter<Tempo.Division> tempo3 =
    new EnumParameter<Tempo.Division>("Tempo 3", Tempo.Division.HALF);

  public final EnumParameter<Tempo.Division> tempo4 =
    new EnumParameter<Tempo.Division>("Tempo 4", Tempo.Division.WHOLE);

  public final EnumParameter<Tempo.Division> tempo5 =
    new EnumParameter<Tempo.Division>("Tempo 5", Tempo.Division.DOUBLE);

  public final EnumParameter<Tempo.Division> tempo6 =
    new EnumParameter<Tempo.Division>("Tempo 6", Tempo.Division.FOUR);

  public final EnumParameter<Tempo.Division> tempo7 =
    new EnumParameter<Tempo.Division>("Tempo 7", Tempo.Division.EIGHT);

  public final EnumParameter<Tempo.Division> tempo8 =
    new EnumParameter<Tempo.Division>("Tempo 8", Tempo.Division.SIXTEEN);

  final ArrayList<EnumParameter<Tempo.Division>> tempos = new ArrayList<>(Arrays.asList(tempo1, tempo2, tempo3, tempo4, tempo5, tempo6, tempo7, tempo8)); 

  /**
   * All system global modulators.  We are listening to their labels.
   */
  private final List<LXModulator> globalModulators = new ArrayList<LXModulator>();

  /**
   * Global modulators that are SuperMod targets
   */
  private final LXModulator[] activeGlobalModulators = new LXModulator[8];

  private final LXParameterListener modulatorLabelListener = new LXParameterListener() {
    @Override
    public void onParameterChanged(LXParameter parameter) {
      checkModulatorLabel((LXModulator)parameter.getParent());
    }
  };

  private final LXModulationEngine.Listener globalModulationListener = new LXModulationEngine.Listener() {
    @Override
    public void modulatorAdded(LXModulationEngine engine, LXModulator modulator) {
      listenGlobalModulator(modulator);
    }

    @Override
    public void modulatorRemoved(LXModulationEngine engine, LXModulator modulator) {
      unlistenGlobalModulator(modulator);
    }

    @Override
    public void modulatorMoved(LXModulationEngine engine, LXModulator modulator) { }
    @Override
    public void modulationAdded(LXModulationEngine engine, LXCompoundModulation modulation) { }
    @Override
    public void modulationRemoved(LXModulationEngine engine, LXCompoundModulation modulation) { }
    @Override
    public void triggerAdded(LXModulationEngine engine, LXTriggerModulation modulation) { }
    @Override
    public void triggerRemoved(LXModulationEngine engine, LXTriggerModulation modulation) { }
  };

  private void listenGlobalModulator(LXModulator modulator) {
    globalModulators.add(modulator);
    modulator.label.addListener(modulatorLabelListener);
    checkModulatorLabel(modulator);
  }

  private void unlistenGlobalModulator(LXModulator modulator) {
    modulator.label.removeListener(modulatorLabelListener);
    globalModulators.remove(modulator);
    removeGlobalTargetModulator(modulator);
  }

  private void unlistenGlobalModulators() {
    for (int i = this.globalModulators.size() - 1; i >= 0; i--) {
      unlistenGlobalModulator(this.globalModulators.get(i));
    }
    for (int i = 0; i < this.activeGlobalModulators.length; i++) {
      this.activeGlobalModulators[i] = null;
    }
  }

  private void checkModulatorLabel(LXModulator modulator) {
    String l = modulator.getLabel(); 
    if (l.startsWith(SUPERMOD_PREFIX)) {
      l = l.substring(SUPERMOD_PREFIX.length());
      try {
        int position = Integer.parseInt(l);
        if (position >= 1 && position <= 8) {
          this.activeGlobalModulators[position - 1] = modulator;
          debug("Found global modulator " + position);

          // Remove from any previous position
          for (int i = 0; i < this.activeGlobalModulators.length; i++) {
            if (i != position - 1 && this.activeGlobalModulators[i] == modulator) {
              this.activeGlobalModulators[i] = null;
              debug("Removed global modulator " + (i + 1));
            }
          }
          notifyTemplatesChanged();
          return;
        }
      } catch (NumberFormatException ex) {
        // Ignore invalid names
      }
    }
    // Remove it from global, in case label was valid and now is not
    removeGlobalTargetModulator(modulator);
  }

  private void removeGlobalTargetModulator(LXModulator modulator) {
    for (int i = 0; i < this.activeGlobalModulators.length; i++) {
      if (this.activeGlobalModulators[i] == modulator) {
        this.activeGlobalModulators[i] = null;
        debug("Removed global modulator " + (i + 1));
        notifyTemplatesChanged();
        return;
      }
    }    
  }

  public SuperMod(LX lx) {
    super(lx);
    LOG.log("SuperMod Plugin version " + VERSION);
    current = this;

    addParameter("tempo1", this.tempo1);
    addParameter("tempo2", this.tempo2);
    addParameter("tempo3", this.tempo3);
    addParameter("tempo4", this.tempo4);
    addParameter("tempo5", this.tempo5);
    addParameter("tempo6", this.tempo6);
    addParameter("tempo7", this.tempo7);
    addParameter("tempo8", this.tempo8);
  }

  /*
   * New for TE variation: Downstream can be a modulator source
   */

  public interface ModulatorSource {
    /**
     * Implement this method to create a modulator for a given col/row on the top half
     * of the APC mini. Return null to get the default modulator.
     */
    LXModulator createModulator(String label, int col, int row);
  }

  private final List<ModulatorSource> modulatorSources = new ArrayList<ModulatorSource>();

  public SuperMod addModulatorSource(ModulatorSource listener) {
    Objects.requireNonNull(listener, "May not add null SuperMod.ModulatorSource: " + this);
    if (this.modulatorSources.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate SuperMod.ModulatorSource " + listener.getClass().getName());
    }
    this.modulatorSources.add(listener);
    return this;
  }

  public SuperMod removeModulatorSource(ModulatorSource listener) {
    if (!this.modulatorSources.contains(listener)) {
      LX.error(new Exception(), "Trying to remove unregistered SuperMod.ModulatorSource " + listener.getClass().getName());
    }
    this.modulatorSources.remove(listener);
    return this;
  }

  private LXModulator createModulator(String label, int row, int col) {
    LXModulator modulator;
    for (ModulatorSource listener : this.modulatorSources) {
      modulator = listener.createModulator(label, row, col);
      if (modulator != null) {
        return modulator;
      }
    }

    VariableLFO lfo = new VariableLFO(label);
    lfo.clockMode.setValue(ClockMode.SYNC);
    lfo.tempoDivision.setValue(tempos.get(templateIndex).getEnum());
    LXWaveshape shape;
    switch (templateVariation) {
      case 3:
        shape = LXWaveshape.UP;
        break;
      case 2:
        shape = LXWaveshape.SQUARE;
        break;
      case 1:
        shape = LXWaveshape.TRI;
        break;
      case 0:
      default:
        shape = LXWaveshape.SIN;
    }
    lfo.waveshape.setValue(shape);
    return lfo;
  }

  /*
   * LX Plugin
   */

  @Override
  public void initialize(LX lx) {
    // Special midi surface versions
    registerAPCmini2(SUPERMOD_PREFIX + APCminiMk2.DEVICE_NAME);
    registerMidiFighterTwister(SUPERMOD_PREFIX + MidiFighterTwister.DEVICE_NAME);

    lx.engine.modulation.addListener(this.globalModulationListener);
  }

  /**
   * A project may call this method to specify a system device name
   * for the SuperMod version of the APCminiMk2 MIDI surface.
   *
   * @param apcMiniMk2 System MIDI surface name for APCminiMk2
   */
  public void registerAPCmini2(String apcMiniMk2) {
    this.lx.engine.midi.registerSurface(APCminiMk2.class);
  }

  /**
   * A project may call this method to specify a system device name
   * for the SuperMod version of the MidiFighterTwister MIDI surface.
   *
   * @param midiFighterTwister System MIDI surface name for MidiFighterTwister
   */
  public void registerMidiFighterTwister(String midiFighterTwister) {
    this.lx.engine.midi.registerSurface(MidiFighterTwister.class);
  }

  @Override
  public void initializeUI(LXStudio lx, UI ui) { }

  @Override
  public void onUIReady(LXStudio lx, UI ui) {
    new UISuperMod(ui, this, ui.leftPane.global.getContentWidth())
    .addToContainer(ui.leftPane.global, 0);
  }

  /*
   * Device wrapper
   */

  /**
   * Wraps a LXDeviceComponent and maintains a ModParameter
   * for each device remote control.
   */
  public class Device {

    /**
     * A special parameter that acts as a proxy to the amount
     * of modulation applied by a modulator to a device parameter.
     *
     * This parameter is directly controllable by a user from a [MidiFighterTwister] midi surface,
     * adding tricks like creating the modulator+modulation if it does not exist
     * and deleting the modulation on parameter reset. 
     */
    public class ModParameter extends BoundedParameter implements LXCompoundModulation.Listener {

      public static final double MOD_LEVEL_EXPONENT = 1;

      public final Target target;
      private LXCompoundModulation modulation;

      public ModParameter(Target target) {
        super(target.getLabel());
        this.setPolarity(Polarity.BIPOLAR);
        this.setExponent(MOD_LEVEL_EXPONENT);

        this.target = target;
        this.target.addModulationListener(this);

        // Link to existing modulations
        List<LXCompoundModulation> modulations = this.target.getModulations();
        if (modulations.size() > 0) {
          setModulation(modulations.get(0));
        }
        this.target.getModulations();
      }


      @Override
      public void modulationAdded(Target parameter, LXCompoundModulation modulation) {
        setModulation(modulation);
      }

      @Override
      public void modulationRemoved(Target parameter, LXCompoundModulation modulation) {
        removeModulation(modulation);
      }

      private void setModulation(LXCompoundModulation modulation) {
        // TODO: Handle multiple modulations
        if (this.modulation != null) {
          return;
        }

        this.modulation = modulation;
        registerModulation(modulation);
        bang();
      }

      private void removeModulation(LXCompoundModulation modulation) {
        // TODO: Handle multiple modulations
        if (modulation != this.modulation) {
          return;
        }

        unregisterModulation(modulation);
        this.modulation = null;
        bang();
      }

      private LXParameterListener rangeListener = (p) -> {
        bang();
      };

      private void registerModulation(LXCompoundModulation modulation) {
        modulation.range.addListener(this.rangeListener);
      }

      private void unregisterModulation(LXCompoundModulation modulation) {
        modulation.range.removeListener(this.rangeListener);
      }

      public ModulationState getState() {
        if (this.modulation != null) {
          if (this.modulation.source instanceof LXModulator && isSMmodulator((LXModulator)this.modulation.source)) {
            return ModulationState.SUPERMOD;
          } else {
            return ModulationState.OTHER;
          }
        } else {
          return ModulationState.EMPTY;
        }
      }

      @Override
      public double getValue() {
        if (this.modulation != null) {
          return this.modulation.range.getValue();
        }
        return 0.5;
      }

      @Override
      public BoundedParameter setNormalized(double value) {
        if (this.modulation == null) {
          createModulation(this);
        }
        if (this.modulation != null) {
          this.modulation.range.setNormalized(value);
        }
        return this;
      }

      @Override
      public double getNormalized() {
        if (this.modulation != null) {
          return this.modulation.range.getNormalized();
        }
        return 0.5;
      }

      @Override
      public LXListenableNormalizedParameter incrementNormalized(double amount) {
        if (this.modulation == null) {
          createModulation(this);
        }
        if (this.modulation != null) {
          this.modulation.range.incrementNormalized(amount);
        }
        return this;
      }

      @Override
      public LXListenableNormalizedParameter incrementNormalized(double amount, boolean wrap) {
        if (this.modulation == null) {
          // Creating a modulation is enough; the listener notification will link it.
          createModulation(this);
        }
        if (this.modulation != null) {
          this.modulation.range.incrementNormalized(amount, wrap);
        }
        return this;
      }

      @Override
      public boolean isMappable() {
        return false;
      }

      @Override
      public boolean isWrappable() {
        return false;
      }

      @Override
      public BoundedParameter reset() {
        clearModulation();
        return this;
      }

      public BoundedParameter clearModulation() {
        if (this.modulation != null) {
          clearModulation(this.modulation);
        }
        return this;
      }

      private void clearModulation(LXCompoundModulation modulation) {
        LXModulationEngine modulationEngine = modulation.scope;
        LXModulator modulator = (LXModulator)modulation.source;
        if (isSMmodulator(modulator)) {
          if (modulationEngine == device.modulation) {
            modulationEngine.removeModulator(modulator);
          } else {
            modulationEngine.removeModulation(modulation);                      
          }
        } else {
          // Non-SuperMod modulations will be reset but not deleted
          this.modulation.range.reset();
        }
      }

      @Override
      public void dispose() {
        if (this.modulation != null) {
          unregisterModulation(this.modulation);
          this.modulation = null;        
        }
        this.target.removeModulationListener(this);
        super.dispose();
      }

      // END ModParameter
    }

    private final LXDeviceComponent device;

    private LXListenableNormalizedParameter[] remoteControls = new LXListenableNormalizedParameter[0];
    private ModParameter[] remoteMods = new ModParameter[0];

    public Device(LXDeviceComponent device) {
      this.device = device;
      // Don't register for device.remoteControlsChanged,
      // that would only work if we were first in line.
    }

    /**
     * Retrieve the array of parameters than can be used to control modulations
     * on the target device.
     */
    public ModParameter[] getRemoteMods() {
      LXListenableNormalizedParameter[] newRemoteControls = device.getRemoteControls();
      final int length = newRemoteControls.length;

      if (length != this.remoteControls.length) {
        // Our remote control collection is definitely stale
        disposeRemoteMods();
        this.remoteMods = new ModParameter[length];
        for (int i = 0; i < length; i++) {
          this.remoteMods[i] = createModParameter(newRemoteControls[i]);
        }
        this.remoteControls = newRemoteControls;
      } else {
        // Length of remote controls didn't change. Check each one to
        // make sure it is the same. You might think we could avoid this
        // by listening to device.remoteControlsChanged, but this method
        // could get called by a midi surface listener prior to our
        // listener firing.
        for (int i = 0; i < length; i++) {
          if (this.remoteControls[i] != newRemoteControls[i]) {
            this.remoteControls[i] = newRemoteControls[i];
            if (this.remoteMods[i] != null) {
              this.remoteMods[i].dispose();
            }
            this.remoteMods[i] = createModParameter(newRemoteControls[i]);
          }
        }
      }

      return this.remoteMods;
    }

    private ModParameter createModParameter(LXListenableNormalizedParameter param) {
      if (param instanceof LXCompoundModulation.Target) {
        return new ModParameter((Target)param);
      }
      return null;
    }

    private LXCompoundModulation createModulation(ModParameter mod) {
      LXModulator modulator = activeGlobalModulators[templateIndex];
      LXModulationEngine modulationEngine;
      if (modulator != null) {
        // Found user-created global modulator
        modulationEngine = lx.engine.modulation;
      } else {
        // Create a device-level modulator based on template settings
        modulationEngine = device.modulation;
        final String label = SUPERMOD_PREFIX + mod.target.getLabel();
        modulator = createModulator(label, templateIndex, templateVariation);
        modulator.running.setValue(true);
        modulationEngine.addModulator(modulator);
      }

      try {
        // Add Modulation (links source -> target)
        LXCompoundModulation modulation = new LXCompoundModulation(modulationEngine, (LXNormalizedParameter) modulator, mod.target);
        modulationEngine.addModulation(modulation);
        return modulation;
      } catch (ModulationException e) {
        e.printStackTrace();
        LX.error(e);
        return null;
      }
    }

    private boolean isSMmodulator(LXModulator modulator) {
      return modulator.getLabel().startsWith(SUPERMOD_PREFIX);
    }

    private void disposeRemoteMods() {
      for (ModParameter remoteMod : this.remoteMods) {
        if (remoteMod != null) {
          remoteMod.dispose();
        }
      }
    }

    public void dispose() {
      disposeRemoteMods();
      this.remoteMods = null;
      this.remoteControls = null;
    }
  }

  private final Map<LXDeviceComponent, Device> devices = new HashMap<LXDeviceComponent, Device>();

  /**
   * Retrieve a device wrapper or create a new one if it does not exist
   */
  private Device getDevice(LXDeviceComponent device) {
    Device wrapper = this.devices.get(device);
    if (wrapper == null) {
      wrapper = new Device(device);
      this.devices.put(device, wrapper);
    }
    return wrapper;
  }

  /**
   * MIDI surfaces will call this method to retrieve a list of
   * remote mods (ModParameters) for a device. There will be one
   * ModParameter per [eligible] device remote control.
   */
  public Device.ModParameter[] getRemoteMods(LXDeviceComponent device) {
    Device deviceWrapper = getDevice(device);
    return deviceWrapper.getRemoteMods();
  }

  /*
   * SuperMod State
   */

  private boolean isMod;

  /**
   * When true, MFT surfaces should show modulator parameters
   */
  public boolean isMod() {
    return this.isMod;
  }

  /**
   * Call to specify whether [MFT] MIDI surfaces should enter
   * modulation-adjustment mode.
   */
  public void setModState(APCminiMk2 apCminiMk2, boolean on) {
    // TODO: Track states for multiple controllers and Primary/Aux
    setIsMod(on);
  }

  private void setIsMod(boolean isMod) {
    if (this.isMod != isMod) {
      this.isMod = isMod;
      for (Listener listener : listeners) {
        listener.stateChanged(isMod);
      }
    }
  }

  private int templateIndex = 0;
  private int templateVariation = 0;

  /**
   * Called by [APCMini] midi surface to set target template and variation(waveform).
   * @param apCminiMk2
   * @param templateIndex
   * @param templateVariation
   */
  public void setTemplate(APCminiMk2 apCminiMk2, int templateIndex, int templateVariation) {
    templateIndex = LXUtils.constrain(templateIndex, 0, this.tempos.size() - 1);
    this.templateIndex = templateIndex;
    this.templateVariation = templateVariation;
  }

  public boolean isTemplateGlobal(int index) {
    if (index < 0 || index >= this.activeGlobalModulators.length) {
      return false;
    }
    return this.activeGlobalModulators[index] != null;
  }

  /*
   * Listeners
   */

  public SuperMod addListener(Listener listener) {
    return addListener(listener, false);
  }

  public SuperMod addListener(Listener listener, boolean fireImmediately) {
    Objects.requireNonNull(listener, "May not add null SuperMod.Listener: " + this);
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate SuperMod.Listener " + listener.getClass().getName());
    }
    this.listeners.add(listener);
    if (fireImmediately) {
      listener.stateChanged(this.isMod);
      listener.templatesChanged();
    }
    return this;
  }

  public final SuperMod removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      LOG.error(new Exception(), "Trying to remove unregistered SuperMod.Listener " + listener.getClass().getName());
    }
    this.listeners.remove(listener);
    return this;
  }

  private void notifyTemplatesChanged() {
    for (Listener listener : this.listeners) {
      listener.templatesChanged();
    }
  }

  private static void debug(String message) {
    // LOG.log(message);
  }

  /*
   * Plugin dispose
   */

  @Override
  public void dispose() {
    LOG.log("SuperMod.dispose()");
    for (int i = this.listeners.size() - 1; i >= 0; i--) {
      this.listeners.get(i).willDispose();
    }
    for (Entry<LXDeviceComponent, Device> entry : this.devices.entrySet()) {
      entry.getValue().dispose();
    }
    this.devices.clear();
    unlistenGlobalModulators();
    this.lx.engine.modulation.removeListener(this.globalModulationListener);
    super.dispose();
  }

}
