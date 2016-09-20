package com.feed_the_beast.ftbl.cmd;

import com.feed_the_beast.ftbl.api.cmd.CommandLM;
import com.feed_the_beast.ftbl.api.events.ReloadType;
import com.feed_the_beast.ftbl.api_impl.FTBLibAPI_Impl;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CmdReload extends CommandLM
{
    public CmdReload()
    {
        super("reload");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender ics, String[] args) throws CommandException
    {
        FTBLibAPI_Impl.INSTANCE.reload(ics, ReloadType.SERVER_ONLY_NOTIFY_CLIENT);
    }
}