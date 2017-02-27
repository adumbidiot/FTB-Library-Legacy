package com.feed_the_beast.ftbl.api_impl;

import com.feed_the_beast.ftbl.FTBLibConfig;
import com.feed_the_beast.ftbl.FTBLibMod;
import com.feed_the_beast.ftbl.FTBLibModCommon;
import com.feed_the_beast.ftbl.api.EnumTeamColor;
import com.feed_the_beast.ftbl.api.EnumTeamStatus;
import com.feed_the_beast.ftbl.api.IForgePlayer;
import com.feed_the_beast.ftbl.api.IForgeTeam;
import com.feed_the_beast.ftbl.api.ITeamMessage;
import com.feed_the_beast.ftbl.api.config.IConfigKey;
import com.feed_the_beast.ftbl.api.config.IConfigTree;
import com.feed_the_beast.ftbl.api.events.team.ForgeTeamOwnerChangedEvent;
import com.feed_the_beast.ftbl.api.events.team.ForgeTeamPlayerJoinedEvent;
import com.feed_the_beast.ftbl.api.events.team.ForgeTeamPlayerLeftEvent;
import com.feed_the_beast.ftbl.api.events.team.ForgeTeamSettingsEvent;
import com.feed_the_beast.ftbl.lib.EnumNameMap;
import com.feed_the_beast.ftbl.lib.FinalIDObject;
import com.feed_the_beast.ftbl.lib.NBTDataStorage;
import com.feed_the_beast.ftbl.lib.config.ConfigKey;
import com.feed_the_beast.ftbl.lib.config.ConfigTree;
import com.feed_the_beast.ftbl.lib.config.PropertyBool;
import com.feed_the_beast.ftbl.lib.config.PropertyEnum;
import com.feed_the_beast.ftbl.lib.config.PropertyString;
import com.feed_the_beast.ftbl.lib.internal.FTBLibIntegrationInternal;
import com.feed_the_beast.ftbl.lib.io.Bits;
import com.feed_the_beast.ftbl.lib.util.LMNetUtils;
import com.feed_the_beast.ftbl.lib.util.LMServerUtils;
import com.feed_the_beast.ftbl.lib.util.LMStringUtils;
import com.feed_the_beast.ftbl.net.MessageDisplayTeamMsg;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by LatvianModder on 26.05.2016.
 */
public final class ForgeTeam extends FinalIDObject implements IForgeTeam
{
    private static final IConfigKey KEY_COLOR = new ConfigKey("display.color", new PropertyEnum<>(EnumTeamColor.NAME_MAP, EnumTeamColor.BLUE), new TextComponentTranslation("ftbteam.config.display.color"));
    private static final IConfigKey KEY_TITLE = new ConfigKey("display.title", new PropertyString(""), new TextComponentTranslation("ftbteam.config.display.title"));
    private static final IConfigKey KEY_DESC = new ConfigKey("display.desc", new PropertyString(""), new TextComponentTranslation("ftbteam.config.display.desc"));
    private static final IConfigKey KEY_FREE_TO_JOIN = new ConfigKey("free_to_join", new PropertyBool(false), new TextComponentTranslation("ftbteam.config.free_to_join"));

    public static class Message implements ITeamMessage
    {
        private final UUID sender;
        private final long time;
        private final String text;

        public Message(UUID s, long t, String txt)
        {
            sender = s;
            time = t;
            text = txt;
        }

        public Message(NBTTagCompound nbt)
        {
            sender = nbt.getUniqueId("Sender");
            time = nbt.getLong("Time");
            text = nbt.getString("Text");
        }

        public Message(ByteBuf io)
        {
            sender = LMNetUtils.readUUID(io);
            time = io.readLong();
            text = ByteBufUtils.readUTF8String(io);
        }

        public static NBTTagCompound toNBT(ITeamMessage msg)
        {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setUniqueId("Sender", msg.getSender());
            nbt.setLong("Time", msg.getTime());
            nbt.setString("Text", msg.getMessage());
            return nbt;
        }

