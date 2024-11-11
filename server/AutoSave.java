package server;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.World;
import server.MedalRanking;
import server.Timer;
import server.marriage.MarriageManager;
import tools.StringUtil;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Admin
 */
public class AutoSave {
    public static void startAutoSave() {
        Timer.EtcTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                try {
                    for (ChannelServer ch : ChannelServer.getAllInstances()) {
                        for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                            chr.saveToDB(false, false);
                        }
                    }
                    World.Guild.save();
                    World.Alliance.save();
                    World.Family.save();
                    MarriageManager.getInstance().saveAll();
                    MedalRanking.saveAll();
                    for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                        ChannelServer.getInstance(i).saveMerchant();
                        System.out.println("Saving merchant... " + (i == 2 ? 20 : i != 1 ? (i - 1) : 1) + " Channel");
                    }
                    System.out.println("[AutoSave System] " + StringUtil.getCurrentTime() + " Complete");
                } catch (Exception e) {
                    System.err.println("[AutoSave System] Err AutoSave");
                }
            }
        }, 1000 * 60 * 60, 1000 * 60 * 60);
    }
}
