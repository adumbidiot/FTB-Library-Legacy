package com.feed_the_beast.ftbl.lib.gui.misc;

import com.feed_the_beast.ftbl.api.config.IConfigValue;
import com.feed_the_beast.ftbl.api.gui.IDrawableObject;
import com.feed_the_beast.ftbl.api.gui.IGui;
import com.feed_the_beast.ftbl.api.gui.IMouseButton;
import com.feed_the_beast.ftbl.lib.MouseButton;
import com.feed_the_beast.ftbl.lib.gui.ButtonLM;
import com.feed_the_beast.ftbl.lib.gui.GuiHelper;
import com.feed_the_beast.ftbl.lib.gui.GuiLM;
import com.feed_the_beast.ftbl.lib.gui.GuiLang;
import com.feed_the_beast.ftbl.lib.gui.TextBoxLM;

public abstract class GuiAbstractField extends GuiLM
{
    private final IConfigValue defValue, value;
    private final IGuiFieldCallback callback;

    private final ButtonLM buttonCancel, buttonAccept;
    private final TextBoxLM textBox;

    GuiAbstractField(IConfigValue val, IGuiFieldCallback c)
    {
        super(100, 40);
        defValue = val.copy();
        value = val.copy();
        callback = c;

        int bsize = getWidth() / 2 - 4;

        buttonCancel = new ButtonLM(2, getHeight() - 18, bsize, 16, GuiLang.BUTTON_CANCEL.translate())
        {
            @Override
            public void onClicked(IGui gui, IMouseButton button)
            {
                GuiHelper.playClickSound();
                callback.onCallback(defValue, false);
            }

            @Override
            public int renderTitleInCenter(IGui gui)
            {
                return gui.getTextColor();
            }
        };

        buttonCancel.setIcon(ButtonLM.DEFAULT_BACKGROUND);

        buttonAccept = new ButtonLM(getWidth() - bsize - 2, getHeight() - 18, bsize, 16, GuiLang.BUTTON_ACCEPT.translate())
        {
            @Override
            public void onClicked(IGui gui, IMouseButton button)
            {
                GuiHelper.playClickSound();
                if(textBox.isValid())
                {
                    setValue(value, textBox.getText());
                    callback.onCallback(value, true);
                }
            }

            @Override
            public int renderTitleInCenter(IGui gui)
            {
                return gui.getTextColor();
            }
        };

        buttonAccept.setIcon(ButtonLM.DEFAULT_BACKGROUND);

        textBox = new TextBoxLM(2, 2, getWidth() - 4, 18)
        {
            @Override
            public boolean isValid()
            {
                return GuiAbstractField.this.isValidText(value, getText());
            }

            @Override
            public void onEnterPressed(IGui gui)
            {
                buttonAccept.onClicked(GuiAbstractField.this, MouseButton.LEFT);
            }
        };

        textBox.setText(val.toString());
        textBox.textRenderX = -1;
        textBox.textRenderY = 6;
        textBox.textColor = 0xFFEEEEEE;
        textBox.setSelected(this, true);
        textBox.background = ButtonLM.DEFAULT_BACKGROUND;
    }

    protected abstract boolean isValidText(IConfigValue value, String val);

    protected abstract void setValue(IConfigValue value, String val);

    public GuiAbstractField setCharLimit(int i)
    {
        textBox.charLimit = i;
        return this;
    }

    @Override
    public void addWidgets()
    {
        add(buttonCancel);
        add(buttonAccept);
        add(textBox);
    }

    @Override
    public void drawBackground()
    {
        getIcon(this).draw(this);
        int size = 8 + getFont().getStringWidth(textBox.getText());
        if(size > getWidth())
        {
            setWidth(size);
            int bsize = size / 2 - 4;
            buttonAccept.setWidth(bsize);
            buttonCancel.setWidth(bsize);
            buttonAccept.posX = getWidth() - bsize - 2;
            textBox.setWidth(getWidth() - 4);
            initGui();
        }
    }

    @Override
    public IDrawableObject getIcon(IGui gui)
    {
        return DEFAULT_BACKGROUND;
    }
}