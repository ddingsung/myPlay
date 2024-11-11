package handling.world;
//

import client.*;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.inventory.PetDataFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.channel.handler.DueyHandler;
import handling.channel.handler.PetHandler;
import handling.gm.GMPacket;
import handling.gm.GMServer;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleBBSThread;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.guild.MapleGuildCharacter;
import server.ItemInfo;
import server.MapleStatEffect;
import server.Timer.WorldTimer;
import server.life.MapleMonster;
import server.log.LogType;
import server.log.ServerLogger;
import server.maps.MapleDoor;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMessageBox;
import server.quest.MapleQuest;
import tools.CollectionUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.PetPacket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static handling.gm.GMServer.broadcastConnection;
import handling.world.exped.ExpeditionType;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import tools.data.ByteArrayByteStream;
import tools.data.LittleEndianAccessor;
import tools.packet.PartyPacket;

public class World {

    //Touch everything...
    public static void init() {
        World.Find.findChannel(0);
        World.Alliance.lock.toString();
        World.Messenger.getMessenger(0);
        World.Party.getParty(0);
    }

    public static boolean sendPackage(String receiver, int itemId, int quantity, String sender, String msg) {
        int cid = MapleCharacterUtil.getIdByName(receiver);
        if (cid == -1) {
            return false;
        }
        if (!ItemInfo.isExist(itemId)) {
            return false;
        }
        int channel = World.Find.findChannel(cid);
        if (channel >= 0) {
            World.Broadcast.sendPacket(cid, MaplePacketCreator.sendDuey((byte) 28, null, null));
            World.Broadcast.sendPacket(cid, MaplePacketCreator.serverNotice(5, "아이템이 지급되었습니다. NPC 택배원 <듀이> 에게서 아이템을 수령하세요!"));
        }
        return DueyHandler.addNewItemToDb(itemId, quantity, cid, sender, msg, channel >= 0);
    }

