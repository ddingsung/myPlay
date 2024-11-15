/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools;

import client.*;
import client.inventory.Equip.ScrollResult;
import client.inventory.*;
import constants.GameConstants;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import handling.channel.MapleGuildRanking.GuildRankingInfo;
import handling.channel.handler.DamageParse;
import handling.channel.handler.InventoryHandler;
import handling.world.MapleParty;
import handling.world.MaplePartyCharacter;
import handling.world.PartyOperation;
import handling.world.World;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleBBSThread;
import handling.world.guild.MapleBBSThread.MapleBBSReply;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.guild.MapleGuildCharacter;
import server.MapleDueyActions;
import server.MapleShop;
import server.MapleTrade;
import server.events.MapleSnowball.MapleSnowballs;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.PlayerNPC;
import server.maps.*;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.movement.LifeMovementFragment;
import server.shops.*;
import tools.data.LittleEndianAccessor;
import tools.data.MaplePacketLittleEndianWriter;
import tools.packet.PacketHelper;

import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import server.Randomizer;
import server.RankingWorker;
import server.RankingWorker.RankingInformation;
import server.marriage.MarriageDataEntry;
import server.marriage.MarriageManager;

public class MaplePacketCreator {

    public static byte[] sombra() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString("Sombra");
        mplew.writeShort(-5);
        mplew.writeShort(32767);
        mplew.write(("                          :PB@Bk:\n"
                + "                      ,jB@@B@B@B@BBL.\n"
                + "                   7G@B@B@BMMMMMB@B@B@Nr\n"
                + "               :kB@B@@@MMOMOMOMOMMMM@B@B@B1,\n"
                + "           :5@B@B@B@BBMMOMOMOMOMOMOMM@@@B@B@BBu.\n"
                + "        70@@@B@B@B@BXBBOMOMOMOMOMOMMBMPB@B@B@B@B@Nr\n"
                + "      G@@@BJ iB@B@@  OBMOMOMOMOMOMOM@2  B@B@B. EB@B@S\n"
                + "      @@BM@GJBU.  iSuB@OMOMOMOMOMOMM@OU1:  .kBLM@M@B@\n"
                + "      B@MMB@B       7@BBMMOMOMOMOMOBB@:       B@BMM@B\n"
                + "      @@@B@B         7@@@MMOMOMOMM@B@:         @@B@B@\n"
                + "      @@OLB.          BNB@MMOMOMM@BEB          rBjM@B\n"
                + "      @@  @           M  OBOMOMM@q  M          .@  @@\n"
                + "      @@OvB           B:u@MMOMOMMBJiB          .BvM@B\n"
                + "      @B@B@J         0@B@MMOMOMOMB@B@u         q@@@B@\n"
                + "      B@MBB@v       G@@BMMMMMMMMMMMBB@5       F@BMM@B\n"
                + "      @BBM@BPNi   LMEB@OMMMM@B@MMOMM@BZM7   rEqB@MBB@\n"
                + "      B@@@BM  B@B@B  qBMOMB@B@B@BMOMBL  B@B@B  @B@B@M\n"
                + "       J@@@@PB@B@B@B7G@OMBB.   ,@MMM@qLB@B@@@BqB@BBv\n"
                + "          iGB@,i0@M@B@MMO@E  :  M@OMM@@@B@Pii@@N:\n"
                + "             .   B@M@B@MMM@B@B@B@MMM@@@M@B\n"
                + "                 @B@B.i@MBB@B@B@@BM@::B@B@\n"
                + "                 B@@@ .B@B.:@B@ :B@B  @B@O\n"
                + "                   :0 r@B@  B@@ .@B@: P:\n"
                + "                       vMB :@B@ :BO7\n"
                + "                           ,B@B").getBytes());

        return mplew.getPacket();
    }

    public final static Map<MapleStat, Integer> EMPTY_STATUPDATE = new EnumMap<MapleStat, Integer>(MapleStat.class);

    /*
     * 이펙트 코드 정리.<br/><br/>
     *
     * 0 : 레벨 업
     * 6 : (null) 아이템을 소비하여 경험치가 떨어지지 않았습니다.
     * 7 : 포탈소리;
     * 8 : 직업변경
     * 9 : 퀘스트 클리어
     * 10 : 체력 회복 (회복 스킬 쓸때..) + 1byte
     * 11 : 괴상한 이펙트
     * 12 : ?
     * 13 : 몬스터북 획득
     * 15 : 타임리스, 리버스 아이템 레벨업
     * 16 : 주문서 성공
     * 17 : 괴상한 이펙트
     *
     */
    public static final byte[] getServerIP(final MapleClient c, final int port, final int clientId) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        if (c.getTempIP().length() > 0) {
            for (String s : c.getTempIP().split(",")) {
                mplew.write(Integer.parseInt(s));
            }
        } else {
            mplew.write(ServerConstants.Gateway_IP);
        }
        mplew.writeShort(port);
        mplew.writeInt(clientId);
        mplew.write(0); //?  not sure
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static final byte[] getChannelChange(final MapleClient c, final int port) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CHANGE_CHANNEL.getValue());
        mplew.write(1);
        if (c.getTempIP().length() > 0) {
            for (String s : c.getTempIP().split(",")) {
                mplew.write(Integer.parseInt(s));
            }
        } else {
            mplew.write(ServerConstants.Gateway_IP);
        }
        mplew.writeShort(port);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static final byte[] getCharInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - 1); // *(_DWORD *)(dword_760728 + 8252) = CInPacket::Decode4(a2);
        mplew.writeInt(0);//확실 - 인트
        mplew.write(1);//확실 - 바이트
        mplew.write(1);//확실 - 바이트
        mplew.writeShort(0);//확실 -쇼트
