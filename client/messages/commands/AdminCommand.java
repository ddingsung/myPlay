package client.messages.commands;

import client.*;
import client.inventory.*;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import scripting.EventInstanceManager;
import scripting.EventManager;
import server.*;
import server.Timer.EventTimer;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.marriage.MarriageManager;
import server.shops.MinervaOwlSearchTop;
import tools.CPUSampler;
import tools.MaplePacketCreator;
import tools.StringUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Emilyx3
 */
public class AdminCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }

    public static class SpeakMega extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            World.Broadcast.broadcastSmega(MaplePacketCreator.serverNotice(3, victim == null ? c.getChannel() : victim.getClient().getChannel(), victim == null ? splitted[1] : victim.getName() + " : " + StringUtil.joinStringFrom(splitted, 2), true));
            return 1;
        }
    }

    public static class Speak extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "unable to find '" + splitted[1]);
                return 0;
            } else {
                victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGM(), 0));
            }
            return 1;
        }
    }

    public static class 스킬 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);

            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            c.getPlayer().changeSkillLevel(skill, level, masterlevel);
            return 1;
        }
    }

    public static class 단체버프 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "!버프 <메용, 메이플용사, 헤이, 헤이스트, 홀심, 홀리심볼, 피뻥, 하이퍼바디, 블레스, 쏜즈, 모두>");
                return 0;
            }
            if (splitted[1].contains("홀심") || splitted[1].contains("홀리심볼")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001002).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 홀리심볼");
                    }
                }
            } else if (splitted[1].contains("메용") || splitted[1].contains("메이플용사")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(1221000).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 일반 캐릭터의 메이플용사~");
                    }
                }
            } else if (splitted[1].contains("헤이") || splitted[1].contains("헤이스트")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001001).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 헤이스트");
                    }
                }
            } else if (splitted[1].contains("피뻥") || splitted[1].contains("하이퍼바디")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001008).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 일반 캐릭터의 하이퍼바디~");
                    }
                }
            } else if (splitted[1].contains("블레스")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001003).getEffect(1).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 블레스~");
                    }
                }
            } else if (splitted[1].contains("샾") || splitted[1].contains("샤프")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(3121002).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 샤프 아이즈~");
                    }
                }
            } else if (splitted[1].contains("배리어")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(21120007).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 콤보 배리어~");
                    }
                }
            } else if (splitted[1].contains("쏜즈")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(4341007).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 쏜즈 이펙트~");
                    }
                }
            } else if (splitted[1].contains("윈부")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(5121009).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 윈부~");
                    }
                }
            } else if (splitted[1].contains("스왈로우")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(33101006).getEffect(20).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 스왈로우~");
                    }
                }
            } else if (splitted[1].contains("다이스")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(5111007).getEffect(20).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 다이스~");
                    }
                }
            } else if (splitted[1].contains("오더스")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(1211011).getEffect(30).applyTo(chr, false);
                        chr.dropMessage(6, "[공지] 운영자의 오더스~");
                    }
                }
            } else if (splitted[1].contains("모두")) {
                for (ChannelServer ch : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                        SkillFactory.getSkill(9001002).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(9001001).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(9001003).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(9001008).getEffect(1).applyTo(chr, false);
                        SkillFactory.getSkill(1221000).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(3121002).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(21120007).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(4341007).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(5111007).getEffect(20).applyTo(chr, false);//다이스 
                        SkillFactory.getSkill(5121009).getEffect(30).applyTo(chr, false);
                        SkillFactory.getSkill(33101006).getEffect(20).applyTo(chr, false);
                        SkillFactory.getSkill(1211011).getEffect(30).applyTo(chr, false);//오더스
                        chr.dropMessage(1, "운영자의 버프타임~");
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "!버프 <헤이, 헤이스트, 홀심, 홀리심볼, 피뻥, 하이퍼바디, 블레스, 배리어, 윈부, 쏜즈, 스왈로우, 오더스, 모두>");
            }
            return 1;
        }
    }

    public static class 단체버프2 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(9001001).getEffect(1).applyTo(chr, false);
                }
            }
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(9001003).getEffect(1).applyTo(chr, false);
                }
            }
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(9001002).getEffect(1).applyTo(chr, false);
                }
            }
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    SkillFactory.getSkill(1121000).getEffect(20).applyTo(chr, false);
                }
            }
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(1, "운영자의 버프타임~"));
            return 1;
        }
    }

    public static class Fame extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "Syntax: !fame <player> <amount>");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            int fame = 0;
            try {
                fame = Integer.parseInt(splitted[2]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(6, "Invalid Number...");
                return 0;
            }
            if (victim != null && player.allowedToTarget(victim)) {
                victim.addFame(fame);
                victim.updateSingleStat(MapleStat.FAME, victim.getFame());
            }
            return 1;
        }
    }

    public static class SP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
            c.getSession().write(MaplePacketCreator.updateSp(c.getPlayer(), false));
            return 1;
        }
    }

    public static class 직업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법: !직업 직업코드");
                return 0;
            }
            if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[1])).length() == 0) {
                c.getPlayer().dropMessage(5, "직업코드가 올바르지 않습니다.");
                return 0;
            }
            c.getPlayer().changeJob(Integer.parseInt(splitted[1]));
            return 1;
        }
    }

    public static class 상점 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory shop = MapleShopFactory.getInstance();
            int shopId = Integer.parseInt(splitted[1]);
            if (shop.getShop(shopId) != null) {
                shop.getShop(shopId).sendShop(c);
            }
            return 1;
        }
    }

    public static class 레벨업 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().levelUp();
                c.getPlayer().setExp(0);
                c.getPlayer().updateSingleStat(MapleStat.EXP, c.getPlayer().getExp());
            } else {
                MapleCharacter player = null;
                for (int i = 1; i <= ChannelServer.getChannelCount(); i++) {
                    for (MapleCharacter other : ChannelServer.getInstance(i).getPlayerStorage().getAllCharacters()) {
                        if (other != null && other.getName().equals(splitted[1])) {
                            player = other;
                        }
                    }
                }
                if (player == null) {
                    return 0;
                }
                int max = 1;
                try {
                    max = Integer.parseInt(splitted[2]);
                } catch (Exception e) {
                    max = 1;
                }
                if (max > 30) {
                    max = 30;
                }
                if (max < 0) {
                    max = 1;
                }
                for (int i = 0; i < max; i++) {
                    player.levelUp();
                    player.setExp(0);
                    player.updateSingleStat(MapleStat.EXP, c.getPlayer().getExp());
                }
                c.getPlayer().dropMessage(5, player.getName() + " 님을 " + max + "번 레벨업 시켰습니다.");
            }
            return 1;
        }
    }

    public static class 경험치 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "사용법 : !경험치 수치");
                return 0;
            }
            if (c.getPlayer().getLevel() < 200) {
                c.getPlayer().gainExp(Integer.parseInt(splitted[1]), true, false, true);
            }
            return 1;
        }
    }

    public static class 아이템 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final int itemId = Integer.parseInt(splitted[1]);
            final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

            if (!c.getPlayer().isAdmin()) {
                for (int i : GameConstants.itemBlock) {
                    if (itemId == i) {
                        c.getPlayer().dropMessage(5, "해당 아이템은 현재 GM 레벨에서는 생성이 불가능합니다.");
                        return 0;
                    }
                }
            }
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, "존재하지 않는 아이템입니다.");
                return 0;
            } else {
                Item item;
                short flag = (short) ItemFlag.LOCK.getValue();

                if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    item = ii.getEquipById(itemId);
                } else {
                    item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                }
                if (!c.getPlayer().isSuperGM()) {
                    item.setFlag(flag);
                }
                //item.setOwner(c.getPlayer().getName());
                item.setGMLog(c.getPlayer().getName() + " !아이템 명령어로 생성된 아이템");
                if (GameConstants.isPet(itemId)) {
                    final int period = CommandProcessorUtil.getOptionalIntArg(splitted, 2, 90);
                    if (period > 0) {
                        item.setQuantity((short) 1);
                        item.setExpiration((long) (System.currentTimeMillis() + (long) ((long) period * 24 * 60 * 60 * 1000)));
                    }
                    final MaplePet pet = MaplePet.createPet(itemId, MapleInventoryIdentifier.getInstance());
                    if (pet != null) {
                        item.setPet(pet);
                    } else {
                        c.getPlayer().dropMessage(5, "펫 생성 실패");
                        return 0;
                    }
                }

                MapleInventoryManipulator.addbyItem(c, item);
            }
            return 1;
        }
    }

    public static class 레벨 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setLevel((short) (Short.parseShort(splitted[1])));
            c.getPlayer().levelUp();
            if (c.getPlayer().getExp() < 0) {
                c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false, true);
            }
            return 1;
        }
    }

    public static class StartAutoEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final EventManager em = c.getChannelServer().getEventSM().getEventManager("AutomatedEvent");
            if (em != null) {
                em.scheduleRandomEvent();
            }
            return 1;
        }
    }

    public static class SetEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleEvent.onStartEvent(c.getPlayer());
            return 1;
        }
    }

    public static class 이벤트시작 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getChannelServer().getEvent() == c.getPlayer().getMapId()) {
                MapleEvent.setEvent(c.getChannelServer(), false);
                c.getPlayer().dropMessage(5, "Started the event and closed off");
                return 1;
            } else {
                c.getPlayer().dropMessage(5, "!이벤트예약 must've been done first, and you must be in the event map.");
                return 0;
            }
        }
    }

    public static class 이벤트예약 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleEventType type = MapleEventType.getByString(splitted[1]);
            if (type == null) {
                final StringBuilder sb = new StringBuilder("Wrong syntax: ");
                for (MapleEventType t : MapleEventType.values()) {
                    sb.append(t.name()).append(",");
                }
                c.getPlayer().dropMessage(5, sb.toString().substring(0, sb.toString().length() - 1));
                return 0;
            }
            final String msg = MapleEvent.scheduleEvent(type, c.getChannelServer());
            if (msg.length() > 0) {
                c.getPlayer().dropMessage(5, msg);
                return 0;
            }
            return 1;
        }
    }

    public static class 아이템제거 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "사용법 : !아이템제거 <캐릭터명> <아이템코드>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 캐릭터입니다.");
                return 0;
            }
            chr.removeAll(Integer.parseInt(splitted[2]), false);
            c.getPlayer().dropMessage(6, splitted[1] + "가 가진 모든 " + splitted[2] + "번 아이템이 제거되었습니다.");
            return 1;

        }
    }

    public static class LockItem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr == null) {
                c.getPlayer().dropMessage(6, "존재하지 않는 캐릭터입니다.");
                return 0;
            }
            int itemid = Integer.parseInt(splitted[2]);
            MapleInventoryType type = GameConstants.getInventoryType(itemid);
            for (Item item : chr.getInventory(type).listById(itemid)) {
                item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                chr.getClient().getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType(), item.getPosition(), true, chr));
            }
            if (type == MapleInventoryType.EQUIP) {
                type = MapleInventoryType.EQUIPPED;
                for (Item item : chr.getInventory(type).listById(itemid)) {
                    item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                    //chr.getClient().getSession().write(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                }
            }
            c.getPlayer().dropMessage(6, "All items with the ID " + splitted[2] + " has been locked from the inventory of " + splitted[1] + ".");
            return 1;
        }
    }

    public static class KillMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter map : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (map != null && !map.isGM()) {
                    map.getStat().setHp((short) 0, map);
                    map.getStat().setMp((short) 0, map);
                    map.updateSingleStat(MapleStat.HP, 0);
                    map.updateSingleStat(MapleStat.MP, 0);
                }
            }
            return 1;
        }
    }

    public static class 디버프 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "!디버프 <type> [charname] <level> where type = 봉인/암흑/허약/기절/저주/중독/슬로우/유혹/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            int type = 0;
            if (splitted[1].equalsIgnoreCase("봉인")) {
                type = 120;
            } else if (splitted[1].equalsIgnoreCase("암흑")) {
                type = 121;
            } else if (splitted[1].equalsIgnoreCase("허약")) {
                type = 122;
            } else if (splitted[1].equalsIgnoreCase("기절")) {
                type = 123;
            } else if (splitted[1].equalsIgnoreCase("저주")) {
                type = 124;
            } else if (splitted[1].equalsIgnoreCase("중독")) {
                type = 125;
            } else if (splitted[1].equalsIgnoreCase("슬로우")) {
                type = 126;
            } else if (splitted[1].equalsIgnoreCase("유혹")) {
                type = 128;
            } else if (splitted[1].equalsIgnoreCase("REVERSE")) {
                type = 132;
            } else if (splitted[1].equalsIgnoreCase("ZOMBIFY")) {
                type = 133;
            } else if (splitted[1].equalsIgnoreCase("POTION")) {
                type = 134;
            } else if (splitted[1].equalsIgnoreCase("SHADOW")) {
                type = 135;
            } else if (splitted[1].equalsIgnoreCase("BLIND")) {
                type = 136;
            } else if (splitted[1].equalsIgnoreCase("FREEZE")) {
                type = 137;
            } else if (splitted[1].equalsIgnoreCase("POTENTIAL")) {
                type = 138;
            } else {
                c.getPlayer().dropMessage(6, "!디버프 <type> [charname] <level> where type = 봉인/암흑/허약/기절/저주/중독/슬로우/유혹/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                return 0;
            }
            if (splitted.length == 4) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                if (victim == null) {
                    c.getPlayer().dropMessage(5, "Not found.");
                    return 0;
                }
                victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
            } else {
                for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                    victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
                }
            }
            return 1;
        }
    }

    public static class SetInstanceProperty extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                em.setProperty(splitted[2], splitted[3]);
                for (EventInstanceManager eim : em.getInstances()) {
                    eim.setProperty(splitted[2], splitted[3]);
                }
            }
            return 1;
        }
    }

    public static class ListInstanceProperty extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "none");
            } else {
                for (EventInstanceManager eim : em.getInstances()) {
                    c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", eventManager: " + em.getName() + " iprops: " + eim.getProperty(splitted[2]) + ", eprops: " + em.getProperty(splitted[2]));
                }
            }
            return 0;
        }
    }

    public static class LeaveInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() == null) {
                c.getPlayer().dropMessage(5, "You are not in one");
            } else {
                c.getPlayer().getEventInstance().unregisterPlayer(c.getPlayer());
            }
            return 1;
        }
    }

    public static class StartInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() != null) {
                c.getPlayer().dropMessage(5, "You are in one");
            } else if (splitted.length > 2) {
                EventManager em = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
                if (em == null || em.getInstance(splitted[2]) == null) {
                    c.getPlayer().dropMessage(5, "Not exist");
                } else {
                    em.getInstance(splitted[2]).registerPlayer(c.getPlayer());
                }
            } else {
                c.getPlayer().dropMessage(5, "!startinstance [eventmanager] [eventinstance]");
            }
            return 1;

        }
    }

    public static class ResetMobs extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().killAllMonsters(false);
            return 1;
        }
    }

    public static class MulungEF extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().mulung_EnergyModify(300);
            return 1;
        }
    }

    public static class KillMonsterByOID extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.killMonster(monster, c.getPlayer(), false, false, (byte) 1);
            }
            return 1;
        }
    }

    public static class 엔피시삭제 extends AdminCommand.RemoveNPCs {}
    public static class RemoveNPCs extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetNPCs();
            return 1;
        }
    }

    public static class 공지 extends CommandExecute {

        protected static int getNoticeType(String typestring) {
            if (typestring.equals("팝업")) {
                return 1;
            }
            return -1;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int joinmod = 1;
            int range = -1;
            if (splitted[1].equals("맵")) {
                range = 0;
            } else if (splitted[1].equals("채널")) {
                range = 1;
            } else if (splitted[1].equals("월드")) {
                range = 2;
            }

            int tfrom = 2;
            if (range == -1) {
                range = 2;
                tfrom = 1;
            }
            int type = getNoticeType(splitted[tfrom]);
            if (type == -1) {
                type = 0;
                joinmod = 0;
            }
            StringBuilder sb = new StringBuilder();
            joinmod += tfrom;
            sb.append(StringUtil.joinStringFrom(splitted, joinmod));

            byte[] packet = MaplePacketCreator.serverNotice(type, sb.toString());
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class TDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().toggleDrops();
            return 1;
        }
    }

    public static class MesoEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.gainMeso(Integer.parseInt(splitted[1]), true);
                }
            }
            return 1;
        }
    }

    public static class 드롭배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                RateManager.DROP = rate;
                c.getPlayer().dropMessage(6, "드롭 배율을 " + rate + "배로 변경했습니다.");
            } else {
                c.getPlayer().dropMessage(6, "사용법 : !드롭배율 <숫자>");
            }
            return 1;
        }
    }

    public static class 경험치배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                RateManager.EXP = rate;
                c.getPlayer().dropMessage(6, "경험치 배율을 " + rate + "배로 변경했습니다.");
            } else {
                c.getPlayer().dropMessage(6, "사용법 : !경험치배율 <숫자>");
            }
            return 1;
        }
    }

    public static class 메소배율 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                final int rate = Integer.parseInt(splitted[1]);
                RateManager.MESO = rate;
                c.getPlayer().dropMessage(6, "메소 배율을 " + rate + "배로 변경했습니다.");
            } else {
                c.getPlayer().dropMessage(6, "사용법 : !메소배율 <숫자>");
            }
            return 1;
        }
    }

    public static class DCAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 1;
            }
            if (range == 0) {
                c.getPlayer().getMap().disconnectAll();
            } else if (range == 1) {
                c.getChannelServer().getPlayerStorage().disconnectAll(true);
            } else if (range == 2) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.getPlayerStorage().disconnectAll(true);
                }
            }
            return 1;
        }
    }

    public static class TPetPickUp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean allowed = c.getPlayer().getMap().togglePetPick();
            c.getPlayer().dropMessage(6, "Current Map's Pet Pickup allowed : " + allowed);
            if (!allowed) {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 비활성화 되었습니다."));
            } else {
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.yellowChat("현재 맵에서 펫 줍기 기능이 활성화 되었습니다."));
            }
            return 1;
        }

    }

    public static class 서버종료 extends CommandExecute {

        protected static Thread t = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Shutting down...");
            if (t == null || !t.isAlive()) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "A shutdown thread is already in progress or shutdown has not been done. Please wait.");
            }
            return 1;
        }
    }

    public static class 서버종료시간 extends 서버종료 {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(6, minutesLeft + "분 후 서버가 종료됩니다.");
            if (ts == null && (t == null || !t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ts = EventTimer.getInstance().register(new Runnable() {

                    public void run() {
                        if (minutesLeft == 0) {
                            ShutdownServer.getInstance().shutdown();
                            t.start();
                            ts.cancel(false);
                            return;
                        }
                        World.Broadcast.broadcastMessage(MaplePacketCreator.serverMessage("서버가 " + minutesLeft + "분 후 종료됩니다. 안전하게 로그아웃해 주세요. 재시작 일정인 경우, 알림이나 공지사항을 참조해 주시기 바랍니다."));
                        minutesLeft--;
                    }
                }, 60000);
            } else {
                c.getPlayer().dropMessage(6, "A shutdown thread is already in progress or shutdown has not been done. Please wait.");
            }
            return 1;
        }
    }

    public static class Shutdown9 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long time = cal.getTimeInMillis();
            long schedulewait = 0;
            if (time > System.currentTimeMillis()) {
                schedulewait = time - System.currentTimeMillis();
            } else {
                schedulewait = time + 86400000L - System.currentTimeMillis();
            }
            if (schedulewait < 3600000) {
                schedulewait += 86400000L;
            }
