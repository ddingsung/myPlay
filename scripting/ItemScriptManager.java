/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation version 3 as published by
 the Free Software Foundation. You may not use, modify or distribute
 this program under any other version of the GNU Affero General Public
 License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package scripting;

import client.MapleClient;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ItemScriptManager {

    private static final ItemScriptManager instance = new ItemScriptManager();
    private ScriptEngineFactory sef;

    private ItemScriptManager() {
        ScriptEngineManager sem = new ScriptEngineManager();
        sef = sem.getEngineByName("javascript").getFactory();
    }

    public static ItemScriptManager getInstance() {
        return instance;
    }

    public boolean scriptExists(String scriptName) {
        File scriptFile = new File("scripts/item/" + scriptName + ".js");
        return scriptFile.exists();
    }

    public void getItemScript(MapleClient c, String scriptName, int npcId, int itemId) {
        File scriptFile = new File("scripts/item/" + scriptName + ".js");
        if (!scriptFile.exists()) {
            c.sendPacket(MaplePacketCreator.enableActions());
            return;
        }
        FileReader fr = null;
        ScriptEngine portal = sef.getScriptEngine();
        try {
            fr = new FileReader(scriptFile);
            CompiledScript compiled = ((Compilable) portal).compile(fr);
            compiled.eval();

            final Invocable script = ((Invocable) portal);
            script.invokeFunction("use", new ItemScriptMethods(c, npcId, itemId, scriptName));
        } catch (Exception e) {
            String msg = "Error executing ITEM script, ITEM ID : " + itemId + "." + e;
            System.err.println(msg);
            FileoutputUtil.log(FileoutputUtil.ScriptEx_Log, msg);
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