//        mplew.writeMapleAsciiString("뚜뚜 기여워 월드 테스트서버");
//        mplew.writeMapleAsciiString("레벨별 경험치 적용중");
//        mplew.writeMapleAsciiString("모든 레벨 구간 테스트를 위해 저레벨 구간은 배율이 낮으니 양해 부탁");
//        mplew.writeMapleAsciiString("30까지는 대충 3배 50까지 5배 70까지 10배 그 이후로 15 20 25 30배");
        chr.getCRand1().connectData(mplew);
        PacketHelper.addCharacterInfo(mplew, chr);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        return mplew.getPacket();
    }

    public static final byte[] enableActions() {
        return enableActions(true);
    }

    public static final byte[] enableActions(boolean hang) {
        return updatePlayerStats(EMPTY_STATUPDATE, true, 0);
    }

    public static final byte[] updatePlayerStats(final Map<MapleStat, Integer> stats, final int evan) {
        return updatePlayerStats(stats, false, evan);
    }

    public static final byte[] updatePlayerStats(final Map<MapleStat, Integer> mystats, final boolean itemReaction, final int evan) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_STATS.getValue());
        mplew.write(itemReaction ? 1 : 0);
        long updateMask = 0;
        for (MapleStat statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        mplew.writeInt((int) updateMask);
        Long value;

        for (final Entry<MapleStat, Integer> statupdate : mystats.entrySet()) {
            value = statupdate.getKey().getValue();
            if (value >= 1) {
                if (value == MapleStat.SKIN.getValue()) {
                    mplew.writeShort(statupdate.getValue().shortValue());
                } else if (value <= MapleStat.HAIR.getValue()) {
                    mplew.writeInt(statupdate.getValue());
                } else if (value < MapleStat.JOB.getValue()) {
                    mplew.write(statupdate.getValue().byteValue());
                } else if (value == MapleStat.AVAILABLESP.getValue()) { //availablesp
                    if (GameConstants.isEvan(evan) || GameConstants.isResist(evan) || GameConstants.isMercedes(evan)) {
                        throw new UnsupportedOperationException("Evan/Resistance/Mercedes wrong updating");
                    } else {
                        mplew.writeShort(statupdate.getValue().shortValue());
                    }
                } else if (value >= MapleStat.HP.getValue() && value <= MapleStat.MAXMP.getValue()) {
                    mplew.writeInt(statupdate.getValue().intValue());
                } else if (value < MapleStat.EXP.getValue()) {
                    mplew.writeShort(statupdate.getValue().shortValue()); //bb - hp/mp are ints
                } else if (value == MapleStat.PET.getValue()) {
                    mplew.writeLong(statupdate.getValue().intValue()); //uniqueID of 3 pets
                    mplew.writeLong(statupdate.getValue().intValue());
                    mplew.writeLong(statupdate.getValue().intValue());
                } else {
                    mplew.writeInt(statupdate.getValue().intValue());
                }
            }
        }
        if (updateMask == 0 && !itemReaction) {
            mplew.write(1);//요기 값이 뭔지 봐야할듯
        }
        mplew.writeShort(0);
        /*
        1byte
        1byte hp mp 회복력 있을시 여기 업데이트 //패킷으로 자동으로 들어올지도?
         */
        return mplew.getPacket();
    }

    public static final byte[] updateSp(MapleCharacter chr, final boolean itemReaction) { //this will do..
        return updateSp(chr, itemReaction, false);
    }

    public static final byte[] updateSp(MapleCharacter chr, final boolean itemReaction, final boolean overrideJob) { //this will do..
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.UPDATE_STATS.getValue());
        mplew.write(itemReaction ? 1 : 0);
        mplew.writeInt(0x8000);
        if (overrideJob || GameConstants.isEvan(chr.getJob()) || GameConstants.isResist(chr.getJob()) || GameConstants.isMercedes(chr.getJob())) {
            mplew.write(chr.getRemainingSpSize());
            for (int i = 0; i < chr.getRemainingSps().length; i++) {
                if (chr.getRemainingSp(i) > 0) {
                    mplew.write(i + 1);
                    mplew.write(chr.getRemainingSp(i));
                }
            }
        } else {
            mplew.writeShort(chr.getRemainingSp());
        }
        mplew.writeShort(0);
        return mplew.getPacket();

    }

    public static final byte[] getWarpToMap(final MapleMap to, final int spawnPoint, final MapleCharacter chr) {

        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.writeInt(chr.getClient().getChannel() - 1);
        mplew.writeInt(0);//
        mplew.write(to.getPortals().size()); //포탈 갯수네
        mplew.write(0);//이 값이 0 이 아니면 인겜 워프투맵
        mplew.writeShort(0);//확실
        mplew.write(0);//확실
        mplew.writeInt(to.getId());
        mplew.write(spawnPoint);
        mplew.writeInt(chr.getStat().getHp());//확실
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.write(GameConstants.isResist(chr.getJob()) ? 0 : 1);//불확실

        return mplew.getPacket();
    }

    public static final byte[] instantMapWarp(final byte portal) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CURRENT_MAP_WARP.getValue());
        mplew.write(0);
        mplew.write(portal); // 6

        return mplew.getPacket();
    }

    public static final byte[] spawnPortal(final int townId, final int targetId, final int skillId, final Point pos) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if (townId != 999999999 && targetId != 999999999) {
            mplew.writeInt(skillId);
            mplew.writePos(pos);
        }

        return mplew.getPacket();
    }

    public static final byte[] spawnDoor(final int oid, final Point pos, final boolean animation) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_DOOR.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(oid);
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    public static byte[] removeDoor(int oid, boolean animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.REMOVE_DOOR.getValue());
        mplew.write(0);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] spawnSummon(MapleSummon summon, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_SUMMON.getValue());
        mplew.writeInt(summon.getOwnerId());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(summon.getSkill());
        mplew.write(summon.getOwnerLevel() - 1);
        mplew.write(summon.getSkillLevel());
        mplew.writePos(summon.getPosition());
        mplew.write(summon.getSkill() == 32111006 ? 5 : 4);// Summon Reaper Buff - Call of the Wild
        if ((summon.getSkill() == 35121003) && (summon.getOwner().getMap() != null)) {//Giant Robot SG-88
            mplew.writeShort(summon.getOwner().getMap().getFootholds().findBelow(summon.getPosition()).getId());
        } else {
            mplew.writeShort(0);
        }
        mplew.write(summon.getMovementType().getValue());
        mplew.write(summon.getSummonType()); // 0 = Summon can't attack - but puppets don't attack with 1 either ^.-
        mplew.write(animated ? 1 : 0);
        final MapleCharacter chr = summon.getOwner();
        mplew.write(summon.getSkill() == 4341006 && chr != null ? 1 : 0); //mirror target
        if (summon.getSkill() == 4341006 && chr != null) {
            PacketHelper.addCharLook(mplew, chr, true);
        }
        if (summon.getSkill() == 35111002) {
            mplew.write(0);
        }

        return mplew.getPacket();
    }

    public static byte[] removeSummon(MapleSummon summon, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_SUMMON.getValue());
        mplew.writeInt(summon.getOwnerId());
        mplew.writeInt(summon.getObjectId());
        if (animated) {
            switch (summon.getSkill()) {
                case 35121003:
                    mplew.write(10);
                    break;
                case 35111001:
                case 35111010:
                case 35111009:
                case 35111002:
                case 35111005:
                case 35111011:
                case 35121009:
                case 35121010:
                case 35121011:
                case 33101008:
                    mplew.write(5);
                    break;
                default:
                    mplew.write(0);
                    break;
            }
        } else {
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    /**
     * Possible values for <code>type</code>:<br> 1: You cannot move that
     * channel. Please try again later.<br> 2: You cannot go into the cash shop.
     * Please try again later.<br> 3: The Item-Trading shop is currently
     * unavailable, please try again later.<br> 4: You cannot go into the trade
     * shop, due to the limitation of user count.<br> 5: You do not meet the
     * minimum level requirement to access the Trade Shop.<br>
     *
     * @param type The type
     * @return The "block" packet.
     */
    public static byte[] serverBlocked(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVER_BLOCKED.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    //9 = cannot join due to party, 1 = cannot join at this time, sry
    public static byte[] pvpBlocked(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_BLOCKED.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] serverMessage(String message) {
        return serverMessage(4, 0, message, false);
    }

    public static byte[] serverNotice(int type, String message) {
        return serverMessage(type, 0, message, false);
    }

    public static byte[] serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false);
    }

    public static byte[] serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, smegaEar);
    }

    private static byte[] serverMessage(int type, int channel, String message, boolean megaEar) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        /*	* 0: [Notice]<br>
         * 1: Popup<br>
         * 2: Megaphone<br>
         * 3: Super Megaphone<br>
         * 4: Scrolling message at top<br>
         * 5: Pink Text<br>
         * 6: Lightblue Text
         * 8: Item megaphone
         * 9: 미작동
         * 10: 3줄 확성기
         * 11: Green megaphone message?
         * 12: Three line of megaphone text
         * 13: End of file =.="
         * 14: Ani msg
         * 15: Red Gachapon box
         * 18: Blue Notice (again)*/
        mplew.writeOpcode(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(type);
        if (type == 4) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(message);

        switch (type) {
            case 3:
            case 9:
                mplew.write(channel - 1); // channel
                mplew.write(megaEar ? 1 : 0);
                break;
            case 11://아이템 사용해버리네
                mplew.writeInt(5120000); // channel
                break;
            case 6:
            case 18:
                mplew.writeInt(channel >= 1000000 && channel < 6000000 ? channel : 0); //cash itemID, displayed in yellow by the {name}
                //E.G. All new EXP coupon {Ruby EXP Coupon} is now available in the Cash Shop!
                //with Ruby Exp Coupon being in yellow and with item info
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] getGachaponMega(final String name, final String message, final Item item, final byte rareness) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(15);
        mplew.writeMapleAsciiString(name + message);
        mplew.writeInt(0); // 0~3 i think
        mplew.writeMapleAsciiString(name);
        PacketHelper.addItemInfo(mplew, item, true, true);

        return mplew.getPacket();
    }

    public static byte[] getAniMsg(final int questID, final int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(14);
        mplew.writeShort(questID);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] tripleSmega(List<String> message, boolean ear, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(10);

        if (message.get(0) != null) {
            mplew.writeMapleAsciiString(message.get(0));
        }
        mplew.write(message.size());
        for (int i = 1; i < message.size(); i++) {
            if (message.get(i) != null) {
                mplew.writeMapleAsciiString(message.get(i));
            }
        }
        mplew.write(channel - 1);
        mplew.write(ear ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getAvatarMega(MapleCharacter chr, int channel, int itemId, String message, boolean ear) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.AVATAR_MEGA.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeMapleAsciiString(message);
        mplew.writeInt(channel - 1); // channel
        mplew.write(ear ? 1 : 0);
        PacketHelper.addCharLook(mplew, chr, true);

        return mplew.getPacket();
    }

    public static byte[] itemMegaphone(String msg, boolean whisper, int channel, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(8);
        mplew.writeMapleAsciiString(msg);
        mplew.write(channel - 1);
        mplew.write(whisper ? 1 : 0);

        if (item == null) {
            mplew.write(0);
        } else {
            PacketHelper.addItemInfo(mplew, item, false, false, true);
        }
        return mplew.getPacket();
    }

    public static byte[] echoMegaphone(String name, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ECHO_MESSAGE.getValue());
        mplew.write(0); //1 = Your echo message has been successfully sent
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.writeMapleAsciiString(name); //name
        mplew.writeMapleAsciiString(message); //message

        return mplew.getPacket();
    }

    public static byte[] spawnNPC(MapleNPC life, boolean show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_NPC.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        mplew.write(life.getF() == 1 ? 0 : 1);
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(show ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] removeNPC(final int objectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_NPC.getValue());
        mplew.writeInt(objectid);

        return mplew.getPacket();
    }

    public static byte[] removeNPCController(final int objectid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(0);
        mplew.writeInt(objectid);

        return mplew.getPacket();
    }

    public static byte[] spawnNPCRequestController(MapleNPC life, boolean MiniMap) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_NPC_REQUEST_CONTROLLER.getValue());
        mplew.write(1);
        mplew.writeInt(life.getObjectId());
        mplew.writeInt(life.getId());
        mplew.writeShort(life.getPosition().x);
        mplew.writeShort(life.getCy());
        mplew.write(life.getF() == 1 ? 0 : 1);
        mplew.writeShort(life.getFh());
        mplew.writeShort(life.getRx0());
        mplew.writeShort(life.getRx1());
        mplew.write(MiniMap ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] spawnPlayerNPC(PlayerNPC npc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.PLAYER_NPC.getValue());
        mplew.write(1);
        mplew.writeInt(npc.getId());
        mplew.writeMapleAsciiString(npc.getName());

        mplew.write(npc.getGender());
        mplew.write(npc.getSkin());
        mplew.writeInt(npc.getFace());
        mplew.write(1);
        mplew.writeInt(npc.getHair());
        Map<Byte, Integer> equip = npc.getEquips();
        Map<Byte, Integer> myEquip = new LinkedHashMap<Byte, Integer>();
        Map<Byte, Integer> maskedEquip = new LinkedHashMap<Byte, Integer>();
        for (Entry<Byte, Integer> position : equip.entrySet()) {
            byte pos = (byte) (position.getKey() * -1);
            if (pos < 100 && myEquip.get(pos) == null) {
                myEquip.put(pos, position.getValue());
            } else if (pos > 100 && pos != 111) { // don't ask. o.o
                pos = (byte) (pos - 100);
                if (myEquip.get(pos) != null) {
                    maskedEquip.put(pos, myEquip.get(pos));
                }
                myEquip.put(pos, position.getValue());
            } else if (myEquip.get(pos) != null) {
                maskedEquip.put(pos, position.getValue());
            }
        }
        for (Entry<Byte, Integer> entry : myEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        for (Entry<Byte, Integer> entry : maskedEquip.entrySet()) {
            mplew.write(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        mplew.write(0xFF);
        Integer cWeapon = equip.get((byte) -111);
        if (cWeapon != null) {
            mplew.writeInt(cWeapon);
        } else {
            mplew.writeInt(0);
        }
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(npc.getPet(0));
        }

        return mplew.getPacket();
    }

    public static byte[] getChatText(int cidfrom, String text, boolean whiteBG, int show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.write(whiteBG ? 1 : 0);
        mplew.writeMapleAsciiString(text);
        mplew.write(show);

        return mplew.getPacket();
    }

    public static byte[] GameMaster_Func(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GM_EFFECT.getValue());
        mplew.write(value);
        mplew.writeZeroBytes(17);

        return mplew.getPacket();
    }

    public static byte[] GmHide(boolean ff) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GM_EFFECT.getValue());
        mplew.write(12);
        mplew.write(ff ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] testCombo(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ARAN_COMBO.getValue());
        mplew.writeInt(value);

        return mplew.getPacket();
    }

    public static byte[] rechargeCombo(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ARAN_COMBO_RECHARGE.getValue());
        mplew.writeInt(value);

        return mplew.getPacket();
    }

    public static byte[] getPacketFromHexString(String hex) {
        return HexTool.getByteArrayFromHexString(hex);
    }

    public static class GainExpPacket {

        public static final byte[] GainExp_Monster(int gain, boolean white, int partyinc, int eventinc, int eventmob, int weddinginc, int equipinc, int time, int 피시보너스) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(3);
            mplew.write(white ? 1 : 0); //white or yellow
            mplew.writeInt(gain); //경험치를 얻었습니다
            mplew.write(0); //경험치 표시 영역 설정
            mplew.writeInt(eventinc); //이벤트 보너스 경험치
            mplew.write(eventmob); //3번째 몬스터를 잡을 때마다 보너스 경험치 x%가 지급됩니다.
            mplew.write(0); //파티 보너스 경험치
            mplew.writeInt(weddinginc); //결혼 보너스
            if (eventmob > 0) {
                mplew.write(time); //x시간이상 사냥 보너스 경험치
            }
            mplew.write(0); //이벤트 파티 보너스 경험치 배율. 100 넣으면 2배 됨
            mplew.writeInt(partyinc); //파티 보너스 경험치 (겉값남) v33 왜 겉값이 나는걸까?
            mplew.writeInt(equipinc);  //아이템 장착 보너스 경험치
            mplew.writeInt(피시보너스); //pc방
            mplew.writeInt(0);  // 레인보우 위크
            return mplew.getPacket();
        }

        public static final byte[] GainExp_Quest(int gain) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(3);
            mplew.write(1);
            mplew.writeInt(gain);
            mplew.write(1);
            mplew.writeZeroBytes(36);
            return mplew.getPacket();
        }

        public static final byte[] GainExp_QuestEvent(int gain, int questbonus, int questnum) {
            MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
            mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.write(3);
            mplew.write(1);
            mplew.writeInt(gain);
            mplew.write(1);
            mplew.writeZeroBytes(5);
            mplew.write(questbonus);
            mplew.write(questnum);
            mplew.writeZeroBytes(10);
            return mplew.getPacket();
        }
    }

    public static final byte[] getShowFameGain(final int gain) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(5);
        mplew.writeInt(gain);

        return mplew.getPacket();
    }

    public static final byte[] getShowGPGain(final int gain) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(6);
        mplew.writeInt(gain);

        return mplew.getPacket();
    }

    /*
     * 4: 인기도
     * 5: 메소
     * 6: 길드포인트
     *
     */
    public static final byte[] showMesoGain(final int gain, final boolean inChat) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            mplew.write(0);
            mplew.write(1);
            mplew.write(0);
            mplew.writeInt(gain);
            mplew.writeShort(0); // inet cafe meso gain ?.o
        } else {
            mplew.write(6);
            mplew.writeInt(gain);
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }

    public static byte[] getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    public static byte[] getShowItemGain(int itemId, short quantity, boolean inChat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (inChat) {
            mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
            mplew.write(5);
            mplew.write(1); // item count
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
            /*	    for (int i = 0; i < count; i++) { // if ItemCount is handled.
             mplew.writeInt(itemId);
             mplew.writeInt(quantity);
             }*/
        } else {
            mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(0);
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        }
        return mplew.getPacket();
    }

    public static byte[] showRewardItemAnimation(int itemId, String effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(16);
        mplew.writeInt(itemId);
        mplew.write(effect != null && effect.length() > 0 ? 1 : 0);
        if (effect != null && effect.length() > 0) {
            mplew.writeMapleAsciiString(effect);
        }

        return mplew.getPacket();
    }

    public static byte[] showRewardItemAnimation(int itemId, String effect, int from_playerid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(from_playerid);
        mplew.write(16);
        mplew.writeInt(itemId);
        mplew.write(effect != null && effect.length() > 0 ? 1 : 0);
        if (effect != null && effect.length() > 0) {
            mplew.writeMapleAsciiString(effect);
        }

        return mplew.getPacket();
    }

    public static byte[] dropItemFromMapObject(MapleMapItem drop, Point dropfrom, Point dropto, byte mod) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(mod); // 1 animation, 2 no animation, 3 spawn disappearing item [Fade], 4 spawn disappearing item
        mplew.writeInt(drop.getObjectId()); // item owner id
        mplew.write(drop.getMeso() > 0 ? 1 : 0); // 1 mesos, 0 item, 2 and above all item meso bag,
        mplew.writeInt(drop.getItemId()); // drop object ID
        mplew.writeInt(drop.getOwner()); // owner charid
        mplew.write(drop.getDropType()); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
        mplew.writePos(dropto);
        mplew.writeInt(0);
        if (mod == 0 || mod == 1 || mod == 3) {
            mplew.writePos(dropfrom);
            mplew.writeShort(0);
        }
        if (drop.getMeso() == 0) {
            PacketHelper.addExpirationTime(mplew, drop.getItem().getExpiration());
        }
        mplew.writeShort(drop.isPlayerDrop() ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] dropItemFromMonster(MapleMapItem drop, Point dropfrom, int mid, Point dropto, byte mod) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(mod); // 1 animation, 2 no animation, 3 spawn disappearing item [Fade], 4 spawn disappearing item
        mplew.writeInt(drop.getObjectId()); // item owner id
        mplew.write(drop.getMeso() > 0 ? 1 : 0); // 1 mesos, 0 item, 2 and above all item meso bag,
        mplew.writeInt(drop.getItemId()); // drop object ID
        mplew.writeInt(drop.getOwner()); // owner charid
        mplew.write(drop.getDropType()); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
        if (mid == 9400273) {
            mplew.writeShort(dropto.x - 500);
            mplew.writeShort(dropto.y - 100);
        } else {
            mplew.writePos(dropto);
        }
        mplew.writeInt(0);

        if (mod == 0 || mod == 1 || mod == 3) {
            mplew.writePos(dropfrom);
            mplew.writeShort(0); //delay
        }
        if (drop.getMeso() == 0) {
            PacketHelper.addExpirationTime(mplew, drop.getItem().getExpiration());
        }
        mplew.writeShort(drop.isPlayerDrop() ? 0 : 1); // pet EQP pickup

        return mplew.getPacket();
    }
    public static int DEFAULT_BUFFMASK = 0; //???CONFIRM

    static {
        DEFAULT_BUFFMASK |= MapleBuffStat.ENERGY_CHARGE.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.DASH_SPEED.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.DASH_JUMP.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.MONSTER_RIDING.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.SPEED_INFUSION.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.HOMING_BEACON.getValue();
        DEFAULT_BUFFMASK |= MapleBuffStat.DEFAULT_BUFFSTAT.getValue();
    }

    public static byte[] spawnPlayerMapobject(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeMapleAsciiString(chr.getName());
        if (chr.getGuildId() <= 0) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        final List<Pair<Integer, Integer>> buffvalue = new ArrayList<Pair<Integer, Integer>>();
        final int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        mask[0] |= DEFAULT_BUFFMASK;
        //NOT SURE: SPARK
        if (chr.getBuffedValue(MapleBuffStat.SPEED) != null) {//CTS_Speed 완료
            mask[mask.length - 1] |= MapleBuffStat.SPEED.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.SPEED).intValue()), 1));
        }
        if (chr.getBuffedValue(MapleBuffStat.COMBO) != null) {//CTS_ComboCounter 완료
            mask[mask.length - 1] |= MapleBuffStat.COMBO.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.COMBO).intValue()), 1));
        }
        /*if (chr.getBuffedValue(MapleBuffStat.WK_CHARGE) != null) {//CTS_WeaponCharge wk차지는 어디서 ??
         mask[mask.length - 1] |= MapleBuffStat.WK_CHARGE.getValue();
         buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.WK_CHARGE).intValue()), 2));
         buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffSource(MapleBuffStat.WK_CHARGE)), 3));
         }*/
 /*
         * 스턴
         * 암흑
         * 봉인
         * 허약
         * 저주
         * 중독
         * 중독
         */
 /*if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {//CTS_ShadowPartner
         mask[mask.length - 1] |= MapleBuffStat.SHADOWPARTNER.getValue();
         buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER).intValue()), 2));
         buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffSource(MapleBuffStat.SHADOWPARTNER)), 3));
         }*/
        if (chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null) {//CTS_ShadowPartner 완료
            mask[mask.length - 1] |= MapleBuffStat.SHADOWPARTNER.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.DARKSIGHT) != null || chr.isHidden()) {//CTS_DarkSight
            mask[mask.length - 1] |= MapleBuffStat.DARKSIGHT.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.SOULARROW) != null) {//CTS_SoulArrow 완료
            mask[mask.length - 1] |= MapleBuffStat.SOULARROW.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.STANCE) != null) {//CTS_SoulArrow 완료
            mask[mask.length - 1] |= MapleBuffStat.STANCE.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {//CTS_Morph 완료
            mask[mask.length - 2] |= MapleBuffStat.MORPH.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getStatForBuff(MapleBuffStat.MORPH).getMorph(chr)), 2));
        }
        if (chr.getBuffedValue(MapleBuffStat.DIVINE_BODY) != null) {//테스트해봐야함
            mask[mask.length - 2] |= MapleBuffStat.DIVINE_BODY.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.BERSERK_FURY) != null) {//테스트해봐야함
            mask[mask.length - 2] |= MapleBuffStat.BERSERK_FURY.getValue();
        }
        //---------------------------------------------------------------
        if (chr.getBuffedValue(MapleBuffStat.WIND_WALK) != null) {//완료
            mask[mask.length - 3] |= MapleBuffStat.WIND_WALK.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.MAGIC_SHIELD) != null) {//매직실드
            mask[mask.length - MapleBuffStat.MAGIC_SHIELD.getPosition()] |= MapleBuffStat.MAGIC_SHIELD.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffSource(MapleBuffStat.MAGIC_SHIELD)), 3));
        }
        //---------------------------------------------------------------
        if (chr.getBuffedValue(MapleBuffStat.TORNADO) != null) {//CTS_Cyclone 완료 데미가 안나옴
            mask[mask.length - 4] |= MapleBuffStat.TORNADO.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getBuffedValue(MapleBuffStat.TORNADO).intValue()), 1));
        }
        if (chr.getBuffedValue(MapleBuffStat.INFILTRATE) != null) {//완료
            mask[mask.length - 4] |= MapleBuffStat.INFILTRATE.getValue();
        }
        if (chr.getBuffedValue(MapleBuffStat.MECH_CHANGE) != null) {//CTS_Mechanic 완료
            mask[mask.length - 4] |= MapleBuffStat.MECH_CHANGE.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.MECH_CHANGE)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.DARK_AURA) != null) {//CTS_DarkAura
            mask[mask.length - 4] |= MapleBuffStat.DARK_AURA.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.DARK_AURA)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.BLUE_AURA) != null) {//CTS_BlueAura
            mask[mask.length - 4] |= MapleBuffStat.BLUE_AURA.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.BLUE_AURA)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.YELLOW_AURA) != null) {//CTS_YellowAura
            mask[mask.length - 4] |= MapleBuffStat.YELLOW_AURA.getValue();
            buffvalue.add(new Pair<Integer, Integer>(Integer.valueOf(chr.getTrueBuffSource(MapleBuffStat.YELLOW_AURA)), 3));
        }
        if (chr.getBuffedValue(MapleBuffStat.DIVINE_SHIELD) != null) {//CTS_ShadowPartner
            mask[mask.length - MapleBuffStat.DIVINE_SHIELD.getPosition()] |= MapleBuffStat.DIVINE_SHIELD.getValue();
        }
        //---------------------------------------------------------------
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[i]);
        }
        for (Pair<Integer, Integer> i : buffvalue) {
            if (i.right == 3) {
                mplew.writeInt(i.left.intValue());
            } else if (i.right == 2) {
                mplew.writeShort(i.left.shortValue());
            } else if (i.right == 1) {
                mplew.write(i.left.byteValue());
            }
        }
        final int CHAR_MAGIC_SPAWN = Randomizer.nextInt();

        mplew.writeShort(0);//?먮꼫吏李⑥??
        mplew.writeLong(0);
        mplew.write(1);
        mplew.writeInt(CHAR_MAGIC_SPAWN); // ENERGY_CHARGE

        mplew.writeShort(0);//??ъ뒪?쇰뱶 ?ㅽ???
        mplew.writeLong(0);
        mplew.write(1);
        mplew.writeInt(CHAR_MAGIC_SPAWN); // DASH_SPEED 

        mplew.writeShort(0);//??ъ젏?꾩뒪???
        mplew.writeLong(0);
        mplew.write(1);
        mplew.writeInt(CHAR_MAGIC_SPAWN); // DASH_JUMP 

        mplew.writeShort(0);
        int buffSrc = chr.getBuffSource(MapleBuffStat.MONSTER_RIDING);
        if (buffSrc > 0) {
            final Item c_mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -118);
            final Item mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
            if (GameConstants.getMountItem(buffSrc, chr) == 0 && c_mount != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -119) != null) {
                mplew.writeInt(c_mount.getItemId());
            } else if (GameConstants.getMountItem(buffSrc, chr) == 0 && mount != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
                mplew.writeInt(mount.getItemId());
            } else {
                mplew.writeInt(GameConstants.getMountItem(buffSrc, chr));
            }
            mplew.writeInt(buffSrc);
        } else {
            mplew.writeLong(0);
        }
        mplew.write(1);
        mplew.writeInt(CHAR_MAGIC_SPAWN);//4
        mplew.writeLong(0); //speed infusion behaves differently here
        mplew.writeShort(0);
        mplew.write(1);
        mplew.writeInt(CHAR_MAGIC_SPAWN);//5
        mplew.writeInt(0);
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.write(1);
        mplew.writeInt(CHAR_MAGIC_SPAWN);//6
        mplew.writeLong(0);
        mplew.writeShort(0);
        mplew.write(1);
        mplew.writeInt(CHAR_MAGIC_SPAWN);//7
        mplew.write(0);
        mplew.write(0);

        mplew.writeShort(chr.getJob());
        PacketHelper.addCharLook(mplew, chr, true);
        mplew.writeInt(0);//this is CHARID to follow
        mplew.writeInt(0); //probably charid following
        mplew.writeInt(Math.min(250, chr.getInventory(MapleInventoryType.CASH).countById(5110000))); //max is like 100. but w/e
        mplew.writeInt(chr.getItemEffect());
        mplew.writeInt(0); //당신은 무엇?
        mplew.writeInt(GameConstants.getInventoryType(chr.getChair()) == MapleInventoryType.SETUP ? chr.getChair() : 0);
        mplew.writePos(chr.getTruePosition());
        mplew.write(chr.getStance());
        mplew.writeShort(chr.getFH()); // FH

        mplew.write(0); //펫
        mplew.writeInt(chr.getMount().getLevel()); // mount lvl
        mplew.writeInt(chr.getMount().getExp()); // exp
        mplew.writeInt(chr.getMount().getFatigue()); // tiredness
        PacketHelper.addAnnounceBox(mplew, chr);
        mplew.write(chr.getChalkboard() != null && chr.getChalkboard().length() > 0 ? 1 : 0);
        if (chr.getChalkboard() != null && chr.getChalkboard().length() > 0) {
            mplew.writeMapleAsciiString(chr.getChalkboard());
        }
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(mplew, rings.getLeft());//커플
        addRingInfo(mplew, rings.getMid());//우정
        addMRingInfo(mplew, rings.getRight(), chr);//매리지
        mplew.write(chr.getStat().Berserk ? 1 : 0);
        if (chr.getCarnivalParty() != null) {
            mplew.write(chr.getCarnivalParty().getTeam());
        } else if (GameConstants.isTeamMap(chr.getMapId())) {
            mplew.write(chr.getTeam()); //is it 0/1 or is it 1/2?
        } // 맞는지 모르겠음 팀맵
        return mplew.getPacket();
    }

    public static byte[] removePlayerFromMap(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] facialExpression(MapleCharacter from, int expression) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        mplew.writeInt(-1); //itemid of expression use
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] movePlayer(int cid, List<LifeMovementFragment> moves, Point startPos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.writePos(startPos);
        mplew.writeInt(0);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static byte[] moveSummon(int cid, int oid, int skillid, Point startPos, List<LifeMovementFragment> moves, LittleEndianAccessor slea) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MOVE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.writePos(startPos);
        mplew.writeInt(0);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static byte[] summonAttack(final int cid, final int summonSkillId, final byte animation, final List<Pair<Integer, Integer>> allDamage, final int level, final boolean darkFlare) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SUMMON_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(level - 1); //? guess
        mplew.write(animation);
        if (allDamage == null) {
            mplew.write(0);
        } else {
            mplew.write(allDamage.size());
        }

        for (final Pair<Integer, Integer> attackEntry : allDamage) {
            mplew.writeInt(attackEntry.left); // oid
            mplew.write(7); // who knows
            mplew.writeInt(attackEntry.right); // damage
            //  System.out.println("cid" + cid + "skillid" + summonSkillId + "animation" + animation + "oid" + attackEntry.left + "damage" + attackEntry.right);
        }
        mplew.write(darkFlare ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] summonAttackSpecial(int cid, int summonSkillId, byte animation, List<Pair<Integer, Integer>> allDamage, int level, byte tbyte, boolean darkFlare) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SUMMON_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(level);
        mplew.write(animation);
        mplew.write(tbyte);
        for (Pair attackEntry : allDamage) {
            mplew.writeInt(((Integer) attackEntry.left));
            mplew.write(7);
            mplew.writeInt(((Integer) attackEntry.right));
        }
        mplew.write(darkFlare ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] showAttack(int cid, int tbyte, int lvl, int skill, int level, byte ultLevel, int display, byte speed, List<AttackPair> damage, final SendPacketOpcode send, byte mastery, byte unk, int charge, int cashbullet, final Point pos, int mobid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(send.getValue());
        mplew.writeInt(cid);
        mplew.write(tbyte);
        mplew.write(lvl); //?
        if (skill == 0) {
            mplew.write(0);
        } else {
            mplew.write(level);
            mplew.writeInt(skill);
        }
        if (skill == 3211006) {
            mplew.write(level);
            if (level > 0) {
                mplew.writeInt(3220010);
            }
        }
        mplew.write(unk);
        mplew.writeShort(display);
        mplew.write(speed);
        mplew.write(mastery * 5);//임시처리
        mplew.writeInt(cashbullet);  // BulletCashItem

        for (AttackPair oned : damage) {
            if (oned.attack != null) {
                mplew.writeInt(oned.objectid);
                mplew.write(0x07);
                if (skill == 4211006) {
                    mplew.write(oned.attack.size());
                }
                for (Pair<Integer, Boolean> eachd : oned.attack) {
                    if (eachd.right) {
                        mplew.writeInt(eachd.left.intValue() | 0x80000000);
                    } else {
                        mplew.writeInt(eachd.left.intValue());
                    }
                }
            }
        }
        if (send == SendPacketOpcode.RANGED_ATTACK) {
            mplew.writePos(pos); // Position
        }
        if (charge > 0) {
            mplew.writeInt(charge); //is it supposed to be here
        } else if (skill == 33101007) {//스왈로우퉤!
            mplew.writeInt(mobid);//먹는몹아이디 팅패킷으로 변질 가능 조심
        }

        return mplew.getPacket();
    }

    public static byte[] getNPCShop(int sid, MapleShop shop, MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.OPEN_NPC_SHOP.getValue());
        //mplew.writeInt(0);
        mplew.writeInt(sid);
        PacketHelper.addShopInfo(mplew, shop, c);
        return mplew.getPacket();
    }

    public static byte[] confirmShopTransaction(byte code/*, MapleShop shop, MapleClient c, int indexBought*/) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CONFIRM_SHOP_TRANSACTION.getValue());
        mplew.write(code); // 8 = sell, 0 = buy, 0x20 = due to an error
