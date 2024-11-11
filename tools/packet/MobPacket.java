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
package tools.packet;

import client.Skill;
import client.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.SendPacketOpcode;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.data.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import server.MapleStatEffect;
import tools.Pair;

public class MobPacket {

    public static byte[] damageMonster(final int oid, final long damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        if (damage > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) damage);
        }

        return mplew.getPacket();
    }

    public static byte[] damageFriendlyMob(final MapleMonster mob, final long damage, final boolean display) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(display ? 1 : 2); //false for when shammos changes map!
        if (damage > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) damage);
        }
        if (mob.getHp() > Integer.MAX_VALUE) {
            mplew.writeInt((int) (((double) mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        } else {
            mplew.writeInt((int) mob.getHp());
        }
        if (mob.getMobMaxHp() > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) mob.getMobMaxHp());
        }
        //System.out.println(HexTool.toString(mplew.getPacket()));
        return mplew.getPacket();
    }

    public static byte[] killMonster(final int oid, final int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation); // 0 = dissapear, 1 = fade out, 2+ = special
        if (animation == 4) {
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }

    public static byte[] suckMonster(final int oid, final int chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(4);
        mplew.writeInt(chr);

        return mplew.getPacket();
    }

    public static byte[] healMonster(final int oid, final int heal) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(-heal);

        return mplew.getPacket();
    }

    public static byte[] showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);

        return mplew.getPacket();
    }

    public static byte[] showBossHP(final MapleMonster mob) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BOSS_ENV.getValue());

        mplew.write(5);
        mplew.writeInt(
                mob.getId() == 9400589 ? 9300184
                : mob.getId() == 8830001 ? 8830000
                : mob.getId() == 8830002 ? 8830000
                : mob.getId() == 8830008 ? 8830007
                : mob.getId() == 8830009 ? 8830007
                : mob.getId()); //hack: MV cant have boss hp bar
        int headChp = 0;
        int leftChp = 0;
        int rightChp = 0;
        MapleMonster mob0 = mob.getMap().getMonsterById(8830000);
        MapleMonster mob1 = mob.getMap().getMonsterById(8830001);
        MapleMonster mob2 = mob.getMap().getMonsterById(8830002);
        if (mob0 != null) {
            headChp = (int) mob0.getHp();
        }
        if (mob1 != null) {
            leftChp = (int) mob1.getHp();
        }
        if (mob2 != null) {
            rightChp = (int) mob2.getHp();
        }
        MapleMonster mob3 = mob.getMap().getMonsterById(8830007);
        MapleMonster mob4 = mob.getMap().getMonsterById(8830008);
        MapleMonster mob5 = mob.getMap().getMonsterById(8830009);
        if (mob3 != null) {
            headChp = (int) mob3.getHp();
        }
        if (mob4 != null) {
            leftChp = (int) mob4.getHp();
        }
        if (mob5 != null) {
            rightChp = (int) mob5.getHp();
        }
        if (mob.getHp() > Integer.MAX_VALUE) {
            mplew.writeInt((int) (((double) mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        } else {
            if (mob.getId() == 8830000 || mob.getId() == 8830001 || mob.getId() == 8830002 || mob.getId() == 8830007 || mob.getId() == 8830008 || mob.getId() == 8830009) {
                mplew.writeInt(headChp + leftChp + rightChp);
            } else {
                mplew.writeInt((int) mob.getHp());
            }
        }
        if (mob.getMobMaxHp() > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            if (mob.getId() == 8830000 || mob.getId() == 8830001 || mob.getId() == 8830002) {
                mplew.writeInt(4280000 + 2640000 + 3060000);
            } else if (mob.getId() == 8830000 || mob.getId() == 8830001 || mob.getId() == 8830002) {
                mplew.writeInt(3210000 + 2059200 + 2233800);
            } else {
                mplew.writeInt((int) mob.getMobMaxHp());
            }
        }
        mplew.write(
                mob.getId() == 8830000 || mob.getId() == 8830001 || mob.getId() == 8830002 ? 3
                : mob.getId() == 8830007 || mob.getId() == 8830008 || mob.getId() == 8830009 ? 3
                : mob.getStats().getTagColor());
        mplew.write(
                mob.getId() == 8830000 || mob.getId() == 8830001 || mob.getId() == 8830002 ? 5
                : mob.getId() == 8830007 || mob.getId() == 8830008 || mob.getId() == 8830009 ? 5
                : mob.getStats().getTagBgColor());
        return mplew.getPacket();
    }

    public static byte[] showBossHP(final int monsterId, final long currentHp, final long maxHp, byte tagcolor, byte tagbgcolor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(5);
        mplew.writeInt(monsterId); //has no image
        if (currentHp > Integer.MAX_VALUE) {
            mplew.writeInt((int) (((double) currentHp / maxHp) * Integer.MAX_VALUE));
        } else {
            mplew.writeInt((int) (currentHp <= 0 ? -1 : currentHp));
        }
        if (maxHp > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) maxHp);
        }
        mplew.write(tagcolor);
        mplew.write(tagbgcolor);

        //colour legend: (applies to both colours)
        //1 = red, 2 = dark blue, 3 = light green, 4 = dark green, 5 = black, 6 = light blue, 7 = purple
        return mplew.getPacket();
    }

    public static byte[] moveMonster(boolean useskill, int skill, int skill1, int skill2, int tdelay, int oid, Point startPos, List<LifeMovementFragment> moves, List<Integer> unk2, List<Pair<Integer, Integer>> unk3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.writeShort(0); //moveid but always 0
        mplew.write(useskill ? 1 : 0); //?? I THINK
        mplew.write(skill); //pCenterSplit

        mplew.write(skill1);  //bIllegalVelocity
        mplew.write(skill2);
        mplew.writeShort(tdelay);

        mplew.writeInt(unk3 == null ? 0 : unk3.size());
        if (unk3 != null) {
            for (Pair i : unk3) {
                mplew.writeInt(((Integer) i.left));
                mplew.writeInt(((Integer) i.right));
            }
        }
        mplew.writeInt(unk2 == null ? 0 : unk2.size());
        if (unk2 != null) {
            for (Integer i : unk2) {
                mplew.writeInt(i);
            }
        }

        mplew.writePos(startPos);
        mplew.writeShort(8); //? sometimes 0? sometimes 22? sometimes random numbers?
        mplew.writeShort(1);
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static byte[] spawnMonster(MapleMonster life, int spawnType, int link) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_MONSTER.getValue());
        mplew.writeInt(life.getObjectId());
        mplew.write(1); // 1 = Control normal, 5 = Control none
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);
        mplew.writePos(life.getTruePosition());
        mplew.write(life.getStance());
        mplew.writeShort(0); // FH
        mplew.writeShort(life.getFh()); // Origin FH
        mplew.write(spawnType);
        if (life.getId() == 8300007) {
            link = 5000;
        }
        if (spawnType == -3 || spawnType >= 0) {
            mplew.writeInt(link); // tdelay
        }
        //System.out.println(life.getId() + " / " + spawnType + " / " + link);
        mplew.write(life.getCarnivalTeam());
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static void addMonsterStatus(MaplePacketLittleEndianWriter mplew, MapleMonster life) {
        Collection<MonsterStatusEffect> buffs = life.getStati().values();
        getLongMask_NoRef(mplew, buffs); //AFTERSHOCK: extra int
        if (life.isBuffed(MonsterStatus.WATK)) {
            mplew.writeShort(life.getBuff(MonsterStatus.WATK).getX());
            if (life.getBuff(MonsterStatus.WATK).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.WATK).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.WATK).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.WATK).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.WATK).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.WDEF)) {
            mplew.writeShort(life.getBuff(MonsterStatus.WDEF).getX());
            if (life.getBuff(MonsterStatus.WDEF).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.WDEF).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.WDEF).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.WDEF).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.WDEF).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MATK)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MATK).getX());
            if (life.getBuff(MonsterStatus.MATK).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MATK).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MATK).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MATK).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MATK).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MDEF)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MDEF).getX());
            if (life.getBuff(MonsterStatus.MDEF).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MDEF).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MDEF).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MDEF).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MDEF).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.ACC)) {
            mplew.writeShort(life.getBuff(MonsterStatus.ACC).getX());
            if (life.getBuff(MonsterStatus.ACC).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.ACC).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.ACC).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.ACC).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.ACC).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.AVOID)) {
            mplew.writeShort(life.getBuff(MonsterStatus.AVOID).getX());
            if (life.getBuff(MonsterStatus.AVOID).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.AVOID).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.AVOID).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.AVOID).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.AVOID).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.SPEED)) {
            mplew.writeShort(life.getBuff(MonsterStatus.SPEED).getX());
            if (life.getBuff(MonsterStatus.SPEED).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.SPEED).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.SPEED).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.SPEED).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.SPEED).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.STUN)) {
            mplew.writeShort(life.getBuff(MonsterStatus.STUN).getX());
            if (life.getBuff(MonsterStatus.STUN).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.STUN).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.STUN).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.STUN).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.STUN).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.FREEZE)) {
            mplew.writeShort(life.getBuff(MonsterStatus.FREEZE).getX());
            if (life.getBuff(MonsterStatus.FREEZE).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.FREEZE).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.FREEZE).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.FREEZE).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.FREEZE).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.POISON)) {
            mplew.writeShort(life.getBuff(MonsterStatus.POISON).getX());
            if (life.getBuff(MonsterStatus.POISON).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.POISON).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.POISON).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.POISON).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.POISON).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.SEAL)) {
            mplew.writeShort(life.getBuff(MonsterStatus.SEAL).getX());
            if (life.getBuff(MonsterStatus.SEAL).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.SEAL).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.SEAL).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.SEAL).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.SEAL).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.SHOWDOWN)) {
            mplew.writeShort(life.getBuff(MonsterStatus.SHOWDOWN).getX());
            if (life.getBuff(MonsterStatus.SHOWDOWN).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.SHOWDOWN).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.SHOWDOWN).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.SHOWDOWN).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.SHOWDOWN).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.WEAPON_ATTACK_UP)) {
            mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_ATTACK_UP).getX());
            if (life.getBuff(MonsterStatus.WEAPON_ATTACK_UP).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_ATTACK_UP).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_ATTACK_UP).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.WEAPON_ATTACK_UP).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.WEAPON_ATTACK_UP).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.WEAPON_DEFENSE_UP)) {
            mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_DEFENSE_UP).getX());
            if (life.getBuff(MonsterStatus.WEAPON_DEFENSE_UP).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_DEFENSE_UP).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_DEFENSE_UP).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.WEAPON_DEFENSE_UP).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.WEAPON_DEFENSE_UP).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MAGIC_ATTACK_UP)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_ATTACK_UP).getX());
            if (life.getBuff(MonsterStatus.MAGIC_ATTACK_UP).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_ATTACK_UP).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_ATTACK_UP).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MAGIC_ATTACK_UP).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MAGIC_ATTACK_UP).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MAGIC_DEFENSE_UP)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_DEFENSE_UP).getX());
            if (life.getBuff(MonsterStatus.MAGIC_DEFENSE_UP).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_DEFENSE_UP).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_DEFENSE_UP).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MAGIC_DEFENSE_UP).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MAGIC_DEFENSE_UP).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.WEAPON_IMMUNITY)) {
            mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_IMMUNITY).getX());
            if (life.getBuff(MonsterStatus.WEAPON_IMMUNITY).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_IMMUNITY).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_IMMUNITY).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.WEAPON_IMMUNITY).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.WEAPON_IMMUNITY).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MAGIC_IMMUNITY)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_IMMUNITY).getX());
            if (life.getBuff(MonsterStatus.MAGIC_IMMUNITY).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_IMMUNITY).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_IMMUNITY).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MAGIC_IMMUNITY).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MAGIC_IMMUNITY).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.DOOM)) {
            mplew.writeShort(life.getBuff(MonsterStatus.DOOM).getX());
            if (life.getBuff(MonsterStatus.DOOM).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.DOOM).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.DOOM).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.DOOM).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.DOOM).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.SHADOW_WEB)) {
            mplew.writeShort(life.getBuff(MonsterStatus.SHADOW_WEB).getX());
            if (life.getBuff(MonsterStatus.SHADOW_WEB).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.SHADOW_WEB).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.SHADOW_WEB).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.SHADOW_WEB).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.SHADOW_WEB).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.DAMAGE_IMMUNITY)) {
            mplew.writeShort(life.getBuff(MonsterStatus.DAMAGE_IMMUNITY).getX());
            if (life.getBuff(MonsterStatus.DAMAGE_IMMUNITY).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.DAMAGE_IMMUNITY).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.DAMAGE_IMMUNITY).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.DAMAGE_IMMUNITY).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.DAMAGE_IMMUNITY).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.NINJA_AMBUSH)) {
            mplew.writeShort(life.getBuff(MonsterStatus.NINJA_AMBUSH).getX());
            if (life.getBuff(MonsterStatus.NINJA_AMBUSH).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.NINJA_AMBUSH).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.NINJA_AMBUSH).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.NINJA_AMBUSH).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.NINJA_AMBUSH).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
