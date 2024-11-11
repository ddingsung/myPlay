package client.messages.commands;

//import client.MapleInventory;
//import client.MapleInventoryType;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.PlayerStats;
import client.SkillFactory;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import scripting.NPCScriptManager;
import scripting.vm.NPCScriptInvoker;
import tools.MaplePacketCreator;

import java.util.Map;
import server.log.ServerLogger;
import server.maps.MapleMap;
import server.maps.SavedLocationType;

/**
 * @author Emilyx3
 */
public class PlayerCommand {

    public static ServerConstants.PlayerGMRank getPlayerLevelRequired() {
        return ServerConstants.PlayerGMRank.NORMAL;
    }

    public static class For extends 렉 {
    }

    public static class fpr extends 렉 {
    }

    public static class 랙 extends 렉 {
    }

    public static class 렉 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            return 1;
        }
    }

    public static class 자리 extends 맵주인 {
    }

    public static class 맵주인 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getMapOwnerPrivate() > -1) {
                c.getPlayer().dropMessage(5, "현재 맵의 주인은 " + c.getPlayer().getMap().getMapOwnerName() + "입니다.");
            }
            return 1;
        }
    }

    public static class 도움말 extends PlayerCommand.명령어 {
    }

    public static class 명령어 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            player.dropMessage(6, "@렉 : 오류로 인한 이미 활성중인 상태를 비활성으로 전환.");
            player.dropMessage(6, "@맵주인, 자리 : 현재 위치하는 맵의 주인을 표시.");
            player.dropMessage(6, "@힘, 덱, 인, 럭 <숫자> : 숫자만큼 AP를 소비하여 스텟을 올립니다.");
            //player.dropMessage(6, "@피시방 : 피시방 맵으로 이동.(피시방 시간이 있을때만 이동가능)");
            return 1;
        }
    }

    public static class 힘 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int str = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();
                if (stat.getStr() + str > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < str || c.getPlayer().getRemainingAp() < 0 || str < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setStr((short) (stat.getStr() + str), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - str));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.STR, stat.getStr());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 지 extends PlayerCommand.인 {
    }

    public static class 지력 extends PlayerCommand.인 {
    }

    public static class 인트 extends PlayerCommand.인 {
    }

    public static class 인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int int_ = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();

                if (stat.getInt() + int_ > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < int_ || c.getPlayer().getRemainingAp() < 0 || int_ < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setInt((short) (stat.getInt() + int_), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - int_));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.INT, stat.getInt());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 민첩 extends PlayerCommand.덱 {
    }

    public static class 민 extends PlayerCommand.덱 {
    }

    public static class 덱스 extends PlayerCommand.덱 {
    }

    public static class 덱 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int dex = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();

                if (stat.getDex() + dex > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < dex || c.getPlayer().getRemainingAp() < 0 || dex < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setDex((short) (stat.getDex() + dex), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - dex));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.DEX, stat.getDex());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 운 extends PlayerCommand.럭 {
    }

    public static class 행운 extends PlayerCommand.럭 {
    }

    public static class 럭 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                int luk = Integer.parseInt(splitted[1]);
                final PlayerStats stat = c.getPlayer().getStat();

                if (stat.getLuk() + luk > Short.MAX_VALUE || c.getPlayer().getRemainingAp() < luk || c.getPlayer().getRemainingAp() < 0 || luk < 0 && !c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "오류가 발생했습니다.");
                } else {
                    stat.setLuk((short) (stat.getLuk() + luk), c.getPlayer());
                    c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - luk));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
                    c.getPlayer().updateSingleStat(MapleStat.LUK, stat.getLuk());
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "숫자를 제대로 입력해주세요.");
            }
            return 1;
        }
    }

    public static class 길드공지 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            final String notice = splitted[1];
            if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) {
                player.dropMessage(6, "길드를 가지고 있지 않거나 권한이 부족한것 같은데?");
                return 1;
            }
            if (notice.length() > 100) {
                player.dropMessage(6, "너무 길어 씹년아");
                return 1;
            }
            World.Guild.setGuildNotice(c.getPlayer().getGuildId(), notice);
            return 1;
        }
    }

    public static class 피씨방MJZ extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int time = 0;
            try {
                time = Integer.parseInt(splitted[1]);
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "피씨방 시간을 초기화하였습니다.");
                c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 0, c.getPlayer().getCalcPcTime()));
                c.getPlayer().setPcTime((long) 0);
                return 0;
            }
            c.getPlayer().setPcTime((long) time);
            c.getPlayer().setPcDate(GameConstants.getCurrentDate_NoTime());
            c.getPlayer().dropMessage(6, time / 1000 + "초 피씨방을 충전하였습니다.");
            c.getSession().write(MaplePacketCreator.enableInternetCafe((byte) 2, c.getPlayer().getCalcPcTime()));
            return 0;
        }
    }

    public static class 아이피등록MJZ extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                if (!c.isEligible()) {
                    Connection con = null;
                    try {
                        con = DatabaseConnection.getConnection();
                        PreparedStatement ps;
                        ps = con.prepareStatement("INSERT INTO master_ip VALUES (?)");
                        ps.setString(1, c.getIp());
                        ps.execute();
                        ps.close();
                        c.getPlayer().dropMessage(6, "어드민 아이피 등록 완료");
                    } catch (SQLException ex) {
                        System.err.println("Error while tempbanning" + ex);
                    } finally {
                        if (con != null) {
                            try {
                                con.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(5, "이미 등록되어 있습니다.");
                    return 0;
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "실패");
                ServerLogger.getInstance().getGMLog("아이피등록 명령어 사용 실패 / 닉네임 : " + c.getPlayer().getName() + " 어카운트 ID : " + c.getAccID() + " / 아이디 : " + c.getAccountName() + " / IP : " + c.getIp());
                return 0;
            }
            ServerLogger.getInstance().getGMLog("아이피등록 명령어 사용 / 닉네임 : " + c.getPlayer().getName() + " 어카운트 ID : " + c.getAccID() + " / 아이디 : " + c.getAccountName() + " / IP : " + c.getIp());
            return 1;
        }
    }

    /*    public static class 피시방 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getLevel() < 10) {
                if (GameConstants.isBeginnerJob(c.getPlayer().getJob())) {
                    c.getPlayer().dropMessage(5, "10레벨 미만 초보자는 이용하실 수 없습니다.");
                    return 0;
                }
            }
            if (c.getPlayer().getMapId() >= 190000000 && c.getPlayer().getMapId() <= 198000000) {
                c.getPlayer().dropMessage(6, "여기서는 이용하실 수 없습니다.");
                return 0;
            }
            if (c.getPlayer().getPcTime() < System.currentTimeMillis()) {
                c.getPlayer().dropMessage(6, "자네 피시방 이용권도 없이 어딜가는겐가??");
                return 0;
            }
            if (c.getPlayer().getMapId() != 193000000) {
                c.getPlayer().saveLocation(SavedLocationType.ARDENTMILL);
                c.getPlayer().dropMessage(5, c.getPlayer().getMap().getMapName() + " 맵을 저장하고 피시방으로 이동하였습니다.");
            }
            MapleMap map = c.getChannelServer().getMapFactory().getMap(193000000);
            c.getPlayer().changeMap(map, map.getPortal(0));
            return 1;
        }
    }*/
}
