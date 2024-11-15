package server;

import client.MapleCharacter;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import server.Timer.*;
import server.marriage.MarriageManager;
import server.shops.HiredMerchantSave;
import server.shops.MinervaOwlSearchTop;
import tools.MaplePacketCreator;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShutdownServer implements ShutdownServerMBean {

    public static ShutdownServer instance;

    public static void registerMBean() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            instance = new ShutdownServer();
            mBeanServer.registerMBean(instance, new ObjectName("server:type=ShutdownServer"));
        } catch (Exception e) {
            System.out.println("Error registering Shutdown MBean");
            e.printStackTrace();
        }
    }

    public static ShutdownServer getInstance() {
        return instance;
    }

    public int mode = 0;

    public void shutdown() {//can execute twice
        run();
    }

    AtomicInteger FinishedThreads = new AtomicInteger(0);

    public void incrementT() {
        FinishedThreads.incrementAndGet();
    }

    @Override
    public void run() {
        if (mode == 0) {
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    try {
                        chr.saveToDB(false, false);
                    } catch (Exception e) {
                    }
                }
            }
            int ret = 0;
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, "서버가 종료됩니다."));
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.setShutdown();
                cs.setServerMessage("잠시 후 서버가 종료됩니다.");
                ret += cs.closeAllMerchant();
            }
            try {
                HiredMerchantSave.Execute(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(ShutdownServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            while (FinishedThreads.get() != HiredMerchantSave.NumSavingThreads) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ShutdownServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
            MarriageManager.getInstance().saveAll();
            MinervaOwlSearchTop.getInstance().saveToFile();
            MedalRanking.saveAll();
            CashItemSaleRank.cleanUp();

//            World.Alliance.save();
            System.out.println("Shutdown 1 has completed. Hired merchants saved: " + ret);
            mode++;
        } else if (mode == 1) {
            mode++;
            System.out.println("Shutdown 2 commencing...");
            try {
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, "서버가 종료됩니다."));
                Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);

                for (int i : chs) {
                    try {
                        ChannelServer cs = ChannelServer.getInstance(i);
                        synchronized (this) {
                            cs.shutdown();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                LoginServer.shutdown();
                CashShopServer.shutdown();
                CloneTimer.getInstance().schedule(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 20000L);
                synchronized (this) {
                    try {
                        wait(3000);
                    } catch (Exception e) {
                        //shutdown
                    }
                }
                DatabaseConnection.shutdown();
            } catch (Exception e) {
                System.err.println("THROW" + e);
            }
            WorldTimer.getInstance().stop();
            MapTimer.getInstance().stop();
            BuffTimer.getInstance().stop();
            EventTimer.getInstance().stop();
            EtcTimer.getInstance().stop();
            PingTimer.getInstance().stop();
            System.out.println("Shutdown 2 has finished. Server will be shutdown in 5 secs.");
        }
    }
}
