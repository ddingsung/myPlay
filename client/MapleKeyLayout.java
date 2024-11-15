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
package client;

import tools.Pair;
import tools.data.MaplePacketLittleEndianWriter;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MapleKeyLayout implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private boolean changed = false;
    private Map<Integer, Pair<Byte, Integer>> keymap;

    public MapleKeyLayout() {
        keymap = new HashMap<Integer, Pair<Byte, Integer>>();
    }

    public MapleKeyLayout(Map<Integer, Pair<Byte, Integer>> keys) {
        keymap = keys;
    }

    public final Map<Integer, Pair<Byte, Integer>> Layout() {
        changed = true;
        return keymap;
    }

    public final void unchanged() {
        changed = false;
    }

    public final void writeData(final MaplePacketLittleEndianWriter mplew) {
        mplew.write(keymap.isEmpty() ? 1 : 0);
        if (keymap.isEmpty()) {
            keymap.put(Integer.valueOf(2), new Pair<Byte, Integer>((byte) 4, 10));
            keymap.put(Integer.valueOf(3), new Pair<Byte, Integer>((byte) 4, 12));
            keymap.put(Integer.valueOf(4), new Pair<Byte, Integer>((byte) 4, 13));
            keymap.put(Integer.valueOf(5), new Pair<Byte, Integer>((byte) 4, 18));
            keymap.put(Integer.valueOf(6), new Pair<Byte, Integer>((byte) 4, 0x17));
            keymap.put(Integer.valueOf(7), new Pair<Byte, Integer>((byte) 4, 28)); // 원정대에게
            keymap.put(Integer.valueOf(16), new Pair<Byte, Integer>((byte) 4, 8));
            keymap.put(Integer.valueOf(17), new Pair<Byte, Integer>((byte) 4, 5));
            keymap.put(Integer.valueOf(18), new Pair<Byte, Integer>((byte) 4, 0));
            keymap.put(Integer.valueOf(19), new Pair<Byte, Integer>((byte) 4, 4));
            keymap.put(Integer.valueOf(20), new Pair<Byte, Integer>((byte) 4, 27)); // 원정대
            keymap.put(Integer.valueOf(23), new Pair<Byte, Integer>((byte) 4, 1));
            keymap.put(Integer.valueOf(24), new Pair<Byte, Integer>((byte) 4, 0x18));
            keymap.put(Integer.valueOf(25), new Pair<Byte, Integer>((byte) 4, 19));
            keymap.put(Integer.valueOf(26), new Pair<Byte, Integer>((byte) 4, 14));
            keymap.put(Integer.valueOf(27), new Pair<Byte, Integer>((byte) 4, 15));
            keymap.put(Integer.valueOf(29), new Pair<Byte, Integer>((byte) 5, 52));
            keymap.put(Integer.valueOf(31), new Pair<Byte, Integer>((byte) 4, 2));
            keymap.put(Integer.valueOf(33), new Pair<Byte, Integer>((byte) 4, 0x19));
            keymap.put(Integer.valueOf(34), new Pair<Byte, Integer>((byte) 4, 17));
            keymap.put(Integer.valueOf(35), new Pair<Byte, Integer>((byte) 4, 11));
            keymap.put(Integer.valueOf(37), new Pair<Byte, Integer>((byte) 4, 3));
            keymap.put(Integer.valueOf(38), new Pair<Byte, Integer>((byte) 4, 20));
            keymap.put(Integer.valueOf(39), new Pair<Byte, Integer>((byte) 4, 26)); // 훈장
            keymap.put(Integer.valueOf(40), new Pair<Byte, Integer>((byte) 4, 16));
            keymap.put(Integer.valueOf(41), new Pair<Byte, Integer>((byte) 4, 0x16));
            keymap.put(Integer.valueOf(43), new Pair<Byte, Integer>((byte) 4, 9));
            keymap.put(Integer.valueOf(44), new Pair<Byte, Integer>((byte) 5, 50));
            keymap.put(Integer.valueOf(45), new Pair<Byte, Integer>((byte) 5, 51));
            keymap.put(Integer.valueOf(46), new Pair<Byte, Integer>((byte) 4, 6));
            keymap.put(Integer.valueOf(50), new Pair<Byte, Integer>((byte) 4, 7));
            keymap.put(Integer.valueOf(56), new Pair<Byte, Integer>((byte) 5, 53));
            keymap.put(Integer.valueOf(57), new Pair<Byte, Integer>((byte) 5, 0x36));//엔피시대화
            keymap.put(Integer.valueOf(59), new Pair<Byte, Integer>((byte) 6, 100));//이모션들
            keymap.put(Integer.valueOf(60), new Pair<Byte, Integer>((byte) 6, 101));
            keymap.put(Integer.valueOf(61), new Pair<Byte, Integer>((byte) 6, 102));
            keymap.put(Integer.valueOf(62), new Pair<Byte, Integer>((byte) 6, 103));
            keymap.put(Integer.valueOf(63), new Pair<Byte, Integer>((byte) 6, 104));
            keymap.put(Integer.valueOf(64), new Pair<Byte, Integer>((byte) 6, 105));
            keymap.put(Integer.valueOf(65), new Pair<Byte, Integer>((byte) 6, 106));
            return;
        }
        Pair<Byte, Integer> binding;
        for (int x = 0; x < 89; x++) {
            binding = keymap.get(Integer.valueOf(x));
            if (binding != null) {
                mplew.write(binding.getLeft());
                mplew.writeInt(binding.getRight());
            } else {
                mplew.write(0);
                mplew.writeInt(0);
            }
        }
    }

    public final void saveKeys(final int charid, Connection con) {
        if (!changed) {
            return;
        }
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM keymap WHERE characterid = ?");
            ps.setInt(1, charid);
            ps.executeUpdate();
            ps.close();
            if (keymap.isEmpty()) {
                return;
            }
            boolean first = true;
            StringBuilder query = new StringBuilder();

            for (Entry<Integer, Pair<Byte, Integer>> keybinding : keymap.entrySet()) {
                if (first) {
                    first = false;
                    query.append("INSERT INTO keymap VALUES (");
                } else {
                    query.append(",(");
                }
                query.append("DEFAULT,");
                query.append(charid).append(",");
                query.append(keybinding.getKey().intValue()).append(",");
                query.append(keybinding.getValue().getLeft().byteValue()).append(",");
                query.append(keybinding.getValue().getRight().intValue()).append(")");
            }
            ps = con.prepareStatement(query.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