//        if (code == 4) {
//            mplew.writeInt(shop.getNpcId()); //oops
//            PacketHelper.addShopInfo(mplew, shop, c);
//        } else {
//            mplew.write(indexBought >= 0 ? 1 : 0);
//            if (indexBought >= 0) {
//                mplew.writeInt(indexBought);
//            }
//        }
        return mplew.getPacket();
    }

    public static byte[] addInventorySlot(MapleInventoryType type, Item item) {
        return addInventorySlot(type, item, false);
    }

    public static byte[] addInventorySlot(MapleInventoryType type, Item item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(1); // how many items to add
        mplew.write(0);
        mplew.write(type.getType()); // iv type
        mplew.write(item.getPosition()); // slot id 1.2.6 바이트
        PacketHelper.addItemInfo(mplew, item, true, false);
        return mplew.getPacket();
    }

    public static byte[] updateInventorySlot(MapleInventoryType type, Item item, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(1); //how many items to update
        mplew.write(1); //bag
        mplew.write(type.getType()); // iv type
        mplew.writeShort(item.getPosition()); // slot id
        mplew.writeShort(item.getQuantity());
        return mplew.getPacket();
    }

    public static byte[] moveInventoryItem(MapleInventoryType type, short src, short dst) {
        return moveInventoryItem(type, src, dst, (byte) -1);
    }

    public static byte[] moveInventoryItem(MapleInventoryType type, short src, short dst, short equipIndicator) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01"));
        mplew.write(2);
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(dst);
        if (equipIndicator != -1) {
            mplew.writeShort(equipIndicator);
        }
        return mplew.getPacket();
    }

    public static byte[] moveAndMergeInventoryItem(MapleInventoryType type, short src, short dst, short total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02"));
        mplew.write(3);
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.write(1); // merge mode?
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(total);

        return mplew.getPacket();
    }

    public static byte[] moveAndMergeWithRestInventoryItem(MapleInventoryType type, short src, short dst, short srcQ, short dstQ) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 02"));
        mplew.write(1);
        mplew.write(type.getType());
        mplew.writeShort(src);
        mplew.writeShort(srcQ);
        mplew.write(1);
        mplew.write(type.getType());
        mplew.writeShort(dst);
        mplew.writeShort(dstQ);

        return mplew.getPacket();
    }

    public static byte[] clearInventoryItem(MapleInventoryType type, short slot, boolean fromDrop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(fromDrop ? 1 : 0);
        mplew.write(1);
        mplew.write(3); //bag
        mplew.write(type.getType());
        mplew.writeShort(slot);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] updateSpecialItemUse(Item item, byte invType, MapleCharacter chr) {
        return updateSpecialItemUse(item, invType, item.getPosition(), false, chr);
    }

    public static byte[] updateSpecialItemUse(Item item, byte invType, short pos, boolean theShort, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0); // could be from drop
        mplew.write(2); // always 2
        //clears the slot and puts item in same slot in one packet
        mplew.write(3); // quantity > 0 (?)
        mplew.write(invType); // Inventory type
        mplew.writeShort(pos); // item slot
        mplew.write(0);
        mplew.write(invType);
        if (item.getType() == 1 || theShort) {
            mplew.writeShort(pos);
        } else {
            mplew.write(pos);
        }
        PacketHelper.addItemInfo(mplew, item, true, true, false, false, chr);
        if (pos < 0) {
            mplew.write(2); //?
        }

        return mplew.getPacket();
    }

    public static byte[] updateSpecialItemUse_(Item item, byte invType, MapleCharacter chr, boolean cash) {
        return updateSpecialItemUse_(item, invType, item.getPosition(), chr, cash);
    }

    public static byte[] updateSpecialItemUse_(Item item, byte invType, short pos, MapleCharacter chr, boolean cash) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0); // could be from drop
        mplew.write(1); // always 2
        mplew.write(0); // quantity > 0 (?)
        mplew.write(invType); // Inventory type
        if (item.getType() == 1) {
            mplew.writeShort(pos);
        } else {
            mplew.write(pos);
        }
        if (cash) {
            PacketHelper.addItemInfo(mplew, item, true, false, false, false, chr);
        } else {
            PacketHelper.addItemInfo(mplew, item, true, true, false, false, chr);
        }
        if (pos < 0) {
            mplew.write(1); //?
        }

        return mplew.getPacket();
    }

    public static byte[] scrolledItem(Item scroll, Item item, boolean destroyed, boolean potential) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1); // fromdrop always true
        mplew.write(destroyed ? 2 : 3);
        mplew.write(scroll.getQuantity() > 0 ? 1 : 3);
        mplew.write(GameConstants.getInventoryType(scroll.getItemId()).getType()); //can be cash
        mplew.writeShort(scroll.getPosition());

        if (scroll.getQuantity() > 0) {
            mplew.writeShort(scroll.getQuantity());
        }
        mplew.write(3);
        if (!destroyed) {
            mplew.write(MapleInventoryType.EQUIP.getType());
            mplew.writeShort(item.getPosition());
            mplew.write(0);
        }
        mplew.write(MapleInventoryType.EQUIP.getType());
        mplew.writeShort(item.getPosition());
        if (!destroyed) {
            PacketHelper.addItemInfo(mplew, item, true, true);
        }
        if (!potential) {
            mplew.write(1);
        } else {
            mplew.write(11);
        }

        return mplew.getPacket();
    }

    public static byte[] moveAndUpgradeItem(MapleInventoryType type, Item item, short oldpos, short newpos, MapleCharacter chr) {//equipping some items  
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1); //fromdrop
        mplew.write(3);
        mplew.write(3);
        mplew.write(type.getType());
        mplew.writeShort(oldpos);
        mplew.write(0);
        mplew.write(1);
        mplew.writeShort(oldpos);
        PacketHelper.addItemInfo(mplew, item, true, true, false, false, chr);
        mplew.write(2);
        mplew.write(type.getType());
        mplew.writeShort(oldpos);//oldslot
        mplew.writeShort(newpos);//new slot
        mplew.write(0);//?
        return mplew.getPacket();
    }

    public static byte[] getScrollEffect(int chr, ScrollResult scrollSuccess, boolean legendarySpirit, boolean whiteScroll) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chr);
        mplew.write(scrollSuccess == ScrollResult.SUCCESS ? 1 : 0);
        mplew.write(scrollSuccess == ScrollResult.CURSE ? 1 : 0);
        mplew.write(legendarySpirit ? 1 : 0);
        mplew.write(whiteScroll ? 1 : 0);
        //    mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] getScrollEffect2(int chr, ScrollResult scrollSuccess, boolean legendarySpirit, boolean whiteScroll) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(6);
        mplew.write(scrollSuccess == ScrollResult.SUCCESS ? 1 : 0);
        return mplew.getPacket();
    }

    //miracle cube?
    public static byte[] getPotentialEffect(final int chr, final int full) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_POTENTIAL_EFFECT.getValue());
        mplew.writeInt(chr);
        mplew.write(full);
        //mplew.writeInt(itemid);
        return mplew.getPacket();
    }

    //magnify glass
    public static byte[] getPotentialReset(final int chr, final short pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_POTENTIAL_RESET.getValue());
        mplew.writeInt(chr);
        mplew.writeShort(pos);
        return mplew.getPacket();
    }

    public static byte[] ItemMaker_Success(boolean success) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//D6 00 00 00 00 00 01 00 00 00 00 DC DD 40 00 01 00 00 00 01 00 00 00 8A 1C 3D 00 01 00 00 00 00 00 00 00 00 B0 AD 01 00
        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(18); //bb +2
        mplew.writeInt(success ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] ItemMaker_Success_3rdParty(final int from_playerid, boolean success) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(from_playerid);
        mplew.write(18);
        mplew.writeInt(success ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] Maker(boolean success, int item, int count) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MAKER.getValue());
        mplew.writeInt(success ? 0 : 1);
        mplew.writeInt(1);
        mplew.write(success ? 0 : 1);
        if (success) {
            mplew.writeInt(item);
            mplew.writeInt(count);
        }
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] Maker(int crystal, int etc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MAKER.getValue());
        mplew.writeInt(0);
        mplew.writeInt(3);
        mplew.writeInt(crystal);
        mplew.writeInt(etc);
        return mplew.getPacket();
    }

    public static byte[] Maker(int item) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MAKER.getValue());
        mplew.writeInt(0);
        mplew.writeInt(4);
        mplew.writeInt(item);
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] explodeDrop(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(4); // 4 = Explode
        mplew.writeInt(oid);
        mplew.writeShort(655);

        return mplew.getPacket();
    }

    public static byte[] removeItemFromMap(int oid, int animation, int cid) {
        return removeItemFromMap(oid, animation, cid, 0);
    }

    public static byte[] removeItemFromMap(int oid, int animation, int cid, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation); // 0 = Expire, 1 = without animation, 2 = pickup, 4 = explode, 5 = pet pickup
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(cid);
            if (animation == 5) { // allow pet pickup?
                mplew.writeInt(slot);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] updateCharLook(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        PacketHelper.addCharLook(mplew, chr, false);
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(mplew, rings.getLeft());
        addRingInfo(mplew, rings.getMid());
        addMRingInfo(mplew, rings.getRight(), chr);
        if (chr.hasEquipped(1000040) && chr.hasEquipped(1050169) && chr.hasEquipped(1102246) && chr.hasEquipped(1082276) && chr.hasEquipped(1072447)
                || chr.hasEquipped(1001060) && chr.hasEquipped(1051210) && chr.hasEquipped(1102246) && chr.hasEquipped(1082276) && chr.hasEquipped(1072447)) { //코딩 수준......ㅠ
            mplew.writeInt(1); //세트아이템 이펙트
        } else {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static void addRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            //mplew.writeInt(1);
            mplew.writeLong(ring.getRingId());
            mplew.writeLong(ring.getPartnerRingId());
            mplew.writeInt(ring.getItemId());
        }
    }

    public static void addMRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings, MapleCharacter chr) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            //mplew.writeInt(1);
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeInt(ring.getItemId());
        }
    }

    public static byte[] dropInventoryItem(MapleInventoryType type, short src) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 03"));
        mplew.write(type.getType());
        mplew.writeShort(src);
        if (src < 0) {
            mplew.write(1);
        }
        return mplew.getPacket();
    }

    public static byte[] dropInventoryItemUpdate(MapleInventoryType type, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01"));
        mplew.write(type.getType());
        mplew.writeShort(item.getPosition());
        mplew.writeShort(item.getQuantity());

        return mplew.getPacket();
    }

    public static byte[] damageTest() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeInt(0); //cid
        mplew.write(0); //?
        mplew.writeInt(99999); //damage

        return mplew.getPacket();
    }

    public static byte[] damagePlayer(int skill, int monsteridfrom, int cid, int rdamage, int damage, int fake, byte direction, int reflect, boolean is_pg, int oid, int pos_x, int pos_y, int offset) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(cid);
        mplew.write(skill);
        mplew.writeInt(rdamage);
        if (skill > -2) {
            mplew.writeInt(monsteridfrom);
            mplew.write(direction);
            if (reflect > 0) {
                mplew.write(reflect);
                mplew.write(is_pg ? 1 : 0);
                mplew.writeInt(oid);
                mplew.write(6); //s2
                mplew.writeShort(pos_x);
                mplew.writeShort(pos_y);
                mplew.write(0);
            } else {
                mplew.writeShort(0);
            }
            mplew.write(offset);
        }
        mplew.writeInt(damage);
        if (damage == -1) {
            mplew.writeInt(0);//이게 페이크인가?
        }
        if (fake > 0) {
            mplew.writeInt(fake);
        }
        return mplew.getPacket();
    }

    public static byte[] playerDamage(int cid, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_DAMAGE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(damage);

        return mplew.getPacket();
    }

    public static final byte[] updateQuest(final MapleQuestStatus quest) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(quest.getQuest().getId());
        mplew.write(quest.getStatus());
        switch (quest.getStatus()) {
            case 0:
                mplew.writeZeroBytes(10);
                break;
            case 1:
                if (quest.getCustomData() != null) {
                    if (quest.getCustomData().startsWith("time_")) {
                        mplew.writeShort(9);
                        mplew.write(1);
                        mplew.writeLong(PacketHelper.getTime(Long.parseLong(quest.getCustomData().substring(5))));
                    } else {
                        mplew.writeMapleAsciiString(quest.getCustomData());
                    }
                } else {
                    mplew.writeZeroBytes(2);
                }
                break;
            case 2:
                mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
                mplew.writeShort(0);
                break;
        }

        return mplew.getPacket();
    }

    public static final byte[] updateInfoQuest(final int quest, final String data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x0B); //AFTERSHOCK: 0x0C
        mplew.writeShort(quest);
        mplew.writeMapleAsciiString(data);

        return mplew.getPacket();
    }

    public static final byte[] updateDiligentExplorer(int todaysecs, int totaldays) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(22);
        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x0B); //AFTERSHOCK: 0x0C
        mplew.writeShort(29003);
        mplew.writeShort(0x15);
        mplew.writeAsciiString("PTR=");
        mplew.write(1);
        mplew.write(1);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.write(1);
        mplew.write(0x12);
        mplew.write(0xCD);
        mplew.writeShort(todaysecs); // today secs
        mplew.write(totaldays);      // total days
        mplew.write(0xCD);
        return mplew.getPacket();
    }

    public static final byte[] updateInfoQuest(final int quest, final byte[] byts) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x0A); //AFTERSHOCK: 0x0C
        mplew.writeShort(quest);
        mplew.writeShort(byts.length);
        mplew.write(byts);

        return mplew.getPacket();
    }

    public static byte[] updateQuestInfo(MapleCharacter c, int quest, int npc, byte progress) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(progress);
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] updateQuestFinish(int quest, int npc, int nextquest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(10); //bb - 10
        mplew.writeShort(quest);
        mplew.writeInt(npc);
        mplew.writeInt(nextquest);

        return mplew.getPacket();
    }

    public static byte[] addQuestTimeLimit(final int quest, final int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(6);
        mplew.writeShort(1);
        mplew.writeShort(quest);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] removeQuestTimeLimit(final int quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(7);
        mplew.writeShort(1);
        mplew.writeShort(quest);

        return mplew.getPacket();
    }

    public static byte[] questExpire(int quest) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(17);
        mplew.writeShort(quest);

        return mplew.getPacket();
    }

    public static final byte[] charInfo(final MapleCharacter chr, final boolean isSelf) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CHAR_INFO.getValue());
        mplew.writeInt(chr.getId());//확실
        mplew.write(chr.getLevel());//확실
        mplew.writeShort(chr.getJob());//확실
        mplew.writeShort(chr.getFame());//확실
        mplew.write(chr.getMarriageId() > 0 ? 1 : 0);//확실
        if (chr.getGuildId() <= 0) {
            mplew.writeMapleAsciiString("-");
            mplew.writeMapleAsciiString("");
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                if (gs.getAllianceId() > 0) {
                    final MapleGuildAlliance allianceName = World.Alliance.getAlliance(gs.getAllianceId());
                    if (allianceName != null) {
                        mplew.writeMapleAsciiString(allianceName.getName());
                    } else {
                        mplew.writeMapleAsciiString("");
                    }
                } else {
                    mplew.writeMapleAsciiString("");
                }
            } else {
                mplew.writeMapleAsciiString("-");
                mplew.writeMapleAsciiString("");
            }
        }
        mplew.write(0); //강제로 펫 넘버 열기
        mplew.write(chr.getPet(0) != null ? 1 : 0);
        for (final MaplePet pet : chr.getPets()) {
            if (pet.getSummoned()) {
                mplew.write(1);
                mplew.writeInt(chr.getPetIndex(pet));
                mplew.writeInt(pet.getPetItemId()); // petid
                mplew.writeMapleAsciiString(pet.getName());
                mplew.write(pet.getLevel()); // pet level
                mplew.writeShort(pet.getCloseness()); // pet closeness
                mplew.write(pet.getFullness()); // pet fullness
                mplew.writeShort(pet.getFlags());
                final Item inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (chr.getPetIndex(pet) == 0 ? -114 : (chr.getPetIndex(pet) == 1 ? -124 : -125)));
                mplew.writeInt(inv == null ? 0 : inv.getItemId());
            }
        }
        mplew.write(0);
        if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
            final MapleMount mount = chr.getMount();
            mplew.write(1);
            mplew.writeInt(mount.getLevel());
            mplew.writeInt(mount.getExp());
            mplew.writeInt(mount.getFatigue());
        } else {
            mplew.write(0);
        }

        final int wishlistSize = chr.getWishlistSize();
        mplew.write(wishlistSize);
        if (wishlistSize > 0) {
            final int[] wishlist = chr.getWishlist();
            for (int x = 0; x < wishlistSize; x++) {
                mplew.writeInt(wishlist[x]);
            }
        }
        Item medal = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -21);
        mplew.writeInt(medal == null ? 0 : medal.getItemId());
        List<Pair<Integer, Long>> medalQuests = chr.getCompletedMedals();
        mplew.writeShort(medalQuests.size());
        for (Pair<Integer, Long> x : medalQuests) {
            mplew.writeShort(x.left);
        }

        return mplew.getPacket();
    }

    public static byte[] updateMount(MapleCharacter chr, boolean levelup) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getMount().getLevel());
        mplew.writeInt(chr.getMount().getExp());
        mplew.writeInt(chr.getMount().getFatigue());
        mplew.write(levelup ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] mountInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(chr.getMount().getLevel());
        mplew.writeInt(chr.getMount().getExp());
        mplew.writeInt(chr.getMount().getFatigue());

        return mplew.getPacket();
    }

    public static byte[] getTradeInvite(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(2);
        mplew.write(3);
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeInt(0); // Trade ID

        return mplew.getPacket();
    }

    public static byte[] getCashTradeInvite(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(2);
        mplew.write(6);
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeInt(0); // Trade ID

        return mplew.getPacket();
    }

    public static byte[] getTradeMesoSet(byte number, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xF);
        mplew.write(number);
        mplew.writeInt(meso);

        return mplew.getPacket();
    }

    public static byte[] getTradeItemAdd(byte number, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xE);
        mplew.write(number);
        PacketHelper.addItemInfo(mplew, item, false, false, true);

        return mplew.getPacket();
    }

    public static byte[] getTradeStart(MapleClient c, MapleTrade trade, byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(5);
        mplew.write(3);
        mplew.write(2);
        mplew.write(number);

        if (number == 1) {
            mplew.write(0);
            PacketHelper.addCharLook(mplew, trade.getPartner().getChr(), false);
            mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
            mplew.writeShort(trade.getPartner().getChr().getJob());
        }
        mplew.write(number);
        PacketHelper.addCharLook(mplew, c.getPlayer(), false);
        mplew.writeMapleAsciiString(c.getPlayer().getName());
        mplew.writeShort(c.getPlayer().getJob());
        mplew.write(0xFF);

        return mplew.getPacket();
    }

    public static byte[] getCashTradeStart(MapleClient c, MapleTrade trade, byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(5);
        mplew.write(6);
        mplew.write(1);//여긴가
        mplew.write(number);

        if (number == 1) {
            mplew.write(0);
            PacketHelper.addCharLook(mplew, trade.getPartner().getChr(), false);
            mplew.writeMapleAsciiString(trade.getPartner().getChr().getName());
            mplew.writeShort(trade.getPartner().getChr().getJob());
        }
        mplew.write(number);
        PacketHelper.addCharLook(mplew, c.getPlayer(), false);
        mplew.writeMapleAsciiString(c.getPlayer().getName());
        mplew.writeShort(c.getPlayer().getJob());
        mplew.write(0xFF);

        return mplew.getPacket();
    }

    public static byte[] getTradeConfirmation() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0x10); //버튼 눌렀을때 부분 멘트는 바로 밑 패킷

        return mplew.getPacket();
    }

    public static byte[] TradeMessage(final byte UserSlot, final byte message) {//교환 완료 메세지
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(UserSlot);
        mplew.write(message);
        //0x02 = cancelled
        //0x07 = success [tax is automated] 세금 지혼자 계산해줌
        //0x08 = unsuccessful
        //0x09 = "You cannot make the trade because there are some items which you cannot carry more than one." //아이템 체크 지혼자 해줌
        //0x0A = "You cannot make the trade because the other person's on a different map."

        return mplew.getPacket();
    }

    public static byte[] getTradeCancel(final byte UserSlot, final int unsuccessful) { //0 = canceled 1 = invent space 2 = pickuprestricted
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(0xA);
        mplew.write(UserSlot);
        mplew.write(unsuccessful == 0 ? 2 : (unsuccessful == 1 ? 8 : 9));

        return mplew.getPacket();
    }

    public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type) {
        return getNPCTalk(npc, msgType, talk, endBytes, type, npc);
    }

    public static byte[] getNPCTalk(int npc, byte msgType, String talk, String endBytes, byte type, int diffNPC) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.write(msgType);
        mplew.write(type); // mask; 1 = no ESC, 2 = playerspeaks, 4 = diff NPC 8 = something, ty KDMS
        if ((type & 0x4) != 0) {
            mplew.writeInt(diffNPC);
        }
        mplew.writeMapleAsciiString(talk);
        mplew.write(HexTool.getByteArrayFromHexString(endBytes));

        return mplew.getPacket();
    }

    public static final byte[] getMapSelection(int npcid, final String sel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npcid);
        mplew.writeShort(14);
        mplew.writeInt((npcid == 2083006 || npcid == 9900002) ? 1 : 0); //neo city
        mplew.writeInt((npcid == 9010022) ? 1 : 0); //dimensional
        mplew.writeMapleAsciiString(sel);

        return mplew.getPacket();
    }

    public static byte[] getNPCTalkStyle(int npc, String talk, int... args) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.writeShort(8);
        mplew.writeMapleAsciiString(talk);
        mplew.write(args.length);

        for (int i = 0; i < args.length; i++) {
            mplew.writeInt(args[i]);
        }
        return mplew.getPacket();
    }

    public static byte[] getNPCTalkNum(int npc, String talk, int def, int min, int max) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.writeShort(4);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(def);
        mplew.writeInt(min);
        mplew.writeInt(max);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getNPCTalkText(int npc, String talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.writeShort(3);
        mplew.writeMapleAsciiString(talk);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getINITIALQuiz(int npc, String talk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.NPC_TALK.getValue());
        mplew.write(4);
        mplew.writeInt(npc);
        mplew.write(5);
        mplew.write(0);
        mplew.writeMapleAsciiString("초성퀴즈");//주제 인가
        mplew.writeMapleAsciiString(talk);//문제
        mplew.writeMapleAsciiString(talk);//힌트
        mplew.writeInt(2);//x2해서 이상
        mplew.writeInt(4);//x2해서 이하
        mplew.writeInt(10);//시간x1000 해버리넹
        //시간 다되면 [NPC_TALK_MORE] 25 05 00 00 이거들어옴
        //맞추면 [NPC_TALK_MORE] 25 05 04 00 64 64 64 64
        //cm.senQuiz("dd", 2041024);사용법
        return mplew.getPacket();
    }

    public static byte[] showForeignEffect(int cid, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effect); // 0 = Level up, 8 = job change

        return mplew.getPacket();
    }

    public static byte[] showBuffeffect(int cid, int skillid, int effectid, int playerLevel, int skillLevel) {
        return showBuffeffect(cid, skillid, effectid, playerLevel, skillLevel, (byte) 3);
    }

    public static byte[] showBuffeffect(int cid, int skillid, int effectid, int playerLevel, int skillLevel, byte direction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effectid); //ehh?
        mplew.writeInt(skillid);
        mplew.write(playerLevel - 1); //player level
        mplew.write(skillLevel); //skill level
        if (direction != (byte) 3 || skillid == 30001062) {
            mplew.write(direction);
        }
        if (skillid == 30001062) { // 헌터의 부름
            mplew.writeShort(0); // x [헌터의 부름으로 나오는 몬스터 x좌표]
            mplew.writeShort(0); // y [헌터의 부름으로 나오는 몬스터 y좌표]
        }
        return mplew.getPacket();
    }

    public static byte[] showBuffeffect2(int cid, int skillid, Point a1, int a2, int a3, int a4) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(7); //ehh?
        mplew.writeInt(skillid);
        mplew.writePos(a1);
        mplew.writeInt(a2);
        mplew.writeInt(a3);
        mplew.writeInt(a4);
        return mplew.getPacket();
    }

    public static byte[] showOwnBuffEffect(int skillid, int effectid, int playerLevel, int skillLevel) {
        return showOwnBuffEffect(skillid, effectid, playerLevel, skillLevel, (byte) 3);
    }

    public static byte[] showOwnBuffEffect(int skillid, int effectid, int playerLevel, int skillLevel, byte direction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(playerLevel - 1); //player level
        mplew.write(skillLevel); //skill level
        if (direction != (byte) 3) {
            mplew.write(direction);
        }

        return mplew.getPacket();
    }

    public static byte[] showOwnDiceEffect(int skillid, int effectid, int effectid2, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(3);
        mplew.writeInt(effectid);
        mplew.writeInt(skillid);
        mplew.write(level);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showDiceEffect(int cid, int skillid, int effectid, int effectid2, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(3);
        mplew.writeInt(effectid);
        mplew.writeInt(skillid);
        mplew.write(level);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showSpecialEffect(int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(effect);

        return mplew.getPacket();
    }

    public static byte[] showSpecialEffect(int cid, int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(effect);

        return mplew.getPacket();
    }

    public static byte[] updateSkill(int skillid, int level, int masterlevel, long expiration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(1);
        mplew.writeShort(1);
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(masterlevel);
        PacketHelper.addExpirationTime(mplew, expiration);
        mplew.write(4);

        return mplew.getPacket();
    }

    public static final byte[] updateQuestMobKills(final MapleQuestStatus status) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(1);
        mplew.writeShort(status.getQuest().getId());
        mplew.write(1);

        final StringBuilder sb = new StringBuilder();
        for (final int kills : status.getMobKills().values()) {
            sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
        }
        mplew.writeMapleAsciiString(sb.toString());
        mplew.writeZeroBytes(8);

        return mplew.getPacket();
    }

    public static byte[] getShowQuestCompletion(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        mplew.writeShort(id);

        return mplew.getPacket();
    }

    public static byte[] getKeymap(MapleKeyLayout layout) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.KEYMAP.getValue());

        layout.writeData(mplew);

        return mplew.getPacket();
    }

    public static byte[] petAutoHP(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PET_AUTO_HP.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] petAutoMP(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PET_AUTO_MP.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] getWhisper(String sender, int channel, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString(sender);
        mplew.writeShort(channel - 1);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    public static byte[] getWhisperReply(String target, byte reply) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x0A); // whisper?
        mplew.writeMapleAsciiString(target);
        mplew.write(reply);//  0x0 = cannot find char, 0x1 = success

        return mplew.getPacket();
    }

    public static byte[] ignoreWhisper(String target) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x22);
        mplew.writeMapleAsciiString(target);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithMap(String target, int mapid, final boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(1);
        mplew.writeInt(mapid);
        mplew.writeZeroBytes(8); // ?? official doesn't send zeros here but whatever

        return mplew.getPacket();
    }

    public static byte[] getFindReply(String target, int channel, final boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(3);
        mplew.writeInt(channel - 1);

        return mplew.getPacket();
    }

    public static byte[] getInventoryFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(1);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getInventoryStatus() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] getShowInventoryFull() {
        return getShowInventoryStatus(0xff);
    }

    public static byte[] showItemUnavailable() {
        return getShowInventoryStatus(0xfe);
    }

    public static byte[] getShowInventoryStatus(int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0);
        mplew.write(mode);
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getStorage(int npcId, byte slots, Collection<Item> items, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x16);
        mplew.writeInt(npcId);
        mplew.write(slots);
        mplew.writeShort(0x7E);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(meso);
        mplew.writeShort(0);
        mplew.write((byte) items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item, true, true);
        }
        mplew.writeShort(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getStorageFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x11);
        return mplew.getPacket();
    }

    public static byte[] getStorageNotEnoughMeso() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x10);
        return mplew.getPacket();
    }

    public static byte[] mesoStorage(byte slots, int meso) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x13);
        mplew.write(slots);
        mplew.writeShort(2);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(meso);

        return mplew.getPacket();
    }

    public static byte[] arrangeStorage(byte slots, Collection<Item> items, boolean changed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x0F);
        mplew.write(slots);
        mplew.write(0x7C); //4 | 8 | 10 | 20 | 40
        mplew.writeZeroBytes(10);
        mplew.write(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item, true, true);
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] storeStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.OPEN_STORAGE.getValue());//
        /*10 메소 부족 
         9 남은 아이템 슬롯이 부족하지 않은지 확인해보세요 
         8 
         14 창고가 꽉*/
        mplew.write(0x0D);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item, true, true);
        }
        return mplew.getPacket();
    }

    public static byte[] takeOutStorage(byte slots, MapleInventoryType type, Collection<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.OPEN_STORAGE.getValue());
        mplew.write(0x9);
        mplew.write(slots);
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.write(items.size());
        for (Item item : items) {
            PacketHelper.addItemInfo(mplew, item, true, true);
        }
        return mplew.getPacket();
    }

    public static byte[] fairyPendantMessage(int type, int percent) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.FAIRY_PEND_MSG.getValue());
        mplew.writeShort(21); // 0x15
        mplew.writeInt(0); // idk
        mplew.writeShort(0); // idk
        mplew.writeShort(percent); // percent
        mplew.writeShort(0); // idk

        return mplew.getPacket();
    }

    public static byte[] giveFameResponse(int mode, String charname, int newfame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(charname);
        mplew.write(mode);
        mplew.writeInt(newfame);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] giveFameErrorResponse(int status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        /*	* 0: ok, use giveFameResponse<br>
         * 1: the username is incorrectly entered<br>
         * 2: users under level 15 are unable to toggle with fame.<br>
         * 3: can't raise or drop fame anymore today.<br>
         * 4: can't raise or drop fame for this character for this month anymore.<br>
         * 5: received fame, use receiveFame()<br>
         * 6: level of fame neither has been raised nor dropped due to an unexpected error*/
        mplew.writeOpcode(SendPacketOpcode.FAME_RESPONSE.getValue());

        mplew.write(status);

        return mplew.getPacket();
    }

    public static byte[] receiveFame(int mode, String charnameFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(5);
        mplew.writeMapleAsciiString(charnameFrom);
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] partyCreated(int partyid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(8);
        mplew.writeInt(partyid);
        mplew.writeInt(999999999);
        mplew.writeInt(999999999);
        mplew.writeLong(0);
        mplew.write(0);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] partyInvite(MapleCharacter from, boolean search) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(4);
        mplew.writeInt(from.getParty() == null ? 0 : from.getParty().getId());
        mplew.writeMapleAsciiString(from.getName());
        mplew.writeInt(from.getLevel());
        mplew.writeInt(from.getJob());
        mplew.write(search ? 1 : 0); // 1 이상이면 강제 가입

        return mplew.getPacket();
    }

    public static byte[] partyRequestInvite(MapleCharacter from) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(7);
        mplew.writeInt(from.getId());
        mplew.writeMapleAsciiString(from.getName());
        mplew.writeInt(from.getLevel());
        mplew.writeInt(from.getJob());

        return mplew.getPacket();
    }

    public static byte[] partyStatusMessage(int message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        /*	* 10: A beginner can't create a party.
         * 1/11/14/19: Your request for a party didn't work due to an unexpected error.
         * 13: You have yet to join a party.
         * 16: Already have joined a party.
         * 17: The party you're trying to join is already in full capacity.
         * 18: Unable to find the requested character in this channel.*/
        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(message);

        return mplew.getPacket();
    }

    public static byte[] partyStatusMessage(int message, String charname) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(message); // 23: 'Char' have denied request to the party.
        mplew.writeMapleAsciiString(charname);

        return mplew.getPacket();
    }

    private static void addPartyStatus(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew, boolean leaving) {
        addPartyStatus(forchannel, party, lew, leaving, false);
    }

    private static void addPartyStatus(int forchannel, MapleParty party, MaplePacketLittleEndianWriter lew, boolean leaving, boolean exped) {
        List<MaplePartyCharacter> partymembers;
        if (party == null) {
            partymembers = new ArrayList<MaplePartyCharacter>();
        } else {
            partymembers = new ArrayList<MaplePartyCharacter>(party.getMembers());
        }
        while (partymembers.size() < 6) {
            partymembers.add(new MaplePartyCharacter());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeAsciiString(partychar.getName(), 13);
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getJobId());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            lew.writeInt(partychar.getLevel());
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.isOnline()) {
                lew.writeInt(partychar.getChannel() - 1);
            } else {
                lew.writeInt(-2);
            }
        }
        /*for (MaplePartyCharacter partychar : partymembers) {
         lew.writeInt(0); //dunno, TODOO CHECK MSEA
         }*/
        lew.writeInt(party == null ? 0 : party.getLeader().getId());
        if (exped) {
            return;
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel) {
                lew.writeInt(partychar.getMapid());
            } else {
                lew.writeInt(0);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getChannel() == forchannel && !leaving) {
                lew.writeInt(partychar.getDoorTown());
                lew.writeInt(partychar.getDoorTarget());
                lew.writeInt(partychar.getDoorSkill());
                lew.writeInt(partychar.getDoorPosition().x);
                lew.writeInt(partychar.getDoorPosition().y);
            } else {
                lew.writeInt(leaving ? 999999999 : 0);
                lew.writeLong(leaving ? 999999999 : 0);
                lew.writeLong(leaving ? -1 : 0);
            }
        }
        for (MaplePartyCharacter partychar : partymembers) {
            if (partychar.getId() > 0) { //exists
                lew.writeInt(255);
            } else {
                lew.writeInt(0);
            }
        }
        for (int i = 0; i < 4; i++) {
            lew.writeLong(0);
        }
    }

    public static byte[] updateParty(int forChannel, MapleParty party, PartyOperation op, MaplePartyCharacter target) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        switch (op) {
            case DISBAND:
            case EXPEL:
            case LEAVE:
                mplew.write(0xC);
                mplew.writeInt(party.getId());
                mplew.writeInt(target.getId());
                mplew.write(op == PartyOperation.DISBAND ? 0 : 1);
                if (op == PartyOperation.DISBAND) {
                    mplew.writeInt(target.getId());
                } else {
                    mplew.write(op == PartyOperation.EXPEL ? 1 : 0);
                    mplew.writeMapleAsciiString(target.getName());
                    addPartyStatus(forChannel, party, mplew, op == PartyOperation.LEAVE);
                }
                break;
            case JOIN:
                mplew.write(0xF);
                mplew.writeInt(party.getId());
                mplew.writeMapleAsciiString(target.getName());
                addPartyStatus(forChannel, party, mplew, false);
                break;
            case SILENT_UPDATE:
            case LOG_ONOFF:
                mplew.write(0x7);
                mplew.writeInt(party.getId());
                addPartyStatus(forChannel, party, mplew, op == PartyOperation.LOG_ONOFF);
                break;
            case CHANGE_LEADER:
            case CHANGE_LEADER_DC:
                mplew.write(0x1F); //test 18 현재 채널 17 이미 풀 15 파끝 20 파끝 21 파끝
                //1f 테스트해봐야함
                mplew.writeInt(target.getId());
                mplew.write(op == PartyOperation.CHANGE_LEADER_DC ? 1 : 0);
                break;
            //1D = expel function not available in this map.
        }
        return mplew.getPacket();
    }

    public static byte[] partyPortal(int townId, int targetId, int skillId, Point position, boolean animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(45);
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        mplew.writeInt(skillId);
        mplew.writePos(position);
//        System.out.println(HexTool.toString(mplew.getPacket()));

        return mplew.getPacket();
    }

    public static byte[] updatePartyMemberHP(int cid, int curhp, int maxhp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_PARTYMEMBER_HP.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(curhp);
        mplew.writeInt(maxhp);

        return mplew.getPacket();
    }

    public static byte[] multiChat(String name, String chattext, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MULTICHAT.getValue());
        mplew.write(mode); //  0 buddychat; 1 partychat; 2 guildchat
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(chattext);

        return mplew.getPacket();
    }

    public static byte[] getClock(int time) { // time in seconds
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CLOCK.getValue());
        mplew.write(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getClockTime(int hour, int min, int sec) { // Current Time
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CLOCK.getValue());
        mplew.write(1); //Clock-Type
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);

        return mplew.getPacket();
    }

    public static byte[] spawnMist(int oid, int ownerCid, int skill, int level, MapleMist mist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(oid);
        if (mist.isSmMist()) {
            mplew.write(0);
        } else if (mist.isRoMist()) {
            mplew.write(0);
        } else {
            mplew.write(mist.isPoisonMist());
        }
        mplew.writeInt(ownerCid);
        mplew.writeInt(skill);
        mplew.write(level);
        mplew.writeShort(mist.getSkillDelay());
        mplew.writeRect(mist.getBox());
        if (mist.isMobMist()) {
            mplew.write(0);
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        if (!mist.isSmMist() && !mist.isRoMist()) {
            mplew.write(0);
        } else {
            mplew.write(mist.isRoMist() ? 0 : mist.isSmMist() ? 1 : 0);
        }
        mplew.writeInt(ownerCid);
        // System.out.print("/" + oid + "/" +ownerCid + "/" +skill + "/" +level + "/" +mist.getSkillDelay() + "/" +mist.getBox() + "/" +mist.getSourceSkill().getId());
        return mplew.getPacket();
    }

    public static byte[] removeMist(final int oid, final boolean eruption) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(oid);
        mplew.write(eruption ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] damageSummon(int cid, int summonSkillId, int damage, int unkByte, int monsterIdFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DAMAGE_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(unkByte);
        mplew.writeInt(damage);
        mplew.writeInt(monsterIdFrom);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] buddylistMessage(byte message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(message);

        return mplew.getPacket();
    }

    public static byte[] updateBuddylist(Collection<BuddylistEntry> buddylist) {
        return updateBuddylist(buddylist, 7);
    }

    public static byte[] updateBuddylist(Collection<BuddylistEntry> buddylist, int deleted) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(deleted);
        mplew.write(buddylist.size());

        for (BuddylistEntry buddy : buddylist) {
            mplew.writeInt(buddy.getCharacterId());
            mplew.writeAsciiString(buddy.getName(), 13);
            mplew.write(buddy.isVisible() ? 0 : 1);
            mplew.writeInt(buddy.getChannel() == -1 ? -1 : (buddy.getChannel() - 1));
            mplew.writeAsciiString(buddy.getGroup(), 17);
        }
        for (int x = 0; x < buddylist.size(); x++) {
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] requestBuddylistAdd(int cidFrom, String nameFrom, int levelFrom, int jobFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(9);
        mplew.writeInt(cidFrom);
        mplew.writeMapleAsciiString(nameFrom);
        mplew.writeInt(levelFrom);
        mplew.writeInt(jobFrom);
        mplew.writeInt(cidFrom);
        mplew.writeAsciiString(nameFrom, 13);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeAsciiString("그룹 미지정", 17);
        mplew.writeShort(1);

        return mplew.getPacket();
    }

    public static byte[] updateBuddyChannel(int characterid, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(0x14);
        mplew.writeInt(characterid);
        mplew.write(0); //cashshop?
        mplew.writeInt(channel);

        return mplew.getPacket();
    }

    public static byte[] itemEffect(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] updateBuddyCapacity(int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(0x15);
        mplew.write(capacity);

        return mplew.getPacket();
    }

    public static byte[] showChair(int characterid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(characterid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] cancelChair(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_CHAIR.getValue());
        if (id == -1) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeShort(id);
        }
        return mplew.getPacket();
    }

    public static byte[] spawnReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getReactorId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.write(reactor.getFacingDirection()); // stance
        mplew.writeMapleAsciiString(reactor.getName());

        return mplew.getPacket();
    }

    public static byte[] triggerReactor(MapleReactor reactor, int stance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.writeShort(stance);
        mplew.write(0);
        mplew.write(4); // frame delay, set to 5 since there doesn't appear to be a fixed formula for it
        return mplew.getPacket();
    }

    public static byte[] destroyReactor(MapleReactor reactor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getPosition());

        return mplew.getPacket();
    }

    public static byte[] musicChange(String song) {
        return environmentChange(song, 6);
    }

    public static byte[] showEffect(String effect) {
        return environmentChange(effect, 3);
    }

    public static byte[] playSound(String sound) {
        return environmentChange(sound, 4);
    }

    public static byte[] environmentChange(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(mode);
        /*if (mode == 2) {
         mplew.write(0);
         }*/
        mplew.writeMapleAsciiString(env);

        return mplew.getPacket();
    }

    public static byte[] environmentMove(String env, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MOVE_ENV.getValue());
        mplew.writeMapleAsciiString(env);
        mplew.writeInt(mode);

        return mplew.getPacket();
    }

    public static byte[] startMapEffect(String msg, int itemid, boolean active) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MAP_EFFECT.getValue());
        //mplew.write(active ? 0 : 1);
        mplew.writeInt(active ? itemid : 0);
        if (active) {
            mplew.writeMapleAsciiString(msg);
        }
        return mplew.getPacket();
    }

    public static byte[] removeMapEffect() {
        return startMapEffect(null, 0, false);
    }

    public static byte[] showGuildInfo(MapleCharacter c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x1C); //signature for showing guild info - 0x20 aftershock

        if (c == null || c.getMGC() == null) { //show empty guild (used for leaving, expelled)
            mplew.write(0);
            return mplew.getPacket();
        }
        MapleGuild g = World.Guild.getGuild(c.getGuildId());
        if (g == null) { //failed to read from DB - don't show a guild
            mplew.write(0);
            return mplew.getPacket();
        }
        mplew.write(1); //bInGuild
        getGuildInfo(mplew, g);

        return mplew.getPacket();
    }

    private static void getGuildInfo(MaplePacketLittleEndianWriter mplew, MapleGuild guild) {
        mplew.writeInt(guild.getId());
        mplew.writeMapleAsciiString(guild.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(guild.getRankTitle(i));
        }
        guild.addMemberData(mplew);
        mplew.writeInt(guild.getCapacity());
        mplew.writeShort(guild.getLogoBG());
        mplew.write(guild.getLogoBGColor());
        mplew.writeShort(guild.getLogo());
        mplew.write(guild.getLogoColor());
        mplew.writeMapleAsciiString(guild.getNotice());
        mplew.writeInt(guild.getGP()); //written twice, aftershock?
        mplew.writeInt(guild.getAllianceId() > 0 ? guild.getAllianceId() : 0);//1.2.6x
        mplew.write(guild.getLevel());
        mplew.writeShort(0); //probably guild rank or somethin related, appears to be 0
        /*mplew.writeShort(guild.getSkills().size()); //AFTERSHOCK: uncomment
         for (MapleGuildSkill i : guild.getSkills()) {
         mplew.writeInt(i.skillID);
         mplew.writeShort(i.level);
         mplew.writeLong(PacketHelper.getTime(i.timestamp));
         mplew.writeMapleAsciiString(i.purchaser);
         mplew.writeMapleAsciiString(i.activator);
         }*/

    }

    public static byte[] guildSkillPurchased(int gid, int sid, int level, long expiration, String purchase, String activate) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x55); //0x55 aftershock
        mplew.writeInt(gid);
        mplew.writeInt(sid);
        mplew.writeShort(level);
        mplew.writeLong(PacketHelper.getTime(expiration));
        mplew.writeMapleAsciiString(purchase);
        mplew.writeMapleAsciiString(activate);

        return mplew.getPacket();
    }

    public static byte[] guildLeaderChanged(int gid, int oldLeader, int newLeader, int allianceId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x59); //0x59 aftershock
        mplew.writeInt(gid);
        //01 36 00 00 00
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);
        mplew.write(1); //new rank lol
        mplew.writeInt(allianceId);

        return mplew.getPacket();
    }

    public static byte[] guildMemberOnline(int gid, int cid, boolean bOnline) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3F);
        mplew.writeInt(gid);
        mplew.writeInt(cid);
        mplew.write(bOnline ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] guildContribution(int gid, int cid, int c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x50);
        mplew.writeInt(gid);
        mplew.writeInt(cid);
        mplew.writeInt(c);

        return mplew.getPacket();
    }

    public static byte[] guildInvite(int gid, String charName, int levelFrom, int jobFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(charName);
        mplew.writeInt(levelFrom);
        mplew.writeInt(jobFrom);

        return mplew.getPacket();
    }

    public static byte[] denyGuildInvitation(byte code, String charname) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);
        mplew.writeMapleAsciiString(charname);

        return mplew.getPacket();
    }

    public static byte[] genericGuildMessage(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);

        return mplew.getPacket();
    }

    public static byte[] newGuildMember(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());

        mplew.write(41); //0x27=39 v4 = 35 - 26 =
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeAsciiString(mgc.getName(), 13);
        mplew.writeInt(mgc.getJobId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getGuildRank()); //should be always 5 but whatevs
        mplew.writeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
        //mplew.writeInt(1); //? could be guild signature, but doesn't seem to matter
        mplew.writeInt(mgc.getAllianceRank()); //? could be guild signature, but doesn't seem to matter
        mplew.writeInt(mgc.getGuildContribution()); //should always 3

        return mplew.getPacket();
    }

    //someone leaving, mode == 0x2c for leaving, 0x2f for expelled
    public static byte[] memberLeft(MapleGuildCharacter mgc, boolean bExpelled) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(bExpelled ? 0x31 : 0x2E);

        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeMapleAsciiString(mgc.getName());

        return mplew.getPacket();
    }

    public static byte[] changeRank(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x42); //+4 aftershock
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.write(mgc.getGuildRank());

        return mplew.getPacket();
    }

    public static byte[] guildNotice(int gid, String notice) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x47);
        mplew.writeInt(gid);
        mplew.writeMapleAsciiString(notice);

        return mplew.getPacket();
    }

    public static byte[] guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3E);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());

        return mplew.getPacket();
    }

    public static byte[] rankTitleChange(int gid, String[] ranks) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x40);
        mplew.writeInt(gid);

        for (String r : ranks) {
            mplew.writeMapleAsciiString(r);
        }
        return mplew.getPacket();
    }

    public static byte[] guildDisband(int gid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x34);
        mplew.writeInt(gid);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] guildEmblemChange(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x45);
        mplew.writeInt(gid);
        mplew.writeShort(bg);
        mplew.write(bgcolor);
        mplew.writeShort(logo);
        mplew.write(logocolor);

        return mplew.getPacket();
    }

    public static byte[] guildCapacityChange(int gid, int capacity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x3C);
        mplew.writeInt(gid);
        mplew.write(capacity);

        return mplew.getPacket();
    }

    public static byte[] removeGuildFromAlliance(MapleGuildAlliance alliance, int gid, MapleGuild expelledGuild, boolean expelled) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x10);
        addAllianceInfo(mplew, alliance);
        mplew.writeInt(gid);
        getGuildInfo(mplew, expelledGuild);
        mplew.write(expelled ? 1 : 0); //1 = expelled, 0 = left
        return mplew.getPacket();
    }

    public static byte[] changeAlliance(MapleGuildAlliance alliance, final boolean in) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x01);
        mplew.write(in ? 1 : 0);
        mplew.writeInt(in ? alliance.getId() : 0);
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < noGuilds; i++) {
            g[i] = World.Guild.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return enableActions();
            }
        }
        mplew.write(noGuilds);
        for (int i = 0; i < noGuilds; i++) {
            mplew.writeInt(g[i].getId());
            //must be world
            Collection<MapleGuildCharacter> members = g[i].getMembers();
            mplew.writeInt(members.size());
            for (MapleGuildCharacter mgc : members) {
                mplew.writeInt(mgc.getId());
                mplew.write(in ? mgc.getAllianceRank() : 0);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] changeAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x02);
        mplew.writeInt(allianceid);
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);
        return mplew.getPacket();
    }

    public static byte[] updateAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x19);
        mplew.writeInt(allianceid);
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);
        return mplew.getPacket();
    }

    public static byte[] sendAllianceInvite(String allianceName, MapleCharacter inviter) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x03);
        mplew.writeInt(inviter.getGuildId());
        mplew.writeMapleAsciiString(inviter.getName());
        //alliance invite did NOT change
        mplew.writeMapleAsciiString(allianceName);
        return mplew.getPacket();
    }

    public static byte[] changeGuildInAlliance(MapleGuildAlliance alliance, MapleGuild guild, final boolean add) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x04);
        mplew.writeInt(add ? alliance.getId() : 0);
        mplew.writeInt(guild.getId());
        Collection<MapleGuildCharacter> members = guild.getMembers();
        mplew.writeInt(members.size());
        for (MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId());
            mplew.write(add ? mgc.getAllianceRank() : 0);
        }
        return mplew.getPacket();
    }

    public static byte[] changeAllianceRank(int allianceid, MapleGuildCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(allianceid);
        mplew.writeInt(player.getId());
        mplew.writeInt(player.getAllianceRank());
        return mplew.getPacket();
    }

    public static byte[] createGuildAlliance(MapleGuildAlliance alliance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0F);
        addAllianceInfo(mplew, alliance);
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            g[i] = World.Guild.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return enableActions();
            }
        }
        for (MapleGuild gg : g) {
            getGuildInfo(mplew, gg);
        }
        return mplew.getPacket();
    }

    public static byte[] getAllianceInfo(MapleGuildAlliance alliance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0C);
        mplew.write(alliance == null ? 0 : 1); //in an alliance
        if (alliance != null) {
            addAllianceInfo(mplew, alliance);
        }
        return mplew.getPacket();
    }

    public static byte[] getAllianceUpdate(MapleGuildAlliance alliance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x17);
        addAllianceInfo(mplew, alliance);
        return mplew.getPacket();
    }

    public static byte[] getGuildAlliance(MapleGuildAlliance alliance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0D);
        if (alliance == null) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        final int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            g[i] = World.Guild.getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return enableActions();
            }
        }
        mplew.writeInt(noGuilds);
        for (MapleGuild gg : g) {
            getGuildInfo(mplew, gg);
        }
        return mplew.getPacket();
    }

    public static byte[] addGuildToAlliance(MapleGuildAlliance alliance, MapleGuild newGuild) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x12);
        addAllianceInfo(mplew, alliance);
        mplew.writeInt(newGuild.getId()); //???
        getGuildInfo(mplew, newGuild);
        mplew.write(0); //???
        return mplew.getPacket();
    }

    private static void addAllianceInfo(MaplePacketLittleEndianWriter mplew, MapleGuildAlliance alliance) {
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRank(i));
        }
        mplew.write(alliance.getNoGuilds());
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            mplew.writeInt(alliance.getGuildId(i));
        }
        mplew.writeInt(alliance.getCapacity()); // ????
        mplew.writeMapleAsciiString(alliance.getNotice());
    }

    public static byte[] allianceMemberOnline(int alliance, int gid, int id, boolean online) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0E);
        mplew.writeInt(alliance);
        mplew.writeInt(gid);
        mplew.writeInt(id);
        mplew.write(online ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] updateAlliance(MapleGuildCharacter mgc, int allianceid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x18);
        mplew.writeInt(allianceid);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());

        return mplew.getPacket();
    }

    public static byte[] updateAllianceRank(int allianceid, MapleGuildCharacter mgc) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1B);
        mplew.writeInt(allianceid);
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getAllianceRank());

        return mplew.getPacket();
    }

    public static byte[] disbandAlliance(int alliance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1D);
        mplew.writeInt(alliance);

        return mplew.getPacket();
    }

    public static byte[] BBSThreadList(final List<MapleBBSThread> bbs, int start) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(6);

        if (bbs == null) {
            mplew.write(0);
            mplew.writeLong(0);
            return mplew.getPacket();
        }
        int threadCount = bbs.size();
        MapleBBSThread notice = null;
        for (MapleBBSThread b : bbs) {
            if (b.isNotice()) { //notice
                notice = b;
                break;
            }
        }
        mplew.write(notice == null ? 0 : 1);
        if (notice != null) { //has a notice
            addThread(mplew, notice);
        }
        if (threadCount < start) { //seek to the thread before where we start
            //uh, we're trying to start at a place past possible
            start = 0;
        }
        //each page has 10 threads, start = page # in packet but not here
        mplew.writeInt(threadCount);
        final int pages = Math.min(10, threadCount - start);
        mplew.writeInt(pages);

        for (int i = 0; i < pages; i++) {
            addThread(mplew, bbs.get(start + i)); //because 0 = notice
        }
        return mplew.getPacket();
    }

    private static void addThread(MaplePacketLittleEndianWriter mplew, MapleBBSThread rs) {
        mplew.writeInt(rs.localthreadID);
        mplew.writeInt(rs.ownerID);
        mplew.writeMapleAsciiString(rs.name);
        mplew.writeLong(PacketHelper.getKoreanTimestamp(rs.timestamp));
        mplew.writeInt(rs.icon);
        mplew.writeInt(rs.getReplyCount());
    }

    public static byte[] showThread(MapleBBSThread thread) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(7);

        mplew.writeInt(thread.localthreadID);
        mplew.writeInt(thread.ownerID);
        mplew.writeLong(PacketHelper.getKoreanTimestamp(thread.timestamp));
        mplew.writeMapleAsciiString(thread.name);
        mplew.writeMapleAsciiString(thread.text);
        mplew.writeInt(thread.icon);
        mplew.writeInt(thread.getReplyCount());
        for (MapleBBSReply reply : thread.replies.values()) {
            mplew.writeInt(reply.replyid);
            mplew.writeInt(reply.ownerID);
            mplew.writeLong(PacketHelper.getKoreanTimestamp(reply.timestamp));
            mplew.writeMapleAsciiString(reply.content);
        }
        return mplew.getPacket();
    }

    public static byte[] showGuildRanks(int npcid, List<GuildRankingInfo> all) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x4C);
        mplew.writeInt(npcid);
        //this error 38s and official servers have it removed
        mplew.writeInt(all.size());

        for (GuildRankingInfo info : all) {
            mplew.writeMapleAsciiString(info.getName());
            mplew.writeInt(info.getGP());
            mplew.writeInt(info.getLogo());
            mplew.writeInt(info.getLogoColor());
            mplew.writeInt(info.getLogoBg());
            mplew.writeInt(info.getLogoBgColor());
        }

        return mplew.getPacket();
    }

    /*
    
            List<RankingWorker.RankingInformation> list = RankingWorker.getRankingInfo(cmd);
            int list_size = list.size();
     */
    public static byte[] showPlayerRanks(int npcid, List<RankingWorker.RankingInformation> list) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x4C);
        mplew.writeInt(npcid);
        //this error 38s and official servers have it removed
        mplew.writeInt(list.size());

        for (RankingInformation info : list) {
            mplew.writeMapleAsciiString("닉네임: " + info.getName());
            mplew.writeInt(info.getLevel());
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] updateGP(int gid, int GP, int glevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x48);
        mplew.writeInt(gid);
        mplew.writeInt(GP); //2nd int = guild level or something
        mplew.writeInt(glevel);

        return mplew.getPacket();
    }

    public static byte[] skillEffect(MapleCharacter from, int skillId, byte level, byte flags, byte speed, byte unk) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);
        mplew.write(level);
        mplew.write(flags);
        mplew.write(speed);
        mplew.write(unk); // Direction ??

        return mplew.getPacket();
    }

    public static byte[] skillCancel(MapleCharacter from, int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    public static byte[] showMagnet(int mobid, byte success) { // Monster Magnet
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_MAGNET.getValue());
        mplew.writeInt(mobid);
        mplew.write(success);
        mplew.write(10);

        return mplew.getPacket();
    }

    public static byte[] sendHint(String hint, int width, int height) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (width < 1) {
            width = hint.length() * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (height < 5) {
            height = 5;
        }
        mplew.writeOpcode(SendPacketOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width);
        mplew.writeShort(height);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] messengerInvite(String from, int messengerid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x03);
        mplew.writeMapleAsciiString(from);
        mplew.write(0x00);
        mplew.writeInt(messengerid);
        mplew.write(0x00);

        return mplew.getPacket();
    }

    public static byte[] addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x00);
        mplew.write(position);
        PacketHelper.addCharLook(mplew, chr, true);
        mplew.writeMapleAsciiString(from);
        mplew.writeShort(channel);

        return mplew.getPacket();
    }

    public static byte[] removeMessengerPlayer(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x02);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static byte[] updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x07);
        mplew.write(position);
        PacketHelper.addCharLook(mplew, chr, true);
        mplew.writeMapleAsciiString(from);
        mplew.writeShort(channel);

        return mplew.getPacket();
    }

    public static byte[] joinMessenger(int position) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x01);
        mplew.write(position);

        return mplew.getPacket();
    }

    public static byte[] messengerChat(String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x06);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    public static byte[] messengerNote(String text, int mode, int mode2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(text);
        mplew.write(mode2);

        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithCS(String target, final boolean buddy) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 72 : 9);
        mplew.writeMapleAsciiString(target);
        mplew.write(2);
        mplew.writeInt(-1);

        return mplew.getPacket();
    }

    public static byte[] showEquipEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());

        return mplew.getPacket();
    }

    public static byte[] showEquipEffect(int team) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());
        mplew.writeShort(team);
        return mplew.getPacket();
    }

    public static byte[] summonSkill(int cid, int summonSkillId, int newStance) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SUMMON_SKILL.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(summonSkillId);
        mplew.write(newStance);
        //System.out.println("cid: " + cid + " summonSkillId: " + summonSkillId + " newStance: " + newStance);
        return mplew.getPacket();
    }

    public static byte[] skillCooldown(int sid, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.COOLDOWN.getValue());
        mplew.writeInt(sid);
        mplew.writeShort(time);

        return mplew.getPacket();
    }

    public static byte[] useSkillBook(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        mplew.write(0); //?
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getMacros(SkillMacro[] macros) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count); // number of macros
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }
        return mplew.getPacket();
    }

    public static byte[] updateAriantPQRanking(String name, int score, boolean empty) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ARIANT_PQ_START.getValue());
        mplew.write(empty ? 0 : 1);
        if (!empty) {
            mplew.writeMapleAsciiString(name);
            mplew.writeInt(score);
        }
        return mplew.getPacket();
    }

    public static byte[] catchMonster(int mobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CATCH_MONSTER.getValue());
        mplew.writeInt(mobid);
        mplew.writeInt(itemid);
        mplew.write(success);

        return mplew.getPacket();
    }

    public static byte[] catchMob(int mobid, int itemid, byte success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CATCH_MOB.getValue());
        mplew.write(success);
        mplew.writeInt(itemid);
        mplew.writeInt(mobid);

        return mplew.getPacket();
    }

    public static byte[] showAriantScoreBoard() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ARIANT_SCOREBOARD.getValue());

        return mplew.getPacket();
    }

    public static byte[] boatPacket(int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
        mplew.writeOpcode(SendPacketOpcode.BOAT_EFFECT.getValue());
        mplew.write(effect);
        mplew.write(0);
//mplew.writeShort(effect); // 0A 04 balrog
        //this packet had 3: boat leaves

        return mplew.getPacket();
    }

    public static byte[] boatEffect(int effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
        mplew.writeOpcode(SendPacketOpcode.BOAT_EFF.getValue());
        mplew.writeShort(effect); // 0A 04 balrog
        //this packet had the other ones o.o

        return mplew.getPacket();
    }

    public static byte[] boatPacket(boolean type) {//don't think this is correct..
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(SendPacketOpcode.BOAT_EFFECT.getValue());
        mplew.write(type ? 1 : 2);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] removeItemFromDuey(boolean remove, int Package) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x18);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);

        return mplew.getPacket();
    }

    public static byte[] receiveParcel(String from, boolean quick) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x1A);
        mplew.writeMapleAsciiString(from);
        mplew.write(quick ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] sendDuey(byte operation, List<MapleDueyActions> packages, List<MapleDueyActions> expired) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DUEY.getValue());
        mplew.write(operation);
        if (packages == null) {
            mplew.write(0);
            mplew.write(0);
            mplew.write(0);
            return mplew.getPacket();
        }

        switch (operation) {
            case 9: { // Request 13 Digit AS
                mplew.write(1);
                // 0xFF = error
                break;
            }
            case 10: { // Open duey
                mplew.write(0);
                mplew.write(packages.size());
                for (MapleDueyActions dp : packages) {

                    mplew.writeInt(dp.getPackageId());                         //4
                    mplew.writeAsciiString(dp.getSender(), 13);                //17
                    mplew.writeInt(dp.getMesos());                             //21
                    if (dp.canReceive()) {
                        mplew.writeLong(PacketHelper.getTime(dp.getExpireTime()));   //택배시간 > 현재시간 : 받을수있음.   택배시간 < 현재시간 : 배송중..
                    } else {
                        mplew.writeLong(0);
                    }                                                          //29
                    mplew.writeInt(dp.isQuick() ? 1 : 0);                      //33
                    mplew.writeAsciiString(dp.getContent(), 100);              //133
                    mplew.writeZeroBytes(100);
                    mplew.write(0);                                            //134

                    if (dp.getItem() != null) {
                        mplew.write(1);
                        PacketHelper.addItemInfo(mplew, dp.getItem(), true, true);
                    } else {
                        mplew.write(0);
                    }

                }
                if (expired == null) {
                    mplew.write(0);
                    return mplew.getPacket();
                }
                mplew.write(expired.size());
                for (MapleDueyActions dp : expired) {
                    mplew.writeInt(dp.getPackageId());                         //4
                    mplew.writeAsciiString(dp.getSender(), 13);                //17
                    mplew.writeInt(dp.getMesos());                             //21
                    if (dp.canReceive()) {
                        mplew.writeLong(PacketHelper.getTime(dp.getExpireTime()));   //택배시간 > 현재시간 : 받을수있음.   택배시간 < 현재시간 : 배송중..
                    } else {
                        mplew.writeLong(0);
                    }
                    mplew.write(dp.isQuick() ? 1 : 0);
                    mplew.writeAsciiString(dp.getContent(), 100);
                    mplew.writeInt(0);
                    if (dp.getItem() != null) {
                        mplew.write(1);
                        PacketHelper.addItemInfo(mplew, dp.getItem(), true, true);
                    } else {
                        mplew.write(0);
                    }
                }
                break;
            }
            case 0x1A: {
                mplew.writeMapleAsciiString("보낸사람? 아니면 내용인가?");
                mplew.write(1);
                break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] Mulung_DojoUp2() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(9); //AFTERSHOCK: 10? MAYBE

        return mplew.getPacket();
    }

    public static byte[] showQuestMsg(final String msg) {
        return serverNotice(5, msg);
    }

    public static byte[] Mulung_Pts(int recv, int total) {
        return showQuestMsg("수련점수를 " + recv + "점 받았습니다. 총 수련점수가 " + total + "점이 되었습니다.");
    }

    public static byte[] showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.OX_QUIZ.getValue());
        mplew.write(askQuestion ? 1 : 0);
        mplew.write(questionSet);
        mplew.writeShort(questionId);
        return mplew.getPacket();
    }

    public static byte[] leftKnockBack() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.LEFT_KNOCK_BACK.getValue());
        return mplew.getPacket();
    }

    public static byte[] rollSnowball(int type, MapleSnowballs ball1, MapleSnowballs ball2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ROLL_SNOWBALL.getValue());
        mplew.write(type); // 0 = normal, 1 = rolls from start to end, 2 = down disappear, 3 = up disappear, 4 = move
        mplew.writeInt(ball1 == null ? 0 : (ball1.getSnowmanHP() / 75));
        mplew.writeInt(ball2 == null ? 0 : (ball2.getSnowmanHP() / 75));
        mplew.writeShort(ball1 == null ? 0 : ball1.getPosition());
        mplew.write(0);
        mplew.writeShort(ball2 == null ? 0 : ball2.getPosition());
        mplew.writeZeroBytes(11);
        return mplew.getPacket();
    }

    public static byte[] enterSnowBall() {
        return rollSnowball(0, null, null);
    }

    public static byte[] hitSnowBall(int team, int damage, int distance, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.HIT_SNOWBALL.getValue());
        mplew.write(team);// 0 is down, 1 is up
        mplew.writeShort(damage);
        mplew.write(distance);
        mplew.write(delay);
        return mplew.getPacket();
    }

    public static byte[] snowballMessage(int team, int message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SNOWBALL_MESSAGE.getValue());
        mplew.write(team);// 0 is down, 1 is up
        mplew.writeInt(message);
        return mplew.getPacket();
    }

    public static byte[] finishedSort(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.FINISH_SORT.getValue());
        mplew.write(1);
        mplew.write(type);
        return mplew.getPacket();
    }

    // 00 01 00 00 00 00
    public static byte[] coconutScore(int[] coconutscore) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.COCONUT_SCORE.getValue());
        mplew.writeShort(coconutscore[0]);
        mplew.writeShort(coconutscore[1]);
        return mplew.getPacket();
    }

    public static byte[] hitCoconut(boolean spawn, int id, int type) {
        // FF 00 00 00 00 00 00
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.HIT_COCONUT.getValue());
        if (spawn) {
            mplew.write(0);
            mplew.writeInt(0x80);
        } else {
            mplew.writeInt(id);
            mplew.write(type); // What action to do for the coconut.
        }
        return mplew.getPacket();
    }

    public static byte[] finishedGather(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.FINISH_GATHER.getValue());
        mplew.write(1);
        mplew.write(type);
        return mplew.getPacket();
    }

    public static byte[] yellowChat(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.YELLOW_CHAT.getValue());
        mplew.write(-1); //could be something like mob displaying message.
        mplew.writeMapleAsciiString(msg);
        return mplew.getPacket();
    }

    public static byte[] getIncubatorResult(int itemId, short quantity, int itemId2, short quantity2, int ourItem) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PIGMI_REWARD.getValue());
        mplew.writeInt(itemId);
        mplew.writeShort(quantity);
        mplew.writeInt(ourItem);
        mplew.writeInt(itemId2);
        mplew.writeInt(quantity2);

        return mplew.getPacket();
    }

    public static byte[] ViciousHammer(boolean start, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        if (start) {
            mplew.write(0);
            mplew.writeInt(0);
        } else if (success) {
            mplew.write(2);
            mplew.writeInt(0);
        } else {
            mplew.write(2);
            mplew.writeInt(1);
        }
        return mplew.getPacket();
    }

    public static byte[] sendLevelup(boolean family, int level, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LEVEL_UPDATE.getValue());
        mplew.write(family ? 1 : 2);
        mplew.writeInt(level);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendMarriage(boolean family, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MARRIAGE_UPDATE.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendJobup(boolean family, int jobid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.JOB_UPDATE.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeInt(jobid); //or is this a short
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] showHorntailShrine(boolean spawned, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.HORNTAIL_SHRINE.getValue());
        mplew.write(spawned ? 1 : 0);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static byte[] showChaosZakumShrine(boolean spawned, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.CHAOS_ZAKUM_SHRINE.getValue());
        mplew.write(spawned ? 1 : 0);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static byte[] showChaosHorntailShrine(boolean spawned, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.CHAOS_HORNTAIL_SHRINE.getValue());
        mplew.write(spawned ? 1 : 0);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static byte[] stopClock() {
        return getPacketFromHexString(Integer.toHexString(SendPacketOpcode.STOP_CLOCK.getValue()) + " 00"); //does the header not work?
    }

    public static final byte[] temporaryStats_Aran() {
        final Map<MapleStat.Temp, Integer> stats = new EnumMap<MapleStat.Temp, Integer>(MapleStat.Temp.class);
        stats.put(MapleStat.Temp.STR, 9999);
        stats.put(MapleStat.Temp.DEX, 9999);
        stats.put(MapleStat.Temp.INT, 9999);
        stats.put(MapleStat.Temp.LUK, 9999);
        stats.put(MapleStat.Temp.WATK, 255);
        stats.put(MapleStat.Temp.ACC, 999);
        stats.put(MapleStat.Temp.AVOID, 999);
        stats.put(MapleStat.Temp.SPEED, 140);
        stats.put(MapleStat.Temp.JUMP, 120);
        return temporaryStats(stats);
    }

    public static final byte[] temporaryStats_Balrog(final MapleCharacter chr) {
        final Map<MapleStat.Temp, Integer> stats = new EnumMap<MapleStat.Temp, Integer>(MapleStat.Temp.class);
        int offset = 1 + (chr.getLevel() - 90) / 20;
        //every 20 levels above 90, +1
        stats.put(MapleStat.Temp.STR, chr.getStat().getTotalStr() / offset);
        stats.put(MapleStat.Temp.DEX, chr.getStat().getTotalDex() / offset);
        stats.put(MapleStat.Temp.INT, chr.getStat().getTotalInt() / offset);
        stats.put(MapleStat.Temp.LUK, chr.getStat().getTotalLuk() / offset);
        stats.put(MapleStat.Temp.WATK, chr.getStat().getTotalWatk() / offset);
        stats.put(MapleStat.Temp.MATK, chr.getStat().getTotalMagic() / offset);
        return temporaryStats(stats);
    }

    public static final byte[] temporaryStats(final Map<MapleStat.Temp, Integer> mystats) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.TEMP_STATS.getValue());
        //str 0x1, dex 0x2, int 0x4, luk 0x8
        //level 0x10 = 255
        //0x100 = 999
        //0x200 = 999
        //0x400 = 120
        //0x800 = 140
        int updateMask = 0;
        for (MapleStat.Temp statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        mplew.writeInt(updateMask);
        Integer value;

        for (final Entry<MapleStat.Temp, Integer> statupdate : mystats.entrySet()) {
            value = statupdate.getKey().getValue();

            if (value >= 1) {
                if (value <= 0x200) { //level 0x10 - is this really short or some other? (FF 00)
                    mplew.writeShort(statupdate.getValue().shortValue());
                } else {
                    mplew.write(statupdate.getValue().byteValue());
                }
            }
        }
        return mplew.getPacket();
    }

    public static final byte[] temporaryStats_Reset() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.TEMP_STATS_RESET.getValue());
        return mplew.getPacket();
    }

    //its likely that durability items use this
    public static final byte[] showHpHealed(final int cid, final int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(12); //bb +2
        mplew.writeInt(amount);
        return mplew.getPacket();
    }

    public static final byte[] showOwnHpHealed(final int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(12);  //bb +2
        mplew.writeInt(amount);
        return mplew.getPacket();
    }

    public static final byte[] sendRepairWindow(int npc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.REPAIR_WINDOW.getValue());
        mplew.writeInt(33);
        /*
         3: 그냥 스킬창 
         7: 친구창
         17: 카니발
         19: 에너지바
         21: 파티서치
         22: 메이커
         25: 랭킹
         26: 패밀리
         27: 가계도
         28: 스토리 보드 편지누르면 뜨는 창
         29: 스토리 보드 편지날아옴
         30: 메달창
         31: 이벤트창
         32: 에반스킬창
         33: 리페어
         35: 전투측정
         */
        mplew.writeInt(npc);
        return mplew.getPacket();
    }

    public static final byte[] openUI(byte value) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.OPEN_UI.getValue());
        mplew.writeInt(value);
        /*
         3: 그냥 스킬창 
         7: 친구창
         17: 카니발
         19: 에너지바
         21: 파티서치
         22: 메이커
         25: 랭킹
         26: 패밀리
         27: 가계도
         28: 스토리 보드 편지누르면 뜨는 창
         29: 스토리 보드 편지날아옴
         30: 메달창
         31: 이벤트창
         32: 에반스킬창
         33: 리페어
         35: 전투측정
         */
        return mplew.getPacket();
    }

    public static final byte[] sendProfessionWindow(int npc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.REPAIR_WINDOW.getValue());
        mplew.writeInt(0x2E); //sending 0x20 here opens evan skill window o.o
        mplew.writeInt(npc);
        return mplew.getPacket();
    }

    public static final byte[] sendPVPWindow(int npc) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.REPAIR_WINDOW.getValue());
        mplew.writeInt(0x35);
        mplew.writeInt(npc);
        return mplew.getPacket();
    }

    public static final byte[] sendPVPMaps() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.PVP_INFO.getValue());
        mplew.write(3); //max amount of players
        for (int i = 0; i < 20; i++) {
            mplew.writeInt(10); //how many peoples in each map
        }
        mplew.writeZeroBytes(124);
        mplew.writeShort(150); ////PVP 1.5 EVENT!
        return mplew.getPacket();
    }

    public static final byte[] sendPyramidUpdate(final int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.PYRAMID_UPDATE.getValue());
        mplew.writeInt(amount); //1-132 ?
        return mplew.getPacket();
    }

    public static final byte[] sendPyramidResult(final byte rank, final int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.PYRAMID_RESULT.getValue());
        mplew.write(rank);
        mplew.writeInt(amount); //1-132 ?
        return mplew.getPacket();
    }

    //show_status_info - 01 53 1E 01
    //10/08/14/19/11
    //update_quest_info - 08 53 1E 00 00 00 00 00 00 00 00
    //show_status_info - 01 51 1E 01 01 00 30
    //update_quest_info - 08 51 1E 00 00 00 00 00 00 00 00
    public static final byte[] sendPyramidEnergy(final String type, final String amount) {
        return sendString(1, type, amount);
    }

    public static final byte[] sendString(final int type, final String object, final String amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        switch (type) {
            case 1:
                mplew.writeOpcode(SendPacketOpcode.ENERGY.getValue());
                break;
            case 2:
                mplew.writeOpcode(SendPacketOpcode.GHOST_POINT.getValue());
                break;
            case 3:
                mplew.writeOpcode(SendPacketOpcode.GHOST_STATUS.getValue());
                break;
        }
        mplew.writeMapleAsciiString(object); //massacre_hit, massacre_cool, massacre_miss, massacre_party, massacre_laststage, massacre_skill
        mplew.writeMapleAsciiString(amount);
        return mplew.getPacket();
    }

    public static final byte[] sendGhostPoint(final String type, final String amount) {
        return sendString(2, type, amount); //PRaid_Point (0-1500???)
    }

    public static final byte[] sendGhostStatus(final String type, final String amount) {
        return sendString(3, type, amount); //Red_Stage(1-5), Blue_Stage, blueTeamDamage, redTeamDamage
    }

    public static byte[] MulungEnergy(int energy) {
        return sendPyramidEnergy("energy", String.valueOf(energy));
    }

    public static byte[] getEvanTutorial(String data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.NPC_TALK.getValue());

        mplew.write(8);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.write(1);
        mplew.write(1);
        mplew.writeMapleAsciiString(data);

        return mplew.getPacket();
    }

    public static byte[] showEventInstructions() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.GMEVENT_INSTRUCTIONS.getValue());
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] getOwlOpen() { //best items! hardcoded
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(7);
        List<Integer> ii = MinervaOwlSearchTop.getInstance().getMostSearched();
        mplew.write(ii.size());
        for (int i : ii) {
            mplew.writeInt(i);
        } //these are the most searched items. too lazy to actually make
        return mplew.getPacket();
    }

    public static byte[] getOwlSearched(final int itemSearch, final List<AbstractPlayerStore> hms) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(6);
        mplew.writeInt(0);
        mplew.writeInt(itemSearch);
        int size = 0;

        for (AbstractPlayerStore hm : hms) {
            List<MaplePlayerShopItem> items = null;
            if (hm instanceof HiredMerchant) {
                items = ((HiredMerchant) hm).searchItem(itemSearch);
            }
            if (hm instanceof MaplePlayerShop) {
                items = ((MaplePlayerShop) hm).searchItem(itemSearch);
            }
            if (items != null) {
                size += items.size();
            }
        }
        mplew.writeInt(size);
        for (AbstractPlayerStore hm : hms) {
            List<MaplePlayerShopItem> items = null;
            if (hm instanceof HiredMerchant) {
                items = ((HiredMerchant) hm).searchItem(itemSearch);
            }
            if (hm instanceof MaplePlayerShop) {
                items = ((MaplePlayerShop) hm).searchItem(itemSearch);
            }
            for (MaplePlayerShopItem item : items) {
                mplew.writeMapleAsciiString(hm.getOwnerName());
                mplew.writeInt(hm.getMap().getId());
                mplew.writeMapleAsciiString(hm.getDescription());
                mplew.writeInt(item.item.getQuantity()); //I THINK.
                mplew.writeInt(item.bundles); //I THINK.
                mplew.writeInt(item.price);
                switch (InventoryHandler.OWL_ID) {
                    case 0:
                        mplew.writeInt(hm.getOwnerId()); //store ID
                        break;
                    case 1:
                        if (hm instanceof HiredMerchant) {
                            mplew.writeInt(((HiredMerchant) hm).getStoreId()); //store ID
                        } else if (hm instanceof MaplePlayerShop) {
                            mplew.writeInt(((MaplePlayerShop) hm).getObjectId()); //store ID
                        }
                        break;
                    default:
                        mplew.writeInt(hm.getObjectId());
                        break;
                }
                mplew.write(hm.getChannel() - 1); //chid
                mplew.write(GameConstants.getInventoryType(itemSearch).getType()); //position?
                if (GameConstants.getInventoryType(itemSearch) == MapleInventoryType.EQUIP) {
                    PacketHelper.addItemInfo(mplew, item.item, true, true);
                }
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getRPSMode(byte mode, int mesos, int selection, int answer) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.RPS_GAME.getValue());
        mplew.write(mode);
        switch (mode) {
            case 6: { //not enough mesos
                if (mesos != -1) {
                    mplew.writeInt(mesos);
                }
                break;
            }
            case 8: { //open (npc)
                mplew.writeInt(9000019);
                break;
            }
            case 11: { //selection vs answer
                mplew.write(selection);
                mplew.write(answer); // FF = lose, or if selection = answer then lose ???
                break;
            }
        }
        return mplew.getPacket();
    }

    public static final byte[] getSlotUpdate(byte invType, byte newSlots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_INVENTORY_SLOT.getValue());
        mplew.write(invType);
        mplew.write(newSlots);
        return mplew.getPacket();
    }

    public static byte[] followRequest(int chrid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.FOLLOW_REQUEST.getValue());
        mplew.writeInt(chrid);
        return mplew.getPacket();
    }

    public static byte[] followEffect(int initiator, int replier, Point toMap) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.FOLLOW_EFFECT.getValue());
        mplew.writeInt(initiator);
        mplew.writeInt(replier);
        if (replier == 0) { //cancel
            mplew.write(toMap == null ? 0 : 1); //1 -> x (int) y (int) to change map
            if (toMap != null) {
                mplew.writeInt(toMap.x);
                mplew.writeInt(toMap.y);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getFollowMsg(int opcode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.FOLLOW_MSG.getValue());
        mplew.writeLong(opcode); //5 = canceled request.
        return mplew.getPacket();
    }

    public static byte[] moveFollow(Point otherStart, Point myStart, Point otherEnd, List<LifeMovementFragment> moves) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.FOLLOW_MOVE.getValue());
        mplew.writePos(otherStart);
        mplew.writePos(myStart);
        PacketHelper.serializeMovementList(mplew, moves);
        mplew.write(0x11); //what? could relate to movePlayer
        for (int i = 0; i < 8; i++) {
            mplew.write(0); //?? sometimes 0x44 sometimes 0x88 sometimes 0x4.. etc.. buffstat or what
        }
        mplew.write(0); //?
        mplew.writePos(otherEnd);
        mplew.writePos(otherStart);

        return mplew.getPacket();
    }

    public static final byte[] getFollowMessage(final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPOUSE_MESSAGE.getValue());
        mplew.writeShort(0x0B); //?
        mplew.writeMapleAsciiString(msg); //white in gms, but msea just makes it pink.. waste
        return mplew.getPacket();
    }

    public static final byte[] getNodeProperties(final MapleMonster objectid, final MapleMap map) {
        //idk.
        if (objectid.getNodePacket() != null) {
            return objectid.getNodePacket();
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MONSTER_PROPERTIES.getValue());
        mplew.writeInt(objectid.getObjectId()); //?
        mplew.writeInt(map.getNodes().size());
        mplew.writeInt(objectid.getPosition().x);
        mplew.writeInt(objectid.getPosition().y);
        for (MapleNodeInfo mni : map.getNodes()) {
            mplew.writeInt(mni.x);
            mplew.writeInt(mni.y);
            mplew.writeInt(mni.attr);
            if (mni.attr == 2) { //msg
                mplew.writeInt(500); //? talkMonster
            }
        }
        mplew.writeZeroBytes(6);
        objectid.setNodePacket(mplew.getPacket());
        return objectid.getNodePacket();
    }

    public static final byte[] getMovingPlatforms(final MapleMap map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MOVE_PLATFORM.getValue());
        mplew.writeInt(map.getPlatforms().size());
        for (MaplePlatform mp : map.getPlatforms()) {
            mplew.writeMapleAsciiString(mp.name);
            mplew.writeInt(mp.start);
            mplew.writeInt(mp.SN.size());
            for (int x = 0; x < mp.SN.size(); x++) {
                mplew.writeInt(mp.SN.get(x));
            }
            mplew.writeInt(mp.speed);
            mplew.writeInt(mp.x1);
            mplew.writeInt(mp.x2);
            mplew.writeInt(mp.y1);
            mplew.writeInt(mp.y2);
            mplew.writeInt(mp.x1);//?
            mplew.writeInt(mp.y1);
            mplew.writeShort(mp.r);
        }
        return mplew.getPacket();
    }

    public static final byte[] getUpdateEnvironment(final MapleMap map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_ENV.getValue());
        mplew.writeInt(map.getEnvironment().size());
        for (Entry<String, Integer> mp : map.getEnvironment().entrySet()) {
            mplew.writeMapleAsciiString(mp.getKey());
            mplew.writeInt(mp.getValue());
        }
        return mplew.getPacket();
    }

    public static byte[] sendEngagementRequest(String name, int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ENGAGE_REQUEST.getValue());
        mplew.write(0); //mode, 0 = engage, 1 = cancel, 2 = answer.. etc
        mplew.writeMapleAsciiString(name); // name
        mplew.writeInt(cid); // playerid
        return mplew.getPacket();
    }

    /**
     * @param type - (0:Light&Long 1:Heavy&Short)
     * @param delay - seconds
     * @return
     */
    public static byte[] trembleEffect(int type, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(1);
        mplew.write(type);
        mplew.writeInt(delay);
        return mplew.getPacket();
    }

    public static byte[] sendEngagement(final byte msg, final int item, final MapleCharacter male, final MapleCharacter female) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        // 0B = Engagement has been concluded.
        // 0D = The engagement is cancelled.
        // 0E = The divorce is concluded.
        // 10 = The marriage reservation has been successsfully made.
        // 12 = Wrong character name
        // 13 = The party in not in the same map.
        // 14 = Your inventory is full. Please empty your E.T.C window.
        // 15 = The person's inventory is full.
        // 16 = The person cannot be of the same gender.
        // 17 = You are already engaged.
        // 18 = The person is already engaged.
        // 19 = You are already married.
        // 1A = The person is already married.
        // 1B = You are not allowed to propose.
        // 1C = The person is not allowed to be proposed to.
        // 1D = Unfortunately, the one who proposed to you has cancelled his proprosal.
        // 1E = The person had declined the proposal with thanks.
        // 1F = The reservation has been cancelled. Try again later.
        // 20 = You cannot cancel the wedding after reservation.
        // 22 = The invitation card is ineffective.
        mplew.writeOpcode(SendPacketOpcode.ENGAGE_RESULT.getValue());
        mplew.write(msg); // 1103 custom quest
        switch (msg) {
            case 11: {
                mplew.writeInt(male.getMarriageId()); // marriageid
                mplew.writeInt(male.getId());
                mplew.writeInt(female.getId());
                mplew.writeShort(1); //always
                mplew.writeInt(item);
                mplew.writeInt(item); // wtf?repeat?
                mplew.writeAsciiString(male.getName(), 13);
                mplew.writeAsciiString(female.getName(), 13);
                break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] playerDamaged(int cid, int dmg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PLAYER_DAMAGED.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(dmg);

        return mplew.getPacket();
    }

    public static byte[] pamsSongEffect(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PAMS_SONG.getValue());
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] pamsSongUI() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PAMS_SONG.getValue());
        mplew.writeShort(0); //doesn't seem to change it

        return mplew.getPacket();
    }

    public static byte[] englishQuizMsg(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ENGLISH_QUIZ.getValue());
        mplew.writeInt(20); //?
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] report(int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REPORT.getValue());
        mplew.write(err); //0 = success
        return mplew.getPacket();
    }

    public static byte[] enableReport() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ENABLE_REPORT.getValue());
        mplew.write(1);
        return mplew.getPacket();
    }

    public static byte[] ultimateExplorer() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ULTIMATE_EXPLORER.getValue());

        return mplew.getPacket();
    }

    public static byte[] GMPoliceMessage() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GM_POLICE.getValue());
        mplew.writeInt(0); //no clue
        return mplew.getPacket();
    }

    public static byte[] pamSongUI() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PAM_SONG.getValue());
        mplew.writeInt(0); //no clue
        return mplew.getPacket();
    }

    public static byte[] dragonBlink(int portalId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DRAGON_BLINK.getValue());
        mplew.write(portalId);
        return mplew.getPacket();
    }

    public static byte[] harvestMessage(int oid, int msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.HARVEST_MESSAGE.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(msg);
        return mplew.getPacket();
    }

    public static byte[] showHarvesting(int cid, int tool) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_HARVEST.getValue());
        mplew.writeInt(cid);
        if (tool > 0) {
            mplew.write(1);
            mplew.writeInt(tool);
        } else {
            mplew.write(0);
        }
        return mplew.getPacket();
    }

    public static byte[] harvestResult(int cid, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.HARVESTED.getValue());
        mplew.writeInt(cid);
        mplew.write(success ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] makeExtractor(int cid, String cname, Point pos, int timeLeft, int itemId, int fee) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_EXTRACTOR.getValue());
        mplew.writeInt(cid);
        mplew.writeMapleAsciiString(cname);
        mplew.writeInt(pos.x);
        mplew.writeInt(pos.y);
        mplew.writeShort(timeLeft); //fh or time left, dunno
        mplew.writeInt(itemId); //3049000, 3049001...
        mplew.writeInt(fee);
        return mplew.getPacket();
    }

    public static byte[] removeExtractor(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_EXTRACTOR.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(1); //probably 1 = animation, 2 = make something?
        return mplew.getPacket();
    }

    public static byte[] spouseMessage(String msg, boolean white) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPOUSE_MESSAGE.getValue());
        mplew.writeShort(white ? 10 : 6); //12 = the blue message thing, 7/8 = yellow
        mplew.writeMapleAsciiString(msg);
        return mplew.getPacket();
    }

    public static byte[] openBag(int index, int itemId, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.OPEN_BAG.getValue());
        mplew.writeInt(index);
        mplew.writeInt(itemId);
        mplew.writeShort(firstTime ? 1 : 0); //this might actually be 2 bytes
        return mplew.getPacket();
    }

    public static byte[] showOwnCraftingEffect(String effect, int time, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(0x1E);
        mplew.writeMapleAsciiString(effect);
        mplew.writeInt(time);
        mplew.writeInt(mode);

        return mplew.getPacket();
    }

    public static byte[] showCraftingEffect(int cid, String effect, int time, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(0x1E);
        mplew.writeMapleAsciiString(effect);
        mplew.writeInt(time);
        mplew.writeInt(mode);

        return mplew.getPacket();
    }

    public static byte[] craftMake(int cid, int something, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CRAFT_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(something);
        mplew.writeInt(time);
        return mplew.getPacket();
    }

    public static byte[] craftFinished(int cid, int craftID, int ranking, int itemId, int quantity, int exp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CRAFT_COMPLETE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(craftID);
        mplew.writeInt(ranking);
        mplew.writeInt(itemId);
        mplew.writeInt(quantity);
        mplew.writeInt(exp);
        return mplew.getPacket();
    }

    public static byte[] shopDiscount(int percent) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOP_DISCOUNT.getValue());
        mplew.write(percent);
        return mplew.getPacket();
    }

    public static byte[] changeCardSet(int set) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CARD_SET.getValue());
        mplew.writeInt(set);
        return mplew.getPacket();
    }

    public static byte[] getCard(int itemid, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GET_CARD.getValue());
        mplew.write(itemid > 0 ? 1 : 0);
        if (itemid > 0) {
            mplew.writeInt(itemid);
            mplew.writeInt(level);
        }
        return mplew.getPacket();
    }

    public static byte[] upgradeBook(Item book, MapleCharacter chr) { //slot -55
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BOOK_STATS.getValue());
        mplew.writeInt(book.getPosition()); //negative or not
        PacketHelper.addItemInfo(mplew, book, true, true, false, false, chr);
        return mplew.getPacket();
    }

    public static byte[] pendantSlot(boolean p) { //slot -59
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PENDANT_SLOT.getValue());
        mplew.write(p ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] getBuffBar(long millis) { //You can use the buff again _ seconds later. + bar above head
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BUFF_BAR.getValue());
        mplew.writeLong(millis);
        return mplew.getPacket();
    }

    // Makes any NPC in the game scriptable.
    //
    // @param npcId - The NPC's ID, found in WZ files/MCDB
    // @param description - If the NPC has quests, this will be the text of the
    // menu item
    // @return
    public static byte[] setNPCScriptable(List<Pair<Integer, String>> npcs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.NPC_SCRIPTABLE.getValue());
        mplew.write(npcs.size());
        for (Pair<Integer, String> s : npcs) {
            mplew.writeInt(s.left);
            mplew.writeMapleAsciiString(s.right);
            mplew.writeInt(0); // start time
            mplew.writeInt(Integer.MAX_VALUE); // end time
        }
        return mplew.getPacket();
    }

    public static byte[] showMidMsg(String s, int l) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MID_MSG.getValue());
        mplew.write(l); //i think this is the line.. or soemthing like that. 1 = lower than 0
        mplew.writeMapleAsciiString(s);
        mplew.write(s.length() > 0 ? 0 : 1); //remove?
        return mplew.getPacket();
    }

    public static byte[] showMemberSearch(List<MapleCharacter> chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.MEMBER_SEARCH.getValue());
        mplew.write(chr.size());
        for (MapleCharacter c : chr) {
            mplew.writeInt(c.getId());
            mplew.writeMapleAsciiString(c.getName());
            mplew.writeShort(c.getJob());
            mplew.write(c.getLevel());
        }
        return mplew.getPacket();
    }

    public static byte[] showPartySearch(List<MapleParty> chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.PARTY_SEARCH.getValue());
        mplew.write(chr.size());
        for (MapleParty c : chr) {
            mplew.writeInt(c.getId());
            mplew.writeMapleAsciiString(c.getLeader().getName());
            mplew.write(c.getLeader().getLevel());
            mplew.write(c.getLeader().isOnline() ? 1 : 0);
            mplew.write(c.getMembers().size());
            for (MaplePartyCharacter ch : c.getMembers()) {
                mplew.writeInt(ch.getId());
                mplew.writeMapleAsciiString(ch.getName());
                mplew.writeShort(ch.getJobId());
                mplew.write(ch.getLevel());
                mplew.write(ch.isOnline() ? 1 : 0);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] showBackgroundEffect(String eff, int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.VISITOR.getValue());
        mplew.writeMapleAsciiString(eff); //"Visitor"
        mplew.write(value);
        return mplew.getPacket();
    }

    public static byte[] loadGuildName(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LOAD_GUILD_NAME.getValue());
        mplew.writeInt(chr.getId());

        if (chr.getGuildId() <= 0) {
            mplew.writeShort(0);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
            } else {
                mplew.writeShort(0);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] loadGuildIcon(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LOAD_GUILD_ICON.getValue());
        mplew.writeInt(chr.getId());

        if (chr.getGuildId() <= 0) {
            mplew.writeZeroBytes(6);
        } else {
            final MapleGuild gs = World.Guild.getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeShort(gs.getLogoBG());
                mplew.write(gs.getLogoBGColor());
                mplew.writeShort(gs.getLogo());
                mplew.write(gs.getLogoColor());
            } else {
                mplew.writeZeroBytes(6);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] updateGender(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.UPDATE_GENDER.getValue());
        mplew.write(chr.getGender());
        return mplew.getPacket();
    }

    public static byte[] achievementRatio(int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ACHIEVEMENT_RATIO.getValue()); //not sure
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] createUltimate(int amount) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CREATE_ULTIMATE.getValue());
        mplew.writeInt(amount); //2 = no slots, 1 = success, 0 = failed

        return mplew.getPacket();
    }

    public static byte[] professionInfo(String skil, int level1, int level2, int chance) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PROFESSION_INFO.getValue());
        mplew.writeMapleAsciiString(skil);
        mplew.writeInt(level1);
        mplew.writeInt(level2);
        mplew.write(1);
        mplew.writeInt(skil.startsWith("9200") || skil.startsWith("9201") ? 100 : chance); //100% chance

        return mplew.getPacket();
    }

    public static byte[] quickSlot(String skil) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.QUICK_SLOT.getValue());
        mplew.write(skil == null ? 0 : 1);
        if (skil != null) {
            for (int i = 0; i < skil.length(); i++) {
                mplew.writeAsciiString(skil.substring(i, i + 1));
                mplew.writeZeroBytes(3); //really hacky
            }
        }

        return mplew.getPacket();
    }

    public static final byte[] spawnFlags(List<Pair<String, Integer>> flags) { //Flag_R_1 to 0, etc
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LOGIN_WELCOME.getValue());
        mplew.write(flags == null ? 0 : flags.size());
        if (flags != null) {
            for (Pair<String, Integer> f : flags) {
                mplew.writeMapleAsciiString(f.left);
                mplew.write(f.right);
            }
        }

        return mplew.getPacket();
    }

    public static final byte[] getPVPScoreboard(List<Pair<Integer, MapleCharacter>> flags, int type) { //Flag_R_1 to 0, etc
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_SCOREBOARD.getValue());
        mplew.writeShort(flags.size());
        for (Pair<Integer, MapleCharacter> f : flags) {
            mplew.writeInt(f.right.getId());
            mplew.writeMapleAsciiString(f.right.getName());
            mplew.writeInt(f.left);
            mplew.write(type == 0 ? 0 : (f.right.getTeam() + 1));
        }

        return mplew.getPacket();
    }

    public static final byte[] showStatusMessage(final String info, final String data) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x16);
        mplew.writeMapleAsciiString(info); //name got Shield.
        mplew.writeMapleAsciiString(data); //Shield applied to name.

        return mplew.getPacket();
    }

    public static final byte[] enablePVP(final boolean enabled) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_ENABLED.getValue());
        mplew.write(enabled ? 1 : 2);

        return mplew.getPacket();
    }

    public static final byte[] getPVPMode(final int mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_MODE.getValue());
        mplew.write(mode); //11 = starting, 0 = started, 4 = ended??? 8 = blue team win???

        return mplew.getPacket();
    }

    public static final byte[] getPVPTeam(List<Pair<Integer, String>> players) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_TEAM.getValue());
        mplew.writeInt(players.size());
        for (Pair<Integer, String> pl : players) {
            mplew.writeInt(pl.left);
            mplew.writeMapleAsciiString(pl.right);
            mplew.writeShort(2660); //?
        }

        return mplew.getPacket();
    }

    public static final byte[] getPVPScore(int score, boolean kill) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_SCORE.getValue());
        mplew.writeInt(score);
        mplew.write(kill ? 1 : 0);

        return mplew.getPacket();
    }

    public static final byte[] getPVPIceGage(int score) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_ICEGAGE.getValue());
        mplew.writeInt(score);

        return mplew.getPacket();
    }

    public static final byte[] getPVPKilled(String lastWords) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_KILLED.getValue());
        mplew.writeMapleAsciiString(lastWords); //____ defeated ____.

        return mplew.getPacket();
    }

    public static final byte[] getPVPPoints(int p1, int p2) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_POINTS.getValue());

        mplew.writeInt(p1);
        mplew.writeInt(p2);

        return mplew.getPacket();
    }

    public static final byte[] getPVPHPBar(int cid, int hp, int maxHp) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_HP.getValue());

        mplew.writeInt(cid);
        mplew.writeInt(hp);
        mplew.writeInt(maxHp);

        return mplew.getPacket();
    }

    public static final byte[] getPVPIceHPBar(int hp, int maxHp) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_ICEKNIGHT.getValue());

        mplew.writeInt(hp);
        mplew.writeInt(maxHp);

        return mplew.getPacket();
    }

    public static final byte[] getPVPMist(int cid, int mistSkill, int mistLevel, int damage) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_MIST.getValue());
        //DOT
        mplew.writeInt(cid);
        mplew.writeInt(mistSkill);
        mplew.write(mistLevel);
        mplew.writeInt(damage);
        mplew.write(8); //skill delay
        mplew.writeInt(1000);

        return mplew.getPacket();
    }

    public static final byte[] getCaptureFlags(MapleMap map) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CAPTURE_FLAGS.getValue());
        mplew.writeRect(map.getArea(0));
        mplew.writeInt(map.getGuardians().get(0).left.x);
        mplew.writeInt(map.getGuardians().get(0).left.y);
        mplew.writeRect(map.getArea(1));
        mplew.writeInt(map.getGuardians().get(1).left.x);
        mplew.writeInt(map.getGuardians().get(1).left.y);
        return mplew.getPacket();
    }

    public static final byte[] getCapturePosition(MapleMap map) { //position of flags if they are still at base
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        final Point p1 = map.getPointOfItem(2910000);
        final Point p2 = map.getPointOfItem(2910001);
        mplew.writeOpcode(SendPacketOpcode.CAPTURE_POSITION.getValue());
        mplew.write(p1 == null ? 0 : 1);
        if (p1 != null) {
            mplew.writeInt(p1.x);
            mplew.writeInt(p1.y);
        }
        mplew.write(p2 == null ? 0 : 1);
        if (p2 != null) {
            mplew.writeInt(p2.x);
            mplew.writeInt(p2.y);
        }

        return mplew.getPacket();
    }

    public static final byte[] resetCapture() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CAPTURE_RESET.getValue());

        return mplew.getPacket();
    }

    public static final byte[] pvpAttack(int cid, int playerLevel, int skill, int skillLevel, int speed, int mastery, int projectile, int attackCount, int chargeTime, int stance, int direction, int range, int linkSkill, int linkSkillLevel, boolean movementSkill, boolean pushTarget, boolean pullTarget, List<AttackPair> attack) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_ATTACK.getValue());
        mplew.writeInt(cid);
        mplew.write(playerLevel);
        mplew.writeInt(skill);
        mplew.write(skillLevel);
        mplew.writeInt(linkSkill != skill ? linkSkill : 0);
        mplew.write(linkSkillLevel != skillLevel ? linkSkillLevel : 0);
        mplew.write(direction);
        mplew.write(movementSkill ? 1 : 0);
        mplew.write(pushTarget ? 1 : 0);
        mplew.write(pullTarget ? 1 : 0); //afaik only chains of hell does chains
        mplew.write(0); //unk
        mplew.writeShort(stance); //display
        mplew.write(speed);
        mplew.write(mastery);
        mplew.writeInt(projectile);
        mplew.writeInt(chargeTime);
        mplew.writeInt(range);
        mplew.writeShort(attack.size());
        mplew.write(attackCount);
        mplew.write(0); //idk: probably does something like immobilize target
        for (AttackPair p : attack) {
            mplew.writeInt(p.objectid);
            mplew.writePos(p.point);
            mplew.writeZeroBytes(5);
            for (Pair<Integer, Boolean> atk : p.attack) {
                mplew.writeInt(atk.left);
                mplew.write(atk.right ? 1 : 0);
                mplew.writeShort(0); //1 = no hit
            }
        }

        return mplew.getPacket();
    }

    public static final byte[] pvpSummonAttack(int cid, int playerLevel, int oid, int animation, Point pos, List<AttackPair> attack) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_SUMMON.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(oid);
        mplew.write(playerLevel);
        mplew.write(animation);
        mplew.writePos(pos);
        mplew.writeInt(0); //<-- delay
        mplew.write(attack.size());
        for (AttackPair p : attack) {
            mplew.writeInt(p.objectid);
            mplew.writePos(p.point);
            mplew.writeShort(p.attack.size());
            for (Pair<Integer, Boolean> atk : p.attack) {
                mplew.writeInt(atk.left);
            }
        }

        return mplew.getPacket();
    }

    public static final byte[] pvpCool(int cid, List<Integer> attack) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_COOL.getValue());
        mplew.writeInt(cid);
        mplew.write(attack.size());
        for (int b : attack) {
            mplew.writeInt(b);
        }
        return mplew.getPacket();
    }

    public static byte[] getPVPClock(int type, int time) { // time in seconds
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CLOCK.getValue());
        mplew.write(3);
        mplew.write(type);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getPVPTransform(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PVP_TRANSFORM.getValue());
        mplew.write(type); //2?

        return mplew.getPacket();
    }

    public static byte[] changeTeam(int cid, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LOAD_TEAM.getValue());
        mplew.writeInt(cid);
        mplew.write(type); //2?

        return mplew.getPacket();
    }

    public static byte[] getPublicNPCInfo() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PUBLIC_NPC.getValue());
        mplew.write(0);
        for (int i = 0; i < GameConstants.publicNpcIds.length; i++) {
            mplew.writeInt(GameConstants.publicNpcIds[i]);
            mplew.writeLong(i); //0, level needed
            mplew.writeMapleAsciiString(GameConstants.publicNpcs[i]);
            mplew.writeShort(0);
        }

        return mplew.getPacket();
    }

    public static byte[] gainForce(int oid, int gain, int max) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(1);
        mplew.writeInt(oid);
        mplew.write(1);
        mplew.writeInt(gain); //total
        mplew.writeInt(max); //gained
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showItemLevelupEffect() {
        return showSpecialEffect(17); //bb +2
    }

    public static byte[] showForeignItemLevelupEffect(int cid) {
        return showSpecialEffect(cid, 17); //bb +2
    }

    public static byte[] showNpcSpecialAction(int npcoid, String str) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.NPC_SPECIAL_ACTION.getValue());
        mplew.writeInt(npcoid);
        mplew.writeMapleAsciiString(str);
        return mplew.getPacket();
    }

    //WEDDING_GIFT = 0x39
