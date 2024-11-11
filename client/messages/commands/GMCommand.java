/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.messages.commands;

import client.MapleBuffStat;
import client.MapleBuffStatValueHolder;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.messages.commands.InternCommand.Ban;
import client.messages.commands.InternCommand.TempBan;
import constants.ServerConstants.PlayerGMRank;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;

/**
 * @author Emilyx3
 */
public class GMCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.GM;
    }

    public static class Invincible extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (player.isInvincible()) {
                player.setInvincible(false);
                player.dropMessage(6, "Invincibility deactivated.");
            } else {
                player.setInvincible(true);
                player.dropMessage(6, "Invincibility activated.");
            }
            return 1;
        }
    }
    
    public static class 테스트 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleBuffStatValueHolder v = c.getPlayer().getBSVH(MapleBuffStat.BLUE_AURA);
            if (v != null) {
                c.getPlayer().dropMessage(5, "버프 건사람 : " + v.cid);
            }
            
            return 1;
        }
    }

    public static class 명령어 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            player.dropMessage(6, "!현재좌표, 좌표 : 맵코드 및 현재 위치를 알려줍니다.");
            player.dropMessage(6, "!검색 <엔피시/몬스터/아이템/맵/스킬/퀘스트> <검색어> : 고유코드를 검색");
            player.dropMessage(6, "!아이템 <아이템코드> : 원하는 아이템 생성");
            player.dropMessage(6, "!맵 <대상이름/맵코드> : 대상에게 순간이동 또는 맵 고유코드로 순간이동");
            player.dropMessage(6, "!소환 <대상이름> : 대상을 자신에게 소환");
            player.dropMessage(6, "!cheaters : 현재 핵으로 의심가는 사람들 목록 (숫자는 의심 감지 횟수)");
            player.dropMessage(6, "!ban <대상이름/이메일주소/IP> <밴 사유> : 대상을 해당 밴 사유로 밴.");
            player.dropMessage(6, "!unban <대상이름/이메일주소/IP> : 대상을 밴 해제");
            player.dropMessage(6, "!hellban <대상이름/이메일주소/IP> <밴 사유> : 대상을 해당 밴 사유로 아이피 포함 영구밴.");
            player.dropMessage(6, "!unhellban <대상이름/이메일주소/IP> : 대상을 아이피 포함 영구밴 해제");
            player.dropMessage(6, "!캐릭터정보, 캐릭터정보2 <대상> : 계정, IP, 스탯, 메소 정보");
            player.dropMessage(6, "!드랍삭제, 드롭삭제, removedrops : 현재 맵에 떨어진 아이템 모두 삭제");
            player.dropMessage(6, "!킬올(killall), 킬올드롭(killalldrop), 킬올경험치(killallexp) : 맵에 있는 몬스터 모두 죽입니다.");
            player.dropMessage(6, "!서버타임 : 서버가 켜져있던 시간");
            player.dropMessage(6, "!캐시 <캐릭터> <숫자> : 캐시를 지급합니다. / !캐시 <숫자> : 캐시를 획득합니다.");
            player.dropMessage(6, "!동접, 온라인 : 동시 접속자를 파악하고 인원을 알려줍니다.");
            player.dropMessage(6, "!사냥동접 : 사냥 중인 유저를 파악하고 인원을 알려줍니다.");
            return 1;
        }
    }

    public static class 맵온라인 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "현재 이 맵에 있는 유저:");
            StringBuilder builder = new StringBuilder();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(6, builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
            builder.setLength(builder.length() - 2);
            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }

    public static class TempBanIP extends TempBan {

        public TempBanIP() {
            ipBan = true;
        }
    }

    public static class BanIP extends Ban {

        public BanIP() {
            ipBan = true;
        }
    }

}
