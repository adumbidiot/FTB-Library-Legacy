package com.feed_the_beast.ftbl.cmd;

import com.feed_the_beast.ftbl.api.config.IConfigContainer;
import com.feed_the_beast.ftbl.lib.cmd.CmdEditConfigBase;
import com.feed_the_beast.ftbl.lib.config.BasicConfigContainer;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * Created by LatvianModder on 29.05.2016.
 */
public class CmdMySettings extends CmdEditConfigBase
{
    private static final ITextComponent TITLE = new TextComponentString("My Server Settings");

    public CmdMySettings()
    {
        super("my_settings", Level.ALL);
    }

    @Override
    public IConfigContainer getConfigContainer(ICommandSender sender) throws CommandException
    {
        return new BasicConfigContainer(TITLE, getForgePlayer(sender).getSettings());
    }
}