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

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import handling.SendPacketOpcode;
import handling.login.LoginServer;
import handling.world.World;
import tools.data.MaplePacketLittleEndianWriter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import server.life.MapleMonster;

public class LoginPacket {

    private static final String version;

    static {
        int ret = 0;
        ret ^= (ServerConstants.MAPLE_VERSION & 0x7FFF);
        ret ^= (ServerConstants.MAPLE_CHECK << 15);
        ret ^= ((ServerConstants.MAPLE_PATCH & 0xFF) << 16);
        version = String.valueOf(ret);
    }

    public static byte[] getHello(byte[] sendIv, byte[] recvIv) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        int packetsize = 13 + version.length();
        mplew.writeShort(packetsize);
        mplew.writeShort(291); //KMS Static
        mplew.writeMapleAsciiString(version);
        mplew.write(recvIv);
        mplew.write(sendIv);
        mplew.write(1); // 1 = KMS, 2 = KMST, 7 = MSEA, 8 = GlobalMS, 5 = Test Server
        //System.out.println(HexTool.toString(mplew.getPacket()));
        return mplew.getPacket();
    }

    public static final byte[] getPing() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);

        mplew.writeOpcode(SendPacketOpcode.PING.getValue());

        return mplew.getPacket();
    }

    public static final byte[] getLoginFailed(final int reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        /*	* 3: ID deleted or blocked
         * 4: Incorrect password
         * 5: Not a registered id
         * 6: System error
         * 7: Already logged in
         * 8: System error
         * 9: System error
         * 10: Cannot process so many connections
         * 11: Only users older than 20 can use this channel
         * 13: Unable to log on as master at this ip
         * 14: Wrong gateway or personal info and weird korean button
         * 15: Processing request with that korean button!
         * 16: Please verify your account through email...
         * 17: Wrong gateway or personal info
         * 21: Please verify your account through email...
         * 23: License agreement
         * 25: Maple Europe notice
         * 27: Some weird full client notice, probably for trial versions
         * 32: IP blocked
         * 84: please revisit website for pass change --> 0x07 recv with response 00/01*/
        mplew.writeOpcode(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(reason);
        if (reason == 84) {
            mplew.writeLong(PacketHelper.getTime(-2));
        } else if (reason == 7) { //prolly this
            mplew.writeZeroBytes(5);
        }

        return mplew.getPacket();
    }

    public static final byte[] getTempBan(final long timestampTill, final byte reason) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);

        mplew.writeOpcode(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeInt(2);
        mplew.writeShort(0);
        mplew.write(reason);
        mplew.writeLong(timestampTill); // Tempban date is handled as a 64-bit long, number of 100NS intervals since 1/1/1601. Lulz.

        return mplew.getPacket();
    }

    public static final byte[] getAuthSuccessRequest(MapleClient client) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(0);
        mplew.writeInt(client.getAccID()); // v65 = CInPacket::Decode4(a2);
        mplew.write(client.getGender()); // v31 = (unsigned __int8)CInPacket::Decode1(v30);
        mplew.write(client.isGm() ? 1 : 0); // LOBYTE(v70) = CInPacket::Decode1(v30);
        mplew.write(client.isGm() ? 6 : 0); // Admin byte - Commands
        mplew.writeMapleAsciiString(client.getAccountName()); // CInPacket::DecodeStr(v30, (int)&v66);
        mplew.writeInt(1234567);//v70 // v73 = CInPacket::Decode4(v30);
        mplew.write(1);// v68 = (unsigned __int8)CInPacket::Decode1(v30); 주민번호 체크
        mplew.write(0); // LOBYTE(Value) = CInPacket::Decode1(v30);
        mplew.write(client.isChatBlocked() ? 1 : 0); //chat block // LOBYTE(v72) = CInPacket::Decode1(v30);
        mplew.writeLong(PacketHelper.getTime(client.getChatBlockTime())); //until chat block time //CInPacket::DecodeBuffer(v30, &v59, 8);
        mplew.writeMapleAsciiString(""); //v33 = CInPacket::DecodeStr(a2, (int)&v69);
        return mplew.getPacket();
    }

    public static final byte[] packetTest(MapleClient client, MapleMonster mob) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(246);
        mplew.writeInt(15);

        return mplew.getPacket();
    }

    public static final byte[] packetTest2(MapleClient client) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(327);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static final byte[] deleteCharResponse(final int cid, final int state) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(cid);
        mplew.write(state);

        return mplew.getPacket();
    }

    public static final byte[] secondPasswordResult(byte op1, byte op2) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);
        mplew.writeOpcode(SendPacketOpcode.SECONDPW_ERROR.getValue());
        mplew.write(op1);
        mplew.write(op2);
        return mplew.getPacket();

    }

    public static final byte[] secondPwError(final byte mode) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        /*
         * 14 - Invalid password
         * 15 - Second password is incorrect
         */
        mplew.writeOpcode(SendPacketOpcode.SECONDPW_ERROR.getValue());
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] enableRecommended(int world) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ENABLE_RECOMMENDED.getValue());
        mplew.writeInt(world); //worldID with most characters
        return mplew.getPacket();
    }

    public static byte[] sendRecommended(int[] world, String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.SEND_RECOMMENDED.getValue());
        if (message == null) {
            mplew.write(0);
        } else {
            mplew.write(world.length); //amount of messages  
        }
        for (int i = 0; i < world.length; i++) {
            mplew.writeInt(world[i]);
            mplew.writeMapleAsciiString(message);
        }
        return mplew.getPacket();
    }

    public static final byte[] getServerList(final int serverId, final Map<Integer, Integer> channelLoad) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(serverId); // 0 = Aquilla, 1 = bootes, 2 = cass, 3 = delphinus
        final String worldName = LoginServer.getTrueServerName(); //remove the SEA
        mplew.writeMapleAsciiString(worldName);
        if (serverId == 23) {
            mplew.write(2);
        } else {
            mplew.write(LoginServer.getFlag());
        }
        mplew.writeMapleAsciiString(LoginServer.getEventMessage()+"\r\n#b현재 접속중인 인원: "+World.getConnected().get(0));
        mplew.writeShort(100);
        mplew.writeShort(100);
        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        mplew.write(lastChannel);

        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (channels.contains(i)) {
                load = channelLoad.get(i);
            } else {
                load = 1200;
            }
            mplew.writeMapleAsciiString(worldName + "-" + (i == 1 ? "1" : i == 2 ? "20세이상" : i - 1));
            mplew.writeInt(load);
            mplew.write(serverId);
            mplew.writeShort(i - 1);
        }
        mplew.writeShort(3);
        mplew.writeShort(120);
        mplew.writeShort(310);
        mplew.writeMapleAsciiString("우와!! 리플렉스라니!!");
        mplew.writeShort(550);
        mplew.writeShort(330);
        mplew.writeMapleAsciiString("으헉 이럴수가 ㅠㅠ 감동이야");
        mplew.writeShort(440);
        mplew.writeShort(320);
        mplew.writeMapleAsciiString("뭐라고? 리플렉스라고?!!");
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    public static final byte[] getEndOfServerList() {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.SERVERLIST.getValue());
        mplew.write(0xFF);

        return mplew.getPacket();
    }

    public static final byte[] getCharList(final String secondpw, final List<MapleCharacter> chars, int charslots) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CHARLIST.getValue());
        mplew.write(0);
        mplew.writeInt(0); //IDCODE2
        mplew.write(chars.size()); // 1
        for (final MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, !chr.isGM() && chr.getLevel() >= 20);
        }
        mplew.write(secondpw != null && secondpw.length() > 0 ? 1 : 2); // second pw request
        mplew.write(0); // 주민등록번호 체크여부 (1: 체크 / 0: 미체크)
        mplew.writeLong(charslots);

        return mplew.getPacket();
    }

    public static final byte[] addNewCharEntry(final MapleCharacter chr, final boolean worked) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(worked ? 0 : 1);
        addCharEntry(mplew, chr, false);

        return mplew.getPacket();
    }

    public static final byte[] charNameResponse(final String charname, final boolean nameUsed) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeOpcode(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    private static final void addCharEntry(final MaplePacketLittleEndianWriter mplew, final MapleCharacter chr, boolean ranking) {
        PacketHelper.addCharStats(mplew, chr);
        PacketHelper.addCharLook(mplew, chr, true);
        mplew.write(0);
        if (chr.getLevel() < 30) {
            if (chr.getRank() == 1 && chr.getRankMove() == 0 && chr.getJobRank() == 1 && chr.getJobRankMove() == 0) {
                ranking = false;
            }
        }
        mplew.write(ranking ? 1 : 0);
        if (ranking) {
            mplew.writeInt(chr.getRank());
            mplew.writeInt(chr.getRankMove());
            mplew.writeInt(chr.getJobRank());
            mplew.writeInt(chr.getJobRankMove());
        }
    }

    public static byte[] showAllCharacter(int chars) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(1); //bIsChar
        mplew.writeInt(chars);
        mplew.writeInt(chars + (3 - chars % 3)); //rowsize
        return mplew.getPacket();
    }

    public static byte[] showAllCharacterInfo(int worldid, List<MapleCharacter> chars, String pic) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeOpcode(SendPacketOpcode.ALL_CHARLIST.getValue());
        mplew.write(chars.size() == 0 ? 5 : 0); //5 = cannot find any
        mplew.write(worldid);
        mplew.write(chars.size());
        for (MapleCharacter chr : chars) {
            addCharEntry(mplew, chr, true);
        }
        mplew.write(pic == null ? 0 : (pic.equals("") ? 2 : 1)); //writing 2 here disables PIC		
        return mplew.getPacket();
    }
}