        public static void write(ByteBuf io, ITeamMessage msg)
        {
            LMNetUtils.writeUUID(io, msg.getSender());
            io.writeLong(msg.getTime());
            ByteBufUtils.writeUTF8String(io, msg.getMessage());
        }

        @Override
        public int compareTo(ITeamMessage o)
        {
            return Long.compare(getTime(), o.getTime());
        }

        @Override
        public UUID getSender()
        {
            return sender;
        }

        @Override
        public long getTime()
        {
            return time;
        }

        @Override
        public String getMessage()
        {
            return text;
        }
    }

    private final NBTDataStorage dataStorage;
    private EnumTeamColor color;
    private IForgePlayer owner;
    private String title;
    private String desc;
    private int flags;
    private List<ITeamMessage> chatHistory;
    private Map<UUID, EnumTeamStatus> players;
    private IConfigTree cachedConfig;

    public ForgeTeam(String id)
    {
        super(id);
        color = EnumTeamColor.BLUE;

        title = "";
        desc = "";
        flags = 0;

        dataStorage = FTBLibMod.PROXY.createDataStorage(this, FTBLibModCommon.DATA_PROVIDER_TEAM);
    }

    @Override
    @Nullable
    public INBTSerializable<?> getData(ResourceLocation id)
    {
        return dataStorage == null ? null : dataStorage.get(id);
    }

    @Override
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("Owner", LMStringUtils.fromUUID(owner.getId()));
        nbt.setByte("Flags", (byte) flags);
        nbt.setString("Color", EnumNameMap.getName(color));

        if(!title.isEmpty())
        {
            nbt.setString("Title", title);
        }

        if(!desc.isEmpty())
        {
            nbt.setString("Desc", desc);
        }

        if(players != null && !players.isEmpty())
        {
            NBTTagCompound nbt1 = new NBTTagCompound();

            for(Map.Entry<UUID, EnumTeamStatus> entry : players.entrySet())
            {
                nbt1.setString(LMStringUtils.fromUUID(entry.getKey()), entry.getValue().getName());
            }

            nbt.setTag("Players", nbt1);
        }

        if(dataStorage != null)
        {
            nbt.setTag("Data", dataStorage.serializeNBT());
        }

        if(chatHistory != null && !chatHistory.isEmpty())
        {
            NBTTagList list = new NBTTagList();

            for(ITeamMessage msg : chatHistory)
            {
                list.appendTag(Message.toNBT(msg));
            }

            nbt.setTag("Chat", list);
        }

