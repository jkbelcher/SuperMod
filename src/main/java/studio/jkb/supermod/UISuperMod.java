/**
 * Copyright 2024- Justin Belcher
 *
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package studio.jkb.supermod;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIDoubleBox;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.Tempo;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.ui.device.UIControls;

public class UISuperMod  extends UICollapsibleSection implements UIControls {

  private static final float VERTICAL_SPACING = 4;
  private static final float CHILD_SPACING = 8;
  private static final float ROW_HEIGHT = 16;
  private static final float LABEL_WIDTH = 90;

  final float controlWidth;
  final VGraphics.Font labelFont;

  public UISuperMod(UI ui, SuperMod devSwitch, float w) {
    super(ui, 0, 0, w, 0);
    this.setTitle("SUPER MOD");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);

    this.controlWidth = getContentWidth() - (PADDING * 2) - LABEL_WIDTH;
    this.labelFont = ui.theme.getControlFont();

    for (EnumParameter<Tempo.Division> p : devSwitch.tempos) {
      addRow(this, ROW_HEIGHT, p.getLabel(), newControl(p));
    }
  }

  public UI2dComponent newControl(LXParameter p) {
    UI2dComponent control = null;
    if (p instanceof BooleanParameter) {
      control = new UIButton(0, 0, (BooleanParameter) p).setActiveLabel("On").setInactiveLabel("Off");
    } else if (p instanceof BoundedParameter) {
      control = new UIDoubleBox(0, 0, (BoundedParameter) p);
    } else if (p instanceof DiscreteParameter) {
      control = new UIDropMenu(0, 0, (DiscreteParameter) p);
    }
    return control;
  }

  // TODO: Add these row methods to UIControls

  public UI2dContainer addRow(UI2dContainer uiDevice, UI2dComponent... components) {
    return addRow(uiDevice, ROW_HEIGHT, components);
  }

  public UI2dContainer addRow(UI2dContainer uiDevice, float rowHeight, UI2dComponent... components) {
    return addRow(uiDevice, rowHeight, null, components);
  }

  public UI2dContainer addRow(UI2dContainer uiDevice, String label) {
    return addRow(uiDevice, ROW_HEIGHT, label, (UI2dComponent[]) null);
  }

  public UI2dContainer addRow(UI2dContainer uiDevice, String label, UI2dComponent... components) {
    return addRow(uiDevice, ROW_HEIGHT, label, components);
  }

  public UI2dContainer addRow(UI2dContainer uiDevice, float rowHeight, String label, UI2dComponent... components) {
    UI2dContainer row = UI2dContainer.newHorizontalContainer(rowHeight, CHILD_SPACING);

    if (label != null) {
      rowLabel(label, LABEL_WIDTH).addToContainer(row);
    }

    if (components != null) {
      float cWidth = (label == null ? getContentWidth() - (PADDING * 2) : this.controlWidth - ((components.length - 1) * CHILD_SPACING)) / components.length;
      for (UI2dComponent component : components) {
        component.setWidth(cWidth).setHeight(ROW_HEIGHT).addToContainer(row);
      }
    }

    row.addToContainer(uiDevice);
    return row;
  }

  public UI2dComponent rowLabel(String label) {
    return sectionLabel(label, LABEL_WIDTH);
  }

  public UI2dComponent rowLabel(String label, float columnWidth) {
    return new UILabel(columnWidth, 16, label)
      .setFont(this.labelFont)
      .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE);
  }

  public UI2dComponent addHorizontalBreak(UI ui, UI2dContainer uiDevice) {
    return new UI2dComponent(0, 0, uiDevice.getContentWidth() - (PADDING * 2), 1) {}
      .setBorderColor(ui.theme.controlBorderColor)
      .addToContainer(uiDevice);
  }
}