//        if (life.isBuffed(MonsterStatus.VENOM)) {
//            mplew.writeShort(life.getBuff(MonsterStatus.VENOM).getX());
//            if (life.getBuff(MonsterStatus.VENOM).getMobSkill() != null) {
//                mplew.writeShort(life.getBuff(MonsterStatus.VENOM).getMobSkill().getSkillId());
//                mplew.writeShort(life.getBuff(MonsterStatus.VENOM).getMobSkill().getSkillLevel());
//            } else {
//                mplew.writeInt(life.getBuff(MonsterStatus.VENOM).getSkill());
//            }
//            mplew.writeShort((int) ((life.getBuff(MonsterStatus.VENOM).getCancelTask() - System.currentTimeMillis()) / 500.0));
//        }
        if (life.isBuffed(MonsterStatus.BLIND)) {
            mplew.writeShort(life.getBuff(MonsterStatus.BLIND).getX());
            if (life.getBuff(MonsterStatus.BLIND).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.BLIND).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.BLIND).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.BLIND).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.BLIND).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.SEAL_SKILL)) {
            mplew.writeShort(life.getBuff(MonsterStatus.SEAL_SKILL).getX());
            if (life.getBuff(MonsterStatus.SEAL_SKILL).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.SEAL_SKILL).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.SEAL_SKILL).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.SEAL_SKILL).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.SEAL_SKILL).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.HYPNOTIZE)) {
            mplew.writeShort(life.getBuff(MonsterStatus.HYPNOTIZE).getX());
            if (life.getBuff(MonsterStatus.HYPNOTIZE).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.HYPNOTIZE).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.HYPNOTIZE).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.HYPNOTIZE).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.HYPNOTIZE).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT)) {
            mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getX());
            if (life.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_DAMAGE_REFLECT).getX());
            if (life.getBuff(MonsterStatus.MAGIC_DAMAGE_REFLECT).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_DAMAGE_REFLECT).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_DAMAGE_REFLECT).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MAGIC_DAMAGE_REFLECT).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MAGIC_DAMAGE_REFLECT).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.RISE_BY_TOSS)) {
            mplew.writeShort(life.getBuff(MonsterStatus.RISE_BY_TOSS).getX());
            if (life.getBuff(MonsterStatus.RISE_BY_TOSS).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.RISE_BY_TOSS).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.RISE_BY_TOSS).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.RISE_BY_TOSS).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.RISE_BY_TOSS).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.NEUTRALISE)) {
            mplew.writeShort(life.getBuff(MonsterStatus.NEUTRALISE).getX());
            if (life.getBuff(MonsterStatus.NEUTRALISE).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.NEUTRALISE).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.NEUTRALISE).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.NEUTRALISE).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.NEUTRALISE).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.IMPRINT)) {
            mplew.writeShort(life.getBuff(MonsterStatus.IMPRINT).getX());
            if (life.getBuff(MonsterStatus.IMPRINT).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.IMPRINT).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.IMPRINT).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.IMPRINT).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.IMPRINT).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MONSTER_BOMB)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MONSTER_BOMB).getX());
            if (life.getBuff(MonsterStatus.MONSTER_BOMB).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MONSTER_BOMB).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MONSTER_BOMB).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MONSTER_BOMB).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MONSTER_BOMB).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.SHOWDOWN)) {
            mplew.writeShort(life.getBuff(MonsterStatus.SHOWDOWN).getX());
            if (life.getBuff(MonsterStatus.SHOWDOWN).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.SHOWDOWN).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.SHOWDOWN).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.SHOWDOWN).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.SHOWDOWN).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.MAGIC_CRASH)) {
            mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_CRASH).getX());
            if (life.getBuff(MonsterStatus.MAGIC_CRASH).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_CRASH).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.MAGIC_CRASH).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.MAGIC_CRASH).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.MAGIC_CRASH).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.DAMAGED_ELEM_ATTR)) {
            mplew.writeShort(life.getBuff(MonsterStatus.DAMAGED_ELEM_ATTR).getX());
            if (life.getBuff(MonsterStatus.DAMAGED_ELEM_ATTR).getMobSkill() != null) {
                mplew.writeShort(life.getBuff(MonsterStatus.DAMAGED_ELEM_ATTR).getMobSkill().getSkillId());
                mplew.writeShort(life.getBuff(MonsterStatus.DAMAGED_ELEM_ATTR).getMobSkill().getSkillLevel());
            } else {
                mplew.writeInt(life.getBuff(MonsterStatus.DAMAGED_ELEM_ATTR).getSkill());
            }
            mplew.writeShort((int) ((life.getBuff(MonsterStatus.DAMAGED_ELEM_ATTR).getCancelTask() - System.currentTimeMillis()) / 500.0));
        }
        if (life.isBuffed(MonsterStatus.BURNED)) {
            mplew.writeInt(life.getPoisons().size());
            for (MonsterStatusEffect m : life.getPoisons()) {
                mplew.writeInt(m.getFromID());
                if (m.isMonsterSkill()) {
                    mplew.writeShort(m.getMobSkill().getSkillId());
                    mplew.writeShort(m.getMobSkill().getSkillLevel());
                } else if (m.getSkill() > 0) {
                    mplew.writeInt(m.getSkill());
                }
                mplew.writeInt(m.getX());
                mplew.writeInt(1000);
                mplew.writeInt(0);
                if (m.getWeakChr() != null && !m.isMonsterSkill()) {
                    Skill skill = SkillFactory.getSkill(m.getSkill());
                    MapleStatEffect dotTime = skill.getEffect(m.getWeakChr().get().getTotalSkillLevel(skill));
                    mplew.writeInt(dotTime.getDOTTime() + m.getWeakChr().get().getStat().dotTime);
                } else {
                    mplew.writeInt(5);
                }
                mplew.writeInt(0);
            }
        }
        if (life.isBuffed(MonsterStatus.WEAPON_DAMAGE_REFLECT)) {
            mplew.writeInt(life.getBuff(MonsterStatus.WEAPON_DAMAGE_REFLECT).getX());
        }
        if (life.isBuffed(MonsterStatus.MAGIC_DAMAGE_REFLECT)) {
            mplew.writeInt(life.getBuff(MonsterStatus.MAGIC_DAMAGE_REFLECT).getX());
        }
        for (Integer ref : life.getReflections()) {
            mplew.writeInt(ref);
        }
        if (life.isBuffed(MonsterStatus.SUMMON)) {
            mplew.write(1);
            mplew.write(1); // 소환몹가방인가 ..........흠흠
        }
    }

    private static void getLongMask_NoRef(MaplePacketLittleEndianWriter mplew, Collection<MonsterStatusEffect> ss) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (MonsterStatusEffect statup : ss) {
            if ((statup != null)) {
                mask[(statup.getStati().getPosition() - 1)] |= statup.getStati().getValue();
            }
        }
        for (int i = mask.length; i >= 1; i--) {
            mplew.writeInt(mask[(i - 1)]);
        }
    }

    public static byte[] controlMonster(MapleMonster life, boolean newSpawn, boolean aggro) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(aggro ? 2 : 1);
        mplew.writeInt(life.getObjectId());
        mplew.write(1); // 1 = Control normal, 5 = Control none
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);
        mplew.writePos(life.getTruePosition());
        mplew.write(life.getStance()); // Bitfield
        mplew.writeShort(0); // FH
        mplew.writeShort(life.getFh()); // Origin FH
        mplew.write(life.isFake() ? -4 : newSpawn ? -2 : -1);
        mplew.write(life.getCarnivalTeam());
        mplew.writeInt((int) life.getMobMaxHp());

        return mplew.getPacket();
    }

    public static byte[] stopControllingMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] makeMonsterReal(MapleMonster life) {
        return spawnMonster(life, -1, 0);
    }

    public static byte[] makeMonsterFake(MapleMonster life) {
        return spawnMonster(life, -4, 0);
    }

    public static byte[] makeMonsterEffect(MapleMonster life, int effect) {
        return spawnMonster(life, effect, 0);
    }

    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.write(useSkills ? 1 : 0); //bCheatResult
        mplew.writeShort(currentMp);
        mplew.write(skillId);
        mplew.write(skillLevel);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus(final int oid, final MonsterStatus mse, int x, MobSkill skil, int tDelay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        PacketHelper.writeSingleMask(mplew, mse);

        mplew.writeShort(x);
        mplew.writeShort(skil.getSkillId());
        mplew.writeShort(skil.getSkillLevel());
        mplew.writeShort((int) (skil.getDuration() / 500)); // might actually be the buffTime but it's not displayed anywhere
        mplew.writeShort(tDelay); // delay in ms
        mplew.write(1); // size
        mplew.write(1); // ? v97

        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus1(final MapleMonster mons, final MonsterStatusEffect ms, int tDelay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(mons.getObjectId());
        PacketHelper.writeSingleMask(mplew, ms.getStati());
        mplew.writeShort(ms.getX().shortValue());
        if (ms.isMonsterSkill()) {
            mplew.writeShort(ms.getMobSkill().getSkillId());
            mplew.writeShort(ms.getMobSkill().getSkillLevel());
        } else if (ms.getSkill() > 0) {
            mplew.writeInt(ms.getSkill());
        }
        mplew.writeShort((int) ((ms.getCancelTask() - System.currentTimeMillis()) / 500));//duration
        mplew.writeShort(tDelay); // delay in ms
        mplew.write(1); // size
        mplew.write(1); // ? v97

        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus2(final MapleMonster mons, final List<MonsterStatusEffect> mse, int tDelay, int Ticks) {
        if (mse.size() <= 0 || mse.get(0) == null) {
            return MaplePacketCreator.enableActions();
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(mons.getObjectId());
        final MonsterStatusEffect ms = mse.get(0);
        if (ms.getStati() == MonsterStatus.BURNED) { //stack ftw
            PacketHelper.writeSingleMask(mplew, MonsterStatus.BURNED);
            mplew.writeInt(mse.size());
            for (MonsterStatusEffect m : mse) {
                mplew.writeInt(m.getFromID());
                if (m.isMonsterSkill()) {
                    mplew.writeShort(m.getMobSkill().getSkillId());
                    mplew.writeShort(m.getMobSkill().getSkillLevel());
                } else if (m.getSkill() > 0) {
                    mplew.writeInt(m.getSkill());
                }
                mplew.writeInt(m.getX()); //데미지
                mplew.writeInt(1000);//1초당
                mplew.writeInt(0);//tend?
                mplew.writeInt(Ticks);//버프지속시간
            }
            mplew.writeShort((short) tDelay);
            mplew.write(3);
        } else if (ms.getStati() == MonsterStatus.POISON || ms.getStati() == MonsterStatus.VENOM) { //stack ftw
            for (MonsterStatusEffect m : mse) {
                if (m.getSkill() == 4220005) {
                    PacketHelper.writeSingleMask(mplew, MonsterStatus.VENOM);
                } else {
                    PacketHelper.writeSingleMask(mplew, MonsterStatus.POISON);
                }
                mplew.writeShort(m.getX()); //dmg
                if (m.isMonsterSkill()) {
                    mplew.writeShort(m.getMobSkill().getSkillId());
                    mplew.writeShort(m.getMobSkill().getSkillLevel());
                } else if (m.getSkill() > 0) {
                    mplew.writeInt(m.getSkill());
                }
                mplew.writeInt(Ticks); //버프타임인듯?donno
                mplew.writeShort(5);
            }
            mplew.writeShort((short) tDelay); // delay in ms
            mplew.write(1); // size
        } else {
            PacketHelper.writeSingleMask(mplew, ms.getStati());
            mplew.writeShort(ms.getX());
            if (ms.isMonsterSkill()) {
                mplew.writeShort(ms.getMobSkill().getSkillId());
                mplew.writeShort(ms.getMobSkill().getSkillLevel());
            } else if (ms.getSkill() > 0) {
                mplew.writeInt(ms.getSkill());
            }
            mplew.writeShort(5000);//duration

            mplew.writeShort((short) tDelay); // delay in ms
            mplew.write(1); // size
        }

        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus3(final int oid, final Map<MonsterStatus, Integer> stati, final List<Integer> reflection, MobSkill skil, int tDelay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        PacketHelper.writeMask(mplew, stati.keySet());

        for (Map.Entry<MonsterStatus, Integer> mse : stati.entrySet()) {
            mplew.writeShort(mse.getValue().shortValue());
            mplew.writeShort(skil.getSkillId());
            mplew.writeShort(skil.getSkillLevel());
            mplew.writeShort((int) (skil.getDuration() / 500));////duration
        }
        for (Integer ref : reflection) {
            mplew.writeInt(ref);
        }
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.writeShort(tDelay);
        mplew.writeShort(0);
        mplew.writeShort(0); // delay in ms

        int size = stati.size(); // size
        if (reflection.size() > 0) {
            size /= 2; // This gives 2 buffs per reflection but it's really one buff
        }
        mplew.write(size); // size
        mplew.write(1); // ? v97

        return mplew.getPacket();
    }

    public static byte[] cancelMonsterStatus(int oid, MonsterStatus stat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        PacketHelper.writeSingleMask(mplew, stat);
        mplew.write(1); // reflector is 3~!??
        mplew.write(2); // ? v97

        return mplew.getPacket();
    }

    public static byte[] cancelPoison(int oid, MonsterStatusEffect m) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        switch (m.getSkill()) {
            case 4120005:
            case 4220005:
            case 4340001:
            case 14110004:
                PacketHelper.writeSingleMask(mplew, MonsterStatus.VENOM);
                break;
            default:
                PacketHelper.writeSingleMask(mplew, MonsterStatus.POISON);
                break;
        }
        mplew.write(1); // ? v97
        /*
        mplew.writeInt(oid);
        PacketHelper.writeSingleMask(mplew, MonsterStatus.BURNED);
        mplew.writeInt(0);
        mplew.writeInt(1);
        mplew.writeInt(m.getFromID());
        if (m.isMonsterSkill()) {
            mplew.writeShort(m.getMobSkill().getSkillId());
            mplew.writeShort(m.getMobSkill().getSkillLevel());
        } else if (m.getSkill() > 0) {
            mplew.writeInt(m.getSkill());
        }
        mplew.write(3);*/

        return mplew.getPacket();
    }

    public static byte[] talkMonster(int oid, int itemId, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.TALK_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(5000); //지속시간
        mplew.writeInt(itemId);
        mplew.write(itemId <= 0 ? 0 : 1);
        mplew.write(msg == null || msg.length() <= 0 ? 0 : 1);
        if (msg != null && msg.length() > 0) {
            mplew.writeMapleAsciiString(msg);
        }
        mplew.writeInt(1); //?

        return mplew.getPacket();
    }

    public static byte[] removeTalkMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.REMOVE_TALK_MONSTER.getValue());
        mplew.writeInt(oid);
        return mplew.getPacket();
    }

    public static byte[] MobSkillDelay(int objectId, int skillID, int skillLv, int skillAfter, short option) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.MOB_SKILL_DELAY.getValue());
        mplew.writeInt(objectId);
        mplew.writeInt(skillAfter);
        mplew.writeInt(skillID);
        mplew.writeInt(skillLv);
        mplew.writeInt(option);

        return mplew.getPacket();
    }

    public static byte[] showMonsterBomb(int mobi, int chrid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_BOMB.getValue());
        mplew.writeInt(mobi);
        mplew.writeInt(4341003);//스킬아이디
        mplew.writeInt(chrid);//캐릭터아이디가 유력함
        mplew.writeShort(3000);//터질때 딜레이시간 3000이면 3초뒤에 폭발함
        return mplew.getPacket();
    }
    
}
