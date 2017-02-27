package com.feed_the_beast.ftbl.client.teamsgui;

import com.feed_the_beast.ftbl.api.gui.IDrawableObject;
import com.feed_the_beast.ftbl.api.gui.IGui;
import com.feed_the_beast.ftbl.api.gui.IMouseButton;
import com.feed_the_beast.ftbl.api.gui.IPanel;
import com.feed_the_beast.ftbl.api.gui.IWidget;
import com.feed_the_beast.ftbl.lib.client.FTBLibClient;
import com.feed_the_beast.ftbl.lib.client.TexturelessRectangle;
import com.feed_the_beast.ftbl.lib.gui.ButtonLM;
import com.feed_the_beast.ftbl.lib.gui.GuiIcons;
import com.feed_the_beast.ftbl.lib.gui.GuiLM;
import com.feed_the_beast.ftbl.lib.gui.PanelLM;
import com.feed_the_beast.ftbl.lib.gui.PanelScrollBar;
import com.feed_the_beast.ftbl.lib.gui.PlayerHeadImage;
import com.feed_the_beast.ftbl.lib.util.LMColorUtils;

import java.util.Collections;
import java.util.List;

/**
 * Created by LatvianModder on 24.02.2017.
 */
public class GuiSelectTeam extends GuiLM
{
    private final PanelLM panelTeams;
    private final PanelScrollBar scrollTeams;

    private static class ButtonCreateTeam extends ButtonLM
    {
        private final IDrawableObject background;

        private ButtonCreateTeam()
        {
            super(0, 0, 32, 32);
            setTitle("Create a New Team");
            setIcon(GuiIcons.ADD);
            background = new TexturelessRectangle(0).setLineColor(0xFFFFFFFF).setRoundEdges(true);
        }

        @Override
        public void onClicked(IGui gui, IMouseButton button)
        {
            new GuiCreateTeam().openGui();
        }

        @Override
        public void renderWidget(IGui gui)
        {
            int ax = getAX();
            int ay = getAY();
            background.draw(ax, ay, 32, 32);
            getIcon(gui).draw(ax + 8, ay + 8, 16, 16);
        }
    }

    private static class ButtonTeam extends ButtonLM
    {
        private final PublicTeamData team;
        private final IDrawableObject background;

        private ButtonTeam(PublicTeamData t)
        {
            super(0, 0, 32, 32);
            team = t;
            setTitle(team.color.getTextFormatting() + team.displayName);
            setIcon(new PlayerHeadImage(t.ownerName));
            background = new TexturelessRectangle(team.isInvited ? 0x6620A32B : 0).setLineColor(LMColorUtils.getColorFromID(team.color.getColorID())).setRoundEdges(true);
        }

        @Override
        public void onClicked(IGui gui, IMouseButton button)
        {
            if(team.isInvited)
            {
                FTBLibClient.execClientCommand("/ftb team join " + team.getName());
            }
            else
            {
                FTBLibClient.execClientCommand("/ftb team request_invite " + team.getName());
            }

            gui.closeGui();
        }

        @Override
        public void renderWidget(IGui gui)
        {
            int ax = getAX();
            int ay = getAY();
            background.draw(ax, ay, 32, 32);
            getIcon(gui).draw(ax + 8, ay + 8, 16, 16);
        }

        @Override
        public void addMouseOverText(IGui gui, List<String> list)
        {
            list.add(getTitle(gui));
            list.add("ID: " + team.getName());

            if(!team.description.isEmpty())
            {
                list.add("");
                list.add(team.description);
            }

            list.add("");
            list.add("Click to " + (team.isInvited ? "join the team" : "request invite to this team"));
        }
    }

    public GuiSelectTeam(List<PublicTeamData> teams)
    {
        super(192, 170);
        Collections.sort(teams);

        panelTeams = new PanelLM(0, 1, 168, 168)
        {
            @Override
            public void addWidgets()
            {
                add(new ButtonCreateTeam());

                for(PublicTeamData t : teams)
                {
                    add(new ButtonTeam(t));
                }
            }

            @Override
            public void updateWidgetPositions()
            {
                scrollTeams.elementSize = 8;
                int x = 0;

                for(IWidget widget : getWidgets())
                {
                    widget.setX(8 + x * 40);
                    widget.setY(scrollTeams.elementSize);

                    x++;

                    if(x == 4)
                    {
                        x = 0;
                        scrollTeams.elementSize += 40;
                    }
                }
            }
        };

        panelTeams.addFlags(IPanel.FLAG_DEFAULTS);

        scrollTeams = new PanelScrollBar(168, 8, 16, 152, 10, panelTeams)
        {
            @Override
            public boolean shouldRender(IGui gui)
            {
                return true;
            }
        };

        scrollTeams.background = ButtonLM.DEFAULT_BACKGROUND;
    }

    @Override
    public void addWidgets()
    {
        add(panelTeams);
        add(scrollTeams);
    }

    @Override
    public IDrawableObject getIcon(IGui gui)
    {
        return DEFAULT_BACKGROUND;
    }
}