        return nbt;
    }

    @Override
    public void deserializeNBT(NBTTagCompound nbt)
    {
        owner = Universe.INSTANCE.getPlayer(LMStringUtils.fromString(nbt.getString("Owner")));
        flags = nbt.getInteger("Flags");
        color = EnumTeamColor.NAME_MAP.get(nbt.getString("Color"));
        title = nbt.getString("Title");
        desc = nbt.getString("Desc");

        if(players != null)
        {
            players.clear();
        }

        if(nbt.hasKey("Players"))
        {
            if(players == null)
            {
                players = new HashMap<>();
            }

            NBTTagCompound nbt1 = nbt.getCompoundTag("Players");

            for(String s : nbt1.getKeySet())
            {
                UUID id = LMStringUtils.fromString(s);

                if(id != null)
                {
                    EnumTeamStatus status = EnumTeamStatus.NAME_MAP.get(nbt1.getString(s));

                    if(status != null && status.canBeSet())
                    {
                        players.put(id, status);
                    }
                }
            }
        }

        if(nbt.hasKey("Invited"))
        {
            if(players == null)
            {
                players = new HashMap<>();
            }

            NBTTagList list = nbt.getTagList("Invited", Constants.NBT.TAG_STRING);

            for(int i = 0; i < list.tagCount(); i++)
            {
                UUID id = LMStringUtils.fromString(list.getStringTagAt(i));

                if(id != null && !players.containsKey(id))
                {
                    players.put(id, EnumTeamStatus.INVITED);
                }
            }
        }

        if(dataStorage != null)
        {
            dataStorage.deserializeNBT(nbt.hasKey("Caps") ? nbt.getCompoundTag("Caps") : nbt.getCompoundTag("Data"));
        }

        if(chatHistory != null)
        {
            chatHistory.clear();
        }

        if(nbt.hasKey("Chat"))
        {
            if(chatHistory == null)
            {
                chatHistory = new ArrayList<>();
            }

            NBTTagList list = nbt.getTagList("Chat", Constants.NBT.TAG_COMPOUND);

            for(int i = 0; i < list.tagCount(); i++)
            {
                chatHistory.add(new Message(list.getCompoundTagAt(i)));
            }
        }
    }

    @Override
    public IForgePlayer getOwner()
    {
        return owner;
    }

    @Override
    public String getTitle()
    {
        return title.isEmpty() ? (owner.getName() + (owner.getName().endsWith("s") ? "' Team" : "'s Team")) : title;
    }

    @Override
    public String getDesc()
    {
        return desc;
    }

    @Override
    public EnumTeamColor getColor()
    {
        return color;
    }

    public void setColor(EnumTeamColor col)
    {
        color = col;
    }

    @Override
    public EnumTeamStatus getHighestStatus(UUID playerId)
    {
        if(owner.getId().equals(playerId))
        {
            return EnumTeamStatus.OWNER;
        }

        EnumTeamStatus status = getSetStatus(playerId);

        if(status == EnumTeamStatus.MOD)
        {
            if(!isMember(playerId))
            {
                status = EnumTeamStatus.NONE;
            }
        }
        else if(!status.isEqualOrGreaterThan(EnumTeamStatus.MEMBER) && isMember(playerId))
        {
            status = EnumTeamStatus.MEMBER;
        }

        return status;
    }

    private boolean isMember(UUID playerId)
    {
        IForgePlayer player = FTBLibIntegrationInternal.API.getUniverse().getPlayer(playerId);

        return player != null && player.getTeamID().equals(getName());

    }

    private EnumTeamStatus getSetStatus(UUID playerId)
    {
        if(players == null || players.isEmpty())
        {
            return EnumTeamStatus.NONE;
        }

        EnumTeamStatus status = players.get(playerId);

        if(status == null)
        {
            status = EnumTeamStatus.NONE;

            if(Bits.getFlag(flags, FLAG_FREE_TO_JOIN))
            {
                status = EnumTeamStatus.INVITED;
            }
        }

        return status;
    }

    @Override
    public boolean hasStatus(UUID playerId, EnumTeamStatus status)
    {
        if(status.isNone())
        {
            return true;
        }

        EnumTeamStatus status1 = getHighestStatus(playerId);
        return status1.isEqualOrGreaterThan(status);
    }

    @Override
    public void setStatus(UUID playerId, EnumTeamStatus status)
    {
        if(status.canBeSet() && !status.isNone())
        {
            if(players == null)
            {
                players = new HashMap<>();
            }

            players.put(playerId, status);
        }
        else if(players != null)
        {
            players.remove(playerId);
        }
    }

    @Override
    public Collection<IForgePlayer> getPlayersWithStatus(Collection<IForgePlayer> c, EnumTeamStatus status)
    {
        for(IForgePlayer p : Universe.INSTANCE.getPlayers())
        {
            if(hasStatus(p, status))
            {
                c.add(p);
            }
        }

        return c;
    }

    @Override
    public boolean addPlayer(IForgePlayer player)
    {
        if(!hasStatus(player, EnumTeamStatus.MEMBER) && hasStatus(player, EnumTeamStatus.INVITED))
        {
            player.setTeamID(getName());
            MinecraftForge.EVENT_BUS.post(new ForgeTeamPlayerJoinedEvent(this, player));
            setStatus(player.getId(), EnumTeamStatus.MEMBER);
            return true;
        }

        return false;
    }

    @Override
    public void removePlayer(IForgePlayer player)
    {
        if(hasStatus(player, EnumTeamStatus.MEMBER))
        {
            player.setTeamID("");
            MinecraftForge.EVENT_BUS.post(new ForgeTeamPlayerLeftEvent(this, player));
        }
    }

    @Override
    public void changeOwner(IForgePlayer newOwner)
    {
        if(owner == null)
        {
            owner = newOwner;
            newOwner.setTeamID(getName());
        }
        else
        {
            IForgePlayer oldOwner = owner;

            if(!oldOwner.equalsPlayer(newOwner) && hasStatus(newOwner, EnumTeamStatus.MEMBER))
            {
                owner = newOwner;
                MinecraftForge.EVENT_BUS.post(new ForgeTeamOwnerChangedEvent(this, oldOwner, newOwner));
            }
        }
    }

    @Override
    public IConfigTree getSettings()
    {
        if(cachedConfig != null)
        {
            return cachedConfig;
        }

        cachedConfig = new ConfigTree();
        MinecraftForge.EVENT_BUS.post(new ForgeTeamSettingsEvent(this, cachedConfig));

        cachedConfig.add(KEY_COLOR, new PropertyEnum<EnumTeamColor>(EnumTeamColor.NAME_MAP, EnumTeamColor.BLUE)
        {
            @Override
            public EnumTeamColor get()
            {
                return color;
            }

            @Override
            public void set(@Nullable EnumTeamColor e)
            {
                color = e == null ? EnumTeamColor.BLUE : e;
            }
        });

        cachedConfig.add(KEY_TITLE, new PropertyString("")
        {
            @Override
            public void setString(String v)
            {
                title = v.trim();
            }

            @Override
            public String getString()
            {
                return title;
            }
        });

        cachedConfig.add(KEY_DESC, new PropertyString("")
        {
            @Override
            public void setString(String v)
            {
                desc = v.trim();
            }

            @Override
            public String getString()
            {
                return desc;
            }
        });

        cachedConfig.add(KEY_FREE_TO_JOIN, new PropertyBool(false)
        {
            @Override
            public boolean getBoolean()
            {
                return Bits.getFlag(flags, FLAG_FREE_TO_JOIN);
            }

            @Override
            public void setBoolean(boolean v)
            {
                flags = Bits.setFlag(flags, FLAG_FREE_TO_JOIN, v);
            }
        });

        return cachedConfig;
    }

    @Override
    public int getFlags()
    {
        return flags;
    }

    @Override
    public void printMessage(ITeamMessage message)
    {
        if(chatHistory == null)
        {
            chatHistory = new ArrayList<>();
        }

        while(chatHistory.size() >= FTBLibConfig.MAX_TEAM_CHAT_HISTORY.getInt())
        {
            chatHistory.remove(0);
        }

        chatHistory.add(message);

        MessageDisplayTeamMsg m = new MessageDisplayTeamMsg(message);

        for(EntityPlayerMP ep : getOnlineTeamPlayers(EnumTeamStatus.MEMBER))
        {
            m.sendTo(ep);
        }
    }

    @Override
    public List<ITeamMessage> getMessages()
    {
        return chatHistory == null ? Collections.emptyList() : chatHistory;
    }

    public Collection<EntityPlayerMP> getOnlineTeamPlayers(EnumTeamStatus status)
    {
        Collection<EntityPlayerMP> list = new ArrayList<>();

        for(EntityPlayerMP ep : LMServerUtils.getServer().getPlayerList().getPlayerList())
        {
            if(hasStatus(ep.getGameProfile().getId(), status))
            {
                list.add(ep);
            }
        }

        return list;
    }
}