//            schedulewait += (86400000L * 2);
            System.out.println("[Shutdown] Server will shutdown automatically in " + StringUtil.getReadableMillis(0, schedulewait).replace("일", "days ").replace("시간", "hours ").replace("분", "mins ").replace("초", "secs."));
            BroadcastMsgSchedule("잠시 후 서버 안정을 위하여 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 3600000);
            BroadcastMsgSchedule("30분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 1800000);
            BroadcastMsgSchedule("15분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 900000);
            BroadcastMsgSchedule("10분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 600000);
            BroadcastMsgSchedule("5분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 300000);
            BroadcastMsgSchedule("2분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 120000);
            BroadcastMsgSchedule("1분 후 오전 9시에 서버 재시작이 있을 예정입니다. 접속중이신 분들은 서버 재시작 시각 이전에 종료해 주시기 바랍니다.", schedulewait - 60000);
            Timer.WorldTimer.getInstance().schedule(new Start.Shutdown(), schedulewait);
            return 1;
        }

        public static void BroadcastMsgSchedule(final String msg, long schedule) {
            Timer.CloneTimer.getInstance().schedule(new Runnable() {
                @Override
                public void run() {
                    World.Broadcast.broadcastMessage(MaplePacketCreator.yellowChat(msg));
                }
            }, schedule);
        }
    }

    public static class 노랑 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            if (splitted[1].equals("m")) {
                range = 0;
            } else if (splitted[1].equals("c")) {
                range = 1;
            } else if (splitted[1].equals("w")) {
                range = 2;
            }
            if (range == -1) {
                range = 2;
            }
            byte[] packet = MaplePacketCreator.yellowChat((splitted[0].equals("!y") ? ("[" + c.getPlayer().getName() + "] ") : "") + StringUtil.joinStringFrom(splitted, 2));
            if (range == 0) {
                c.getPlayer().getMap().broadcastMessage(packet);
            } else if (range == 1) {
                ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
            } else if (range == 2) {
                World.Broadcast.broadcastMessage(packet);
            }
            return 1;
        }
    }

    public static class StartProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            sampler.addIncluded("client");
            sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
            sampler.addIncluded("database");
            sampler.addIncluded("handling");
            sampler.addIncluded("provider");
            sampler.addIncluded("scripting");
            sampler.addIncluded("server");
            sampler.addIncluded("tools");
            sampler.start();
            return 1;
        }
    }

    public static class StopProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            try {
                String filename = "odinprofile.txt";
                if (splitted.length > 1) {
                    filename = splitted[1];
                }
                File file = new File(filename);
                if (file.exists()) {
                    c.getPlayer().dropMessage(6, "The entered filename already exists, choose a different one");
                    return 0;
                }
                sampler.stop();
                FileWriter fw = new FileWriter(file);
                sampler.save(fw, 1, 10);
                fw.close();
            } catch (IOException e) {
                System.err.println("Error saving profile" + e);
            }
            sampler.reset();
            return 1;
        }
    }

    public static class 저장 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            // User Data Save Start
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    chr.saveToDB(false, false);
                }
            }
            // User Data Save End
            // Server Data Save Start
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
            MarriageManager.getInstance().saveAll();
            MinervaOwlSearchTop.getInstance().saveToFile();
            MedalRanking.saveAll();
            //       RankingWorker.getInstance().run();
            // Server Data Save End
            c.getPlayer().dropMessage(6, "저장이 완료되었습니다.");
            return 1;
        }
    }

    public static class 아이피대조 extends CommandExecute {

        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "사용법: !아이피대조 <캐릭터 닉네임>");
                return 0;
            }
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            PreparedStatement ps2 = null;
            ResultSet rs2 = null;

            int Accid = 0, Count = 0;
            String IP = "";
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
                ps.setString(1, splitted[1]);
                rs = ps.executeQuery();
                if (rs.next()) {
                    Accid = rs.getInt("accountid");
                    ps.close();
                    rs.close();
                    ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    ps.setInt(1, Accid);
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        IP = rs.getString("SessionIP");
                        c.getPlayer().dropMessage(2, "검색한 캐릭터의 접속 아이디 파악 (검색 값 : " + splitted[1] + ") [아이피 : " + IP + "]");
                        ps.close();
                        rs.close();
                        ps = con.prepareStatement("SELECT * FROM accounts WHERE SessionIP = ?");
                        ps.setString(1, IP);
                        rs = ps.executeQuery();
                        String Text = "";
                        while (rs.next()) {
                            if (rs.getInt("banned") > 0) {
                                Text = " / 밴 당한 아이디";
                            }
                            c.getPlayer().dropMessage(5, "아이디 : " + rs.getString("name") + " / " + Text);
                            Accid = rs.getInt("id");
                            ps2 = con.prepareStatement("SELECT * FROM characters WHERE accountid = ?");
                            ps2.setInt(1, Accid);
                            rs2 = ps2.executeQuery();
                            while (rs2.next()) {
                                Count++;
                                c.getPlayer().dropMessage(6, Count + "번 캐릭터 : " + rs2.getString("name"));
                            }
                            if (Count == 0) {
                                c.getPlayer().dropMessage(6, rs.getString("name") + " 아이디는 캐릭터가 없습니다.");
                            }
                            Count = 0;
                            ps2.close();
                            rs2.close();
                        }
                        ps.close();
                        rs.close();
                    } else {
                        c.getPlayer().dropMessage(5, "버그가 발생했습니다.");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(5, "존재하지 않는 닉네임입니다.");
                    return 0;
                }
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (ps != null) {
                        ps.close();
                    }
                    if (con != null) {
                        con.close();
                    }
                    if (rs2 != null) {
                        rs2.close();
                    }
                    if (ps2 != null) {
                        ps2.close();
                    }
                } catch (Exception e) {
                }
            }
            return 1;
        }
    }
}