//NOTIFY_MARRIED_PARTNER_MAP_TRANSFER = 0x3A
//HOUR_CHANGED = 0x43
//MINIMAP_ON_OFF = 0x44
    public static byte[] showWeddingWishInputDialog() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ENGAGE_REQUEST.getValue());
        mplew.write(9);
        return mplew.getPacket();
    }

    // 15 : 청첩장
    // 22 : 이 초대장은 유효하지 않습니다.
    public static byte[] showWeddingInvitation(String groom, String bride, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ENGAGE_RESULT.getValue());
        mplew.write(15);
        mplew.writeMapleAsciiString(groom);
        mplew.writeMapleAsciiString(bride);
        mplew.writeShort(type); //wedding type  0 : 조촐한, 1 : 스위티, 2 : 프리미엄
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishGiveDialog(List<String> wishes) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(9);
        mplew.write(wishes.size());
        for (String s : wishes) {
            mplew.writeMapleAsciiString(s);
        }
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishGiveToServerResult(List<String> wishes, MapleInventoryType type, Item item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(11);
        mplew.write(wishes.size());
        for (String s : wishes) {
            mplew.writeMapleAsciiString(s);
        }
        mplew.writeShort(type.getBitfieldEncoding());
        mplew.writeZeroBytes(6);
        mplew.write(1); //equip
        PacketHelper.addItemInfo(mplew, item, true, true);
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishRecvDialog(Collection<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(10);
        mplew.writeLong(0x7E);
        mplew.write(items.size()); //equip
        for (Item i : items) {
            PacketHelper.addItemInfo(mplew, i, true, true);
        }
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static byte[] showWeddingWishRecvToLocalResult(Collection<Item> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(15);
        mplew.writeLong(0x7E);
        mplew.write(items.size()); //equip
        for (Item i : items) {
            PacketHelper.addItemInfo(mplew, i, true, true);
        }
        mplew.writeInt(0);
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    // 아마 오류메시지를 dropMessage 1번으로 표시하고 이 패킷을 보내면 될 듯 하다.
    public static byte[] showWeddingWishRecvDisableHang() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.WEDDING_GIFT.getValue());
        mplew.write(16);
        return mplew.getPacket();
    }

    public static byte[] showAriantArenaUserScore(List<Pair<String, Integer>> scores) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ARIANT_SCORE.getValue());
        mplew.write(scores.size());
        for (Pair<String, Integer> p : scores) {
            mplew.writeMapleAsciiString(p.getLeft());
            mplew.writeInt(p.getRight());
        }
        return mplew.getPacket();
    }

    public static byte[] showAriantArenaResult() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ARIANT_ARIANT_SCOREBOARD.getValue());
        return mplew.getPacket();
    }

    public static byte[] throwGrenadeResult(int cid, int x, int y, int charge, int skill, int level) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.THROW_GRENADE_RESULT.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.writeInt(charge);
        mplew.writeInt(skill);
        mplew.writeInt(level);
        return mplew.getPacket();
    }

    public static byte[] showbomb(int cid, int oid, int x, int y) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(7);
        mplew.writeInt(4341003);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.writeInt(x);
        mplew.writeInt(x);

        return mplew.getPacket();
    }

    public static byte[] getTimeBombAttack(Point a, int skillid, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_BOMB.getValue());
        mplew.writeInt(skillid);
        mplew.writeInt((int) a.x);
        mplew.writeInt((int) a.y);
        mplew.writeInt(damage > 0 ? 1 : 0);
        mplew.writeInt(damage);//데미지

        return mplew.getPacket();
    }

    public static byte[] showWheelEffect(int cid, byte[] proxy) { //오딘이라면 이런 이름이었을듯
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SHOW_WHEEL.getValue());
        mplew.writeInt(cid);
        mplew.write(proxy);
        return mplew.getPacket();
    }

    public static byte[] getLieDetector(byte type, String tester, int size) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LIE_DETECTOR.getValue()); // 2A 00 01 00 00 00  
        mplew.write(type); // 1 = not attacking, 2 = tested, 3 = going through, 4 save screenshot 
        switch (type) {
            case 4: //save screen shot 
                mplew.write(0);
                mplew.writeMapleAsciiString(""); // file name 
                break;
            case 5:
                mplew.write(1); // 2 = save screen shot 
                mplew.writeMapleAsciiString(tester); // me or file name 
                break;
            case 6:
                mplew.write(4); // 2 or anything else, 2 = with maple admin picture, basicaly manager's skill? 
                mplew.write(1); // if > 0, then time = 60,000..maybe try < 0? 
                mplew.writeInt(size);
                mplew.write(type); // bytes 
                break;
            case 7://send this if failed 
                // 2 = You have been appointed as a auto BOT program user and will be restrained. 
                mplew.write(4); // default 
                break;
            case 9:
                // 0 = passed lie detector test 
                // 1 = reward 5000 mesos for not botting. 
                // 2 = thank you for your cooperation with administrator. 
                mplew.write(0);
                break;
            case 8: // save screen shot.. it appears that you may be using a macro-assisted program 
                mplew.write(0); // 2 or anything else , 2 = show msg, 0 = none 
                mplew.writeMapleAsciiString(""); // file name 
                break;
            case 10: // no save 
                mplew.write(0); // 2 or anything else, 2 = show msg 
                mplew.writeMapleAsciiString(""); // ?? // hi_You have passed the lie detector test 
                break;
            default:
                mplew.write(0);
                break;
        }
        return mplew.getPacket();
    }

    public static byte[] sendLieDetector(final byte[] image, boolean item) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LIE_DETECTOR.getValue());
        mplew.write(6);

        mplew.write(item ? 4 : 2);
        mplew.write(1);
        if (image == null) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(image.length);
        mplew.write(image);

        return mplew.getPacket();
    }

    public static byte[] LieDetectorResponse(final byte msg) {
        return LieDetectorResponse(msg, (byte) 0);
    }

    public static byte[] LieDetectorResponse(final byte msg1, final byte msg2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.LIE_DETECTOR.getValue());
        mplew.write(msg1); // 1 = not attacking, 2 = tested, 3 = going through 
        mplew.write(msg2);
        //msg1은 7번과 9번으로 나뉨

        return mplew.getPacket();
    }

    public static byte[] showGainNx(int amount, byte quantity) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(5);
        mplew.write(0);
        //   mplew.writeMapleAsciiString("캐시를 "+amount+"만큼 얻었습니다.");
        if (quantity == 1) {
            mplew.writeMapleAsciiString("캐시를 얻었습니다 " + "(+" + amount + ")");
        } else if (quantity > 1) {
            mplew.writeMapleAsciiString("캐시를 얻었습니다 " + "(+" + amount + "(x" + quantity + "))");
        }
        mplew.writeInt(-1);

        return mplew.getPacket();
    }

    public static final byte[] showFlameEffect(final int cid) {//SHOW_FOREIGN_EFFECT
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.writeOpcode(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.write(0x0C);
        //mplew.writeMapleAsciiString("Effect/BasicEff/Flame/SquibEffect");
        mplew.writeMapleAsciiString("Effect/BasicEff/SkillBook/Failure");
        return mplew.getPacket();
    }

    public static final byte[] showFlameEffect2() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        // mplew.writeInt(cid);
        mplew.write(0x0C);
        mplew.writeMapleAsciiString("Effect/BasicEff/Flame/SquibEffect2");
        return mplew.getPacket();
    }

    /**
     * <code>limitMinutes</code>분 만큼 정량제를 표시하며, <code>status</code>에 따라 상황이
     * 달라집니다.
     *
     * @param status
     *
     * 0, 1 : 없음 2, 3, 6 : PC방 혜택 제공, 남은 정량제 시간 표시 4, 12, 13 : 접속 후 바로 팅긴 후 "PC방
     * 정량제 시간 만료로 게임이 종료 됩니다." 표시 5, 6, 7 : 접속 후 PC방 혜택 안내를 닫을시 팅긴 후 4와 같음
     *
     * 이 이상에 코드는 모두 중복
     *
     * @return packet.getPacket();
     */
    public static final byte[] enableInternetCafe(byte status, int lefttime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.CS_USE.getValue());
        mplew.write(status);
        mplew.writeInt(lefttime);
        return mplew.getPacket();
    }

    public static final byte[] APPLYVENGEANCE() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.APPLY_VENGEANCE.getValue());
        mplew.writeInt(3120010);
        return mplew.getPacket();
    }

    public static final byte[] APPLYEXJABLIN() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.APPLY_EXJABLIN.getValue());
        return mplew.getPacket();
    }

    public static byte[] spawnDragon(MapleDragon d) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.DRAGON_SPAWN.getValue());
        mplew.writeInt(d.getOwner());
        mplew.writeInt(d.getPosition().x);
        mplew.writeInt(d.getPosition().y);
        mplew.write(d.getStance()); //stance?
        mplew.writeShort(0);
        mplew.writeShort(d.getJobId());
        return mplew.getPacket();
    }

    public static byte[] removeDragon(int chrid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.DRAGON_REMOVE.getValue());
        mplew.writeInt(chrid);
        return mplew.getPacket();
    }

    public static byte[] moveDragon(MapleDragon d, Point startPos, List<LifeMovementFragment> moves) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DRAGON_MOVE.getValue()); //not sure
        mplew.writeInt(d.getOwner());
        mplew.writePos(startPos);
        mplew.writeInt(0);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static byte[] updateJaguar(MapleCharacter from) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.UPDATE_JAGUAR.getValue());
        PacketHelper.addJaguarInfo(mplew, from);

        return mplew.getPacket();
    }

    public static byte[] teslaTriangle(int cid, int sum1, int sum2, int sum3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.TESLA_TRIANGLE.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(sum1);
        mplew.writeInt(sum2);
        mplew.writeInt(sum3);
        return mplew.getPacket();
    }

    public static byte[] mechPortal(Point pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MECH_PORTAL.getValue());
        mplew.writePos(pos);
        return mplew.getPacket();
    }

    public static byte[] spawnMechDoor(MechDoor md, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MECH_DOOR_SPAWN.getValue());
        mplew.write(animated ? 0 : 1);
        mplew.writeInt(md.getOwnerId());
        mplew.writePos(md.getTruePosition());
        mplew.write(md.getId());
        mplew.writeInt(md.getPartyId());
        return mplew.getPacket();
    }

    public static byte[] removeMechDoor(MechDoor md, boolean animated) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MECH_DOOR_REMOVE.getValue());
        mplew.write(animated ? 0 : 0);
        mplew.writeInt(md.getOwnerId());
        mplew.write(md.getId());
        return mplew.getPacket();
    }

    public static byte[] useSPReset(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SP_RESET.getValue());
        mplew.write(1);
        mplew.writeInt(cid);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] showPQreward(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_PQ_REWARD.getValue());
        mplew.writeInt(cid);
        for (int k = 0; k < 6; k++) {
            mplew.write(k);
        }

        return mplew.getPacket();
    }

    public static byte[] recievePQrewardFail(byte celino) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(41);
        mplew.write(celino);

        return mplew.getPacket();
    }

    public static byte[] recievePQrewardSuccess(byte box, int itemid, boolean isPotential) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(42);//마지막에 오픈하는 패킷인듯
        for (int k = 0; k < 6; k++) {
            mplew.write(k);//상자넘버
            if (k == box) {
                mplew.writeInt(itemid);
                if (itemid / 1000000 == 1) {
                    mplew.write(isPotential ? 1 : 0);
                    mplew.writeShort(0);
                    mplew.writeShort(0);
                    mplew.writeShort(0);
                }
            } else {
                mplew.writeInt(-1);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] selectPQrewardSuccess(int cid, String name, byte box) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.PARTY_OPERATION.getValue());
        mplew.write(40);//선택해서 오픈하는 패킷
        mplew.writeInt(1);
        mplew.writeAsciiString(name);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static final byte[] OnDotDamageInfo(MapleClient client, int donno1, int nDotTime, boolean check, int donno3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ON_DOTDAMAGE_INFO.getValue());
        mplew.writeInt(donno1);
        mplew.writeInt(nDotTime); //nDotTime
        mplew.write(check ? 1 : 0);
        if (check) {
            mplew.writeInt(donno3);
        }

        return mplew.getPacket();
    }

    public static final byte[] calcRequestResult(MapleClient client, byte open) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CALC_REQUEST_RESULT.getValue());
        mplew.write(open);

        return mplew.getPacket();
    }

    public static final byte[] onChatMessage(short opcode, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ON_CHAT_MESSAGE.getValue());
        mplew.writeShort(opcode);
        mplew.writeMapleAsciiString(message);
        /*
        0 일챗
        1 귓
        2 파티
        3 친창
        4 길드
        5 연합
        6 어두움
        7 연노랑
        8 더 연노랑
        9 파랑
        10 영자
        11 공지 메시지
        12 일반 확성기
        13 고확 (튕김)
        14 아확 (튕김)
        15 보라색 희귀 메시지!!
        16 옛날 gms 가차폰 메시지
        17 옛날 gms 투명 고확임 ㄷㄷ (팅김)
        18 그냥 노랑
        19 하늘색 메시지임! ㄷㄷ
        20 부턴 존재하지 않음
         */

        return mplew.getPacket();
    }

    public static byte[] setQuestTime() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SET_QUEST_TIME.getValue());
        ModifiedQuestTime[] modifiedQuestTimes = ModifiedQuestTime.values();
        byte disablesize = 0;//83;
        mplew.write(modifiedQuestTimes.length + disablesize);
        //disable