    public static String getStatus() {
        StringBuilder ret = new StringBuilder();
        int totalUsers = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            ret.append("Channel ");
            ret.append(cs.getChannel());
            ret.append(": ");
            int channelUsers = cs.getConnectedClients();
            totalUsers += channelUsers;
            ret.append(channelUsers);
            ret.append(" users\n");
        }
        ret.append("Total users online: ");
        ret.append(totalUsers);
        ret.append("\n");
        return ret.toString();
    }

    public static Map<Integer, Integer> getConnected() {
        Map<Integer, Integer> ret = new HashMap<Integer, Integer>();
        int total = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            int curConnected = cs.getConnectedClients();
            ret.put(cs.getChannel(), curConnected);
            total += curConnected;
        }
        int cashShop = World.getStorage(-10).getAllCharacters().size();
        total += cashShop;
        ret.put(-10, cashShop);
        ret.put(0, total);
        return ret;
    }

    public static List<CheaterData> getCheaters() {
        List<CheaterData> allCheaters = new ArrayList<CheaterData>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            allCheaters.addAll(cs.getCheaters());
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    public static List<CheaterData> getReports() {
        List<CheaterData> allCheaters = new ArrayList<CheaterData>();
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            allCheaters.addAll(cs.getReports());
        }
        Collections.sort(allCheaters);
        return CollectionUtil.copyFirst(allCheaters, 20);
    }

    public static boolean isConnected(String charName) {
        return Find.findChannel(charName) > 0;
    }

    public static void toggleMegaphoneMuteState() {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.toggleMegaphoneMuteState();
        }
    }

    public static void ChannelChange_Data(CharacterTransfer Data, int characterid, int toChannel) {
        getStorage(toChannel).registerPendingPlayer(Data, characterid);
    }

    public static boolean isCharacterListConnected(List<String> charName) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            for (final String c : charName) {
                if (cs.getPlayerStorage().getCharacterByName(c) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasMerchant(int accountID, int characterID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            if (cs.containsMerchant(accountID, characterID)) {
                return true;
            }
        }
        return false;
    }

    public static Pair<Integer, Integer> findMerchant(int accountID, int characterID) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            Pair<Integer, Integer> ret = cs.findMerchant(accountID, characterID);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static MapleCharacter getCharacterById(int targetId) {
        int ch = Find.findChannel(targetId);
        if (ch < 0) {
            return null;
        }
        final MapleCharacter c = getStorage(ch).getCharacterById(targetId);
        return c;
    }

    public static MapleCharacter getCharacterByName(String name) {
        int ch = Find.findChannel(name);
        if (ch < 0) {
            return null;
        }
        final MapleCharacter c = getStorage(ch).getCharacterByName(name);
        return c;
    }

    public static PlayerStorage getStorage(int channel) {
        if (channel == -10) {
            return CashShopServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(channel).getPlayerStorage();
    }

    public static int getPendingCharacterSize() {
        int ret = CashShopServer.getPlayerStorage().pendingCharacterSize();
        for (ChannelServer cserv : ChannelServer.getAllInstances()) {
            ret += cserv.getPlayerStorage().pendingCharacterSize();
        }
        return ret;
    }

    public static boolean isChannelAvailable(final int ch) {
        if (ChannelServer.getInstance(ch) == null || ChannelServer.getInstance(ch).getPlayerStorage() == null) {
            return false;
        }
        return ChannelServer.getInstance(ch).getPlayerStorage().getConnectedClients() < (ch == 1 ? 600 : 400);
    }

    public static class Party {

        private static Map<Integer, MapleParty> parties = new HashMap<Integer, MapleParty>();
        private static Map<Integer, MapleExpedition> expeds = new HashMap<Integer, MapleExpedition>();
        private static Map<PartySearchType, List<PartySearch>> searches = new EnumMap<PartySearchType, List<PartySearch>>(PartySearchType.class);
        private static final AtomicInteger runningPartyId = new AtomicInteger(1), runningExpedId = new AtomicInteger(1);

        static {
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("UPDATE characters SET party = -1");
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (Exception e) {
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
            }
            for (PartySearchType pst : PartySearchType.values()) {
                searches.put(pst, new ArrayList<PartySearch>()); //according to client, max 10, even though theres page numbers ?!
            }
        }

        public static void expedChat(int expedId, String chattext, String namefrom) {
            MapleExpedition party = getExped(expedId);
            if (party == null) {
                return;
            }
            for (int i : party.getParties()) {
                partyChat(i, chattext, namefrom, 4);
            }
        }

        public static void expedPacket(int expedId, byte[] packet, MaplePartyCharacter exception) {
            MapleExpedition party = getExped(expedId);
            if (party == null) {
                return;
            }
            for (int i : party.getParties()) {
                partyPacket(i, packet, exception);
            }
        }

        public static void partyChat(int partyid, String chattext, String namefrom) {
            partyChat(partyid, chattext, namefrom, 1);
        }

        public static void partyPacket(int partyid, byte[] packet, MaplePartyCharacter exception) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0 && (exception == null || partychar.getId() != exception.getId())) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) { //Extra check just in case
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        }

        public static void partyChat(int partyid, String chattext, String namefrom, int mode) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            String targets = "";
            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null && !chr.getName().equalsIgnoreCase(namefrom)) { //Extra check just in case
                        targets += chr.getName() + ", ";
                        chr.getClient().getSession().write(MaplePacketCreator.multiChat(namefrom, chattext, mode));
                        if (chr.getClient().isMonitored()) {
                            World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM Message] " + namefrom + " said to " + chr.getName() + " (Party): " + chattext));
                        }
                    }
                }
            }

            ServerLogger.getInstance().logChat(LogType.Chat.Party, partyid, namefrom, chattext, "수신 : " + targets);
        }

        public static void partyMessage(int partyid, String chattext) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) { //Extra check just in case
                        chr.dropMessage(5, chattext);
                    }
                }
            }
        }

        public static void expedMessage(int expedId, String chattext) {
            MapleExpedition party = getExped(expedId);
            if (party == null) {
                return;
            }
            for (int i : party.getParties()) {
                partyMessage(i, chattext);
            }
        }

        public static void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return; //Don't update, just return. And definitely don't throw a damn exception.
                //throw new IllegalArgumentException("no party with the specified partyid exists");
            }
            final int oldExped = party.getExpeditionId();
            int oldInd = -1;
            if (oldExped > 0) {
                MapleExpedition exped = getExped(oldExped);
                if (exped != null) {
                    oldInd = exped.getIndex(partyid);
                }
            }
            switch (operation) {
                case JOIN:
                    party.addMember(target);
                    MapleMap.changeMapOwner(null, target.getId(), true);
                    MapleMap.changeMapOwner(null, party.getLeader().getId(), true);
                    break;
                case EXPEL:
                case LEAVE:
                    party.removeMember(target);
                    break;
                case DISBAND:
                    disbandParty(partyid);
                    break;
                case SILENT_UPDATE:
                case LOG_ONOFF:
                    party.updateMember(target);
                    break;
                case CHANGE_LEADER:
                case CHANGE_LEADER_DC:
                    party.setLeader(target);
                    break;
                default:
                    throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
            }
            if (operation == PartyOperation.LEAVE || operation == PartyOperation.EXPEL) {
                int chz = Find.findChannel(target.getName());
                if (chz > 0) {
                    MapleCharacter chr = getStorage(chz).getCharacterByName(target.getName());
                    if (chr != null) {
                        MapleMap.changeMapOwner(chr, chr.getId(), false);
                        chr.setParty(null);
                        if (chr.getDoors().size() == 2) {
                            try {
                                //door owner's leave party
                                chr.getDoors().get(0).leavePartyDoorOwner(party);
                                chr.getDoors().get(1).leavePartyDoorOwner(party);
                                chr.getDoors().get(0).updateTownDoorPosition(null);
                                chr.getDoors().get(1).updateTownDoorPosition(null);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, e);
                            }
                        }

                        for (MaplePartyCharacter ppp : party.getMembers()) {
                            //else door owner's leave party
                            if (ppp.getId() == target.getId()) {
                                continue;
                            }
                            int chzz = Find.findChannel(ppp.getName());
                            if (chzz == chz) {
                                MapleCharacter pppc = getStorage(chzz).getCharacterByName(ppp.getName());
                                if (pppc.getDoors().size() == 2) {
                                    try {
                                        MapleDoor door1 = pppc.getDoors().get(0);
                                        MapleDoor door2 = pppc.getDoors().get(1);
                                        door1.leavePartyElseDoorOwner(chr.getClient());
                                        door2.leavePartyElseDoorOwner(chr.getClient());
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, e);
                                    }
                                }
                            }
                        }
                        chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));

                        if (chr.getDoors().size() == 2) {
                            try {
                                chr.getDoors().get(0).sendSinglePortal();
                                chr.getDoors().get(1).sendSinglePortal();
                            } catch (ArrayIndexOutOfBoundsException e) {
                            }
                        }
                    }
                }

                if (target.getId() == party.getLeader().getId() && party.getMembers().size() > 0) { //pass on lead
                    MaplePartyCharacter lchr = null;
                    for (MaplePartyCharacter pchr : party.getMembers()) {
                        if (pchr != null && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                            lchr = pchr;
                        }
                    }
                    if (lchr != null) {
                        updateParty(partyid, PartyOperation.CHANGE_LEADER_DC, lchr);
                    }
                }
            }
            if (party.getMembers().size() <= 0) { //no members left, plz disband
                disbandParty(partyid);
            }
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar == null) {
                    continue;
                }
                int ch = Find.findChannel(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = getStorage(ch).getCharacterByName(partychar.getName());
                    if (chr != null) {
                        if (operation == PartyOperation.DISBAND) {
                            MapleMap.changeMapOwner(chr, chr.getId(), false);
                            chr.setParty(null);
                        } else {
                            chr.setParty(party);
                        }
                        chr.getClient().getSession().write(MaplePacketCreator.updateParty(chr.getClient().getChannel(), party, operation, target));
                    }
                }
            }

            if (operation == PartyOperation.JOIN) {
                int chz = Find.findChannel(target.getName());
                if (chz > 0) {
                    MapleCharacter chr = getStorage(chz).getCharacterByName(target.getName());
                    if (chr != null) {
                        if (chr.getDoors().size() == 2) {
                            try {
                                //door owner's join party
                                chr.getDoors().get(0).joinPartyDoorOwner(party);
                                chr.getDoors().get(1).joinPartyDoorOwner(party);
                                chr.getDoors().get(0).updateTownDoorPosition(party);
                                chr.getDoors().get(1).updateTownDoorPosition(party);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, e);
                            }
                        }

                        for (MaplePartyCharacter ppp : party.getMembers()) {
                            //else door owner's join party
                            int chzz = Find.findChannel(ppp.getName());
                            if (chzz == chz) {
                                MapleCharacter pppc = getStorage(chzz).getCharacterByName(ppp.getName());
                                if (pppc.getDoors().size() == 2) {
                                    try {
                                        MapleDoor door1 = pppc.getDoors().get(0);
                                        MapleDoor door2 = pppc.getDoors().get(1);
                                        door1.joinPartyElseDoorOwner(chr.getClient());
                                        door2.joinPartyElseDoorOwner(chr.getClient());
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        Logger.getLogger(World.class.getName()).log(Level.SEVERE, null, e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (oldExped > 0) {
                expedPacket(oldExped, PartyPacket.expeditionUpdate(oldInd, party), operation == PartyOperation.LOG_ONOFF || operation == PartyOperation.SILENT_UPDATE ? target : null);
            }
        }

        public static MapleParty createParty(MaplePartyCharacter chrfor) {
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor);
            parties.put(party.getId(), party);
            MapleMap.changeMapOwner(null, chrfor.getId(), true);
            return party;
        }

        public static MapleParty createParty(MaplePartyCharacter chrfor, int expedId) {
            ExpeditionType ex = ExpeditionType.getById(expedId);
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, ex != null ? runningExpedId.getAndIncrement() : -1);
            parties.put(party.getId(), party);
            if (ex != null) {
                final MapleExpedition exp = new MapleExpedition(ex, chrfor.getId(), party.getExpeditionId());
                exp.getParties().add(party.getId());
                expeds.put(party.getExpeditionId(), exp);
            }
            MapleMap.changeMapOwner(null, chrfor.getId(), true);
            return party;
        }

        public static MapleParty createPartyAndAdd(MaplePartyCharacter chrfor, int expedId) {
            MapleExpedition ex = getExped(expedId);
            if (ex == null) {
                return null;
            }
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, expedId);
            parties.put(party.getId(), party);
            ex.getParties().add(party.getId());
            MapleMap.changeMapOwner(null, chrfor.getId(), true);
            return party;
        }

        public static MapleParty getParty(int partyid) {
            return parties.get(partyid);
        }

        public static MapleExpedition getExped(int partyid) {
            return expeds.get(partyid);
        }

        public static MapleParty disbandParty(int partyid) {
            final MapleParty ret = parties.remove(partyid);
            if (ret == null) {
                return null;
            }
            if (ret.getExpeditionId() > 0) {
                MapleExpedition exped = getExped(ret.getExpeditionId());
                if (exped != null) {
                    final int ind = exped.getIndex(partyid);
                    if (ind >= 0) {
                        exped.getParties().remove(ind);
                        expedPacket(exped.getId(), PartyPacket.expeditionUpdate(ind, null), null);
                    }
                }
            }
            ret.disband();
            return ret;
        }

        public static MapleExpedition disbandExped(int partyid) {
            PartySearch toRemove = getSearchByExped(partyid);
            if (toRemove != null) {
                removeSearch(toRemove, "파티가 해체되어 파티광고가 삭제되었습니다.");
            }
            final MapleExpedition ret = expeds.remove(partyid);
            if (ret != null) {
                for (int p : ret.getParties()) {
                    MapleParty pp = getParty(p);
                    if (pp != null) {
                        updateParty(p, PartyOperation.DISBAND, pp.getLeader());
                    }
                }
            }
            return ret;
        }

        public static List<PartySearch> searchParty(PartySearchType pst) {
            return searches.get(pst);
        }

        public static void removeSearch(PartySearch ps, String text) {
            List<PartySearch> ss = searches.get(ps.getType());
            if (ss.contains(ps)) {
                ss.remove(ps);
                ps.cancelRemoval();
                if (ps.getType().exped) {
                    expedMessage(ps.getId(), text);
                } else {
                    partyMessage(ps.getId(), text);
                }
            }
        }

        public static void addSearch(PartySearch ps) {
            searches.get(ps.getType()).add(ps);
        }

        public static PartySearch getSearch(MapleParty party) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if ((p.getId() == party.getId() && !p.getType().exped) || (p.getId() == party.getExpeditionId() && p.getType().exped)) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static PartySearch getSearchByParty(int partyId) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if (p.getId() == partyId && !p.getType().exped) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static PartySearch getSearchByExped(int partyId) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if (p.getId() == partyId && p.getType().exped) {
                        return p;
                    }
                }
            }
            return null;
        }

    }

    public static class Buddy {

        public static void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) {
            String targets = "";
            for (int characterId : recipientCharacterIds) {
                int ch = Find.findChannel(characterId);
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(characterId);
                    if (chr != null && chr.getBuddylist().containsVisible(cidFrom)) {
                        chr.getClient().getSession().write(MaplePacketCreator.multiChat(nameFrom, chattext, 0));
                        targets += chr.getName() + ", ";
                        if (chr.getClient().isMonitored()) {
                            World.Broadcast.broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM Message] " + nameFrom + " said to " + chr.getName() + " (Buddy): " + chattext));
                        }
                    }
                }
            }

            ServerLogger.getInstance().logChat(LogType.Chat.Buddy, cidFrom, nameFrom, chattext, "수신 : " + targets);
        }

        private static void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
            for (int buddy : buddies) {
                int ch = Find.findChannel(buddy);
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(buddy);
                    if (chr != null) {
                        BuddylistEntry ble = chr.getBuddylist().get(characterId);
                        if (ble != null && ble.isVisible()) {
                            int mcChannel;
                            if (offline) {
                                ble.setChannel(-1);
                                mcChannel = -1;
                            } else {
                                ble.setChannel(channel);
                                mcChannel = channel - 1;
                            }
                            chr.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                        }
                    }
                }
            }
        }

        public static void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation, String group) {
            int ch = Find.findChannel(cid);
            if (ch > 0) {
                final MapleCharacter addChar = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(cid);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    switch (operation) {
                        case ADDED:
                            if (buddylist.contains(cidFrom)) {
                                buddylist.put(new BuddylistEntry(name, cidFrom, group, channel, true));
                                addChar.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(cidFrom, channel - 1));
                            }
                            break;
                        case DELETED:
                            if (buddylist.contains(cidFrom)) {
                                buddylist.put(new BuddylistEntry(name, cidFrom, group, -1, buddylist.get(cidFrom).isVisible()));
                                addChar.getClient().getSession().write(MaplePacketCreator.updateBuddyChannel(cidFrom, -1));
                            }
                            break;
                    }
                }
            }
        }

        public static BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom, int levelFrom, int jobFrom) {
            int ch = Find.findChannel(addName);
            if (ch > 0) {
                final MapleCharacter addChar = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(addName);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    if (buddylist.isFull()) {
                        return BuddyAddResult.BUDDYLIST_FULL;
                    }
                    if (!buddylist.contains(cidFrom)) {
                        buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom, levelFrom, jobFrom);
                    } else if (buddylist.containsVisible(cidFrom)) {
                        return BuddyAddResult.ALREADY_ON_LIST;
                    }
                }
            }
            return BuddyAddResult.OK;
        }

        public static void loggedOn(String name, int characterId, int channel, int[] buddies) {
            updateBuddies(characterId, channel, buddies, false);
        }

        public static void loggedOff(String name, int characterId, int channel, int[] buddies) {
            updateBuddies(characterId, channel, buddies, true);
        }
    }

    public static class Messenger {

        private static Map<Integer, MapleMessenger> messengers = new HashMap<Integer, MapleMessenger>();
        private static final AtomicInteger runningMessengerId = new AtomicInteger();

        static {
            runningMessengerId.set(1);
        }

        public static MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
            int messengerid = runningMessengerId.getAndIncrement();
            MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
            messengers.put(messenger.getId(), messenger);
            return messenger;
        }

        public static void declineChat(String target, String namefrom) {
            int ch = Find.findChannel(target);
            if (ch > 0) {
                ChannelServer cs = ChannelServer.getInstance(ch);
                MapleCharacter chr = cs.getPlayerStorage().getCharacterByName(target);
                if (chr != null) {
                    MapleMessenger messenger = chr.getMessenger();
                    if (messenger != null) {
                        chr.getClient().getSession().write(MaplePacketCreator.messengerNote(namefrom, 5, 0));
                    }
                }
            }
        }

        public static MapleMessenger getMessenger(int messengerid) {
            return messengers.get(messengerid);
        }

        public static void leaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            int position = messenger.getPositionByName(target.getName());
            messenger.removeMember(target);

            for (MapleMessengerCharacter mmc : messenger.getMembers()) {
                if (mmc != null) {
                    int ch = Find.findChannel(mmc.getId());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(mmc.getName());
                        if (chr != null) {
                            chr.getClient().getSession().write(MaplePacketCreator.removeMessengerPlayer(position));
                        }
                    }
                }
            }
        }

        public static void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentRemoveMember(target);
        }

        public static void silentJoinMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentAddMember(target);
        }

        public static void updateMessenger(int messengerid, String namefrom, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            int position = messenger.getPositionByName(namefrom);

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            MapleCharacter from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                            chr.getClient().getSession().write(MaplePacketCreator.updateMessengerPlayer(namefrom, from, position, fromchannel - 1));
                        }
                    }
                }
            }
        }

        public static void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.addMember(target);
            int position = messenger.getPositionByName(target.getName());
            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null) {
                    int mposition = messenger.getPositionByName(messengerchar.getName());
                    int ch = Find.findChannel(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            if (!messengerchar.getName().equals(from)) {
                                MapleCharacter fromCh = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(from);
                                if (fromCh != null) {
                                    chr.getClient().getSession().write(MaplePacketCreator.addMessengerPlayer(from, fromCh, position, fromchannel - 1));
                                    fromCh.getClient().getSession().write(MaplePacketCreator.addMessengerPlayer(chr.getName(), chr, mposition, messengerchar.getChannel() - 1));
                                }
                            } else {
                                chr.getClient().getSession().write(MaplePacketCreator.joinMessenger(mposition));
                            }
                        }
                    }
                }
            }
        }

        public static void messengerChat(int messengerid, String chattext, String namefrom) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            chr.getClient().getSession().write(MaplePacketCreator.messengerChat(chattext));
                        }
                    }
                }
            }
        }

        public static void messengerInvite(String sender, int messengerid, String target, int fromchannel, boolean gm) {

            if (isConnected(target)) {

                int ch = Find.findChannel(target);
                if (ch > 0) {
                    MapleCharacter from = ChannelServer.getInstance(fromchannel).getPlayerStorage().getCharacterByName(sender);
                    MapleCharacter targeter = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(target);
                    if (targeter != null && targeter.getMessenger() == null) {
                        if (!targeter.isIntern() || gm) {
                            targeter.getClient().getSession().write(MaplePacketCreator.messengerInvite(sender, messengerid));
                            from.getClient().getSession().write(MaplePacketCreator.messengerNote(target, 4, 1));
                        } else {
                            from.getClient().getSession().write(MaplePacketCreator.messengerNote(target, 4, 0));
                        }
                    } else {
                        from.getClient().getSession().write(MaplePacketCreator.messengerChat(sender + " : " + target + " 님은 이미 메이플 메신저를 사용중입니다."));
                    }
                }
            }

        }
    }

    public static class Guild {

        private static final Map<Integer, MapleGuild> guilds = new LinkedHashMap<Integer, MapleGuild>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public static void addLoadedGuild(MapleGuild f) {
            if (f.isProper()) {
                guilds.put(f.getId(), f);
            }
        }

        public static int createGuild(int leaderId, String name) {
            return MapleGuild.createGuild(leaderId, name);
        }

        public static MapleGuild getGuild(int id) {
            MapleGuild ret = null;
            lock.readLock().lock();
            try {
                ret = guilds.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleGuild(id);
                    if (ret == null || ret.getId() <= 0 || !ret.isProper()) { //failed to load
                        return null;
                    }
                    guilds.put(id, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret; //Guild doesn't exist?
        }

        public static MapleGuild getGuildByName(String guildName) {
            lock.readLock().lock();
            try {
                for (MapleGuild g : guilds.values()) {
                    if (g.getName().equalsIgnoreCase(guildName)) {
                        return g;
                    }
                }
                return null;
            } finally {
                lock.readLock().unlock();
            }
        }

        public static MapleGuild getGuild(MapleCharacter mc) {
            return getGuild(mc.getGuildId());
        }

        public static void setGuildMemberOnline(MapleGuildCharacter mc, boolean bOnline, int channel) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.setOnline(mc.getId(), bOnline, channel);
            }
        }

        public static void guildPacket(int gid, byte[] message) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.broadcast(message);
            }
        }

        public static int addGuildMember(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                return g.addGuildMember(mc);
            }
            return 0;
        }

        public static void leaveGuild(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.leaveGuild(mc);
            }
        }

        public static void guildChat(int gid, String name, int cid, String msg) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.guildChat(name, cid, msg);
            }
        }

        public static void changeRank(int gid, int cid, int newRank) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeRank(cid, newRank);
            }
        }

        public static void expelMember(MapleGuildCharacter initiator, String name, int cid) {
            MapleGuild g = getGuild(initiator.getGuildId());
            if (g != null) {
                g.expelMember(initiator, name, cid);
            }
        }

        public static void setGuildNotice(int gid, String notice) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setGuildNotice(notice);
            }
        }

        public static void setGuildLeader(int gid, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeGuildLeader(cid);
            }
        }

        public static void memberLevelJobUpdate(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.memberLevelJobUpdate(mc);
            }
        }

        public static void changeRankTitle(int gid, String[] ranks) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeRankTitle(ranks);
            }
        }

        public static void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setGuildEmblem(bg, bgcolor, logo, logocolor);
            }
        }

        public static void disbandGuild(int gid) {
            MapleGuild g = getGuild(gid);
            lock.writeLock().lock();
            try {
                if (g != null) {
                    g.disbandGuild();
                    guilds.remove(gid);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void deleteGuildCharacter(int guildid, int charid) {

            //ensure it's loaded on world server
            //setGuildMemberOnline(mc, false, -1);
            MapleGuild g = getGuild(guildid);
            if (g != null) {
                MapleGuildCharacter mc = g.getMGC(charid);
                if (mc != null) {
                    if (mc.getGuildRank() > 1) //not leader
                    {
                        g.leaveGuild(mc);
                    } else {
                        g.disbandGuild();
                    }
                }
            }
        }

        public static boolean increaseGuildCapacity(int gid, boolean b) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.increaseCapacity(b);
            }
            return false;
        }

        public static void gainGP(int gid, int amount) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.gainGP(amount);
            }
        }

        public static void gainGP(int gid, int amount, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.gainGP(amount, false, cid);
            }
        }

        public static int getGP(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getGP();
            }
            return 0;
        }

        public static int getInvitedId(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getInvitedId();
            }
            return 0;
        }

        public static void setInvitedId(final int gid, final int inviteid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setInvitedId(inviteid);
            }
        }

        public static int getGuildLeader(final int guildName) {
            final MapleGuild mga = getGuild(guildName);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static int getGuildLeader(final String guildName) {
            final MapleGuild mga = getGuildByName(guildName);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void save() {
            System.out.println("Saving guilds...");
            lock.writeLock().lock();
            try {
                for (MapleGuild a : guilds.values()) {
                    a.writeToDB(false);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static List<MapleBBSThread> getBBS(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getBBS();
            }
            return null;
        }

        public static int addBBSThread(final int guildid, final String title, final String text, final int icon, final boolean bNotice, final int posterID) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                return g.addBBSThread(title, text, icon, bNotice, posterID);
            }
            return -1;
        }

        public static final void editBBSThread(final int guildid, final int localthreadid, final String title, final String text, final int icon, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.editBBSThread(localthreadid, title, text, icon, posterID, guildRank);
            }
        }

        public static final void deleteBBSThread(final int guildid, final int localthreadid, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.deleteBBSThread(localthreadid, posterID, guildRank);
            }
        }

        public static final void addBBSReply(final int guildid, final int localthreadid, final String text, final int posterID) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.addBBSReply(localthreadid, text, posterID);
            }
        }

        public static final void deleteBBSReply(final int guildid, final int localthreadid, final int replyid, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.deleteBBSReply(localthreadid, replyid, posterID, guildRank);
            }
        }

        public static void changeEmblem(int gid, int affectedPlayers, MapleGuild mgs) {
            Broadcast.sendGuildPacket(affectedPlayers, MaplePacketCreator.guildEmblemChange(gid, (short) mgs.getLogoBG(), (byte) mgs.getLogoBGColor(), (short) mgs.getLogo(), (byte) mgs.getLogoColor()), -1, gid);
            setGuildAndRank(affectedPlayers, -1, -1, -1, -1);    //respawn player
        }

        public static void setGuildAndRank(int cid, int guildid, int rank, int contribution, int alliancerank) {
            int ch = Find.findChannel(cid);
            if (ch == -1) {
                // System.out.println("ERROR: cannot find player in given channel");
                return;
            }
            MapleCharacter mc = getStorage(ch).getCharacterById(cid);
            if (mc == null) {
                return;
            }
            boolean bDifferentGuild;
            if (guildid == -1 && rank == -1) { //just need a respawn
                bDifferentGuild = true;
            } else {
                bDifferentGuild = guildid != mc.getGuildId();
                mc.setGuildId(guildid);
                mc.setGuildRank((byte) rank);
                mc.setGuildContribution(contribution);
                mc.setAllianceRank((byte) alliancerank);
                mc.saveGuildStatus();
            }
            if (bDifferentGuild && ch > 0) {
                mc.getMap().broadcastMessage(mc, MaplePacketCreator.loadGuildName(mc), false);
                mc.getMap().broadcastMessage(mc, MaplePacketCreator.loadGuildIcon(mc), false);
            }
        }
    }

    public static class Broadcast {

        public static void broadcastSmega(byte[] message) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.broadcastSmega(message);
            }
        }

        public static void broadcastGMMessage(byte[] message) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.broadcastGMMessage(message);
            }
        }

        public static void broadcastMessage(byte[] message) {
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.broadcastMessage(message);
            }
        }

        public static void sendPacket(List<Integer> targetIds, byte[] packet, int exception) {
            MapleCharacter c;
            for (int i : targetIds) {
                if (i == exception) {
                    continue;
                }
                int ch = Find.findChannel(i);
                if (ch < 0) {
                    continue;
                }
                c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(i);
                if (c != null) {
                    c.getClient().getSession().write(packet);
                }
            }
        }

        public static void sendPacket(int targetId, byte[] packet) {
            int ch = Find.findChannel(targetId);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetId);
            if (c != null) {
                c.getClient().getSession().write(packet);
            }
        }

        public static void sendGuildPacket(int targetIds, byte[] packet, int exception, int guildid) {
            if (targetIds == exception) {
                return;
            }
            int ch = Find.findChannel(targetIds);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetIds);
            if (c != null && c.getGuildId() == guildid) {
                c.getClient().getSession().write(packet);
            }
        }

        public static void sendFamilyPacket(int targetIds, byte[] packet, int exception, int guildid) {
            if (targetIds == exception) {
                return;
            }
            int ch = Find.findChannel(targetIds);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetIds);
            if (c != null && c.getFamilyId() == guildid) {
                c.getClient().getSession().write(packet);
            }
        }
    }

    public static class Find {

        private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private static HashMap<Integer, Integer> idToChannel = new HashMap<Integer, Integer>();
        private static HashMap<String, Integer> nameToChannel = new HashMap<String, Integer>();

        public static void register(int id, String name, int channel) {
            lock.writeLock().lock();
            try {
                idToChannel.put(id, channel);
                nameToChannel.put(name.toLowerCase(), channel);
                GMServer.broadcast(GMPacket.appendLog("접속", name));
            } finally {
                lock.writeLock().unlock();
            }
            broadcastConnection(null);
            //System.out.println("Char added: " + id + " " + name + " to channel " + channel);
        }

        public static void forceDeregister(int id) {
            lock.writeLock().lock();
            try {
                idToChannel.remove(id);
            } finally {
                lock.writeLock().unlock();
            }
            //System.out.println("Char removed: " + id);
        }

        public static void forceDeregister(String id) {
            lock.writeLock().lock();
            try {
                nameToChannel.remove(id.toLowerCase());
            } finally {
                lock.writeLock().unlock();
            }
            //System.out.println("Char removed: " + id);
        }

        public static void forceDeregister(int id, String name) {
            lock.writeLock().lock();
            try {
                idToChannel.remove(id);
                nameToChannel.remove(name.toLowerCase());
                GMServer.broadcast(GMPacket.appendLog("해제", name));
            } finally {
                lock.writeLock().unlock();
            }
            broadcastConnection(null);
            //System.out.println("Char removed: " + id + " " + name);
        }

        public static int size() {
            lock.writeLock().lock();
            try {
                return idToChannel.size();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static int findChannel(int id) {
            Integer ret;
            lock.readLock().lock();
            try {
                ret = idToChannel.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret) == null) { //wha
                    forceDeregister(id);
                    return -1;
                }
                return ret;
            }
            return -1;
        }

        public static int findChannel(String st) {
            Integer ret;
            lock.readLock().lock();
            try {
                ret = nameToChannel.get(st.toLowerCase());
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret) == null) { //wha
                    forceDeregister(st);
                    return -1;
                }
                return ret;
            }
            return -1;
        }

        public static CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
            List<CharacterIdChannelPair> foundsChars = new ArrayList<CharacterIdChannelPair>(characterIds.length);
            for (int i : characterIds) {
                int channel = findChannel(i);
                if (channel > 0) {
                    foundsChars.add(new CharacterIdChannelPair(i, channel));
                }
            }
            Collections.sort(foundsChars);
            return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
        }
    }

    public static class Alliance {

        private static final Map<Integer, MapleGuildAlliance> alliances = new LinkedHashMap<Integer, MapleGuildAlliance>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        static {
            Collection<MapleGuildAlliance> allGuilds = MapleGuildAlliance.loadAll();
            for (MapleGuildAlliance g : allGuilds) {
                alliances.put(g.getId(), g);
            }
        }

        public static MapleGuildAlliance getAlliance(final int allianceid) {
            MapleGuildAlliance ret = null;
            lock.readLock().lock();
            try {
                ret = alliances.get(allianceid);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleGuildAlliance(allianceid);
                    if (ret == null || ret.getId() <= 0) { //failed to load
                        return null;
                    }
                    alliances.put(allianceid, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret;
        }

        public static int getAllianceLeader(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void updateAllianceRanks(final int allianceid, final String[] ranks) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setRank(ranks);
            }
        }

        public static void updateAllianceNotice(final int allianceid, final String notice) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setNotice(notice);
            }
        }

        public static boolean canInvite(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getCapacity() > mga.getNoGuilds();
            }
            return false;
        }

        public static boolean changeAllianceLeader(final int allianceid, final int cid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setLeaderId(cid);
            }
            return false;
        }

        public static boolean changeAllianceLeader(final int allianceid, final int cid, final boolean sameGuild) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setLeaderId(cid, sameGuild);
            }
            return false;
        }

        public static boolean changeAllianceRank(final int allianceid, final int cid, final int change) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.changeAllianceRank(cid, change);
            }
            return false;
        }

        public static boolean changeAllianceCapacity(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setCapacity();
            }
            return false;
        }

        public static boolean disbandAlliance(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.disband();
            }
            return false;
        }

        public static boolean addGuildToAlliance(final int allianceid, final int gid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.addGuild(gid);
            }
            return false;
        }

        public static boolean removeGuildFromAlliance(final int allianceid, final int gid, final boolean expelled) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.removeGuild(gid, expelled);
            }
            return false;
        }

        public static void sendGuild(final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                sendGuild(MaplePacketCreator.getAllianceUpdate(alliance), -1, allianceid);
                sendGuild(MaplePacketCreator.getGuildAlliance(alliance), -1, allianceid);
            }
        }

        public static void sendGuild(final byte[] packet, final int exceptionId, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    int gid = alliance.getGuildId(i);
                    if (gid > 0 && gid != exceptionId) {
                        Guild.guildPacket(gid, packet);
                    }
                }
            }
        }

        public static boolean createAlliance(final String alliancename, final int cid, final int cid2, final int gid, final int gid2) {
            final int allianceid = MapleGuildAlliance.createToDb(cid, alliancename, gid, gid2);
            if (allianceid <= 0) {
                return false;
            }
            final MapleGuild g = Guild.getGuild(gid), g_ = Guild.getGuild(gid2);
            g.setAllianceId(allianceid);
            g_.setAllianceId(allianceid);
            g.changeARank(true);
            g_.changeARank(false);

            final MapleGuildAlliance alliance = getAlliance(allianceid);

            sendGuild(MaplePacketCreator.createGuildAlliance(alliance), -1, allianceid);
            sendGuild(MaplePacketCreator.getAllianceInfo(alliance), -1, allianceid);
            sendGuild(MaplePacketCreator.getGuildAlliance(alliance), -1, allianceid);
            sendGuild(MaplePacketCreator.changeAlliance(alliance, true), -1, allianceid);
            return true;
        }

        public static void allianceChat(final int gid, final String name, final int cid, final String msg) {
            final MapleGuild g = Guild.getGuild(gid);
            if (g != null) {
                final MapleGuildAlliance ga = getAlliance(g.getAllianceId());
                if (ga != null) {
                    for (int i = 0; i < ga.getNoGuilds(); i++) {
                        final MapleGuild g_ = Guild.getGuild(ga.getGuildId(i));
                        if (g_ != null) {
                            g_.allianceChat(name, cid, msg);
                        }
                    }
                }
            }
        }

        public static void setNewAlliance(final int gid, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild guild = Guild.getGuild(gid);
            if (alliance != null && guild != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    if (gid == alliance.getGuildId(i)) {
                        guild.setAllianceId(allianceid);
                        guild.broadcast(MaplePacketCreator.getAllianceInfo(alliance));
                        guild.broadcast(MaplePacketCreator.getGuildAlliance(alliance));
                        guild.broadcast(MaplePacketCreator.changeAlliance(alliance, true));
                        guild.changeARank();
                        guild.writeToDB(false);
                    } else {
                        final MapleGuild g_ = Guild.getGuild(alliance.getGuildId(i));
                        if (g_ != null) {
                            g_.broadcast(MaplePacketCreator.addGuildToAlliance(alliance, guild));
                            g_.broadcast(MaplePacketCreator.changeGuildInAlliance(alliance, guild, true));
                        }
                    }
                }
            }
        }

        public static void setOldAlliance(final int gid, final boolean expelled, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild g_ = Guild.getGuild(gid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    final MapleGuild guild = Guild.getGuild(alliance.getGuildId(i));
                    if (guild == null) {
                        if (gid != alliance.getGuildId(i)) {
                            alliance.removeGuild(gid, false, true);
                        }
                        continue; //just skip
                    }
                    if (g_ == null) {
                        guild.changeARank(5);
                        guild.setAllianceId(0);
                        guild.broadcast(MaplePacketCreator.disbandAlliance(allianceid));
                    } else if (g_ != null) {
                        if (gid != alliance.getGuildId(i)) {
                            guild.broadcast(MaplePacketCreator.changeGuildInAlliance(alliance, g_, false));
                        }
                        guild.broadcast(MaplePacketCreator.removeGuildFromAlliance(alliance, gid, g_, expelled));
                    }

                }
            }

            if (gid == -1) {
                lock.writeLock().lock();
                try {
                    alliances.remove(allianceid);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        public static List<byte[]> getAllianceInfo(final int allianceid, final boolean start) {
            List<byte[]> ret = new ArrayList<byte[]>();
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                if (start) {
                    ret.add(MaplePacketCreator.getAllianceInfo(alliance));
                    ret.add(MaplePacketCreator.getGuildAlliance(alliance));
                }
                ret.add(MaplePacketCreator.getAllianceUpdate(alliance));
            }
            return ret;
        }

        public static void save() {
            System.out.println("Saving alliances...");
            lock.writeLock().lock();
            try {
                for (MapleGuildAlliance a : alliances.values()) {
                    a.saveToDb();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public static class Family {

        private static final Map<Integer, MapleFamily> families = new LinkedHashMap<Integer, MapleFamily>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public static void addLoadedFamily(MapleFamily f) {
            if (f.isProper()) {
                families.put(f.getId(), f);
            }
        }

        public static MapleFamily getFamily(int id) {
            MapleFamily ret = null;
            lock.readLock().lock();
            try {
                ret = families.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleFamily(id);
                    if (ret == null || ret.getId() <= 0 || !ret.isProper()) { //failed to load
                        return null;
                    }
                    families.put(id, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret;
        }

        public static void memberFamilyUpdate(MapleFamilyCharacter mfc, MapleCharacter mc) {
            MapleFamily f = getFamily(mfc.getFamilyId());
            if (f != null) {
                f.memberLevelJobUpdate(mc);
            }
        }

        public static void setFamilyMemberOnline(MapleFamilyCharacter mfc, boolean bOnline, int channel) {
            MapleFamily f = getFamily(mfc.getFamilyId());
            if (f != null) {
                f.setOnline(mfc.getId(), bOnline, channel);
            }
        }

        public static int setRep(int fid, int cid, int addrep, int oldLevel, String oldName) {
            MapleFamily f = getFamily(fid);
            if (f != null) {
                return f.setRep(cid, addrep, oldLevel, oldName);
            }
            return 0;
        }

        public static void save() {
            System.out.println("Saving families...");
            lock.writeLock().lock();
            try {
                for (MapleFamily a : families.values()) {
                    a.writeToDB(false);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void setFamily(int familyid, int seniorid, int junior1, int junior2, int currentrep, int totalrep, int cid) {
            int ch = Find.findChannel(cid);
            if (ch == -1) {
                // System.out.println("ERROR: cannot find player in given channel");
                return;
            }
            MapleCharacter mc = getStorage(ch).getCharacterById(cid);
            if (mc == null) {
                return;
            }
            boolean bDifferent = mc.getFamilyId() != familyid || mc.getSeniorId() != seniorid || mc.getJunior1() != junior1 || mc.getJunior2() != junior2;
            mc.setFamily(familyid, seniorid, junior1, junior2);
            mc.setCurrentRep(currentrep);
            mc.setTotalRep(totalrep);
            if (bDifferent) {
                mc.saveFamilyStatus();
            }
        }

        public static void familyPacket(int gid, byte[] message, int cid) {
            MapleFamily f = getFamily(gid);
            if (f != null) {
                f.broadcast(message, -1, f.getMFC(cid).getPedigree());
            }
        }

        public static void disbandFamily(int gid) {
            MapleFamily g = getFamily(gid);
            if (g != null) {
                lock.writeLock().lock();
                try {
                    families.remove(gid);
                } finally {
                    lock.writeLock().unlock();
                }
                g.disbandFamily();
            }
        }
    }

    private final static int CHANNELS_PER_THREAD = 1;

    public static void registerRespawn() {
        Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);
        for (int i = 0; i < chs.length; i += CHANNELS_PER_THREAD) {
            WorldTimer.getInstance().register(new Respawn(chs, i), 1000); //divisible by 9000 if possible.
            WorldTimer.getInstance().register(new DiseaseRunnable(chs, i), 1000); //test. is it expensive?
        }
        //3000 good or bad? ive no idea >_>
        //buffs can also be done, but eh
    }

    public static class DiseaseRunnable implements Runnable {

        private final List<ChannelServer> cservs = new ArrayList<ChannelServer>(CHANNELS_PER_THREAD);

        public DiseaseRunnable(Integer[] chs, int c) {
            StringBuilder s = new StringBuilder("[Fast Respawn Worker] Registered for channels ");
            for (int i = 1; i <= CHANNELS_PER_THREAD && chs.length >= (c + i); i++) {
                cservs.add(ChannelServer.getInstance(c + i));
                s.append(c + i).append(" ");
            }
            System.out.println(s.toString());
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            for (ChannelServer cserv : cservs) {
                if (!cserv.hasFinishedShutdown()) {
                    for (MapleMap map : cserv.getMapFactory().getAllLoadedMaps()) { //iterating through each map o_x
                        for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                            if (chr.getDiseaseSize() > 0) {
                                for (MapleDiseaseValueHolder m : chr.getAllDiseases()) {
                                    if (m != null && m.startTime + m.length < now) {
                                        chr.dispelDebuff(m.disease);
                                    }
                                }
                            }
                            chr.updateBonusExp(now);
                        }

                        if (map.getMobsSize() > 0) {
                            for (MapleMonster mons : map.getAllMonstersThreadsafe()) {
                                if (mons.isAlive() && mons.getStatiSize() > 0) {
                                    for (MonsterStatusEffect mse : mons.getAllBuffs()) {
                                        if ((mse.getStati() == MonsterStatus.BURNED || mse.getStati() == MonsterStatus.POISON)) {
                                            mons.doPoison(mse, mse.getWeakChr());
                                            /*if (mse.getStati() == MonsterStatus.VENOM) {
                                                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Venom Count : " + mse.getVenomCount() + " / Damage : " + mse.getX() + " / TotalBuffCount : " + mons.getAllBuffs().size()));
                                            } else if (mse.getStati() == MonsterStatus.POISON) {
                                                map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Poison Damage : " + mse.getX()));
                                            }*/
                                        }
                                        if (mse.shouldCancel(now)) {
                                            mons.cancelSingleStatus(mse);
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public static class Respawn implements Runnable { //is putting it here a good idea?

        private int numTimes = 0;
        private final List<ChannelServer> cservs = new ArrayList<ChannelServer>(CHANNELS_PER_THREAD);

        public Respawn(Integer[] chs, int c) {
            StringBuilder s = new StringBuilder("[Respawn Worker] Registered for channels ");
            for (int i = 1; i <= CHANNELS_PER_THREAD && chs.length >= (c + i); i++) {
                cservs.add(ChannelServer.getInstance(c + i));
                s.append(c + i).append(" ");
            }
            System.out.println(s.toString());
        }

        @Override
        public void run() {
            numTimes++;
            long now = System.currentTimeMillis();
            for (ChannelServer cserv : cservs) {
                if (!cserv.hasFinishedShutdown()) {
                    for (MapleMap map : cserv.getMapFactory().getAllLoadedMaps()) { //iterating through each map o_x
                        handleMap(map, numTimes, map.getCharactersSize(), now);
                    }
                }
            }
        }
    }

    public static void handleMap(final MapleMap map, final int numTimes, final int size, final long now) {
        if (map.getItemsSize() > 0 && !GameConstants.isPickupRestrictedMap(map.getId())) {
            for (MapleMapItem item : map.getAllItemsThreadsafe()) {
                if (item.shouldExpire(now)) {
                    if (map.getFieldLimit() != 270972) { //파티퀘스트 필드리밋 같은데
                        item.expire(map);
                    }
                } else if (item.shouldFFA(now)) {
                    item.setDropType((byte) 2);
                }
            }
        }
        if (map.getMessageBoxSize() > 0) {
            for (MapleMessageBox mmb : map.getAllMsgBoxesThreadsafe()) {
                if (mmb.shouldRemove()) {
                    mmb.expire(map);
                }
            }
        }
        /*
         if (map.characterSize() > 0 && map.getMobsSize() > 0) {
         for (MapleMonster mob : map.getAllMonstersThreadsafe()) {
         if (mob.getLastReceivedMovePacket() > 0 && mob.getLastReceivedMovePacket() + 60000L < System.currentTimeMillis()) {
         try {
         if (mob.getController() != null) {
         MapleCharacter chr = mob.getController();
         chr.stopControllingMonster(mob);
         if (map.getCharactersSize() == 1) {
         chr.getClient().setStop();
         }
         mob.setController(null);
         mob.setControllerHasAggro(false);
         }
         if (map.getCharactersSize() > 1) {
         map.updateMonsterController(mob);
         }
         } catch (Exception e) {
         //ignore
         }
         }
         }
         }
         */
        if (map.characterSize() > 0 || map.getId() == 931000500 || map.getId() == 100000002) { //jaira hack
            if (map.canSpawn(now)) {
                map.respawn(false, now);
            }
            boolean hurt = map.canHurt(now);
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                handleCooldowns(chr, numTimes, hurt, now);
            }
            if (map.getOutMapTime() > 0) {
                if (map.getOutMapTime() < System.currentTimeMillis()) {
                    if (map.getId() / 100000 == 9250) {
                        map.broadcastMessage(MaplePacketCreator.serverNotice(5, "시간이 다 되어 자동으로 퇴장되었습니다."));
                        MapleMap toMap = map.getForcedReturnMap();
                        for (MapleCharacter chrs : map.getCharacters()) {
                            chrs.changeMap(toMap, toMap.getPortal(0));
                        }
                    }
                    map.setOutMapTime(0); //reset
                }
            }
            if (map.getMobsSize() > 0) {
                for (MapleMonster mons : map.getAllMonstersThreadsafe()) {
                    if (mons.isAlive() && mons.shouldKill(now)) {
                        map.killMonster(mons);
                    } else if (mons.isAlive() && mons.shouldDrop(now)) {
                        mons.doDropItem(now);
                    }
                    if (mons.isAlive() && mons.getLastReceivedMovePacket() + 30000L < System.currentTimeMillis()) {
                        mons.setController(null);
                        mons.setControllerHasAggro(false);
                        map.updateMonsterController(mons);
                    }
                }
            }
        }
    }

    public static void handleCooldowns(final MapleCharacter chr, final int numTimes, final boolean hurt, final long now) { //is putting it here a good idea? expensive?
        if (chr.getCooldownSize() > 0) {
            for (MapleCoolDownValueHolder m : chr.getCooldowns()) {
                if (m.startTime + m.length < now) {
                    final int skil = m.skillId;
                    chr.removeCooldown(skil);
                    chr.getClient().getSession().write(MaplePacketCreator.skillCooldown(skil, 0));
                }
            }
        }
        //PC방 쓰레드
        chr.getPcManager();

        if (chr.getJob() == 131 || chr.getJob() == 132) {
            if (chr.canBlood(now)) {
                //chr.doBeholderHealing();
                chr.doDragonBlood();
            }
            /*if (chr.canBeholderBuff()) {
             chr.doBeholderHealing();
             }*/
        }
        MapleBuffStat[] buffs = {MapleBuffStat.BLUE_AURA, MapleBuffStat.DARK_AURA, MapleBuffStat.YELLOW_AURA};
        for (MapleBuffStat buff : buffs) {
            MapleBuffStatValueHolder bAura = chr.getBSVH(buff);
            if (bAura != null) {
                if (chr.getId() != bAura.cid) {
                    MapleCharacter pChr = chr.getMap().getCharacterById_InMap(bAura.cid);
                    if (pChr == null || chr.getTruePosition().distance(pChr.getPosition()) >= 500) {
                        chr.setSaveBSVH(bAura);
                        chr.cancelBuffStats(true, buff);
                    }
                }
            } else {
                bAura = chr.getSaveBSVH();
                if (bAura != null && bAura.startTime + bAura.localDuration > System.currentTimeMillis()) {
                    MapleCharacter pChr = chr.getMap().getCharacterById_InMap(bAura.cid);
                    if (pChr != null && chr.getTruePosition().distance(pChr.getPosition()) <= 500) {
                        bAura.effect.applyTo(pChr, chr, true, null, bAura.localDuration, false);
                        chr.setSaveBSVH(null);
                    }
                }
            }
        }
        chr.expirationTask2(true, false);
        if (chr.isAlive()) {
            if (chr.canRecover(now)) {
                chr.doRecovery();
            }
            if (chr.canPotentialRecover(now)) {
                chr.doPotentialRecovery();
            }
            if (chr.canHPRecover(now)) {
                chr.addHP((int) chr.getStat().getHealHP());
                //chr.dropMessage(6, "H힐함" + chr.getStat().getHealHP());
            }
            if (chr.canMPRecover(now)) {
                chr.addMP((int) chr.getStat().getHealMP());
                //chr.dropMessage(6, "M힐함" + chr.getStat().getHealMP());
            }
            if (chr.isSearchingParty()) {
                chr.searchParty();
            }
//            if (chr.canFairy(now)) {
//                chr.doFairy();
//            }
//            if (chr.canDOT(now)) {
//                chr.doDOT();
//            }
        }

        if (numTimes % 33 == 0 && chr.getMount() != null && chr.getMount().canTire(now)) {
            chr.getMount().increaseFatigue();
        }
        if (chr.getLastBattleTime() > 0 && chr.getLastBattleTime() + MapleMap.OWNER_TIME < now) {
            chr.getMap().calcMapOwner(chr);
            chr.setLastBattleTime(0);
        }
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_MONTH) == 1
                && cal.get(Calendar.HOUR_OF_DAY) == 0
                && cal.get(Calendar.MINUTE) == 0
                && cal.get(Calendar.SECOND) == 0) { //달의 첫달, 0시 0분 0초에 초기화
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DatabaseConnection.getConnection();
                ps = con.prepareStatement("DELETE FROM `medalranks`");
                ps.executeUpdate();
                ps.executeBatch();
                ps.close();
                System.out.println("sucessfully cleard MedalRanks.");
            } catch (Exception e) {
                e.printStackTrace(System.err);
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (Exception e) {
                    }
                }
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (Exception e) {
                    }
                }
            }

            if (chr.haveItem(1142014, 1, true, true) && chr.getQuestStatus(29503) >= 1) {
                chr.removeAllEquip(1142014, false);
                chr.dropMessage(5, "헤네시스 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
            if (chr.haveItem(1142015, 1, true, true) && chr.getQuestStatus(29503) >= 1) {
                chr.removeAllEquip(1142015, false);
                chr.dropMessage(5, "엘리니아 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
            if (chr.haveItem(1142016, 1, true, true) && chr.getQuestStatus(29503) >= 1) {
                chr.removeAllEquip(1142016, false);
                chr.dropMessage(5, "페리온 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
            if (chr.haveItem(1142017, 1, true, true) && chr.getQuestStatus(29503) >= 1) {
                chr.removeAllEquip(1142017, false);
                chr.dropMessage(5, "커닝시티 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
            if (chr.haveItem(1142018, 1, true, true) && chr.getQuestStatus(29503) >= 1) {
                chr.removeAllEquip(1142018, false);
                chr.dropMessage(5, "슬리피우드 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
            if (chr.haveItem(1142019, 1, true, true) && chr.getQuestStatus(29503) >= 1) {
                chr.removeAllEquip(1142019, false);
                chr.dropMessage(5, "노틸러스 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
            if (chr.haveItem(1142030, 1, true, true) && chr.getQuestStatus(29503) >= 1) {
                chr.removeAllEquip(1142030, false);
                chr.dropMessage(5, "리스항구 기부왕 자격이 박탈되어, 칭호가 회수되었습니다.");
            }
        }
        if (chr.getQuestStatus(29003) == 1 && numTimes % 9 == 0) {
            MapleQuestStatus q = chr.getQuestNAdd(MapleQuest.getInstance(147100));   // date
            MapleQuestStatus q2 = chr.getQuestNAdd(MapleQuest.getInstance(147101));  // today secs
            MapleQuestStatus q3 = chr.getQuestNAdd(MapleQuest.getInstance(147102));  // total days
            MapleQuestStatus q4 = chr.getQuestNAdd(MapleQuest.getInstance(147103));  // today complete
            if (q.getCustomData() == null) {
                q.setCustomData("" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                q2.setCustomData("0");
                q3.setCustomData("0");
                q4.setCustomData("0");
            }
            if (!q.getCustomData().equals("" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH))) {
                //날이 바뀜
                q2.setCustomData("0");
                q4.setCustomData("0");
                q.setCustomData("" + Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            }

            if (q4.getCustomData().equals("0")) {
                q2.setCustomData(String.valueOf(Integer.parseInt(q2.getCustomData()) + 9));
                if (Integer.parseInt(q2.getCustomData()) >= 10800) {
                    q4.setCustomData("1");
                    q3.setCustomData(String.valueOf(Integer.parseInt(q3.getCustomData()) + 1));
                    if (q3.getCustomData().equals("27")) {
                        chr.showQuestCompletion(29003);
                    }
                }
                int f = Integer.parseInt(q2.getCustomData());
                if (f >= 257) {
                    chr.getClient().sendPacket(MaplePacketCreator.updateDiligentExplorer(f, Integer.parseInt(q3.getCustomData())));
                }
            }
        }

        for (MaplePet pet : chr.getSummonedPets()) { //달팽이
            if (pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0) {
                pet.setSecondsLeft(pet.getSecondsLeft() - 1);
                if (pet.getSecondsLeft() <= 0) {
                    chr.unequipPet(pet, true, true);
                    return;
                }
            }
        }

        if (numTimes % 164 == 0) { //we're parsing through the characters anyway (:
            for (MaplePet pet : chr.getSummonedPets()) {
                int newFullness = pet.getFullness() - PetDataFactory.getHunger(pet.getPetItemId());
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    chr.unequipPet(pet, false, true);
                } else {
                    pet.setFullness(newFullness);
                    chr.getClient().getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), true));
                }
            }
        }
        if (hurt && chr.isAlive()) {
            if (chr.getInventory(MapleInventoryType.EQUIPPED).findById(chr.getMap().getHPDecProtect()) == null) {
                int shouldDecHp = chr.getMap().getHPDec();
                MapleStatEffect itemeff = chr.getStatForBuff(MapleBuffStat.HP_LOSS_GUARD);
                switch (chr.getMapId() / 10000000) {
                    case 23:
                        if (chr.hasEquipped(1102061)) {
                            shouldDecHp = 0;
                        } else if (itemeff != null && itemeff.getSourceId() == 2022040) {
                            shouldDecHp = 0;
                        }
                        break;
                    case 21:
                        if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -9) != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -9).getFlag() == 4) {
                            shouldDecHp = 0;
                        } else if (itemeff != null && (itemeff.getSourceId() == 2022001 || itemeff.getSourceId() == 2022186)) {
                            shouldDecHp = 0;
                        }
                        break;
                }
                if (chr.getBuffedValue(MapleBuffStat.HP_LOSS_GUARD) != null) {
                    shouldDecHp = 0;
                    //   chr.dropMessage(6, "shouldDecHp: " + shouldDecHp);
                }

                if (shouldDecHp > 0) {
                    chr.addHP(-shouldDecHp);
                    //   chr.dropMessage(6, "shouldDecHp: " + shouldDecHp);
                }
            }
        }
    }
}
