package handling.cashshop;

import client.MapleClient;
import constants.ServerConstants;
import handling.ServerType;
import handling.SessionOpen;
import server.GeneralThreadPool;
import tools.SystemUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CashShopServerThread extends Thread {

    protected ServerSocket _serverSocket;
    private static Logger _log = Logger.getLogger(CashShopServerThread.class.getName());

    @Override
    public void run() {

        System.out.println("CashShop Server Thread Started. Memory used : " + SystemUtils.getUsedMemoryMB() + "MB");

        while (!CashShopServer.isShutdown()) {
            try {
                Socket socket = _serverSocket.accept();
                System.out.println("CashShop New Connection from "
                        + socket.getInetAddress());
                String host = socket.getInetAddress().getHostAddress();
                if (SessionOpen.sessionOpen(host, ServerType.CASHSHOP, -10)) {
                    // Session OK!
                    MapleClient client = new MapleClient(socket, -10, !ServerConstants.Use_Fixed_IV);
                    GeneralThreadPool.getInstance().execute(client);
                } else {
                    // Session Failed or Banned
                    _log.log(Level.INFO, "Session Opening Failed on ({0})", host);
                }
            } catch (IOException ioe) {
            }
        }
    }
}