//        for (int i = 0; i <= 24; ++i) {
//            try {
//                mplew.writeInt(50000 + i);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd/hh/mm");
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//            } catch (ParseException e) {
//            }
//        }
//        for (int i = 0; i <= 4; ++i) {
//            try {
//                mplew.writeInt(8167 + i);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd/hh/mm");
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//            } catch (ParseException e) {
//            }
//        }
//        for (int i = 0; i <= 3; ++i) {
//            try {
//                mplew.writeInt(8531 + i);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd/hh/mm");
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//            } catch (ParseException e) {
//            }
//        }
//        for (int i = 0; i <= 9; ++i) {
//            try {
//                mplew.writeInt(50100 + i);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd/hh/mm");
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//            } catch (ParseException e) {
//            }
//        }
//        for (int i = 0; i <= 33; ++i) {
//            try {
//                mplew.writeInt(4401 + i);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd/hh/mm");
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//            } catch (ParseException e) {
//            }
//        }
//        for (int i = 0; i <= 4; ++i) {
//            try {
//                mplew.writeInt(4526 + i);
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd/hh/mm");
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2020/12/18/00/00").getTime()));
//                mplew.writeLong(PacketHelper.getTime(sdf.parse("2099/06/01/00/00").getTime()));
//            } catch (ParseException e) {
//            }
//        }
        for (ModifiedQuestTime modifiedQuestTime : modifiedQuestTimes) {
            mplew.writeInt(modifiedQuestTime.getQuestID());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/M/dd/hh/mm");
            try {
                if (modifiedQuestTime.getStart() != null) {
                    mplew.writeLong(PacketHelper.getTime(sdf.parse(modifiedQuestTime.getStart()).getTime()));
                } else {
                    mplew.writeLong(0);
                }
                if (modifiedQuestTime.getEnd() != null) {
                    mplew.writeLong(PacketHelper.getTime(sdf.parse(modifiedQuestTime.getEnd()).getTime()));
                } else {
                    mplew.writeLong(0);
                }
            } catch (ParseException e) {
                System.err.println(String.format("Couldn't parse modified quest time [%d]", modifiedQuestTime.getQuestID()));
            }
        }

        return mplew.getPacket();
    }
}
